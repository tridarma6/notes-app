package com.example.notesapp.ui

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.notesapp.R
import com.example.notesapp.databinding.ActivityInputSecurityAnswerBinding // Sesuaikan
import com.example.notesapp.util.PinManager

class InputSecurityAnswerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInputSecurityAnswerBinding
    private var questionIndex: Int = -1 // Untuk menyimpan indeks pertanyaan yang diterima

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInputSecurityAnswerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // --- Listener untuk tombol "Back" ---
        binding.backArrow.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
        binding.backText.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        // --- Ambil indeks pertanyaan dari Intent ---
        questionIndex = intent.getIntExtra("QUESTION_INDEX", -1)
        if (questionIndex == -1) {
            Toast.makeText(this, "Kesalahan: Pertanyaan tidak valid.", Toast.LENGTH_SHORT).show()
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        // --- Tampilkan pertanyaan yang dipilih ---
        val questions = resources.getStringArray(R.array.security_questions)
        if (questionIndex >= 0 && questionIndex < questions.size) {
            binding.tvSelectedQuestion.text = questions[questionIndex]
        } else {
            binding.tvSelectedQuestion.text = getString(R.string.error_question_not_found)
            binding.btnSaveAnswer.isEnabled = false // Nonaktifkan tombol simpan jika pertanyaan tidak valid
        }

        // --- Listener untuk tombol "Simpan Jawaban" ---
        binding.btnSaveAnswer.setOnClickListener {
            val answer = binding.etSecurityAnswer.text.toString().trim()
            if (answer.isEmpty()) {
                Toast.makeText(this, "Jawaban tidak boleh kosong.", Toast.LENGTH_SHORT).show()
            } else {
                // Simpan pertanyaan dan jawaban ke PinManager
                PinManager.saveSecurityQuestion(this, questionIndex, answer)
                Toast.makeText(this, "Jawaban keamanan berhasil disimpan.", Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_OK) // Beri tahu SetSecurityQuestionActivity bahwa berhasil
                finish()
            }
        }
    }
}