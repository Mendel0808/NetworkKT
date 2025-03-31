package ci.miage.mob.networkkt
//1
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import ci.miage.mob.networkkt.models.Graph

class MainActivity : AppCompatActivity() {

    private lateinit var graph: Graph
    private lateinit var graphView: GraphView
    private var dialogue: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        // Initialisation du graphe
        graph = Graph()
        graphView = findViewById(R.id.graphView)
        graphView.definirGraphe(graph)
        val planAppartement = BitmapFactory.decodeResource(resources, R.drawable.plan_appartement)
        // Définir le plan d'appartement dans le GraphView
        graphView.PlanAppartement(planAppartement)  // à activer ici

    }

    override fun onDestroy() {
        super.onDestroy()
        dialogue?.dismiss()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_reset -> {
                graph.nœuds.clear()
                graph.connexions.clear()
                graphView.invalidate()
                Toast.makeText(this, getString(R.string.toast_graphs_reset), Toast.LENGTH_SHORT).show()
                true
            }
            R.id.menu_save -> {
                afficherDialogueSauvegarde()
                true
            }
            R.id.menu_load -> {
                afficherDialogueChargementReseau()
                true
            }
            R.id.menu_add_object -> {
                graphView.activerModeAjoutObjet(true)
                Toast.makeText(this, getString(R.string.toast_add_object_mode), Toast.LENGTH_SHORT).show()
                true
            }
            R.id.menu_add_connection -> {
                graphView.activerModeAjoutConnexion(true)
                Toast.makeText(this, getString(R.string.toast_add_connection_mode), Toast.LENGTH_SHORT).show()
                true
            }
            R.id.menu_edit -> {
                graphView.activerModeAjoutObjet(false)
                graphView.activerModeAjoutConnexion(false)
                Toast.makeText(this, getString(R.string.toast_edit_mode), Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun afficherDialogueSauvegarde() {
        val saisie = EditText(this)
        dialogue = AlertDialog.Builder(this)
            .setTitle(getString(R.string.menu_save))
            .setView(saisie)
            .setPositiveButton(getString(R.string.menu_save)) { _, _ ->
                val nomFichier = saisie.text.toString()
                if (nomFichier.isNotEmpty()) {
                    val fullFilename = if (nomFichier.endsWith(".json")) nomFichier else "$nomFichier.json"
                    if (graph.sauvegarderDansFichier(this, fullFilename)) {
                        Toast.makeText(
                            this,
                            getString(R.string.toast_network_saved, fullFilename),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(this, getString(R.string.toast_error_saving), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, getString(R.string.toast_invalid_filename), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .create()
        dialogue?.show()
    }

    private fun afficherDialogueChargementReseau() {
        val reseaux = graph.listerFichiersSauvegardes(this)
        if (reseaux.isEmpty()) {
            Toast.makeText(this, getString(R.string.toast_no_networks_saved), Toast.LENGTH_SHORT).show()
            return
        }
        dialogue = AlertDialog.Builder(this)
            .setTitle(getString(R.string.menu_load))
            .setItems(reseaux.toTypedArray()) { _, which ->
                val nomFichier = reseaux[which]
                if (graph.chargerDepuisFichier(this, nomFichier)) {
                    graphView.invalidate()
                    Toast.makeText(
                        this,
                        getString(R.string.toast_network_loaded, nomFichier),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .create()
        dialogue?.show()
    }
}