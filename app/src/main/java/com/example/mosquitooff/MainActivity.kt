package com.example.mosquitooff

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.pdf.PdfDocument
import android.location.Geocoder
import android.os.Bundle
import android.os.Environment
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
import org.osmdroid.views.overlay.Polygon
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private val mosquitoMarkers = mutableListOf<Marker>()
    private var selectedLocation: GeoPoint? = null
    private var marcadorSelecionado: Marker? = null
    private lateinit var marcadorDB: MarcadorDBHelper
    private val REQUEST_PERMISSIONS_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))
        setContentView(R.layout.activity_main)

        marcadorDB = MarcadorDBHelper(this)
        requestPermissionsIfNecessary(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        )

        map = findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.controller.setZoom(13.0)
        map.controller.setCenter(GeoPoint(-23.5015, -47.4526)) // Sorocaba

        carregarMarcadoresSalvos()

        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, getString(R.string.google_maps_key), Locale.getDefault())
        }

        val autocompleteFragment = AutocompleteSupportFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(R.id.autocomplete_container, autocompleteFragment)
            .commit()

        autocompleteFragment.setPlaceFields(listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG))
        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                place.latLng?.let {
                    selectedLocation = GeoPoint(it.latitude, it.longitude)
                    map.controller.setZoom(17.0)
                    map.controller.animateTo(selectedLocation)
                }
            }

            override fun onError(status: com.google.android.gms.common.api.Status) {
                Toast.makeText(this@MainActivity, "Erro: ${status.statusMessage}", Toast.LENGTH_SHORT).show()
            }
        })

        val botaoAdicionar = findViewById<Button>(R.id.botaoAdicionar)
        val botaoDesfazer = findViewById<Button>(R.id.botaoDesfazer)
        val botaoRemover = findViewById<Button>(R.id.botaoRemover)
        val botaoExportar = findViewById<Button>(R.id.botaoExportarPDF)
        val botaoDeslogar = findViewById<Button>(R.id.botaoDeslogar)
        val botaoSair = findViewById<Button>(R.id.botaoSair)

        botaoAdicionar.setOnClickListener {
            selectedLocation?.let {
                if (!existeMarcadorNoLocal(it)) {
                    adicionarZonaDeCalor(it, 250.0)
                    adicionarMarcador(it, true)
                } else {
                    Toast.makeText(this, "Já existe um marcador neste local.", Toast.LENGTH_SHORT).show()
                }
            } ?: Toast.makeText(this, "Selecione um local primeiro.", Toast.LENGTH_SHORT).show()
        }

        botaoDesfazer.setOnClickListener {
            if (mosquitoMarkers.isNotEmpty()) {
                val last = mosquitoMarkers.removeAt(mosquitoMarkers.size - 1)
                map.overlays.remove(last)
                marcadorDB.removerMarcador(last.position.latitude, last.position.longitude)
                atualizarZonasDeCalor()
                map.invalidate()
            }
        }

        botaoRemover.setOnClickListener {
            marcadorSelecionado?.let {
                map.overlays.remove(it)
                mosquitoMarkers.remove(it)
                marcadorDB.removerMarcador(it.position.latitude, it.position.longitude)
                atualizarZonasDeCalor()
                map.invalidate()
                marcadorSelecionado = null
            }
        }

        botaoExportar.setOnClickListener {
            gerarRelatorioPDF()
        }

        botaoDeslogar.setOnClickListener {
            startActivity(Intent(this, TelaLogin::class.java))
            finish()
        }

        botaoSair.setOnClickListener {
            finishAffinity()
        }
    }

    private fun adicionarMarcador(ponto: GeoPoint, salvarNoBanco: Boolean) {
        val marker = Marker(map)
        marker.position = ponto
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = "Local de dengue"

        val drawable = ContextCompat.getDrawable(this, R.drawable.ic_mosquito)
        if (drawable is BitmapDrawable) {
            val bitmap = Bitmap.createScaledBitmap(drawable.bitmap, 48, 48, true)
            marker.icon = BitmapDrawable(resources, bitmap)
        }

        marker.setOnMarkerClickListener { m, _ ->
            marcadorSelecionado = m
            Toast.makeText(this, "Marcador selecionado. Clique em Remover para excluir.", Toast.LENGTH_SHORT).show()
            true
        }

        map.overlays.add(marker)
        mosquitoMarkers.add(marker)
        if (salvarNoBanco) marcadorDB.salvarMarcador(ponto.latitude, ponto.longitude)
        map.invalidate()
    }

    private fun adicionarZonaDeCalor(ponto: GeoPoint, raioMetros: Double) {
        val circulo = Polygon().apply {
            points = Polygon.pointsAsCircle(ponto, raioMetros)
            fillColor = 0x44FF0000
            strokeColor = 0x88FF0000.toInt()
            strokeWidth = 2f
        }
        map.overlays.add(0, circulo) // para o círculo ficar atrás do marcador
    }

    private fun atualizarZonasDeCalor() {
        map.overlays.removeAll { it is Polygon }
        mosquitoMarkers.forEach {
            adicionarZonaDeCalor(it.position, 250.0)
        }
    }

    private fun existeMarcadorNoLocal(ponto: GeoPoint): Boolean {
        return mosquitoMarkers.any {
            val distancia = it.position.distanceToAsDouble(ponto)
            distancia < 15
        }
    }

    private fun carregarMarcadoresSalvos() {
        marcadorDB.buscarTodos().forEach {
            val ponto = GeoPoint(it.first, it.second)
            adicionarMarcador(ponto, false)
            adicionarZonaDeCalor(ponto, 250.0)
        }
    }

    private fun gerarRelatorioPDF() {
        val lista = marcadorDB.buscarTodos()
        if (lista.isEmpty()) {
            Toast.makeText(this, "Nenhum marcador cadastrado.", Toast.LENGTH_SHORT).show()
            return
        }

        val geocoder = Geocoder(this, Locale.getDefault())
        val paint = android.graphics.Paint().apply {
            textSize = 10f
            isAntiAlias = true
        }

        val pageWidth = 300
        val pageHeight = 600
        val lineHeight = 20f
        var yPosition = 30f
        var pageNumber = 1

        val pdfDocument = PdfDocument()
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        // Título
        canvas.drawText("Relatório de Identificação de Pontos de Água Parada", 10f, yPosition, paint)
        yPosition += lineHeight

        lista.forEachIndexed { index, par ->
            val lat = par.first
            val lon = par.second

            // Buscar endereço via Geocoder
            val endereco = try {
                val resultado = geocoder.getFromLocation(lat, lon, 1)
                if (!resultado.isNullOrEmpty()) {
                    val addr = resultado[0]
                    val linha = addr.getAddressLine(0) ?: "${addr.thoroughfare ?: "Rua desconhecida"}, ${addr.subLocality ?: ""}, ${addr.locality ?: ""}"
                    linha
                } else {
                    "Endereço não encontrado"
                }
            } catch (e: Exception) {
                "Erro ao obter endereço"
            }

            // Verifica se cabe na página atual
            if (yPosition + lineHeight * 3 > pageHeight - 30) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPosition = 30f
            }

            // Escreve ponto
            canvas.drawText("${index + 1})", 10f, yPosition, paint)
            yPosition += lineHeight
            canvas.drawText("Lat: %.5f, Lon: %.5f".format(lat, lon), 20f, yPosition, paint)
            yPosition += lineHeight
            canvas.drawText("Endereço: $endereco", 20f, yPosition, paint)
            yPosition += lineHeight
        }

        pdfDocument.finishPage(page)

        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "relatorio_dengue_$timestamp.pdf"
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)

            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.close()

            Toast.makeText(this, "PDF salvo em: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao salvar PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }


    private fun requestPermissionsIfNecessary(permissions: Array<String>) {
        val toRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (toRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, toRequest.toTypedArray(), REQUEST_PERMISSIONS_REQUEST_CODE)
        }
    }
}
