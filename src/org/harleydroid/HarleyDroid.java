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
import android.view.WindowManager;
import android.widget.Toast;

public class HarleyDroid extends Activity implements ServiceConnection, Eula.OnEulaAgreedTo
{
	private static final boolean D = false;
	private static final String TAG = HarleyDroid.class.getSimpleName();
	public static final boolean EMULATOR = false;
	
    static final int CONNECTING_TO_ELM327 = 1;

    // Message types sent from HarleyDroidService
    public static final int STATUS_ERROR = 1;
    public static final int STATUS_ERRORAT = 2;
    public static final int STATUS_CONNECTED = 3;
    public static final int STATUS_NODATA = 4;
    public static final int STATUS_TOOMANYERRORS = 5;

    private static final int REQUEST_ENABLE_BT = 2;
    
    private SharedPreferences mPrefs;    
    private BluetoothAdapter mBluetoothAdapter = null;
    private Menu mOptionsMenu = null;
    private String mBluetoothID = null;
    private boolean mLogging = false;
    private HarleyDroidService mService = null;
    private boolean mModeText = false;
    private boolean mUnitMetric = false;
    private int mOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
    private HarleyDroidView mHarleyDroidView;
    private HarleyData mHD;
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
    	if (D) Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);

        mHarleyDroidView = new HarleyDroidView(this);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
			setContentView(R.layout.portrait);
		else
			setContentView(R.layout.landscape);
        
        if (Eula.show(this, false))
        	onEulaAgreedTo();
    }
    
    @Override
    public void onEulaAgreedTo() {
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
    public void onStart() {
    	if (D) Log.d(TAG, "onStart()");
    	super.onStart();

    	// get preferences which may have been changed
    	mBluetoothID = mPrefs.getString("bluetoothid", null);
    	if (mPrefs.getString("orientation", "auto").equals("auto"))
    		mOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
    	else if (mPrefs.getString("orientation", "auto").equals("portrait")) {
    		mOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
    		setContentView(R.layout.portrait);
    	}
    	else if (mPrefs.getString("orientation", "auto").equals("landscape")) {
    		mOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
    		setContentView(R.layout.landscape);
    	}
    	this.setRequestedOrientation(mOrientation);
    	mLogging = false;
    	if (mPrefs.getBoolean("logging", false)) {
        	if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))
        		Toast.makeText(this, R.string.nologging, Toast.LENGTH_LONG).show();
        	else
        		mLogging = true;  	
        }
    	if (mPrefs.getBoolean("screenon", false)) 
    		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    	if (mPrefs.getString("unit", "metric").equals("metric"))
    		mUnitMetric = true;
    	else
    		mUnitMetric = false;
    	mModeText = mPrefs.getBoolean("modetext", false);
    		   	
    	mHarleyDroidView.drawAll(mHD, mModeText, mUnitMetric);
            
    	// bind to the service
    	bindService(new Intent(this, HarleyDroidService.class), this, 0);	
    }
 
    @Override
    public void onStop() {
    	if (D) Log.d(TAG, "onStop()");
    	super.onStop();

    	unbindService(this);
    	mService = null;
    	
    	SharedPreferences.Editor editor = mPrefs.edit();
    	editor.putBoolean("modetext", mModeText);
    	editor.commit();
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
        	mHarleyDroidView.drawAll(mHD, mModeText, mUnitMetric);
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
    	
    	mOptionsMenu.findItem(R.id.capture_menu).setEnabled(
    			(mBluetoothID == null) ? false : true);
    	if (mService != null && mService.isRunning()) {
    		mOptionsMenu.findItem(R.id.capture_menu).setIcon(R.drawable.ic_menu_stop);
    		mOptionsMenu.findItem(R.id.capture_menu).setTitle(R.string.stopcapture_label);
    	}
    	else {
    		mOptionsMenu.findItem(R.id.capture_menu).setIcon(R.drawable.ic_menu_play_clip);
    		mOptionsMenu.findItem(R.id.capture_menu).setTitle(R.string.startcapture_label);
    	}
    	if (mModeText)
    	mOptionsMenu.findItem(R.id.mode_menu).setTitle(
    			mModeText ? R.string.mode_labelgr : R.string.mode_labelraw);
    		
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
        	mModeText = !mModeText;
        	mHarleyDroidView.drawAll(mHD, mModeText, mUnitMetric);
        	return true;
        case R.id.preferences_menu:
        	Intent settingsActivity = new Intent(getBaseContext(), HarleyDroidSettings.class);
        	startActivity(settingsActivity);
        	return true;
        case R.id.resetodo_menu:
        	mHD.resetOdometer();
        	return true;
        case R.id.about_menu:
        	Eula.show(this, true);
        	return true;
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
		mHD = mService.getHarleyData();
		mHD.addHarleyDataListener(mHarleyDroidView);
		mHarleyDroidView.drawAll(mHD, mModeText, mUnitMetric);
		
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
       		}
    	}
    };
}
