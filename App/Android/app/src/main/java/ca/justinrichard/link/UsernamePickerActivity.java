package ca.justinrichard.link;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.amazonaws.mobile.AWSMobileClient;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.models.nosql.UsersDO;


/**
 * A login screen that offers login via email/password.
 */
public class UsernamePickerActivity extends Activity {

    final String TAG = "UsernamePickerActivity";

    // Keep track of the login task to ensure we can cancel it if requested.
    private UsernameTask mAuthTask = null;

    // UI references.
    private EditText mUsernameView;
    private Button mSubmitButton;
    private View mProgressView;
    private View mLoginFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_username_picker);

        // Set up the form
        mUsernameView = (EditText) findViewById(R.id.editUsername);
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        mUsernameView.append(settings.getString("myUsername", ""));
        Log.i("UsernamePicker", "Setting text field text to: "+settings.getString("myUsername", ""));

        mSubmitButton = (Button) findViewById(R.id.username_submit_button);
        mSubmitButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptUsername();
            }
        });
        mLoginFormView = findViewById(R.id.username_form);
        mProgressView = findViewById(R.id.username_progress);
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptUsername() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mUsernameView.setError(null);

        // Store values at the time of the submission attempt
        String username = mUsernameView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid username
        if (TextUtils.isEmpty(username)) {
            mUsernameView.setError(getString(R.string.error_field_required));
            focusView = mUsernameView;
            cancel = true;
        } else if (!isUsernameValid(username)) {
            mUsernameView.setError("Username must be 3-20 characters long");
            focusView = mUsernameView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            String userId = AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID();
            mAuthTask = new UsernameTask(username, userId);
            mAuthTask.execute((Void) null);
        }
    }

    /*
     * Makes sure a username is valid, currently only checks length
     */
    private boolean isUsernameValid(String username) {
        return username.length() > 2 && username.length() < 21;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }


    /**
     * A asynchronous task used to check if username is valid, and if so, assign it and move
     * into main activity - if not, then get a new one from user.
     */
    public class UsernameTask extends AsyncTask<Void, Void, Boolean> {
        private final String mUserId;
        private final String mUsername;

        UsernameTask(String username, String userId) {
            mUserId = userId;
            mUsername = username;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // Check to see if username is taken, if so, who the owner is
            final DynamoDBMapper dynamoDBMapper = AWSMobileClient.defaultMobileClient().getDynamoDBMapper();
            DynamoDB db = new DynamoDB();
            UsersDO user = db.GetUserFromUsername(mUsername);

            Log.i(TAG, "For username "+mUsername+" found user: "+user);

            if(user == null){
                // Nobody with that username, we can have it!
                // Set username for me and return true
                user = db.GetUserFromUserId(mUserId);
                Log.i(TAG, "For self "+mUserId+" found user: "+user);
                user.setUsername(mUsername);
                dynamoDBMapper.save(user);

                // Save username to local prefs to access later
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("myUsername", mUsername);
                editor.commit();
                return true;
            } else {
                if(user.getUserId().equals(mUserId)){
                    // It is me!
                    return true;
                } else {
                    // Not me, and username taken
                    return false;
                }
            }
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);
            if (success){
                Intent intent = new Intent(UsernamePickerActivity.this, MainActivity.class);
                startActivity(intent);
            } else {
                mUsernameView.setError("Username is currently unavailable");
                mUsernameView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}

