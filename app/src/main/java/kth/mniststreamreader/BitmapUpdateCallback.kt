package kth.mniststreamreader

import android.graphics.Bitmap

interface BitmapUpdateCallback {
    fun onBitmapUpdated(bitmap: Bitmap)
    fun onGotNumbers(bitmaps: List<Bitmap>)
    fun getActivityContext(): MainActivity
}