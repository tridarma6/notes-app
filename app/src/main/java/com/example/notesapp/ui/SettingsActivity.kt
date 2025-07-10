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

        setPinLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, "PIN berhasil diatur/diperbarui.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Pengaturan PIN dibatalkan.", Toast.LENGTH_SHORT).show()
            }
        }

        val sharedPrefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val isDarkMode = sharedPrefs.getBoolean("dark_mode", false)
        binding.switchDarkMode.isChecked = isDarkMode

        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode)
                AppCompatDelegate.MODE_NIGHT_YES
            else
                AppCompatDelegate.MODE_NIGHT_NO
        )

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_notes -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                R.id.menu_event -> {
                    val intent = Intent(this, EventActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.menu_setting -> {
                    true
                }
                else -> false
            }
        }
        binding.bottomNavigation.selectedItemId = R.id.menu_setting

        // --- Logika Toggle Dark Mode ---
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("dark_mode", isChecked).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked)
                    AppCompatDelegate.MODE_NIGHT_YES
                else
                    AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        binding.darkModeSettingRow.setOnClickListener {
            binding.switchDarkMode.toggle() // Mengalihkan status SwitchCompat
        }


        binding.setPinSettingRow.setOnClickListener {
            val intent = Intent(this, SetPinActivity::class.java)
            setPinLauncher.launch(intent)
        }
    }
}