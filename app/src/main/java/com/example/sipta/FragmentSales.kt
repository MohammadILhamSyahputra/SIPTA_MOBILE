package com.example.sipta

import android.app.AlertDialog
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.sipta.databinding.ActivityFragmentSalesBinding
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import android.preference.PreferenceManager

class FragmentSales : Fragment(), View.OnClickListener {
    private var vb: ActivityFragmentSalesBinding? = null
    private val binding get() = vb!!
    private var selectedId: String = ""

    // URL Menembak Server Laragon (Sesuaikan dengan IP Laptop Server-mu)
    private val urlSales = "http://192.168.18.21/sipta_api/crud_sales.php"

    // Menyimpan list data sementara dari MySQL server
    private var daftarSales = mutableListOf<HashMap<String, String>>()

    private var selectedLat: Double = -7.8170  // Default: Kediri Kota
    private var selectedLng: Double = 112.0118
    private lateinit var markerSales: Marker

    // 🟢 KODE BARU: Marker permanen untuk Toko Pusat SIPTA
    private lateinit var markerTokoPusat: Marker

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        vb = ActivityFragmentSalesBinding.inflate(inflater, container, false)

        // 🟢 KODE BARU: Inisialisasi awal konfigurasi OpenStreetMap (OSM) [cite: 264, 265, 266, 267, 269, 271]
        Configuration.getInstance().load(requireContext(), PreferenceManager.getDefaultSharedPreferences(requireContext()))
        binding.mapSales.setTileSource(TileSourceFactory.MAPNIK)
        binding.mapSales.setMultiTouchControls(true)

        // Siapkan penanda pin tunggal di peta
        markerSales = Marker(binding.mapSales)
        markerSales.title = "Lokasi Alamat Sales"

        // 2. 🟢 SOLUSI PASTI BERHASIL: Ganti icon Toko Pusat menggunakan Marker Bawaan Android
        markerTokoPusat = Marker(binding.mapSales)
        markerTokoPusat.title = "Toko Pusat SIPTA"
        markerTokoPusat.position = GeoPoint(-7.825736185861851, 112.00635935822284)

        // 🟢 AMBIL GAMBAR PIN LAIN: Menggunakan ikon penanda peta bawaan sistem Google Android
        val gambarTokoPusat = androidx.core.content.ContextCompat.getDrawable(
            requireContext(),
            android.R.drawable.ic_menu_myplaces // Ini adalah ikon penanda peta (Pin/Bintang) bawaan Android
        )

        // Pasang gambar tersebut ke Toko Pusat
        markerTokoPusat.icon = gambarTokoPusat

        // Atur tumpuan jangkar pas di tengah bawah gambar
        markerTokoPusat.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        tampilkanTitikDiPeta(selectedLat, selectedLng, "Pilih Lokasi")

        binding.btnInsertSales.setOnClickListener(this)
        binding.btnUpdateSales.setOnClickListener(this)
        binding.btnDeleteSales.setOnClickListener(this)

        binding.mapSales.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Saat peta disentuh, larang ScrollView luar merebut gerakan jari kasir
                    view.parent.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // Saat jari diangkat dari peta, kembalikan izin scroll ke halaman utama
                    view.parent.requestDisallowInterceptTouchEvent(false)
                }
            }
            false // Biarkan false agar event klik single-tap di bawah tetap berfungsi
        }

        // 🟢 KODE BARU: Pasang pendeteksi Klik Manual pada Peta
        val receiverKlikPeta = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                p?.let {
                    selectedLat = it.latitude
                    selectedLng = it.longitude
                    binding.tvKoordinatSales.text = "Lokasi Terkunci: $selectedLat, $selectedLng"

                    // Pindahkan penanda pin ke titik baru yang dicolek admin
                    markerSales.position = it
                    if (!binding.mapSales.overlays.contains(markerSales)) {
                        binding.mapSales.overlays.add(markerSales)
                    }
                    binding.mapSales.invalidate() // Segarkan peta
                }
                return true
            }
            override fun longPressHelper(p: GeoPoint?): Boolean { return false }
        }
        binding.mapSales.overlays.add(MapEventsOverlay(receiverKlikPeta))

        binding.lsSales.setOnItemClickListener { parent, _, position, _ ->
            val itemData = daftarSales[position]
            val idBaru = itemData["id"].toString()

            if (selectedId == idBaru) {
                refreshData()
                Toast.makeText(requireContext(), "Pilihan dibatalkan", Toast.LENGTH_SHORT).show()
            } else {
                selectedId = idBaru
                binding.edNamaSales.setText(itemData["nama_sales"])
                binding.edNoTelp.setText(itemData["no_telp"])
                binding.edAlamatSales.setText(itemData["alamat"])

                // 🟢 KODE BARU: Pindahkan kamera peta ke koordinat sales yang dipilih dari list
                val latSales = itemData["latitude"]?.toDoubleOrNull() ?: -7.8170
                val lngSales = itemData["longitude"]?.toDoubleOrNull() ?: 112.0118
                selectedLat = latSales
                selectedLng = lngSales

                binding.tvKoordinatSales.text = "Lokasi Sales: $selectedLat, $selectedLng"
                tampilkanTitikDiPeta(selectedLat, selectedLng, itemData["nama_sales"] ?: "Sales")

                binding.btnInsertSales.isEnabled = false
                binding.btnInsertSales.alpha = 0.5f
            }
        }

        return binding.root
    }

    // 🟢 KODE BARU: Fungsi pembantu memindahkan titik kamera dan menancapkan pin [cite: 288, 289, 291]
    private fun tampilkanTitikDiPeta(latitude: Double, longitude: Double, namaTitle: String) {
        val titikGeo = GeoPoint(latitude, longitude)
        binding.mapSales.controller.setZoom(15.0)
        binding.mapSales.controller.animateTo(titikGeo)

        markerSales.position = titikGeo
        markerSales.title = namaTitle
        if (!binding.mapSales.overlays.contains(markerSales)) {
            binding.mapSales.overlays.add(markerSales)
        }

        if (!binding.mapSales.overlays.contains(markerTokoPusat)) {
            binding.mapSales.overlays.add(markerTokoPusat)
        }
        binding.mapSales.invalidate()
    }

    override fun onStart() {
        super.onStart()
        showDataSales()
    }

    private fun showDataSales() {
        val request = object : StringRequest(Request.Method.POST, urlSales,
            Response.Listener { response ->
                try {
                    daftarSales.clear()
                    val jsonArray = JSONArray(response)

                    for (x in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(x)
                        val hm = HashMap<String, String>()
                        hm["id"] = jsonObject.getString("id")
                        hm["nama_sales"] = jsonObject.getString("nama_sales")
                        hm["no_telp"] = jsonObject.getString("no_telp")
                        hm["alamat"] = jsonObject.getString("alamat")

                        // 🟢 KODE BARU: Tangkap data koordinat dari database Laragon
                        hm["latitude"] = jsonObject.optString("latitude", "-7.8170")
                        hm["longitude"] = jsonObject.optString("longitude", "112.0118")

                        daftarSales.add(hm)
                    }

                    val adapter = SimpleAdapter(
                        requireContext(), daftarSales, R.layout.item_data_sales,
                        arrayOf("nama_sales", "no_telp", "alamat"),
                        intArrayOf(R.id.txNamaSales, R.id.txNoTelp, R.id.txAlamat)
                    )
                    binding.lsSales.adapter = adapter
                } catch (e: Exception) {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Parsing Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            Response.ErrorListener { error ->
                Toast.makeText(requireContext(), "Gagal mengambil data sales", Toast.LENGTH_SHORT).show()
            }) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["mode"] = "show"
                return params
            }
        }
        Volley.newRequestQueue(requireContext()).add(request)
    }

    override fun onClick(v: View?) {
        val dialog = AlertDialog.Builder(requireContext())

        when (v?.id) {
            R.id.btnInsertSales -> {
                val nama = binding.edNamaSales.text.toString().trim()
                val telp = binding.edNoTelp.text.toString().trim()
                val alamat = binding.edAlamatSales.text.toString().trim()

                if (selectedId.isNotEmpty()) {
                    Toast.makeText(requireContext(), "Gunakan tombol UPDATE untuk mengubah data", Toast.LENGTH_SHORT).show()
                } else if (nama.isEmpty()) {
                    Toast.makeText(requireContext(), "Nama sales tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                } else {
                    dialog.setTitle("Konfirmasi Simpan")
                        .setMessage("Apakah data sales ini sudah benar?")
                        .setPositiveButton("Ya") { _, _ ->
                            val request = object : StringRequest(Request.Method.POST, urlSales,
                                Response.Listener { response ->
                                    val jsonObject = JSONObject(response)
                                    val kode = jsonObject.getString("kode")
                                    if (kode == "000") {
                                        Toast.makeText(requireContext(), "Sales berhasil disimpan pusat", Toast.LENGTH_SHORT).show()
                                        refreshData()
                                    } else if (kode == "111") {
                                        Toast.makeText(requireContext(), "Sales dengan nama '$nama' sudah ada!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                Response.ErrorListener { Toast.makeText(requireContext(), "Koneksi Terputus", Toast.LENGTH_SHORT).show() }) {
                                override fun getParams(): MutableMap<String, String> {
                                    val params = HashMap<String, String>()
                                    params["mode"] = "insert"
                                    params["nama_sales"] = nama
                                    params["no_telp"] = telp
                                    params["alamat"] = alamat

                                    // 🟢 KODE BARU: Kirim koordinat saat input sales baru
                                    params["latitude"] = selectedLat.toString()
                                    params["longitude"] = selectedLng.toString()
                                    return params
                                }
                            }
                            Volley.newRequestQueue(requireContext()).add(request)
                        }
                        .setNegativeButton("Tidak", null)
                        .show()
                }
            }

            R.id.btnUpdateSales -> {
                val nama = binding.edNamaSales.text.toString().trim()
                val telp = binding.edNoTelp.text.toString().trim()
                val alamat = binding.edAlamatSales.text.toString().trim()

                if (selectedId.isEmpty()) {
                    Toast.makeText(requireContext(), "Pilih data dari daftar terlebih dahulu!", Toast.LENGTH_SHORT).show()
                } else {
                    dialog.setTitle("Konfirmasi Update")
                        .setMessage("Yakin ingin mengubah data sales ini?")
                        .setPositiveButton("Ya") { _, _ ->
                            val request = object : StringRequest(Request.Method.POST, urlSales,
                                Response.Listener { response ->
                                    val jsonObject = JSONObject(response)
                                    val kode = jsonObject.getString("kode")
                                    if (kode == "000") {
                                        Toast.makeText(requireContext(), "Data sales diperbarui", Toast.LENGTH_SHORT).show()
                                        refreshData()
                                    } else if (kode == "111") {
                                        Toast.makeText(requireContext(), "Nama sales '$nama' sudah digunakan!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                Response.ErrorListener { Toast.makeText(requireContext(), "Koneksi Terputus", Toast.LENGTH_SHORT).show() }) {
                                override fun getParams(): MutableMap<String, String> {
                                    val params = HashMap<String, String>()
                                    params["mode"] = "update"
                                    params["id"] = selectedId
                                    params["nama_sales"] = nama
                                    params["no_telp"] = telp
                                    params["alamat"] = alamat

                                    // 🟢 KODE BARU: Kirim koordinat baru saat update data sales
                                    params["latitude"] = selectedLat.toString()
                                    params["longitude"] = selectedLng.toString()
                                    return params
                                }
                            }
                            Volley.newRequestQueue(requireContext()).add(request)
                        }
                        .setNegativeButton("Tidak", null)
                        .show()
                }
            }

            R.id.btnDeleteSales -> {
                if (selectedId.isNotEmpty()) {
                    dialog.setTitle("Konfirmasi Hapus")
                        .setMessage("Yakin ingin menghapus data sales ini?")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton("Ya") { _, _ ->
                            val request = object : StringRequest(Request.Method.POST, urlSales,
                                Response.Listener { response ->
                                    if (JSONObject(response).getString("kode") == "000") {
                                        Toast.makeText(requireContext(), "Data sales berhasil dihapus", Toast.LENGTH_SHORT).show()
                                        refreshData()
                                    }
                                },
                                Response.ErrorListener { Toast.makeText(requireContext(), "Koneksi Terputus", Toast.LENGTH_SHORT).show() }) {
                                override fun getParams(): MutableMap<String, String> {
                                    val params = HashMap<String, String>()
                                    params["mode"] = "delete"
                                    params["id"] = selectedId
                                    return params
                                }
                            }
                            Volley.newRequestQueue(requireContext()).add(request)
                        }
                        .setNegativeButton("Tidak", null)
                        .show()
                }
            }
        }
    }

    private fun refreshData() {
        binding.edNamaSales.setText("")
        binding.edNoTelp.setText("")
        binding.edAlamatSales.setText("")
        selectedId = ""
        binding.btnInsertSales.isEnabled = true
        binding.btnInsertSales.alpha = 1.0f

        // 🟢 KODE BARU: Reset Posisi Peta saat data dibersihkan
        selectedLat = -7.8170
        selectedLng = 112.0118
        binding.tvKoordinatSales.text = "Titik Koordinat: (Klik pada peta untuk mengunci lokasi sales)"
        tampilkanTitikDiPeta(selectedLat, selectedLng, "Pilih Lokasi")

        showDataSales()
    }

    // 🟢 KODE BARU: Siklus hidup agar map hemat memory RAM [cite: 324, 327]
    override fun onResume() {
        super.onResume()
        binding.mapSales.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapSales.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        vb = null
    }
}