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

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import java.lang.ref.WeakReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONArray;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

// BusesWidgetUpdate
@SuppressWarnings("deprecation")
public class BusesWidgetUpdate extends Service
{
    public static final String TAG = "BusesWidgetUpdate";
    public static final String EXTRA_UPDATE_DONE =
        "org.billthefarmer.buses.EXTRA_UPDATE_DONE";

    public static final int RESET_DELAY = 5 * 1000;
    public static final int RETRY_DELAY = 30 * 1000;
    public static final int STOP_DELAY = 5 * 60 * 1000;

    private Timer timer;
    private Handler handler;
    private boolean stopUpdate;

    // onCreate
    @Override
    public void onCreate()
    {
        // Get timer
        timer = new Timer();

        // Get handler
        handler = new Handler(Looper.getMainLooper());

        if (BuildConfig.DEBUG)
            Log.d(TAG, "onCreate " + handler);
    }

    // onStartCommand
    @Override
    @SuppressWarnings("deprecation")
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "onStartCommand " + intent);

        startBusesTask();

        // Schedule update
        timer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                handler.post(() -> startBusesTask());
            }
        }, RETRY_DELAY, RETRY_DELAY);

        // Schedule cancel
        timer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                handler.post(() ->
                {
                    timer.cancel();
                    stopSelf();
                });
            };
        }, STOP_DELAY);

        // handler.postDelayed(() ->
        // {
        //     stopUpdate = true;
        //     stopSelf();
        // }, STOP_DELAY);

        return START_NOT_STICKY;
    }

    // onBind
    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    // startBusesTask
    private void startBusesTask()
    {
        // Get preferences
        SharedPreferences preferences =
            PreferenceManager.getDefaultSharedPreferences(this);
        String url = preferences.getString(Buses.PREF_URL, null);

        BusesTask task = new BusesTask(this);
        task.execute(url);

        // Get the layout for the widget
        RemoteViews views = new
            RemoteViews(getPackageName(), R.layout.widget);
        views.setViewVisibility(R.id.refresh, View.INVISIBLE);
        views.setViewVisibility(R.id.progress, View.VISIBLE);

        // Get manager
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        ComponentName provider = new
            ComponentName(this, BusesWidgetProvider.class);

        int appWidgetIds[] = appWidgetManager.getAppWidgetIds(provider);
        appWidgetManager.partiallyUpdateAppWidget(appWidgetIds, views);

        handler.postDelayed(() ->
        {
            views.setViewVisibility(R.id.refresh, View.VISIBLE);
            views.setViewVisibility(R.id.progress, View.INVISIBLE);
            appWidgetManager.partiallyUpdateAppWidget(appWidgetIds, views);
        }, RESET_DELAY);
    }

    // BusesTask
    private static class BusesTask
            extends AsyncTask<String, Void, Document>
    {
        private WeakReference<BusesWidgetUpdate> busesWeakReference;

        // BusesTask
        public BusesTask(BusesWidgetUpdate buses)
        {
            busesWeakReference = new WeakReference<>(buses);
        }

        // doInBackground
        @Override
        protected Document doInBackground(String... params)
        {
            final BusesWidgetUpdate buses = busesWeakReference.get();
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
                e.printStackTrace();
            }

            return null;
        }

        // onPostExecute
        @Override
        protected void onPostExecute(Document doc)
        {
            final BusesWidgetUpdate buses = busesWeakReference.get();
            if (buses == null)
                return;

            if (doc == null)
            {
                buses.stopSelf();
                return;
            }

            if (BuildConfig.DEBUG)
                Log.d(TAG, "onPostExecute " + doc.head());

            String title = doc.select("h2").first().text();

            List<String> list = new ArrayList<>();
            Elements tds = doc.select("td.Number");
            for (Element td: tds)
            {
                String n = td.select("p.Stops > a[href]").text();
                td = td.nextElementSibling();
                String s = td.select("p.Stops").first().text();
                String bus = String.format(Locale.getDefault(),
                                           Buses.BUS_FORMAT, n, s);
                list.add(bus);
            }

            // Get preferences
            SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(buses);
            // Get editor
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(Buses.PREF_TITLE, title);
            JSONArray busArray = new JSONArray(list);
            editor.putString(Buses.PREF_LIST, busArray.toString());
            editor.apply();

            // Get manager
            AppWidgetManager appWidgetManager =
                AppWidgetManager.getInstance(buses);
            ComponentName provider = new
                ComponentName(buses, BusesWidgetProvider.class);

            int appWidgetIds[] = appWidgetManager.getAppWidgetIds(provider);
            Intent broadcast = new
                Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            broadcast.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,
                               appWidgetIds);
            broadcast.putExtra(EXTRA_UPDATE_DONE, true);
            buses.sendBroadcast(broadcast);

            if (BuildConfig.DEBUG)
                Log.d(TAG, "Broadcast " + broadcast);

            // if (!buses.stopUpdate)
            //     buses.handler.postDelayed(() ->
            //         buses.startBusesTask(), RETRY_DELAY);
        }
    }
}
