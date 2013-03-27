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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;

public class HarleyDroidSettings extends PreferenceActivity implements OnSharedPreferenceChangeListener {

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			addPreferencesFromResource(R.xml.preferences);
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onResume() {
	    super.onResume();

	    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
	    	fillBluetoothTable((ListPreference) findPreference("bluetoothid"));
			SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();
	    	prefs.registerOnSharedPreferenceChangeListener(this);
	    	onSharedPreferenceChanged(prefs, "interfacetype");
	    	onSharedPreferenceChanged(prefs, "bluetoothid");
	    	onSharedPreferenceChanged(prefs, "reconnectdelay");
	    	onSharedPreferenceChanged(prefs, "unit");
	    	onSharedPreferenceChanged(prefs, "orientation");
	    }
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onPause() {
	    super.onPause();
	    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
	    	getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}

	private static void fillBluetoothTable(ListPreference btlist) {
		BluetoothAdapter bluetoothAdapter = null;
		ArrayList<CharSequence> bluetoothDevices = new ArrayList<CharSequence>();;
		ArrayList<CharSequence> bluetoothAddresses = new ArrayList<CharSequence>();

		if (!HarleyDroid.EMULATOR) {
			bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
			Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
			for (BluetoothDevice dev : pairedDevices) {
				bluetoothDevices.add(dev.getAddress() + " - " + dev.getName());
				bluetoothAddresses.add(dev.getAddress());
			}
		}
		else {
			bluetoothDevices.add("0:0:0:0 - Zerro");
			bluetoothAddresses.add("0:0:0:0");
			bluetoothDevices.add("1:1:1:1 - One");
			bluetoothAddresses.add("1:1:1:1");
			bluetoothDevices.add("2:2:2:2 - Two");
			bluetoothAddresses.add("2:2:2:2");
			bluetoothDevices.add("3:3:3:3 - Three");
			bluetoothAddresses.add("3:3:3:3");
			bluetoothDevices.add("4:4:4:4 - Four");
			bluetoothAddresses.add("4:4:4:4");
		}
		btlist.setEntryValues(bluetoothAddresses.toArray(new CharSequence[0]));
		btlist.setEntries(bluetoothDevices.toArray(new CharSequence[0]));
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public void onBuildHeaders(List<Header> target) {
	   loadHeadersFromResource(R.xml.preferences_headers, target);
	}

	@SuppressWarnings("deprecation")
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals("interfacetype")) {
			ListPreference list = (ListPreference) findPreference(key);
			list.setSummary(list.getEntry());
		}
		else if (key.equals("bluetoothid")) {
			ListPreference list = (ListPreference) findPreference(key);
			list.setSummary(list.getEntry());
		}
		else if (key.equals("reconnectdelay")) {
			EditTextPreference edit = (EditTextPreference) findPreference(key);
			edit.setSummary(edit.getText() + " " + getText(R.string.pref_seconds));
		}
		else if (key.equals("unit")) {
			ListPreference list = (ListPreference) findPreference(key);
			list.setSummary(list.getEntry());
		}
		else if (key.equals("orientation")) {
			ListPreference list = (ListPreference) findPreference(key);
			list.setSummary(list.getEntry());
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static class Fragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {
		 @Override
		 public void onCreate(Bundle savedInstanceState) {
			 super.onCreate(savedInstanceState);

			 addPreferencesFromResource(R.xml.preferences);
		 }

		 @Override
		 public void onResume() {
			 super.onResume();
			 fillBluetoothTable((ListPreference) findPreference("bluetoothid"));
			 SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();
			 prefs.registerOnSharedPreferenceChangeListener(this);
			 onSharedPreferenceChanged(prefs, "interfacetype");
			 onSharedPreferenceChanged(prefs, "bluetoothid");
			 onSharedPreferenceChanged(prefs, "reconnectdelay");
			 onSharedPreferenceChanged(prefs, "unit");
			 onSharedPreferenceChanged(prefs, "orientation");
		}

		@Override
		public void onPause() {
			super.onPause();
			getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
		}

		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			if (key.equals("interfacetype")) {
				ListPreference list = (ListPreference) findPreference(key);
				list.setSummary(list.getEntry());
			}
			else if (key.equals("bluetoothid")) {
				ListPreference list = (ListPreference) findPreference(key);
				list.setSummary(list.getEntry());
			}
			else if (key.equals("reconnectdelay")) {
				EditTextPreference edit = (EditTextPreference) findPreference(key);
				edit.setSummary(edit.getText() + " " + getText(R.string.pref_seconds));
			}
			else if (key.equals("unit")) {
				ListPreference list = (ListPreference) findPreference(key);
				list.setSummary(list.getEntry());
			}
			else if (key.equals("orientation")) {
				ListPreference list = (ListPreference) findPreference(key);
				list.setSummary(list.getEntry());
			}
		}
	}

}
