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
//import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

public class ELM327Interface implements J1850Interface
{
	private static final boolean D = false;
	private static final String TAG = ELM327Interface.class.getSimpleName();

	//private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private static final int AT_TIMEOUT = 2000;
	private static final int ATZ_TIMEOUT = 5000;
	private static final int ATMA_TIMEOUT = 10000;
	private static final int MAX_ERRORS = 10;

	private HarleyDroidService mHarleyDroidService;
	private HarleyData mHD;
	private ConnectThread mConnectThread;
	private PollThread mPollThread;
	private SendThread mSendThread;
	private BluetoothDevice mDevice;
	private NonBlockingBluetoothSocket mSock = null;

	public ELM327Interface(HarleyDroidService harleyDroidService, BluetoothDevice device) {
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
			// if the ELM327 is polling, we need to get it out of ATMA
			// or it will be left unusable (blocked in poll mode)
			try {
				mSock.writeLine("");
			} catch (Exception e) {
			}
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
			int elmVersionMajor = 0;
			int elmVersionMinor = 0;
			@SuppressWarnings("unused")
			int elmVersionRelease = 0;

			setName("ELM327Interface: ConnectThread");

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

			try {
				mSock.chat("AT", "", AT_TIMEOUT);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e2) {
				}
				// Warm Start
				String reply = mSock.chat("ATWS", "ELM327", ATZ_TIMEOUT);
				// parse reply to extract version information
				Pattern p = Pattern.compile("(?s)^.*ELM327 v(\\d+)\\.(\\d+)(\\w?).*$");
				Matcher m = p.matcher(reply);
				if (m.matches()) {
					elmVersionMajor = Integer.parseInt(m.group(1));
					elmVersionMinor = Integer.parseInt(m.group(2));
					if (m.group(3).length() > 0)
						elmVersionRelease = m.group(3).charAt(0);
				}
				// Echo ON
				mSock.chat("ATE1", "OK", AT_TIMEOUT);
				// Headers ON
				mSock.chat("ATH1", "OK", AT_TIMEOUT);
				// Allow long (>7 bytes) messages
				mSock.chat("ATAL", "OK", AT_TIMEOUT);
				// Spaces OFF
				if (elmVersionMajor >= 1 && elmVersionMinor >= 3)
					mSock.chat("ATS0", "OK", AT_TIMEOUT);
				// Select Protocol SAE J1850 VPW (10.4 kbaud)
				mSock.chat("ATSP2", "OK", AT_TIMEOUT);
			} catch (Exception e1) {
				mHarleyDroidService.disconnected(HarleyDroid.STATUS_ERRORAT);
				mSock.close();
				mSock = null;
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
			int errors = 0;

			setName("ELM327Interface: PollThread");
			try {
				// Monitor All
				mSock.writeLine("ATMA");
			} catch (IOException e1) {
				mHarleyDroidService.disconnected(HarleyDroid.STATUS_ERRORAT);
				mSock.close();
				mSock = null;
				return;
			}

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

				byte[] bytes = myGetBytes(line);
				mHD.setRaw(bytes);
				if (J1850.parse(bytes, mHD))
					errors = 0;
				else
					++errors;

				if (errors > MAX_ERRORS) {
					try {
						mSock.writeLine("");
					} catch (IOException e1) {
					}
					mSock.close();
					mSock = null;
					mHarleyDroidService.disconnected(HarleyDroid.STATUS_TOOMANYERRORS);
					return;
				}
			}
			try {
				mSock.writeLine("");
			} catch (IOException e1) {
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
			setName("ELM327Interface: SendThread");
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
			String lastHeaders = "";

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
					try {

						if (!lastHeaders.equals(mType[i] + mTA[i] + mSA[i])) {
							lastHeaders = mType[i] + mTA[i] + mSA[i];
							if (D) Log.d(TAG, "send: ATSH" + lastHeaders);
							mSock.chat("ATSH" + lastHeaders, "OK", AT_TIMEOUT);
						}

						if (D) Log.d(TAG, "send: " + mCommand[i] + "-" + mExpect[i]);

						recv = mSock.chat(mCommand[i], mExpect[i], mTimeout[i]);
						// split into lines
						if (stop || newData)
							break;

						String lines[] = recv.split("\n");
						for (int j = 0; j < lines.length; ++j) {
							byte[] bytes = myGetBytes(lines[j]);
							mHD.setRaw(bytes);
							J1850.parse(bytes, mHD);
						}
						errors = 0;
					} catch (IOException e) {
						mHarleyDroidService.disconnected(HarleyDroid.STATUS_ERROR);
						mSock.close();
						mSock = null;
						return;
					} catch (TimeoutException e) {

						String lines[] = e.getMessage().split("\n");
						for (int j = 0; j < lines.length; ++j) {
							byte[] bytes = myGetBytes(lines[j]);
							mHD.setRaw(bytes);
							J1850.parse(bytes, mHD);
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
