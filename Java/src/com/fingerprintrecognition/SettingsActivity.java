package com.fingerprintrecognition;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import java.util.HashMap;

/**
 * Set settings.
 */
public class SettingsActivity extends Activity {

    // region Private Static Variables

    private static final String TAG = "FingerprintRecognition::SettingsActivity";

    // endregion Private Variables

    // region Private Variables

    private EditText editTextMatching;
    private Button buttonSave;

    // endregion Variables

    // region Constructor

    public SettingsActivity() {

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
                // return back to previous activity
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
     * Save settings.
     *
     * @param view
     */
    private void buttonSave_OnClick(View view) {

        MatchActivity.MatchingThreshold = Double.parseDouble(editTextMatching.getText().toString());

        this.finish();
    }

    // endregion Private Event Handlers

    // region Private Methods

    /**
     * Initialize the activity.
     */
    private void initialize() {

        setContentView(R.layout.settings);

        // set Home button of the ActionBar as back
        ActionBar actionBar = this.getActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        // get views
        editTextMatching = (EditText) findViewById(R.id.settingsEditTextMatching);

        // set values
        editTextMatching.setText(Double.toString(MatchActivity.MatchingThreshold));

        // event handlers
        Button buttonSave = (Button) findViewById(R.id.settingsButtonSave);
        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonSave_OnClick(view);
            }
        });
    }

    // endregion Private Methods
}