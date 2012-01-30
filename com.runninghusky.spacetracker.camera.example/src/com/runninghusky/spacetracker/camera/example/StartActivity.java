package com.runninghusky.spacetracker.camera.example;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

/**
 * The Class StartActivity.
 */
public class StartActivity extends Activity implements SurfaceHolder.Callback,
		Runnable {

	/** The camera. */
	private Camera mCamera;

	/** The surface view. */
	private SurfaceView mSurfaceView;

	/** The surface holder. */
	private SurfaceHolder mSurfaceHolder;

	/** The preview running. */
	private Boolean mPreviewRunning = false;

	/** The Constant FOTO_MODE. */
	private static final int FOTO_MODE = 0;

	/** The first run. */
	private Boolean firstRun = true;

	/** The should take pics. */
	private Boolean shouldTakePics = true;

	/** The context. */
	private Context ctx = this;

	/** The TAG. */
	private String TAG = "camera_example";

	/** The Constant DELAY in milliseconds. */
	private static final int DELAY = 999999999;

	/** The defined time out. */
	int defTimeOut = 0;

	/** The shared preferences. */
	private SharedPreferences prefs;

	/**
	 * The not taking pic. Used to ensure pictures aren't taken when a picture
	 * is already being processed
	 */
	private Boolean notTakingPic = true;

	/**
	 * Called when the activity is first created.
	 * 
	 * @param savedInstanceState
	 *            the saved instance state
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Set the orientation to landscape and lock it
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		// Set pixel format to translucent
		getWindow().setFormat(PixelFormat.TRANSLUCENT);
		// Set no title for the window
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.main);
		// Move the current system timeout to defTimeOut
		defTimeOut = Settings.System.getInt(getContentResolver(),
				Settings.System.SCREEN_OFF_TIMEOUT, DELAY);
		// Change the current system timeout to our DELAY variable so
		// the screen does not time out
		Settings.System.putInt(getContentResolver(),
				Settings.System.SCREEN_OFF_TIMEOUT, DELAY);

		// Setupt the surface view
		mSurfaceView = (SurfaceView) findViewById(R.id.SurfaceViewPicture);
		mSurfaceHolder = mSurfaceView.getHolder();
		mSurfaceHolder.addCallback(this);
		mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		// The onclick listener for the surfaceview
		mSurfaceView.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// We only want the pictures to start after it is clicked
				// once. No need to do anything after that
				if (firstRun) {
					startSnapping();
					Toast.makeText(ctx, "Snapping has started...",
							Toast.LENGTH_SHORT).show();
					firstRun = false;
				}
			}
		});

		Toast.makeText(ctx, "Tap the screen to start snapping...",
				Toast.LENGTH_SHORT).show();

		prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

	}

	/**
	 * Start snapping.
	 */
	private void startSnapping() {
		// Spin off a new thread for the picture taking to start
		Thread thread = new Thread(this);
		thread.start();
	}

	/**
	 * Take picture.
	 */
	private void takePicture() {
		notTakingPic = false;
		try {
			// Try to auto-focus
			mCamera.autoFocus(onFocus);
		} catch (RuntimeException e) {
			e.printStackTrace();
			// If auto-focus didn't work, call the take picture method using the
			// jpegCallback variable
			mCamera.takePicture(null, null, jpegCallback);
		}
	}

	/** The on focus. */
	Camera.AutoFocusCallback onFocus = new Camera.AutoFocusCallback() {
		public void onAutoFocus(boolean success, Camera camera) {
			// Call the take picture method using the jpegCallback variable
			camera.takePicture(null, null, jpegCallback);
		}
	};

	/*
	 * The method called when the new thread was spun off
	 */
	@Override
	public void run() {
		// Create sleep interval
		int sleepInterval = 5000;
		// Get the interval from the preferences. Use 5 seconds as the default
		// if not found
		sleepInterval = (Integer.valueOf(prefs.getString("interval", "5")) * 1000);

		// Start the loop that should keep on looping until should take pics is
		// set to false
		while (shouldTakePics) {
			try {
				// This stops the thread for the sleep interval
				Thread.sleep(sleepInterval);
				if (shouldTakePics) {
					if (notTakingPic) {
						try {
							// Take the picture
							takePicture();
						} catch (Exception e) {
							Toast.makeText(ctx, "Error occurred...",
									Toast.LENGTH_SHORT).show();
						}
					}
				} else {
					return;
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/** The handler. This is used when the thread is told to stop */
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			// Stop the loop and finish the activity
			shouldTakePics = false;
			finish();
		}
	};

	/*
	 * Surface changed method called when the surface view is changed
	 * 
	 * @param holder The surface holder
	 * 
	 * @param format Format for the surface
	 * 
	 * @param w The width
	 * 
	 * @param h The height
	 */
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		// Check if the preview is running, if it is stop the preview
		if (mPreviewRunning) {
			mCamera.stopPreview();
		}

		String sizePref = "";
		sizePref = prefs.getString("resolution", "Not Present");

		Camera.Parameters p = mCamera.getParameters();
		// If no camera size is found in the shared preferences get the largest
		// supported size and set the picture size to that size
		if (sizePref.equals("Not Present")) {
			List<Size> sizes = p.getSupportedPictureSizes();
			Size s = sizes.get(0);
			for (Size size : sizes) {
				if (size.width > s.width) {
					s = size;
				}
			}
			p.setPictureSize(s.width, s.height);
		} else {
			// Set to the users specified camera size
			String[] userResolution = sizePref.split(":");
			p.setPictureSize(Integer.valueOf(userResolution[0]), Integer
					.valueOf(userResolution[1]));
		}

		// Use the first supported preview size
		List<Size> previewSizes = p.getSupportedPreviewSizes();
		p.setPreviewSize(previewSizes.get(0).width, previewSizes.get(0).height);

		// Set the foucs mode to infinity
		List<String> focusModes = p.getSupportedFocusModes();
		for (String str : focusModes) {
			if (str.equalsIgnoreCase("infinity")) {
				p.setFocusMode(str);
			}
		}

		try {
			// Set the camera parameters
			mCamera.setParameters(p);
		} catch (Exception e) {
			Toast.makeText(ctx, "Error occurred while setting parameters...",
					Toast.LENGTH_SHORT).show();
		}
		try {
			// Set the preview to the holder
			mCamera.setPreviewDisplay(holder);
		} catch (IOException e) {
			e.printStackTrace();
		}
		// Start the preview
		mCamera.startPreview();
		mPreviewRunning = true;
	}

	/*
	 * On surface created
	 */
	@Override
	public void surfaceCreated(SurfaceHolder arg0) {
		mCamera = Camera.open();
	}

	/*
	 * On surface destroyed
	 */
	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
		Settings.System.putInt(getContentResolver(),
				Settings.System.SCREEN_OFF_TIMEOUT, defTimeOut);

		mCamera.stopPreview();
		mPreviewRunning = false;
		mCamera.release();
	}

	/** Handles data for jpeg picture. */
	Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {
			FileOutputStream outStream = null;
			Intent mIntent = new Intent();
			if (data != null) {
				try {
					// Gets the root of the external storage directory
					File root = Environment.getExternalStorageDirectory();
					// Check to see if the app can write to external storage
					if (root.canWrite()) {
						// Appends the directory we want to use
						File f = new File(root + "/DCIM/camera_example/");
						// Creates the directory
						f.mkdirs();

						// Create an output stream to actually write the data
						// from the image to the directory specified
						outStream = new FileOutputStream(root
								+ "/DCIM/camera_example/"
								+ System.currentTimeMillis() + ".jpg");
						outStream.write(data);
						outStream.close();
						Log.d(TAG, "onPictureTaken - wrote bytes: "
								+ data.length);
						Toast.makeText(ctx, "Picture saved...",
								Toast.LENGTH_SHORT).show();
					}
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					mCamera.startPreview();
					setResult(FOTO_MODE, mIntent);
					notTakingPic = true;
				}
			}
		}
	};

	/*
	 * Prevents the activity from restarted on orientation change
	 */
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	/*
	 * Called when the view is attached to a window.
	 */
	@Override
	public void onAttachedToWindow() {
		// disables the home button so logging will continue
		this.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD);
		super.onAttachedToWindow();
	}

	/*
	 * Called when a key is pressed
	 */
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// Check to see if it was the back button, if it is call the quit logger
		// method
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			quitLogger();
		}
		return super.onKeyDown(keyCode, event);
	}

	/**
	 * Quit logger.
	 */
	private void quitLogger() {
		// The onclick listener for the dialog
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				switch (which) {
				case DialogInterface.BUTTON_POSITIVE:
					// On positive click, send the handler an empty message
					handler.sendEmptyMessage(0);
					break;
				case DialogInterface.BUTTON_NEGATIVE:
					// Do nothing on negative click
					break;
				}
			}
		};

		// Build the dialog box to ask use if they want to quit
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Are you sure you want to stop snapping?")
				.setPositiveButton("Of Course...", dialogClickListener)
				.setNegativeButton("No Way!", dialogClickListener).show();

	}

	/*
	 * The final call you receive before your activity is destroyed.
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		try {
			// Evo 3d camera not releasing on surface destroy...
			mCamera.release();
		} catch (Exception e) {

		}
	}

	/* Creates the menu items */
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	/*
	 * Initialize the contents of the Activity's standard options menu.
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.preferences:
			// Get the supported widths and heights to send to the preferences
			// activity
			List<String> widths = new ArrayList<String>();
			List<String> heights = new ArrayList<String>();
			Camera.Parameters p = mCamera.getParameters();
			for (Size s : p.getSupportedPictureSizes()) {
				widths.add(String.valueOf(s.width));
				heights.add(String.valueOf(s.height));
			}

			// Setup new intent to get us to the preferences activity. After the
			// activity is fired off, finish our current activity
			Intent myIntent = new Intent(StartActivity.this, Preferences.class);
			myIntent.putExtra("widths", widths
					.toArray(new String[widths.size()]));
			myIntent.putExtra("heights", heights.toArray(new String[heights
					.size()]));
			StartActivity.this.startActivity(myIntent);
			finish();
			break;
		}
		return true;
	}
}