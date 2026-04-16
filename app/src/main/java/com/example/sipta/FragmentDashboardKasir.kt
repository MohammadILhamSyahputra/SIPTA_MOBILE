package com.example.sipta

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
import com.example.sipta.databinding.ActivityFragmentDashboardKasirBinding

class FragmentDashboardKasir : Fragment() {
    private var _binding: ActivityFragmentDashboardKasirBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: SQLiteDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityFragmentDashboardKasirBinding.inflate(inflater, container, false)

        val parentActivity = activity as MainActivityKasir
        db = parentActivity.getDbObject()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateDashboardStats()
        tampilkanGrafikStok()
    }

    private fun updateDashboardStats() {
        val countBarang = getCount("SELECT COUNT(*) FROM barang")
        binding.tvCountBarang.text = countBarang.toString()

        val countKategori = getCount("SELECT COUNT(*) FROM kategori")
        binding.tvCountKategori.text = countKategori.toString()

        val countSales = getCount("SELECT COUNT(*) FROM sales")
        binding.tvCountSales.text = countSales.toString()

        val totalStok = getCount("SELECT IFNULL(SUM(stok),0) FROM barang")
        binding.tvTotalUnit.text = "$totalStok Unit"
    }

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

        // Ambil data stok per nama barang dari DB
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

        // Atur warna dan tampilan grafik
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
        _binding = null
    }
}