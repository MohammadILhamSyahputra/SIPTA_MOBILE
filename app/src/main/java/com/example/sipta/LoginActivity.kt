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
import androidx.activity.enableEdgeToEdge
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import android.content.Context

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    // Deklarasi URL Login Server Laragon (Gunakan IP laptop server kalian)
    private val urlLogin = "http://192.168.1.127/sipta_api/login.php"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                // Panggil fungsi login server
                performLoginKeServer(email, password)
            } else {
                Toast.makeText(this, "Email dan Password wajib diisi", Toast.LENGTH_SHORT).show()
            }
        }

        binding.tvRegisterLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun performLoginKeServer(emailInput: String, passwordInput: String) {
        val request = object : StringRequest(Request.Method.POST, urlLogin,
            Response.Listener { response ->
                try {
                    val jsonObject = JSONObject(response)
                    val kode = jsonObject.getString("kode")

                    if (kode == "000") {
                        // Ambil data profil yang dikirim balik oleh PHP
                        val name = jsonObject.getString("nama")
                        val email = jsonObject.getString("email")
                        val role = jsonObject.getString("user_type")

                        Toast.makeText(this, "Selamat Datang, $name!", Toast.LENGTH_SHORT).show()

                        // =========================================================================
                        // 🔐 KUNCI PROSES: Simpan data email ke SharedPreferences sebagai Session
                        // =========================================================================
                        val sharedPref = getSharedPreferences("SIPTA_SESSION", Context.MODE_PRIVATE)
                        val editor = sharedPref.edit()
                        editor.putString("email_login", email) // Menyimpan email akun terlogin secara global
                        editor.apply()

                        // Menentukan Halaman Dashboard berdasarkan role dari database MySQL pusat
                        val targetClass = when (role.trim().lowercase()) {
                            "admin" -> MainActivityAdmin::class.java
                            "owner" -> MainActivityOwner::class.java
                            "kasir" -> MainActivityKasir::class.java
                            else -> MainActivityKasir::class.java
                        }

                        val intent = Intent(this, targetClass)

                        // Masukkan data profil ke dalam Intent sebagai data bawaan hantar halaman
                        intent.putExtra("USER_NAME", name)
                        intent.putExtra("USER_EMAIL", email)
                        intent.putExtra("USER_ROLE", role)

                        startActivity(intent)
                        finish() // Tutup LoginActivity agar tidak bisa di-back
                    } else {
                        Toast.makeText(this, "Email atau Password Salah!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Parsing Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            Response.ErrorListener { error ->
                Toast.makeText(this, "Tidak dapat terhubung ke server: ${error.message}", Toast.LENGTH_LONG).show()
            }) {

            // Kirim parameter data login ke file PHP
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["email"] = emailInput
                params["password"] = passwordInput
                return params
            }
        }

        // Jalankan antrean Volley
        val queue = Volley.newRequestQueue(this)
        queue.add(request)
    }
}