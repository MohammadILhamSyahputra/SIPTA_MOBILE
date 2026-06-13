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
import android.widget.*
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject

class FragmentPOS : Fragment() {
    private var _binding: ActivityFragmentPosBinding? = null
    private val binding get() = _binding!!

    private val keranjangList = mutableListOf<CartItem>()
    private lateinit var adapterKeranjang: KeranjangAdapter

    // URL Web Service Laragon POS Transaksi
    private val urlPos = "http://192.168.1.127/sipta_api/crud_transaksi_pos.php"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ActivityFragmentPosBinding.inflate(inflater, container, false)

        setupCartList()

        binding.btnTambahBaris.setOnClickListener { showTambahBarangDialog() }
        binding.btnCheckout.setOnClickListener { showCheckoutDialog() }

        return binding.root
    }

    private fun setupCartList() {
        adapterKeranjang = KeranjangAdapter(requireContext(), keranjangList)
        binding.lvKeranjang.adapter = adapterKeranjang
    }

    private fun showTambahBarangDialog() {
        val listBarang = mutableListOf<BarangSimple>()

        // Ambil data barang dinamis langsung dari server MySQL pusat
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

                    // Tampilkan Dialog AutoComplete setelah data berhasil ditarik dari server
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

        // Konversi list belanja menjadi format Array JSON String agar ramah dibaca PHP
        val jsonArrayItems = JSONArray()
        for (item in keranjangList) {
            val itemObj = JSONObject()
            itemObj.put("barang_id", item.idBarang) // Menyesuaikan kolom barang_id database pusat
            itemObj.put("qty", item.qty)
            itemObj.put("harga_satuan", item.harga)
            itemObj.put("subtotal", item.subtotal)
            jsonArrayItems.put(itemObj)
        }

        val requestCheckout = object : StringRequest(Request.Method.POST, urlPos,
            Response.Listener { response ->
                if (!isAdded) return@Listener
                try {
                    val jsonRes = JSONObject(response)
                    if (jsonRes.getString("kode") == "000") {
                        AlertDialog.Builder(requireContext())
                            .setTitle("Transaksi Berhasil")
                            .setMessage("Kembalian: Rp $kembalian")
                            .setPositiveButton("OK") { _, _ ->
                                keranjangList.clear()
                                adapterKeranjang.notifyDataSetChanged()
                                updateTotal()
                            }
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
