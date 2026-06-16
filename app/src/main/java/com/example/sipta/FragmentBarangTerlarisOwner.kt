package com.example.sipta

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
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject
import java.util.HashMap

class FragmentBarangTerlarisOwner : Fragment() {

    private var _binding: ActivityFragmentBarangTerlarisOwnerBinding? = null
    private val binding get() = _binding!!

    // Tambah URL IP Server Manual (Menembak ke file laporan_barang_terlaris.php)
    private val urlLaporan = "http://192.168.0.102/sipta_api/laporan_barang_terlaris.php"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityFragmentBarangTerlarisOwnerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.etTanggalMulai.setOnClickListener { showDatePicker(binding.etTanggalMulai) }
        binding.etTanggalAkhir.setOnClickListener { showDatePicker(binding.etTanggalAkhir) }

        binding.btnTampilkan.setOnClickListener {
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
            val displayDate = String.format("%02d/%02d/%04d", day, bulan, year)
            val dbDate = String.format("%04d-%02d-%02d", year, bulan, day)

            editText.setText(displayDate)
            editText.tag = dbDate
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun loadDataFiltered(tglMulai: String, tglAkhir: String) {
        val count = binding.tableBarang.childCount
        if (count > 1) {
            binding.tableBarang.removeViews(1, count - 1)
        }

        // Ganti SQLite rawQuery dengan StringRequest Volley secara manual
        val request = object : StringRequest(Request.Method.POST, urlLaporan,
            Response.Listener { response ->
                if (isAdded && activity != null) {
                    try {
                        val jsonArray = JSONArray(response)
                        val entries = ArrayList<BarEntry>()
                        val labels = ArrayList<String>()
                        var indexChart = 0f

                        for (x in 0 until jsonArray.length()) {
                            val jsonObject = jsonArray.getJSONObject(x)
                            val kode = jsonObject.getString("kode_barang")
                            val nama = jsonObject.getString("nama")
                            val hargaBeli = jsonObject.getInt("harga_beli")
                            val qty = jsonObject.getInt("total_qty")

                            // --- BAGIAN TABEL ---
                            val row = TableRow(requireContext())
                            row.setPadding(8, 8, 8, 8)

                            row.addView(createTextView(kode))
                            row.addView(createTextView(nama))
                            row.addView(createTextView("Rp $hargaBeli"))

                            val tvQty = createTextView(qty.toString())
                            tvQty.setTextColor(android.graphics.Color.RED)
                            row.addView(tvQty)

                            binding.tableBarang.addView(row)

                            // --- BAGIAN CHART ---
                            entries.add(BarEntry(indexChart, qty.toFloat()))
                            labels.add(nama)
                            indexChart++
                        }

                        if (entries.isNotEmpty()) {
                            tampilkanChart(entries, labels)
                        } else {
                            binding.barChart.clear()
                            Toast.makeText(context, "Tidak ada data pada rentang tanggal ini", Toast.LENGTH_SHORT).show()
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            },
            Response.ErrorListener {
                if (isAdded) Toast.makeText(requireContext(), "Gagal memuat barang terlaris", Toast.LENGTH_SHORT).show()
            }) {

            // Tambah blok getParams di bagian bawah StringRequest
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["mode"] = "show_barang_terlaris"
                params["tgl_mulai"] = tglMulai
                params["tgl_akhir"] = tglAkhir
                return params
            }
        }
        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun tampilkanChart(entries: ArrayList<BarEntry>, labels: ArrayList<String>) {
        val dataSet = BarDataSet(entries, "Total Terjual")
        dataSet.color = android.graphics.Color.parseColor("#03A9F4")
        dataSet.valueTextSize = 12f

        val barData = BarData(dataSet)
        binding.barChart.data = barData

        val xAxis = binding.barChart.xAxis
        xAxis.valueFormatter = com.github.mikephil.charting.formatter.IndexAxisValueFormatter(labels)
        xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.labelRotationAngle = -45f

        binding.barChart.description.isEnabled = false
        binding.barChart.animateY(1000)
        binding.barChart.invalidate()
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