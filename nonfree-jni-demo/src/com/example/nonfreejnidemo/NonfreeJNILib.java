// A simple demo of JNI interface to implement SIFT detection for Android application using nonfree module in OpenCV4Android.
// Created by Guohui Wang 
// Email: robertwgh_at_gmail_com
// Data: 2/26/2014

package com.example.nonfreejnidemo;

public class NonfreeJNILib {
	
    static 
    {
    	try
    	{ 
    		// Load necessary libraries.
    		System.loadLibrary("opencv_java");
    		System.loadLibrary("nonfree");
    		System.loadLibrary("nonfree_jni");
    	}
    	catch( UnsatisfiedLinkError e )
		{
           System.err.println("Native code library failed to load.\n" + e);		
		}
    }

    public static native void runDemo();
}