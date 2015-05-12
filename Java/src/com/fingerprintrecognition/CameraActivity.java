package com.fingerprintrecognition;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.util.Log;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.fingerprintrecognition.app.*;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.*;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.features2d.*;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View.OnTouchListener;

import java.util.*;

/**
 * Show camera and take snapShot.
 */
public class CameraActivity extends Activity implements OnTouchListener, CvCameraViewListener2 {

    // region Private Static Variables

    private static final String TAG = "FingerprintRecognition::CameraActivity";
    private static HashMap<String, Mat> processedImages;

    // endregion Private Variables

    // region Private Variables

    private Mat matCameraFrame;
    private AppJavaCameraView cameraView;
    private ImageView imageView;
    private TextView textViewCounter;
    private android.hardware.Camera.Size cameraSize;
    private int maskWidth;
    private int maskHeight;

    // endregion Private Variables

    // region Private Delegates

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    cameraView.enableView();
                    cameraView.setOnTouchListener(CameraActivity.this);
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    // endregion Private Delegates

    // region Constructor

    public CameraActivity() {


    }

    // endregion Constructor

    // region Public Static Method

    /**
     * Add processed image to the list.
     *
     * @param image
     * @param name
     */
    public static void addProcessedImage(Mat image, String name) {

        processedImages.put(name, image);

    }

    /**
     * Get processed image from the list, with the given name/key.
     *
     * @param name
     */
    public static Mat getProcessedImage(String name) {

        return processedImages.get(name);
    }

    /**
     * Get processed image from the list, with the given index.
     *
     * @param index
     */
    public static Mat getProcessedImage(int index) {

        ArrayList keys = new ArrayList(processedImages.keySet());
        return processedImages.get(keys.get(index));
    }

    /**
     * Get names of the processed images.
     */
    public static Object[] getProcessedImageNames() {

        return processedImages.keySet().toArray();
    }

    /**
     * Count processed images.
     */
    public static int processedImageCount() {

        return processedImages.size();
    }

    // endregion Public Static Method

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
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                // exist the application by finishing the Camera activity
                this.finish();
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (cameraView != null) {
            cameraView.disableView();
        }
    }

    @Override
    public void onResume() {

        super.onResume();

        // re-load openCV
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);

        // this is to over-come the problem of SIFT does not exist in default openCV
        try {
            // Load necessary libraries.
            System.loadLibrary("opencv_java");
            System.loadLibrary("nonfree");
            System.loadLibrary("nonfree_jni");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Couldn't load this libs");
        }

        // update counter
        int count = processedImageCount();
        textViewCounter.setText(Integer.toString(count));
    }

    public void onDestroy() {
        super.onDestroy();
        if (cameraView != null) {
            cameraView.disableView();
        }
    }

    public void onCameraViewStarted(int width, int height) {

        // get size of the screen
        android.view.Display display = getWindowManager().getDefaultDisplay();
        android.graphics.Point size = new android.graphics.Point();
        display.getSize(size);

        // set resolution of the camera to fill all the screen
        cameraSize = cameraView.getResolution();
        cameraSize.height = size.x * height / width;
        cameraSize.width = size.x;
        cameraView.setResolution(cameraSize);
        cameraView.setFocusMode(this.getApplicationContext(), 5);

        // must be initialized after setting the camera resolution
        matCameraFrame = new Mat(cameraSize.height, cameraSize.width, CvType.CV_8UC4);
    }

    public void onCameraViewStopped() {

        matCameraFrame.release();
    }

    public boolean onTouch(View v, MotionEvent event) {

        cameraView_OnTouch(v, event);
        return false;
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

        matCameraFrame = inputFrame.rgba();
        drawEllipse(matCameraFrame);
        return matCameraFrame;
    }

    // endregion Public Methods

    // region Private Event Handlers

    /**
     * Navigate to process activity.
     *
     * @param view
     */
    private void buttonProcess_OnClick(View view) {

        // navigate to Process activity
        Intent intent = new Intent(this, com.fingerprintrecognition.ProcessActivity.class);
        this.startActivity(intent);
    }

    /**
     * Retake the image.
     *
     * @param view
     */
    private void buttonRetake_OnClick(View view) {

        // hide imageView and show cameraView
        cameraView.setVisibility(View.VISIBLE);
        imageView.setVisibility(View.INVISIBLE);
    }

    /**
     * Navigate to Settings activity.
     *
     * @param view
     */
    private void buttonSettings_OnClick(View view) {

        // navigate to Settings activity
        Intent intent = new Intent(this, com.fingerprintrecognition.SettingsActivity.class);
        this.startActivity(intent);
    }

    /**
     * Process the onTouch event.
     */
    private void cameraView_OnTouch(View view, MotionEvent event) {

        // take snapshot
        Mat snapShot = takeSnapShort();
        int rows = snapShot.rows();
        int cols = snapShot.cols();
        ProcessActivity.MatSnapShot = snapShot;
        ProcessActivity.MatSnapShotMask = snapShotMask(rows, cols, 10);
        MatchActivity.MatMatchMask = snapShotMask(rows, cols, 20);

        // set it to the imageView
        imageView.setImageBitmap(matToBitmap(snapShot));

        // show imageView and hide cameraView
        cameraView.setVisibility(View.INVISIBLE);
        imageView.setVisibility(View.VISIBLE);
    }

    // endregion Private Event Handlers

    // region Private Methods

    /**
     * Initialize the activity.
     */
    private void initialize() {

        setContentView(R.layout.camera);

        // disable screen sleep
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // set Home button of the ActionBar as back
        ActionBar actionBar = this.getActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        maskWidth = 260;
        maskHeight = 160;

        // processed images
        processedImages = new HashMap<String, Mat>();

        // get views
        textViewCounter = (TextView) findViewById(R.id.cameraTextViewCounter);
        imageView = (ImageView) findViewById(R.id.cameraImageView);
        cameraView = (AppJavaCameraView) findViewById(R.id.cameraCameraView);

        // adjust camera view
        cameraView.setCvCameraViewListener(this);
        cameraView.setFocusable(true);
        cameraView.setFocusableInTouchMode(true);

        // event handlers
        Button buttonRetake = (Button) findViewById(R.id.cameraButtonRetake);
        buttonRetake.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonRetake_OnClick(view);
            }
        });
        Button buttonProcess = (Button) findViewById(R.id.cameraButtonProcess);
        buttonProcess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonProcess_OnClick(view);
            }
        });
        Button buttonSettings = (Button) findViewById(R.id.cameraButtonSettings);
        buttonSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonSettings_OnClick(view);
            }
        });
    }

    /**
     * Draw ellipse to guide user to put finger inside it.
     *
     * @param img
     */
    private void drawEllipse(Mat img) {

        Point center = new Point(cameraSize.width / 2, cameraSize.height / 2);
        Size axes = new Size(maskWidth, maskHeight);
        int thickness = 2;
        int lineType = 8;

        Core.ellipse(img, center, axes, 0, 0, 360, AppUtils.ThemeColor, thickness, lineType, 0);
    }

    /**
     * Take snapshot, convert it to grayScale, crop it using the ellipse and return it.
     *
     * @return
     */
    private Mat takeSnapShort() {

        int rows = matCameraFrame.rows();
        int cols = matCameraFrame.cols();
        int width = cameraSize.width;
        int height = cameraSize.height;

        // get graysScale
        Mat matGrayScale = new Mat(rows, cols, CvType.CV_8UC1);
        Imgproc.cvtColor(matCameraFrame, matGrayScale, Imgproc.COLOR_RGB2GRAY);

        // crop using ellipse and masking
        Mat roi = new Mat(rows, cols, CvType.CV_8UC1);

        Point center = new Point(width / 2, height / 2);
        Size axes = new Size(maskWidth, maskHeight);
        Scalar scalarWhite = new Scalar(255, 255, 255);
        Scalar scalarGray = new Scalar(100, 100, 100);
        Scalar scalarBlack = new Scalar(0, 0, 0);
        int thickness = -1;
        int lineType = 8;

        // method 1: crop using ellipse and mask
        //roi.setTo(scalarBlack);
        //Core.ellipse(roi, center, axes, 0, 0, 360, scalarWhite, thickness, lineType, 0);
        //Core.bitwise_and(matGrayScale, matGrayScale, matGrayScale, roi);
        //roi.release();

        // method 2: fill with gray instead of while
        roi.setTo(scalarWhite);
        Core.ellipse(roi, center, axes, 0, 0, 360, scalarBlack, thickness, lineType, 0);
        matGrayScale.setTo(scalarGray, roi);
        roi.release();

        // now crop the image to the boundaries of the ellipse
        int colStart = (int) ((width - axes.width * 2) / 2);
        int rowStart = ((int) (height - axes.height * 2) / 2);
        matGrayScale = matGrayScale.submat(new Rect(colStart, rowStart, (int) axes.width * 2, (int) axes.height * 2));

        // now scale the image
        //float scaleFactor = 1 / 4;
        //int newWidth = (int) (matGrayScale.cols() * scaleFactor);
        //int newHeight = height * newWidth / width;
        //Mat matScaled = new Mat(newHeight, newWidth, CvType.CV_8UC1);
        //Imgproc.resize(matGrayScale, matScaled, new Size(newWidth, newHeight));
        //return matScaled;

        return matGrayScale;
    }

    /**
     * Mask used in the snapshot.
     *
     * @return
     */
    private Mat snapShotMask(int rows, int cols, int offset) {

        Point center = new Point(cols / 2, rows / 2);
        Size axes = new Size(maskWidth - offset, maskHeight - offset);
        Scalar scalarWhite = new Scalar(255, 255, 255);
        Scalar scalarGray = new Scalar(100, 100, 100);
        Scalar scalarBlack = new Scalar(0, 0, 0);
        int thickness = -1;
        int lineType = 8;

        Mat mask = new Mat(rows, cols, CvType.CV_8UC1, scalarBlack);
        Core.ellipse(mask, center, axes, 0, 0, 360, scalarWhite, thickness, lineType, 0);
        return mask;
    }

    /**
     * Convert Mat image to bitmap image.
     *
     * @param image
     */
    private Bitmap matToBitmap(Mat image) {

        Bitmap bitmap = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(image, bitmap);
        return bitmap;
    }

    // endregion Private Methods
}
