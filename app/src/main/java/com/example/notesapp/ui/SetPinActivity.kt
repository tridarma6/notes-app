package com.example.notesapp.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.notesapp.databinding.ActivitySetPinBinding
import com.example.notesapp.util.PinManager

class SetPinActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetPinBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetPinBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSavePin.setOnClickListener {
            val pin = binding.editPin.text.toString()
            val confirmPin = binding.editConfirmPin.text.toString()

            if (pin.length < 4) {
                Toast.makeText(this, "PIN harus minimal 4 digit", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pin != confirmPin) {
                Toast.makeText(this, "PIN tidak cocok", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            PinManager.savePin(this, pin)
            Toast.makeText(this, "PIN berhasil disimpan", Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }
    }
}
