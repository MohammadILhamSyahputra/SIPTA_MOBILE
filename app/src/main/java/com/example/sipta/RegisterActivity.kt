package com.example.sipta

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.widget.Toast
import com.example.sipta.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var db: SQLiteDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 1. Inisialisasi ViewBinding
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 2. Inisialisasi Database
        val dbHelper = DBOpenHelper(this)
        db = dbHelper.writableDatabase

        // 3. Logika Klik Tombol Daftar
        binding.btnRegisterSubmit.setOnClickListener {
            val nama = binding.etFullName.text.toString()
            val email = binding.etEmailRegister.text.toString()
            val pass = binding.etPasswordRegister.text.toString()
            val confirmPass = binding.etConfirmPassword.text.toString()

            // Validasi Input
            if (nama.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Semua kolom wajib diisi!", Toast.LENGTH_SHORT).show()
            } else if (pass != confirmPass) {
                Toast.makeText(this, "Password tidak cocok!", Toast.LENGTH_SHORT).show()
            } else {
                saveUserToDatabase(nama, email, pass)
            }
        }

        // 4. Kembali ke Login
        binding.tvToLogin.setOnClickListener {
            finish() // Menutup halaman register dan kembali ke login
        }
//        setContentView(R.layout.activity_register)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
    private fun saveUserToDatabase(nama: String, email: String, pass: String) {
        try {
            val values = ContentValues().apply {
                put("name", nama)
                put("email", email)
                put("password", pass)
                put("userType", "admin") // Default role sesuai dokumentasi SIPTA
                put("created_at", System.currentTimeMillis().toString())
            }

            // Insert ke tabel users [cite: 1234]
            val result = db.insert("users", null, values)

            if (result != -1L) {
                Toast.makeText(this, "Registrasi Berhasil! Silahkan Login.", Toast.LENGTH_LONG).show()
                finish() // Kembali ke LoginActivity
            } else {
                Toast.makeText(this, "Email sudah terdaftar!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}