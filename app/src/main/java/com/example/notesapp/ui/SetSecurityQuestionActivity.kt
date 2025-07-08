package com.example.notesapp.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.notesapp.R
import com.example.notesapp.databinding.ActivitySetSecurityQuestionBinding // Sesuaikan
import com.example.notesapp.util.PinManager

class SetSecurityQuestionActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetSecurityQuestionBinding
    private val REQUEST_CODE_INPUT_ANSWER = 1003 // Request code BARU

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetSecurityQuestionBinding.inflate(layoutInflater)
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

        // --- Setup Spinner Pertanyaan Keamanan ---
        val questions = resources.getStringArray(R.array.security_questions)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, questions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSecurityQuestion.adapter = adapter

        // --- Listener untuk tombol "Next" ---
        binding.btnNext.setOnClickListener {
            val selectedQuestionIndex = binding.spinnerSecurityQuestion.selectedItemPosition
            if (selectedQuestionIndex == 0) { // Asumsikan item pertama adalah "Pilih pertanyaan..."
                Toast.makeText(this, "Silakan pilih pertanyaan keamanan.", Toast.LENGTH_SHORT).show()
            } else {
                // Luncurkan Activity berikutnya untuk input jawaban
                val intent = Intent(this, InputSecurityAnswerActivity::class.java).apply {
                    putExtra("QUESTION_INDEX", selectedQuestionIndex)
                }
                startActivityForResult(intent, REQUEST_CODE_INPUT_ANSWER)
            }
        }
    }

    // --- Tangani Hasil dari InputSecurityAnswerActivity ---
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_INPUT_ANSWER) {
            if (resultCode == Activity.RESULT_OK) {
                // Pertanyaan dan jawaban berhasil disimpan
                setResult(Activity.RESULT_OK)
                finish() // Selesaikan Activity ini
            } else {
                // Pengguna membatalkan input jawaban, atau gagal
                Toast.makeText(this, "Pengaturan jawaban dibatalkan.", Toast.LENGTH_SHORT).show()
                // Tetap di layar ini, biarkan pengguna memilih lagi atau keluar
            }
        }
    }
}