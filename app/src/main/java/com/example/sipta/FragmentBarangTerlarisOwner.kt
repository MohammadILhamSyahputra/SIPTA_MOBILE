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
 import com.github.mikephil.charting.data.BarData
 import com.github.mikephil.charting.data.BarDataSet
 import com.github.mikephil.charting.data.BarEntry

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

        binding.etTanggalMulai.setOnClickListener { showDatePicker(binding.etTanggalMulai) }
        binding.etTanggalAkhir.setOnClickListener { showDatePicker(binding.etTanggalAkhir) }

        binding.btnTampilkan.setOnClickListener {
            // AMBIL DARI TAG (format yyyy-MM-dd), BUKAN DARI TEXT (dd/MM/yyyy)
            val tglMulai = binding.etTanggalMulai.tag?.toString() ?: ""
            val tglAkhir = binding.etTanggalAkhir.tag?.toString() ?: ""

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
            val bulan = month + 1
            // displayDate untuk dilihat user (16/04/2026)
            val displayDate = String.format("%02d/%02d/%04d", day, bulan, year)
            // dbDate untuk dikirim ke SQL (2026-04-16)
            val dbDate = String.format("%04d-%02d-%02d", year, bulan, day)

            editText.setText(displayDate)
            editText.tag = dbDate // SIMPAN FORMAT DB DI SINI
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun loadDataFiltered(tglMulai: String, tglAkhir: String) {
        // 1. Hapus data lama di tabel (kecuali header)
        val count = binding.tableBarang.childCount
        if (count > 1) {
            binding.tableBarang.removeViews(1, count - 1)
        }

        // 2. Query SQL: Pastikan mengambil semua kolom yang dibutuhkan untuk tabel & chart
        val sql = """
        SELECT b.kode_barang, b.nama, b.harga_beli, SUM(dt.qty) AS total_qty
        FROM detail_transaksi dt
        JOIN transaksi t ON dt.id_transaksi = t.id
        JOIN barang b ON dt.id_barang = b.id
        WHERE date(t.tanggal) BETWEEN date(?) AND date(?)
        GROUP BY b.id
        ORDER BY total_qty DESC
        LIMIT 10
    """.trimIndent()

        val cursor = db.rawQuery(sql, arrayOf(tglMulai, tglAkhir))

        // List untuk menyimpan data grafik
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()
        var indexChart = 0f

        while (cursor.moveToNext()) {
            // Ambil data dari cursor
            val kode = cursor.getString(0)
            val nama = cursor.getString(1)
            val hargaBeli = cursor.getInt(2)
            val qty = cursor.getInt(3)

            // --- BAGIAN TABEL ---
            val row = TableRow(requireContext())
            row.setPadding(8, 8, 8, 8)

            row.addView(createTextView(kode))          // Kolom Kode
            row.addView(createTextView(nama))          // Kolom Nama
            row.addView(createTextView("Rp $hargaBeli")) // Kolom Harga Beli

            val tvQty = createTextView(qty.toString())
            tvQty.setTextColor(android.graphics.Color.RED)
            row.addView(tvQty)                         // Kolom Qty Terjual

            binding.tableBarang.addView(row)

            // --- BAGIAN CHART ---
            entries.add(BarEntry(indexChart, qty.toFloat()))
            labels.add(nama)
            indexChart++
        }

        // 3. Panggil fungsi untuk menggambar Chart
        if (entries.isNotEmpty()) {
            tampilkanChart(entries, labels)
        } else {
            binding.barChart.clear() // Bersihkan chart jika data tidak ditemukan
            Toast.makeText(context, "Tidak ada data pada rentang tanggal ini", Toast.LENGTH_SHORT).show()
        }

        cursor.close()
    }

    private fun tampilkanChart(entries: ArrayList<BarEntry>, labels: ArrayList<String>) {
        val dataSet = BarDataSet(entries, "Total Terjual")
        dataSet.color = android.graphics.Color.parseColor("#03A9F4")
        dataSet.valueTextSize = 12f

        val barData = BarData(dataSet)
        binding.barChart.data = barData

        // Atur Axis X (Label Nama Barang)
        val xAxis = binding.barChart.xAxis
        xAxis.valueFormatter = com.github.mikephil.charting.formatter.IndexAxisValueFormatter(labels)
        xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.labelRotationAngle = -45f // Miringkan label agar tidak tabrakan

        binding.barChart.description.isEnabled = false
        binding.barChart.animateY(1000)
        binding.barChart.invalidate() // Refresh chart
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