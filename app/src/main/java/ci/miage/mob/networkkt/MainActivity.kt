package ci.miage.mob.networkkt
//1
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import ci.miage.mob.networkkt.models.Graph
import ci.miage.mob.networkkt.models.Node

class MainActivity : AppCompatActivity() {

    private lateinit var graph: Graph
    private lateinit var graphView: GraphView
    private var dialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        // Initialisation du graphe
        graph = Graph()
        graphView = findViewById(R.id.graphView)
        graphView.setGraph(graph)
    }

    override fun onDestroy() {
        super.onDestroy()
        dialog?.dismiss()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_reset -> {
                graph.nodes.clear()
                graph.edges.clear()
                graphView.invalidate()
                Toast.makeText(this, getString(R.string.toast_graphs_reset), Toast.LENGTH_SHORT).show()
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

    private fun showSaveDialog() {
        val input = EditText(this)
        dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.menu_save))
            .setView(input)
            .setPositiveButton(getString(R.string.menu_save)) { _, _ ->
                val filename = input.text.toString()
                if (filename.isNotEmpty()) {
                    val fullFilename = if (filename.endsWith(".json")) filename else "$filename.json"
                    if (graph.saveToFile(this, fullFilename)) {
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
        dialog?.show()
    }

    private fun showLoadNetworkDialog() {
        val networks = graph.listSavedNetworks(this)
        if (networks.isEmpty()) {
            Toast.makeText(this, getString(R.string.toast_no_networks_saved), Toast.LENGTH_SHORT).show()
            return
        }
        dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.menu_load))
            .setItems(networks.toTypedArray()) { _, which ->
                val filename = networks[which]
                if (graph.loadFromFile(this, filename)) {
                    graphView.invalidate()
                    Toast.makeText(
                        this,
                        getString(R.string.toast_network_loaded, filename),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .create()
        dialog?.show()
    }
}