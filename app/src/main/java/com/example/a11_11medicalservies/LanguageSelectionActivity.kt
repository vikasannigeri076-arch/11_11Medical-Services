package com.example.a11_11medicalservies

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class LanguageSelectionActivity : AppCompatActivity() {

    private lateinit var btnEnglish: Button
    private lateinit var btnKannada: Button
    private lateinit var btnHindi: Button
    private lateinit var btnTamil: Button
    private lateinit var btnTelugu: Button
    private lateinit var btnContinue: Button

    private var selectedLanguageCode = "en" // Default
    private var userType: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_language_selection)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        userType = intent.getStringExtra("userType")

        btnEnglish = findViewById(R.id.btnEnglish)
        btnKannada = findViewById(R.id.btnKannada)
        btnHindi = findViewById(R.id.btnHindi)
        btnTamil = findViewById(R.id.btnTamil)
        btnTelugu = findViewById(R.id.btnTelugu)
        btnContinue = findViewById(R.id.button2)

        val buttons = listOf(btnEnglish, btnKannada, btnHindi, btnTamil, btnTelugu)

        btnEnglish.setOnClickListener { selectLanguage("en", btnEnglish, buttons) }
        btnKannada.setOnClickListener { selectLanguage("kn", btnKannada, buttons) }
        btnHindi.setOnClickListener { selectLanguage("hi", btnHindi, buttons) }
        btnTamil.setOnClickListener { selectLanguage("ta", btnTamil, buttons) }
        btnTelugu.setOnClickListener { selectLanguage("te", btnTelugu, buttons) }

        btnContinue.setOnClickListener {
            setLocale(selectedLanguageCode)
            
            val nextActivity = if (userType == "Shop") {
                ShopHomeActivity::class.java
            } else {
                CustomerHomeActivity::class.java
            }
            
            startActivity(Intent(this, nextActivity))
            finish()
        }
        
        // Select English by default visually
        selectLanguage("en", btnEnglish, buttons)
    }

    private fun selectLanguage(langCode: String, selectedBtn: Button, allButtons: List<Button>) {
        selectedLanguageCode = langCode

        for (btn in allButtons) {
            if (btn == selectedBtn) {
                btn.backgroundTintList = ContextCompat.getColorStateList(this, R.color.selectedlangbg)
                btn.setTextColor(ContextCompat.getColor(this, R.color.selectedlangtxt))
            } else {
                btn.backgroundTintList = ContextCompat.getColorStateList(this, R.color.unselectedlangbg)
                btn.setTextColor(ContextCompat.getColor(this, R.color.unselectedlangtxt))
            }
        }
    }

    private fun setLocale(langCode: String) {
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(langCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
        
        val sharedPref = getSharedPreferences("Settings", Context.MODE_PRIVATE)
        sharedPref.edit().putString("My_Lang", langCode).apply()
    }
}