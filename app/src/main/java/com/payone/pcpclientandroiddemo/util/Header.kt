package com.payone.pcpclientandroiddemo.util

import androidx.appcompat.app.AppCompatActivity

class Header(
    private val activity: AppCompatActivity,
    private val title: String
) {
    fun setup() {
        val actionBar = activity.supportActionBar
        actionBar?.title = title
        if (!activity.isTaskRoot) {
            actionBar?.setDisplayHomeAsUpEnabled(true)
        }
    }

    companion object {
        fun onNavigateUp(activity: AppCompatActivity): Boolean {
            activity.onBackPressedDispatcher.onBackPressed()
            return true
        }
    }
}