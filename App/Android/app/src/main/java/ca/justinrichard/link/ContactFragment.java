package ca.justinrichard.link;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

import java.util.Iterator;

import ca.justinrichard.link.adapters.ContactAdapter;
import ca.justinrichard.link.models.Contact;

import static com.facebook.FacebookSdk.getApplicationContext;


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
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Log.i(TAG, "List item clicked at position "+i+" with id "+l);
                Contact c = (Contact)listView.getItemAtPosition(i);
                Log.i(TAG, c.toString());

                PopupMenu popup = new PopupMenu(getActivity(), view);
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.menu_contact, popup.getMenu());
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if(item.getTitle() == "Link up"){
                            // Call function to handle a request to link up - use existing session if one, otherwise create one
                            createLinkSsession(); // TODO

                        } else if(item.getTitle() == "Remove contact"){
                            Toast.makeText(getApplicationContext(), "Feature not yet available", Toast.LENGTH_SHORT).show();
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

    // Dynamically add new list item
    public void refreshContent(){
        // Empty all items
        listContacts.clear();

        // Add items from stored data
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        String contactString = sharedPref.getString("contacts", "");

        // Turn contacts JSON to ArrayList of contact objects
        Gson gson = new GsonBuilder().create();
        Type listType = new TypeToken<ArrayList<Contact>>() {}.getType();
        ArrayList<Contact> contacts;
        try {
            contacts = gson.fromJson(contactString, listType);
            // Loop through ArrayList of objects and add to the adapter
            Iterator<Contact> contactIterator = contacts.iterator();
            while(contactIterator.hasNext()){
                Contact c = contactIterator.next();
                listContacts.add(c);
            }
        } catch(com.google.gson.JsonSyntaxException ex){
            Log.e("ContactFragment", "Failed to read JSON from shared prefs");
        }

        // Tell adapter to update the list
        adapter.notifyDataSetChanged();
        mSwipeRefreshLayout.setRefreshing(false);
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
