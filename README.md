# Ti.SpectrumAnalyzer

## Permissions

You need the runtime permission _AUDIO_RECORDING_
## Usage

```javascript
var SA = require("ti.spectrumanalyzer");
var win = Ti.UI.createWindow();
var SpectrumView = SA.createView({
	color : "green",
	backgroundColor : "black",
	frequency :  8000, // optional
	width: "90%",
	top: 10,
	height : 300
	
});
window.add(SpectrumView);
SpectrumView.start();

```