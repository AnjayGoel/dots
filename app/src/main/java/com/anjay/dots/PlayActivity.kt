package com.anjay.dots

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_play.*
import kotlin.math.abs

class PlayActivity : Activity() {
    private var con: Context? = null
    private var dimensions = Point()
    override fun onCreate(savedInstanceState: Bundle?) {
        con = this
        windowManager.defaultDisplay.getSize(dimensions)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play)
        game.dotsPerCol = (intent.getIntExtra("gridSize", 5))
        game.setNoOfPlayers(intent.getIntExtra("noOfPlayers", 4))
        game.playActivity = this
        game.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_MOVE -> game.stretch(event.x.toInt(), event.y.toInt())
                MotionEvent.ACTION_DOWN -> game.down(event.x.toInt(), event.y.toInt())
                MotionEvent.ACTION_UP -> game.up(event.x.toInt(), event.y.toInt())
            }
            true
        }
        val s = intent.getIntExtra("noOfPlayers", 4).toString() + ""
        info.text = s + " " + game.dotsPerCol
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return super.onKeyUp(keyCode, event)
    }

    fun complete(winner: Int) {
        var i = Intent(this, Finish::class.java)
        i.putExtra("Winner", winner)
        startActivity(i)
        finish()
    }

    companion object {
        var info: TextView? = null
    }

    override fun onDestroy() {
        Log.d("Sb", "Destroyed___________________________________")
        super.onDestroy()
    }

}

private class Game(con: Context?, attrs: AttributeSet?) : SurfaceView(con, attrs) {
    private lateinit var players: Array<Player?>
    private var totalPlayers = 0
    private var currentPlayer = 0

    private var surfaceHolder: SurfaceHolder
    private var dots: Array<Dot?>
    private var lines: Array<Line?>
    private var squares: Array<Square?>

    private var time: Long = 0
    private var paint: Paint = Paint()
    var dotsPerCol = 5
    private var totalNoOfDots = dotsPerCol * dotsPerCol
    private var dotRadius = 0
    private var focusedDot = 0
    private var dotPrevColor = Color.GRAY
    private var strokeWidth = dotRadius / 2.toFloat()

    var board: Board
    lateinit var playActivity: PlayActivity


    init {
        var margin = width / (dotsPerCol + 2)
        board = Board(dotsPerCol, margin, 3 * margin / 2, (height - width) / 2 + margin)
        paint.color = Color.GRAY
        surfaceHolder = holder
        dots = arrayOfNulls(totalNoOfDots)
        lines = arrayOfNulls(2 * (dotsPerCol * (dotsPerCol - 1)))
        squares = arrayOfNulls(totalNoOfDots)
        surfaceHolder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                val c = holder.lockCanvas(null)
                initialize()
                Draw(c)
                holder.unlockCanvasAndPost(c)
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
            override fun surfaceDestroyed(holder: SurfaceHolder) {}
        })
    }


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

    fun initialize() {
        val x = width
        val y = height
        val uY = (y - x) / 2
        val margin = width / (dotsPerCol + 2)
        dotRadius = margin / 4
        strokeWidth = margin / 6.toFloat()
        var j = uY + margin
        var col = 0
        while (col < dotsPerCol) {
            var i = 3 * margin / 2
            var row = 0
            while (row < dotsPerCol) {
                dots[col * dotsPerCol + row] = Dot(i, j, col * dotsPerCol + row)
                i += margin
                row++
            }
            j += margin
            col++
        }
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
                    playActivity.complete(getHighestScorer())


                }
            }
            connect(focusedDot, nearest_d)
            togglePlayer()
        }
        Draw(c)
        surfaceHolder.unlockCanvasAndPost(c)
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
        paint.isAntiAlias = true
        paint.color = Color.WHITE
        c.drawPaint(paint)
        paint.style = Paint.Style.FILL


        for (i in 0..Square.counter) {
            paint.color = squares[i]!!.color
            c.drawRect(squares[i]!!.x1.toFloat(), squares[i]!!.y1.toFloat(), squares[i]!!.x2.toFloat(), squares[i]!!.y2.toFloat(), paint)
        }
        for (i in 0..Line.counter) {
            paint.color = Color.LTGRAY
            paint.strokeWidth = strokeWidth
            c.drawLine(lines[i]!!.x1.toFloat(), lines[i]!!.y1.toFloat(), lines[i]!!.x2.toFloat(), lines[i]!!.y2.toFloat(), paint)
        }
        for (curr_dot in 0 until totalNoOfDots) {
            paint.color = Color.LTGRAY
            c.drawCircle(dots[curr_dot]!!.x.toFloat(), dots[curr_dot]!!.y.toFloat(), dotRadius.toFloat(), paint)
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
            if (abs(y - dots[i]!!.y) < leastYDistance) {
                leastYDistance = Math.abs(y - dots[i]!!.y)
                nearestDot = i
            }
            i += dotsPerCol
        }
        if (abs(x - dots[nearestDot]!!.x) > 60 || abs(y - dots[nearestDot]!!.y) > 60) nearestDot = -1
        return nearestDot
    }

    fun stretch(x: Int, y: Int) {
        time = System.currentTimeMillis()
        if (focusedDot == -1) return
        val c = surfaceHolder.lockCanvas(null)
        Draw(c)
        paint.strokeWidth = strokeWidth
        paint.color = Color.LTGRAY
        c.drawLine(dots[focusedDot]!!.x.toFloat(), dots[focusedDot]!!.y.toFloat(), x.toFloat(), y.toFloat(), paint)
        c.drawCircle(dots[focusedDot]!!.x.toFloat(), dots[focusedDot]!!.y.toFloat(), strokeWidth / 2, paint)
        c.drawCircle(x.toFloat(), y.toFloat(), strokeWidth / 2, paint)
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


}

internal class Line(var x1: Int, var y1: Int, var x2: Int, var y2: Int, color: Int) {
    var color = Color.LTGRAY

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


class Board(val dotsPerColumn: Int, val margin: Int, var marginX: Int, var marginY: Int) {
    var arr: Array<Array<Doot>>
    var sq = mutableSetOf<Int>()
    var count = 0

    init {
        arr = Array(dotsPerColumn) { Array(dotsPerColumn) { Doot(null, null) } }
    }

    class Doot(var down: Doot?, var right: Doot?)


    fun areAdjacent(x: Int, y: Int): Boolean {
        var min = getXY(minOf(x, y))
        var max = getXY(maxOf(x, y))
        if (max[0] == min[0] - 1 && max[1] == min[1]) return true
        else if (max[1] == min[1] - 1 && max[0] == max[0]) return true
        return false
    }

    fun getCoors(i: Int): Array<Int> {
        var xy = getXY(i)
        return arrayOf(xy[0] * margin + marginX, xy[1] + marginY)
    }

    fun getNearest(x: Int, y: Int): Int {
        var nx = x / margin
        var ny = y / margin
        if (x % margin > margin / 2) nx++
        if (y % margin > margin / 2) ny++
        return dotsPerColumn * ny + nx
    }

    fun getXY(i: Int): Array<Int> {
        return arrayOf(i / margin, i % margin)
    }

    fun getFlat(a: IntArray): Int {
        return a[0] * dotsPerColumn + a[1]
    }

    fun connect(a: Int, b: Int) {
        var mx = getXY(maxOf(a, b))
        var mn = getXY(minOf(a, b))

        if (mx[0] > mn[0]) {
            arr[mn[0]][mn[1]].down = arr[mx[0]][mx[1]]
        } else {
            arr[mn[0]][mn[1]].right = arr[mx[0]][mx[1]]
        }
    }

    fun getSquareCount(): Int {
        var count = 0
        for (i in 0 until dotsPerColumn - 1) {
            for (j in 0 until dotsPerColumn - 1) {
                if (arr[i][j].right?.down != null && arr[i][j].down?.right != null) {
                    sq.add(i * dotsPerColumn + j)
                    count++
                }
            }
        }
        return count
    }

}

internal class Dot(var x: Int, var y: Int, pos: Int) {
    var selfPos: Int
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