package kth.mniststreamreader

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentPagerAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView

class ImagePageAdapter(val activity: MainActivity) : FragmentPagerAdapter(activity.supportFragmentManager) {

    /**
     * @return the number of pages to display
     */
    override fun getCount(): Int {
        return activity.croppedBitmaps.count()
    }

    override fun getItem(position: Int): Fragment {
        return SwipeFragment.newInstance(position)
    }

    class SwipeFragment : Fragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                                  savedInstanceState: Bundle?): View? {
            val swipeView = inflater.inflate(R.layout.image_fragment_page, container, false)
            val imageView = swipeView.findViewById(R.id.numberImgView) as ImageView
            val bundle = arguments
            val position = bundle!!.getInt("position")
            imageView.setImageBitmap((activity!! as MainActivity).croppedBitmaps[position])
            return swipeView
        }

        companion object {

            internal fun newInstance(position: Int): SwipeFragment {
                val swipeFragment = SwipeFragment()
                val bundle = Bundle()
                bundle.putInt("position", position)
                swipeFragment.arguments = bundle
                return swipeFragment
            }
        }
    }
}