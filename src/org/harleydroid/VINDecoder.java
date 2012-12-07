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

import android.app.Activity;
import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class VINDecoder {

	static void show(final Activity activity, CharSequence vin) {
		LayoutInflater li = LayoutInflater.from(activity);
		View view = li.inflate(R.layout.vin, null);
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setView(view);
		builder.setPositiveButton(R.string.vin_ok, null);
		builder.setIcon(R.drawable.ic_launcher_harleydroid);
		AlertDialog dialog = builder.create();
		dialog.show();

		TextView t = (TextView) dialog.findViewById(R.id.vin_destination);
		int r = activity.getResources().getIdentifier("vin_destination_" + vin.charAt(0), "string", activity.getPackageName());
		if (r != 0)
			t.setText(r);
		else
			t.setText("?");

		t = ((TextView) dialog.findViewById(R.id.vin_class));
		r = activity.getResources().getIdentifier("vin_class_" + vin.charAt(3), "string", activity.getPackageName());
		if (r != 0)
			t.setText(r);
		else
			t.setText("?");

		t = ((TextView) dialog.findViewById(R.id.vin_model));
		r = activity.getResources().getIdentifier("vin_model_" + vin.subSequence(4,  6), "string", activity.getPackageName());
		if (r != 0)
			t.setText(activity.getText(r) + " (" + vin.subSequence(4,  6) + ")");
		else
			t.setText("? (" + vin.subSequence(4,  6) + ")");

		t = ((TextView) dialog.findViewById(R.id.vin_engine));
		r = activity.getResources().getIdentifier("vin_engine_" + vin.charAt(6), "string", activity.getPackageName());
		if (r != 0)
			t.setText(r);
		else
			t.setText("?");

		t = ((TextView) dialog.findViewById(R.id.vin_date));
		r = activity.getResources().getIdentifier("vin_date_" + vin.charAt(7), "string", activity.getPackageName());
		if (r != 0)
			t.setText(r);
		else
			t.setText("?");

		t = ((TextView) dialog.findViewById(R.id.vin_year));
		if (vin.charAt(9) >= '1' && vin.charAt(9) <= '9')
			t.setText("" + (2000 + (vin.charAt(9) - '0')));
		else if (vin.charAt(9) >= 'A' && vin.charAt(9) <= 'Z')
			t.setText("" + (2010 + (vin.charAt(9) - 'A')));
		else
			t.setText("?");

		t = ((TextView) dialog.findViewById(R.id.vin_plant));
		r = activity.getResources().getIdentifier("vin_plant_" + vin.charAt(10), "string", activity.getPackageName());
		if (r != 0)
			t.setText(r);
		else
			t.setText("?");

		t = ((TextView) dialog.findViewById(R.id.vin_serial));
		t.setText(vin.subSequence(11,  17));
	}
}
