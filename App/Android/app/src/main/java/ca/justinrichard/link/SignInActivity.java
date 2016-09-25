package ca.justinrichard.link;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.amazonaws.mobile.AWSMobileClient;
import com.amazonaws.mobile.user.IdentityManager;
import com.amazonaws.mobile.user.IdentityProvider;
import com.amazonaws.mobile.user.signin.FacebookSignInProvider;
import com.amazonaws.mobile.user.signin.GoogleSignInProvider;
import com.amazonaws.mobile.user.signin.SignInManager;
import com.amazonaws.mobile.user.signin.SignInProvider;
import com.facebook.FacebookSdk;

import org.json.JSONObject;

public class SignInActivity extends Activity {

    private SignInManager signInManager;

    /** Permission Request Code (Must be < 256). */
    private static final int GET_ACCOUNTS_PERMISSION_REQUEST_CODE = 93;

    /** The Google OnClick listener, since we must override it to get permissions on Marshmallow and above. */
    private View.OnClickListener googleOnClickListener;

    static final String LOG_TAG = "SignInClass";

    /**
     * SignInResultsHandler handles the final result from sign in. Making it static is a best
     * practice since it may outlive the SplashActivity's life span.
     */
    private class SignInResultsHandler implements IdentityManager.SignInResultsHandler {
        /**
         * Receives the successful sign-in result and starts the main activity.
         * @param provider the identity provider used for sign-in.
         */
        @Override
        public void onSuccess(final IdentityProvider provider) {
            // The sign-in manager is no longer needed once signed in
            SignInManager.dispose();

            // Store users preferred name and image in Dynamo
            IdentityManager idm = AWSMobileClient.defaultMobileClient().getIdentityManager();
            String userId = idm.getCachedUserID();
            String userName = idm.getCurrentIdentityProvider().getUserName();
            String imageUrl = idm.getCurrentIdentityProvider().getUserImageUrl();
            Log.i(LOG_TAG, "onSuccess: Attempting to add user information into Dynamo");
            JSONObject payload = new JSONObject();
            try {
                Log.i(LOG_TAG, "onSuccess: userId - " + userId);
                Log.i(LOG_TAG, "onSuccess: userName - " + userName);
                Log.i(LOG_TAG, "onSuccess: imageUrl - " + imageUrl);
                payload.put("userId", userId);
                payload.put("userName", userName);
                payload.put("imageUrl", imageUrl);
                AmazonLambdaConnector aws = new AmazonLambdaConnector("updateUser", payload.toString()){
                    @Override
                    public void Callback(String result) {
                        System.out.println("Result from AWS Lambda call: "+result);
                    }
                };
                aws.Execute();
            } catch(Exception e){
                Log.e(LOG_TAG, "onSuccess: Failed to create JSON object to send to aws");
            }

            // Go to home page
            Intent intent = new Intent(SignInActivity.this, MainActivity.class);
            startActivity(intent);
        }

        /**
         * Receives the sign-in result indicating the user canceled.
         * @param provider the identity provider with which the user attempted sign-in.
         */
        @Override
        public void onCancel(final IdentityProvider provider) {
            // Sign in was cancelled for the specified provider.

            // ... Nothing may need to be done here, but if you added a spinner that
            //     has been shown, you could remove it and allow the user to press
            //     one of the sign in buttons again ...
            Context context = getApplicationContext();
            CharSequence text = "Sign in cancelled";
            int duration = Toast.LENGTH_LONG;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        }

        /**
         * Receives the sign-in result that an error occurred signing in.
         * @param provider the identity provider with which the user attempted sign-in.
         * @param ex the exception that occurred.
         */
        @Override
        public void onError(final IdentityProvider provider, final Exception ex) {
            // ... Handle informing the user of an error signing in ...
            Context context = getApplicationContext();
            CharSequence text = "Error signing in";
            int duration = Toast.LENGTH_LONG;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());
        setContentView(R.layout.activity_sign_in);

        // Refresh credentials if previously signed in
        final Thread thread = new Thread(new Runnable() {
            public void run() {
                signInManager = SignInManager.getInstance(SignInActivity.this);
                final SignInProvider provider = signInManager.getPreviouslySignedInProvider();
                // If the user was already previously signed-in by a provider.
                if (provider != null) {
                    // asynchronously handle refreshing credentials and call our handler.
                    signInManager.refreshCredentialsWithProvider(SignInActivity.this, provider, new SignInResultsHandler());
                    Log.i(LOG_TAG, "Refreshing credentials.");
                } {
                    // User was not previously signed in - stay here since we are in the sign in activity
                    Log.i(LOG_TAG, "User was not previously signed in.");
                }
            }
        });
        thread.start();

        // Prepare sign in handlers and add listeners to sign in buttons
        signInManager = SignInManager.getInstance(this);
        signInManager.setResultsHandler(this, new SignInResultsHandler());
        signInManager.initializeSignInButton(FacebookSignInProvider.class, this.findViewById(R.id.fb_login_button));
        googleOnClickListener = signInManager.initializeSignInButton(GoogleSignInProvider.class, findViewById(R.id.g_login_button));
        if (googleOnClickListener != null) {
            // if the onClick listener was null, initializeSignInButton will have removed the view.
            this.findViewById(R.id.g_login_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {
                    final Activity thisActivity = SignInActivity.this;
                    if (ContextCompat.checkSelfPermission(thisActivity, Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(SignInActivity.this, new String[]{Manifest.permission.GET_ACCOUNTS}, GET_ACCOUNTS_PERMISSION_REQUEST_CODE);
                        return;
                    }
                    // call the Google onClick listener.
                    googleOnClickListener.onClick(view);
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, final String permissions[], final int[] grantResults) {
        if (requestCode == GET_ACCOUNTS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                this.findViewById(R.id.g_login_button).callOnClick();
            } else {
                Log.i(LOG_TAG, "Permissions not granted for Google sign-in. :(");
            }
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        signInManager.handleActivityResult(requestCode, resultCode, data);
    }

    // ... handle other activity life cycle events here if needed
    //     such as instrumenting onResume and onPause for Mobile Analytics ...

}