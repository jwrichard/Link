package ca.justinrichard.link.adapters;

import android.content.Context;
import android.location.Location;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Color;

import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;
import java.util.Locale;

import ca.justinrichard.link.R;
import ca.justinrichard.link.models.Participant;

import static java.lang.Math.floor;

/**
 * Created by Justin on 11/1/2016.
 */

public class ParticipantAdapter extends ArrayAdapter<Participant> {

    private Location mLocation;

    public ParticipantAdapter(Context context, ArrayList<Participant> participants) {
        super(context, 0, participants);
    }

    public void updateMyLocation(Location l){
        mLocation = l;
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

        // Calculate and display distance and altitude to that user if set
        TextView distanceTextView = (TextView) convertView.findViewById(R.id.distance);
        //TextView altitudeDifference = (TextView) convertView.findViewById(R.id.altitudeDifference);
        if((item.getLatitude() != 0 || item.getLongitude() != 0 || item.getAltitude() != 0) && mLocation != null){
            Location theirLocation = new Location("");
            theirLocation.setAltitude(item.getAltitude());
            theirLocation.setLatitude(item.getLatitude());
            theirLocation.setLongitude(item.getLongitude());
            float distance = mLocation.distanceTo(theirLocation);
            if(distance > 1000){
                distanceTextView.setText(String.format(Locale.US, "%.2f", distance/1000)+"km");
            } else {
                distanceTextView.setText(String.format(Locale.US, "%.0f", distance)+"m");
            }
            // Display altitude difference
            /*
            double altitudeDistance;
            if(mLocation.getAltitude() != 0 && theirLocation.getAltitude() != 0){
                altitudeDistance = mLocation.getAltitude() - theirLocation.getAltitude();
            } else {
                altitudeDistance = 0;
            }
            altitudeDifference.setText(floor((altitudeDistance/3.5))+" floors");
            */
        } else {
            //altitudeDifference.setText("-");
            distanceTextView.setText("-");
        }

        // Populate the data into the template view using the data object
        displayName.setText(item.getDisplayName());
        if(item.getLastUpdate() > 0){
            lastUpdate.setText(DateUtils.getRelativeDateTimeString(getContext(), item.getLastUpdate(), 0L, DateUtils.WEEK_IN_MILLIS, 0));
        } else {
            lastUpdate.setText("Inactive");
        }

        // Get singleton instance of image loader and use it to load and set the image for the contact
        ImageLoader imageLoader = ImageLoader.getInstance();
        ImageView profileImage = (ImageView) convertView.findViewById(R.id.profileImage);
        if(item.getImageUrl() != null) imageLoader.displayImage(item.getImageUrl(), profileImage);

        // Set background color of image based on hue value from participant
        float[] color = {item.getHue(), 1f, 1f};
        profileImage.setBackgroundColor(Color.HSVToColor(color));

        // Set bg color of whole item
        LinearLayout ll = (LinearLayout) convertView.findViewById(R.id.participantItem);
        float[] lighterColor = {item.getHue(), .2f, 1f};
        ll.setBackgroundColor(Color.HSVToColor(lighterColor));

        // Return the completed view to render on screen
        return convertView;
    }
}