package ca.justinrichard.link;

import com.amazonaws.mobile.AWSMobileClient;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBQueryExpression;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.PaginatedQueryList;
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


}
