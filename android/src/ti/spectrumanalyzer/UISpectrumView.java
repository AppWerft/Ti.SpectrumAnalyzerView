package ti.spectrumanalyzer;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.view.TiUIView;
import ca.uol.aig.fftpack.RealDoubleFFT;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.view.View;
import android.graphics.*;

public class UISpectrumView extends TiUIView {
	private static final String LCAT = SpectrumanalyzerModule.LCAT;
	private RealDoubleFFT transformer;
	private int blockSize = 256;
	private int frequency = 44100;
	private int color = Color.GREEN;
	private int width;
	private int height;
	private Paint tiPaint;
	private Bitmap tiBitmap;
	private Canvas tiCanvas;
	private SpectrumView tiSpectrumView; // extended from view
	final private int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;
	final private int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
	AudioRecord audioRecord;
	private boolean started = false;
	private boolean CANCELLED_FLAG = false;
	MicrophoneLevelGrabber recordTask;

	public UISpectrumView(final TiViewProxy proxy) {
		super(proxy);

		importOptions(proxy.getProperties());
		transformer = new RealDoubleFFT(blockSize);
		setPaintOptions(); // set initial paint options
		tiSpectrumView = new SpectrumView(proxy.getActivity());
		setNativeView(tiSpectrumView);
	}

	private void setPaintOptions() {
		tiPaint = new Paint();
		tiPaint.setAntiAlias(true);
		tiPaint.setDither(true);
		tiPaint.setColor(color);
		tiPaint.setStyle(Paint.Style.STROKE);
		tiPaint.setStrokeJoin(Paint.Join.ROUND);
		tiPaint.setStrokeCap(Paint.Cap.ROUND);
	}

	private void importOptions(KrollDict props) {
		if (props.containsKeyAndNotNull(TiC.PROPERTY_COLOR)) {
			color = TiConvert.toColor(props, TiC.PROPERTY_COLOR);
		}
		if (props.containsKeyAndNotNull(SpectrumanalyzerModule.PROP_BLOCKSIZE)) {
			blockSize = props.getInt(SpectrumanalyzerModule.PROP_BLOCKSIZE);
		}
	}

	@Override
	public void processProperties(KrollDict d) {
		super.processProperties(d);
	}

	public void start() {
		started = true;
		CANCELLED_FLAG = false;
		new MicrophoneLevelGrabber().execute();
		Log.d(LCAT, "TiSpectrumView started");
	}

	public void stop() {
		CANCELLED_FLAG = true;
		tiCanvas.drawColor(Color.BLACK);
		started = false;
	}

	private class MicrophoneLevelGrabber extends
			AsyncTask<Void, double[], Boolean> {

		@Override
		protected Boolean doInBackground(Void... params) {
			int bufferSize = AudioRecord.getMinBufferSize(frequency,
					channelConfiguration, audioEncoding);
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
						toTransform[i] = (double) buffer[i] / Short.MAX_VALUE;
					}
					transformer.ft(toTransform);
					publishProgress(toTransform);
				}
			}
			if (audioRecord != null
					&& audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
				// if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED)
				// {
				// audioRecord.stop();
				// }
				audioRecord.release();
			}
			return true;
		}

		@SuppressWarnings("unused")
		@Override
		protected void onProgressUpdate(double[]... progress) {
			double[] vals = progress[0];
			if (vals.length == 0 || tiCanvas == null || tiSpectrumView == null)
				return;
			double sum = 0;
			for (int i = 0; i < vals.length; i++) {
				sum += vals[i];
				int x = width * i / vals.length;
				int downy = (int) (height / 2 - (vals[i] * height / 2));
				int upy = height / 2;
				tiCanvas.drawLine(x, downy, x, upy, tiPaint);
				if (i == vals.length / 2 && false)
					Log.d(LCAT, "SUM = " + sum + "    Y = " + downy + " LEN = "
							+ vals.length);
			}
			tiSpectrumView.invalidate();
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			try {
				audioRecord.stop();
			} catch (IllegalStateException e) {
				Log.e("Stop failed", e.toString());

			}
			tiCanvas.drawColor(Color.BLACK);
			tiSpectrumView.invalidate();
		}
	}

	public class SpectrumView extends View {
		public SpectrumView(Context ctx) {
			super(ctx);
		}

		@Override
		protected void onSizeChanged(int w, int h, int oldw, int oldh) {
			super.onSizeChanged(w, h, oldw, oldh);
			width = w;
			height = h;
			Log.d(LCAT, "onSizeChanged " + w + "x" + h);
			if (tiBitmap == null) {
				tiBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
			} else {
				tiBitmap = Bitmap.createScaledBitmap(tiBitmap, w, h, true);
			}
			tiCanvas = new Canvas(tiBitmap);
		}
	}

}
