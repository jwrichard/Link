package ca.justinrichard.link;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.amazonaws.mobile.AWSMobileClient;
import com.amazonaws.mobile.user.IdentityManager;
import com.amazonaws.mobile.AWSMobileClient;
import com.amazonaws.services.lambda.model.InvocationType;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

public class MainActivity extends AppCompatActivity {

    static final String LOG_TAG = "MainActivityClass";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AWSMobileClient.defaultMobileClient()
                .getIdentityManager()
                .getUserID(new IdentityManager.IdentityHandler() {
                    @Override
                    public void handleIdentityID(String identityId) {
                        // User's identity retrieved. You can use the identityId value
                        // to uniquely identify the user.
                        TextView t = (TextView)findViewById(R.id.userId);
                        t.setText(identityId);
                    }

                    @Override
                    public void handleError(Exception exception) {

                        // We failed to retrieve the user's identity. Set unknown user identifier
                        // in text view. Perhaps there was no network access available.

                        // ... add error handling logic here ...
                    }
                });

        // Make a test call to Lambda and set result in a textView
        new AsyncTask<Void, Void, InvokeResult>() {
            @Override
            protected InvokeResult doInBackground(Void... params) {
                try {
                    String query = "{\"operation\":\"echo\",\"payload\":{\"message\":\"hello I am lambda\"}}";
                    Charset utf8 = Charset.forName("UTF-8");
                    final ByteBuffer payload = utf8.encode(CharBuffer.wrap(query));
                    final InvokeRequest invokeRequest = new InvokeRequest().withFunctionName("Echo").withInvocationType(InvocationType.RequestResponse).withPayload(payload);
                    final InvokeResult invokeResult = AWSMobileClient.defaultMobileClient().getCloudFunctionClient().invoke(invokeRequest);
                    return invokeResult;
                } catch (final Exception e) {
                    Log.e(LOG_TAG, "AWS Lambda invocation failed : " + e.getMessage(), e);
                    final InvokeResult result = new InvokeResult();
                    result.setStatusCode(500);
                    result.setFunctionError(e.getMessage());
                    return result;
                }
            }

            @Override
            protected void onPostExecute(final InvokeResult invokeResult) {
                try {
                    final int statusCode = invokeResult.getStatusCode();
                    final String functionError = invokeResult.getFunctionError();
                    final String logResult = invokeResult.getLogResult();
                    if (statusCode != 200) {
                        //showError(invokeResult.getFunctionError());
                    } else {
                        final ByteBuffer resultPayloadBuffer = invokeResult.getPayload();
                        Charset utf8 = Charset.forName("UTF-8");
                        final String resultPayload = utf8.decode(resultPayloadBuffer).toString();
                        // ... handle the result payload ...
                        TextView t = (TextView)findViewById(R.id.lambdaResult);
                        t.setText(resultPayload);
                    }
                    if (functionError != null) {
                        Log.e(LOG_TAG, "AWS Lambda Function Error: " + functionError);
                    }
                    if (logResult != null) {
                        Log.d(LOG_TAG, "AWS Lambda Log Result: " + logResult);
                    }
                }
                catch (final Exception e) {
                    Log.e(LOG_TAG, "Unable to decode results. " + e.getMessage(), e);
                    //showError(e.getMessage());
                }
            }
        }.execute();
    }

    // User clicked the sign out button
    public void signOut(View view) {
        // Sign out
        AWSMobileClient.defaultMobileClient().getIdentityManager().signOut();

        // Go back to login screen
        Intent intent = new Intent(MainActivity.this, SignInActivity.class);
        startActivity(intent);
    }
}
