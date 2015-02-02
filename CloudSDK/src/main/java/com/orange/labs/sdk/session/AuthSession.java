/*
 * Copyright (c) 2014 Orange.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Created by Erwan Morvillez on 07/10/14.
 */
package com.orange.labs.sdk.session;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Base64;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.Volley;
import com.orange.labs.sdk.OrangeListener;
import com.orange.labs.sdk.RestUtils;
import com.orange.labs.sdk.activity.AuthActivity;
import com.orange.labs.sdk.exception.OrangeAPIException;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class to authenticate Orange user via a web site thanks to OAuth protocol.
 */
public class AuthSession implements Session {

    private static final String SHARED_PREFERENCES_KEY
            = "com.orange.sdk.androidAuthSession.SHARED_PREFERENCE";

    private static final String SHARED_PREFERENCES_REFRESH_TOKEN
            = "com.orange.sdk.androidAuthSession.REFRESH_TOKEN";

    private static final String SHARED_PREFERENCES_FORCE_LOGIN
            = "com.orange.sdk.androidAuthSession.FORCE_LOGIN";

    public static final String API_SERVER = "https://api.orange.com";

    private static final String[] API_DEFAULT_SCOPES = new String[]{"openid", "offline_access"};

    private final String appKey;
    private final String appSecret;
    private final String redirectURI;

    private String accessToken = "";
    private String refreshToken = "";
    private Date expiresIn;

    private Context context;
    private RestUtils restClient;

    private List<String> scopes;


    /**
     * Create a new session to authenticate Android apps with the given app
     * key pair. The session will not be linked because it has no access token
     * or secret.
     *
     * @param context Activity context
     * @param appKey
     * @param appSecret
     * @param redirectURI
     */
    public AuthSession(Context context, String appKey, String appSecret, String redirectURI) {
        if (appKey == null)
            throw new IllegalArgumentException("'appKey' must be non-null");

        if (appSecret == null)
            throw new IllegalArgumentException("'appSecret' must be non-null");

        if (redirectURI == null)
            throw new IllegalArgumentException("'redirectURI' must be non-null");

        if (context == null)
            throw new IllegalArgumentException("'context' must be non-null");

        this.appKey = appKey;
        this.appSecret = appSecret;
        this.redirectURI = redirectURI;
        this.context = context;

        // Create default scope:
        for (String scope : API_DEFAULT_SCOPES)
            addScope(scope);
    }

    @Override
    public String getAppkey() {
        return appKey;
    }

    @Override
    public String getAppSecret() {
        return appSecret;
    }

    @Override
    public String getRedirectUri() {
        return redirectURI;
    }

    @Override
    public String getAccessToken() {
        return accessToken;
    }

    @Override
    public Date getExpiresIn() {
        return expiresIn;
    }

    @Override
    public RestUtils getRestClient() {
        if (restClient == null) {
            restClient = new RestUtils(context);
        }
        return restClient;
    }

    @Override
    public Map<String, String> getHeaders() {

        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", "Basic " + encodedCredentials(appKey, appSecret));
        return headers;
    }

    @Override
    public String getScope() {
        String listString = "";
        for (String s : scopes) {
            listString += s + " ";
        }
        return listString.trim();
    }


    @Override
    public void addScope(String scope) {
        if (scopes == null) {
            scopes = new ArrayList<String>();
        }
        scopes.add(scope);
    }

    @Override
    public boolean isLinked() {

        if (accessToken == null || accessToken.length() == 0)
            return false;

        // Check if access token is still valid function of expire date.
        Date now = new Date();
        if (now.getTime() < this.expiresIn.getTime())
            return true;

        return false;
    }

    @Override
    public void unlink() {
        accessToken = "";
        setRefreshToken("");

        // Write that first connection user has to be connected
        // TODO: use this method because no way to do a real logout in IDENTITY Orange API.
        SharedPreferences sharedPref = context.getSharedPreferences(SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(SHARED_PREFERENCES_FORCE_LOGIN, true);
        editor.commit();
    }

    @Override
    public void refresh(final OrangeListener.Success<String> success,
                        final OrangeListener.Error failure) {

        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(context);

        // Prepare URL
        String url = API_SERVER + "/oauth/v2/token";

        // Prepare params in body request
        JSONObject params = new JSONObject();
        try {
            params.put("grant_type", "refresh_token");
            params.put("refresh_token", getRefreshToken());
            params.put("scope", getScope());
            params.put("redirect_uri", getRedirectUri());

            getRestClient().jsonRequest(
                    "/session/refresh/",
                    Request.Method.POST,
                    url,
                    params,
                    getHeaders(),
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            accessToken = response.optString("access_token");
                            setExpiresIn(response.optInt("expires_in"));
                            success.onResponse("OK");
                        }
                    }, failure);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Start authentication. If needed, launch {@code AuthActivity}
     * allowing an user authentication else wait to check authentication with refresh token
     * {@link #checkAuthentication(com.orange.labs.sdk.OrangeListener.Success,
     * com.orange.labs.sdk.OrangeListener.Error)}
     */
    public void startAuthentication() {

        String refreshToken = getRefreshToken();
        // If refresh token exists, connect directly else
        // show web authent inside AuthActivity
        if (refreshToken == null || refreshToken.length() == 0) {

            // Start Orange auth activity.
            Intent intent = new Intent(context, AuthActivity.class);

            // Check if Oauth has to force user to enter login & password
            SharedPreferences sharedPref = context.getApplicationContext()
                    .getSharedPreferences(SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
            boolean forceLogin = sharedPref.getBoolean(SHARED_PREFERENCES_FORCE_LOGIN, false);
            intent.putExtra(AuthActivity.EXTRA_INTERNAL_APP_FORCE_LOGIN, forceLogin);

            intent.putExtra(AuthActivity.EXTRA_INTERNAL_APP_KEY, getAppkey());
            intent.putExtra(AuthActivity.EXTRA_INTERNAL_APP_SCOPE, getScope());
            intent.putExtra(AuthActivity.EXTRA_INTERNAL_APP_REDIRECT_URI, getRedirectUri());
            if (!(context instanceof Activity)) {
                // If starting the intent outside of an Activity, must include
                // this. See startActivity(). Otherwise, we prefer to stay in
                // the same task.
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(intent);
        }
    }

    /**
     * Check if authentication is successfull.
     * @param success
     * @param failure
     */
    public void checkAuthentication(
            final OrangeListener.Success<String> success,
            final OrangeListener.Error failure) {

        String refreshToken = getRefreshToken();
        // Check Refresh Token
        if (refreshToken == null || refreshToken.length() == 0) {
            // Check the result of AuthActivity

            // Check if an error occurred
            Intent data = AuthActivity.result;
            if (data != null) {
                Bundle extras = data.getExtras();
                OrangeAPIException exception = (OrangeAPIException) extras.
                        getSerializable(AuthActivity.EXTRA_EXCEPTION_ERROR);

                if (exception != null) {

                    failure.onErrorResponse(exception);
                } else {
                    // Check the authorization code to return an access token, refresh token...

                    // Prepare URL
                    String url = API_SERVER + "/oauth/v2/token";
                    // Prepare params in body request
                    JSONObject params = new JSONObject();
                    try {
                        params.put("grant_type", "authorization_code");
                        params.put("code", data.getStringExtra(AuthActivity.EXTRA_AUTHORIZATION_CODE));
                        params.put("redirect_uri", redirectURI);

                        getRestClient().jsonRequest(
                                "/session/check/authorizationCode",
                                Request.Method.POST,
                                url,
                                params,
                                getHeaders(),
                                new Response.Listener<JSONObject>() {
                                    @Override
                                    public void onResponse(JSONObject response) {
                                        // save values :
                                        accessToken = response.optString("access_token");

                                        setExpiresIn(response.optInt("expires_in"));

                                        setRefreshToken(response.optString("refresh_token"));

                                        // Check that authent is success !
                                        SharedPreferences sharedPref = context.getSharedPreferences(SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
                                        SharedPreferences.Editor editor = sharedPref.edit();
                                        editor.putBoolean(SHARED_PREFERENCES_FORCE_LOGIN, false);
                                        editor.commit();

                                        success.onResponse("OK");
                                    }
                                }, failure);

                    } catch (JSONException e) {
                        failure.onErrorResponse(new OrangeAPIException());
                    }
                }
            }
        } else {
            // get the access token with refresh token
            refresh(success, failure);
        }
    }


    private void setExpiresIn(int expiresIn) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, expiresIn);
        this.expiresIn = calendar.getTime();
    }

    private String getRefreshToken() {
        if (refreshToken == null || refreshToken.length() == 0) {
            // Check if refresh token has been stored
            SharedPreferences sharedPref = context.getApplicationContext().getSharedPreferences(SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
            refreshToken = sharedPref.getString(SHARED_PREFERENCES_REFRESH_TOKEN, "");
        }
        return refreshToken;
    }

    private void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;

        // Save refresh token in Shared Preferences
        SharedPreferences sharedPref = context.getSharedPreferences(SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(SHARED_PREFERENCES_REFRESH_TOKEN, refreshToken);
        editor.commit();
    }

    /**
     * Static method to encode (UTF-8 + Base64)
     * @param key AppKey of service
     * @param secret AppSecret of service
     * @return the encoded credentials
     */
    public static String encodedCredentials(final String key, final String secret) {

        String credentials = key + ":"
                + secret;

        byte[] encodedBytes;
        encodedBytes = Base64.encode(credentials.getBytes(), 0);
        String result = "";
        try {
            result = new String(encodedBytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return result;
    }
}
