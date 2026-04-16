package com.example.sipta

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.widget.Toast
import com.example.sipta.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var db: SQLiteDatabase
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 1. Inisialisasi ViewBinding sesuai standar praktik [cite: 1280, 1288]
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val dbHelper = DBOpenHelper(this)
        db = dbHelper.readableDatabase

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                performLogin(email, password)
            } else {
                Toast.makeText(this, "Email dan Password wajib diisi", Toast.LENGTH_SHORT).show()
            }
        }

        binding.tvRegisterLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
//        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
    private fun performLogin(email: String, password: String) {
        // Query untuk mencari user berdasarkan email dan password [cite: 1332, 1421]
        val cursor: Cursor = db.rawQuery(
            "SELECT * FROM users WHERE email = ? AND password = ?",
            arrayOf(email, password)
        )

        if (cursor.moveToFirst()) {
            val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
            val email = cursor.getString(cursor.getColumnIndexOrThrow("email"))
            val role = cursor.getString(cursor.getColumnIndexOrThrow("userType"))

            Toast.makeText(this, "Selamat Datang, $name!", Toast.LENGTH_SHORT).show()

//            val targetClass = when (role.lowercase()) {
//                "admin" -> Intent(this, MainActivityAdmin::class.java)
//                "owner" -> Intent(this, MainActivityOwner::class.java)
//                else -> Intent(this, MainActivityKasir::class.java)
//            }
            val targetClass = when (role.lowercase()) {
                "admin" -> MainActivityAdmin::class.java
                "owner" -> MainActivityOwner::class.java
                else -> MainActivityKasir::class.java
            }

            val intent = Intent(this, targetClass)

            // 3. Masukkan "Barang Bawaan" (data profil) ke dalam kendaraan
            intent.putExtra("USER_NAME", name)
            intent.putExtra("USER_EMAIL", email)
            intent.putExtra("USER_ROLE", role)

            startActivity(intent)
            finish() // Agar user tidak bisa kembali ke halaman login setelah masuk
        } else {
            Toast.makeText(this, "Email atau Password Salah!", Toast.LENGTH_SHORT).show()
        }
        cursor.close()
    }


}