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

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class HarleyDroidDashboard extends HarleyDroid {
	private static final boolean D = false;
	private static final String TAG = HarleyDroidDashboard.class.getSimpleName();

	private HarleyDroidDashboardView mHarleyDroidDashboardView;
	private int mViewMode = HarleyDroidDashboardView.VIEW_GRAPHIC;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		if (D) Log.d(TAG, "onCreate()");
		super.onCreate(savedInstanceState);

		mHarleyDroidDashboardView = new HarleyDroidDashboardView(this);
		mHarleyDroidDashboardView.changeView(mViewMode, getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT, mUnitMetric);
	}

	@Override
	public void onStart() {
		if (D) Log.d(TAG, "onStart()");
		super.onStart();

		mViewMode = mPrefs.getInt("dashboardviewmode", HarleyDroidDashboardView.VIEW_GRAPHIC);
		mHarleyDroidDashboardView.changeView(mViewMode,
				mOrientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED ? getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT
																			: mOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT ? true : false,
				mUnitMetric);
		mHarleyDroidDashboardView.drawAll(mHD);
	}

	@Override
	public void onStop() {
		if (D) Log.d(TAG, "onStop()");
		super.onStop();

		SharedPreferences.Editor editor = mPrefs.edit();
		editor.putInt("dashboardviewmode", mViewMode);
		editor.commit();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		if (D) Log.d(TAG, "onConfigurationChanged()");
		super.onConfigurationChanged(newConfig);

		if (mOrientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
			mHarleyDroidDashboardView.changeView(mViewMode, newConfig.orientation == Configuration.ORIENTATION_PORTRAIT, mUnitMetric);
			mHarleyDroidDashboardView.drawAll(mHD);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (D) Log.d(TAG, "onCreateOptionsMenu()");
		super.onCreateOptionsMenu(menu);

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.dashboard_menu, menu);
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
		}
		else {
			menu.findItem(R.id.startstop_menu).setIcon(R.drawable.ic_menu_play_clip);
			menu.findItem(R.id.startstop_menu).setTitle(R.string.connect_label);
		}
		if (mViewMode == HarleyDroidDashboardView.VIEW_GRAPHIC)
			menu.findItem(R.id.mode_menu).setTitle(R.string.mode_labelraw);
		else
			menu.findItem(R.id.mode_menu).setTitle(R.string.mode_labelgr);

		return true;
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
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
		case R.id.mode_menu:
			if (mViewMode == HarleyDroidDashboardView.VIEW_GRAPHIC)
				mViewMode = HarleyDroidDashboardView.VIEW_TEXT;
			else
				mViewMode = HarleyDroidDashboardView.VIEW_GRAPHIC;
			mHarleyDroidDashboardView.changeView(mViewMode, getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT, mUnitMetric);
			mHarleyDroidDashboardView.drawAll(mHD);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
				invalidateOptionsMenu();
			return true;
		case R.id.diag_menu:
			Intent diagnosticsActivity = new Intent(getBaseContext(), HarleyDroidDiagnostics.class);
			startActivity(diagnosticsActivity);
			return true;
		case R.id.preferences_menu:
			Intent settingsActivity = new Intent(getBaseContext(), HarleyDroidSettings.class);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				settingsActivity.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, HarleyDroidSettings.Fragment.class.getName());
				settingsActivity.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
			}
			startActivity(settingsActivity);
			return true;
		case R.id.reset_menu:
			if (mHD != null)
				mHD.resetCounters();
			else {
				SharedPreferences.Editor editor = mPrefs.edit();
				editor.putInt("odometer", 0);
				editor.putInt("fuel", 0);
				editor.commit();
			}
			mHarleyDroidDashboardView.drawAll(mHD);
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

		if (!mService.isPolling())
			mService.startPoll();

		mHD.addHarleyDataDashboardListener(mHarleyDroidDashboardView);
		mHarleyDroidDashboardView.drawAll(mHD);
	}

	public void onServiceDisconnected(ComponentName name) {
		if (D) Log.d(TAG, "onServiceDisconnected()");

		mHD.removeHarleyDataDashboardListener(mHarleyDroidDashboardView);
		mHarleyDroidDashboardView.drawAll(null);
		super.onServiceDisconnected(name);
	}
}
