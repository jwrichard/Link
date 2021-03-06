package ca.justinrichard.link;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.amazonaws.mobile.AWSMobileClient;
import com.github.clans.fab.FloatingActionMenu;
import com.github.clans.fab.FloatingActionButton;
import com.google.firebase.iid.FirebaseInstanceId;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;


public class MainActivity extends AppCompatActivity implements ContactFragment.OnFragmentInteractionListener, LinkFragment.OnFragmentInteractionListener, SettingsFragment.OnFragmentInteractionListener {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */

    public final static String LINK_ID = "ca.justinrichard.link.MESSAGE";

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private FragmentManager mFragmentManager;

    private ContactFragment mCF;
    private LinkFragment mLF;

    private FloatingActionMenu mMenu;
    private FloatingActionButton mFab, mSubFab1, mSubFab2;

    // New contact dialog
    private AlertDialog dialog;

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

    // Called from ContactFragment when a new Link session is created to update the links fragment
    public void onContactFragmentNewLinkCreated(String linkId){
        if(mLF != null) {
            Log.i(TAG, "Got link fragment, calling a refresh");
            mLF.refreshContent();
        } else {
            Log.i(TAG, "Failed to get link fragment, so opening Link activity right away");
            Intent intent = new Intent(this, LinkActivity.class);
            intent.putExtra(LINK_ID, linkId);
            startActivity(intent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Ensure we have a firebase id
        Log.i(TAG, "Getting firebase token...");
        String firebaseToken = FirebaseInstanceId.getInstance().getToken();
        Log.i(TAG, "Sending token to db: "+firebaseToken);
        sendRegistrationToServer(firebaseToken);

        // Setup the toolbar
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
        final TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
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

        // Set up floating action button and submenu items
        mMenu = (FloatingActionMenu) findViewById(R.id.menu);
        mSubFab1 = (FloatingActionButton) findViewById(R.id.menu_item);
        mSubFab2 = (FloatingActionButton) findViewById(R.id.menu_item2);

        // Create handlers for the Fab submenu actions
        // New Link
        mSubFab1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v){
                // Open contact tab and tell user to select a contact
                Toast.makeText(getApplicationContext(), "Select a contact", Toast.LENGTH_LONG).show();
                tabLayout.getTabAt(0).select();
                mMenu.close(true);
            }
        });

        // New Contact
        mSubFab2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v){
            // Open contact tab
            tabLayout.getTabAt(0).select();
            mMenu.close(true);

            // Initiate new contact prompt
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            final View view = getLayoutInflater().inflate(R.layout.contact_prompt, null);
            builder.setView(view);
            builder.setMessage("Enter a username to add as a contact").setTitle("Add a contact");
            builder.setPositiveButton("Submit", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User clicked OK button - call async task to add contact and reprompt on failure
                    EditText usernameTextEdit = (EditText) view.findViewById(R.id.username);
                    new addContact(usernameTextEdit.getText().toString()).execute();
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User cancelled the dialog
                }
            });
            dialog = builder.create();
            dialog.show();
            }
        });
    }

    // Async task to add a contact and re-prompt on failure
    public class addContact extends AsyncTask<Void, Void, Boolean> {
        DynamoDB db = new DynamoDB();
        String userId = AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID();
        String username;

        public addContact(String username){
            this.username = username;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            return db.AddContact(username, userId);
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            if(success){
                final TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
                tabLayout.getTabAt(0).select();
                if(mCF != null) mCF.refreshContent();
                Toast.makeText(getApplicationContext(), "Contact added", Toast.LENGTH_SHORT).show();
            } else {
                // Edit the dialog and re-prompt
                dialog.setMessage("Unable to find user with that username. Please try again.");
                dialog.show();
            }
        }
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
        // noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            mViewPager.setCurrentItem(2, false);
            return true;
        }
        if (id == R.id.menu_logout) {
            try {
                AWSMobileClient.defaultMobileClient().getIdentityManager().getCurrentIdentityProvider().signOut();
            } catch(Exception e){
                Log.wtf(TAG, "Already signed out?");
            } finally {
                Intent intent = new Intent(MainActivity.this, SignInActivity.class);
                startActivity(intent);
            }
        }
        return super.onOptionsItemSelected(item);
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
                case 0: return mCF = ContactFragment.newInstance();
                case 1: return mLF = LinkFragment.newInstance();
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

    /**
     * Persist token to third-party servers.
     *
     * Modify this method to associate the user's FCM InstanceID token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private void sendRegistrationToServer(String token) {
        new sendAsync(token).execute();
    }

    public class sendAsync extends AsyncTask<Void, Void, Void> {
        DynamoDB db = new DynamoDB();
        String userId = AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID();
        String token;

        public sendAsync(String token){
            this.token = token;
        }

        @Override
        protected Void doInBackground(Void... params) {
            Log.i(TAG, "Calling DB store for my registration token");
            db.StoreFirebaseInstanceId(userId, token);
            return null;
        }
    }
}


