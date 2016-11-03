package ca.justinrichard.link.models;

/**
 * Created by Justin on 11/1/2016.
 */

public class Contact {
    public String imageUrl;
    public String fullName;
    public String userId;

    public Contact(String imageUrl, String fullName, String userId) {
        this.imageUrl = imageUrl;
        this.fullName = fullName;
        this.userId = userId;
    }

    public String getImageUrl(){
        return this.imageUrl;
    }
    public String getFullName(){
        return this.fullName;
    }
    public String getUserId(){
        return this.userId;
    }

    public void setImageUrl(String imageUrl){
        this.imageUrl = imageUrl;
    }
    public void setFullName(String fullName){
        this.fullName = fullName;
    }
    public void setUserId(String userId){
        this.userId = userId;
    }

    @Override
    public String toString(){
        return "Contact: "+fullName+", "+imageUrl+", "+userId;
    }

}
