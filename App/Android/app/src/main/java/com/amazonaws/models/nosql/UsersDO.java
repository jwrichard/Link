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

@DynamoDBTable(tableName = "link-mobilehub-1766662627-users")

public class UsersDO {
    private String _userId;
    private String _firstName;
    private String _imageUrl;
    private String _lastName;
    private String _username;

    @DynamoDBHashKey(attributeName = "userId")
    @DynamoDBAttribute(attributeName = "userId")
    public String getUserId() {
        return _userId;
    }

    public void setUserId(final String _userId) {
        this._userId = _userId;
    }
    @DynamoDBAttribute(attributeName = "firstName")
    public String getFirstName() {
        return _firstName;
    }

    public void setFirstName(final String _firstName) {
        this._firstName = _firstName;
    }
    @DynamoDBAttribute(attributeName = "imageUrl")
    public String getImageUrl() {
        return _imageUrl;
    }

    public void setImageUrl(final String _imageUrl) {
        this._imageUrl = _imageUrl;
    }
    @DynamoDBAttribute(attributeName = "lastName")
    public String getLastName() {
        return _lastName;
    }

    public void setLastName(final String _lastName) {
        this._lastName = _lastName;
    }
    @DynamoDBIndexHashKey(attributeName = "username", globalSecondaryIndexName = "username")
    public String getUsername() {
        return _username;
    }

    public void setUsername(final String _username) {
        this._username = _username;
    }

}
