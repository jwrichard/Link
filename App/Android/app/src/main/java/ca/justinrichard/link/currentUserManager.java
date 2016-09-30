package ca.justinrichard.link;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import com.amazonaws.mobile.AWSMobileClient;
import com.amazonaws.mobile.user.IdentityManager;

import org.json.JSONException;
import org.json.JSONObject;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.models.nosql.UsersDO;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import static com.facebook.AccessToken.getCurrentAccessToken;


/**
 * Created by Justin on 9/29/2016.
 */

public class currentUserManager {
    private final String TAG = "CurrentUserManagerClass";

    private String provider;
    private String loginToken;

    public currentUserManager(Context context) {
        IdentityManager idm = AWSMobileClient.defaultMobileClient().getIdentityManager();
        this.provider = idm.getCurrentIdentityProvider().getCognitoLoginKey();
        this.loginToken = idm.getCurrentIdentityProvider().getToken();

        if(provider.equals("accounts.google.com")) {
            // Call reloadUserInfo on separate thread and put results into db
            new GoogleUpdate().execute();

        } else if(provider.equals("graph.facebook.com")) {
            // Retrieve access token
            AccessToken ak = getCurrentAccessToken();
            Log.w(TAG, "currentUserManager: Attempted to use facebook sdk to get access token, result:"+ak);

            // Initiate facebook sdk
            GraphRequest request = GraphRequest.newMeRequest(ak, new GraphRequest.GraphJSONObjectCallback() {
                @Override
                public void onCompleted(JSONObject object, GraphResponse response) {
                    // Handle response by updating dynamo
                    new FacebookUpdate().execute(object);
                }
            });
            Bundle parameters = new Bundle();
            parameters.putString("fields", "first_name,last_name,picture,email");
            request.setParameters(parameters);
            request.executeAsync();

        } else {
            Log.w(TAG, "currentUserManager: Failed to recognize the login provider, user data not retrievable");
        }
    }

    private class FacebookUpdate extends AsyncTask<JSONObject, Void, Void> {
        @Override
        protected Void doInBackground(JSONObject... objects) {
            // Fetch the default configured DynamoDB ObjectMapper
            final DynamoDBMapper dynamoDBMapper = AWSMobileClient.defaultMobileClient().getDynamoDBMapper();
            final UsersDO user = new UsersDO();
            user.setUserId(AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID());
            try {
                user.setFirstName(objects[0].get("first_name").toString());
                user.setLastName(objects[0].get("last_name").toString());
                user.setImageUrl(objects[0].getJSONObject("picture").getJSONObject("data").get("url").toString());
                dynamoDBMapper.save(user);
            } catch(JSONException e){
                Log.w(TAG, "doInBackground: Failed to parse JSON and update DB");
            }
            return null;
        }
    }

    private class GoogleUpdate extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... objects) {
            // Fetch the default configured DynamoDB ObjectMapper
            final DynamoDBMapper dynamoDBMapper = AWSMobileClient.defaultMobileClient().getDynamoDBMapper();
            final UsersDO user = new UsersDO();
            user.setUserId(AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID());
            try {
                IdentityManager idm = AWSMobileClient.defaultMobileClient().getIdentityManager();
                idm.getCurrentIdentityProvider().reloadUserInfo();
                String fullName = idm.getCurrentIdentityProvider().getUserName();
                String firstName = fullName.substring(0, fullName.indexOf(" "));
                String lastName = fullName.substring(fullName.indexOf(" "));
                String imageUrl = idm.getCurrentIdentityProvider().getUserImageUrl();

                user.setFirstName(firstName);
                user.setLastName(lastName);
                user.setImageUrl(imageUrl);

                dynamoDBMapper.save(user);
            } catch(Exception e){
                Log.w(TAG, "doInBackground: Handled exception:"+e);
            }
            return null;
        }
    }
}
