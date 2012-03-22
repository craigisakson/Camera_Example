package com.runninghusky.spacetracker.camera.example;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.view.KeyEvent;

/**
 * The Class Preferences.
 */
public class Preferences extends PreferenceActivity {

	/**
	 * Called when the activity is first created
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Add the preferences from our predefined xml file
		addPreferencesFromResource(R.xml.preferences);

		// Get the widths and heights sent to this activity from the
		// calling activity for display of the camera sizes
		String[] widths = getIntent().getStringArrayExtra("widths");
		String[] heights = getIntent().getStringArrayExtra("heights");
		String[] previewWidths = getIntent().getStringArrayExtra("pwidths");
		String[] previewHeights = getIntent().getStringArrayExtra("pheights");

		ListPreference cameraSizesList = (ListPreference) findPreference("resolution");
		// Check to see if the resolution preference is in the preferences.xml
		// file
		if (cameraSizesList != null) {
			CharSequence entries[] = new String[widths.length];
			CharSequence entryValues[] = new String[widths.length];
			for (int i = 0; i < widths.length; i++) {
				// Create the lists and list values from our passed in values
				entries[i] = widths[i] + " x " + heights[i];
				entryValues[i] = widths[i] + ":" + heights[i];
			}
			// Set the resolution preference to use the list and values
			cameraSizesList.setEntries(entries);
			cameraSizesList.setEntryValues(entryValues);
		}

		ListPreference cameraPreviewSizesList = (ListPreference) findPreference("previewresolution");
		// Check to see if the preview resolution preference is in the
		// preferences.xml
		// file
		if (cameraPreviewSizesList != null) {
			CharSequence entries[] = new String[previewWidths.length];
			CharSequence entryValues[] = new String[previewWidths.length];
			for (int i = 0; i < widths.length; i++) {
				// Create the lists and list values from our passed in values
				entries[i] = previewWidths[i] + " x " + previewHeights[i];
				entryValues[i] = previewWidths[i] + ":" + previewHeights[i];
			}
			// Set the resolution preference to use the list and values
			cameraPreviewSizesList.setEntries(entries);
			cameraPreviewSizesList.setEntryValues(entryValues);
		}

	}

	/**
	 * Called when key's are pressed
	 */
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// Check to see if it was the back key
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			// On back, launch the start activity and finish this activity
			Intent myIntent = new Intent(Preferences.this, StartActivity.class);
			Preferences.this.startActivity(myIntent);
			finish();
		}
		return super.onKeyDown(keyCode, event);
	}

	/**
	 * Prevents the activity from restarted on orientation change
	 */
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

}
