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