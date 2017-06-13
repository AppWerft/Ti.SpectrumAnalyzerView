package ti.spectrumanalyzer;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.view.TiUIView;
import android.graphics.*;

public class TiSpectrumView extends TiUIView {
	private SpectrumView spectrumView; // extended from view

	public TiSpectrumView(final TiViewProxy proxy) {
		super(proxy);
		// all props with defaults:
		int blockSize = 256;
		int frequency = 44100;
		int color = Color.GREEN;
		boolean fftEnabled = true;
		boolean fadeEnabled = true;
		double modulation = 1.0;
		int compressType = SpectrumanalyzerModule.CURVE_LINEAR;
		boolean filled = false;
		boolean autoStart;
		// import props from JS
		KrollDict props = proxy.getProperties();
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
		if (props.containsKeyAndNotNull(SpectrumanalyzerModule.PROP_FILLED)) {
			filled = props.getBoolean(SpectrumanalyzerModule.PROP_FILLED);
		}
		if (props
				.containsKeyAndNotNull(SpectrumanalyzerModule.PROP_COMPRESSTYPE)) {
			compressType = props
					.getInt(SpectrumanalyzerModule.PROP_COMPRESSTYPE);
		}
		setNativeView(spectrumView = new SpectrumView(proxy.getActivity(),
				blockSize, frequency, color, fftEnabled, fadeEnabled,
				modulation, compressType, filled));
	}

	public void start() {
		spectrumView.start();
	}

	public void stop() {
		spectrumView.stop();
	}
}
