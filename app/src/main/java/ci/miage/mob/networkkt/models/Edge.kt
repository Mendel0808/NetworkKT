package ci.miage.mob.networkkt.models
//1
import android.graphics.Color
import android.graphics.Path
import android.graphics.PathMeasure
import java.io.Serializable
import kotlin.math.*

data class Edge(
    val start: Node,
    val end: Node,
    var color: Int = Color.YELLOW,
    var thickness: Float = 5f,
    var label: String? = null,
    var controlX: Float = (start.x + end.x) / 2,
    var controlY: Float = (start.y + end.y) / 2,
    var isCurved: Boolean = false
) : Serializable {

    fun getLabelPosition(): Pair<Float, Float> {
        val path = Path().apply {
            moveTo(start.x, start.y)
            if (isCurved) {
                quadTo(controlX, controlY, end.x, end.y)
            } else {
                lineTo(end.x, end.y)
            }
        }
        val measure = PathMeasure(path, false)
        val length = measure.length
        val midPoint = FloatArray(2)
        measure.getPosTan(length / 2, midPoint, null)

        val dx = end.x - start.x
        val dy = end.y - start.y
        val normalX = -dy / sqrt(dx * dx + dy * dy)
        val normalY = dx / sqrt(dx * dx + dy * dy)
        val labelOffset = 78f
        val labelX = midPoint[0] + normalX * labelOffset
        val labelY = midPoint[1] + normalY * labelOffset
        return Pair(labelX, labelY)
    }

    // Méthode pour ajuster la courbure en fonction de la position du doigt
    fun adjustCurvature(touchX: Float, touchY: Float) {
        val dx = end.x - start.x
        val dy = end.y - start.y
        val length = sqrt(dx * dx + dy * dy)

        // Éviter les courbures si les objets sont trop proches
        if (length < 100) {
            isCurved = false
            return
        }

        // Calculer la projection du point touché sur la ligne droite entre start et end
        val t = ((touchX - start.x) * dx + (touchY - start.y) * dy) / (length * length)
        val projX = start.x + t * dx
        val projY = start.y + t * dy

        // Calculer la distance perpendiculaire entre le point touché et la ligne droite
        val distanceToLine = hypot(touchX - projX, touchY - projY)

        // Si la distance est inférieure à un seuil, désactiver la courbure
        if (distanceToLine < 10f) { // Seuil de tolérance pour considérer la connexion comme "droite"
            isCurved = false
            return
        }

        // Sinon, activer la courbure et ajuster le point de contrôle
        isCurved = true
        controlX = touchX
        controlY = touchY
    }

    // Méthode pour recalculer le point de contrôle lors du déplacement des nœuds
    fun updateControlPoint() {
        if (!isCurved) return

        val dx = end.x - start.x
        val dy = end.y - start.y
        val length = sqrt(dx * dx + dy * dy)

        if (length < 150f) {
            // Désactiver la courbure si les nœuds sont trop proches
            isCurved = false
            return
        }

        // Recalculer la position du point de contrôle en respectant la courbure
        val midpointX = (start.x + end.x) / 2
        val midpointY = (start.y + end.y) / 2

        // Ajout d'un offset basé sur la position précédente du point de contrôle
        val offsetX = controlX - midpointX
        val offsetY = controlY - midpointY

        controlX = midpointX + offsetX
        controlY = midpointY + offsetY
    }



    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Edge) return false
        return (this.start == other.start && this.end == other.end) ||
                (this.start == other.end && this.end == other.start)
    }

    override fun hashCode(): Int {
        return start.hashCode() + end.hashCode()
    }

}