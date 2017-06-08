package ti.spectrumanalyzer;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiDimension;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.view.TiCompositeLayout;
import org.appcelerator.titanium.view.TiUIView;
import org.appcelerator.titanium.view.TiCompositeLayout.LayoutArrangement;

import ca.uol.aig.fftpack.RealDoubleFFT;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class TiSpectrumView extends TiUIView {
	final static String LCAT = "TiSpec";
	TiViewProxy proxy;
	TiCompositeLayout view;
	private RealDoubleFFT transformer;
	int blockSize = 256;
	int frequency = 44100;
	int color = Color.GREEN;
	int width;
	int height;
	int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;
	int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
	AudioRecord audioRecord;
	boolean started = false;
	boolean CANCELLED_FLAG = false;
	Canvas canvasDS;
	Bitmap bitmapDS;
	RecordAudio recordTask;
	Context ctx;
	Paint paintSD = new Paint();
	ImageView imageViewDS;
	Activity activity;

	public TiSpectrumView(final TiViewProxy _proxy) {
		super(_proxy);
		// copy proxy instance
		proxy = _proxy;
		activity = proxy.getActivity();
		// getting context from TiApp
		ctx = TiApplication.getInstance().getApplicationContext();
		// creating empty container
		SpectrumLayout container = new SpectrumLayout(activity);
		container.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT));
		imageViewDS = new ImageView(activity);
		container.addView(imageViewDS);
		setNativeView(container);
		transformer = new RealDoubleFFT(blockSize);

		paintSD.setColor(color);
		// initRenderer();
	}

	@Override
	public void processProperties(KrollDict d) {
		super.processProperties(d);
	}

	public void initRenderer() {
		Log.d(LCAT, "w x h = " + width + "x" + height);
		Log.d(LCAT, "imageViewDS.getWidth = " + imageViewDS.getWidth());
		if (width > 0 && height > 0) {
			bitmapDS = Bitmap.createBitmap(width, height,
					Bitmap.Config.ARGB_8888);
			imageViewDS.setImageBitmap(bitmapDS);
			canvasDS = new Canvas(bitmapDS);
			Log.d(LCAT, "TiSpectrumView initialized");
		} else
			Log.w(LCAT, "width was 0");
	}

	public void start() {
		started = true;
		CANCELLED_FLAG = false;
		recordTask = new RecordAudio();
		recordTask.execute();
		Log.d(LCAT, "TiSpectrumView started");
	}

	public void stop() {
		CANCELLED_FLAG = true;
		try {
			audioRecord.stop();
		} catch (IllegalStateException e) {
			Log.e("Stop failed", e.toString());
		}
		canvasDS.drawColor(Color.BLACK);
	}

	private class RecordAudio extends AsyncTask<Void, double[], Boolean> {

		@Override
		protected Boolean doInBackground(Void... params) {
			int bufferSize = AudioRecord.getMinBufferSize(frequency,
					channelConfiguration, audioEncoding);
			Log.d(LCAT, "Buffersize=" + bufferSize);
			audioRecord = new AudioRecord(MediaRecorder.AudioSource.DEFAULT,
					frequency, channelConfiguration, audioEncoding, bufferSize);
			int bufferReadResult;
			short[] buffer = new short[blockSize];
			double[] toTransform = new double[blockSize];
			try {
				audioRecord.startRecording();
			} catch (IllegalStateException e) {
				Log.e(LCAT, "Recording failed" + e.toString());
			}
			while (started) {
				if (isCancelled() || (CANCELLED_FLAG == true)) {
					started = false;
					// publishProgress(cancelledResult);
					Log.d("doInBackground", "Cancelling the RecordTask");
					break;
				} else {
					bufferReadResult = audioRecord.read(buffer, 0, blockSize);
					for (int i = 0; i < blockSize && i < bufferReadResult; i++) {
						toTransform[i] = (double) buffer[i] / 32768.0;
					}
					transformer.ft(toTransform);
					publishProgress(toTransform);
				}
			}
			return true;
		}

		@Override
		protected void onProgressUpdate(double[]... progress) {
			double[] vals = progress[0];
			if (vals.length == 0 || canvasDS == null)
				return;
			for (int i = 0; i < vals.length; i++) {
				int x = width * i / vals.length;
				int downy = (int) (height / 2 - (vals[i] * 10));
				int upy = height / 2;

				canvasDS.drawLine(x, downy, x, upy, paintSD);
				if (i == 0)
					Log.d(LCAT, "Y = " + downy + " LEN = " + vals.length);

			}
			imageViewDS.invalidate();
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			try {
				audioRecord.stop();
			} catch (IllegalStateException e) {
				Log.e("Stop failed", e.toString());

			}
			canvasDS.drawColor(Color.BLACK);
			imageViewDS.invalidate();
		}
	}

	public class SpectrumLayout extends LinearLayout {
		public SpectrumLayout(Context context) {
			super(context);
		}

		@Override
		protected void onLayout(boolean changed, int l, int t, int r, int b) {
			if (changed) {
				width = this.getWidth();
				height = this.getHeight();
				initRenderer();
			}
		}
	}

}
