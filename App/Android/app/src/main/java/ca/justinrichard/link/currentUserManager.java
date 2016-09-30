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

            // Instantiate the RequestQueue.
            RequestQueue queue = Volley.newRequestQueue(context);
            String url ="https://www.googleapis.com/plus/v1/people/me?access_token="+this.loginToken;
            // Request a string response from the provided URL.
            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            // Display the first 500 characters of the response string.
                            Log.w(TAG, "currentUserManager: Response from G+API:"+response.substring(0,500));
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.w(TAG, "currentUserManager: Response from G+API: Failed");
                }
            });
            // Add the request to the RequestQueue.
            queue.add(stringRequest);


        } else if(provider.equals("graph.facebook.com")) {
            // Retrieve access token
            AccessToken ak = getCurrentAccessToken();
            Log.w(TAG, "currentUserManager: Attempted to use facebook sdk to get access token, result:"+ak);

            // Initiate facebook sdk
            GraphRequest request = GraphRequest.newMeRequest(ak, new GraphRequest.GraphJSONObjectCallback() {
                @Override
                public void onCompleted(JSONObject object, GraphResponse response) {
                    // Handle response by updating dynamo
                    new UpdateDynamoAsync().execute(object);
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


    private class UpdateDynamoAsync extends AsyncTask<JSONObject, Void, Void> {
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
}
