# Ti.SpectrumAnalyzer

<img src="https://raw.githubusercontent.com/AppWerft/Ti.SpectrumAnalyzerView/master/screen.png" width=240 />

## Permissions

You need the runtime permission _AUDIO_RECORDING_

## Constants

- [x] CURVE_LINEAR
- [x] CURVE_LOG
- [x] CURVE_SQRT

## Usage

```javascript
var SA = require("ti.spectrumanalyzer");

var SpectrumView = SA.createView({
	color : "green",
	backgroundColor : "black",
	frequency :  44100, // optional
	blockSize :  512, // optional
	fadeEnabled : true, // default
	fftEnabled  : true, // default
	fadeTime : 50, // 0 … 255
	compressType : SA.CURVE_LOG,
	width: "90%",
	top: 10,
	height : 300
	
});
var Window = Ti.UI.createWindow();
Window.add(SpectrumView);

Window.addEventListener("focus",function(){SpectrumView.start();});
Window.addEventListener("blur",function(){SpectrumView.stop();});


```