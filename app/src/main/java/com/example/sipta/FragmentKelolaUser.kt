package com.example.sipta

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.sipta.databinding.ActivityFragmentKelolaUserBinding

class FragmentKelolaUser : Fragment(R.layout.activity_fragment_kelola_user) {

    private var _binding: ActivityFragmentKelolaUserBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbHelper: DBOpenHelper

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = ActivityFragmentKelolaUserBinding.bind(view)
        dbHelper = DBOpenHelper(requireContext())

        tampilkanDataUser()

        binding.btnTambahUser.setOnClickListener {
            showAddUserDialog()
        }
    }

    private fun tampilkanDataUser() {
        val childCount = binding.containerKelolaUser.childCount
        if (childCount > 1) {
            binding.containerKelolaUser.removeViews(1, childCount - 1)
        }

        val listUser = dbHelper.getAllUsers()

        for (user in listUser) {
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(0, 20, 0, 20)
                gravity = android.view.Gravity.CENTER_VERTICAL
                setBackgroundResource(android.R.drawable.divider_horizontal_bright)
            }

            val tvName = TextView(requireContext()).apply {
                text = user.username
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.2f)
                textSize = 14f
                setPadding(10, 0, 5, 0)
            }

            val tvEmail = TextView(requireContext()).apply {
                text = user.email
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.5f)
                textSize = 12f
                setPadding(5, 0, 5, 0)
            }

            val tvLevel = TextView(requireContext()).apply {
                text = user.level.uppercase()
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.8f)
                textSize = 12f
                setPadding(5, 0, 5, 0)
            }

            val actionLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.7f)
                gravity = android.view.Gravity.CENTER
            }

            val btnEdit = ImageButton(requireContext()).apply {
                setImageResource(android.R.drawable.ic_menu_edit)
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                layoutParams = LinearLayout.LayoutParams(80, 80) // Ukuran pixel
                setOnClickListener { showEditRoleDialog(user) }
            }

            val btnDelete = ImageButton(requireContext()).apply {
                setImageResource(android.R.drawable.ic_menu_delete)
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                layoutParams = LinearLayout.LayoutParams(80, 80)
                setOnClickListener { showDeleteDialog(user) }
            }

            actionLayout.addView(btnEdit)
            actionLayout.addView(btnDelete)

            row.addView(tvName)
            row.addView(tvEmail)
            row.addView(tvLevel)
            row.addView(actionLayout)

            binding.containerKelolaUser.addView(row)
        }
    }
    // FORM TAMBAH USER (DIALOG)
    private fun showAddUserDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Tambah User Baru")

        // Buat View secara programmatically atau inflate layout custom
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val etNama = EditText(requireContext()).apply { hint = "Nama Lengkap" }
        val etEmail = EditText(requireContext()).apply { hint = "Email" }
        val etPass = EditText(requireContext()).apply {
            hint = "Password"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        val spinnerRole = Spinner(requireContext())
        val roles = arrayOf("owner", "admin", "kasir")
        spinnerRole.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, roles)

        layout.addView(etNama)
        layout.addView(etEmail)
        layout.addView(etPass)
        layout.addView(TextView(requireContext()).apply { text = "Pilih Role:"; setPadding(0, 20, 0, 0) })
        layout.addView(spinnerRole)

        builder.setView(layout)

        builder.setPositiveButton("Simpan") { _, _ ->
            val nama = etNama.text.toString()
            val email = etEmail.text.toString()
            val pass = etPass.text.toString()
            val role = spinnerRole.selectedItem.toString()

            if (nama.isNotEmpty() && email.isNotEmpty() && pass.isNotEmpty()) {
                val result = dbHelper.addUser(nama, email, pass, role)
                if (result != -1L) {
                    Toast.makeText(context, "User berhasil ditambah!", Toast.LENGTH_SHORT).show()
                    tampilkanDataUser()
                } else {
                    Toast.makeText(context, "Gagal! Email mungkin sudah terdaftar", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Semua field harus diisi!", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Batal", null)
        builder.show()
    }

    private fun showEditRoleDialog(user: User) {
        val roles = arrayOf("owner", "admin", "kasir")
        AlertDialog.Builder(requireContext())
            .setTitle("Ubah Role ${user.username}")
            .setItems(roles) { _, which ->
                dbHelper.updateUserRole(user.id, roles[which])
                tampilkanDataUser()
                Toast.makeText(context, "Role diperbarui!", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showDeleteDialog(user: User) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus User")
            .setMessage("Yakin ingin menghapus ${user.username}?")
            .setPositiveButton("Ya") { _, _ ->
                dbHelper.deleteUser(user.id)
                tampilkanDataUser()
                Toast.makeText(context, "User dihapus", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}