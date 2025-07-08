package com.example.notesapp.ui

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.notesapp.R
import com.example.notesapp.databinding.ActivityPinRecoveryBinding // Pastikan nama ini sesuai dengan file XML di atas
import com.example.notesapp.util.PinManager

class PinRecoveryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPinRecoveryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPinRecoveryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // --- Listener untuk tombol "Back" (panah + teks) ---
        binding.backArrow.setOnClickListener {
            setResult(Activity.RESULT_CANCELED) // Mengindikasikan pembatalan
            finish()
        }
        binding.backText.setOnClickListener {
            setResult(Activity.RESULT_CANCELED) // Mengindikasikan pembatalan
            finish()
        }

        // --- Tampilkan Pertanyaan Keamanan ---
        displaySecurityQuestion()

        // --- Listener untuk tombol "Verifikasi Jawaban" ---
        binding.btnVerifyAnswer.setOnClickListener {
            val userAnswer = binding.etSecurityAnswer.text.toString().trim()
            if (userAnswer.isEmpty()) {
                Toast.makeText(this, "Jawaban tidak boleh kosong.", Toast.LENGTH_SHORT).show()
            } else {
                if (PinManager.verifySecurityAnswer(this, userAnswer)) {
                    // Jawaban benar, kembalikan OK ke SetPinActivity
                    setResult(Activity.RESULT_OK)
                    finish()
                } else {
                    // Jawaban salah
                    Toast.makeText(this, "Jawaban salah. Silakan coba lagi.", Toast.LENGTH_LONG).show()
                    binding.etSecurityAnswer.text.clear() // Bersihkan input jawaban
                }
            }
        }
    }

    private fun displaySecurityQuestion() {
        val questionIndex = PinManager.getSecurityQuestionIndex(this)
        if (questionIndex != -1) {
            val questions = resources.getStringArray(R.array.security_questions)
            if (questionIndex >= 0 && questionIndex < questions.size) {
                binding.tvSecurityQuestion.text = questions[questionIndex]
            } else {
                // Jika indeks pertanyaan tidak valid
                binding.tvSecurityQuestion.text = getString(R.string.error_question_not_found)
                binding.btnVerifyAnswer.isEnabled = false // Nonaktifkan tombol verifikasi
                Toast.makeText(this, "Terjadi kesalahan pada pertanyaan keamanan.", Toast.LENGTH_LONG).show()
            }
        } else {
            // Jika belum ada pertanyaan keamanan yang diatur
            binding.tvSecurityQuestion.text = getString(R.string.no_security_question_set)
            binding.btnVerifyAnswer.isEnabled = false // Nonaktifkan tombol verifikasi
            Toast.makeText(this, "Tidak ada pertanyaan keamanan yang diatur. Tidak bisa memulihkan PIN.", Toast.LENGTH_LONG).show()
            // Anda mungkin ingin menambahkan finish() di sini atau opsi untuk kontak dukungan
        }
    }
}