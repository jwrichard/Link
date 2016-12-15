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
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
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

    // Dialog for adding members
    private android.app.AlertDialog dialog;

    // Initial location
    private Location mLastLocation;
    private LocationRequest mLocationRequest;
    Marker mCurrLocationMarker;
    LocationManager locationManager;

    // LinkLooper trigger
    boolean linkLooper = false;

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
        final ListView listView = (ListView) findViewById(R.id.linkListView);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Participant p = (Participant) listView.getItemAtPosition(position);
                if(p.getLatitude() != 0  || p.getLongitude() != 0){
                    LatLng latlng = new LatLng(p.getLatitude(), p.getLongitude());
                    mGoogleMap.animateCamera(CameraUpdateFactory.newLatLng(latlng));
                }
            }
        });

        // Set and add the adapter to the listView
        adapter = new ParticipantAdapter(this, listParticipants);
        listView.setAdapter(adapter);

        // Start location services up
        /*
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        } else {
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                showGPSDisabledAlertToUser();
            }
        }*/

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
            Toast.makeText(this, "Push notification sent", Toast.LENGTH_LONG).show();
            new sendPushNotification().execute();
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
        if (id == R.id.action_add) {
            // Open a prompt for a username
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(LinkActivity.this);
            final View view = getLayoutInflater().inflate(R.layout.contact_prompt, null);
            builder.setView(view);
            builder.setMessage("Enter a username to add to this link session").setTitle("Add a member");
            builder.setPositiveButton("Submit", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User clicked OK button - call async task to add contact and reprompt on failure
                    EditText usernameTextEdit = (EditText) view.findViewById(R.id.username);
                    new LinkActivity.addMember(usernameTextEdit.getText().toString()).execute();
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User cancelled the dialog
                }
            });
            dialog = builder.create();
            dialog.show();
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
        Log.i(TAG, "onResume called");
        // Stop location updates when Activity is no longer active
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
        // Stop runnables if running
        try {
            Log.i(TAG, "ActivityPaused, stopping runnables");
            mHandler.removeCallbacks(mLooper);
            mHandler.removeCallbacks(mTimerDecrementor);
            // Hide pause buttons and show resume button
            if(mMenu != null) {
                MenuItem mPause = mMenu.findItem(R.id.action_pause);
                MenuItem mResume = mMenu.findItem(R.id.action_resume);
                mPause.setVisible(false);
                mResume.setVisible(true);
            }
            linkLooper = false;
        } catch(Exception e){
            Log.e(TAG, "Unable to stop runnables, already running");
        }

    }

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first
        Log.i(TAG, "onResume called");
        // Start location updates when Activity is resumed or initially started
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        } else {
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                showGPSDisabledAlertToUser();
            }
        }
        // Hide this menu item and show the pause button
        if(mMenu != null){
            MenuItem mPause = mMenu.findItem(R.id.action_pause);
            MenuItem mResume = mMenu.findItem(R.id.action_resume);
            mPause.setVisible(true);
            mResume.setVisible(false);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        // Save the map object
        mGoogleMap = googleMap;

        // Move to my last location
        if(mLastLocation != null){
            LatLng latlng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, 17.f));
        }

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
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            // Move to my last location
            if(mGoogleMap != null && mLastLocation != null){
                LatLng latlng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, 17.f));
            }
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

        // Update my location
        mLastLocation = location;

        // Give our list adapter our new location so it can update with relevant distances
        adapter.updateMyLocation(location);
    }

    public boolean checkLocationPermission() {
        Log.i(TAG, "Call to checkLocationPermission activated");
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Permission not yet granted, asking for permission");
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CHECK_SETTINGS);
            return false;
        } else {
            Log.i(TAG, "Permission already granted, making sure GPS enabled for user");
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Log.i(TAG, "GPS not enabled for user, showing prompt");
                showGPSDisabledAlertToUser();
            }
            linkLooper();
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                            showGPSDisabledAlertToUser();
                        }
                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mGoogleMap.setMyLocationEnabled(true);
                        linkLooper();
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
        Log.i(TAG, "LinkLooper called");
        if(!linkLooper){
            Log.i(TAG, "LinkLooper passed, starting runnables");
            // Get update rates from user preferences
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            updateRate = prefs.getInt("data_link_refresh_rate", 30);
            mProgressBar.setMax(updateRate * 40);
            mProgressBar.setProgress(updateRate * 40);

            // Run async tasks to call updates periodically
            mHandler = new Handler();
            mLooper.run();
            mTimerDecrementor.run();
            linkLooper = true;
        }
    }

    // Called when an update is forced by the user
    public void refreshContent() {
        new updateLinkWithMyLocation().execute();
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

            // Tell adapter to update the list
            mProgressBar.setProgress(updateRate*40);
            adapter.notifyDataSetChanged();
            mSwipeRefreshLayout.setRefreshing(false);
        }
    }

    // Async task to add a member to the group and re-prompt on failure
    public class addMember extends AsyncTask<Void, Void, Boolean> {
        DynamoDB db = new DynamoDB();
        String userId = AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID();
        String username;

        public addMember(String username){
            this.username = username;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            UsersDO you = db.GetUserFromUsername(username);
            if(you == null){
                return false;
            } else {
                final DynamoDBMapper dynamoDBMapper = AWSMobileClient.defaultMobileClient().getDynamoDBMapper();
                // Create participant for other user
                ParticipantsDO pYou = new ParticipantsDO();
                pYou.setUserId(you.getUserId());
                pYou.setLastUpdate(0.0);
                pYou.setLat(0.0);
                pYou.setLong(0.0);
                pYou.setAltitude(0.0);
                pYou.setLinkId(linkId);
                dynamoDBMapper.save(pYou);
                return true;
            }
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            if(success){
                refreshContent();
                Toast.makeText(getApplicationContext(), "Member added", Toast.LENGTH_SHORT).show();
            } else {
                // Edit the dialog and re-prompt
                dialog.setMessage("Unable to find user with that username. Please try again.");
                dialog.show();
            }
        }
    }

    // Async task to send my last position
    public class updateLinkWithMyLocation extends AsyncTask<Void, Void, Boolean> {
        DynamoDB db = new DynamoDB();
        String userId = AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID();

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                if(mLastLocation != null){
                    final DynamoDBMapper dynamoDBMapper = AWSMobileClient.defaultMobileClient().getDynamoDBMapper();
                    ParticipantsDO me = new ParticipantsDO();
                    me.setLinkId(linkId);
                    me.setUserId(userId);
                    me.setAltitude(mLastLocation.getAltitude());
                    me.setLat(mLastLocation.getLatitude());
                    me.setLong(mLastLocation.getLongitude());
                    me.setLastUpdate((double) System.currentTimeMillis());
                    dynamoDBMapper.save(me);
                }
                return true;
            } catch(Exception e){
                Log.e(TAG, "Failed to update location with e: "+e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if(success){
                Log.i(TAG, "Sent my location to the server! - Getting updates now");
                new updateLink().execute();
            } else {
                Log.i(TAG, "Failed to send my location to the server!");
            }
        }
    }

    // Async task to send push notifications to all users in the group
    public class sendPushNotification extends AsyncTask<Void, Void, Boolean> {
        DynamoDB db = new DynamoDB();
        String userId = AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID();

        @Override
        protected Boolean doInBackground(Void... params) {
            UsersDO me = db.GetUserFromUserId(userId);
            String body = me.getFirstName() + " " + me.getLastName() + " wants to Link up!";

            PaginatedQueryList<ParticipantsDO> pql = db.GetParticipantsFromLinkId(linkId);
            int size = pql.size();
            for(int i=0; i<size; i++){
                ParticipantsDO item = pql.get(i);
                if(! item.getUserId().equals(userId)){
                    // If it isn't me, send a notification to this user
                    String firebaseId = db.GetFirebaseTokenFromUserId(item.getUserId());
                    if(firebaseId != null){
                        // Send notification to this firebase id
                        try {
                            Log.i(TAG, "https://justinrichard.ca/fcm.php?id="+URLEncoder.encode(firebaseId)+"&message="+URLEncoder.encode(body)+"&linkId="+URLEncoder.encode(linkId));
                            URL url = new URL("https://justinrichard.ca/fcm.php?id="+URLEncoder.encode(firebaseId)+"&message="+URLEncoder.encode(body)+"&linkId="+URLEncoder.encode(linkId));
                            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                            urlConnection.setRequestMethod("GET");
                            urlConnection.setDoOutput(true);
                            urlConnection.setConnectTimeout(5000);
                            urlConnection.setReadTimeout(5000);
                            urlConnection.connect();
                            BufferedReader rd = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                            String content = "", line;
                            while ((line = rd.readLine()) != null) {
                                content += line + "\n";
                            }
                            Log.i(TAG, "RESPONSE: "+content);
                            urlConnection.disconnect();
                        } catch(Exception e){
                            Log.e(TAG, "Unable to create request: "+e);
                        }

                    }
                }
            }
            return true;
        }
    }

}
