package ci.miage.mob.networkkt

import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import ci.miage.mob.networkkt.models.Graph
import ci.miage.mob.networkkt.models.Node
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var graph: Graph
    private lateinit var graphView: GraphView
    private var dialog: AlertDialog? = null // Variable pour stocker la boîte de dialogue

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateLocale() // Mettre à jour la locale en fonction de la langue du système
        setContentView(R.layout.activity_main)

        // Configurer la Toolbar comme barre d'action
        setSupportActionBar(findViewById(R.id.toolbar))

        // Initialiser le graphe avec quelques nœuds de démonstration
        graph = Graph()
        graph.addNode(Node(200f, 200f, "A"))
        graph.addNode(Node(400f, 400f, "B"))

        // Initialiser le GraphView et lui passer le graphe
        graphView = findViewById(R.id.graphView)
        graphView.setGraph(graph)
    }

    // Mettre à jour la locale en fonction de la langue du système
    private fun updateLocale() {
        val locale = when (Locale.getDefault().language) {
            "fr" -> Locale("fr") // Français
            else -> Locale("en") // Anglais (par défaut)
        }
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    // Redémarrer l'activité lors du changement de langue
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateLocale() // Mettre à jour la locale lorsque la configuration change
        recreate() // Redémarrer l'activité pour appliquer les changements
    }

    // Fermer la boîte de dialogue lors de la destruction de l'Activity
    override fun onDestroy() {
        super.onDestroy()
        dialog?.dismiss() // Fermer la boîte de dialogue si elle est ouverte
    }

    // Créer le menu
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    // Gérer les actions du menu
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_reset -> {
                graph.nodes.clear()
                graph.edges.clear()
                graphView.invalidate()
                Toast.makeText(this, getString(R.string.toast_graph_reset), Toast.LENGTH_SHORT).show()
                true
            }
            R.id.menu_save -> {
                showSaveDialog()
                true
            }
            R.id.menu_load -> {
                showLoadNetworkDialog()
                true
            }
            R.id.menu_add_object -> {
                graphView.setAddObjectMode(true)
                Toast.makeText(this, getString(R.string.toast_add_object_mode), Toast.LENGTH_SHORT).show()
                true
            }
            R.id.menu_add_connection -> {
                graphView.setAddConnectionMode(true)
                Toast.makeText(this, getString(R.string.toast_add_connection_mode), Toast.LENGTH_SHORT).show()
                true
            }
            R.id.menu_edit -> {
                graphView.setAddObjectMode(false)
                graphView.setAddConnectionMode(false)
                Toast.makeText(this, getString(R.string.toast_edit_mode), Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Boîte de dialogue pour sauvegarder un réseau
    private fun showSaveDialog() {
        val input = EditText(this)
        val dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_save_title))
            .setMessage(getString(R.string.dialog_save_message))
            .setView(input)
            .setPositiveButton(getString(R.string.dialog_save_button)) { _, _ ->
                val filename = input.text.toString()
                if (filename.isNotEmpty()) {
                    val fullFilename = if (filename.endsWith(".json")) filename else "$filename.json"
                    if (graph.saveToFile(this, fullFilename)) {
                        Toast.makeText(this, getString(R.string.toast_network_saved, fullFilename), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, getString(R.string.toast_error_saving), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, getString(R.string.toast_invalid_filename), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.dialog_cancel_button), null)
            .create()
        dialog.show()
    }

    // Boîte de dialogue pour charger un réseau
    private fun showLoadNetworkDialog() {
        val networks = graph.listSavedNetworks(this)
        if (networks.isEmpty()) {
            Toast.makeText(this, "Aucun réseau sauvegardé trouvé", Toast.LENGTH_SHORT).show()
            return
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle("Charger un réseau")
            .setItems(networks.toTypedArray()) { _, which ->
                val filename = networks[which]
                if (graph.loadFromFile(this, filename)) {
                    graphView.invalidate() // Redessiner la vue après le chargement
                    Toast.makeText(this, "Réseau chargé : $filename", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Erreur lors du chargement", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Annuler", null)
            .create()
        dialog.show()
    }
}