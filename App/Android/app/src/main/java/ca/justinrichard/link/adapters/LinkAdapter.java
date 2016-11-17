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
import ca.justinrichard.link.models.Link;

/**
 * Created by Justin on 11/1/2016.
 */

public class LinkAdapter extends ArrayAdapter<Link> {
    public LinkAdapter(Context context, ArrayList<Link> links) {
        super(context, 0, links);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        Link link = getItem(position);

        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_link, parent, false);
        }

        // Lookup view for data population
        TextView groupAlias = (TextView) convertView.findViewById(R.id.linkGroupAlias);
        TextView linkLastUpdate = (TextView) convertView.findViewById(R.id.linkLastUpdate);
        ImageView groupImage = (ImageView) convertView.findViewById(R.id.linkImage);
        ImageView linkActive = (ImageView) convertView.findViewById(R.id.linkActive);

        // Populate the data into the template view using the data object
        groupAlias.setText(link.getGroupAlias());
        linkLastUpdate.setText(link.getLastUpdate().toString());

        // Get singleton instance of image loader and use it to load and set the image for the contact
        ImageLoader imageLoader = ImageLoader.getInstance();
        if(groupImage != null) imageLoader.displayImage(link.getGroupImage(), groupImage);

        // Add indicator if the link session is active - if update within last 10 minutes
        if(link.getLastUpdateNum() > System.currentTimeMillis()-600000){
            linkActive.setImageResource(R.drawable.ic_link_active);
        } else {
            linkActive.setImageDrawable(null);
        }

        // Return the completed view to render on screen
        return convertView;
    }
}