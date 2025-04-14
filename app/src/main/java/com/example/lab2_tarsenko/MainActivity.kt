package com.example.lab2_tarsenko

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

class MainActivity : AppCompatActivity() {

    private lateinit var selfieImageView: ImageView
    private lateinit var takeSelfieButton: Button
    private lateinit var sendSelfieButton: Button
    private lateinit var clearButton: Button
    private var photoUri: Uri? = null
    private var permissionsGranted = false

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            Log.d("MainActivity", "Фото успішно зроблено, photoUri: $photoUri")
            photoUri?.let {
                selfieImageView.setImageURI(it)
            } ?: run {
                Log.e("MainActivity", "photoUri є null після зйомки фото")
                Toast.makeText(this, "Не вдалося завантажити фото!", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.e("MainActivity", "Зйомка фото не вдалася")
            Toast.makeText(this, "Не вдалося зробити фото!", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestPermissionsLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        permissionsGranted = permissions.all { it.value }
        if (permissionsGranted) {
            Log.d("MainActivity", "Усі дозволи отримано")
            Toast.makeText(this, "Дозволи отримано!", Toast.LENGTH_SHORT).show()
        } else {
            Log.w("MainActivity", "Деякі дозволи не отримано: $permissions")
            Toast.makeText(this, "Дозволи потрібні для роботи додатку!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        selfieImageView = findViewById(R.id.selfieImageView)
        takeSelfieButton = findViewById(R.id.takeSelfieButton)
        sendSelfieButton = findViewById(R.id.sendSelfieButton)
        clearButton = findViewById(R.id.clearButton)

        checkAndRequestPermissions()

        takeSelfieButton.setOnClickListener {
            if (permissionsGranted) {
                dispatchTakePictureIntent()
            } else {
                checkAndRequestPermissions()
            }
        }

        sendSelfieButton.setOnClickListener {
            if (photoUri != null) {
                sendEmailWithSelfie()
            } else {
                Toast.makeText(this, "Спочатку зробіть селфі!", Toast.LENGTH_SHORT).show()
            }
        }

        clearButton.setOnClickListener {
            selfieImageView.setImageDrawable(null)
            photoUri = null
            Toast.makeText(this, "Фото очищено!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            permissionsGranted = true
        }
    }

    private fun dispatchTakePictureIntent() {
        val photoFile: File? = try {
            createImageFile()
        } catch (ex: IOException) {
            Log.e("MainActivity", "Помилка створення файлу: ${ex.message}")
            Toast.makeText(this, "Помилка створення файлу!", Toast.LENGTH_SHORT).show()
            null
        }

        photoFile?.let {
            try {
                photoUri = FileProvider.getUriForFile(
                    this,
                    "com.example.selfieapp.fileprovider", // Змінено на відповідний authority
                    it
                )
                Log.d("MainActivity", "Запуск камери з photoUri: $photoUri")
                takePictureLauncher.launch(photoUri!!)
            } catch (e: Exception) {
                Log.e("MainActivity", "Помилка FileProvider: ${e.message}")
                Toast.makeText(this, "Помилка FileProvider: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } ?: run {
            Log.e("MainActivity", "Не вдалося створити файл для фото")
            Toast.makeText(this, "Не вдалося підготувати файл для фото!", Toast.LENGTH_SHORT).show()
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }

    private fun sendEmailWithSelfie() {
        val emailIntent = Intent(Intent.ACTION_SEND)
        emailIntent.type = "message/rfc822"

        emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf("hodovychenko@op.edu.ua"))
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "ANDROID Тарасенко Ірина")
        emailIntent.putExtra(Intent.EXTRA_TEXT, "Посилання на репозиторій: https://github.com/tarasenko-ira/Android_Lab2.git")

        photoUri?.let {
            emailIntent.putExtra(Intent.EXTRA_STREAM, it)
            emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            startActivity(Intent.createChooser(emailIntent, "Відправити лист..."))
        } catch (ex: android.content.ActivityNotFoundException) {
            Toast.makeText(this, "Немає додатку для відправки email!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable("photoUri", photoUri)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        photoUri = savedInstanceState.getParcelable("photoUri")
        photoUri?.let {
            selfieImageView.setImageURI(it)
        }
    }
}