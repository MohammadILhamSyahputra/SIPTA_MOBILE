package com.example.sipta

import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.sipta.databinding.ActivityFragmentDetailRiwayatSalesBinding

class FragmentDetailRiwayatSales : Fragment() {

    private var _binding: ActivityFragmentDetailRiwayatSalesBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: SQLiteDatabase
    private var idRiwayat: Int = 0
    private var salesId: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityFragmentDetailRiwayatSalesBinding.inflate(inflater, container, false)

        val parentActivity = requireActivity() as MainActivityOwner
        db = parentActivity.getDbObject()

        idRiwayat = arguments?.getInt("id_riwayat") ?: 0

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        loadData()
        binding.btnTambahBarang.setOnClickListener {
            simpanBarang()
        }
        val c = db.rawQuery(
            "SELECT status FROM riwayat_sales WHERE id=?",
            arrayOf(idRiwayat.toString())
        )
        if (c.moveToFirst()) {
            val status = c.getString(0)
            if (status != "sudah datang") {
                binding.btnTambahBarang.isEnabled = false
                binding.btnTambahBarang.alpha = 0.5f
            }
        }
        c.close()
    }

    private fun loadData() {
        val cInfo = db.rawQuery("""
            SELECT sales.id, sales.nama_sales, riwayat_sales.status, 
                   riwayat_sales.tanggal_kunjungan, riwayat_sales.created_at
            FROM riwayat_sales
            JOIN sales ON riwayat_sales.sales_id = sales.id
            WHERE riwayat_sales.id=?
        """.trimIndent(), arrayOf(idRiwayat.toString()))

        if (cInfo.moveToFirst()) {
            salesId = cInfo.getInt(0)
            binding.tvNamaSales.text = "Sales: " + cInfo.getString(1)
            binding.tvStatus.text = "Status: " + cInfo.getString(2)
            binding.tvTanggal.text = "Tanggal Kunjungan: " + formatTanggalWaktu(cInfo.getString(3))
            binding.tvCreated.text = "Dibuat Pada: " + formatTanggalWaktu(cInfo.getString(4))
        }
        cInfo.close()

        val cursor = db.rawQuery("""
            SELECT detail_riwayat_sales.id, barang.nama, qty_masuk, qty_retur
            FROM detail_riwayat_sales
            JOIN barang ON detail_riwayat_sales.barang_id = barang.id
            WHERE riwayat_sales_id=?
        """.trimIndent(), arrayOf(idRiwayat.toString()))

        binding.containerBarang.removeAllViews()

        while (cursor.moveToNext()) {
            val idDetail = cursor.getInt(0)
            val nama = cursor.getString(1)
            val masuk = cursor.getInt(2)
            val retur = cursor.getInt(3)

            val row = LinearLayout(requireContext())
            row.orientation = LinearLayout.HORIZONTAL
            row.setPadding(8, 12, 8, 12)

            val pNama = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.5f)
            val pMasuk = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            val pRetur = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            val pAksi = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.8f)

            val tvNama = TextView(requireContext())
            tvNama.layoutParams = pNama
            tvNama.text = nama

            val tvMasuk = TextView(requireContext())
            tvMasuk.layoutParams = pMasuk
            tvMasuk.text = masuk.toString()
            tvMasuk.gravity = Gravity.CENTER

            val tvRetur = TextView(requireContext())
            tvRetur.layoutParams = pRetur
            tvRetur.text = retur.toString()
            tvRetur.gravity = Gravity.CENTER

            val btnEdit = ImageView(requireContext())
            val size = (28 * resources.displayMetrics.density).toInt()
            btnEdit.layoutParams = LinearLayout.LayoutParams(size, size)
            btnEdit.setImageResource(R.drawable.edit)

            btnEdit.setOnClickListener {
                editBarang(idDetail)
            }

            val aksi = LinearLayout(requireContext())
            aksi.layoutParams = pAksi
            aksi.gravity = Gravity.CENTER
            aksi.addView(btnEdit)

            row.addView(tvNama)
            row.addView(tvMasuk)
            row.addView(tvRetur)
            row.addView(aksi)

            // garis bawah biar rapi
            val line = View(requireContext())
            line.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1
            )
            line.setBackgroundColor(0xFFDDDDDD.toInt())

            binding.containerBarang.addView(row)
            binding.containerBarang.addView(line)
        }

        cursor.close()
    }

    private fun simpanBarang() {
        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(32,16,32,16)

        val spinner = Spinner(requireContext())
        val etMasuk = EditText(requireContext())
        val etRetur = EditText(requireContext())

        etMasuk.hint = "Qty Masuk"
        etRetur.hint = "Qty Retur"

        // ambil barang sesuai sales
        val listBarang = ArrayList<String>()
        val listId = ArrayList<Int>()

        val c = db.rawQuery("""
        SELECT id, nama FROM barang WHERE id_sales=?
    """, arrayOf(salesId.toString()))

        while (c.moveToNext()) {
            listId.add(c.getInt(0))
            listBarang.add(c.getString(1))
        }
        c.close()

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, listBarang)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        layout.addView(spinner)
        layout.addView(etMasuk)
        layout.addView(etRetur)

        AlertDialog.Builder(requireContext())
            .setTitle("Tambah Barang")
            .setView(layout)
            .setPositiveButton("Simpan") { _, _ ->

                val posisi = spinner.selectedItemPosition
                val idBarang = listId[posisi]

                db.execSQL("""
                INSERT INTO detail_riwayat_sales 
                (riwayat_sales_id, barang_id, qty_masuk, qty_retur)
                VALUES (?, ?, ?, ?)
            """, arrayOf(
                    idRiwayat.toString(),
                    idBarang.toString(),
                    etMasuk.text.toString(),
                    etRetur.text.toString()
                ))

                loadData()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun editBarang(idDetail: Int) {
        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(32,16,32,16)

        val etMasuk = EditText(requireContext())
        val etRetur = EditText(requireContext())

        etMasuk.hint = "Qty Masuk"
        etRetur.hint = "Qty Retur"

        // ambil data lama
        val c = db.rawQuery("""
        SELECT qty_masuk, qty_retur 
        FROM detail_riwayat_sales WHERE id=?
    """, arrayOf(idDetail.toString()))

        if (c.moveToFirst()) {
            etMasuk.setText(c.getInt(0).toString())
            etRetur.setText(c.getInt(1).toString())
        }
        c.close()

        layout.addView(etMasuk)
        layout.addView(etRetur)

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Barang")
            .setView(layout)
            .setPositiveButton("Update") { _, _ ->

                db.execSQL("""
                UPDATE detail_riwayat_sales 
                SET qty_masuk=?, qty_retur=? 
                WHERE id=?
            """, arrayOf(
                    etMasuk.text.toString(),
                    etRetur.text.toString(),
                    idDetail.toString()
                ))

                loadData()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun formatTanggalWaktu(tanggal: String?): String {
        if (tanggal.isNullOrEmpty()) return "-"

        return try {
            if (tanggal.matches(Regex("\\d+"))) {
                val millis = tanggal.toLong()
                val sdf = java.text.SimpleDateFormat("dd MMMM yyyy, HH:mm", java.util.Locale("id","ID"))
                sdf.format(java.util.Date(millis))
            } else {
                val input = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                val output = java.text.SimpleDateFormat("dd MMM yyyy HH:mm", java.util.Locale("id", "ID"))
                val date = input.parse(tanggal)
                output.format(date!!)
            }
        } catch (e: Exception) {
            tanggal
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}