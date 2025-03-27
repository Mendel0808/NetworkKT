package ci.miage.mob.networkkt.ui.theme

import android.content.Context
import android.graphics.*
import android.util.Log
import ci.miage.mob.networkkt.models.Edge
import ci.miage.mob.networkkt.models.Graph
import ci.miage.mob.networkkt.models.Node

class DrawableGraph(private val graph: Graph, private val context: Context) {
    fun draw(canvas: Canvas) {
        // Dessiner les arêtes
        for (edge in graph.edges) {
            val paint = Paint().apply {
                color = edge.color
                strokeWidth = edge.thickness
                style = Paint.Style.STROKE
            }
            canvas.drawLine(edge.start.x, edge.start.y, edge.end.x, edge.end.y, paint)

            // Dessiner l'étiquette de l'arête
            val midX = (edge.start.x + edge.end.x) / 2
            val midY = (edge.start.y + edge.end.y) / 2
            val textPaint = Paint().apply {
                color = Color.BLACK
                textSize = 40f
            }
            edge.label?.let {
                canvas.drawText(it, midX + 20, midY - 20, textPaint)
            }
        }

        // Dessiner les nœuds
        for (node in graph.nodes) {
            // Dessiner le cercle du nœud
            val paint = Paint().apply {
                color = node.color
                style = Paint.Style.FILL
            }
            canvas.drawCircle(node.x, node.y, node.radius, paint)

            // Dessiner l'étiquette du nœud
            val textPaint = Paint().apply {
                color = node.labelColor
                textSize = 40f
            }
            val (labelX, labelY) = node.getLabelPosition()
            canvas.drawText(node.label, labelX, labelY, textPaint)
        }
    }
}