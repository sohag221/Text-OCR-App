package com.example.textocrapp

import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.method.ScrollingMovementMethod
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.textocrapp.databinding.ActivityMainBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding


    private  var currentPhotoPath:String? = null
    private lateinit var requestPermissionLauncher : ActivityResultLauncher<String>
    private lateinit var takePictureLauncher:ActivityResultLauncher<Uri>



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //take permission for camera
        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            isGranted: Boolean ->
            if (isGranted) {
                captureImage()
            }
        }



        //take picture from camera
        takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
            if (success) {
                currentPhotoPath?.let { path->
                    val bitmap=BitmapFactory.decodeFile(path)
                    binding.cameraImage.setImageBitmap(bitmap)
                    recongnizeText(bitmap)
                }
            } else {
                Toast.makeText(this, "Image capture failed", Toast.LENGTH_SHORT).show()
            }
        }


         binding.captureButton.setOnClickListener {
                requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
         }

    }

    private fun createImageFile(): File {

        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }


    private fun captureImage(){
        val photoFile:File?= try {
            createImageFile()
        }catch (ex:IOException){
            Toast.makeText(this, ex.message.toString(), Toast.LENGTH_SHORT).show()
            null
        }
        photoFile?.also {
            val photoURI: Uri = FileProvider.getUriForFile(this,"${applicationContext.packageName}.provider",it)
            takePictureLauncher.launch(photoURI)
        }
    }


    private fun recongnizeText(bitmap: Bitmap){

       val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image).addOnSuccessListener { ocrTxt ->
            binding.resultText.text = ocrTxt.text
            binding.resultText.movementMethod =ScrollingMovementMethod()

            binding.copyTextBtn.visibility = Button.VISIBLE
            binding.copyTextBtn.setOnClickListener {
                val clipboard = ContextCompat.getSystemService(this, ClipboardManager::class.java)
                val clip = android.content.ClipData.newPlainText("Recognized Text", ocrTxt.text)
                binding.copyTextBtn.visibility = Button.GONE

                clipboard?.setPrimaryClip(clip)
                Toast.makeText(this, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, e.message.toString(), Toast.LENGTH_SHORT).show()
        }

    }





}