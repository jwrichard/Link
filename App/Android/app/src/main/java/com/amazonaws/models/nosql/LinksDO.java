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

@DynamoDBTable(tableName = "link-mobilehub-1766662627-links")

public class LinksDO {
    private String _id;
    private Double _active;
    private String _groupAlias;
    private String _groupImageUrl;
    private Double _lastUpdate;

    @DynamoDBHashKey(attributeName = "id")
    @DynamoDBAttribute(attributeName = "id")
    public String getId() {
        return _id;
    }

    public void setId(final String _id) {
        this._id = _id;
    }
    @DynamoDBIndexHashKey(attributeName = "active", globalSecondaryIndexName = "ActiveUpdated")
    public Double getActive() {
        return _active;
    }

    public void setActive(final Double _active) {
        this._active = _active;
    }
    @DynamoDBAttribute(attributeName = "groupAlias")
    public String getGroupAlias() {
        return _groupAlias;
    }

    public void setGroupAlias(final String _groupAlias) {
        this._groupAlias = _groupAlias;
    }
    @DynamoDBAttribute(attributeName = "groupImageUrl")
    public String getGroupImageUrl() {
        return _groupImageUrl;
    }

    public void setGroupImageUrl(final String _groupImageUrl) {
        this._groupImageUrl = _groupImageUrl;
    }
    @DynamoDBIndexRangeKey(attributeName = "lastUpdate", globalSecondaryIndexName = "ActiveUpdated")
    public Double getLastUpdate() {
        return _lastUpdate;
    }

    public void setLastUpdate(final Double _lastUpdate) {
        this._lastUpdate = _lastUpdate;
    }

}
