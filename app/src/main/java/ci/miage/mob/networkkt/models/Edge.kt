package ci.miage.mob.networkkt.models

import android.graphics.Color
import java.io.Serializable

data class Edge(
    val start: Node,
    val end: Node,
    var color: Int = Color.YELLOW,
    var thickness: Float = 5f,
    var label: String? = null
) : Serializable {
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