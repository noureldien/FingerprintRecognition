package com.fingerprintrecognition;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

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

    // endregion Public Methods

    // region Private Methods

    /**
     * Initialize the activity.
     */
    private void initialize(){

        setContentView(R.layout.process);

        // convert to bitmap
        //Bitmap bm = Bitmap.createBitmap(MatSnapShot.cols(), MatSnapShot.rows(), Bitmap.Config.ARGB_8888);
        //Utils.matToBitmap(MatSnapShot, bm);

        //ImageView processImageViewSource = (ImageView)this.findViewById(R.id.processImageViewSource);
        //processImageViewSource.setImageBitmap(bm);
    }

    // endregion Private Methods
}
