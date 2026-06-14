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
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class FragmentDashboardAdmin : Fragment() {
    private var _binding: ActivityFragmentDashboardAdminBinding? = null
    private val binding get() = _binding!!

    // URL Menembak Server Laragon (Sesuaikan dengan IP Laptop Server kelompokmu)
    private val urlDashboard = "http://192.168.0.120/sipta_api/dashboard_admin.php"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityFragmentDashboardAdminBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Cukup panggil satu fungsi utama ini untuk memuat seluruh data dari MySQL
        loadDataDashboardPusat()
    }

    override fun onStart() {
        super.onStart()
        loadDataDashboardPusat()
    }

    private fun loadDataDashboardPusat() {
        val request = object : StringRequest(Request.Method.POST, urlDashboard,
            Response.Listener { response ->
                // PROTEKSI KUNCI: Amankan jalannya UI thread dari Fragment Detached
                if (isAdded && activity != null) {
                    try {
                        val jsonObject = JSONObject(response)

                        // 1. Parsing Data Statistik CardView Utama
                        val countBarang = jsonObject.getInt("count_barang")
                        val countKategori = jsonObject.getInt("count_kategori")
                        val countSales = jsonObject.getInt("count_sales")
                        val totalStok = jsonObject.getInt("total_stok")

                        // Suntikkan data ke komponen widget XML
                        binding.tvCountBarang.text = countBarang.toString()
                        binding.tvCountKategori.text = countKategori.toString()
                        binding.tvCountSales.text = countSales.toString()
                        binding.tvTotalUnit.text = "$totalStok Unit"

                        // 2. Parsing Array Data untuk PieChart
                        val arrayGrafik = jsonObject.getJSONArray("grafik_stok")
                        val listEntri = ArrayList<PieEntry>()

                        if (arrayGrafik.length() == 0) {
                            binding.pieChart.setNoDataText("Belum ada data barang untuk ditampilkan")
                            binding.pieChart.invalidate()
                        } else {
                            for (x in 0 until arrayGrafik.length()) {
                                val itemBarang = arrayGrafik.getJSONObject(x)
                                val namaBarang = itemBarang.getString("nama")
                                val stokBarang = itemBarang.getDouble("stok").toFloat()

                                // Masukkan data riil MySQL ke penampung grafik
                                listEntri.add(PieEntry(stokBarang, namaBarang))
                            }

                            // Setup tampilan MPAndroidChart menggunakan data hasil parsing
                            val dataSet = PieDataSet(listEntri, "Distribusi Stok")
                            dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
                            dataSet.valueTextColor = Color.BLACK
                            dataSet.valueTextSize = 12f

                            val data = PieData(dataSet)
                            binding.pieChart.data = data
                            binding.pieChart.centerText = "Stok Barang"
                            binding.pieChart.description.isEnabled = false
                            binding.pieChart.animateY(1000) // Jalankan animasi putar grafik
                            binding.pieChart.invalidate()   // Gambar ulang grafik di layar
                        }

                    } catch (e: Exception) {
                        if (isAdded) Toast.makeText(requireContext(), "Parsing Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            Response.ErrorListener {
                if (isAdded) Toast.makeText(requireContext(), "Gagal menyinkronkan data dashboard", Toast.LENGTH_SHORT).show()
            }) {
            override fun getParams(): MutableMap<String, String> {
                // Diisi array kosong karena script backend hanya mendeteksi request POST standar
                return HashMap()
            }
        }
        Volley.newRequestQueue(requireContext()).add(request)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}