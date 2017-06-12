/**
 * Copyright 2013, Haruki Hasegawa
 *
 * Licensed under the MIT license:
 * http://creativecommons.org/licenses/MIT/
 */

package com.pheelicks.utils;



import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

// https://gist.github.com/pec1985/5847653
import org.appcelerator.titanium.util.TiRHelper;
import org.appcelerator.titanium.util.TiRHelper.ResourceNotFoundException;

public class TunnelPlayerWorkaround {
	public static int getRaw(String str) {
		try {
			return TiRHelper.getApplicationResource("raw." + str);
		} catch (ResourceNotFoundException e) {
			e.printStackTrace();
			return 0;
		}
	}

	private static final String LTAG = "Pheelicks";
	private static final String SYSTEM_PROP_TUNNEL_DECODE_ENABLED = "tunnel.decode";

	private TunnelPlayerWorkaround() {
	}

	/**
	 * Obtain "tunnel.decode" system property value
	 * 
	 * @param context
	 *            Context
	 * @return Whether tunnel player is enabled
	 */
	public static boolean isTunnelDecodeEnabled(Context context) {
		return SystemPropertiesProxy.getBoolean(context,
				SYSTEM_PROP_TUNNEL_DECODE_ENABLED, false);
	}

	/**
	 * Create silent MediaPlayer instance to avoid tunnel player issue
	 * 
	 * @param context
	 *            Context
	 * @return MediaPlayer instance
	 */
	public static MediaPlayer createSilentMediaPlayer(Context context) {
		boolean result = false;
		MediaPlayer mp = null;
		try {
			// mp = MediaPlayer.create(context, R.raw.workaround_1min);
			mp = MediaPlayer.create(context, getRaw("workaround_1min"));
			Log.d(LTAG, "AudioSessionId=" + mp.getAudioSessionId());
			mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mp.setVolume(1.0f, 1.0f);
			mp.setLooping(false);
			// mp.prepare();
			// NOTE: start() is no needed
			result = true;
		} catch (RuntimeException e) {
			Log.e(LTAG, "createSilentMediaPlayer()", e);
		} finally {
			if (!result && mp != null) {
				try {
					mp.release();
				} catch (IllegalStateException e) {
				}
			}
		}

		return mp;
	}
}
