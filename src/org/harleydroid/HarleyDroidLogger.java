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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.zip.GZIPOutputStream;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

public class HarleyDroidLogger implements HarleyDataListener
{
	private static final boolean D = false;
	private static final String TAG = HarleyDroidLogger.class.getSimpleName();

	static final SimpleDateFormat TIMESTAMP_FORMAT =
		new SimpleDateFormat("yyyyMMddHHmmss");
	static {
		TIMESTAMP_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	private OutputStream mLog = null;
	private boolean mUnitMetric = false;
	
	public HarleyDroidLogger(Context context, boolean metric) {
		mUnitMetric = metric;
	}
	
	public void start() {
		if (D) Log.d(TAG, "start()");
		
		try {
			File path = new File(Environment.getExternalStorageDirectory(), "/Android/data/org.harleydroid/files/");
       		path.mkdirs();
          	File logFile = new File(path, "harley-" + TIMESTAMP_FORMAT.format(new Date()) + ".log.gz");
			mLog = new GZIPOutputStream(new FileOutputStream(logFile, false));
		} catch (IOException e) {
			Log.d(TAG, "Logfile open " + e);
		}
	}
	
	public void write(String data) {
		if (D) Log.d(TAG, "write()");
		
		if (mLog != null) {
			try {
				mLog.write(TIMESTAMP_FORMAT.format(new Date()).getBytes());
				mLog.write(',');
				mLog.write(data.getBytes());
				mLog.write('\n');
			} catch (IOException e) {
			}
		}
	}
	
	public void stop() {
		if (D) Log.d(TAG, "stop()");
		
		if (mLog != null) {
			try {
				mLog.close();
			} catch (IOException e) {
			}
			mLog = null;
		}
	}
	
	public void onRPMChanged(int rpm) {
		write("RPM," + rpm);
    }
	
	public void onSpeedImperialChanged(int speed) {
		if (!mUnitMetric)
			write("SPD," + speed);
    }
	
	public void onSpeedMetricChanged(int speed) {
		if (mUnitMetric)
			write("SPD," + speed);
    }
	
	public void onEngineTempImperialChanged(int engineTemp) {
		if (!mUnitMetric)
			write("ETP," + engineTemp);
    }
	
	public void onEngineTempMetricChanged(int engineTemp) {
		if (mUnitMetric)
			write("ETP," + engineTemp);
    }
    
	public void onFuelGaugeChanged(int full) {
		write("FGE," + full);
    }
	
	public void onTurnSignalsChanged(int turnSignals) {
		if ((turnSignals & 0x03) == 0x03)
			write("TRN,W");
    	else if ((turnSignals & 0x01) == 0x01)
    		write("TRN,R");
	    else if ((turnSignals & 0x02) == 0x02)
	    	write("TRN,L");
	    else
	    	write("TRN,");
    }
	
	public void onNeutralChanged(boolean neutral) {
		write("NTR," + (neutral ? "1" : "0"));
    }
    
	public void onClutchChanged(boolean clutch) {
   		write("CLU," + (clutch ? "1" : "0"));
   	}
 
	public void onGearChanged(int gear) {
   		write("GER," + gear);
   	}

	public void onCheckEngineChanged(boolean checkEngine) {
   		write("CHK," + (checkEngine ? "1" : "0"));
   	}
 
	public void onOdometerImperialChanged(int odometer) {
   		if (!mUnitMetric)
   			write("ODO," + odometer);
   	}
	
	public void onOdometerMetricChanged(int odometer) {
   		if (mUnitMetric)
   			write("ODO," + odometer);
   	}
	
   	public void onFuelImperialChanged(int fuel) {
   		if (!mUnitMetric)
   			write("FUL," + fuel);
   	}

   	public void onFuelMetricChanged(int fuel) {
   		if (mUnitMetric)
   			write("FUL," + fuel);
   	}

   	public void onBadCRCChanged(byte[] buffer) {
   		String bad = new String(buffer);
   		if (bad.equals(">ATMA") || bad.equals("SEARCHING..."))
   			return;
   		write("CRC," + bad.trim());
    }
    
   	public void onUnknownChanged(byte[] buffer) {
   		write("UNK," + new String(buffer).trim());
   	}
}