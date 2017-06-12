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
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;

public class UISpectrumView extends TiUIView {
	private static final String LCAT = SpectrumanalyzerModule.LCAT;
	private RealDoubleFFT transformer;
	private int blockSize = 256;
	private int frequency = 44100;
	private int color = Color.GREEN;
	private boolean fftEnabled = true;
	private boolean fadeEnabled = true;
	private double modulation = 1.0;
	private int compressType = SpectrumanalyzerModule.CURVE_LINEAR;

	private int width;
	private int height;
	private Paint mainPaint = new Paint();
	private Paint fadePaint = new Paint();
	private Bitmap tiBitmap;
	private Canvas canvas;
	private SpectrumView spectrumView; // extended from view
	final private int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;
	final private int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
	private AudioRecord audioRecord;
	private boolean started = false;
	private boolean autoStart = false;
	private boolean CANCELLED_FLAG = false;
	private MicrophoneLevelGrabber microLevelGrabber;

	public UISpectrumView(final TiViewProxy proxy) {
		super(proxy);
		spectrumView = new SpectrumView(proxy.getActivity());
		spectrumView.setBackgroundColor(Color.GREEN);
		spectrumView.invalidate();
		setNativeView(spectrumView);
		importOptions(proxy.getProperties());
		transformer = new RealDoubleFFT(blockSize);
		setPaintOptions(); // set initial paint options
	}

	private void importOptions(KrollDict props) {
		if (props.containsKeyAndNotNull(TiC.PROPERTY_COLOR)) {
			color = TiConvert.toColor(props, TiC.PROPERTY_COLOR);
		}
		if (props.containsKeyAndNotNull(SpectrumanalyzerModule.PROP_BLOCKSIZE)) {
			blockSize = props.getInt(SpectrumanalyzerModule.PROP_BLOCKSIZE);
		}
		if (props.containsKeyAndNotNull(TiC.PROPERTY_AUTOPLAY)) {
			autoStart = props.getBoolean(TiC.PROPERTY_AUTOPLAY);
		}
		if (props.containsKeyAndNotNull(SpectrumanalyzerModule.PROP_FFTENABLED)) {
			fftEnabled = props
					.getBoolean(SpectrumanalyzerModule.PROP_FFTENABLED);
		}
		if (props
				.containsKeyAndNotNull(SpectrumanalyzerModule.PROP_FADEENABLED)) {
			fadeEnabled = props
					.getBoolean(SpectrumanalyzerModule.PROP_FADEENABLED);
		}
		if (props.containsKeyAndNotNull(SpectrumanalyzerModule.PROP_MODULATION)) {
			modulation = props
					.getDouble(SpectrumanalyzerModule.PROP_MODULATION);
		}
		if (props
				.containsKeyAndNotNull(SpectrumanalyzerModule.PROP_COMPRESSTYPE)) {
			compressType = props
					.getInt(SpectrumanalyzerModule.PROP_COMPRESSTYPE);
		}
	}

	private void setPaintOptions() {
		// main pencil
		mainPaint.setAntiAlias(true);
		mainPaint.setDither(true);
		mainPaint.setColor(Color.GREEN);
		mainPaint.setStyle(Paint.Style.STROKE);
		mainPaint.setStrokeJoin(Paint.Join.ROUND);
		mainPaint.setStrokeCap(Paint.Cap.ROUND);
		fadePaint.setColor(Color.argb(238, 255, 255, 255)); // Adjust alpha to
		fadePaint.setXfermode(new PorterDuffXfermode(Mode.MULTIPLY));
	}

	public void start() {
		started = true;
		CANCELLED_FLAG = false;
		microLevelGrabber = new MicrophoneLevelGrabber();
		microLevelGrabber.execute();
	}

	public void stop() {
		CANCELLED_FLAG = true;
		canvas.drawColor(Color.BLACK);
		started = false;
		microLevelGrabber.cancel(true);
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
			int count = 0;
			while (started) {
				count++;
				if (count == 100) {
					count = 0;
				}
				if (isCancelled() || (CANCELLED_FLAG == true)) {
					started = false;
					// publishProgress(cancelledResult);
					Log.d("doInBackground", "Cancelling the RecordTask");
					break;
				} else {
					bufferReadResult = audioRecord.read(buffer, 0, blockSize);
					for (int i = 0; i < blockSize && i < bufferReadResult; i++) {
						toTransform[i] = compress((double) buffer[i])
								* modulation;
					}

					if (fftEnabled)
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
			if (vals.length == 0 || canvas == null || spectrumView == null)
				return;
			double sum = 0;
			int steps = width / vals.length;
			for (int i = 0; i < vals.length; i++) {
				int x = i;
				int downy = (int) (height / 2 - (vals[i] * height / blockSize));
				int upy = height / 2;
				int s;
				for (s = 0; s < steps; s++)
					canvas.drawLine(steps * x + s, downy, steps * x + s, upy,
							mainPaint);
			}
			spectrumView.invalidate();
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			try {
				audioRecord.stop();
			} catch (IllegalStateException e) {
				Log.e("Stop failed", e.toString());

			}
			canvas.drawColor(Color.BLACK);
			spectrumView.invalidate();
		}
	}

	private class SpectrumView extends View {
		public SpectrumView(Context ctx) {
			super(ctx);
		}

		@Override
		protected void onSizeChanged(int w, int h, int oldw, int oldh) {
			super.onSizeChanged(w, h, oldw, oldh);
			tiBitmap = (tiBitmap == null) ? Bitmap.createBitmap(w, h,
					Bitmap.Config.ARGB_8888) : Bitmap.createScaledBitmap(
					tiBitmap, w, h, true);
			canvas = new Canvas(tiBitmap);
			width = w; // need for scaled drawing
			height = h;
			Log.d(LCAT, "width x height = " + w + " x " + h);
		}

		@Override
		protected void onDraw(Canvas _canvas) {
			canvas.drawPaint(fadePaint);
			_canvas.drawBitmap(tiBitmap, new Matrix(), null);
		}
	}

	private double compress(double foo) {
		switch (compressType) {
		case SpectrumanalyzerModule.CURVE_LOG:
			if (foo < 0) {
				return -10 * Math.log10(-foo);
			} else if (foo > 0) {
				return 10 * Math.log10(foo);
			} else
				return 0.0;
		case SpectrumanalyzerModule.CURVE_LINEAR:
			return foo;
		case SpectrumanalyzerModule.CURVE_SQRT:
			if (foo < 0) {
				return -10 * Math.sqrt(-foo);
			} else if (foo > 0) {
				return 10 * Math.sqrt(foo);
			} else
				return 0;
		default:
			return foo;
		}
	}
}
// https://github.com/sommukhopadhyay/FFTBasedSpectrumAnalyzer/blob/master/src/com/somitsolutions/android/spectrumanalyzer/SoundRecordAndAnalysisActivity.java
