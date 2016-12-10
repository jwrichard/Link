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

@DynamoDBTable(tableName = "link-mobilehub-1766662627-participants")

public class ParticipantsDO {
    private String _userId;
    private String _linkId;
    private Double _altitude;
    private Double _lastUpdate;
    private Double _lat;
    private Double _long;

    @DynamoDBHashKey(attributeName = "userId")
    @DynamoDBAttribute(attributeName = "userId")
    public String getUserId() {
        return _userId;
    }

    public void setUserId(final String _userId) {
        this._userId = _userId;
    }
    @DynamoDBRangeKey(attributeName = "linkId")
    @DynamoDBIndexHashKey(attributeName = "linkId", globalSecondaryIndexName = "linkId-lastUpdate")
    public String getLinkId() {
        return _linkId;
    }

    public void setLinkId(final String _linkId) {
        this._linkId = _linkId;
    }
    @DynamoDBAttribute(attributeName = "altitude")
    public Double getAltitude() {
        return _altitude;
    }

    public void setAltitude(final Double _altitude) {
        this._altitude = _altitude;
    }
    @DynamoDBIndexRangeKey(attributeName = "lastUpdate", globalSecondaryIndexName = "linkId-lastUpdate")
    public Double getLastUpdate() {
        return _lastUpdate;
    }

    public void setLastUpdate(final Double _lastUpdate) {
        this._lastUpdate = _lastUpdate;
    }
    @DynamoDBAttribute(attributeName = "lat")
    public Double getLat() {
        return _lat;
    }

    public void setLat(final Double _lat) {
        this._lat = _lat;
    }
    @DynamoDBAttribute(attributeName = "long")
    public Double getLong() {
        return _long;
    }

    public void setLong(final Double _long) {
        this._long = _long;
    }

}
