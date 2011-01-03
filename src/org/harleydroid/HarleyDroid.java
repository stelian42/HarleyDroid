package org.harleydroid;

import java.io.File;
import java.util.ArrayList;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class HarleyDroid extends Activity implements ServiceConnection
{
	private static final boolean D = true;
	private static final String TAG = HarleyDroid.class.getSimpleName();
	private static final boolean EMULATOR = true;
	
    static final int CHOOSE_BLUETOOTH_DEV = 1;
    static final int CONNECTING_TO_ELM327 = 2;

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
    private ArrayList<CharSequence> mBluetoothDevices = null;
    private Menu mOptionsMenu = null;
    private String mBluetoothID = null;
    private SharedPreferences mPrefs = null;
    private File mLogFile = null;
    private HarleyDroidService mService = null;
    private boolean mModeRaw = false;
    
    // Views references cached for performance
    private View mViewRaw;
    private View mViewGr;
    private TextView mViewRpm;
    private Gauge mGaugeRpm;
    private TextView mViewSpeed;
    private Gauge mGaugeSpeed;
    private TextView mViewEngTemp;
    private TextView mViewFull;
    private TextView mViewTurnSignals;
    private TextView mViewNeutral;
    private TextView mViewClutch;
    private TextView mViewGear;
    private TextView mViewCheckEngine;
    private TextView mViewOdometer;
    private TextView mViewFuel;
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
    	if (D) Log.d(TAG, "onCreate()");
    	
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mPrefs = this.getPreferences(MODE_PRIVATE);
        setLogging(mPrefs.getBoolean("logging", false));
        	
        if (!EMULATOR) {
        	mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        	if (mBluetoothAdapter == null) {
        		Toast.makeText(this, R.string.nobluetooth, Toast.LENGTH_LONG).show();
        		finish();
        		return;
        	}
        }
        
        mViewGr = findViewById(R.id.gr_layout);
		mViewRaw = findViewById(R.id.raw_layout);
        mViewRpm = (TextView) findViewById(R.id.rpm_field);
        mGaugeRpm = (Gauge) findViewById(R.id.rpm_meter);
        mViewSpeed = (TextView) findViewById(R.id.speed_field);
        mGaugeSpeed = (Gauge) findViewById(R.id.speed_meter);
        mViewEngTemp = (TextView) findViewById(R.id.enginetemp_field);
        mViewFull = (TextView) findViewById(R.id.full_field);
        mViewTurnSignals = (TextView) findViewById(R.id.turnsignals_field);
        mViewNeutral = (TextView) findViewById(R.id.neutral_field);
        mViewClutch = (TextView) findViewById(R.id.clutch_field);
        mViewGear = (TextView) findViewById(R.id.gear_field);
        mViewCheckEngine = (TextView) findViewById(R.id.checkengine_field);
        mViewOdometer = (TextView) findViewById(R.id.odometer_field);
        mViewFuel = (TextView) findViewById(R.id.fuel_field);
            
        if (savedInstanceState != null) {
        	mModeRaw = savedInstanceState.getBoolean("moderaw");
        	if (D) Log.d(TAG, "savedInstanceState: mModeRaw = " + mModeRaw);
        	drawLayout();
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
    
    @Override
    public void onDestroy() {
    	if (D) Log.d(TAG, "onDestroy()");
    	super.onDestroy();
    }
    
    @Override
    public void onStart() {
    	if (D) Log.d(TAG, "onStart()");
    	super.onStart();

    	// bind to the service
    	bindService(new Intent(this, HarleyDroidService.class), this, 0);

    	if ((!EMULATOR) && (!mBluetoothAdapter.isEnabled())) {
    		Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
    		startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    	}
    	else
    		onBluetoothEnabled();
    	
    }
 
    @Override
    public void onStop() {
    	if (D) Log.d(TAG, "onStop()");
    	super.onStop();

    	unbindService(this);
    	mService = null;
    }
  
    @Override
    public void onSaveInstanceState(Bundle outState) {
    	if (D) Log.d(TAG, "onSaveInstanceState(mModeRaw= " + mModeRaw + ")");
    	
    	outState.putBoolean("moderaw", mModeRaw);
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
    	if (D) Log.d(TAG, "onCreateDialog()");
    	
    	switch (id) {
    	case CHOOSE_BLUETOOTH_DEV:
    		AlertDialog.Builder build = new AlertDialog.Builder(this);
    		int current = -1;
    		build.setTitle(R.string.choosebluetooth);
    		if (mBluetoothID != null) {
    			for (int i = 0; i < mBluetoothDevices.size(); i++)
    				if (mBluetoothDevices.get(i).equals(mBluetoothID))
    					current = i;
    		}
    		build.setSingleChoiceItems((CharSequence[])mBluetoothDevices.toArray(new CharSequence[0]), current, new DialogInterface.OnClickListener() {
    			public void onClick(DialogInterface dialog, int item) {
    				dismissDialog(CHOOSE_BLUETOOTH_DEV);
    				onBluetoothDeviceChosen((String)mBluetoothDevices.get(item));
    		    }
    		});
    		return build.create();
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
    		mOptionsMenu.findItem(R.id.startcapture_menu).setEnabled(false);
            mOptionsMenu.findItem(R.id.stopcapture_menu).setEnabled(true);
            mOptionsMenu.findItem(R.id.choosedev_menu).setEnabled(false);
            mOptionsMenu.findItem(R.id.logging_menu).setEnabled(false);
    	}
    	else {
    		mOptionsMenu.findItem(R.id.startcapture_menu).setEnabled(true);
            mOptionsMenu.findItem(R.id.stopcapture_menu).setEnabled(false);
            mOptionsMenu.findItem(R.id.choosedev_menu).setEnabled(true);
            mOptionsMenu.findItem(R.id.logging_menu).setEnabled(true);
    	}
    	if (mModeRaw)
    		mOptionsMenu.findItem(R.id.mode_menu).setTitle(R.string.mode_labelgr);
    	else
    		mOptionsMenu.findItem(R.id.mode_menu).setTitle(R.string.mode_labelraw);
    		
    	if (mLogFile != null)
        	mOptionsMenu.findItem(R.id.logging_menu).setTitle(R.string.logging_labeloff);
        else
        	mOptionsMenu.findItem(R.id.logging_menu).setTitle(R.string.logging_labelon);
		return true;
    }
    
    private void setLogging(boolean log) {
    	if (D) Log.d(TAG, "setLogging(" + log + ")");
    	
    	mLogFile = null;
    	if (log) {
        	if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
        		Toast.makeText(this, R.string.nologging, Toast.LENGTH_LONG).show();
        		log = false;
        	}
        	else	
           		mLogFile = new File(getExternalFilesDir(null), "harley.log");
        }
    	Editor editor = mPrefs.edit();
    	editor.putBoolean("logging", log);
    	editor.commit();
    }
  
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	if (D) Log.d(TAG, "onOptionsItemSelected");
    	
        switch (item.getItemId()) {
        case R.id.startcapture_menu:
        	startCapture();
            return true;
        case R.id.stopcapture_menu:
        	stopCapture();
        	return true;
        case R.id.mode_menu:
        	mModeRaw = !mModeRaw;
        	drawLayout();
        	return true;
        case R.id.choosedev_menu:
        	showDialog(CHOOSE_BLUETOOTH_DEV);
        	return true;
        case R.id.logging_menu:
        	setLogging(!mPrefs.getBoolean("logging", false));
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
            if (resultCode == Activity.RESULT_OK)
            	onBluetoothEnabled();
            else {
            	Toast.makeText(this, R.string.noenablebluetooth, Toast.LENGTH_LONG).show();
            	finish();
            }
        }
    }

    private void onBluetoothEnabled() {
    	if (D) Log.d(TAG, "onBluetoothEnabled()");
    	
    	mBluetoothDevices = new ArrayList<CharSequence>();
    
    	if (!EMULATOR) {
    		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
    		for (BluetoothDevice dev : pairedDevices)
    			mBluetoothDevices.add(dev.getAddress());
    	}
    	else {
    		mBluetoothDevices.add("0:0:0:0");
    		mBluetoothDevices.add("1:1:1:1");
    		mBluetoothDevices.add("2:2:2:2");
    		mBluetoothDevices.add("3:3:3:3");
    		mBluetoothDevices.add("4:4:4:4");
    	}
  
    	mBluetoothID = mPrefs.getString("bluetoothid", null);
    	
    	if (mBluetoothID == null) {
    		showDialog(CHOOSE_BLUETOOTH_DEV);
    	}
    }
    
    private void onBluetoothDeviceChosen(String item) {
    	if (D) Log.d(TAG, "onBluetoothDeviceChosen()");
    	
    	mBluetoothID = item;
    	
    	Editor editor = mPrefs.edit();
    	editor.putString("bluetoothid", mBluetoothID);
    	editor.commit();
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
    		mService.startService(mBluetoothAdapter.getRemoteDevice(mBluetoothID), mLogFile);
    	else
    		mService.startService(null, mLogFile);
	}
    
    private void startCapture() {
    	if (D) Log.d(TAG, "startCapture()");
    	
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
    	if (mModeRaw) {
    		mViewGr.setVisibility(View.GONE);
        	mViewRaw.setVisibility(View.VISIBLE);
    	}
    	else {
    		mViewRaw.setVisibility(View.GONE);
        	mViewGr.setVisibility(View.VISIBLE);
    	}
    }
    
    public void drawRPM(int value) {
    	mViewRpm.setText(Integer.toString(value));
        mGaugeRpm.setValue(value / 100);
    }
    
    public void drawSpeed(int value) {
    	mViewSpeed.setText(Integer.toString(value));
        mGaugeSpeed.setValue(value);
    }
    
    public void drawEngineTemp(int value) {
    	mViewEngTemp.setText(Integer.toString(value));
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
    	mViewOdometer.setText(Integer.toString(value));
    }
    
    public void drawFuel(int value) {
    	mViewFuel.setText(Integer.toString(value));
    }
}