package ci.miage.mob.networkkt
//1
import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import ci.miage.mob.networkkt.models.Edge
import ci.miage.mob.networkkt.models.Graph
import ci.miage.mob.networkkt.models.Node
import kotlin.math.hypot
import kotlin.math.sqrt

class GraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private lateinit var graph: Graph
    private var selectedNode: Node? = null
    private var selectedEdge: Edge? = null
    private var isBendingEdge = false
    private var isDragging = false
    private var isAddObjectMode = false
    private var isAddConnectionMode = false
    private var isDrawingConnection = false
    private var tempConnectionStart: Node? = null
    private var tempConnectionEndX: Float = 0f
    private var tempConnectionEndY: Float = 0f
    private val handler = Handler(Looper.getMainLooper())
    private var pendingRunnable: Runnable? = null

    fun setGraph(graph: Graph) {
        this.graph = graph
        invalidate()
    }

    fun setAddObjectMode(enabled: Boolean) {
        isAddObjectMode = enabled
        isAddConnectionMode = false
        resetSelection()
    }

    fun setAddConnectionMode(enabled: Boolean) {
        isAddConnectionMode = enabled
        isAddObjectMode = false
        resetSelection()
    }

    private fun resetSelection() {
        selectedNode = null
        selectedEdge = null
        isBendingEdge = false
        isDragging = false
        isDrawingConnection = false
        tempConnectionStart = null
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Dessiner les arêtes
        graph.edges.forEach { edge ->
            drawEdge(canvas, edge)
        }
        // Dessiner les nœuds
        graph.nodes.forEach { node ->
            drawNode(canvas, node)
        }
        // Dessiner la connexion temporaire
        if (isDrawingConnection && tempConnectionStart != null) {
            drawTemporaryConnection(canvas)
        }
    }

    private fun drawEdge(canvas: Canvas, edge: Edge) {
        val paint = Paint().apply {
            color = edge.color
            strokeWidth = edge.thickness
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        val path = Path().apply {
            moveTo(edge.start.x, edge.start.y)
            if (edge.isCurved) {
                quadTo(edge.controlX, edge.controlY, edge.end.x, edge.end.y)
            } else {
                lineTo(edge.end.x, edge.end.y)
            }
        }
        canvas.drawPath(path, paint)

        // Dessiner l'étiquette de la connexion
        edge.label?.let { label ->
            val (x, y) = edge.getLabelPosition()
            canvas.drawText(label, x, y, Paint().apply {
                color = Color.BLACK
                textSize = 40f
                isAntiAlias = true
            })
        }
    }

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

    private fun drawTemporaryConnection(canvas: Canvas) {
        canvas.drawLine(
            tempConnectionStart!!.x, tempConnectionStart!!.y,
            tempConnectionEndX, tempConnectionEndY,
            Paint().apply {
                color = Color.GRAY
                strokeWidth = 5f
                style = Paint.Style.STROKE
                isAntiAlias = true
                strokeCap = Paint.Cap.ROUND
            }
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> handleTouchDown(x, y)
            MotionEvent.ACTION_MOVE -> handleTouchMove(x, y)
            MotionEvent.ACTION_UP -> handleTouchUp(x, y)
        }
        return true
    }

    private fun handleTouchDown(x: Float, y: Float) {
        cancelPendingActions()
        isDragging = false
        isBendingEdge = false
        isDrawingConnection = false
        selectedNode = graph.findNodeAt(x, y)
        selectedEdge = if (selectedNode == null) graph.findEdgeAt(x, y) else null
        when {
            isAddObjectMode -> {
                scheduleAction(500) {
                    if (!isDragging) showLabelDialog(x, y)
                }
            }
            isAddConnectionMode && selectedNode != null -> {
                isDrawingConnection = true
                tempConnectionStart = selectedNode
                tempConnectionEndX = x
                tempConnectionEndY = y
                invalidate()
            }
            selectedNode != null -> {
                scheduleAction(500) {
                    if (!isDragging) showNodeContextMenu(selectedNode!!)
                }
            }
            selectedEdge != null -> {
                handleEdgeSelection(x, y)
            }
        }
    }

    private fun handleEdgeSelection(x: Float, y: Float) {
        val (labelX, labelY) = selectedEdge!!.getLabelPosition()
        val distance = hypot(x - labelX, y - labelY)
        if (distance <= 50f) {
            scheduleAction(500) {
                if (!isDragging) showEdgeContextMenu(selectedEdge!!)
            }
        } else {
            // Activer le mode de courbure
            isBendingEdge = true
        }
    }

    private fun handleTouchMove(x: Float, y: Float) {
        when {
            isBendingEdge && selectedEdge != null -> {
                // Ajuster la courbure directement avec le doigt
                selectedEdge!!.adjustCurvature(x, y)
                invalidate()
            }
            isDrawingConnection -> {
                tempConnectionEndX = x
                tempConnectionEndY = y
                invalidate()
            }
            selectedNode != null -> {
                isDragging = true
                cancelPendingActions()
                selectedNode!!.move(x, y)

                // Mettre à jour les points de contrôle des connexions liées au nœud déplacé
                graph.edges.forEach { edge ->
                    if (edge.start == selectedNode || edge.end == selectedNode) {
                        edge.updateControlPoint()
                    }
                }
                invalidate()
            }
        }
    }


    private fun handleTouchUp(x: Float, y: Float) {
        cancelPendingActions()
        when {
            isBendingEdge -> {
                isBendingEdge = false
                selectedEdge?.let { edge ->
                    val dx = edge.end.x - edge.start.x
                    val dy = edge.end.y - edge.start.y
                    if (sqrt(dx * dx + dy * dy) < 100) edge.isCurved = false
                }
            }
            isDrawingConnection -> {
                val endNode = graph.findNodeAt(x, y)
                if (tempConnectionStart != null && endNode != null && endNode != tempConnectionStart) {
                    if (!graph.edges.any { it.start == tempConnectionStart && it.end == endNode ||
                                it.start == endNode && it.end == tempConnectionStart }) {
                        showConnectionLabelDialog(tempConnectionStart!!, endNode)
                    } else {
                        Toast.makeText(context, context.getString(R.string.toast_connection_exists), Toast.LENGTH_SHORT).show()
                    }
                }
                isDrawingConnection = false
                tempConnectionStart = null
            }
        }
        selectedNode = null
        selectedEdge = null
        isDragging = false
        invalidate()
    }

    private fun scheduleAction(delay: Long, action: () -> Unit) {
        cancelPendingActions()
        pendingRunnable = Runnable { action() }
        handler.postDelayed(pendingRunnable!!, delay)
    }

    private fun cancelPendingActions() {
        pendingRunnable?.let {
            handler.removeCallbacks(it)
            pendingRunnable = null
        }
    }

    private fun showLabelDialog(x: Float, y: Float) {
        val input = EditText(context).apply {
            hint = context.getString(R.string.dialog_add_object_hint)
        }
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.dialog_add_object_title))
            .setView(input)
            .setPositiveButton(context.getString(R.string.button_add)) { _, _ ->
                input.text.toString().takeIf { it.isNotBlank() }?.let { label ->
                    graph.addNode(Node(x, y, label))
                    invalidate()
                }
            }
            .setNegativeButton(context.getString(R.string.button_cancel), null)
            .show()
    }

    private fun showConnectionLabelDialog(start: Node, end: Node) {
        val input = EditText(context).apply {
            hint = context.getString(R.string.dialog_add_connection_hint)
        }
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.dialog_add_connection_title))
            .setView(input)
            .setPositiveButton(context.getString(R.string.button_create)) { _, _ ->
                input.text.toString().takeIf { it.isNotBlank() }?.let { label ->
                    if (graph.addEdge(Edge(start, end, label = label))) {
                        invalidate()
                    }
                }
            }
            .setNegativeButton(context.getString(R.string.button_cancel), null)
            .show()
    }

    private fun showNodeContextMenu(node: Node) {
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.dialog_node_context_title, node.label))
            .setItems(
                arrayOf(
                    context.getString(R.string.dialog_node_delete),
                    context.getString(R.string.dialog_node_rename),
                    context.getString(R.string.dialog_node_change_color)
                )
            ) { _, which ->
                when (which) {
                    0 -> deleteNode(node)
                    1 -> showEditLabelDialog(node)
                    2 -> showNodeColorPickerDialog(node)
                }
            }
            .setNegativeButton(context.getString(R.string.button_cancel), null)
            .show()
    }

    private fun showEdgeContextMenu(edge: Edge) {
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.dialog_edge_context_title))
            .setItems(
                arrayOf(
                    context.getString(R.string.dialog_edge_delete),
                    context.getString(R.string.dialog_edge_rename),
                    context.getString(R.string.dialog_edge_change_color),
                    context.getString(R.string.dialog_edge_change_thickness)
                )
            ) { _, which ->
                when (which) {
                    0 -> deleteEdge(edge)
                    1 -> showEditEdgeLabelDialog(edge)
                    2 -> showEdgeColorPickerDialog(edge)
                    3 -> showEdgeThicknessDialog(edge)
                }
            }
            .setNegativeButton(context.getString(R.string.button_cancel), null)
            .show()
    }

    private fun deleteNode(node: Node) {
        graph.removeNode(node)
        invalidate()
        Toast.makeText(context, context.getString(R.string.toast_node_deleted), Toast.LENGTH_SHORT).show()
    }

    private fun deleteEdge(edge: Edge) {
        graph.removeEdge(edge)
        invalidate()
        Toast.makeText(context, context.getString(R.string.toast_edge_deleted), Toast.LENGTH_SHORT).show()
    }

    private fun showEditLabelDialog(node: Node) {
        val input = EditText(context).apply {
            setText(node.label)
        }
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.dialog_rename_node_title))
            .setView(input)
            .setPositiveButton(context.getString(R.string.button_validate)) { _, _ ->
                input.text.toString().takeIf { it.isNotBlank() }?.let {
                    node.label = it
                    invalidate()
                }
            }
            .setNegativeButton(context.getString(R.string.button_cancel), null)
            .show()
    }

    private fun showEditEdgeLabelDialog(edge: Edge) {
        val input = EditText(context).apply {
            setText(edge.label)
        }
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.dialog_edge_rename_title))
            .setView(input)
            .setPositiveButton(context.getString(R.string.button_validate)) { _, _ ->
                input.text.toString().takeIf { it.isNotBlank() }?.let {
                    edge.label = it
                    invalidate()
                }
            }
            .setNegativeButton(context.getString(R.string.button_cancel), null)
            .show()
    }

    private fun showNodeColorPickerDialog(node: Node) {
        val colors = arrayOf(
            context.getString(R.string.color_red) to ContextCompat.getColor(context, R.color.red),
            context.getString(R.string.color_green) to ContextCompat.getColor(context, R.color.green),
            context.getString(R.string.color_blue) to ContextCompat.getColor(context, R.color.blue),
            context.getString(R.string.color_orange) to ContextCompat.getColor(context, R.color.orange),
            context.getString(R.string.color_cyan) to ContextCompat.getColor(context, R.color.cyan),
            context.getString(R.string.color_magenta) to ContextCompat.getColor(context, R.color.magenta),
            context.getString(R.string.color_black) to ContextCompat.getColor(context, R.color.black)
        )
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.dialog_choose_color_title))
            .setItems(colors.map { it.first }.toTypedArray()) { _, which ->
                node.color = colors[which].second
                invalidate()
            }
            .setNegativeButton(context.getString(R.string.button_cancel), null)
            .show()
    }

    private fun showEdgeColorPickerDialog(edge: Edge) {
        val colors = arrayOf(
            context.getString(R.string.color_red) to ContextCompat.getColor(context, R.color.red),
            context.getString(R.string.color_green) to ContextCompat.getColor(context, R.color.green),
            context.getString(R.string.color_blue) to ContextCompat.getColor(context, R.color.blue),
            context.getString(R.string.color_orange) to ContextCompat.getColor(context, R.color.orange),
            context.getString(R.string.color_cyan) to ContextCompat.getColor(context, R.color.cyan),
            context.getString(R.string.color_magenta) to ContextCompat.getColor(context, R.color.magenta),
            context.getString(R.string.color_black) to ContextCompat.getColor(context, R.color.black)
        )
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.dialog_choose_color_title))
            .setItems(colors.map { it.first }.toTypedArray()) { _, which ->
                edge.color = colors[which].second
                invalidate()
            }
            .setNegativeButton(context.getString(R.string.button_cancel), null)
            .show()
    }

    private fun showEdgeThicknessDialog(edge: Edge) {
        val input = EditText(context).apply {
            setText(edge.thickness.toString())
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.dialog_edge_thickness_title))
            .setView(input)
            .setPositiveButton(context.getString(R.string.button_validate)) { _, _ ->
                input.text.toString().toFloatOrNull()?.let {
                    edge.thickness = it.coerceIn(1f, 20f)
                    invalidate()
                }
            }
            .setNegativeButton(context.getString(R.string.button_cancel), null)
            .show()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacksAndMessages(null)
    }
}