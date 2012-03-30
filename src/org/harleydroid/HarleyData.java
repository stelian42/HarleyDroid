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

public class HarleyData {

	// raw values reported in the J1850 stream
	private int mRPM = 0;					// RPM in rotation/minute * 4
	private int mSpeed = 0;					// speed in mph * 200
	private int mEngineTemp = 0;			// XXX engine temperature in Fahrenheit
	private int mFuelGauge = 0;				// fuel gauge: 0 (empty) to 6 (full)
	private int mTurnSignals = 0;			// turn signals bitmap: 0x1=right, 0x2=left
	private boolean mNeutral = false;		// XXX boolean: in neutral
	private boolean mClutch = false;		// boolean: clutch engaged
	private int mGear = 0;					// current gear: -1 or 1 to 6
	private boolean mCheckEngine = false;	// boolean: check engine
	private int mOdometer = 0;				// odometer tick (1 tick = 0.4 meters)
	private int mFuel = 0;					// fuel consumption tick (1 tick = 0.000040 liters)
	private String mVIN = "N/A";			// VIN
	private String mECMPN = "N/A";			// ECM Part Number
	private String mECMCalID = "N/A";		// ECM Calibration ID
	private int mECMSWLevel = 0;			// ECM Software Level
	private CopyOnWriteArrayList<Integer> mHistoricDTC; // Historic DTC
	private CopyOnWriteArrayList<Integer> mCurrentDTC;	// Current DTC

	private int mResetOdometer = 0;
	private int mResetFuel = 0;

	private CopyOnWriteArrayList<HarleyDataDashboardListener> mDashboardListeners;
	private CopyOnWriteArrayList<HarleyDataDiagnosticsListener> mDiagnosticsListeners;
	private CopyOnWriteArrayList<HarleyDataRawListener> mRawListeners;

	public HarleyData() {
		mDashboardListeners = new CopyOnWriteArrayList<HarleyDataDashboardListener>();
		mDiagnosticsListeners = new CopyOnWriteArrayList<HarleyDataDiagnosticsListener>();
		mRawListeners = new CopyOnWriteArrayList<HarleyDataRawListener>();

		mHistoricDTC = new CopyOnWriteArrayList<Integer>();
		mCurrentDTC = new CopyOnWriteArrayList<Integer>();
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
		return mEngineTemp;
	}

	// returns the temperature in C
	public int getEngineTempMetric() {
		return (mEngineTemp - 32) * 5 / 9;
	}

	public void setEngineTemp(int engineTemp) {
		if (mEngineTemp != engineTemp) {
			mEngineTemp = engineTemp;
			for (HarleyDataDashboardListener l : mDashboardListeners) {
				l.onEngineTempImperialChanged(mEngineTemp);
				l.onEngineTempMetricChanged((mEngineTemp - 32) * 5 / 9);
			}
		}
	}

	// returns the fuel gauge as 0 (empty) to 6 (full)
	public int getFuelGauge() {
		return mFuelGauge;
	}

	public void setFuelGauge(int fuelGauge) {
		if (mFuelGauge != fuelGauge) {
			mFuelGauge = fuelGauge;
			for (HarleyDataDashboardListener l : mDashboardListeners)
				l.onFuelGaugeChanged(mFuelGauge);
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
		return ((mOdometer - mResetOdometer) * 40) / 1609;
	}

	// returns the odometer in km * 100
	public int getOdometerMetric() {
		return (mOdometer - mResetOdometer) / 25;
	}

	public void setOdometer(int odometer) {
		if (mOdometer != odometer) {
			mOdometer = odometer;
			for (HarleyDataDashboardListener l : mDashboardListeners) {
				int o = mOdometer - mResetOdometer;
				l.onOdometerImperialChanged((o * 40) / 1609);
				l.onOdometerMetricChanged(o / 25);
			}
		}
	}

	// returns the fuel in fl oz
	public int getFuelImperial() {
		return ((mFuel - mResetFuel) * 338) / 250000;
	}

	// returns the fuel in milliliters
	public int getFuelMetric() {
		return (mFuel - mResetFuel) / 25;
	}

	public void setFuel(int fuel) {
		if (mFuel != fuel) {
			mFuel = fuel;
			for (HarleyDataDashboardListener l : mDashboardListeners) {
				int f = mFuel - mResetFuel;
				l.onFuelImperialChanged((f * 338) / 250000);
				l.onFuelMetricChanged(f / 25);
			}
		}
	}

	public void resetCounters() {
		mResetOdometer = mOdometer;
		mResetFuel = mFuel;
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

	public int[] getHistoricDTC() {
		int[] dtclist = new int[mHistoricDTC.size()];
		int i = 0;
		for (Integer n : mHistoricDTC)
			dtclist[i++] = n;
		return dtclist;
	}

	public void resetHistoricDTC() {
		mHistoricDTC.clear();
	}

	public void addHistoricDTC(int dtc) {
		if (mHistoricDTC.contains(dtc))
			return;
		mHistoricDTC.add(dtc);
		int[] dtclist = new int[mHistoricDTC.size()];
		int i = 0;
		for (Integer n : mHistoricDTC)
			dtclist[i++] = n;
		for (HarleyDataDiagnosticsListener l : mDiagnosticsListeners)
			l.onHistoricDTCChanged(dtclist);
	}

	public int[] getCurrentDTC() {
		int[] dtclist = new int[mCurrentDTC.size()];
		int i = 0;
		for (Integer n : mCurrentDTC)
			dtclist[i++] = n;
		return dtclist;
	}

	public void resetCurrentDTC() {
		mCurrentDTC.clear();
	}

	public void addCurrentDTC(int dtc) {
		if (mCurrentDTC.contains(dtc))
			return;
		mCurrentDTC.add(dtc);
		int[] dtclist = new int[mCurrentDTC.size()];
		int i = 0;
		for (Integer n : mCurrentDTC)
			dtclist[i++] = n;
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
};
