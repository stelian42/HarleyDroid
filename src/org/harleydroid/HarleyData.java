//
// HarleyDroid: Harley Davidson J1850 Data Analyser for Android.
//
// Copyright (C) 2010,2011 Stelian Pop <stelian@popies.net>
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

import android.os.Handler;

public class HarleyData {
	private int rpm;				// RPM in 1000*rotation/minute */
	private int speed;				// XXX speed in 1000*mph ??? */
	private int engineTemp;			// XXX engine temperature in degrees */
	private int full;				// fuel gauge: 0 (empty) to 6 (full) */
	private int turnSignals;		// turn signals bitmap: 0x1=right, 0x2=left */
	private boolean neutral;		// XXX boolean: in neutral */
	private boolean clutch;			// XXX boolean: clutch engaged */
	private int gear;				// XXX current gear: 1 to 6 */
	private boolean checkEngine;	// XXX boolean: check engine */
	private int odometer;			// odometer tick (1 tick = 0.4 meters) */
	private int fuel;				// fuel consumption tick (1 tick = 0.000040 liters) */

	private Handler handler;

	public HarleyData() {
		setHandler(null);
	}
	
	public void setHandler(Handler handler) {
		this.handler = handler;
		rpm = 0;
		speed = 0;
		engineTemp = 0;
		full = 0;
		turnSignals = 0;
		neutral = false;
		clutch = false;
		gear = 0;
		checkEngine = false;
		odometer = 0;
		fuel = 0;
	}

	public int getRPM() {
		return rpm;
	}

	public void setRPM(int rpm) {
		if (this.rpm != rpm) {
			this.rpm = rpm;
			if (handler != null)
				handler.obtainMessage(HarleyDroid.UPDATE_RPM, rpm / 1000, -1).sendToTarget();
		}
	}

	public int getSpeed() {
		return speed;
	}

	public void setSpeed(int speed) {
		if (this.speed != speed) {
			this.speed = speed;
			if (handler != null)
				handler.obtainMessage(HarleyDroid.UPDATE_SPEED, speed / 1000, -1).sendToTarget();
		}
	}

	public int getEngineTemp() {
		return engineTemp;
	}

	public void setEngineTemp(int engineTemp) {
		if (this.engineTemp != engineTemp) {
			this.engineTemp = engineTemp;
			if (handler != null)
				handler.obtainMessage(HarleyDroid.UPDATE_ENGINETEMP, engineTemp, -1).sendToTarget();
		}
	}

	public int getFull() {
		return full;
	}

	public void setFull(int full) {
		if (this.full != full) {
			this.full = full;
			if (handler != null)
				handler.obtainMessage(HarleyDroid.UPDATE_FULL, full, -1).sendToTarget();
		}
	}

	public int getTurnSignals() {
		return turnSignals;
	}

	public void setTurnSignals(int turnSignals) {
		if (this.turnSignals != turnSignals) {
			this.turnSignals = turnSignals;
			if (handler != null)
				handler.obtainMessage(HarleyDroid.UPDATE_TURNSIGNALS, turnSignals, -1).sendToTarget();
		}
	}

	public boolean getNeutral() {
		return neutral;
	}

	public void setNeutral(boolean neutral) {
		if (this.neutral != neutral) {
			this.neutral = neutral;
			if (handler != null)
				handler.obtainMessage(HarleyDroid.UPDATE_NEUTRAL, neutral ? 1 : 0, -1).sendToTarget();
		}
	}

	public boolean getClutch() {
		return clutch;
	}

	public void setClutch(boolean clutch) {
		if (this.clutch != clutch) {
			this.clutch = clutch;
			if (handler != null)
				handler.obtainMessage(HarleyDroid.UPDATE_CLUTCH, clutch ? 1 : 0, -1).sendToTarget();
		}
	}

	public int getGear() {
		return gear;
	}

	public void setGear(int gear) {
		if (this.gear != gear) {
			this.gear = gear;
			if (handler != null)
				handler.obtainMessage(HarleyDroid.UPDATE_GEAR, gear, -1).sendToTarget();
		}
	}

	public boolean getCheckEngine() {
		return checkEngine;
	}

	public void setCheckEngine(boolean checkEngine) {
		if (this.checkEngine != checkEngine) {
			this.checkEngine = checkEngine;
			if (handler != null)
				handler.obtainMessage(HarleyDroid.UPDATE_CHECKENGINE, checkEngine ? 1 : 0, -1).sendToTarget();
		}
	}

	public int getOdometer() {
		return odometer;
	}

	public void setOdometer(int odometer) {
		if (this.odometer != odometer) {
			this.odometer = odometer;
			if (handler != null)
				handler.obtainMessage(HarleyDroid.UPDATE_ODOMETER, (int)(odometer / 2.5f), -1).sendToTarget();
		}
	}

	public int getFuel() {
		return fuel;
	}

	public void setFuel(int fuel) {
		if (this.fuel != fuel) {
			this.fuel = fuel;
			if (handler != null)
				handler.obtainMessage(HarleyDroid.UPDATE_FUEL, fuel / 25, -1).sendToTarget();
		}
	}

	public String toString() {
		String ret;

		ret = "RPM:" + rpm / 1000;
		ret += " SPEED:" + speed / 1000;
		ret += " ENGTEMP:" + engineTemp;
		ret += " FULL:" + full;
		ret += " TURN:";
		if ((turnSignals & 0x3) == 0x3)
			ret += "W";
		else if ((turnSignals & 0x1) != 0)
			ret += "R";
		else if ((turnSignals & 0x2) != 0)
			ret += "L";
		else
			ret += "x";
		ret += " CLUTCH:";
		if (neutral)
			ret += "N";
		else
			ret += "x";
		if (clutch)
			ret += "C";
		else
			ret += "x";
		if (gear > 0 && gear < 7)
			ret += gear;
		else
			ret += "x";
		ret += " CHECK:" + checkEngine;
		ret += " ODO:" + odometer;
		ret += " FUEL:" + fuel;
		return ret;
	}
};
