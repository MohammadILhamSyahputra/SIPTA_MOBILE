package com.example.sipta

import android.app.AlertDialog
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.sipta.databinding.ActivityFragmentRiwayatBinding
import java.text.SimpleDateFormat
import java.util.*
import android.widget.*
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject

class FragmentRiwayat : Fragment() {
    private var _binding: ActivityFragmentRiwayatBinding? = null
    private val binding get() = _binding!!

    // URL Web Service Laragon Riwayat Kasir
    private val urlRiwayat = "http://192.168.18.21/sipta_api/crud_riwayat_pos.php"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ActivityFragmentRiwayatBinding.inflate(inflater, container, false)

        loadRiwayatHariIni()

        return binding.root
    }

    private fun loadRiwayatHariIni() {
        val listRiwayat = mutableListOf<RiwayatTransaksi>()

        val request = object : StringRequest(Request.Method.POST, urlRiwayat,
            Response.Listener { response ->
                if (!isAdded) return@Listener
                try {
                    val jsonArray = JSONArray(response)
                    for (x in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(x)
                        listRiwayat.add(RiwayatTransaksi(
                            obj.getInt("id"),
                            obj.getInt("total_harga"),
                            obj.getInt("total_bayar"),
                            obj.getInt("kembalian"),
                            obj.getString("tanggal")
                        ))
                    }

                    // Pasang Custom Adapter untuk List View
                    val adapter = RiwayatAdapter(requireContext(), listRiwayat)
                    binding.lvRiwayat.adapter = adapter

                } catch (e: Exception) { e.printStackTrace() }
            },
            Response.ErrorListener {
                if (isAdded) Toast.makeText(requireContext(), "Gagal memuat riwayat", Toast.LENGTH_SHORT).show()
            }) {
            override fun getParams(): MutableMap<String, String> = hashMapOf("mode" to "show_today")
        }
        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun showDetailTransaksi(trx: RiwayatTransaksi) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.layout_dialog_detail_transaksi, null)
        val tvHeader = dialogView.findViewById<TextView>(R.id.tvDetailHeader)
        val llItems = dialogView.findViewById<LinearLayout>(R.id.llDetailItems)
        val tvTotal = dialogView.findViewById<TextView>(R.id.tvDetailTotal)
        val tvBayar = dialogView.findViewById<TextView>(R.id.tvDetailBayar)
        val tvKembali = dialogView.findViewById<TextView>(R.id.tvDetailKembali)

        // Set Data Header langsung dari objek transaksi terpilih
        tvHeader.text = "Transaksi: TRX-${String.format("%03d", trx.id)}\nTanggal: ${trx.tanggal}"
        tvTotal.text = "Total: Rp ${trx.totalHarga}"
        tvBayar.text = "Bayar: Rp ${trx.totalBayar}"
        tvKembali.text = "Kembali: Rp ${trx.kembalian}"

        // Tarik detail list barang pembelian dari server MySQL
        val reqDetail = object : StringRequest(Request.Method.POST, urlRiwayat,
            Response.Listener { response ->
                if (!isAdded) return@Listener
                try {
                    val jsonArray = JSONArray(response)
                    for (x in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(x)
                        val nama = obj.getString("nama")
                        val qty = obj.getInt("qty")
                        val subtotal = obj.getInt("subtotal")

                        val tvItem = TextView(requireContext()).apply {
                            text = "$nama ($qty x) = Rp $subtotal"
                            setPadding(0, 6, 0, 6)
                            textSize = 14f
                        }
                        llItems.addView(tvItem)
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }, Response.ErrorListener {}) {
            override fun getParams(): MutableMap<String, String> = hashMapOf(
                "mode" to "show_detail",
                "transaksi_id" to trx.id.toString()
            )
        }
        Volley.newRequestQueue(requireContext()).add(reqDetail)

        AlertDialog.Builder(requireContext())
            .setTitle("Detail Transaksi")
            .setView(dialogView)
            .setPositiveButton("Tutup", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // --- CUSTOM ADAPTER UNTUK RENDER ITEM LIST RIWAYAT ---
    inner class RiwayatAdapter(context: Context, val items: List<RiwayatTransaksi>) : ArrayAdapter<RiwayatTransaksi>(context, 0, items) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var itemView = convertView
            if (itemView == null) {
                itemView = LayoutInflater.from(context).inflate(R.layout.item_riwayat, parent, false)
            }
            val data = items[position]

            val tvKode = itemView!!.findViewById<TextView>(R.id.tvKodeTransaksi)
            val tvWaktu = itemView.findViewById<TextView>(R.id.tvWaktuTransaksi)
            val tvTotal = itemView.findViewById<TextView>(R.id.tvTotalRiwayat)
            val btnDetail = itemView.findViewById<ImageButton>(R.id.btnDetailRiwayat)

            // Memotong jam saja dari datetime string (yyyy-MM-dd HH:mm:ss)
            val waktuOnly = data.tanggal.split(" ").getOrNull(1) ?: ""

            tvKode.text = "TRX-${String.format("%03d", data.id)}"
            tvWaktu.text = waktuOnly
            tvTotal.text = "Rp ${data.totalHarga}"

            btnDetail.setOnClickListener {
                showDetailTransaksi(data)
            }

            return itemView
        }
    }
}
