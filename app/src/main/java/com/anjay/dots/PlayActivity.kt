package com.anjay.dots

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.widget.TextView

class PlayActivity : Activity() {
    var con: Context? = null
    private var gameCanvas: myCanvas? = null
    var dimensions = Point()
    override fun onCreate(savedInstanceState: Bundle?) {
        con = this
        windowManager.defaultDisplay.getSize(dimensions)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play)
        gameCanvas = findViewById<View>(R.id.gameCanvas) as myCanvas
        info = findViewById<View>(R.id.info) as TextView
        gameCanvas!!.set_no_of_players(intent.getIntExtra("no_of_players", 4))
        val s = intent.getIntExtra("no_of_players", 4).toString() + ""
        info!!.text = s
        gameCanvas!!.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_MOVE -> gameCanvas!!.stretch(event.x.toInt(), event.y.toInt())
                MotionEvent.ACTION_DOWN -> gameCanvas!!.down(event.x.toInt(), event.y.toInt())
                MotionEvent.ACTION_UP -> gameCanvas!!.up(event.x.toInt(), event.y.toInt())
            }
            true
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return super.onKeyUp(keyCode, event)
    }

    companion object {
        var info: TextView? = null
    }
}

private class myCanvas(c: Context?, attrs: AttributeSet?) : SurfaceView(c, attrs) {
    var surfaceHolder: SurfaceHolder
    var time: Long = 0
    var myPaint: Paint
    var dots_per_col = 7
    var total_no_of_dots = dots_per_col * dots_per_col
    var dots: Array<Dot?>
    var lines: Array<Line?>
    var squares: Array<Square?>
    var dot_radius = 0
    var focused_dot = 0
    var dot_prev_color = Color.GRAY
    var stroke_width = dot_radius / 2.toFloat()
    private lateinit var players: Array<Player?>
    var no_of_players = 0
    var current_player = 0
    fun toggle_player() {
        if (current_player == no_of_players - 1) current_player = 0 else current_player++
    }


    override fun performClick(): Boolean {
        return true
    }
    fun set_no_of_players(n: Int) {
        no_of_players = n
        val arr = intArrayOf(Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW)
        players = arrayOfNulls(n)
        for (i in 0 until n) {
            players[i] = Player(arr[i])
        }
    }

    fun init(canvas: Canvas) {
        val x = width
        val y = height
        val uY = (y - x) / 2
        val Margin = width / (dots_per_col + 2)
        dot_radius = Margin / 4
        stroke_width = Margin / 6.toFloat()
        var j = uY + Margin
        var col = 0
        while (col < dots_per_col) {
            var i = 3 * Margin / 2
            var row = 0
            while (row < dots_per_col) {
                dots[col * dots_per_col + row] = Dot(i, j, col * dots_per_col + row)
                i += Margin
                row++
            }
            j += Margin
            col++
        }
        Draw(canvas)
    }

    fun check_if_adjacent(fixed_dot: Int, d_to_check: Int): Boolean {
        val adjacent = intArrayOf(fixed_dot - 1, fixed_dot + 1, fixed_dot + dots_per_col, fixed_dot - dots_per_col)
        for (i in 0..3) {
            if (d_to_check == adjacent[i]) {
                return true
            }
        }
        return false
    }

    fun up(x: Int, y: Int) {
        var info = ""
        time = System.currentTimeMillis()
        if (focused_dot == -1) return
        val nearest_d = get_nearest(x, y)
        val c = surfaceHolder.lockCanvas()
        if (nearest_d == -1 || nearest_d == focused_dot || !check_if_adjacent(focused_dot, nearest_d) || check_connected(focused_dot, nearest_d)) {
            dots[focused_dot]!!.color = dot_prev_color
            Draw(c)
            surfaceHolder.unlockCanvasAndPost(c)
            return
        } else {
            dots[focused_dot]!!.color = players[current_player]!!.color
            dots[nearest_d]!!.color = dots[focused_dot]!!.color
            lines[Line.counter + 1] = Line(dots[nearest_d]!!.x, dots[nearest_d]!!.y, dots[focused_dot]!!.x, dots[focused_dot]!!.y, players[current_player]!!.color)
            val arr = check_if_forms_square(focused_dot, nearest_d)
            if (arr[0] != 0) {
                players[current_player]!!.points += arr[0]
                squares[Square.counter + 1] = Square(dots[focused_dot]!!.x, dots[focused_dot]!!.y, arr[1], arr[2], players[current_player]!!.color)
                if (arr[0] == 2) squares[Square.counter + 1] = Square(dots[focused_dot]!!.x, dots[focused_dot]!!.y, arr[3], arr[4], players[current_player]!!.color)
                if (Square.counter + 1 == (dots_per_col - 1) * (dots_per_col - 1)) {
                    info += " Grid Complete"
                    PlayActivity.info!!.text = "Player " + get_highest_scorer() + " wins"
                }
            }
            connect(focused_dot, nearest_d)
            toggle_player()
        }
        Draw(c)
        surfaceHolder.unlockCanvasAndPost(c)
        Log.d("abz", "Current frame rendering time in up is " + (System.currentTimeMillis() - time) + info)
    }

    fun get_highest_scorer(): Int {
        var highest_scorer = 0
        var highest_score = players[0]!!.points
        for (i in 0 until no_of_players) {
            if (players[i]!!.points > highest_score) {
                highest_score = players[i]!!.points
                highest_scorer = i
            }
        }
        return highest_scorer
    }

    fun Draw(c: Canvas) {
        myPaint.isAntiAlias = true
        myPaint.color = Color.WHITE
        c.drawPaint(myPaint)
        myPaint.style = Paint.Style.FILL
        for (i in 0..Square.counter) {
            myPaint.color = squares[i]!!.color
            Log.d("abz", "$i val of i for debugging")
            c.drawRect(squares[i]!!.x1.toFloat(), squares[i]!!.y1.toFloat(), squares[i]!!.x2.toFloat(), squares[i]!!.y2.toFloat(), myPaint)
        }
        for (i in 0..Line.counter) {
            myPaint.color = lines[i]!!.color
            myPaint.strokeWidth = stroke_width
            c.drawLine(lines[i]!!.x1.toFloat(), lines[i]!!.y1.toFloat(), lines[i]!!.x2.toFloat(), lines[i]!!.y2.toFloat(), myPaint)
        }
        for (curr_dot in 0 until total_no_of_dots) {
            myPaint.color = dots[curr_dot]!!.color
            c.drawCircle(dots[curr_dot]!!.x.toFloat(), dots[curr_dot]!!.y.toFloat(), dot_radius.toFloat(), myPaint)
        }
    }

    fun check_if_forms_square(first: Int, second: Int): IntArray {
        var arr = IntArray(5)
        arr[0] = 0
        for (i in dots[first]!!.get_connected_dots()) {
            for (j in dots[i]!!.get_connected_dots()) {
                for (k in dots[j]!!.get_connected_dots()) {
                    if (k == second) {
                        if (arr[0] != 0) {
                            arr[0] = 2
                            arr[3] = dots[j]!!.x
                            arr[4] = dots[j]!!.y
                        } else {
                            arr = intArrayOf(1, dots[j]!!.x, dots[j]!!.y, 0, 0)
                        }
                    }
                }
            }
        }
        return arr
    }

    fun down(x: Int, y: Int) {
        time = System.currentTimeMillis()
        focused_dot = get_nearest(x, y)
        if (focused_dot == -1) return
        dot_prev_color = dots[focused_dot]!!.color
        dots[focused_dot]!!.color = players[current_player]!!.color
        val c = surfaceHolder.lockCanvas(null)
        Draw(c)
        surfaceHolder.unlockCanvasAndPost(c)
        Log.d("abz", "Current frame rendering time is " + (System.currentTimeMillis() - time))
    }

    fun get_nearest(x: Int, y: Int): Int {
        var least_x_distance = Math.abs(x - dots[0]!!.x)
        var least_x = 0
        for (i in 0 until dots_per_col) {
            if (Math.abs(x - dots[i]!!.x) < least_x_distance) {
                least_x_distance = Math.abs(x - dots[i]!!.x)
                least_x = i
            }
        }
        var nearest_dot = least_x
        var least_y_distance = Math.abs(y - dots[nearest_dot]!!.y)
        var i = nearest_dot
        while (i < total_no_of_dots) {
            if (Math.abs(y - dots[i]!!.y) < least_y_distance) {
                least_y_distance = Math.abs(y - dots[i]!!.y)
                nearest_dot = i
            }
            i += dots_per_col
        }
        if (Math.abs(x - dots[nearest_dot]!!.x) > 60 || Math.abs(y - dots[nearest_dot]!!.y) > 60) nearest_dot = -1
        return nearest_dot
    }

    fun stretch(x: Int, y: Int) {
        time = System.currentTimeMillis()
        if (focused_dot == -1) return
        val c = surfaceHolder.lockCanvas(null)
        Draw(c)
        myPaint.strokeWidth = stroke_width
        myPaint.color = players[current_player]!!.color
        c.drawLine(dots[focused_dot]!!.x.toFloat(), dots[focused_dot]!!.y.toFloat(), x.toFloat(), y.toFloat(), myPaint)
        c.drawCircle(dots[focused_dot]!!.x.toFloat(), dots[focused_dot]!!.y.toFloat(), stroke_width / 2, myPaint)
        c.drawCircle(x.toFloat(), y.toFloat(), stroke_width / 2, myPaint)
        surfaceHolder.unlockCanvasAndPost(c)
        Log.d("abz", "Current frame rendering time in stretch is " + (System.currentTimeMillis() - time))
    }

    fun connect(f: Int, s: Int) {
        dots[f]!!.add_connected(s)
        dots[s]!!.add_connected(f)
    }

    fun check_connected(first: Int, second: Int): Boolean {
        if (dots[first]!!.counter == -1) return false
        for (i in 0..dots[first]!!.counter) {
            if (dots[first]!!.adjacent_dots[i] == second) return true
        }
        return false
    }

    init {
        myPaint = Paint()
        myPaint.color = Color.GRAY
        surfaceHolder = getHolder()
        dots = arrayOfNulls(total_no_of_dots)
        lines = arrayOfNulls(2 * (dots_per_col * (dots_per_col - 1)))
        squares = arrayOfNulls(total_no_of_dots)
        surfaceHolder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                val c = holder.lockCanvas(null)
                init(c)
                holder.unlockCanvasAndPost(c)
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
            override fun surfaceDestroyed(holder: SurfaceHolder) {}
        })
    }
}

internal class Line(var x1: Int, var y1: Int, var x2: Int, var y2: Int, color: Int) {
    var color = Color.GRAY

    companion object {
        var counter = -1
    }

    init {
        this.color = color
        counter++
    }
}

internal class Square(var x1: Int, var y1: Int, var x2: Int, var y2: Int, color: Int) {
    var color = Color.RED

    companion object {
        var counter = -1
    }

    init {
        this.color = color
        counter++
    }
}

internal class Dot(x: Int, y: Int, pos: Int) {
    var x: Int
    var y: Int
    protected var self_pos: Int
    var color: Int
    var counter = -1
    var adjacent_dots = IntArray(4)
    fun add_connected(d: Int) {
        if (counter == 3) return
        for (i in 0..counter) {
            if (d == adjacent_dots[i]) return
        }
        counter++
        adjacent_dots[counter] = d
    }

    fun get_connected_dots(): IntArray {
        if (counter == -1) {
            return intArrayOf(self_pos)
        }
        val coors = IntArray(counter + 1)
        System.arraycopy(adjacent_dots, 0, coors, 0, counter + 1)
        return coors
    }

    init {
        adjacent_dots[0] = -1
        self_pos = pos
        color = Color.GRAY
        this.x = x
        this.y = y
    }
}

internal class Player(var color: Int) {
    var points = 0f

    companion object {
        var total_players = 0
    }

    init {
        total_players++
    }
}