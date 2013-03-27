//
// HarleyDroid: Harley Davidson J1850 Data Analyser for Android.
//
// Copyright (C) 2010-2012 Stelian Pop <stelian@popies.net>
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

import java.lang.ref.WeakReference;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class HarleyDroidService extends Service
{
	private static final boolean D = false;
	private static final String TAG = HarleyDroidService.class.getSimpleName();

	private static final int MSG_NONE = 0;
	private static final int MSG_CONNECTED = 1;
	private static final int MSG_DISCONNECTED = 2;
	private static final int MSG_START_POLL = 3;
	private static final int MSG_STARTED_POLL = 4;
	private static final int MSG_START_SEND = 5;
	private static final int MSG_STARTED_SEND = 6;
	private static final int MSG_SET_SEND = 7;
	private static final int MSG_DISCONNECT = 8;

	private static final int STATE_DISCONNECT = 1;
	private static final int STATE_TO_DISCONNECT = 2;
	private static final int STATE_CONNECT = 3;
	private static final int STATE_TO_CONNECT = 4;
	private static final int STATE_POLL = 5;
	private static final int STATE_TO_POLL = 6;
	private static final int STATE_SEND = 7;
	private static final int STATE_TO_SEND = 8;
	private static final int STATE_WAIT_RECONNECT = 9;

	private final IBinder binder = new HarleyDroidServiceBinder();
	private HarleyDroidLogger mLogger = null;
	private HarleyData mHD;
	private int mNotifyId;
	private NotificationCompat.Builder mNotifyBuilder;
	private NotificationManager mNotificationManager;
	private Handler mHandler = null;
	private String mInterfaceType = null;
	private BluetoothDevice mDevice = null;
	private J1850Interface mInterface = null;
	private boolean mLogging = false;
	private boolean mMetric = true;
	private boolean mGPS = true;
	private boolean mLogRaw = false;
	private boolean mLogUnknown = false;
	private boolean mAutoReconnect = false;
	private Thread mReconThread = null;
	private int mReconnectDelay = 0;
	private int mCurrentState = STATE_DISCONNECT;
	private int mWantedState = STATE_DISCONNECT;
	private String mSendType[];
	private String mSendTA[];
	private String mSendSA[];
	private String mSendCommand[];
	private String mSendExpect[];
	private int mSendTimeout[];
	private int mSendDelay;
	protected SharedPreferences mPrefs;
	private Handler mServiceHandler;

	@Override
	public void onCreate() {
		super.onCreate();
		if (D) Log.d(TAG, "onCreate()");

		mPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		mHD = new HarleyData(mPrefs);
		mServiceHandler = new HarleyDroidServiceHandler(this);

		mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		mNotifyId = 1;
		mNotifyBuilder = new NotificationCompat.Builder(this)
		        .setSmallIcon(R.drawable.ic_stat_notify_harleydroid)
		        .setContentTitle(getText(R.string.notification_label))
		        .setOngoing(true)
		        .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, HarleyDroidDashboard.class), 0));
		notify(R.string.notification_connecting);
		startForeground(mNotifyId, mNotifyBuilder.build());
	}

	public void onDestroy() {
		super.onDestroy();
		if (D) Log.d(TAG, "onDestroy()");

		doDisconnect();
		mNotificationManager.cancel(mNotifyId);
		mNotificationManager = null;
		mHD.destroy();
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

	public class HarleyDroidServiceBinder extends Binder {
		HarleyDroidService getService() {
			return HarleyDroidService.this;
		}
	}

	public HarleyData getHarleyData() {
		return mHD;
	}

	public void setHandler(Handler handler) {
		if (D) Log.d(TAG, "setHandler()");

		mHandler = handler;
	}

	public void setLogging(boolean logging, boolean metric, boolean gps, boolean logRaw, boolean logUnknown) {
		mLogging = logging;
		mMetric = metric;
		mGPS = gps;
		mLogRaw = logRaw;
		mLogUnknown = logUnknown;
	}

	public void setAutoReconnect(boolean autoReconnect, int reconnectDelay) {
		mAutoReconnect = autoReconnect;
		mReconnectDelay = reconnectDelay;
	}

	public void setInterfaceType(String interfaceType, BluetoothDevice dev) {
		boolean reconnect = (mInterface != null);

		if (mInterfaceType == null ||
			!mInterfaceType.equals(interfaceType) ||
			(!HarleyDroid.EMULATOR && (mDevice == null ||
									   !mDevice.getAddress().equals(dev.getAddress())))) {

			if (D) Log.d(TAG, "setInterfaceType(" + interfaceType + ")");

			mInterfaceType = interfaceType;
			mDevice = dev;
			if (reconnect)
				doDisconnect();
			if (HarleyDroid.EMULATOR)
				mInterface = new EmulatorInterface(this);
			else {
				if (interfaceType.equals("elm327"))
					mInterface = new ELM327Interface(this, dev);
				else if (interfaceType.equals("hdi"))
					mInterface = new HarleyDroidInterface(this, dev);
			}
			mCurrentState = STATE_DISCONNECT;
			if (reconnect)
				stateMachine();
		}
	}

	public boolean isInitialized() {
		return mInterface != null;
	}

	private void notify(int id) {
		if (mNotificationManager != null) {
			CharSequence text = getText(id);
			mNotifyBuilder.setContentText(text);
			mNotificationManager.notify(mNotifyId, mNotifyBuilder.build());
		}
	}

	private void doDisconnect() {
		if (mReconThread != null)
			mReconThread.interrupt();
		if (mLogger != null) {
			mHD.removeHarleyDataDashboardListener(mLogger);
			mHD.removeHarleyDataDiagnosticsListener(mLogger);
			mHD.removeHarleyDataRawListener(mLogger);
			mLogger.stop();
			mLogger = null;
		}
		if (mInterface != null)
			mInterface.disconnect();
	}

	private void stateMachine() {
		if (D) Log.d(TAG, "stateMachine(): transition from " + mCurrentState + " to " + mWantedState);

		if (mCurrentState == mWantedState)
			return;

		switch (mWantedState) {
		case STATE_CONNECT:
			switch (mCurrentState) {
			case STATE_DISCONNECT:
			case STATE_WAIT_RECONNECT:
				mCurrentState = STATE_TO_CONNECT;
				notify(R.string.notification_connecting);
				mHandler.obtainMessage(HarleyDroid.STATUS_CONNECTING, -1, -1).sendToTarget();
				mInterface.connect(mHD);
				return;
			case STATE_POLL:
			case STATE_SEND:
			case STATE_TO_DISCONNECT:
			case STATE_TO_CONNECT:
			case STATE_TO_POLL:
			case STATE_TO_SEND:
				/* nothing to done, wait for state to settle */
				return;
			}
			break;
		case STATE_DISCONNECT:
			switch (mCurrentState) {
			case STATE_CONNECT:
			case STATE_SEND:
			case STATE_POLL:
			case STATE_WAIT_RECONNECT:
				stopSelf();
				return;
			case STATE_TO_DISCONNECT:
			case STATE_TO_CONNECT:
			case STATE_TO_POLL:
			case STATE_TO_SEND:
				/* nothing to done, wait for state to settle */
				return;
			}
			return;
		case STATE_POLL:
			switch (mCurrentState) {
			case STATE_DISCONNECT:
			case STATE_WAIT_RECONNECT:
				mCurrentState = STATE_TO_CONNECT;
				notify(R.string.notification_connecting);
				mHandler.obtainMessage(HarleyDroid.STATUS_CONNECTING, -1, -1).sendToTarget();
				mInterface.connect(mHD);
				return;
			case STATE_CONNECT:
			case STATE_SEND:
				mCurrentState = STATE_TO_POLL;
				mInterface.startPoll();
				return;
			case STATE_TO_DISCONNECT:
			case STATE_TO_CONNECT:
			case STATE_TO_POLL:
			case STATE_TO_SEND:
				/* nothing to done, wait for state to settle */
				return;
			}
			break;
		case STATE_SEND:
			switch (mCurrentState) {
			case STATE_DISCONNECT:
			case STATE_WAIT_RECONNECT:
				mCurrentState = STATE_TO_CONNECT;
				notify(R.string.notification_connecting);
				mHandler.obtainMessage(HarleyDroid.STATUS_CONNECTING, -1, -1).sendToTarget();
				mInterface.connect(mHD);
				return;
			case STATE_CONNECT:
			case STATE_POLL:
				mCurrentState = STATE_TO_SEND;
				mInterface.startSend(mSendType, mSendTA, mSendSA, mSendCommand, mSendExpect, mSendTimeout, mSendDelay);
				return;
			case STATE_TO_DISCONNECT:
			case STATE_TO_CONNECT:
			case STATE_TO_POLL:
			case STATE_TO_SEND:
				/* nothing to done, wait for state to settle */
				return;
			}
			break;
		case STATE_WAIT_RECONNECT:
			switch (mCurrentState) {
			case STATE_DISCONNECT:
				mCurrentState = STATE_WAIT_RECONNECT;
				return;
			case STATE_CONNECT:
			case STATE_POLL:
			case STATE_TO_DISCONNECT:
			case STATE_TO_CONNECT:
			case STATE_TO_POLL:
			case STATE_TO_SEND:
				/* nothing to done, wait for state to settle */
				return;
			}
			break;
		}
		if (D) Log.d(TAG, "stateMachine(): bad state transition from " + mCurrentState + " to " + mWantedState);
	}

	public void handleMessage(Message msg) {
		if (D) Log.d(TAG, "handleMessage " + msg.what);

		switch (msg.what) {
		case MSG_NONE:
			mReconThread = null;
			mWantedState = msg.arg1;
			break;
		case MSG_CONNECTED:
			mHandler.obtainMessage(HarleyDroid.STATUS_CONNECTED, -1, -1).sendToTarget();
			HarleyDroidService.this.notify(R.string.notification_connected);
			J1850.resetCounters();
			mCurrentState = STATE_CONNECT;
			if (mLogging) {
				mLogger = new HarleyDroidLogger(HarleyDroidService.this, mMetric, mGPS, mLogRaw, mLogUnknown);
				mLogger.start();
				mHD.addHarleyDataDashboardListener(mLogger);
				mHD.addHarleyDataDiagnosticsListener(mLogger);
				mHD.addHarleyDataRawListener(mLogger);
			}
			break;
		case MSG_DISCONNECT:
			mHandler.obtainMessage(HarleyDroid.STATUS_NONE, -1, -1).sendToTarget();
			mWantedState = STATE_DISCONNECT;
			break;
		case MSG_DISCONNECTED:
			mHandler.obtainMessage(msg.arg1, -1, -1).sendToTarget();
			mCurrentState = STATE_DISCONNECT;
			mHD.savePersistentData();
			if (mAutoReconnect) {
				final int lastState = mWantedState;
				HarleyDroidService.this.notify(R.string.notification_autorecon);
				mHandler.obtainMessage(HarleyDroid.STATUS_AUTORECON, -1, -1).sendToTarget();

				doDisconnect();
				mReconThread = new Thread() {
					public void run() {
						setName("HarleyDroidService: reconThread");
						try {
							Thread.sleep(mReconnectDelay * 1000);
							//mServiceHandler.removeCallbacksAndMessages(null);
							mServiceHandler.obtainMessage(MSG_NONE, lastState, -1).sendToTarget();
							// try again to go to mWantedState
						} catch (InterruptedException e) {
						}
					}
				};
				mWantedState = STATE_WAIT_RECONNECT;
				mReconThread.start();
			}
			else {
				stopSelf();
				return;
			}
			break;
		case MSG_START_POLL:
			mWantedState = STATE_POLL;
			break;
		case MSG_STARTED_POLL:
			HarleyDroidService.this.notify(R.string.notification_polling);
			mCurrentState = STATE_POLL;
			break;
		case MSG_START_SEND:
			mWantedState = STATE_SEND;
			mSendType = msg.getData().getStringArray("type");
			mSendTA = msg.getData().getStringArray("ta");
			mSendSA = msg.getData().getStringArray("sa");
			mSendCommand = msg.getData().getStringArray("command");
			mSendExpect = msg.getData().getStringArray("expect");
			mSendTimeout = msg.getData().getIntArray("timeout");
			mSendDelay = msg.getData().getInt("delay");
			break;
		case MSG_STARTED_SEND:
			HarleyDroidService.this.notify(R.string.notification_diagnostics);
			mCurrentState = STATE_SEND;
			break;
		case MSG_SET_SEND:
			mInterface.setSendData(msg.getData().getStringArray("type"),
					msg.getData().getStringArray("ta"),
					msg.getData().getStringArray("sa"),
					msg.getData().getStringArray("command"),
					msg.getData().getStringArray("expect"),
					msg.getData().getIntArray("timeout"),
					msg.getData().getInt("delay"));
		}
		stateMachine();
	}

	public void connected() {
		if (D) Log.d(TAG, "connected()");
		//mServiceHandler.removeCallbacksAndMessages(null);
		mServiceHandler.obtainMessage(MSG_CONNECTED, -1, -1).sendToTarget();
	}

	public void disconnected(int error) {
		if (D) Log.d(TAG, "disconnected()" + mCurrentState + " " + mWantedState);
		//mServiceHandler.removeCallbacksAndMessages(null);
		mServiceHandler.obtainMessage(MSG_DISCONNECTED, error, -1).sendToTarget();
	}

	public void disconnect() {
		if (D) Log.d(TAG, "disconnect()" + mCurrentState + " " + mWantedState);
		//mServiceHandler.removeCallbacksAndMessages(null);
		mServiceHandler.obtainMessage(MSG_DISCONNECT, -1, -1).sendToTarget();
	}

	public void startPoll() {
		if (D) Log.d(TAG, "startPoll()");
		//mServiceHandler.removeCallbacksAndMessages(null);
		mServiceHandler.obtainMessage(MSG_START_POLL, -1, -1).sendToTarget();
	}

	public void startedPoll() {
		if (D) Log.d(TAG, "startedPoll()");
		//mServiceHandler.removeCallbacksAndMessages(null);
		mServiceHandler.obtainMessage(MSG_STARTED_POLL, -1, -1).sendToTarget();
	}

	public void startSend(String type[], String ta[], String sa[], String command[], String expect[], int timeout[], int delay) {
		if (D) Log.d(TAG, "send()");
		Message m = mServiceHandler.obtainMessage(MSG_START_SEND);
		Bundle b = new Bundle();
		b.putStringArray("type", type);
		b.putStringArray("ta", ta);
		b.putStringArray("sa", sa);
		b.putStringArray("command", command);
		b.putStringArray("expect", expect);
		b.putIntArray("timeout", timeout);
		b.putInt("delay", delay);
		m.setData(b);
		//mServiceHandler.removeCallbacksAndMessages(null);
		m.sendToTarget();
	}

	public void startedSend() {
		if (D) Log.d(TAG, "sendDone()");
		//mServiceHandler.removeCallbacksAndMessages(null);
		mServiceHandler.obtainMessage(MSG_STARTED_SEND, -1, -1).sendToTarget();
	}

	public void setSendData(String type[], String ta[], String sa[], String command[], String expect[], int timeout[], int delay) {
		if (D) Log.d(TAG, "setSendData()");
		Message m = mServiceHandler.obtainMessage(MSG_SET_SEND);
		Bundle b = new Bundle();
		b.putStringArray("type", type);
		b.putStringArray("ta", ta);
		b.putStringArray("sa", sa);
		b.putStringArray("command", command);
		b.putStringArray("expect", expect);
		b.putIntArray("timeout", timeout);
		b.putInt("delay", delay);
		m.setData(b);
		//mServiceHandler.removeCallbacksAndMessages(null);
		m.sendToTarget();
	}

	public boolean isPolling() {
		if (D) Log.d(TAG, "isPolling()");

		return (mCurrentState == STATE_POLL);
	}

	public boolean isSending() {
		if (D) Log.d(TAG, "isSending()");

		return (mCurrentState == STATE_SEND);
	}

	static class HarleyDroidServiceHandler extends Handler {
		private final WeakReference<HarleyDroidService> mHarleyDroidService;

	    HarleyDroidServiceHandler(HarleyDroidService harleyDroidService) {
	        mHarleyDroidService = new WeakReference<HarleyDroidService>(harleyDroidService);
	    }

		@Override
		public void handleMessage(Message msg) {
			HarleyDroidService hds = mHarleyDroidService.get();
			if (hds != null)
				hds.handleMessage(msg);
		}
	}
}
