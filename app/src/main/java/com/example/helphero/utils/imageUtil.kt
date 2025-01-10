package com.example.helphero.utils

import android.content.ContentResolver
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import android.widget.ImageView
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import android.widget.ProgressBar
import com.example.helphero.R
import com.google.android.gms.tasks.Task
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ImageUtil private constructor() {
    companion object {

        fun loadImage(imageUri: Uri?, imageView: ImageView) {
            Picasso.get()
                .load(imageUri)
                .placeholder(R.drawable.applogo)
                .into(imageView)
        }

        fun loadImageInFeed(imageUri: Uri?, imageView: ImageView, onLoadComplete: () -> Unit) {
            Picasso.get()
                .load(imageUri)
                .fit()
                .centerCrop()
                .placeholder(R.drawable.applogo)
                .into(imageView, object : com.squareup.picasso.Callback {
                    override fun onSuccess() {
                        onLoadComplete()
                    }

                    override fun onError(e: Exception?) {
                        onLoadComplete()
                    }
                })
        }

        fun ShowImgInViewFromGallery(contentResolver: ContentResolver, imageView: ImageView, imageUri: Uri) {
            val inputStream = contentResolver.openInputStream(imageUri)
            if (inputStream != null) {
                val exif = ExifInterface(inputStream)
                val rotation =
                    exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

                val degrees = when (rotation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90F
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180F
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270F
                    else -> 0F
                }

                inputStream.close()

                Picasso.get()
                    .load(imageUri.toString())
                    .rotate(degrees)
                    .fit()
                    .centerCrop()
                    .into(imageView)
            } else {
                Log.d("Picturerequest", "Input stream is null")
            }
        }

        fun showImgInViewFromUrl(imageUri: String, imageView: ImageView, progressBar: ProgressBar) {
            progressBar.visibility = ProgressBar.VISIBLE

            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val degrees = withContext(Dispatchers.IO) {
                        val client = OkHttpClient()
                        val request = Request.Builder().url(imageUri).build()
                        client.newCall(request).execute().use { response ->
                            if (!response.isSuccessful) throw IOException("Unexpected code $response")
                            response.body?.byteStream()?.use { inputStream ->
                                val exif = ExifInterface(inputStream!!)
                                val rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                                when (rotation) {
                                    ExifInterface.ORIENTATION_ROTATE_90 -> 90F
                                    ExifInterface.ORIENTATION_ROTATE_180 -> 180F
                                    ExifInterface.ORIENTATION_ROTATE_270 -> 270F
                                    else -> 0F
                                }
                            } ?: 0F
                        }
                    }

                    Picasso.get()
                        .load(imageUri)
                        .rotate(degrees)
                        .fit()
                        .centerCrop()
                        .into(imageView)

                    progressBar.visibility = ProgressBar.GONE
                } catch (e: Exception) {
                    progressBar.visibility = ProgressBar.GONE
                }
            }
        }

        suspend fun UploadImage(imageId: String, imageUri: Uri, storageRef: StorageReference): Uri? {
            val imageRef = storageRef.child(imageId)

            return try {
                imageRef.putFile(imageUri).await()

                val downloadUrl = withContext(Dispatchers.IO) {
                    imageRef.downloadUrl.await()
                }

                downloadUrl
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        fun deleteStorageImage(imageId: String, storageRef: StorageReference): Task<Void> {
            val imageRef = storageRef.child(imageId)
            return imageRef.delete()
        }
    }
}