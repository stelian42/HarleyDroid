#! /usr/bin/python

import sys, time, zipfile

def kml_header(name, description):
	print '<?xml version="1.0" encoding="UTF-8"?>'
	print '<kml xmlns="http://earth.google.com/kml/2.0" xmlns:atom="http://www.w3.org/2005/Atom">'
	print '<Document>'
	print '<atom:author><atom:name>HarleyDroid</atom:name></atom:author>\n'
	print '<name>%s</name>' % name
	print '<description>%s</description>' % description
	print '<Style id="trk"><LineStyle><color>7f0000ff</color><width>4</width></LineStyle></Style>'
	print '<Style id="rpm"><LineStyle><color>7f00ff00</color><width>4</width></LineStyle><PolyStyle><color>5f00ff00</color></PolyStyle></Style>'
	print '<Style id="spd"><LineStyle><color>7fff0000</color><width>4</width></LineStyle><PolyStyle><color>5fff0000</color></PolyStyle></Style>'
	print '<Style id="odotick"><LineStyle><color>7f00ffff</color><width>4</width></LineStyle><PolyStyle><color>5f00ffff</color></PolyStyle></Style>'
	print '<Style id="odopt"><IconStyle><Icon><href>http://google-maps-icons.googlecode.com/files/motorcycle.png</href></Icon><hotSpot x="0.5" y="0.5" xunits="fraction" yunits="fraction"/></IconStyle></Style>'
	print '<Style id="left"><IconStyle><Icon><href>http://google-maps-icons.googlecode.com/files/left.png</href></Icon><hotSpot x="0.5" y="0.5" xunits="fraction" yunits="fraction"/></IconStyle></Style>'
	print '<Style id="right"><IconStyle><Icon><href>http://google-maps-icons.googlecode.com/files/right.png</href></Icon><hotSpot x="0.5" y="0.5" xunits="fraction" yunits="fraction"/></IconStyle></Style>'

def kml_start_track(name, description, style):
	print '<Placemark>'
	print '<name>%s</name>' % name
	print '<description>%s</description>' % description
	print '<styleUrl>%s</styleUrl>' % style
	print '<MultiGeometry>'

def kml_start_line_track(name, description, style, extrude):
	kml_start_track(name, description, style)
	print '<LineString>'
	if extrude:
		print '<extrude>1</extrude>'
		print '<altitudeMode>relativeToGround</altitudeMode>'
	print '<coordinates>'

def kml_end_track():
	print '</MultiGeometry>'
	print '</Placemark>'

def kml_end_line_track():
	print '</coordinates></LineString>'
	kml_end_track()

def kml_footer():
	print '</Document>'
	print '</kml>'

if len(sys.argv) != 2:
	print 'usage: %s <harley log file>' % sys.argv[0]
	sys.exit(1)

f = open(sys.argv[1], 'r')
kmlfile = sys.argv[1].replace('.log','') + '.kml'
kmzfile = sys.argv[1].replace('.log','') + '.kmz'
sys.stdout = open(kmlfile, 'w')

print >> sys.stderr, 'Generating description'
oldlon = oldlat = 0
first_odo = -1
last_odo = -1
desc_date = ''
desc_odo = 0
for line in f:
	line = line[:-1]
	if line.count(',') != 5:
		print >> sys.stderr, 'skipping', line
		continue
	(date, typ, val, lon, lat, alt) = tuple(line.split(','))
	# special test for wrong locale decimal separator...
	if val.count('.') != 0:
		print >> sys.stderr, 'error', line
	if date and not desc_date:
		desc_date = time.asctime(time.strptime(date[:-3], "%Y%m%d%H%M%S"))
	if typ == "ODO":
		if first_odo == -1:
			first_odo = int(val)
		last_odo = int(val)
desc_odo = float(last_odo - first_odo) / 100.0
kml_header(sys.argv[1], "HarleyDroid track\n%s km on %s" % ( desc_odo, desc_date))
f.seek(0)

print >> sys.stderr, 'Generating raw track'
kml_start_line_track('Track', '', '#trk', 0)
oldlon = oldlat = 0
for line in f:
	line = line[:-1]
	if line.count(',') != 5:
		print >> sys.stderr, 'skipping', line
		continue
	(date, typ, val, lon, lat, alt) = tuple(line.split(','))
	if lon and lat and ( lon != oldlon and lat != oldlat):
		print '%s,%s,%s' % (lon, lat, alt),
		oldlon = lon
		oldlat = lat
kml_end_line_track()
f.seek(0)

print >> sys.stderr, 'Generating RPM track'
kml_start_line_track('RPM', '', '#rpm', 1)
oldlon = oldlat = 0
for line in f:
	line = line[:-1]
	if line.count(',') != 5:
		print >> sys.stderr, 'skipping', line
		continue
	(date, typ, val, lon, lat, alt) = tuple(line.split(','))
	if typ == "RPM" and lon and lat and (lon != oldlon and lat != oldlat):
		# RPM is 800 - 6000
		alt = int(val) / 10
		print '%s,%s,%s' % (lon, lat, alt),
		oldlon = lon
		oldlat = lat
kml_end_line_track()
f.seek(0)

print >> sys.stderr, 'Generating Speed track'
kml_start_line_track('Speed', '', '#spd', 1)
oldlon = oldlat = 0
for line in f:
	line = line[:-1]
	if line.count(',') != 5:
		print >> sys.stderr, 'skipping', line
		continue
	(date, typ, val, lon, lat, alt) = tuple(line.split(','))
	if typ == "SPD" and lon and lat and (lon != oldlon and lat != oldlat):
		# SPD is 0 - 160 (km/h)
		alt = val
		print '%s,%s,%s' % (lon, lat, alt),
		oldlon = lon
		oldlat = lat
kml_end_line_track()
f.seek(0)

print >> sys.stderr, 'Generating Odo tick track'
kml_start_line_track('Odometer Tick', '', '#odotick', 1)
oldlon = oldlat = 0
for line in f:
	line = line[:-1]
	if line.count(',') != 5:
		print >> sys.stderr, 'skipping', line
		continue
	(date, typ, val, lon, lat, alt) = tuple(line.split(','))
	if typ == "ODO" and lon and lat and (lon != oldlon and lat != oldlat):
		# ODO tick only
		alt = 100
		print '%s,%s,%s' % (lon, lat, alt),
		oldlon = lon
		oldlat = lat
kml_end_line_track()
f.seek(0)

print >> sys.stderr, 'Generating Odometer points track'
print '<Folder>'
print '<name>Odometer points</name>'
kml_start_track('Odometer points', '', '#odopt')
oldlon = oldlat = 0
oldval = -100
for line in f:
	line = line[:-1]
	if line.count(',') != 5:
		print >> sys.stderr, 'skipping', line
		continue
	(date, typ, val, lon, lat, alt) = tuple(line.split(','))
	if typ == "ODO" and (int(val) - oldval > 100) and lon and lat and (lon != oldlon and lat != oldlat):
		print '<Point>'
		print '<coordinates>'
		print '%s,%s' % (lon, lat)
		print '</coordinates>'
		print '</Point>'
		oldval = int(val)
kml_end_track()
print '</Folder>'
f.seek(0)

print >> sys.stderr, 'Generating Turn Left track'
print '<Folder>'
print '<name>Turn Signals</name>'
kml_start_track('Turn Left', '', '#left')
oldlon = oldlat = 0
for line in f:
	line = line[:-1]
	if line.count(',') != 5:
		print >> sys.stderr, 'skipping', line
		continue
	(date, typ, val, lon, lat, alt) = tuple(line.split(','))
	if typ == "TRN" and (val == "L" or val == "W") and lon and lat and (lon != oldlon and lat != oldlat):
		print '<Point>'
		print '<coordinates>'
		print '%s,%s' % (lon, lat)
		print '</coordinates>'
		print '</Point>'
kml_end_track()
f.seek(0)

print >> sys.stderr, 'Generating Turn Right track'
kml_start_track('Turn Right', '', '#right')
oldlon = oldlat = 0
for line in f:
	line = line[:-1]
	if line.count(',') != 5:
		print >> sys.stderr, 'skipping', line
		continue
	(date, typ, val, lon, lat, alt) = tuple(line.split(','))
	if typ == "TRN" and (val == "R" or val == "W") and lon and lat and (lon != oldlon and lat != oldlat):
		print '<Point>'
		print '<coordinates>'
		print '%s,%s' % (lon, lat)
		print '</coordinates>'
		print '</Point>'
kml_end_track()
print '</Folder>'
f.seek(0)

kml_footer()

sys.stdout.close()
sys.stdout = sys.__stdout__

kmz = zipfile.ZipFile(kmzfile, 'w', zipfile.ZIP_DEFLATED)
kmz.write(kmlfile)
kmz.close()

