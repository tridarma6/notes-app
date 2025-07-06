package com.example.notesapp.ui

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.notesapp.R
import com.example.notesapp.databinding.ActivitySetPinBinding
import com.example.notesapp.util.PinManager

class SetPinActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetPinBinding
    private val enteredPin = StringBuilder()
    private var firstPin: String? = null // Untuk menyimpan PIN baru yang dimasukkan pertama kali
    private val PIN_LENGTH = 4 // Panjang PIN yang diharapkan

    // Enum untuk mengelola state input PIN
    private enum class PinInputState {
        VERIFY_OLD_PIN, // State baru: Verifikasi PIN lama
        ENTER_NEW_PIN,
        CONFIRM_NEW_PIN
    }

    private var currentState: PinInputState = PinInputState.VERIFY_OLD_PIN // State awal

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetPinBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Tentukan state awal berdasarkan apakah PIN sudah diatur atau belum
        if (PinManager.isPinSet(this)) {
            currentState = PinInputState.VERIFY_OLD_PIN
            binding.tvPinInputTitle.text = getString(R.string.enter_your_old_pin)
        } else {
            // Jika belum ada PIN lama, langsung ke pengaturan PIN baru
            currentState = PinInputState.ENTER_NEW_PIN
            binding.tvPinInputTitle.text = getString(R.string.enter_your_new_pin)
        }

        updatePinDots() // Semua dot kosong saat awal

        // --- Listener untuk Tombol "Back" (Panah + Teks) ---
        binding.backArrow.setOnClickListener {
            when (currentState) {
                PinInputState.VERIFY_OLD_PIN -> {
                    // Jika di tahap verifikasi PIN lama, langsung batal dan kembali
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                }
                PinInputState.CONFIRM_NEW_PIN -> {
                    // Jika di tahap konfirmasi PIN baru, kembali ke tahap enter PIN baru
                    enteredPin.clear()
                    updatePinDots()
                    currentState = PinInputState.ENTER_NEW_PIN
                    binding.tvPinInputTitle.text = getString(R.string.enter_your_new_pin)
                    Toast.makeText(this, "Silakan masukkan PIN baru Anda lagi.", Toast.LENGTH_SHORT).show()
                }
                PinInputState.ENTER_NEW_PIN -> {
                    // Jika di tahap enter PIN baru, kembali ke tahap verifikasi PIN lama
                    // Hanya jika ada PIN lama yang tersimpan
                    if (PinManager.isPinSet(this)) {
                        enteredPin.clear()
                        updatePinDots()
                        currentState = PinInputState.VERIFY_OLD_PIN
                        binding.tvPinInputTitle.text = getString(R.string.enter_your_old_pin)
                        Toast.makeText(this, "Silakan masukkan PIN lama Anda lagi.", Toast.LENGTH_SHORT).show()
                    } else {
                        // Jika tidak ada PIN lama, ini adalah pengaturan pertama, jadi batal saja
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    }
                }
            }
        }
        binding.backText.setOnClickListener {
            binding.backArrow.performClick() // Panggil listener backArrow
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
        val currentInput = enteredPin.toString()

        when (currentState) {
            PinInputState.VERIFY_OLD_PIN -> {
                val oldPin = PinManager.getPin(this)
                if (oldPin != null && currentInput == oldPin) {
                    // PIN lama cocok, lanjutkan ke pengaturan PIN baru
                    enteredPin.clear()
                    updatePinDots()
                    currentState = PinInputState.ENTER_NEW_PIN
                    binding.tvPinInputTitle.text = getString(R.string.enter_your_new_pin)
                    Toast.makeText(this, "PIN lama cocok. Silakan masukkan PIN baru Anda.", Toast.LENGTH_SHORT).show()
                } else {
                    // PIN lama tidak cocok
                    Toast.makeText(this, "PIN lama salah. Silakan coba lagi.", Toast.LENGTH_LONG).show()
                    enteredPin.clear()
                    updatePinDots()
                }
            }
            PinInputState.ENTER_NEW_PIN -> {
                // Validasi panjang PIN
                if (currentInput.length < PIN_LENGTH) {
                    Toast.makeText(this, "PIN harus minimal $PIN_LENGTH digit", Toast.LENGTH_SHORT).show()
                    enteredPin.clear()
                    updatePinDots()
                    return
                }

                firstPin = currentInput // Simpan PIN baru yang pertama
                enteredPin.clear() // Reset untuk input konfirmasi
                updatePinDots() // Reset dot tampilan
                currentState = PinInputState.CONFIRM_NEW_PIN // Pindah ke state konfirmasi
                binding.tvPinInputTitle.text = getString(R.string.confirm_your_new_pin) // Ubah judul
                Toast.makeText(this, "PIN baru sudah direkam. Masukkan ulang PIN untuk konfirmasi.", Toast.LENGTH_LONG).show()
            }
            PinInputState.CONFIRM_NEW_PIN -> {
                if (firstPin == currentInput) {
                    // PIN cocok! Simpan menggunakan PinManager
                    PinManager.savePin(this, firstPin!!)
                    Toast.makeText(this, "PIN berhasil diatur dan disimpan!", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK) // Mengindikasikan berhasil
                    finish()
                } else {
                    // PIN tidak cocok
                    Toast.makeText(this, "PIN baru tidak cocok. Silakan coba lagi dari awal.", Toast.LENGTH_LONG).show()
                    firstPin = null // Reset PIN pertama
                    enteredPin.clear() // Hapus PIN yang dimasukkan saat ini
                    updatePinDots() // Reset dot tampilan
                    // Kembali ke state verifikasi PIN lama jika ada, atau langsung ke enter PIN baru
                    if (PinManager.isPinSet(this)) {
                        currentState = PinInputState.VERIFY_OLD_PIN
                        binding.tvPinInputTitle.text = getString(R.string.enter_your_old_pin)
                    } else {
                        currentState = PinInputState.ENTER_NEW_PIN
                        binding.tvPinInputTitle.text = getString(R.string.enter_your_new_pin)
                    }
                }
            }
        }
    }
}