package com.example.sipta

import android.app.AlertDialog
import android.content.ContentValues
import android.content.DialogInterface
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.sipta.databinding.ActivityFragmentKategoriBinding

class FragmentKategori : Fragment(), View.OnClickListener {
    private var vb: ActivityFragmentKategoriBinding? = null
    private val binding get() = vb!!
    private lateinit var thisParent: MainActivityAdmin
    private lateinit var db: SQLiteDatabase
    private lateinit var dialog: AlertDialog.Builder
    private var selectedId: String = "" // Untuk menyimpan ID yang dipilih di List

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        thisParent = activity as MainActivityAdmin
        vb = ActivityFragmentKategoriBinding.inflate(inflater, container, false)

        db = thisParent.getDbObject()
        dialog = AlertDialog.Builder(thisParent)

        // Event Klik
        binding.btnInsert.setOnClickListener(this)
        binding.btnUpdate.setOnClickListener(this)
        binding.btnDelete.setOnClickListener(this)
        binding.lsKategori.setOnItemClickListener { parent, view, position, id ->
            val cursor = parent.adapter.getItem(position) as Cursor
            selectedId = cursor.getString(cursor.getColumnIndexOrThrow("_id"))
            binding.edNamaKategori.setText(cursor.getString(cursor.getColumnIndexOrThrow("nama_kategori")))

            binding.btnInsert.isEnabled = false
            binding.btnInsert.alpha = 0.5f
        }

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        showDataKategori()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btnInsert -> {
                // Validasi: Jika sedang memilih data (selectedId tidak kosong), larang Insert
                if (selectedId.isNotEmpty()) {
                    Toast.makeText(thisParent, "Data sudah terpilih. Gunakan UPDATE untuk mengubah atau klik daftar lain.", Toast.LENGTH_SHORT).show()
                } else {
                    showConfirmDialog("INSERT")
                }
            }

            R.id.btnUpdate -> {
                // Validasi: Harus pilih data dulu sebelum Update
                if (selectedId.isEmpty()) {
                    Toast.makeText(thisParent, "Silahkan pilih data kategori dari daftar terlebih dahulu!", Toast.LENGTH_SHORT).show()
                } else {
                    showConfirmDialog("UPDATE")
                }
            }

            R.id.btnDelete -> {
                // Validasi: Harus pilih data dulu sebelum Delete
                if (selectedId.isEmpty()) {
                    Toast.makeText(thisParent, "Silahkan pilih data kategori yang ingin dihapus!", Toast.LENGTH_SHORT).show()
                } else {
                    showConfirmDialog("DELETE")
                }
            }
        }
    }

    private fun showConfirmDialog(action: String) {
        val message = when(action) {
            "DELETE" -> "Yakin akan menghapus kategori ini?"
            else -> "Apakah data sudah benar?"
        }

        dialog.setTitle("Konfirmasi $action")
            .setMessage(message)
            .setIcon(android.R.drawable.ic_dialog_info)
            .setPositiveButton("Ya") { _, _ ->
                when(action) {
                    "INSERT" -> insertData()
                    "UPDATE" -> updateData()
                    "DELETE" -> deleteData()
                }
            }
            .setNegativeButton("Tidak", null)
            .show()
    }

    private fun showDataKategori() {
        // Query SQLite: id as _id wajib untuk SimpleCursorAdapter
        val cursor: Cursor = db.query("kategori", arrayOf("id as _id", "nama_kategori"),
            null, null, null, null, "nama_kategori ASC")

        val adapter = SimpleCursorAdapter(
            thisParent, R.layout.item_data_kategori, cursor,
            arrayOf("_id", "nama_kategori"),
            intArrayOf(R.id.txIdKategori, R.id.txNamaKategori),
            0
        )
        binding.lsKategori.adapter = adapter
    }

    private fun insertData() {
        val nama = binding.edNamaKategori.text.toString()
        if (nama.isNotEmpty()) {
            val cv = ContentValues()
            cv.put("nama_kategori", nama)
            db.insert("kategori", null, cv)
            clearForm()
        }
    }

    private fun updateData() {
        val nama = binding.edNamaKategori.text.toString()
        if (selectedId.isNotEmpty() && nama.isNotEmpty()) {
            val cv = ContentValues()
            cv.put("nama_kategori", nama)
            db.update("kategori", cv, "id = ?", arrayOf(selectedId))
            clearForm()
        }
    }

    private fun deleteData() {
        if (selectedId.isNotEmpty()) {
            db.delete("kategori", "id = ?", arrayOf(selectedId))
            clearForm()
        }
    }

    private fun clearForm() {
        binding.edNamaKategori.setText("")
        selectedId = ""
        binding.btnInsert.isEnabled = true
        binding.btnInsert.alpha = 1.0f
        showDataKategori()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        vb = null
    }
}