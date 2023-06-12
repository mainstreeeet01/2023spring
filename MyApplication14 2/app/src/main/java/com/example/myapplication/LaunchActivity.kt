package com.example.myapplication

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.KeywordResponseData
import com.example.myapplication.databinding.ActivityLaunchBinding
import com.example.myapplication.databinding.ListItemViewBinding
import com.example.myapplication.databinding.ListMainItemViewBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class LaunchActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLaunchBinding
    private val viewModel by viewModels<MainViewModel>()

    var firstLoad = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_launch)
        init()
        observe()
        viewModel.getData()
    }

    private fun observe() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.searchBubbleItems.collect {
                    (binding.recyclerView.adapter as? BubbleAdapter)?.setItems(it)

                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.keyword.collect {
                    viewModel.setSearchBubbleItems(it)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.title.collect {
                    if (it.isNotEmpty()) {
                        viewModel.reset()
                        startActivity(MainActivity.createIntent(this@LaunchActivity, it))
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.searchResult.collect {
                    (binding.searchRv.adapter as? BubbleAdapter)?.setItems(it.map { KeywordResponseData(it, false) })
                }
            }
        }

        viewModel.initEvent.observe(this) {
            (binding.recyclerView.adapter as? BubbleAdapter)?.setItems(it)
            if (firstLoad && it.isNotEmpty()) {
                firstLoad = false
                (binding.bottomRecyclerView.adapter as? ListMainAdapter)?.setItems(it.map { ListItemData(it.keyword) })
            }
        }
    }

    private fun init() {
        binding.apply {
            viewModel = this@LaunchActivity.viewModel
            lifecycleOwner = this@LaunchActivity
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
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

        binding.searchRv.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
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
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = ListMainAdapter(viewModel)
        }
    }
}

class ListMainVH(private val binding: ListMainItemViewBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: ListItemData?, viewModel: MainViewModel) {
        binding.viewModel = viewModel
        binding.item = item
        binding.executePendingBindings()
    }
}

class ListMainAdapter(private val viewModel: MainViewModel) : RecyclerView.Adapter<ListMainVH>() {
    private val itemList = mutableListOf<ListItemData>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListMainVH {
        return ListMainVH(ListMainItemViewBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int = itemList.size

    override fun onBindViewHolder(holder: ListMainVH, position: Int) {
        holder.bind(itemList.getOrNull(position), viewModel)
    }

    fun setItems(items: List<ListItemData>) {
        itemList.clear()
        itemList.addAll(items)
        notifyDataSetChanged()
    }
}