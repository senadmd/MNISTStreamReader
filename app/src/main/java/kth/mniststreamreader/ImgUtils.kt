package kth.mniststreamreader

import android.content.ContentResolver
import android.graphics.*
import android.graphics.Rect
import android.net.Uri
import android.os.AsyncTask
import android.os.ParcelFileDescriptor
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.io.FileDescriptor

class ImgUtils {
    companion object {
        private val INTENSITY_TRESHOLD = 150
        private val MAX_INTENSITY = Math.pow(2.0, 8.0).toInt() - 1
        fun getBitmap(content: Uri, contentResolver: ContentResolver): Bitmap {
            val parcelFileDescriptor: ParcelFileDescriptor = contentResolver.openFileDescriptor(content, "r")
            val fileDescriptor: FileDescriptor = parcelFileDescriptor.fileDescriptor
            val orgBitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor, Rect(), BitmapFactory.Options())
            if (orgBitmap.width > 1020 || orgBitmap.height > 1020) { //downsample if greater than HD
                var options = BitmapFactory.Options()
                options.inTargetDensity = 10 //reduce density
                options.inSampleSize = 4 //downsample the image by 4
                orgBitmap.recycle()
                return BitmapFactory.decodeFileDescriptor(fileDescriptor, Rect(), options)
            } else return orgBitmap
        }

        fun getImageArrayForPrediction(bitmap: Bitmap): FloatArray {
            val matrix = Mat()
            val matrixOut = Mat()
            Utils.bitmapToMat(bitmap, matrix);
            Imgproc.cvtColor(matrix, matrixOut, Imgproc.COLOR_RGB2GRAY) // Make sure it is grayscale
            val size = bitmap.width * bitmap.height
            val arrayOut = FloatArray(size)
            var cnt = 0
            for (x in 0 until bitmap.height) {
                for (y in 0 until bitmap.width) {
                    arrayOut[cnt] = matrixOut.get(x, y).first().toFloat() / 255f //TF handles only floats
                    cnt++;
                }
            }
            return arrayOut
        }

        fun toGrayscaleDownsampled(bmpOriginal: Bitmap): Bitmap {
            val width: Int
            val height: Int
            height = bmpOriginal.height
            width = bmpOriginal.width

            val bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmpGrayscale)
            val paint = Paint()
            val cm = ColorMatrix()
            cm.setSaturation(0f)
            val f = ColorMatrixColorFilter(cm)
            paint.colorFilter = f
            c.drawBitmap(bmpOriginal, Matrix(), paint)
            return bmpGrayscale
        }

        private fun getDilatedOrgImage(input: Mat): Mat {
            var contourList = ArrayList<MatOfPoint>()
            var dilatedImg: Mat = Mat()
            var tmp: Mat = Mat()
            Imgproc.threshold(input, tmp, 0.0, 255.0, Imgproc.THRESH_BINARY or Imgproc.THRESH_OTSU)
            Imgproc.dilate(tmp, dilatedImg, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(2.0, 2.0)))
            Imgproc.findContours(dilatedImg, contourList, tmp, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
            Imgproc.drawContours(dilatedImg, contourList, -1, Scalar(255.0, 255.0, 255.0), 2)
            return dilatedImg
        }

        private fun getIntensity(color: Int): Int {
            val red = color shr 16 and 0xFF
            val blue = color shr 0 and 0xFF
            val green = color shr 8 and 0xFF
            assert(red == blue && green == blue) // doesn't hold for green in RGB 565
            return MAX_INTENSITY - blue
        }

        private fun removePixelsBelowThreshold(bitmap: Bitmap) {
            var currentBit: Int
            var orgValue = 0
            for (y in 0 until bitmap.height) {
                for (x in 0 until bitmap.width) {
                    orgValue = bitmap.getPixel(x, y)
                    if (x == 0 && y == 0 && orgValue == -1) return //the background is white, no need for separation
                    currentBit = getIntensity(orgValue)
                    if (currentBit < INTENSITY_TRESHOLD) // set pixel to white color as the pixel is below threshold
                        bitmap.setPixel(x, y, Color.rgb(255, 255, 255))

                }

            }
        }
    }

    class AsyncBitmapExtractor(val onBitmapUpdateCallback: BitmapUpdateCallback) : AsyncTask<Pair<Int, Bitmap>, Void, Pair<Bitmap, List<Bitmap>>>() {

        override fun doInBackground(vararg bitmap: Pair<Int, Bitmap>): Pair<Bitmap, List<Bitmap>> {
            val numbersFound = ArrayList<Bitmap>()
            val bitmapOrientation = bitmap.first().first
            val nBitmap = bitmap.first().second
            ImgUtils.removePixelsBelowThreshold(nBitmap) // Get only pixels above threshold
            var matrix = Mat()
            var matrixOut = Mat()
            var blackHatOut = Mat()
            var contourList = ArrayList<MatOfPoint>()
            Utils.bitmapToMat(nBitmap, matrix);
            Imgproc.cvtColor(matrix, matrixOut, Imgproc.COLOR_RGB2GRAY)
            var kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(2.0, 2.0))
            Imgproc.morphologyEx(matrixOut, matrix, Imgproc.MORPH_BLACKHAT, kernel) //blackhat
            var dilatedImg = ImgUtils.getDilatedOrgImage(matrix) //get the original image dilated
            // expand contours based on adaptive threshold
            Imgproc.adaptiveThreshold(matrix, matrixOut, 255.0, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 11, 2.0)

            Imgproc.threshold(matrixOut, blackHatOut, 0.0, 255.0, Imgproc.THRESH_BINARY_INV or Imgproc.THRESH_OTSU)
            Imgproc.dilate(blackHatOut, matrix, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(2.0, 2.0)))
            var hierarchy = Mat()
            Imgproc.findContours(matrix, contourList, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
            Imgproc.cvtColor(matrix, matrixOut, Imgproc.COLOR_GRAY2RGB)

            val avgArea = contourList.sumBy { Imgproc.contourArea(it).toInt() } / contourList.count()
            for (cont in contourList) {
                if (Imgproc.contourArea(cont) < avgArea / 2) continue //skip contours below average area
                var numbBitmap = Bitmap.createBitmap(28, 28, Bitmap.Config.ARGB_8888)
                var rect = Imgproc.boundingRect(cont)
                var newImg = dilatedImg.submat(rect)
                var sz = Size(numbBitmap.width.toDouble(), numbBitmap.height.toDouble())
                Imgproc.resize(newImg, matrix, sz)
                //Core.bitwise_not(matrix, newImg) enable this if background should be white instead of black
                Utils.matToBitmap(matrix, numbBitmap)
                //change rotation of identified objects based on main image´s exif information
                numbBitmap = ExifUtils.checkExifAndRotate(bitmapOrientation, numbBitmap)
                numbersFound.add(numbBitmap)
                Imgproc.rectangle(matrixOut, org.opencv.core.Point(rect.x.toDouble(), rect.y.toDouble()), org.opencv.core.Point((rect.x + rect.width).toDouble(), (rect.y + rect.height).toDouble()), Scalar(0.0, 255.0, 0.0), 2)
            }
            Utils.matToBitmap(matrixOut, nBitmap)

            return Pair(nBitmap, numbersFound)
        }


        override fun onPostExecute(result: Pair<Bitmap, List<Bitmap>>?) {
            super.onPostExecute(result)
            val ctx = onBitmapUpdateCallback.getActivityContext()
            ctx.runOnUiThread { onBitmapUpdateCallback.onBitmapUpdated(result!!.first) }
            ctx.runOnUiThread { onBitmapUpdateCallback.onGotNumbers(result!!.second) }
        }
    }
}