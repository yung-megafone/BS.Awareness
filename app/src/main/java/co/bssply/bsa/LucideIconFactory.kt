package co.bssply.bsa

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

/**
 * Small Android-native stroke renderer using Lucide's visual conventions
 * (24-unit canvas, round caps/joins, 2-unit stroke). The repo credits Lucide's
 * ISC-licensed design system. Keeping icons procedural avoids an SVG runtime.
 */
object LucideIconFactory {
    enum class Glyph { LOCATE, REFRESH, LAYERS, FUEL, ROUTE, EDIT, SETTINGS, SEARCH, CAMERA, UTENSILS, CART, BED, INFO, SHIELD, BUG, FILE_DOWN, CAR, MAP_PIN }

    fun bitmap(context: Context, glyph: Glyph, px: Int = 72, color: Int = Color.WHITE): Bitmap {
        val b = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color; style = Paint.Style.STROKE; strokeWidth = px / 12f; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
        }
        fun x(v: Float) = v / 24f * px
        fun y(v: Float) = v / 24f * px
        fun line(x1:Float,y1:Float,x2:Float,y2:Float)=c.drawLine(x(x1),y(y1),x(x2),y(y2),p)
        fun circle(cx:Float,cy:Float,r:Float)=c.drawCircle(x(cx),y(cy),x(r),p)
        fun rect(l:Float,t:Float,r:Float,bb:Float,rad:Float=2f)=c.drawRoundRect(RectF(x(l),y(t),x(r),y(bb)),x(rad),x(rad),p)
        when(glyph) {
            Glyph.LOCATE -> { circle(12f,12f,3f); line(12f,2f,12f,5f); line(12f,19f,12f,22f); line(2f,12f,5f,12f); line(19f,12f,22f,12f); circle(12f,12f,7f) }
            Glyph.REFRESH -> { c.drawArc(RectF(x(4f),y(4f),x(20f),y(20f)),-55f,260f,false,p); line(18f,3f,20f,7f); line(20f,7f,16f,7f) }
            Glyph.LAYERS -> { val a=Path().apply{moveTo(x(12f),y(3f));lineTo(x(21f),y(8f));lineTo(x(12f),y(13f));lineTo(x(3f),y(8f));close()};c.drawPath(a,p); line(3f,12f,12f,17f);line(12f,17f,21f,12f);line(3f,16f,12f,21f);line(12f,21f,21f,16f) }
            Glyph.FUEL -> { rect(4f,2f,14f,22f,1f); rect(6f,5f,12f,9f,0.5f); line(14f,7f,18f,10f); line(18f,10f,18f,18f); c.drawArc(RectF(x(16f),y(16f),x(20f),y(20f)),0f,180f,false,p) }
            Glyph.ROUTE -> { circle(6f,19f,2f); circle(18f,5f,2f); c.drawPath(Path().apply{moveTo(x(8f),y(19f));cubicTo(x(16f),y(19f),x(8f),y(5f),x(16f),y(5f))},p) }
            Glyph.EDIT -> { line(4f,20f,9f,19f); line(9f,19f,20f,8f); line(16f,4f,20f,8f); line(4f,20f,5f,15f); line(5f,15f,16f,4f) }
            Glyph.SETTINGS -> { circle(12f,12f,3f); circle(12f,12f,8f); line(12f,2f,12f,4f);line(12f,20f,12f,22f);line(2f,12f,4f,12f);line(20f,12f,22f,12f) }
            Glyph.SEARCH -> { circle(10f,10f,6f); line(14.5f,14.5f,21f,21f) }
            Glyph.CAMERA -> { rect(3f,7f,21f,19f,2f); circle(12f,13f,3f); line(7f,7f,9f,4f); line(9f,4f,15f,4f); line(15f,4f,17f,7f) }
            Glyph.UTENSILS -> { line(7f,3f,7f,21f); line(4f,3f,4f,8f); line(10f,3f,10f,8f); c.drawPath(Path().apply{moveTo(x(4f),y(8f));cubicTo(x(4f),y(12f),x(10f),y(12f),x(10f),y(8f))},p); line(17f,3f,17f,21f); c.drawArc(RectF(x(14f),y(3f),x(20f),y(13f)),90f,180f,false,p) }
            Glyph.CART -> { line(3f,4f,5f,4f); line(5f,4f,8f,16f); line(8f,16f,19f,16f); line(19f,16f,21f,8f); line(7f,8f,21f,8f); circle(9f,20f,1f);circle(18f,20f,1f) }
            Glyph.BED -> { line(3f,5f,3f,21f); line(21f,11f,21f,21f); line(3f,16f,21f,16f); rect(8f,10f,21f,16f,1f); circle(6f,12f,2f) }
            Glyph.INFO -> { circle(12f,12f,9f); line(12f,11f,12f,17f); line(12f,7f,12f,7.1f) }
            Glyph.SHIELD -> { c.drawPath(Path().apply{moveTo(x(12f),y(2f));lineTo(x(20f),y(5f));lineTo(x(20f),y(11f));cubicTo(x(20f),y(16f),x(16f),y(20f),x(12f),y(22f));cubicTo(x(8f),y(20f),x(4f),y(16f),x(4f),y(11f));lineTo(x(4f),y(5f));close()},p) }
            Glyph.BUG -> { rect(7f,6f,17f,19f,4f); line(9f,3f,11f,6f);line(15f,3f,13f,6f);line(3f,10f,7f,10f);line(17f,10f,21f,10f);line(3f,15f,7f,15f);line(17f,15f,21f,15f) }
            Glyph.FILE_DOWN -> { c.drawPath(Path().apply{moveTo(x(6f),y(2f));lineTo(x(15f),y(2f));lineTo(x(20f),y(7f));lineTo(x(20f),y(22f));lineTo(x(6f),y(22f));close()},p); line(15f,2f,15f,7f);line(15f,7f,20f,7f);line(13f,11f,13f,18f);line(10f,15f,13f,18f);line(13f,18f,16f,15f) }
            Glyph.CAR -> { c.drawPath(Path().apply{moveTo(x(5f),y(17f));lineTo(x(5f),y(11f));lineTo(x(8f),y(6f));lineTo(x(16f),y(6f));lineTo(x(19f),y(11f));lineTo(x(19f),y(17f));close()},p);line(5f,12f,19f,12f);circle(8f,17f,1f);circle(16f,17f,1f) }
            Glyph.MAP_PIN -> { c.drawPath(Path().apply{moveTo(x(12f),y(22f));cubicTo(x(7f),y(16f),x(5f),y(13f),x(5f),y(9f));cubicTo(x(5f),y(5f),x(8f),y(2f),x(12f),y(2f));cubicTo(x(16f),y(2f),x(19f),y(5f),x(19f),y(9f));cubicTo(x(19f),y(13f),x(17f),y(16f),x(12f),y(22f))},p);circle(12f,9f,2f) }
        }
        return b
    }
}
