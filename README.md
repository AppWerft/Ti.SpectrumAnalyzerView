# Ti.SpectrumAnalyzer

<img src="https://raw.githubusercontent.com/AppWerft/Ti.SpectrumAnalyzerView/master/screen.png" width=240 />

## Permissions

You need the runtime permission _AUDIO_RECORDING_
## Usage

```javascript
var SA = require("ti.spectrumanalyzer");

var SpectrumView = SA.createView({
	color : "green",
	backgroundColor : "black",
	frequency :  44100, // optional
	blockSize :  512, // optional
	fade : true,
	fadeTime : 1000,
	width: "90%",
	top: 10,
	height : 300
	
});
var Window = Ti.UI.createWindow();
Window.add(SpectrumView);

Window.addEventListener("focus",function(){SpectrumView.start();});
Window.addEventListener("blur",function(){SpectrumView.stop();});


```