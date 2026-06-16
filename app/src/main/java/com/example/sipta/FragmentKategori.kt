package com.example.sipta

import android.app.AlertDialog
import android.content.ContentValues
import android.content.DialogInterface
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.sipta.databinding.ActivityFragmentKategoriBinding
import android.view.*
import android.widget.*
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject

class FragmentKategori : Fragment(), View.OnClickListener {
    private var vb: ActivityFragmentKategoriBinding? = null
    private val binding get() = vb!!
    private lateinit var thisParent: MainActivityAdmin
    private lateinit var dialog: AlertDialog.Builder
    private var selectedId: String = ""

    // URL Server Laragon (Sesuaikan dengan IP Laptop Server-mu)
    private val urlKategori = "http://192.168.0.102/sipta_api/crud_kategori.php"

    // Menyimpan list data sementara untuk ListView
    private var daftarKategori = mutableListOf<HashMap<String, String>>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        thisParent = activity as MainActivityAdmin
        vb = ActivityFragmentKategoriBinding.inflate(inflater, container, false)
        dialog = AlertDialog.Builder(thisParent)

        // Event Klik Button
        binding.btnInsert.setOnClickListener(this)
        binding.btnUpdate.setOnClickListener(this)
        binding.btnDelete.setOnClickListener(this)

        // Event Klik pada Item ListKategori
        binding.lsKategori.setOnItemClickListener { parent, view, position, id ->
            val itemData = daftarKategori[position]
            val idBaru = itemData["id"].toString()

            if (selectedId == idBaru) {
                clearForm()
                Toast.makeText(thisParent, "Pilihan dibatalkan", Toast.LENGTH_SHORT).show()
            } else {
                selectedId = idBaru
                binding.edNamaKategori.setText(itemData["nama_kategori"])

                binding.btnInsert.isEnabled = false
                binding.btnInsert.alpha = 0.5f
            }
        }

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        showDataKategori() // Memuat data dari MySQL saat fragment aktif
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btnInsert -> {
                if (selectedId.isNotEmpty()) {
                    Toast.makeText(thisParent, "Data sudah terpilih. Gunakan EDIT atau klik daftar lain.", Toast.LENGTH_SHORT).show()
                } else {
                    showConfirmDialog("INSERT")
                }
            }
            R.id.btnUpdate -> {
                if (selectedId.isEmpty()) {
                    Toast.makeText(thisParent, "Silahkan pilih data kategori terlebih dahulu!", Toast.LENGTH_SHORT).show()
                } else {
                    showConfirmDialog("UPDATE")
                }
            }
            R.id.btnDelete -> {
                if (selectedId.isEmpty()) {
                    Toast.makeText(thisParent, "Silahkan pilih data kategori yang ingin dihapus!", Toast.LENGTH_SHORT).show()
                } else {
                    showConfirmDialog("DELETE")
                }
            }
        }
    }

    private fun showConfirmDialog(action: String) {
        val message = when(action) {
            "DELETE" -> "Yakin akan menghapus kategori ini?"
            else -> "Apakah data sudah benar?"
        }

        dialog.setTitle("Konfirmasi $action")
            .setMessage(message)
            .setIcon(android.R.drawable.ic_dialog_info)
            .setPositiveButton("Ya") { _, _ ->
                when(action) {
                    "INSERT" -> insertData()
                    "UPDATE" -> updateData()
                    "DELETE" -> deleteData()
                }
            }
            .setNegativeButton("Tidak", null)
            .show()
    }

    private fun showDataKategori() {
        val request = object : StringRequest(Request.Method.POST, urlKategori,
            Response.Listener { response ->
                try {
                    daftarKategori.clear()
                    val jsonArray = JSONArray(response)

                    for (x in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(x)
                        val hm = HashMap<String, String>()
                        hm["id"] = jsonObject.getString("id")
                        hm["nama_kategori"] = jsonObject.getString("nama_kategori")
                        daftarKategori.add(hm)
                    }

                    // Setup Adapter ListView sesuai data HashMap Volley
                    val adapter = SimpleAdapter(
                        thisParent, daftarKategori, R.layout.item_data_kategori,
                        arrayOf("id", "nama_kategori"),
                        intArrayOf(R.id.txIdKategori, R.id.txNamaKategori)
                    )
                    binding.lsKategori.adapter = adapter
                } catch (e: Exception) {
                    //Toast.makeText(thisParent, "Parsing Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Parsing Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            Response.ErrorListener { error ->
                Toast.makeText(thisParent, "Gagal memuat data dari MySQL server", Toast.LENGTH_SHORT).show()
            }) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["mode"] = "show"
                return params
            }
        }
        Volley.newRequestQueue(thisParent).add(request)
    }

    private fun insertData() {
        val nama = binding.edNamaKategori.text.toString().trim()
        if (nama.isNotEmpty()) {
            val request = object : StringRequest(Request.Method.POST, urlKategori,
                Response.Listener { response ->
                    val jsonObject = JSONObject(response)
                    val kode = jsonObject.getString("kode")
                    if (kode == "000") {
                        Toast.makeText(thisParent, "Kategori berhasil disimpan", Toast.LENGTH_SHORT).show()
                        clearForm()
                    } else if (kode == "111") {
                        Toast.makeText(thisParent, "Kategori '$nama' sudah ada!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(thisParent, "Operasi Gagal!", Toast.LENGTH_SHORT).show()
                    }
                },
                Response.ErrorListener { error -> Toast.makeText(thisParent, "Koneksi Bermasalah", Toast.LENGTH_SHORT).show() }) {
                override fun getParams(): MutableMap<String, String> {
                    val params = HashMap<String, String>()
                    params["mode"] = "insert"
                    params["nama_kategori"] = nama
                    return params
                }
            }
            Volley.newRequestQueue(thisParent).add(request)
        } else {
            Toast.makeText(thisParent, "Nama kategori tidak boleh kosong!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateData() {
        val nama = binding.edNamaKategori.text.toString().trim()
        if (selectedId.isNotEmpty() && nama.isNotEmpty()) {
            val request = object : StringRequest(Request.Method.POST, urlKategori,
                Response.Listener { response ->
                    val jsonObject = JSONObject(response)
                    val kode = jsonObject.getString("kode")
                    if (kode == "000") {
                        Toast.makeText(thisParent, "Data diperbarui", Toast.LENGTH_SHORT).show()
                        clearForm()
                    } else if (kode == "111") {
                        Toast.makeText(thisParent, "Nama kategori '$nama' sudah digunakan!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(thisParent, "Gagal memperbarui data", Toast.LENGTH_SHORT).show()
                    }
                },
                Response.ErrorListener { error -> Toast.makeText(thisParent, "Koneksi Bermasalah", Toast.LENGTH_SHORT).show() }) {
                override fun getParams(): MutableMap<String, String> {
                    val params = HashMap<String, String>()
                    params["mode"] = "update"
                    params["id"] = selectedId
                    params["nama_kategori"] = nama
                    return params
                }
            }
            Volley.newRequestQueue(thisParent).add(request)
        }
    }

    private fun deleteData() {
        if (selectedId.isNotEmpty()) {
            val request = object : StringRequest(Request.Method.POST, urlKategori,
                Response.Listener { response ->
                    val jsonObject = JSONObject(response)
                    if (jsonObject.getString("kode") == "000") {
                        Toast.makeText(thisParent, "Data berhasil dihapus", Toast.LENGTH_SHORT).show()
                        clearForm()
                    }
                },
                Response.ErrorListener { error -> Toast.makeText(thisParent, "Koneksi Bermasalah", Toast.LENGTH_SHORT).show() }) {
                override fun getParams(): MutableMap<String, String> {
                    val params = HashMap<String, String>()
                    params["mode"] = "delete"
                    params["id"] = selectedId
                    return params
                }
            }
            Volley.newRequestQueue(thisParent).add(request)
        }
    }

    private fun clearForm() {
        binding.edNamaKategori.setText("")
        selectedId = ""
        binding.btnInsert.isEnabled = true
        binding.btnInsert.alpha = 1.0f
        showDataKategori()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        vb = null
    }
}