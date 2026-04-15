package com.example.sipta

import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.sipta.databinding.ActivityFragmentRiwayatSalesOwnerBinding

class FragmentRiwayatSalesOwner : Fragment() {

    private var _binding: ActivityFragmentRiwayatSalesOwnerBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: SQLiteDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityFragmentRiwayatSalesOwnerBinding.inflate(inflater, container, false)

        val parentActivity = requireActivity() as MainActivityOwner
        db = parentActivity.getDbObject()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        loadData()

        binding.btnTambah.setOnClickListener {
            tambahData()
        }
    }

    private fun loadData() {
        val cursor = db.rawQuery("""
            SELECT riwayat_sales.id, sales.nama_sales, riwayat_sales.status, riwayat_sales.tanggal_kunjungan
            FROM riwayat_sales
            JOIN sales ON riwayat_sales.sales_id = sales.id
            ORDER BY riwayat_sales.id DESC
        """.trimIndent(), null)

        binding.containerRiwayat.removeAllViews()

        while (cursor.moveToNext()) {
            val id = cursor.getInt(0)
            val nama = cursor.getString(1)
            val status = cursor.getString(2)
            val tanggal = cursor.getString(3)

            val statusFix = status.trim().lowercase()

            val row = LinearLayout(requireContext())
            row.orientation = LinearLayout.HORIZONTAL
            row.setBackgroundResource(android.R.drawable.dialog_holo_light_frame)
            row.setPadding(24, 20, 24, 20)
            row.elevation = 4f
            row.gravity = Gravity.CENTER_VERTICAL

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 12)
            row.layoutParams = params

            val tvNama = TextView(requireContext())
            tvNama.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.3f)
            tvNama.text = nama
            tvNama.textSize = 14f

            val tvStatus = TextView(requireContext())
            tvStatus.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            tvStatus.text = status

            when (statusFix) {
                "sudah datang" -> tvStatus.setTextColor(android.graphics.Color.parseColor("#2E7D32"))
                "proses" -> tvStatus.setTextColor(android.graphics.Color.parseColor("#F9A825"))
                "belum datang" -> tvStatus.setTextColor(android.graphics.Color.parseColor("#C62828"))
            }

            val tvTanggal = TextView(requireContext())
            tvTanggal.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.2f)
            tvTanggal.text = if (tanggal.isNullOrEmpty()) "--belum dicatat--" else tanggal

            val aksiLayout = LinearLayout(requireContext())
            aksiLayout.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
            aksiLayout.orientation = LinearLayout.HORIZONTAL
            aksiLayout.gravity = Gravity.END

            fun createIcon(resId: Int, onClick: () -> Unit): ImageView {
                val img = ImageView(requireContext())
                val size = (36 * resources.displayMetrics.density).toInt()
                val lp = LinearLayout.LayoutParams(size, size)
                lp.setMargins(4, 0, 4, 0)
                img.layoutParams = lp
                img.setImageResource(resId)
                img.scaleType = ImageView.ScaleType.FIT_CENTER
                img.setOnClickListener { onClick() }
                return img
            }

            val btnEdit = createIcon(R.drawable.edit) {
                editData(id)
            }

            val btnHapus = createIcon(R.drawable.hapus) {
                hapusData(id)
            }

            aksiLayout.addView(btnEdit)
            aksiLayout.addView(btnHapus)

            if (statusFix.contains("sudah datang")) {
                val btnDetail = createIcon(R.drawable.detail) {
                    bukaDetail(id)
                }
                aksiLayout.addView(btnDetail)
            }

            row.addView(tvNama)
            row.addView(tvStatus)
            row.addView(tvTanggal)
            row.addView(aksiLayout)

            binding.containerRiwayat.addView(row)
        }

        cursor.close()
    }

    private fun tambahData() {
        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(32, 16, 32, 16)

        val spinnerSales = Spinner(requireContext())
        val salesList = ArrayList<String>()
        val salesIdList = ArrayList<Int>()

        val cursorSales = db.rawQuery("SELECT id, nama_sales FROM sales", null)
        while (cursorSales.moveToNext()) {
            salesIdList.add(cursorSales.getInt(0))
            salesList.add(cursorSales.getString(1))
        }
        cursorSales.close()

        spinnerSales.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, salesList)

        val etTanggal = EditText(requireContext())
        etTanggal.hint = "Tanggal (opsional)"
        etTanggal.isFocusable = false

        etTanggal.setOnClickListener {
            val c = java.util.Calendar.getInstance()
            android.app.DatePickerDialog(requireContext(),
                { _, y, m, d ->
                    etTanggal.setText(String.format("%04d-%02d-%02d", y, m+1, d))
                },
                c.get(java.util.Calendar.YEAR),
                c.get(java.util.Calendar.MONTH),
                c.get(java.util.Calendar.DAY_OF_MONTH)
            ).show()
        }

        val spinnerStatus = Spinner(requireContext())
        val statusList = listOf("belum datang", "proses", "sudah datang")
        spinnerStatus.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, statusList)

        val note = TextView(requireContext())
        note.text = "*Jika status belum datang, tanggal boleh kosong"
        note.textSize = 12f

        layout.addView(spinnerSales)
        layout.addView(etTanggal)
        layout.addView(spinnerStatus)
        layout.addView(note)

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Tambah Jadwal")
            .setView(layout)
            .setPositiveButton("Simpan") { _, _ ->

                val salesId = salesIdList[spinnerSales.selectedItemPosition]
                val status = spinnerStatus.selectedItem.toString()
                val tanggalInput = etTanggal.text.toString()

                // VALIDASI
                if ((status == "proses" || status == "sudah datang") && tanggalInput.isEmpty()) {
                    Toast.makeText(requireContext(), "Tanggal wajib diisi untuk status ini!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val tanggalFix = if (status == "belum datang") "" else tanggalInput

                db.execSQL("""
                        INSERT INTO riwayat_sales 
                        (sales_id, tanggal_kunjungan, status, created_at) 
                        VALUES (?, ?, ?, datetime('now'))
                    """.trimIndent(),
                    arrayOf(
                        salesId.toString(),
                        tanggalFix,
                        status
                    )
                )

                loadData()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun editData(id: Int) {
        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(32, 16, 32, 16)
        val spinnerSales = Spinner(requireContext())
        val salesList = ArrayList<String>()
        val salesIdList = ArrayList<Int>()

        val cursorSales = db.rawQuery("SELECT id, nama_sales FROM sales", null)
        while (cursorSales.moveToNext()) {
            salesIdList.add(cursorSales.getInt(0))
            salesList.add(cursorSales.getString(1))
        }
        cursorSales.close()

        val adapterSales = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, salesList)
        spinnerSales.adapter = adapterSales
        val spinnerStatus = Spinner(requireContext())
        val statusList = listOf("belum datang", "proses", "sudah datang")

        val adapterStatus = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, statusList)
        spinnerStatus.adapter = adapterStatus
        val etTanggal = EditText(requireContext())
        etTanggal.hint = "Tanggal (opsional)"
        etTanggal.isFocusable = false

        etTanggal.setOnClickListener {
            val c = java.util.Calendar.getInstance()
            android.app.DatePickerDialog(requireContext(),
                { _, y, m, d ->
                    etTanggal.setText(String.format("%04d-%02d-%02d", y, m + 1, d))
                },
                c.get(java.util.Calendar.YEAR),
                c.get(java.util.Calendar.MONTH),
                c.get(java.util.Calendar.DAY_OF_MONTH)
            ).show()
        }

        val cursor = db.rawQuery(
            "SELECT sales_id, status, tanggal_kunjungan FROM riwayat_sales WHERE id=?",
            arrayOf(id.toString())
        )

        if (cursor.moveToFirst()) {
            val salesId = cursor.getInt(0)
            val status = cursor.getString(1)
            val tanggal = cursor.getString(2)

            // set spinner sales
            val indexSales = salesIdList.indexOf(salesId)
            if (indexSales >= 0) spinnerSales.setSelection(indexSales)

            // set spinner status
            val indexStatus = statusList.indexOf(status)
            if (indexStatus >= 0) spinnerStatus.setSelection(indexStatus)

            etTanggal.setText(tanggal ?: "")
        }
        cursor.close()

        val note = TextView(requireContext())
        note.text = "*Jika status belum datang, tanggal boleh kosong"
        note.textSize = 12f
        layout.addView(TextView(requireContext()).apply { text = "Pilih Sales" })
        layout.addView(spinnerSales)

        layout.addView(TextView(requireContext()).apply { text = "Tanggal" })
        layout.addView(etTanggal)

        layout.addView(TextView(requireContext()).apply { text = "Status" })
        layout.addView(spinnerStatus)

        layout.addView(note)

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Edit Jadwal Kunjungan")
            .setView(layout)
            .setPositiveButton("Simpan") { _, _ ->

                val salesId = salesIdList[spinnerSales.selectedItemPosition]
                val status = spinnerStatus.selectedItem.toString()
                val tanggalInput = etTanggal.text.toString()

                // VALIDASI
                if ((status == "proses" || status == "sudah datang") && tanggalInput.isEmpty()) {
                    Toast.makeText(requireContext(), "Tanggal wajib diisi!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val tanggalFix = if (status == "belum datang") "" else tanggalInput

                db.execSQL("""
                        UPDATE riwayat_sales 
                        SET sales_id=?, status=?, tanggal_kunjungan=? 
                        WHERE id=?
                    """.trimIndent(),
                    arrayOf(
                        salesId.toString(),
                        status,
                        tanggalFix,
                        id.toString()
                    )
                )

                Toast.makeText(requireContext(), "Data berhasil diupdate", Toast.LENGTH_SHORT).show()
                loadData()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun hapusData(id: Int) {
        db.execSQL("DELETE FROM riwayat_sales WHERE id=?", arrayOf(id.toString()))
        loadData()
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