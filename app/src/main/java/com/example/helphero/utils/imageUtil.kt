package com.example.helphero.utils

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.ImageView
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.UploadCallback
import com.example.helphero.BuildConfig
import com.squareup.picasso.Picasso
import com.example.helphero.R
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class ImageUtil private constructor() {
    companion object {
        private const val TAG = "ImageUtil"

        fun loadImage(imageUri: Uri?, imageView: ImageView, placeholderResId: Int = R.drawable.applogo) {
            imageView.visibility = View.INVISIBLE

            Picasso.get()
                .load(imageUri)
                .into(object : com.squareup.picasso.Target {
                    override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                        imageView.visibility = View.VISIBLE
                        imageView.setImageBitmap(bitmap)
                    }

                    override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                        imageView.visibility = View.VISIBLE
                        imageView.setImageDrawable(errorDrawable)
                    }

                    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}
                })
        }

        suspend fun uploadImage(
            imageId: String,
            imageUri: Uri
        ): Uri? {
            Log.d(TAG, "Starting upload for imageId: $imageId, Uri: $imageUri")

            return try {
                suspendCoroutine { continuation ->
                    MediaManager.get()
                        .upload(imageUri)
                        .option("folder", "images") // Folder in Cloudinary
                        .option("public_id", imageId) // Assign a public ID
                        .callback(object : UploadCallback {
                            override fun onStart(requestId: String) {
                                Log.d(TAG, "Upload started for requestId: $requestId")
                            }

                            override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                                val progress = (bytes.toDouble() / totalBytes) * 100
                                Log.d(TAG, "Upload progress for requestId: $requestId: $progress%")
                            }

                            override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                                val downloadUrl = resultData["secure_url"] as String
                                Log.d(TAG, "Upload successful: $downloadUrl")

                                Picasso.get().load(downloadUrl).into(object : com.squareup.picasso.Target {
                                    override fun onBitmapLoaded(bitmap: android.graphics.Bitmap?, from: Picasso.LoadedFrom?) {
                                        Log.d(TAG, "Image loaded into Picasso: $downloadUrl")
                                    }

                                    override fun onBitmapFailed(e: java.lang.Exception?, errorDrawable: android.graphics.drawable.Drawable?) {
                                        Log.e(TAG, "Failed to load image into Picasso: $downloadUrl", e)
                                    }

                                    override fun onPrepareLoad(placeHolderDrawable: android.graphics.drawable.Drawable?) {}
                                })

                                continuation.resume(Uri.parse(downloadUrl))
                            }

                            override fun onError(requestId: String, error: com.cloudinary.android.callback.ErrorInfo) {
                                Log.e(TAG, "Upload failed for requestId: $requestId", Exception(error.description))
                                continuation.resumeWithException(Exception(error.description))
                            }

                            override fun onReschedule(requestId: String, error: com.cloudinary.android.callback.ErrorInfo) {
                                Log.w(TAG, "Upload rescheduled for requestId: $requestId")
                            }
                        })
                        .dispatch()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Upload failed for imageId: $imageId, Uri: $imageUri", e)
                throw e
            }
        }

        suspend fun deleteImage(imageId: String) {
            Log.d(TAG, "Deleting image with id: $imageId")

            try {
                val formattedImageId = "images/$imageId"
                Log.d(TAG, "Formatted Image ID for deletion: $formattedImageId")

                val options = mapOf("invalidate" to true)
                val result = MediaManager.get().cloudinary.uploader().destroy(formattedImageId, options)

                if (result.isNotEmpty() && result.containsKey("error")) {
                    Log.e(TAG, "Error deleting image: ${result["error"]}")
                    throw Exception("Error deleting image: ${result["error"]}")
                } else {
                    Log.d(TAG, "Image deleted successfully")
                }

                val cloudinaryBaseUrl = BuildConfig.CLOUDINARY_BASE_URL
                val imageUrl = "$cloudinaryBaseUrl/$formattedImageId"
                Picasso.get().invalidate(imageUrl)
                Log.d(TAG, "Picasso cache invalidated for: $imageUrl")

            } catch (e: Exception) {
                Log.e(TAG, "Error deleting image", e)
                throw e
            }
        }
    }
}
