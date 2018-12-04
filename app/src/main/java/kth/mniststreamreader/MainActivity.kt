package kth.mniststreamreader

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v4.view.ViewPager
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader


class MainActivity : FragmentActivity() {
    val PICK_IMAGE_CODE = 329
    val TAG = "MNISTMain"
    val activity = this
    val croppedBitmaps = ArrayList<Bitmap>()
    var croppedImageViewPager: ViewPager? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)
        val btn = findViewById<Button>(R.id.imgSelectButton)
        btn.setOnClickListener {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_CODE)
        }
        var mLoaderCallback: BaseLoaderCallback = BaseLoaderUtil.getBaseLoader(this)
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback!!.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        croppedImageViewPager = findViewById<ViewPager>(R.id.viewpager)
        val predictBtn = findViewById<Button>(R.id.predictSelectedBtn)
        predictBtn.setOnClickListener {
            if (croppedImageViewPager != null) { //<- check that adapter is initialized/ i.e. we have selected an image
                Log.d(TAG, "Instantiating TF and the bitmap used for prediction")
                val bitmap = activity.croppedBitmaps[croppedImageViewPager!!.currentItem]
                val model = TensorflowUtil.getModel(activity)
                val prediction = TensorflowUtil.predict(bitmap, model)
                Log.d(TAG, "Got prediction using TF, setting text to the predicted class")
                val argMax = prediction.indices.maxBy { prediction[it] } ?: -1
                val predictionTxt = findViewById<TextView>(R.id.predictedNumbTxt)
                predictionTxt.visibility = View.VISIBLE
                predictionTxt.text = "Predicted: $argMax"
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == PICK_IMAGE_CODE && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                //Display an error
                return;
            } else {
                var bitm = ImgUtils.getBitmap(data!!.getData(), contentResolver)
                bitm = ImgUtils.toGrayscaleDownsampled(bitm)
                val imgView = findViewById<ImageView>(R.id.imageView)
                imgView.setImageBitmap(bitm)
                val exifOrientation = ExifUtils.getExifOrientation(data!!.getData(), contentResolver)
                ImgUtils.AsyncBitmapExtractor(object : BitmapUpdateCallback {
                    override fun onBitmapUpdated(bitmap: Bitmap) {
                        imgView.setImageBitmap(bitmap)
                    }

                    override fun onGotNumbers(bitmaps: List<Bitmap>) {
                        val predictBtn = findViewById<Button>(R.id.predictSelectedBtn)
                        predictBtn.visibility = View.VISIBLE
                        croppedBitmaps.clear()
                        croppedBitmaps.addAll(bitmaps)
                        if (croppedImageViewPager!!.adapter == null)
                            croppedImageViewPager!!.adapter = ImagePageAdapter(activity)
                        else croppedImageViewPager!!.adapter!!.notifyDataSetChanged()
                    }

                    override fun getActivityContext(): MainActivity {
                        return activity
                    }
                }).execute(Pair(exifOrientation, bitm))
            }
        }
    }

}