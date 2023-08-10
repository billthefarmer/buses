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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

// BusesWidgetRefresh
@SuppressWarnings("deprecation")
public class BusesWidgetRefresh extends Activity
{
    public static final String TAG = "BusesWidgetRefresh";

    // On create
    @Override
    @SuppressWarnings("deprecation")
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if (BuildConfig.DEBUG)
            Log.d(TAG, "onCreate " + getIntent());

        // Start update service
        Intent update = new Intent(this, BusesWidgetUpdate.class);
        startService(update);

        if (BuildConfig.DEBUG)
            Log.d(TAG, "Update " + update);

        finish();
    }
}
