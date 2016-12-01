package ca.justinrichard.link;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.preference.PreferenceManager;

import com.amazonaws.mobile.AWSMobileClient;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.PaginatedQueryList;
import com.amazonaws.models.nosql.ParticipantsDO;
import com.amazonaws.models.nosql.UsersDO;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Iterator;

import ca.justinrichard.link.adapters.ParticipantAdapter;
import ca.justinrichard.link.models.Participant;

public class LinkActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    // Constants
    protected static final int REQUEST_CHECK_SETTINGS = 0x1;
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    private float mScale;

    // Globals
    private String linkId;
    private GoogleMap mGoogleMap;
    SupportMapFragment mapFragment;

    // Menu
    private Menu mMenu;

    // List adapter
    private ParticipantAdapter adapter;

    // Our list of participants
    private ArrayList<Participant> listParticipants = new ArrayList<>();

    // Refresh Layout obj
    protected SwipeRefreshLayout mSwipeRefreshLayout;

    // Progress bar
    protected ProgressBar mProgressBar;

    // Google API Client
    private GoogleApiClient mGoogleApiClient;

    // Update rates
    private int updateRate;
    private Handler mHandler;

    // Initial location
    private Location mLastLocation;
    private LocationRequest mLocationRequest;
    Marker mCurrLocationMarker;
    LocationManager locationManager;

    private final String TAG = "LinkActivity";
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    // Runnables
    Runnable mLooper = new Runnable() {
        @Override
        public void run() {
            try {
                // Call to get updates and send my own
                new updateLink().execute();
                new updateLinkWithMyLocation().execute();
            } finally {
                // 100% guarantee that this always happens, even if
                // your update method throws an exception
                mHandler.postDelayed(mLooper, updateRate*1000);
            }
        }
    };
    Runnable mTimerDecrementor = new Runnable() {
        @Override
        public void run() {
            try {
                // Call to get updates and send my own
                int progress = mProgressBar.getProgress();
                if(progress > 0){
                    mProgressBar.setProgress(progress-1);
                }
            } finally {
                // 100% guarantee that this always happens, even if
                // your update method throws an exception
                mHandler.postDelayed(mTimerDecrementor, 25);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_link);

        // Get intent
        Intent intent = getIntent();
        linkId = intent.getStringExtra(LinkFragment.LINK_ID);
        Log.i(TAG, "Viewing Link session with LinkId: " + linkId);

        // Get our swipe refresh handler
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefreshlink);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshContent();
            }
        });

        // Save pixel density
        // Get the screen's density scale
        final float mScale = getResources().getDisplayMetrics().density;

        // Get our progress bar
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);

        // Define an adapter for our list view so we can add items
        ListView listView = (ListView) findViewById(R.id.linkListView);

        // Set and add the adapter to the listView
        adapter = new ParticipantAdapter(this, listParticipants);
        listView.setAdapter(adapter);

        // Start location services up
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        } else {
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                showGPSDisabledAlertToUser();
            }
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_link, menu);
        mMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        // noinspection SimplifiableIfStatement
        if (id == R.id.action_notify) {
            Toast.makeText(this, "Push notification sent - TODO", Toast.LENGTH_LONG).show();
            return true;
        }
        if (id == R.id.action_pause) {
            // Stop runnables if running
            mHandler.removeCallbacks(mLooper);
            mHandler.removeCallbacks(mTimerDecrementor);
            // Hide pause buttons and show resume button
            MenuItem mPause = mMenu.findItem(R.id.action_pause);
            MenuItem mResume = mMenu.findItem(R.id.action_resume);
            mPause.setVisible(false);
            mResume.setVisible(true);
            return true;
        }
        if (id == R.id.action_resume) {
            // Start the runnables
            if(mLooper != null && mTimerDecrementor != null){
                mLooper.run();
                mTimerDecrementor.run();
            }
            // Hide this menu item and show the pause button
            MenuItem mPause = mMenu.findItem(R.id.action_pause);
            MenuItem mResume = mMenu.findItem(R.id.action_resume);
            mPause.setVisible(true);
            mResume.setVisible(false);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void showGPSDisabledAlertToUser() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("GPS is disabled in your device. Would you like to enable it?")
                .setCancelable(false)
                .setPositiveButton("Settings", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        Intent callGPSSettingIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(callGPSSettingIntent);
                        mapFragment.getMapAsync(LinkActivity.this);
                    }
                });
        alertDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                dialog.cancel();
            }
        });
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Stop location updates when Activity is no longer active
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
        // Stop runnables if running
        mHandler.removeCallbacks(mLooper);
        mHandler.removeCallbacks(mTimerDecrementor);
    }

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first
        // Start location updates when Activity is resumed
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        } else {
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                showGPSDisabledAlertToUser();
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        // Save the map object
        mGoogleMap = googleMap;

        // Initialize Google Play Services
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                buildGoogleApiClient();
                mGoogleMap.setMyLocationEnabled(true);
            }
        } else {
            buildGoogleApiClient();
            mGoogleMap.setMyLocationEnabled(true);
        }

        // Get list of participants and add them to the list and map
        linkLooper();
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {}

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {}

    @Override
    public void onLocationChanged(Location location) {
        // Get my current lat long
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        // Move map camera only if first update
        if(mLastLocation == null){
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(17));
        }

        // Update my location
        mLastLocation = location;

        // Give our list adapter our new location so it can update with relevant distances
        adapter.updateMyLocation(location);

        // TODO
        if (mCurrLocationMarker != null){
            mCurrLocationMarker.remove();
        }
    }

    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                // Prompt the user once explanation has been shown
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CHECK_SETTINGS);
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CHECK_SETTINGS);
            }
            return false;
        } else {
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                showGPSDisabledAlertToUser();
            }
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                            showGPSDisabledAlertToUser();
                        }
                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mGoogleMap.setMyLocationEnabled(true);
                    }
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "Location privileges required", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(LinkActivity.this, MainActivity.class);
                    startActivity(intent);
                }
            }
        }
    }

    /* Link methods */

    // Loops through creating async tasks while activity active
    public void linkLooper() {
        // Get update rates from user preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        updateRate = prefs.getInt("data_link_refresh_rate", 30);
        mProgressBar.setMax(updateRate*40);
        mProgressBar.setProgress(updateRate*40);

        // Run async tasks to call updates periodically
        mHandler = new Handler();
        mLooper.run();
        mTimerDecrementor.run();
    }

    // Called when an update is forced by the user
    public void refreshContent() {
        new updateLink().execute();
    }

    // Async task to get updates from the server
    public class updateLink extends AsyncTask<Void, Void, ArrayList<Participant>> {
        DynamoDB db = new DynamoDB();
        String userId = AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID();

        @Override
        protected ArrayList<Participant> doInBackground(Void... params) {

            // Get a list of participant objects for current user, and get the associated Link object
            PaginatedQueryList<ParticipantsDO> pql = db.GetParticipantsFromLinkId(linkId);

            // Create an array of participants to store results and pass on
            ArrayList<Participant> participants = new ArrayList<>();
            int results = pql.size();
            for (int i = 0; i < results; i++) {
                ParticipantsDO item = pql.get(i);
                Participant p = new Participant(item.getUserId(), item.getLastUpdate().longValue(), item.getAltitude(), item.getLat(), item.getLong());
                UsersDO pUser = db.GetUserFromUserId(item.getUserId());
                p.setDisplayName(pUser.getFirstName() + " " + pUser.getLastName());
                p.setImageUrl(pUser.getImageUrl());
                Log.i(TAG, "Adding participant p:"+p);
                participants.add(p);
            }
            return participants;
        }

        @Override
        protected void onPostExecute(ArrayList<Participant> participants) {
            // Empty all items
            listParticipants.clear();

            // Remove all markers
            mGoogleMap.clear();

            // Get iterator and loop through and add links to listView
            Iterator<Participant> participantIterator = participants.iterator();
            float hue = 0f;
            while (participantIterator.hasNext()) {
                Participant item = participantIterator.next();

                // Update the map by removing all markers and adding a new one with matching color for each user
                if(item.getLatitude() != 0 || item.getLongitude() != 0){
                    mGoogleMap.addMarker(new MarkerOptions()
                            .position(new LatLng(item.getLatitude(), item.getLongitude()))
                            .title(item.getDisplayName())
                            .icon(BitmapDescriptorFactory.defaultMarker(hue%360)));
                }
                item.setHue(hue);
                listParticipants.add(item);
                hue += 36;
            }

            // Set height of maps fragment
            //LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int)(400*mScale + 0.5f));
            //mapFragment.getView().setLayoutParams(layoutParams);


            // Tell adapter to update the list
            mProgressBar.setProgress(updateRate*40);
            adapter.notifyDataSetChanged();
            mSwipeRefreshLayout.setRefreshing(false);
        }
    }

    // Async task to send my last position
    public class updateLinkWithMyLocation extends AsyncTask<Void, Void, Boolean> {
        DynamoDB db = new DynamoDB();
        String userId = AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID();

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                final DynamoDBMapper dynamoDBMapper = AWSMobileClient.defaultMobileClient().getDynamoDBMapper();
                ParticipantsDO me = new ParticipantsDO();
                me.setLinkId(linkId);
                me.setUserId(userId);
                me.setAltitude(mLastLocation.getAltitude());
                me.setLat(mLastLocation.getLatitude());
                me.setLong(mLastLocation.getLongitude());
                me.setLastUpdate((double) System.currentTimeMillis());
                dynamoDBMapper.save(me);
                return true;
            } catch(Exception e){
                Log.e(TAG, "Failed to update location with e: "+e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if(success){
                Log.i(TAG, "Sent my location to the server!");
            } else {
                Log.i(TAG, "Failed to send my location to the server!");
            }
        }
    }
}
