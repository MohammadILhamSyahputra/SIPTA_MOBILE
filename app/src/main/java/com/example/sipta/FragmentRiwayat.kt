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

class FragmentRiwayat : Fragment() {
    private var _binding: ActivityFragmentRiwayatBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: SQLiteDatabase

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val parentActivity = activity as MainActivityKasir
        db = parentActivity.getDbObject()
        _binding = ActivityFragmentRiwayatBinding.inflate(inflater, container, false)
        
        loadRiwayatHariIni()
        
        return binding.root
    }

    private fun loadRiwayatHariIni() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = sdf.format(Date())
        
        val sql = "SELECT id as _id, total_harga, tanggal FROM transaksi WHERE tanggal LIKE '$today%' ORDER BY id DESC"
        val cursor = db.rawQuery(sql, null)
        
        val adapter = object : CursorAdapter(requireContext(), cursor, 0) {
            override fun newView(context: Context?, cursor: Cursor?, parent: ViewGroup?): View {
                return LayoutInflater.from(context).inflate(R.layout.item_riwayat, parent, false)
            }

            override fun bindView(view: View?, context: Context?, cursor: Cursor?) {
                val tvKode = view?.findViewById<TextView>(R.id.tvKodeTransaksi)
                val tvWaktu = view?.findViewById<TextView>(R.id.tvWaktuTransaksi)
                val tvTotal = view?.findViewById<TextView>(R.id.tvTotalRiwayat)
                val btnDetail = view?.findViewById<ImageButton>(R.id.btnDetailRiwayat)

                val id = cursor?.getInt(cursor.getColumnIndexOrThrow("_id"))
                val total = cursor?.getInt(cursor.getColumnIndexOrThrow("total_harga"))
                val tanggalFull = cursor?.getString(cursor.getColumnIndexOrThrow("tanggal"))
                
                // Ambil jam saja dari tanggalFull (yyyy-MM-dd HH:mm:ss)
                val waktu = tanggalFull?.split(" ")?.getOrNull(1) ?: ""

                tvKode?.text = "TRX-${String.format("%03d", id)}"
                tvWaktu?.text = waktu
                tvTotal?.text = "Rp $total"

                btnDetail?.setOnClickListener {
                    showDetailTransaksi(id ?: 0)
                }
            }
        }
        binding.lvRiwayat.adapter = adapter
    }

    private fun showDetailTransaksi(idTransaksi: Int) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.layout_dialog_detail_transaksi, null)
        val tvHeader = dialogView.findViewById<TextView>(R.id.tvDetailHeader)
        val llItems = dialogView.findViewById<LinearLayout>(R.id.llDetailItems)
        val tvTotal = dialogView.findViewById<TextView>(R.id.tvDetailTotal)
        val tvBayar = dialogView.findViewById<TextView>(R.id.tvDetailBayar)
        val tvKembali = dialogView.findViewById<TextView>(R.id.tvDetailKembali)

        // Ambil Data Header Transaksi
        val cursorH = db.rawQuery("SELECT * FROM transaksi WHERE id = ?", arrayOf(idTransaksi.toString()))
        if (cursorH.moveToFirst()) {
            val total = cursorH.getInt(cursorH.getColumnIndexOrThrow("total_harga"))
            val bayar = cursorH.getInt(cursorH.getColumnIndexOrThrow("total_bayar"))
            val kembali = cursorH.getInt(cursorH.getColumnIndexOrThrow("kembalian"))
            val tgl = cursorH.getString(cursorH.getColumnIndexOrThrow("tanggal"))

            tvHeader.text = "Transaksi: TRX-${String.format("%03d", idTransaksi)}\nTanggal: $tgl"
            tvTotal.text = "Total: Rp $total"
            tvBayar.text = "Bayar: Rp $bayar"
            tvKembali.text = "Kembali: Rp $kembali"
        }
        cursorH.close()

        // Ambil Data Detail Barang
        val sqlD = """
            SELECT d.qty, d.harga_satuan, d.subtotal, b.nama 
            FROM detail_transaksi d 
            JOIN barang b ON d.id_barang = b.id 
            WHERE d.id_transaksi = ?
        """.trimIndent()
        val cursorD = db.rawQuery(sqlD, arrayOf(idTransaksi.toString()))
        if (cursorD.moveToFirst()) {
            do {
                val nama = cursorD.getString(cursorD.getColumnIndexOrThrow("nama"))
                val qty = cursorD.getInt(cursorD.getColumnIndexOrThrow("qty"))
                val sub = cursorD.getInt(cursorD.getColumnIndexOrThrow("subtotal"))
                
                val tvItem = TextView(requireContext())
                tvItem.text = "$nama ($qty x) = Rp $sub"
                tvItem.setPadding(0, 4, 0, 4)
                llItems.addView(tvItem)
            } while (cursorD.moveToNext())
        }
        cursorD.close()

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
}
