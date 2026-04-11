package com.example.sipta

import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.sipta.databinding.ActivityFragmentBarangTerlarisOwnerBinding

class FragmentBarangTerlarisOwner : Fragment() {

    private var _binding: ActivityFragmentBarangTerlarisOwnerBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: SQLiteDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityFragmentBarangTerlarisOwnerBinding.inflate(inflater, container, false)

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
            SELECT barang.nama, SUM(detail_transaksi.qty) AS total
            FROM detail_transaksi
            JOIN barang ON detail_transaksi.id_barang = barang.id
            GROUP BY barang.nama
            ORDER BY total DESC
            LIMIT 5
        """.trimIndent(), null)

        binding.containerBarangTerlaris.removeAllViews()

        while (cursor.moveToNext()) {
            val nama = cursor.getString(0)
            val total = cursor.getInt(1)

            val tv = android.widget.TextView(requireContext())
            tv.text = "$nama - Terjual: $total"
            tv.textSize = 16f

            binding.containerBarangTerlaris.addView(tv)
        }

        cursor.close()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}