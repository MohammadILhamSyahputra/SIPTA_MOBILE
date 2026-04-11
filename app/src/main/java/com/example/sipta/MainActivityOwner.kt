package com.example.sipta

import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.example.sipta.databinding.ActivityMainOwnerBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView

class MainActivityOwner : AppCompatActivity(), NavigationBarView.OnItemSelectedListener {

    private lateinit var binding: ActivityMainOwnerBinding
    private lateinit var db: SQLiteDatabase

    // Fragment Owner
    private lateinit var fragDashboard: FragmentDashboardOwner
    private lateinit var fragBarangTerlaris: FragmentBarangTerlarisOwner
    private lateinit var fragPenjualan: FragmentLapPenjualanOwner
    private lateinit var fragRiwayat: FragmentRiwayatSalesOwner
    private lateinit var fragUser: FragmentKelolaUser

    // Data login
    private var loggedInName: String? = null
    private var loggedInEmail: String? = null
    private var loggedInRole: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainOwnerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = DBOpenHelper(this).writableDatabase

        fragDashboard = FragmentDashboardOwner()
        fragBarangTerlaris = FragmentBarangTerlarisOwner()
        fragPenjualan = FragmentLapPenjualanOwner()
        fragRiwayat = FragmentRiwayatSalesOwner()
        fragUser = FragmentKelolaUser()

        binding.bottomNavOwner.setOnItemSelectedListener(this)

        // Ambil data login
        loggedInName = intent.getStringExtra("USER_NAME")
        loggedInEmail = intent.getStringExtra("USER_EMAIL")
        loggedInRole = intent.getStringExtra("USER_ROLE")

        val navView: BottomNavigationView = findViewById(R.id.bottomNavOwner)
        navView.itemIconTintList = null

        // Insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // Navigasi menu owner
    override fun onNavigationItemSelected(item: MenuItem): Boolean {

        // Sembunyikan welcome jika ada
        binding.frameOwner.visibility = View.VISIBLE

        when (item.itemId) {

            R.id.itemDashboardOwner -> {
                loadFragment(fragDashboard)
                return true
            }

            R.id.itemBarangTerlaris -> {
                loadFragment(fragBarangTerlaris)
                return true
            }

            R.id.itemPenjualan -> {
                loadFragment(fragPenjualan)
                return true
            }

            R.id.itemRiwayat -> {
                loadFragment(fragRiwayat)
                return true
            }

            R.id.itemUser -> {
                loadFragment(fragUser)
                return true
            }
        }
        return false
    }

    // Menu atas (profile & logout)
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            R.id.menu_profile -> {
                showProfileDialog()
                return true
            }

            R.id.menu_logout -> {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    // Popup profile
    private fun showProfileDialog() {
        val name = loggedInName ?: "Tamu"
        val email = loggedInEmail ?: "-"
        val role = loggedInRole ?: "-"

        AlertDialog.Builder(this)
            .setTitle("Profil Owner")
            .setMessage("Nama: $name\nEmail: $email\nLevel: $role")
            .setPositiveButton("Tutup", null)
            .show()
    }

    // Load fragment
    private fun loadFragment(fragment: Fragment) {
        val ft: FragmentTransaction = supportFragmentManager.beginTransaction()
        ft.replace(R.id.frameOwner, fragment)
        ft.commit()
    }

    // Akses DB ke fragment
    fun getDbObject(): SQLiteDatabase {
        return db
    }
}