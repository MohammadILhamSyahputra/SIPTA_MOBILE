package com.example.sipta

import android.app.Activity
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.graphics.Color
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import androidx.fragment.app.Fragment
import com.example.sipta.databinding.ActivityFragmentDashboardAdminBinding

class FragmentDashboardAdmin : Fragment() {
    private var _binding: ActivityFragmentDashboardAdminBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: SQLiteDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityFragmentDashboardAdminBinding.inflate(inflater, container, false)

        // 1. Ambil objek database dari MainActivityAdmin
        val parentActivity = activity as MainActivityAdmin
        db = parentActivity.getDbObject()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 2. Tampilkan data statistik saat fragment dimuat
        updateDashboardStats()
        tampilkanGrafikStok()
    }

    private fun updateDashboardStats() {
        // Query untuk menghitung jumlah barang
        val countBarang = getCount("SELECT COUNT(*) FROM barang")
        binding.tvCountBarang.text = countBarang.toString()

        // Query untuk menghitung jumlah kategori
        val countKategori = getCount("SELECT COUNT(*) FROM kategori")
        binding.tvCountKategori.text = countKategori.toString()

        // Query untuk menghitung jumlah sales
        val countSales = getCount("SELECT COUNT(*) FROM sales")
        binding.tvCountSales.text = countSales.toString()

        // Query untuk menghitung total stok seluruh barang
        val totalStok = getCount("SELECT SUM(stok) FROM barang")
        binding.tvTotalUnit.text = "$totalStok Unit"
    }

    // Fungsi helper untuk menjalankan query count sederhana
    private fun getCount(sql: String): Int {
        val cursor: Cursor = db.rawQuery(sql, null)
        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
        return count
    }

    private fun tampilkanGrafikStok() {
        val pieChart = binding.pieChart // Sesuaikan dengan ID di XML
        val listEntri = ArrayList<PieEntry>()

        // 1. Ambil data stok per nama barang dari DB
        val cursor = db.rawQuery("SELECT nama, stok FROM barang", null)
        if (cursor.count == 0) {
            pieChart.setNoDataText("Belum ada data barang untuk ditampilkan")
            pieChart.invalidate()
            cursor.close()
            return
        }
        if (cursor.moveToFirst()) {
            do {
                val nama = cursor.getString(0)
                val stok = cursor.getFloat(1)
                listEntri.add(PieEntry(stok, nama))
            } while (cursor.moveToNext())
        }
        cursor.close()

        // 2. Atur warna dan tampilan grafik
        val dataSet = PieDataSet(listEntri, "Distribusi Stok")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTextSize = 12f

        val data = PieData(dataSet)
        pieChart.data = data
        pieChart.centerText = "Stok Barang"
        pieChart.description.isEnabled = false
        pieChart.animateY(1000) // Animasi putar
        pieChart.invalidate() // Refresh grafik
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Membersihkan binding untuk mencegah kebocoran memori
    }
}