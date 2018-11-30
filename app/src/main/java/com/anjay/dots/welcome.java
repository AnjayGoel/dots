package com.anjay.dots;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class welcome extends FragmentActivity {
Button play;
NumberPicker picker;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_welcome);
        play = (Button)findViewById(R.id.play);
        picker= (NumberPicker)findViewById(R.id.picker);
        picker.setDisplayedValues(new String[] {"2","3","4",});
        picker.setMinValue(0);
        picker.setMaxValue(2);
        picker.setValue(0);
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               launch();

            }
        });
    }

void launch (){
    Intent launch = new Intent(this,Play_Activity.class);
    launch.putExtra("no_of_players",picker.getValue()+2);
    startActivity(launch);

}

}
