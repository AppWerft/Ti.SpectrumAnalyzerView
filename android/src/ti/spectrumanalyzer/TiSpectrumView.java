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
	Paint paintSD;
	ImageView imageViewDS;

	public TiSpectrumView(TiViewProxy proxy) {
		super(proxy);
		this.proxy = proxy;
		ctx = TiApplication.getInstance().getApplicationContext();
		KrollDict opts = proxy.getProperties();
		if (opts.containsKeyAndNotNull("frequency"))
			frequency = opts.getInt("frequency");
		if (opts.containsKeyAndNotNull("blockSize"))
			blockSize = opts.getInt("blockSize");
		if (opts.containsKeyAndNotNull(TiC.PROPERTY_COLOR))
			color = TiConvert.toColor(opts.getString(TiC.PROPERTY_COLOR));
		view = new TiSpectrumLayout(proxy.getActivity(),
				LayoutArrangement.DEFAULT);
		setNativeView(view);
		transformer = new RealDoubleFFT(blockSize);
		paintSD = new Paint();
		paintSD.setColor(color);
	}

	@Override
	public void processProperties(KrollDict d) {
		super.processProperties(d);
	}

	public void initRenderer() {
		imageViewDS = new ImageView(ctx);
		view.addView(imageViewDS, new LayoutParams(width, height));
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
			// Log.e(LCAT, "RecordingProgress Displaying in progress");
			// Log.d(LCAT, "Test:" + Integer.toString(progress[0].length));
			for (int i = 0; i < progress[0].length; i++) {
				int x = 2 * i;
				int downy = (int) (150 - (progress[0][i] * 10));
				int upy = 150;
				canvasDS.drawLine(x, downy, x, upy, paintSD);
				// Log.d(LCAT, "x=" + x + " downy=" + downy + " x=" + x +
				// " upy="
				// + upy);
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

	public class TiSpectrumLayout extends TiCompositeLayout {
		public TiSpectrumLayout(Context context, LayoutArrangement arrangement) {
			super(context, arrangement, proxy);
		}

		@Override
		protected int getWidthMeasureSpec(View child) {

			return super.getWidthMeasureSpec(child);

		}

		@Override
		protected int getHeightMeasureSpec(View child) {
			return super.getHeightMeasureSpec(child);

		}

		@Override
		protected int getMeasuredWidth(int maxWidth, int widthSpec) {
			return resolveSize(maxWidth, widthSpec);
		}

		@Override
		protected void onLayout(boolean changed, int l, int t, int r, int b) {
			if (changed) {
				width = this.getWidth();
				height = this.getHeight();
				initRenderer();
			}
		}

		@Override
		protected int getMeasuredHeight(int maxHeight, int heightSpec) {
			return resolveSize(maxHeight, heightSpec);

		}
	}

}
