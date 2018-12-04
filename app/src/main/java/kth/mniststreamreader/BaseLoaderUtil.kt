package kth.mniststreamreader

import android.content.Context
import android.util.Log
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface


class BaseLoaderUtil {
    companion object {
        fun getBaseLoader(context: Context): BaseLoaderCallback {
            val TAG = "BaseLoaderUtil"
            return object : BaseLoaderCallback(context) {
                override fun onManagerConnected(status: Int) {
                    when (status) {
                        LoaderCallbackInterface.SUCCESS -> {
                            Log.i(TAG, "OpenCV loaded successfully")
                        }
                        else -> {
                            super.onManagerConnected(status)
                        }
                    }
                }
            }
        }
    }

}