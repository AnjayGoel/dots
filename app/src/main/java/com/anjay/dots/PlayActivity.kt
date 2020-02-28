package com.anjay.dots

import android.annotation.SuppressLint
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
import com.anjay.dots.PlayActivity.Companion.info
import kotlinx.android.synthetic.main.activity_play.*

class PlayActivity : Activity() {
    private var con: Context? = null
    private var dimensions = Point()
    override fun onCreate(savedInstanceState: Bundle?) {
        con = this
        windowManager.defaultDisplay.getSize(dimensions)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play)
        game.dPC = (intent.getIntExtra("gridSize", 5))
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
        info.text = s + " " + game.dPC
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
    private var totalPlayers = 4
    private var currentPlayer = 0

    private var surfaceHolder: SurfaceHolder

    private var paint: Paint = Paint()
    var dPC = 5
    private var dotRadius = 0
    private var focusedDot = 0
    private var strokeWidth = dotRadius / 2.toFloat()

    lateinit var board: Board
    lateinit var playActivity: PlayActivity


    init {

        surfaceHolder = holder
        surfaceHolder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {

                var margin = width / (dPC + 2)
                board = Board(dPC, margin, 3 * margin / 2, (height - width) / 2 + margin)

                paint.color = Color.GRAY
                dotRadius = margin / 4
                strokeWidth = margin / 6.toFloat()



                val c = holder.lockCanvas(null)

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
    fun up(x: Int, y: Int) {
        if (focusedDot == -1) return
        val nearestD = board.getNearest(x, y)
        val c = surfaceHolder.lockCanvas()
        if (nearestD == -1 || nearestD == focusedDot || !board.areAdjacent(focusedDot, nearestD)) {
            lg("ERROR $nearestD $focusedDot ${board.areAdjacent(focusedDot, nearestD)}")
            var coors = board.getXY(focusedDot)
            board.arr[coors[0]][coors[1]].color = Color.LTGRAY
            Draw(c)
            surfaceHolder.unlockCanvasAndPost(c)
            return
        } else {
            var coors = board.getXY(focusedDot)
            board.arr[coors[0]][coors[1]].color = Color.LTGRAY
            board.connect(focusedDot, nearestD)
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
        for (i in board.sq) {
            var coors = board.getXY(i)
            val coors2 = board.getCoors(i + dPC + 1)
            c.drawRect(coors[0].toFloat(), coors[1].toFloat(), coors2[0].toFloat(), coors2[0].toFloat(), paint)
        }
        for (i in 0 until dPC) {
            for (j in 0 until dPC) {
                var coors = board.getCoors(i * dPC + j)
                paint.color = board.arr[i][j].color
                c.drawCircle(coors[0].toFloat(), coors[1].toFloat(), strokeWidth / 2.toFloat(), paint)
                paint.color = Color.LTGRAY

                if (board.arr[i][j].down != null) {
                    var coors2 = board.getCoors(i * dPC + j + dPC)
                    c.drawRect(coors[0].toFloat(), coors[1].toFloat(), coors2[0].toFloat(), coors2[0].toFloat(), paint)
                }
                if (board.arr[i][j].right != null) {
                    var coors2 = board.getCoors(i * dPC + j + 1)
                    c.drawRect(coors[0].toFloat(), coors[1].toFloat(), coors2[0].toFloat(), coors2[0].toFloat(), paint)
                }
            }
        }
    }

    fun down(x: Int, y: Int) {
        lg("Down Called")
        focusedDot = board.getNearest(x, y)
        if (focusedDot == -1) return
        lg("Focused dot is $focusedDot")
        val c = surfaceHolder.lockCanvas(null)
        board.arr[(focusedDot) / dPC][(focusedDot) % dPC].color = Color.BLACK
        Draw(c)
        surfaceHolder.unlockCanvasAndPost(c)
    }

    @SuppressLint("SetTextI18n")
    fun stretch(x: Int, y: Int) {
        info?.text = "$x $y"
        if (focusedDot == -1) return
        val c = surfaceHolder.lockCanvas(null)
        Draw(c)
        paint.strokeWidth = strokeWidth
        paint.color = Color.LTGRAY

        var coors = board.getCoors(focusedDot)

        c.drawLine(coors[0].toFloat(), coors[1].toFloat(), x.toFloat(), y.toFloat(), paint)
        c.drawCircle(x.toFloat(), y.toFloat(), strokeWidth / 2, paint)
        surfaceHolder.unlockCanvasAndPost(c)
    }

}


class Board(val dotsPerColumn: Int, val margin: Int, val marginX: Int, val marginY: Int) {
    var arr: Array<Array<Doot>>
    var sq = mutableSetOf<Int>()
    var count = 0

    init {
        arr = Array(dotsPerColumn) { Array(dotsPerColumn) { Doot(null, null, Color.LTGRAY) } }
    }

    class Doot(var down: Doot?, var right: Doot?, var color: Int)


    fun areAdjacent(x: Int, y: Int): Boolean {
        var min = getXY(minOf(x, y))
        var max = getXY(maxOf(x, y))
        if (max[0] == min[0] - 1 && max[1] == min[1]) return true
        else if (max[1] == min[1] - 1 && max[0] == max[0]) return true
        return false
    }

    fun getCoors(i: Int): Array<Int> {
        var xy = getXY(i)
        return arrayOf(xy[0] * margin + marginX, xy[1] * margin + marginY)
    }

    fun getNearest(x: Int, y: Int): Int {
        var xx = x - marginX
        var yy = y - marginY
        if (xx > dotsPerColumn * margin || xx < -margin / 2) {
            return -1
        } else if (yy > dotsPerColumn * margin || yy < -margin / 2) {
            return -1
        }
        var nx = xx / margin
        var ny = yy / margin
        lg("divisions $nx $ny")
        if (xx % margin > margin / 2) nx++
        if (yy % margin > margin / 2) ny++
        if (dotsPerColumn * ny + nx > dotsPerColumn * dotsPerColumn) return -1
        lg("Nearest Dot is " + (dotsPerColumn * ny + nx - 1))
        return dotsPerColumn * ny + nx
    }

    fun getXY(i: Int): Array<Int> {
        return arrayOf(i % dotsPerColumn, i / dotsPerColumn)
    }


    fun getFlat(a: IntArray): Int {
        return a[1] * dotsPerColumn + a[0]
    }

    fun connect(a: Int, b: Int) {
        var mx = getXY(maxOf(a, b))
        var mn = getXY(minOf(a, b))

        if (mx[0] > mn[0]) {
            arr[mn[0]][mn[1]].right = arr[mx[0]][mx[1]]
        } else {
            arr[mn[0]][mn[1]].down = arr[mx[0]][mx[1]]
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


internal class Player(var color: Int) {
    var points = 0f

    companion object {
        var totalPlayers = 0
    }

    init {
        totalPlayers++
    }
}