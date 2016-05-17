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

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import org.smap.smapTask.android.receivers.LocationChangedReceiver;

import com.mapbox.mapboxsdk.annotations.BaseMarkerOptions;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import org.odk.collect.android.application.Collect;
import org.odk.collect.android.preferences.PreferencesActivity;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.PathOverlay;
import org.smap.smapTask.android.R;
import org.smap.smapTask.android.fragments.MapFragment;
import org.smap.smapTask.android.loaders.MapDataLoader;
import org.smap.smapTask.android.loaders.MapEntry;
import org.smap.smapTask.android.loaders.MapLocationObserver;
import org.smap.smapTask.android.loaders.PointEntry;
import org.smap.smapTask.android.loaders.TaskEntry;
import org.smap.smapTask.android.receivers.LocationChangedReceiver;
import org.smap.smapTask.android.utilities.TraceUtilities;
import org.smap.smapTask.android.utilities.Utilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Responsible for displaying maps of tasks.
 * 
 * @author Neil Penman 
 */
public class MapsActivity extends FragmentActivity implements LoaderManager.LoaderCallbacks<MapEntry> {

    private LocationListener locationListener;
    private MapLocationObserver mo = null;
    private static MainTabsActivity tabsActivity;
    private MapsActivity mapsActivity = null;
    private MapView mv;
    private MapboxMap map;

    ItemizedIconOverlay markerOverlay = null;
    ArrayList<Marker> markers = null;
    HashMap<Marker, Integer> markerMap = null;
    private double tasksNorth;
    private double tasksSouth;
    private double tasksEast;
    private double tasksWest;

    Marker userLocationMarker = null;
    Icon userLocationIcon = null;
    Icon accepted = null;
    Icon repeat = null;
    Icon rejected = null;
    Icon complete = null;
    Icon submitted = null;
    Icon triggered = null;
    Icon triggered_repeat = null;

    PathOverlay po = null;
    private PointEntry lastPathPoint;

    private static final String TAG = "MapsActivity";
    private static final int MAP_LOADER_ID = 2;

    private LocationManager locationManager;
    protected PendingIntent locationListenerPendingIntent;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mapsActivity = this;

        setContentView(R.layout.map);
        mv = (MapView) findViewById(R.id.mapView);
        mv.onCreate(savedInstanceState);
        mv.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {

                map = mapboxMap;
                tabsActivity = (MainTabsActivity) getParent();

                // Create icons
                IconFactory iconFactory = IconFactory.getInstance(mapsActivity);
                userLocationIcon = iconFactory.fromDrawable(ContextCompat.getDrawable(mapsActivity, R.drawable.ic_userlocation));
                accepted = iconFactory.fromDrawable(ContextCompat.getDrawable(mapsActivity, R.drawable.ic_task_open));
                repeat = iconFactory.fromDrawable(ContextCompat.getDrawable(mapsActivity, R.drawable.ic_task_repeat));
                rejected = iconFactory.fromDrawable(ContextCompat.getDrawable(mapsActivity, R.drawable.ic_task_reject));
                complete = iconFactory.fromDrawable(ContextCompat.getDrawable(mapsActivity, R.drawable.ic_task_done));
                submitted = iconFactory.fromDrawable(ContextCompat.getDrawable(mapsActivity, R.drawable.ic_task_submitted));
                triggered = iconFactory.fromDrawable(ContextCompat.getDrawable(mapsActivity, R.drawable.ic_task_triggered));
                triggered_repeat = iconFactory.fromDrawable(ContextCompat.getDrawable(mapsActivity, R.drawable.ic_task_triggered_repeat));

                // Customize map with markers, polylines, etc.
                mo = new MapLocationObserver(getApplicationContext(), mapsActivity);
                Intent activeIntent = new Intent(mapsActivity, LocationChangedReceiver.class);
                locationListenerPendingIntent = PendingIntent.getBroadcast(mapsActivity, 1000, activeIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                getLoaderManager().initLoader(MAP_LOADER_ID, null, mapsActivity);       // Get the task locations
            }
        });

    }

    ItemizedIconOverlay.OnItemGestureListener<Marker> onItemGestureListener
            = new ItemizedIconOverlay.OnItemGestureListener<Marker>(){

        @Override
        public boolean onItemLongPress(int arg0, Marker item) {
            return processTouch(item);
        }

        @Override
        public boolean onItemSingleTapUp(int index, Marker item) {
            return processTouch(item);
        }

        public boolean processTouch(Marker item) {

            Integer iPos = markerMap.get(item);

            Log.i(TAG, "process Touch");
            if(iPos != null) {
                /*
                int position = iPos;
                List<TaskEntry> mapTasks = mainTabsActivity.getMapTasks();
                TaskEntry entry = mapTasks.get(position);

                if(entry.locationTrigger != null) {
                    Toast.makeText(
                            getActivity(),
                            getString(R.string.smap_must_start_from_nfc),
                            Toast.LENGTH_SHORT).show();
                } else {
                    mainTabsActivity.completeTask(entry);
                }

                */
            }

            return true;
        }

    };

    /*
     * Update the user location
     */
    public void setUserLocation(Location location, boolean recordLocation) {
        Log.i(TAG, "setUserLocation()");

        if(location != null && map != null) {
            LatLng point = new LatLng(location.getLatitude(), location.getLongitude());

            if (markers == null) {
                markers = new ArrayList<Marker>();
            }
            if (userLocationMarker == null) {
                MarkerOptions markerOptions = new MarkerOptions()
                        .position(point)
                        .title("you")
                        .snippet("your location")
                        .icon(userLocationIcon);

                userLocationMarker = new Marker(markerOptions);
                map.addMarker(markerOptions);

                /*
                if (markerOverlay == null) {
                    markers.add(userLocationMarker);
                    markerOverlay = new ItemizedIconOverlay(mapsActivity, markers, onItemGestureListener);
                    //mv.getOverlays().add(markerOverlay);
                } else {
                    //markerOverlay.addItem(userLocationMarker);
                }
                */
            } else {
                // userLocationMarker.setPoint(point);
                //userLocationMarker.updateDrawingPosition();
                userLocationMarker.setIcon(userLocationIcon);
            }
            if (recordLocation) {
                // updatePath(point);  TODO
            }
            zoomToData(true);
        }
    }

    @Override
    public void onLoaderReset(Loader<MapEntry> loader) {
        clearTasks();
    }

    @Override
    public Loader<MapEntry> onCreateLoader(int id, Bundle args) {
        return new MapDataLoader(mapsActivity);
    }

    private void zoomToData(boolean userLocationChanged) {

        Log.i(TAG, "zoomToData");

        boolean userOutsideBoundingBox = false;
        double north = tasksNorth;
        double south = tasksSouth;
        double east = tasksEast;
        double west = tasksWest;

        // Add current location to bounding box
        if(userLocationMarker != null) {

            /*
            LatLngBounds latLngBounds = new LatLngBounds.Builder().
                    include(userLocationMarker.getPosition()).build();

            map.animateCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 10), 7000);
            */
            CameraPosition position = new CameraPosition.Builder()
                    .target(userLocationMarker.getPosition()) // Sets the new camera position
                    .zoom(17) // Sets the zoom
                    .build();


            map.animateCamera(CameraUpdateFactory
                    .newCameraPosition(position), 7000);
           // double lat = userLocationMarker.getPosition().getLatitude();
           // double lon = userLocationMarker.getPosition().getLongitude();

            /*
            if(lat > north) {
                north = lat;
            }
            if(lat < south) {
                south = lat;
            }
            if(lon > east) {
                east = lon;
            }
            if(lon < west) {
                west = lon;
            }
            */

            /*
            if(userLocationChanged) {
                Rect viewableBox = mv.getClipBounds();
                if(viewableBox != null) {
                    if(!viewableBox.contains(userLocationMarker.getPosition()))
                    if (lat > viewableBox.getLatNorth() ||
                            lat < viewableBox.getLatSouth() ||
                            lon > viewableBox.getLonEast() ||
                            lon < viewableBox.getLonWest()
                            ) {
                        userOutsideBoundingBox = true;
                    }
                } else {
                    userOutsideBoundingBox = true;      // User location being set on resume of activity
                }
            }
            */

        }

        // Add last path point to bounding box
        /*
        if(lastPathPoint != null) {
            double lat = lastPathPoint.lat;
            double lon = lastPathPoint.lon;
            if(lat > north) {
                north = lat;
            }
            if(lat < south) {
                south = lat;
            }
            if(lon > east) {
                east = lon;
            }
            if(lon < west) {
                west = lon;
            }

        }

        // Make sure bounding box is not a point
        if(north == south) {
            north += 0.01;
            south -= 0.01;
        }
        if(east == west) {
            east += 0.01;
            west -= 0.01;
        }
        */

        /*
         * Zoom to the new bounding box only if the task list has changed or the user is outside of the current
         *  viewable area
         *
        if(north > south && east > west) {
            if(!userLocationChanged || userOutsideBoundingBox) {
                BoundingBox bb = new BoundingBox(north, east, south, west);
                mv.zoomToBoundingBox(bb, true, true, true, true);
                mv.fit
            }
        }
        */


    }

    private void clearTasks() {
        if(markerOverlay != null) {
            markerOverlay.removeAllItems();
        }
    }

    @Override
    public void onLoadFinished(Loader<MapEntry> loader, MapEntry data) {
        Log.i(TAG, "######### Load Finished");
        tabsActivity.setLocationTriggers(data.tasks, true);
        showTasks(data.tasks);
        //showPoints(data.points);  TODO
        zoomToData(false);
    }

    private void showTasks(List<TaskEntry> data) {

        tasksNorth = -90.0;
        tasksSouth = 90.0;
        tasksEast = -180.0;
        tasksWest = 180.0;

        markers = new ArrayList<Marker> ();
        markerMap = new HashMap<Marker, Integer> ();

        // Add the user location
        if(userLocationMarker != null) {
            markers.add(userLocationMarker);
        }

        // Add the tasks to the marker array
        int index = 0;
        for(TaskEntry t : data) {
            if(t.type.equals("task")) {
                LatLng ll = getTaskCoords(t);
                if (ll != null) {
                    MarkerOptions markerOptions = new MarkerOptions()
                            .position(ll)
                            .title("task")
                            .snippet("Some task");

                    markerOptions.icon(getIcon(t.taskStatus, t.repeat, t.locationTrigger != null));

                    Marker m = new Marker(markerOptions);

                    markers.add(m);
                    markerMap.put(m, index);
                }
            }
            index++;
        }

        // Remove any existing markers
        if(markerOverlay != null) {
            markerOverlay.removeAllItems();
        }

        // Add the marker layer
        if(markers.size() > 0) {
            if (markerOverlay == null) {
                markerOverlay = new ItemizedIconOverlay(mapsActivity, markers, onItemGestureListener);
                //mv.getOverlays().add(markerOverlay);
            } else {
                markerOverlay.addItems(markers);
            }
        }

    }

    /*
    * Get the icon to represent the passed in task status
    */
    private Icon getIcon(String status, boolean isRepeat, boolean hasTrigger) {

        if(status.equals(Utilities.STATUS_T_REJECTED) || status.equals(Utilities.STATUS_T_CANCELLED)) {
            return rejected;
        } else if(status.equals(Utilities.STATUS_T_ACCEPTED)) {
            if(hasTrigger && !isRepeat) {
                return triggered;
            } else if (hasTrigger && isRepeat) {
                return triggered_repeat;
            } else if(isRepeat) {
                return repeat;
            } else {
                return accepted;
            }
        } else if(status.equals(Utilities.STATUS_T_COMPLETE)) {
            return complete;
        } else if(status.equals(Utilities.STATUS_T_SUBMITTED)) {
            return submitted;
        } else {
            Log.i(TAG, "Unknown task status: " + status);
            return accepted;
        }
    }


    @Override
    protected void onPause() {
        Log.i("mapsActivity", "---------------- onPause");
        super.onPause();
        mv.onPause();

    }

    @Override
    protected void onResume() {
        Log.i("mapsActivity", "---------------- onResume");
        super.onResume();
        mv.onResume();
        //map.setUserLocation(Collect.getInstance().getLocation(), false);
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
        mv.onLowMemory();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        mv.onDestroy();
    }

    /*
 * Get the coordinates of the task and update the bounding box
 */
    private LatLng getTaskCoords(TaskEntry t) {

        double lat = 0.0;
        double lon = 0.0;
        LatLng locn = null;

        if((t.actLat == 0.0) && (t.actLon == 0.0)) {
            lat = t.schedLat;       // Scheduled coordinates of task
            lon = t.schedLon;
        } else  {
            lat = t.actLat;         // Actual coordinates of task
            lon = t.actLon;
        }

        if(lat != 0.0 && lon != 0.0) {
            // Update bounding box
            if(lat > tasksNorth) {
                tasksNorth = lat;
            }
            if(lat < tasksSouth) {
                tasksSouth = lat;
            }
            if(lon > tasksEast) {
                tasksEast = lon;
            }
            if(lat < tasksWest) {
                tasksWest = lon;
            }

            // Create Point
            locn = new LatLng(lat, lon);
        }


        return locn;
    }
	
}