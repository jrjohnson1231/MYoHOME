package ndhackers2015.myohome;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import android.os.AsyncTask;

/**
 * Created by BillBrown30188 on 11/4/14.
 */
public class DigitalLifeController {

    private static DigitalLifeController me;

    private String penguinAppId;
    private String authToken;
    private String requestToken;
    private String gatewayGUID;

    // holds the digital life device objects retrieved from the server.
    private JSONObject deviceJsonData;

    private boolean zip = true;

    protected String baseURL;
    protected HttpClient httpclient;
    protected HttpContext localContext;

    public boolean isLoggedIn = false;

    private final JSONParser jsonParser = new JSONParser();

    public static synchronized DigitalLifeController getInstance() {
        if (me == null) {
            me = new DigitalLifeController();
        }

        return me;
    }

    private DigitalLifeController() {
    }

    public void init(String penguinAppId, String baseURL) {
        this.penguinAppId = penguinAppId;
        this.baseURL = baseURL;

        // Create a local instance of cookie store
        CookieStore cookieStore = new BasicCookieStore();

        // Create local HTTP context
        localContext = new BasicHttpContext();

        // Bind custom cookie store to the local context
        localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);

        httpclient = new DefaultHttpClient();
    }

    public void login(final String userName, final String password)
            throws Exception {

        AsyncTask<JSONObject, Void, JSONObject> task = new AsyncTask<JSONObject, Void, JSONObject>() {

            @Override
            protected JSONObject doInBackground(JSONObject... params) {
                System.out.println("starting task in background");
                JSONObject data = null;
                try {
                    data = getResource("/penguin/api/authtokens",
                            new BasicNameValuePair("domain", "DL"),
                            new BasicNameValuePair("appKey", penguinAppId),
                            new BasicNameValuePair("userId", userName),
                            new BasicNameValuePair("password", password));

                    // content contains customer data
                    JSONObject content = (JSONObject) data.get("content");

                    // extract gateway guid
                    JSONArray gateways = (JSONArray) content.get("gateways");
                    JSONObject gateway = (JSONObject) gateways.get(0);  // just grabbing the first for this illustration
                    gatewayGUID = (String) gateway.get("id");

                    // extract the auth token
                    authToken = (String) content.get("authToken");

                    // extract the request token
                    requestToken = (String) content.get("requestToken");

                    System.out.println("Gateway:  " + gatewayGUID
                            + "\nAuthToken: " + authToken + "\nRequestToken: "
                            + requestToken);

                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    deviceJsonData = getResource("/penguin/api/{gatewayGUID}/devices");
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return data;
            }

            @Override
            protected void onPostExecute(JSONObject data) {
                if (data == null) {
                    isLoggedIn = false;
                    return;
                }

                isLoggedIn = true;

                // content contains customer data
                JSONObject content = (JSONObject) data.get("content");

                // extract gateway guid
                JSONArray gateways = (JSONArray) content.get("gateways");
                JSONObject gateway = (JSONObject) gateways.get(0);  // just grabbing the first for this illustration
                gatewayGUID = (String) gateway.get("id");

                // extract the auth token
                authToken = (String) content.get("authToken");

                // extract the request token
                requestToken = (String) content.get("requestToken");

                System.out.println("Gateway:  " + gatewayGUID
                        + "\nAuthToken: " + authToken + "\nRequestToken: "
                        + requestToken);
            }
        };

        task.execute();
        task.get(5000, TimeUnit.MILLISECONDS);

    }

    public JSONObject getResource(String relativeUrl) throws Exception {
        return fastRequestHelper(new HttpGet(cleanURL(relativeUrl)));
    }

    public JSONObject getResource(String relativeUrl,
                                  NameValuePair... postParams) throws Exception {
        HttpPost request = new HttpPost(cleanURL(relativeUrl));
        request.setEntity(new UrlEncodedFormEntity(Arrays.asList(postParams)));
        return fastRequestHelper(request);
    }

    private JSONObject fastRequestHelper(HttpRequestBase request)
            throws IOException, org.json.simple.parser.ParseException {

        request.addHeader("authToken", authToken);
        request.addHeader("requestToken", requestToken);
        request.addHeader("appKey", penguinAppId);

        HttpResponse response = httpclient.execute(request, localContext);
        HttpEntity entity = response.getEntity();

        InputStreamReader in = zip ? new InputStreamReader(new GZIPInputStream(
                entity.getContent())) : new InputStreamReader(
                entity.getContent());

        Object result;
        synchronized (jsonParser) { // Parser is not thread safe!
            result = jsonParser.parse(in);
        }
        System.out.println("fastRequest" +(JSONObject) result );
        return (JSONObject) result;
    }

    public InputStream getInputStreamForResource(String absoluteUrl) throws Exception {
        URL u = new URL(absoluteUrl);
        return u.openStream();
    }

    public String updateDevice(final String deviceGUID, final String action, final String value) {
        Runnable r = new Runnable() {

            @Override
            public void run() {
                try {
                    HttpPost request = new HttpPost(baseURL + "/penguin/api/" + gatewayGUID + "/devices/" + deviceGUID + "/" + action + "/" + value);
                    System.out.println("request:  " + request.getURI());

                    request.addHeader("authToken", authToken);
                    request.addHeader("requestToken", requestToken);
                    request.addHeader("appKey", penguinAppId);


                    HttpResponse response = httpclient.execute(request);
                    HttpEntity entity = response.getEntity();

                    String returnValue = EntityUtils.toString(entity);
                    System.out.println(returnValue);
                } catch (Exception exp) {
                    System.out.println(exp.getCause() + " " + exp.getMessage());
                    throw new RuntimeException(exp);
                }
            }
        };
        new Thread(r).start();
        return "";
    }

    private String cleanURL(String relativeUrl) {
        relativeUrl = (baseURL + relativeUrl).replaceAll(
                "(?i)\\{gatewayGUID\\}", gatewayGUID);
        if (zip) {
            relativeUrl += relativeUrl.contains("?") ? "&" : "?";
            return relativeUrl + "zip=1";
        } else
            return relativeUrl;
    }

    public static JSONArray getDevicesAsJSON(Object objectIn) {

        JSONArray object = (JSONArray) ((JSONObject) objectIn).get("content");
        return object;

    }

    /**
     * Assigns the correct ICON to the device by device type.
     *
     * @param
     */
 /*   public static void decorateDevice(DigitalLifeDevice device) {
        String deviceType = device.getDeviceType();
        if ("door-lock".equalsIgnoreCase(deviceType)) {
            if ("unlocked".equalsIgnoreCase(device.getStatus())) {
                device.setResourceID(R.drawable.device_list_icon_lock_inactive);
            } else {
                device.setResourceID(R.drawable.device_list_icon_lock_active);
            }
        } else if ("smart-plug".equalsIgnoreCase(deviceType)) {
            if ("off".equalsIgnoreCase(device.getStatus())) {
                device.setResourceID(R.drawable.device_list_icon_power_inactive);
            } else {
                device.setResourceID(R.drawable.device_list_icon_power_active);
            }
        } else if ("contact-sensor".equalsIgnoreCase(deviceType)) {
            if ("closed".equalsIgnoreCase(device.getStatus())) {
                device.setResourceID(R.drawable.device_list_icon_sensor_inactive);
            } else {
                device.setResourceID(R.drawable.device_list_icon_sensor_active);
            }
        } else if ("water-sensor".equalsIgnoreCase(deviceType)) {
            if ("closed".equalsIgnoreCase(device.getStatus())) {
                device.setResourceID(R.drawable.device_list_icon_water_temp_inactive);
            } else {
                device.setResourceID(R.drawable.device_list_icon_water_temp_active);
            }
        }
    }
*/
    public JSONArray fetchDevices() {
        JSONArray devices = getDevicesAsJSON(deviceJsonData);
        return devices;
    }

}