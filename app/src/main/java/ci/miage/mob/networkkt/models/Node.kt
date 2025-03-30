package ci.miage.mob.networkkt.models

import android.graphics.Color
import java.io.Serializable
import java.util.UUID

data class Node(
    var x: Float,
    var y: Float,
    var label: String,
    var color: Int = Color.BLACK,
    var labelColor: Int = Color.BLUE,
    val radius: Float = 60f,
    val id: String = UUID.randomUUID().toString() // Identifiant unique des noeuds
) : Serializable {

    fun move(newX: Float, newY: Float) {
        x = newX
        y = newY
    }

    fun isInside(px: Float, py: Float): Boolean {
        val dx = x - px
        val dy = y - py
        return (dx * dx + dy * dy) <= (radius * radius)
    }

    fun getLabelPosition(): Pair<Float, Float> {
        val labelOffset = radius + 7 // Décalage fixe entre le bord du cercle et l'étiquette
        val labelX = x + labelOffset
        val labelY = y - labelOffset
        return Pair(labelX, labelY)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Node) return false
        return this.id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}