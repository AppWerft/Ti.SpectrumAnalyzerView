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

public class SpectrumView extends View {
	private static final String LCAT = SpectrumanalyzerModule.LCAT;
	private RealDoubleFFT transformer;
	private int blockSize = 256;
	private int frequency = 44100;
	private int color = Color.GREEN;
	private boolean fftEnabled = true;
	private boolean fadeEnabled = true;
	private double modulation = 1.0;
	private int compressType = SpectrumanalyzerModule.CURVE_LINEAR;
	private boolean filled = false;
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

	public SpectrumView(Context context) {
		this(context, 256, 44100, Color.GREEN, true, true, 1.0,
				SpectrumanalyzerModule.CURVE_LINEAR, false);
	}

	public SpectrumView(Context context, int blockSize, int frequency,
			int color, boolean fftEnabled, boolean fadeEnabled,
			double modulation, int compressType, boolean filled) {
		super(context);
		this.blockSize = blockSize;
		this.frequency = frequency;
		this.color = color;
		this.fftEnabled = fftEnabled;
		this.fadeEnabled = fadeEnabled;
		this.modulation = modulation;
		this.compressType = compressType;
		this.filled = filled;
		transformer = new RealDoubleFFT(blockSize);
		setPaintOptions();
	}

	public void start() {
		started = true;
		CANCELLED_FLAG = false;
		microLevelGrabber = new MicrophoneLevelGrabber();
		microLevelGrabber.execute();
		Log.d(LCAT, "microLevelGrabber.execute()");
	}

	public void stop() {
		CANCELLED_FLAG = true;
		canvas.drawColor(Color.BLACK);
		started = false;
		microLevelGrabber.cancel(true);
	}

	public void setPaintOptions() {
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

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		tiBitmap = (tiBitmap == null) ? Bitmap.createBitmap(w, h,
				Bitmap.Config.ARGB_8888) : Bitmap.createScaledBitmap(tiBitmap,
				w, h, true);
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
					break;
				} else {
					bufferReadResult = audioRecord.read(buffer, 0, blockSize);
					for (int i = 0; i < blockSize && i < bufferReadResult; i++) {
						toTransform[i] = compress((double) buffer[i])
								* modulation;
					}
					if (transformer == null)
						transformer = new RealDoubleFFT(blockSize);
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

		@Override
		protected void onProgressUpdate(double[]... progress) {
			double[] vals = progress[0];
			if (vals.length == 0 || canvas == null)
				return;
			int lasty = (fftEnabled) ? height : height / 2;
			int lastx = 0;
			int x;
			int y;
			int steps = width / vals.length;
			double scale = (fftEnabled == true) ? 0.001 : 0.01;
			if (filled) {
				for (int i = 0; i < vals.length; i++) {
					x = i;
					int downy = (int) (height / 2 - (vals[i] * height / blockSize));
					int upy = height / 2;
					int s;
					for (s = 0; s < steps; s++)
						canvas.drawLine(steps * x + s, downy, steps * x + s,
								upy, mainPaint);
				}
			} else {
				for (int i = 0; i < vals.length; i++) {
					x = i * width / blockSize;
					y = (fftEnabled) ? (int) (height - Math.abs(vals[i]
							* height / blockSize * scale))
							: (int) (height / 2 - (vals[i] * height / blockSize * scale));
					canvas.drawLine(lastx, lasty, x, y, mainPaint);
					lasty = y;
					lastx = x;
				}
			}
			invalidate();
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
			invalidate();
		}
	}

	private double compress(double foo) {
		switch (compressType) {
		case SpectrumanalyzerModule.CURVE_LOG:
			if (foo < 0) {
				return -1000 * Math.log10(-10000 * foo);
			} else if (foo > 0) {
				return 1000 * Math.log10(10000 * foo);
			} else
				return 0.0;
		case SpectrumanalyzerModule.CURVE_LINEAR:
			return foo;
		case SpectrumanalyzerModule.CURVE_SQRT:
			if (foo < 0) {
				return -1 * Math.sqrt(-10000 * foo);
			} else if (foo > 0) {
				return Math.sqrt(10000 * foo);
			} else
				return 0;
		default:
			return foo;
		}
	}
}
