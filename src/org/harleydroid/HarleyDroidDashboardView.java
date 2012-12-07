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

import java.lang.ref.WeakReference;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

@SuppressLint("DefaultLocale")
public class HarleyDroidDashboardView implements HarleyDataDashboardListener
{
	private static final boolean D = false;
	private static final String TAG = HarleyDroidDashboardView.class.getSimpleName();

	public static final int UPDATE_RPM = 1;
	public static final int UPDATE_SPEED_IMPERIAL = 2;
	public static final int UPDATE_SPEED_METRIC = 3;
	public static final int UPDATE_ENGINETEMP_IMPERIAL = 4;
	public static final int UPDATE_ENGINETEMP_METRIC = 5;
	public static final int UPDATE_FUELGAUGE = 6;
	public static final int UPDATE_TURNSIGNALS = 7;
	public static final int UPDATE_NEUTRAL = 8;
	public static final int UPDATE_CLUTCH = 9;
	public static final int UPDATE_GEAR = 10;
	public static final int UPDATE_CHECKENGINE = 11;
	public static final int UPDATE_ODOMETER_IMPERIAL = 12;
	public static final int UPDATE_ODOMETER_METRIC = 13;
	public static final int UPDATE_FUEL_IMPERIAL = 14;
	public static final int UPDATE_FUEL_METRIC = 15;
	public static final int UPDATE_FUEL_AVERAGE_IMPERIAL = 16;
	public static final int UPDATE_FUEL_AVERAGE_METRIC = 17;
	public static final int UPDATE_FUEL_INSTANT_IMPERIAL = 18;
	public static final int UPDATE_FUEL_INSTANT_METRIC = 19;

	public static final int VIEW_GRAPHIC = 1;
	public static final int VIEW_TEXT = 2;

	private Activity mActivity;
	private HarleyDroidDashboardViewHandler mHandler;

	// Views references cached for performance
	private TextView mViewRpm;
	private Gauge mGaugeRpm;
	private TextView mLabelSpeedMetric;
	private TextView mLabelSpeedImperial;
	private TextView mViewSpeedMetric;
	private TextView mViewSpeedImperial;
	private Gauge mGaugeSpeedMetric;
	private Gauge mGaugeSpeedImperial;
	private TextView mLabelEngTempMetric;
	private TextView mLabelEngTempImperial;
	private TextView mViewEngTempMetric;
	private TextView mViewEngTempImperial;
	private TextView mViewFuelGauge;
	private View mImageTurnSignalsLeft;
	private View mImageTurnSignalsRight;
	private TextView mViewTurnSignals;
	private TextView mViewNeutral;
	private TextView mViewClutch;
	private TextView mViewGear;
	private TextView mViewGearNeutral;
	private View mImageCheckEngine;
	private TextView mViewCheckEngine;
	private TextView mLabelOdometerMetric;
	private TextView mLabelOdometerImperial;
	private TextView mViewOdometerMetric;
	private TextView mViewOdometerImperial;
	private TextView mLabelFuelMetric;
	private TextView mLabelFuelImperial;
	private TextView mViewFuelMetric;
	private TextView mViewFuelImperial;
	private TextView mLabelFuelAvgMetric;
	private TextView mLabelFuelAvgImperial;
	private TextView mViewFuelAvgMetric;
	private TextView mViewFuelAvgImperial;
	private TextView mLabelFuelInstMetric;
	private TextView mLabelFuelInstImperial;
	private TextView mViewFuelInstMetric;
	private TextView mViewFuelInstImperial;
	private TextView mViewMileageMetric;
	private TextView mViewMileageImperial;
	private View mImageLowFuel;

	public HarleyDroidDashboardView(Activity activity) {
		mActivity = activity;
		mHandler = new HarleyDroidDashboardViewHandler(this);
	}

	public void changeView(int viewMode, boolean portrait, boolean unitMetric) {
		if (D) Log.d(TAG, "changeView to " + viewMode + " portrait=" + portrait + " metric=" + unitMetric);

		int view = R.layout.portrait_graphic;

		switch (viewMode) {
		case VIEW_GRAPHIC:
			if (portrait)
				view = R.layout.portrait_graphic;
			else
				view = R.layout.landscape_graphic;
			mActivity.setContentView(view);

			mGaugeSpeedMetric = (Gauge) mActivity.findViewById(R.id.speed_metric_meter);
			mGaugeSpeedImperial = (Gauge) mActivity.findViewById(R.id.speed_imperial_meter);
			mGaugeRpm = (Gauge) mActivity.findViewById(R.id.rpm_meter);
			mImageTurnSignalsLeft = (View) mActivity.findViewById(R.id.turn_left);
			mImageCheckEngine = (View) mActivity.findViewById(R.id.check_engine);
			mImageTurnSignalsRight = (View) mActivity.findViewById(R.id.turn_right);
			mViewGearNeutral = (TextView) mActivity.findViewById(R.id.gearneutral);
			mViewMileageMetric = (TextView) mActivity.findViewById(R.id.mileage_metric);
			mViewMileageImperial = (TextView) mActivity.findViewById(R.id.mileage_imperial);
			mImageLowFuel = (View) mActivity.findViewById(R.id.low_fuel);

			mViewRpm = null;
			mLabelSpeedMetric = null;
			mLabelSpeedImperial = null;
			mViewSpeedMetric = null;
			mViewSpeedImperial = null;
			mLabelEngTempMetric = null;
			mLabelEngTempImperial = null;
			mViewEngTempMetric = null;
			mViewEngTempImperial = null;
			mViewFuelGauge = null;
			mViewTurnSignals = null;
			mViewClutch = null;
			mViewCheckEngine = null;
			mViewGear = null;
			mViewNeutral = null;
			mLabelOdometerMetric = null;
			mLabelOdometerImperial = null;
			mViewOdometerMetric = null;
			mViewOdometerImperial = null;
			mLabelFuelMetric = null;
			mLabelFuelImperial = null;
			mViewFuelMetric = null;
			mViewFuelImperial = null;
			mLabelFuelAvgMetric = null;
			mLabelFuelAvgImperial = null;
			mViewFuelAvgMetric = null;
			mViewFuelAvgImperial = null;
			mLabelFuelInstMetric = null;
			mLabelFuelInstImperial = null;
			mViewFuelInstMetric = null;
			mViewFuelInstImperial = null;

			if (unitMetric) {
				mGaugeSpeedImperial.setVisibility(View.GONE);
				mGaugeSpeedMetric.setVisibility(View.VISIBLE);
				mViewMileageImperial.setVisibility(View.GONE);
				mViewMileageMetric.setVisibility(View.VISIBLE);
			} else {
				mGaugeSpeedMetric.setVisibility(View.GONE);
				mGaugeSpeedImperial.setVisibility(View.VISIBLE);
				mViewMileageMetric.setVisibility(View.GONE);
				mViewMileageImperial.setVisibility(View.VISIBLE);
			}
			break;
		case VIEW_TEXT:
			if (portrait)
				view = R.layout.portrait_text;
			else
				view = R.layout.landscape_text;
			mActivity.setContentView(view);

			mGaugeSpeedMetric = null;
			mGaugeSpeedImperial = null;
			mGaugeRpm = null;
			mImageTurnSignalsLeft = null;
			mImageCheckEngine = null;
			mImageTurnSignalsRight = null;
			mViewMileageMetric = null;
			mViewMileageImperial = null;
			mViewGearNeutral = null;
			mImageLowFuel = null;

			mViewRpm = (TextView) mActivity.findViewById(R.id.rpm_field);
			mLabelSpeedMetric = (TextView) mActivity.findViewById(R.id.speed_metric_label);
			mLabelSpeedImperial = (TextView) mActivity.findViewById(R.id.speed_imperial_label);
			mViewSpeedMetric = (TextView) mActivity.findViewById(R.id.speed_metric_field);
			mViewSpeedImperial = (TextView) mActivity.findViewById(R.id.speed_imperial_field);
			mLabelEngTempMetric = (TextView) mActivity.findViewById(R.id.enginetemp_metric_label);
			mLabelEngTempImperial = (TextView) mActivity.findViewById(R.id.enginetemp_imperial_label);
			mViewEngTempMetric = (TextView) mActivity.findViewById(R.id.enginetemp_metric_field);
			mViewEngTempImperial = (TextView) mActivity.findViewById(R.id.enginetemp_imperial_field);
			mViewFuelGauge = (TextView) mActivity.findViewById(R.id.fuelgauge_field);
			mViewTurnSignals = (TextView) mActivity.findViewById(R.id.turnsignals_field);
			mViewNeutral = (TextView) mActivity.findViewById(R.id.neutral_field);
			mViewClutch = (TextView) mActivity.findViewById(R.id.clutch_field);
			mViewGear = (TextView) mActivity.findViewById(R.id.gear_field);
			mViewCheckEngine = (TextView) mActivity.findViewById(R.id.checkengine_field);
			mLabelOdometerMetric = (TextView) mActivity.findViewById(R.id.odometer_metric_label);
			mLabelOdometerImperial = (TextView) mActivity.findViewById(R.id.odometer_imperial_label);
			mViewOdometerMetric = (TextView) mActivity.findViewById(R.id.odometer_metric_field);
			mViewOdometerImperial = (TextView) mActivity.findViewById(R.id.odometer_imperial_field);
			mLabelFuelMetric = (TextView) mActivity.findViewById(R.id.fuel_metric_label);
			mLabelFuelImperial = (TextView) mActivity.findViewById(R.id.fuel_imperial_label);
			mViewFuelMetric = (TextView) mActivity.findViewById(R.id.fuel_metric_field);
			mViewFuelImperial = (TextView) mActivity.findViewById(R.id.fuel_imperial_field);
			mLabelFuelAvgMetric = (TextView) mActivity.findViewById(R.id.fuelavg_metric_label);
			mLabelFuelAvgImperial = (TextView) mActivity.findViewById(R.id.fuelavg_imperial_label);
			mViewFuelAvgMetric = (TextView) mActivity.findViewById(R.id.fuelavg_metric_field);
			mViewFuelAvgImperial = (TextView) mActivity.findViewById(R.id.fuelavg_imperial_field);
			mLabelFuelInstMetric = (TextView) mActivity.findViewById(R.id.fuelinst_metric_label);
			mLabelFuelInstImperial = (TextView) mActivity.findViewById(R.id.fuelinst_imperial_label);
			mViewFuelInstMetric = (TextView) mActivity.findViewById(R.id.fuelinst_metric_field);
			mViewFuelInstImperial = (TextView) mActivity.findViewById(R.id.fuelinst_imperial_field);

			if (unitMetric) {
				mLabelSpeedImperial.setVisibility(View.GONE);
				mLabelSpeedImperial = null;
				mLabelSpeedMetric.setVisibility(View.VISIBLE);
				mViewSpeedImperial.setVisibility(View.GONE);
				mViewSpeedImperial = null;
				mViewSpeedMetric.setVisibility(View.VISIBLE);
				mLabelEngTempImperial.setVisibility(View.GONE);
				mLabelEngTempImperial = null;
				mLabelEngTempMetric.setVisibility(View.VISIBLE);
				mViewEngTempImperial.setVisibility(View.GONE);
				mViewEngTempImperial = null;
				mViewEngTempMetric.setVisibility(View.VISIBLE);
				mLabelOdometerImperial.setVisibility(View.GONE);
				mLabelOdometerImperial = null;
				mLabelOdometerMetric.setVisibility(View.VISIBLE);
				mViewOdometerImperial.setVisibility(View.GONE);
				mViewOdometerImperial = null;
				mViewOdometerMetric.setVisibility(View.VISIBLE);
				mLabelFuelImperial.setVisibility(View.GONE);
				mLabelFuelImperial = null;
				mLabelFuelMetric.setVisibility(View.VISIBLE);
				mViewFuelImperial.setVisibility(View.GONE);
				mViewFuelImperial = null;
				mViewFuelMetric.setVisibility(View.VISIBLE);
				mLabelFuelAvgImperial.setVisibility(View.GONE);
				mLabelFuelAvgImperial = null;
				mLabelFuelAvgMetric.setVisibility(View.VISIBLE);
				mViewFuelAvgImperial.setVisibility(View.GONE);
				mViewFuelAvgImperial = null;
				mViewFuelAvgMetric.setVisibility(View.VISIBLE);
				mLabelFuelInstImperial.setVisibility(View.GONE);
				mLabelFuelInstImperial = null;
				mLabelFuelInstMetric.setVisibility(View.VISIBLE);
				mViewFuelInstImperial.setVisibility(View.GONE);
				mViewFuelInstImperial = null;
				mViewFuelInstMetric.setVisibility(View.VISIBLE);
			} else {
				mLabelSpeedMetric.setVisibility(View.GONE);
				mLabelSpeedMetric = null;
				mLabelSpeedImperial.setVisibility(View.VISIBLE);
				mViewSpeedMetric.setVisibility(View.GONE);
				mViewSpeedMetric = null;
				mViewSpeedImperial.setVisibility(View.VISIBLE);
				mLabelEngTempMetric.setVisibility(View.GONE);
				mLabelEngTempMetric = null;
				mLabelEngTempImperial.setVisibility(View.VISIBLE);
				mViewEngTempMetric.setVisibility(View.GONE);
				mViewEngTempMetric = null;
				mViewEngTempImperial.setVisibility(View.VISIBLE);
				mLabelOdometerMetric.setVisibility(View.GONE);
				mLabelOdometerMetric = null;
				mLabelOdometerImperial.setVisibility(View.VISIBLE);
				mViewOdometerMetric.setVisibility(View.GONE);
				mViewOdometerMetric = null;
				mViewOdometerImperial.setVisibility(View.VISIBLE);
				mLabelFuelMetric.setVisibility(View.GONE);
				mLabelFuelMetric = null;
				mLabelFuelImperial.setVisibility(View.VISIBLE);
				mViewFuelMetric.setVisibility(View.GONE);
				mViewFuelMetric = null;
				mViewFuelImperial.setVisibility(View.VISIBLE);
				mLabelFuelAvgMetric.setVisibility(View.GONE);
				mLabelFuelAvgMetric = null;
				mLabelFuelAvgImperial.setVisibility(View.VISIBLE);
				mViewFuelAvgMetric.setVisibility(View.GONE);
				mViewFuelAvgMetric = null;
				mViewFuelAvgImperial.setVisibility(View.VISIBLE);
				mLabelFuelInstMetric.setVisibility(View.GONE);
				mLabelFuelInstMetric = null;
				mLabelFuelInstImperial.setVisibility(View.VISIBLE);
				mViewFuelInstMetric.setVisibility(View.GONE);
				mViewFuelInstMetric = null;
				mViewFuelInstImperial.setVisibility(View.VISIBLE);
			}

			break;
		}
	}

	public void handleMessage(Message msg) {
		if (D) Log.d(TAG, "handleMessage " + msg.what);

		switch (msg.what) {
		case UPDATE_RPM:
			drawRPM(msg.arg1);
			break;
		case UPDATE_SPEED_IMPERIAL:
			drawSpeedImperial(msg.arg1);
			break;
		case UPDATE_SPEED_METRIC:
			drawSpeedMetric(msg.arg1);
			break;
		case UPDATE_ENGINETEMP_IMPERIAL:
			drawEngineTempImperial(msg.arg1);
			break;
		case UPDATE_ENGINETEMP_METRIC:
			drawEngineTempMetric(msg.arg1);
			break;
		case UPDATE_FUELGAUGE:
			drawFuelGauge(msg.arg1, msg.arg2 != 0 ? true : false);
			break;
		case UPDATE_TURNSIGNALS:
			drawTurnSignals(msg.arg1);
			break;
		case UPDATE_NEUTRAL:
			drawNeutral(msg.arg1 != 0 ? true : false);
			break;
		case UPDATE_CLUTCH:
			drawClutch(msg.arg1 != 0 ? true : false);
			break;
		case UPDATE_GEAR:
			drawGear(msg.arg1);
			break;
		case UPDATE_CHECKENGINE:
			drawCheckEngine(msg.arg1 != 0 ? true : false);
			break;
		case UPDATE_ODOMETER_IMPERIAL:
			drawOdometerImperial(msg.arg1);
			break;
		case UPDATE_ODOMETER_METRIC:
			drawOdometerMetric(msg.arg1);
			break;
		case UPDATE_FUEL_IMPERIAL:
			drawFuelImperial(msg.arg1);
			break;
		case UPDATE_FUEL_METRIC:
			drawFuelMetric(msg.arg1);
			break;
		case UPDATE_FUEL_AVERAGE_IMPERIAL:
			drawFuelAvgImperial(msg.arg1);
			break;
		case UPDATE_FUEL_AVERAGE_METRIC:
			drawFuelAvgMetric(msg.arg1);
			break;
		case UPDATE_FUEL_INSTANT_IMPERIAL:
			drawFuelInstImperial(msg.arg1);
			break;
		case UPDATE_FUEL_INSTANT_METRIC:
			drawFuelInstMetric(msg.arg1);
			break;
		}
	}

	public void onRPMChanged(int rpm) {
		mHandler.obtainMessage(HarleyDroidDashboardView.UPDATE_RPM, rpm, -1).sendToTarget();
	}

	public void onSpeedImperialChanged(int speed) {
		mHandler.obtainMessage(HarleyDroidDashboardView.UPDATE_SPEED_IMPERIAL, speed, -1).sendToTarget();
	}

	public void onSpeedMetricChanged(int speed) {
		mHandler.obtainMessage(HarleyDroidDashboardView.UPDATE_SPEED_METRIC, speed, -1).sendToTarget();
	}

	public void onEngineTempImperialChanged(int engineTemp) {
		mHandler.obtainMessage(HarleyDroidDashboardView.UPDATE_ENGINETEMP_IMPERIAL, engineTemp, -1).sendToTarget();
	}

	public void onEngineTempMetricChanged(int engineTemp) {
		mHandler.obtainMessage(HarleyDroidDashboardView.UPDATE_ENGINETEMP_METRIC, engineTemp, -1).sendToTarget();
	}

	public void onFuelGaugeChanged(int full, boolean low) {
		mHandler.obtainMessage(HarleyDroidDashboardView.UPDATE_FUELGAUGE, full, low ? 1 : 0).sendToTarget();
	}

	public void onTurnSignalsChanged(int turnSignals) {
		mHandler.obtainMessage(HarleyDroidDashboardView.UPDATE_TURNSIGNALS, turnSignals, -1).sendToTarget();
	}

	public void onNeutralChanged(boolean neutral) {
		mHandler.obtainMessage(HarleyDroidDashboardView.UPDATE_NEUTRAL, neutral ? 1 : 0, -1).sendToTarget();
	}

	public void onClutchChanged(boolean clutch) {
		mHandler.obtainMessage(HarleyDroidDashboardView.UPDATE_CLUTCH, clutch ? 1 : 0, -1).sendToTarget();
	}

	public void onGearChanged(int gear) {
		mHandler.obtainMessage(HarleyDroidDashboardView.UPDATE_GEAR, gear, -1).sendToTarget();
	}

	public void onCheckEngineChanged(boolean checkEngine) {
		mHandler.obtainMessage(HarleyDroidDashboardView.UPDATE_CHECKENGINE, checkEngine ? 1 : 0, -1).sendToTarget();
	}

	public void onOdometerImperialChanged(int odometer) {
		mHandler.obtainMessage(HarleyDroidDashboardView.UPDATE_ODOMETER_IMPERIAL, odometer, -1).sendToTarget();
	}

	public void onOdometerMetricChanged(int odometer) {
		mHandler.obtainMessage(HarleyDroidDashboardView.UPDATE_ODOMETER_METRIC, odometer, -1).sendToTarget();
	}

	public void onFuelImperialChanged(int fuel) {
		mHandler.obtainMessage(HarleyDroidDashboardView.UPDATE_FUEL_IMPERIAL, fuel, -1).sendToTarget();
	}

	public void onFuelMetricChanged(int fuel) {
		mHandler.obtainMessage(HarleyDroidDashboardView.UPDATE_FUEL_METRIC, fuel, -1).sendToTarget();
	}

	public void onFuelAverageImperialChanged(int fuel) {
		mHandler.obtainMessage(HarleyDroidDashboardView.UPDATE_FUEL_AVERAGE_IMPERIAL, fuel, -1).sendToTarget();
	}

	public void onFuelAverageMetricChanged(int fuel) {
		mHandler.obtainMessage(HarleyDroidDashboardView.UPDATE_FUEL_AVERAGE_METRIC, fuel, -1).sendToTarget();
	}

	public void onFuelInstantImperialChanged(int fuel) {
		mHandler.obtainMessage(HarleyDroidDashboardView.UPDATE_FUEL_INSTANT_IMPERIAL, fuel, -1).sendToTarget();
	}

	public void onFuelInstantMetricChanged(int fuel) {
		mHandler.obtainMessage(HarleyDroidDashboardView.UPDATE_FUEL_INSTANT_METRIC, fuel, -1).sendToTarget();
	}

	public void drawAll(HarleyData hd) {

		if (hd != null) {
			drawRPM(hd.getRPM());
			drawSpeedImperial(hd.getSpeedImperial());
			drawSpeedMetric(hd.getSpeedMetric());
			drawEngineTempImperial(hd.getEngineTempImperial());
			drawEngineTempMetric(hd.getEngineTempMetric());
			drawFuelGauge(hd.getFuelGauge(), hd.getFuelLow());
			drawTurnSignals(hd.getTurnSignals());
			drawNeutral(hd.getNeutral());
			drawClutch(hd.getClutch());
			drawGear(hd.getGear());
			drawCheckEngine(hd.getCheckEngine());
			drawOdometerImperial(hd.getOdometerImperial());
			drawOdometerMetric(hd.getOdometerMetric());
			drawFuelImperial(hd.getFuelImperial());
			drawFuelMetric(hd.getFuelMetric());
			drawFuelAvgImperial(hd.getFuelAverageImperial());
			drawFuelAvgMetric(hd.getFuelAverageMetric());
			drawFuelInstImperial(hd.getFuelInstantImperial());
			drawFuelInstMetric(hd.getFuelInstantMetric());
		} else {
			drawRPM(0);
			drawSpeedImperial(0);
			drawSpeedMetric(0);
			drawEngineTempImperial(0);
			drawEngineTempMetric(0);
			drawFuelGauge(0, false);
			drawTurnSignals(0);
			drawNeutral(false);
			drawClutch(false);
			drawGear(-1);
			drawCheckEngine(false);
			drawFuelInstImperial(-1);
			drawFuelInstMetric(-1);

			/* need to retrieve the saved odometer/fuel */
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity.getBaseContext());
			int savedOdometer = prefs.getInt("odometer", 0);
			int savedFuel = prefs.getInt("fuel", 0);
			drawOdometerMetric(savedOdometer / 25);
			drawOdometerImperial((savedOdometer * 40) / 1609);
			drawFuelMetric(savedFuel / 20);
			drawFuelImperial((savedFuel * 264) / 20000);
			if (savedOdometer == 0 || savedFuel == 0) {
				drawFuelAvgMetric(-1);
				drawFuelAvgImperial(-1);
			}
			else {
				drawFuelAvgMetric((1250 * savedFuel) / savedOdometer);
				drawFuelAvgImperial(2352146 / ((1250 * savedFuel) / savedOdometer));
			}
		}
	}

	public void drawRPM(int value) {
		if (mViewRpm != null)
			mViewRpm.setText(Integer.toString(value));
		if (mGaugeRpm != null)
			mGaugeRpm.setValue(value / 100);
	}

	public void drawSpeedImperial(int value) {
		// value is in mph
		if (mViewSpeedImperial != null)
			mViewSpeedImperial.setText(Integer.toString(value));
		if (mGaugeSpeedImperial != null)
			mGaugeSpeedImperial.setValue(value);
	}

	public void drawSpeedMetric(int value) {
		// value is in km/h
		if (mViewSpeedMetric != null)
			mViewSpeedMetric.setText(Integer.toString(value));
		if (mGaugeSpeedMetric != null)
			mGaugeSpeedMetric.setValue(value);
	}

	public void drawEngineTempImperial(int value) {
		// value is in F
		if (mViewEngTempImperial != null)
			mViewEngTempImperial.setText(Integer.toString(value));
	}

	public void drawEngineTempMetric(int value) {
		// value is in C
		if (mViewEngTempMetric != null)
			mViewEngTempMetric.setText(Integer.toString(value));
	}

	public void drawFuelGauge(int value, boolean low) {
		if (mViewFuelGauge != null) {
			if (low)
				mViewFuelGauge.setText(R.string.low_fuel_text);
			else
				mViewFuelGauge.setText(Integer.toString(value));
		}
		if (mImageLowFuel != null) {
			if (low)
				mImageLowFuel.setVisibility(View.VISIBLE);
			else
				mImageLowFuel.setVisibility(View.INVISIBLE);
		}
	}

	public void drawTurnSignals(int value) {
		if ((value & 0x03) == 0x03) {
			if (mImageTurnSignalsLeft != null)
				mImageTurnSignalsLeft.setVisibility(View.VISIBLE);
			if (mImageTurnSignalsRight != null)
				mImageTurnSignalsRight.setVisibility(View.VISIBLE);
			if (mViewTurnSignals != null)
				mViewTurnSignals.setText("W");
		}
		else if ((value & 0x01) == 0x01) {
			if (mImageTurnSignalsLeft != null)
				mImageTurnSignalsLeft.setVisibility(View.INVISIBLE);
			if (mImageTurnSignalsRight != null)
				mImageTurnSignalsRight.setVisibility(View.VISIBLE);
			if (mViewTurnSignals != null)
				mViewTurnSignals.setText("R");
		}
		else if ((value & 0x02) == 0x02) {
			if (mImageTurnSignalsLeft != null)
				mImageTurnSignalsLeft.setVisibility(View.VISIBLE);
			if (mImageTurnSignalsRight != null)
				mImageTurnSignalsRight.setVisibility(View.INVISIBLE);
			if (mViewTurnSignals != null)
				mViewTurnSignals.setText("L");
		}
		else {
			if (mImageTurnSignalsLeft != null)
				mImageTurnSignalsLeft.setVisibility(View.INVISIBLE);
			if (mImageTurnSignalsRight != null)
				mImageTurnSignalsRight.setVisibility(View.INVISIBLE);
			if (mViewTurnSignals != null)
				mViewTurnSignals.setText("-");
		}
	}

	public void drawNeutral(boolean value) {
		if (mViewNeutral != null) {
			if (value)
				mViewNeutral.setText("N");
			else
				mViewNeutral.setText("-");
		}
		if (mViewGearNeutral != null) {
			if (value)
				mViewGearNeutral.setText("N");
			else
				mViewGearNeutral.setText("-");
		}
	}

	public void drawClutch(boolean value) {
		if (mViewClutch != null) {
			if (value)
				mViewClutch.setText("C");
			else
				mViewClutch.setText("-");
		}
	}

	public void drawGear(int value) {
		if (mViewGear != null) {
			if (value == -1)
				mViewGear.setText("-");
			else
				mViewGear.setText(Integer.toString(value));
		}
		if (mViewGearNeutral != null) {
			if (value == -1)
				mViewGearNeutral.setText("-");
			else
				mViewGearNeutral.setText(Integer.toString(value));
		}
	}

	public void drawCheckEngine(boolean value) {
		if (mImageCheckEngine != null)
			mImageCheckEngine.setVisibility(value ? View.VISIBLE : View.INVISIBLE);
		if (mViewCheckEngine != null) {
			if (value)
				mViewCheckEngine.setText("E");
			else
				mViewCheckEngine.setText("-");
		}
	}

	public void drawOdometerImperial(int value) {
		// value is miles * 100
		float miles = value / 100f;
		if (mViewOdometerImperial != null)
			mViewOdometerImperial.setText(String.format("%.2f", miles));
		if (mGaugeSpeedImperial != null)
			mGaugeSpeedImperial.setOdoValue(miles);
	}

	public void drawOdometerMetric(int value) {
		// value is km * 100
		float km = value / 100f;
		if (mViewOdometerMetric != null)
			mViewOdometerMetric.setText(String.format("%4.2f", km));
		if (mGaugeSpeedMetric != null)
			mGaugeSpeedMetric.setOdoValue(km);
	}

	public void drawFuelImperial(int value) {
		// value is in gallons * 1000
		float gallons = value / 1000f;
		if (mViewFuelImperial != null)
			mViewFuelImperial.setText(String.format("%5.3f", gallons));
	}

	public void drawFuelMetric(int value) {
		// value is in milliliters
		if (mViewFuelMetric != null)
			mViewFuelMetric.setText(Integer.toString(value));
	}

	private float lastFuelAvgImperial = -1;
	private float lastFuelInstImperial = -1;
	private float lastFuelAvgMetric = -1;
	private float lastFuelInstMetric = -1;

	public void drawFuelAvgImperial(int value) {
		// value is in MPG * 100
		if (value == -1)
			lastFuelAvgImperial = -1;
		else
			lastFuelAvgImperial = value / 100f;
		if (mViewFuelAvgImperial != null) {
			if (lastFuelAvgImperial == -1)
				mViewFuelAvgImperial.setText("-");
			else
				mViewFuelAvgImperial.setText(String.format("%4.2f", lastFuelAvgImperial));
		}
		drawMileageImperial();
	}

	public void drawFuelAvgMetric(int value) {
		// value is in l / 100 km * 100
		if (value == -1)
			lastFuelAvgMetric = -1;
		else
			lastFuelAvgMetric = value / 100f;
		if (mViewFuelAvgMetric != null) {
			if (lastFuelAvgMetric == -1)
				mViewFuelAvgMetric.setText("-");
			else
				mViewFuelAvgMetric.setText(String.format("%4.2f", lastFuelAvgMetric));
		}
		drawMileageMetric();
	}

	public void drawFuelInstImperial(int value) {
		// value is in MPG * 100
		if (value == -1)
			lastFuelInstImperial = -1;
		else
			lastFuelInstImperial = value / 100f;
		if (mViewFuelInstImperial != null) {
			if (lastFuelInstImperial == -1)
				mViewFuelInstImperial.setText("-");
			else
				mViewFuelInstImperial.setText(String.format("%4.2f", lastFuelInstImperial));
		}
		drawMileageImperial();
	}

	public void drawFuelInstMetric(int value) {
		// value is in l / 100 km * 100
		if (value == -1)
			lastFuelInstMetric = -1;
		else
			lastFuelInstMetric = value / 100f;
		if (mViewFuelInstMetric != null) {
			if (lastFuelInstMetric == -1)
				mViewFuelInstMetric.setText("-");
			else
				mViewFuelInstMetric.setText(String.format("%4.2f", lastFuelInstMetric));
		}
		drawMileageMetric();
	}

	private void drawMileageImperial() {
		if (mViewMileageImperial != null) {
			String s;
			if (lastFuelInstImperial == -1)
				s = "-";
			else
				s = String.format("%3.1f",  lastFuelInstImperial);
			s += " / ";
			if (lastFuelAvgImperial == -1)
				s += "-";
			else
				s += String.format("%3.1f",  lastFuelAvgImperial);
			mViewMileageImperial.setText(s);
		}
	}

	private void drawMileageMetric() {
		if (mViewMileageMetric != null) {
			String s;
			if (lastFuelInstMetric == -1)
				s = "-";
			else
				s = String.format("%3.1f",  lastFuelInstMetric);
			s += " / ";
			if (lastFuelAvgMetric == -1)
				s += "-";
			else
				s += String.format("%3.1f",  lastFuelAvgMetric);
			mViewMileageMetric.setText(s);
		}
	}

	static class HarleyDroidDashboardViewHandler extends Handler {
		private final WeakReference<HarleyDroidDashboardView> mHarleyDroidDashboardView;

	    HarleyDroidDashboardViewHandler(HarleyDroidDashboardView harleyDroidDashboardView) {
	        mHarleyDroidDashboardView = new WeakReference<HarleyDroidDashboardView>(harleyDroidDashboardView);
	    }

		@Override
		public void handleMessage(Message msg) {
			HarleyDroidDashboardView hddv = mHarleyDroidDashboardView.get();
			if (hddv != null)
				hddv.handleMessage(msg);
		}
	}
}
