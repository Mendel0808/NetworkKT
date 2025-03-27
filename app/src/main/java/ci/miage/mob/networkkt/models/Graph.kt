package ci.miage.mob.networkkt.models

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import java.io.File
import java.io.IOException
import java.io.Serializable

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

    // Supprimer une arête
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

    fun findEdgeAt(x: Float, y: Float): Edge? {
        return edges.find { edge ->
            // Calculer la position de l'étiquette (milieu de la ligne)
            val midX = (edge.start.x + edge.end.x) / 2
            val midY = (edge.start.y + edge.end.y) / 2

            // Calculer la distance entre le point de toucher et l'étiquette
            val distance = Math.sqrt(
                Math.pow((x - midX).toDouble(), 2.0) + Math.pow((y - midY).toDouble(), 2.0)
            )

            // Tolérance pour cliquer sur l'étiquette (ajustez la valeur si nécessaire)
            distance <= 50 // 50 pixels de tolérance
        }
    }

    fun hasEdgeBetween(start: Node, end: Node): Boolean {
        return edges.any { (it.start == start && it.end == end) || (it.start == end && it.end == start) }
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

    // Supprimer un fichier sauvegardé
    fun deleteSavedNetwork(context: Context, filename: String): Boolean {
        val folder = File(context.filesDir, "saved_networks")
        val file = File(folder, filename)
        return if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }

    // Mettre à jour les arêtes connectées à un nœud
    fun updateEdgesForNode(node: Node) {
        for (edge in edges) {
            if (edge.start == node || edge.end == node) {
                // Les arêtes sont déjà liées aux nœuds, donc elles se mettront à jour automatiquement
            }
        }
    }
}