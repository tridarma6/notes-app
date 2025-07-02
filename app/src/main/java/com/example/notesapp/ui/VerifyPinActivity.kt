package com.example.notesapp.ui

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.notesapp.databinding.ActivityVerifyPinBinding
import com.example.notesapp.util.PinManager

class VerifyPinActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVerifyPinBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerifyPinBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnVerify.setOnClickListener {
            val inputPin = binding.editPin.text.toString()
            val savedPin = PinManager.getPin(this)

            if (savedPin == null) {
                Toast.makeText(this, "PIN belum diatur", Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_CANCELED)
                finish()
                return@setOnClickListener
            }

            if (inputPin == savedPin) {
                Toast.makeText(this, "PIN benar", Toast.LENGTH_SHORT).show()

                // ✅ Kirim hasil ke MainActivity
                val resultIntent = intent
                resultIntent.putExtra("isVerified", true)
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            } else {
                Toast.makeText(this, "PIN salah", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnCancel.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }
}
