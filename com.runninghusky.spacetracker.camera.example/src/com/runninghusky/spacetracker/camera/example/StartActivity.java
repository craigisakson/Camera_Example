package com.runninghusky.spacetracker.camera.example;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

public class StartActivity extends Activity implements SurfaceHolder.Callback,
		Runnable {
	private Camera mCamera;
	private SurfaceView mSurfaceView;
	private SurfaceHolder mSurfaceHolder;
	private Boolean mPreviewRunning = false;
	private static final int FOTO_MODE = 0;
	private Boolean firstRun = true;
	private Boolean shouldTakePics = true;
	private Context ctx = this;
	private String TAG = "camera_example";
	private static final int DELAY = 999999999;
	int defTimeOut = 0;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		getWindow().setFormat(PixelFormat.TRANSLUCENT);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.main);
		defTimeOut = Settings.System.getInt(getContentResolver(),
				Settings.System.SCREEN_OFF_TIMEOUT, DELAY);
		Settings.System.putInt(getContentResolver(),
				Settings.System.SCREEN_OFF_TIMEOUT, DELAY);

		mSurfaceView = (SurfaceView) findViewById(R.id.SurfaceViewPicture);
		mSurfaceHolder = mSurfaceView.getHolder();
		mSurfaceHolder.addCallback(this);
		mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		mSurfaceView.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
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

	}

	private void startSnapping() {
		Thread thread = new Thread(this);
		thread.start();
	}

	private void takePicture() {
		try {
			mCamera.autoFocus(onFocus);
		} catch (RuntimeException e) {
			e.printStackTrace();
			mCamera.takePicture(null, null, jpegCallback);
		}
	}

	Camera.AutoFocusCallback onFocus = new Camera.AutoFocusCallback() {
		public void onAutoFocus(boolean success, Camera camera) {
			camera.takePicture(null, null, jpegCallback);
		}
	};

	@Override
	public void run() {
		while (shouldTakePics) {
			try {
				// The thread will pause for 5 seconds (5*1000)
				Thread.sleep(5000);
				if (shouldTakePics) {
					try {
						takePicture();
					} catch (Exception e) {
						Toast.makeText(ctx, "Error occurred...",
								Toast.LENGTH_SHORT).show();
					}
				} else {
					return;
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			shouldTakePics = false;
			finish();
		}
	};

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		if (mPreviewRunning) {
			mCamera.stopPreview();
		}

		Camera.Parameters p = mCamera.getParameters();
		List<Size> sizes = p.getSupportedPictureSizes();
		Size s = sizes.get(0);
		for (Size size : sizes) {
			if (size.width > s.width) {
				s = size;
			}
		}
		p.setPictureSize(s.width, s.height);

		List<Size> previewSizes = p.getSupportedPreviewSizes();
		p.setPreviewSize(previewSizes.get(0).width, previewSizes.get(0).height);

		List<String> focusModes = p.getSupportedFocusModes();
		for (String str : focusModes) {
			if (str.equalsIgnoreCase("infinity")) {
				p.setFocusMode(str);
			}
		}

		try {
			mCamera.setParameters(p);
		} catch (Exception e) {
			Toast.makeText(ctx, "Error occurred while setting parameters...",
					Toast.LENGTH_SHORT).show();
		}
		try {
			mCamera.setPreviewDisplay(holder);
		} catch (IOException e) {
			e.printStackTrace();
		}
		mCamera.startPreview();
		mPreviewRunning = true;
	}

	@Override
	public void surfaceCreated(SurfaceHolder arg0) {
		mCamera = Camera.open();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
		Settings.System.putInt(getContentResolver(),
				Settings.System.SCREEN_OFF_TIMEOUT, defTimeOut);

		mCamera.stopPreview();
		mPreviewRunning = false;
		mCamera.release();
	}

	/** Handles data for jpeg picture */
	Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {
			FileOutputStream outStream = null;
			Intent mIntent = new Intent();
			if (data != null) {
				try {
					File root = Environment.getExternalStorageDirectory();
					File f = new File(root + "/DCIM/camera_example/");
					f.mkdirs();
					if (root.canWrite()) {
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
				}
			}
		}
	};

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public void onAttachedToWindow() {
		// disables the home button so logging will continue
		this.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD);
		super.onAttachedToWindow();
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			quitLogger();
		}
		return super.onKeyDown(keyCode, event);
	}

	private void quitLogger() {
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				switch (which) {
				case DialogInterface.BUTTON_POSITIVE:
					handler.sendEmptyMessage(0);
					break;
				case DialogInterface.BUTTON_NEGATIVE:
					break;
				}
			}
		};
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Are you sure you want to stop snapping?")
				.setPositiveButton("Of Course...", dialogClickListener)
				.setNegativeButton("No Way!", dialogClickListener).show();

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		try {
			// Evo 3d camera not releasing on surface destroy...
			mCamera.release();
		} catch (Exception e) {

		}
	}
}