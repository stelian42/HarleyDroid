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

import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class HarleyDroidView implements HarleyDataListener
{
	private static final boolean D = true;
	private static final String TAG = HarleyDroidView.class.getSimpleName();

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
	public static final int UPDATE_VIN = 16;
	public static final int UPDATE_ECMPN = 17;
	public static final int UPDATE_ECMCALID = 18;
	public static final int UPDATE_ECMSWLEVEL = 19;
	public static final int UPDATE_HISTORICDTC = 20;
	public static final int UPDATE_CURRENTDTC = 21;

	public static final int VIEW_GRAPHIC = 1;
	public static final int VIEW_TEXT = 2;
	public static final int VIEW_DIAGNOSTIC = 3;

	private Activity mActivity;
	
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
	private TextView mViewVIN;
	private TextView mViewECMPN;
	private TextView mViewECMCalID;
	private TextView mViewECMSWLevel;
	private ListView mViewCurrentDTC;
	private ListView mViewHistoricDTC;

	public HarleyDroidView(Activity activity) {
		mActivity = activity;
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
			mViewNeutral = null;
			mViewClutch = null;
			mViewGear = null;
			mViewCheckEngine = null;
			mLabelOdometerMetric = null;
			mLabelOdometerImperial = null;
			mViewOdometerMetric = null;
			mViewOdometerImperial = null;
			mLabelFuelMetric = null;
			mLabelFuelImperial = null;
			mViewFuelMetric = null;
			mViewFuelImperial = null;

			mViewVIN = null;
			mViewECMPN = null;
			mViewECMCalID = null;
			mViewECMSWLevel = null;
			mViewCurrentDTC = null;
			mViewHistoricDTC = null;

			if (unitMetric) {
				mGaugeSpeedImperial.setVisibility(View.GONE);
				mGaugeSpeedMetric.setVisibility(View.VISIBLE);
			} else {
				mGaugeSpeedMetric.setVisibility(View.GONE);
				mGaugeSpeedImperial.setVisibility(View.VISIBLE);
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
			}
			
			break;
		case VIEW_DIAGNOSTIC:
			if (portrait)
				view = R.layout.portrait_diag;
			else
				view = R.layout.landscape_diag;
			mActivity.setContentView(view);

			mGaugeSpeedMetric = null;
			mGaugeSpeedImperial = null;
			mGaugeRpm = null;
			mImageTurnSignalsLeft = null;
			mImageCheckEngine = null;
			mImageTurnSignalsRight = null;

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
			mViewNeutral = null;
			mViewClutch = null;
			mViewGear = null;
			mViewCheckEngine = null;
			mLabelOdometerMetric = null;
			mLabelOdometerImperial = null;
			mViewOdometerMetric = null;
			mViewOdometerImperial = null;
			mLabelFuelMetric = null;
			mLabelFuelImperial = null;
			mViewFuelMetric = null;
			mViewFuelImperial = null;

			mViewVIN = (TextView) mActivity.findViewById(R.id.vin_field);
			mViewECMPN = (TextView) mActivity.findViewById(R.id.ecmpn_field);
			mViewECMCalID = (TextView) mActivity.findViewById(R.id.ecmcalid_field);
			mViewECMSWLevel = (TextView) mActivity.findViewById(R.id.ecmswlevel_field);
			mViewCurrentDTC = (ListView) mActivity.findViewById(R.id.currentdtc_field);
			mViewHistoricDTC = (ListView) mActivity.findViewById(R.id.historicdtc_field);

			break;
		}
	}
	
	private final Handler mHandler = new Handler() {
		@Override
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
				drawFuelGauge(msg.arg1);
				break;
			case UPDATE_TURNSIGNALS:
				drawTurnSignals(msg.arg1);
				break;
			case UPDATE_NEUTRAL:
				drawNeutral(msg.arg1);
				break;
			case UPDATE_CLUTCH:
				drawClutch(msg.arg1);
				break;
			case UPDATE_GEAR:
				drawGear(msg.arg1);
				break;
			case UPDATE_CHECKENGINE:
				drawCheckEngine(msg.arg1);
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
			case UPDATE_VIN:
				drawVIN((String)msg.getData().get("vin"));
				break;
			case UPDATE_ECMPN:
				drawECMPN((String)msg.getData().get("ecmpn"));
				break;
			case UPDATE_ECMCALID:
				drawECMCalID((String)msg.getData().get("ecmcalid"));
				break;
			case UPDATE_ECMSWLEVEL:
				drawECMSWLevel(msg.arg1);
				break;
			case UPDATE_HISTORICDTC:
				drawHistoricDTC((int[])msg.getData().get("historicdtc"));
				break;
			case UPDATE_CURRENTDTC:
				drawCurrentDTC((int[])msg.getData().get("currentdtc"));
				break;
			}
		}
	};

	public void onRPMChanged(int rpm) {
		mHandler.obtainMessage(HarleyDroidView.UPDATE_RPM, rpm, -1).sendToTarget();
	}

	public void onSpeedImperialChanged(int speed) {
		mHandler.obtainMessage(HarleyDroidView.UPDATE_SPEED_IMPERIAL, speed, -1).sendToTarget();
	}

	public void onSpeedMetricChanged(int speed) {
		mHandler.obtainMessage(HarleyDroidView.UPDATE_SPEED_METRIC, speed, -1).sendToTarget();
	}

	public void onEngineTempImperialChanged(int engineTemp) {
		mHandler.obtainMessage(HarleyDroidView.UPDATE_ENGINETEMP_IMPERIAL, engineTemp, -1).sendToTarget();
	}

	public void onEngineTempMetricChanged(int engineTemp) {
		mHandler.obtainMessage(HarleyDroidView.UPDATE_ENGINETEMP_METRIC, engineTemp, -1).sendToTarget();
	}

	public void onFuelGaugeChanged(int full) {
		mHandler.obtainMessage(HarleyDroidView.UPDATE_FUELGAUGE, full, -1).sendToTarget();
	}

	public void onTurnSignalsChanged(int turnSignals) {
		mHandler.obtainMessage(HarleyDroidView.UPDATE_TURNSIGNALS, turnSignals, -1).sendToTarget();
	}

	public void onNeutralChanged(boolean neutral) {
		mHandler.obtainMessage(HarleyDroidView.UPDATE_NEUTRAL, neutral ? 1 : 0, -1).sendToTarget();
	}

	public void onClutchChanged(boolean clutch) {
		mHandler.obtainMessage(HarleyDroidView.UPDATE_CLUTCH, clutch ? 1 : 0, -1).sendToTarget();
	}

	public void onGearChanged(int gear) {
		mHandler.obtainMessage(HarleyDroidView.UPDATE_GEAR, gear, -1).sendToTarget();
	}

	public void onCheckEngineChanged(boolean checkEngine) {
		mHandler.obtainMessage(HarleyDroidView.UPDATE_CHECKENGINE, checkEngine ? 1 : 0, -1).sendToTarget();
	}

	public void onOdometerImperialChanged(int odometer) {
		mHandler.obtainMessage(HarleyDroidView.UPDATE_ODOMETER_IMPERIAL, odometer, -1).sendToTarget();
	}

	public void onOdometerMetricChanged(int odometer) {
		mHandler.obtainMessage(HarleyDroidView.UPDATE_ODOMETER_METRIC, odometer, -1).sendToTarget();
	}

	public void onFuelImperialChanged(int fuel) {
		mHandler.obtainMessage(HarleyDroidView.UPDATE_FUEL_IMPERIAL, fuel, -1).sendToTarget();
	}

	public void onFuelMetricChanged(int fuel) {
		mHandler.obtainMessage(HarleyDroidView.UPDATE_FUEL_METRIC, fuel, -1).sendToTarget();
	}

	public void onVINChanged(String vin) {
		Message m = mHandler.obtainMessage(HarleyDroidView.UPDATE_VIN);
		Bundle b = new Bundle();
		b.putString("vin", vin);
		m.setData(b);
		m.sendToTarget();
	}

	public void onECMPNChanged(String ecmPN) {
		Message m = mHandler.obtainMessage(HarleyDroidView.UPDATE_ECMPN);
		Bundle b = new Bundle();
		b.putString("ecmpn", ecmPN);
		m.setData(b);
		m.sendToTarget();
	}

	public void onECMCalIDChanged(String ecmCalID) {
		Message m = mHandler.obtainMessage(HarleyDroidView.UPDATE_ECMCALID);
		Bundle b = new Bundle();
		b.putString("ecmcalid", ecmCalID);
		m.setData(b);
		m.sendToTarget();
	}

	public void onECMSWLevelChanged(int ecmSWLevel) {
		mHandler.obtainMessage(HarleyDroidView.UPDATE_ECMSWLEVEL, ecmSWLevel, -1).sendToTarget();
	}

	public void onHistoricDTCChanged(int[] dtc) {
		Message m = mHandler.obtainMessage(HarleyDroidView.UPDATE_HISTORICDTC);
		Bundle b = new Bundle();
		b.putIntArray("historicdtc", dtc);
		m.setData(b);
		m.sendToTarget();
	}

	public void onCurrentDTCChanged(int[] dtc) {
		Message m = mHandler.obtainMessage(HarleyDroidView.UPDATE_CURRENTDTC);
		Bundle b = new Bundle();
		b.putIntArray("currentdtc", dtc);
		m.setData(b);
		m.sendToTarget();
	}

	public void onBadCRCChanged(byte[] buffer) {
		Log.d(TAG, "onBadCRC(" + new String(buffer) + ")");
	}

	public void onUnknownChanged(byte[] buffer) {
		Log.d(TAG, "onUnknown(" + new String(buffer) + ")");
	}

	public void onRawChanged(byte[] buffer) {
		Log.d(TAG, "onRaw(" + new String(buffer) + ")");
	}

	public void drawAll(HarleyData hd) {

		if (hd != null) {
			drawRPM(hd.getRPM());
			drawSpeedImperial(hd.getSpeedImperial());
			drawSpeedMetric(hd.getSpeedMetric());
			drawEngineTempImperial(hd.getEngineTempImperial());
			drawEngineTempMetric(hd.getEngineTempMetric());
			drawFuelGauge(hd.getFuelGauge());
			drawTurnSignals(hd.getTurnSignals());
			drawNeutral(hd.getNeutral() ? 1 : 0);
			drawClutch(hd.getClutch() ? 1 : 0);
			drawGear(hd.getGear());
			drawCheckEngine(hd.getCheckEngine() ? 1 : 0);
			drawOdometerImperial(hd.getOdometerImperial());
			drawOdometerMetric(hd.getOdometerMetric());
			drawFuelImperial(hd.getFuelImperial());
			drawFuelMetric(hd.getFuelMetric());
			drawVIN(hd.getVIN());
			drawECMPN(hd.getECMPN());
			drawECMCalID(hd.getECMCalID());
			drawECMSWLevel(hd.getECMSWLevel());
			drawHistoricDTC(hd.getHistoricDTC());
			drawCurrentDTC(hd.getCurrentDTC());
		} else {
			drawRPM(0);
			drawSpeedImperial(0);
			drawSpeedMetric(0);
			drawEngineTempImperial(0);
			drawEngineTempMetric(0);
			drawFuelGauge(0);
			drawTurnSignals(0);
			drawNeutral(0);
			drawClutch(0);
			drawGear(0);
			drawCheckEngine(0);
			drawOdometerImperial(0);
			drawOdometerMetric(0);
			drawFuelImperial(0);
			drawFuelMetric(0);
			drawVIN("N/A");
			drawECMPN("N/A");
			drawECMCalID("N/A");
			drawECMSWLevel(0);
			drawHistoricDTC(null);
			drawCurrentDTC(null);
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

	public void drawFuelGauge(int value) {
		if (mViewFuelGauge != null)
			mViewFuelGauge.setText(Integer.toString(value));
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
				mViewTurnSignals.setText("");
		}
	}

	public void drawNeutral(int value) {
		if (mViewNeutral != null)
			mViewNeutral.setText(Integer.toString(value));
	}

	public void drawClutch(int value) {
		if (mViewClutch != null)
			mViewClutch.setText(Integer.toString(value));
	}

	public void drawGear(int value) {
		if (mViewGear != null)
			mViewGear.setText(Integer.toString(value));
	}

	public void drawCheckEngine(int value) {
		if (mImageCheckEngine != null)
			mImageCheckEngine.setVisibility(value == 0 ? View.INVISIBLE : View.VISIBLE);
		if (mViewCheckEngine != null)
			mViewCheckEngine.setText(Integer.toString(value));
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
		// value is in fl oz
		if (mViewFuelImperial != null)
			mViewFuelImperial.setText(Float.toString(value));
	}

	public void drawFuelMetric(int value) {
		// value is in milliliters
		if (mViewFuelMetric != null)
			mViewFuelMetric.setText(Integer.toString(value));
	}

	public void drawVIN(String value) {
		if (mViewVIN != null)
			mViewVIN.setText(value);
	}

	public void drawECMPN(String value) {
		if (mViewECMPN != null)
			mViewECMPN.setText(value);
	}

	public void drawECMCalID(String value) {
		if (mViewECMCalID != null)
			mViewECMCalID.setText(value);
	}

	public void drawECMSWLevel(int value) {
		if (mViewECMSWLevel != null)
			mViewECMSWLevel.setText("0x" + Integer.toString(value, 16));
	}

	public void drawHistoricDTC(int[] dtc) {
		Log.d("DTC", "drawHistoric");

		if (mViewHistoricDTC == null)
			return;
		
		// arrayAdapter.notifyDataSetChanged();
		ArrayList<Integer> items = new ArrayList<Integer>();
		if (dtc != null)
			for (int i = 0; i < dtc.length; i++)
				items.add(new Integer(dtc[i]));
		mViewHistoricDTC.setAdapter(new ArrayAdapter<Integer>(mActivity, R.layout.dtc_item, items));
	}

	public void drawCurrentDTC(int[] dtc) {
		Log.d("DTC", "drawCurrent");
		
		if (mViewCurrentDTC == null)
			return;
		
		ArrayList<Integer> items = new ArrayList<Integer>();
		if (dtc != null)
			for (int i = 0; i < dtc.length; i++)
				items.add(new Integer(dtc[i]));
		mViewCurrentDTC.setAdapter(new ArrayAdapter<Integer>(mActivity, R.layout.dtc_item, items));
	}
}
