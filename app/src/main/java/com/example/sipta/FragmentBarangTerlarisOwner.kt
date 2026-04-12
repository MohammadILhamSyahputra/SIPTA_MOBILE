package com.example.sipta

import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.sipta.databinding.ActivityFragmentBarangTerlarisOwnerBinding
import android.app.DatePickerDialog
import android.widget.TableRow
import android.widget.Toast
import java.util.*

class FragmentBarangTerlarisOwner : Fragment() {

    private var _binding: ActivityFragmentBarangTerlarisOwnerBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: SQLiteDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityFragmentBarangTerlarisOwnerBinding.inflate(inflater, container, false)
        val parentActivity = requireActivity() as MainActivityOwner
        db = parentActivity.getDbObject()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Set Klik untuk DatePicker
        binding.etTanggalMulai.setOnClickListener { showDatePicker(binding.etTanggalMulai) }
        binding.etTanggalAkhir.setOnClickListener { showDatePicker(binding.etTanggalAkhir) }

        // 2. Set Klik Tombol Tampilkan
        binding.btnTampilkan.setOnClickListener {
            val tglMulai = binding.etTanggalMulai.text.toString()
            val tglAkhir = binding.etTanggalAkhir.text.toString()

            if (tglMulai.isNotEmpty() && tglAkhir.isNotEmpty()) {
                loadDataFiltered(tglMulai, tglAkhir)
            } else {
                Toast.makeText(context, "Pilih rentang tanggal dulu!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDatePicker(editText: android.widget.EditText) {
        val c = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, year, month, day ->
            // Format disamakan dengan hint di XML: dd/MM/yyyy
            val date = String.format("%02d/%02d/%d", day, month + 1, year)
            editText.setText(date)
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun loadDataFiltered(tglMulai: String, tglAkhir: String) {
        // Hapus data lama di tabel (kecuali header)
        val count = binding.tableBarang.childCount
        if (count > 1) {
            binding.tableBarang.removeViews(1, count - 1)
        }

        // Query JOIN dengan Filter Tanggal dari tabel transaksi
        val sql = """
            SELECT b.kode_barang, b.nama, b.harga_beli, SUM(dt.qty) AS total_qty
            FROM detail_transaksi dt
            JOIN transaksi t ON dt.id_transaksi = t.id
            JOIN barang b ON dt.id_barang = b.id
            WHERE t.tanggal BETWEEN ? AND ?
            GROUP BY b.id
            ORDER BY total_qty DESC
            LIMIT 10
        """.trimIndent()

        val cursor = db.rawQuery(sql, arrayOf(tglMulai, tglAkhir))

        while (cursor.moveToNext()) {
            val row = TableRow(requireContext())
            row.setPadding(8, 8, 8, 8)

            row.addView(createTextView(cursor.getString(0))) // Kode
            row.addView(createTextView(cursor.getString(1))) // Nama
            row.addView(createTextView("Rp ${cursor.getInt(2)}")) // Harga Beli

            val tvQty = createTextView(cursor.getInt(3).toString())
            tvQty.setTextColor(android.graphics.Color.RED) // Samakan dengan XML (merah)
            row.addView(tvQty)

            binding.tableBarang.addView(row)
        }
        cursor.close()
    }

    private fun createTextView(text: String): TextView {
        val tv = TextView(requireContext())
        tv.text = text
        tv.setPadding(8, 8, 8, 8)
        return tv
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}