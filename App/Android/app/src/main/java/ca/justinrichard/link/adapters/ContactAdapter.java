package ca.justinrichard.link.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;

import ca.justinrichard.link.R;
import ca.justinrichard.link.models.Contact;

/**
 * Created by Justin on 11/1/2016.
 */

public class ContactAdapter extends ArrayAdapter<Contact> {
    public ContactAdapter(Context context, ArrayList<Contact> contacts) {
        super(context, 0, contacts);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        Contact contact = getItem(position);

        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_contact, parent, false);
        }

        // Lookup view for data population
        TextView tv = (TextView) convertView.findViewById(R.id.contactFullName);
        TextView tv2 = (TextView) convertView.findViewById(R.id.contactUsername);
        ImageView iv = (ImageView) convertView.findViewById(R.id.contactImageUrl);

        // Populate the data into the template view using the data object
        tv.setText(contact.getFullName());
        tv2.setText(contact.getUsername());

        // Get singleton instance of image loader and use it to load and set the image for the contact
        ImageLoader imageLoader = ImageLoader.getInstance();
        imageLoader.displayImage(contact.imageUrl, iv);

        // Return the completed view to render on screen
        return convertView;
    }
}