package com.example.sipta

import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.sipta.databinding.ActivityFragmentBarangBinding
import android.view.*
import android.widget.*
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder

class FragmentBarang : Fragment() {
    private var _binding: ActivityFragmentBarangBinding? = null
    private val binding get() = _binding!!
    private lateinit var thisParent: MainActivityAdmin

    private val urlBarang = "http://192.168.0.120/sipta_api/crud_barang.php"
    private val urlImageFolder = "http://192.168.0.120/sipta_api/images/"

    private var daftarBarang = mutableListOf<HashMap<String, String>>()
    private var listNamaBarangAutoComplete = ArrayList<String>()

    private var listSpinnerKategori = mutableListOf<HashMap<String, String>>()
    private var listSpinnerSales = mutableListOf<HashMap<String, String>>()

    // Variabel Penampung Sementara Data Foto
    private var bitmapFotoTerpilih: Bitmap? = null
    private var ivPreviewDialogRef: ImageView? = null

    // Tambahkan pendaftar pop-up izin ini di baris atas kelas fragment, dekat mulaiKamera
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Jika user mengklik "Izinkan", langsung buka kameranya
            mulaiKamera.launch(null)
        } else {
            Toast.makeText(requireContext(), "Izin kamera ditolak! Tidak bisa memotret produk.", Toast.LENGTH_LONG).show()
        }
    }

    private var uriFotoKamera: Uri? = null
    // Launcher untuk Mengambil Foto Lewat Kamera HP
    private val mulaiKamera = registerForActivityResult(ActivityResultContracts.TakePicture()) { sukses ->
        if (sukses) {
            try {
                val inputStream = requireContext().contentResolver.openInputStream(uriFotoKamera!!)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                bitmapFotoTerpilih = bitmap
                ivPreviewDialogRef?.setImageBitmap(bitmap)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    // Launcher untuk Memilih Gambar Lewat Galeri File HP
    private val mulaiGaleri = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream: InputStream? = requireContext().contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                bitmapFotoTerpilih = bitmap
                ivPreviewDialogRef?.setImageBitmap(bitmap)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        thisParent = activity as MainActivityAdmin
        _binding = ActivityFragmentBarangBinding.inflate(inflater, container, false)

        binding.acBarang.setOnItemClickListener { parent, _, position, _ ->
            val selectedName = parent.getItemAtPosition(position).toString()
            loadDataBarang(selectedName)
        }

        binding.acBarang.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                loadDataBarang(s.toString())
            }
        })

        binding.fabAddBarang.setOnClickListener { showBarangDialog(null) }

        // 🟢 TAMBAHKAN KODE BARU INI DI SINI:
//        binding.lvBarang.setOnItemClickListener { _, _, position, _ ->
//            // Ambil data hashmap barang berdasarkan baris yang diklik
//            val itemTerpilih = daftarBarang[position]
//            val namaBarang = itemTerpilih["nama"] ?: ""
//            val kodeBarang = itemTerpilih["kode_barang"] ?: ""
//
//            // Panggil dialog popup QR Code
//            tampilkanDialogDetailBarang(namaBarang, kodeBarang)
//        }

        loadDataBarang("")
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        loadDataBarang("")
    }

    private fun loadDataBarang(query: String) {
        val request = object : StringRequest(Request.Method.POST, urlBarang,
            Response.Listener { response ->
                if (isAdded && activity != null) {
                    try {
                        daftarBarang.clear()
                        listNamaBarangAutoComplete.clear()
                        val jsonArray = JSONArray(response)

                        for (x in 0 until jsonArray.length()) {
                            val jsonObject = jsonArray.getJSONObject(x)
                            val hm = HashMap<String, String>()
                            hm["id"] = jsonObject.getString("id")
                            hm["kode_barang"] = jsonObject.getString("kode_barang")
                            hm["nama"] = jsonObject.getString("nama")
                            hm["stok"] = jsonObject.getString("stok")
                            hm["harga_beli"] = jsonObject.getString("harga_beli")
                            hm["harga_jual"] = jsonObject.getString("harga_jual")
                            hm["nama_kategori"] = jsonObject.getString("nama_kategori")
                            hm["nama_sales"] = jsonObject.getString("nama_sales")
                            hm["id_kategori"] = jsonObject.getString("kategori_id")
                            hm["id_sales"] = jsonObject.getString("sales_id")
                            hm["foto"] = jsonObject.optString("foto", "")

                            daftarBarang.add(hm)
                            listNamaBarangAutoComplete.add(jsonObject.getString("nama"))
                        }

                        val autoAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, listNamaBarangAutoComplete)
                        binding.acBarang.setAdapter(autoAdapter)

                        val adapter = object : ArrayAdapter<HashMap<String, String>>(requireContext(), R.layout.item_data_barang, daftarBarang) {
                            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                                val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_data_barang, parent, false)

                                val txKodeBarang = view.findViewById<TextView>(R.id.txKodeBarang)
                                val txNama = view.findViewById<TextView>(R.id.txNamaBarang)
                                val txStok = view.findViewById<TextView>(R.id.txStokBarang)
                                val txHarga = view.findViewById<TextView>(R.id.txHargaJual)
                                val txInfo = view.findViewById<TextView>(R.id.txInfoKategoriSales)
                                val btnDel = view.findViewById<ImageButton>(R.id.btnDeleteBarang)
                                val imgRow = view.findViewById<ImageView>(R.id.imgFotoBarangRow)

                                val item = getItem(position)!!
                                val stokValue = item["stok"]?.toIntOrNull() ?: 0
                                val idBarang = item["id"]
                                val namaFoto = item["foto"] ?: ""

                                txKodeBarang.text = item["kode_barang"]
                                txNama.text = item["nama"]
                                txStok.text = stokValue.toString()
                                txHarga.text = "Rp ${item["harga_jual"]}"
                                txInfo.text = "${item["nama_kategori"]} | ${item["nama_sales"]}"

                                // Reset gambar bawaan row list
                                imgRow.setImageResource(android.R.drawable.ic_menu_gallery)

                                // Mengunduh gambar dari server Laragon secara asinkronus (Thread Terpisah)
                                if (namaFoto.isNotEmpty() && namaFoto != "null") {
                                    val fullUrlGambar = urlImageFolder + namaFoto
                                    thread {
                                        try {
                                            val url = URL(fullUrlGambar)
                                            val koneksi = url.openConnection() as HttpURLConnection
                                            koneksi.doInput = true
                                            koneksi.connect()
                                            val input = koneksi.inputStream
                                            val bitmapUnduh = BitmapFactory.decodeStream(input)

                                            activity?.runOnUiThread {
                                                imgRow.setImageBitmap(bitmapUnduh)
                                            }
                                        } catch (e: Exception) { e.printStackTrace() }
                                    }
                                }

                                val shape = GradientDrawable()
                                shape.cornerRadius = 100f
                                when {
                                    stokValue <= 10 -> shape.setColor(Color.parseColor("#D32F2F"))
                                    stokValue <= 25 -> shape.setColor(Color.parseColor("#FBC02D"))
                                    else -> shape.setColor(Color.parseColor("#388E3C"))
                                }
                                txStok.background = shape

                                btnDel.setOnClickListener {
                                    AlertDialog.Builder(requireContext())
                                        .setTitle("Hapus Barang")
                                        .setMessage("Apakah Anda yakin ingin menghapus ${item["nama"]}?")
                                        .setPositiveButton("Ya, Hapus") { _, _ ->
                                            hapusBarangPusat(idBarang.toString())
                                        }
                                        .setNegativeButton("Batal", null)
                                        .show()
                                }

//                                view.setOnClickListener {
//                                    Toast.makeText(requireContext(), "Tekan lama untuk mengedit data", Toast.LENGTH_SHORT).show()
//                                }
                                view.setOnClickListener {
                                    val namaBarang = item["nama"] ?: ""
                                    val kodeBarang = item["kode_barang"] ?: ""

                                    // Langsung tembak fungsi popup QR Code dari dalam adapter
                                    tampilkanDialogDetailBarang(namaBarang, kodeBarang)
                                }

                                view.setOnLongClickListener {
                                    showBarangDialog(item)
                                    true
                                }

                                return view
                            }
                        }
                        binding.lvBarang.adapter = adapter

                    } catch (e: Exception) {
                        if (isAdded) Toast.makeText(requireContext(), "Parsing Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            Response.ErrorListener { if (isAdded) Toast.makeText(requireContext(), "Gagal memuat data dari MySQL", Toast.LENGTH_SHORT).show() }) {
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf("mode" to "show", "query" to query)
            }
        }
        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun showBarangDialog(itemEdit: HashMap<String, String>?) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.layout_dialog_barang, null)

        val etKode = dialogView.findViewById<EditText>(R.id.etKode)
        val etNama = dialogView.findViewById<EditText>(R.id.etNama)
        val etStok = dialogView.findViewById<EditText>(R.id.etStok)
        val etHargaBeli = dialogView.findViewById<EditText>(R.id.etHargaBeli)
        val etHargaJual = dialogView.findViewById<EditText>(R.id.etHargaJual)
        val spKategori = dialogView.findViewById<Spinner>(R.id.spKategori)
        val spSales = dialogView.findViewById<Spinner>(R.id.spSales)

        val btnAmbilFoto = dialogView.findViewById<Button>(R.id.btnAmbilFoto)
        val imgPreviewDialog = dialogView.findViewById<ImageView>(R.id.imgPreviewDialog)

        ivPreviewDialogRef = imgPreviewDialog
        bitmapFotoTerpilih = null // Reset pilihan foto setiap kali dialog dibuka

        // Logika Klik Tombol Pilih Gambar (Muncul Pilihan Kamera atau Galeri)
        btnAmbilFoto.setOnClickListener {
            val opsi = arrayOf("Ambil Foto (Kamera)", "Pilih dari Galeri")
            AlertDialog.Builder(requireContext())
                .setTitle("Sumber Foto Produk")
                .setItems(opsi) { _, urutan ->
                    if (urutan == 0) {
                        // KUNCI PENCEGAH FORCE CLOSE: Cek apakah izin kamera sudah diberikan oleh HP
                        if (androidx.core.content.ContextCompat.checkSelfPermission(
                                requireContext(), android.Manifest.permission.CAMERA
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED) {

                            // 1. Ambil teks kode barang dari EditText dialog untuk penamaan file di HP
                            val kodeInput = etKode.text.toString().trim()
                            val suffixKode = if(kodeInput.isNotEmpty()) kodeInput else "BARANG"

                            // 2. Buat slot file kosong di DCIM/SIPTA dan tampung alamat lokasinya
                            uriFotoKamera = buatUriFotoDiDCIM(suffixKode)

                            // 3. Buka kamera dengan membawa alamat lokasi file tersebut
                            if (uriFotoKamera != null) {
                                mulaiKamera.launch(uriFotoKamera) // <-- null diganti menjadi uriFotoKamera
                            } else {
                                Toast.makeText(requireContext(), "Gagal menyiapkan media penyimpanan", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                        }
                    } else {
                        mulaiGaleri.launch("image/*") // Galeri tidak sensitif kamera, langsung buka aman
                    }
                }.show()
        }

        val requestSpinner = object : StringRequest(Request.Method.POST, urlBarang,
            Response.Listener { response ->
                if (isAdded && activity != null) {
                    try {
                        listSpinnerKategori.clear()
                        listSpinnerSales.clear()

                        val jsonObject = JSONObject(response)
                        val arrayKat = jsonObject.getJSONArray("kategori")
                        val arraySal = jsonObject.getJSONArray("sales")

                        val namaKategoriList = ArrayList<String>()
                        for (i in 0 until arrayKat.length()) {
                            val obj = arrayKat.getJSONObject(i)
                            val hm = HashMap<String, String>()
                            hm["id"] = obj.getString("id")
                            hm["nama_kategori"] = obj.getString("nama_kategori")
                            listSpinnerKategori.add(hm)
                            namaKategoriList.add(obj.getString("nama_kategori"))
                        }

                        val namaSalesList = ArrayList<String>()
                        for (i in 0 until arraySal.length()) {
                            val obj = arraySal.getJSONObject(i)
                            val hm = HashMap<String, String>()
                            hm["id"] = obj.getString("id")
                            hm["nama_sales"] = obj.getString("nama_sales")
                            listSpinnerSales.add(hm)
                            namaSalesList.add(obj.getString("nama_sales"))
                        }

                        spKategori.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, namaKategoriList)
                        spSales.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, namaSalesList)

                        if (itemEdit != null) {
                            val posKat = namaKategoriList.indexOf(itemEdit["nama_kategori"])
                            val posSal = namaSalesList.indexOf(itemEdit["nama_sales"])
                            if (posKat != -1) spKategori.setSelection(posKat)
                            if (posSal != -1) spSales.setSelection(posSal)
                        }

                    } catch (e: Exception) { e.printStackTrace() }
                }
            }, Response.ErrorListener {}) {
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf("mode" to "get_spinner_data")
            }
        }
        Volley.newRequestQueue(requireContext()).add(requestSpinner)

        if (itemEdit != null) {
            etKode.setText(itemEdit["kode_barang"])
            etNama.setText(itemEdit["nama"])
            etStok.setText(itemEdit["stok"])
            etHargaBeli.setText(itemEdit["harga_beli"])
            etHargaJual.setText(itemEdit["harga_jual"])
            etKode.isEnabled = false
            etKode.alpha = 0.6f

            // Jika dalam mode EDIT dan ada nama fotonya, download fotonya untuk pratinjau dialog
            val namaFotoLama = itemEdit["foto"] ?: ""
            if (namaFotoLama.isNotEmpty() && namaFotoLama != "null") {
                thread {
                    try {
                        val bitmapLama = BitmapFactory.decodeStream(URL(urlImageFolder + namaFotoLama).openConnection().inputStream)
                        activity?.runOnUiThread { imgPreviewDialog.setImageBitmap(bitmapLama) }
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
        } else {
            etKode.isEnabled = true
            etKode.alpha = 1.0f
        }

        val mDialog = AlertDialog.Builder(requireContext())
            .setTitle(if (itemEdit == null) "Tambah Barang Baru" else "Edit Data Barang")
            .setView(dialogView)
            .setPositiveButton("Simpan", null)
            .setNegativeButton("Batal", null)
            .create()

        mDialog.show()

        mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val kode = etKode.text.toString().trim()
            val nama = etNama.text.toString().trim()

            if (kode.isEmpty() || nama.isEmpty()) {
                Toast.makeText(context, "Kode dan Nama wajib diisi!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val idKat = listSpinnerKategori[spKategori.selectedItemPosition]["id"].toString()
            val idSal = listSpinnerSales[spSales.selectedItemPosition]["id"].toString()

            // Fungsi Pembantu Mengonversi Gambar ke Teks String Base64 sebelum dikirim Volley
            var teksBase64Foto = ""
            bitmapFotoTerpilih?.let { bmp ->
                val streamKeluar = ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.JPEG, 60, streamKeluar) // Kompresi kualitas gambar ke 60% agar hemat kuota bandwidth
                val byteGambar = streamKeluar.toByteArray()
                teksBase64Foto = Base64.encodeToString(byteGambar, Base64.DEFAULT)
            }

            val requestAction = object : StringRequest(Request.Method.POST, urlBarang,
                Response.Listener { response ->
                    try {
                        val jsonRes = JSONObject(response)
                        if (jsonRes.getString("kode") == "000") {
                            Toast.makeText(context, "Data dan Foto Berhasil Sinkron", Toast.LENGTH_SHORT).show()
                            loadDataBarang("")
                            mDialog.dismiss()
                        } else if (jsonRes.getString("kode") == "111") {
                            etKode.error = "Kode sudah terdaftar!"
                        } else if (jsonRes.getString("kode") == "222") {
                            etNama.error = "Nama sudah terdaftar!"
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }, Response.ErrorListener {}) {
                override fun getParams(): MutableMap<String, String> {
                    val params = HashMap<String, String>()
                    params["mode"] = if (itemEdit == null) "insert" else "update"
                    if (itemEdit != null) params["id"] = itemEdit["id"].toString()
                    params["kode_barang"] = kode
                    params["nama"] = nama
                    params["stok"] = etStok.text.toString().toIntOrNull()?.toString() ?: "0"
                    params["harga_beli"] = etHargaBeli.text.toString().toIntOrNull()?.toString() ?: "0"
                    params["harga_jual"] = etHargaJual.text.toString().toIntOrNull()?.toString() ?: "0"
                    params["id_kategori"] = idKat
                    params["id_sales"] = idSal
                    params["foto_base64"] = teksBase64Foto // Kirim data string teks gambar ke server Laragon
                    return params
                }
            }
            Volley.newRequestQueue(requireContext()).add(requestAction)
        }
    }

    private fun hapusBarangPusat(id: String) {
        val requestDel = object : StringRequest(Request.Method.POST, urlBarang,
            Response.Listener { response ->
                if (JSONObject(response).getString("kode") == "000") {
                    loadDataBarang("")
                    Toast.makeText(context, "Barang Berhasil Dihapus dari MySQL", Toast.LENGTH_SHORT).show()
                }
            }, Response.ErrorListener {}) {
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf("mode" to "delete", "id" to id)
            }
        }
        Volley.newRequestQueue(requireContext()).add(requestDel)
    }

    private fun buatUriFotoDiDCIM(kodeBarang: String): Uri? {
        // Format penamaan file di HP: yyyyMMdd_HHmmss sesuai request-mu
        val waktuSekarang = android.text.format.DateFormat.format("yyyyMMdd_HHmmss", java.util.Date())
        val namaFile = "IMG_${kodeBarang}_$waktuSekarang"

        val resolver = requireContext().contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$namaFile.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")

            // RUTE UTAMA: Membuat folder baru 'SIPTA' di dalam direktori DCIM HP
            put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/SIPTA")
        }

        return resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    }

    private fun buatGambarQR(teksKode: String): Bitmap? {
        return try {
            val barcodeEncoder = BarcodeEncoder()
            // Mengubah string kode barang menjadi Bitmap berukuran 500x500 pixel
            barcodeEncoder.encodeBitmap(teksKode, BarcodeFormat.QR_CODE, 500, 500)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun tampilkanDialogDetailBarang(namaBarang: String, kodeBarang: String) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.layout_dialog_detail_barang, null)

        val tvNama = dialogView.findViewById<TextView>(R.id.tvNamaBarangDetail)
        val tvKode = dialogView.findViewById<TextView>(R.id.tvKodeBarangDetail)
        val ivQr = dialogView.findViewById<ImageView>(R.id.ivQrCodeBarang)

        tvNama.text = namaBarang
        tvKode.text = "Kode: $kodeBarang"

        // Panggil fungsi pembuat QR
        val bitmapQr = buatGambarQR(kodeBarang)
        if (bitmapQr != null) {
            ivQr.setImageBitmap(bitmapQr) // Pasang gambar QR ke ImageView
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Informasi QR Produk")
            .setView(dialogView)
            .setPositiveButton("Selesai", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}