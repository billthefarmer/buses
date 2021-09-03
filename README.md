# ![Logo](src/main/res/drawable-hdpi/ic_launcher.png) Buses [![.github/workflows/main.yml](https://github.com/billthefarmer/crossword/workflows/.github/workflows/main.yml/badge.svg)](https://github.com/billthefarmer/buses/actions)

![Buses](https://github.com/billthefarmer/billthefarmer.github.io/raw/master/images/Buses.png) ![Zoom](https://github.com/billthefarmer/billthefarmer.github.io/raw/master/images/Buses-zoom.png)

This app is a gegraphical front end to and wrapper around
the [NextBuses](https://nextbuses.mobi/) web site.

## Intro
Scroll and zoom the map to find a bus stop and tap the map to get a
map and a list of nearby stops. The one you tapped on should be the
first on the list. Tap a link to get bus times from that stop.

## Search
There are several ways of finding bus times:

 * **Tap the map** &ndash; This will send a geographic point to the
    NextBuses web site. This will show a map and list.
 * **Tap the search button** &ndash; Type in a street name and town, or
    postcode and tap the button on the search widget or keyboard. This
    will show a map and list.
 * **Tap the search button** &ndash; Type in the eight character
    code on the bus stop sign, if it exists, and tap the button on the
    search widget or keyboard. This will take you straight to the bus
    times.

## Location
The map shows a person icon in a blue shaded circle. The size of the
circle indicates the accuracy of the location. If the app thinks you
are moving, the person icon is replaced by an arrow. The current time
and date, OS six figure reference, and OS twelve figure reference are
shown in the left upper corner of the map. The current longitude,
latitude, altitude and accuracy are shown in the right upper corner of
the map. If the map is panned, these figures will change to the
current map centre. After a delay they will revert to the current GPS
location.

## Navigate
You can pan and zoom the map using pinch, expand, and swipe gestures,
and by using the zoom in and out buttons. The floating blue **Locate**
button will return the map to your current location. The button icon
centre is filled in once the app has a GPS location.

## Permissions
The app will ask for location and file access permissions. The
location permission is to find out where you are. The file access
permission is so the mapping function can cache map tiles. If map
tiles are cached, there is a limited amount of mapping available
without internet access, otherwise map tiles will be downloaded every
time the app is used.
