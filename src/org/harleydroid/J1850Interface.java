//
// HarleyDroid: Harley Davidson J1850 Data Analyser for Android.
//
// Copyright (C) 2010,2011 Stelian Pop <stelian@popies.net>
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

public interface J1850Interface {

	// Connects to the J1850 bus. HarleyDroidService.connected()
	// will be called upon completion.
	public void connect(HarleyData hd);
	// Disconnects from the J1850 bus. HarleyDroidService.disconnected()
	// will be called upon completion.
	public void disconnect();
	// Sends a command on the J1850 bus. HarleyDroidService.sendDone()
	// will be called upon completion.
	public void send(String type, String ta, String sa,
					 String command, String expect);
	// Starts polling the J1850 bus. HarleyDroidService.startedPoll()
	// will be called.
	public void startPoll();
	// Stops polling the J1850 bus. HarleyDroidService.stoppedPoll()
	// will be called.
	public void stopPoll();
};
