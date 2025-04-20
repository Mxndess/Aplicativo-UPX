package com.example.mosquitooff

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private val mosquitoMarkers = mutableListOf<Marker>()
    private var selectedLocation: GeoPoint? = null
    private val REQUEST_PERMISSIONS_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Solicita permissões
        requestPermissionsIfNecessary(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        )

        // Configura o OSMDroid
        val osmConfig = Configuration.getInstance()
        osmConfig.userAgentValue = packageName
        val basePath = File(cacheDir.absolutePath, "osmdroid")
        osmConfig.osmdroidBasePath = basePath
        osmConfig.osmdroidTileCache = File(basePath, "tile")
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))

        setContentView(R.layout.activity_main)

        // Botões
        val botaoAdicionar = findViewById<Button>(R.id.botaoAdicionar)
        val botaoDesfazer = findViewById<Button>(R.id.botaoDesfazer)
        botaoAdicionar.setBackgroundResource(R.drawable.botao_adicionar)
        botaoDesfazer.setBackgroundResource(R.drawable.botao_desfazer)

        // Inicializa Places API
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, getString(R.string.google_maps_key), Locale.getDefault())
        }

        // Campo de busca com autocomplete
        val autocompleteFragment = AutocompleteSupportFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(R.id.autocomplete_container, autocompleteFragment)
            .commit()

        autocompleteFragment.setPlaceFields(listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG))
        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                place.latLng?.let {
                    selectedLocation = GeoPoint(it.latitude, it.longitude)

                    // Centraliza e aplica zoom ao local selecionado
                    map.controller.setZoom(17.0)
                    map.controller.animateTo(selectedLocation)
                }
            }

            override fun onError(status: com.google.android.gms.common.api.Status) {
                Toast.makeText(this@MainActivity, "Erro: ${status.statusMessage}", Toast.LENGTH_SHORT).show()
            }
        })

        // Inicializa o mapa
        map = findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.controller.setZoom(13.0)
        map.controller.setCenter(GeoPoint(-23.5015, -47.4526)) // Centro inicial: Sorocaba

        // Botão Adicionar
        botaoAdicionar.setOnClickListener {
            selectedLocation?.let {
                adicionarMarcador(it)
            } ?: Toast.makeText(this, "Selecione um local primeiro.", Toast.LENGTH_SHORT).show()
        }

        // Botão Desfazer
        botaoDesfazer.setOnClickListener {
            if (mosquitoMarkers.isNotEmpty()) {
                val last = mosquitoMarkers.removeAt(mosquitoMarkers.lastIndex)
                map.overlays.remove(last)
                map.invalidate()
            } else {
                Toast.makeText(this, "Nenhum marcador para desfazer.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Adiciona marcador no mapa
    private fun adicionarMarcador(ponto: GeoPoint) {
        val tolerancia = 0.0001 // Aproximadamente ~11m de precisão

        // Verifica se já existe marcador nessa posição
        val marcadorExistente = mosquitoMarkers.any { marcador ->
            val latDiff = Math.abs(marcador.position.latitude - ponto.latitude)
            val lonDiff = Math.abs(marcador.position.longitude - ponto.longitude)
            latDiff < tolerancia && lonDiff < tolerancia
        }

        if (marcadorExistente) {
            Toast.makeText(this, "Já existe um marcador neste local.", Toast.LENGTH_SHORT).show()
            return
        }

        // Cria novo marcador
        val marker = Marker(map)
        marker.position = ponto
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = "Local de dengue"

        val drawable = ContextCompat.getDrawable(this, R.drawable.ic_mosquito)
        if (drawable is BitmapDrawable) {
            val bitmap = Bitmap.createScaledBitmap(drawable.bitmap, 64, 64, true)
            marker.icon = BitmapDrawable(resources, bitmap)
        }

        map.overlays.add(marker)
        mosquitoMarkers.add(marker)
        map.invalidate()
    }


    // Solicita permissões se necessário
    private fun requestPermissionsIfNecessary(permissions: Array<String>) {
        val toRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (toRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                toRequest.toTypedArray(),
                REQUEST_PERMISSIONS_REQUEST_CODE
            )
        }
    }
}
