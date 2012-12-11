#! /usr/bin/python

import sys, time, datetime

if len(sys.argv) != 2:
	print 'usage: %s <harley log file>' % sys.argv[0]
	sys.exit(1)

f = open(sys.argv[1], 'r')

first_odo = -1
last_odo = -1
first_ful = -1
last_ful = -1
first_date = ''
last_date = ''
max_rpm = -1
max_spd = -1
max_etp = -1
clu0 = 0
clu1 = 0
ntr0 = 0
ntr1 = 0
ger0 = 0
ger1 = 0
ger2 = 0
ger3 = 0
ger4 = 0
ger5 = 0
ger6 = 0
ltrn = 0
rtrn = 0
wtrn = 0
fgee = 0
fgen = 0
chk0 = 0
chk1 = 0
crc = 0
unk = 0
vin = ''
epn = ''
eci = ''
esl = ''
dtc = ''
dth = ''
for line in f:
	line = line[:-1]
	if line.count(',') == 5:
		(date, typ, val, lon, lat, alt) = tuple(line.split(','))
	elif line.count(',') > 5:
		(date, typ, other) = tuple(line.split(',', 2))
		if typ == "DTC" or typ == "DTH":
			val = other[:other.rfind(',', 0, other.rfind(',', 0, other.rfind(',')))]
		else:
			print 'skipping', line
			continue
	else:
		print 'skipping', line
		continue
	# special test for wrong locale decimal separator...
	if val.count('.') != 0:
		print 'error', line
	if date and not first_date:
		first_date = time.asctime(time.strptime(date[:-3], "%Y%m%d%H%M%S"))
	if date:
		last_date = time.asctime(time.strptime(date[:-3], "%Y%m%d%H%M%S"))
	if typ == "ODO":
		if first_odo == -1:
			first_odo = int(val)
		last_odo = int(val)
	elif typ == "FUL":
		if first_ful == -1:
			first_ful = int(val)
		last_ful = int(val)
	elif typ == "RPM":
		if max_rpm < int(val):
			max_rpm = int(val)
	elif typ == "SPD":
		if max_spd < int(val):
			max_spd = int(val)
	elif typ == "ETP":
		if max_etp < int(val):
			max_etp = int(val)
	elif typ == "CLU":
		if int(val) == 0:
			clu0 += 1
		else:
			clu1 += 1
	elif typ == "NTR":
		if int(val) == 0:
			ntr0 += 1
		else:
			ntr1 += 1
	elif typ == "GER":
		if int(val) == 0:
			ger0 += 1
		elif int(val) == 1:
			ger1 += 1
		elif int(val) == 2:
			ger2 += 1
		elif int(val) == 3:
			ger3 += 1
		elif int(val) == 4:
			ger4 += 1
		elif int(val) == 5:
			ger5 += 1
		elif int(val) == 6:
			ger6 += 1
	elif typ == "TRN":
		if val == "L":
			ltrn += 1
		elif val == "R":
			rtrn += 1
		elif val == "W":
			wtrn += 1
	elif typ == "FGE":
		if val == "EMPTY":
			fgee += 1
		else:
			fgen += 1
	elif typ == "VIN":
		vin = val
	elif typ == "EPN":
		epn = val
	elif typ == "ECI":
		eci = val
	elif typ == "ESL":
		esl = val
	elif typ == "DTC":
		dtc = val
	elif typ == "DTH":
		dth = val
	elif typ == "CHK":
		if int(val) == 0:
			chk0 += 1
		else:
			chk1 += 1
	elif typ == "CRC":
		crc += 1
	elif typ == "UNK":
		unk += 1
	elif typ == "RAW":
		pass
	else:
		print 'unknown line:', line

print 'HarleyDroid track from file', sys.argv[1]
if vin:
	print 'VIN:', vin
if epn:
	print 'ECM Part Number:', epn
if eci:
	print 'ECM Calibration ID:', eci
if esl:
	print 'ECM Software Level:', esl
if dtc:
	print 'DTC:', dtc
if dth:
	print 'DTH:', dth

if first_date:
	print 'Start date:', first_date
	print 'End date:', last_date
	print 'Duration (hh:mm:ss):', datetime.timedelta(seconds = time.mktime(time.strptime(last_date)) - time.mktime(time.strptime(first_date)))
if first_odo != -1:
	print 'Start odo (km): %.2f' % (float(first_odo) / 100.0)
	print 'End odo (km): %.2f' % (float(last_odo) / 100.0)
	print 'Odometer (km): %.2f' % (float(last_odo - first_odo) / 100.0)
if first_ful != -1:
	print 'Start fuel (l): %.3f' % (float(first_ful) / 1000.0)
	print 'End fuel (l): %.3f' % (float(last_ful) / 1000.0)
	print 'Overall fuel consumption (l): %.3f' % (float(last_ful) / 1000.0)
if first_odo != -1 and first_ful != -1:
	print 'Overall average consumption (l/100km): %.3f' % (10.0 * float(last_ful) / float(last_odo))
	print 'This track fuel consumption (l): %.3f' % (float(last_ful - first_ful) / 1000.0)
	print 'This track average consumption (l/100km): %.3f' % (10.0 * float(last_ful - first_ful) / float(last_odo - first_odo))
if max_rpm != -1:
	print 'Max RPM:', max_rpm
if max_spd != -1:
	print 'Max speed (km/h):', max_spd
if first_date and last_odo - first_odo != 0:
	print 'Average speed (km/h): %.2f' % (36.0 * float(last_odo - first_odo) / (time.mktime(time.strptime(last_date)) - time.mktime(time.strptime(first_date))))
if max_etp != -1:
	print 'Max coolant temp (C):', max_etp
print 'Left turns:', ltrn
print 'Right turns:', rtrn
print 'Warning turns:', wtrn
print 'Fuel gauge empty:', fgee
print 'Fuel gauge other:', fgen
print 'Clutch (off/on):', clu0, '/', clu1
print 'Neutral (off/on):', ntr0, '/', ntr1
print 'Gear (0/1/2/3/4/5/6):', ger0, '/', ger1, '/', ger2, '/', ger3, '/', ger4, '/', ger5, '/', ger6
print 'Check engine (off/on):', chk0, '/', chk1
print 'CRC errors:', crc
print 'Unknown messages:', unk

