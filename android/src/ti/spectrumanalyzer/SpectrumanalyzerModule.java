package ti.spectrumanalyzer;

import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.annotations.Kroll;

@Kroll.module(name = "Spectrumanalyzer", id = "ti.spectrumanalyzer")
public class SpectrumanalyzerModule extends KrollModule {

	public final static String PROP_BLOCKSIZE = "blockSize";
	public final static String PROP_FADEENABLED = "fadeEnabled";
	public final static String PROP_FADETIME = "fadeTime";
	public final static String PROP_COMPRESSTYPE = "compressType";
	public final static String PROP_FFTENABLED = "fftEnabled";
	public final static String PROP_MODULATION = "modulation";

	@Kroll.constant
	public final static int CURVE_LINEAR = 0;
	@Kroll.constant
	public final static int CURVE_LOG = 1;
	@Kroll.constant
	public final static int CURVE_SQRT = 2;

	public static final String LCAT = "TiSpec";

	public SpectrumanalyzerModule() {
		super();
	}
}