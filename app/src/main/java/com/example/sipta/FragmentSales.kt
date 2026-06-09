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
import android.view.*
import android.widget.*
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject

class FragmentSales : Fragment(), View.OnClickListener {
    private var vb: ActivityFragmentSalesBinding? = null
    private val binding get() = vb!!
    private var selectedId: String = ""

    // URL Menembak Server Laragon (Sesuaikan dengan IP Laptop Server-mu)
    private val urlSales = "http://10.146.68.249/sipta_api/crud_sales.php"

    // Menyimpan list data sementara dari MySQL server
    private var daftarSales = mutableListOf<HashMap<String, String>>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        vb = ActivityFragmentSalesBinding.inflate(inflater, container, false)

        binding.btnInsertSales.setOnClickListener(this)
        binding.btnUpdateSales.setOnClickListener(this)
        binding.btnDeleteSales.setOnClickListener(this)

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

                binding.btnInsertSales.isEnabled = false
                binding.btnInsertSales.alpha = 0.5f
            }
        }

        return binding.root
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
                        daftarSales.add(hm)
                    }

                    // Mapping data dari HashMap Volley ke item komponen ListView
                    val adapter = SimpleAdapter(
                        requireContext(), daftarSales, R.layout.item_data_sales,
                        arrayOf("nama_sales", "no_telp", "alamat"),
                        intArrayOf(R.id.txNamaSales, R.id.txNoTelp, R.id.txAlamat)
                    )
                    binding.lsSales.adapter = adapter
                } catch (e: Exception) {
                    //Toast.makeText(requireContext(), "Parsing Error: ${e.message}", Toast.LENGTH_SHORT).show()
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
        showDataSales()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        vb = null
    }
}