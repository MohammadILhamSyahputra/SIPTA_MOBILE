package com.example.sipta

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.database.sqlite.SQLiteDatabase
import android.view.MenuItem
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.example.sipta.databinding.ActivityMainAdminBinding
import android.content.Intent // Untuk berpindah Activity
import android.view.Menu // Untuk mendefinisikan menu
import androidx.appcompat.app.AlertDialog // Untuk menampilkan popup profil
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView

class MainActivityAdmin : AppCompatActivity(), NavigationBarView.OnItemSelectedListener {
    private lateinit var binding: ActivityMainAdminBinding
    private lateinit var db: SQLiteDatabase

    private lateinit var fragDashboard: FragmentDashboardAdmin
    private lateinit var fragBarang: FragmentBarang
    private lateinit var fragKategori: FragmentKategori
    private lateinit var fragSales: FragmentSales

    // Variabel untuk menampung data login
    private var loggedInName: String? = null
    private var loggedInEmail: String? = null
    private var loggedInRole: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inisialisasi ViewBinding
        binding = ActivityMainAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inisialisasi Database SIPTA [cite: 1297]
        db = DBOpenHelper(this).writableDatabase

        // Inisialisasi Semua Fragment
        fragDashboard = FragmentDashboardAdmin()
        fragBarang = FragmentBarang()
        fragKategori = FragmentKategori()
        fragSales = FragmentSales()

        // Set Listener untuk Bottom Navigation [cite: 1289]
        binding.bottomNavigationView.setOnItemSelectedListener(this)

        loggedInName = intent.getStringExtra("USER_NAME")
        loggedInEmail = intent.getStringExtra("USER_EMAIL")
        loggedInRole = intent.getStringExtra("USER_ROLE")

        val navView: BottomNavigationView = findViewById(R.id.bottomNavigationView)
        navView.itemIconTintList = null
//        setContentView(R.layout.activity_main_admin)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
    // Fungsi untuk menangani klik pada menu BottomNav [cite: 1301, 1302]
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Sembunyikan Logo dan Tulisan Selamat Datang saat Fragment aktif
        binding.layoutWelcome.visibility = View.GONE
        binding.fragmentContainer.visibility = View.VISIBLE

        when (item.itemId) {
            R.id.itemDashboard -> {
                loadFragment(fragDashboard)
                return true
            }
            R.id.itemBarang -> {
                loadFragment(fragBarang)
                return true
            }
            R.id.itemKategori -> {
                loadFragment(fragKategori)
                return true
            }
            R.id.itemSales -> {
                loadFragment(fragSales)
                return true
            }
        }
        return false
    }

    // Menampilkan Menu Titik Tiga
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_menu, menu)
        return true
    }

    // Menangani Klik pada Menu
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_profile -> {
                showProfileDialog()
                return true
            }
            R.id.menu_logout -> {
                // Kembali ke Login
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    // Fungsi Menampilkan Profile (Hanya Lihat)
    private fun showProfileDialog() {
        // Gunakan data yang ditangkap dari login, jika null gunakan default
        val name = loggedInName ?: "Tamu"
        val email = loggedInEmail ?: "-"
        val role = loggedInRole ?: "-"

        AlertDialog.Builder(this)
            .setTitle("Profil Pengguna")
            .setMessage("Nama: $name\nUsername: $email\nLevel: $role")
            .setPositiveButton("Tutup", null)
            .show()
    }

    // Fungsi helper untuk menukar fragment [cite: 1275, 1304, 1305]
    private fun loadFragment(fragment: Fragment) {
        val ft: FragmentTransaction = supportFragmentManager.beginTransaction()
        ft.replace(R.id.fragment_container, fragment)
        ft.commit()
    }

    // Memberikan akses database ke Fragment-Fragment [cite: 1298, 1299]
    fun getDbObject(): SQLiteDatabase {
        return db
    }
}