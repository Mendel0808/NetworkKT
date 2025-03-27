package ci.miage.mob.networkkt.models

import android.graphics.Color

class GraphUtils {

    // Modifier la couleur d'un objet connecté
    fun changeNodeColor(node: Node, newColor: Int) {
        node.color = newColor
    }

    // Modifier la couleur et l'épaisseur d'une connexion
    fun editEdge(edge: Edge, newColor: Int, newThickness: Float) {
        edge.color = newColor
        edge.thickness = newThickness
    }
}