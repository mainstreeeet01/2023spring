package com.example.myapplication

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.DetailActivityBinding
import kotlinx.coroutines.launch

class DetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA = "extra"

        fun createIntent(context: Context, data: ListItemData): Intent {
            return Intent(context, DetailActivity::class.java).apply {
                putExtra(EXTRA, data)
            }
        }
    }

    private lateinit var binding: DetailActivityBinding
    private val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.detail_activity)
        init()
        observe()
    }

    private fun observe() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.title.collect {
                    if (it.isNotEmpty()) {
                        setResult(Activity.RESULT_OK, Intent().apply {
                            putExtra(EXTRA, it)
                        })
                        finish()
                    }
                }
            }
        }
    }

    private fun init() {
        val data: ListItemData? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA, ListItemData::class.java)
        } else {
            intent.getParcelableExtra(EXTRA)
        }

        binding.apply {
            item = data
            viewModel = this@DetailActivity.viewModel
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

            data?.keywords?.let {
                Log.d("TAG", "init: ${it.sortedBy { it.isDuplicate }}")
                (adapter as? BubbleAdapter)?.setItems(it.sortedByDescending { it.isDuplicate }, true)
            }
        }

        viewModel.moveToWeb.observe(this) {
            startActivity(WebActivity.createIntent(this, it))
        }
    }
}