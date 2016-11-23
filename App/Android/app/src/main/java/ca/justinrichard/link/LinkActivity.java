package ca.justinrichard.link;

import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.ListView;
import android.widget.Toast;

import com.amazonaws.mobile.AWSMobileClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

import ca.justinrichard.link.adapters.LinkAdapter;
import ca.justinrichard.link.adapters.ParticipantAdapter;
import ca.justinrichard.link.models.Link;
import ca.justinrichard.link.models.Participant;

public class LinkActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private String linkId;

    // List adapter
    ParticipantAdapter adapter;

    // Our list of participants
    ArrayList<Participant> listParticipants = new ArrayList<>();

    // Refresh Layout obj
    SwipeRefreshLayout mSwipeRefreshLayout;

    private final String TAG = "LinkActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_link);

        // Get intent
        Intent intent = getIntent();
        linkId = intent.getStringExtra(LinkFragment.LINK_ID);
        Log.i(TAG, "Viewing Link session with LinkId: "+linkId);

        // Enabled 'Up' action in the action bar
        try {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        } catch(Exception e){
            Log.i(TAG, "Failed to set home up as enabled");
        }

        // Get our swipe refresh handler
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefreshlink);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshContent();
            }
        });

        // Define an adapter for our list view so we can add items
        final ListView listView = (ListView) findViewById(R.id.linksListView);

        // Set and add the adapter to the listView
        adapter = new ParticipantAdapter(this, listParticipants);
        listView.setAdapter(adapter);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    // Loops through creating async tasks while activity active
    public void linkLooper(){
        // Get update rates from user preferences

    }

    // Called when an update is forced by the user
    public void refreshContent(){

    }

    // Async task to get updates from the server and send my last position
    public class updateLink extends AsyncTask<Void, Void, Boolean> {
        DynamoDB db = new DynamoDB();
        String userId = AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID();
        Location location;
        ContactFragment fragment;

        public updateLink(Location l){
            this.location = l;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            if (success) {

            } else {

            }
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }
}
