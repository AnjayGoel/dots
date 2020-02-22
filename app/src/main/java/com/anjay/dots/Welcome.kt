package com.anjay.dots

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.NumberPicker
import androidx.fragment.app.FragmentActivity

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class Welcome : FragmentActivity() {
    var play: Button? = null
    var picker: NumberPicker? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)
        play = findViewById<View>(R.id.play) as Button
        picker = findViewById<View>(R.id.picker) as NumberPicker
        picker!!.displayedValues = arrayOf("2", "3", "4")
        picker!!.minValue = 0
        picker!!.maxValue = 2
        picker!!.value = 0
        play!!.setOnClickListener { launch() }
    }

    private fun launch() {
        val launch = Intent(this, PlayActivity::class.java)
        launch.putExtra("no_of_players", picker!!.value + 2)
        startActivity(launch)
    }
}