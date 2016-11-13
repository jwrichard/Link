package ca.justinrichard.link;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.design.widget.TabLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.amazonaws.mobile.AWSMobileClient;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBQueryExpression;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.PaginatedQueryList;
import com.amazonaws.models.nosql.ContactsDO;

import com.amazonaws.models.nosql.UsersDO;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;

import ca.justinrichard.link.models.Contact;


public class MainActivity extends AppCompatActivity implements ContactFragment.OnFragmentInteractionListener, LinkFragment.OnFragmentInteractionListener {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private FragmentManager mFragmentManager;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;
    private String TAG = "MainActivity";

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public void onFragmentInteraction(String s){
        // Do something
        return;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mFragmentManager = getSupportFragmentManager();
        mSectionsPagerAdapter = new SectionsPagerAdapter(mFragmentManager);

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setCurrentItem(1, false);
        mViewPager.setOffscreenPageLimit(3);

        // Set up tab layout
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
        try {
            tabLayout.getTabAt(0).setIcon(R.drawable.ic_contacts);
            tabLayout.getTabAt(1).setIcon(R.drawable.ic_home);
            tabLayout.getTabAt(2).setIcon(R.drawable.ic_settings);
        } catch(Exception e){
            Log.e(TAG, "onCreate: Unable to set tab icons");
        }

        // Create global config for image loader and apply to singleton
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this).build();
        ImageLoader.getInstance().init(config);

        // Set up floating action button
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        if (id == R.id.menu_logout) {
            AWSMobileClient.defaultMobileClient().getIdentityManager().getCurrentIdentityProvider().signOut();
            Intent intent = new Intent(MainActivity.this, SignInActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Async task to update the list of contacts
     */
    private class GetContactsList extends AsyncTask<Void, Void, ArrayList<Contact>> {
        protected ArrayList<Contact> doInBackground(Void... nothings) {
            // Get my UserId
            String myUserId = AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID();
            Log.i(TAG, "Found userId: "+myUserId);

            // Get all of my contacts
            try {
                // Create db mapper and build object to reference in query
                final DynamoDBMapper dynamoDBMapper = AWSMobileClient.defaultMobileClient().getDynamoDBMapper();
                ContactsDO withThisContact = new ContactsDO();
                withThisContact.setUserId(myUserId);

                // Create and run query to get contacts for current user
                DynamoDBQueryExpression<ContactsDO> query = new DynamoDBQueryExpression<ContactsDO>().withHashKeyValues(withThisContact).withConsistentRead(false);
                PaginatedQueryList<ContactsDO> contacts = dynamoDBMapper.query(ContactsDO.class, query);

                // Create an array of contact objects from the db result
                DynamoDB db = new DynamoDB();
                ArrayList<Contact> contactsArray = new ArrayList<>();
                Iterator<ContactsDO> contactIterator = contacts.iterator();
                while(contactIterator.hasNext()){
                    ContactsDO element = contactIterator.next();
                    UsersDO user = db.GetUserFromUserId(element.getContactUserId());
                    Contact c = new Contact(user.getImageUrl(), user.getFirstName()+" "+user.getLastName(), user.getUsername());
                    if(user != null){
                        contactsArray.add(c);
                    }
                }
                return contactsArray;
            } catch(NullPointerException e){
                Log.e(TAG, "FAILED to get contacts from DB, query failed");
                return null;
            }
        }
        protected void onPostExecute(ArrayList<Contact> contacts) {
            // Store list of contacts
            Log.i(TAG, "Got contacts list: "+TextUtils.join(",", contacts));

            // Turn list of contacts into JSON
            Gson gson = new GsonBuilder().create();
            Type listType = new TypeToken<ArrayList<Contact>>() {}.getType();
            String jsonString = gson.toJson(contacts, listType);
            Log.i(TAG, "JSON RESULT:"+jsonString);

            // Store JSON result into shared prefs (WIll have some limit, 1.4mb or something? 1/8th of total app mem? Future concern)
            SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("contacts", jsonString);
            editor.commit();

            // Tell the fragment to update its list
            ContactFragment fragment = (ContactFragment) mFragmentManager.findFragmentById(R.id.swiperefreshcontacts);
            if(fragment != null) fragment.refreshContent();
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            switch(position){
                case 0: Fragment f = ContactFragment.newInstance();
                        new GetContactsList().execute();
                        return f;
                case 1: return LinkFragment.newInstance();
                case 2: return SettingsFragment.newInstance();
                default: return ContactFragment.newInstance();
            }
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }
    }
}
