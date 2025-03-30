package ci.miage.mob.networkkt.ui.theme

import android.content.Context
import android.graphics.*
import ci.miage.mob.networkkt.models.Edge
import ci.miage.mob.networkkt.models.Graph
import ci.miage.mob.networkkt.models.Node

class DrawableGraph(private val graph: Graph, private val context: Context) {
    fun draw(canvas: Canvas) {
        for (edge in graph.edges) {
            drawEdge(canvas, edge)
        }
        for (node in graph.nodes) {
            drawNode(canvas, node)
        }
    }

    //methode pour dessiner une connexion
    private fun drawEdge(canvas: Canvas, edge: Edge) {
        val paint = Paint().apply {
            color = edge.color
            strokeWidth = edge.thickness
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        val path = Path().apply {
            moveTo(edge.start.x, edge.start.y)
            if (edge.isCurved) quadTo(edge.controlX, edge.controlY, edge.end.x, edge.end.y)
            else lineTo(edge.end.x, edge.end.y)
        }
        canvas.drawPath(path, paint)
        edge.label?.let { label ->
            val (x, y) = edge.getLabelPosition()
            canvas.drawText(label, x, y, Paint().apply {
                color = Color.BLACK
                textSize = 40f
                isAntiAlias = true
            })
        }
    }

    //methode pour dessiner un noeud
    private fun drawNode(canvas: Canvas, node: Node) {
        canvas.drawCircle(node.x, node.y, node.radius, Paint().apply {
            color = node.color
            style = Paint.Style.FILL
            isAntiAlias = true
        })
        val (labelX, labelY) = node.getLabelPosition()
        canvas.drawText(node.label, labelX, labelY, Paint().apply {
            color = node.labelColor
            textSize = 40f
            isAntiAlias = true
        })
    }
}