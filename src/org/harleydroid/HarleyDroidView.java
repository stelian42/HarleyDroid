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

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class HarleyDroidView implements HarleyDataListener
{    
	private static final boolean D = false;
	private static final String TAG = HarleyDroid.class.getSimpleName();

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

    private Activity mActivity;
    
	// Views references cached for performance
    private View mViewText;
    private View mViewGraphic;
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
 
    public HarleyDroidView(Activity activity) {
    	mActivity = activity;
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
 
   	public void onBadCRCChanged(byte[] buffer) {
   		Log.d(TAG, "onBadCRC(" + new String(buffer) + ")");
   	}
  
   	public void onUnknownChanged(byte[] buffer) {
   		Log.d(TAG, "onUnknown(" + new String(buffer) + ")");
   	}
  
    public void drawAll(HarleyData hd, boolean modeText, boolean unitMetric) {
    	
    	mViewGraphic = mActivity.findViewById(R.id.graphic_layout);
		mViewText = mActivity.findViewById(R.id.text_layout);
        mViewRpm = (TextView) mActivity.findViewById(R.id.rpm_field);
        mGaugeRpm = (Gauge) mActivity.findViewById(R.id.rpm_meter);
        mLabelSpeedMetric = (TextView) mActivity.findViewById(R.id.speed_metric_label);
        mLabelSpeedImperial = (TextView) mActivity.findViewById(R.id.speed_imperial_label);
        mViewSpeedMetric = (TextView) mActivity.findViewById(R.id.speed_metric_field);
        mViewSpeedImperial = (TextView) mActivity.findViewById(R.id.speed_imperial_field);
        mGaugeSpeedMetric = (Gauge) mActivity.findViewById(R.id.speed_metric_meter);
        mGaugeSpeedImperial = (Gauge) mActivity.findViewById(R.id.speed_imperial_meter);
        mLabelEngTempMetric = (TextView) mActivity.findViewById(R.id.enginetemp_metric_label);
        mLabelEngTempImperial = (TextView) mActivity.findViewById(R.id.enginetemp_imperial_label);
        mViewEngTempMetric = (TextView) mActivity.findViewById(R.id.enginetemp_metric_field);
        mViewEngTempImperial = (TextView) mActivity.findViewById(R.id.enginetemp_imperial_field);
        mViewFuelGauge = (TextView) mActivity.findViewById(R.id.fuelgauge_field);
        mImageTurnSignalsLeft = (View) mActivity.findViewById(R.id.turn_left);
        mImageTurnSignalsRight = (View) mActivity.findViewById(R.id.turn_right);
        mViewTurnSignals = (TextView) mActivity.findViewById(R.id.turnsignals_field);
        mViewNeutral = (TextView) mActivity.findViewById(R.id.neutral_field);
        mViewClutch = (TextView) mActivity.findViewById(R.id.clutch_field);
        mViewGear = (TextView) mActivity.findViewById(R.id.gear_field);
        mViewCheckEngine = (TextView) mActivity.findViewById(R.id.checkengine_field);
        mImageCheckEngine = (View) mActivity.findViewById(R.id.check_engine);
        mLabelOdometerMetric = (TextView) mActivity.findViewById(R.id.odometer_metric_label);
        mLabelOdometerImperial = (TextView) mActivity.findViewById(R.id.odometer_imperial_label);
        mViewOdometerMetric = (TextView) mActivity.findViewById(R.id.odometer_metric_field);
        mViewOdometerImperial = (TextView) mActivity.findViewById(R.id.odometer_imperial_field);
        mLabelFuelMetric = (TextView) mActivity.findViewById(R.id.fuel_metric_label);
        mLabelFuelImperial = (TextView) mActivity.findViewById(R.id.fuel_imperial_label);
        mViewFuelMetric = (TextView) mActivity.findViewById(R.id.fuel_metric_field);
        mViewFuelImperial = (TextView) mActivity.findViewById(R.id.fuel_imperial_field);
        
        if (modeText) {
    		mViewGraphic.setVisibility(View.GONE);
        	mViewText.setVisibility(View.VISIBLE);
    	}
    	else {
    		mViewText.setVisibility(View.GONE);
        	mViewGraphic.setVisibility(View.VISIBLE);
    	}
        
        if (unitMetric) {
    		mGaugeSpeedImperial.setVisibility(View.GONE);
    		mGaugeSpeedMetric.setVisibility(View.VISIBLE);
    		mLabelSpeedImperial.setVisibility(View.GONE);
    		mLabelSpeedMetric.setVisibility(View.VISIBLE);
    		mViewSpeedImperial.setVisibility(View.GONE);
    		mViewSpeedMetric.setVisibility(View.VISIBLE);
    		mLabelEngTempImperial.setVisibility(View.GONE);
    		mLabelEngTempMetric.setVisibility(View.VISIBLE);
    		mViewEngTempImperial.setVisibility(View.GONE);
    		mViewEngTempMetric.setVisibility(View.VISIBLE);
    		mLabelOdometerImperial.setVisibility(View.GONE);
    		mLabelOdometerMetric.setVisibility(View.VISIBLE);
    		mViewOdometerImperial.setVisibility(View.GONE);
    		mViewOdometerMetric.setVisibility(View.VISIBLE);
    		mLabelFuelImperial.setVisibility(View.GONE);
    		mLabelFuelMetric.setVisibility(View.VISIBLE);
    		mViewFuelImperial.setVisibility(View.GONE);
    		mViewFuelMetric.setVisibility(View.VISIBLE);
    	} else {
    		mGaugeSpeedMetric.setVisibility(View.GONE);
    		mGaugeSpeedImperial.setVisibility(View.VISIBLE);
    		mLabelSpeedMetric.setVisibility(View.GONE);
    		mLabelSpeedImperial.setVisibility(View.VISIBLE);
    		mViewSpeedMetric.setVisibility(View.GONE);
    		mViewSpeedImperial.setVisibility(View.VISIBLE);
    		mLabelEngTempMetric.setVisibility(View.GONE);
    		mLabelEngTempImperial.setVisibility(View.VISIBLE);
    		mViewEngTempMetric.setVisibility(View.GONE);
    		mViewEngTempImperial.setVisibility(View.VISIBLE);
    		mLabelOdometerMetric.setVisibility(View.GONE);
    		mLabelOdometerImperial.setVisibility(View.VISIBLE);
    		mViewOdometerMetric.setVisibility(View.GONE);
    		mViewOdometerImperial.setVisibility(View.VISIBLE);
    		mLabelFuelMetric.setVisibility(View.GONE);
    		mLabelFuelImperial.setVisibility(View.VISIBLE);
    		mViewFuelMetric.setVisibility(View.GONE);
    		mViewFuelImperial.setVisibility(View.VISIBLE);
    	}
    	
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
        }
    }

    public void drawRPM(int value) {
    	mViewRpm.setText(Integer.toString(value));
        mGaugeRpm.setValue(value / 100);
    }
    
    public void drawSpeedImperial(int value) {
    	// value is in mph
        mViewSpeedImperial.setText(Integer.toString(value));
        mGaugeSpeedImperial.setValue(value);
    }

    public void drawSpeedMetric(int value) {
    	// value is in km/h
    	mViewSpeedMetric.setText(Integer.toString(value));
        mGaugeSpeedMetric.setValue(value);
    }
   
    public void drawEngineTempImperial(int value) {
    	// value is in F
    	mViewEngTempImperial.setText(Integer.toString(value));
    }

    public void drawEngineTempMetric(int value) {
    	// value is in C
    	mViewEngTempMetric.setText(Integer.toString(value));
    }
   
    public void drawFuelGauge(int value) {
    	mViewFuelGauge.setText(Integer.toString(value));
    }
   
    public void drawTurnSignals(int value) {
    	if ((value & 0x03) == 0x03) {
    		mImageTurnSignalsLeft.setVisibility(View.VISIBLE);
    		mImageTurnSignalsRight.setVisibility(View.VISIBLE);
    		mViewTurnSignals.setText("W");
    	}
    	else if ((value & 0x01) == 0x01) {
    		mImageTurnSignalsLeft.setVisibility(View.INVISIBLE);
    		mImageTurnSignalsRight.setVisibility(View.VISIBLE);
    		mViewTurnSignals.setText("R");
    	}
    	else if ((value & 0x02) == 0x02) {
    		mImageTurnSignalsLeft.setVisibility(View.VISIBLE);
    		mImageTurnSignalsRight.setVisibility(View.INVISIBLE);
    		mViewTurnSignals.setText("L");
    	}
    	else {
    		mImageTurnSignalsLeft.setVisibility(View.INVISIBLE);
    		mImageTurnSignalsRight.setVisibility(View.INVISIBLE);
    		mViewTurnSignals.setText("");
    	}
    }
    
    public void drawNeutral(int value) {
    	mViewNeutral.setText(Integer.toString(value));
    }
    
    public void drawClutch(int value) {
    	mViewClutch.setText(Integer.toString(value));
    }
    
    public void drawGear(int value) {
    	mViewGear.setText(Integer.toString(value));
    }
    
    public void drawCheckEngine(int value) {
    	mImageCheckEngine.setVisibility(value == 0 ? View.INVISIBLE : View.VISIBLE);
    	mViewCheckEngine.setText(Integer.toString(value));
    }
    
    public void drawOdometerImperial(int value) {
    	// value is miles * 100
    	float miles = value / 100f;
    	mViewOdometerImperial.setText(String.format("%.2f", miles));
    	mGaugeSpeedImperial.setOdoValue(miles);
    }

    public void drawOdometerMetric(int value) {
    	// value is km * 100
	float km = value / 100f;
    	mViewOdometerMetric.setText(String.format("%4.2f", km));
    	mGaugeSpeedMetric.setOdoValue(km);
    }
    
    public void drawFuelImperial(int value) {
    	// value is in fl oz
    	mViewFuelImperial.setText(Float.toString(value));
    }

    public void drawFuelMetric(int value) {
    	// value is in milliliters
    	mViewFuelMetric.setText(Integer.toString(value));
    }
}