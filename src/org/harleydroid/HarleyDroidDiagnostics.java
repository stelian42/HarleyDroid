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
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class HarleyDroidDiagnostics extends HarleyDroid
{
	private static final boolean D = false;
	private static final String TAG = HarleyDroidDiagnostics.class.getSimpleName();

	private HarleyDroidDiagnosticsView mHarleyDroidDiagnosticsView;

	private DiagnosticsThread dThread = null;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		if (D) Log.d(TAG, "onCreate()");
		super.onCreate(savedInstanceState);

		mHarleyDroidDiagnosticsView = new HarleyDroidDiagnosticsView(this);
		mHarleyDroidDiagnosticsView.changeView(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);
	}

	@Override
	public void onStop()
	{
		if (D) Log.d(TAG, "onStop()");
		super.onStop();

		if (dThread != null) {
			dThread.cancel();
			dThread = null;
		}
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

		if (dThread != null)
			dThread.cancel();
		dThread = new DiagnosticsThread(false);
		dThread.start();

		mHD.addHarleyDataDiagnosticsListener(mHarleyDroidDiagnosticsView);
		mHarleyDroidDiagnosticsView.drawAll(mHD);
	}

	public void onServiceDisconnected(ComponentName name) {
		if (D) Log.d(TAG, "onServiceDisconnected()");
		super.onServiceDisconnected(name);

		if (dThread != null) {
			dThread.cancel();
			dThread = null;
		}

		mHD.removeHarleyDataDiagnosticsListener(mHarleyDroidDiagnosticsView);
	}

	public void clearDTC() {
		if (D) Log.d(TAG, "clearDTC()");

		if (dThread != null)
			dThread.cancel();
		dThread = new DiagnosticsThread(true);
		dThread.start();
	}

	private class DiagnosticsThread extends Thread {
		private boolean clearDTC = false;
		private boolean stop = false;

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
				"0C",
				"0C",
				// Get DTC
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
				//"10",
				//"10",
				// Get DTC
				"FE",
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
				//"F1",
				//"F1",
				// Get DTC
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
				//"3C12", unknown
				//"3C19", unknown
				// Get DTC
				"1912FF00",
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
				//"0CF1107C12",
				//"0CF1107C19",
				// Get DTC
				"6CF11059"
		};

		public DiagnosticsThread(boolean cDTC) {
			setName("HarleyDroidDiagnostics");
			clearDTC = cDTC;
		}

		public void cancel() {
			if (D) Log.d(TAG, "cancel()");

			stop = true;
		}

		public void run() {
			if (D) Log.d(TAG, "run(clearDTC=" + clearDTC + ")");

			if (clearDTC) {
				if (D) Log.d(TAG, "6C10F114 - ???");
				mService.send("6C", "10", "F1", "14", "???");

				while (!stop && mService.isBusy()) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
					}
				}
			}

			while (!stop) {
				for (int i = 0; !stop && i < commands.length; i++) {
					if (D) Log.d(TAG, commands[i] + " - " + expects[i]);
					mService.send(types[i], tas[i], sas[i], commands[i], expects[i]);
					/* answer should come in 20 ms or so */
					while (!stop && mService.isBusy()) {
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
						}
					}
				}
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
				}
			}
		}
	}
}
