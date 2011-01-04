package org.harleydroid;

import java.util.ArrayList;
import java.util.Set;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;

public class HarleyDroidSettings extends PreferenceActivity {
	private static final boolean EMULATOR = true;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        
        BluetoothAdapter bluetoothAdapter = null;
        ArrayList<CharSequence> bluetoothDevices = new ArrayList<CharSequence>();
        
    	if (!EMULATOR) {
    		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    		Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
    		for (BluetoothDevice dev : pairedDevices)
    			bluetoothDevices.add(dev.getAddress());
    	}
    	else {
    		bluetoothDevices.add("0:0:0:0");
    		bluetoothDevices.add("1:1:1:1");
    		bluetoothDevices.add("2:2:2:2");
    		bluetoothDevices.add("3:3:3:3");
    		bluetoothDevices.add("4:4:4:4");
    	}
        	
        ListPreference btlist = (ListPreference) findPreference("bluetoothid");
        btlist.setEntryValues(bluetoothDevices.toArray(new CharSequence[0]));
        btlist.setEntries(bluetoothDevices.toArray(new CharSequence[0]));
    }
}
