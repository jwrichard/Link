package ca.justinrichard.link;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.models.nosql.UsersDO;
import com.amazonaws.util.StringUtils;
import com.facebook.FacebookSdk;

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

            // Get and save information about the user from either Google or Facebook
            new CurrentUserManager(getApplicationContext());

            // Check if user has a username, if they do, then we can skip the username picker
            new CheckUsernameSetTask().execute();

            // Go to the username selection activity, if one already picked, we will skip by it
            //Intent intent = new Intent(SignInActivity.this, UsernamePickerActivity.class);
            //startActivity(intent);
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

            // TODO: Clear saved credentials or something to fix bug where can't sign in with googlez
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

    /**
     * A asynchronous task used to check if username is set for the current user
     */
    public class CheckUsernameSetTask extends AsyncTask<Void, Void, Boolean> {
        private final String mUserId;

        CheckUsernameSetTask() {
            mUserId = AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // Check to see if username is taken, if so, who the owner is
            DynamoDB db = new DynamoDB();
            UsersDO user = db.GetUserFromUserId(mUserId);
            //Log.i(LOG_TAG, "Checking if I have username, user: "+user.getUserId()+", "+user.getUsername());
            if(user == null){
                // Go to sign in activity
                Intent intent = new Intent(SignInActivity.this, SignInActivity.class);
                startActivity(intent);
                Log.wtf(LOG_TAG, "Failed to get current signed in user, should rediect to sign in page");
                return true; // Should never get here!
            } else {
                if(user.getUsername() == null){
                    // Does not have a username
                    return false;
                } else {
                    // User has a username, make sure its saved
                    Log.i(LOG_TAG, "Saving my username to prefs -> "+user.getUsername());
                    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString("myUsername", user.getUsername());
                    editor.commit();
                    return true;
                }
            }
        }

        @Override
        protected void onPostExecute(final Boolean hasUsername) {
            if (hasUsername){
                Intent intent = new Intent(SignInActivity.this, MainActivity.class);
                startActivity(intent);
            } else {
                Intent intent = new Intent(SignInActivity.this, UsernamePickerActivity.class);
                startActivity(intent);
            }
        }
    }

}