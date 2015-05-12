package com.fingerprintrecognition;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.HashMap;

/**
 * Match the query image with the closet one in the database.
 */
public class ResultActivity extends Activity {

    // region Public Static Members

    public static HashMap<String, Integer> Scores;

    // endregion Public Static Members

    // region Private Static Variables

    private static final String TAG = "FingerprintRecognition::ResultActivity";

    // endregion Private Variables

    // region Constructor

    public ResultActivity() {

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
                // return back to Match activity
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

        setContentView(R.layout.result);

        // set Home button of the ActionBar as back
        ActionBar actionBar = this.getActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        // get views
        ListView listView = (ListView) findViewById(R.id.resultListView);

        String[] listValues = new String[Scores.size() + 1];
        Object[] names = Scores.keySet().toArray();
        listValues[0] = String.format("%-38s%s", "Name", "Score");
        for (int i =0; i< Scores.size(); i++){
            listValues[i+1] = String.format("%-40s%d", names[i].toString(), Scores.get(names[i]));
        }

        // show the results
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listValues);
        listView.setAdapter(adapter);
    }

    // endregion Private Methods
}

