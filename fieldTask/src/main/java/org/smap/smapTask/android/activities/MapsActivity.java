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
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;

import org.smap.smapTask.android.R;
import org.smap.smapTask.android.fragments.SmapMapFragment;
import org.smap.smapTask.android.loaders.MapLocationObserver;

/**
 * Responsible for displaying maps of tasks.
 * 
 * @author Neil Penman 
 */
public class MapsActivity extends FragmentActivity  {

    private LocationManager locationManager;
    private LocationListener locationListener;
    private SmapMapFragment mapManager = null;
    private MapLocationObserver mo = null;
    private GoogleMap mMap;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Insert the fragment by replacing any existing fragment
        setContentView(R.layout.map_layout);
        mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
        if ( mMap == null ) {
            Toast.makeText(getBaseContext(), getString(org.odk.collect.android.R.string.google_play_services_error_occured),
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);

        mapManager = new SmapMapFragment();
        mapManager.setTabsActivity((MainTabsActivity) getParent());
        // Listen for new locations
        mo = new MapLocationObserver(getApplicationContext(), mapManager);



    }

    @Override
    protected void onPause() {
        Log.i("mapsActivity", "---------------- onPause");
        super.onPause();

        //locationManager.removeUpdates(locationListener);
    }

    @Override
    protected void onResume() {
        Log.i("mapsActivity", "---------------- onResume");
        super.onResume();
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        //mapManager.setUserLocation(Collect.getInstance().getLocation(), false);
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



	
}