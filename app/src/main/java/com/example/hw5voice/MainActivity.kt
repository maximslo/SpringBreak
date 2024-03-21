package com.example.hw5voice

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.util.Log
import android.widget.Button
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    companion object {
        private const val MY_PERMISSIONS_RECORD_AUDIO = 101
    }

    private lateinit var languageSpinner: Spinner
    private lateinit var editText: EditText

    private lateinit var sensorManager: SensorManager
    private lateinit var shakeDetector: ShakeDetector

    private val INITIAL_PROMPT = "Select a language" // Default prompt


    private val SPEECH_REQUEST_CODE = 100

    // Define languages, corresponding vacation spots, and greetings
    private val languageMap = mapOf(
        INITIAL_PROMPT to Pair("", ""),
        "Spanish" to Pair("geo:19.432608,-99.133209?q=Madrid", "Hola"), // Madrid, Spain
        "French" to Pair("geo:48.856614,2.352222?q=Paris", "Bonjour"), // Paris, France
        "Chinese" to Pair("geo:39.904200,116.407396?q=Beijing", "NiHao"), // Beijing, China
        "Japanese" to Pair("geo:35.689487,139.691706?q=Tokyo", "Konichiwa"), // Tokyo, Japan
        "German" to Pair("geo:52.520008,13.404954?q=Berlin", "Hallo"), // Berlin, Germany
        "Italian" to Pair("geo:41.902784,12.496366?q=Rome", "Ciao") // Rome, Italy// Beijing, China
    )

    private var selectedLanguage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        languageSpinner = findViewById(R.id.spinnerLanguages)
        editText = findViewById(R.id.etTranscribedText)

        setupSpinner()
        // Setup ShakeDetector
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        shakeDetector = ShakeDetector().apply {
            setOnShakeListener(object : ShakeDetector.OnShakeListener {
                override fun onShake() {
                    Log.d("ShakeDetector", "Shake detected!")
                    onShakeDetected()
                }
            })
        }
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languageMap.keys.toList())
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        languageSpinner.adapter = adapter

        languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val selection = parent.getItemAtPosition(position) as String
                if (selection != INITIAL_PROMPT) { // Check if the selection is not the initial prompt
                    selectedLanguage = selection
                    startSpeechRecognition()
                } else {
                    selectedLanguage = null // No language selected
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun startSpeechRecognition() {
        // Check if permission is already granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            // Permission is already granted, start speech recognition
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now")
            }

            try {
                startActivityForResult(intent, SPEECH_REQUEST_CODE)
            } catch (e: ActivityNotFoundException) {
                // Handle exception where the device does not support speech recognition
            }
        } else {
            // Permission is not granted, request permission
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), MY_PERMISSIONS_RECORD_AUDIO)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SPEECH_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val spokenText: String? = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.let { results ->
                results[0] // The first match is usually the most accurate
            }
            editText.setText(spokenText) // Update your EditText with the spoken text
        }
    }

    // Call this method when you detect a shake gesture
    private fun onShakeDetected() {
        selectedLanguage?.let { language ->
            languageMap[language]?.first?.let { geoUri ->
                if (geoUri.isNotEmpty()) {
                    val gmmIntentUri = Uri.parse(geoUri)
                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                    playGreeting(language)

                    if (mapIntent.resolveActivity(packageManager) != null) {
                        startActivity(mapIntent) // Start the intent to open the map
                        playGreeting(language) // Play the greeting after launching the map
                    } else {
                        // Fallback mechanism if no app can handle the geo URI
                        val query = geoUri.substringAfter("?q=")

                        // Log the extracted query
                        Log.d("ShakeDetection", "Extracted query: $query")

                        val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$query"))
                        startActivity(fallbackIntent)
                    }
                }
            }
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            MY_PERMISSIONS_RECORD_AUDIO -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permission was granted, you can start audio recording
                } else {
                    // Permission denied, disable the functionality that depends on this permission.
                }
                return
            }
            // Handle other permission results if there are any
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(shakeDetector, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(shakeDetector)
    }

    private fun playGreeting(language: String) {
        val greetingResId = when (language) {
            "Spanish" -> R.raw.hola
            "French" -> R.raw.bonjour
            "Chinese" -> R.raw.nihao
            else -> null // can follow this example for the rest
        }

        greetingResId?.let {
            val mediaPlayer = MediaPlayer.create(this, it)
            mediaPlayer.setOnCompletionListener { mp -> mp.release() }
            mediaPlayer.start()
        }
    }

}
