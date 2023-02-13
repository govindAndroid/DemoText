package com.andromedalabs.demo

import android.content.Context
import android.graphics.*
import android.graphics.Paint.Align
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Pair
import androidx.appcompat.app.AppCompatActivity
import com.andromedalabs.demo.databinding.ActivityMainBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import java.io.IOException
import java.io.InputStream


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var SELECT_PICTURE = 200
    private var mSelectedImage: Bitmap? = null
    private var temp: Bitmap? = null
    private var mImageMaxWidth: Int? = null

    // Max height (portrait mode)
    private var mImageMaxHeight: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mSelectedImage = getBitmapFromAsset(this, "tech.png")
        binding.imageView.setImageBitmap(mSelectedImage)
        binding.imageView.drawable?.let {
            temp = (it as BitmapDrawable).bitmap
        }


        if (mSelectedImage != null) {
            // Get the dimensions of the View
            val targetedSize: Pair<Int, Int> = getTargetedWidthHeight()!!
            val targetWidth = targetedSize.first
            val maxHeight = targetedSize.second

            // Determine how much to scale down the image
            val scaleFactor = Math.max(
                mSelectedImage!!.getWidth().toFloat() / targetWidth.toFloat(),
                mSelectedImage!!.getHeight().toFloat() / maxHeight.toFloat()
            )
            val resizedBitmap = Bitmap.createScaledBitmap(
                mSelectedImage!!,
                (mSelectedImage!!.getWidth() / scaleFactor).toInt(),
                (mSelectedImage!!.getHeight() / scaleFactor).toInt(),
                true
            )
            binding.imageView.setImageBitmap(resizedBitmap)
            mSelectedImage = resizedBitmap
        }

        runTextRecognition()

    }


    fun getBitmapFromAsset(context: Context, filePath: String?): Bitmap? {
        val assetManager = context.assets
        val `is`: InputStream
        var bitmap: Bitmap? = null
        try {
            `is` = assetManager.open(filePath!!)
            bitmap = BitmapFactory.decodeStream(`is`)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return bitmap
    }
    private fun getTargetedWidthHeight(): Pair<Int, Int>? {
        val targetWidth: Int
        val targetHeight: Int
        val maxWidthForPortraitMode: Int = getImageMaxWidth()!!
        val maxHeightForPortraitMode: Int = getImageMaxHeight()!!
        targetWidth = maxWidthForPortraitMode
        targetHeight = maxHeightForPortraitMode
        return Pair(targetWidth, targetHeight)
    }

    // Functions for loading images from app assets.
    private fun getImageMaxWidth(): Int? {
        if (mImageMaxWidth == null) {
            // Calculate the max width in portrait mode. This is done lazily since we need to
            // wait for
            // a UI layout pass to get the right values. So delay it to first time image
            // rendering time.
            mImageMaxWidth = mSelectedImage!!.getWidth()
        }
        return mImageMaxWidth
    }
    private fun getImageMaxHeight(): Int? {
        if (mImageMaxHeight == null) {
            // Calculate the max width in portrait mode. This is done lazily since we need to
            // wait for
            // a UI layout pass to get the right values. So delay it to first time image
            // rendering time.
            mImageMaxHeight = mSelectedImage!!.getHeight()
        }
        return mImageMaxHeight
    }

    private fun runTextRecognition() {
        val image = InputImage.fromBitmap(mSelectedImage!!, 0)
        val recognizer = TextRecognition.getClient()
        binding.imageView.setEnabled(false)
        recognizer.process(image)
            .addOnSuccessListener { texts ->
                processTextRecognitionResult(texts)
            }
            .addOnFailureListener { e -> // Task failed with an exception
                e.printStackTrace()
            }
    }

    private fun processTextRecognitionResult(texts: Text) {
        val blocks = texts.textBlocks
        if (blocks.size == 0) {
//            showToast("No text found")
            return
        }
        binding.graphicOverlay.clear()
        for (i in blocks.indices) {
            val lines = blocks[i].lines
            for (j in lines.indices) {
                val elements = lines[j].elements
                for (k in elements.indices) {
//                    val textGraphic: GraphicOverlay.Graphic = TextGraphic(binding.graphicOverlay, elements[k])
//                    binding.graphicOverlay.add(textGraphic)
                    binding.imageView.setImageDrawable(writeTextOnDrawable(mSelectedImage!!,elements[k].text))
                    break
                }
            }
        }
    }

    private fun writeTextOnDrawable(drawableId: Bitmap, text: String): BitmapDrawable? {
//        val bm = drawableId
        val bm: Bitmap = drawableId.copy(Bitmap.Config.ARGB_8888, true)
        val tf = Typeface.create("Helvetica", Typeface.BOLD)
        val paint = Paint()
        paint.setStyle(Paint.Style.FILL)
        paint.setColor(Color.RED)
        paint.setTypeface(tf)
        paint.setTextAlign(Align.CENTER)
        paint.setTextSize(convertToPixels(this, 25))
        val textRect = Rect()
        paint.getTextBounds(text, 0, text.length, textRect)
        val canvas = Canvas(bm)

        //If the text is bigger than the canvas , reduce the font size
        if (textRect.width() >= canvas.getWidth() - 4) //the padding on either sides is considered as 4, so as to appropriately fit in the text
            paint.setTextSize(
                convertToPixels(
                    this,
                    7
                )
            ) //Scaling needs to be used for different dpi's

        //Calculate the positions
        val xPos: Int = canvas.getWidth() / 2 - 2 //-2 is for regulating the x position offset

        //"- ((paint.descent() + paint.ascent()) / 2)" is the distance from the baseline to the center.
        val yPos = (canvas.getHeight() / 2 - (paint.descent() + paint.ascent()) / 2)
        canvas.drawText(text, xPos.toFloat(), yPos.toFloat(), paint)
        return BitmapDrawable(resources, bm)
    }

    fun convertToPixels(context: Context, nDP: Int): Float {
        val conversionScale = context.resources.displayMetrics.density
        return (nDP * conversionScale + 0.5f)
    }
https://stackoverflow.com/questions/6650398/android-imageview-zoom-in-and-zoom-out
}
