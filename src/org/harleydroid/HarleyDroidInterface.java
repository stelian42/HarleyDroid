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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class HarleyDroidInterface implements J1850Interface
{
	private static final boolean D = true;
	private static final String TAG = HarleyDroidInterface.class.getSimpleName();

	private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private static final int AT_TIMEOUT = 2000;
	private static final int ATMA_TIMEOUT = 10000;
	private static final int MAX_ERRORS = 10;

	private HarleyDroidService mHarleyDroidService;
	private HarleyData mHD;
	private ConnectThread mConnectThread;
	private PollThread mPollThread;
	private SendThread mSendThread;
	private BluetoothDevice mDevice;
	private BluetoothSocket mSock;
	private BufferedReader mIn;
	private OutputStream mOut;
	private Timer mTimer;

	public HarleyDroidInterface(HarleyDroidService harleyDroidService, BluetoothDevice device) {
		mHarleyDroidService = harleyDroidService;
		mDevice = device;
		mTimer = new Timer();
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
		mHarleyDroidService.disconnected(HarleyDroid.STATUS_OK);
	}

	public void send(String type, String ta, String sa,
			 		 String command, String expect) {
		if (D) Log.d(TAG, "send: " + type + "-" + ta + "-" +
					 sa + "-" + command + "-" + expect);

		stopPoll();

		byte[] data = new byte[3 + command.length() / 2];
		data[0] = (byte)Integer.parseInt(type, 16);
		data[1] = (byte)Integer.parseInt(ta, 16);
		data[2] = (byte)Integer.parseInt(sa, 16);
		for (int i = 0; i < command.length() / 2; i++)
			data[i + 3] = (byte)Integer.parseInt(command.substring(2 * i, 2 * i + 2), 16);
		command += String.format("%02X", ((int)~J1850.crc(data)) & 0xff);

		if (D) Log.d(TAG, "send: " + type + "-" + ta + "-" +
				 sa + "-" + command + "-" + expect);

		mSendThread = new SendThread(type, ta, sa, command, expect);
		mSendThread.start();
	}

	public void startPoll() {
		if (D) Log.d(TAG, "startPoll");

		stopPoll();
		mPollThread = new PollThread();
		mPollThread.start();
	}

	public void stopPoll() {
		if (D) Log.d(TAG, "stopPoll");

		if (mPollThread != null) {
			mPollThread.cancel();
			mPollThread = null;
		}
	}

	private class CancelTimer extends TimerTask {
		public void run() {
			if (D) Log.d(TAG, "CANCEL AT " + System.currentTimeMillis());
			try {
				mSock.close();
			} catch (IOException e) {
			}
		}
	};

	private String readLine(long timeout) throws IOException {
		CancelTimer t = new CancelTimer();
		mTimer.schedule(t, timeout);
		if (D) Log.d(TAG, "READLINE AT " + System.currentTimeMillis());
		String line = mIn.readLine();
		t.cancel();
		if (D) Log.d(TAG, "read (" + line.length() + "): " + line);
		return line;
	}

	private void writeLine(String line) throws IOException {
		line += "\r";
		if (D) Log.d(TAG, "write: " + line);
		mOut.write(line.getBytes());
		mOut.flush();
	}

	private String chat(String send, String expect, long timeout) throws IOException {
		StringBuilder line = new StringBuilder();
		writeLine(send);
		long start = System.currentTimeMillis();
		while (timeout > 0) {
			line.append(readLine(timeout) + "\n");
			long now = System.currentTimeMillis();
			if (line.indexOf(expect) != -1)
				return line.toString();
			timeout -= (now - start);
			start = now;
		}
		throw new IOException("timeout");
	}

	private class ConnectThread extends Thread {

		public ConnectThread() {
			BluetoothSocket tmp = null;

			try {
				//tmp = mDevice.createRfcommSocketToServiceRecord(SPP_UUID);
				Method m = mDevice.getClass().getMethod("createRfcommSocket", new Class[] { int.class });
				tmp = (BluetoothSocket) m.invoke(mDevice, 1);
			} catch (Exception e) {
				Log.e(TAG, "createRfcommSocket() failed", e);
				mHarleyDroidService.disconnected(HarleyDroid.STATUS_ERROR);
			}
			mSock = tmp;
		}

		 public void run() {

			try {
				mSock.connect();
				mIn = new BufferedReader(new InputStreamReader(mSock.getInputStream()), 128);
				mOut = mSock.getOutputStream();
			} catch (IOException e1) {
				Log.e(TAG, "connect() socket failed", e1);
				try {
					mSock.close();
				} catch (IOException e2) {
					Log.e(TAG, "close() of connect socket failed", e2);
				}
				mHarleyDroidService.disconnected(HarleyDroid.STATUS_ERROR);
				return;
			}

			mHarleyDroidService.connected();
		}

		public void cancel() {
			try {
				mSock.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of connect socket failed", e);
			}
		}
	}

	private class PollThread extends Thread {
		private boolean stop = false;

		public void run() {
			int errors = 0;

			mHarleyDroidService.startedPoll();

			while (!stop) {
				String line;

				try {
					line = readLine(ATMA_TIMEOUT);
				} catch (IOException e1) {
					mHarleyDroidService.disconnected(HarleyDroid.STATUS_NODATA);
					// socket is already closed...
					return;
				}

				// strip off timestamp
				if (line.indexOf('J') != -1) {
					line = line.substring(line.indexOf('J') + 1).trim();
					if (J1850.parse(line.getBytes(), mHD))
						errors = 0;
					else
						++errors;
				}
				else
					++errors;

				if (errors > MAX_ERRORS) {
					try {
						mSock.close();
					} catch (IOException e2) {
					}
					mHarleyDroidService.disconnected(HarleyDroid.STATUS_TOOMANYERRORS);
					return;
				}
			}
			try {
				mSock.close();
			} catch (IOException e2) {
			}
			mHarleyDroidService.stoppedPoll();
		}

		public void cancel() {
			stop = true;
		}
	}

	private class SendThread extends Thread {
		private String mType, mTA, mSA, mSend, mExpect;

		public SendThread(String type, String ta, String sa, String send, String expect) {
			mType = type;
			mTA = ta;
			mSA = sa;
			mSend = send;
			mExpect = expect;
		}

		public void run() {
			String recv;

			try {
				recv = chat(mType + mTA + mSA + mSend, mExpect, AT_TIMEOUT);
			} catch (IOException e1) {
				mHarleyDroidService.disconnected(HarleyDroid.STATUS_NODATA);
				// socket is already closed...
				return;
			}

			// split into lines and strip off timestamp
			String lines[] = recv.split("\n");
			for (int i = 0; i < lines.length; ++i)
				if (lines[i].indexOf('J') != -1)
					J1850.parse(lines[i].substring(lines[i].indexOf('J') + 1).trim().getBytes(), mHD);

			mHarleyDroidService.sendDone();
		}
	}
}
