/**
 * This file was auto-generated by the Titanium Module SDK helper for Android
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 *
 */
package ti.spectrumanalyzer;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.AsyncResult;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.common.TiMessenger;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.view.TiUIView;

import android.app.Activity;
import android.os.Message;

// This proxy can be created by calling Spectrumanalyzer.createExample({message: "hello world"})
@Kroll.proxy(creatableInModule = SpectrumanalyzerModule.class)
public class ViewProxy extends TiViewProxy {
	// Standard Debugging variables
	private static final String LCAT = "TiSpec";
	private static final int MSG_FIRST_ID = TiViewProxy.MSG_LAST_ID + 1;
	private static final int MSG_START = MSG_FIRST_ID + 500;
	private static final int MSG_STOP = MSG_FIRST_ID + 501;
	TiSpectrumView tiSpectrumView;
	private boolean shouldstart = false;

	// Constructor
	public ViewProxy() {
		super();
	}

	@Override
	public TiUIView createView(Activity activity) {
		Log.d(LCAT, "createView in ViewProxy");
		tiSpectrumView = new TiSpectrumView(this);
		tiSpectrumView.getLayoutParams().autoFillsHeight = true;
		tiSpectrumView.getLayoutParams().autoFillsWidth = true;
		if (shouldstart)
			tiSpectrumView.start();
		shouldstart = false;
		return tiSpectrumView;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public boolean handleMessage(Message msg) {
		AsyncResult result = null;
		switch (msg.what) {

		case MSG_START: {
			result = (AsyncResult) msg.obj;
			Log.d(LCAT, "handleMessage START");
			handleStart();
			result.setResult(null);
			return true;
		}
		case MSG_STOP: {
			result = (AsyncResult) msg.obj;
			handleStop();
			result.setResult(null);
			return true;
		}
		default: {
			return super.handleMessage(msg);
		}
		}
	}

	@Kroll.method
	public void start() {
		if (TiApplication.isUIThread()) {
			Log.d(LCAT, "direct handleStart()");
			handleStart();
		} else {
			TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(
					MSG_START));

		}
	}

	private void handleStart() {

		if (tiSpectrumView != null)
			tiSpectrumView.start();
		else
			shouldstart = true;
	}

	private void handleStop() {
		if (tiSpectrumView != null)
			tiSpectrumView.stop();
	}

	@Kroll.method
	public void stop() {
		if (TiApplication.isUIThread()) {
			handleStart();
		} else {
			TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(
					MSG_STOP));

		}
	}

	// Handle creation options
	@Override
	public void handleCreationDict(KrollDict options) {
		super.handleCreationDict(options);

		if (options.containsKey("message")) {
			Log.d(LCAT,
					"example created with message: " + options.get("message"));
		}
	}

	public void onDestroy(Activity activity) {
		stop();
		super.onDestroy(activity);
	}

	@Override
	public void onStop(Activity activity) {
		handleStop();
		super.onStop(activity);
	}

	@Override
	public void onResume(Activity activity) {
		super.onResume(activity);
		handleStart();
	}

	@Override
	public void onPause(Activity activity) {
		handleStop();
		super.onPause(activity);
	}

	public void onStart(Activity activity) {
		super.onStart(activity);
		handleStart();
	}
}