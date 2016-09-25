package ca.justinrichard.link;

import android.os.AsyncTask;
import android.util.Log;

import com.amazonaws.mobile.AWSMobileClient;
import com.amazonaws.services.lambda.model.InvocationType;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

/**
 * Created by Justin on 9/24/2016.
 *
 * Used to call methods from Amazon Lambda by the app.
 * Constructor params:
 * @
 */

public class AmazonLambdaConnector {

    static final String LOG_TAG = "AmazonLambdaConnector";
    private String functionName;
    private String inputPayload;

    public AmazonLambdaConnector(final String functionName, final String inputPayload){
        this.functionName = functionName;
        this.inputPayload = inputPayload;
    }

    public void Execute(){
        new AsyncTask<Void, Void, InvokeResult>() {
            @Override
            protected InvokeResult doInBackground(Void... params) {
                try {
                    Charset utf8 = Charset.forName("UTF-8");
                    final ByteBuffer payload = utf8.encode(CharBuffer.wrap(inputPayload));
                    final InvokeRequest invokeRequest = new InvokeRequest().withFunctionName(functionName).withInvocationType(InvocationType.RequestResponse).withPayload(payload);
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
                        Callback(resultPayload);
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

    // Called with the resulting payload from the server. Should be overwritten by calling class.
    public void Callback(String s){
        Log.i(LOG_TAG, "No callback override, dumping data: "+s);
    }
}
