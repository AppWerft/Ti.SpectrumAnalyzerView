package ti.spectrumanalyzer;

import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.annotations.Kroll;

@Kroll.module(name = "Spectrumanalyzer", id = "ti.spectrumanalyzer")
public class SpectrumanalyzerModule extends KrollModule {

	@Kroll.constant
	public final static String PROP_BLOCKSIZE = "blockSize";
	public static final String LCAT = "TiSpec";

	public SpectrumanalyzerModule() {
		super();
	}
}