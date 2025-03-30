package ci.miage.mob.networkkt.models
//1
import android.content.Context
import android.graphics.Path
import android.graphics.PathMeasure
import android.util.Log
import com.google.gson.Gson
import java.io.File
import java.io.IOException
import java.io.Serializable
import kotlin.math.*


class Graph : Serializable {
    val nodes = mutableSetOf<Node>()
    val edges = mutableSetOf<Edge>()

    fun addNode(node: Node) {
        if (nodes.none { it.id == node.id }) {
            nodes.add(node)
            Log.d("Graph", "Nœud ajouté : ${node.label}.")
        }
    }

    fun removeNode(node: Node) {
        if (nodes.contains(node)) {
            nodes.remove(node)
            edges.removeAll { it.start == node || it.end == node }
            Log.d("Graph", "Nœud supprimé : ${node.label}.")
        }
    }

    fun addEdge(edge: Edge): Boolean {
        Log.d("Graph", "Nœuds disponibles dans le graphe : ${nodes.joinToString { it.label }}")

        if (edge.start == edge.end) {
            Log.d("Graph", "Arête non ajoutée : les nœuds de départ et d'arrivée sont identiques.")
            return false
        }
        if (!nodes.contains(edge.start) || !nodes.contains(edge.end)) {
            Log.d("Graph", "Arête non ajoutée : un des nœuds n'existe pas dans le graphe.")
            Log.d("Graph", "Nœud de départ : ${edge.start.label}, Nœud d'arrivée : ${edge.end.label}")
            return false
        }
        if (edges.any { (it.start == edge.start && it.end == edge.end) || (it.start == edge.end && it.end == edge.start) }) {
            Log.d("Graph", "Arête non ajoutée : une arête existe déjà entre ces nœuds.")
            return false
        }
        edges.add(edge)
        Log.d("Graph", "Arête ajoutée entre ${edge.start.label} et ${edge.end.label}.")
        return true
    }

    fun removeEdge(edge: Edge) {
        val removed = edges.remove(edge)
        if (removed) {
            Log.d("Graph", "Arête supprimée entre ${edge.start.label} et ${edge.end.label}.")
        } else {
            Log.d("Graph", "Échec de la suppression de l'arête entre ${edge.start.label} et ${edge.end.label}.")
        }
    }

    // Trouver un nœud à une position donnée
    fun findNodeAt(x: Float, y: Float): Node? {
        return nodes.find { it.isInside(x, y) }
    }

    // Sauvegarder le graphe dans un fichier
    fun saveToFile(context: Context, filename: String): Boolean {
        return try {
            val folder = File(context.filesDir, "saved_networks")
            if (!folder.exists()) {
                folder.mkdir()
            }
            val file = File(folder, filename)
            file.writeText(Gson().toJson(this))
            Log.d("Graph", "Graphe sauvegardé dans ${file.absolutePath}.")
            true
        } catch (e: IOException) {
            Log.e("Graph", "Erreur lors de la sauvegarde du graphe", e)
            false
        }
    }

    // Charger un graphe depuis un fichier
    fun loadFromFile(context: Context, filename: String): Boolean {
        return try {
            val folder = File(context.filesDir, "saved_networks")
            val file = File(folder, filename)
            if (file.exists()) {
                val json = file.readText()
                val loadedGraph = Gson().fromJson(json, Graph::class.java)

                // Réinitialiser les nœuds et arêtes actuels
                nodes.clear()
                edges.clear()

                // Ajouter les nœuds chargés
                nodes.addAll(loadedGraph.nodes)

                // Reconstruire les arêtes avec les bonnes références aux nœuds
                for (edge in loadedGraph.edges) {
                    val startNode = nodes.find { it.id == edge.start.id }
                    val endNode = nodes.find { it.id == edge.end.id }
                    if (startNode != null && endNode != null) {
                        val newEdge = Edge(
                            start = startNode,
                            end = endNode,
                            color = edge.color,
                            thickness = edge.thickness,
                            label = edge.label
                        )
                        edges.add(newEdge)
                    }
                }

                Log.d("Graph", "Graphe chargé depuis ${file.absolutePath}.")
                true
            } else {
                Log.d("Graph", "Aucun fichier de graphe trouvé.")
                false
            }
        } catch (e: IOException) {
            Log.e("Graph", "Erreur lors du chargement du graphe", e)
            false
        }
    }

    // Lister les fichiers sauvegardés
    fun listSavedNetworks(context: Context): List<String> {
        val folder = File(context.filesDir, "saved_networks")
        return if (folder.exists() && folder.isDirectory) {
            folder.listFiles()?.map { it.name } ?: emptyList()
        } else {
            emptyList()
        }
    }


    fun findEdgeAt(x: Float, y: Float): Edge? {
        return edges.find { edge ->
            val (labelX, labelY) = edge.getLabelPosition()
            val isNearLabel = edge.label != null &&
                    sqrt((x - labelX).pow(2) + (y - labelY).pow(2)) <= 50f

            isNearLabel || isPointNearEdge(x, y, edge)
        }
    }

    private fun isPointNearEdge(px: Float, py: Float, edge: Edge): Boolean {
        return if (edge.isCurved) isPointNearCurve(px, py, edge)
        else isPointNearLine(px, py, edge.start.x, edge.start.y, edge.end.x, edge.end.y, 20f)
    }

    private fun isPointNearCurve(px: Float, py: Float, edge: Edge): Boolean {
        val path = Path().apply {
            moveTo(edge.start.x, edge.start.y)
            quadTo(edge.controlX, edge.controlY, edge.end.x, edge.end.y)
        }
        val measure = PathMeasure(path, false)
        val point = FloatArray(2)
        for (i in 0..10) {
            measure.getPosTan(i * measure.length / 10, point, null)
            if (sqrt((px - point[0]).pow(2) + (py - point[1]).pow(2)) <= 20f) return true
        }
        return false
    }


    private fun isPointNearLine(px: Float, py: Float, x1: Float, y1: Float,
                                x2: Float, y2: Float, tolerance: Float): Boolean {
        val lineLength = sqrt((x2 - x1).pow(2) + (y2 - y1).pow(2))
        val distance = abs((y2 - y1) * px - (x2 - x1) * py + x2 * y1 - y2 * x1) / lineLength
        return distance <= tolerance
    }
}