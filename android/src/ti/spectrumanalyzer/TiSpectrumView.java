package ti.spectrumanalyzer;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.view.TiCompositeLayout;
import org.appcelerator.titanium.view.TiUIView;
import org.appcelerator.titanium.view.TiCompositeLayout.LayoutArrangement;

import ca.uol.aig.fftpack.RealDoubleFFT;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ImageView;

public class TiSpectrumView extends TiUIView {
	TiViewProxy proxy;
	TiCompositeLayout layout;
	private RealDoubleFFT transformer;
	int blockSize = 256;
	int frequency = 44100;
	int width;
	int height;
	int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;
	int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
	AudioRecord audioRecord;
	boolean started = false;
	boolean CANCELLED_FLAG = false;
	Canvas canvasDisplaySpectrum;
	Bitmap bitmapDisplaySpectrum;
	RecordAudio recordTask;

	Paint paintSpectrumDisplay;
	ImageView imageViewDisplaySpectrum;

	public TiSpectrumView(TiViewProxy proxy) {
		super(proxy);
		this.proxy = proxy;
		KrollDict opts = proxy.getProperties();
		LayoutArrangement arrangement = LayoutArrangement.DEFAULT;

		if (proxy.hasProperty(TiC.PROPERTY_LAYOUT)) {
			String layoutProperty = TiConvert.toString(proxy
					.getProperty(TiC.PROPERTY_LAYOUT));
			if (layoutProperty.equals(TiC.LAYOUT_HORIZONTAL)) {
				arrangement = LayoutArrangement.HORIZONTAL;
			} else if (layoutProperty.equals(TiC.LAYOUT_VERTICAL)) {
				arrangement = LayoutArrangement.VERTICAL;
			}
		}
		layout = new TiCompositeLayout(proxy.getActivity(), arrangement);
		setNativeView(layout);
	}

	@Override
	public void processProperties(KrollDict d) {
		super.processProperties(d);
	}

	public void init() {
		transformer = new RealDoubleFFT(blockSize);

		// imageview as container for ImageBitmap
		imageViewDisplaySpectrum = new ImageView(TiApplication.getInstance());
		layout.addView(imageViewDisplaySpectrum);
		width = imageViewDisplaySpectrum.getWidth();
		height = imageViewDisplaySpectrum.getHeight();

		bitmapDisplaySpectrum = Bitmap.createBitmap(width, height,
				Bitmap.Config.ARGB_8888);
		canvasDisplaySpectrum = new Canvas(bitmapDisplaySpectrum);

		paintSpectrumDisplay = new Paint();
		paintSpectrumDisplay.setColor(Color.GREEN);
		imageViewDisplaySpectrum.setImageBitmap(bitmapDisplaySpectrum);

	}

	public void start() {
		started = true;
		CANCELLED_FLAG = false;

		recordTask = new RecordAudio();
		recordTask.execute();
	}

	public void stop() {
		CANCELLED_FLAG = true;
		try {
			audioRecord.stop();
		} catch (IllegalStateException e) {
			Log.e("Stop failed", e.toString());

		}
		canvasDisplaySpectrum.drawColor(Color.BLACK);
	}

	private float handleCalculateHeight() {
		layout.setVisibility(View.VISIBLE);
		layout.measure(View.MeasureSpec.makeMeasureSpec(0,
				View.MeasureSpec.UNSPECIFIED), View.MeasureSpec
				.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
		return layout.getMeasuredHeight();
	}

	private class RecordAudio extends AsyncTask<Void, double[], Boolean> {

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
				Log.e("Recording failed", e.toString());

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
			Log.e("RecordingProgress", "Displaying in progress");
			Log.d("Test:", Integer.toString(progress[0].length));
			for (int i = 0; i < progress[0].length; i++) {
				int x = 2 * i;
				int downy = (int) (150 - (progress[0][i] * 10));
				int upy = 150;
				canvasDisplaySpectrum.drawLine(x, downy, x, upy,
						paintSpectrumDisplay);
			}
			imageViewDisplaySpectrum.invalidate();
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			try {
				audioRecord.stop();
			} catch (IllegalStateException e) {
				Log.e("Stop failed", e.toString());

			}

			canvasDisplaySpectrum.drawColor(Color.BLACK);
			imageViewDisplaySpectrum.invalidate();

		}
	}

}
