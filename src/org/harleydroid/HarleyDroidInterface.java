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

import java.io.IOException;
import java.util.concurrent.TimeoutException;
//import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

public class HarleyDroidInterface implements J1850Interface
{
	private static final boolean D = false;
	private static final String TAG = HarleyDroidInterface.class.getSimpleName();

	//private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private static final int ATMA_TIMEOUT = 10000;
	private static final int MAX_ERRORS = 10;

	private HarleyDroidService mHarleyDroidService;
	private HarleyData mHD;
	private ConnectThread mConnectThread;
	private PollThread mPollThread;
	private SendThread mSendThread;
	private BluetoothDevice mDevice;
	private NonBlockingBluetoothSocket mSock = null;

	public HarleyDroidInterface(HarleyDroidService harleyDroidService, BluetoothDevice device) {
		mHarleyDroidService = harleyDroidService;
		mDevice = device;
	}

	public void connect(HarleyData hd) {
		if (D) Log.d(TAG, "connect");

		mHD = hd;
		if (mConnectThread != null)
			mConnectThread.cancel();
		mConnectThread = new ConnectThread();
		mConnectThread.start();
	}

	public void disconnect() {
		if (D) Log.d(TAG, "disconnect");

		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}
		if (mPollThread != null) {
			mPollThread.cancel();
			mPollThread = null;
		}
		if (mSendThread != null) {
			mSendThread.cancel();
			mSendThread = null;
		}
		if (mSock != null) {
			mSock.close();
			mSock = null;
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
		if (mSendThread != null) {
			mSendThread.cancel();
		}
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

		if (mSendThread != null) {
			mSendThread.cancel();
			mSendThread = null;
		}
		if (mPollThread != null) {
			mPollThread.cancel();
		}
		mPollThread = new PollThread();
		mPollThread.start();
	}

	static byte[] myGetBytes(String s, int start, int end) {
		byte[] result = new byte[end - start];
		for (int i = start; i < end; i++) {
			result[i - start] = (byte) s.charAt(i);
		}
		return result;
	}

	static byte[] myGetBytes(String s) {
		return myGetBytes(s, 0, s.length());
	}

	private class ConnectThread extends Thread {

		public void run() {

			setName("HarleyDroidInterface: ConnectThread");

			try {
				mSock = new NonBlockingBluetoothSocket();
				mSock.connect(mDevice);
			} catch (IOException e1) {
				Log.e(TAG, "connect() socket failed", e1);
				mSock.close();
				mSock = null;
				mHarleyDroidService.disconnected(HarleyDroid.STATUS_ERROR);
				return;
			}

			mHarleyDroidService.connected();
		}

		public void cancel() {
		}
	}

	private class PollThread extends Thread {
		private boolean stop = false;

		public void run() {
			int errors = 0, idxJ;

			setName("HarleyDroidInterface: PollThread");
			mHarleyDroidService.startedPoll();

			while (!stop) {
				String line;

				try {
					line = mSock.readLine(ATMA_TIMEOUT);
				} catch (TimeoutException e1) {
					if (!stop)
						mHarleyDroidService.disconnected(HarleyDroid.STATUS_NODATA);
					if (mSock != null) {
						mSock.close();
						mSock = null;
					}
					return;
				}

				mHD.setRaw(myGetBytes(line));

				// strip off timestamp
				idxJ = line.indexOf('J');
				if (idxJ != -1) {
					if (J1850.parse(myGetBytes(line, idxJ + 1, line.length()), mHD))
						errors = 0;
					else
						++errors;
				}
				else
					++errors;

				if (errors > MAX_ERRORS) {
					mSock.close();
					mSock = null;
					mHarleyDroidService.disconnected(HarleyDroid.STATUS_TOOMANYERRORS);
					return;
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
			setName("HarleyDroidInterface: SendThread");
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
				this.interrupt();
			}
		}

		public void run() {
			int errors = 0;
			String recv;
			int idxJ;

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

				for (int i = 0; !stop && !newData && i < mCommand.length; i++) {

					byte[] data = new byte[3 + mCommand[i].length() / 2];
					data[0] = (byte)Integer.parseInt(mType[i], 16);
					data[1] = (byte)Integer.parseInt(mTA[i], 16);
					data[2] = (byte)Integer.parseInt(mSA[i], 16);
					for (int j = 0; j < mCommand[i].length() / 2; j++)
						data[j + 3] = (byte)Integer.parseInt(mCommand[i].substring(2 * j, 2 * j + 2), 16);

					String command = mCommand[i] + String.format("%02X", ((int)~J1850.crc(data)) & 0xff);

					if (D) Log.d(TAG, "send: " + mType[i] + "-" + mTA[i] + "-" +
							 mSA[i] + "-" + command + "-" + mExpect[i]);

					try {
						recv = mSock.chat(mType[i] + mTA[i] + mSA[i] + command, mExpect[i], mTimeout[i]);

						if (stop || newData)
							break;

						// split into lines and strip off timestamp
						String lines[] = recv.split("\n");
						for (int j = 0; j < lines.length; ++j) {
							mHD.setRaw(myGetBytes(lines[j]));
							idxJ = lines[j].indexOf('J');
							if (idxJ != -1)
								J1850.parse(myGetBytes(lines[j], idxJ + 1, lines[j].length()), mHD);
						}
						errors = 0;
					} catch (IOException e) {
						mHarleyDroidService.disconnected(HarleyDroid.STATUS_ERROR);
						mSock.close();
						mSock = null;
						return;
					} catch (TimeoutException e) {

						// split into lines and strip off timestamp
						String lines[] = e.getMessage().split("\n");
						for (int j = 0; j < lines.length; ++j) {
							mHD.setRaw(myGetBytes(lines[j]));
							idxJ = lines[j].indexOf('J');
							if (idxJ != -1)
								J1850.parse(myGetBytes(lines[j], idxJ + 1, lines[j].length()), mHD);
						}

						++errors;
						if (errors > MAX_ERRORS) {
							mHarleyDroidService.disconnected(HarleyDroid.STATUS_TOOMANYERRORS);
							if (mSock != null) {
								mSock.close();
								mSock = null;
							}
							return;
						}
					}

					if (!stop && !newData) {
						try {
							Thread.sleep(mTimeout[i]);
						} catch (InterruptedException e) {
						}
					}
				}

				if (!stop && !newData) {
					try {
						Thread.sleep(mDelay);
					} catch (InterruptedException e) {
					}
				}
			}
		}

		public void cancel() {
			stop = true;
		}
	}
}
