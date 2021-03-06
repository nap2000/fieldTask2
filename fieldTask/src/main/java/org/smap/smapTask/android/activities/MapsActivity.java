/*
 * Copyright (C) 2011 Cloudtec Pty Ltd
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */


package org.smap.smapTask.android.activities;

import android.content.SharedPreferences;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;

import org.odk.collect.android.application.Collect;
import org.smap.smapTask.android.R;
import org.smap.smapTask.android.fragments.MapFragment;
import org.smap.smapTask.android.loaders.MapLocationObserver;

/**
 * Responsible for displaying maps of tasks.
 * 
 * @author Neil Penman 
 */
public class MapsActivity extends FragmentActivity  {

    private LocationManager locationManager;
    private LocationListener locationListener;
    private MapFragment mapFragment = null;
    private MapLocationObserver mo = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Insert the fragment by replacing any existing fragment
        FragmentManager fm = getSupportFragmentManager();
        if (fm.findFragmentById(R.id.map_content_frame) == null) {
            mapFragment = new MapFragment();
            mapFragment.setTabsActivity((MainTabsActivity) getParent());
            fm.beginTransaction().add(android.R.id.content, mapFragment).commit();

            // Listen for new locations
            mo = new MapLocationObserver(getApplicationContext(), mapFragment);
        }


    }

    @Override
    protected void onPause() {
        Log.i("mapsActivity", "---------------- onPause");
        super.onPause();
        mapFragment.pauseMap();
    }

    @Override
    protected void onResume() {
        Log.i("mapsActivity", "---------------- onResume");
        super.onResume();
        mapFragment.resumeMap();
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mapFragment.setUserLocation(Collect.getInstance().getLocation(), false);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i("mapsActivity", "---------------- onStop");

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i("mapsActivity", "---------------- onStart");

    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapFragment.lowMemoryMap();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        mapFragment.destroyMap();
    }


}