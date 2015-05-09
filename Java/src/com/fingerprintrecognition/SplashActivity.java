package com.fingerprintrecognition;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

/**
 * Acts as splash screen.
 */
public class SplashActivity extends Activity {

    // region Public Methods

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        navigateToCameraActivity();
    }

    // endregion Public Methods

    // region Private Methods

    /**
     * Navigate to the Camera activity.
     */
    private void navigateToCameraActivity() {
        /* New Handler to start the Menu-Activity
         * and close this Splash-Screen after some seconds.*/
        new Handler().postDelayed(new Runnable(){
            @Override
            public void run() {
                // navigate to Camera page
                Intent intent = new Intent(SplashActivity.this, TestActivity.class);
                SplashActivity.this.startActivity(intent);
                SplashActivity.this.finish();
            }
        }, 1000);

    }

    // endregion Private Methods
}
