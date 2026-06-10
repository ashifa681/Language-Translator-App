package com.example.languagetranslator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import java.util.*

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var spinnerFrom: Spinner
    private lateinit var spinnerTo: Spinner
    private lateinit var etInputText: EditText
    private lateinit var btnTranslate: Button
    private lateinit var btnSwap: Button
    private lateinit var btnCopy: Button
    private lateinit var btnVoice: Button
    private lateinit var btnSpeakResult: Button
    private lateinit var tvResult: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tts: TextToSpeech
    private val VOICE_REQUEST_CODE = 100

    private val languages = listOf(
        Pair("Afrikaans", "af"),
        Pair("Albanian", "sq"),
        Pair("Arabic", "ar"),
        Pair("Armenian", "hy"),
        Pair("Azerbaijani", "az"),
        Pair("Bengali", "bn"),
        Pair("Bosnian", "bs"),
        Pair("Bulgarian", "bg"),
        Pair("Catalan", "ca"),
        Pair("Chinese (Simplified)", "zh"),
        Pair("Chinese (Traditional)", "zh-TW"),
        Pair("Croatian", "hr"),
        Pair("Czech", "cs"),
        Pair("Danish", "da"),
        Pair("Dutch", "nl"),
        Pair("English", "en"),
        Pair("Estonian", "et"),
        Pair("Finnish", "fi"),
        Pair("French", "fr"),
        Pair("German", "de"),
        Pair("Greek", "el"),
        Pair("Gujarati", "gu"),
        Pair("Hebrew", "he"),
        Pair("Hindi", "hi"),
        Pair("Hungarian", "hu"),
        Pair("Icelandic", "is"),
        Pair("Indonesian", "id"),
        Pair("Irish", "ga"),
        Pair("Italian", "it"),
        Pair("Japanese", "ja"),
        Pair("Kannada", "kn"),
        Pair("Korean", "ko"),
        Pair("Latvian", "lv"),
        Pair("Lithuanian", "lt"),
        Pair("Macedonian", "mk"),
        Pair("Malay", "ms"),
        Pair("Maltese", "mt"),
        Pair("Marathi", "mr"),
        Pair("Norwegian", "no"),
        Pair("Persian", "fa"),
        Pair("Polish", "pl"),
        Pair("Portuguese", "pt"),
        Pair("Punjabi", "pa"),
        Pair("Romanian", "ro"),
        Pair("Russian", "ru"),
        Pair("Serbian", "sr"),
        Pair("Slovak", "sk"),
        Pair("Slovenian", "sl"),
        Pair("Spanish", "es"),
        Pair("Swahili", "sw"),
        Pair("Swedish", "sv"),
        Pair("Tagalog", "tl"),
        Pair("Tamil", "ta"),
        Pair("Telugu", "te"),
        Pair("Thai", "th"),
        Pair("Turkish", "tr"),
        Pair("Ukrainian", "uk"),
        Pair("Urdu", "ur"),
        Pair("Vietnamese", "vi"),
        Pair("Welsh", "cy")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tts = TextToSpeech(this, this)

        spinnerFrom = findViewById(R.id.spinnerFrom)
        spinnerTo = findViewById(R.id.spinnerTo)
        etInputText = findViewById(R.id.etInputText)
        btnTranslate = findViewById(R.id.btnTranslate)
        btnSwap = findViewById(R.id.btnSwap)
        btnCopy = findViewById(R.id.btnCopy)
        btnVoice = findViewById(R.id.btnVoice)
        btnSpeakResult = findViewById(R.id.btnSpeakResult)
        tvResult = findViewById(R.id.tvResult)
        progressBar = findViewById(R.id.progressBar)

        setupSpinners()

        btnTranslate.setOnClickListener { translateText() }

        // Voice input
        btnVoice.setOnClickListener {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US")
            intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Bol do — main sun rahi hoon!")
            try {
                startActivityForResult(intent, VOICE_REQUEST_CODE)
            } catch (e: Exception) {
                Toast.makeText(this, "Voice not supported!", Toast.LENGTH_SHORT).show()
            }
        }

        // Swap languages
        btnSwap.setOnClickListener {
            val fromPos = spinnerFrom.selectedItemPosition
            val toPos = spinnerTo.selectedItemPosition
            spinnerFrom.setSelection(toPos)
            spinnerTo.setSelection(fromPos)
            val inputText = etInputText.text.toString()
            val resultText = tvResult.text.toString()
            if (resultText != "Translation will appear here...") {
                etInputText.setText(resultText)
                tvResult.text = inputText
            }
        }

        // Copy result
        btnCopy.setOnClickListener {
            val result = tvResult.text.toString()
            if (result != "Translation will appear here...") {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("translation", result)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Copied!", Toast.LENGTH_SHORT).show()
            }
        }

        // Speak result
        btnSpeakResult.setOnClickListener {
            val result = tvResult.text.toString()
            if (result != "Translation will appear here..." && result.isNotEmpty()) {
                val langCode = languages[spinnerTo.selectedItemPosition].second
                val locale = Locale(langCode)
                tts.language = locale
                tts.speak(result, TextToSpeech.QUEUE_FLUSH, null, null)
            } else {
                Toast.makeText(this, "Pehle translate karo!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VOICE_REQUEST_CODE && resultCode == RESULT_OK) {
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty()) {
                etInputText.setText(results[0])
                translateText()
            }
        }
    }

    private fun setupSpinners() {
        val languageNames = languages.map { it.first }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languageNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFrom.adapter = adapter
        spinnerTo.adapter = adapter
        spinnerFrom.setSelection(languageNames.indexOf("English"))
        spinnerTo.setSelection(languageNames.indexOf("Urdu"))
    }

    private fun translateText() {
        val inputText = etInputText.text.toString().trim()
        if (inputText.isEmpty()) {
            Toast.makeText(this, "Please enter text!", Toast.LENGTH_SHORT).show()
            return
        }

        val fromCode = languages[spinnerFrom.selectedItemPosition].second
        val toCode = languages[spinnerTo.selectedItemPosition].second

        if (fromCode == toCode) {
            Toast.makeText(this, "Same language selected!", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        btnTranslate.isEnabled = false
        tvResult.text = "Translating..."

        lifecycleScope.launch {
            try {
                val encoded = URLEncoder.encode(inputText, "UTF-8")
                val url = "https://api.mymemory.translated.net/get?q=$encoded&langpair=$fromCode|$toCode"
                val response = withContext(Dispatchers.IO) {
                    URL(url).readText()
                }
                val json = JSONObject(response)
                val translated = json
                    .getJSONObject("responseData")
                    .getString("translatedText")

                progressBar.visibility = View.GONE
                btnTranslate.isEnabled = true
                tvResult.text = translated
                tvResult.setTextColor(getColor(R.color.text_primary))

            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                btnTranslate.isEnabled = true
                tvResult.text = "Error! Check internet."
                Toast.makeText(this@MainActivity, "Failed!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.ENGLISH
        }
    }

    override fun onDestroy() {
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }
}