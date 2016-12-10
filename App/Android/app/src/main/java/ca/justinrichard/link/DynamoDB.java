package ca.justinrichard.link;

import com.amazonaws.mobile.AWSMobileClient;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBQueryExpression;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.PaginatedQueryList;
import com.amazonaws.models.nosql.ContactsDO;
import com.amazonaws.models.nosql.FirebaseDO;
import com.amazonaws.models.nosql.LinksDO;
import com.amazonaws.models.nosql.ParticipantsDO;
import com.amazonaws.models.nosql.UsersDO;

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

    public PaginatedQueryList<ParticipantsDO> GetParticipantsFromLinkId(String linkId){
        ParticipantsDO result = new ParticipantsDO();
        result.setLinkId(linkId);
        DynamoDBQueryExpression<ParticipantsDO> query = new DynamoDBQueryExpression<ParticipantsDO>().withHashKeyValues(result).withConsistentRead(false);
        PaginatedQueryList<ParticipantsDO> pql = dynamoDBMapper.query(ParticipantsDO.class, query);
        return pql;
    }

    public LinksDO GetLinkFromId(String linkId, String myUserId){
        LinksDO result = new LinksDO();
        result.setId(linkId);
        DynamoDBQueryExpression<LinksDO> query = new DynamoDBQueryExpression<LinksDO>().withHashKeyValues(result).withConsistentRead(false);
        PaginatedQueryList<LinksDO> pql = dynamoDBMapper.query(LinksDO.class, query);
        if(!pql.isEmpty()){
            result = pql.get(0);
        }

        // Find group alias, image url and last update
        String s = ""; // Result string
        String imageUrl = "";
        Double lastUpdate = 0.0;
        ParticipantsDO participant = new ParticipantsDO();
        participant.setLinkId(linkId);
        DynamoDBQueryExpression<ParticipantsDO> query2 = new DynamoDBQueryExpression<ParticipantsDO>().withHashKeyValues(participant).withConsistentRead(false);
        PaginatedQueryList<ParticipantsDO> pql2 = dynamoDBMapper.query(ParticipantsDO.class, query2);
        int numResults = pql2.size();
        boolean first = true;
        for(int i=0; i<numResults; i++){
            ParticipantsDO item = pql2.get(i);

            // Store the most recent update time
            if(item.getLastUpdate() > lastUpdate){
                    lastUpdate = item.getLastUpdate();
            }

            // Get a name and imageUrl if it isnt me
            if(!item.getUserId().equals(myUserId)){
                UsersDO user = this.GetUserFromUserId(item.getUserId());
                imageUrl = user.getImageUrl();
                // Make csv if more than 1 user
                if(!first) s+= ", ";
                s += user.getFirstName()+" "+user.getLastName();
                first = false;
            }
        }
        result.setGroupAlias(s);
        result.setGroupImageUrl(imageUrl);
        result.setLastUpdate(lastUpdate);
        return result;
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

    // Removes a contact
    public boolean RemoveContact(String theirUsername, String myUserId){
        UsersDO user = GetUserFromUsername(theirUsername);
        if(user != null){
            ContactsDO contact = new ContactsDO();
            contact.setUserId(myUserId);
            contact.setContactUserId(user.getUserId());
            DynamoDBQueryExpression<ContactsDO> query = new DynamoDBQueryExpression<ContactsDO>().withHashKeyValues(contact).withConsistentRead(false);
            PaginatedQueryList<ContactsDO> pql = dynamoDBMapper.query(ContactsDO.class, query);

            int size = pql.size();
            for(int i=0; i<size; i++){
                ContactsDO item = pql.get(i);
                if(item.getContactUserId().equals(user.getUserId())){
                    dynamoDBMapper.delete(pql.get(i));
                    return true;
                }
            }
        }
        return false;
    }

    // Stores a firebase instance Id for a user
    public void StoreFirebaseInstanceId(String userId, String firebaseInstanceId){
        FirebaseDO f = new FirebaseDO();
        f.setUserId(userId);
        f.setFirebaseInstanceId(firebaseInstanceId);
        dynamoDBMapper.save(f);
    }

}
