package com.example.sipta

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.example.sipta.databinding.ActivityMainKasirBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView

class MainActivityKasir : AppCompatActivity(), NavigationBarView.OnItemSelectedListener {
    private lateinit var binding: ActivityMainKasirBinding
    private lateinit var db: SQLiteDatabase

    private lateinit var fragDashboard: FragmentDashboardKasir
    private lateinit var fragPOS: FragmentPOS
    private lateinit var fragRiwayat: FragmentRiwayat

    private var loggedInName: String? = null
    private var loggedInEmail: String? = null
    private var loggedInRole: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainKasirBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = DBOpenHelper(this).writableDatabase

        fragDashboard = FragmentDashboardKasir()
        fragPOS = FragmentPOS()
        fragRiwayat = FragmentRiwayat()

        binding.bottomNavigationView.setOnItemSelectedListener(this)

        loggedInName = intent.getStringExtra("USER_NAME")
        loggedInEmail = intent.getStringExtra("USER_EMAIL")
        loggedInRole = intent.getStringExtra("USER_ROLE")

        val navView: BottomNavigationView = findViewById(R.id.bottomNavigationView)
        navView.itemIconTintList = null

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        binding.layoutWelcome.visibility = View.GONE
        binding.frameKasir.visibility = View.VISIBLE

        when (item.itemId) {
            R.id.itemDashboardKasir -> {
                loadFragment(fragDashboard)
                return true
            }
            R.id.POS -> {
                loadFragment(fragPOS)
                return true
            }
            R.id.itemRiwayat -> {
                loadFragment(fragRiwayat)
                return true
            }
        }
        return false
    }

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

    private fun showProfileDialog() {
        val name = loggedInName ?: "Tamu"
        val email = loggedInEmail ?: "-"
        val role = loggedInRole ?: "-"

        AlertDialog.Builder(this)
            .setTitle("Profil Pengguna")
            .setMessage("Nama: $name\nUsername: $email\nLevel: $role")
            .setPositiveButton("Tutup", null)
            .show()
    }

    private fun loadFragment(fragment: Fragment) {
        val ft: FragmentTransaction = supportFragmentManager.beginTransaction()
        ft.replace(R.id.frameKasir, fragment)
        ft.commit()
    }

    fun getDbObject(): SQLiteDatabase {
        return db
    }
}
