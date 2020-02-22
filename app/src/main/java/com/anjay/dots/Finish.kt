package com.anjay.dots

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_finish.*

class Finish : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_finish)
        message.text = "Player ${intent.getIntExtra("Winner", 1)} Wins"
        restart.setOnClickListener {
            var i = Intent(this, Welcome::class.java)
            startActivity(i)
            finish()
        }
    }
}