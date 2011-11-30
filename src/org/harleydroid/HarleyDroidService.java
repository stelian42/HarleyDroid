//
// HarleyDroid: Harley Davidson J1850 Data Analyser for Android.
//
// Copyright (C) 2010,2011 Stelian Pop <stelian@popies.net>
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.
//

package org.harleydroid;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class HarleyDroidService extends Service
{
	private static final boolean D = false;
	private static final String TAG = HarleyDroidService.class.getSimpleName();

	private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

	private static final int AT_TIMEOUT = 2000;
	private static final int ATZ_TIMEOUT = 5000;
	private static final int ATMA_TIMEOUT = 30000;

	private static final int MAX_ERRORS = 10;

	private final IBinder binder = new HarleyDroidServiceBinder();
	private HarleyDroidLogger mLogger = null;
	private HarleyData mHD;
	private NotificationManager mNM;
	private Handler mHandler = null;
	private ThreadELM mThread = null;
	private String mInterfaceType = null;
	private boolean mAutoReconnect = false;
	private int mReconnectDelay = 0;

	@Override
	public void onCreate() {
		super.onCreate();
		if (D) Log.d(TAG, "onCreate()");

		mHD = new HarleyData();

		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		CharSequence text = getText(R.string.notification_start);
		Notification notification = new Notification(R.drawable.ic_stat_notify_harleydroid, text, System.currentTimeMillis());
		notification.flags = Notification.FLAG_ONGOING_EVENT;
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, HarleyDroid.class), 0);
		notification.setLatestEventInfo(this, getText(R.string.notification_label), text, contentIntent);
		mNM.notify(R.string.notification_label, notification);
	}

	public void onDestroy() {
		super.onDestroy();
		if (D) Log.d(TAG, "onDestroy()");

		mNM.cancel(R.string.notification_label);
	}

	@Override
	public IBinder onBind(Intent intent) {
		if (D) Log.d(TAG, "onBind()");

		return binder;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (D) Log.d(TAG, "onStartCommand()");

		return START_STICKY;
	}

	public HarleyData getHarleyData() {
		return mHD;
	}

	public void setHandler(Handler handler) {
		if (D) Log.d(TAG, "setHandler()");

		mHandler = handler;
	}

	public void startService(String interfaceType, BluetoothDevice dev, boolean metric, boolean logging, boolean gps, boolean autoReconnect, int reconnectDelay) {
		if (D) Log.d(TAG, "startService()");

		mInterfaceType = interfaceType;
		mAutoReconnect = autoReconnect;
		mReconnectDelay = reconnectDelay;

		if (logging) {
			mLogger = new HarleyDroidLogger(this, metric, gps);
			mLogger.start();
			mHD.addHarleyDataListener(mLogger);
		}
		mThread = new ThreadELM(dev);
		mThread.start();
	}

	public void stopService() {
		if (D) Log.d(TAG, "stopService()");

		mThread.stop = true;
		mThread = null;
		if (mLogger != null) {
			mHD.removeHarleyDataListener(mLogger);
			mLogger.stop();
		}
	}

	public boolean isRunning() {
		if (D) Log.d(TAG, "isRunning()");

		return (mThread != null);
	}

	public class HarleyDroidServiceBinder extends Binder {
		HarleyDroidService getService() {
			return HarleyDroidService.this;
		}
	}

	private class ThreadELM extends Thread {
		private BluetoothDevice mDevice;
		private BufferedReader mIn;
		private OutputStream mOut;
		private BluetoothSocket mSock;
		private Timer mTimer;
		boolean stop = false;

		class CancelTimer extends TimerTask {
			public void run() {
				if (D) Log.d(TAG, "CANCEL AT " + System.currentTimeMillis());
				try {
					writeLine("");
				} catch (IOException e) {
				}
				try {
					mSock.close();
				} catch (IOException e) {
				}
			}
		};

		public ThreadELM(BluetoothDevice device) {
			mDevice = device;
			mTimer = new Timer();
		}

		private String readLine(long timeout) throws IOException {
			CancelTimer t = new CancelTimer();
			mTimer.schedule(t, timeout);
			if (D) Log.d(TAG, "READLINE AT " + System.currentTimeMillis());
			String line = mIn.readLine();
			t.cancel();
			if (D) Log.d(TAG, "read (" + line.length() + "): " + line);
			return line;
		}

		private void writeLine(String line) throws IOException {
			line += "\r";
			if (D) Log.d(TAG, "write: " + line);
			mOut.write(line.getBytes());
			mOut.flush();
		}

		private String chat(String send, String expect, long timeout) throws IOException {
			StringBuilder line = new StringBuilder();
			writeLine(send);
			long start = System.currentTimeMillis();
			while (timeout > 0) {
				line.append(readLine(timeout));
				long now = System.currentTimeMillis();
				if (line.indexOf(expect) != -1)
					return line.toString();
				timeout -= (now - start);
				start = now;
			}
			throw new IOException("timeout");
		}

		public void runEmulator() {
			int cnt = 0;
			int errors = 0;

			if (D) Log.d(TAG, "ready");
			mHandler.obtainMessage(HarleyDroid.STATUS_CONNECTED, -1, -1).sendToTarget();

			while (!stop) {
				String line;

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
				}

				/* send several messages to update the UI */
				mHD.setOdometer(cnt);
				cnt += 50;
				if (cnt % 100 == 0) {
					mHD.setCheckEngine(true);
					mHD.setTurnSignals(0);
				}
				else {
					mHD.setCheckEngine(false);
					mHD.setTurnSignals(3);
				}

				// RPM at 1053
				line = "28 1B 10 02 10 74 4C";
				// Speed at 100 km/h
				line = "J4829100232000f";
				// Odometer
				//line = "a8 69 10 06 00 00 FF 61";


				if (J1850.parse(line.getBytes(), mHD))
					errors = 0;
					//++errors;
				else
					++errors;

				if (errors > MAX_ERRORS) {
					mHandler.obtainMessage(HarleyDroid.STATUS_TOOMANYERRORS, -1, -1).sendToTarget();
					break;
				}
			}
		}

		public void runBluetooth() {
			int errors = 0;
			int elmVersionMajor = 0;
			int elmVersionMinor = 0;
			@SuppressWarnings("unused")
			int elmVersionRelease = 0;

			if (D) Log.d(TAG, "started");
			mHandler.obtainMessage(HarleyDroid.STATUS_CONNECTING, -1, -1).sendToTarget();

			try {
				mSock = mDevice.createRfcommSocketToServiceRecord(MY_UUID);
				mSock.connect();
				mIn = new BufferedReader(new InputStreamReader(mSock.getInputStream()), 128);
				mOut = mSock.getOutputStream();
			} catch (IOException e1) {
				try {
					mSock.close();
				} catch (IOException e2) {
				}
				mHandler.obtainMessage(HarleyDroid.STATUS_ERROR, -1, -1).sendToTarget();
				return;
			}

			if (D) Log.d(TAG, "connected");

			if (mInterfaceType.equals("elm327")) {
				try {
					chat("AT", "", AT_TIMEOUT);
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e2) {
					}
					// Warm Start
					String reply = chat("ATWS", "ELM327", ATZ_TIMEOUT);
					// parse reply to extract version information
					Pattern p = Pattern.compile("(?s)^.*ELM327 v(\\d+)\\.(\\d+)(\\w?).*$");
					Matcher m = p.matcher(reply);
					if (m.matches()) {
						elmVersionMajor = Integer.parseInt(m.group(1));
						elmVersionMinor = Integer.parseInt(m.group(2));
						if (m.group(3).length() > 0)
							elmVersionRelease = m.group(3).charAt(0);
					}
					// Echo ON
					chat("ATE1", "OK", AT_TIMEOUT);
					// Headers ON
					chat("ATH1", "OK", AT_TIMEOUT);
					// Spaces OFF
					if (elmVersionMajor >= 1 && elmVersionMinor >= 3)
						chat("ATS0", "OK", AT_TIMEOUT);
					// Select Protocol SAE J1850 VPW (10.4 kbaud)
					chat("ATSP2", "OK", AT_TIMEOUT);
					// Monitor All
					chat("ATMA", "", AT_TIMEOUT);
				} catch (IOException e1) {
					mHandler.obtainMessage(HarleyDroid.STATUS_ERRORAT, -1, -1).sendToTarget();
					// socket is already closed...
					return;
				}
			}

			if (D) Log.d(TAG, "ready");
			mHandler.obtainMessage(HarleyDroid.STATUS_CONNECTED, -1, -1).sendToTarget();

			while (!stop) {
				String line;

				try {
					line = readLine(ATMA_TIMEOUT);
				} catch (IOException e1) {
					mHandler.obtainMessage(HarleyDroid.STATUS_NODATA, -1, -1).sendToTarget();
					// socket is already closed...
					return;
				}

				if (J1850.parse(line.getBytes(), mHD))
					errors = 0;
				else
					++errors;

				if (errors > MAX_ERRORS) {
					mHandler.obtainMessage(HarleyDroid.STATUS_TOOMANYERRORS, -1, -1).sendToTarget();
					try {
						writeLine("");
					} catch (IOException e1) {
					}
					try {
						mSock.close();
					} catch (IOException e2) {
					}
					return;
				}
			}

			try {
				writeLine("");
			} catch (IOException e1) {
			}
			try {
				mSock.close();
			} catch (IOException e2) {
			}
		}

		public void run() {

			while (true) {

				if (HarleyDroid.EMULATOR)
					runEmulator();
				else
					runBluetooth();

				if (stop || !mAutoReconnect)
					break;

				mHandler.obtainMessage(HarleyDroid.STATUS_AUTORECON, -1, -1).sendToTarget();

				try {
					Thread.sleep(mReconnectDelay * 1000);
				} catch (InterruptedException e) {
				}

				if (stop)
					break;
			}
			finish();
		}

		private void finish() {
			if (mLogger != null)
				mLogger.stop();
			mTimer.cancel();
			stopSelf();
		}
	}
}
