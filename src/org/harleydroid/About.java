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
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class About {

	static void about(final Activity activity) {
		LayoutInflater li = LayoutInflater.from(activity);
		View view = li.inflate(R.layout.about, null);
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setView(view);
		builder.setPositiveButton(R.string.about_ok, null);
		builder.setNeutralButton(R.string.about_license, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Eula.show(activity, true);
			}
		});
		builder.setIcon(R.drawable.ic_launcher_harleydroid);
		AlertDialog dialog = builder.create();
		dialog.show();

		try {
			PackageInfo pi = activity.getPackageManager().getPackageInfo("org.harleydroid", PackageManager.GET_META_DATA);
			((TextView) dialog.findViewById(R.id.about_version)).setText(pi.versionName);
		} catch (NameNotFoundException e) { }
	}
}
