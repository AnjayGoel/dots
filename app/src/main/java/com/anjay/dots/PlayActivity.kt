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
    private var con: Context? = null
    private var gameCanvas: MyCanvas? = null
    private var dimensions = Point()
    override fun onCreate(savedInstanceState: Bundle?) {
        con = this
        windowManager.defaultDisplay.getSize(dimensions)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play)
        gameCanvas = findViewById<View>(R.id.gameCanvas) as MyCanvas
        info = findViewById<View>(R.id.info) as TextView
        gameCanvas!!.setNoOfPlayers(intent.getIntExtra("no_of_players", 4))
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

private class MyCanvas(c: Context?, attrs: AttributeSet?) : SurfaceView(c, attrs) {
    var surfaceHolder: SurfaceHolder
    var time: Long = 0
    var myPaint: Paint = Paint()
    var dotsPerCol = 7
    var totalNoOfDots = dotsPerCol * dotsPerCol
    var dots: Array<Dot?>
    var lines: Array<Line?>
    var squares: Array<Square?>
    var dotRadius = 0
    var focusedDot = 0
    var dotPrevColor = Color.GRAY
    var strokeWidth = dotRadius / 2.toFloat()
    private lateinit var players: Array<Player?>
    var totalPlayers = 0
    var currentPlayer = 0
    fun togglePlayer() {
        currentPlayer = (currentPlayer + 1) % totalPlayers
    }


    override fun performClick(): Boolean {
        return true
    }

    fun setNoOfPlayers(n: Int) {
        totalPlayers = n
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
        val Margin = width / (dotsPerCol + 2)
        dotRadius = Margin / 4
        strokeWidth = Margin / 6.toFloat()
        var j = uY + Margin
        var col = 0
        while (col < dotsPerCol) {
            var i = 3 * Margin / 2
            var row = 0
            while (row < dotsPerCol) {
                dots[col * dotsPerCol + row] = Dot(i, j, col * dotsPerCol + row)
                i += Margin
                row++
            }
            j += Margin
            col++
        }
        Draw(canvas)
    }

    fun checkIfAdjacent(fixed_dot: Int, d_to_check: Int): Boolean {
        val adjacent = intArrayOf(fixed_dot - 1, fixed_dot + 1, fixed_dot + dotsPerCol, fixed_dot - dotsPerCol)
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
        if (focusedDot == -1) return
        val nearest_d = getNearest(x, y)
        val c = surfaceHolder.lockCanvas()
        if (nearest_d == -1 || nearest_d == focusedDot || !checkIfAdjacent(focusedDot, nearest_d) || checkConnected(focusedDot, nearest_d)) {
            dots[focusedDot]!!.color = dotPrevColor
            Draw(c)
            surfaceHolder.unlockCanvasAndPost(c)
            return
        } else {
            dots[focusedDot]!!.color = players[currentPlayer]!!.color
            dots[nearest_d]!!.color = dots[focusedDot]!!.color
            lines[Line.counter + 1] = Line(dots[nearest_d]!!.x, dots[nearest_d]!!.y, dots[focusedDot]!!.x, dots[focusedDot]!!.y, players[currentPlayer]!!.color)
            val arr = checkIfFormsSquare(focusedDot, nearest_d)
            if (arr[0] != 0) {
                players[currentPlayer]!!.points += arr[0]
                squares[Square.counter + 1] = Square(dots[focusedDot]!!.x, dots[focusedDot]!!.y, arr[1], arr[2], players[currentPlayer]!!.color)
                if (arr[0] == 2) squares[Square.counter + 1] = Square(dots[focusedDot]!!.x, dots[focusedDot]!!.y, arr[3], arr[4], players[currentPlayer]!!.color)
                if (Square.counter + 1 == (dotsPerCol - 1) * (dotsPerCol - 1)) {
                    info += " Grid Complete"
                    PlayActivity.info!!.text = "Player " + getHighestScorer() + " wins"
                }
            }
            connect(focusedDot, nearest_d)
            togglePlayer()
        }
        Draw(c)
        surfaceHolder.unlockCanvasAndPost(c)
        Log.d("abz", "Current frame rendering time in up is " + (System.currentTimeMillis() - time) + info)
    }

    fun getHighestScorer(): Int {
        var highestScorer = 0
        var highestScore = players[0]!!.points
        for (i in 0 until totalPlayers) {
            if (players[i]!!.points > highestScore) {
                highestScore = players[i]!!.points
                highestScorer = i
            }
        }
        return highestScorer
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
            myPaint.strokeWidth = strokeWidth
            c.drawLine(lines[i]!!.x1.toFloat(), lines[i]!!.y1.toFloat(), lines[i]!!.x2.toFloat(), lines[i]!!.y2.toFloat(), myPaint)
        }
        for (curr_dot in 0 until totalNoOfDots) {
            myPaint.color = dots[curr_dot]!!.color
            c.drawCircle(dots[curr_dot]!!.x.toFloat(), dots[curr_dot]!!.y.toFloat(), dotRadius.toFloat(), myPaint)
        }
    }

    fun checkIfFormsSquare(first: Int, second: Int): IntArray {
        var arr = IntArray(5)
        arr[0] = 0
        for (i in dots[first]!!.getConnectedDots()) {
            for (j in dots[i]!!.getConnectedDots()) {
                for (k in dots[j]!!.getConnectedDots()) {
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
        focusedDot = getNearest(x, y)
        if (focusedDot == -1) return
        dotPrevColor = dots[focusedDot]!!.color
        dots[focusedDot]!!.color = players[currentPlayer]!!.color
        val c = surfaceHolder.lockCanvas(null)
        Draw(c)
        surfaceHolder.unlockCanvasAndPost(c)
        Log.d("abz", "Current frame rendering time is " + (System.currentTimeMillis() - time))
    }

    fun getNearest(x: Int, y: Int): Int {
        var leastXDistance = Math.abs(x - dots[0]!!.x)
        var leastX = 0
        for (i in 0 until dotsPerCol) {
            if (Math.abs(x - dots[i]!!.x) < leastXDistance) {
                leastXDistance = Math.abs(x - dots[i]!!.x)
                leastX = i
            }
        }
        var nearestDot = leastX
        var leastYDistance = Math.abs(y - dots[nearestDot]!!.y)
        var i = nearestDot
        while (i < totalNoOfDots) {
            if (Math.abs(y - dots[i]!!.y) < leastYDistance) {
                leastYDistance = Math.abs(y - dots[i]!!.y)
                nearestDot = i
            }
            i += dotsPerCol
        }
        if (Math.abs(x - dots[nearestDot]!!.x) > 60 || Math.abs(y - dots[nearestDot]!!.y) > 60) nearestDot = -1
        return nearestDot
    }

    fun stretch(x: Int, y: Int) {
        time = System.currentTimeMillis()
        if (focusedDot == -1) return
        val c = surfaceHolder.lockCanvas(null)
        Draw(c)
        myPaint.strokeWidth = strokeWidth
        myPaint.color = players[currentPlayer]!!.color
        c.drawLine(dots[focusedDot]!!.x.toFloat(), dots[focusedDot]!!.y.toFloat(), x.toFloat(), y.toFloat(), myPaint)
        c.drawCircle(dots[focusedDot]!!.x.toFloat(), dots[focusedDot]!!.y.toFloat(), strokeWidth / 2, myPaint)
        c.drawCircle(x.toFloat(), y.toFloat(), strokeWidth / 2, myPaint)
        surfaceHolder.unlockCanvasAndPost(c)
        Log.d("abz", "Current frame rendering time in stretch is " + (System.currentTimeMillis() - time))
    }

    fun connect(f: Int, s: Int) {
        dots[f]!!.addConnected(s)
        dots[s]!!.addConnected(f)
    }

    fun checkConnected(first: Int, second: Int): Boolean {
        if (dots[first]!!.counter == -1) return false
        for (i in 0..dots[first]!!.counter) {
            if (dots[first]!!.adjacentDots[i] == second) return true
        }
        return false
    }

    init {
        myPaint.color = Color.GRAY
        surfaceHolder = holder
        dots = arrayOfNulls(totalNoOfDots)
        lines = arrayOfNulls(2 * (dotsPerCol * (dotsPerCol - 1)))
        squares = arrayOfNulls(totalNoOfDots)
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
    protected var selfPos: Int
    var color: Int
    var counter = -1
    var adjacentDots = IntArray(4)
    fun addConnected(d: Int) {
        if (counter == 3) return
        for (i in 0..counter) {
            if (d == adjacentDots[i]) return
        }
        counter++
        adjacentDots[counter] = d
    }

    fun getConnectedDots(): IntArray {
        if (counter == -1) {
            return intArrayOf(selfPos)
        }
        val coors = IntArray(counter + 1)
        System.arraycopy(adjacentDots, 0, coors, 0, counter + 1)
        return coors
    }

    init {
        adjacentDots[0] = -1
        selfPos = pos
        color = Color.GRAY
        this.x = x
        this.y = y
    }
}

internal class Player(var color: Int) {
    var points = 0f

    companion object {
        var totalPlayers = 0
    }

    init {
        totalPlayers++
    }
}