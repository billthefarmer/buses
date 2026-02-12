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
import android.location.Location;
import android.location.LocationListener;
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

    public static final String PREF_URL = "pref_url";
    public static final String PREF_TITLE = "pref_title";
    public static final String PREF_LIST = "pref_list";

    public static final String LOCATION = "location";
    public static final String MAPCENTRE = "mapcentre";
    public static final String ZOOMLEVEL = "zoomlevel";
    public static final String LOCATED = "located";

    public static final String MULTI_FORMAT =
        "https://nextbuses.mobi/WebView/BusStopSearch/BusStopSearchResults" +
        "?id=%s&submit=Search";

    public static final String SINGLE_FORMAT =
        "https://nextbuses.mobi/WebView/BusStopSearch/BusStopSearchResults/" +
        "%s?currentPage=0";

    public static final String LOCATION_FORMAT =
        "https://nextbuses.mobi/WebView/BusStopSearch/BusStopSearchResults/" +
        "ll_%f,%f~Location";

    public static final String STOP_FORMAT = "%s, %s";
    public static final String URL_FORMAT = "https://nextbuses.mobi%s";
    public static final String BUS_FORMAT = "%s: %s";

    public static final String SEARCH_PATTERN = ".*searchMap=true.*";
    public static final String LOCALITY_PATTERN = ".*/ll_.*";
    public static final String STOP_PATTERN =
        "((nld|man|lin|bou|ahl|her|buc|shr|dvn|rtl|mer|twr|nth|cor|war|ntm|" +
        "sta|bfs|nts|cum|sto|blp|wil|che|dor|knt|glo|woc|oxf|brk|chw|wok|" +
        "dbs|yny|dur|soa|dby|tel|crm|sot|wsx|lan|esu|lec|suf|esx|nwm|dlo|" +
        "lei|mlt|cej|hal|ham|sur|hrt)[a-z]{5})|[0-9]{8}";

    private final static int REQUEST_PERMS = 1;

    private DateFormat dateFormat;
    private ImageButton button;
    private Location last = null;
    private Location location = null;
    private LocationListener listener;
    private MapView map = null;  
    private MenuItem searchItem;
    private MyLocationNewOverlay myLocation;
    private ProgressBar progressBar;
    private SearchView searchView;
    private TextOverlay leftOverlay;
    private TextOverlay rightOverlay;

    private GestureDetector gestureDetector;
    private ExecutorService executor;

    private boolean located;

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
            last = location;

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
            v.performClick();
            return false;
        });

        // Executor
        executor = Executors.newSingleThreadExecutor();

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
        });

        progressBar = findViewById(R.id.progress);

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
            leftOverlay.setText(leftList);
            map.invalidate();
	}

        catch (Exception e) {}
    }

    // stopsFromLocation
    private void stopsFromLocation(String url)
    {
        // Do web search
        try
        {
            Document doc = Jsoup.connect(url).get();
            map.post(() ->
            {
                Elements tds = doc.select("td.Number");
                try
                {
                    if (tds.first() != null)
                    {
                        Element td = tds.first().nextElementSibling();
                        Element p = td.select("p").first();
                        String url2 =
                            String.format(Locale.getDefault(), URL_FORMAT,
                                          p.select("a[href]").first()
                                          .attr("href"));
                        executor.execute(() -> busesFromStop(url2));
                        progressBar.setVisibility(View.VISIBLE);
                    }

                    else
                    {
                        String title = doc.select("h2").first().text();
                        String message = doc.select("h5").first().text();
                        // Build dialog
                        AlertDialog.Builder builder =
                            new AlertDialog.Builder(this);
                        builder.setTitle(title);
                        builder.setMessage(message);
                        builder.setNegativeButton(android.R.string.ok, null);
                        builder.show();
                        progressBar.setVisibility(View.GONE);
                    }
                }

                catch (Exception e)
                {
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "stopsFromLocation " +
                              tds.first().outerHtml());
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
            });
            e.printStackTrace();
        }
    }

    // busesFromStop
    private void busesFromStop(String url)
    {
        // Do web search
        try
        {
            Document doc = Jsoup.connect(url).get();
            map.post(() ->
            {
                // Build dialog
                AlertDialog.Builder builder =
                    new AlertDialog.Builder(this);
                String title = doc.select("h2").first().text();
                builder.setTitle(title);

                List<String> list = new ArrayList<>();
                Elements tds = doc.select("td.Number");
                for (Element td: tds)
                {
                    String n = td.select("p.Stops > a[href]").text();
                    td = td.nextElementSibling();
                    String s = td.select("p.Stops").first().text();
                    String bus = String.format(Locale.getDefault(),
                                               BUS_FORMAT,
                                               n, s);
                    list.add(bus);
                }

                String[] buses = list.toArray(new String[0]);
                builder.setItems(buses, null);

                builder.setNegativeButton(android.R.string.ok, null);
                builder.show();

                progressBar.setVisibility(View.GONE);

                // Get context
                Context context = getApplicationContext();
                // Get preferences
                SharedPreferences preferences =
                    PreferenceManager.getDefaultSharedPreferences(context);
                // Get editor
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(PREF_TITLE, title);
                JSONArray busArray = new JSONArray(list);
                editor.putString(PREF_LIST, busArray.toString());
                editor.apply();
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

        // Get context
        Context context = getApplicationContext();
        // Get preferences
        SharedPreferences preferences =
            PreferenceManager.getDefaultSharedPreferences(context);
        // Get editor
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PREF_URL, url);
        editor.apply();
    }

    // stopsFromText
    private void stopsFromText(String url)
    {
        // Do web search
        try
        {
            Document doc = Jsoup.connect(url).get();
            map.post(() ->
            {
                String title = doc.select("h2").first().text();
                Elements tds = doc.select("td.Number");
                // Build dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(title);

                List<String> list = new ArrayList<>();
                List<String> urls = new ArrayList<>();

                // Location
                if (tds.isEmpty())
                {
                    Elements links = doc.select("p.Stops > a[href]");

                    try
                    {
                        for (Element link: links)
                        {
                            String url2 =
                                String.format(Locale.getDefault(), URL_FORMAT,
                                              link.attr("href"));
                            if (url.matches(SEARCH_PATTERN))
                                continue;

                            urls.add(url2);
                            String s = link.text();
                            list.add(s);
                        }
                    }

                    catch (Exception e)
                    {
                        if (BuildConfig.DEBUG)
                            Log.d(TAG, "StopsFromText " + links.first()
                                  .parent().outerHtml());
                        e.printStackTrace();
                    }
                }

                else
                {
                    try
                    {
                        for (Element td: tds)
                        {
                            td = td.nextElementSibling();
                            Element p = td.select("p").first();
                            String url2 =
                                String.format(Locale.getDefault(), URL_FORMAT,
                                              p.select("a[href]").first()
                                              .attr("href"));
                            urls.add(url2);
                            String s =
                                String.format(Locale.getDefault(), STOP_FORMAT,
                                              p.select("a[href]")
                                              .first().text(),
                                              p.nextElementSibling().text());
                            list.add(s);
                        }
                    }

                    catch (Exception e)
                    {
                        if (BuildConfig.DEBUG)
                            Log.d(TAG, "StopsFromText " + tds.first()
                                  .parent().outerHtml());
                        e.printStackTrace();
                    }
                }

                String[] stops = list.toArray(new String[0]);
                builder.setItems(stops, (dialog, which) ->
                {
                    String url2 = urls.get(which);
                    if (url2.matches(LOCALITY_PATTERN))
                        executor.execute(() -> stopsFromText(url2));

                    else
                        executor.execute(() -> busesFromStop(url2));

                    progressBar.setVisibility(View.VISIBLE);
                });

                builder.setNegativeButton(android.R.string.cancel, null);
                builder.show();

                progressBar.setVisibility(View.GONE);
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
                String url = String.format(Locale.getDefault(),
                                           SINGLE_FORMAT, query);
                executor.execute(() -> busesFromStop(url));
            }

            else
            {
                String url = String.format(Locale.getDefault(),
                                           MULTI_FORMAT, query);

                executor.execute(() -> stopsFromText(url));
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
            // Get point
            IGeoPoint point = map.getProjection()
                .fromPixels((int) e.getX(), (int) e.getY());
            String url =
                String.format(Locale.getDefault(), LOCATION_FORMAT,
                              point.getLatitude(), point.getLongitude());
            executor.execute(() -> stopsFromLocation(url));
            progressBar.setVisibility(View.VISIBLE);
            return true;
        }
    }
}
