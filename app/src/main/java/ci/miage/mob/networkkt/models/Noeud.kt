package ci.miage.mob.networkkt.models

import android.graphics.Color
import java.io.Serializable
import java.util.UUID

data class Noeud(
    var coordX: Float,
    var coordY: Float,
    var etiquette: String,
    var couleur: Int = Color.BLACK,
    var couleurEtiquette: Int = Color.BLACK,
    val rayon: Float = 60f,
    val identifiant: String = UUID.randomUUID().toString() // Identifiant unique des noeuds
) : Serializable {

    // Déplace le nœud vers une nouvelle position.
    fun deplacer(nouvelleCoordX: Float, nouvelleCoordY: Float) {
        coordX = nouvelleCoordX
        coordY = nouvelleCoordY
    }

    // Vérifie si un point donné est à l'intérieur du nœud (dans le rayon).
    fun estDedans(px: Float, py: Float): Boolean {
        val deltaX = coordX - px
        val deltaY = coordY - py
        return (deltaX * deltaX + deltaY * deltaY) <= (rayon * rayon)
    }

    // Calcule la position de l'étiquette par rapport au bord du nœud.
    fun obtenirPositionEtiquette(): Pair<Float, Float> {
        val decalageEtiquette = rayon + 7 // Décalage entre le bord du nœud et l'étiquette.
        val xEtiquette = coordX + decalageEtiquette
        val yEtiquette = coordY - decalageEtiquette
        return Pair(xEtiquette, yEtiquette)
    }

    override fun equals(autre: Any?): Boolean {
        if (this === autre) return true
        if (autre !is Noeud) return false
        return this.identifiant == autre.identifiant
    }

    override fun hashCode(): Int {
        return identifiant.hashCode()
    }
}