package ti.spectrumanalyzer;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.AsyncResult;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.common.TiMessenger;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiLifecycle.OnLifecycleEvent;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.view.TiUIView;

import org.appcelerator.titanium.TiBaseActivity;
//import org.appcelerator.titanium.TiLifecycle.OnLifecycleEvent;

import android.app.Activity;
import android.os.Message;

@Kroll.proxy(creatableInModule = SpectrumanalyzerModule.class)
public class ViewProxy extends TiViewProxy implements OnLifecycleEvent {
	private static final String LCAT = SpectrumanalyzerModule.LCAT;
	private static final int MSG_FIRST_ID = TiViewProxy.MSG_LAST_ID + 1;
	private static final int MSG_START = MSG_FIRST_ID + 500;
	private static final int MSG_STOP = MSG_FIRST_ID + 501;
	private static final int MSG_CLEAR = MSG_FIRST_ID + 502;
	UISpectrumView spectrumView;
	private boolean shouldstart = false;

	public ViewProxy() {
		super();
	}

	@Override
	public TiUIView createView(Activity activity) {
		((TiBaseActivity) activity).addOnLifecycleEventListener(this);
		spectrumView = new UISpectrumView(this);
		if (shouldstart) {
			spectrumView.start();
			shouldstart = false;
		}
		return spectrumView;
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
			handleStart();
		} else {
			TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(
					MSG_START));

		}
	}

	private void handleStart() {
		if (spectrumView != null) {
			spectrumView.start();
		}
	}

	private void handleStop() {
		if (spectrumView != null) {
			spectrumView.stop();
		}
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

	@Override
	public void handleCreationDict(KrollDict options) {
		super.handleCreationDict(options);
	}

	public void onDestroy(Activity activity) {
		handleStop();
		super.onDestroy(activity);
	}

	@Override
	public void onResume(Activity activity) {
		super.onResume(activity);
		// handleStart();
	}

	@Override
	public void onPause(Activity activity) {
		Log.d(LCAT, "onPause");
		handleStop();
		super.onPause(activity);
	}

	@Override
	public void onStart(Activity activity) {
		super.onStart(activity);
	}

	@Override
	public void onStop(Activity activity) {
	}

	@Override
	public String getApiName() {
		return "Ti.Module.SpectrumAnalyzerView";
	}

}