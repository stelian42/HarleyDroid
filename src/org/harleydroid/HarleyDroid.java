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
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

public class HarleyDroid extends Activity implements ServiceConnection
{
	private static final boolean D = true;
	private static final String TAG = HarleyDroid.class.getSimpleName();
	public static final boolean EMULATOR = false;
	
    static final int CONNECTING_TO_ELM327 = 1;

    // Message types sent from HarleyDroidService
    public static final int STATUS_ERROR = 1;
    public static final int STATUS_ERRORAT = 2;
    public static final int STATUS_CONNECTED = 3;
    public static final int STATUS_NODATA = 4;
    public static final int STATUS_TOOMANYERRORS = 5;
    public static final int UPDATE_RPM = 6;
    public static final int UPDATE_SPEED = 7;
    public static final int UPDATE_ENGINETEMP = 8;
    public static final int UPDATE_FULL = 9;
    public static final int UPDATE_TURNSIGNALS = 10;
    public static final int UPDATE_NEUTRAL = 11;
    public static final int UPDATE_CLUTCH = 12;
    public static final int UPDATE_GEAR = 13;
    public static final int UPDATE_CHECKENGINE = 14;
    public static final int UPDATE_ODOMETER = 15;
    public static final int UPDATE_FUEL = 16;

    private static final int REQUEST_ENABLE_BT = 2;
    
    private BluetoothAdapter mBluetoothAdapter = null;
    private Menu mOptionsMenu = null;
    private String mBluetoothID = null;
    private boolean mLogging = false;
    private HarleyDroidService mService = null;
    private boolean mModeRaw = false;
    private boolean mUnitMetric = false;
    private int mOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
    private int mCurrentOdoValue = 0;
    private int mResetOdoValue = 0;
    
    // Views references cached for performance
    private View mViewRaw;
    private View mViewGr;
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
    private TextView mViewFull;
    private TextView mViewTurnSignals;
    private TextView mViewNeutral;
    private TextView mViewClutch;
    private TextView mViewGear;
    private TextView mViewCheckEngine;
    private TextView mLabelOdometerMetric;
    private TextView mLabelOdometerImperial;
    private TextView mViewOdometerMetric;
    private TextView mViewOdometerImperial;
    private TextView mLabelFuelMetric;
    private TextView mLabelFuelImperial;
    private TextView mViewFuelMetric;
    private TextView mViewFuelImperial;
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
    	if (D) Log.d(TAG, "onCreate()");
    	
        super.onCreate(savedInstanceState);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
			setContentView(R.layout.portrait);
		else
			setContentView(R.layout.landscape);
                
        if (savedInstanceState != null) {
        	mModeRaw = savedInstanceState.getBoolean("moderaw");
        	if (D) Log.d(TAG, "savedInstanceState: mModeRaw = " + mModeRaw);
        	
        }
            
        // enable Bluetooth if necessary
        if (!EMULATOR) {
        	mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        	if (mBluetoothAdapter == null) {
        		Toast.makeText(this, R.string.nobluetooth, Toast.LENGTH_LONG).show();
        		finish();
        		return;
        	}
        	if (!mBluetoothAdapter.isEnabled()) {
        		Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        		startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        	}
        }
    }
    
    @Override
    public void onDestroy() {
    	if (D) Log.d(TAG, "onDestroy()");
    	super.onDestroy();
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
    	if (D) Log.d(TAG, "onSaveInstanceState(mModeRaw= " + mModeRaw + ")");
    	
    	outState.putBoolean("moderaw", mModeRaw);
    }
    
    @Override
    public void onStart() {
    	if (D) Log.d(TAG, "onStart()");
    	super.onStart();

    	// get preferences which may have been changed
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
    	mBluetoothID = prefs.getString("bluetoothid", null);
    	if (prefs.getString("orientation", "auto").equals("auto"))
    		mOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
    	else if (prefs.getString("orientation", "auto").equals("portrait")) {
    		mOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
    		setContentView(R.layout.portrait);
    	}
    	else if (prefs.getString("orientation", "auto").equals("landscape")) {
    		mOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
    		setContentView(R.layout.landscape);
    	}
    	this.setRequestedOrientation(mOrientation);
    	
    	mLogging = false;
    	if (prefs.getBoolean("logging", false)) {
        	if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))
        		Toast.makeText(this, R.string.nologging, Toast.LENGTH_LONG).show();
        	else
        		mLogging = true;  	
        }
    	if (prefs.getBoolean("screenon", false)) 
    		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    	if (prefs.getString("unit", "metric").equals("metric"))
    		mUnitMetric = true;
    	else
    		mUnitMetric = false;
    		   	
    	drawLayout();
            
    	// bind to the service
    	bindService(new Intent(this, HarleyDroidService.class), this, 0);	
    }
 
    @Override
    public void onStop() {
    	if (D) Log.d(TAG, "onStop()");
    	super.onStop();

    	unbindService(this);
    	mService = null;
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    	if (D) Log.d(TAG, "onConfigurationChanged()");
    	super.onConfigurationChanged(newConfig);
    	
    	if (mOrientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
    		Log.d(TAG, "orientation now " + newConfig.orientation);
    		if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT)
    			setContentView(R.layout.portrait);
    		else
    			setContentView(R.layout.landscape);
    		drawLayout();
    	}
    }

    @Override
    protected Dialog onCreateDialog(int id) {
    	if (D) Log.d(TAG, "onCreateDialog()");
    	
    	switch (id) {
    	case CONNECTING_TO_ELM327:
    		ProgressDialog pd = new ProgressDialog(this);
    		pd.setMessage(getText(R.string.connectingelm327));
    		pd.setIndeterminate(true);
    		return pd;
    	}
    	return null;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	if (D) Log.d(TAG, "onCreateOptionsMenu()");
    	
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        mOptionsMenu = menu;    
        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	if (D) Log.d(TAG, "onPrepareOptionsMenu()");
    	
    	if (mService != null && mService.isRunning()) {
    		mOptionsMenu.findItem(R.id.capture_menu).setIcon(R.drawable.ic_menu_stop);
    		mOptionsMenu.findItem(R.id.capture_menu).setTitle(R.string.stopcapture_label);
    	}
    	else {
    		mOptionsMenu.findItem(R.id.capture_menu).setIcon(R.drawable.ic_menu_play_clip);
    		mOptionsMenu.findItem(R.id.capture_menu).setTitle(R.string.startcapture_label);
    	}
    	if (mModeRaw)
    		mOptionsMenu.findItem(R.id.mode_menu).setTitle(R.string.mode_labelgr);
    	else
    		mOptionsMenu.findItem(R.id.mode_menu).setTitle(R.string.mode_labelraw);
    		
		return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	if (D) Log.d(TAG, "onOptionsItemSelected");
    	
        switch (item.getItemId()) {
        case R.id.capture_menu:
        	if (mService != null && mService.isRunning())
        		stopCapture();
        	else
        		startCapture();
            return true;
        case R.id.mode_menu:
        	mModeRaw = !mModeRaw;
        	drawLayout();
        	return true;
        case R.id.preferences_menu:
        	Intent settingsActivity = new Intent(getBaseContext(), HarleyDroidSettings.class);
        	startActivity(settingsActivity);
        	return true;
        case R.id.resetodo_menu:
        	mResetOdoValue = mCurrentOdoValue;
        	drawOdometer(mCurrentOdoValue);
        	return true;
        case R.id.quit_menu:
        	stopCapture();
        	finish();
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if (D) Log.d(TAG, "onActivityResult");
    	
        switch (requestCode) {
        case REQUEST_ENABLE_BT:
            if (resultCode != Activity.RESULT_OK) {
            	Toast.makeText(this, R.string.noenablebluetooth, Toast.LENGTH_LONG).show();
            	finish();
            }
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
    	if (D) Log.d(TAG, "onServiceConnected()");
    	
		mService = ((HarleyDroidService.HarleyDroidServiceBinder)service).getService();
		mService.setHandler(mHandler);
		
		if (mService.isRunning())
			return;
		
		showDialog(CONNECTING_TO_ELM327);
		
    	if (!EMULATOR)
    		mService.startService(mBluetoothAdapter.getRemoteDevice(mBluetoothID), mLogging);
    	else
    		mService.startService(null, mLogging);
	}
    
    private void startCapture() {
    	if (D) Log.d(TAG, "startCapture()");
    	
    	mCurrentOdoValue = 0;
    	mResetOdoValue = 0;
    	startService(new Intent(this, HarleyDroidService.class));
    	bindService(new Intent(this, HarleyDroidService.class), this, 0);
    }
    
    private void stopCapture() {
    	if (D) Log.d(TAG, "stopCapture()");
    	
    	if (mService == null)
    		return;
    	mService.stopService();
    	unbindService(this);
    	stopService(new Intent(this, HarleyDroidService.class));
    	mService = null;
    	// ugly, but we unbind() in onStop()...
    	bindService(new Intent(this, HarleyDroidService.class), this, 0);
    }
    
    @Override
    public void onServiceDisconnected(ComponentName name) {
    	if (D) Log.d(TAG, "onServiceDisconnected()");
    	
    	unbindService(this);
		mService = null;
		// ugly, but we unbind() in onStop()...
		bindService(new Intent(this, HarleyDroidService.class), this, 0);
	}
	
    private final Handler mHandler = new Handler() {
    	@Override
    	public void handleMessage(Message msg) {
        	if (D) Log.d(TAG, "handleMessage " + msg.what);

    		switch (msg.what) {
    			case STATUS_ERROR: 
    				dismissDialog(CONNECTING_TO_ELM327);
    				Toast.makeText(getApplicationContext(), R.string.errorconnecting, Toast.LENGTH_LONG).show();
    				break;
    			case STATUS_ERRORAT: 
    				dismissDialog(CONNECTING_TO_ELM327);
    				Toast.makeText(getApplicationContext(), R.string.errorat, Toast.LENGTH_LONG).show();
    				break;
    			case STATUS_CONNECTED:
    				dismissDialog(CONNECTING_TO_ELM327);
    				break;
    			case STATUS_NODATA:
    				Toast.makeText(getApplicationContext(), R.string.errornodata, Toast.LENGTH_LONG).show();
    				break;
    			case STATUS_TOOMANYERRORS:
    				Toast.makeText(getApplicationContext(), R.string.errortoomany, Toast.LENGTH_LONG).show();
    				break;
    			case UPDATE_RPM:
    				drawRPM(msg.arg1);
    				break;
    			case UPDATE_SPEED:
    				drawSpeed(msg.arg1);
    				break;
    			case UPDATE_ENGINETEMP:
    				drawEngineTemp(msg.arg1);
    				break;
    			case UPDATE_FULL:
    				drawFull(msg.arg1);
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
    			case UPDATE_ODOMETER:
    				drawOdometer(msg.arg1);
    				break;
    			case UPDATE_FUEL:
    				drawFuel(msg.arg1);
    				break;
       		}
    	}
    };
   
    private void drawLayout() {
    	
    	mViewGr = findViewById(R.id.gr_layout);
		mViewRaw = findViewById(R.id.raw_layout);
        mViewRpm = (TextView) findViewById(R.id.rpm_field);
        mGaugeRpm = (Gauge) findViewById(R.id.rpm_meter);
        mLabelSpeedMetric = (TextView) findViewById(R.id.speed_metric_label);
        mLabelSpeedImperial = (TextView) findViewById(R.id.speed_imperial_label);
        mViewSpeedMetric = (TextView) findViewById(R.id.speed_metric_field);
        mViewSpeedImperial = (TextView) findViewById(R.id.speed_imperial_field);
        mGaugeSpeedMetric = (Gauge) findViewById(R.id.speed_metric_meter);
        mGaugeSpeedImperial = (Gauge) findViewById(R.id.speed_imperial_meter);
        mLabelEngTempMetric = (TextView) findViewById(R.id.enginetemp_metric_label);
        mLabelEngTempImperial = (TextView) findViewById(R.id.enginetemp_imperial_label);
        mViewEngTempMetric = (TextView) findViewById(R.id.enginetemp_metric_field);
        mViewEngTempImperial = (TextView) findViewById(R.id.enginetemp_imperial_field);
        mViewFull = (TextView) findViewById(R.id.full_field);
        mViewTurnSignals = (TextView) findViewById(R.id.turnsignals_field);
        mViewNeutral = (TextView) findViewById(R.id.neutral_field);
        mViewClutch = (TextView) findViewById(R.id.clutch_field);
        mViewGear = (TextView) findViewById(R.id.gear_field);
        mViewCheckEngine = (TextView) findViewById(R.id.checkengine_field);
        mLabelOdometerMetric = (TextView) findViewById(R.id.odometer_metric_label);
        mLabelOdometerImperial = (TextView) findViewById(R.id.odometer_imperial_label);
        mViewOdometerMetric = (TextView) findViewById(R.id.odometer_metric_field);
        mViewOdometerImperial = (TextView) findViewById(R.id.odometer_imperial_field);
        mLabelFuelMetric = (TextView) findViewById(R.id.fuel_metric_label);
        mLabelFuelImperial = (TextView) findViewById(R.id.fuel_imperial_label);
        mViewFuelMetric = (TextView) findViewById(R.id.fuel_metric_field);
        mViewFuelImperial = (TextView) findViewById(R.id.fuel_imperial_field);
        
        if (mModeRaw) {
    		mViewGr.setVisibility(View.GONE);
        	mViewRaw.setVisibility(View.VISIBLE);
    	}
    	else {
    		mViewRaw.setVisibility(View.GONE);
        	mViewGr.setVisibility(View.VISIBLE);
    	}
        
        if (mUnitMetric) {
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
    	
        drawRPM(0);
        drawSpeed(0);
        drawEngineTemp(0);
        drawFull(0);
        drawTurnSignals(0);
        drawNeutral(0);
        drawClutch(0);
        drawGear(0);
        drawCheckEngine(0);
        drawOdometer(0);
        drawFuel(0);
    }
    
    public void drawRPM(int value) {
    	mViewRpm.setText(Integer.toString(value));
        mGaugeRpm.setValue(value / 100);
    }
    
    public void drawSpeed(int value) {
    	// value is in mph
    	mViewSpeedMetric.setText(Integer.toString((int)(value * 1.609f)));
        mGaugeSpeedMetric.setValue((int)(value * 1.609f));
        mViewSpeedImperial.setText(Integer.toString(value));
        mGaugeSpeedImperial.setValue(value);
    }
    
    public void drawEngineTemp(int value) {
    	// value is in F
    	mViewEngTempMetric.setText(Integer.toString((value - 32) * 5 / 9));
    	mViewEngTempImperial.setText(Integer.toString(value));
    }
   
    public void drawFull(int value) {
    	mViewFull.setText(Integer.toString(value));
    }
   
    public void drawTurnSignals(int value) {
    	if ((value & 0x03) == 0x03)
    		mViewTurnSignals.setText("W");
    	else if ((value & 0x01) == 0x01)
    		mViewTurnSignals.setText("R");
    	else if ((value & 0x02) == 0x02)
    		mViewTurnSignals.setText("L");
    	else
    		mViewTurnSignals.setText("");
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
    	mViewCheckEngine.setText(Integer.toString(value));
    }
    
    public void drawOdometer(int value) {
    	// value is in ticks, at 1 tick = 0.00025 miles
    	mCurrentOdoValue = value;
    	value -= mResetOdoValue;
    	float miles = value * 0.00025f;
    	float km = miles * 1.609344f; 
    	mViewOdometerMetric.setText(String.format("%4.2f", km));
    	mGaugeSpeedMetric.setOdoValue(km);
    	mViewOdometerImperial.setText(String.format("%.2f", miles));
    	mGaugeSpeedImperial.setOdoValue(miles);
    }
    
    public void drawFuel(int value) {
    	// value is in milliliters
    	float valueFlOz;
    	mViewFuelMetric.setText(Integer.toString(value));
    	valueFlOz = value * 0.0338140227f;
    	mViewFuelImperial.setText(Float.toString(valueFlOz));
    }
}
