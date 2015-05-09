package com.fingerprintrecognition;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;
import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Process the image to extract skeleton from it.
 */
public class ProcessActivity extends Activity {

    // region Public Static Members

    public static Mat MatSnapShot;

    // endregion Public Static Members

    // region Private Static Variables

    private static final String TAG = "FingerprintRecognition::ProcessActivity";

    // endregion Private Variables

    // region Private Variables

    private ImageView processImageViewSource;

    //endregion Private Variables

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
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
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
    private void initialize() {

        setContentView(R.layout.process);

        // set Home button of the ActionBar as back
        ActionBar actionBar = this.getActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        // convert to bitmap and show
        processImageViewSource = (ImageView) this.findViewById(R.id.processImageViewSource);
        processImageViewSource.setImageBitmap(matToBitmap(MatSnapShot));

        // start processing the image
        processImage();
    }

    /**
     * Process the image to get the skeleton.
     */
    private void processImage() {

        int rows = MatSnapShot.rows();
        int cols = MatSnapShot.cols();
        int width = MatSnapShot.width();
        int height = MatSnapShot.height();

        // apply histogram equalization
        Mat equalized = new Mat(rows, cols, CvType.CV_8UC1);
        Imgproc.equalizeHist(MatSnapShot, equalized);

        // normalise image to have zero mean and 1 standard deviation
        Mat normalized = new Mat(rows, cols, CvType.CV_32FC1);
        Core.normalize(equalized, normalized, 0, 1, Core.NORM_MINMAX, CvType.CV_32FC1);

        Mat normalized_ = new Mat(rows, cols, CvType.CV_32FC1);
        normalizeImage(equalized, normalized_);

        Log.i(TAG, "Hi There");
        Log.i(TAG, normalized.dump());
        Log.i(TAG, normalized_.dump());


        if (true)
            return;

        // step 1: get ridge segment by padding then do block process
        int blockSize = 16;
        double threshold = 0.05;
        Mat padded = imagePadding(normalized, blockSize);
        Mat matRidgeSegment = new Mat(padded.rows(), padded.cols(), CvType.CV_32FC1);
        Mat mask = new Mat(padded.rows(), padded.cols(), CvType.CV_8UC1);
        ridgeSegment(padded, matRidgeSegment, mask, blockSize, threshold);

        // step 2: get ridge orientation
        int gradientSigma = 1;
        int blockSigma = 5;
        int orientSmoothSigma = 5;
        Mat matRidgeOrientation = new Mat(padded.rows(), padded.cols(), CvType.CV_32FC1);
        ridgeOrientation(matRidgeSegment, matRidgeOrientation, gradientSigma, blockSigma, orientSmoothSigma);


        //Mat result = new Mat(rows, cols, CvType.CV_32FC1);
        //Core.normalize(matRidgeOrientation, result, 0, 255, Core.NORM_MINMAX, CvType.CV_32FC1);
        //processImageViewSource.setImageBitmap(matToBitmap(matRidgeOrientation));

        Log.i(TAG, "Hi There");
        Log.i(TAG, matRidgeOrientation.dump());
    }

    /**
     * calculate ridge segment by doing block process for the given image using the given block size.
     *
     * @param source
     * @param blockSize
     * @return
     */
    private void ridgeSegment(Mat source, Mat result, Mat mask, int blockSize, double threshold) {

        // for each block, get standard deviation
        // and replace the block with it
        int widthSteps = source.width() / blockSize;
        int heightSteps = source.height() / blockSize;

        MatOfDouble mean = new MatOfDouble(0);
        MatOfDouble std = new MatOfDouble(0);
        Mat window;
        Scalar scalarBlack = Scalar.all(0);
        Scalar scalarWhile = Scalar.all(255);

        Mat windowMask = new Mat(source.rows(), source.cols(), CvType.CV_8UC1);

        Rect roi;
        double[] stdVals;
        double stdVal;

        for (int y = 1; y <= heightSteps; y++) {
            for (int x = 1; x <= widthSteps; x++) {

                roi = new Rect((blockSize) * (x - 1), (blockSize) * (y - 1), blockSize, blockSize);
                windowMask.setTo(scalarBlack);
                Core.rectangle(windowMask, new Point(roi.x, roi.y), new Point(roi.x + roi.width, roi.y + roi.height), scalarWhile);

                window = source.submat(roi);
                Core.meanStdDev(window, mean, std);
                stdVals = std.toArray();
                stdVal = stdVals[0];

                result.setTo(Scalar.all(stdVal), windowMask);

                // mask used to calc mean and standard deviation later
                mask.setTo(Scalar.all(stdVal >= threshold ? 1 : 0), windowMask);
            }
        }

        // get mean and standard deviation
        Core.meanStdDev(result, mean, std);
        double m = mean.toArray()[0];
        double s = std.toArray()[0];
        Core.subtract(result, Scalar.all(m), result);
        Core.divide(result, Scalar.all(s), result);
    }

    /**
     * Calculate ridge orientation.
     */
    private void ridgeOrientation(Mat source, Mat result, int gradientSigma, int blockSigma, int orientSmoothSigma) {

        int rows = source.rows();
        int cols = source.cols();

        // calculate image gradients
        int kSize = Math.round(6 * gradientSigma);
        if (kSize % 2 == 0) {
            kSize++;
        }
        Mat kernel = Imgproc.getGaussianKernel(kSize, gradientSigma, CvType.CV_32FC1);

        Mat fXKernel = new Mat(1, 3, CvType.CV_32FC1);
        Mat fYKernel = new Mat(3, 1, CvType.CV_32FC1);
        fXKernel.put(0, 0, -1);
        fXKernel.put(0, 1, 0);
        fXKernel.put(0, 2, 1);
        fYKernel.put(0, 0, -1);
        fYKernel.put(1, 0, 0);
        fYKernel.put(2, 0, 1);

        Mat fX = new Mat(kSize, kSize, CvType.CV_32FC1);
        Mat fY = new Mat(kSize, kSize, CvType.CV_32FC1);
        Imgproc.filter2D(kernel, fX, CvType.CV_32FC1, fXKernel);
        Imgproc.filter2D(kernel, fY, CvType.CV_32FC1, fYKernel);

        Mat gX = new Mat(rows, cols, CvType.CV_32FC1);
        Mat gY = new Mat(rows, cols, CvType.CV_32FC1);
        Imgproc.filter2D(source, gX, CvType.CV_32FC1, fX);
        Imgproc.filter2D(source, gY, CvType.CV_32FC1, fY);

        // covariance data for the image gradients
        Mat gXX = new Mat(rows, cols, CvType.CV_32FC1);
        Mat gXY = new Mat(rows, cols, CvType.CV_32FC1);
        Mat gYY = new Mat(rows, cols, CvType.CV_32FC1);
        Core.multiply(gX, gX, gXX);
        Core.multiply(gX, gY, gXY);
        Core.multiply(gY, gY, gYY);

        // smooth the covariance data to perform a weighted summation of the data.
        kSize = Math.round(6 * blockSigma);
        if (kSize % 2 == 0) {
            kSize++;
        }
        kernel = Imgproc.getGaussianKernel(kSize, blockSigma, CvType.CV_32FC1);
        Imgproc.filter2D(gXX, gXX, CvType.CV_32FC1, kernel);
        Imgproc.filter2D(gYY, gYY, CvType.CV_32FC1, kernel);
        Imgproc.filter2D(gXY, gXY, CvType.CV_32FC1, kernel);
        Core.multiply(gXY, Scalar.all(2), gXY);

        // analytic solution of principal direction
        Mat denom = new Mat(rows, cols, CvType.CV_32FC1);
        Mat gXXMiusgYY = new Mat(rows, cols, CvType.CV_32FC1);
        Mat gXXMiusgYYSquared = new Mat(rows, cols, CvType.CV_32FC1);
        Mat gXYSquared = new Mat(rows, cols, CvType.CV_32FC1);
        Core.subtract(gXX, gYY, gXXMiusgYY);
        Core.multiply(gXXMiusgYY, gXXMiusgYY, gXXMiusgYYSquared);
        Core.multiply(gXY, gXY, gXYSquared);
        Core.add(gXXMiusgYYSquared, gXYSquared, denom);
        Core.sqrt(denom, denom);

        // sine and cosine of doubled angles
        Mat sin2Theta = new Mat(rows, cols, CvType.CV_32FC1);
        Mat cos2Theta = new Mat(rows, cols, CvType.CV_32FC1);
        Core.divide(gXY, denom, sin2Theta);
        Core.divide(gXXMiusgYY, denom, cos2Theta);

        // smooth orientations (sine and cosine)
        // smoothed sine and cosine of doubled angles
        kSize = Math.round(6 * orientSmoothSigma);
        if (kSize % 2 == 0) {
            kSize++;
        }
        kernel = Imgproc.getGaussianKernel(kSize, orientSmoothSigma, CvType.CV_32FC1);
        Imgproc.filter2D(sin2Theta, sin2Theta, CvType.CV_32FC1, kernel);
        Imgproc.filter2D(cos2Theta, cos2Theta, CvType.CV_32FC1, kernel);

        // calculate the result as the following
        //orientim = pi/2 + atan2(sin2theta,cos2theta)/2;
        Atan2(sin2Theta, cos2Theta, result);
        Core.divide(result, Scalar.all(2), result);
        Core.add(result, Scalar.all(Math.PI/(double)2.0), result);

        Log.i(TAG, "Hooooooooooray");

    }

    /**
     * Apply padding to the image.
     *
     * @param source
     * @param blockSize
     * @return
     */
    private Mat imagePadding(Mat source, int blockSize) {

        int width = source.width();
        int height = source.height();

        int bottomPadding = 0;
        int rightPadding = 0;

        if (width % blockSize != 0) {
            bottomPadding = blockSize - (width % blockSize);
        }
        if (height % blockSize != 0) {
            rightPadding = blockSize - (height % blockSize);
        }
        Imgproc.copyMakeBorder(source, source, 0, bottomPadding, 0, rightPadding, Imgproc.BORDER_CONSTANT, Scalar.all(0));
        return source;
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

    /**
     * Calculate bitwise atan2 for the given 2 images.
     * @param src1
     * @param src2
     * @param dstn
     */
    private void Atan2(Mat src1, Mat src2, Mat dstn){

        int height = src1.height();
        int width = src2.width();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                dstn.put(y, x, Core.fastAtan2((float) src1.get(y, x)[0], (float)src2.get(y, x)[0]));
            }
        }
    }

    /**
     * Normalize the image to have zero mean and unit standard deviation.
     */
    private void normalizeImage(Mat src, Mat dst){

        MatOfDouble mean = new MatOfDouble(0);
        MatOfDouble std = new MatOfDouble(0);

        // get mean and standard deviation
        Core.meanStdDev(src, mean, std);
        double m = mean.toArray()[0];
        double s = std.toArray()[0];
        Core.subtract(src, Scalar.all(m), dst);
        Core.divide(dst, Scalar.all(s), dst);
    }

    /**
     * Print/Log the given mat.
     *
     * @param mat
     */
    private void logMat(Mat mat) {

        int width = mat.width();
        int height = mat.height();

        ArrayList<Double> list = new ArrayList<Double>();

        Log.i(TAG, "Start Printing Mat");

        for (int y = 0; y < height; y++) {
            list.clear();
            for (int x = 0; x < width; x++) {
                list.add(mat.get(y, x)[0]);
            }
            Log.i(TAG, list.toString());
        }

        Log.i(TAG, "Finish Printing Mat");
    }

    /**
     * Calculate mean of given array.
     *
     * @param m
     * @return
     */
    private double calculateMean(double[] m) {
        double sum = 0;
        for (int i = 0; i < m.length; i++) {
            sum += m[i];
        }
        return sum / m.length;
    }

    /**
     * Calculate mean of given array.
     *
     * @param m
     * @return
     */
    private double calculateMean(ArrayList<Double> data) {
        double sum = 0;
        for (int i = 0; i < data.size(); i++) {
            sum += data.get(i);
        }
        return sum / data.size();
    }

    /**
     * Calculate variance of a list.
     *
     * @param data
     * @param mean
     * @return
     */
    double getVariance(ArrayList<Double> data, double mean) {
        double temp = 0;
        for (double a : data) {
            temp += (mean - a) * (mean - a);
        }
        return Math.sqrt(temp / data.size());
    }

    // endregion Private Methods
}
