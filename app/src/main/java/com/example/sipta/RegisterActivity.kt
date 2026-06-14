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
import androidx.activity.enableEdgeToEdge
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding

    // Deklarasi URL Root Server Laragon (Ganti dengan IP Laptop kamu via ipconfig)
    private val urlRegister = "http://192.168.0.120/sipta_api/register.php"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Logika Klik Tombol Daftar
        binding.btnRegisterSubmit.setOnClickListener {
            val nama = binding.etFullName.text.toString().trim()
            val email = binding.etEmailRegister.text.toString().trim()
            val pass = binding.etPasswordRegister.text.toString()
            val confirmPass = binding.etConfirmPassword.text.toString()

            // Validasi Input
            if (nama.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Semua kolom wajib diisi!", Toast.LENGTH_SHORT).show()
            } else if (pass != confirmPass) {
                Toast.makeText(this, "Password tidak cocok!", Toast.LENGTH_SHORT).show()
            } else {
                // Jalankan pendaftaran ke MySQL pusat
                registerUserKeServer(nama, email, pass)
            }
        }

        // Kembali ke Login
        binding.tvToLogin.setOnClickListener {
            finish()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun registerUserKeServer(nama: String, email: String, pass: String) {
        // Membuat request POST menggunakan Volley sesuai modul dosen
        val request = object : StringRequest(Request.Method.POST, urlRegister,
            Response.Listener { response ->
                try {
                    val jsonObject = JSONObject(response)
                    val kode = jsonObject.getString("kode")

                    if (kode == "000") {
                        Toast.makeText(this, "Registrasi Berhasil! Silahkan Login.", Toast.LENGTH_LONG).show()
                        finish() // Sukses, langsung tutup activity dan balik ke login
                    } else if (kode == "111") {
                        Toast.makeText(this, "Registrasi Gagal! Email sudah terdaftar.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Operasi GAGAL!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Parsing Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            Response.ErrorListener { error ->
                Toast.makeText(this, "Tidak dapat terhubung ke server: ${error.message}", Toast.LENGTH_LONG).show()
            }) {

            // Menyisipkan data parameter yang dikirim ke file PHP
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["nama"] = nama
                params["email"] = email
                params["password"] = pass
                params["user_type"] = "Kasir" // Default sesuai arsitektur awal kelompokmu
                return params
            }
        }

        // Eksekusi request ke dalam antrean Volley
        val queue = Volley.newRequestQueue(this)
        queue.add(request)
    }
}