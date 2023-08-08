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
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import org.json.JSONArray;

// BusesWidgetProvider
@SuppressWarnings("deprecation")
public class BusesWidgetProvider extends AppWidgetProvider
{
    public static final String TAG = "BusesWidgetProvider";

    private boolean updateDone;

    // onReceive
    @Override
    public void onReceive(Context context, Intent intent)
    {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "onReceive " + intent);

        updateDone = intent.getBooleanExtra
            (BusesWidgetUpdate.EXTRA_UPDATE_DONE, false);

        super.onReceive(context, intent);
    }

    // onAppWidgetOptionsChanged
    @Override
    public void onAppWidgetOptionsChanged(Context context,
                                          AppWidgetManager appWidgetManager,
                                          int appWidgetId,
                                          Bundle newOptions)
    {
        // Update widget, ignore options
        int[] appWidgetIds = {appWidgetId};
        onUpdate(context, appWidgetManager, appWidgetIds);
    }

    // onUpdate
    @Override
    public void onUpdate(Context context,
                         AppWidgetManager appWidgetManager,
                         int[] appWidgetIds)
    {
        // Get preferences
        SharedPreferences preferences =
            PreferenceManager.getDefaultSharedPreferences(context);
        String title =
            preferences.getString(Buses.PREF_TITLE,
                                  context.getString(R.string.appName));
        String listJSON = preferences.getString(Buses.PREF_LIST, null);

        if (BuildConfig.DEBUG)
            Log.d(TAG, "List " + listJSON);

        StringBuilder buffer = new StringBuilder();
        // Check list
        if (listJSON != null)
        {
            try
            {
                // Update list from JSON array
                JSONArray listArray = new JSONArray(listJSON);
                for (int i = 0; !listArray.isNull(i); i++)
                    buffer.append(listArray.getString(i))
                        .append(System.getProperty("line.separator"));
            }

            catch (Exception e) {}
        }

        if (BuildConfig.DEBUG)
            Log.d(TAG, "Buffer " + buffer);

        // Create an Intent to launch Buses
        Intent intent = new Intent(context, Buses.class);
        //noinspection InlinedApi
        PendingIntent pendingIntent =
            PendingIntent.getActivity(context, 0, intent,
                                      PendingIntent.FLAG_UPDATE_CURRENT |
                                      PendingIntent.FLAG_IMMUTABLE);
        // Create an Intent to update widget
        Intent refresh = new Intent(context, BusesWidgetRefresh.class);
        //noinspection InlinedApi
        PendingIntent refreshIntent =
            PendingIntent.getActivity(context, 0, refresh,
                                      PendingIntent.FLAG_UPDATE_CURRENT |
                                      PendingIntent.FLAG_IMMUTABLE);

        // Get the layout for the widget and attach an on-click
        // listener to the view.
        RemoteViews views = new
            RemoteViews(context.getPackageName(), R.layout.widget);
        views.setOnClickPendingIntent(R.id.widget, pendingIntent);
        views.setOnClickPendingIntent(R.id.refresh, refreshIntent);
        views.setViewVisibility(R.id.refresh, View.VISIBLE);
        views.setViewVisibility(R.id.progress, View.INVISIBLE);
        views.setTextViewText(R.id.title, title);
        views.setTextViewText(R.id.list, buffer);

        // Tell the AppWidgetManager to perform an update on the app
        // widgets.
        appWidgetManager.updateAppWidget(appWidgetIds, views);

        // Update done
        if (updateDone)
            return;

        // Start update service, won't work on android 10+
        try
        {
            Intent update = new Intent(context, BusesWidgetUpdate.class);
            context.startService(update);

            if (BuildConfig.DEBUG)
                Log.d(TAG, "Update " + update);
        }

        catch (Exception e)
        {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Update " + e);
        }
    }
}
