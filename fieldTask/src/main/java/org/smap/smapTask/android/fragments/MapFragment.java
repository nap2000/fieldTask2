package org.smap.smapTask.android.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;


import org.odk.collect.android.utilities.InfoLogger;
import org.smap.smapTask.android.R;
import org.smap.smapTask.android.activities.MainListActivity;
import org.smap.smapTask.android.activities.MainTabsActivity;
import org.smap.smapTask.android.utilities.Utilities;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.smap.smapTask.android.loaders.MapDataLoader;
import org.smap.smapTask.android.loaders.MapEntry;
import org.smap.smapTask.android.loaders.PointEntry;
import org.smap.smapTask.android.loaders.TaskEntry;

import static org.smap.smapTask.android.R.drawable;

public class MapFragment extends Fragment implements  LocationListener, OnMarkerDragListener, OnMapLongClickListener
{

    private static final String TAG = "MapFragment";
    // PathOverlay po = null;  TODO
    private PointEntry lastPathPoint;

    // ItemizedIconOverlay markerOverlay = null;  TODO
    ArrayList<Marker> markers = null;
    HashMap<Marker, Integer> markerMap = null;
    private double tasksNorth;
    private double tasksSouth;
    private double tasksEast;
    private double tasksWest;

    Marker userLocationMarker = null;

    private static MainTabsActivity mainTabsActivity;

    private GoogleMap mMap;
    private MarkerOptions mMarkerOption;
    private Marker mMarker;
    private LatLng mLatLng;

    private Location mLocation;
    private Button mAcceptLocation;
    private Button mReloadLocation;

    private boolean mRefreshLocation = true;
    private boolean mIsDragged = false;
    private Button mShowLocation;
    private Button mLayers;

    private boolean mGPSOn = false;
    private boolean mNetworkOn = false;

    private double mLocationAccuracy;
    private int mLocationCount = 0;

    private boolean setClear = false;
    private boolean mCaptureLocation = false;
    private Boolean foundFirstLocation = false;
    private TextView mLocationStatus;

    private static final int MAP_LOADER_ID = 2;

    public void setTabsActivity(MainTabsActivity activity) {
        mainTabsActivity = activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {

        View view = inflater.inflate(R.layout.map_layout, container, false);

        // TODO Create icons

        //getLoaderManager().initLoader(MAP_LOADER_ID, null, this);       // Get the task locations


        return view;
    }






    /**
     * Method to show settings  in alert dialog
     * On pressing Settings button will launch Settings Options - GPS
     */
    public void showSettingsAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());

        // Setting Dialog Title
        alertDialog.setTitle("GPS settings");

        // Setting Dialog Message
        alertDialog.setMessage("GPS is not enabled. Do you want to go to settings menu?");

        // On pressing Settings button
        alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                getActivity().startActivity(intent);
            }
        });

        // on pressing cancel button
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        // Showing Alert Message
        alertDialog.show();
    }

    /*
     * Show Tasks TODO
     *
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
                    Marker m = new Marker(mv, t.name, t.taskAddress, ll);
                    m.setIcon(getIcon(t.taskStatus, t.repeat, t.locationTrigger != null));

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
                markerOverlay = new ItemizedIconOverlay(getActivity(), markers, onItemGestureListener);
                mv.getOverlays().add(markerOverlay);
            } else {
                markerOverlay.addItems(markers);
            }
        }

    }


    private void clearTasks() {
        if(markerOverlay != null) {
            markerOverlay.removeAllItems();
        }
    }

    private void showPoints(List<PointEntry> data) {
        if(po == null) {
            Log.i(TAG, "====== po null");
            addPathOverlay();
        } else {
            Log.i(TAG, "====== Removed all points");
            po.removeAllPoints();
            mv.removeOverlay(po);
            addPathOverlay();
        }


        for(int i = 0; i < data.size(); i++) {
            po.addPoint(data.get(i).lat, data.get(i).lon);
        }
        if(data.size() > 0) {
            lastPathPoint = new PointEntry();
            PointEntry lastPoint = data.get(data.size() - 1);
            lastPathPoint.lat = lastPoint.lat;
            lastPathPoint.lon = lastPoint.lon;
        }
    }
    */


    /*
     * Update path TODO
     *
    private void updatePath(LatLng point) {
        if(po == null) {
            addPathOverlay();
        }

        po.addPoint(point);

    }

    private void addPathOverlay() {
        Paint linePaint = new Paint();
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setColor(Color.BLUE);
        linePaint.setStrokeWidth(5);

        po = new PathOverlay().setPaint(linePaint);
        mv.getOverlays().add(po);
    }
    */

    /*
     * Zoom to data TODO
     *
    private void zoomToData(boolean userLocationChanged) {

        boolean userOutsideBoundingBox = false;
        double north = tasksNorth;
        double south = tasksSouth;
        double east = tasksEast;
        double west = tasksWest;

        // Add current location to bounding box
        if(userLocationMarker != null) {
            double lat = userLocationMarker.getPoint().getLatitude();
            double lon = userLocationMarker.getPoint().getLongitude();

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

            if(userLocationChanged) {
                BoundingBox viewableBox = mv.getBoundingBox();
                if(viewableBox != null) {
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
        }

        // Add last path point to bounding box
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


        if(north > south && east > west) {
            if(!userLocationChanged || userOutsideBoundingBox) {
                BoundingBox bb = new BoundingBox(north, east, south, west);
                mv.zoomToBoundingBox(bb, true, true, true, true);
            }
        }
    }
    */

    /*
     * Get the colour to represent the passed in task status TODO
     *
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
    */

    /*
     * Get the coordinates of the task and update the bounding box TODO
     *
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
    */


        /*
         * Process touch on a task TODO
         *
        public boolean processTouch(Marker item) {

            Integer iPos = markerMap.get(item);

            Log.i(TAG, "process Touch");
            if(iPos != null) {

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


            }

            return true;
        }

    };
*/

    @Override
    public void onMapLongClick(LatLng latLng) {
        mLatLng=latLng;
        if (mMarker == null) {
            mMarkerOption.position(latLng);
            mMarker = mMap.addMarker(mMarkerOption);
//			mShowLocation.setClickable(true);
        } else {
            mMarker.setPosition(latLng);
        }
        mShowLocation.setEnabled(true);
        mMarker.setDraggable(true);
        mIsDragged = true;
        setClear = false;
        mCaptureLocation = true;

    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        mLatLng = marker.getPosition();
        mIsDragged = true;
        mCaptureLocation = true;
        setClear = false;
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mLatLng, mMap.getCameraPosition().zoom));

    }

    @Override
    public void onProviderDisabled(String provider) {
    }


    @Override
    public void onProviderEnabled(String provider) {
    }


    @Override
    public void onMarkerDragStart(Marker arg0) {
//		stopGeolocating();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onMarkerDrag(Marker arg0) {

    }

    @Override
    public void onLocationChanged(Location location) {
        mLocation = location;
        mReloadLocation.setEnabled(true);
        mShowLocation.setEnabled(true);


        if (mLocation != null) {
            mLocationStatus.setText(getString(org.odk.collect.android.R.string.location_provider_accuracy, mLocation.getProvider(), truncateFloat(mLocation.getAccuracy())));
            if (!mCaptureLocation & !setClear){
                mLatLng = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());
                mMarkerOption.position(mLatLng);
                mMarker = mMap.addMarker(mMarkerOption);
//				mMarker.setPosition(mLatLng);
                mCaptureLocation = true;

            }
            if(!foundFirstLocation){
//				showZoomDialog();
                zoomToPoint();
                foundFirstLocation = true;

            }

        } else {
            InfoLogger.geolog("GeoPointMapActivity: " + System.currentTimeMillis() +
                    " onLocationChanged(" + mLocationCount + ") null location");
        }

    }

    private void zoomToLocation() {
        LatLng here = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());
        if(mLocation != null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(here, 16));
        }
    }

    private void zoomToPoint(){
        if(mLatLng != null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mMarker.getPosition(), 16));
        }

    }

    private String truncateFloat(float f) {
        return new DecimalFormat("#.##").format(f);
    }
}
