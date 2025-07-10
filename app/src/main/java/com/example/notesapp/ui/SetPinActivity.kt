package com.example.notesapp.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.notesapp.R
import com.example.notesapp.databinding.ActivitySetPinBinding
import com.example.notesapp.util.PinManager

class SetPinActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetPinBinding
    private val enteredPin = StringBuilder()
    private var firstPin: String? = null
    private val PIN_LENGTH = 4

    // Request codes
    private val REQUEST_CODE_PIN_RECOVERY = 1001
    private val REQUEST_CODE_SET_SECURITY_QUESTION = 1002 // Request code BARU

    private enum class PinInputState {
        VERIFY_OLD_PIN,
        ENTER_NEW_PIN,
        CONFIRM_NEW_PIN
    }

    private var currentState: PinInputState = PinInputState.VERIFY_OLD_PIN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetPinBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInitialState()

        binding.backArrow.setOnClickListener { handleBackButton() }
        binding.backText.setOnClickListener { handleBackButton() }

        setupNumberPadListeners()

        binding.btnBackspace.setOnClickListener {
            if (enteredPin.isNotEmpty()) {
                enteredPin.deleteCharAt(enteredPin.length - 1)
                updatePinDots()
            }
        }

        // --- Tombol "Lupa PIN?" ---
        binding.tvForgotPassword.setOnClickListener {
            // Pastikan pertanyaan keamanan sudah disetel sebelum mencoba memulihkan
            if (PinManager.isSecurityQuestionSet(this)) {
                startActivityForResult(Intent(this, PinRecoveryActivity::class.java), REQUEST_CODE_PIN_RECOVERY)
            } else {
                Toast.makeText(this, "Tidak ada pertanyaan keamanan yang diatur. Tidak bisa memulihkan PIN.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupInitialState() {
        if (PinManager.isPinSet(this)) {
            currentState = PinInputState.VERIFY_OLD_PIN
            binding.tvPinInputTitle.text = getString(R.string.enter_your_old_pin)
            binding.tvForgotPassword.visibility = View.VISIBLE
        } else {
            currentState = PinInputState.ENTER_NEW_PIN
            binding.tvPinInputTitle.text = getString(R.string.enter_your_new_pin)
            binding.tvForgotPassword.visibility = View.GONE
        }
        updatePinDots() // Inisialisasi dot kosong
    }

    private fun handleBackButton() {
        when (currentState) {
            PinInputState.VERIFY_OLD_PIN -> {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
            PinInputState.CONFIRM_NEW_PIN -> {
                enteredPin.clear()
                updatePinDots()
                currentState = PinInputState.ENTER_NEW_PIN
                binding.tvPinInputTitle.text = getString(R.string.enter_your_new_pin)
                Toast.makeText(this, "Silakan masukkan PIN baru Anda lagi.", Toast.LENGTH_SHORT).show()
            }
            PinInputState.ENTER_NEW_PIN -> {
                if (PinManager.isPinSet(this)) { // Jika ada PIN lama, kembali ke verifikasi
                    enteredPin.clear()
                    updatePinDots()
                    currentState = PinInputState.VERIFY_OLD_PIN
                    binding.tvPinInputTitle.text = getString(R.string.enter_your_old_pin)
                    Toast.makeText(this, "Silakan masukkan PIN lama Anda lagi.", Toast.LENGTH_SHORT).show()
                } else { // Jika tidak ada PIN lama, ini setup pertama, langsung batal
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                }
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
                    Toast.makeText(this, "PIN lama cocok. Silakan masukkan PIN baru Anda.", Toast.LENGTH_SHORT).show()
                    enteredPin.clear()
                    updatePinDots()
                    currentState = PinInputState.ENTER_NEW_PIN
                    binding.tvPinInputTitle.text = getString(R.string.enter_your_new_pin)
                } else {
                    Toast.makeText(this, "PIN lama salah. Silakan coba lagi.", Toast.LENGTH_LONG).show()
                    enteredPin.clear()
                    updatePinDots()
                }
            }
            PinInputState.ENTER_NEW_PIN -> {
                if (currentInput.length < PIN_LENGTH) {
                    Toast.makeText(this, "PIN harus minimal $PIN_LENGTH digit", Toast.LENGTH_SHORT).show()
                    enteredPin.clear()
                    updatePinDots()
                    return
                }

                firstPin = currentInput
                enteredPin.clear()
                updatePinDots()
                currentState = PinInputState.CONFIRM_NEW_PIN
                binding.tvPinInputTitle.text = getString(R.string.confirm_your_new_pin)
                Toast.makeText(this, "PIN baru sudah direkam. Masukkan ulang PIN untuk konfirmasi.", Toast.LENGTH_LONG).show()
            }
            PinInputState.CONFIRM_NEW_PIN -> {
                if (firstPin == currentInput) {
                    PinManager.savePin(this, firstPin!!) // PIN baru disimpan di sini
                    Toast.makeText(this, "PIN berhasil diatur! Sekarang atur pertanyaan keamanan.", Toast.LENGTH_LONG).show()

                    val intent = Intent(this, SetSecurityQuestionActivity::class.java)
                    startActivityForResult(intent, REQUEST_CODE_SET_SECURITY_QUESTION)
                } else {
                    Toast.makeText(this, "PIN baru tidak cocok. Silakan coba lagi dari awal.", Toast.LENGTH_LONG).show()
                    firstPin = null
                    enteredPin.clear()
                    updatePinDots()
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

    // Menangani Hasil dari Activity Lain
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_PIN_RECOVERY -> {
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "PIN berhasil diverifikasi melalui pertanyaan keamanan. Silakan atur PIN baru Anda.", Toast.LENGTH_LONG).show()
                    firstPin = null
                    enteredPin.clear()
                    updatePinDots()
                    currentState = PinInputState.ENTER_NEW_PIN // Langsung ke setting PIN baru
                    binding.tvPinInputTitle.text = getString(R.string.enter_your_new_pin)
                } else {
                    Toast.makeText(this, "Pemulihan PIN dibatalkan atau gagal.", Toast.LENGTH_SHORT).show()
                    enteredPin.clear()
                    updatePinDots()
                    // Kembali ke state verifikasi PIN lama jika ada, atau reset awal
                    setupInitialState()
                }
            }
            REQUEST_CODE_SET_SECURITY_QUESTION -> {
                if (resultCode == Activity.RESULT_OK) {
                    setResult(Activity.RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this, "Pengaturan pertanyaan keamanan dibatalkan.", Toast.LENGTH_LONG).show()
                    setResult(Activity.RESULT_CANCELED) // Atau RESULT_OK jika Anda mengizinkan tanpa pertanyaan
                    finish()
                }
            }
        }
    }
}