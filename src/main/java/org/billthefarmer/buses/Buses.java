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
import android.os.AsyncTask;
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
import android.widget.ProgressBar;
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

import java.lang.ref.WeakReference;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import uk.me.jstott.jcoord.LatLng;
import uk.me.jstott.jcoord.OSRef;

public class Buses extends Activity
{
    private final static String TAG = "Buses";

    public final static String CODE = "code";

    private final static String LOCATION = "location";
    private final static String MAPCENTRE = "mapcentre";
    private final static String ZOOMLEVEL = "zoomlevel";
    private final static String SCROLLED = "scrolled";
    private final static String LOCATED = "located";
    private final static String ZOOMED = "zoomed";

    public static final String MULTI_FORMAT =
        "https://nextbuses.mobi/WebView/BusStopSearch/BusStopSearchResults" +
        "?id=%s&submit=Search";

    public static final String SINGLE_FORMAT =
        "https://nextbuses.mobi/WebView/BusStopSearch/BusStopSearchResults/" +
        "%s?currentPage=0";

    public static final String QUERY_FORMAT = "point(%f,%f)";
    public static final String STOP_FORMAT = "%s, %s";
    public static final String URL_FORMAT = "https://nextbuses.mobi%s";
    public static final String BUS_FORMAT = "%s: %s";

    public static final String POINT_PATTERN = ".+POINT\\(.+\\).+";
    public static final String SEARCH_PATTERN = ".*searchMap=true.*";
    public static final String STOP_PATTERN =
        "((nld|man|lin|bou|ahl|her|buc|shr|dvn|rtl|mer|twr|nth|cor|war|ntm|" +
        "sta|bfs|nts|cum|sto|blp|wil|che|dor|knt|glo|woc|oxf|brk|chw|wok|" +
        "dbs|yny|dur|soa|dby|tel|crm|sot|wsx|lan|esu|lec|suf|esx|nwm|dlo|" +
        "lei|mlt|cej|hal|ham|sur|hrt)[a-z]{5})|[0-9]{8}";

    private final static int REQUEST_PERMS = 1;

    private static final int SHORT_DELAY = 5000;
    private static final int LONG_DELAY = 10000;

    private MapView map = null;  
    private MenuItem searchItem;
    private SearchView searchView;
    private ImageButton button;
    private ProgressBar progressBar;
    private Location last = null;
    private Location location = null;
    private LocationManager locationManager;
    private DateFormat dateFormat;
    private LocationListener listener;
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


        if (savedInstanceState == null)
        {
            // Zoom map
            map.getController().setZoom(7.0);

            // Get point
            GeoPoint point = new GeoPoint(52.561928, -1.464854);

            // Centre map
            map.getController().setCenter(point);
        }

        else
        {
            // Get flags
            located = savedInstanceState.getBoolean(LOCATED);
            scrolled = savedInstanceState.getBoolean(SCROLLED);
            zoomed = savedInstanceState.getBoolean(ZOOMED);

            // Get location
            location = savedInstanceState.getParcelable(LOCATION);
            last = location;

            // Set zoom
            map.getController().setZoom(savedInstanceState
                                        .getDouble(ZOOMLEVEL));
            // Get centre
            Location centre = savedInstanceState.getParcelable(MAPCENTRE);
            GeoPoint point = new GeoPoint(centre);

            // Centre map
            map.getController().setCenter(point);
        }

        // Map listener
        map.addMapListener(new MapAdapter()
        {
            public boolean onScroll(ScrollEvent event)
            {
                if (located)
                {
                    if (zoomed)
                        scrolled = true;

                    IGeoPoint point = map.getMapCenter();
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

        button = findViewById(R.id.locate);
        button.setOnClickListener((v) ->
        {
            if (locationManager != null)
            {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED)
                {
                    requestPermissions(new String[]
                    {Manifest.permission.ACCESS_FINE_LOCATION,
                     Manifest.permission.READ_EXTERNAL_STORAGE,
                     Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                       REQUEST_PERMS);
                    return;
                }

                // Get location
                Location location =
                    locationManager.getLastKnownLocation(LocationManager
                                                         .GPS_PROVIDER);
                if (location != null)
                {
                    // Get point
                    GeoPoint p = new GeoPoint(location);
                    // Centre map
                    map.getController().animateTo(p);
                }
            }
        });

        if (located)
            button.setImageResource(R.drawable.ic_action_location_found);

        progressBar = findViewById(R.id.progress);

        listener = new LocationListener()
        {
            @Override
            public void onLocationChanged(Location location)
            {
                // Get point
                GeoPoint point = new GeoPoint(location);

                // Centre and zoom map once
                if (!located)
                {
                    map.getController().animateTo(point, 19.0, null);
                    button.setImageResource(R.drawable
                                            .ic_action_location_found);
                    located = true;
                    zoomed = true;
                }

                if (!scrolled && location.getSpeed() > 0.5)
                    map.getController().animateTo(point);

                if (scrolled)
                    map.postDelayed(() ->
                    {
                        scrolled = false;
                    }, LONG_DELAY);

                else
                    showLocation(location);
            }

            @Override
            public void onStatusChanged(String provider, int status,
                                        Bundle extras) {}
            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}

        };

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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
            {
                requestPermissions(new String[]
                {Manifest.permission.ACCESS_FINE_LOCATION,
                 Manifest.permission.READ_EXTERNAL_STORAGE,
                 Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                   REQUEST_PERMS);
                return;
            }

            Location location =
                locationManager.getLastKnownLocation(LocationManager
                                                     .GPS_PROVIDER);
            if (location != null)
                showLocation(location);

            locationManager
                .requestSingleUpdate(LocationManager.GPS_PROVIDER,
                                     listener, null);
            locationManager
                .requestLocationUpdates(LocationManager.GPS_PROVIDER,
                                        SHORT_DELAY, 0, listener);
        }
    }

    // onPause
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
            locationManager.removeUpdates(listener);
    }

    // onSaveInstanceState
    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);

        outState.putBoolean(LOCATED, located);
        outState.putBoolean(SCROLLED, scrolled);
        outState.putBoolean(ZOOMED, zoomed);

        outState.putParcelable(LOCATION, location);
        IGeoPoint geopoint = map.getMapCenter();
        Location centre = new Location("MapView");
        centre.setLatitude(geopoint.getLatitude());
        centre.setLongitude(geopoint.getLongitude());
        outState.putParcelable(MAPCENTRE, centre);
        outState.putDouble(ZOOMLEVEL, map.getZoomLevelDouble());
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
                    if (locationManager == null)
                        locationManager =
                            (LocationManager)getSystemService(LOCATION_SERVICE);
        }
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
        try
        {
            LatLng coord = new LatLng(lat, lng);
            coord.toOSGB36();

            OSRef OSCoord = coord.toOSRef();

	    double east = OSCoord.getEasting();
	    double north = OSCoord.getNorthing();
            String OSString =
                OSCoord.getOsRefWithPrecisionOf(OSRef.Precision.SIX_DIGITS);

            leftList.add(OSString);
            leftList.add(String.format(Locale.getDefault(),
                                       "%1.0f, %1.0f", east, north));
	}

        catch (Exception e) {}

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

    // alertDialog
    private void alertDialog(int title, String message, int neutralButton)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);

        // Add the buttons
        builder.setNeutralButton(neutralButton, null);

        // Create the AlertDialog
        builder.show();
    }

    // QueryTextListener
    private class QueryTextListener
        implements SearchView.OnQueryTextListener
    {
        private Buses buses;

        // QueryTextListener
        QueryTextListener(Buses buses)
        {
            this.buses = buses;
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
            if (query.matches(STOP_PATTERN))
            {
                String url = String.format(Locale.getDefault(),
                                           SINGLE_FORMAT, query);
                BusesTask task = new BusesTask(buses);
                task.execute(url);
            }

            else
            {
                String url = String.format(Locale.getDefault(),
                                           MULTI_FORMAT, query);

                StopsTask task = new StopsTask(buses);
                task.execute(url);
            }

            progressBar.setVisibility(View.VISIBLE);

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
        Buses buses;

        // GestureListener
        GestureListener(Buses buses)
        {
            this.buses = buses;
        }

        // onSingleTapConfirmed
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e)
        {
            // Get point
            IGeoPoint point = map.getProjection()
                .fromPixels((int) e.getX(), (int) e.getY());

            // Construct query
            String query = String.format(Locale.getDefault(),
                                         QUERY_FORMAT,
                                         point.getLatitude(),
                                         point.getLongitude());

            String url = String.format(Locale.getDefault(),
                                       MULTI_FORMAT, query);
                                       
            StopsTask task = new StopsTask(buses);
            task.execute(url);

            progressBar.setVisibility(View.VISIBLE);
            return true;
        }
    }

    // StopsTask
    private static class StopsTask
            extends AsyncTask<String, Void, Document>
    {
        private WeakReference<Buses> busesWeakReference;

        // FindTask
        public StopsTask(Buses buses)
        {
            busesWeakReference = new WeakReference<>(buses);
        }

        // doInBackground
        @Override
        protected Document doInBackground(String... params)
        {
            final Buses buses = busesWeakReference.get();
            if (buses == null)
                return null;

            String url = params[0];
            // Do web search
            try
            {
                Document doc = Jsoup.connect(url).get();
                return doc;
            }

            catch (Exception e)
            {
                buses.runOnUiThread(() ->
                {
                    buses.alertDialog(R.string.appName,
                                      e.getMessage(),
                                      android.R.string.ok);
                    buses.progressBar.setVisibility(View.GONE);
                });
                e.printStackTrace();
            }

            return null;
        }

        // onPostExecute
        @Override
        protected void onPostExecute(Document doc)
        {
            final Buses buses = busesWeakReference.get();
            if (buses == null)
                return;

            if (doc == null)
                return;

            String title = doc.select("h2").first().text();
            Elements tds = doc.select("td.Number");
            if (tds.first() != null && title.matches(POINT_PATTERN))
            {
                Element td = tds.first().nextElementSibling();
                Element p = td.select("p").first();
                String url =
                    String.format(Locale.getDefault(), URL_FORMAT,
                                  p.select("a[href]").first().attr("href"));

                BusesTask task = new BusesTask(buses);
                task.execute(url);

                buses.progressBar.setVisibility(View.VISIBLE);
                return;
            }

            // Build dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(buses);
            builder.setTitle(title);

            List<String> list = new ArrayList<>();
            List<String> urls = new ArrayList<>();
            for (Element td: tds)
            {
                td = td.nextElementSibling();
                Element p = td.select("p").first();
                String url =
                    String.format(Locale.getDefault(), URL_FORMAT,
                                  p.select("a[href]").first().attr("href"));
                urls.add(url);
                String s =
                    String.format(Locale.getDefault(), STOP_FORMAT,
                                  p.select("a[href]").first().text(),
                                  p.nextElementSibling().text());
                list.add(s);
            }

            Elements italics = doc.select("p.Number > i");
            Elements links = doc.select("p.Stops > a[href]");
            if (urls.isEmpty())
            {
                for (Element link: links)
                {
                    String url =
                        String.format(Locale.getDefault(), URL_FORMAT,
                                      link.attr("href"));
                    if (url.matches(SEARCH_PATTERN))
                        continue;
                    urls.add(url);
                    String s = link.text();
                    list.add(s);
                }
            }

            String[] stops = list.toArray(new String[0]);
            builder.setItems(stops, (dialog, which) ->
            {
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Stop " + list.get(which));

                if (!italics.isEmpty() &&
                    italics.get(which).hasClass("mx-bus_stop"))
                {
                    BusesTask task = new BusesTask(buses);
                    task.execute(urls.get(which));
                }

                else
                {
                    StopsTask task = new StopsTask(buses);
                    task.execute(urls.get(which));
                }

                buses.progressBar.setVisibility(View.VISIBLE);
            });

            builder.setNegativeButton(android.R.string.cancel, null);
            builder.show();

            buses.progressBar.setVisibility(View.GONE);
        }
    }

    // BusesTask
    private static class BusesTask
            extends AsyncTask<String, Void, Document>
    {
        private WeakReference<Buses> busesWeakReference;

        // BusesTask
        public BusesTask(Buses buses)
        {
            busesWeakReference = new WeakReference<>(buses);
        }

        // doInBackground
        @Override
        protected Document doInBackground(String... params)
        {
            final Buses buses = busesWeakReference.get();
            if (buses == null)
                return null;

            String url = params[0];
            // Do web search
            try
            {
                Document doc = Jsoup.connect(url).get();
                return doc;
            }

            catch (Exception e)
            {
                buses.runOnUiThread(() ->
                {
                    buses.alertDialog(R.string.appName,
                                      e.getMessage(),
                                      android.R.string.ok);
                    buses.progressBar.setVisibility(View.GONE);
                });
                e.printStackTrace();
            }

            return null;
        }

        // onPostExecute
        @Override
        protected void onPostExecute(Document doc)
        {
            final Buses buses = busesWeakReference.get();
            if (buses == null)
                return;

            if (doc == null)
                return;

            // Build dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(buses);
            String title = doc.select("h2").first().text();
            builder.setTitle(title);

            List<String> list = new ArrayList<>();
            Elements tds = doc.select("td.Number");
            for (Element td: tds)
            {
                String n = td.select("p.Stops > a[href]").text();
                td = td.nextElementSibling();
                String s = td.select("p.Stops").first().text();
                String bus = String.format(Locale.getDefault(), BUS_FORMAT,
                                           n, s);
                list.add(bus);
            }

            String[] busez = list.toArray(new String[0]);
            builder.setItems(busez, null);

            builder.setNegativeButton(android.R.string.ok, null);
            builder.show();

            buses.progressBar.setVisibility(View.GONE);
        }
    }
}
