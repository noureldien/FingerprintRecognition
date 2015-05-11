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
import org.opencv.ml.CvStatModel;

import java.security.SecureClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Process the image to extract skeleton from it.
 */
public class ProcessActivity extends Activity {

    // region Public Static Members

    public static Mat MatSnapShot;

    public static Mat MatSnapShotMask;

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
     * Normalize-back the result to the default range of the image and show it.
     *
     * @param image
     */
    private void showImage(Mat image) {

        int rows = image.rows();
        int cols = image.cols();

        Mat result = new Mat(rows, cols, CvType.CV_8UC1);
        Core.normalize(image, result, 0, 255, Core.NORM_MINMAX, CvType.CV_8UC1);
        processImageViewSource.setImageBitmap(matToBitmap(result));
    }

    /**
     * Process the image to get the skeleton.
     */
    private void processImage() {

        int rows = MatSnapShot.rows();
        int cols = MatSnapShot.cols();

        // apply histogram equalization
        //Mat equalized = new Mat(rows, cols, CvType.CV_32FC1);
        //Imgproc.equalizeHist(MatSnapShot, equalized);

        // convert to float, very important
        Mat floated = new Mat(rows, cols, CvType.CV_32FC1);
        MatSnapShot.convertTo(floated, CvType.CV_32FC1);

        // normalise image to have zero mean and 1 standard deviation
        Mat normalized = new Mat(rows, cols, CvType.CV_32FC1);
        normalizeImage(floated, normalized);

        // step 1: get ridge segment by padding then do block process
        int blockSize = 16;
        double threshold = 0.05;
        Mat padded = imagePadding(floated, blockSize);
        int imgRows = padded.rows();
        int imgCols = padded.cols();
        Mat matRidgeSegment = new Mat(imgRows, imgCols, CvType.CV_32FC1);
        Mat segmentMask = new Mat(imgRows, imgCols, CvType.CV_8UC1);
        ridgeSegment(padded, matRidgeSegment, segmentMask, blockSize, threshold);

        // step 2: get ridge orientation
        int gradientSigma = 1;
        int blockSigma = 5;
        int orientSmoothSigma = 5;
        Mat matRidgeOrientation = new Mat(imgRows, imgCols, CvType.CV_32FC1);
        ridgeOrientation(matRidgeSegment, matRidgeOrientation, gradientSigma, blockSigma, orientSmoothSigma);

        // step 3: get ridge frequency
        int fBlockSize = 36;
        int fWindowSize = 5;
        int fMinWaveLength = 5;
        int fMaxWaveLength = 25;
        Mat matFrequency = new Mat(imgRows, imgCols, CvType.CV_32FC1);
        double medianFreq = ridgeFrequency(matRidgeSegment, segmentMask, matRidgeOrientation, matFrequency, fBlockSize, fWindowSize, fMinWaveLength, fMaxWaveLength);

        // step 4: get ridge filter
        Mat matRidgeFilter = new Mat(imgRows, imgCols, CvType.CV_32FC1);
        double filterKx = 0.5;
        double filterKy = 0.5;
        ridgeFilter(matRidgeSegment, matRidgeOrientation, matFrequency, matRidgeFilter, filterKx, filterKy, medianFreq);

        // step 5: enhance image after ridge filter
        Mat matEnhanced = new Mat(imgRows, imgCols, CvType.CV_8UC1);
        enhancement(matRidgeFilter, matEnhanced, blockSize);
        showImage(matEnhanced);
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
        double stdVal;

        for (int y = 1; y <= heightSteps; y++) {
            for (int x = 1; x <= widthSteps; x++) {

                roi = new Rect((blockSize) * (x - 1), (blockSize) * (y - 1), blockSize, blockSize);
                windowMask.setTo(scalarBlack);
                Core.rectangle(windowMask, new Point(roi.x, roi.y), new Point(roi.x + roi.width, roi.y + roi.height), scalarWhile, -1, 8, 0);

                window = source.submat(roi);
                Core.meanStdDev(window, mean, std);
                stdVal = std.toArray()[0];
                result.setTo(Scalar.all(stdVal), windowMask);

                // mask used to calc mean and standard deviation later
                mask.setTo(Scalar.all(stdVal >= threshold ? 1 : 0), windowMask);
            }
        }

        // get mean and standard deviation
        Core.meanStdDev(source, mean, std, mask);
        Core.subtract(source, Scalar.all(mean.toArray()[0]), result);
        Core.meanStdDev(result, mean, std, mask);
        Core.divide(result, Scalar.all(std.toArray()[0]), result);
    }

    /**
     * Calculate ridge orientation.
     *
     * @param ridgeSegment
     * @param result
     * @param gradientSigma
     * @param blockSigma
     * @param orientSmoothSigma
     */
    private void ridgeOrientation(Mat ridgeSegment, Mat result, int gradientSigma, int blockSigma, int orientSmoothSigma) {

        int rows = ridgeSegment.rows();
        int cols = ridgeSegment.cols();

        // calculate image gradients
        int kSize = Math.round(6 * gradientSigma);
        if (kSize % 2 == 0) {
            kSize++;
        }
        Mat kernel = gaussianKernel(kSize, gradientSigma);

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
        Imgproc.filter2D(ridgeSegment, gX, CvType.CV_32FC1, fX);
        Imgproc.filter2D(ridgeSegment, gY, CvType.CV_32FC1, fY);

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
        kernel = gaussianKernel(kSize, blockSigma);
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
        kernel = gaussianKernel(kSize, orientSmoothSigma);
        Imgproc.filter2D(sin2Theta, sin2Theta, CvType.CV_32FC1, kernel);
        Imgproc.filter2D(cos2Theta, cos2Theta, CvType.CV_32FC1, kernel);

        // calculate the result as the following, so the values of the matrix range [0, PI]
        //orientim = atan2(sin2theta,cos2theta)/360;
        atan2(sin2Theta, cos2Theta, result);
        Core.multiply(result, Scalar.all(Math.PI / 360.0), result);
    }

    /**
     * Calculate ridge frequency.
     *
     * @param ridgeSegment
     * @param segmentMask
     * @param ridgeOrientation
     * @param frequencies
     * @param blockSize
     * @param windowSize
     * @param minWaveLength
     * @param maxWaveLength
     * @return
     */
    private double ridgeFrequency(Mat ridgeSegment, Mat segmentMask, Mat ridgeOrientation, Mat frequencies, int blockSize, int windowSize, int minWaveLength, int maxWaveLength) {

        int rows = ridgeSegment.rows();
        int cols = ridgeSegment.cols();

        Mat blockSegment;
        Mat blockOrientation;
        Mat frequency;

        for (int y = 0; y < rows - blockSize; y += blockSize) {
            for (int x = 0; x < cols - blockSize; x += blockSize) {
                blockSegment = ridgeSegment.submat(y, y + blockSize, x, x + blockSize);
                blockOrientation = ridgeOrientation.submat(y, y + blockSize, x, x + blockSize);
                frequency = calculateFrequency(blockSegment, blockOrientation, windowSize, minWaveLength, maxWaveLength);
                frequency.copyTo(frequencies.rowRange(y, y + blockSize).colRange(x, x + blockSize));
            }
        }

        // mask out frequencies calculated for non ridge regions
        Core.multiply(frequencies, segmentMask, frequencies, 1.0, CvType.CV_32FC1);

        // find median frequency over all the valid regions of the image.
        double medianFrequency = medianFrequency(frequencies);

        // the median frequency value used across the whole fingerprint gives a more satisfactory result
        Core.multiply(segmentMask, Scalar.all(medianFrequency), frequencies, 1.0, CvType.CV_32FC1);

        return medianFrequency;
    }

    /**
     * Estimate fingerprint ridge frequency within image block.
     *
     * @param block
     * @param blockOrientation
     * @param windowSize
     * @param minWaveLength
     * @param maxWaveLength
     * @return
     */
    private Mat calculateFrequency(Mat block, Mat blockOrientation, int windowSize, int minWaveLength, int maxWaveLength) {

        int rows = block.rows();
        int cols = block.cols();

        Mat orientation = blockOrientation.clone();
        Core.multiply(orientation, Scalar.all(2.0), orientation);

        int orientLength = (int) (orientation.total());
        float[] orientations = new float[orientLength];
        orientation.get(0, 0, orientations);

        double[] sinOrient = new double[orientLength];
        double[] cosOrient = new double[orientLength];
        for (int i = 1; i < orientLength; i++) {
            sinOrient[i] = Math.sin((double) orientations[i]);
            cosOrient[i] = Math.cos((double) orientations[i]);
        }
        float orient = Core.fastAtan2((float) calculateMean(sinOrient), (float) calculateMean(cosOrient)) / (float) 2.0;

        // rotate the image block so that the ridges are vertical
        Mat rotated = new Mat(rows, cols, CvType.CV_32FC1);
        Point center = new Point(cols / 2, rows / 2);
        double rotateAngle = ((orient / Math.PI) * (180.0)) + 90.0;
        double rotateScale = 1.0;
        Size rotatedSize = new Size(cols, rows);
        Mat rotateMatrix = Imgproc.getRotationMatrix2D(center, rotateAngle, rotateScale);
        Imgproc.warpAffine(block, rotated, rotateMatrix, rotatedSize, Imgproc.INTER_NEAREST);

        // crop the image so that the rotated image does not contain any invalid regions
        // this prevents the projection down the columns from being mucked up
        int cropSize = (int) Math.round(rows / Math.sqrt(2));
        int offset = (int) Math.round((rows - cropSize) / 2.0) - 1;
        Mat cropped = rotated.submat(offset, offset + cropSize, offset, offset + cropSize);

        // get sums of columns
        float sum = 0;
        Mat proj = new Mat(1, cropped.cols(), CvType.CV_32FC1);
        for (int c = 1; c < cropped.cols(); c++) {
            sum = 0;
            for (int r = 1; r < cropped.cols(); r++) {
                sum += cropped.get(r, c)[0];
            }
            proj.put(0, c, sum);
        }

        // find peaks in projected grey values by performing a grayScale
        // dilation and then finding where the dilation equals the original values.
        Mat dilateKernel = new Mat(windowSize, windowSize, CvType.CV_32FC1, Scalar.all(1.0));
        Mat dilate = new Mat(1, cropped.cols(), CvType.CV_32FC1);
        Imgproc.dilate(proj, dilate, dilateKernel, new Point(-1, -1), 1);
        //Imgproc.dilate(proj, dilate, dilateKernel, new Point(-1, -1), 1, Imgproc.BORDER_CONSTANT, Scalar.all(0.0));

        double projMean = Core.mean(proj).val[0];
        double projValue;
        double dilateValue;
        final double ROUND_POINTS = 1000;
        ArrayList<Integer> maxind = new ArrayList<Integer>();
        for (int i = 0; i < cropped.cols(); i++) {

            projValue = proj.get(0, i)[0];
            dilateValue = dilate.get(0, i)[0];

            // round to maximize the likelihood of equality
            projValue = (double) Math.round(projValue * ROUND_POINTS) / ROUND_POINTS;
            dilateValue = (double) Math.round(dilateValue * ROUND_POINTS) / ROUND_POINTS;

            if (dilateValue == projValue && projValue > projMean) {
                maxind.add(i);
            }
        }

        // determine the spatial frequency of the ridges by dividing the distance between
        // the 1st and last peaks by the (No of peaks-1). If no peaks are detected
        // or the wavelength is outside the allowed bounds, the frequency image is set to 0
        Mat result = new Mat(rows, cols, CvType.CV_32FC1, Scalar.all(0.0));
        int peaks = maxind.size();
        if (peaks >= 2) {
            double waveLength = (maxind.get(peaks - 1) - maxind.get(0)) / (peaks - 1);
            if (waveLength >= minWaveLength && waveLength <= maxWaveLength) {
                result = new Mat(rows, cols, CvType.CV_32FC1, Scalar.all((1.0 / waveLength)));
            }
        }

        return result;
    }

    /**
     * Enhance fingerprint image using oriented filters.
     *
     * @param ridgeSegment
     * @param orientation
     * @param frequency
     * @param result
     * @param kx
     * @param ky
     * @param medianFreq
     * @return
     */
    private void ridgeFilter(Mat ridgeSegment, Mat orientation, Mat frequency, Mat result, double kx, double ky, double medianFreq) {

        int angleInc = 3;
        int rows = ridgeSegment.rows();
        int cols = ridgeSegment.cols();

        int filterCount = 180 / 3;
        Mat[] filters = new Mat[filterCount];

        double sigmaX = kx / medianFreq;
        double sigmaY = ky / medianFreq;

        //mat refFilter = exp(-(x. ^ 2 / sigmaX ^ 2 + y. ^ 2 / sigmaY ^ 2) / 2). * cos(2 * pi * medianFreq * x);
        int size = (int) Math.round(3 * Math.max(sigmaX, sigmaY));
        size = (size % 2 == 0) ? size : size + 1;
        int length = (size * 2) + 1;
        Mat x = meshGrid(size);
        Mat y = x.t();

        Mat xSquared = new Mat(length, length, CvType.CV_32FC1);
        Mat ySquared = new Mat(length, length, CvType.CV_32FC1);
        Core.multiply(x, x, xSquared);
        Core.multiply(y, y, ySquared);
        Core.divide(xSquared, Scalar.all(sigmaX * sigmaX), xSquared);
        Core.divide(ySquared, Scalar.all(sigmaY * sigmaY), ySquared);

        Mat refFilterPart1 = new Mat(length, length, CvType.CV_32FC1);
        Core.add(xSquared, ySquared, refFilterPart1);
        Core.divide(refFilterPart1, Scalar.all(-2.0), refFilterPart1);
        Core.exp(refFilterPart1, refFilterPart1);

        Mat refFilterPart2 = new Mat(length, length, CvType.CV_32FC1);
        Core.multiply(x, Scalar.all(2 * Math.PI * medianFreq), refFilterPart2);
        refFilterPart2 = matCos(refFilterPart2);

        Mat refFilter = new Mat(length, length, CvType.CV_32FC1);
        Core.multiply(refFilterPart1, refFilterPart2, refFilter);

        // Generate rotated versions of the filter.  Note orientation
        // image provides orientation *along* the ridges, hence +90
        // degrees, and the function requires angles +ve anticlockwise, hence the minus sign.
        Mat rotated;
        Mat rotateMatrix;
        double rotateAngle;
        Point center = new Point(length / 2, length / 2);
        Size rotatedSize = new Size(length, length);
        double rotateScale = 1.0;
        for (int i = 0; i < filterCount; i++) {
            rotateAngle = -(i * angleInc);
            rotated = new Mat(length, length, CvType.CV_32FC1);
            rotateMatrix = Imgproc.getRotationMatrix2D(center, rotateAngle, rotateScale);
            Imgproc.warpAffine(refFilter, rotated, rotateMatrix, rotatedSize, Imgproc.INTER_LINEAR);
            filters[i] = rotated;
        }

        // convert orientation matrix values from radians to an index value
        // that corresponds to round(degrees/angleInc)
        Mat orientIndexes = new Mat(orientation.rows(), orientation.cols(), CvType.CV_8UC1);
        Core.multiply(orientation, Scalar.all((double) filterCount / Math.PI), orientIndexes, 1.0, CvType.CV_8UC1);

        Mat orientMask;
        Mat orientThreshold;

        orientMask = new Mat(orientation.rows(), orientation.cols(), CvType.CV_8UC1, Scalar.all(0));
        orientThreshold = new Mat(orientation.rows(), orientation.cols(), CvType.CV_8UC1, Scalar.all(0.0));
        Core.compare(orientIndexes, orientThreshold, orientMask, Core.CMP_LT);
        Core.add(orientIndexes, Scalar.all(filterCount), orientIndexes, orientMask);

        orientMask = new Mat(orientation.rows(), orientation.cols(), CvType.CV_8UC1, Scalar.all(0));
        orientThreshold = new Mat(orientation.rows(), orientation.cols(), CvType.CV_8UC1, Scalar.all(filterCount));
        Core.compare(orientIndexes, orientThreshold, orientMask, Core.CMP_GE);
        Core.subtract(orientIndexes, Scalar.all(filterCount), orientIndexes, orientMask);

        // finally, find where there is valid frequency data then do the filtering
        Mat value = new Mat(length, length, CvType.CV_32FC1);
        Mat subSegment;
        int orientIndex;
        double sum;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (frequency.get(r, c)[0] > 0
                        && r > (size + 1)
                        && r < (rows - size - 1)
                        && c > (size + 1)
                        && c < (cols - size - 1)) {
                    orientIndex = (int) orientIndexes.get(r, c)[0];
                    // Log.i(TAG, String.format("Row %d, Column %d, rows %d, cols %d"", r, c, rows, cols));
                    subSegment = ridgeSegment.submat(r - size - 1, r + size, c - size - 1, c + size);
                    Core.multiply(subSegment, filters[orientIndex], value);
                    sum = Core.sumElems(value).val[0];
                    result.put(r, c, sum);
                }
            }
        }
    }

    /**
     * Enhance the image after ridge filter.
     *
     * @param source
     * @param result
     * @param blockSize
     */
    private void enhancement(Mat source, Mat result, int blockSize) {

        // apply mask, binary threshold, thinning, ..., etc

        Mat paddedMask = imagePadding(MatSnapShotMask, blockSize);

        // apply the original mask to get rid of extras
        Core.multiply(source, paddedMask, result, 1.0, CvType.CV_8UC1);

        // apply binary threshold
        Core.MinMaxLocResult minMaxResult = Core.minMaxLoc(result);
        Imgproc.threshold(result, result, 0, minMaxResult.maxVal, Imgproc.THRESH_BINARY);

        // normalize the values to the full scale [0, 255]
        Core.normalize(result, result, 0, 255, Core.NORM_MINMAX, CvType.CV_8UC1);

        // apply thinning
        result = thin(result);
    }

    /**
     * Thinning the given matrix.
     * @param source
     * @return
     */
    private Mat thin(Mat source){

        int rows = source.rows();
        int cols = source.cols();

        Mat sourceFloated = new Mat(rows, cols, CvType.CV_32FC1);
        source.convertTo(sourceFloated, CvType.CV_32FC1);

        /// start to thin
        Mat p_thinMat1 = new Mat(rows, cols, CvType.CV_32FC1, Scalar.all(0.0));
        Mat p_thinMat2 = new Mat(rows, cols, CvType.CV_32FC1, Scalar.all(0.0));
        Mat p_cmp = new Mat(rows, cols, CvType.CV_8UC1, Scalar.all(0.0));

        boolean bDone = false;
        while (!bDone) {
            /// sub-iteration 1
            thinSubIteration1(sourceFloated, p_thinMat1);
            /// sub-iteration 2
            thinSubIteration2(p_thinMat1, p_thinMat2);
            /// compare
            Core.compare(sourceFloated, p_thinMat2, p_cmp, Core.CMP_EQ);
            /// check
            int num_non_zero = Core.countNonZero(p_cmp);
            if(num_non_zero == (rows + 2) * (cols + 2)) {
                bDone = true;
            }
            /// copy
            p_thinMat2.copyTo(sourceFloated);
        }

        // copy result
        Mat result = new Mat(rows, cols, CvType.CV_8UC1);
        sourceFloated.convertTo(result, CvType.CV_8UC1);
        return  result;
    }

    /**
     * Iteration 1 for thinning.
     * @param pSrc
     * @param pDst
     */
    private void thinSubIteration1(Mat pSrc, Mat pDst) {
        int rows = pSrc.rows();
        int cols = pSrc.cols();
        pSrc.copyTo(pDst);
        for (int i = 1; i < rows - 1; i++) {
            for (int j = 1; j < cols - 1; j++) {
                if (pSrc.get(i, j)[0] == 1.0f) {
                    /// get 8 neighbors
                    /// calculate C(p)
                    int neighbor0 = (int) pSrc.get(i - 1, j - 1)[0];
                    int neighbor1 = (int) pSrc.get(i - 1, j)[0];
                    int neighbor2 = (int) pSrc.get(i - 1, j + 1)[0];
                    int neighbor3 = (int) pSrc.get(i, j + 1)[0];
                    int neighbor4 = (int) pSrc.get(i + 1, j + 1)[0];
                    int neighbor5 = (int) pSrc.get(i + 1, j)[0];
                    int neighbor6 = (int) pSrc.get(i + 1, j - 1)[0];
                    int neighbor7 = (int) pSrc.get(i, j - 1)[0];
                    int C = (~neighbor1 & (neighbor2 | neighbor3)) + (~neighbor3 & (neighbor4 | neighbor5)) + (~neighbor5 & (neighbor6 | neighbor7)) + (~neighbor7 & (neighbor0 | neighbor1));
                    if (C == 1) {
                        /// calculate N
                        int N1 = (neighbor0 | neighbor1) + (neighbor2 | neighbor3) + (neighbor4 | neighbor5) + (neighbor6 | neighbor7);
                        int N2 = (neighbor1 | neighbor2) + (neighbor3 | neighbor4) + (neighbor5 | neighbor6) + (neighbor7 | neighbor0);
                        int N = Math.min(N1, N2);
                        if ((N == 2) || (N == 3)) {
                            /// calculate criteria 3
                            int c3 = (neighbor1 | neighbor2 | ~neighbor4) & neighbor3;
                            if (c3 == 0) {
                                pDst.put(i, j, 0.0f);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Iteration 2 for thinning.
     * @param pSrc
     * @param pDst
     */
    private void thinSubIteration2(Mat pSrc, Mat pDst) {
        int rows = pSrc.rows();
        int cols = pSrc.cols();
        pSrc.copyTo(pDst);
        for (int i = 1; i < rows - 1; i++) {
            for (int j = 1; j < cols - 1; j++) {
                if (pSrc.get(i, j)[0] == 1.0f) {
                    /// get 8 neighbors
                    /// calculate C(p)
                    int neighbor0 = (int) pSrc.get(i - 1, j - 1)[0];
                    int neighbor1 = (int) pSrc.get(i - 1, j)[0];
                    int neighbor2 = (int) pSrc.get(i - 1, j + 1)[0];
                    int neighbor3 = (int) pSrc.get(i, j + 1)[0];
                    int neighbor4 = (int) pSrc.get(i + 1, j + 1)[0];
                    int neighbor5 = (int) pSrc.get(i + 1, j)[0];
                    int neighbor6 = (int) pSrc.get(i + 1, j - 1)[0];
                    int neighbor7 = (int) pSrc.get(i, j - 1)[0];
                    int C = (~neighbor1 & (neighbor2 | neighbor3)) + (~neighbor3 & (neighbor4 | neighbor5)) + (~neighbor5 & (neighbor6 | neighbor7)) + (~neighbor7 & (neighbor0 | neighbor1));
                    if (C == 1) {
                        /// calculate N
                        int N1 = (neighbor0 | neighbor1) + (neighbor2 | neighbor3) + (neighbor4 | neighbor5) + (neighbor6 | neighbor7);
                        int N2 = (neighbor1 | neighbor2) + (neighbor3 | neighbor4) + (neighbor5 | neighbor6) + (neighbor7 | neighbor0);
                        int N = Math.min(N1, N2);
                        if ((N == 2) || (N == 3)) {
                            int E = (neighbor5 | neighbor6 | ~neighbor0) & neighbor7;
                            if (E == 0) {
                                pDst.put(i, j, 0.0f);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Function for thinning the given binary image
     *
     * @param im
     */
    private void thinningGuoHall(Mat im) {

        int rows = im.rows();
        int cols = im.cols();

        Core.divide(im, Scalar.all(255.0), im);
        Mat prev = new Mat(rows, cols, CvType.CV_8UC1, Scalar.all(0.0));
        Mat diff = new Mat(rows, cols, CvType.CV_8UC1);

        do {
            thinningGuoHallIteration(im, 0);
            thinningGuoHallIteration(im, 1);
            Core.absdiff(im, prev, diff);
            im.copyTo(prev);
        }
        while (Core.countNonZero(diff) > 0);

        Core.multiply(im, Scalar.all(255.0), im);
    }

    /**
     * Perform one thinning iteration.
     * Normally you wouldn't call this function directly from your code.
     *
     * @param im         Binary image with range = 0-1
     * @param iterations 0=even, 1=odd
     */
    private void thinningGuoHallIteration(Mat im, int iterations) {

        int rows = im.rows();
        int cols = im.cols();

        Mat marker = new Mat(rows, cols, CvType.CV_8UC1, Scalar.all(0.0));

        for (int i = 1; i < rows - 1; i++) {
            for (int j = 1; j < cols - 1; j++) {
                byte p2 = (byte) im.get(i - 1, j)[0];
                byte p3 = (byte) im.get(i - 1, j + 1)[0];
                byte p4 = (byte) im.get(i, j + 1)[0];
                byte p5 = (byte) im.get(i + 1, j + 1)[0];
                byte p6 = (byte) im.get(i + 1, j)[0];
                byte p7 = (byte) im.get(i + 1, j - 1)[0];
                byte p8 = (byte) im.get(i, j - 1)[0];
                byte p9 = (byte) im.get(i - 1, j - 1)[0];

                int C = (~p2 & (p3 | p4)) + (~p4 & (p5 | p6)) + (~p6 & (p7 | p8)) + (~p8 & (p9 | p2));
                int N1 = (p9 | p2) + (p3 | p4) + (p5 | p6) + (p7 | p8);
                int N2 = (p2 | p3) + (p4 | p5) + (p6 | p7) + (p8 | p9);
                int N = N1 < N2 ? N1 : N2;
                int m = iterations == 0 ? ((p6 | p7 | ~p9) & p8) : ((p2 | p3 | ~p5) & p4);

                if (C == 1 && (N >= 2 && N <= 3) & m == 0) {
                    marker.put(i, j, 1);
                }
            }
        }

        Core.bitwise_not(marker, marker);
        Core.add(im, marker, im);
    }

    /**
     * Function for thinning the given binary image
     *
     * @param im Binary image with range = 0-255
     */
    void thinning(Mat im) {

        int rows = im.rows();
        int cols = im.cols();

        Core.divide(im, Scalar.all(255.0), im);

        Mat prev = new Mat(rows, cols, CvType.CV_8UC1, Scalar.all(0.0));
        Mat diff = new Mat(rows, cols, CvType.CV_8UC1);

        do {
            thinningIteration(im, 0);
            thinningIteration(im, 1);
            Core.absdiff(im, prev, diff);
            im.copyTo(prev);
        }
        while (Core.countNonZero(diff) > 0);

        Core.multiply(im, Scalar.all(255.0), im);
    }

    /**
     * Perform one thinning iteration.
     * Normally you wouldn't call this function directly from your code.
     *
     * @param im         Binary image with range = 0-1
     * @param iterations 0=even, 1=odd
     */
    private void thinningIteration(Mat im, int iterations) {
        int rows = im.rows();
        int cols = im.cols();

        Mat marker = new Mat(rows, cols, CvType.CV_8UC1, Scalar.all(0.0));

        for (int i = 1; i < rows - 1; i++) {
            for (int j = 1; j < cols - 1; j++) {
                byte p2 = (byte) im.get(i - 1, j)[0];
                byte p3 = (byte) im.get(i - 1, j + 1)[0];
                byte p4 = (byte) im.get(i, j + 1)[0];
                byte p5 = (byte) im.get(i + 1, j + 1)[0];
                byte p6 = (byte) im.get(i + 1, j)[0];
                byte p7 = (byte) im.get(i + 1, j - 1)[0];
                byte p8 = (byte) im.get(i, j - 1)[0];
                byte p9 = (byte) im.get(i - 1, j - 1)[0];

                boolean a = (p2 == 0 && p3 == 1) || (p3 == 0 && p4 == 1) || (p4 == 0 && p5 == 1) || (p5 == 0 && p6 == 1) || (p6 == 0 && p7 == 1) || (p7 == 0 && p8 == 1) || (p8 == 0 && p9 == 1) || (p9 == 0 && p2 == 1);
                int A = a ? 1 : 0;
                int B = p2 + p3 + p4 + p5 + p6 + p7 + p8 + p9;
                int m1 = iterations == 0 ? (p2 * p4 * p6) : (p2 * p4 * p8);
                int m2 = iterations == 0 ? (p4 * p6 * p8) : (p2 * p6 * p8);

                if (A == 1 && (B >= 2 && B <= 6) && m1 == 0 && m2 == 0) {
                    marker.put(i, j, 1);
                }
            }
        }

        Core.bitwise_not(marker, marker);
        Core.add(im, marker, im);
    }

    /**
     * Create mesh grid.
     *
     * @param size
     * @return
     */
    private Mat meshGrid(int size) {

        int l = (size * 2) + 1;
        int value = -size;

        Mat result = new Mat(l, l, CvType.CV_32FC1);
        for (int c = 0; c < l; c++) {
            for (int r = 0; r < l; r++) {
                result.put(r, c, value);
            }
            value++;
        }
        return result;
    }

    /**
     * Round the values of the given mat to
     *
     * @param source
     * @param points
     * @return
     */
    private Mat roundMat(Mat source, double points) {

        int cols = source.cols();
        int rows = source.rows();

        Mat doubleMat = new Mat(rows, cols, CvType.CV_32FC1);
        Mat intMat = new Mat(rows, cols, CvType.CV_8UC1);

        Core.multiply(source, Scalar.all(points), doubleMat);
        doubleMat.convertTo(intMat, CvType.CV_8UC1);
        intMat.convertTo(doubleMat, CvType.CV_32FC1);
        Core.divide(doubleMat, Scalar.all(points), doubleMat);

        return doubleMat;
    }

    /**
     * Get unique items in the given mat using the given mask.
     *
     * @param source
     * @param mask
     * @return
     */
    private float[] uniqueValues(Mat source, Mat mask) {

        Mat result = new Mat(source.cols(), source.rows(), CvType.CV_32FC1);
        Core.multiply(source, mask, result, 1.0, CvType.CV_32FC1);

        logMat(source);
        logMat(result);

        int length = (int) (result.total());
        float[] values = new float[length];
        result.get(0, 0, values);

        return values;
    }

    /**
     * Apply sin to each element of the matrix.
     *
     * @param source
     * @return
     */
    private Mat matSin(Mat source) {

        int cols = source.cols();
        int rows = source.rows();
        Mat result = new Mat(cols, rows, CvType.CV_32FC1);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                result.put(r, c, Math.sin(source.get(r, c)[0]));
            }
        }

        return result;
    }

    /**
     * Apply cos to each element of the matrix.
     *
     * @param source
     * @return
     */
    private Mat matCos(Mat source) {

        int rows = source.rows();
        int cols = source.cols();

        Mat result = new Mat(cols, rows, CvType.CV_32FC1);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                result.put(r, c, Math.cos(source.get(r, c)[0]));
            }
        }

        return result;
    }

    /**
     * Calculate the median of all values greater than zero.
     *
     * @param image
     * @return
     */
    private double medianFrequency(Mat image) {

        ArrayList<Double> values = new ArrayList<Double>();
        double value = 0;

        for (int r = 0; r < image.rows(); r++) {
            for (int c = 0; c < image.cols(); c++) {
                value = image.get(r, c)[0];
                if (value > 0) {
                    values.add(value);
                }
            }
        }

        Collections.sort(values);
        int size = values.size();
        double median = 0;

        if (size > 0) {
            int halfSize = size / 2;
            if ((size % 2) == 0) {
                median = (values.get(halfSize - 1) + values.get(halfSize)) / 2.0;
            } else {
                median = values.get(halfSize);
            }
        }
        return median;
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
     *
     * @param src1
     * @param src2
     * @param dstn
     */
    private void atan2(Mat src1, Mat src2, Mat dst) {

        int height = src1.height();
        int width = src2.width();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                dst.put(y, x, Core.fastAtan2((float) src1.get(y, x)[0], (float) src2.get(y, x)[0]));
            }
        }
    }

    /**
     * Normalize the image to have zero mean and unit standard deviation.
     */
    private void normalizeImage(Mat src, Mat dst) {

        MatOfDouble mean = new MatOfDouble(0.0);
        MatOfDouble std = new MatOfDouble(0.0);

        // get mean and standard deviation
        Core.meanStdDev(src, mean, std);
        Core.subtract(src, Scalar.all(mean.toArray()[0]), dst);
        Core.meanStdDev(dst, mean, std);
        Core.divide(dst, Scalar.all(std.toArray()[0]), dst);
    }

    /**
     * Create Gaussian kernel.
     *
     * @param sigma
     */
    private Mat gaussianKernel(int kSize, int sigma) {

        Mat kernelX = Imgproc.getGaussianKernel(kSize, sigma, CvType.CV_32FC1);
        Mat kernelY = Imgproc.getGaussianKernel(kSize, sigma, CvType.CV_32FC1);

        Mat kernel = new Mat(kSize, kSize, CvType.CV_32FC1);
        Core.gemm(kernelX, kernelY.t(), 1, Mat.zeros(kSize, kSize, CvType.CV_32FC1), 0, kernel, 0);
        return kernel;
    }

    /**
     * Create Gaussian kernel.
     *
     * @param sigma
     */
    private Mat gaussianKernel_(int kSize, int sigma) {

        Mat kernel = new Mat(kSize, kSize, CvType.CV_32FC1);

        double total = 0;
        int l = kSize / 2;
        double distance = 0;
        double value = 0;

        for (int y = -l; y <= l; y++) {
            for (int x = -l; x <= l; x++) {
                distance = ((x * x) + (y * y)) / (2 * (sigma * sigma));
                value = Math.exp(-distance);
                kernel.put(y + l, x + l, value);
                total += value;
            }
        }

        for (int y = 0; y < kSize; y++) {
            for (int x = 0; x < kSize; x++) {
                value = kernel.get(y, x)[0];
                value /= total;
                kernel.put(y, x, value);
            }
        }

        return kernel;
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
