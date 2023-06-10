import requests, json, sqlite3
import threading
from bs4 import BeautifulSoup
from keybert import KeyBERT
from kiwipiepy import Kiwi
from transformers import BertModel, AutoTokenizer, pipeline, AutoModelForTokenClassification,TokenClassificationPipeline
from transformers.pipelines import AggregationStrategy
import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
import difflib
from krwordrank.hangle import normalize
from krwordrank.word import KRWordRank
from soykeyword.lasso import LassoKeywordExtractor
from flask import Flask, jsonify, request
import RAKE, nltk
from rake_nltk import Rake
from summa import keywords
import re
import math
from collections import Counter
import gensim.downloader
from gensim.models import KeyedVectors





app = Flask(__name__)
app.config['JSON_AS_ASCII'] = False

class Data:
    def __init__(self, category, title, desc, keywords, url):
        self.url = url
        self.category = category
        self.title = title
        self.desc = desc
        self.keywords = keywords


class KeywordData:
    def __init__(self, keyword, isDuplicate):
        self.keyword = keyword
        self.isDuplicate = isDuplicate

            
    def __eq__(self, other):
        return self.keyword == other.keyword

    def __hash__(self):
        return hash(self.keyword)

dataList = []
keywordSet = set()
pageSize = 100
global kw_model
global connect
global kw_model2
global glove_vectors


def word2vec(word):
    from collections import Counter
    from math import sqrt

    # count the characters in word
    cw = Counter(word)
    # precomputes a set of the different characters
    sw = set(cw)
    # precomputes the "length" of the word vector
    lw = sqrt(sum(c*c for c in cw.values()))

    # return a tuple
    return cw, sw, lw

def cosdis(v1, v2):
    # which characters are common to the two words?
    common = v1[1].intersection(v2[1])
    # by definition of cosine distance we have
    return sum(v1[0][ch]*v2[0][ch] for ch in common)/v1[2]/v2[2]

def get_cosine(vec1, vec2):
    intersection = set(vec1.keys()) & set(vec2.keys())
    numerator = sum([vec1[x] * vec2[x] for x in intersection])

    sum1 = sum([vec1[x]**2 for x in vec1.keys()])
    sum2 = sum([vec2[x]**2 for x in vec2.keys()])
    denominator = math.sqrt(sum1) * math.sqrt(sum2)

    if not denominator:
        return 0.0
    else:
        return float(numerator) / denominator


def text_to_vector(text):
    word = re.compile(r'\w+')
    words = word.findall(text)
    return Counter(words)


def get_result(content_a, content_b):
    text1 = content_a
    text2 = content_b

    vector1 = text_to_vector(text1)
    vector2 = text_to_vector(text2)

    cosine_result = get_cosine(vector1, vector2)
    return cosine_result

def simility(a, b):
    ab = bytes(a, 'utf-8')
    bb = bytes(b, 'utf-8')
    alist = list(ab)
    blist = list(bb)

    sm = difflib.SequenceMatcher(None, alist, blist)
    return sm.ratio()

def EN(keys):
    kiwi = Kiwi()

    keywordLists = []
    # print(keys)
    for keyword in keys:
        result = kiwi.analyze(keyword)
        # print(keyword)
        okay = True
        for token, pos, _, _ in result[0][0]:
            # print(token, pos)
            condition = pos.startswith('N') or pos == 'XSN'
            if condition == False:
                okay = False
                break
        if okay == True:
            keywordLists.append(keyword)
    filteredList = list(set(keywordLists))
    # if len(filteredList) > 3:
    #     filteredList = filteredList[:3]
    return filteredList

def crawling():
    initDB()
    kiwi = Kiwi()

    global connect, kw_model, kw_model2
    cursor = connect.cursor()

    crawling = False
    dB = False
    if crawling:
        for page in range(1, 110):
            url = f"https://www.data.go.kr/tcs/dss/selectDataSetList.do?dType=API&keyword=&detailKeyword=&publicDataPk=&recmSe=&detailText=&relatedKeyword=&commaNotInData=&commaAndData=&commaOrData=&must_not=&tabId=&dataSetCoreTf=&coreDataNm=&sort=updtDt&relRadio=&orgFullName=&orgFilter=&org=&orgSearch=&currentPage={page}&perPage={pageSize}&brm=&instt=&svcType=&kwrdArray=&extsn=&coreDataNmArray=&pblonsipScopeCode="
            
            response = requests.get(url)
            html = response.text
            # HTML 코드를 파싱합니다.
            soup = BeautifulSoup(html, 'html.parser')

            # div 태그 중 class가 "result-list"인 태그를 찾습니다.
            result_list = soup.find('div', {'class': 'result-list'})

            # result_list 태그 안에 있는 ul 태그를 찾습니다.
            ul_tag = result_list.find('ul')

            # ul 태그 안에 있는 li 태그를 모두 찾습니다.
            li_tags = ul_tag.find_all('li')

            # 각 li 태그에서 필요한 정보를 추출합니다.
            for li_tag in li_tags:

                # p 태그에서 labelset 클래스가 있는 span 태그를 모두 찾아서 텍스트를 추출합니다.
                label = [label.get_text().strip() for label in li_tag.select('p .labelset')][0]

                # dl 태그에서 title 클래스가 있는 span 태그의 텍스트를 추출합니다.
                title = li_tag.find('span', {'class': 'title'}).get_text().strip()
                title = title.replace('Update', '').replace('\n', '').strip()
                # title = title[0:title.find('  ')].replace('Update', '').replace('\n', '')

                # dd 태그에서 publicDataDesc 클래스가 있는 태그의 텍스트를 추출합니다.
                desc = li_tag.find('dd', {'class': 'publicDataDesc'}).get_text().strip()
                desc = desc.replace("'", '')

                # 키워드 정보를 추출합니다.
                keywords = li_tag.select_one('.info-data p:nth-child(5)').get_text().strip().replace(' ', '').replace('\n', '').replace('키워드', '').split(',')

                url = str(li_tag.select('dl dt a'))
                url = 'https://www.data.go.kr/' + url[url.find("/"):url.find('>',) - 1]

                calKeywords = kw_model.extract_keywords(f'{title} {desc}', keyphrase_ngram_range=(1, 1), stop_words=None, top_n=3)
                keywordList = []
                for keyword in calKeywords:
                    results = []
                    result = kiwi.analyze(keyword[0])
                    for token, pos, _, _ in result[0][0]:
                        if len(token) != 1 and pos.startswith('N') or pos.startswith('SL'):
                            results.append(token)
                    for res in results:
                        keywordList.append(res)
                filteredList = list(set(keywordList))
                if len(filteredList) > 3:
                    filteredList = filteredList[:3]

                keywordsStr = ", ".join(filteredList)                

                try:
                    cursor.execute(f"INSERT INTO TEST_TABLE (category, title, desc, keywords, url) VALUES(\'{label}\', \'{title}\', \'{desc}\', \'{keywordsStr}\', \'{url}\')")
                except:
                    print(label, title, desc, keywordsStr, url, sep='\n')
            print(page)
            connect.commit()
        print("fin")
    elif dB:
        cursor.execute("SELECT * FROM TEST_TABLE")
        fetchList = cursor.fetchall()
        for data in fetchList:
            category = data[1]
            title = data[2]
            desc = data[3]
            keywordList = data[4].split(", ")
            url = data[5]

            # text = normalize(f'{title} {desc}', english=True, number=True)
            text = f'{title} {desc}'
            wordrank_extractor = KRWordRank(
                min_count = 2, # 단어의 최소 출현 빈도수 (그래프 생성 시)
                max_length = 10, # 단어의 최대 길이
            )

            beta = 0.85    # PageRank의 decaying factor beta
            max_iter = 10

            texts = []
            texts.append(text)
            
            textRankKeywords = []
            try:
                keyword, rank, graph = wordrank_extractor.extract(texts, beta, max_iter)
                
                for word, r in sorted(keyword.items(), key=lambda x:x[1], reverse=True)[:3]:
                    textRankKeywords.append(word)
            except:
                print('error')
            
            # print(textRankKeywords)
            textRankKeywords = EN(textRankKeywords)

            result = kw_model2.extract_keywords(text, keyphrase_ngram_range=(1,1),stop_words=None, top_n=3)
            results = []
            for item in result:
                results.append(item[0])
            results = EN(results)


            keybert = kw_model.extract_keywords(text, keyphrase_ngram_range=(1,1),stop_words=None, top_n=3)
            keybertList = []
            for item in keybert:
                keybertList.append(item[0])
            keybertList = EN(keybertList)
            
            keybertList.extend(results)
            keybertList.extend(textRankKeywords)
            resultSet = set()
            for item in keybertList:
                if keybertList.count(item) > 1:
                    resultSet.add(KeywordData(item, True))
                else:
                    resultSet.add(KeywordData(item, False))

            jsonStr = []
            for item in resultSet:
                jsonStr.append(json.dumps(item.__dict__, ensure_ascii=False))
            try:
                cursor.execute(f"update TEST_TABLE set keywords = \'{json.dumps(jsonStr, ensure_ascii=False)}\' WHERE url == \'{url}\';")
            except:
                print(title)
        print('fin')

    else:
        print('start')
        cursor.execute("SELECT * FROM TEST_TABLE")
        fetchList = cursor.fetchall()
        for data in fetchList:
            category = data[1]
            title = data[2]
            desc = data[3]
            keywordList = data[4]
            url = data[5]
            # filteredList = []
            keywordList = json.loads(keywordList)
            for item in keywordList:
                dict = json.loads(item)
                # filteredList.append(KeywordData(dict['keyword'].replace('_', ''), dict['isDuplicate']))
                keywordSet.add(dict['keyword'])
            dataList.append(Data(category, title, desc, data[4], url))
        print(fetchList[0])
        print('fin')


def initModel():
    model = BertModel.from_pretrained('skt/kobert-base-v1')
    global kw_model, kw_model2, extractor, glove_vectors
    kw_model = KeyBERT(model)
    kw_model2 = KeyBERT('distilbert-base-nli-mean-tokens')
    

def initDB():
    global connect
    connect = sqlite3.connect("temp.db", isolation_level=None)
    cursor = connect.cursor()

    #cursor.execute("CREATE TABLE IF NOT EXISTS TEST_TABLE(id INTEGER PRIMARY KEY AUTOINCREMENT, category TEXT, title TEXT, desc TEXT, keywords TEXT, url TEXT)")
    #cursor.execute("alter TABLE TEST_TABLE ADD textRankKeyword TEXT sentenceKeyBert TEXT;")
    #cursor.execute("alter TABLE TEST_TABLE ADD sentenceKeyBert TEXT;")

    

@app.route("/")
def hello():
    initModel()

    thread = threading.Thread(target=crawling)
    thread.start()
    return "hello"

@app.route("/data")
def getData():
    jsonList = []
    for data in dataList:
        dic = {}
        dic['category'] = data.category
        dic['title'] = data.title
        dic['desc'] = data.desc
        dic['keywordStr'] = data.keywords
        dic['url'] = data.url
        jsonList.append(dic)
    print(jsonList[0])
    return json.dumps(jsonList, ensure_ascii=False)

@app.route("/search")
def getSearchData():
    global glove_vectors
    query = request.args['query']
    print(query)

    threashold = 0.7
    similarList = []
    for item in keywordSet:
        result = cosdis(word2vec(query), word2vec(item))
        if (result >= threashold):
            similarList.append(item)

    return similarList

if __name__ == "__main__":
    # app.debug = True
    app.run(host='0.0.0.0', port=443)



