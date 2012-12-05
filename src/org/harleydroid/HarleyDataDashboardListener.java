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

public interface HarleyDataDashboardListener {

	public void onRPMChanged(int rpm);
	public void onSpeedImperialChanged(int speed);
	public void onSpeedMetricChanged(int speed);
	public void onEngineTempImperialChanged(int engineTemp);
	public void onEngineTempMetricChanged(int engineTemp);
	public void onFuelGaugeChanged(int full, boolean low);
	public void onTurnSignalsChanged(int turnSignals);
	public void onNeutralChanged(boolean neutral);
	public void onClutchChanged(boolean clutch);
	public void onGearChanged(int gear);
	public void onCheckEngineChanged(boolean checkEngine);
	public void onOdometerImperialChanged(int odometer);
	public void onOdometerMetricChanged(int odometer);
	public void onFuelImperialChanged(int fuel);
	public void onFuelMetricChanged(int fuel);
	public void onFuelAverageImperialChanged(int fuel);
	public void onFuelAverageMetricChanged(int fuel);
	public void onFuelInstantImperialChanged(int fuel);
	public void onFuelInstantMetricChanged(int fuel);
};
