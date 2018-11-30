package com.anjay.dots;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

public class Play_Activity extends FragmentActivity {
    Context con;
    myCanvas gameCanvas;
    static public TextView info;
    Point dimensions = new Point();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        con = this;

        getWindowManager().getDefaultDisplay().getSize(dimensions);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);
        gameCanvas = (myCanvas) findViewById(R.id.gameCanvas);
        info = (TextView) findViewById(R.id.info);
        gameCanvas.set_no_of_players(getIntent().getIntExtra("no_of_players",4));
        String s = getIntent().getIntExtra("no_of_players",4)+"";
        info.setText(s);
        gameCanvas.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_MOVE:

                        gameCanvas.stretch((int) event.getX(), (int) event.getY());
                        break;

                    case MotionEvent.ACTION_DOWN:

                        gameCanvas.down((int) event.getX(), (int) event.getY());

                        break;

                    case MotionEvent.ACTION_UP:
                        gameCanvas.up((int) event.getX(), (int) event.getY());
                        break;
                }


                return true;
            }
        });

    }
    @Override
    protected void   onPause(){
        super.onPause();
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return super.onKeyUp(keyCode, event);
    }
}

class myCanvas extends SurfaceView {
    SurfaceHolder holder;
    long time = 0;
    Paint myPaint;
    int dots_per_col = 7;
    int total_no_of_dots = dots_per_col * dots_per_col;
    Dot[] dots;
    Line[] lines;
    Square[] squares;
    int dot_radius;
    int focused_dot;
    int dot_prev_color = Color.GRAY;
    float stroke_width = dot_radius / 2;
    Player[] players;
    int no_of_players;
    int current_player=0;


    void toggle_player(){
        if (current_player==no_of_players-1)current_player=0;
        else current_player++;

    }
    public void set_no_of_players (int n){
     this.no_of_players=n;
        int[] arr= new int[]{Color.RED,Color.BLUE,Color.GREEN,Color.YELLOW};
        players = new Player[n];
        for (int i=0;i<n;i++){
            players[i]=new Player(arr[i]);
        }
    }
    public myCanvas(Context c, AttributeSet attrs) {
        super(c, attrs);
        myPaint = new Paint();
        myPaint.setColor(Color.GRAY);
        holder = getHolder();
        dots = new Dot[total_no_of_dots];
        lines=new Line[2*(dots_per_col *(dots_per_col -1))];
        squares=new Square[total_no_of_dots];
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Canvas c = holder.lockCanvas(null);
                init(c);
                holder.unlockCanvasAndPost(c);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });
    }

    void init(Canvas canvas) {

        int x = getWidth();
        int y = getHeight();
        int uY = (y - x) / 2;

        int Margin = getWidth() / (dots_per_col + 2);
        dot_radius=Margin/4;
        stroke_width=Margin/6;
        for (int j = uY + Margin, col = 0; col < dots_per_col; j += Margin, col++) {
            for (int i = (3 * Margin) / 2, row = 0; row < dots_per_col; i += Margin, row++) {
                dots[col * dots_per_col + row] = new Dot(i, j, col * dots_per_col + row);


            }

        }
        Draw(canvas);


    }

    boolean check_if_adjacent(int fixed_dot, int d_to_check) {
        int[] adjacent = {fixed_dot - 1, fixed_dot + 1, fixed_dot + dots_per_col, fixed_dot - dots_per_col};

        for (int i = 0; i < 4; i++) {
            if (d_to_check == adjacent[i]) {
                return true;
            }


        }
        return false;
    }

    void up(int x, int y) {
        String info = "";
        time = System.currentTimeMillis();
        if (focused_dot == -1) return;
        int nearest_d = get_nearest(x, y);
        Canvas c = holder.lockCanvas();
        if (nearest_d == -1 || nearest_d == focused_dot || !check_if_adjacent(focused_dot, nearest_d) || check_connected(focused_dot, nearest_d)) {
            dots[focused_dot].color = dot_prev_color;
            Draw(c);
            holder.unlockCanvasAndPost(c);
            return;

        } else {
            dots[nearest_d].color = dots[focused_dot].color = players[current_player].color;
            lines[Line.counter+1]=new Line(dots[nearest_d].x, dots[nearest_d].y, dots[focused_dot].x, dots[focused_dot].y,players[current_player].color);
            int arr[] = check_if_forms_square(focused_dot, nearest_d);
            if (arr[0] != 0 ) {
                players[current_player].points+=arr[0];
                squares[Square.counter+1]=new Square( dots[focused_dot].x, dots[focused_dot].y,arr[1],arr[2],players[current_player].color);
                if (arr[0]==2)squares[Square.counter+1]=new Square( dots[focused_dot].x, dots[focused_dot].y,arr[3],arr[4],players[current_player].color);
                if (Square.counter + 1 == (dots_per_col - 1) * (dots_per_col - 1)){
                    info += " Grid Complete";
                Play_Activity.info.setText("Player "+get_highest_scorer()+" wins" );
                }

            }
            connect(focused_dot, nearest_d);
            toggle_player();
        }
        Draw(c);
        holder.unlockCanvasAndPost(c);
     Log.d("abz","Current frame rendering time in up is " + (System.currentTimeMillis() - time) + info);
    }
    int get_highest_scorer (){
        int highest_scorer=0;
        float highest_score = players[0].points;
        for (int i=0;i<no_of_players;i++){
            if (players[i].points>highest_score){
                highest_score=players[i].points;
                highest_scorer=i;
            }

        };

        return highest_scorer;
    }
    void Draw(Canvas c) {
        myPaint.setAntiAlias(true);
        myPaint.setColor(Color.WHITE);
        c.drawPaint(myPaint);
        myPaint.setStyle(Paint.Style.FILL);
       ;
        for (int i=0;i<=Square.counter;i++){
            myPaint.setColor(squares[i].color);
            Log.d ("abz",i+" val of i for debugging");
            c.drawRect(squares[i].x1, squares[i].y1, squares[i].x2, squares[i].y2, myPaint);
        }

        for (int i=0;i<=Line.counter;i++){
            myPaint.setColor(lines[i].color);
            myPaint.setStrokeWidth(stroke_width);
            c.drawLine(lines[i].x1, lines[i].y1, lines[i].x2, lines[i].y2, myPaint);
        }
        for (int curr_dot = 0; curr_dot < total_no_of_dots; curr_dot++) {
            myPaint.setColor(dots[curr_dot].color);
            c.drawCircle(dots[curr_dot].x, dots[curr_dot].y, dot_radius, myPaint);
        }


    }

    int[] check_if_forms_square(int first, int second) {

        int[] arr = new int[5];
        arr[0] = 0;
        for (int i : dots[first].get_connected_dots()) {
            for (int j : dots[i].get_connected_dots()) {
                for (int k : dots[j].get_connected_dots()) {
                    if (k == second) {
                        if (arr[0]!=0){
                            arr[0]=2;
                            arr[3]=dots[j].x;
                            arr[4]=dots[j].y;
                        }
                        else {
                            arr = new int[]{1, dots[j].x, dots[j].y, 0, 0};
                        }
                    }
                }
            }

        }
        return arr;
    }

    void down(int x, int y) {
        time = System.currentTimeMillis();

        focused_dot = get_nearest(x, y);
        if (focused_dot == -1) return;
        dot_prev_color = dots[focused_dot].color;
        dots[focused_dot].color = players[current_player].color;
        Canvas c = holder.lockCanvas(null);
        Draw(c);
        holder.unlockCanvasAndPost(c);
        Log.d("abz", "Current frame rendering time is " + (System.currentTimeMillis() - time));
    }

    int get_nearest(int x, int y) {
        int least_x_distance = Math.abs(x - dots[0].x);
        int least_x = 0;
        for (int i = 0; i < dots_per_col; i++) {
            if (Math.abs(x - dots[i].x) < least_x_distance) {
                least_x_distance = Math.abs(x - dots[i].x);
                least_x = i;
            }
        }
        int nearest_dot = least_x;
        int least_y_distance = Math.abs(y - dots[nearest_dot].y);
        for (int i = nearest_dot; i < total_no_of_dots; i += dots_per_col) {
            if (Math.abs(y - dots[i].y) < least_y_distance) {
                least_y_distance = Math.abs(y - dots[i].y);
                nearest_dot = i;

            }
        }
        if (Math.abs(x - dots[nearest_dot].x) > 60 || Math.abs(y - dots[nearest_dot].y) > 60)
            nearest_dot = -1;

        return nearest_dot;

    }

    void stretch(int x, int y) {
        time = System.currentTimeMillis();
        if (focused_dot == -1) return;
        Canvas c = holder.lockCanvas(null);
        Draw(c);
        myPaint.setStrokeWidth(stroke_width);
        myPaint.setColor(players[current_player].color);
        c.drawLine(dots[focused_dot].x, dots[focused_dot].y, x, y, myPaint);
        c.drawCircle(dots[focused_dot].x, dots[focused_dot].y, stroke_width / 2, myPaint);
        c.drawCircle(x, y, stroke_width / 2, myPaint);
        holder.unlockCanvasAndPost(c);
        Log.d("abz", "Current frame rendering time in stretch is " + (System.currentTimeMillis() - time));
    }

    void connect(int f, int s) {
        dots[f].add_connected(s);
        dots[s].add_connected(f);
    }

    boolean check_connected(int first, int second) {
        if (dots[first].counter == -1)return false;
        for (int i = 0; i <= dots[first].counter; i++) {
            if (dots[first].adjacent_dots[i] == second) return true;
        }
        return false;
    }
}

class Line {
    static int counter=-1;
    int color = Color.GRAY;
    int x1, x2, y1, y2;

    Line(int x1, int y1, int x2, int y2, int color) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.color =color;
        counter++;
    }

}

class Square {
    static int counter =-1;
    int color = Color.RED;
    int x1, x2, y1, y2;

    Square(int x1, int y1, int x2, int y2, int color) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.color =color;
        counter++;
    }

}

class Dot {
    protected int x;
    protected int y;
    protected int self_pos;
    protected int color;
    int counter = -1;
    int[] adjacent_dots = new int[4];

    Dot(int x, int y, int pos) {
        adjacent_dots[0] = -1;
        this.self_pos = pos;
        this.color = Color.GRAY;
        this.x = x;
        this.y = y;
    }

    void add_connected(int d) {
        if (counter == 3) return;
        for (int i = 0; i <= counter; i++) {
            if (d == adjacent_dots[i]) return;
        }
        counter++;
        adjacent_dots[counter] = d;

    }

    int[] get_connected_dots() {
        if (counter == -1) {
            return new int[]{self_pos};
        }
        int[] coors = new int[counter + 1];
        System.arraycopy(adjacent_dots, 0, coors, 0, counter + 1);
        return coors;
    }
}
class Player {
    static int total_players=0;
    float points=0;
    int color;
    Player (int color){
    this.color = color;
        total_players++;
    }


}