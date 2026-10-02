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
            moveTo(215f, 0f)
            quadTo(280f, 195f, 310f, HEIGHT.toFloat())
            lineTo(490f, HEIGHT.toFloat())
            quadTo(450f, 110f, 435f, 0f)
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
            moveTo(5f, 40f)
            quadTo(140f, 25f, 215f, 75f)
            quadTo(245f, 130f, 230f, 255f)
            quadTo(175f, 365f, 5f, 355f)
            close()
        }
        canvas.drawPath(honoPath, landPaint)
        canvas.drawPath(honoPath, landStroke)

        // Mainland (Right)
        val mainlandPath = Path().apply {
            moveTo(450f, 75f)
            quadTo(560f, 45f, WIDTH.toFloat(), 55f)
            lineTo(WIDTH.toFloat(), 365f)
            quadTo(560f, 365f, 450f, 280f)
            close()
        }
        canvas.drawPath(mainlandPath, landPaint)
        canvas.drawPath(mainlandPath, landStroke)

        // 4. Texts & Region Titles
        val textPaint = Paint().apply {
            color = Color.parseColor("#94A3B8")
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("HÖNÖ & ÖCKERÖ", 40f, 75f, textPaint)

        val smallTextPaint = Paint().apply {
            color = Color.parseColor("#64748B")
            textSize = 11f
            isAntiAlias = true
        }
        canvas.drawText("Väg 574 mot Pinan", 40f, 95f, smallTextPaint)

        // 5. Water Route (Dotted Ferry Line)
        val ferryRoutePaint = Paint().apply {
            color = Color.parseColor("#06B6D4")
            strokeWidth = 4f
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(12f, 12f), 0f)
            isAntiAlias = true
        }
        val ferryPath = Path().apply {
            moveTo(205f, 192f)
            quadTo(330f, 212f, 455f, 192f)
        }
        canvas.drawPath(ferryPath, ferryRoutePaint)

        val ferryLabelPaint = Paint().apply {
            color = Color.parseColor("#38BDF8")
            textSize = 11.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("⚓ Hönöleden (~12 min)", 330f, 160f, ferryLabelPaint)

        // Harbor Badges
        drawHarborPoint(canvas, 205f, 192f, "🏝️ Hönö Pinan")
        drawHarborPoint(canvas, 455f, 192f, "⚓ Lilla Varholmen")

        val segments = trafficStatus?.segments ?: emptyList()
        val getSegLevel: (String) -> Int = { segId ->
            val seg = segments.find { it.id == segId }
            getLevelColor(seg?.level ?: CongestionLevel.GREEN)
        }

        // ========================================================
        // 6. DUAL ROAD LANES ON HÖNÖ ISLAND (VÄG 574)
        // ========================================================

        // HÖNÖ UPPER LANE: UT PÅ HÖNÖ / ÖCKERÖ (VÄSTERUT ◄◄◄)
        val honoUtColor = getSegLevel("hono_ut")
        drawRoadSegment(canvas, 205f, 178f, 20f, 178f, honoUtColor)
        drawFlowArrow(canvas, 150f, 178f, true)
        drawFlowArrow(canvas, 80f, 178f, true)
        drawPillBadge(canvas, 105f, 150f, "◄ UT PÅ HÖNÖ & ÖCKERÖ", "#38BDF8")

        // HÖNÖ MEDIAN STRIP
        val medianPaint = Paint().apply {
            color = Color.parseColor("#64748B")
            strokeWidth = 2f
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(6f, 6f), 0f)
            isAntiAlias = true
        }
        canvas.drawLine(20f, 204f, 205f, 204f, medianPaint)

        // HÖNÖ LOWER LANE: MOT PINAN FÄRJA (ÖSTERUT ►►►)
        // Segment 1: Klåva / Öckerö -> Pinan korsväg
        val honoApproachesColor = getSegLevel("hono_approaches")
        drawRoadSegment(canvas, 20f, 230f, 110f, 230f, honoApproachesColor)

        // Segment 2: Pinan korsväg -> Uppställningsfiler vid rampen
        val honoPinanColor = getSegLevel("hono_pinan")
        drawRoadSegment(canvas, 110f, 230f, 205f, 230f, honoPinanColor)

        drawFlowArrow(canvas, 65f, 230f, false)
        drawFlowArrow(canvas, 160f, 230f, false)

        val isHonoCongested = honoPinanColor != Color.parseColor("#10B981")
        val lowerBadgeText = if (isHonoCongested) "► MOT PINAN (KÖBILDNING)" else "► MOT PINAN FÄRJA"
        val lowerBadgeColor = if (isHonoCongested) "#F59E0B" else "#10B981"
        drawPillBadge(canvas, 105f, 268f, lowerBadgeText, lowerBadgeColor)

        // Hönö Node Labels
        drawNodeLabel(canvas, 40f, 318f, "Klåva/Öckerö")
        drawNodeLabel(canvas, 110f, 318f, "Pinankorset")
        drawNodeLabel(canvas, 175f, 318f, "Uppställning")

        // Hönö Queue or Flow Indicator below node labels
        if (isHonoCongested) {
            drawWarningPointer(canvas, 110f, 350f, "MORGONKÖ I FILERNA")
        } else {
            drawGreenPointer(canvas, 110f, 350f, "FRI VÄG MOT PINAN")
        }

        // ========================================================
        // 7. DUAL ROAD LANES ON MAINLAND (VÄG 155)
        // ========================================================

        // UPPER LANE: MOT GÖTEBORG & STAN (ÖSTERUT ►►►)
        drawRoadSegment(canvas, 455f, 180f, 540f, 180f, getSegLevel("36958")) // Slip -> Lulles
        drawRoadSegment(canvas, 540f, 180f, 630f, 181f, getSegLevel("36957")) // Lulles -> Hjuvik
        drawRoadSegment(canvas, 630f, 181f, 730f, 176f, getSegLevel("6157"))  // Hjuvik -> Hästevik
        drawRoadSegment(canvas, 730f, 176f, 830f, 171f, getSegLevel("33621")) // Hästevik -> Amhult
        drawRoadSegment(canvas, 830f, 171f, 960f, 163f, getSegLevel("6154"))  // Amhult -> Bur

        drawFlowArrow(canvas, 500f, 180f, false)
        drawFlowArrow(canvas, 585f, 180f, false)
        drawFlowArrow(canvas, 680f, 178f, false)
        drawFlowArrow(canvas, 780f, 173f, false)
        drawFlowArrow(canvas, 895f, 167f, false)

        // Mainland Median divider line
        canvas.drawLine(455f, 206f, 960f, 194f, medianPaint)

        // LOWER LANE: MOT FÄRJAN / HÖNÖ (VÄSTERUT ◄◄◄)
        drawRoadSegment(canvas, 960f, 224f, 830f, 232f, getSegLevel("33611")) // Bur -> Amhult
        drawRoadSegment(canvas, 830f, 232f, 730f, 237f, getSegLevel("6152"))  // Amhult -> Hällsvik
        drawRoadSegment(canvas, 730f, 237f, 630f, 242f, getSegLevel("6156"))  // Hällsvik -> Hästevik
        drawRoadSegment(canvas, 630f, 242f, 540f, 241f, getSegLevel("36956")) // Hjuvik -> Lulles
        drawRoadSegment(canvas, 540f, 241f, 455f, 241f, getSegLevel("36959")) // Lulles -> Slip

        drawFlowArrow(canvas, 895f, 228f, true)
        drawFlowArrow(canvas, 780f, 235f, true)
        drawFlowArrow(canvas, 680f, 240f, true)
        drawFlowArrow(canvas, 585f, 241f, true)
        drawFlowArrow(canvas, 500f, 241f, true)

        // Mainland Lane Badges
        drawPillBadge(canvas, 680f, 150f, "► ÖSTERUT: MOT GÖTEBORG & STAN", "#10B981")
        drawPillBadge(canvas, 680f, 268f, "◄ VÄSTERUT: MOT FÄRJAN & HÖNÖ", "#38BDF8")

        // Mainland Node Labels
        drawNodeLabel(canvas, 540f, 318f, "Lulles väg")
        drawNodeLabel(canvas, 630f, 318f, "Hjuvik")
        drawNodeLabel(canvas, 730f, 318f, "Hästevik")
        drawNodeLabel(canvas, 830f, 318f, "Amhult")
        drawNodeLabel(canvas, 930f, 318f, "Bur / Göteborg")

        // Mainland "HÄR BÖRJAR DET BLI GRÖNT" Pointer
        val slipColor = getSegLevel("36959")
        val hjuvikColor = getSegLevel("36956")
        val greenMarkerX = when {
            slipColor == Color.parseColor("#10B981") -> 485f
            hjuvikColor == Color.parseColor("#10B981") -> 580f
            else -> 730f
        }
        drawGreenPointer(canvas, greenMarkerX, 350f, "HÄR BÖRJAR DET BLI GRÖNT")

        return bitmap
    }

    private fun drawRoadSegment(canvas: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, colorInt: Int) {
        val roadBorder = Paint().apply {
            color = Color.parseColor("#0F172A")
            strokeWidth = 13f
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
        }
        canvas.drawLine(x1, y1, x2, y2, roadBorder)

        val roadFill = Paint().apply {
            color = colorInt
            strokeWidth = 7.5f
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
        }
        canvas.drawLine(x1, y1, x2, y2, roadFill)
    }

    private fun drawFlowArrow(canvas: Canvas, cx: Float, cy: Float, pointsWest: Boolean) {
        val arrowPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val symbol = if (pointsWest) "◄" else "►"
        canvas.drawText(symbol, cx, cy + 3f, arrowPaint)
    }

    private fun drawHarborPoint(canvas: Canvas, cx: Float, cy: Float, title: String) {
        val pointPaint = Paint().apply {
            color = Color.parseColor("#06B6D4")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(cx, cy, 7.5f, pointPaint)

        val labelBg = Paint().apply {
            color = Color.parseColor("#0F172A")
            style = Paint.Style.FILL
        }
        val labelBorder = Paint().apply {
            color = Color.parseColor("#06B6D4")
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        val rect = RectF(cx - 62f, cy - 43f, cx + 62f, cy - 20f)
        canvas.drawRoundRect(rect, 6f, 6f, labelBg)
        canvas.drawRoundRect(rect, 6f, 6f, labelBorder)

        val labelText = Paint().apply {
            color = Color.parseColor("#38BDF8")
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(title, cx, cy - 27f, labelText)
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
        val rect = RectF(cx - 105f, cy - 10f, cx + 105f, cy + 10f)
        canvas.drawRoundRect(rect, 5f, 5f, paint)
        canvas.drawRoundRect(rect, 5f, 5f, border)

        val textP = Paint().apply {
            color = Color.parseColor(colorHex)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(text, cx, cy + 3.5f, textP)
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
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(name, cx, cy + 15f, textP)
    }

    private fun drawGreenPointer(canvas: Canvas, cx: Float, cy: Float, text: String = "HÄR BÖRJAR DET BLI GRÖNT") {
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
        canvas.drawPath(arrow, Paint().apply { color = Color.parseColor("#10B981"); style = Paint.Style.FILL; isAntiAlias = true })

        val textP = Paint().apply {
            color = Color.parseColor("#ECFDF5")
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(text, cx, cy + 5f, textP)
    }

    private fun drawWarningPointer(canvas: Canvas, cx: Float, cy: Float, text: String) {
        val bgPaint = Paint().apply {
            color = Color.parseColor("#450A0A")
            style = Paint.Style.FILL
        }
        val borderPaint = Paint().apply {
            color = Color.parseColor("#EF4444")
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        val rect = RectF(cx - 85f, cy - 10f, cx + 85f, cy + 12f)
        canvas.drawRoundRect(rect, 6f, 6f, bgPaint)
        canvas.drawRoundRect(rect, 6f, 6f, borderPaint)

        val arrow = Path().apply {
            moveTo(cx - 6f, cy - 10f)
            lineTo(cx + 6f, cy - 10f)
            lineTo(cx, cy - 18f)
            close()
        }
        canvas.drawPath(arrow, Paint().apply { color = Color.parseColor("#EF4444"); style = Paint.Style.FILL; isAntiAlias = true })

        val textP = Paint().apply {
            color = Color.parseColor("#FEE2E2")
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(text, cx, cy + 5f, textP)
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
