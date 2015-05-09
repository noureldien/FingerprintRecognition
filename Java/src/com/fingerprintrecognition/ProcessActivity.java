package com.fingerprintrecognition;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

/**
 * Process the image to extract skeleton from it.
 */
public class ProcessActivity extends Activity {

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

    private void initialize(){

    }

    // endregion Private Methods
}
