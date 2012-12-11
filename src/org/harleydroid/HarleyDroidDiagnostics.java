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

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class HarleyDroidDiagnostics extends HarleyDroid
{
	private static final boolean D = false;
	private static final String TAG = HarleyDroidDiagnostics.class.getSimpleName();

	private HarleyDroidDiagnosticsView mHarleyDroidDiagnosticsView;
	private Handler mHandler = new Handler();

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		if (D) Log.d(TAG, "onCreate()");
		super.onCreate(savedInstanceState);

		mHarleyDroidDiagnosticsView = new HarleyDroidDiagnosticsView(this);
		mHarleyDroidDiagnosticsView.changeView(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);
	}

	@Override
	public void onStart() {
		if (D) Log.d(TAG, "onStart()");
		super.onStart();

		mHarleyDroidDiagnosticsView.changeView(
				mOrientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED ? getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT
																			: mOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT ? true : false);
		mHarleyDroidDiagnosticsView.drawAll(mHD);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		if (D) Log.d(TAG, "onConfigurationChanged()");
		super.onConfigurationChanged(newConfig);

		if (mOrientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
			mHarleyDroidDiagnosticsView.changeView(newConfig.orientation == Configuration.ORIENTATION_PORTRAIT);
			mHarleyDroidDiagnosticsView.drawAll(mHD);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (D) Log.d(TAG, "onCreateOptionsMenu()");
		super.onCreateOptionsMenu(menu);

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.diagnostics_menu, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (D) Log.d(TAG, "onPrepareOptionsMenu()");
		super.onPrepareOptionsMenu(menu);

		menu.findItem(R.id.startstop_menu).setEnabled(
			(mBluetoothID == null) ? false : true);

		if (mService != null) {
			menu.findItem(R.id.startstop_menu).setIcon(R.drawable.ic_menu_stop);
			menu.findItem(R.id.startstop_menu).setTitle(R.string.disconnect_label);
			menu.findItem(R.id.cleardtc_menu).setEnabled(true);
		}
		else {
			menu.findItem(R.id.startstop_menu).setIcon(R.drawable.ic_menu_play_clip);
			menu.findItem(R.id.startstop_menu).setTitle(R.string.connect_label);
			menu.findItem(R.id.cleardtc_menu).setEnabled(false);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (D) Log.d(TAG, "onOptionsItemSelected");
		super.onOptionsItemSelected(item);

		switch (item.getItemId()) {
		case R.id.startstop_menu:
			if (mService == null)
				startHDS();
			else
				stopHDS();
			return true;
		case R.id.cleardtc_menu:
			mHD.resetHistoricDTC();
			clearDTC();
			return true;
		case R.id.dash_menu:
			Intent dashboardActivity = new Intent(getBaseContext(), HarleyDroidDashboard.class);
			startActivity(dashboardActivity);
			return true;
		case R.id.preferences_menu:
			Intent settingsActivity = new Intent(getBaseContext(), HarleyDroidSettings.class);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				settingsActivity.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, HarleyDroidSettings.Fragment.class.getName());
				settingsActivity.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
			}
			startActivity(settingsActivity);
			return true;
		case R.id.about_menu:
			About.about(this);
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public void onServiceConnected(ComponentName name, IBinder service) {
		if (D) Log.d(TAG, "onServiceConnected()");
		super.onServiceConnected(name, service);

		if (!mService.isSending())
			mService.startSend(types, tas, sas, commands, expects, timeouts, COMMAND_DELAY);

		mHD.addHarleyDataDiagnosticsListener(mHarleyDroidDiagnosticsView);
		mHarleyDroidDiagnosticsView.drawAll(mHD);
	}

	public void onServiceDisconnected(ComponentName name) {
		if (D) Log.d(TAG, "onServiceDisconnected()");

		mHD.removeHarleyDataDiagnosticsListener(mHarleyDroidDiagnosticsView);
		mHarleyDroidDiagnosticsView.drawAll(null);
		super.onServiceDisconnected(name);
	}

	private static final int COMMAND_TIMEOUT = 500;
	private static final int GET_DTC_TIMEOUT = 2000;
	private static final int CLEAR_DTC_TIMEOUT = 500;

	private static final int COMMAND_DELAY = 10000;
	private static final int CLEAR_DTC_DELAY = 2000;

	public void clearDTC() {
		if (D) Log.d(TAG, "clearDTC()");

		String[] cTypes =		{ "6C", "6C", "6C" };
		String[] cTas =			{ "10", "40", "60" };
		String[] cSas =			{ "F1", "F1", "F1" };
		String[] cCommands =	{ "14", "14", "14" };
		String[] cExpects =		{ "6CF11054", "6CF14054", "6CF16054" };
		int[] cCommandTimeout =	{ CLEAR_DTC_TIMEOUT, CLEAR_DTC_TIMEOUT, CLEAR_DTC_TIMEOUT };

		if (mService != null)
			mService.setSendData(cTypes, cTas, cSas, cCommands, cExpects, cCommandTimeout, COMMAND_DELAY);

		mHandler.postDelayed(mRestartTask, CLEAR_DTC_DELAY);
	}

	private Runnable mRestartTask = new Runnable() {
		public void run() {
			mHD.resetHistoricDTC();
			if (mService != null)
				mService.setSendData(types, tas, sas, commands, expects, timeouts, COMMAND_DELAY);
		}
	};

	private String[] types = {
		// Get VIN, ECM info etc...
		"0C",
		"0C",
		"0C",
		"0C",
		"0C",
		"0C",
		"0C",
		"0C",
		// Get DTC
		"6C",
		"6C",
		"6C",
	};

	private String[] tas = {
		// Get VIN, ECM info etc...
		"10",
		"10",
		"10",
		"10",
		"10",
		"10",
		"10",
		"10",
		// Get DTC
		"10",
		"40",
		"60",
	};

	private String[] sas = {
		// Get VIN, ECM info etc...
		"F1",
		"F1",
		"F1",
		"F1",
		"F1",
		"F1",
		"F1",
		"F1",
		// Get DTC
		"F1",
		"F1",
		"F1",
	};

	private String[] commands = {
		// Get VIN, ECM info etc...
		"3C01",
		"3C02",
		"3C03",
		"3C04",
		"3C0B",
		"3C0F",
		"3C10",
		"3C11",
		// Get DTC
		"1952FF00",
		"1952FF00",
		"1952FF00",
	};

	private String[] expects = {
		// Get VIN, ECM info etc...
		"0CF1107C01",
		"0CF1107C02",
		"0CF1107C03",
		"0CF1107C04",
		"0CF1107C0B",
		"0CF1107C0F",
		"0CF1107C10",
		"0CF1107C11",
		// Get DTC
		"6CF11059",
		"6CF14059",
		"6CF16059",
	};

	private int[] timeouts = {
		// Get VIN, ECM info etc...
		COMMAND_TIMEOUT,
		COMMAND_TIMEOUT,
		COMMAND_TIMEOUT,
		COMMAND_TIMEOUT,
		COMMAND_TIMEOUT,
		COMMAND_TIMEOUT,
		COMMAND_TIMEOUT,
		COMMAND_TIMEOUT,
		// Get DTC
		GET_DTC_TIMEOUT,
		GET_DTC_TIMEOUT,
		GET_DTC_TIMEOUT,
	};
}
