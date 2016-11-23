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
import ca.justinrichard.link.models.Link;
import ca.justinrichard.link.models.Participant;

/**
 * Created by Justin on 11/1/2016.
 */

public class ParticipantAdapter extends ArrayAdapter<Participant> {
    public ParticipantAdapter(Context context, ArrayList<Participant> participants) {
        super(context, 0, participants);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        Participant item = getItem(position);

        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_participant, parent, false);
        }

        // Lookup view for data population
        TextView displayName = (TextView) convertView.findViewById(R.id.displayName);
        TextView lastUpdate = (TextView) convertView.findViewById(R.id.lastUpdate);

        TextView altitudeDifference = (TextView) convertView.findViewById(R.id.altitudeDifference);
        TextView distance = (TextView) convertView.findViewById(R.id.distance);

        ImageView profileImage = (ImageView) convertView.findViewById(R.id.linkImage);

        // Populate the data into the template view using the data object
        displayName.setText(item.getDisplayName());
        lastUpdate.setText(item.getLastUpdate().toString());

        // Get singleton instance of image loader and use it to load and set the image for the contact
        ImageLoader imageLoader = ImageLoader.getInstance();
        if(item.getImageUrl() != null) imageLoader.displayImage(item.getImageUrl(), profileImage);

        // Return the completed view to render on screen
        return convertView;
    }
}