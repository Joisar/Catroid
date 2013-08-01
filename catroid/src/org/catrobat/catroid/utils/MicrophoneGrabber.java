/**
 *  Catroid: An on-device visual programming system for Android devices
 *  Copyright (C) 2010-2013 The Catrobat Team
 *  (<http://developer.catrobat.org/credits>)
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *  
 *  An additional term exception under section 7 of the GNU Affero
 *  General Public License, version 3, is available at
 *  http://developer.catrobat.org/license_additional_term
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU Affero General Public License for more details.
 *  
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.utils;

import java.util.ArrayList;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

public class MicrophoneGrabber extends Thread {
	private static MicrophoneGrabber instance = null;
	private static final String TAG = MicrophoneGrabber.class.getSimpleName();

	public static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
	public static final int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;
	public static final int sampleRate = 16000;
	public static final int frameByteSize = 2048;

	private ArrayList<microphoneListener> microphoneListenerList = new ArrayList<microphoneListener>();
	private boolean isRecording;
	public boolean isPaused = false;
	private AudioRecord audioRecord;
	private byte[] buffer;

	private MicrophoneGrabber() {
		int recBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfiguration, audioEncoding); // need to be larger than size of a frame
		audioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, sampleRate, channelConfiguration,
				audioEncoding, recBufSize);
		buffer = new byte[frameByteSize];
	}

	public static MicrophoneGrabber getInstance() {
		if (instance == null) {
			instance = new MicrophoneGrabber();
		}
		return instance;
	}

	public void registerListener(microphoneListener litener) {
		synchronized (microphoneListenerList) {
			if (!isRecording && !isPaused) {
				this.start();
			}
			microphoneListenerList.add(litener);
		}
		return;
	}

	public void unregisterListener(microphoneListener litener) {
		synchronized (microphoneListenerList) {
			if (microphoneListenerList.contains(litener)) {
				microphoneListenerList.remove(litener);
			} else {
				Log.v(TAG, "Tried to remove not registered microphoneListener. " + litener.getClass().getSimpleName());
			}

			if (microphoneListenerList.size() == 0) {
				isRecording = false;
			}
		}
		return;
	}

	@Override
	public void run() {

		isRecording = true;
		audioRecord.startRecording();

		while (isRecording) {
			int offset = 0;
			int shortRead = 0;

			while (offset < frameByteSize) {
				shortRead = audioRecord.read(buffer, 0, frameByteSize);
				offset += shortRead;
			}

			final byte[] broadcastBuffer = buffer.clone();
			final ArrayList<microphoneListener> dataListener = new ArrayList<microphoneListener>(microphoneListenerList);
			for (microphoneListener listener : dataListener) {
				listener.onMicrophoneData(broadcastBuffer);
			}
		}

		audioRecord.stop();
	}

	public boolean isRecording() {
		return this.isAlive() && isRecording;
	}

	public void pauseRecording() {
		if (microphoneListenerList.size() > 0 && isPaused == false && isRecording == true) {
			ArrayList<microphoneListener> dataListener = new ArrayList<microphoneListener>(microphoneListenerList);
			for (microphoneListener listener : dataListener) {
				listener.onPauseStateChanged(true);
			}
			isPaused = true;
			this.isRecording = false;
		}
	}

	public void resumeRecording() {
		if (microphoneListenerList.size() > 0 && isPaused == true && isRecording == false) {
			ArrayList<microphoneListener> dataListener = new ArrayList<microphoneListener>(microphoneListenerList);
			for (microphoneListener listener : dataListener) {
				listener.onPauseStateChanged(false);
			}
			isPaused = false;
			this.start();
		}

	}

	public interface microphoneListener {
		public void onMicrophoneData(byte[] recievedBuffer);

		public void onPauseStateChanged(boolean isPaused);
	}
}
