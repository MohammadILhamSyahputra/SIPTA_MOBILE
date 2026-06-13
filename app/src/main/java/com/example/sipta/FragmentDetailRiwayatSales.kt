package com.example.sipta

import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.sipta.databinding.ActivityFragmentDetailRiwayatSalesBinding
import android.view.*
import android.widget.*
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject
import android.text.InputType

class FragmentDetailRiwayatSales : Fragment() {

    private var _binding: ActivityFragmentDetailRiwayatSalesBinding? = null
    private val binding get() = _binding!!
    private var idRiwayat: Int = 0
    private var salesId: Int = 0

    // URL API menuju Laragon (Sesuaikan IP Laptop Server kelompokmu)
    private val urlDetail = "http://192.168.1.127/sipta_api/crud_detail_riwayat.php"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityFragmentDetailRiwayatSalesBinding.inflate(inflater, container, false)
        idRiwayat = arguments?.getInt("id_riwayat") ?: 0
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadDataHeaderDanItems()

        binding.btnTambahBarang.setOnClickListener {
            simpanBarang()
        }
    }

    private fun loadDataHeaderDanItems() {
        val queue = Volley.newRequestQueue(requireContext())

        // 1. Ambil Info Header Kunjungan Sales
        val reqHeader = object : StringRequest(Request.Method.POST, urlDetail,
            Response.Listener { response ->
                if (isAdded && activity != null) {
                    try {
                        val obj = JSONObject(response)
                        salesId = obj.getInt("sales_id")
                        val status = obj.getString("status")

                        binding.tvNamaSales.text = "Sales: " + obj.getString("nama_sales")
                        binding.tvStatus.text = "Status: " + status
                        binding.tvTanggal.text = "Tanggal Kunjungan: " + formatTanggalWaktu(obj.getString("tanggal_kunjungan"))
                        binding.tvCreated.text = "Dibuat Pada: " + formatTanggalWaktu(obj.getString("created_at"))

                        // PROTEKSI KUNCI: Kunci tombol jika status kunjungan belum kelar ("sudah datang")
                        if (status != "sudah datang") {
                            binding.btnTambahBarang.isEnabled = false
                            binding.btnTambahBarang.alpha = 0.5f
                        } else {
                            binding.btnTambahBarang.isEnabled = true
                            binding.btnTambahBarang.alpha = 1.0f
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }, Response.ErrorListener {}) {
            override fun getParams(): MutableMap<String, String> = hashMapOf(
                "mode" to "get_info",
                "riwayat_sales_id" to idRiwayat.toString()
            )
        }

        // 2. Ambil List Array Item Transaksi Detail Barang
        val reqItems = object : StringRequest(Request.Method.POST, urlDetail,
            Response.Listener { response ->
                if (isAdded && activity != null) {
                    try {
                        binding.containerBarang.removeAllViews()
                        val jsonArray = JSONArray(response)

                        for (x in 0 until jsonArray.length()) {
                            val item = jsonArray.getJSONObject(x)
                            val idDetail = item.getInt("id")
                            val nama = item.getString("nama")
                            val masuk = item.getInt("qty_masuk")
                            val retur = item.getInt("qty_return")

                            val row = LinearLayout(requireContext()).apply {
                                orientation = LinearLayout.HORIZONTAL
                                setPadding(8, 12, 8, 12)
                            }

                            val tvNama = TextView(requireContext()).apply {
                                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.5f)
                                text = nama
                            }

                            val tvMasuk = TextView(requireContext()).apply {
                                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                                text = masuk.toString()
                                gravity = Gravity.CENTER
                            }

                            val tvRetur = TextView(requireContext()).apply {
                                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                                text = retur.toString()
                                gravity = Gravity.CENTER
                            }

                            val btnEdit = ImageView(requireContext()).apply {
                                val size = (28 * resources.displayMetrics.density).toInt()
                                layoutParams = LinearLayout.LayoutParams(size, size)
                                setImageResource(R.drawable.edit)
                                setOnClickListener { editBarang(idDetail) }
                            }

                            val aksi = LinearLayout(requireContext()).apply {
                                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.8f)
                                gravity = Gravity.CENTER
                                addView(btnEdit)
                            }

                            row.addView(tvNama)
                            row.addView(tvMasuk)
                            row.addView(tvRetur)
                            row.addView(aksi)

                            val line = View(requireContext()).apply {
                                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
                                setBackgroundColor(0xFFDDDDDD.toInt())
                            }

                            binding.containerBarang.addView(row)
                            binding.containerBarang.addView(line)
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }, Response.ErrorListener {}) {
            override fun getParams(): MutableMap<String, String> = hashMapOf(
                "mode" to "show_items",
                "riwayat_sales_id" to idRiwayat.toString()
            )
        }

        queue.add(reqHeader)
        queue.add(reqItems)
    }

    private fun simpanBarang() {
        // SAFETY VALIDATION: Jika async reqHeader belum selesai memuat, cegah buka dialog
        if (salesId == 0) {
            Toast.makeText(requireContext(), "Mohon tunggu, info sales sedang disinkronkan...", Toast.LENGTH_SHORT).show()
            return
        }

        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 16, 32, 16)
        }

        val spinner = Spinner(requireContext())
        val etMasuk = EditText(requireContext()).apply {
            hint = "Qty Masuk"
            inputType = InputType.TYPE_CLASS_NUMBER // Memaksa keyboard berbentuk angka saja
        }
        val etRetur = EditText(requireContext()).apply {
            hint = "Qty Retur"
            inputType = InputType.TYPE_CLASS_NUMBER // Memaksa keyboard berbentuk angka saja
        }

        val listBarang = ArrayList<String>()
        val listId = ArrayList<Int>()

        layout.addView(spinner)
        layout.addView(etMasuk)
        layout.addView(etRetur)

        // Tarik daftar barang bawaan milik sales ini dari MySQL
        val reqBarang = object : StringRequest(Request.Method.POST, urlDetail,
            Response.Listener { response ->
                try {
                    // PERBAIKAN KUNCI 1: Clear data lama agar tidak menumpuk ganda di memori RAM HP
                    listId.clear()
                    listBarang.clear()

                    val array = JSONArray(response)
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        listId.add(obj.getInt("id"))
                        listBarang.add(obj.getString("nama"))
                    }

                    if (isAdded) {
                        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, listBarang)
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        spinner.adapter = adapter
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }, Response.ErrorListener {}) {
            override fun getParams(): MutableMap<String, String> = hashMapOf(
                "mode" to "get_barang_spinner",
                "sales_id" to salesId.toString()
            )
        }
        Volley.newRequestQueue(requireContext()).add(reqBarang)

        AlertDialog.Builder(requireContext())
            .setTitle("Tambah Barang")
            .setView(layout)
            .setPositiveButton("Simpan") { _, _ ->
                // PERBAIKAN KUNCI 2: Proteksi jika spinner kosong/sales belum punya barang terpaut
                if (spinner.selectedItem == null || listId.isEmpty()) {
                    Toast.makeText(requireContext(), "Gagal: Barang tidak tersedia!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val posisi = spinner.selectedItemPosition
                val idBarang = listId[posisi]

                val reqInsert = object : StringRequest(Request.Method.POST, urlDetail,
                    Response.Listener { response ->
                        try {
                            val jsonRes = JSONObject(response)
                            if (jsonRes.getString("kode") == "000") {
                                Toast.makeText(requireContext(), "Barang berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                                loadDataHeaderDanItems() // Refresh list item rill otomatis
                            } else {
                                Toast.makeText(requireContext(), "Gagal menyimpan ke database pusat", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) { e.printStackTrace() }
                    }, Response.ErrorListener {}) {
                    override fun getParams(): MutableMap<String, String> = hashMapOf(
                        "mode" to "insert_item",
                        "riwayat_sales_id" to idRiwayat.toString(),
                        "barang_id" to idBarang.toString(),
                        "qty_masuk" to etMasuk.text.toString().trim(),
                        "qty_return" to etRetur.text.toString().trim()
                    )
                }
                Volley.newRequestQueue(requireContext()).add(reqInsert)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun editBarang(idDetail: Int) {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 16, 32, 16)
        }

        val etMasuk = EditText(requireContext()).apply {
            hint = "Qty Masuk"
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        val etRetur = EditText(requireContext()).apply {
            hint = "Qty Retur"
            inputType = InputType.TYPE_CLASS_NUMBER
        }

        layout.addView(etMasuk)
        layout.addView(etRetur)

        // Tarik data qty kuantitas lama dari MySQL pusat
        val reqGetOne = object : StringRequest(Request.Method.POST, urlDetail,
            Response.Listener { response ->
                try {
                    val obj = JSONObject(response)
                    etMasuk.setText(obj.getInt("qty_masuk").toString())
                    etRetur.setText(obj.getInt("qty_return").toString())
                } catch (e: Exception) { e.printStackTrace() }
            }, Response.ErrorListener {}) {
            override fun getParams(): MutableMap<String, String> = hashMapOf(
                "mode" to "get_one_item",
                "id_detail" to idDetail.toString()
            )
        }
        Volley.newRequestQueue(requireContext()).add(reqGetOne)

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Barang")
            .setView(layout)
            .setPositiveButton("Update") { _, _ ->
                val reqUpdate = object : StringRequest(Request.Method.POST, urlDetail,
                    Response.Listener { response ->
                        try {
                            val jsonRes = JSONObject(response)
                            if (jsonRes.getString("kode") == "000") {
                                Toast.makeText(requireContext(), "Data stok berhasil diperbarui", Toast.LENGTH_SHORT).show()
                                loadDataHeaderDanItems()
                            }
                        } catch (e: Exception) { e.printStackTrace() }
                    }, Response.ErrorListener {}) {
                    override fun getParams(): MutableMap<String, String> = hashMapOf(
                        "mode" to "update_item",
                        "id_detail" to idDetail.toString(),
                        "qty_masuk" to etMasuk.text.toString().trim(),
                        "qty_return" to etRetur.text.toString().trim()
                    )
                }
                Volley.newRequestQueue(requireContext()).add(reqUpdate)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun formatTanggalWaktu(tanggal: String?): String {
        if (tanggal.isNullOrEmpty() || tanggal == "null") return "-"
        return try {
            val input = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            val output = java.text.SimpleDateFormat("dd MMM yyyy HH:mm", java.util.Locale("id", "ID"))
            val date = input.parse(tanggal)
            output.format(date!!)
        } catch (e: Exception) {
            tanggal
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}