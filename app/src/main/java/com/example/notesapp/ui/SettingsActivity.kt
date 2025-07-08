package com.example.notesapp.ui

import com.example.notesapp.R
import android.app.Activity // Import ini untuk Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.activity.result.ActivityResultLauncher // BARU
import androidx.activity.result.contract.ActivityResultContracts // BARU
import com.example.notesapp.databinding.ActivitySettingsBinding
import com.google.android.material.bottomnavigation.BottomNavigationView // Pastikan ini diimpor

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var setPinLauncher: ActivityResultLauncher<Intent> // BARU: Untuk meluncurkan SetPinActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // --- BARU: Inisialisasi ActivityResultLauncher untuk SetPinActivity ---
        setPinLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, "PIN berhasil diatur/diperbarui.", Toast.LENGTH_SHORT).show()
                // Anda bisa melakukan aksi lain di sini setelah PIN berhasil diatur
            } else {
                Toast.makeText(this, "Pengaturan PIN dibatalkan.", Toast.LENGTH_SHORT).show()
            }
        }
        // --- AKHIR BARU ---

        // Load saved dark mode state
        val sharedPrefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val isDarkMode = sharedPrefs.getBoolean("dark_mode", false)
        binding.switchDarkMode.isChecked = isDarkMode

        // Set initial dark mode state
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode)
                AppCompatDelegate.MODE_NIGHT_YES
            else
                AppCompatDelegate.MODE_NIGHT_NO
        )

        // --- Bottom Navigation (Tidak Berubah) ---
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_notes -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                R.id.menu_setting -> {
                    // Sudah di halaman settings
                    true
                }
                else -> false
            }
        }
        binding.bottomNavigation.selectedItemId = R.id.menu_setting

        // --- Logika Toggle Dark Mode ---
        // Listener pada SwitchCompat itu sendiri tetap ada (untuk kasus jika pengguna mengklik langsung switch)
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("dark_mode", isChecked).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked)
                    AppCompatDelegate.MODE_NIGHT_YES
                else
                    AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        // --- BARU: Listener untuk seluruh baris Dark Mode (darkModeSettingRow) ---
        // Ini akan memicu toggle switch saat baris diklik
        binding.darkModeSettingRow.setOnClickListener {
            binding.switchDarkMode.toggle() // Mengalihkan status SwitchCompat
        }
        // --- AKHIR BARU ---


        // --- BARU: Logika Set PIN ---
        // Tombol 'btnSetPin' tidak lagi ada di XML baru.
        // Sekarang, seluruh baris 'setPinSettingRow' yang dapat diklik.
        binding.setPinSettingRow.setOnClickListener {
            val intent = Intent(this, SetPinActivity::class.java)
            setPinLauncher.launch(intent) // Gunakan launcher baru
        }
        // --- AKHIR BARU ---
    }
}