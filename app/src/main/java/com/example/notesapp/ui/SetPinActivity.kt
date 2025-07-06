package com.example.notesapp.ui

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.notesapp.R // Pastikan ini diimpor untuk mengakses R.drawable dan R.string
import com.example.notesapp.databinding.ActivitySetPinBinding // Sesuaikan dengan nama layout XML Anda
import com.example.notesapp.util.PinManager // Asumsikan ini adalah utilitas untuk menyimpan PIN

class SetPinActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetPinBinding
    private val enteredPin = StringBuilder() // Untuk menyimpan PIN yang sedang dimasukkan pengguna
    private var firstPin: String? = null // Untuk menyimpan PIN yang dimasukkan pertama kali

    // Anda bisa menyesuaikan panjang PIN di sini
    private val PIN_LENGTH = 4

    // Enum untuk mengelola state input PIN
    private enum class PinInputState {
        ENTER_NEW_PIN,
        CONFIRM_NEW_PIN
    }

    private var currentState: PinInputState = PinInputState.ENTER_NEW_PIN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetPinBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // --- Inisialisasi Tampilan Awal ---
        updatePinDots() // Semua dot kosong saat awal
        binding.tvPinInputTitle.text = getString(R.string.enter_your_new_pin) // Set judul awal

        // --- Listener untuk Tombol "Back" (Panah + Teks) ---
        binding.backArrow.setOnClickListener {
            // Jika pengguna menekan back saat di tahap konfirmasi, kembali ke tahap enter PIN
            if (currentState == PinInputState.CONFIRM_NEW_PIN) {
                firstPin = null // Reset PIN pertama
                enteredPin.clear() // Hapus input saat ini
                updatePinDots() // Reset dot tampilan
                currentState = PinInputState.ENTER_NEW_PIN // Kembali ke fase input pertama
                binding.tvPinInputTitle.text = getString(R.string.enter_your_new_pin)
            } else {
                // Jika di tahap enter PIN, atau setelah PIN berhasil diatur, finish activity
                setResult(Activity.RESULT_CANCELED) // Mengindikasikan pembatalan
                finish()
            }
        }
        binding.backText.setOnClickListener {
            // Sama dengan backArrow
            binding.backArrow.performClick()
        }

        // --- Listener untuk Tombol Keypad Numerik ---
        setupNumberPadListeners()

        // --- Listener untuk Tombol Backspace ---
        binding.btnBackspace.setOnClickListener {
            if (enteredPin.isNotEmpty()) {
                enteredPin.deleteCharAt(enteredPin.length - 1)
                updatePinDots()
            }
        }
    }

    private fun setupNumberPadListeners() {
        val numberButtons = arrayOf(
            binding.btn0, binding.btn1, binding.btn2, binding.btn3,
            binding.btn4, binding.btn5, binding.btn6, binding.btn7,
            binding.btn8, binding.btn9
        )

        for (button in numberButtons) {
            button.setOnClickListener {
                val digit = (it as TextView).text.toString()
                if (enteredPin.length < PIN_LENGTH) {
                    enteredPin.append(digit)
                    updatePinDots()
                    // Otomatis memproses PIN setelah mencapai panjang yang ditentukan
                    if (enteredPin.length == PIN_LENGTH) {
                        handlePinCompletion()
                    }
                }
            }
        }
    }

    private fun updatePinDots() {
        val pinDots = arrayOf(binding.pinDot1, binding.pinDot2, binding.pinDot3, binding.pinDot4)

        for (i in 0 until PIN_LENGTH) {
            if (i < enteredPin.length) {
                pinDots[i].setBackgroundResource(R.drawable.pin_dot_filled)
            } else {
                pinDots[i].setBackgroundResource(R.drawable.pin_dot_empty)
            }
        }
    }

    private fun handlePinCompletion() {
        when (currentState) {
            PinInputState.ENTER_NEW_PIN -> {
                val currentPinInput = enteredPin.toString()

                // Validasi panjang PIN (Anda sudah punya logika ini)
                if (currentPinInput.length < PIN_LENGTH) {
                    // Ini seharusnya tidak terpanggil jika PIN_LENGTH sudah terpenuhi,
                    // tapi sebagai fallback
                    Toast.makeText(this, "PIN harus minimal $PIN_LENGTH digit", Toast.LENGTH_SHORT).show()
                    enteredPin.clear()
                    updatePinDots()
                    return
                }

                firstPin = currentPinInput // Simpan PIN pertama
                enteredPin.clear() // Reset untuk input konfirmasi
                updatePinDots() // Reset dot tampilan
                currentState = PinInputState.CONFIRM_NEW_PIN // Pindah ke state konfirmasi
                binding.tvPinInputTitle.text = getString(R.string.confirm_your_new_pin) // Ubah judul
                Toast.makeText(this, "PIN pertama sudah direkam. Masukkan ulang PIN untuk konfirmasi.", Toast.LENGTH_LONG).show()
            }

            PinInputState.CONFIRM_NEW_PIN -> {
                val confirmedPin = enteredPin.toString()

                if (firstPin == confirmedPin) {
                    // PIN cocok! Simpan menggunakan PinManager
                    PinManager.savePin(this, firstPin!!)
                    Toast.makeText(this, "PIN berhasil diatur dan disimpan!", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK) // Mengindikasikan berhasil
                    finish()
                } else {
                    // PIN tidak cocok
                    Toast.makeText(this, "PIN tidak cocok. Silakan coba lagi dari awal.", Toast.LENGTH_LONG).show()
                    firstPin = null // Reset PIN pertama
                    enteredPin.clear() // Hapus PIN yang dimasukkan saat ini
                    updatePinDots() // Reset dot tampilan
                    currentState = PinInputState.ENTER_NEW_PIN // Kembali ke fase input pertama
                    binding.tvPinInputTitle.text = getString(R.string.enter_your_new_pin) // Kembalikan judul
                }
            }
        }
    }
}