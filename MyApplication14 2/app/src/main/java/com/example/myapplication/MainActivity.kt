package com.example.myapplication

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.KeywordResponseData
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.databinding.BubbleViewItemBinding
import com.example.myapplication.databinding.ListItemViewBinding
import ir.mahozad.android.PieChart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun Number.toDp(): Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), Resources.getSystem().displayMetrics).toInt()

class MainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA = "extra"

        fun createIntent(context: Context, title: String): Intent {
            return Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA, title)
            }
        }
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var registerResult: ActivityResultLauncher<Intent>
    private val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        init()
        observe()
        viewModel.getData()
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleScope.launch(Dispatchers.Main) {
            registerResult.unregister()
        }
    }

    private fun observe() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.bubbleItems.collect {
                    (binding.recyclerView.adapter as? BubbleAdapter)?.setItems(it)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.listItems.collect {
                    (binding.bottomRecyclerView.adapter as? ListAdapter)?.setItems(it)
                }
            }
        }

        viewModel.event.observe(this) {
            registerResult.launch(DetailActivity.createIntent(this@MainActivity, it))
        }


        viewModel.chartItems.observe(this) {
            binding.chat.apply {
                slices = it
                labelType = PieChart.LabelType.INSIDE
                isLegendEnabled = true
                legendsColor = Color.GRAY
                legendPosition = PieChart.LegendPosition.BOTTOM
            }
        }

        viewModel.initEvent.observe(this) {
            viewModel.init(intent.getStringExtra(EXTRA).orEmpty())
        }

    }

    private fun init() {
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = BubbleAdapter(viewModel)
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                    super.getItemOffsets(outRect, view, parent, state)

                    val position = parent.getChildAdapterPosition(view)
                    if (position == 0) {
                        outRect.left = 10.toDp()
                        outRect.right = 5.toDp()
                    } else if (position + 1 == parent.adapter?.itemCount) {
                        outRect.right = 10.toDp()
                    } else {
                        outRect.right = 5.toDp()
                    }
                }
            })
        }

        binding.bottomRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.VERTICAL, false)
            adapter = ListAdapter(viewModel)
        }

        registerResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.let { intent ->
                    intent.getStringExtra(DetailActivity.EXTRA)?.let { title ->
                        viewModel.onClickBubble(title)
                    }
                }
            }
        }
    }
}

class ListVH(private val binding: ListItemViewBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: ListItemData?, viewModel: MainViewModel) {
        binding.viewModel = viewModel
        binding.item = item
        binding.executePendingBindings()
    }
}

class ListAdapter(private val viewModel: MainViewModel) : RecyclerView.Adapter<ListVH>() {
    private val itemList = mutableListOf<ListItemData>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListVH {
        return ListVH(ListItemViewBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int = itemList.size

    override fun onBindViewHolder(holder: ListVH, position: Int) {
        holder.bind(itemList.getOrNull(position), viewModel)
    }

    fun setItems(items: List<ListItemData>) {
        itemList.clear()
        itemList.addAll(items)
        notifyDataSetChanged()
    }
}

class BubbleAdapter(private val viewModel: MainViewModel) : RecyclerView.Adapter<BubbleVH>() {
    private val itemList = mutableListOf<KeywordResponseData>()
    private var isDetailScreen = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BubbleVH {
        return BubbleVH(BubbleViewItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int = itemList.size

    override fun onBindViewHolder(holder: BubbleVH, position: Int) {
        holder.bind(itemList.getOrNull(position), viewModel, isDetailScreen)
    }

    fun setItems(items: List<KeywordResponseData>, isDetailScreen: Boolean = false) {
        Log.d("setItems", items.toString())
        itemList.clear()
        itemList.addAll(items)
        this.isDetailScreen = isDetailScreen
        notifyDataSetChanged()
    }
}

class BubbleVH(private val binding: BubbleViewItemBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: KeywordResponseData?, viewModel: MainViewModel, isDetailScreen: Boolean) {
        binding.viewModel = viewModel
        binding.item = item
        binding.isDetail = isDetailScreen
        binding.executePendingBindings()
    }
}