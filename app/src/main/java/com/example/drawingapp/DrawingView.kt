package com.example.drawingapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import java.lang.reflect.Type

class DrawingView(context:Context, attr:AttributeSet) : View(context, attr){

    private var drawpaint: Paint? = null
    private var drawpath : CustomPath? = null
    private var canvasbitmap : Bitmap? = null
    private var canvaspaint:Paint? = null
    private var canvas:Canvas? = null
    private var brushsize:Float = 0.toFloat()
    private var color = Color.BLACK
    private val paths = ArrayList<CustomPath>()
    private val undopaths = ArrayList<CustomPath>()

    init {
        setupdrawing()
    }

    fun undo(){
        if(paths.size > 0){
            undopaths.add(paths.removeLast())
            invalidate()                    // THIS WILL AUTOMATICALLY CALL "onDraw()" METHOD IN ORDER TO REDRAW ALL THE PATHS EXCEPT LAST ONE
        }
    }

    fun redo(){
        if(undopaths.size > 0){
            paths.add(undopaths.removeLast())
            invalidate()                    // THIS WILL AUTOMATICALLY CALL "onDraw()" METHOD IN ORDER TO REDRAW ALL THE PATHS EXCEPT LAST ONE
        }
    }

    private fun setupdrawing() {
        drawpaint = Paint()
        drawpath = CustomPath(color, brushsize)
        drawpaint?.color = color
        drawpaint?.style = Paint.Style.STROKE
        drawpaint?.strokeJoin = Paint.Join.ROUND
        drawpaint?.strokeCap = Paint.Cap.ROUND
        canvaspaint = Paint(Paint.DITHER_FLAG)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        canvasbitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvas = Canvas(canvasbitmap!!)
    }

    // CHANGE CANVAS TO CANVAS? IF FAILS
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        paths.forEach { it ->
            drawpaint!!.strokeWidth = it.brushthickness
            drawpaint!!.color = it.color
            canvas.drawPath(it, drawpaint!!)
        }

        canvas.drawBitmap(canvasbitmap!!, 0f, 0f, canvaspaint)
        if(!drawpath!!.isEmpty){
            drawpaint!!.strokeWidth = drawpath!!.brushthickness
            drawpaint!!.color = drawpath!!.color
            canvas.drawPath(drawpath!!, drawpaint!!)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val touchX = event?.x
        val touchY = event?.y

        when(event?.action){

            MotionEvent.ACTION_DOWN -> {
                drawpath?.color = color
                drawpath?.brushthickness = brushsize
                drawpath?.reset()
                drawpath?.moveTo(touchX!!, touchY!!)
            }
            MotionEvent.ACTION_MOVE -> {
                drawpath?.lineTo(touchX!!, touchY!!)
            }
            MotionEvent.ACTION_UP -> {
                paths.add(drawpath!!)
                drawpath = CustomPath(color, brushsize)
            }
            else ->  return false

        }
        invalidate()
        return true
    }

    fun setsizeforbrush(newsize:Float){
        brushsize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, newsize, resources.displayMetrics)
        drawpaint?.strokeWidth = brushsize
    }

    fun setcolor(newcolor:String){

        color = Color.parseColor(newcolor)
        drawpaint?.color = color

    }

    internal inner class CustomPath(var color:Int, var brushthickness:Float) : Path() {

    }

}