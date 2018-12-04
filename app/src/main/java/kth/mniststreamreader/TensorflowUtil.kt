package kth.mniststreamreader

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.contrib.android.TensorFlowInferenceInterface


class TensorflowUtil {
    companion object {
        private val TAG = "TfUtil"
        private val INPUT_NAME = "conv2d_1_input"
        private val OUTPUT_NAME = "dense_2/Softmax"
        private val MODEL_FILE = "file:///android_asset/MNIST_model.pb"
        fun getModel(context: Context): TensorFlowInferenceInterface {
            Log.d(TAG, "Loading TF-model")
            return TensorFlowInferenceInterface(context.assets, MODEL_FILE)
        }

        fun predict(bitmap: Bitmap, model: TensorFlowInferenceInterface): FloatArray {
            Log.d(TAG, "Beginning prediction based on bitmap")
            val numClasses: Int = model.graph().operation(OUTPUT_NAME).output<Float>(0).shape().size(1).toInt()
            //+1 below (maybe for tre extra dimension in the output?) or else we for some reason get IndexOutOfBounds exception
            val outputs = FloatArray(numClasses + 1)
            val input = ImgUtils.getImageArrayForPrediction(bitmap)
            Log.d(TAG, "Feeding bitmap to TF-model")
            model.feed(INPUT_NAME, input, 1, bitmap.width.toLong(), bitmap.height.toLong(), 1) //shape:(?,img_rows, img_cols, 1)
            Log.d(TAG, "Running prediction..")
            model.run(arrayOf(OUTPUT_NAME), false)
            Log.d(TAG, "Fetching predicted results")
            model.fetch(OUTPUT_NAME, outputs)
            Log.d(TAG, "Returning results")
            return outputs
        }
    }
}