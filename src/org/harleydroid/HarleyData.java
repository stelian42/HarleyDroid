//
// HarleyDroid: Harley Davidson J1850 Data Analyser for Android.
//
// Copyright (C) 2010-2012 Stelian Pop <stelian@popies.net>
// Based on various sources, especially:
//	minigpsd by Tom Zerucha <tz@execpc.com>
//	AVR J1850 VPW Interface by Michael Wolf <webmaster@mictronics.de>
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

import java.util.concurrent.CopyOnWriteArrayList;

import android.content.SharedPreferences;

public class HarleyData {

	// raw values reported in the J1850 stream
	private int mRPM = 0;						// RPM in rotation/minute * 4
	private int mSpeed = 0;						// speed in mph * 200
	private int mEngineTemp = 40;				// engine temperature in Celsius + 40
	private int mFuelGauge = 0;					// fuel gauge: 0 (empty or N/A) to 6 (full)
	private boolean mFuelLow = false;			// fuel low light
	private int mTurnSignals = 0;				// turn signals bitmap: 0x1=right, 0x2=left
	private boolean mNeutral = false;			// boolean: in neutral
	private boolean mClutch = false;			// boolean: clutch engaged
	private int mGear = -1;						// current gear: -1 or 1 to 6
	private boolean mCheckEngine = false;		// boolean: check engine
	private int mOdometer = 0;					// odometer tick (1 tick = 0.4 meters)
	private int mFuel = 0;						// fuel consumption tick (1 tick = 0.000040 liters)
	private String mVIN = "-----------------";	// VIN
	private String mECMPN = "------------";		// ECM Part Number
	private String mECMCalID = "------------";	// ECM Calibration ID
	private int mECMSWLevel = 0;				// ECM Software Level
	private int mFuelAverage = -1;				// average fuel consumption
	private int mFuelInstant = 1;				// instant fuel consumption
	private CopyOnWriteArrayList<String> mHistoricDTC;	// Historic DTC
	private CopyOnWriteArrayList<String> mCurrentDTC;	// Current DTC

	private int mResetOdometer = -1;
	private int mResetFuel = -1;
	private int mSavedOdometer = 0;
	private int mSavedFuel = 0;

	private CopyOnWriteArrayList<HarleyDataDashboardListener> mDashboardListeners;
	private CopyOnWriteArrayList<HarleyDataDiagnosticsListener> mDiagnosticsListeners;
	private CopyOnWriteArrayList<HarleyDataRawListener> mRawListeners;

	private HarleyDataThread mHarleyDataThread;
	private SharedPreferences mPrefs;

	public HarleyData(SharedPreferences prefs) {
		mPrefs = prefs;
		mSavedOdometer = prefs.getInt("odometer", 0);
		mSavedFuel = prefs.getInt("fuel", 0);
		if (mSavedOdometer != 0 && mSavedFuel != 0)
			mFuelAverage = (1250 * mSavedFuel) / mSavedOdometer;

		mDashboardListeners = new CopyOnWriteArrayList<HarleyDataDashboardListener>();
		mDiagnosticsListeners = new CopyOnWriteArrayList<HarleyDataDiagnosticsListener>();
		mRawListeners = new CopyOnWriteArrayList<HarleyDataRawListener>();

		mHistoricDTC = new CopyOnWriteArrayList<String>();
		mCurrentDTC = new CopyOnWriteArrayList<String>();

		mHarleyDataThread = new HarleyDataThread();
		mHarleyDataThread.start();
	}

	public void savePersistentData() {
		if (mResetOdometer >= 0) {
			mSavedOdometer = mSavedOdometer + mOdometer - mResetOdometer;
			mResetOdometer = -1;
		}
		if (mResetFuel >= 0) {
			mSavedFuel = mSavedFuel + mFuel - mResetFuel;
			mResetFuel = -1;
		}
		SharedPreferences.Editor editor = mPrefs.edit();
		editor.putInt("odometer", mSavedOdometer);
		editor.putInt("fuel", mSavedFuel);
		editor.commit();
	}

	public void destroy() {
		savePersistentData();
		mHarleyDataThread.cancel();
		mHarleyDataThread = null;
	}

	public void addHarleyDataDashboardListener(HarleyDataDashboardListener l) {
		mDashboardListeners.add(l);
	}

	public void removeHarleyDataDashboardListener(HarleyDataDashboardListener l) {
		mDashboardListeners.remove(l);
	}

	public void addHarleyDataDiagnosticsListener(HarleyDataDiagnosticsListener l) {
		mDiagnosticsListeners.add(l);
	}

	public void removeHarleyDataDiagnosticsListener(HarleyDataDiagnosticsListener l) {
		mDiagnosticsListeners.remove(l);
	}

	public void addHarleyDataRawListener(HarleyDataRawListener l) {
		mRawListeners.add(l);
	}

	public void removeHarleyDataRawListener(HarleyDataRawListener l) {
		mRawListeners.remove(l);
	}

	// returns the rotations per minute
	public int getRPM() {
		return mRPM / 4;
	}

	public void setRPM(int rpm) {
		if (mRPM != rpm) {
			mRPM = rpm;
			for (HarleyDataDashboardListener l : mDashboardListeners)
				l.onRPMChanged(mRPM / 4);
		}
	}

	// returns the speed in mph
	public int getSpeedImperial() {
		return (mSpeed * 125) / (16 * 1609);
	}

	// returns the speed in km/h
	public int getSpeedMetric() {
		return mSpeed / 128;
	}

	public void setSpeed(int speed) {
		if (mSpeed != speed) {
			mSpeed = speed;
			for (HarleyDataDashboardListener l : mDashboardListeners) {
				l.onSpeedImperialChanged((mSpeed * 125) / (16 * 1609));
				l.onSpeedMetricChanged(mSpeed / 128);
			}
		}
	}

	// returns the temperature in F
	public int getEngineTempImperial() {
		return (mEngineTemp - 40) * 9 / 5 + 32;
	}

	// returns the temperature in C
	public int getEngineTempMetric() {
		return mEngineTemp - 40;
	}

	public void setEngineTemp(int engineTemp) {
		if (mEngineTemp != engineTemp) {
			mEngineTemp = engineTemp;
			for (HarleyDataDashboardListener l : mDashboardListeners) {
				l.onEngineTempImperialChanged((mEngineTemp - 40) * 9 / 5 + 32);
				l.onEngineTempMetricChanged(mEngineTemp - 40);
			}
		}
	}

	// returns the fuel gauge as 0 (empty) to 6 (full)
	public int getFuelGauge() {
		return mFuelGauge;
	}

	// returns the low fuel indicator
	public boolean getFuelLow() {
		return mFuelLow;
	}

	public void setFuelGauge(int fuelGauge, boolean fuelLow) {
		if (mFuelGauge != fuelGauge || mFuelLow != fuelLow) {
			mFuelGauge = fuelGauge;
			mFuelLow = fuelLow;
			for (HarleyDataDashboardListener l : mDashboardListeners)
				l.onFuelGaugeChanged(mFuelGauge, mFuelLow);
		}
	}

	// returns the turn signals bitmap: 0x1=right, 0x2=left
	public int getTurnSignals() {
		return mTurnSignals;
	}

	public void setTurnSignals(int turnSignals) {
		if (mTurnSignals != turnSignals) {
			mTurnSignals = turnSignals;
			for (HarleyDataDashboardListener l : mDashboardListeners)
				l.onTurnSignalsChanged(mTurnSignals);
		}
	}

	// returns the neutral clutch: true = in neutral
	public boolean getNeutral() {
		return mNeutral;
	}

	public void setNeutral(boolean neutral) {
		if (mNeutral != neutral) {
			mNeutral = neutral;
			for (HarleyDataDashboardListener l : mDashboardListeners)
				l.onNeutralChanged(mNeutral);
		}
	}

	// returns the clutch position: true = clutch engaged
	public boolean getClutch() {
		return mClutch;
	}

	public void setClutch(boolean clutch) {
		if (mClutch != clutch) {
			mClutch = clutch;
			for (HarleyDataDashboardListener l : mDashboardListeners)
				l.onClutchChanged(mClutch);
		}
	}

	// returns the current gear: 1 to 6
	public int getGear() {
		return mGear;
	}

	public void setGear(int gear) {
		if (mGear != gear) {
			mGear = gear;
			for (HarleyDataDashboardListener l : mDashboardListeners)
				l.onGearChanged(mGear);
		}
	}

	// returns the check engine light: true = on
	public boolean getCheckEngine() {
		return mCheckEngine;
	}

	public void setCheckEngine(boolean checkEngine) {
		if (mCheckEngine != checkEngine) {
			mCheckEngine = checkEngine;
			for (HarleyDataDashboardListener l : mDashboardListeners)
				l.onCheckEngineChanged(mCheckEngine);
		}
	}

	// returns the odometer in miles * 100
	public int getOdometerImperial() {
		if (mResetOdometer < 0)
			return (mSavedOdometer * 40) / 1609;
		return ((mSavedOdometer + mOdometer - mResetOdometer) * 40) / 1609;
	}

	// returns the odometer in km * 100
	public int getOdometerMetric() {
		if (mResetOdometer < 0)
			return mSavedOdometer / 25;
		return (mSavedOdometer + mOdometer - mResetOdometer) / 25;
	}

	public void setOdometer(int odometer) {
		if (mResetOdometer < 0)
			mResetOdometer = odometer;
		if (mOdometer != odometer) {
			mOdometer = odometer;
			for (HarleyDataDashboardListener l : mDashboardListeners) {
				int o = mSavedOdometer + mOdometer - mResetOdometer;
				l.onOdometerImperialChanged((o * 40) / 1609);
				l.onOdometerMetricChanged(o / 25);
			}
		}
	}

	// returns the fuel in gallons * 1000
	public int getFuelImperial() {
		if (mResetFuel < 0)
			return (mSavedFuel * 264) / 20000;
		return ((mSavedFuel + mFuel - mResetFuel) * 264) / 20000;
	}

	// returns the fuel in milliliters
	public int getFuelMetric() {
		if (mResetFuel < 0)
			return mSavedFuel / 20;
		return (mSavedFuel + mFuel - mResetFuel) / 20;
	}

	public void setFuel(int fuel) {
		if (mResetFuel < 0)
			mResetFuel = fuel;
		if (mFuel != fuel) {
			mFuel = fuel;
			int f = mSavedFuel + mFuel - mResetFuel;
			for (HarleyDataDashboardListener l : mDashboardListeners) {
				l.onFuelImperialChanged((f * 264) / 20000);
				l.onFuelMetricChanged(f / 20);
			}
			/* update average fuel consumption */
			if ((getOdometerMetric() != 0) && (f != 0))
				setFuelAverage(( 50 * f ) / getOdometerMetric());
			else
				setFuelAverage(-1);
		}
	}

	// returns the mileage in MPG * 100 or -1 if not available
	public int getFuelAverageImperial() {
		if (mFuelAverage == -1)
			return -1;
		else
			return 2352146 / mFuelAverage;
	}

	// returns the mileage in l / 100km * 100 or -1 if not available
	public int getFuelAverageMetric() {
		return mFuelAverage;
	}

	private void setFuelAverage(int fuel) {
		if (mFuelAverage != fuel) {
			mFuelAverage = fuel;
			for (HarleyDataDashboardListener l : mDashboardListeners) {
				if (mFuelAverage == -1)
					l.onFuelAverageImperialChanged(-1);
				else
					l.onFuelAverageImperialChanged(2352146 / mFuelAverage);
				l.onFuelAverageMetricChanged(mFuelAverage);
			}
		}
	}

	// returns the instant mileage in MPG * 100 or -1 if not available
	public int getFuelInstantImperial() {
		if (mFuelInstant == -1)
			return -1;
		else
			return 2352146 / mFuelInstant;
	}

	// returns the instant mileage in l / 100km * 100 or -1 if not available
	public int getFuelInstantMetric() {
		return mFuelInstant;
	}

	private void setFuelInstant(int fuel) {
		if (mFuelInstant != fuel) {
			mFuelInstant = fuel;
			for (HarleyDataDashboardListener l : mDashboardListeners) {
				if (mFuelInstant == -1)
					l.onFuelInstantImperialChanged(-1);
				else
					l.onFuelInstantImperialChanged(2352146 / mFuelInstant);
				l.onFuelInstantMetricChanged(mFuelInstant);
			}
		}
	}

	public void resetCounters() {
		mSavedOdometer = 0;
		mSavedFuel = 0;
		mResetOdometer = -1;
		mResetFuel = -1;
		for (HarleyDataDashboardListener l : mDashboardListeners) {
			l.onOdometerImperialChanged(0);
			l.onOdometerMetricChanged(0);
			l.onFuelImperialChanged(0);
			l.onFuelMetricChanged(0);
		}
	}

	public String getVIN() {
		return mVIN;
	}

	public void setVIN(String vin) {
		mVIN = vin;
		for (HarleyDataDiagnosticsListener l : mDiagnosticsListeners)
			l.onVINChanged(mVIN);
	}

	public String getECMPN() {
		return mECMPN;
	}

	public void setECMPN(String ecmPN) {
		mECMPN = ecmPN;
		for (HarleyDataDiagnosticsListener l : mDiagnosticsListeners)
			l.onECMPNChanged(mECMPN);
	}

	public String getECMCalID() {
		return mECMCalID;
	}

	public void setECMCalID(String ecmCalID) {
		mECMCalID = ecmCalID;
		for (HarleyDataDiagnosticsListener l : mDiagnosticsListeners)
			l.onECMCalIDChanged(mECMCalID);
	}

	public int getECMSWLevel() {
		return mECMSWLevel;
	}

	public void setECMSWLevel(int ecmSWLevel) {
		mECMSWLevel = ecmSWLevel;
		for (HarleyDataDiagnosticsListener l : mDiagnosticsListeners)
			l.onECMSWLevelChanged(mECMSWLevel);
	}

	public String[] getHistoricDTC() {
		String[] dtclist = new String[mHistoricDTC.size()];
		int i = 0;
		for (String n : mHistoricDTC)
			dtclist[i++] = n;
		return dtclist;
	}

	public void resetHistoricDTC() {
		mHistoricDTC.clear();
		String[] dtclist = getHistoricDTC();
		for (HarleyDataDiagnosticsListener l : mDiagnosticsListeners)
			l.onHistoricDTCChanged(dtclist);
	}

	public void addHistoricDTC(String dtc) {
		if (!mHistoricDTC.contains(dtc))
			mHistoricDTC.add(dtc);
		String[] dtclist = getHistoricDTC();
		for (HarleyDataDiagnosticsListener l : mDiagnosticsListeners)
			l.onHistoricDTCChanged(dtclist);
	}

	public String[] getCurrentDTC() {
		String[] dtclist = new String[mCurrentDTC.size()];
		int i = 0;
		for (String n : mCurrentDTC)
			dtclist[i++] = n;
		return dtclist;
	}

	public void resetCurrentDTC() {
		mCurrentDTC.clear();
		String[] dtclist = getCurrentDTC();
		for (HarleyDataDiagnosticsListener l : mDiagnosticsListeners)
			l.onCurrentDTCChanged(dtclist);
	}

	public void addCurrentDTC(String dtc) {
		if (!mCurrentDTC.contains(dtc))
			mCurrentDTC.add(dtc);
		String[] dtclist = getCurrentDTC();
		for (HarleyDataDiagnosticsListener l : mDiagnosticsListeners)
			l.onCurrentDTCChanged(dtclist);
	}

	public void setBadCRC(byte[] buffer) {
		for (HarleyDataRawListener l : mRawListeners)
			l.onBadCRCChanged(buffer);
	}

	public void setUnknown(byte[] buffer) {
		for (HarleyDataRawListener l : mRawListeners)
			l.onUnknownChanged(buffer);
	}

	public void setRaw(byte[] buffer) {
		for (HarleyDataRawListener l : mRawListeners)
			l.onRawChanged(buffer);
	}

	public String toString() {
		String ret;

		ret = "RPM:" + mRPM / 4;
		ret += " SPD:" + mSpeed / 128;
		ret += " ETP:" + mEngineTemp;
		ret += " FGE:" + mFuelGauge;
		ret += " TRN:";
		if ((mTurnSignals & 0x3) == 0x3)
			ret += "W";
		else if ((mTurnSignals & 0x1) != 0)
			ret += "R";
		else if ((mTurnSignals & 0x2) != 0)
			ret += "L";
		else
			ret += "x";
		ret += " CLU/NTR:";
		if (mNeutral)
			ret += "N";
		else
			ret += "x";
		if (mClutch)
			ret += "C";
		else
			ret += "x";
		if (mGear > 0 && mGear < 7)
			ret += mGear;
		else
			ret += "x";
		ret += " CHK:" + mCheckEngine;
		ret += " ODO:" + mOdometer;
		ret += " FUL:" + mFuel;
		return ret;
	}

	private class HarleyDataThread extends Thread {
		private static final int MAX_ITEMS = 10;
		private int fuelItems[];
		private int odoItems[];
		private int head, tail;
		private boolean stop = false;

		public void run() {
			setName("HarleyDataThread");
			fuelItems = new int[MAX_ITEMS];
			odoItems = new int[MAX_ITEMS];
			head = 0;
			tail = 1;

			while (!stop) {
				fuelItems[head] = getFuelMetric();
				odoItems[head] = getOdometerMetric();
				if ((fuelItems[head] != fuelItems[tail]) &&
					(odoItems[head] != odoItems[tail]))
					setFuelInstant((1000 * (fuelItems[head] - fuelItems[tail])) / (odoItems[head] - odoItems[tail]));
				else
					setFuelInstant(-1);
				head++;
				if (head >= MAX_ITEMS)
					head = 0;
				tail++;
				if (tail >= MAX_ITEMS)
					tail = 0;
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
			}
		}

		public void cancel() {
			stop = true;
		}
	};
};
