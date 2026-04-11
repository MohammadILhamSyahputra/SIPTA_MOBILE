package com.example.sipta

import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.sipta.databinding.ActivityFragmentRiwayatSalesOwnerBinding

class FragmentRiwayatSalesOwner : Fragment() {

    private var _binding: ActivityFragmentRiwayatSalesOwnerBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: SQLiteDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityFragmentRiwayatSalesOwnerBinding.inflate(inflater, container, false)

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
            SELECT sales.nama_sales, riwayat_sales.tanggal_kunjungan, riwayat_sales.status
            FROM riwayat_sales
            JOIN sales ON riwayat_sales.sales_id = sales.id
            ORDER BY riwayat_sales.tanggal_kunjungan DESC
        """.trimIndent(), null)

        binding.containerRiwayat.removeAllViews()

        while (cursor.moveToNext()) {
            val nama = cursor.getString(0)
            val tanggal = cursor.getString(1)
            val status = cursor.getString(2)

            val tv = android.widget.TextView(requireContext())
            tv.text = "$nama - $tanggal ($status)"

            binding.containerRiwayat.addView(tv)
        }

        cursor.close()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}