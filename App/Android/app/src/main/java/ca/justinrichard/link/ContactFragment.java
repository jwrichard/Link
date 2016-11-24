package ca.justinrichard.link;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.mobile.AWSMobileClient;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.PaginatedQueryList;
import com.amazonaws.models.nosql.ContactsDO;
import com.amazonaws.models.nosql.LinksDO;
import com.amazonaws.models.nosql.ParticipantsDO;
import com.amazonaws.models.nosql.UsersDO;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

import java.util.Iterator;

import ca.justinrichard.link.adapters.ContactAdapter;
import ca.justinrichard.link.models.Contact;
import ca.justinrichard.link.models.Link;

import static com.facebook.FacebookSdk.getApplicationContext;

import java.util.UUID;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ContactFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ContactFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ContactFragment extends Fragment {

    final String TAG = "ContactFragment";

    // Our list of contacts
    ArrayList<Contact> listContacts = new ArrayList<Contact>();

    // List adapter
    ContactAdapter adapter;

    // Refresh Layout obj
    SwipeRefreshLayout mSwipeRefreshLayout;

    private OnFragmentInteractionListener mListener;

    public ContactFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ContactFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ContactFragment newInstance() {
        ContactFragment fragment = new ContactFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_contact, container, false);

        // Get our swipe refresh handler
        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swiperefreshcontacts);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshContent();
            }
        });

        // Define an adapter for our list view so we can add items
        final ListView listView = (ListView) view.findViewById(R.id.contactsListView);

        // Set and add the adapter to the listView
        adapter = new ContactAdapter(getActivity(), listContacts);
        listView.setAdapter(adapter);

        // Handle list item clicks
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, final View view, int i, long l) {
            Log.i(TAG, "List item clicked at position "+i+" with id "+l);
            Contact c = (Contact)listView.getItemAtPosition(i);
            Log.i(TAG, c.toString());

            final PopupMenu popup = new PopupMenu(getActivity(), view);
            MenuInflater inflater = popup.getMenuInflater();
            inflater.inflate(R.menu.menu_contact, popup.getMenu());
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                Log.i(TAG, "Clicked menu item "+item.getOrder());
                if(item.getOrder() == 100){
                    // Call function to handle a request to link up - use existing session if one, otherwise create one
                    TextView userIdTextView = (TextView) view.findViewById(R.id.contactUsername);
                    new createLinkSession(userIdTextView.getText().toString()).execute();
                } else if(item.getOrder() == 200){
                    TextView usernameTextView = (TextView) view.findViewById(R.id.contactUsername);
                    String username = usernameTextView.getText().toString();
                    new removeContact(username).execute();
                }
                return true;
                }
            });
            popup.show();
            }
        });

        // Fill out the content
        refreshContent();
        return view;
    }

    // Called from pull to refresh as well as on create to get list of contacts from db
    public void refreshContent(){
        new getContactsAsync().execute();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /*
     * Used to get list of contacts and update list async
     */
    public class getContactsAsync extends AsyncTask<Void, Void, ArrayList<Contact>> {
        DynamoDB db = new DynamoDB();
        String userId = AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID();

        @Override
        protected ArrayList<Contact> doInBackground(Void... params) {
            // Get a list of participant objects for current user, and get the associated Link object
            PaginatedQueryList<ContactsDO> pql = db.GetContactsForUser(userId);

            // Create an array of links to store results and pass on
            ArrayList<Contact> contacts = new ArrayList<>();

            // Loop through participant objects and get links
            int results = pql.size();
            Log.i(TAG, "Iterating through contact objects of count: "+results);
            for(int i=0; i < results; i++){
                ContactsDO item = pql.get(i);
                UsersDO contactUser = db.GetUserFromUserId(item.getContactUserId());
                Contact c = new Contact(contactUser.getImageUrl(), contactUser.getFirstName()+' '+contactUser.getLastName(), contactUser.getUsername());
                if(c != null) {
                    contacts.add(c);
                }
            }
            return contacts;
        }

        @Override
        protected void onPostExecute(final ArrayList<Contact> contacts) {
            // Empty all items
            listContacts.clear();

            // Get iterator and loop through and add links to listView
            Iterator<Contact> contactIterator = contacts.iterator();
            while(contactIterator.hasNext()){
                Contact item = contactIterator.next();
                listContacts.add(item);
            }

            // Tell adapter to update the list
            adapter.notifyDataSetChanged();
            mSwipeRefreshLayout.setRefreshing(false);
            Toast.makeText(getApplicationContext(), "Successfully refreshed contacts!", Toast.LENGTH_SHORT).show();
        }
    }


    /*
     * Used to either find and go to a link session with a user if one already exists, or make on if it doesn't
     */
    public class createLinkSession extends AsyncTask<Void, Void, Boolean> {
        private String username;
        DynamoDB db = new DynamoDB();

        createLinkSession(String username) {
            this.username = username;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            final DynamoDBMapper dynamoDBMapper = AWSMobileClient.defaultMobileClient().getDynamoDBMapper();
            UsersDO me = db.GetUserFromUserId(AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID());
            UsersDO you = db.GetUserFromUsername(username);

            // First check if we already have an active session with that user, if so, just go to it and let the user know
            // TODO


            // If not, make one!
            LinksDO link = new LinksDO();
            link.setLastUpdate(0.0);
            link.setActive(0.0);
            link.setGroupAlias("");
            link.setGroupImageUrl("");
            // Generate a random unique identifier
            String linkId = UUID.randomUUID().toString();
            link.setId(linkId);

            // Create participant for myself
            ParticipantsDO pMe = new ParticipantsDO();
            pMe.setUserId(me.getUserId());
            pMe.setLastUpdate(0.0);
            pMe.setLat(0.0);
            pMe.setLong(0.0);
            pMe.setAltitude(0.0);
            pMe.setLinkId(linkId);

            // Create participant for other user
            ParticipantsDO pYou = new ParticipantsDO();
            pYou.setUserId(you.getUserId());
            pYou.setLastUpdate(0.0);
            pYou.setLat(0.0);
            pYou.setLong(0.0);
            pYou.setAltitude(0.0);
            pYou.setLinkId(linkId);

            try {
                // Save all objects
                dynamoDBMapper.save(link);
                dynamoDBMapper.save(pMe);
                dynamoDBMapper.save(pYou);

                // Call a refresh on the link fragment
                mListener.onContactFragmentNewLinkCreated();
                return true;
            } catch (Exception e){
                Log.i(TAG, "Caught exception: "+e);
                return false;
            }

        }

        @Override
        protected void onPostExecute(final Boolean success) {
            if (success){
                Toast.makeText(getApplicationContext(), "Successfully created Link!", Toast.LENGTH_SHORT).show();
                TabLayout tabLayout = (TabLayout) getActivity().findViewById(R.id.tabs);
                TabLayout.Tab tab = tabLayout.getTabAt(1);
                tab.select();
            } else {
                Toast.makeText(getApplicationContext(), "An unexpected error occurred", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Async task to remove a contact then call a refresh on the list
    public class removeContact extends AsyncTask<Void, Void, Boolean> {
        DynamoDB db = new DynamoDB();
        String userId = AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID();
        String username;

        public removeContact(String username){
            Log.i(TAG, "Attempting to remove contact: "+username);
            this.username = username;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            return db.RemoveContact(username, userId);
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            if(success){
                new getContactsAsync().execute();
                Toast.makeText(getApplicationContext(), "Contact removed", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "Failed to delete contact :(", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onContactFragmentNewLinkCreated();
    }
}
