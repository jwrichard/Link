package com.amazonaws.models.nosql;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBIndexHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBIndexRangeKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBRangeKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;

import java.util.List;
import java.util.Map;
import java.util.Set;

@DynamoDBTable(tableName = "link-mobilehub-1766662627-contacts")

public class ContactsDO {
    private String _userId;
    private String _contactUserId;

    @DynamoDBHashKey(attributeName = "userId")
    @DynamoDBIndexHashKey(attributeName = "userId", globalSecondaryIndexName = "userId-contactUserId")
    public String getUserId() {
        return _userId;
    }

    public void setUserId(final String _userId) {
        this._userId = _userId;
    }
    @DynamoDBRangeKey(attributeName = "contactUserId")
    @DynamoDBIndexRangeKey(attributeName = "contactUserId", globalSecondaryIndexName = "userId-contactUserId")
    public String getContactUserId() {
        return _contactUserId;
    }

    public void setContactUserId(final String _contactUserId) {
        this._contactUserId = _contactUserId;
    }

}
