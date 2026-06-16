package com.example.sipta

import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.sipta.databinding.ActivityFragmentPosBinding
import java.text.SimpleDateFormat
import java.util.*
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.property.TextAlignment
import android.provider.MediaStore
import android.os.Environment

class FragmentPOS : Fragment() {
    private var _binding: ActivityFragmentPosBinding? = null
    private val binding get() = _binding!!

    private val keranjangList = mutableListOf<CartItem>()
    private lateinit var adapterKeranjang: KeranjangAdapter

    // URL Web Service Laragon POS Transaksi
    private val urlPos = "http://192.168.0.102/sipta_api/crud_transaksi_pos.php"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ActivityFragmentPosBinding.inflate(inflater, container, false)

        setupCartList()

        binding.btnTambahBaris.setOnClickListener { showTambahBarangDialog() }
        binding.btnScanQR.setOnClickListener {
            bukaKameraScan()
        }
        binding.btnCheckout.setOnClickListener { showCheckoutDialog() }

        return binding.root
    }

    private fun setupCartList() {
        adapterKeranjang = KeranjangAdapter(requireContext(), keranjangList)
        binding.lvKeranjang.adapter = adapterKeranjang
    }

    private fun showTambahBarangDialog() {
        val listBarang = mutableListOf<BarangSimple>()

        val request = object : StringRequest(Request.Method.POST, urlPos,
            Response.Listener { response ->
                if (!isAdded) return@Listener
                try {
                    val jsonArray = JSONArray(response)
                    for (x in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(x)
                        listBarang.add(BarangSimple(
                            obj.getInt("id"),
                            obj.getString("kode_barang"),
                            obj.getString("nama"),
                            obj.getInt("stok"),
                            obj.getInt("harga_jual")
                        ))
                    }

                    val autoView = AutoCompleteTextView(requireContext())
                    autoView.setPadding(40, 40, 40, 40)
                    autoView.hint = "Ketik Nama atau Kode Barang"
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, listBarang.map { "${it.kode_barang} - ${it.nama}" })
                    autoView.setAdapter(adapter)

                    val dialog = AlertDialog.Builder(requireContext())
                        .setTitle("Tambah Barang ke Keranjang")
                        .setView(autoView)
                        .create()

                    autoView.setOnItemClickListener { _, _, position, _ ->
                        val selectedText = autoView.adapter.getItem(position).toString()
                        val kode = selectedText.split(" - ")[0]
                        val barang = listBarang.find { it.kode_barang == kode }

                        barang?.let { tambahAtauUpdateKeranjang(it) }
                        dialog.dismiss()
                    }
                    dialog.show()

                } catch (e: Exception) { e.printStackTrace() }
            }, Response.ErrorListener {
                if (isAdded) Toast.makeText(requireContext(), "Gagal koneksi server barang", Toast.LENGTH_SHORT).show()
            }) {
            override fun getParams(): MutableMap<String, String> = hashMapOf("mode" to "get_barang")
        }
        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun tambahAtauUpdateKeranjang(barang: BarangSimple) {
        val existingItem = keranjangList.find { it.idBarang == barang.id }
        if (existingItem != null) {
            if (existingItem.qty + 1 <= barang.stok) {
                existingItem.qty += 1
                existingItem.subtotal = existingItem.qty * existingItem.harga
            } else {
                Toast.makeText(requireContext(), "Stok tidak mencukupi!", Toast.LENGTH_SHORT).show()
            }
        } else {
            if (barang.stok > 0) {
                keranjangList.add(CartItem(barang.id, barang.kode_barang, barang.nama, barang.harga, 1, barang.harga, barang.stok))
            } else {
                Toast.makeText(requireContext(), "Stok Kosong!", Toast.LENGTH_SHORT).show()
            }
        }

        updateTotal()
        adapterKeranjang.notifyDataSetChanged()
    }

    private fun updateTotal() {
        val total = keranjangList.sumOf { it.subtotal }
        binding.tvTotalHarga.text = "Rp $total"
    }

    private fun showCheckoutDialog() {
        if (keranjangList.isEmpty()) {
            Toast.makeText(requireContext(), "Keranjang masih kosong", Toast.LENGTH_SHORT).show()
            return
        }

        val total = keranjangList.sumOf { it.subtotal }
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.layout_dialog_checkout, null)
        val tvTotal = dialogView.findViewById<TextView>(R.id.tvTotalCheckout)
        val etBayar = dialogView.findViewById<EditText>(R.id.etBayar)

        tvTotal.text = "Total: Rp $total"

        AlertDialog.Builder(requireContext())
            .setTitle("Pembayaran Tunai")
            .setView(dialogView)
            .setPositiveButton("Proses") { _, _ ->
                val bayarStr = etBayar.text.toString()
                if (bayarStr.isEmpty()) return@setPositiveButton

                val bayar = bayarStr.toInt()
                if (bayar < total) {
                    Toast.makeText(requireContext(), "Uang tidak cukup!", Toast.LENGTH_SHORT).show()
                } else {
                    prosesTransaksiKeServer(total, bayar)
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun prosesTransaksiKeServer(total: Int, bayar: Int) {
        val kembalian = bayar - total
        val jsonArrayItems = JSONArray()
        for (item in keranjangList) {
            val itemObj = JSONObject()
            itemObj.put("barang_id", item.idBarang)
            itemObj.put("qty", item.qty)
            itemObj.put("harga_satuan", item.harga)
            itemObj.put("subtotal", item.subtotal)
            jsonArrayItems.put(itemObj)
        }

        // Salinan list untuk cetak nota karena keranjang akan dikosongkan
        val listNota = ArrayList(keranjangList)

        val requestCheckout = object : StringRequest(Request.Method.POST, urlPos,
            Response.Listener { response ->
                if (!isAdded) return@Listener
                try {
                    val jsonRes = JSONObject(response)
                    if (jsonRes.getString("kode") == "000") {
                        AlertDialog.Builder(requireContext())
                            .setTitle("Transaksi Berhasil")
                            .setMessage("Kembalian: Rp $kembalian\nApakah ingin cetak nota?")
                            .setPositiveButton("Cetak Nota") { _, _ ->
                                cetakNotaPdf(listNota, total, bayar, kembalian)
                                resetPOS()
                            }
                            .setNegativeButton("Tidak") { _, _ ->
                                resetPOS()
                            }
                            .setCancelable(false)
                            .show()
                    } else {
                        Toast.makeText(requireContext(), "Gagal: ${jsonRes.getString("pesan")}", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }, Response.ErrorListener {
                if (isAdded) Toast.makeText(requireContext(), "Koneksi ke server terputus", Toast.LENGTH_SHORT).show()
            }) {
            override fun getParams(): MutableMap<String, String> = hashMapOf(
                "mode" to "proses_checkout",
                "total_harga" to total.toString(),
                "total_bayar" to bayar.toString(),
                "kembalian" to kembalian.toString(),
                "items_json" to jsonArrayItems.toString()
            )
        }
        Volley.newRequestQueue(requireContext()).add(requestCheckout)
    }

    private fun resetPOS() {
        keranjangList.clear()
        adapterKeranjang.notifyDataSetChanged()
        updateTotal()
    }

    private fun cetakNotaPdf(items: List<CartItem>, total: Int, bayar: Int, kembali: Int) {
        try {
            val resolver = requireContext().contentResolver
            val fileName = "Nota_SIPTA_${System.currentTimeMillis()}.pdf"
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
            if (uri == null) return

            val outputStream = resolver.openOutputStream(uri)
            val writer = PdfWriter(outputStream)
            val pdf = PdfDocument(writer)
            val document = Document(pdf)

            // Header Toko
            document.add(Paragraph("TOKO SIPTA").setBold().setFontSize(20f).setTextAlignment(TextAlignment.CENTER))
            document.add(Paragraph("Jl. KH Wachid Hasyim No.94, Bandar Lor, Kota Kediri").setFontSize(10f).setTextAlignment(TextAlignment.CENTER))
            document.add(Paragraph("------------------------------------------------------------------").setTextAlignment(TextAlignment.CENTER))

            // Info Transaksi
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            document.add(Paragraph("Tgl: ${sdf.format(Date())}").setFontSize(10f))
            document.add(Paragraph("\n"))

            // Tabel Barang
            val table = Table(floatArrayOf(3f, 1f, 2f, 2f))
            table.addHeaderCell(Cell().add(Paragraph("Item").setBold()))
            table.addHeaderCell(Cell().add(Paragraph("Qty").setBold()))
            table.addHeaderCell(Cell().add(Paragraph("Harga").setBold()))
            table.addHeaderCell(Cell().add(Paragraph("Subtotal").setBold()))

            for (item in items) {
                table.addCell(item.nama)
                table.addCell(item.qty.toString())
                table.addCell("Rp ${item.harga}")
                table.addCell("Rp ${item.subtotal}")
            }
            document.add(table)

            document.add(Paragraph("\n"))
            document.add(Paragraph("------------------------------------------------------------------").setTextAlignment(TextAlignment.CENTER))
            
            // Footer Total
            document.add(Paragraph("Total Belanja: Rp $total").setBold().setTextAlignment(TextAlignment.RIGHT))
            document.add(Paragraph("Bayar: Rp $bayar").setTextAlignment(TextAlignment.RIGHT))
            document.add(Paragraph("Kembali: Rp $kembali").setBold().setTextAlignment(TextAlignment.RIGHT))

            document.add(Paragraph("\n"))
            document.add(Paragraph("Terima kasih atas kunjungan Anda!").setFontSize(10f).setTextAlignment(TextAlignment.CENTER))

            document.close()
            outputStream?.close()

            Toast.makeText(requireContext(), "Nota berhasil disimpan di Download", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Gagal cetak nota: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents == null) {
            Toast.makeText(requireContext(), "Scan dibatalkan", Toast.LENGTH_SHORT).show()
        } else {
            val hasilTeksQR = result.contents
            prosesInputBarangViaQR(hasilTeksQR)
        }
    }

    private fun bukaKameraScan() {
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(listOf(ScanOptions.QR_CODE))
            setPrompt("Arahkan kamera ke QR Code Barang")
            setCameraId(0)
            setBeepEnabled(true)
            setOrientationLocked(true)
        }
        barcodeLauncher.launch(options)
    }

    private fun prosesInputBarangViaQR(kodeHasilScan: String) {
        Toast.makeText(requireContext(), "Mencari barang...", Toast.LENGTH_SHORT).show()

        val request = object : StringRequest(Request.Method.POST, urlPos,
            Response.Listener { response ->
                if (!isAdded) return@Listener
                try {
                    val jsonObject = JSONObject(response)

                    if (jsonObject.has("status") && jsonObject.getString("status") == "gagal") {
                        Toast.makeText(requireContext(), jsonObject.getString("pesan"), Toast.LENGTH_LONG).show()
                        return@Listener
                    }

                    val cocok = BarangSimple(
                        jsonObject.getInt("id"),
                        jsonObject.getString("kode_barang"),
                        jsonObject.getString("nama"),
                        jsonObject.getInt("stok"),
                        jsonObject.getInt("harga_jual")
                    )

                    tambahAtauUpdateKeranjang(cocok)
                    Toast.makeText(requireContext(), "${cocok.nama} ditambahkan", Toast.LENGTH_SHORT).show()

                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Barang tidak ditemukan", Toast.LENGTH_SHORT).show()
                }
            }, Response.ErrorListener {
                if (isAdded) Toast.makeText(requireContext(), "Gagal koneksi server", Toast.LENGTH_SHORT).show()
            }) {
            override fun getParams(): MutableMap<String, String> = hashMapOf(
                "mode" to "scan_single_barang",
                "kode_barang" to kodeHasilScan.trim()
            )
        }
        Volley.newRequestQueue(requireContext()).add(request)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    data class BarangSimple(val id: Int, val kode_barang: String, val nama: String, val stok: Int, val harga: Int)
    data class CartItem(val idBarang: Int, val kode: String, val nama: String, val harga: Int, var qty: Int, var subtotal: Int, val maxStok: Int)

    inner class KeranjangAdapter(context: Context, val items: MutableList<CartItem>) : ArrayAdapter<CartItem>(context, 0, items) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var itemView = convertView
            if (itemView == null) {
                itemView = LayoutInflater.from(context).inflate(R.layout.item_keranjang, parent, false)
            }
            val item = items[position]

            val tvKode = itemView!!.findViewById<TextView>(R.id.tvKodeItem)
            val tvNama = itemView.findViewById<TextView>(R.id.tvNamaItem)
            val etQty = itemView.findViewById<EditText>(R.id.etQtyItem)
            val tvSubtotal = itemView.findViewById<TextView>(R.id.tvSubtotalItem)
            val btnHapus = itemView.findViewById<ImageButton>(R.id.btnHapusItem)

            tvKode.text = item.kode
            tvNama.text = item.nama
            tvSubtotal.text = "Rp ${item.subtotal}"

            etQty.tag?.let { (it as TextWatcher).let { etQty.removeTextChangedListener(it) } }
            etQty.setText(item.qty.toString())

            val watcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val input = s.toString().toIntOrNull() ?: 0
                    if (input > item.maxStok) {
                        Toast.makeText(context, "Maksimal stok: ${item.maxStok}", Toast.LENGTH_SHORT).show()
                        etQty.setText(item.maxStok.toString())
                        item.qty = item.maxStok
                    } else {
                        item.qty = input
                    }
                    item.subtotal = item.qty * item.harga
                    tvSubtotal.text = "Rp ${item.subtotal}"
                    updateTotal()
                }
            }
            etQty.addTextChangedListener(watcher)
            etQty.tag = watcher

            btnHapus.setOnClickListener {
                items.removeAt(position)
                notifyDataSetChanged()
                updateTotal()
            }

            return itemView
        }
    }
}
