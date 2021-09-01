////////////////////////////////////////////////////////////////////////////////
//
//  Buses - An Android bus times app.
//
//  Copyright (C) 2021	Bill Farmer
//
//  This program is free software; you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation; either version 3 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program.  If not, see <http://www.gnu.org/licenses/>.
//
//  Bill Farmer	 william j farmer [at] yahoo [dot] co [dot] uk.
//
///////////////////////////////////////////////////////////////////////////////

package org.billthefarmer.buses;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapAdapter;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.CopyrightOverlay;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.mylocation.SimpleLocationOverlay;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import uk.me.jstott.jcoord.LatLng;
import uk.me.jstott.jcoord.OSRef;

public class Buses extends Activity
    implements LocationListener
{
    private final static String TAG = "Buses";

    public final static String CODE = "code";

    private final static String LOCATION = "location";
    private final static String LATITUDE = "latitude";
    private final static String LONGITUDE = "longitude";

    private final static int REQUEST_PERMS = 1;

    private static final int SHORT_DELAY = 5000;
    private static final int LONG_DELAY = 10000;

    private MapView map = null;  

    private Location last = null;
    private Location location = null;
    private LocationManager locationManager;
    private DateFormat dateFormat;

    private SimpleLocationOverlay simpleLocation;
    private TextOverlay leftOverlay;
    private TextOverlay rightOverlay;

    private boolean located;
    private boolean scrolled;
    private boolean zoomed;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // handle permissions first, before map is created. not
        // depicted here
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            requestPermissionsIfNecessary(new String[]
            {
                // if you need to show the current location, uncomment the
                // line below
                Manifest.permission.ACCESS_FINE_LOCATION,
                // WRITE_EXTERNAL_STORAGE is required in order to show the map
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            });

        // load/initialize the osmdroid configuration, this can be
        // done
        Configuration.getInstance()
            .load(this, PreferenceManager.getDefaultSharedPreferences(this));
        // setting this before the layout is inflated is a good idea
        // it 'should' ensure that the map has a writable location for
        // the map cache, even without permissions if no tiles are
        // displayed, you can try overriding the cache path using
        // Configuration.getInstance().setCachePath see also
        // StorageUtils note, the load method also sets the HTTP User
        // Agent to your application's package name, abusing osm's
        // tile servers will get you banned based on this string

        // inflate and create the map
        setContentView(R.layout.main);

        map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);

        List<Overlay> overlayList = map.getOverlays();

        // Add the overlays
        CopyrightOverlay copyright =
            new CopyrightOverlay(this);
        overlayList.add(copyright);
        copyright.setAlignBottom(true);
        copyright.setAlignRight(false);

        ScaleBarOverlay scale = new ScaleBarOverlay(map);
        scale.setAlignBottom(true);
        scale.setAlignRight(true);
        overlayList.add(scale);

        simpleLocation =
            new SimpleLocationOverlay(this);
        overlayList.add(simpleLocation);

        leftOverlay = new TextOverlay(this);
        overlayList.add(leftOverlay);
        leftOverlay.setAlignBottom(false);
        leftOverlay.setAlignRight(false);

        rightOverlay = new TextOverlay(this);
        overlayList.add(rightOverlay);
        rightOverlay.setAlignBottom(false);
        rightOverlay.setAlignRight(true);

        map.setMapListener(new MapAdapter()
        {
            public boolean onScroll(ScrollEvent event)
            {
                if (located)
                {
                    IGeoPoint point = map.getMapCenter();
                    if (zoomed)
                        scrolled = true;

                    double lat = point.getLatitude();
                    double lng = point.getLongitude();

                    Location location = new Location("MapView");
                    location.setLatitude(lat);
                    location.setLongitude(lng);
                    showLocation(location);
                }

                return true;
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            requestPermissionsIfNecessary(new String[]
            {
                // if you need to show the current location, uncomment the
                // line below
                Manifest.permission.ACCESS_FINE_LOCATION,
                // WRITE_EXTERNAL_STORAGE is required in order to show the map
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            });

        // Acquire a reference to the system Location Manager
        locationManager = (LocationManager)
                          getSystemService(LOCATION_SERVICE);

        if (savedInstanceState != null)
        {
            double lat = savedInstanceState.getDouble(LATITUDE);
            double lng = savedInstanceState.getDouble(LONGITUDE);

            location = new Location(TAG);
            location.setLatitude(lat);
            location.setLongitude(lng);

            last = location;
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        // this will refresh the osmdroid configuration on resuming.
        // if you make changes to the configuration, use
        SharedPreferences prefs =
            PreferenceManager.getDefaultSharedPreferences(this);
        Configuration.getInstance()
            .load(this, PreferenceManager.getDefaultSharedPreferences(this));
        map.onResume(); // needed for compass, my location overlays,
                        // v6.0.0 and up
    
        IMapController mapController = map.getController();

        // Zoom map
        mapController.setZoom(7);

        // Get point
        GeoPoint point = new GeoPoint(52.561928, -1.464854);

        // Centre map
        mapController.setCenter(point);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        // this will refresh the osmdroid configuration on resuming.
        // if you make changes to the configuration, use
        SharedPreferences prefs =
            PreferenceManager.getDefaultSharedPreferences(this);
        Configuration.getInstance().save(this, prefs);
        map.onPause();  // needed for compass, my location overlays,
                        // v6.0.0 and up
    }

    // On create options menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
	// Inflate the menu; this adds items to the action bar if it
	// is present.
	MenuInflater inflater = getMenuInflater();
	inflater.inflate(R.menu.main, menu);

	return true;
    }

    // On options item selected
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
	// Get id
	int id = item.getItemId();
	switch (id)
	{
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults)
    {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (int i = 0; i < grantResults.length; i++)
        {
            permissionsToRequest.add(permissions[i]);
        }
        if (permissionsToRequest.size() > 0)
        {
            requestPermissions(permissionsToRequest.toArray(new String[0]),
                               REQUEST_PERMS);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onLocationChanged(Location location)
    {
	IMapController mapController = map.getController();

	// Zoom map once
	if (!zoomed)
	{
	    mapController.setZoom(14);
	    zoomed = true;
	}

	// Get point
	GeoPoint point = new GeoPoint(location);

	// Centre map once
	if (!located)
	{
	    mapController.setCenter(point);
	    located = true;
	}

	// Set location
	simpleLocation.setLocation(point);

        if (scrolled)
            map.postDelayed(new Runnable()
            {
                // run
                @Override
                public void run()
                {
                    scrolled = false;
                }
            }, LONG_DELAY);

        else
            showLocation(location);
    }

    private void requestPermissionsIfNecessary(String[] permissions)
    {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions)
        {
            if (checkSelfPermission(permission)
                != PackageManager.PERMISSION_GRANTED)
            {
                // Permission is not granted
                permissionsToRequest.add(permission);
            }
        }
        if (permissionsToRequest.size() > 0)
        {
            requestPermissions(permissionsToRequest.toArray(new String[0]),
                               REQUEST_PERMS);
        }
    }

    // Show location
    private void showLocation(Location location)
    {
	float  acc = location.getAccuracy();
	double lat = location.getLatitude();
	double lng = location.getLongitude();
	double alt = location.getAltitude();

	String latString = Location.convert(lat, Location.FORMAT_SECONDS);
	String lngString = Location.convert(lng, Location.FORMAT_SECONDS);

        List<String> rightList = new ArrayList<String>();
        rightList.add(String.format(Locale.getDefault(),
                                   "%s, %s", latString, lngString));
        rightList.add(String.format(Locale.getDefault(),
                                       "Altitude: %1.0fm", alt));
        rightList.add(String.format(Locale.getDefault(),
                                       "Accuracy: %1.0fm", acc));
        rightOverlay.setText(rightList);

	long   time = location.getTime();

        String date;
        if (scrolled)
            date = dateFormat.format(new Date());

        else
            date = dateFormat.format(new Date(time));

        List<String> leftList = new ArrayList<String>();
        leftList.add(date);

	LatLng coord = new LatLng(lat, lng);
	coord.toOSGB36();
	OSRef OSCoord = coord.toOSRef();

	if (true) // (OSCoord.isValid())
	{
	    double east = OSCoord.getEasting();
	    double north = OSCoord.getNorthing();
	    String OSString = OSCoord.toSixFigureString();

            leftList.add(OSString);
            leftList.add(String.format(Locale.getDefault(),
                                       "%1.0f, %1.0f", east, north));
	}

        leftOverlay.setText(leftList);
        map.invalidate();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {}
}
