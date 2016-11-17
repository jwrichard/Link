package ca.justinrichard.link;

import android.util.Log;

import com.amazonaws.mobile.AWSMobileClient;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBQueryExpression;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.PaginatedQueryList;
import com.amazonaws.models.nosql.ContactsDO;
import com.amazonaws.models.nosql.LinksDO;
import com.amazonaws.models.nosql.ParticipantsDO;
import com.amazonaws.models.nosql.UsersDO;
import com.google.android.gms.games.multiplayer.Participant;

/**
 * Created by Justin on 9/29/2016.
 *
 * Used to complete various tasks from DynamoDB
 */

public class DynamoDB {

    final DynamoDBMapper dynamoDBMapper;

    public DynamoDB(){
        dynamoDBMapper = AWSMobileClient.defaultMobileClient().getDynamoDBMapper();
    }

    public UsersDO GetUserFromUserId(String userId){
        UsersDO result = new UsersDO();
        result.setUserId(userId);
        DynamoDBQueryExpression<UsersDO> query = new DynamoDBQueryExpression<UsersDO>().withHashKeyValues(result).withConsistentRead(false);
        PaginatedQueryList<UsersDO> pql = dynamoDBMapper.query(UsersDO.class, query);
        if(!pql.isEmpty()){
            return pql.get(0);
        }
        return null;
    }

    public UsersDO GetUserFromUsername(String username){
        UsersDO result = new UsersDO();
        result.setUsername(username);
        DynamoDBQueryExpression<UsersDO> query = new DynamoDBQueryExpression<UsersDO>().withHashKeyValues(result).withConsistentRead(false);
        PaginatedQueryList<UsersDO> pql = dynamoDBMapper.query(UsersDO.class, query);
        if(!pql.isEmpty()){
            return pql.get(0);
        }
        return null;
    }

    public PaginatedQueryList<ParticipantsDO> GetParticipantsForUser(String userId){
        ParticipantsDO result = new ParticipantsDO();
        result.setUserId(userId);
        DynamoDBQueryExpression<ParticipantsDO> query = new DynamoDBQueryExpression<ParticipantsDO>().withHashKeyValues(result).withConsistentRead(false);
        PaginatedQueryList<ParticipantsDO> pql = dynamoDBMapper.query(ParticipantsDO.class, query);
        return pql;
    }

    public LinksDO GetLinkFromId(String linkId){
        LinksDO result = new LinksDO();
        result.setId(linkId);
        DynamoDBQueryExpression<LinksDO> query = new DynamoDBQueryExpression<LinksDO>().withHashKeyValues(result).withConsistentRead(false);
        PaginatedQueryList<LinksDO> pql = dynamoDBMapper.query(LinksDO.class, query);
        if(!pql.isEmpty()){
            return pql.get(0);
        }
        return null;
    }

    // Called by a Link refresh if no group alias is set, so generate a name based off of users that are not me
    public String GetLinkTitle(String linkId, String myUserId){
        // Result string
        String s = "";

        // Get list of participants for that LinkId
        ParticipantsDO result = new ParticipantsDO();
        result.setLinkId(linkId);
        DynamoDBQueryExpression<ParticipantsDO> query = new DynamoDBQueryExpression<ParticipantsDO>().withHashKeyValues(result).withConsistentRead(false);
        PaginatedQueryList<ParticipantsDO> pql = dynamoDBMapper.query(ParticipantsDO.class, query);
        int numResults = pql.size();
        boolean first = true;
        for(int i=0; i<numResults; i++){
            ParticipantsDO item = pql.get(i);
            if(!item.getUserId().equals(myUserId)){
                UsersDO user = this.GetUserFromUserId(item.getUserId());
                if(!first) s+= ", ";
                s += user.getFirstName()+" "+user.getLastName();
                first = false;
            }
        }
        return s;
    }

    // Called if group has not set an image, so set the image to be the first user thats not me
    public String GetLinkImageUrl(String linkId, String myUserId){
        // Get list of participants for that LinkId
        ParticipantsDO result = new ParticipantsDO();
        result.setLinkId(linkId);
        DynamoDBQueryExpression<ParticipantsDO> query = new DynamoDBQueryExpression<ParticipantsDO>().withHashKeyValues(result).withConsistentRead(false);
        PaginatedQueryList<ParticipantsDO> pql = dynamoDBMapper.query(ParticipantsDO.class, query);
        int numResults = pql.size();
        for(int i=0; i<numResults; i++){
            ParticipantsDO item = pql.get(i);
            if(!item.getUserId().equals(myUserId)){
                UsersDO user = this.GetUserFromUserId(item.getUserId());
                return user.getImageUrl();
            }
        }
        return null;
    }

    public PaginatedQueryList<ContactsDO> GetContactsForUser(String userId){
        ContactsDO result = new ContactsDO();
        result.setUserId(userId);
        DynamoDBQueryExpression<ContactsDO> query = new DynamoDBQueryExpression<ContactsDO>().withHashKeyValues(result).withConsistentRead(false);
        PaginatedQueryList<ContactsDO> pql = dynamoDBMapper.query(ContactsDO.class, query);
        return pql;
    }

    // Adds a contact, returns boolean of if successful
    public boolean AddContact(String theirUsername, String myUserId){
        UsersDO user = GetUserFromUsername(theirUsername);
        if(user != null){
            ContactsDO contact = new ContactsDO();
            contact.setUserId(myUserId);
            contact.setContactUserId(user.getUserId());
            dynamoDBMapper.save(contact);
            return true;
        } else {
            return false;
        }
    }


}
