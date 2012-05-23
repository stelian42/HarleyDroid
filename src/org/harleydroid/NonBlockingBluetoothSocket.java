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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
//import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class NonBlockingBluetoothSocket extends Thread
{
	private static final boolean D = false;
	private static final String TAG = NonBlockingBluetoothSocket.class.getSimpleName();

	//private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

	private BluetoothSocket mSock = null;
	private BufferedReader mIn;
	private OutputStream mOut;
	private LinkedBlockingQueue<String> queue;

	public void connect(BluetoothDevice device) throws IOException {
		if (D) Log.d(TAG, "" + System.currentTimeMillis() + " connect");

		BluetoothAdapter.getDefaultAdapter().cancelDiscovery();

		try {
			//tmp = mDevice.createRfcommSocketToServiceRecord(SPP_UUID);
			Method m = device.getClass().getMethod("createRfcommSocket", new Class[] { int.class });
			mSock = (BluetoothSocket) m.invoke(device, 1);
		} catch (Exception e) {
			Log.e(TAG, "create bluetooth socket: "+ e);
			throw new IOException("createRfcommSocket() failed");
		}

		try {
			mSock.connect();
			mIn = new BufferedReader(new InputStreamReader(mSock.getInputStream()), 128);
			mOut = mSock.getOutputStream();
			queue = new LinkedBlockingQueue<String>();
			start();
		} catch (IOException e) {
			Log.e(TAG, "connect() failed", e);
			mSock = null;
			throw e;
		}
	}

	public void close() {
		if (D) Log.d(TAG, "" + System.currentTimeMillis() + " close()");

		if (mSock != null) {
			try {
				mSock.close();
			} catch (IOException e) {
				Log.e(TAG, "close() failed", e);
			}
			mSock = null;
		}
	}

	public String readLine(long timeout) throws TimeoutException {
		String line = null;
		try {
			line = queue.poll(timeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			if (D) Log.e(TAG, "" + System.currentTimeMillis() + " readLine() interrupted: " + e);
		}
		if (line == null) {
			if (D) Log.d(TAG, "" + System.currentTimeMillis() + " readLine() timeout");
			throw new TimeoutException(null);
		}
		if (D) Log.d(TAG, "" + System.currentTimeMillis() + " readLine (" + line.length() + "): " + line);
		return line;
	}

	public void writeLine(String line) throws IOException {
		line += "\r";
		if (D) Log.d(TAG, "" + System.currentTimeMillis() + " writeLine: " + line);
		mOut.write(myGetBytes(line));
		mOut.flush();
	}

	public String chat(String send, String expect, long timeout) throws IOException, TimeoutException {
		StringBuilder result = new StringBuilder();
		writeLine(send);
		try {
			long start = System.currentTimeMillis();
			while (timeout > 0) {
				String line = readLine(timeout);
				long now = System.currentTimeMillis();
				timeout -= (now - start);
				start = now;
				result.append(line + "\n");
				if (line.indexOf(expect) != -1)
					return result.toString();
			}
			throw new TimeoutException(null);
		} catch (TimeoutException e) {
			throw new TimeoutException(result.toString());
		}
	}

	static byte[] myGetBytes(String s, int start, int end) {
		byte[] result = new byte[end - start];
		for (int i = start; i < end; i++) {
			result[i] = (byte) s.charAt(i);
		}
		return result;
	}

	static byte[] myGetBytes(String s) {
		return myGetBytes(s, 0, s.length());
	}

	public void run() {
		setName("NonBlockingBluetoothSocket Thread");
		try {
			while (true) {
				String line = mIn.readLine().trim();
				if (line.length() > 0)
					queue.add(line);
			}
		} catch (IOException e) {
			if (D) Log.e(TAG, "" + System.currentTimeMillis() + " mReadThread exception: " + e);
		}
	}
}
