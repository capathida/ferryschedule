package com.example.ferryschedule.util

import android.graphics.*
import com.example.ferryschedule.domain.model.CongestionLevel
import com.example.ferryschedule.domain.model.RoadSegment
import com.example.ferryschedule.domain.model.TrafficStatus

object RoadCorridorBitmapGenerator {

    private const val WIDTH = 980
    private const val HEIGHT = 390

    fun generateCorridorBitmap(trafficStatus: TrafficStatus?): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Background gradient
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, HEIGHT.toFloat(),
                Color.parseColor("#051124"), Color.parseColor("#0A192F"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), bgPaint)

        // 2. Subtle Water Area in Middle
        val waterPaint = Paint().apply {
            color = Color.parseColor("#0D253F")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val waterPath = Path().apply {
            moveTo(140f, 0f)
            quadTo(250f, 195f, 290f, HEIGHT.toFloat())
            lineTo(510f, HEIGHT.toFloat())
            quadTo(460f, 110f, 440f, 0f)
            close()
        }
        canvas.drawPath(waterPath, waterPaint)

        // 3. Islands & Landmasses
        val landPaint = Paint().apply {
            color = Color.parseColor("#1E293B")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val landStroke = Paint().apply {
            color = Color.parseColor("#334155")
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }

        // Hönö Island (Left)
        val honoPath = Path().apply {
            moveTo(10f, 40f)
            quadTo(130f, 30f, 200f, 80f)
            quadTo(240f, 130f, 220f, 250f)
            quadTo(170f, 350f, 20f, 340f)
            close()
        }
        canvas.drawPath(honoPath, landPaint)
        canvas.drawPath(honoPath, landStroke)

        // Mainland (Right)
        val mainlandPath = Path().apply {
            moveTo(460f, 80f)
            quadTo(560f, 50f, WIDTH.toFloat(), 60f)
            lineTo(WIDTH.toFloat(), 360f)
            quadTo(560f, 360f, 460f, 280f)
            close()
        }
        canvas.drawPath(mainlandPath, landPaint)
        canvas.drawPath(mainlandPath, landStroke)

        // 4. Texts & Harbor Titles
        val textPaint = Paint().apply {
            color = Color.parseColor("#94A3B8")
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("HÖNÖ", 75f, 85f, textPaint)

        val smallTextPaint = Paint().apply {
            color = Color.parseColor("#64748B")
            textSize = 11f
            isAntiAlias = true
        }
        canvas.drawText("Öckerö kommun", 75f, 105f, smallTextPaint)

        // 5. Water Route (Dotted Ferry Line)
        val ferryRoutePaint = Paint().apply {
            color = Color.parseColor("#06B6D4")
            strokeWidth = 4f
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(12f, 12f), 0f)
            isAntiAlias = true
        }
        val ferryPath = Path().apply {
            moveTo(200f, 192f)
            quadTo(330f, 212f, 460f, 192f)
        }
        canvas.drawPath(ferryPath, ferryRoutePaint)

        val ferryLabelPaint = Paint().apply {
            color = Color.parseColor("#38BDF8")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("⚓ Hönöleden (2,5 km • ~12 min)", 330f, 160f, ferryLabelPaint)

        // Harbor Badges
        drawHarborPoint(canvas, 200f, 192f, "🏝️ Hönö Pinan")
        drawHarborPoint(canvas, 460f, 192f, "⚓ Lilla Varholmen")

        // 6. DUAL ROAD LANES ON MAINLAND (Väg 155)
        val segments = trafficStatus?.segments ?: emptyList()
        val getSegLevel: (String) -> Int = { segId ->
            val seg = segments.find { it.id == segId }
            getLevelColor(seg?.level ?: CongestionLevel.GREEN)
        }

        // ==========================================
        // UPPER LANE: MOT GÖTEBORG (ÖSTERUT ►►►)
        // ==========================================
        drawRoadSegment(canvas, 460f, 182f, 540f, 182f, getSegLevel("36958")) // Slip -> Lulles
        drawRoadSegment(canvas, 540f, 182f, 630f, 183f, getSegLevel("36957")) // Lulles -> Hjuvik
        drawRoadSegment(canvas, 630f, 183f, 730f, 178f, getSegLevel("6157"))  // Hjuvik -> Hästevik
        drawRoadSegment(canvas, 730f, 178f, 830f, 173f, getSegLevel("33621")) // Hästevik -> Amhult
        drawRoadSegment(canvas, 830f, 173f, 960f, 165f, getSegLevel("6154"))  // Amhult -> Bur

        // Median divider line
        val medianPaint = Paint().apply {
            color = Color.parseColor("#64748B")
            strokeWidth = 2f
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(8f, 8f), 0f)
            isAntiAlias = true
        }
        canvas.drawLine(460f, 210f, 960f, 198f, medianPaint)

        // ==========================================
        // LOWER LANE: MOT FÄRJAN / HÖNÖ (VÄSTERUT ◄◄◄)
        // ==========================================
        drawRoadSegment(canvas, 960f, 228f, 830f, 236f, getSegLevel("33611")) // Bur -> Amhult
        drawRoadSegment(canvas, 830f, 236f, 730f, 241f, getSegLevel("6152"))  // Amhult -> Hällsvik
        drawRoadSegment(canvas, 730f, 241f, 630f, 245f, getSegLevel("6156"))  // Hällsvik -> Hästevik
        drawRoadSegment(canvas, 630f, 245f, 540f, 244f, getSegLevel("36956")) // Hjuvik -> Lulles
        drawRoadSegment(canvas, 540f, 244f, 460f, 244f, getSegLevel("36959")) // Lulles -> Slip

        // 7. Lane Label Badges
        drawPillBadge(canvas, 680f, 152f, "► ÖSTERUT: MOT GÖTEBORG & STAN", "#10B981")
        drawPillBadge(canvas, 680f, 268f, "◄ VÄSTERUT: MOT FÄRJAN & HÖNÖ", "#38BDF8")

        // 8. Road Node Labels
        drawNodeLabel(canvas, 540f, 318f, "Lulles väg")
        drawNodeLabel(canvas, 630f, 318f, "Hjuvik")
        drawNodeLabel(canvas, 730f, 318f, "Hästevik")
        drawNodeLabel(canvas, 830f, 318f, "Amhult")
        drawNodeLabel(canvas, 930f, 318f, "Bur / Göteborg")

        // 9. "HÄR BÖRJAR DET BLI GRÖNT" Pointer
        val slipColor = getSegLevel("36959")
        val hjuvikColor = getSegLevel("36956")
        val greenMarkerX = when {
            slipColor == Color.parseColor("#10B981") -> 490f
            hjuvikColor == Color.parseColor("#10B981") -> 580f
            else -> 730f
        }
        drawGreenPointer(canvas, greenMarkerX, 292f)

        return bitmap
    }

    private fun drawRoadSegment(canvas: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, colorInt: Int) {
        val roadBorder = Paint().apply {
            color = Color.parseColor("#0F172A")
            strokeWidth = 14f
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
        }
        canvas.drawLine(x1, y1, x2, y2, roadBorder)

        val roadFill = Paint().apply {
            color = colorInt
            strokeWidth = 8f
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
        }
        canvas.drawLine(x1, y1, x2, y2, roadFill)
    }

    private fun drawHarborPoint(canvas: Canvas, cx: Float, cy: Float, title: String) {
        val pointPaint = Paint().apply {
            color = Color.parseColor("#06B6D4")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(cx, cy, 8f, pointPaint)

        val labelBg = Paint().apply {
            color = Color.parseColor("#0F172A")
            style = Paint.Style.FILL
        }
        val labelBorder = Paint().apply {
            color = Color.parseColor("#06B6D4")
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        val rect = RectF(cx - 65f, cy - 45f, cx + 65f, cy - 20f)
        canvas.drawRoundRect(rect, 6f, 6f, labelBg)
        canvas.drawRoundRect(rect, 6f, 6f, labelBorder)

        val labelText = Paint().apply {
            color = Color.parseColor("#38BDF8")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(title, cx, cy - 28f, labelText)
    }

    private fun drawPillBadge(canvas: Canvas, cx: Float, cy: Float, text: String, colorHex: String) {
        val paint = Paint().apply {
            color = Color.parseColor("#020617")
            style = Paint.Style.FILL
        }
        val border = Paint().apply {
            color = Color.parseColor(colorHex)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val rect = RectF(cx - 120f, cy - 10f, cx + 120f, cy + 10f)
        canvas.drawRoundRect(rect, 6f, 6f, paint)
        canvas.drawRoundRect(rect, 6f, 6f, border)

        val textP = Paint().apply {
            color = Color.parseColor(colorHex)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(text, cx, cy + 4f, textP)
    }

    private fun drawNodeLabel(canvas: Canvas, cx: Float, cy: Float, name: String) {
        val dot = Paint().apply {
            color = Color.parseColor("#64748B")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(cx, cy, 3f, dot)

        val textP = Paint().apply {
            color = Color.parseColor("#94A3B8")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(name, cx, cy + 16f, textP)
    }

    private fun drawGreenPointer(canvas: Canvas, cx: Float, cy: Float) {
        val bgPaint = Paint().apply {
            color = Color.parseColor("#064E3B")
            style = Paint.Style.FILL
        }
        val borderPaint = Paint().apply {
            color = Color.parseColor("#10B981")
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        val rect = RectF(cx - 75f, cy - 10f, cx + 75f, cy + 12f)
        canvas.drawRoundRect(rect, 6f, 6f, bgPaint)
        canvas.drawRoundRect(rect, 6f, 6f, borderPaint)

        // Upward arrow
        val arrow = Path().apply {
            moveTo(cx - 6f, cy - 10f)
            lineTo(cx + 6f, cy - 10f)
            lineTo(cx, cy - 18f)
            close()
        }
        canvas.drawPath(arrow, Paint().apply { color = Color.parseColor("#10B981"); style = Paint.Style.FILL })

        val textP = Paint().apply {
            color = Color.parseColor("#ECFDF5")
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("HÄR BÖRJAR DET BLI GRÖNT", cx, cy + 5f, textP)
    }

    private fun getLevelColor(level: CongestionLevel): Int {
        return when (level) {
            CongestionLevel.GREEN -> Color.parseColor("#10B981")
            CongestionLevel.YELLOW -> Color.parseColor("#F59E0B")
            CongestionLevel.RED -> Color.parseColor("#F43F5E")
            CongestionLevel.DARK_RED -> Color.parseColor("#DC2626")
        }
    }
}
