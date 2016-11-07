package ca.justinrichard.link.models;

import com.amazonaws.models.nosql.ParticipantsDO;

import java.util.ArrayList;

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
    public Double getLastUpdate(){
        return this.lastUpdate;
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
