// A simple demo of JNI interface to implement SIFT detection for Android application using nonfree module in OpenCV4Android.
// Created by Guohui Wang 
// Email: robertwgh_at_gmail_com
// Data: 2/26/2014

package com.example.nonfreejnidemo;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import org.opencv.core.*;
import org.opencv.features2d.*;
import org.opencv.highgui.Highgui;

import java.util.LinkedList;
import java.util.List;

public class MainActivity extends Activity {

	private  static String TAG = "MainActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		txtView = (TextView)findViewById(R.id.textView1);

		runBtn = (Button)findViewById(R.id.button_run_demo);
		runBtn.setOnClickListener(new Button.OnClickListener()
		{
			@Override
			public void onClick(View v) 
			{
				Log.v("nonfree_jni_demo", "start runDemo");
				// Call the JNI interface
				NonfreeJNILib.runDemo();
		
				Toast.makeText(MainActivity.this, "Finished!", Toast.LENGTH_SHORT).show();
				txtView.setText("Finished! Please check /sdcard/nonfree for result image.");

				Mat im1 = new Mat(300, 300, CvType.CV_32FC1);
				Mat im2 = new Mat(300, 300, CvType.CV_32FC1);

				try {
					//im1 = Utils.loadResource(this, R.drawable.sift_3, Highgui.CV_LOAD_IMAGE_GRAYSCALE);
					//im2 = Utils.loadResource(this, R.drawable.sift_4, Highgui.CV_LOAD_IMAGE_GRAYSCALE);
					Log.i(TAG, "Succeeded Reading Images");
				} catch (Exception exp) {
					Log.i(TAG, "Error Reading the images");
					Log.i(TAG, exp.toString());
				}

				// start matching
				if (im1 != null && im2 != null) {
					Log.i(TAG, "Start Matching");
					matching(im1, im2);
				}
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	Button runBtn;
	TextView txtView;

	// endregion Private Methods

	/**
	 * Match the query image with the closet one in the database, using SIFT, RANSAC.
	 *
	 * @param image1
	 * @param image2
	 */
	private void matching(Mat image1, Mat image2) {

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
		DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_SL2);

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
		double maxDist = 0;
		double minDist = 520;
		double min = 2000000;
		double max = 0;
		double distance;
		for (int i = 0; i < descriptor1.rows(); i++) {
			distance = matchesList.get(i).distance;
			if (distance> max) max = distance;
			if (distance < min) min = distance;
			if (distance < minDist * 1000) {
				goodMatchesList.add(matchesList.get(i));
			}
		}
		goodMatches.fromList(goodMatchesList);
		Log.i(TAG, String.format("Min, Max: %f %f", min, max));

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
		Scalar blue = new Scalar(0, 0, 255);
		MatOfByte mask = new MatOfByte();
		int flag = Features2d.NOT_DRAW_SINGLE_POINTS;
		Features2d.drawMatches(image1, keyPoints1, image2, keyPoints2, goodMatches, result, green, blue, mask, flag);

		//imageView.setVisibility(View.VISIBLE);
		//cameraView.setVisibility(View.INVISIBLE);
		//imageView.setImageBitmap(matToBitmap(result));
	}

	/**
	 * Convert Mat image to bitmap image.
	 *
	 * @param image
	 */
	private void matToBitmap(Mat image) {

		//Bitmap bitmap = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.ARGB_8888);
		//Utils.matToBitmap(image, bitmap);
		//return bitmap;
	}
}
