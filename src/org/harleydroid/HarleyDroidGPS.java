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

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.util.Log;

public class HarleyDroidGPS implements LocationListener
{
	private static final boolean D = false;
	private static final String TAG = HarleyDroidGPS.class.getSimpleName();

	private LocationManager mLocationManager;
	private Location mCurrentBestLocation;

	public HarleyDroidGPS(Context context) {
		mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
	}

	public void start() {
		if (D) Log.d(TAG, "start()");

		mCurrentBestLocation = null;
		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
	}

	public void stop() {
		if (D) Log.d(TAG, "stop()");

		mLocationManager.removeUpdates(this);
	}

	public String getLocation() {
		if (D) Log.d(TAG, "getLocation() = " + mCurrentBestLocation);

		if (mCurrentBestLocation == null)
			return ",,";

		return Double.toString(mCurrentBestLocation.getLongitude()) + "," +
			   Double.toString(mCurrentBestLocation.getLatitude()) + "," +
			   Double.toString(mCurrentBestLocation.getAltitude());
	}

	private static final int TWO_MINUTES = 1000 * 60 * 2;

	/** Determines whether one Location reading is better than the current Location fix
	 * @param location  The new Location that you want to evaluate
	 * @param currentBestLocation  The current Location fix, to which you want to compare the new one
	 */
	protected boolean isBetterLocation(Location location, Location currentBestLocation) {
		if (currentBestLocation == null) {
			// A new location is always better than no location
			return true;
		}

		// Check whether the new location fix is newer or older
		long timeDelta = location.getTime() - currentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
		boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
		boolean isNewer = timeDelta > 0;

		// If it's been more than two minutes since the current location, use the new location
		// because the user has likely moved
		if (isSignificantlyNewer) {
			return true;
			// If the new location is more than two minutes older, it must be worse
		} else if (isSignificantlyOlder) {
			return false;
		}

		// Check whether the new location fix is more or less accurate
		int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		// Check if the old and new location are from the same provider
		boolean isFromSameProvider = isSameProvider(location.getProvider(),
				currentBestLocation.getProvider());

		// Determine location quality using a combination of timeliness and accuracy
		if (isMoreAccurate) {
			return true;
		} else if (isNewer && !isLessAccurate) {
			return true;
		} else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
			return true;
		}
		return false;
	}

	/** Checks whether two providers are the same */
	private boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}

	public void onLocationChanged(Location location) {
		if (D) Log.d(TAG, "onLocationChanged(" + location + ")");

		if ((mCurrentBestLocation == null) ||
				(isBetterLocation(location, mCurrentBestLocation)))
			mCurrentBestLocation = location;
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
		if (D) Log.d(TAG, "onStatusChanged(" + status + ")");

		if (status == LocationProvider.OUT_OF_SERVICE)
			mCurrentBestLocation = null;
	}

	public void onProviderEnabled(String provider) {
		if (D) Log.d(TAG, "onProviderEnabled()");

		mCurrentBestLocation = null;
	}

	public void onProviderDisabled(String provider) {
		if (D) Log.d(TAG, "onProviderDisabled()");

		mCurrentBestLocation = null;
	}
};
