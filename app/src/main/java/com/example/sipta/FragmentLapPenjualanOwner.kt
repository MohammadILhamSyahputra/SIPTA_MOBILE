package com.example.sipta

import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.sipta.databinding.ActivityFragmentLapPenjualanOwnerBinding

class FragmentLapPenjualanOwner : Fragment() {

    private var _binding: ActivityFragmentLapPenjualanOwnerBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: SQLiteDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityFragmentLapPenjualanOwnerBinding.inflate(inflater, container, false)

        val parentActivity = requireActivity() as MainActivityOwner
        db = parentActivity.getDbObject()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadData()
    }

    private fun loadData() {
        val cursor = db.rawQuery("""
            SELECT barang.nama, detail_transaksi.qty, detail_transaksi.harga_satuan
            FROM detail_transaksi
            JOIN barang ON detail_transaksi.id_barang = barang.id
        """.trimIndent(), null)

        var totalPenjualan = 0
        binding.containerPenjualan.removeAllViews()

        while (cursor.moveToNext()) {
            val nama = cursor.getString(0)
            val qty = cursor.getInt(1)
            val harga = cursor.getInt(2)

            val subtotal = qty * harga
            totalPenjualan += subtotal

            val tv = android.widget.TextView(requireContext())
            tv.text = "$nama - $qty x $harga = $subtotal"

            binding.containerPenjualan.addView(tv)
        }

        binding.tvTotalPenjualan.text = "Total Penjualan: Rp $totalPenjualan"

        cursor.close()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}