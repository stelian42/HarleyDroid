//
// HarleyDroid: Harley Davidson J1850 Data Analyser for Android.
//
// Copyright (C) 2010-2012 Stelian Pop <stelian@popies.net>
// Based on various sources, especially:
//	minigpsd by Tom Zerucha <tz@execpc.com>
//	AVR J1850 VPW Interface by Michael Wolf <webmaster@mictronics.de>
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

import java.util.Arrays;

import android.annotation.SuppressLint;
import android.util.Log;

public class J1850 {
	private static final boolean D = false;
	private static final String TAG = J1850.class.getSimpleName();

	public static final int MAXBUF = 1024;
	// last reading of odometer ticks
	private static int odolast = 0;
	// accumulated odometer ticks (deals with overflow at 0xffff)
	private static int odoaccum = 0;
	// last reading of fuel ticks
	private static int fuellast = 0;
	// accumulated fuel ticks (deals with overflow at 0xffff)
	private static int fuelaccum = 0;

	private static byte[] vin = { '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-' };
	private static byte[] ecmPN = { '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-' };
	private static byte[] ecmCalID = { '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-' };

	public static void resetCounters() {
		odolast = odoaccum = 0;
		fuellast = fuelaccum = 0;
		Arrays.fill(vin, (byte)'-');
		Arrays.fill(ecmPN, (byte)'-');
		Arrays.fill(ecmCalID, (byte)'-');
	}

	static byte[] bytes_to_hex(byte[] in) {
		byte out[] = new byte[MAXBUF];
		int inidx = 0, outidx = 0;

		while (inidx < in.length) {
			int digit0, digit1;

			while (inidx < in.length &&
					Character.digit((char)in[inidx], 16) == -1)
				inidx++;
			if (inidx >= in.length)
				break;
			digit0 = Character.digit((char)in[inidx++], 16);

			while (inidx < in.length &&
					Character.digit((char)in[inidx], 16) == -1)
				inidx++;
			if (inidx >= in.length)
				break;
			digit1 = Character.digit((char)in[inidx++], 16);

			out[outidx++] = (byte) (digit0 * 16 + digit1);
		}
		byte[] ret = new byte[outidx];
		System.arraycopy(out, 0, ret, 0, outidx);
		return ret;
	}

	public static byte crc(byte[] in) {
		int i, j;
		byte crc = (byte)0xff;

		for (i = 0; i < in.length; i++) {
			byte c = in[i];
			for (j = 0; j < 8; ++j) {
				byte poly = 0;
				if ((0x80 & (crc ^ c)) != 0)
					poly = 0x1d;
				crc = (byte) (((crc << 1) & 0xff) ^ poly);
				c <<= 1;
			}
		}
		return crc;
	}

	@SuppressLint("DefaultLocale")
	public static boolean parse(byte[] buffer, HarleyData hd) {
		byte[] in;
		int x;
		int y;

		in = bytes_to_hex(buffer);

		/*
		System.out.print("BUF: ");
		for (int i = 0; i < in.length; i++)
			System.out.print(Integer.toHexString(in[i]) + " ");
		System.out.println("");
		 */

		if (crc(in) != (byte)0xc4) {
			hd.setBadCRC(buffer);
			return false;
		}

		x = y = 0;
		if (in.length >= 4)
			x = ((in[0] << 24) & 0xff000000) |
			    ((in[1] << 16) & 0x00ff0000) |
			    ((in[2] <<  8) & 0x0000ff00) |
			     (in[3]        & 0x000000ff);
		if (in.length >= 6)
			y = ((in[4] << 8) & 0x0000ff00) |
			     (in[5]       & 0x000000ff);

		if (x == 0x281b1002) {
			hd.setRPM(y);
		} else if (x == 0x48291002) {
			hd.setSpeed(y);
		} else if (x == 0xa8491010) {
			hd.setEngineTemp((int)in[4] & 0xff);
		} else if (x == 0xa83b1003) {
			if (in[4] != 0) {
				int gear = 0;
				while ((in[4] >>= 1) != 0)
					gear++;
				hd.setGear(gear);
			} else
				hd.setGear(-1);
		} else if ((x == 0x48da4039) && ((in[4] & 0xfc) == 0)) {
			hd.setTurnSignals(in[4] & 0x03);
		} else if ((x & 0xffffff7f) == 0xa8691006) {
			odolast = y - odolast;
			if (odolast < 0)	// ...could also test for (x & 0x80)
				odolast += 65536;
			odoaccum += odolast;
			odolast = y;
			hd.setOdometer(odoaccum);
		} else if ((x & 0xffffff7f) == 0xa883100a) {
			fuellast = y - fuellast;
			if (fuellast < 0)	// ...could also test for (x & 0x80)
				fuellast += 65536;
			fuelaccum += fuellast;
			fuellast = y;
			hd.setFuel(fuelaccum);
		} else if ((x & 0xffffff7f) == 0xa8836112) {
			hd.setFuelGauge(in[4] & 0x0f,
					((in[3] & 0x80) != 0) ? true : false);
		} else if ((x & 0xffffff5d) == 0x483b4000) {
			if (((int)in[3] & 0xff) == 0x20)
				hd.setNeutral(false);
			else if (((int)in[3] & 0xff) == 0xA0)
				hd.setNeutral(true);
			hd.setClutch((in[3] & 0x80) != 0);
		} else if ((x & 0xffffff7f) == 0x68881003) {
			if ((in[3] & 0x80) != 0)
				hd.setCheckEngine(true);
			else
				hd.setCheckEngine(false);
		} else if (x == 0x0c10f13c) {
			/* this is the read block command, answers are below... */
		} else if (x == 0x0cf1107c) {
			switch (in[4]) {
			case 0x01:
				System.arraycopy(in, 5, ecmPN, 0, 6);
				hd.setECMPN(new String(ecmPN).trim());
				break;
			case 0x02:
				System.arraycopy(in, 5, ecmPN, 6, 6);
				hd.setECMPN(new String(ecmPN).trim());
				break;
			case 0x03:
				System.arraycopy(in, 5, ecmCalID, 0, 6);
				hd.setECMCalID(new String(ecmCalID).trim());
				break;
			case 0x04:
				System.arraycopy(in, 5, ecmCalID, 6, 6);
				hd.setECMCalID(new String(ecmCalID).trim());
				break;
			case 0x0b:
				hd.setECMSWLevel((int)in[5] & 0xff);
				break;
			case 0x0f:
				System.arraycopy(in, 5, vin, 0, 6);
				hd.setVIN(new String(vin).trim());
				break;
			case 0x10:
				System.arraycopy(in, 5, vin, 6, 6);
				hd.setVIN(new String(vin).trim());
				break;
			case 0x11:
				System.arraycopy(in, 5, vin, 12, 5);
				hd.setVIN(new String(vin).trim());
				break;
			default:
				hd.setUnknown(buffer);
			}
		} else if ((x & 0xff0fffff) == 0x6c00f119) {
			/* this is the get DTC command, answers are below... */
			if (D) Log.d(TAG, "DTC start");
		} else if ((x & 0xffff0fff) == 0x6cf10059) {
			if (in[4] != 0 || in[5] != 0) {
				String dtc = "";
				switch ((in[4] & 0xc0) >> 6) {
					case 0: dtc = "P"; break;
					case 1: dtc = "C"; break;
					case 2: dtc = "B"; break;
					case 3: dtc = "U"; break;
				}
				dtc += Integer.toString((in[4] & 0x30) >> 4, 16);
				dtc += Integer.toString(in[4] & 0x0f, 16);
				dtc += Integer.toString((in[5] & 0xf0) >> 4, 16);
				dtc += Integer.toString(in[5] & 0x0f, 16);
				dtc = dtc.toUpperCase();
				if (in[2] == 0x10) {
					/* historic DTC */
					if (D) Log.d(TAG, "historic DTC: " + dtc);
					hd.addHistoricDTC(dtc);
				} else if (in[2] == 0x40) {
					/* current DTC */
					if (D) Log.d(TAG, "current DTC: " + dtc);
					hd.addCurrentDTC(dtc);
				} else
					hd.setUnknown(buffer);
			}
		} else if ((x & 0xff0fffff) == 0x6c00f114) {
			if (D) Log.d(TAG, "DTC clear request");
		} else if ((x & 0xffff0fff) == 0x6cf10054) {
			if (D) Log.d(TAG, "DTC clear reply");
		} else
			hd.setUnknown(buffer);
		return true;
	}

	/*
	public static void main(String[] args) {
		// RPM at 1000 RPM
		//String line = "28 1B 10 02 0f a0 d7";
		// Speed at 100 km/h
		String line = "48 29 10 02 32 00 0f";

		byte[] in = line.getBytes();
		byte[] out = bytes_to_hex(in);

		System.out.print("Input: ");
		for (int i = 0; i < out.length; i++)
			System.out.print(String.format("0x%02x ", out[i]));
		System.out.println("");

		System.out.println("CRC: " + String.format("0x%02x", crc(out)));

		byte[] out_without_crc = new byte[out.length - 1];
		System.arraycopy(out, 0, out_without_crc, 0, out.length - 1);

		System.out.println("Need CRC byte: " + String.format("0x%02x", ~crc(out_without_crc)));

		HarleyData data = new HarleyData();
		if (parse(in, data))
			System.out.println(data);
		else
			System.out.println("Parse error");
	}
	*/
}
