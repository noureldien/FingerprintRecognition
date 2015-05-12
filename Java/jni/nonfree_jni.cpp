// A simple demo of JNI interface to implement SIFT detection for Android application using nonfree module in OpenCV4Android.
// Created by Guohui Wang 
// Email: robertwgh_at_gmail_com
// Data: 2/26/2014

#include <jni.h>
#include <android/log.h>

#include <opencv2/core/core.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/nonfree/features2d.hpp>
#include <opencv2/nonfree/nonfree.hpp>
#include <iostream>

using namespace cv;
using namespace std;

#define  LOG_TAG    "nonfree_jni_demo"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)

typedef unsigned char uchar;

int run_demo();

extern "C" {
    JNIEXPORT void JNICALL Java_com_example_nonfreejnidemo_NonfreeJNILib_runDemo(JNIEnv * env, jobject obj);
};

JNIEXPORT void JNICALL Java_com_example_nonfreejnidemo_NonfreeJNILib_runDemo(JNIEnv * env, jobject obj)
{
	LOGI( "Start run_demo! \n");
	run_demo();
	LOGI( "End run_demo!\n");
}


int run_demo()
{
	//cv::initModule_nonfree();
	//cout <<"initModule_nonfree() called" << endl;

	// Input and output image path.
	const char * imgInFile = "/sdcard/nonfree/img1.jpg";
	const char * imgOutFile = "/sdcard/nonfree/img1_result.jpg";

	Mat image;
	image = imread(imgInFile, CV_LOAD_IMAGE_COLOR);
	if(! image.data )
	{
		LOGI("Could not open or find the image!\n");
		return -1;
	}

	vector<KeyPoint> keypoints;
	Mat descriptors;

	// Create a SIFT keypoint detector.
	SiftFeatureDetector detector;
	detector.detect(image, keypoints);
	LOGI("Detected %d keypoints\n", (int) keypoints.size());

	// Compute feature description.
	detector.compute(image,keypoints, descriptors);
	LOGI("Compute feature.\n");

	// Store description to "descriptors.des".
	FileStorage fs;
	fs.open("descriptors.des", FileStorage::WRITE);
	LOGI("Opened file to store the features.\n");
	fs << "descriptors" << descriptors;
	LOGI("Finished writing file.\n");
	fs.release();
	LOGI("Released file.\n");

	// Show keypoints in the output image.
	Mat outputImg;
	Scalar keypointColor = Scalar(255, 0, 0);
	drawKeypoints(image, keypoints, outputImg, keypointColor, DrawMatchesFlags::DRAW_RICH_KEYPOINTS);
	LOGI("Drew keypoints in output image file.\n");

#ifdef WIN32
	namedWindow("Output image", CV_WINDOW_AUTOSIZE );
	imshow("Output image", outputImg);
	waitKey(0);
#endif
	
	LOGI("Generate the output image.\n");
	imwrite(imgOutFile, outputImg);

	LOGI("Done.\n");
	return 0;
}
