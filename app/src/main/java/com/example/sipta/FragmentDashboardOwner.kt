package com.example.sipta

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.sipta.databinding.ActivityFragmentDashboardOwnerBinding

class FragmentDashboardOwner : Fragment() {

    private var _binding: ActivityFragmentDashboardOwnerBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: SQLiteDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityFragmentDashboardOwnerBinding.inflate(inflater, container, false)

        // Ambil database dari MainActivityOwner
        val parentActivity = activity as MainActivityOwner
        db = parentActivity.getDbObject()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateDashboardStats()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}