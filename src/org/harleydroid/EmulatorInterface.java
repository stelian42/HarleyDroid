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

import android.util.Log;

public class EmulatorInterface implements J1850Interface
{
	private static final boolean D = false;
	private static final String TAG = EmulatorInterface.class.getSimpleName();

	private static final int MAX_ERRORS = 10;

	private HarleyDroidService mHarleyDroidService;
	private HarleyData mHD;
	private PollThread mPollThread = null;

	public EmulatorInterface(HarleyDroidService harleyDroidService) {
		mHarleyDroidService = harleyDroidService;
	}

	public void connect(HarleyData hd) {
		if (D) Log.d(TAG, "connect: " + hd);

		mHD = hd;
		mHarleyDroidService.connected();
	}

	public void disconnect() {
		if (D) Log.d(TAG, "disconnect");

		if (mPollThread != null) {
			mPollThread.cancel();
			mPollThread = null;
		}
	}

	public void send(String type, String ta, String sa,
			 		 String command, String expect) {
		if (D) Log.d(TAG, "send: " + type + "-" + ta + "-" +
				  sa + "-" + command + "-" + expect);

		if (mPollThread != null) {
			mPollThread.cancel();
			mPollThread = null;
		}

		byte[] data = new byte[3 + command.length() / 2];
		data[0] = (byte)Integer.parseInt(type, 16);
		data[1] = (byte)Integer.parseInt(ta, 16);
		data[2] = (byte)Integer.parseInt(sa, 16);
		for (int i = 0; i < command.length() / 2; i++)
			data[i + 3] = (byte)Integer.parseInt(command.substring(2 * i, 2 * i + 2), 16);
		command += String.format("%02X", ((int)~J1850.crc(data)) & 0xff);

		if (D) Log.d(TAG, "send: " + type + "-" + ta + "-" +
				 sa + "-" + command + "-" + expect);

		// fake some answer...
		//String line = "J0CF1107C11303138393045";
		String line = "6CF11059ABCD20";

		J1850.parse(line.getBytes(), mHD);

		mHarleyDroidService.sendDone();
	}

	public void startPoll() {
		if (D) Log.d(TAG, "startPoll");

		if (mPollThread != null)
			mPollThread.cancel();
		mPollThread = new PollThread();
		mPollThread.start();
	}

	private class PollThread extends Thread {
		private boolean stop = false;

		public void run() {
			int cnt = 0;
			int errors = 0;

			setName("EmulatorInterface: PollThread");
			mHarleyDroidService.startedPoll();

			while (!stop) {
				String line;

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
				}

				/* send several messages to update the UI */
				mHD.setOdometer(cnt);
				cnt += 50;
				if (cnt % 100 == 0) {
					mHD.setCheckEngine(true);
					mHD.setTurnSignals(0);
				}
				else {
					mHD.setCheckEngine(false);
					mHD.setTurnSignals(3);
				}

				// RPM at 1053
				line = "28 1B 10 02 10 74 4C";
				// Speed at 100 km/h
				line = "J4829100232000f";
				// Odometer
				//line = "a8 69 10 06 00 00 FF 61";
				// ECM Sw Level
				line = "J0CF1107C0BB085";
				line = "J0CF1107C023134302D30371D";
				line = "J0CF1107C11303138393045";

				if (J1850.parse(line.getBytes(), mHD))
					errors = 0;
				else
					++errors;

				if (errors > MAX_ERRORS) {
					mHarleyDroidService.disconnected(HarleyDroid.STATUS_TOOMANYERRORS);
					stop = true;
				}
			}
		}

		public void cancel() {
			stop = true;
		}
	}
}
