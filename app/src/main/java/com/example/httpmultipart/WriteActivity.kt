package com.example.httpmultipart

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.httpmultipart.databinding.ActivityMainBinding
import com.example.httpmultipart.databinding.ActivityWriteBinding
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class WriteActivity : AppCompatActivity() {

    private lateinit var binding : ActivityWriteBinding

    var dirPath:String? = null
    var contentUri: Uri? = null
    var pic_path:String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_write)
        binding = ActivityWriteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var tempPath = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        //var tempPath = Environment.getExternalStorageDirectory() .getExternalStorageDirectory().absolutePath
        //dirPath = "${tempPath}/Android/data/${packageName}"
        dirPath = tempPath.toString()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.write_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item?.itemId){
            R.id.menu_camera ->{
                var camera_intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

                var fileName = "temp_${System.currentTimeMillis()}.jpg"
                var picPath = "${dirPath}/${fileName}"

                var file = File(picPath)

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                    contentUri = FileProvider.getUriForFile(this, "kr.co.softcampus.httpmultipart.file_provider", file)
                } else {
                    contentUri = Uri.fromFile(file)
                }

                camera_intent.putExtra(MediaStore.EXTRA_OUTPUT, contentUri)
                startActivityForResult(camera_intent, 1)
            }
            R.id.menu_gallery ->{
                var gallery_intent = Intent(Intent.ACTION_PICK)
                gallery_intent.type = MediaStore.Images.Media.CONTENT_TYPE
                startActivityForResult(gallery_intent, 2)
            }
            R.id.menu_upload ->{
                var thread = UploadThread()
                thread.start()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == 1){
            if(resultCode == RESULT_OK){
                var bitmap = BitmapFactory.decodeFile(contentUri?.path)

                bitmap = resizeBitmap(1024, bitmap)
                var degree = getDegree(contentUri?.path)

                pic_path = contentUri?.path
                rebuildImage(bitmap, pic_path!!, degree)

                binding.imageView2.setImageBitmap(bitmap)
                binding.imageView2.rotation = degree
            }
        } else if(requestCode == 2){
            if(resultCode == RESULT_OK){

                val uri = data?.data
                if(uri == null) return

                var c = contentResolver.query(uri, null, null, null, null)
                if(c != null) {
                    c.moveToNext()

                    var index = c?.getColumnIndex(MediaStore.Images.Media.DATA)
                    var source = c?.getString(index)

                    var bitmap = BitmapFactory.decodeFile(source)

                    bitmap = resizeBitmap(1024, bitmap)
                    var degree = getDegree(source)

                    var fileName = "temp_${System.currentTimeMillis()}.jpg"
                    pic_path = "${dirPath}/${fileName}"

                    rebuildImage(bitmap, pic_path!!, degree)

                    binding.imageView2.setImageBitmap(bitmap)
                    binding.imageView2.rotation = degree
                }
            }
        }
    }

    fun resizeBitmap(targetWidth : Int, source : Bitmap) : Bitmap{

        var ratio = source.height.toDouble() / source.width.toDouble()
        var targetHeight = (targetWidth * ratio).toInt()

        var result = Bitmap.createScaledBitmap(source, targetWidth, targetHeight, false)
        if(result != source){
            source.recycle()
        }
        return result
    }

    fun getDegree(path:String ?) : Float {
        var exif = ExifInterface(path!!)

        var degree = 0

        var ori = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1)
        when(ori){
            ExifInterface.ORIENTATION_ROTATE_90 ->{
                degree = 90
            }
            ExifInterface.ORIENTATION_ROTATE_180 ->{
                degree = 180
            }
            ExifInterface.ORIENTATION_ROTATE_270 ->{
                degree = 270
            }
        }

        return degree.toFloat()
    }

    fun rebuildImage(source : Bitmap, path:String, angle:Float){

        var matrix = Matrix()
        matrix.postRotate(angle)

        var bitmap2 = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, false)

        var out = FileOutputStream(path)
        bitmap2.compress(Bitmap.CompressFormat.JPEG, 100, out)
        out.flush()
        out.close()
    }

    inner class UploadThread : Thread(){
        override fun run() {
            var client = OkHttpClient()
            var request_builder = Request.Builder()
            var url = request_builder.url("http://192.168.0.2:8080/upload.jsp")

            var multipart_builder = MultipartBody.Builder()
            multipart_builder.setType(MultipartBody.FORM)

            multipart_builder.addFormDataPart("mobile_str1", binding.editText.text.toString())
            multipart_builder.addFormDataPart("mobile_str2", binding.editText2.text.toString())

            var file = File(pic_path)
            multipart_builder.addFormDataPart("mobile_img", file.name, file.asRequestBody("image/*".toMediaTypeOrNull()))
            var body = multipart_builder.build()

            var post = url.post(body)

            var request = post.build()

            //client.newCall(request).execute()
            client.newCall(request).enqueue(Callback1())
        }
    }

    inner class Callback1 : Callback {
        override fun onFailure(call: Call, e: IOException) {

        }

        override fun onResponse(call: Call, response: Response) {

            var result = response?.body?.string()

            if(result?.trim().equals("OK")){
                finish()
            }
        }
    }
}











