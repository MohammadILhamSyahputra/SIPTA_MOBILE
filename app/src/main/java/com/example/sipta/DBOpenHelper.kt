package com.example.sipta
import android.content.Context
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBOpenHelper (context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VER) {
    override fun onCreate(db: SQLiteDatabase?) {
        val tUsers = "CREATE TABLE users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "userType TEXT NOT NULL DEFAULT 'owner', " +
                "email TEXT UNIQUE NOT NULL, " +
                "password TEXT NOT NULL, " +
                "created_at TEXT, updated_at TEXT)"

        val tKategori = "CREATE TABLE kategori (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nama_kategori TEXT NOT NULL, " +
                "created_at TEXT, updated_at TEXT)"

        val tSales = "CREATE TABLE sales (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nama_sales TEXT NOT NULL, " +
                "no_telp TEXT NOT NULL, " +
                "alamat TEXT NOT NULL, " +
                "created_at TEXT, updated_at TEXT)"

        val tBarang = "CREATE TABLE barang (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "kode_barang TEXT UNIQUE NOT NULL, " +
                "nama TEXT NOT NULL, " +
                "stok INTEGER NOT NULL, " +
                "harga_beli INTEGER NOT NULL, " +
                "harga_jual INTEGER NOT NULL, " +
                "id_kategori INTEGER NOT NULL, " +
                "id_sales INTEGER NOT NULL, " +
                "created_at TEXT, updated_at TEXT, " +
                "FOREIGN KEY(id_kategori) REFERENCES kategori(id), " +
                "FOREIGN KEY(id_sales) REFERENCES sales(id))"


        val tTransaksi = "CREATE TABLE transaksi (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "total_harga INTEGER NOT NULL, " +
                "total_bayar INTEGER NOT NULL, " +
                "kembalian INTEGER NOT NULL, " +
                "tanggal TEXT NOT NULL, " +
                "created_at TEXT, updated_at TEXT)"

        val tDetailTransaksi = "CREATE TABLE detail_transaksi (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "id_transaksi INTEGER NOT NULL, " +
                "id_barang INTEGER NOT NULL, " +
                "qty INTEGER NOT NULL, " +
                "harga_satuan INTEGER NOT NULL, " +
                "subtotal INTEGER NOT NULL, " +
                "created_at TEXT, updated_at TEXT, " +
                "FOREIGN KEY(id_transaksi) REFERENCES transaksi(id), " +
                "FOREIGN KEY(id_barang) REFERENCES barang(id))"

        val tRiwayatSales = "CREATE TABLE riwayat_sales (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "sales_id INTEGER NOT NULL, " +
                "status TEXT CHECK(status IN ('belum datang', 'proses', 'sudah datang')) NOT NULL DEFAULT 'belum datang', " +
                "tanggal_kunjungan TEXT, " +
                "created_at TEXT, updated_at TEXT, " +
                "FOREIGN KEY(sales_id) REFERENCES sales(id))"

        val tDetailRiwayatSales = "CREATE TABLE detail_riwayat_sales (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "riwayat_sales_id INTEGER NOT NULL, " +
                "barang_id INTEGER NOT NULL, " +
                "qty_masuk INTEGER NOT NULL, " +
                "qty_retur INTEGER NOT NULL, " +
                "created_at TEXT, updated_at TEXT, " +
                "FOREIGN KEY(riwayat_sales_id) REFERENCES riwayat_sales(id), " +
                "FOREIGN KEY(barang_id) REFERENCES barang(id))"

        // Eksekusi Semua Query
        db?.execSQL(tUsers)
        db?.execSQL(tKategori)
        db?.execSQL(tSales)
        db?.execSQL(tBarang)
        db?.execSQL(tTransaksi)
        db?.execSQL(tDetailTransaksi)
        db?.execSQL(tRiwayatSales)
        db?.execSQL(tDetailRiwayatSales)

        // Data Awal
        db?.execSQL("INSERT INTO users (name, userType, email, password) VALUES ('SHOFI DINA ANGGRAINI', 'owner', 'shofidina@gmail.com', '123456')")
        db?.execSQL("INSERT INTO users (name, userType, email, password) VALUES ('MOHAMMAD ILHAM SYAHPUTRA', 'admin', 'ilham@gmail.com', '12345678')")
        db?.execSQL("INSERT INTO users (name, userType, email, password) VALUES ('QADAFI ZAN ZABILL', 'kasir', 'dapy@gmail.com', '12345678')")
        db?.execSQL("INSERT INTO kategori (nama_kategori) VALUES ('Makanan Instan'), ('Makanan Ringan'), ('Kebutuhan Dapur'), ('Peralatan Mandi')")
    }

    fun getAllUsers(): ArrayList<User> {
        val list = ArrayList<User>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM users", null)
        if (cursor.moveToFirst()) {
            do {
                list.add(User(
                    cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("name")), // Ini username di model
                    cursor.getString(cursor.getColumnIndexOrThrow("email")),
                    cursor.getString(cursor.getColumnIndexOrThrow("userType")) // Ini level di model
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun deleteUser(id: Int): Int {
        return writableDatabase.delete("users", "id=?", arrayOf(id.toString()))
    }

    fun updateUserRole(id: Int, newRole: String): Int {
        val values = ContentValues()
        values.put("userType", newRole)
        return writableDatabase.update("users", values, "id=?", arrayOf(id.toString()))

    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS detail_riwayat_sales")
        db?.execSQL("DROP TABLE IF EXISTS riwayat_sales")
        db?.execSQL("DROP TABLE IF EXISTS detail_transaksi")
        db?.execSQL("DROP TABLE IF EXISTS transaksi")
        db?.execSQL("DROP TABLE IF EXISTS barang")
        db?.execSQL("DROP TABLE IF EXISTS sales")
        db?.execSQL("DROP TABLE IF EXISTS kategori")
        db?.execSQL("DROP TABLE IF EXISTS users")
        onCreate(db)
    }

    fun addUser(name: String, email: String, pass: String, type: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("name", name)
            put("email", email)
            put("password", pass)
            put("userType", type)
            put("created_at", System.currentTimeMillis().toString())
        }
        return db.insert("users", null, values)
    }

    companion object {
        const val DB_NAME = "sipta_mobile.db"
        const val DB_VER = 2
    }
}
// Model Data User
//data class User(val id: Int, val username: String, val email: String, val level: String)