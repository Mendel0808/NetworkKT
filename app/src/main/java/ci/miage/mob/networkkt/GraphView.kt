package ci.miage.mob.networkkt

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import ci.miage.mob.networkkt.models.Connexion
import ci.miage.mob.networkkt.models.Graph
import ci.miage.mob.networkkt.models.Noeud
import kotlin.math.hypot
import kotlin.math.sqrt

class GraphView @JvmOverloads constructor(
    contexte: Context,
    attributs: AttributeSet? = null,
    styleDefaut: Int = 0
) : View(contexte, attributs, styleDefaut) {

    private lateinit var graphe: Graph
    private var noeudSelectionne: Noeud? = null
    private var connexionSelectionnee: Connexion? = null
    private var estEnCourbureConnexion = false
    private var estEnDeplacement = false
    private var estModeAjoutObjet = false
    private var estModeAjoutConnexion = false
    private var estEnDessinConnexion = false
    private var debutConnexionTemporaire: Noeud? = null
    private var finConnexionTemporaireX: Float = 0f
    private var finConnexionTemporaireY: Float = 0f
    private val gestionnaire = Handler(Looper.getMainLooper())
    private var actionEnAttente: Runnable? = null
    private var planAppartement: Bitmap? = null

    fun definirGraphe(graphe: Graph) {
        this.graphe = graphe
        invalidate()
    }

    fun PlanAppartement(bitmap: Bitmap) {
        this.planAppartement = bitmap
        invalidate() // Redessiner la vue
    }

    fun activerModeAjoutObjet(active: Boolean) {
        estModeAjoutObjet = active
        estModeAjoutConnexion = false
        reinitialiserSelection()
    }

    fun activerModeAjoutConnexion(active: Boolean) {
        estModeAjoutConnexion = active
        estModeAjoutObjet = false
        reinitialiserSelection()
    }

    private fun reinitialiserSelection() {
        noeudSelectionne = null
        connexionSelectionnee = null
        estEnCourbureConnexion = false
        estEnDeplacement = false
        estEnDessinConnexion = false
        debutConnexionTemporaire = null
    }

    override fun onTouchEvent(evenement: MotionEvent): Boolean {
        val x = evenement.x
        val y = evenement.y
        when (evenement.action) {
            MotionEvent.ACTION_DOWN -> gererToucherDebut(x, y)
            MotionEvent.ACTION_MOVE -> gererToucherDeplacement(x, y)
            MotionEvent.ACTION_UP -> gererToucherFin(x, y)
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        planAppartement?.let { bitmap ->
            // Dimensions actuelles du GraphView
            val viewWidth = width.toFloat()
            val viewHeight = height.toFloat()

            // Dimensions de l'image
            val imageWidth = bitmap.width.toFloat()
            val imageHeight = bitmap.height.toFloat()

            val imageAspectRatio = imageWidth / imageHeight
            val viewAspectRatio = viewWidth / viewHeight

            // Calculer les dimensions redimensionnées
            val (scaledWidth, scaledHeight) = if (viewAspectRatio > imageAspectRatio) {
                Pair(viewWidth, viewWidth / imageAspectRatio)
            } else {
                Pair(viewHeight * imageAspectRatio, viewHeight)
            }

            // Centrer l'image
            val left = (viewWidth - scaledWidth) / 2
            val top = (viewHeight - scaledHeight) / 2

            canvas.save()
            canvas.scale(scaledWidth / imageWidth, scaledHeight / imageHeight)
            canvas.translate(left, top)
            canvas.drawBitmap(bitmap, 50f, 0f, null)
            canvas.restore()
        }

        // Dessiner les connexions
        graphe.connexions.forEach { connexion ->
            dessinerConnexion(canvas, connexion)
        }
        // Dessiner les noeuds
        graphe.nœuds.forEach { noeud ->
            dessinerNoeud(canvas, noeud)
        }
        // Dessiner la connexion temporaire
        if (estEnDessinConnexion && debutConnexionTemporaire != null) {
            dessinerConnexionTemporaire(canvas)
        }
    }

    private fun dessinerConnexion(canvas: Canvas, connexion: Connexion) {
        val pinceau = Paint().apply {
            color = connexion.couleur
            strokeWidth = connexion.epaisseur
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        val chemin = Path().apply {
            moveTo(connexion.debut.coordX, connexion.debut.coordY)
            if (connexion.estCourbe) {
                quadTo(connexion.pointDeControleX, connexion.pointDeControleY, connexion.fin.coordX, connexion.fin.coordY)
            } else {
                lineTo(connexion.fin.coordX, connexion.fin.coordY)
            }
        }
        canvas.drawPath(chemin, pinceau)
        // Dessiner l'etiquette de la connexion
        connexion.etiquette?.let { etiquette ->
            val (x, y) = connexion.obtenirPositionEtiquette()
            canvas.drawText(etiquette, x, y, Paint().apply {
                color = Color.BLACK
                textSize = 40f
                isAntiAlias = true
            })
        }
    }

    private fun dessinerNoeud(canvas: Canvas, noeud: Noeud) {
        canvas.drawCircle(noeud.coordX, noeud.coordY, noeud.rayon, Paint().apply {
            color = noeud.couleur
            style = Paint.Style.FILL
            isAntiAlias = true
        })
        val (xEtiquette, yEtiquette) = noeud.obtenirPositionEtiquette()
        canvas.drawText(noeud.etiquette, xEtiquette, yEtiquette, Paint().apply {
            color = noeud.couleurEtiquette
            textSize = 40f
            isAntiAlias = true
        })
    }

    private fun dessinerConnexionTemporaire(canvas: Canvas) {
        canvas.drawLine(
            debutConnexionTemporaire!!.coordX, debutConnexionTemporaire!!.coordY,
            finConnexionTemporaireX, finConnexionTemporaireY,
            Paint().apply {
                color = Color.GRAY
                strokeWidth = 5f
                style = Paint.Style.STROKE
                isAntiAlias = true
                strokeCap = Paint.Cap.ROUND
            }
        )
    }

    private fun gererToucherDebut(x: Float, y: Float) {
        annulerActionsEnAttente()
        estEnDeplacement = false
        estEnCourbureConnexion = false
        estEnDessinConnexion = false
        noeudSelectionne = graphe.trouverNœudA(x, y)
        connexionSelectionnee = if (noeudSelectionne == null) graphe.trouverConnexionA(x, y) else null
        when {
            estModeAjoutObjet -> {
                planifierAction(500) {
                    if (!estEnDeplacement) afficherDialogueEtiquette(x, y)
                }
            }
            estModeAjoutConnexion && noeudSelectionne != null -> {
                estEnDessinConnexion = true
                debutConnexionTemporaire = noeudSelectionne
                finConnexionTemporaireX = x
                finConnexionTemporaireY = y
                invalidate()
            }
            noeudSelectionne != null -> {
                planifierAction(500) {
                    if (!estEnDeplacement) afficherMenuContextuelNoeud(noeudSelectionne!!)
                }
            }
            connexionSelectionnee != null -> {
                gererSelectionConnexion(x, y)
            }
        }
    }

    private fun gererSelectionConnexion(x: Float, y: Float) {
        val (xEtiquette, yEtiquette) = connexionSelectionnee!!.obtenirPositionEtiquette()
        val distance = hypot(x - xEtiquette, y - yEtiquette)
        if (distance <= 50f) {
            planifierAction(500) {
                if (!estEnDeplacement) afficherMenuContextuelConnexion(connexionSelectionnee!!)
            }
        } else {
            // Activer le mode de courbure
            estEnCourbureConnexion = true
        }
    }

    private fun gererToucherDeplacement(x: Float, y: Float) {
        when {
            estEnCourbureConnexion && connexionSelectionnee != null -> {
                // Ajuster la courbure directement avec le doigt
                connexionSelectionnee!!.ajusterCourbure(x, y)
                invalidate()
            }
            estEnDessinConnexion -> {
                finConnexionTemporaireX = x
                finConnexionTemporaireY = y
                invalidate()
            }
            noeudSelectionne != null -> {
                estEnDeplacement = true
                annulerActionsEnAttente()
                noeudSelectionne!!.deplacer(x, y)
                // Mettre a jour les points de controle des connexions liees au noeud deplace
                graphe.connexions.forEach { connexion ->
                    if (connexion.debut == noeudSelectionne || connexion.fin == noeudSelectionne) {
                        connexion.mettreAJourPointDeControle()
                    }
                }
                invalidate()
            }
        }
    }

    private fun gererToucherFin(x: Float, y: Float) {
        annulerActionsEnAttente()
        when {
            estEnCourbureConnexion -> {
                estEnCourbureConnexion = false
                connexionSelectionnee?.let { connexion ->
                    val dx = connexion.fin.coordX - connexion.debut.coordX
                    val dy = connexion.fin.coordY - connexion.debut.coordY
                    if (sqrt(dx * dx + dy * dy) < 100) connexion.estCourbe = false
                }
            }
            estEnDessinConnexion -> {
                val noeudFinal = graphe.trouverNœudA(x, y)
                if (debutConnexionTemporaire != null && noeudFinal != null && noeudFinal != debutConnexionTemporaire) {
                    if (!graphe.connexions.any { it.debut == debutConnexionTemporaire && it.fin == noeudFinal ||
                                it.debut == noeudFinal && it.fin == debutConnexionTemporaire }) {
                        afficherDialogueEtiquetteConnexion(debutConnexionTemporaire!!, noeudFinal)
                    } else {
                        Toast.makeText(context, context.getString(R.string.toast_connection_exists), Toast.LENGTH_SHORT).show()
                    }
                }
                estEnDessinConnexion = false
                debutConnexionTemporaire = null
            }
        }
        noeudSelectionne = null
        connexionSelectionnee = null
        estEnDeplacement = false
        invalidate()
    }

    private fun planifierAction(delai: Long, action: () -> Unit) {
        annulerActionsEnAttente()
        actionEnAttente = Runnable { action() }
        gestionnaire.postDelayed(actionEnAttente!!, delai)
    }

    private fun annulerActionsEnAttente() {
        actionEnAttente?.let {
            gestionnaire.removeCallbacks(it)
            actionEnAttente = null
        }
    }

    private fun afficherDialogueEtiquette(x: Float, y: Float) {
        val saisie = EditText(context).apply {
            hint = context.getString(R.string.dialog_add_object_hint)
        }
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.dialog_add_object_title))
            .setView(saisie)
            .setPositiveButton(context.getString(R.string.button_add)) { _, _ ->
                saisie.text.toString().takeIf { it.isNotBlank() }?.let { etiquette ->
                    graphe.ajouterNœud(Noeud(x, y, etiquette))
                    invalidate()
                }
            }
            .setNegativeButton(context.getString(R.string.button_cancel), null)
            .show()
    }

    private fun afficherDialogueEtiquetteConnexion(debut: Noeud, fin: Noeud) {
        val saisie = EditText(context).apply {
            hint = context.getString(R.string.dialog_add_connection_hint)
        }
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.dialog_add_connection_title))
            .setView(saisie)
            .setPositiveButton(context.getString(R.string.button_create)) { _, _ ->
                saisie.text.toString().takeIf { it.isNotBlank() }?.let { etiquette ->
                    if (graphe.ajouterConnexion(Connexion(debut, fin, etiquette = etiquette))) {
                        invalidate()
                    }
                }
            }
            .setNegativeButton(context.getString(R.string.button_cancel), null)
            .show()
    }

    private fun afficherMenuContextuelNoeud(noeud: Noeud) {
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.dialog_node_context_title, noeud.etiquette))
            .setItems(
                arrayOf(
                    context.getString(R.string.dialog_node_delete),
                    context.getString(R.string.dialog_node_rename),
                    context.getString(R.string.dialog_node_change_color)
                )
            ) { _, choix ->
                when (choix) {
                    0 -> supprimerNoeud(noeud)
                    1 -> afficherDialogueModificationEtiquette(noeud)
                    2 -> afficherDialogueChoixCouleurNoeud(noeud)
                }
            }
            .setNegativeButton(context.getString(R.string.button_cancel), null)
            .show()
    }

    private fun afficherMenuContextuelConnexion(connexion: Connexion) {
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.dialog_edge_context_title))
            .setItems(
                arrayOf(
                    context.getString(R.string.dialog_edge_delete),
                    context.getString(R.string.dialog_edge_rename),
                    context.getString(R.string.dialog_edge_change_color),
                    context.getString(R.string.dialog_edge_change_thickness)
                )
            ) { _, choix ->
                when (choix) {
                    0 -> supprimerConnexion(connexion)
                    1 -> afficherDialogueModificationEtiquetteConnexion(connexion)
                    2 -> afficherDialogueChoixCouleurConnexion(connexion)
                    3 -> afficherDialogueEpaisseurConnexion(connexion)
                }
            }
            .setNegativeButton(context.getString(R.string.button_cancel), null)
            .show()
    }

    private fun supprimerNoeud(noeud: Noeud) {
        graphe.supprimerNœud(noeud)
        invalidate()
        Toast.makeText(context, context.getString(R.string.toast_node_deleted), Toast.LENGTH_SHORT).show()
    }

    private fun supprimerConnexion(connexion: Connexion) {
        graphe.supprimerConnexion(connexion)
        invalidate()
        Toast.makeText(context, context.getString(R.string.toast_edge_deleted), Toast.LENGTH_SHORT).show()
    }

    private fun afficherDialogueModificationEtiquette(noeud: Noeud) {
        val saisie = EditText(context).apply {
            setText(noeud.etiquette)
        }
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.dialog_rename_node_title))
            .setView(saisie)
            .setPositiveButton(context.getString(R.string.button_validate)) { _, _ ->
                saisie.text.toString().takeIf { it.isNotBlank() }?.let {
                    noeud.etiquette = it
                    invalidate()
                }
            }
            .setNegativeButton(context.getString(R.string.button_cancel), null)
            .show()
    }

    private fun afficherDialogueModificationEtiquetteConnexion(connexion: Connexion) {
        val saisie = EditText(context).apply {
            setText(connexion.etiquette)
        }
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.dialog_edge_rename_title))
            .setView(saisie)
            .setPositiveButton(context.getString(R.string.button_validate)) { _, _ ->
                saisie.text.toString().takeIf { it.isNotBlank() }?.let {
                    connexion.etiquette = it
                    invalidate()
                }
            }
            .setNegativeButton(context.getString(R.string.button_cancel), null)
            .show()
    }

    private fun afficherDialogueChoixCouleurNoeud(noeud: Noeud) {
        val couleurs = arrayOf(
            context.getString(R.string.color_red) to ContextCompat.getColor(context, R.color.red),
            context.getString(R.string.color_green) to ContextCompat.getColor(context, R.color.green),
            context.getString(R.string.color_blue) to ContextCompat.getColor(context, R.color.blue),
            context.getString(R.string.color_orange) to ContextCompat.getColor(context, R.color.orange),
            context.getString(R.string.color_cyan) to ContextCompat.getColor(context, R.color.cyan),
            context.getString(R.string.color_magenta) to ContextCompat.getColor(context, R.color.magenta),
            context.getString(R.string.color_black) to ContextCompat.getColor(context, R.color.black)
        )
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.dialog_choose_color_title))
            .setItems(couleurs.map { it.first }.toTypedArray()) { _, choix ->
                noeud.couleur = couleurs[choix].second
                invalidate()
            }
            .setNegativeButton(context.getString(R.string.button_cancel), null)
            .show()
    }

    private fun afficherDialogueChoixCouleurConnexion(connexion: Connexion) {
        val couleurs = arrayOf(
            context.getString(R.string.color_red) to ContextCompat.getColor(context, R.color.red),
            context.getString(R.string.color_green) to ContextCompat.getColor(context, R.color.green),
            context.getString(R.string.color_blue) to ContextCompat.getColor(context, R.color.blue),
            context.getString(R.string.color_orange) to ContextCompat.getColor(context, R.color.orange),
            context.getString(R.string.color_cyan) to ContextCompat.getColor(context, R.color.cyan),
            context.getString(R.string.color_magenta) to ContextCompat.getColor(context, R.color.magenta),
            context.getString(R.string.color_black) to ContextCompat.getColor(context, R.color.black)
        )
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.dialog_choose_color_title))
            .setItems(couleurs.map { it.first }.toTypedArray()) { _, choix ->
                connexion.couleur = couleurs[choix].second
                invalidate()
            }
            .setNegativeButton(context.getString(R.string.button_cancel), null)
            .show()
    }

    private fun afficherDialogueEpaisseurConnexion(connexion: Connexion) {
        val saisie = EditText(context).apply {
            setText(connexion.epaisseur.toString())
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.dialog_edge_thickness_title))
            .setView(saisie)
            .setPositiveButton(context.getString(R.string.button_validate)) { _, _ ->
                saisie.text.toString().toFloatOrNull()?.let {
                    connexion.epaisseur = it.coerceIn(1f, 20f)
                    invalidate()
                }
            }
            .setNegativeButton(context.getString(R.string.button_cancel), null)
            .show()
    }
}