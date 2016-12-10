package ca.justinrichard.link.models;

import android.content.Context;
import android.text.format.DateUtils;

import com.amazonaws.models.nosql.ParticipantsDO;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import ca.justinrichard.link.MainActivity;

/**
 * Created by Justin on 11/1/2016.
 */

public class Link {
    public String linkId;
    public String groupAlias;
    public String groupImage;
    public Double lastUpdate;
    public ArrayList<ParticipantsDO> participants;

    // variable to hold context
    private Context context;

    public Link(Context c, String linkId, String groupAlias, String groupImage, Double lastUpdate) {
        this.linkId = linkId;
        this.groupAlias = groupAlias;
        this.groupImage = groupImage;
        this.lastUpdate = lastUpdate;
        this.context = c;
    }

    public String getLinkId(){
        return this.linkId;
    }
    public String getGroupAlias(){
        if(this.groupAlias != null){
            return this.groupAlias;
        } else {
            return "Link session";
        }
    }
    public String getGroupImage(){
        return this.groupImage;
    }
    public String getLastUpdate(){
        if(this.lastUpdate == 0.0){
            return "New session";
        } else {
            return DateUtils.getRelativeDateTimeString(context, this.lastUpdate.longValue(), 0L, DateUtils.WEEK_IN_MILLIS, 0).toString();

        }
    }
    public Long getLastUpdateNum(){
        return this.lastUpdate.longValue();
    }

    public void setLinkId(String linkId){
        this.linkId = linkId;
    }
    public void setGroupAlias(String groupAlias){
        this.groupAlias = groupAlias;
    }
    public void setGroupImage(String groupImage){
        this.groupImage = groupImage;
    }
    public void setLastUpdate(Double lastUpdate){
        this.lastUpdate = lastUpdate;
    }

    @Override
    public String toString(){
        return "Link: "+linkId+", "+groupAlias+", "+groupImage;
    }

}
