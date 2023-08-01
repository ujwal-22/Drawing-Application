package com.example.drawingapp

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.contentcapture.ContentCaptureCondition
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.ShareActionProvider
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.PackageManagerCompat
import androidx.lifecycle.lifecycleScope
import com.example.drawingapp.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOError
import java.io.IOException
import java.io.OutputStream
import java.net.URI

class MainActivity : AppCompatActivity() {

    private var binding:ActivityMainBinding? = null
    private var imagebuttoncurrentpaint : ImageButton? = null
    var customprogressdialog : Dialog? = null

    val opengallerylauncher : ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            result ->
            if(result.resultCode == RESULT_OK && result.data!=null){
                binding?.bg?.setImageURI(result.data?.data)
            }
        }

    val requestPermission : ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
            permission ->
            permission.entries.forEach{
                val permissionname = it.key
                val isgranted = it.value

                if(isgranted) {
                    if(permissionname == Manifest.permission.READ_EXTERNAL_STORAGE){
                        val galleryintent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        opengallerylauncher.launch(galleryintent)
                    }
                }
                else{
                    if(permissionname == Manifest.permission.READ_EXTERNAL_STORAGE) Toast.makeText(this, "Permission denied for reading external stprage", Toast.LENGTH_SHORT).show()
                    else if(permissionname == Manifest.permission.WRITE_EXTERNAL_STORAGE) Toast.makeText(this, "Permission denied for writing in external storage", Toast.LENGTH_SHORT).show()
                }

            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        binding?.drawingview?.setsizeforbrush(10.toFloat())
        binding?.brush?.setOnClickListener{
            binding?.llcolor?.visibility = View.INVISIBLE
            binding?.seekbar?.visibility = View.VISIBLE
            binding?.seek?.visibility = View.VISIBLE
            binding?.seekbar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
                override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                    val str = "${p1+1}/20"
                    binding?.seek?.text = str
//                    binding?.seekbar?.progress = p1
                }

                override fun onStartTrackingTouch(p0: SeekBar?) {
//                    TODO("Not yet implemented")
                }

                override fun onStopTrackingTouch(p0: SeekBar?) {
                    var progress : Int = binding?.seekbar?.progress!!
                    progress++
                    binding?.drawingview?.setsizeforbrush(progress.toFloat())
                    binding?.llcolor?.visibility = View.VISIBLE
                    binding?.seekbar?.visibility = View.INVISIBLE
                    binding?.seek?.visibility = View.INVISIBLE
                }
            })
//            showbrushsizechooserdialog()
        }
        imagebuttoncurrentpaint = binding?.llcolor?.getChildAt(1) as ImageButton
        imagebuttoncurrentpaint?.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallet_selected ))
        binding?.gallery?.setOnClickListener{ requeststorage() }
        binding?.undo?.setOnClickListener{ binding?.drawingview?.undo() }
        binding?.redo?.setOnClickListener{ binding?.drawingview?.redo() }
        binding?.save?.setOnClickListener{
//            requeststorage()
            showprogressdialog()
            if(isreadstorageallowed()){
                lifecycleScope.launch{
                    savebitmapfile(getbitmapfromview(binding?.fl!!))
                }
            }
        }

    }

//    fun showbrushsizechooserdialog(){
//        val brushdialog = Dialog(this)
//        brushdialog.setContentView(R.layout.dialog_brush_size)
//        brushdialog.setTitle("Brush Size: ")
//        brushdialog.show()
//        val small:ImageButton = brushdialog.findViewById(R.id.small_brush)
//        small.setOnClickListener{
//            binding?.drawingview?.setsizeforbrush(10.toFloat())
//            brushdialog.dismiss()
//        }
//        val medium:ImageButton = brushdialog.findViewById(R.id.medium_brush)
//        medium.setOnClickListener{
//            binding?.drawingview?.setsizeforbrush(20.toFloat())
//            brushdialog.dismiss()
//        }
//        val large:ImageButton = brushdialog.findViewById(R.id.large_brush)
//        large.setOnClickListener{
//            binding?.drawingview?.setsizeforbrush(30.toFloat())
//            brushdialog.dismiss()
//        }
//    }

    fun paintclicked(view : View){
        if(view != imagebuttoncurrentpaint){
            val imagebutton = view as ImageButton
            val colortag = imagebutton.tag.toString()
            binding?.drawingview?.setcolor(colortag)
            imagebuttoncurrentpaint?.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallet))
            imagebutton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallet_selected ))
            imagebuttoncurrentpaint = view
        }
    }

    private fun showrationaledialog(title:String, message:String){
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cancel"){
                    dialog, _-> dialog.dismiss()
            }
        builder.create().show()
    }

    private fun isreadstorageallowed():Boolean {
        val result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requeststorage(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)){
            showrationaledialog("Drawing App", "The application needs access to your external storage")
        }
        else requestPermission.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE))
    }

    private fun getbitmapfromview(view: View) : Bitmap {
        val returnedbitmap : Bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedbitmap)
        val bg = view.background
        if(bg != null) bg.draw(canvas)
        else canvas.drawColor(Color.WHITE)
        view.draw(canvas)
        return returnedbitmap
    }

    private suspend fun savebitmapfile(bitmap:Bitmap?) : String{
        var result = ""
        withContext(Dispatchers.IO){
            if(bitmap!=null){
                try{
                    val bytes = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)

                    // THE FILE PATH WHERE IMAGE WILL BE STORED
                    val file = File(externalCacheDir?.absoluteFile.toString() + File.separator + System.currentTimeMillis()/1000 + ".png")
                    Log.d("ujwal", file.toString())
                    val name = System.currentTimeMillis().toString() + ".png"
                    Log.d("ujwal", name)
                    val relativelocation = Environment.DIRECTORY_PICTURES + "/Drawing App"
                    Log.d("ujwal", relativelocation)
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Images.ImageColumns.DISPLAY_NAME, name)
                        put(MediaStore.Images.Media.MIME_TYPE, "image.png")
                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                            put(MediaStore.Images.ImageColumns.RELATIVE_PATH, relativelocation)
                        }
                    }
                    var contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    var stream : OutputStream? = null
                    var uri : Uri? = null       // THIS IS THE MAIN IMAGE URI
                    try {

                        uri = contentResolver.insert(contentUri, contentValues)
                        if(uri == null){
                            throw IOException("Failed to create new mediastore recors")
                        }
                        stream = contentResolver.openOutputStream(uri)
                        if(stream == null){
                            throw IOException("failed to get output stream")
                        }
                        if(!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)){
                            throw IOException("failed to save bitmap")
                        }

                    }
                    catch (e : IOException){
                            if (uri != null) {
                                contentResolver.delete(uri, null, null)
                            }
                        throw IOException(e)
                    }
                    finally {
                        stream?.close()
                    }
                    var fileOutputStream : FileOutputStream? = FileOutputStream(file)
                    fileOutputStream?.write(bytes.toByteArray())
                    fileOutputStream?.close()
                    result = file.absolutePath
                    runOnUiThread{
                        cancelprogressdialog()
                        if(result.isNotEmpty()) {
                            Toast.makeText(
                                this@MainActivity,
                                "$relativelocation/$name",
                                Toast.LENGTH_LONG
                            ).show()
                            uri?.let { shareimage(it) }
                        }
                        else Toast.makeText(this@MainActivity, "Something went wrong", Toast.LENGTH_LONG).show()
                    }
                }
                catch (e : Exception){
                    result = ""
                    e.printStackTrace()
                }
            }
        }
        return result
    }

//    private fun refreshgallery(newfile: File) {
//        val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
//        intent.setData(Uri.fromFile(newfile))
//        sendBroadcast(intent)
//    }

    private fun showprogressdialog(){

        customprogressdialog = Dialog(this)
        customprogressdialog?.setContentView(R.layout.dialog_custom_progress)
        customprogressdialog?.show()

    }

    private fun cancelprogressdialog(){
        if(customprogressdialog != null){
            customprogressdialog?.dismiss()
            customprogressdialog = null
        }
    }

    private fun shareimage(result : Uri){
//        Log.d("ujwal", result + "share")
//        MediaScannerConnection.scanFile(this, arrayOf(result), null){
//            path, uri ->
            val shareintent = Intent(Intent.ACTION_SEND)
//            val bytes = ByteArrayOutputStream()
//            Log.d("ujwal", uri.toString())
            shareintent.putExtra(Intent.EXTRA_STREAM, result)
            shareintent.type = "image/png"
//            shareintent.setPackage("com.whatsapp")
//            startActivity(shareintent)
            startActivity(Intent.createChooser(shareintent, "Share Via"))
//        }
    }

}