package com.example.sipta

import android.app.AlertDialog
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.sipta.databinding.ActivityFragmentSalesBinding

class FragmentSales : Fragment(), View.OnClickListener {
    private var vb: ActivityFragmentSalesBinding? = null
    private val binding get() = vb!!
    private lateinit var db: SQLiteDatabase
    private var selectedId: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        vb = ActivityFragmentSalesBinding.inflate(inflater, container, false)

        // Ambil DB dari MainActivityAdmin
        db = (activity as MainActivityAdmin).getDbObject()

        binding.btnInsertSales.setOnClickListener(this)
        binding.btnUpdateSales.setOnClickListener(this)
        binding.btnDeleteSales.setOnClickListener(this)

        binding.lsSales.setOnItemClickListener { parent, _, position, _ ->
            val c = parent.adapter.getItem(position) as Cursor
            selectedId = c.getString(c.getColumnIndexOrThrow("_id"))
            binding.edNamaSales.setText(c.getString(c.getColumnIndexOrThrow("nama_sales")))
            binding.edNoTelp.setText(c.getString(c.getColumnIndexOrThrow("no_telp")))
            binding.edAlamatSales.setText(c.getString(c.getColumnIndexOrThrow("alamat")))
            binding.btnInsertSales.isEnabled = false // Matikan tombol Insert
            binding.btnInsertSales.alpha = 0.5f
        }

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        showDataSales()
    }

    private fun showDataSales() {
        val cursor: Cursor = db.rawQuery("SELECT id as _id, nama_sales, no_telp, alamat FROM sales ORDER BY nama_sales ASC", null)
        val adapter = SimpleCursorAdapter(
            requireContext(), R.layout.item_data_sales, cursor,
            arrayOf("nama_sales", "no_telp", "alamat"),
            intArrayOf(R.id.txNamaSales, R.id.txNoTelp, R.id.txAlamat), 0
        )
        binding.lsSales.adapter = adapter
    }

    override fun onClick(v: View?) {
        // 1. Inisialisasi Dialog Builder
        val dialog = AlertDialog.Builder(requireContext())

        when (v?.id) {
            R.id.btnInsertSales -> {
                // CEK: Jika selectedId tidak kosong, berarti sedang mode EDIT, jangan izinkan INSERT
                if (selectedId.isNotEmpty()) {
                    Toast.makeText(requireContext(), "Gunakan tombol UPDATE untuk mengubah data yang dipilih", Toast.LENGTH_SHORT).show()
                } else {
                    // Logika Insert tetap seperti biasa
                    dialog.setTitle("Konfirmasi Simpan")
                        .setMessage("Apakah data sales ini sudah benar?")
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setPositiveButton("Ya") { _, _ ->
                            val cv = ContentValues().apply {
                                put("nama_sales", binding.edNamaSales.text.toString())
                                put("no_telp", binding.edNoTelp.text.toString())
                                put("alamat", binding.edAlamatSales.text.toString())
                            }
                            db.insert("sales", null, cv)
                            refreshData()
                        }
                        .setNegativeButton("Tidak", null)
                        .show()
                }
            }

            R.id.btnUpdateSales -> {
                if (selectedId.isEmpty()) {
                    Toast.makeText(requireContext(), "Pilih data dari daftar terlebih dahulu!", Toast.LENGTH_SHORT).show()
                } else {
                    // Logika Update (Sudah benar)
                    dialog.setTitle("Konfirmasi Update")
                        .setMessage("Yakin ingin mengubah data sales ini?")
                        .setPositiveButton("Ya") { _, _ ->
                            val cv = ContentValues().apply {
                                put("nama_sales", binding.edNamaSales.text.toString())
                                put("no_telp", binding.edNoTelp.text.toString())
                                put("alamat", binding.edAlamatSales.text.toString())
                            }
                            db.update("sales", cv, "id = ?", arrayOf(selectedId))
                            refreshData()
                        }
                        .setNegativeButton("Tidak", null)
                        .show()
                }
            }
            R.id.btnDeleteSales -> {
                if (selectedId.isNotEmpty()) {
                    dialog.setTitle("Konfirmasi Hapus")
                        .setMessage("Yakin ingin menghapus data sales ini?")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton("Ya") { _, _ ->
                            db.delete("sales", "id = ?", arrayOf(selectedId))
                            refreshData()
                        }
                        .setNegativeButton("Tidak", null)
                        .show()
                }
            }
        }
    }

    private fun refreshData() {
        binding.edNamaSales.setText(""); binding.edNoTelp.setText(""); binding.edAlamatSales.setText("")
        selectedId = ""
        binding.btnInsertSales.isEnabled = true  // Aktifkan kembali
        binding.btnInsertSales.alpha = 1.0f
        showDataSales()
    }
}