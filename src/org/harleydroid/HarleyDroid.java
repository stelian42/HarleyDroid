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

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

public class HarleyDroid extends Activity implements ServiceConnection, Eula.OnEulaAgreedTo
{
	private static final boolean D = true;
	private static final boolean DTRACE = false;
	private static final String TAG = HarleyDroid.class.getSimpleName();
	public static final boolean EMULATOR = true;

	// Message types sent from HarleyDroidService
	public static final int STATUS_OK = 0;
	public static final int STATUS_CONNECTING = 1;
	public static final int STATUS_CONNECTED = 2;
	public static final int STATUS_ERROR = 3;
	public static final int STATUS_ERRORAT = 4;
	public static final int STATUS_NODATA = 5;
	public static final int STATUS_TOOMANYERRORS = 6;
	public static final int STATUS_AUTORECON = 7;

	private static final int REQUEST_ENABLE_BT = 2;

	private SharedPreferences mPrefs;
	private String mInterfaceType = null;
	private BluetoothAdapter mBluetoothAdapter = null;
	private String mBluetoothID = null;
	private boolean mAutoConnect = false;
	private boolean mAutoReconnect = false;
	private String mReconnectDelay;
	private boolean mLogging = false;
	private boolean mGPS = false;
	private boolean mLogRaw = false;
	private HarleyDroidService mService = null;
	private int mViewMode = HarleyDroidView.VIEW_GRAPHIC;
	private boolean mUnitMetric = false;
	private int mOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
	private HarleyDroidView mHarleyDroidView;
	private HarleyData mHD;
	private HarleyDiagnostics mDiag = null;
	private boolean startPoll = true;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		if (D) Log.d(TAG, "onCreate()");
		super.onCreate(savedInstanceState);

		if (DTRACE) Debug.startMethodTracing("harleydroid");

		mHarleyDroidView = new HarleyDroidView(this);
		mHarleyDroidView.changeView(mViewMode, getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT, mUnitMetric);
		mPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
			
		mAutoConnect = true;

		if (Eula.show(this, false))
			onEulaAgreedTo();
	}

	@Override
	public void onEulaAgreedTo() {
		if (!EMULATOR) {
			mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
			if (mBluetoothAdapter == null) {
				Toast.makeText(this, R.string.toast_nobluetooth, Toast.LENGTH_LONG).show();
				finish();
				return;
			}
			if (!mBluetoothAdapter.isEnabled()) {
				Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
			}
		}
	}

	@Override
	public void onDestroy() {
		if (D) Log.d(TAG, "onDestroy()");
		super.onDestroy();

		if (DTRACE) Debug.stopMethodTracing();
	}

	@Override
	public void onStart() {
		if (D) Log.d(TAG, "onStart()");
		super.onStart();

		// get preferences which may have been changed
		mInterfaceType = mPrefs.getString("interfacetype", null);
		mBluetoothID = mPrefs.getString("bluetoothid", null);
		mAutoConnect = mAutoConnect && mPrefs.getBoolean("autoconnect", false);
		mAutoReconnect = mPrefs.getBoolean("autoreconnect", false);
		mReconnectDelay = mPrefs.getString("reconnectdelay", "30");
		if (mPrefs.getString("orientation", "auto").equals("auto"))
			mOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
		else if (mPrefs.getString("orientation", "auto").equals("portrait")) {
			mOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
		}
		else if (mPrefs.getString("orientation", "auto").equals("landscape")) {
			mOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
		}
		this.setRequestedOrientation(mOrientation);
		mLogging = false;
		if (mPrefs.getBoolean("logging", false)) {
			if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))
				Toast.makeText(this, R.string.toast_errorlogging, Toast.LENGTH_LONG).show();
			else
				mLogging = true;
		}
		mGPS = mPrefs.getBoolean("gps", false);
		mLogRaw = mPrefs.getBoolean("lograw", false);
		if (mPrefs.getBoolean("screenon", false))
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		if (mPrefs.getString("unit", "metric").equals("metric"))
			mUnitMetric = true;
		else
			mUnitMetric = false;

		mViewMode = mPrefs.getInt("viewmode", HarleyDroidView.VIEW_GRAPHIC);
		mHarleyDroidView.changeView(mViewMode, 
				mOrientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED ? getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT
																			: mOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT ? true : false,
				mUnitMetric);
		mHarleyDroidView.drawAll(mHD);

		// bind to the service
		bindService(new Intent(this, HarleyDroidService.class), this, 0);

		if (mAutoConnect && mService == null) {
			mAutoConnect = false;
			startPoll();
		}
		if (mViewMode == HarleyDroidView.VIEW_DIAGNOSTIC)
			startDiag();
	}

	@Override
	public void onStop() {
		if (D) Log.d(TAG, "onStop()");
		super.onStop();

		stopDiag();
		if (mService != null && !mService.isPolling()) {
			unbindService(this);
			stopService(new Intent(this, HarleyDroidService.class));
		}
		else
			unbindService(this);
		mService = null;

		SharedPreferences.Editor editor = mPrefs.edit();
		editor.putInt("viewmode", mViewMode);
		editor.commit();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		if (D) Log.d(TAG, "onConfigurationChanged()");
		super.onConfigurationChanged(newConfig);

		if (mOrientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
			Log.d(TAG, "orientation now " + newConfig.orientation);
			mHarleyDroidView.changeView(mViewMode, newConfig.orientation == Configuration.ORIENTATION_PORTRAIT, mUnitMetric);
			mHarleyDroidView.drawAll(mHD);
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (D) Log.d(TAG, "onPrepareOptionsMenu()");

		menu.clear();
		MenuInflater inflater = getMenuInflater();

		if (mViewMode == HarleyDroidView.VIEW_DIAGNOSTIC) {
			inflater.inflate(R.menu.diagnostics_menu, menu);
		}
		else {
			inflater.inflate(R.menu.main_menu, menu);

			menu.findItem(R.id.capture_menu).setEnabled(
				(mBluetoothID == null) ? false : true);

			if (mService != null && mService.isPolling()) {
				menu.findItem(R.id.capture_menu).setIcon(R.drawable.ic_menu_stop);
				menu.findItem(R.id.capture_menu).setTitle(R.string.stopcapture_label);
				menu.findItem(R.id.diag_menu).setEnabled(false);
			}
			else {
				menu.findItem(R.id.capture_menu).setIcon(R.drawable.ic_menu_play_clip);
				menu.findItem(R.id.capture_menu).setTitle(R.string.startcapture_label);
				menu.findItem(R.id.diag_menu).setEnabled(true);
			}
			if (mViewMode == HarleyDroidView.VIEW_GRAPHIC)
				menu.findItem(R.id.mode_menu).setTitle(R.string.mode_labelraw);
			else
				menu.findItem(R.id.mode_menu).setTitle(R.string.mode_labelgr);
		}

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (D) Log.d(TAG, "onOptionsItemSelected");

		switch (item.getItemId()) {
		case R.id.capture_menu:
			if (mService != null && mService.isPolling())
				stopPoll();
			else
				startPoll();
			return true;
		case R.id.mode_menu:
			if (mViewMode == HarleyDroidView.VIEW_GRAPHIC)
				mViewMode = HarleyDroidView.VIEW_TEXT;
			else
				mViewMode = HarleyDroidView.VIEW_GRAPHIC;
			mHarleyDroidView.changeView(mViewMode, getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT, mUnitMetric);
			mHarleyDroidView.drawAll(mHD);
			return true;
		case R.id.diag_menu:
			mViewMode = HarleyDroidView.VIEW_DIAGNOSTIC;
			mHarleyDroidView.changeView(mViewMode, getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT, mUnitMetric);
			mHarleyDroidView.drawAll(mHD);
			stopPoll();
			startDiag();
			return true;
		case R.id.preferences_menu:
			Intent settingsActivity = new Intent(getBaseContext(), HarleyDroidSettings.class);
			startActivity(settingsActivity);
			return true;
		case R.id.reset_menu:
			if (mHD != null)
				mHD.resetCounters();
			return true;
		case R.id.about_menu:
			About.about(this);
			return true;
		case R.id.cleardtc_menu:
			mHD.resetHistoricDTC();
			mDiag.clearDTC();
			return true;
		case R.id.exitdiag_menu:
			mViewMode = HarleyDroidView.VIEW_GRAPHIC;
			mHarleyDroidView.changeView(mViewMode, getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT, mUnitMetric);
			mHarleyDroidView.drawAll(mHD);
			stopDiag();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (D) Log.d(TAG, "onActivityResult");

		switch (requestCode) {
		case REQUEST_ENABLE_BT:
			if (resultCode != Activity.RESULT_OK) {
				Toast.makeText(this, R.string.toast_errorenablebluetooth, Toast.LENGTH_LONG).show();
				finish();
			}
		}
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		if (D) Log.d(TAG, "onServiceConnected()");

		mService = ((HarleyDroidService.HarleyDroidServiceBinder)service).getService();
		mService.setHandler(mHandler);
		mHD = mService.getHarleyData();
		mHD.addHarleyDataListener(mHarleyDroidView);
		mHarleyDroidView.drawAll(mHD);

		if (mService.isConnected())
			return;

		if (!EMULATOR) {
			// should not happen because capture menu is disabled, but the
			// error was somehow reproduced by users.
			if (mBluetoothID == null)
				return;
			Log.e(TAG, "**** " + mBluetoothAdapter.getRemoteDevice(mBluetoothID) + " ****");
			mService.setInterfaceType(mInterfaceType,
									  mBluetoothAdapter.getRemoteDevice(mBluetoothID));
		}
		else
			mService.setInterfaceType(mInterfaceType, null);

		mService.setLogging(mLogging, mUnitMetric, mGPS, mLogRaw);
		mService.setAutoReconnect(mAutoReconnect, Integer.parseInt(mReconnectDelay));

		if (startPoll)
			startPoll();
		else
			startDiag();
	}

	private void startPoll() {
		if (D) Log.d(TAG, "startPoll()");

		if (mService == null) {
			startPoll = true;
			startService(new Intent(this, HarleyDroidService.class));
			bindService(new Intent(this, HarleyDroidService.class), this, 0);
		}
		else
			mService.startPoll();
	}

	private void stopPoll() {
		if (D) Log.d(TAG, "stopCapture()");

		if (mService == null)
			return;
		mService.stopPoll();
	}

	private void startDiag() {
		if (D) Log.d(TAG, "startDiag()");

		if (mService == null) {
			startPoll = false;
			startService(new Intent(this, HarleyDroidService.class));
			bindService(new Intent(this, HarleyDroidService.class), this, 0);
		}
		else {
			stopDiag();
			mDiag = new HarleyDiagnostics(mService);
			mDiag.start();
		}
	}

	private void stopDiag() {
		if (D) Log.d(TAG, "stopDiag()");

		if (mDiag != null)
			mDiag.cancel();
		mDiag = null;
	}


	@Override
	public void onServiceDisconnected(ComponentName name) {
		if (D) Log.d(TAG, "onServiceDisconnected()");

		unbindService(this);
		mService = null;
		// ugly, but we unbind() in onStop()...
		bindService(new Intent(this, HarleyDroidService.class), this, 0);
	}

	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (D) Log.d(TAG, "handleMessage " + msg.what);

			switch (msg.what) {
			case STATUS_CONNECTING:
				Toast.makeText(getApplicationContext(), R.string.toast_connecting, Toast.LENGTH_LONG).show();
				break;
			case STATUS_ERROR:
				Toast.makeText(getApplicationContext(), R.string.toast_errorconnecting, Toast.LENGTH_LONG).show();
				break;
			case STATUS_ERRORAT:
				Toast.makeText(getApplicationContext(), R.string.toast_errorat, Toast.LENGTH_LONG).show();
				break;
			case STATUS_CONNECTED:
				Toast.makeText(getApplicationContext(), R.string.toast_connected, Toast.LENGTH_LONG).show();
				break;
			case STATUS_NODATA:
				Toast.makeText(getApplicationContext(), R.string.toast_nodata, Toast.LENGTH_LONG).show();
				break;
			case STATUS_TOOMANYERRORS:
				Toast.makeText(getApplicationContext(), R.string.toast_toomanyerrors, Toast.LENGTH_LONG).show();
				break;
			case STATUS_AUTORECON:
				Toast.makeText(getApplicationContext(), String.format(getText(R.string.toast_autorecon).toString(), mReconnectDelay), Toast.LENGTH_LONG).show();
				break;
			}
		}
	};
}
