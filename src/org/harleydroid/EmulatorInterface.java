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
	private SendThread mSendThread = null;

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
		if (mSendThread != null) {
			mSendThread.cancel();
			mSendThread = null;
		}
	}

	public void startSend(String type[], String ta[], String sa[],
						  String command[], String expect[],
						  int timeout[], int delay) {
		if (D) Log.d(TAG, "send: " + type + "-" + ta + "-" +
					 sa + "-" + command + "-" + expect);

		if (mPollThread != null) {
			mPollThread.cancel();
			mPollThread = null;
		}
		if (mSendThread != null)
			mSendThread.cancel();
		mSendThread = new SendThread(type, ta, sa, command, expect, timeout, delay);
		mSendThread.start();
	}

	public void setSendData(String type[], String ta[], String sa[],
							String command[], String expect[],
							int timeout[], int delay) {
		if (D) Log.d(TAG, "setSendData");

		if (mSendThread != null)
			mSendThread.setData(type, ta, sa, command, expect, timeout, delay);
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
			int odo = 0;
			int fuel = 0;
			int errors = 0;

			setName("EmulatorInterface: PollThread");
			mHarleyDroidService.startedPoll();

			while (!stop) {
				String line;

				try {
					Thread.sleep(500);
				} catch (InterruptedException e1) {
				}

				/* send several messages to update the UI */
				mHD.setOdometer(odo);
				odo += 150;
				mHD.setFuel(fuel);
				fuel += 50;
				if (odo % 100 == 0) {
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

				line = "483B402094";
				line = "483B40A0B2";

				byte[] bytes = line.getBytes();
				mHD.setRaw(bytes);
				if (J1850.parse(bytes, mHD))
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

	private class SendThread extends Thread {
		private boolean stop = false;
		private boolean newData = false;
		private String mType[], mTA[], mSA[], mCommand[], mExpect[];
		private int mTimeout[];
		private String mNewType[], mNewTA[], mNewSA[], mNewCommand[], mNewExpect[];
		private int mNewTimeout[];
		private int mDelay, mNewDelay;

		public SendThread(String type[], String ta[], String sa[], String command[], String expect[], int timeout[], int delay) {
			setName("EmulatorInterface: SendThread");
			mType = type;
			mTA = ta;
			mSA = sa;
			mCommand = command;
			mExpect = expect;
			mTimeout = timeout;
			mDelay = delay;
		}

		public void setData(String type[], String ta[], String sa[], String command[], String expect[], int timeout[], int delay) {
			synchronized (this) {
				mNewType = type;
				mNewTA = ta;
				mNewSA = sa;
				mNewCommand = command;
				mNewExpect = expect;
				mNewTimeout = timeout;
				mNewDelay = delay;
				newData = true;
			}
		}

		public void run() {

			mHarleyDroidService.startedSend();

			while (!stop) {

				synchronized (this) {
					if (newData) {
						mType = mNewType;
						mTA = mNewTA;
						mSA = mNewSA;
						mCommand = mNewCommand;
						mExpect = mNewExpect;
						mTimeout = mNewTimeout;
						mDelay = mNewDelay;
						newData = false;
					}
				}

				for (int i = 0; !stop && i < mCommand.length; i++) {

					byte[] data = new byte[3 + mCommand[i].length() / 2];
					data[0] = (byte)Integer.parseInt(mType[i], 16);
					data[1] = (byte)Integer.parseInt(mTA[i], 16);
					data[2] = (byte)Integer.parseInt(mSA[i], 16);
					for (int j = 0; j < mCommand[i].length() / 2; j++)
						data[j + 3] = (byte)Integer.parseInt(mCommand[i].substring(2 * j, 2 * j + 2), 16);

					String command = mCommand[i] + String.format("%02X", ((int)~J1850.crc(data)) & 0xff);

					if (D) Log.d(TAG, "send: " + mType[i] + "-" + mTA[i] + "-" +
							 mSA[i] + "-" + command + "-" + mExpect[i]);

					// fake some answer...
					//String line = "J0CF1107C11303138393045";
					String line = "6CF1105901341167";

					byte[] bytes = line.getBytes();
					mHD.setRaw(bytes);
					J1850.parse(bytes, mHD);

					try {
						Thread.sleep(mTimeout[i]);
					} catch (InterruptedException e) {
					}
				}

				try {
					Thread.sleep(mDelay);
				} catch (InterruptedException e) {
				}
			}
		}

		public void cancel() {
			stop = true;
		}
	}
}
