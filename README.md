# ![Logo](src/main/res/drawable-hdpi/ic_launcher.png) Buses [![.github/workflows/main.yml](https://github.com/billthefarmer/crossword/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/billthefarmer/buses/actions) [![Release](https://img.shields.io/github/release/billthefarmer/buses.svg?logo=github)](https://github.com/billthefarmer/buses/releases)
[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.svg" alt="Get it on F-Droid" height="80">](https://f-droid.org/packages/org.billthefarmer.buses/)

Geographical GB bus times lookup app.

### Non-free network services
Unfortunately, this app had been tagged by F-Droid as promoting
non-free network services. Actually, this app doesn't promote anything
apart from buses. It does use a web site to get it's data, but then so
do several other bus times apps for various locations also on F-Droid
which haven't been tagged.

![Buses](https://github.com/billthefarmer/billthefarmer.github.io/raw/master/images/Buses.png) ![Zoom](https://github.com/billthefarmer/billthefarmer.github.io/raw/master/images/Buses-zoom.png)

## Intro
Scroll and zoom the map to find a bus stop and tap the map to get bus
times from that stop.

## Search

![Stops](https://github.com/billthefarmer/billthefarmer.github.io/raw/master/images/Buses-stops.png) ![Times](https://github.com/billthefarmer/billthefarmer.github.io/raw/master/images/Buses-times.png)

There are several ways of finding bus times:

 * **Tap the map** &ndash; This will show a list of bus times from the
    nearest stop.
 * **Tap the search button** &ndash; Type in a street name and town, or
    postcode and tap the button on the search widget or keyboard. This
    will show a list of bus stops.
 * **Tap the search button** &ndash; Type in the eight character code
    on the bus stop sign, if it exists, and tap the button on the
    search widget or keyboard. This will show a list of bus times from
    that stop.

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

## Widget
The app widget will show the bus times from the last stop viewed. Tap
the refresh button to update. Due to restrictions in later versions of
android the refresh button will no longer work after a short
period. Tap the widget anywhere to show the app. The widget will
update when the app is dismissed and the refresh button will work
again.

## Permissions
The app will ask for location and file access permissions. The
location permission is to find out where you are. The file access
permission is so the mapping function can cache map tiles. If map
tiles are cached, there is a limited amount of mapping available
without internet access, otherwise map tiles will be downloaded every
time the app is used.
