package ca.justinrichard.link.models;

/**
 * Created by Justin on 11/1/2016.
 */

public class Contact {
    public String imageUrl;
    public String fullName;
    public String username;

    public Contact(String imageUrl, String fullName, String username) {
        this.imageUrl = imageUrl;
        this.fullName = fullName;
        this.username = username;
    }

    public String getImageUrl(){
        return this.imageUrl;
    }
    public String getFullName(){
        return this.fullName;
    }
    public String getUsername(){
        return this.username;
    }

    public void setImageUrl(String imageUrl){
        this.imageUrl = imageUrl;
    }
    public void setFullName(String fullName){
        this.fullName = fullName;
    }
    public void setUsername(String username){
        this.username = username;
    }

    @Override
    public String toString(){
        return "Contact: "+fullName+", "+imageUrl+", "+username;
    }

}
