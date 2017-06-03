# Ti.SpectrumAnalyzer

## Permissions

You need the runtime permission _AUDIO_RECORDING_
## Usage

```javascript
var SA = require("ti.spectrumanalyzer");

var SpectrumView = SA.createView({
	color : "green",
	backgroundColor : "black",
	frequency :  8000, // optional
	width: "90%",
	top: 10,
	height : 300
	
});
var Window = Ti.UI.createWindow();
Window.add(SpectrumView);

Window.addEventListener("focus",function(){SpectrumView.start();});
Window.addEventListener("blur",function(){SpectrumView.stop();});


```