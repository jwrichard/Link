package ca.justinrichard.link;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.icu.text.MessagePattern;
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
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.mobile.AWSMobileClient;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.PaginatedQueryList;
import com.amazonaws.models.nosql.LinksDO;
import com.amazonaws.models.nosql.ParticipantsDO;
import com.amazonaws.models.nosql.UsersDO;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.UUID;

import ca.justinrichard.link.adapters.ContactAdapter;
import ca.justinrichard.link.adapters.LinkAdapter;
import ca.justinrichard.link.models.Contact;
import ca.justinrichard.link.models.Link;

import static com.facebook.FacebookSdk.getApplicationContext;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link LinkFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link LinkFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LinkFragment extends Fragment {

    final String TAG = "ContactFragment";
    public final static String LINK_ID = "ca.justinrichard.link.MESSAGE";

    // Our list of links
    ArrayList<Link> listLinks = new ArrayList<>();

    // List adapter
    LinkAdapter adapter;

    // Refresh Layout obj
    SwipeRefreshLayout mSwipeRefreshLayout;

    private OnFragmentInteractionListener mListener;

    public LinkFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ContactFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static LinkFragment newInstance() {
        LinkFragment fragment = new LinkFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_link, container, false);

        // Get our swipe refresh handler
        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swiperefreshlinks);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshContent();
            }
        });

        // Define an adapter for our list view so we can add items
        final ListView listView = (ListView) view.findViewById(R.id.linksListView);

        // Set and add the adapter to the listView
        adapter = new LinkAdapter(getActivity(), listLinks);
        listView.setAdapter(adapter);

        // Handle list item clicks
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, final View view, int i, long l) {
            Log.i(TAG, "List item clicked at position "+i+" with id "+l);
            Link item = (Link)listView.getItemAtPosition(i);
            Intent intent = new Intent(getActivity(), LinkActivity.class);
            intent.putExtra(LINK_ID, item.getLinkId());
            startActivity(intent);
            }
        });

        // Fill out the content
        refreshContent();

        return view;
    }

    // Dynamically add new list items
    public void refreshContent(){
        // Get new links from the server and add - async
        new getLinksAsync().execute();
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
    * Reloads the list of links
    */
    public class getLinksAsync extends AsyncTask<Void, Void, Boolean> {
        DynamoDB db = new DynamoDB();
        String userId = AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID();

        @Override
        protected Boolean doInBackground(Void... params) {

            // Get a list of participant objects for current user, and get the associated Link object
            PaginatedQueryList<ParticipantsDO> pql = db.GetParticipantsForUser(userId);

            // Create an array of links to store results and pass on
            ArrayList<LinksDO> links = new ArrayList<>();

            // Loop through participant objects and get links
            int results = pql.size();
            Log.i(TAG, "Iterating through participant objects of count: "+results);
            for(int i=0; i < results; i++){
                ParticipantsDO item = pql.get(i);
                LinksDO link = db.GetLinkFromId(item.getLinkId(), userId);
                if(link != null){
                    links.add(link);
                    Log.i(TAG, "Valid link found, adding to list");
                } else {
                    Log.i(TAG, "Invalid link found, skipping");
                }
            }

            // Empty all items
            listLinks.clear();

            // Get iterator and loop through and add links to listView
            Iterator<LinksDO> linksDOIterator = links.iterator();
            while(linksDOIterator.hasNext()){
                LinksDO item = linksDOIterator.next();
                String groupAlias = item.getGroupAlias();
                String imageUrl = item.getGroupImageUrl();
                Double lastUpdate = item.getLastUpdate();
                Link itemToAdd = new Link(getContext(), item.getId(), groupAlias, imageUrl, lastUpdate);
                listLinks.add(itemToAdd);
            }

            // Sort the array
            Collections.sort(listLinks, new Comparator<Link>() {
                public int compare(Link o1, Link o2) {
                    if(o1.getLastUpdateNum() == 0) return -1;
                    if(o2.getLastUpdateNum() == 0) return 1;
                    if(o1.getLastUpdateNum() > o2.getLastUpdateNum()){
                        return -1;
                    } else {
                        return 1;
                    }
                }
            });
            return true;
        }

        @Override
        protected void onPostExecute(Boolean success) {

            // If no items, show message
            ListView lv = (ListView) getView().findViewById(R.id.linksListView);
            TextView tv = (TextView) getView().findViewById(R.id.empty);
            if(listLinks.size() == 0){
                lv.setVisibility(View.INVISIBLE);
                tv.setVisibility(View.VISIBLE);
            } else {
                lv.setVisibility(View.VISIBLE);
                tv.setVisibility(View.INVISIBLE);
            }

            if(success){
                // Tell adapter to update the list
                adapter.notifyDataSetChanged();
                mSwipeRefreshLayout.setRefreshing(false);
                //Toast.makeText(getApplicationContext(), "Successfully refreshed Links!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "Failed to update Links", Toast.LENGTH_SHORT).show();
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
        void onFragmentInteraction(String s);
    }
}
