package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.myapplication.databinding.WebActivityBinding

class WebActivity: AppCompatActivity() {

    companion object {
        const val EXTRA = "extra"
        fun createIntent(context: Context, url: String): Intent {
            return Intent(context, WebActivity::class.java).apply {
                putExtra(EXTRA, url)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = DataBindingUtil.setContentView<WebActivityBinding>(this, R.layout.web_activity)
        val url = intent.getStringExtra(EXTRA)
        url?.let {
            binding.webview.loadUrl(it)
        }
    }
}