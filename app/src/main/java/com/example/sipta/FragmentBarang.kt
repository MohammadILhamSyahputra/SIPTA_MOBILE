package com.example.sipta

import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.sipta.databinding.ActivityFragmentBarangBinding

class FragmentBarang : Fragment() {
    private var _binding: ActivityFragmentBarangBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: SQLiteDatabase
    private lateinit var thisParent: MainActivityAdmin

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        thisParent = activity as MainActivityAdmin
        db = thisParent.getDbObject()
        _binding = ActivityFragmentBarangBinding.inflate(inflater, container, false)

        val namaBarangList = getListData("barang", "nama")
        val autoAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, namaBarangList)
        binding.acBarang.setAdapter(autoAdapter)

        binding.acBarang.setOnItemClickListener { parent, _, position, _ ->
            val selectedName = parent.getItemAtPosition(position).toString()
            loadDataBarang(selectedName) // Tampilkan hanya barang yang dipilih di ListView
        }

        binding.acBarang.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                loadDataBarang(s.toString())
            }
        })
        binding.fabAddBarang.setOnClickListener { showBarangDialog(null) }
//        binding.svBarang.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
//            override fun onQueryTextSubmit(query: String?): Boolean = false
//            override fun onQueryTextChange(newText: String?): Boolean {
//                loadDataBarang(newText ?: "")
//                return true
//            }
//        })

        loadDataBarang("")
        return binding.root
    }

    private fun loadDataBarang(query: String) {
        val sql = """
            SELECT b.id as _id, b.kode_barang, b.nama, b.stok, b.harga_jual, 
            k.nama_kategori, s.nama_sales 
            FROM barang b
            JOIN kategori k ON b.id_kategori = k.id
            JOIN sales s ON b.id_sales = s.id
            WHERE b.nama LIKE '%$query%' OR b.kode_barang LIKE '%$query%'
            ORDER BY b.nama ASC
        """.trimIndent()

        val cursor = db.rawQuery(sql, null)
        val adapter = object : CursorAdapter(requireContext(), cursor, 0) {
            override fun newView(context: Context?, cursor: Cursor?, parent: ViewGroup?): View {
                return LayoutInflater.from(context).inflate(R.layout.item_data_barang, parent, false)
            }

            override fun bindView(view: View?, context: Context?, cursor: Cursor?) {
                val txKodeBarang = view?.findViewById<TextView>(R.id.txKodeBarang)
                val txNama = view?.findViewById<TextView>(R.id.txNamaBarang)
                val txStok = view?.findViewById<TextView>(R.id.txStokBarang)
                val txHarga = view?.findViewById<TextView>(R.id.txHargaJual)
                val txInfo = view?.findViewById<TextView>(R.id.txInfoKategoriSales)
                val btnDel = view?.findViewById<ImageButton>(R.id.btnDeleteBarang)

                val stok = cursor?.getInt(cursor.getColumnIndexOrThrow("stok")) ?: 0
                val id = cursor?.getInt(cursor.getColumnIndexOrThrow("_id"))

                txKodeBarang?.text = cursor?.getString(cursor.getColumnIndexOrThrow("kode_barang"))
                txNama?.text = cursor?.getString(cursor.getColumnIndexOrThrow("nama"))
                txStok?.text = stok.toString()
                txHarga?.text = "Rp ${cursor?.getInt(cursor.getColumnIndexOrThrow("harga_jual"))}"
                txInfo?.text = "${cursor?.getString(cursor.getColumnIndexOrThrow("nama_kategori"))} | ${cursor?.getString(cursor.getColumnIndexOrThrow("nama_sales"))}"

                // Logika Warna Stok
                val shape = GradientDrawable()
                shape.cornerRadius = 100f
                when {
                    stok <= 10 -> shape.setColor(Color.parseColor("#D32F2F")) // Merah
                    stok <= 25 -> shape.setColor(Color.parseColor("#FBC02D")) // Kuning
                    else -> shape.setColor(Color.parseColor("#388E3C")) // Hijau
                }
                txStok?.background = shape

                btnDel?.setOnClickListener {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Hapus Barang")
                        .setMessage("Apakah Anda yakin ingin menghapus ${cursor?.getString(cursor.getColumnIndexOrThrow("nama"))}?")
                        .setPositiveButton("Ya, Hapus") { _, _ ->
                            db.delete("barang", "id=?", arrayOf(id.toString()))
                            loadDataBarang("") // Refresh List setelah hapus
                            Toast.makeText(context, "Barang Dihapus", Toast.LENGTH_SHORT).show()
                        }
                        .setNegativeButton("Batal", null)
                        .show()
                }


                view?.setOnClickListener {
                    Toast.makeText(requireContext(), "Tekan lama untuk mengedit data", Toast.LENGTH_SHORT).show()
                }

                view?.setOnLongClickListener {
                    showBarangDialog(id) // Panggil dialog edit
                    true
                }
            }
        }
        binding.lvBarang.adapter = adapter
    }

    private fun showBarangDialog(id: Int?) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.layout_dialog_barang, null)

        // Inisialisasi View dari Dialog
        val etKode = dialogView.findViewById<EditText>(R.id.etKode)
        val etNama = dialogView.findViewById<EditText>(R.id.etNama)
        val etStok = dialogView.findViewById<EditText>(R.id.etStok)
        val etHargaBeli = dialogView.findViewById<EditText>(R.id.etHargaBeli)
        val etHargaJual = dialogView.findViewById<EditText>(R.id.etHargaJual)
        val spKategori = dialogView.findViewById<Spinner>(R.id.spKategori)
        val spSales = dialogView.findViewById<Spinner>(R.id.spSales)

        // Isi Spinner Kategori & Sales
        val listKategori = getListData("kategori", "nama_kategori")
        val listSales = getListData("sales", "nama_sales")

        spKategori.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, listKategori)
        spSales.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, listSales)

        if (id != null) {
            val cursor = db.rawQuery("SELECT * FROM barang WHERE id = ?", arrayOf(id.toString()))
            if (cursor.moveToFirst()) {
                etKode.setText(cursor.getString(cursor.getColumnIndexOrThrow("kode_barang")))
                etNama.setText(cursor.getString(cursor.getColumnIndexOrThrow("nama")))
                etStok.setText(cursor.getString(cursor.getColumnIndexOrThrow("stok")))
                etHargaBeli.setText(cursor.getString(cursor.getColumnIndexOrThrow("harga_beli")))
                etHargaJual.setText(cursor.getString(cursor.getColumnIndexOrThrow("harga_jual")))

                etKode.isEnabled = false // User tidak bisa klik atau ketik di sini
                etKode.alpha = 0.6f      // Opsional: Membuat warnanya agak pudar agar terlihat "terkunci"

                val currentKat = getOneData("kategori", "nama_kategori", cursor.getInt(cursor.getColumnIndexOrThrow("id_kategori")))
                val currentSal = getOneData("sales", "nama_sales", cursor.getInt(cursor.getColumnIndexOrThrow("id_sales")))

                val posKat = listKategori.indexOf(currentKat)
                val posSal = listSales.indexOf(currentSal)

                if (posKat != -1) spKategori.setSelection(posKat)
                if (posSal != -1) spSales.setSelection(posSal)
            }
            cursor.close()
        } else {
            // Mode TAMBAH: Pastikan Kode Barang aktif kembali
            etKode.isEnabled = true
            etKode.alpha = 1.0f
            etKode.setText("")
        }

        val mDialog = AlertDialog.Builder(requireContext())
            .setTitle(if (id == null) "Tambah Barang Baru" else "Edit Data Barang")
            .setView(dialogView)
            .setPositiveButton("Simpan", null) // Biarkan null dulu
            .setNegativeButton("Batal", null)
            .create()

        mDialog.show()

        // Logika Klik Tombol Simpan Manual
        mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val kode = etKode.text.toString().trim()
            val nama = etNama.text.toString().trim()

            // Validasi input kosong
            if (kode.isEmpty() || nama.isEmpty()) {
                Toast.makeText(context, "Kode dan Nama wajib diisi!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // VALIDASI DUPLIKAT (Hanya saat tambah barang baru)
            if (id == null) {
                if (isDataExist("kode_barang", kode)) {
                    etKode.error = "Kode sudah terdaftar!"
                    Toast.makeText(context, "Gagal! Kode Barang '$kode' sudah digunakan", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (isDataExist("nama", nama)) {
                    etNama.error = "Nama sudah terdaftar!"
                    Toast.makeText(context, "Gagal! Nama Barang '$nama' sudah ada", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            // Jika lolos validasi, lakukan proses simpan
            val idKat = getIDFromName("kategori", "nama_kategori", spKategori.selectedItem.toString())
            val idSal = getIDFromName("sales", "nama_sales", spSales.selectedItem.toString())

            val values = ContentValues().apply {
                put("kode_barang", kode)
                put("nama", nama)
                put("stok", etStok.text.toString().toIntOrNull() ?: 0)
                put("harga_beli", etHargaBeli.text.toString().toIntOrNull() ?: 0)
                put("harga_jual", etHargaJual.text.toString().toIntOrNull() ?: 0)
                put("id_kategori", idKat)
                put("id_sales", idSal)
            }

            if (id == null) {
                db.insert("barang", null, values)
                Toast.makeText(context, "Barang berhasil disimpan", Toast.LENGTH_SHORT).show()
            } else {
                db.update("barang", values, "id=?", arrayOf(id.toString()))
                Toast.makeText(context, "Data berhasil diperbarui", Toast.LENGTH_SHORT).show()
            }

            loadDataBarang("") // Refresh List
            mDialog.dismiss()  // Tutup dialog hanya jika sukses
        }
    }

    // Fungsi mengambil daftar nama untuk Spinner
    private fun getListData(table: String, column: String): ArrayList<String> {
        val list = ArrayList<String>()
        val cursor = db.rawQuery("SELECT $column FROM $table", null)
        if (cursor.moveToFirst()) {
            do { list.add(cursor.getString(0)) } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    // Fungsi mencari ID berdasarkan nama yang dipilih di Spinner
    private fun getIDFromName(table: String, column: String, value: String): Int {
        var idTarget = 0
        val cursor = db.rawQuery("SELECT id FROM $table WHERE $column = ?", arrayOf(value))
        if (cursor.moveToFirst()) idTarget = cursor.getInt(0)
        cursor.close()
        return idTarget
    }

    // Fungsi untuk mengecek keberadaan data di database
    private fun isDataExist(column: String, value: String): Boolean {
        val query = "SELECT 1 FROM barang WHERE LOWER($column) = LOWER(?)"
        val cursor = db.rawQuery(query, arrayOf(value))
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    private fun getOneData(table: String, column: String, id: Int): String {
        var hasil = ""
        val cursor = db.rawQuery("SELECT $column FROM $table WHERE id = ?", arrayOf(id.toString()))
        if (cursor.moveToFirst()) {
            hasil = cursor.getString(0)
        }
        cursor.close()
        return hasil
    }
}