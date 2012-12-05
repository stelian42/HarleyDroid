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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.zip.GZIPOutputStream;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

public class HarleyDroidLogger implements HarleyDataDashboardListener, HarleyDataDiagnosticsListener, HarleyDataRawListener
{
	private static final boolean D = false;
	private static final String TAG = HarleyDroidLogger.class.getSimpleName();

	static final SimpleDateFormat TIMESTAMP_FORMAT =
		new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.US);
	static {
		TIMESTAMP_FORMAT.setTimeZone(TimeZone.getDefault());
	}

	private HarleyDroidGPS mGPS = null;
	private BufferedOutputStream mLog = null;
	private boolean mUnitMetric = false;
	private boolean mLogRaw = false;
	private boolean mLogUnknown = false;

	public HarleyDroidLogger(Context context, boolean metric, boolean gps, boolean logRaw, boolean logUnknown) {
		mUnitMetric = metric;
		mLogRaw = logRaw;
		mLogUnknown = logUnknown;
		if (gps)
			mGPS = new HarleyDroidGPS(context);
	}

	public void start() {
		if (D) Log.d(TAG, "start()");

		if (mGPS != null)
			mGPS.start();

		try {
			File path = new File(Environment.getExternalStorageDirectory(), "/Android/data/org.harleydroid/files/");
			path.mkdirs();
			File logFile = new File(path, "harley-" + TIMESTAMP_FORMAT.format(new Date()) + ".log.gz");
			mLog = new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(logFile, false)));
		} catch (IOException e) {
			Log.d(TAG, "Logfile open " + e);
		}
	}

	static byte[] myGetBytes(String s) {
		byte[] result = new byte[s.length()];
		for (int i = 0; i < s.length(); i++) {
			result[i] = (byte) s.charAt(i);
		}
		return result;
	}

	public void write(String header, byte[] data) {
		if (D) Log.d(TAG, "write()");

		if (mLog != null) {
			try {
				mLog.write(myGetBytes(TIMESTAMP_FORMAT.format(new Date())));
				mLog.write(',');
				mLog.write(myGetBytes(header));
				if (data != null)
					mLog.write(data);
				mLog.write(',');
				if (mGPS != null)
					mLog.write(myGetBytes(mGPS.getLocation()));
				else {
					mLog.write(',');
					mLog.write(',');
				}
				mLog.write('\n');
			} catch (IOException e) {
			}
		}
	}

	public void write(String header, String data) {
		write(header, myGetBytes(data));
	}

	public void write(String data) {
		write(data, (byte[]) null);
	}


	public void stop() {
		if (D) Log.d(TAG, "stop()");

		if (mGPS != null) {
			mGPS.stop();
			mGPS = null;
		}
		if (mLog != null) {
			try {
				mLog.close();
			} catch (IOException e) {
			}
			mLog = null;
		}
	}

	public void onRPMChanged(int rpm) {
		write("RPM," + rpm);
	}

	public void onSpeedImperialChanged(int speed) {
		if (!mUnitMetric)
			write("SPD," + speed);
	}

	public void onSpeedMetricChanged(int speed) {
		if (mUnitMetric)
			write("SPD," + speed);
	}

	public void onEngineTempImperialChanged(int engineTemp) {
		if (!mUnitMetric)
			write("ETP," + engineTemp);
	}

	public void onEngineTempMetricChanged(int engineTemp) {
		if (mUnitMetric)
			write("ETP," + engineTemp);
	}

	public void onFuelGaugeChanged(int full, boolean low) {
		if (low)
			write("FGE,EMPTY");
		else
			write("FGE," + full);
	}

	public void onTurnSignalsChanged(int turnSignals) {
		if ((turnSignals & 0x03) == 0x03)
			write("TRN,W");
		else if ((turnSignals & 0x01) == 0x01)
			write("TRN,R");
		else if ((turnSignals & 0x02) == 0x02)
			write("TRN,L");
		else
			write("TRN,");
	}

	public void onNeutralChanged(boolean neutral) {
		write("NTR," + (neutral ? "1" : "0"));
	}

	public void onClutchChanged(boolean clutch) {
		write("CLU," + (clutch ? "1" : "0"));
	}

	public void onGearChanged(int gear) {
		write("GER," + gear);
	}

	public void onCheckEngineChanged(boolean checkEngine) {
		write("CHK," + (checkEngine ? "1" : "0"));
	}

	public void onOdometerImperialChanged(int odometer) {
		if (!mUnitMetric)
			write("ODO," + odometer);
	}

	public void onOdometerMetricChanged(int odometer) {
		if (mUnitMetric)
			write("ODO," + odometer);
	}

	public void onFuelImperialChanged(int fuel) {
		if (!mUnitMetric)
			write("FUL," + fuel);
	}

	public void onFuelMetricChanged(int fuel) {
		if (mUnitMetric)
			write("FUL," + fuel);
	}

	public void onFuelAverageImperialChanged(int fuel) {
	}

	public void onFuelAverageMetricChanged(int fuel) {
	}

	public void onFuelInstantImperialChanged(int fuel) {
	}

	public void onFuelInstantMetricChanged(int fuel) {
	}

	public void onVINChanged(String vin) {
		write("VIN," + vin);
	}

	public void onECMPNChanged(String ecmPN) {
		write("EPN," + ecmPN);
	}

	public void onECMCalIDChanged(String ecmCalID) {
		write("ECI," + ecmCalID);
	}

	public void onECMSWLevelChanged(int ecmSWLevel) {
		write("ESL," + ecmSWLevel);
	}

	public void onHistoricDTCChanged(String[] dtc) {
		String data = "";
		if (dtc.length > 0)
			data = dtc[0];
		for (int i = 1; i < dtc.length; i++)
			data += "," + dtc[i];
		write("DTH,", data);
	}

	public void onCurrentDTCChanged(String[] dtc) {
		String data = "";
		if (dtc.length > 0)
			data = dtc[0];
		for (int i = 1; i < dtc.length; i++)
			data += "," + dtc[i];
		write("DTC,", data);
	}

	public void onBadCRCChanged(byte[] buffer) {
		write("CRC,", buffer);
	}

	public void onUnknownChanged(byte[] buffer) {
		if (mLogUnknown)
			write("UNK,", buffer);
	}

	public void onRawChanged(byte[] buffer) {
		if (mLogRaw)
			write("RAW,", buffer);
	}
}
