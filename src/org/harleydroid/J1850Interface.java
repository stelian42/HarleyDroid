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

public interface J1850Interface {

	// Connects to the J1850 bus. HarleyDroidService.connected()
	// will be called upon completion.
	public void connect(HarleyData hd);

	// Disconnects immediately from the J1850 bus.
	public void disconnect();

	// Starts polling the J1850 bus. HarleyDroidService.startedPoll()
	// will be called when polling begins.
	public void startPoll();

	// Starts sending a list of commands on the J1850 bus.
	// HarleyDroidService.startedSend() will be called when send begins.
	// will be called upon completion.
	public void startSend(String type[], String ta[], String sa[],
						  String command[], String expect[],
						  int timeout[], int delay);

	// Changes the data to be used in the send thread.
	public void setSendData(String type[], String ta[], String sa[],
							String command[], String expect[],
							int timeout[], int delay);
};
