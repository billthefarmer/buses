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
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageButton;
import android.widget.SearchView;
import android.widget.TextView;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapAdapter;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.CopyrightOverlay;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private MenuItem searchItem;
    private SearchView searchView;
    private ImageButton button;
    private Location last = null;
    private Location location = null;
    private LocationManager locationManager;
    private DateFormat dateFormat;

    private MyLocationNewOverlay myLocation;
    private TextOverlay leftOverlay;
    private TextOverlay rightOverlay;

    private GestureDetector gestureDetector;

    private boolean located;
    private boolean scrolled;
    private boolean zoomed;

    @Override
    @SuppressWarnings("deprecation")
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Configuration.getInstance()
            .setUserAgentValue(BuildConfig.APPLICATION_ID);
        // load/initialize the osmdroid configuration
        Configuration.getInstance()
            .load(this, PreferenceManager.getDefaultSharedPreferences(this));

        // inflate and create the map
        setContentView(R.layout.main);

        dateFormat = DateFormat.getDateTimeInstance();

        map = (MapView) findViewById(R.id.map);

        map.setTileSource(TileSourceFactory.MAPNIK);
        map.getZoomController()
            .setVisibility(CustomZoomButtonsController
                           .Visibility.SHOW_AND_FADEOUT);
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

        myLocation = new MyLocationNewOverlay(map);
        overlayList.add(myLocation);

        leftOverlay = new TextOverlay(this);
        overlayList.add(leftOverlay);
        leftOverlay.setAlignBottom(false);
        leftOverlay.setAlignRight(false);

        rightOverlay = new TextOverlay(this);
        overlayList.add(rightOverlay);
        rightOverlay.setAlignBottom(false);
        rightOverlay.setAlignRight(true);

        // Zoom map
        map.getController().setZoom(7.0);

        // Get point
        GeoPoint point = new GeoPoint(52.561928, -1.464854);

        // Centre map
        map.getController().setCenter(point);

        // Map listener
        map.addMapListener(new MapAdapter()
        {
            public boolean onScroll(ScrollEvent event)
            {
                if (located)
                {
                    IGeoPoint point = map.getMapCenter();
                    if (zoomed)
                        scrolled = true;

                    Location location = new Location("MapView");
                    location.setLatitude(point.getLatitude());
                    location.setLongitude(point.getLongitude());
                    showLocation(location);
                }

                return true;
            }
        });

        // Gesture detector
        gestureDetector = new GestureDetector(this, new GestureListener(this));
        map.setOnTouchListener((v, event) ->
        {
            gestureDetector.onTouchEvent(event);
            return false;
        });

        button = (ImageButton) findViewById(R.id.locate);
        button.setOnClickListener((v) ->
        {
            if (locationManager != null)
            {
                // Get location
                Location location =
                    locationManager.getLastKnownLocation(LocationManager
                                                         .GPS_PROVIDER);
                if (location != null)
                {
                    // Get point
                    GeoPoint p = new GeoPoint(location.getLatitude(),
                                                  location.getLongitude());
                    // Centre map
                    map.getController().animateTo(p);
                }
            }
        });

        // Check permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
            {
                requestPermissions(new String[]
                {Manifest.permission.ACCESS_FINE_LOCATION,
                 Manifest.permission.READ_EXTERNAL_STORAGE,
                 Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                   REQUEST_PERMS);
                return;
            }
        }

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

        if (locationManager != null)
        {
            Location location =
                locationManager.getLastKnownLocation(LocationManager
                                                     .GPS_PROVIDER);
            if (location != null)
                showLocation(location);

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
						   SHORT_DELAY, 0, this);
        }
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

        if (locationManager != null)
            locationManager.removeUpdates(this);
    }

    // onSaveInstanceState
    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);

        if (location != null)
        {
            double lat = location.getLatitude();
            double lng = location.getLongitude();

            outState.putDouble(LATITUDE, lat);
            outState.putDouble(LONGITUDE, lng);
        }
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

    // onPrepareOptionsMenu
    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        // Set up search view
        searchItem = menu.findItem(R.id.action_search);
        searchView = (SearchView) searchItem.getActionView();

        // Set up search view options and listener
        if (searchView != null)
        {
            searchView.setSubmitButtonEnabled(true);
            searchView.setImeOptions(EditorInfo.IME_ACTION_GO);
            searchView.setOnQueryTextListener(new QueryTextListener(this));
        }

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
            // Search
        case R.id.action_search:
            break;

            // Help
        case R.id.action_help:
            help();
            break;

            // About
        case R.id.action_about:
            about();
            break;

        default:
            return super.onOptionsItemSelected(item);
        }

        return true;
    }

    // onRequestPermissionsResult
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults)
    {
        switch (requestCode)
        {
        case REQUEST_PERMS:
            for (int i = 0; i < grantResults.length; i++)
                if (permissions[i].equals(Manifest.permission
                                          .ACCESS_FINE_LOCATION) &&
                    grantResults[i] == PackageManager.PERMISSION_GRANTED)
                    // Acquire a reference to the system Location
                    // Manager
                    locationManager =
                        (LocationManager)getSystemService(LOCATION_SERVICE);
        }
    }

    @Override
    public void onLocationChanged(Location location)
    {
	// Zoom map once
	if (!zoomed)
	{
	    map.getController().setZoom(19.0);
	    zoomed = true;
	}

	// Get point
	GeoPoint point = new GeoPoint(location);

	// Centre map once
	if (!located)
	{
	    map.getController().setCenter(point);
            button.setImageResource(R.drawable.ic_action_location_found);
	    located = true;
	}

        if (scrolled)
            map.postDelayed(() ->
            {
                scrolled = false;
            }, LONG_DELAY);

        else
            showLocation(location);
    }

    // Show location
    private void showLocation(Location location)
    {
	float  acc = location.getAccuracy();
	double lat = location.getLatitude();
	double lng = location.getLongitude();
	double alt = location.getAltitude();

	String latString = Location.convert(lat, Location.FORMAT_DEGREES);
	String lngString = Location.convert(lng, Location.FORMAT_DEGREES);

        List<String> rightList = new ArrayList<String>();
        rightList.add(String.format(Locale.getDefault(),
                                   "%s, %s", latString, lngString));
        rightList.add(String.format(Locale.getDefault(),
                                       "Altitude: %1.0fm", alt));
        rightList.add(String.format(Locale.getDefault(),
                                       "Accuracy: %1.0fm", acc));
        rightOverlay.setText(rightList);

	long time = location.getTime();

        String date = dateFormat.format(new Date());

        List<String> leftList = new ArrayList<String>();
        leftList.add(date);

	LatLng coord = new LatLng(lat, lng);
	coord.toOSGB36();
	OSRef OSCoord = coord.toOSRef();

	if (OSCoord.isValid())
	{
	    double east = OSCoord.getEasting();
	    double north = OSCoord.getNorthing();
            String OSString =
                OSCoord.getOsRefWithPrecisionOf(OSRef.Precision.SIX_DIGITS);

            leftList.add(OSString);
            leftList.add(String.format(Locale.getDefault(),
                                       "%1.0f, %1.0f", east, north));
	}

        leftOverlay.setText(leftList);
        map.invalidate();
    }

    // help
    private void help()
    {
        // Start help activity
        Intent intent = new Intent(this, Help.class);
        startActivity(intent);
    }

    // about
    private void about()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.appName);

        DateFormat dateFormat = DateFormat.getDateTimeInstance();
        SpannableStringBuilder spannable =
            new SpannableStringBuilder(getText(R.string.version));
        Pattern pattern = Pattern.compile("%s");
        Matcher matcher = pattern.matcher(spannable);
        if (matcher.find())
            spannable.replace(matcher.start(), matcher.end(),
                              BuildConfig.VERSION_NAME);
        matcher.reset(spannable);
        if (matcher.find())
            spannable.replace(matcher.start(), matcher.end(),
                              dateFormat.format(BuildConfig.BUILT));
        builder.setMessage(spannable);

        // Add the button
        builder.setPositiveButton(android.R.string.ok, null);

        // Create the AlertDialog
        Dialog dialog = builder.show();

        // Set movement method
        TextView text = dialog.findViewById(android.R.id.message);
        if (text != null)
            text.setMovementMethod(LinkMovementMethod.getInstance());
    }

    // QueryTextListener
    private class QueryTextListener
        implements SearchView.OnQueryTextListener
    {
        private Context context;

        // QueryTextListener
        QueryTextListener(Context context)
        {
            this.context = context;
        }

        // onQueryTextChange
        @Override
        @SuppressWarnings("deprecation")
        public boolean onQueryTextChange(String newText)
        {
            return true;
        }

        // onQueryTextSubmit
        @Override
        public boolean onQueryTextSubmit(String query)
        {
            // Start search activity
            Intent intent = new Intent(context, Search.class);
            intent.putExtra(CODE, query);
            startActivity(intent);

            // Close text search
            if (searchItem != null && searchItem.isActionViewExpanded())
                searchItem.collapseActionView();

            return true;
        }
    }

    // GestureListener
    private class GestureListener
        extends GestureDetector.SimpleOnGestureListener
    {
        Context context;

        // GestureListener
        GestureListener(Context context)
        {
            this.context = context;
        }

        // onSingleTapConfirmed
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e)
        {
            // Get point
            IGeoPoint point = map.getProjection()
                .fromPixels((int) e.getX(), (int) e.getY());

            if (BuildConfig.DEBUG)
                Log.d(TAG, "Coords " + point.getLatitude() +
                      ", " + point.getLongitude());

            // Construct query
            String query = String.format(Locale.getDefault(), "point(%f,%f)",
                                         point.getLatitude(),
                                         point.getLongitude());
            // Start search activity
            Intent intent = new Intent(context, Search.class);
            intent.putExtra(CODE, query);
            startActivity(intent);

            return true;
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {}
}
