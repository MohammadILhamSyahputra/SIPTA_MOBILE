package com.example.sipta

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.sipta.databinding.ActivityFragmentKelolaUserBinding
import android.view.ViewGroup
import android.widget.*
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject


class FragmentKelolaUser : Fragment(R.layout.activity_fragment_kelola_user) {

    private var _binding: ActivityFragmentKelolaUserBinding? = null
    private val binding get() = _binding!!

    // URL Web Service Laragon Kelola User
    private val urlUser = "http://192.168.1.127/sipta_api/crud_kelola_user.php"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = ActivityFragmentKelolaUserBinding.bind(view)

        tampilkanDataUser()

        binding.btnTambahUser.setOnClickListener {
            showAddUserDialog()
        }
    }

    private fun tampilkanDataUser() {
        val request = object : StringRequest(Request.Method.POST, urlUser,
            Response.Listener { response ->
                if (isAdded && activity != null) {
                    try {
                        // Bersihkan row lama kecuali header TableRow index ke-0
                        val childCount = binding.containerKelolaUser.childCount
                        if (childCount > 1) {
                            binding.containerKelolaUser.removeViews(1, childCount - 1)
                        }

                        // =========================================================================
                        // 🟢 AMBIL EMAIL SESSION YANG SEDANG LOGIN DARI SHAREDPREFERENCES
                        // =========================================================================
                        val sharedPref = requireActivity().getSharedPreferences("SIPTA_SESSION", android.content.Context.MODE_PRIVATE)
                        val emailAktifMilikUser = sharedPref.getString("email_login", "") ?: ""

                        val jsonArray = JSONArray(response)
                        for (x in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(x)
                            val uData = UserPusat(
                                obj.getInt("id"),
                                obj.getString("username"),
                                obj.getString("email"),
                                obj.getString("level")
                            )

                            // Render row layout dinamis
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
                                text = uData.username
                                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.2f)
                                textSize = 14f
                                setPadding(10, 0, 5, 0)
                            }

                            val tvEmail = TextView(requireContext()).apply {
                                text = uData.email
                                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.5f)
                                textSize = 12f
                                setPadding(5, 0, 5, 0)
                            }

                            val tvLevel = TextView(requireContext()).apply {
                                text = uData.level.uppercase()
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
                                layoutParams = LinearLayout.LayoutParams(80, 80)
                                setOnClickListener { showEditRoleDialog(uData) }
                            }

                            val btnDelete = ImageButton(requireContext()).apply {
                                setImageResource(android.R.drawable.ic_menu_delete)
                                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                layoutParams = LinearLayout.LayoutParams(80, 80)
                                setOnClickListener { showDeleteDialog(uData) }
                            }

                            // =========================================================================
                            // 🔐 PERBAIKAN LOGIKA: Hanya kunci jika email di tabel == email session login
                            // =========================================================================
                            if (uData.email.lowercase() == emailAktifMilikUser.lowercase()) {
                                btnEdit.isEnabled = false
                                btnEdit.alpha = 0.3f // Membuat ikon pudar tanda terkunci

                                btnDelete.isEnabled = false
                                btnDelete.alpha = 0.3f
                            }

                            actionLayout.addView(btnEdit)
                            actionLayout.addView(btnDelete)

                            row.addView(tvName)
                            row.addView(tvEmail)
                            row.addView(tvLevel)
                            row.addView(actionLayout)

                            binding.containerKelolaUser.addView(row)
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
            },
            Response.ErrorListener {
                if (isAdded) Toast.makeText(requireContext(), "Gagal memuat data pengguna", Toast.LENGTH_SHORT).show()
            }) {
            override fun getParams(): MutableMap<String, String> = hashMapOf("mode" to "show")
        }
        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun showAddUserDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Tambah User Baru")

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
            val nama = etNama.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val pass = etPass.text.toString().trim()
            val role = spinnerRole.selectedItem.toString()

            if (nama.isNotEmpty() && email.isNotEmpty() && pass.isNotEmpty()) {
                val reqInsert = object : StringRequest(Request.Method.POST, urlUser,
                    Response.Listener { response ->
                        try {
                            val jsonRes = JSONObject(response)
                            val kode = jsonRes.getString("kode")
                            if (kode == "000") {
                                Toast.makeText(context, "User berhasil ditambah!", Toast.LENGTH_SHORT).show()
                                tampilkanDataUser()
                            } else if (kode == "444") {
                                Toast.makeText(context, "Gagal! Email sudah terdaftar", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Gagal menambahkan user", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) { e.printStackTrace() }
                    }, Response.ErrorListener {}) {
                    override fun getParams(): MutableMap<String, String> = hashMapOf(
                        "mode" to "insert",
                        "username" to nama,
                        "email" to email,
                        "password" to pass,
                        "level" to role
                    )
                }
                Volley.newRequestQueue(requireContext()).add(reqInsert)
            } else {
                Toast.makeText(context, "Semua field harus diisi!", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Batal", null)
        builder.show()
    }

    private fun showEditRoleDialog(user: UserPusat) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Ubah Role ${user.username}")

        val rgRole = RadioGroup(requireContext()).apply {
            orientation = RadioGroup.VERTICAL
            setPadding(60, 40, 60, 40)
        }

        val roles = arrayOf("owner", "admin", "kasir")
        for (roleName in roles) {
            val rb = RadioButton(requireContext()).apply {
                text = roleName
                id = View.generateViewId()
                if (user.level.lowercase() == roleName) isChecked = true
            }
            rgRole.addView(rb)
        }

        builder.setView(rgRole)

        builder.setPositiveButton("Update") { _, _ ->
            val selectedId = rgRole.checkedRadioButtonId
            if (selectedId == -1) return@setPositiveButton

            val radioButton = rgRole.findViewById<RadioButton>(selectedId)
            val newRole = radioButton.text.toString()

            val reqUpdate = object : StringRequest(Request.Method.POST, urlUser,
                Response.Listener { response ->
                    try {
                        val jsonRes = JSONObject(response)
                        if (jsonRes.getString("kode") == "000") {
                            Toast.makeText(requireContext(), "Role diperbarui!", Toast.LENGTH_SHORT).show()
                            tampilkanDataUser()
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }, Response.ErrorListener {}) {
                override fun getParams(): MutableMap<String, String> = hashMapOf(
                    "mode" to "update_role",
                    "id" to user.id.toString(),
                    "level" to newRole
                )
            }
            Volley.newRequestQueue(requireContext()).add(reqUpdate)
        }
        builder.setNegativeButton("Batal", null)
        builder.show()
    }

    private fun showDeleteDialog(user: UserPusat) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus User")
            .setMessage("Yakin ingin menghapus ${user.username}?")
            .setPositiveButton("Ya") { _, _ ->
                val reqDelete = object : StringRequest(Request.Method.POST, urlUser,
                    Response.Listener { response ->
                        try {
                            val jsonRes = JSONObject(response)
                            if (jsonRes.getString("kode") == "000") {
                                // PERBAIKAN: Mengubah 'context' menjadi 'requireContext()'
                                Toast.makeText(requireContext(), "User dihapus", Toast.LENGTH_SHORT).show()
                                tampilkanDataUser()
                            }
                        } catch (e: Exception) { e.printStackTrace() }
                    }, Response.ErrorListener {}) {
                    override fun getParams(): MutableMap<String, String> = hashMapOf(
                        "mode" to "delete",
                        "id" to user.id.toString()
                    )
                }
                Volley.newRequestQueue(requireContext()).add(reqDelete)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}