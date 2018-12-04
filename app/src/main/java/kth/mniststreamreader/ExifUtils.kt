package kth.mniststreamreader

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.FileDescriptor


class ExifUtils {
    companion object {
        fun getExifOrientation(content: Uri, contentResolver: ContentResolver): Int {
            val parcelFileDescriptor: ParcelFileDescriptor = contentResolver.openFileDescriptor(content, "r")
            val fileDescriptor: FileDescriptor = parcelFileDescriptor.fileDescriptor
            val ei = ExifInterface(fileDescriptor)
            return ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED)
        }

        fun checkExifAndRotate(orientation: Int, bitmap: Bitmap): Bitmap {
            var rotatedBitmap: Bitmap? = null
            when (orientation) {

                ExifInterface.ORIENTATION_ROTATE_90 -> rotatedBitmap = rotateImage(bitmap, 90f)

                ExifInterface.ORIENTATION_ROTATE_180 -> rotatedBitmap = rotateImage(bitmap, 180f)

                ExifInterface.ORIENTATION_ROTATE_270 -> rotatedBitmap = rotateImage(bitmap, 270f)

                ExifInterface.ORIENTATION_NORMAL -> rotatedBitmap = bitmap
                else -> rotatedBitmap = bitmap
            }
            return rotatedBitmap
        }

        private fun rotateImage(source: Bitmap, angle: Float): Bitmap {
            val matrix = Matrix()
            matrix.postRotate(angle)
            return Bitmap.createBitmap(source, 0, 0, source.width, source.height,
                    matrix, true)
        }
    }
}