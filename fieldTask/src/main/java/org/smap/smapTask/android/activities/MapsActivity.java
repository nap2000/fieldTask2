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

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.mapbox.mapboxsdk.geometry.LatLng;

import org.odk.collect.android.application.Collect;
import org.smap.smapTask.android.R;
import org.smap.smapTask.android.fragments.MapFragment;
import org.smap.smapTask.android.utilities.TraceUtilities;
import org.smap.smapTask.android.utilities.Utilities;

/**
 * Responsible for displaying maps of tasks.
 * 
 * @author Neil Penman 
 */
public class MapsActivity extends FragmentActivity  {

    private LocationManager locationManager;
    private LocationListener locationListener;
    private MapFragment map = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Insert the fragment by replacing any existing fragment
        FragmentManager fm = getSupportFragmentManager();
        if (fm.findFragmentById(R.id.map_content_frame) == null) {
            map = new MapFragment();
            fm.beginTransaction().add(android.R.id.content, map).commit();
        }

        /*
		 * Add a location listener
		 */
        locationManager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );
        locationListener = new LocationListener() {

            @Override
            public void onLocationChanged(Location location) {

                // TODO check for accuracy and discard results that are not accurate
                boolean updatePath = Collect.getInstance().isRecordLocation();
                if(updatePath) {
                    TraceUtilities.insertPoint(location);
                }
                Collect.getInstance().setLocation(location);
                map.setUserLocation(location, updatePath);
            }

            @Override
            public void onProviderDisabled(String arg0) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onStatusChanged(String provider, int status,
                                        Bundle extras) {

            }

        };

    }

    @Override
    protected void onPause() {
        Log.i("mapsActivity", "---------------- onPause");
        super.onPause();

    }

    @Override
    protected void onResume() {
        Log.i("mapsActivity", "---------------- onResume");
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i("mapsActivity", "---------------- onStop");
        locationManager.removeUpdates(locationListener);
    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.i("mapsActivity", "---------------- onStart");
        if ( locationManager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, (float) 6.0, locationListener);
        }


    }



	
}