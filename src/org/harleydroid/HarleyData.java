package org.harleydroid;

import android.os.Handler;

public class HarleyData {
	private int rpm;			// RPM in 1000*rotation/minute */
	private int speed;			// speed in meters/hour */
	private int engineTemp;		// engine temperature in degrees */
	private int full;			// XXX gas ??? */
	private int turnSignals;	// turn signals bitmap: 0x1=right, 0x2=left     */
	private boolean neutral;	// boolean: in neutral */
	private boolean clutch;		// boolean: clutch engaged */
	private int gear;			// current gear: 1 to 6 */
	private boolean checkEngine;// boolean: check engine */
	private int odometer;		// XXX odometer tick = 4 mm */
	private int fuel;			// XXX fuel tick = 0.05 mm */

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
		this.full = full;
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
		this.neutral = neutral;
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
		this.gear = gear;
	}

	public boolean getCheckEngine() {
		return checkEngine;
	}

	public void setCheckEngine(boolean checkEngine) {
		this.checkEngine = checkEngine;
	}

	public int getOdometer() {
		return odometer;
	}

	public void setOdometer(int odometer) {
		this.odometer = odometer;
	}

	public int getFuel() {
		return fuel;
	}

	public void setFuel(int fuel) {
		this.fuel = fuel;
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