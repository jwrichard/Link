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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;

import com.amazonaws.mobile.AWSMobileClient;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBQueryExpression;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.PaginatedQueryList;
import com.amazonaws.models.nosql.ContactsDO;
import com.amazonaws.models.nosql.LinksDO;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity implements ContactFragment.OnFragmentInteractionListener {

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

        // Start separate thread which will update conversation lists since its first tab open
        new GetConversationList().execute();

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

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        try {
            tabLayout.getTabAt(0).setIcon(R.drawable.ic_contacts);
            tabLayout.getTabAt(1).setIcon(R.drawable.ic_home);
            tabLayout.getTabAt(2).setIcon(R.drawable.ic_settings);
        } catch(Exception e){
            Log.e(TAG, "onCreate: Unable to set tab icons");
        }

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
    private class GetContactsList extends AsyncTask<Void, Void, PaginatedQueryList<ContactsDO>> {
        protected PaginatedQueryList<ContactsDO> doInBackground(Void... nothings) {
            // Get my UserId
            String myUserId = AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID();
            Log.i(TAG, "Found userId: "+myUserId);

            // Get all of my contacts
            try {
                final DynamoDBMapper dynamoDBMapper = AWSMobileClient.defaultMobileClient().getDynamoDBMapper();
                ContactsDO withThisContact = new ContactsDO();
                withThisContact.setUserId(myUserId);

                DynamoDBQueryExpression<ContactsDO> query = new DynamoDBQueryExpression<ContactsDO>().withHashKeyValues(withThisContact).withConsistentRead(false);
                return dynamoDBMapper.query(ContactsDO.class, query);
            } catch(NullPointerException e){
                Log.e(TAG, "FAILED to get contacts from DB, query failed");
                return null;
            }
        }
        protected void onPostExecute(PaginatedQueryList<ContactsDO> contacts) {
            // Take paginated query list and store it for the listView to read from
            ArrayList<String> contactsArray = new ArrayList<String>();
            Iterator<ContactsDO> contactIterator = contacts.iterator();
            while(contactIterator.hasNext()){
                ContactsDO element = contactIterator.next();
                contactsArray.add(element.getContactUserId());
            }

            // Store list of contacts
            SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("contacts", TextUtils.join(",", contactsArray));
            editor.commit();

            /*
            // Get our fragment to interact with
            try {
                ContactFragment fragment = (ContactFragment) mFragmentManager.findFragmentById(R.id.fragment_contact);
                // Loop through contacts and add them all to the listView
                for(String s: contactsArray) {
                    fragment.addItems(fragment.getView(), s);
                }
            } catch(Exception e){
                Log.e(TAG, "FAILED !"+e);
            }
            */
        }
    }

    /**
     * Async task to update the list of conversations
     */
    private class GetConversationList extends AsyncTask<Void, Void, Boolean> {
        protected Boolean doInBackground(Void... nothings) {
            // Get last timestamp we updated from preferences so we can request a delta
            SharedPreferences settings = getPreferences(MODE_PRIVATE);
            long lastUpdate = settings.getLong("lastLinksUpdate", 0);

            // Get conversation changes since last update
            // SQL Eq - Select * from links where lastUpdate > this.lastUpdate
            final DynamoDBMapper dynamoDBMapper = AWSMobileClient.defaultMobileClient().getDynamoDBMapper();
            LinksDO linkToFind = new LinksDO();


            // Update
            String string = "tester";
            try {
                FileOutputStream fos = openFileOutput("linksData", Context.MODE_APPEND);
                fos.write(string.getBytes());
                fos.close();
            } catch(IOException e){
                Log.e(TAG, "doInBackground: IO Exception:" + e);
                return false;
            }

            // Update preferences to set last update to now
            SharedPreferences.Editor editor = settings.edit();
            Date d = new Date();
            editor.putLong("lastLinksUpdate", d.getTime());
            editor.commit();
            return true;
        }
        protected void onPostExecute(Boolean success) {
            // Tell the UI to update the list of links based on the updated data store
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
                case 1: Fragment f = ContactFragment.newInstance();
                        new GetContactsList().execute();
                        return f;
                case 2: return ContactFragment.newInstance();
                case 3: return ContactFragment.newInstance();
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
