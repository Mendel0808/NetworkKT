package ci.miage.mob.networkkt

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import ci.miage.mob.networkkt.models.Edge
import ci.miage.mob.networkkt.models.Graph
import ci.miage.mob.networkkt.models.Node
import ci.miage.mob.networkkt.ui.theme.DrawableGraph

class GraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private lateinit var graph: Graph
    private lateinit var drawableGraph: DrawableGraph
    private var isAddObjectMode = false
    private var isAddConnectionMode = false
    private var selectedNode: Node? = null
    private var tempConnectionStart: Node? = null
    private var tempConnectionEndX: Float = 0f
    private var tempConnectionEndY: Float = 0f
    private var isDragging = false

    // Initialisation du graphe
    fun setGraph(graph: Graph) {
        this.graph = graph
        this.drawableGraph = DrawableGraph(graph, context)
        Log.d("GraphView", "Graph initialisé avec ${graph.nodes.size} nœuds et ${graph.edges.size} arêtes.")
    }

    // Activer/désactiver le mode ajout d'objet
    fun setAddObjectMode(enabled: Boolean) {
        isAddObjectMode = enabled
        isAddConnectionMode = false
        selectedNode = null
        Log.d("GraphView", "Mode Ajout d'objet : $enabled")
    }

    // Activer/désactiver le mode ajout de connexion
    fun setAddConnectionMode(enabled: Boolean) {
        isAddConnectionMode = enabled
        isAddObjectMode = false
        selectedNode = null
        tempConnectionStart = null
        Log.d("GraphView", "Mode Ajout de connexion : $enabled")
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (::drawableGraph.isInitialized) {
            drawableGraph.draw(canvas) // Redessiner le graphe (nœuds et arêtes)
        }

        // Dessiner la connexion temporaire (si en mode ajout de connexion)
        if (isAddConnectionMode && tempConnectionStart != null) {
            val paint = Paint().apply {
                color = Color.GRAY
                strokeWidth = 5f
                style = Paint.Style.STROKE
            }
            canvas.drawLine(
                tempConnectionStart!!.x,
                tempConnectionStart!!.y,
                tempConnectionEndX,
                tempConnectionEndY,
                paint
            )
            Log.d("GraphView", "Dessin de la connexion temporaire.")
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!::graph.isInitialized) return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isDragging = false

                if (isAddObjectMode) {
                    postDelayed({
                        if (isAddObjectMode && !isDragging) {
                            showLabelDialog(event.x, event.y)
                        }
                    }, 500)
                } else if (isAddConnectionMode) {
                    tempConnectionStart = graph.findNodeAt(event.x, event.y)
                    if (tempConnectionStart != null) {
                        tempConnectionEndX = event.x
                        tempConnectionEndY = event.y
                        Log.d("GraphView", "Début de la connexion temporaire depuis ${tempConnectionStart!!.label}.")
                    }
                } else {
                    selectedNode = graph.findNodeAt(event.x, event.y)
                    if (selectedNode != null) {
                        Log.d("GraphView", "Nœud sélectionné : ${selectedNode!!.label}.")
                        postDelayed({
                            if (selectedNode != null && !isDragging) {
                                showNodeContextMenu(selectedNode!!)
                            }
                        }, 500)
                    } else {
                        val edge = graph.findEdgeAt(event.x, event.y)
                        if (edge != null) {
                            postDelayed({
                                if (!isDragging) {
                                    showEdgeContextMenu(edge)
                                }
                            }, 500)
                        }
                    }
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (isAddConnectionMode && tempConnectionStart != null) {
                    tempConnectionEndX = event.x
                    tempConnectionEndY = event.y
                    invalidate() // Redessiner la connexion temporaire
                } else if (selectedNode != null) {
                    isDragging = true
                    selectedNode?.let { node ->
                        node.move(event.x, event.y) // Déplacer le nœud
                        invalidate() // Redessiner le graphe après le déplacement du nœud
                        Log.d("GraphView", "Nœud déplacé : ${node.label} à (${node.x}, ${node.y})")
                    }
                }
            }

            MotionEvent.ACTION_UP -> {
                if (isAddConnectionMode && tempConnectionStart != null) {
                    val endNode = graph.findNodeAt(event.x, event.y)
                    if (endNode != null && endNode != tempConnectionStart) {
                        if (!graph.hasEdgeBetween(tempConnectionStart!!, endNode)) {
                            showConnectionLabelDialog(tempConnectionStart!!, endNode)
                        } else {
                            Log.d("GraphView", "Connexion déjà existante entre ${tempConnectionStart!!.label} et ${endNode.label}.")
                        }
                    } else {
                        Log.d("GraphView", "Connexion annulée : nœud d'arrivée invalide.")
                    }
                    tempConnectionStart = null
                    invalidate() // Redessiner après la création de la connexion
                }
                selectedNode = null
                isDragging = false
            }
        }
        return true
    }

    // Boîte de dialogue pour ajouter un objet
    private fun showLabelDialog(x: Float, y: Float) {
        val context = this.context
        val input = android.widget.EditText(context)
        val dialog = AlertDialog.Builder(context)
            .setTitle("Ajouter un objet")
            .setMessage("Entrez l'étiquette de l'objet :")
            .setView(input)
            .setPositiveButton("Ajouter") { _, _ ->
                val label = input.text.toString()
                if (label.isNotEmpty()) {
                    val newNode = Node(x, y, label)
                    graph.addNode(newNode)
                    invalidate()
                }
            }
            .setNegativeButton("Annuler", null)
            .create()
        dialog.show()
    }

    // Boîte de dialogue pour ajouter une connexion
    private fun showConnectionLabelDialog(start: Node, end: Node) {
        val context = this.context
        val input = android.widget.EditText(context)
        val dialog = AlertDialog.Builder(context)
            .setTitle("Ajouter une connexion")
            .setMessage("Entrez l'étiquette de la connexion :")
            .setView(input)
            .setPositiveButton("Ajouter") { _, _ ->
                val label = input.text.toString()
                if (label.isNotEmpty()) {
                    val edge = Edge(start, end, label = label)
                    if (graph.addEdge(edge)) {
                        invalidate()
                    }
                }
            }
            .setNegativeButton("Annuler", null)
            .create()
        dialog.show()
    }

    // Boîte de dialogue pour les options du nœud
    private fun showNodeContextMenu(node: Node) {
        val context = this.context
        val options = arrayOf("Supprimer", "Modifier l'étiquette", "Changer la couleur")
        val dialog = AlertDialog.Builder(context)
            .setTitle("Options pour ${node.label}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> graph.removeNode(node)
                    1 -> showEditLabelDialog(node)
                    2 -> showColorPickerDialog(node)
                }
                invalidate()
            }
            .setNegativeButton("Annuler", null)
            .create()
        dialog.show()
    }

    // Boîte de dialogue pour modifier l'étiquette d'un nœud
    private fun showEditLabelDialog(node: Node) {
        val context = this.context
        val input = android.widget.EditText(context).apply {
            setText(node.label)
        }
        val dialog = AlertDialog.Builder(context)
            .setTitle("Modifier l'étiquette")
            .setMessage("Entrez la nouvelle étiquette :")
            .setView(input)
            .setPositiveButton("Valider") { _, _ ->
                val newLabel = input.text.toString()
                if (newLabel.isNotEmpty()) {
                    node.label = newLabel
                    invalidate()
                }
            }
            .setNegativeButton("Annuler", null)
            .create()
        dialog.show()
    }

    // Boîte de dialogue pour choisir une couleur
    private fun showColorPickerDialog(node: Node) {
        val context = this.context
        val colors = arrayOf("Rouge", "Vert", "Bleu", "Orange", "Cyan", "Magenta", "Noir")
        val dialog = AlertDialog.Builder(context)
            .setTitle("Choisir une couleur")
            .setItems(colors) { _, which ->
                val newColor = when (which) {
                    0 -> Color.RED
                    1 -> Color.GREEN
                    2 -> Color.BLUE
                    3 -> Color.parseColor("#FFA500")
                    4 -> Color.CYAN
                    5 -> Color.MAGENTA
                    6 -> Color.BLACK
                    else -> Color.BLACK
                }
                node.color = newColor
                invalidate()
            }
            .setNegativeButton("Annuler", null)
            .create()
        dialog.show()
    }


    // Afficher le menu contextuel pour une connexion
    private fun showEdgeContextMenu(edge: Edge) {
        val context = this.context
        val options = arrayOf("Supprimer", "Modifier l'étiquette", "Changer la couleur", "Modifier l'épaisseur")
        val dialog = AlertDialog.Builder(context)
            .setTitle("Options pour la connexion")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        graph.removeEdge(edge)
                        invalidate() // Redessiner la vue après la suppression
                        Toast.makeText(context, "Connexion supprimée", Toast.LENGTH_SHORT).show()
                    }
                    1 -> showEditEdgeLabelDialog(edge)
                    2 -> showEdgeColorPickerDialog(edge)
                    3 -> showEdgeThicknessDialog(edge)
                }
            }
            .setNegativeButton("Annuler", null)
            .create()
        dialog.show()
    }

    // Boîte de dialogue pour modifier l'étiquette de la connexion
    private fun showEditEdgeLabelDialog(edge: Edge) {
        val context = this.context
        val input = android.widget.EditText(context).apply {
            setText(edge.label)
        }
        val dialog = AlertDialog.Builder(context)
            .setTitle("Modifier l'étiquette")
            .setMessage("Entrez la nouvelle étiquette :")
            .setView(input)
            .setPositiveButton("Valider") { _, _ ->
                val newLabel = input.text.toString()
                if (newLabel.isNotEmpty()) {
                    edge.label = newLabel
                    invalidate()
                }
            }
            .setNegativeButton("Annuler", null)
            .create()
        dialog.show()
    }

    // Boîte de dialogue pour choisir une couleur pour la connexion
    private fun showEdgeColorPickerDialog(edge: Edge) {
        val context = this.context
        val colors = arrayOf("Rouge", "Vert", "Bleu", "Orange", "Cyan", "Magenta", "Noir")
        val dialog = AlertDialog.Builder(context)
            .setTitle("Choisir une couleur")
            .setItems(colors) { _, which ->
                val newColor = when (which) {
                    0 -> Color.RED
                    1 -> Color.GREEN
                    2 -> Color.BLUE
                    3 -> Color.parseColor("#FFA500")
                    4 -> Color.CYAN
                    5 -> Color.MAGENTA
                    6 -> Color.BLACK
                    else -> Color.BLACK
                }
                edge.color = newColor
                invalidate()
            }
            .setNegativeButton("Annuler", null)
            .create()
        dialog.show()
    }

    // Boîte de dialogue pour modifier l'épaisseur de la connexion
    private fun showEdgeThicknessDialog(edge: Edge) {
        val context = this.context
        val input = android.widget.EditText(context).apply {
            setText(edge.thickness.toString())
        }
        val dialog = AlertDialog.Builder(context)
            .setTitle("Modifier l'épaisseur")
            .setMessage("Entrez la nouvelle épaisseur :")
            .setView(input)
            .setPositiveButton("Valider") { _, _ ->
                val newThickness = input.text.toString().toFloatOrNull()
                if (newThickness != null) {
                    edge.thickness = newThickness
                    invalidate()
                }
            }
            .setNegativeButton("Annuler", null)
            .create()
        dialog.show()
    }
}