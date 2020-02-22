package com.anjay.dots

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import kotlinx.android.synthetic.main.activity_welcome.*

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class Welcome : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)
        picker.displayedValues = arrayOf("2", "3", "4")
        picker.minValue = 0
        picker.maxValue = 2
        picker.value = 0
        picker2.displayedValues = arrayOf("5", "6", "7", "8", "9", "10")
        picker2.minValue = 5
        picker2.maxValue = 10
        picker2.value = 5
        play.setOnClickListener { launch() }
    }

    private fun launch() {
        val launch = Intent(this, PlayActivity::class.java)
        launch.putExtra("noOfPlayers", picker.value + 2)
        launch.putExtra("gridSize", picker2.value + 2)
        startActivity(launch)
        finish()
    }
}