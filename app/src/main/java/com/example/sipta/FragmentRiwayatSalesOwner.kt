package com.example.sipta

import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.sipta.databinding.ActivityFragmentRiwayatSalesOwnerBinding
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.view.*
import android.widget.*
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import android.app.TimePickerDialog

class FragmentRiwayatSalesOwner : Fragment() {

    private var _binding: ActivityFragmentRiwayatSalesOwnerBinding? = null
    private val binding get() = _binding!!

    // URL Web Service Laragon (Sesuaikan IP Laptop Server kelompokmu)
    private val urlRiwayat = "http://192.168.1.127/sipta_api/crud_riwayat_sales.php"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityFragmentRiwayatSalesOwnerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadData()

        binding.btnTambah.setOnClickListener {
            tambahData()
        }
    }

    override fun onStart() {
        super.onStart()
        loadData()
    }

    private fun loadData() {
        val request = object : StringRequest(Request.Method.POST, urlRiwayat,
            Response.Listener { response ->
                if (isAdded && activity != null) {
                    try {
                        binding.containerRiwayat.removeAllViews()
                        val jsonArray = JSONArray(response)

                        for (x in 0 until jsonArray.length()) {
                            val jsonObject = jsonArray.getJSONObject(x)
                            val id = jsonObject.getInt("id")
                            val nama = jsonObject.getString("nama_sales")
                            val status = jsonObject.getString("status")
                            val tanggal = jsonObject.optString("tanggal_kunjungan", "")

                            val statusFix = status.trim().lowercase()

                            // Render baris data secara dinamis ke dalam LinearLayout container
                            val row = LinearLayout(requireContext()).apply {
                                orientation = LinearLayout.HORIZONTAL
                                setBackgroundResource(android.R.drawable.dialog_holo_light_frame)
                                setPadding(24, 20, 24, 20)
                                elevation = 4f
                                gravity = Gravity.CENTER_VERTICAL
                                layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                ).apply { setMargins(0, 0, 0, 12) }
                            }

                            val tvNama = TextView(requireContext()).apply {
                                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.3f)
                                text = nama
                                textSize = 14f
                            }

                            val tvStatus = TextView(requireContext()).apply {
                                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                                text = status
                                when (statusFix) {
                                    "sudah datang" -> setTextColor(android.graphics.Color.parseColor("#2E7D32"))
//                                    "proses" -> setTextColor(android.graphics.Color.parseColor("#F9A825"))
                                    "belum datang" -> setTextColor(android.graphics.Color.parseColor("#C62828"))
                                }
                            }

                            val tvTanggal = TextView(requireContext()).apply {
                                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.2f)
                                text = if (tanggal.isEmpty() || tanggal == "null") "--belum dicatat--" else tanggal
                            }

                            val aksiLayout = LinearLayout(requireContext()).apply {
                                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
                                orientation = LinearLayout.HORIZONTAL
                                gravity = Gravity.END
                            }

                            fun createIcon(resId: Int, onClick: () -> Unit): ImageView {
                                val img = ImageView(requireContext())
                                val size = (36 * resources.displayMetrics.density).toInt()
                                img.layoutParams = LinearLayout.LayoutParams(size, size).apply { setMargins(4, 0, 4, 0) }
                                img.setImageResource(resId)
                                img.scaleType = ImageView.ScaleType.FIT_CENTER
                                img.setOnClickListener { onClick() }
                                return img
                            }

                            aksiLayout.addView(createIcon(R.drawable.edit) { editData(id) })
                            aksiLayout.addView(createIcon(R.drawable.hapus) { hapusData(id) })

                            if (statusFix.contains("sudah datang")) {
                                aksiLayout.addView(createIcon(R.drawable.detail) { bukaDetail(id) })
                            }

                            row.addView(tvNama)
                            row.addView(tvStatus)
                            row.addView(tvTanggal)
                            row.addView(aksiLayout)

                            binding.containerRiwayat.addView(row)
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
            },
            Response.ErrorListener { if (isAdded) Toast.makeText(requireContext(), "Gagal memuat riwayat", Toast.LENGTH_SHORT).show() }) {
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf("mode" to "show")
            }
        }
        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun tambahData() {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 16, 32, 16)
        }

        val spinnerSales = Spinner(requireContext())
        val salesList = ArrayList<String>()
        val salesIdList = ArrayList<Int>()

        // =========================================================================
        // PERBAIKAN 1: Menggabungkan DatePicker dan TimePicker secara berantai (Berurutan)
        // =========================================================================
        val etTanggal = EditText(requireContext()).apply {
            hint = "Tanggal & Waktu (opsional)"
            isFocusable = false
            setOnClickListener {
                val c = Calendar.getInstance()

                // 1. Munculkan Pemilih Tanggal
                DatePickerDialog(requireContext(), { _, y, m, d ->
                    val tanggalTerpilih = String.format("%04d-%02d-%02d", y, m + 1, d)

                    // 2. Otomatis memicu Pemilih Jam setelah tanggal ditekan
                    TimePickerDialog(requireContext(), { _, hour, minute ->

                        // Gabungkan menjadi format standar DATETIME: YYYY-MM-DD HH:mm:ss
                        val waktuLengkap = String.format("%s %02d:%02d:00", tanggalTerpilih, hour, minute)
                        setText(waktuLengkap)

                    }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()

                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
            }
        }

        val spinnerStatus = Spinner(requireContext())
        val statusList = listOf("belum datang", "sudah datang")
        spinnerStatus.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, statusList)

        layout.addView(spinnerSales)
        layout.addView(spinnerStatus)
        layout.addView(etTanggal)
        layout.addView(TextView(requireContext()).apply {
            text = "*Jika status belum datang, tanggal boleh kosong"
            textSize = 12f
        })

        // Muat daftar sales dari MySQL untuk disuntikkan ke spinner
        val reqSales = object : StringRequest(Request.Method.POST, urlRiwayat,
            Response.Listener { response ->
                try {
                    val array = JSONArray(response)
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        salesIdList.add(obj.getInt("id"))
                        salesList.add(obj.getString("nama_sales"))
                    }
                    spinnerSales.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, salesList)
                } catch (e: Exception) { e.printStackTrace() }
            }, Response.ErrorListener {}) {
            override fun getParams(): MutableMap<String, String> = hashMapOf("mode" to "get_sales_spinner")
        }
        Volley.newRequestQueue(requireContext()).add(reqSales)

        AlertDialog.Builder(requireContext())
            .setTitle("Tambah Jadwal")
            .setView(layout)
            .setPositiveButton("Simpan") { _, _ ->
                if (spinnerSales.selectedItem == null) return@setPositiveButton
                val salesId = salesIdList[spinnerSales.selectedItemPosition]
                val status = spinnerStatus.selectedItem.toString()
                val tanggalInput = etTanggal.text.toString()

                if ((status == "sudah datang") && tanggalInput.isEmpty()) {
                    Toast.makeText(requireContext(), "Tanggal wajib diisi untuk status ini!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val tanggalFix = if (status == "belum datang") "" else tanggalInput

                // =========================================================================
                // PERBAIKAN 2: Validasi Respon JSON Server Pusat (Bukan asal memunculkan Toast)
                // =========================================================================
                val reqInsert = object : StringRequest(Request.Method.POST, urlRiwayat,
                    Response.Listener { response ->
                        try {
                            val jsonRes = JSONObject(response)
                            if (jsonRes.getString("kode") == "000") {
                                Toast.makeText(requireContext(), "Jadwal kunjungan berhasil disimpan", Toast.LENGTH_SHORT).show()
                                loadData() // Refresh list LinearLayout
                            } else {
                                Toast.makeText(requireContext(), "Gagal menyimpan ke database server pusat", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) { e.printStackTrace() }
                    },
                    Response.ErrorListener {
                        Toast.makeText(requireContext(), "Gangguan koneksi internet ke Laragon", Toast.LENGTH_SHORT).show()
                    }) {
                    override fun getParams(): MutableMap<String, String> = hashMapOf(
                        "mode" to "insert",
                        "sales_id" to salesId.toString(),
                        "status" to status,
                        "tanggal_kunjungan" to tanggalFix
                    )
                }
                Volley.newRequestQueue(requireContext()).add(reqInsert)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun editData(id: Int) {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 16, 32, 16)
        }

        val spinnerSales = Spinner(requireContext())
        val salesList = ArrayList<String>()
        val salesIdList = ArrayList<Int>()

        val spinnerStatus = Spinner(requireContext())
        val statusList = listOf("belum datang", "sudah datang")
        spinnerStatus.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, statusList)

        // =========================================================================
        // PERBAIKAN 1: Gabungkan DatePicker dan TimePicker Berantai di Mode Edit
        // =========================================================================
        val etTanggal = EditText(requireContext()).apply {
            hint = "Tanggal & Waktu (opsional)"
            isFocusable = false
            setOnClickListener {
                val c = Calendar.getInstance()

                // 1. Munculkan Pemilih Tanggal
                DatePickerDialog(requireContext(), { _, y, m, d ->
                    val tanggalTerpilih = String.format("%04d-%02d-%02d", y, m + 1, d)

                    // 2. Otomatis munculkan Pemilih Jam setelah tanggal dipilih
                    TimePickerDialog(requireContext(), { _, hour, minute ->

                        // Gabungkan menjadi format DATETIME: YYYY-MM-DD HH:mm:ss
                        val waktuLengkap = String.format("%s %02d:%02d:00", tanggalTerpilih, hour, minute)
                        setText(waktuLengkap)

                    }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()

                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
            }
        }

        val note = TextView(requireContext()).apply {
            text = "*Jika status belum datang, tanggal boleh kosong"
            textSize = 12f
        }

        layout.addView(TextView(requireContext()).apply { text = "Pilih Sales" })
        spinnerSales.isEnabled = false
        spinnerSales.alpha = 0.6f
        layout.addView(spinnerSales)

        layout.addView(TextView(requireContext()).apply { text = "Status" })
        layout.addView(spinnerStatus)

        layout.addView(TextView(requireContext()).apply { text = "Tanggal" })
        layout.addView(etTanggal)

        layout.addView(note)

        // Tarik data sales dan data riwayat target secara berurutan
        val reqSales = object : StringRequest(Request.Method.POST, urlRiwayat,
            Response.Listener { response ->
                try {
                    val array = JSONArray(response)
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        salesIdList.add(obj.getInt("id"))
                        salesList.add(obj.getString("nama_sales"))
                    }
                    spinnerSales.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, salesList)

                    // Setelah spinner sales siap, ambil data riwayat lama untuk di-set ke form
                    val reqOne = object : StringRequest(Request.Method.POST, urlRiwayat,
                        Response.Listener { resOne ->
                            val objRiwayat = JSONObject(resOne)
                            val sId = objRiwayat.getInt("sales_id")
                            val stat = objRiwayat.getString("status")
                            val tgl = objRiwayat.optString("tanggal_kunjungan", "")

                            val indexSales = salesIdList.indexOf(sId)
                            if (indexSales >= 0) spinnerSales.setSelection(indexSales)

                            val indexStatus = statusList.indexOf(stat)
                            if (indexStatus >= 0) spinnerStatus.setSelection(indexStatus)

                            etTanggal.setText(if(tgl == "null" || tgl.isEmpty()) "" else tgl)
                        }, Response.ErrorListener {}) {
                        override fun getParams(): MutableMap<String, String> = hashMapOf("mode" to "get_one", "id" to id.toString())
                    }
                    Volley.newRequestQueue(requireContext()).add(reqOne)

                } catch (e: Exception) { e.printStackTrace() }
            }, Response.ErrorListener {}) {
            override fun getParams(): MutableMap<String, String> = hashMapOf("mode" to "get_sales_spinner")
        }
        Volley.newRequestQueue(requireContext()).add(reqSales)

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Jadwal Kunjungan")
            .setView(layout)
            .setPositiveButton("Simpan") { _, _ ->
                val salesId = salesIdList[spinnerSales.selectedItemPosition]
                val status = spinnerStatus.selectedItem.toString()
                val tanggalInput = etTanggal.text.toString()

                if ((status == "sudah datang") && tanggalInput.isEmpty()) {
                    Toast.makeText(requireContext(), "Tanggal wajib diisi!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val tanggalFix = if (status == "belum datang") "" else tanggalInput

                // =========================================================================
                // PERBAIKAN 2: Validasi Balasan JSON Object Sukses dari PHP Backend ("kode":"000")
                // =========================================================================
                val reqUpdate = object : StringRequest(Request.Method.POST, urlRiwayat,
                    Response.Listener { response ->
                        try {
                            val jsonRes = JSONObject(response)
                            if (jsonRes.getString("kode") == "000") {
                                Toast.makeText(requireContext(), "Data berhasil diupdate", Toast.LENGTH_SHORT).show()
                                loadData() // Refresh list agar jam barunya langsung kelihatan
                            } else {
                                Toast.makeText(requireContext(), "Gagal memperbarui data di database Laragon", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) { e.printStackTrace() }
                    },
                    Response.ErrorListener {
                        Toast.makeText(requireContext(), "Gagal terhubung ke jaringan server", Toast.LENGTH_SHORT).show()
                    }) {
                    override fun getParams(): MutableMap<String, String> = hashMapOf(
                        "mode" to "update",
                        "id" to id.toString(),
                        "sales_id" to salesId.toString(),
                        "status" to status,
                        "tanggal_kunjungan" to tanggalFix
                    )
                }
                Volley.newRequestQueue(requireContext()).add(reqUpdate)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun hapusData(id: Int) {
        val request = object : StringRequest(Request.Method.POST, urlRiwayat,
            Response.Listener { loadData() }, Response.ErrorListener {}) {
            override fun getParams(): MutableMap<String, String> = hashMapOf("mode" to "delete", "id" to id.toString())
        }
        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun bukaDetail(id: Int) {
        val fragment = FragmentDetailRiwayatSales()
        val bundle = Bundle()
        bundle.putInt("id_riwayat", id)
        fragment.arguments = bundle

        parentFragmentManager.beginTransaction()
            .replace(R.id.frameOwner, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}