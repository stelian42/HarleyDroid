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

import android.util.Log;

public class HarleyDiagnostics
{
	private static final boolean D = true;
	private static final String TAG = HarleyDiagnostics.class.getSimpleName();

	private HarleyDroidService mService;
	private DiagnosticsThread dThread = null;

	public HarleyDiagnostics(HarleyDroidService service) {
		mService = service;
	}

	public void start() {
		if (D) Log.d(TAG, "start()");
		dThread = new DiagnosticsThread(false);
		dThread.start();
	}

	public void cancel() {
		if (D) Log.d(TAG, "cancel()");
		if (dThread != null) {
			dThread.cancel();
			dThread = null;
		}
	}

	public void clearDTC() {
		if (D) Log.d(TAG, "clearDTC()");
		cancel();
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
			}

			while (!stop) {
				for (int i = 0; !stop && i < commands.length; i++) {
					if (D) Log.d(TAG, commands[i] + " - " + expects[i]);
					mService.send(types[i], tas[i], sas[i], commands[i], expects[i]);
					/* answer should come in 20 ms or so */
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
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
