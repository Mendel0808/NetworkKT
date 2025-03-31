package ci.miage.mob.networkkt.models

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
    val nœuds = mutableSetOf<Noeud>()
    val connexions = mutableSetOf<Connexion>()

    fun ajouterNœud(nœud: Noeud) {
        if (nœuds.none { it.identifiant == nœud.identifiant }) {
            nœuds.add(nœud)
            Log.d("Graphe", "Nœud ajouté : ${nœud.etiquette}.")
        }
    }

    fun supprimerNœud(nœud: Noeud) {
        if (nœuds.contains(nœud)) {
            nœuds.remove(nœud)
            connexions.removeAll { it.debut == nœud || it.fin == nœud }
            Log.d("Graphe", "Nœud supprimé : ${nœud.etiquette}.")
        }
    }

    fun ajouterConnexion(connexion: Connexion): Boolean {
        Log.d("Graphe", "Nœuds disponibles dans le graphe : ${nœuds.joinToString { it.etiquette }}")

        if (connexion.debut == connexion.fin) {
            Log.d("Graphe", "Connexion non ajoutée : les nœuds de départ et d'arrivée sont identiques.")
            return false
        }
        if (!nœuds.contains(connexion.debut) || !nœuds.contains(connexion.fin)) {
            Log.d("Graphe", "Connexion non ajoutée : un des nœuds n'existe pas dans le graphe.")
            Log.d("Graphe", "Nœud de départ : ${connexion.debut.etiquette}, Nœud d'arrivée : ${connexion.fin.etiquette}")
            return false
        }
        if (connexions.any { (it.debut == connexion.debut && it.fin == connexion.fin) || (it.debut == connexion.fin && it.fin == connexion.debut) }) {
            Log.d("Graphe", "Connexion non ajoutée : une connexion existe déjà entre ces nœuds.")
            return false
        }
        connexions.add(connexion)
        Log.d("Graphe", "Connexion ajoutée entre ${connexion.debut.etiquette} et ${connexion.fin.etiquette}.")
        return true
    }

    fun supprimerConnexion(connexion: Connexion) {
        val supprimee = connexions.remove(connexion)
        if (supprimee) {
            Log.d("Graphe", "Connexion supprimée entre ${connexion.debut.etiquette} et ${connexion.fin.etiquette}.")
        } else {
            Log.d("Graphe", "Échec de la suppression de la connexion entre ${connexion.debut.etiquette} et ${connexion.fin.etiquette}.")
        }
    }

    fun trouverNœudA(x: Float, y: Float): Noeud? {
        return nœuds.find { it.estDedans(x, y) }
    }

    fun sauvegarderDansFichier(contexte: Context, nomFichier: String): Boolean {
        return try {
            val dossier = File(contexte.filesDir, "graphes_sauvegardés")
            if (!dossier.exists()) {
                dossier.mkdir()
            }
            val fichier = File(dossier, nomFichier)
            fichier.writeText(Gson().toJson(this))
            Log.d("Graphe", "Graphe sauvegardé dans ${fichier.absolutePath}.")
            true
        } catch (e: IOException) {
            Log.e("Graphe", "Erreur lors de la sauvegarde du graphe", e)
            false
        }
    }

    fun chargerDepuisFichier(contexte: Context, nomFichier: String): Boolean {
        return try {
            val dossier = File(contexte.filesDir, "graphes_sauvegardés")
            val fichier = File(dossier, nomFichier)
            if (fichier.exists()) {
                val json = fichier.readText()
                val grapheCharge = Gson().fromJson(json, Graph::class.java)

                nœuds.clear()
                connexions.clear()
                nœuds.addAll(grapheCharge.nœuds)

                // Reconstruire les connexions avec les bonnes références aux nœuds
                for (connexion in grapheCharge.connexions) {
                    val nœudDepart = nœuds.find { it.identifiant == connexion.debut.identifiant }
                    val nœudArrivee = nœuds.find { it.identifiant == connexion.fin.identifiant }
                    if (nœudDepart != null && nœudArrivee != null) {
                        val nouvelleConnexion = Connexion(
                            debut = nœudDepart,
                            fin = nœudArrivee,
                            couleur = connexion.couleur,
                            epaisseur = connexion.epaisseur,
                            etiquette = connexion.etiquette,
                            pointDeControleX = connexion.pointDeControleX,
                            pointDeControleY = connexion.pointDeControleY,
                            estCourbe = connexion.estCourbe
                        )
                        connexions.add(nouvelleConnexion)
                    }
                }

                Log.d("Graphe", "Graphe chargé depuis ${fichier.absolutePath}.")
                true
            } else {
                Log.d("Graphe", "Aucun fichier de graphe trouvé.")
                false
            }
        } catch (e: IOException) {
            Log.e("Graphe", "Erreur lors du chargement du graphe", e)
            false
        }
    }

    fun listerFichiersSauvegardes(contexte: Context): List<String> {
        val dossier = File(contexte.filesDir, "graphes_sauvegardés")
        return if (dossier.exists() && dossier.isDirectory) {
            dossier.listFiles()?.map { it.name } ?: emptyList()
        } else {
            emptyList()
        }
    }

    fun trouverConnexionA(x: Float, y: Float): Connexion? {
        return connexions.find { connexion ->
            val (positionEtiquetteX, positionÉtiquetteY) = connexion.obtenirPositionEtiquette()
            val estPrèsDeLÉtiquette = connexion.etiquette != null &&
                    sqrt((x - positionEtiquetteX).pow(2) + (y - positionÉtiquetteY).pow(2)) <= 50f

            estPrèsDeLÉtiquette || estPointPrèsDeLaConnexion(x, y, connexion)
        }
    }

    private fun estPointPrèsDeLaConnexion(px: Float, py: Float, connexion: Connexion): Boolean {
        return if (connexion.estCourbe) estPointPrèsDeLaCourbe(px, py, connexion)
        else estPointPresDeLaLigne(px, py, connexion.debut.coordX, connexion.debut.coordY, connexion.fin.coordX, connexion.fin.coordY, 20f)
    }

    private fun estPointPrèsDeLaCourbe(px: Float, py: Float, connexion: Connexion): Boolean {
        val chemin = Path().apply {
            moveTo(connexion.debut.coordX, connexion.debut.coordY)
            quadTo(connexion.pointDeControleX, connexion.pointDeControleY, connexion.fin.coordX, connexion.fin.coordY)
        }
        val mesure = PathMeasure(chemin, false)
        val point = FloatArray(2)
        for (i in 0..10) {
            mesure.getPosTan(i * mesure.length / 10, point, null)
            if (sqrt((px - point[0]).pow(2) + (py - point[1]).pow(2)) <= 20f) return true
        }
        return false
    }

    private fun estPointPresDeLaLigne(px: Float, py: Float, x1: Float, y1: Float, x2: Float, y2: Float, tolérance: Float): Boolean {
        val longueurLigne = sqrt((x2 - x1).pow(2) + (y2 - y1).pow(2))
        val distance = abs((y2 - y1) * px - (x2 - x1) * py + x2 * y1 - y2 * x1) / longueurLigne
        return distance <= tolérance
    }
}