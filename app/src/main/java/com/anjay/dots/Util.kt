package com.anjay.dots

import android.util.Log

val LOG_TAG = "SilverBug"

fun lg(s: String) {
    Log.d(LOG_TAG, s)
}

fun lg(i: Int) {
    lg("" + i)
}