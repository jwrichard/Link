package ca.justinrichard.link;

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

    public PaginatedQueryList<ContactsDO> GetContactsForUser(String userId){
        ContactsDO result = new ContactsDO();
        result.setUserId(userId);
        DynamoDBQueryExpression<ContactsDO> query = new DynamoDBQueryExpression<ContactsDO>().withHashKeyValues(result).withConsistentRead(false);
        PaginatedQueryList<ContactsDO> pql = dynamoDBMapper.query(ContactsDO.class, query);
        return pql;
    }


}
