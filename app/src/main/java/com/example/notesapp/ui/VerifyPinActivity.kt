package com.example.notesapp.ui

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.notesapp.R // Pastikan ini diimpor untuk mengakses R.drawable
import com.example.notesapp.databinding.ActivityVerifyPinBinding
import com.example.notesapp.util.PinManager

class VerifyPinActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVerifyPinBinding
    private val enteredPin = StringBuilder() // Untuk menyimpan PIN yang dimasukkan
    private val PIN_LENGTH = 4 // Panjang PIN yang diharapkan

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerifyPinBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Mengatur listener untuk tombol "Back" (panah + teks)
        binding.backArrow.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
        binding.backText.setOnClickListener { // Juga klik pada teks "Back"
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        // Mengatur listener untuk semua tombol keypad numerik
        setupNumberPadListeners()

        // Mengatur listener untuk tombol backspace
        binding.btnBackspace.setOnClickListener {
            if (enteredPin.isNotEmpty()) {
                enteredPin.deleteCharAt(enteredPin.length - 1)
                updatePinDots()
            }
        }

        // Memperbarui tampilan dot PIN awal (semuanya kosong)
        updatePinDots()
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
                        verifyPin()
                    }
                }
            }
        }
    }

    private fun updatePinDots() {
        val pinDots = arrayOf(binding.pinDot1, binding.pinDot2, binding.pinDot3, binding.pinDot4)

        for (i in 0 until PIN_LENGTH) {
            if (i < enteredPin.length) {
                // Dot sudah terisi
                pinDots[i].setBackgroundResource(R.drawable.pin_dot_filled)
            } else {
                // Dot masih kosong
                pinDots[i].setBackgroundResource(R.drawable.pin_dot_empty)
            }
        }
    }

    private fun verifyPin() {
        val inputPin = enteredPin.toString()
        val savedPin = PinManager.getPin(this)

        if (savedPin == null) {
            Toast.makeText(this, "PIN belum diatur", Toast.LENGTH_SHORT).show()
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        if (inputPin == savedPin) {
            Toast.makeText(this, "PIN benar", Toast.LENGTH_SHORT).show()
            val resultIntent = intent
            resultIntent.putExtra("isVerified", true)
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        } else {
            Toast.makeText(this, "PIN salah", Toast.LENGTH_SHORT).show()
            enteredPin.clear() // Hapus PIN yang salah untuk input ulang
            updatePinDots() // Reset dot tampilan
        }
    }
}