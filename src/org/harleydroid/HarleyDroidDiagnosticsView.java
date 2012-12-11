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
import java.util.ArrayList;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class HarleyDroidDiagnosticsView implements HarleyDataDiagnosticsListener, OnItemClickListener, OnClickListener
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
	private HarleyDroidDiagnosticsViewHandler mHandler;

	// Views references cached for performance
	private Button mViewVIN;
	private TextView mViewECMPN;
	private TextView mViewECMCalID;
	private TextView mViewECMSWLevel;
	private ListView mViewCurrentDTC;
	private ListView mViewHistoricDTC;

	public HarleyDroidDiagnosticsView(Activity activity) {
		mActivity = activity;
		mHandler = new HarleyDroidDiagnosticsViewHandler(this);
	}

	public void changeView(boolean portrait) {
		if (D) Log.d(TAG, "changeView portrait=" + portrait);

		int view = R.layout.portrait_graphic;

		if (portrait)
			view = R.layout.portrait_diag;
		else
			view = R.layout.landscape_diag;
		mActivity.setContentView(view);

		mViewVIN = (Button) mActivity.findViewById(R.id.vin_field);
		mViewECMPN = (TextView) mActivity.findViewById(R.id.ecmpn_field);
		mViewECMCalID = (TextView) mActivity.findViewById(R.id.ecmcalid_field);
		mViewECMSWLevel = (TextView) mActivity.findViewById(R.id.ecmswlevel_field);
		mViewCurrentDTC = (ListView) mActivity.findViewById(R.id.currentdtc_field);
		mViewHistoricDTC = (ListView) mActivity.findViewById(R.id.historicdtc_field);

		mViewVIN.setOnClickListener(this);
		mViewCurrentDTC.setOnItemClickListener(this);
		mViewHistoricDTC.setOnItemClickListener(this);
	}

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
			drawHistoricDTC(msg.getData().getStringArray("historicdtc"));
			break;
		case UPDATE_CURRENTDTC:
			drawCurrentDTC(msg.getData().getStringArray("currentdtc"));
			break;
		}
	}

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

	public void onHistoricDTCChanged(String[] dtc) {
		Message m = mHandler.obtainMessage(HarleyDroidDiagnosticsView.UPDATE_HISTORICDTC);
		Bundle b = new Bundle();
		b.putStringArray("historicdtc", dtc);
		m.setData(b);
		m.sendToTarget();
	}

	public void onCurrentDTCChanged(String[] dtc) {
		Message m = mHandler.obtainMessage(HarleyDroidDiagnosticsView.UPDATE_CURRENTDTC);
		Bundle b = new Bundle();
		b.putStringArray("currentdtc", dtc);
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
			drawVIN("");
			drawECMPN("");
			drawECMCalID("");
			drawECMSWLevel(-1);
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
		if (mViewECMSWLevel != null) {
			if (value == -1)
				mViewECMSWLevel.setText("");
			else
				mViewECMSWLevel.setText("0x" + Integer.toString(value, 16));
		}
	}

	public void drawHistoricDTC(String[] dtc) {
		if (D) Log.d("DTC", "drawHistoric");

		if (mViewHistoricDTC == null)
			return;

		// arrayAdapter.notifyDataSetChanged();
		ArrayList<String> items = new ArrayList<String>();
		if (dtc != null)
			for (int i = 0; i < dtc.length; i++)
				items.add(dtc[i]);
		mViewHistoricDTC.setAdapter(new ArrayAdapter<String>(mActivity, R.layout.dtc_item, items));
	}

	public void drawCurrentDTC(String[] dtc) {
		if (D) Log.d("DTC", "drawCurrent");

		if (mViewCurrentDTC == null)
			return;

		ArrayList<String> items = new ArrayList<String>();
		if (dtc != null)
			for (int i = 0; i < dtc.length; i++)
				items.add(dtc[i]);
		mViewCurrentDTC.setAdapter(new ArrayAdapter<String>(mActivity, R.layout.dtc_item, items));
	}

	@Override
	public void onItemClick(AdapterView<?> adapter, View view, int pos, long id) {
		Resources res = mActivity.getResources();
		String dtc = ((TextView)view).getText().toString();
		String[] dtcCodes = res.getStringArray(R.array.dtc_codes);
		String[] dtcStrings = res.getStringArray(R.array.dtc_strings);

		if (D) Log.i(TAG, "Clicked on [" + ((TextView)view).getText() + "]");
		for (int i = 0; i < dtcCodes.length; ++i) {
			if (dtc.equals(dtcCodes[i])) {
				Toast.makeText(mActivity.getApplicationContext(), dtcStrings[i], Toast.LENGTH_SHORT).show();
				break;
			}
		}
	}

	@Override
	public void onClick(View v) {
		CharSequence text = mViewVIN.getText();
		if (text.length() == 17)
			VINDecoder.show(mActivity, text);
	}

	static class HarleyDroidDiagnosticsViewHandler extends Handler {
		private final WeakReference<HarleyDroidDiagnosticsView> mHarleyDroidDiagnosticsView;

	    HarleyDroidDiagnosticsViewHandler(HarleyDroidDiagnosticsView harleyDroidDiagnosticsView) {
	        mHarleyDroidDiagnosticsView = new WeakReference<HarleyDroidDiagnosticsView>(harleyDroidDiagnosticsView);
	    }

		@Override
		public void handleMessage(Message msg) {
			HarleyDroidDiagnosticsView hddv = mHarleyDroidDiagnosticsView.get();
			if (hddv != null)
				hddv.handleMessage(msg);
		}
	}
}
