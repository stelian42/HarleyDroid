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

import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class HarleyDroidDiagnosticsView implements HarleyDataDiagnosticsListener
{
	private static final boolean D = false;
	private static final String TAG = HarleyDroidDiagnosticsView.class.getSimpleName();

	public static final int UPDATE_VIN = 1;
	public static final int UPDATE_ECMPN = 2;
	public static final int UPDATE_ECMCALID = 3;
	public static final int UPDATE_ECMSWLEVEL = 4;
	public static final int UPDATE_HISTORICDTC = 5;
	public static final int UPDATE_CURRENTDTC = 6;

	private Activity mActivity;

	// Views references cached for performance
	private TextView mViewVIN;
	private TextView mViewECMPN;
	private TextView mViewECMCalID;
	private TextView mViewECMSWLevel;
	private ListView mViewCurrentDTC;
	private ListView mViewHistoricDTC;

	public HarleyDroidDiagnosticsView(Activity activity) {
		mActivity = activity;
	}

	public void changeView(boolean portrait) {
		if (D) Log.d(TAG, "changeView portrait=" + portrait);

		int view = R.layout.portrait_graphic;

		if (portrait)
			view = R.layout.portrait_diag;
		else
			view = R.layout.landscape_diag;
		mActivity.setContentView(view);

		mViewVIN = (TextView) mActivity.findViewById(R.id.vin_field);
		mViewECMPN = (TextView) mActivity.findViewById(R.id.ecmpn_field);
		mViewECMCalID = (TextView) mActivity.findViewById(R.id.ecmcalid_field);
		mViewECMSWLevel = (TextView) mActivity.findViewById(R.id.ecmswlevel_field);
		mViewCurrentDTC = (ListView) mActivity.findViewById(R.id.currentdtc_field);
		mViewHistoricDTC = (ListView) mActivity.findViewById(R.id.historicdtc_field);
	}

	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (D) Log.d(TAG, "handleMessage " + msg.what);

			switch (msg.what) {
			case UPDATE_VIN:
				drawVIN(msg.getData().getString("vin"));
				break;
			case UPDATE_ECMPN:
				drawECMPN(msg.getData().getString("ecmpn"));
				break;
			case UPDATE_ECMCALID:
				drawECMCalID(msg.getData().getString("ecmcalid"));
				break;
			case UPDATE_ECMSWLEVEL:
				drawECMSWLevel(msg.arg1);
				break;
			case UPDATE_HISTORICDTC:
				drawHistoricDTC(msg.getData().getIntArray("historicdtc"));
				break;
			case UPDATE_CURRENTDTC:
				drawCurrentDTC(msg.getData().getIntArray("currentdtc"));
				break;
			}
		}
	};


	public void onVINChanged(String vin) {
		Message m = mHandler.obtainMessage(HarleyDroidDiagnosticsView.UPDATE_VIN);
		Bundle b = new Bundle();
		b.putString("vin", vin);
		m.setData(b);
		m.sendToTarget();
	}

	public void onECMPNChanged(String ecmPN) {
		Message m = mHandler.obtainMessage(HarleyDroidDiagnosticsView.UPDATE_ECMPN);
		Bundle b = new Bundle();
		b.putString("ecmpn", ecmPN);
		m.setData(b);
		m.sendToTarget();
	}

	public void onECMCalIDChanged(String ecmCalID) {
		Message m = mHandler.obtainMessage(HarleyDroidDiagnosticsView.UPDATE_ECMCALID);
		Bundle b = new Bundle();
		b.putString("ecmcalid", ecmCalID);
		m.setData(b);
		m.sendToTarget();
	}

	public void onECMSWLevelChanged(int ecmSWLevel) {
		mHandler.obtainMessage(HarleyDroidDiagnosticsView.UPDATE_ECMSWLEVEL, ecmSWLevel, -1).sendToTarget();
	}

	public void onHistoricDTCChanged(int[] dtc) {
		Message m = mHandler.obtainMessage(HarleyDroidDiagnosticsView.UPDATE_HISTORICDTC);
		Bundle b = new Bundle();
		b.putIntArray("historicdtc", dtc);
		m.setData(b);
		m.sendToTarget();
	}

	public void onCurrentDTCChanged(int[] dtc) {
		Message m = mHandler.obtainMessage(HarleyDroidDiagnosticsView.UPDATE_CURRENTDTC);
		Bundle b = new Bundle();
		b.putIntArray("currentdtc", dtc);
		m.setData(b);
		m.sendToTarget();
	}

	public void drawAll(HarleyData hd) {

		if (hd != null) {
			drawVIN(hd.getVIN());
			drawECMPN(hd.getECMPN());
			drawECMCalID(hd.getECMCalID());
			drawECMSWLevel(hd.getECMSWLevel());
			drawHistoricDTC(hd.getHistoricDTC());
			drawCurrentDTC(hd.getCurrentDTC());
		} else {
			drawVIN("N/A");
			drawECMPN("N/A");
			drawECMCalID("N/A");
			drawECMSWLevel(0);
			drawHistoricDTC(null);
			drawCurrentDTC(null);
		}
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
		if (D) Log.d("DTC", "drawHistoric");

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
		if (D) Log.d("DTC", "drawCurrent");

		if (mViewCurrentDTC == null)
			return;

		ArrayList<Integer> items = new ArrayList<Integer>();
		if (dtc != null)
			for (int i = 0; i < dtc.length; i++)
				items.add(new Integer(dtc[i]));
		mViewCurrentDTC.setAdapter(new ArrayAdapter<Integer>(mActivity, R.layout.dtc_item, items));
	}
}
