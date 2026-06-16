package com.example.sipta

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.sipta.databinding.ActivityFragmentLapPenjualanOwnerBinding
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.HashMap

class FragmentLapPenjualanOwner : Fragment() {

    private var _binding: ActivityFragmentLapPenjualanOwnerBinding? = null
    private val binding get() = _binding!!

    // Tambah URL IP Server Manual (Menembak ke file laporan_penjualan.php)
    private val urlLaporan = "http://192.168.18.21/sipta_api/laporan_penjualan.php"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityFragmentLapPenjualanOwnerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnExportPdf.setOnClickListener {
            exportPdf()
        }
        binding.etTglMulaiJual.setOnClickListener {
            showDatePicker(binding.etTglMulaiJual)
        }
        binding.etTglAkhirJual.setOnClickListener {
            showDatePicker(binding.etTglAkhirJual)
        }
        binding.btnTampilkanLap.setOnClickListener {
            val tglMulai = binding.etTglMulaiJual.tag?.toString() ?: ""
            val tglAkhir = binding.etTglAkhirJual.tag?.toString() ?: ""

            if (tglMulai.isNotEmpty() && tglAkhir.isNotEmpty()) {
                loadData(tglMulai, tglAkhir)
            }
        }
    }

    private fun loadData(tglMulai: String, tglAkhir: String) {
        binding.layoutHasilPenjualan.removeAllViews()

        // Ganti SQLite rawQuery dengan StringRequest Volley secara manual
        val request = object : StringRequest(Request.Method.POST, urlLaporan,
            Response.Listener { response ->
                if (isAdded && activity != null) {
                    try {
                        val jsonArray = JSONArray(response)
                        var totalPenjualan = 0
                        var totalModal = 0

                        for (x in 0 until jsonArray.length()) {
                            val jsonObject = jsonArray.getJSONObject(x)
                            val nama = jsonObject.getString("nama")
                            val qty = jsonObject.getInt("qty")
                            val harga = jsonObject.getInt("harga_satuan")
                            val tanggal = jsonObject.getString("tanggal")
                            val hargaBeli = jsonObject.getInt("harga_beli")

                            val subtotal = qty * harga
                            totalPenjualan += subtotal

                            val modal = qty * hargaBeli
                            totalModal += modal

                            val itemView = createItemView(nama, qty, harga, subtotal, tanggal)
                            binding.layoutHasilPenjualan.addView(itemView)
                        }

                        val keuntungan = totalPenjualan - totalModal
                        val margin = if (totalPenjualan > 0) (keuntungan * 100 / totalPenjualan) else 0
                        binding.tvTotalPenjualan.text =
                            "Total Penjualan: Rp $totalPenjualan\n" + "Keuntungan: Rp $keuntungan\n" + "Margin: $margin%"

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            },
            Response.ErrorListener {
                if (isAdded) Toast.makeText(requireContext(), "Gagal memuat data dari MySQL", Toast.LENGTH_SHORT).show()
            }) {

            // Tambah blok getParams di bagian bawah StringRequest
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["mode"] = "show_lap_penjualan"
                params["tgl_mulai"] = tglMulai
                params["tgl_akhir"] = tglAkhir
                return params
            }
        }
        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun createItemView(nama: String, qty: Int, harga: Int, subtotal: Int, tanggal: String): View {
        val card = androidx.cardview.widget.CardView(requireContext())
        val params = ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 0, 0, 16)
        card.layoutParams = params
        card.radius = 16f
        card.setCardBackgroundColor(android.graphics.Color.WHITE)
        card.cardElevation = 6f
        card.setContentPadding(20, 20, 20, 20)

        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL

        val tvNama = TextView(requireContext())
        tvNama.text = nama
        tvNama.textSize = 16f
        tvNama.setTypeface(null, android.graphics.Typeface.BOLD)

        val tvTanggal = TextView(requireContext())
        tvTanggal.text = "Tanggal: $tanggal"
        tvTanggal.textSize = 12f

        val tvDetail = TextView(requireContext())
        tvDetail.text = "$qty x Rp $harga"

        val tvSubtotal = TextView(requireContext())
        tvSubtotal.text = "Subtotal: Rp $subtotal"
        tvSubtotal.setTextColor(android.graphics.Color.parseColor("#2E7D32"))
        tvSubtotal.textSize = 14f

        layout.addView(tvNama)
        layout.addView(tvTanggal)
        layout.addView(tvDetail)
        layout.addView(tvSubtotal)

        card.addView(layout)

        return card
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

    private fun exportPdf() {
        try {
            if (binding.layoutHasilPenjualan.childCount == 0) {
                android.widget.Toast.makeText(
                    requireContext(),
                    "Tampilkan data dulu sebelum export!",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                return
            }

            val resolver = requireContext().contentResolver
            val fileName = "Laporan_Penjualan_${System.currentTimeMillis()}.pdf"
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(
                    android.provider.MediaStore.MediaColumns.RELATIVE_PATH,
                    android.os.Environment.DIRECTORY_DOWNLOADS
                )
            }

            val uri = resolver.insert(
                android.provider.MediaStore.Files.getContentUri("external"),
                contentValues
            )

            if (uri == null) {
                android.widget.Toast.makeText(requireContext(), "Gagal membuat file", android.widget.Toast.LENGTH_SHORT).show()
                return
            }

            val outputStream = resolver.openOutputStream(uri)
            val writer = com.itextpdf.kernel.pdf.PdfWriter(outputStream)
            val pdf = com.itextpdf.kernel.pdf.PdfDocument(writer)
            val document = com.itextpdf.layout.Document(pdf)
            document.add(
                com.itextpdf.layout.element.Paragraph("LAPORAN PENJUALAN")
                    .setBold()
                    .setFontSize(18f)
            )

            val tglMulai = binding.etTglMulaiJual.text.toString()
            val tglAkhir = binding.etTglAkhirJual.text.toString()

            document.add(
                com.itextpdf.layout.element.Paragraph("Periode: $tglMulai - $tglAkhir")
            )
            document.add(com.itextpdf.layout.element.Paragraph("\n"))
            val table = com.itextpdf.layout.element.Table(5)

            // HEADER
            table.addHeaderCell(
                com.itextpdf.layout.element.Cell().add(
                    com.itextpdf.layout.element.Paragraph("Nama Barang").setBold()
                )
            )
            table.addHeaderCell(
                com.itextpdf.layout.element.Cell().add(
                    com.itextpdf.layout.element.Paragraph("Tanggal").setBold()
                )
            )
            table.addHeaderCell(
                com.itextpdf.layout.element.Cell().add(
                    com.itextpdf.layout.element.Paragraph("Qty").setBold()
                )
            )
            table.addHeaderCell(
                com.itextpdf.layout.element.Cell().add(
                    com.itextpdf.layout.element.Paragraph("Harga").setBold()
                )
            )
            table.addHeaderCell(
                com.itextpdf.layout.element.Cell().add(
                    com.itextpdf.layout.element.Paragraph("Subtotal").setBold()
                )
            )

            val count = binding.layoutHasilPenjualan.childCount
            for (i in 0 until count) {
                val item = binding.layoutHasilPenjualan.getChildAt(i) as androidx.cardview.widget.CardView
                val layout = item.getChildAt(0) as LinearLayout

                val nama = (layout.getChildAt(0) as TextView).text.toString()
                val tanggal = (layout.getChildAt(1) as TextView).text.toString().replace("Tanggal: ", "")
                val detail = (layout.getChildAt(2) as TextView).text.toString()
                val subtotal = (layout.getChildAt(3) as TextView).text.toString().replace("Subtotal: ", "")

                // Pecah qty & harga
                val parts = detail.split(" x Rp ")
                val qty = parts[0]
                val harga = if (parts.size > 1) parts[1] else "0"

                table.addCell(nama)
                table.addCell(tanggal)
                table.addCell(qty)
                table.addCell(harga)
                table.addCell(subtotal)
            }

            document.add(table)
            document.add(com.itextpdf.layout.element.Paragraph("\n"))
            document.add(
                com.itextpdf.layout.element.Paragraph(binding.tvTotalPenjualan.text.toString())
                    .setBold()
            )

            document.close()
            outputStream?.close()

            android.widget.Toast.makeText(
                requireContext(),
                "PDF berhasil disimpan di Download!",
                android.widget.Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(
                requireContext(),
                "Gagal export PDF: ${e.message}",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}