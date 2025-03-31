package ci.miage.mob.networkkt.models

import android.graphics.Color
import android.graphics.Path
import android.graphics.PathMeasure
import java.io.Serializable
import kotlin.math.*

data class Connexion(
    val debut: Noeud,
    val fin: Noeud,
    var couleur: Int = Color.YELLOW,
    var epaisseur: Float = 15f,
    var etiquette: String? = null,
    var pointDeControleX: Float = (debut.coordX + fin.coordX) / 2,
    var pointDeControleY: Float = (debut.coordY + fin.coordY) / 2,
    var estCourbe: Boolean = false
) : Serializable {

    // Calcule la position de l'étiquette sur l'arête.
    fun obtenirPositionEtiquette(): Pair<Float, Float> {
        val chemin = Path().apply {
            moveTo(debut.coordX, debut.coordY)
            if (estCourbe) {
                quadTo(pointDeControleX, pointDeControleY, fin.coordX, fin.coordY)
            } else {
                lineTo(fin.coordX, fin.coordY)
            }
        }
        val mesure = PathMeasure(chemin, false)
        val longueur = mesure.length
        val pointMilieu = FloatArray(2)
        mesure.getPosTan(longueur / 2, pointMilieu, null)

        val deltaX = fin.coordX - debut.coordX
        val deltaY = fin.coordY - debut.coordY
        val normalX = -deltaY / sqrt(deltaX * deltaX + deltaY * deltaY)
        val normalY = deltaX / sqrt(deltaX * deltaX + deltaY * deltaY)
        val decalageEtiquette = 78f
        val xEtiquette = pointMilieu[0] + normalX * decalageEtiquette
        val yEtiquette = pointMilieu[1] + normalY * decalageEtiquette
        return Pair(xEtiquette, yEtiquette)
    }

    // Ajuste la courbure en fonction de la position du toucher.
    fun ajusterCourbure(toucherX: Float, toucherY: Float) {
        val deltaX = fin.coordX - debut.coordX
        val deltaY = fin.coordY - debut.coordY
        val longueur = sqrt(deltaX * deltaX + deltaY * deltaY)

        // Désactiver les courbures si les nœuds sont trop proches.
        if (longueur < 100) {
            estCourbe = false
            return
        }

        val t = ((toucherX - debut.coordX) * deltaX + (toucherY - debut.coordY) * deltaY) / (longueur * longueur)
        val projX = debut.coordX + t * deltaX
        val projY = debut.coordY + t * deltaY

        val distanceLigne = hypot(toucherX - projX, toucherY - projY)

        if (distanceLigne < 10f) { // Seuil pour désactiver la courbure.
            estCourbe = false
            return
        }

        estCourbe = true
        pointDeControleX = toucherX
        pointDeControleY = toucherY
    }

    // Recalcule le point de contrôle lorsqu'un nœud est déplacé.
    fun mettreAJourPointDeControle() {
        if (!estCourbe) return

        val deltaX = fin.coordX - debut.coordX
        val deltaY = fin.coordY - debut.coordY
        val longueur = sqrt(deltaX * deltaX + deltaY * deltaY)

        if (longueur < 150f) {
            estCourbe = false
            return
        }

        val milieuX = (debut.coordX + fin.coordX) / 2
        val milieuY = (debut.coordY + fin.coordY) / 2

        val decalageX = pointDeControleX - milieuX
        val decalageY = pointDeControleY - milieuY

        pointDeControleX = milieuX + decalageX
        pointDeControleY = milieuY + decalageY
    }

    override fun equals(autre: Any?): Boolean {
        if (this === autre) return true
        if (autre !is Connexion) return false
        return (this.debut == autre.debut && this.fin == autre.fin) ||
                (this.debut == autre.fin && this.fin == autre.debut)
    }

    override fun hashCode(): Int {
        return debut.hashCode() + fin.hashCode()
    }

}