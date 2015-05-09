package com.fingerprintrecognition;

import android.app.ActionBar;
import android.content.Intent;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;
import com.fingerprintrecognition.app.*;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.*;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnTouchListener;

/**
 * Show camera and take snapShot.
 */
public class CameraActivity extends Activity implements OnTouchListener, CvCameraViewListener2 {

    // region Private Static Variables

    private static final String TAG = "FingerprintRecognition::CameraActivity";

    // endregion Private Variables

    // region Private Variables

    private Mat matCameraFrame;
    private AppJavaCameraView cameraView;
    private android.hardware.Camera.Size cameraSize;

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
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
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

        processOnTouch();
        return false;
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

        matCameraFrame = inputFrame.rgba();
        drawEllipse(matCameraFrame);
        return matCameraFrame;
    }

    // endregion Public Methods

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

        cameraView = (AppJavaCameraView) findViewById(R.id.cameraCameraView);
        cameraView.setCvCameraViewListener(this);
        cameraView.setFocusable(true);
        cameraView.setFocusableInTouchMode(true);
    }

    /**
     * Draw ellipse to guide user to put finger inside it.
     *
     * @param img
     */
    private void drawEllipse(Mat img) {

        Point center = new Point(cameraSize.width / 2, cameraSize.height / 2);
        Size axes = new Size(180, 120);
        int thickness = 2;
        int lineType = 8;

        Core.ellipse(img, center, axes, 0, 0, 360, AppUtils.ThemeColor, thickness, lineType, 0);
    }

    /**
     * Process the onTouch event.
     */
    private void processOnTouch() {

        // take snapshot
        Mat matSnapShot = takeSnapShort();

        // pass it to the process activity then navigate to it
        ProcessActivity.MatSnapShot = matSnapShot;
        navigateToProcessActivity();
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
        Size axes = new Size(180, 120);
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

        return matGrayScale;
    }

    /**
     * Navigate to the 'Main' activity.
     */
    private void navigateToProcessActivity() {

        // navigate to Process activity
        Intent intent = new Intent(this, com.fingerprintrecognition.ProcessActivity.class);
        this.startActivity(intent);
    }

    // endregion Private Methods
}
