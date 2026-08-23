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
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.util.JsonReader;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageButton;
import android.widget.ProgressBar;
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

import java.io.InputStreamReader;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import uk.me.jstott.jcoord.LatLng;
import uk.me.jstott.jcoord.OSRef;

@SuppressWarnings("deprecation")
public class Buses extends Activity
{
    public static final String TAG = "Buses";

    public static final String PREF_CODE = "pref_code";
    public static final String PREF_TITLE = "pref_title";
    public static final String PREF_LIST = "pref_list";

    public static final String LOCATION = "location";
    public static final String MAPCENTRE = "mapcentre";
    public static final String ZOOMLEVEL = "zoomlevel";
    public static final String LOCATED = "located";

    public static final String ATCOCODE = "ATCOCode";
    public static final String NAPTANCODE = "NaptanCode";
    public static final String EASTING = "Easting";
    public static final String NORTHING = "Northing";
    public static final String LATITUDE = "Latitude";
    public static final String LONGITUDE = "Longitude";

    public static final String BUSTIMES_URL =
        "https://bustimes.org%s";

    public static final String BUSTIMES_STOP =
        "https://bustimes.org/stops/%s";

    public static final String BUSTIMES_QUERY =
        "https://bustimes.org/search?q=%s";

    public static final String STOPS_PREFIX =
        "/stops/";

    public static final String STOP_FORMAT = "%3s,  %s";
    public static final String BUS_FORMAT = "%3s:  %s  %s";

    public static final String SEARCH_PATTERN = ".*searchMap=true.*";
    public static final String LOCALITY_PATTERN = ".*/ll_.*";
    public static final String STOP_PATTERN =
        "((nld|man|lin|bou|ahl|her|buc|shr|dvn|rtl|mer|twr|nth|cor|war|ntm|" +
        "sta|bfs|nts|cum|sto|blp|wil|che|dor|knt|glo|woc|oxf|brk|chw|wok|" +
        "dbs|yny|dur|soa|dby|tel|crm|sot|wsx|lan|esu|lec|suf|esx|nwm|dlo|" +
        "lei|mlt|cej|hal|ham|sur|hrt)[a-z]{5})|[0-9]{8}";

    private final static int REQUEST_PERMS = 1;
    private final static int POSTCODE_DELAY = 1000;

    private DateFormat dateFormat;
    private ImageButton button;
    private Location location = null;
    private MapView map = null;  
    private MenuItem searchItem;
    private MyLocationNewOverlay myLocation;
    private ProgressBar progressBar;
    private SearchView searchView;
    private TextOverlay leftOverlay;
    private TextOverlay rightOverlay;

    private GestureDetector gestureDetector;
    private ExecutorService executor;
    private Geocoder geocoder;

    private List<Stop> stopList;

    private boolean located;
    private boolean started;
    private boolean loaded;

    @Override
    @SuppressWarnings("deprecation")
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Context context = getApplicationContext();
        Configuration.getInstance()
            .setUserAgentValue(BuildConfig.APPLICATION_ID);
        // load/initialize the osmdroid configuration
        Configuration.getInstance()
            .load(context, PreferenceManager
                  .getDefaultSharedPreferences(context));

        // inflate and create the map
        setContentView(R.layout.main);

        dateFormat = DateFormat.getDateTimeInstance();

        // Set up the map
        map = (MapView)findViewById(R.id.map);
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
        myLocation.enableFollowLocation();
        myLocation.setEnableAutoStop(true);
        myLocation.runOnFirstFix(() ->
        {
            // Run on UI thread
            map.post(() ->
            {
                // Show location
                button.setImageResource(R.drawable.ic_my_location_white_24dp);
                // Zoom in
                map.getController().setZoom(19.0);
                // Set flag;
                located = true;
            });
        });
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
            IGeoPoint point = new GeoPoint(52.561928, -1.464854);

            // Centre map
            map.getController().setCenter(point);
        }

        else
        {
            // Get flag
            located = savedInstanceState.getBoolean(LOCATED);

            // Get location
            location = savedInstanceState.getParcelable(LOCATION);

            // Set zoom
            map.getController().setZoom(savedInstanceState
                                        .getDouble(ZOOMLEVEL));
            // Get centre
            Location centre = savedInstanceState.getParcelable(MAPCENTRE);
            IGeoPoint point = new GeoPoint(centre);

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
                    // Show location from map
                    if (!myLocation.isFollowLocationEnabled())
                    {
                        // Show scrolled location (No height or accuracy)
                        IGeoPoint point = map.getMapCenter();
                        Location location = new Location("MapView");
                        location.setLatitude(point.getLatitude());
                        location.setLongitude(point.getLongitude());
                        showLocation(location);
                    }

                    else
                        // Show location from fix
                        showLocation(myLocation.getLastFix());
                }

                return true;
            }
        });

        // Gesture detector
        gestureDetector = new GestureDetector(this, new GestureListener(this));
        map.setOnTouchListener((v, event) ->
        {
            gestureDetector.onTouchEvent(event);
            return v.performClick();
        });

        // Executor
        executor = Executors.newSingleThreadExecutor();
        geocoder = new Geocoder(this);

        button = findViewById(R.id.locate);
        button.setOnClickListener((v) ->
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

            // Resume following
            myLocation.enableFollowLocation();
            // Centre map
            map.getController().animateTo(myLocation.getMyLocation());
            // Set zoom
            map.getController().setZoom(19.0);
            showLocation(myLocation.getLastFix());
            progressBar.setVisibility(View.GONE);
        });

        progressBar = findViewById(R.id.progress);

        stopReader(R.raw.stops);

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
    }

    @Override
    public void onResume()
    {
        super.onResume();
        // this will refresh the osmdroid configuration on resuming.
        // if you make changes to the configuration, use
        Configuration.getInstance()
            .load(this, PreferenceManager
                  .getDefaultSharedPreferences(this));
        map.onResume(); // needed for compass, my location overlays,
                        // v6.0.0 and up

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
    }

    // onPause
    @Override
    public void onPause()
    {
        super.onPause();
        // this will refresh the osmdroid configuration on resuming.
        // if you make changes to the configuration, use
        Configuration.getInstance()
            .save(this, PreferenceManager
                  .getDefaultSharedPreferences(this));
        map.onPause();  // needed for compass, my location overlays,
                        // v6.0.0 and up

        // Get widget manager
        AppWidgetManager appWidgetManager =
            AppWidgetManager.getInstance(this);
        ComponentName provider = new
            ComponentName(this, BusesWidgetProvider.class);

        int appWidgetIds[] = appWidgetManager.getAppWidgetIds(provider);
        Intent broadcast = new
            Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        broadcast.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        sendBroadcast(broadcast);
    }

    // onSaveInstanceState
    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);

        outState.putBoolean(LOCATED, located);

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
            searchView.setOnQueryTextListener(new QueryTextListener());
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
                    grantResults[i] == PackageManager.PERMISSION_GRANTED);
        }
    }

    // Show location
    private void showLocation(Location location)
    {
        if (location == null)
            return;

	float  acc = location.getAccuracy();
	double lat = location.getLatitude();
	double lng = location.getLongitude();
	double alt = location.getAltitude();

	String latString = Location.convert(lat, Location.FORMAT_DEGREES);
	String lngString = Location.convert(lng, Location.FORMAT_DEGREES);

        List<String> rightList = new ArrayList<String>();
        rightList.add(String.format(Locale.getDefault(),
                                   "%s, %s", latString, lngString));
        if (alt > 0.0)
            rightList.add(String.format(Locale.getDefault(),
                                        "Altitude: %1.0fm", alt));
        if (alt > 0.0)
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
            leftOverlay.setText(leftList);
            map.invalidate();
	}

        catch (Exception e) {}

        if (started)
            return;

        started = true;
        executor.execute(() ->
        {
            try
            {
                List<Address> list = geocoder.getFromLocation(lat, lng, 1);
                String postcode = list.get(0).getPostalCode();
                map.post(() ->
                {
                    leftList.add(postcode);
                    map.invalidate();
                    started = false;
                });
            }

            catch(Exception e) {}
        });
    }

    // stopFromLocation
    private void stopFromLocation(double lat, double lng)
    {

        // Check stops loaded
        if (!loaded)
        {
            map.post(() -> alertDialog(R.string.appName,
                                       getString(R.string.loaded),
                                       android.R.string.ok));
            return;
        }

        try
        {
            LatLng coord = new LatLng(lat, lng);
            coord.toOSGB36();
            OSRef OSCoord = coord.toOSRef();
            double east = OSCoord.getEasting();
            double nort = OSCoord.getNorthing();

            Stop nearest = null;
            double min = Double.MAX_VALUE;
            for (Stop stop: stopList)
            {
                double dist = Math.hypot(east - stop.east, nort - stop.nort);
                if (dist < min)
                {
                    nearest = stop;
                    min = dist;
                }

                if (min < 10.0)
                    break;
            }

            busesFromCode(nearest.code);
        }

        catch (Exception e)
        {
            map.post(() ->
            {
                alertDialog(R.string.appName,
                            e.getMessage(),
                            android.R.string.ok);
                progressBar.setVisibility(View.GONE);
            });

            e.printStackTrace();
        } 
    }

    private void busesFromCode(String code)
    {
        String url = String.format(Locale.getDefault(), BUSTIMES_STOP, code);
        // Do web search
        try
        {
            Document doc = Jsoup.connect(url).get();
            map.post(() ->
            {
                try
                {
                    Element content = doc.selectFirst("#content");

                    // Build dialog
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    String title = content.selectFirst("h1").text();
                    builder.setTitle(title);
                    List<String> list = new ArrayList<>();
                    Element tbody = content.selectFirst("tbody");
                    if (tbody != null)
                    {
                        Elements trs = tbody.select("tr");
                        for (Element tr: trs)
                        {
                            String num = tr.selectFirst("td.nowrap > a").text();
                            String desc =
                                tr.selectFirst("td.nowrap + td").ownText();
                            String time = tr.selectFirst
                                ("td.nowrap + td + td").text();
                            Element td =
                                tr.selectFirst("td.nowrap + td + td + td");
                            if (td != null && td.hasText())
                                time = time + "  " + td.text();
                            String bus =
                                String.format(Locale.getDefault(),
                                              BUS_FORMAT, num, desc, time);
                            list.add(bus);
                        }
                        String[] buses = list.toArray(new String[0]);
                        builder.setItems(buses, null);
                    }

                    else
                    {
                        Element h2 = content.selectFirst("h2");
                        if (h2 != null && h2.hasText())
                            list.add(h2.text());
                        Element ul = content.selectFirst("ul.has-smalls");
                        if (ul != null)
                        {
                            Elements lis = ul.select("li");
                            for (Element li: lis)
                            {
                                if (li.selectFirst("strong.name") != null)
                                {
                                    String num =
                                        li.selectFirst("strong.name").text();
                                    String desc =
                                        li.selectFirst
                                        ("span.description").text();
                                    String bus =
                                        String.format(Locale.getDefault(),
                                                      STOP_FORMAT, num, desc);
                                    list.add(bus);
                                }
                            }
                        }
                        String[] buses = list.toArray(new String[0]);
                        builder.setItems(buses, null);
                    }

                    builder.setNegativeButton(android.R.string.ok, null);
                    builder.show();

                    progressBar.setVisibility(View.GONE);
                }

                catch (Exception e)
                {
                    alertDialog(R.string.appName,
                                e.getMessage(),
                                android.R.string.ok);
                    progressBar.setVisibility(View.GONE);
                    e.printStackTrace();
                }
            });
        }

        catch (Exception e)
        {
            map.post(() ->
            {
                alertDialog(R.string.appName,
                            e.getMessage(),
                            android.R.string.ok);
                progressBar.setVisibility(View.GONE);
                e.printStackTrace();
            });
        }

        // Get context
        Context context = getApplicationContext();
        // Get preferences
        SharedPreferences preferences =
            PreferenceManager.getDefaultSharedPreferences(context);
        // Get editor
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PREF_CODE, code);
        editor.apply();
    }

    // busesFromStop
    private void busesFromStop(String code)
    {
        for (Stop stop: stopList)
        {
            if (stop.text.equals(code))
            {
                code = stop.code;
                break;
            }
        }

        busesFromCode(code);
    }

    // locationsFromText
    private void locationsFromText(String text)
    {
        String uri = String.format(Locale.getDefault(), BUSTIMES_QUERY, text);
        // Do web search
        try
        {
            Document doc = Jsoup.connect(uri).get();
            map.post(() ->
            {
                try
                {
                    Element content = doc.selectFirst("#content");

                    // Build dialog
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    String title = content.selectFirst("h1").text();
                    builder.setTitle(title);

                    List<String> list = new ArrayList<>();
                    List<String> urls = new ArrayList<>();

                    Element ul = content.selectFirst("ul.long");
                    if (ul != null)
                    {
                        Elements lis = ul.select("li");
                        for (Element li: lis)
                        {
                            list.add(li.text());
                            urls.add(li.selectFirst("a").attr("href"));
                        }

                        String[] locations = list.toArray(new String[0]);
                        builder.setItems(locations, (dialog, which) ->
                        {
                            String loc = urls.get(which);
                            executor.execute(() -> stopsFromLocation(loc));
                        });
                    }

                    builder.setNegativeButton(android.R.string.cancel, null);
                    builder.show();

                    progressBar.setVisibility(View.GONE);
                }

                catch (Exception e)
                {
                    alertDialog(R.string.appName,
                                e.getMessage(),
                                android.R.string.ok);
                    progressBar.setVisibility(View.GONE);
                    e.printStackTrace();
                }
            });
        }

        catch (Exception e)
        {
            map.post(() ->
            {
                alertDialog(R.string.appName,
                            e.getMessage(),
                            android.R.string.ok);
                progressBar.setVisibility(View.GONE);
                e.printStackTrace();
            });
        }
    }

    // stopsFromLocation
    private void stopsFromLocation(String loc)
    {
        String uri = String.format(Locale.getDefault(), BUSTIMES_URL, loc);
        // Do web search
        try
        {
            Document doc = Jsoup.connect(uri).get();
            map.post(() ->
            {
                Element content = doc.selectFirst("#content");

                // Build dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                String title = content.selectFirst("h2").text();
                builder.setTitle(title);

                List<String> list = new ArrayList<>();
                List<String> urls = new ArrayList<>();

                Element ul = content.selectFirst("ul.long");
                if (ul != null)
                {
                    Elements lis = ul.select("li");
                    for (Element li: lis)
                    {
                        list.add(li.text());
                        urls.add(li.selectFirst("a").attr("href"));
                    }

                    String[] locations = list.toArray(new String[0]);
                    builder.setItems(locations, (dialog, which) ->
                    {
                        String url = urls.get(which);
                        String code = url.replace(STOPS_PREFIX, "");
                        executor.execute(() -> busesFromCode(code));
                    });
                }

                builder.setNegativeButton(android.R.string.cancel, null);
                builder.show();
            });
        }

        catch (Exception e)
        {
            map.post(() ->
            {
                alertDialog(R.string.appName,
                            e.getMessage(),
                            android.R.string.ok);
                progressBar.setVisibility(View.GONE);
            });
            e.printStackTrace();
        }
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

    // stopReader
    private void stopReader(int id)
    {
        List<Stop> list = new ArrayList<>();
        progressBar.setVisibility(View.VISIBLE);
        executor.execute(() ->
        {
            try (JsonReader reader = new
                 JsonReader(new InputStreamReader
                            (getResources().openRawResource(id))))
            {
                reader.beginArray();
                while (reader.hasNext())
                {
                    String code = null;
                    String text = null;
                    double east = 0;
                    double nort = 0;
                    double lng = 0;
                    double lat = 0;

                    reader.beginObject();
                    while (reader.hasNext())
                    {
                        switch (reader.nextName())
                        {
                        case ATCOCODE:
                            code = reader.nextString();
                            break;

                        case NAPTANCODE:
                            text = reader.nextString();
                            break;

                        case EASTING:
                            east = reader.nextDouble();
                            break;

                        case NORTHING:
                            nort = reader.nextDouble();
                            break;

                        case LATITUDE:
                            lat = reader.nextDouble();
                            break;

                        case LONGITUDE:
                            lng = reader.nextDouble();
                            break;

                        default:
                            reader.skipValue();
                        }
                    }
                    reader.endObject();
                    Stop stop = new Stop(code, text, east, nort, lat, lng);
                    list.add(stop);
                }
                reader.endArray();
                reader.close();
            }

            catch (Exception e)
            {
                map.post(() -> alertDialog(R.string.appName,
                                           e.getMessage(),
                                           android.R.string.ok));
                e.printStackTrace();
            }

            stopList = list;
            map.post(() -> progressBar.setVisibility(View.GONE));
            loaded = true;
        });
    }

    // Stop
    private class Stop
    {
        String code;
        String text;
        double east;
        double nort;
        double lat;
        double lng;

        Stop(String code, String text,
             double east, double nort,
             double lat, double lng)
        {
            this.code = code;
            this.text = text;
            this.east = east;
            this.nort = nort;
            this.lat = lat;
            this.lng = lng;
        }
    }

    // QueryTextListener
    private class QueryTextListener
        implements SearchView.OnQueryTextListener
    {
        // QueryTextListener
        QueryTextListener()
        {
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
                executor.execute(() -> busesFromStop(query));
            }

            else
            {
                executor.execute(() -> locationsFromText(query));
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
            // Check stops loaded
            if (!loaded)
            {
                alertDialog(R.string.appName,
                            context.getString(R.string.loaded),
                            android.R.string.ok);
                return false;
            }

            // Get point
            IGeoPoint point = map.getProjection()
                .fromPixels((int) e.getX(), (int) e.getY());
            executor.execute(() -> stopFromLocation(point.getLatitude(),
                                                    point.getLongitude()));
            progressBar.setVisibility(View.VISIBLE);
            return true;
        }
    }
}
