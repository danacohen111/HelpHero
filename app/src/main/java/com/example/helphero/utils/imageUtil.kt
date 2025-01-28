package com.example.helphero.utils

import android.net.Uri
import android.util.Log
import android.widget.ImageView
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.UploadCallback
import com.squareup.picasso.Picasso
import com.example.helphero.R
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class ImageUtil private constructor() {
    companion object {

        fun loadImage(imageUri: Uri?, imageView: ImageView, placeholderResId: Int = R.drawable.applogo) {
            Picasso.get()
                .load(imageUri)
                .placeholder(placeholderResId)
                .into(imageView)
        }

        suspend fun uploadImage(
            imageId: String,
            imageUri: Uri
        ): Uri? {
            Log.d("ImageUtil", "Starting upload for imageId: $imageId, Uri: $imageUri")

            return try {
                suspendCoroutine { continuation ->
                    MediaManager.get()
                        .upload(imageUri)
                        .option("folder", "images") // Folder in Cloudinary
                        .option("public_id", imageId) // Assign a public ID
                        .callback(object : UploadCallback {
                            override fun onStart(requestId: String) {
                                Log.d("ImageUtil", "Upload started for requestId: $requestId")
                            }

                            override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                                val progress = (bytes.toDouble() / totalBytes) * 100
                                Log.d("ImageUtil", "Upload progress for requestId: $requestId: $progress%")
                            }

                            override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                                val downloadUrl = resultData["secure_url"] as String
                                Log.d("ImageUtil", "Upload successful: $downloadUrl")

                                Picasso.get().load(downloadUrl).into(object : com.squareup.picasso.Target {
                                    override fun onBitmapLoaded(bitmap: android.graphics.Bitmap?, from: Picasso.LoadedFrom?) {
                                        Log.d("ImageUtil", "Image loaded into Picasso: $downloadUrl")
                                    }

                                    override fun onBitmapFailed(e: java.lang.Exception?, errorDrawable: android.graphics.drawable.Drawable?) {
                                        Log.e("ImageUtil", "Failed to load image into Picasso: $downloadUrl", e)
                                    }

                                    override fun onPrepareLoad(placeHolderDrawable: android.graphics.drawable.Drawable?) {
                                        // Do nothing
                                    }
                                })

                                continuation.resume(Uri.parse(downloadUrl))
                            }

                            override fun onError(requestId: String, error: com.cloudinary.android.callback.ErrorInfo) {
                                Log.e("ImageUtil", "Upload failed for requestId: $requestId", Exception(error.description))
                                continuation.resumeWithException(Exception(error.description))
                            }

                            override fun onReschedule(requestId: String, error: com.cloudinary.android.callback.ErrorInfo) {
                                Log.w("ImageUtil", "Upload rescheduled for requestId: $requestId")
                            }
                        })
                        .dispatch()
                }
            } catch (e: Exception) {
                Log.e("ImageUtil", "Upload failed for imageId: $imageId, Uri: $imageUri", e)
                null
            }
        }

        fun deleteImage(imageId: String): Boolean {
            Log.d("ImageUtil", "Deleting image from Cloudinary: $imageId")

            return try {
                val options = mapOf("public_id" to "images/$imageId")
                MediaManager.get().uploader().destroy(options)

                // Invalidate the image in Picasso cache
                val imageUrl = "https://res.cloudinary.com/${BuildConfig.CLOUD_NAME}/image/upload/v1/images/$imageId"
                Picasso.get().invalidate(imageUrl)

                true
            } catch (e: Exception) {
                Log.e("ImageUtil", "Failed to delete imageId: $imageId", e)
                false
            }
        }
    }
}
