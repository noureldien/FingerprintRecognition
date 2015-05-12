package com.fingerprintrecognition;

import android.app.*;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.*;
import org.opencv.core.*;
import org.opencv.features2d.*;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import java.lang.reflect.Array;
import java.security.Key;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Match the query image with the closet one in the database.
 */
public class MatchActivity extends Activity {

    // region Public Static Members

    public static Mat MatQuery;
    public static Mat MatMatchMask;
    public static double MatchingThreshold;

    // endregion Public Static Members

    // region Private Static Variables

    private static final String TAG = "FingerprintRecognition::MatchActivity";

    // endregion Private Variables

    // region Private Variables

    private int showIndex;
    private ImageView imageViewResult;
    private ArrayList<Bitmap> matchResults;
    private TextView textViewCount;

    //endregion Private Variables

    // region Constructor

    public MatchActivity() {

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
                // return back to Process activity
                this.finish();
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    // endregion Public Methods

    // region Private Event Handlers

    /**
     * Navigate to process activity.
     *
     * @param view
     */
    private void buttonRematch_OnClick(View view) {

        showIndex = 0;
        matchResults.clear();

        processMatching();

    }

    /**
     * Show images resulted from matching.
     *
     * @param view
     */
    private void buttonShow_OnClick(View view) {
        if (showIndex >= this.matchResults.size()) {
            showIndex = 0;
        }
        imageViewResult.setImageBitmap(matchResults.get(showIndex));
        textViewCount.setText(Integer.toString(showIndex));
        showIndex++;
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

    // endregion Private Event Handlers

    // region Private Methods

    /**
     * Initialize the activity.
     */
    private void initialize() {

        setContentView(R.layout.match);

        // set Home button of the ActionBar as back
        ActionBar actionBar = this.getActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        MatchingThreshold = 45;
        showIndex = 0;
        matchResults = new ArrayList<Bitmap>();

        // get views
        textViewCount = (TextView) findViewById(R.id.matchTextViewCounter);
        imageViewResult = (ImageView) findViewById(R.id.matchImageViewResult);

        // event handlers
        Button buttonRematch = (Button) findViewById(R.id.matchButtonRematch);
        buttonRematch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonRematch_OnClick(view);
            }
        });
        Button buttonShow = (Button) findViewById(R.id.matchButtonShow);
        buttonShow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonShow_OnClick(view);
            }
        });
        Button buttonSettings = (Button) findViewById(R.id.matchButtonSettings);
        buttonSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonSettings_OnClick(view);
            }
        });

        // start matching
         processMatching();
    }

    /**
     * Process matching for all the images.
     */
    private void processMatching() {

        // loop on all the target images, do matching and get score
        // also get the index of the max score
        int count = CameraActivity.processedImageCount();
        Object[] names = CameraActivity.getProcessedImageNames();
        final HashMap<String, Integer> scores = new HashMap<String, Integer>(count);
        int[] scoreValues = new int[count];
        int maxIndex = 0;
        for (int i = 0; i < count; i++) {
            scoreValues[i] = matching(MatQuery, CameraActivity.getProcessedImage(i));
            if (scoreValues[i] > scoreValues[maxIndex]) {
                maxIndex = i;
            }
            scores.put(names[i].toString(), scoreValues[i]);
        }

        // now, show message box with decision, if max score > min accepted score, then
        // declare a winner, else: say than no match found
        String title = scoreValues[maxIndex] > 15
                ? String.format("This fingerprint belongs to: %s.", names[maxIndex].toString())
                : "All scores below the minimum, we can't identify this fingerprint.";
        new AlertDialog.Builder(this)
                .setTitle("Result")
                .setMessage(title)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // navigate to Result activity
                        ResultActivity.Scores = scores;
                        Intent intent = new Intent(MatchActivity.this, com.fingerprintrecognition.ResultActivity.class);
                        MatchActivity.this.startActivity(intent);
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();

    }

    /**
     * Match the given images and return the score. Match using SURF, RANSAC.
     *
     * @param image1
     * @param image2
     * @return
     */
    private int matching(Mat image1, Mat image2) {

        List<DMatch> matchesList;
        List<DMatch> goodMatchesList = new LinkedList<DMatch>();
        MatOfPoint2f goodPoints1 = new MatOfPoint2f();
        MatOfPoint2f goodPoints2 = new MatOfPoint2f();
        List<Point> goodPointsList1 = new LinkedList<Point>();
        List<Point> goodPointsList2 = new LinkedList<Point>();

        Mat descriptor1 = new Mat();
        Mat descriptor2 = new Mat();

        FeatureDetector detector = FeatureDetector.create(FeatureDetector.SIFT);
        DescriptorExtractor extractor = DescriptorExtractor.create(DescriptorExtractor.SIFT);
        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.FLANNBASED);

        MatOfKeyPoint keyPoints1 = new MatOfKeyPoint();
        MatOfKeyPoint keyPoints2 = new MatOfKeyPoint();
        MatOfDMatch matches = new MatOfDMatch();
        MatOfDMatch goodMatches = new MatOfDMatch();

        // detect features
        detector.detect(image1, keyPoints1);
        detector.detect(image2, keyPoints2);

        // extract features
        extractor.compute(image1, keyPoints1, descriptor1);
        extractor.compute(image2, keyPoints2, descriptor2);

        // match features
        matcher.match(descriptor1, descriptor2, matches);
        matchesList = matches.toList();

        // find good matches
        double minDist = MatchingThreshold;
        double min = 1000000;
        double max = 0;
        double distance;
        for (int i = 0; i < descriptor1.rows(); i++) {
            distance = matchesList.get(i).distance;
            if (distance > max) max = distance;
            if (distance < min) min = distance;
            if (distance < minDist) {
                goodMatchesList.add(matchesList.get(i));
            }
        }
        goodMatches.fromList(goodMatchesList);
        Log.i(TAG, String.format("MinMax: %f %f", min, max));
        Log.i(TAG, String.format("All, good: %d %d", matchesList.size(), goodMatchesList.size()));

        // keyPoints of good matches
        List<KeyPoint> keyPointsList1 = keyPoints1.toList();
        List<KeyPoint> keyPointsList2 = keyPoints2.toList();
        DMatch m;
        for (int i = 0; i < goodMatchesList.size(); i++) {

            m = goodMatchesList.get(i);
            goodPointsList1.add(keyPointsList1.get(m.queryIdx).pt);
            goodPointsList2.add(keyPointsList2.get(m.trainIdx).pt);
        }
        goodPoints1.fromList(goodPointsList1);
        goodPoints2.fromList(goodPointsList2);

        // get homography
        // Mat homography = Calib3d.findHomography( goodPoints1, goodPoints2, Calib3d.RANSAC, 1.0);

        // draw result

        Mat result = new Mat();
        Scalar green = new Scalar(0, 255, 0);
        Scalar yellow = new Scalar(255, 255, 0);
        Scalar blue = new Scalar(0, 0, 255);
        Scalar red = new Scalar(255, 0, 0);
        MatOfByte mask = new MatOfByte();
        int flag = Features2d.NOT_DRAW_SINGLE_POINTS;
        Features2d.drawMatches(image1, keyPoints1, image2, keyPoints2, goodMatches, result, red, blue, mask, flag);

        // save the result and return the score
        matchResults.add(matToBitmap(result));
        int score = goodMatchesList.size();
        return score;
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

