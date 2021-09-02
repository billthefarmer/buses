////////////////////////////////////////////////////////////////////////////////
//
//  Buses - An android bus times app.
//
//  Copyright (C) 2021	Bill Farmer
//
//  This program is free software: you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation, either version 3 of the License, or
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

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import java.util.Locale;

// SearchActivity
public class Search extends Activity
{
    public static final String MULTI_FORMAT =
        "http://nextbuses.mobi/WebView/BusStopSearch/BusStopSearchResults" +
        "?id=%s&submit=Search";

    public static final String SINGLE_FORMAT =
        "http://nextbuses.mobi/WebView/BusStopSearch/BusStopSearchResults/" +
        "%s?currentPage=0";

    private WebView webview;
    private ProgressBar progress;

    // Called when the activity is first created
    @Override
    @SuppressWarnings("deprecation")
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Set content
        setContentView(R.layout.search);

        // Find web view
        webview = findViewById(R.id.webview);
        progress = findViewById(R.id.progress);

        // Enable back navigation on action bar
        ActionBar actionBar = getActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        if (webview != null)
        {
            WebSettings settings = webview.getSettings();

            // Enable zoom
            settings.setBuiltInZoomControls(true);
            settings.setDisplayZoomControls(false);

            // Follow links and set title
            webview.setWebViewClient(new WebViewClient()
            {
                // onPageFinished
                @Override
                public void onPageFinished(WebView view, String url)
                {
                    // Remove progress
                    progress.setVisibility(View.GONE);

                    // Get page title
                    if (view.getTitle() != null)
                        setTitle(view.getTitle());
                }

                // shouldOverrideUrlLoading
                @Override
                public boolean shouldOverrideUrlLoading (WebView view, 
                                                         String url)
                {
                    // Show progress
                    progress.setVisibility(View.VISIBLE);

                    return false;
                }
            });

            if (savedInstanceState != null)
                // Restore state
                webview.restoreState(savedInstanceState);

            else
            {
                // Get the postcode, address or stop code from the
                // intent and create url
                Intent intent = getIntent();
                String code = intent.getStringExtra(Buses.CODE);
                String url = null;
                if (code.matches("[a-z]{8}"))
                    url = String.format(Locale.getDefault(),
                                        SINGLE_FORMAT, code);
                else
                    url = String.format(Locale.getDefault(),
                                        MULTI_FORMAT, code);
                // Show progress
                progress.setVisibility(View.VISIBLE);

                // Do web search
                webview.loadUrl(url);
            }
        }
    }

    // On save instance state
    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);

        if (webview != null)
            // Save state
            webview.saveState(outState);
    }

    // On options item selected
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Get id
        int id = item.getItemId();
        switch (id)
        {
        // Home
        case android.R.id.home:
            // Back navigation
            if (webview != null && webview.canGoBack())
                webview.goBack();

            else
                finish();
            break;

        default:
            return false;
        }

        return true;
    }

    // On back pressed
    @Override
    public void onBackPressed()
    {
        // Back navigation
        if (webview != null && webview.canGoBack())
            webview.goBack();

        else
            finish();
    }
}
