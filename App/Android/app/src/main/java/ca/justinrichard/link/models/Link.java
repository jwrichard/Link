package ca.justinrichard.link.models;

import com.amazonaws.models.nosql.ParticipantsDO;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Justin on 11/1/2016.
 */

public class Link {
    public String linkId;
    public String groupAlias;
    public String groupImage;
    public Double lastUpdate;
    public ArrayList<ParticipantsDO> participants;

    public Link(String linkId, String groupAlias, String groupImage, Double lastUpdate) {
        this.linkId = linkId;
        this.groupAlias = groupAlias;
        this.groupImage = groupImage;
        this.lastUpdate = lastUpdate;
    }

    public String getLinkId(){
        return this.linkId;
    }
    public String getGroupAlias(){
        return this.groupAlias;
    }
    public String getGroupImage(){
        return this.groupImage;
    }
    public String getLastUpdate(){
        if(this.lastUpdate == 0.0){
            return "New session";
        } else {
            Date date = new Date(this.lastUpdate.longValue());
            DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
            return df.format(date);
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
