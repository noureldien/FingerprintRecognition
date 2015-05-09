package com.fingerprintrecognition;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.widget.ImageView;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

/**
 * Process the image to extract skeleton from it.
 */
public class ProcessActivity extends Activity {

    // region Public Static Members

    public static Mat MatSnapShot;

    // endregion Public Static Members

    // region Constructor

    public ProcessActivity() {


    }

    // endregion Constructor

    // region Public Methods

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        initialize();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){

        switch (item.getItemId()){
            case android.R.id.home:
                // return back to Camera activity
                this.finish();
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    // endregion Public Methods

    // region Private Methods

    /**
     * Initialize the activity.
     */
    private void initialize(){

        setContentView(R.layout.process);

        // set Home button of the ActionBar as back
        ActionBar actionBar = this.getActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        // convert to bitmap and show
        Bitmap bm = Bitmap.createBitmap(MatSnapShot.cols(), MatSnapShot.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(MatSnapShot, bm);

        ImageView processImageViewSource = (ImageView)this.findViewById(R.id.processImageViewSource);
        processImageViewSource.setImageBitmap(bm);

        // start processing the image
        processImage();
    }

    /**
     * Process the image to get the skeleton.
     */
    private void processImage(){

        int rows = MatSnapShot.rows();
        int cols = MatSnapShot.cols();
        int width = MatSnapShot.width();
        int height = MatSnapShot.height();
        int cvType = CvType.CV_8UC1;

        // apply histogram equalization
        Mat histogramEqual = new Mat(rows, cols, cvType);
        Imgproc.equalizeHist(MatSnapShot, histogramEqual);

        // normalise image to have zero mean and 1 standard deviation
        Mat normalized = new Mat(rows, cols, cvType);
        Core.normalize(histogramEqual, normalized);
    }

    // endregion Private Methods
}
