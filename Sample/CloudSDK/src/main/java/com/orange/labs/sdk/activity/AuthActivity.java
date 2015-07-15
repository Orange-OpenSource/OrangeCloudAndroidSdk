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
package com.orange.labs.sdk.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.orange.labs.sdk.exception.OrangeAPIException;
import com.orange.labs.sdk.session.AuthSession;

import java.io.Serializable;

/**
 * This activity is used internally for authentication. It opens a web view with
 * Orange OAuth protcol.
 */
public class AuthActivity extends Activity {

    // For communication from AuthSession to this activity.
    public static final String EXTRA_INTERNAL_APP_KEY = "EXTRA_INTERNAL_APP_KEY";
    public static final String EXTRA_INTERNAL_APP_SCOPE = "EXTRA_INTERNAL_APP_SCOPE";
    public static final String EXTRA_INTERNAL_APP_REDIRECT_URI = "EXTRA_INTERNAL_APP_REDIRECT_URI";
    public static final String EXTRA_INTERNAL_APP_FORCE_LOGIN = "EXTRA_INTERNAL_APP_FORCE_LOGIN";

    // For communication from this activity to AuthSession.
    public static final String EXTRA_AUTHORIZATION_CODE = "EXTRA_AUTHORIZATION_CODE";
    public static final String EXTRA_EXCEPTION_ERROR = "EXTRA_EXCEPTION_ERROR";

    public static Intent result = null;
    private String appKey;
    private String appScope;
    private String appRedirectURI;
    private boolean appForceLogin;
    private WebView mWebView;

    // Stored in savedInstanceState to track an ongoing auth attempt, which
    // must include a locally-generated nonce in the response.
    private String authStateNonce = null;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if (savedInstanceState == null) {
            authStateNonce = null;
            Intent intent = getIntent();
            appKey = intent.getStringExtra(EXTRA_INTERNAL_APP_KEY);
            appScope = intent.getStringExtra(EXTRA_INTERNAL_APP_SCOPE);
            appRedirectURI = intent.getStringExtra(EXTRA_INTERNAL_APP_REDIRECT_URI);
            appForceLogin = intent.getBooleanExtra(EXTRA_INTERNAL_APP_FORCE_LOGIN, false);

            setTheme(android.R.style.Theme_Translucent_NoTitleBar);

            // Create webView and add to Activity.
            mWebView = new WebView(this);
            setContentView(mWebView);
            mWebView.getSettings().setJavaScriptEnabled(true);

            // Add a web client that return an authorization code.
            mWebView.setWebViewClient(new AuthWebViewClient());

            startWebAuth();

        } else {
            authStateNonce = savedInstanceState.getString("authStateNonce");
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("authStateNonce", authStateNonce);
    }

    /**
     * Open a webview with OAuth protocol.
     */
    private void startWebAuth() {
        String path = "/oauth/v2/authorize";

        String url = AuthSession.API_SERVER
                + path
                + "?scope=" + appScope
                + "&response_type=code"
                + "&client_id=" + appKey
                + "&state=state"
                + "&redirect_uri=" + appRedirectURI;

        if (appForceLogin) {
            url += "&prompt=login%20consent";
        }
        mWebView.loadUrl(url);
    }

    private void finishWebAuth(String authorizationCode) {
        // Successful auth.
        // Create new intent and set result
        Intent newResult = new Intent();
        newResult.putExtra(EXTRA_AUTHORIZATION_CODE, authorizationCode);

        result = newResult;
        // and Stop Activity
        finish();
    }

    private void finishWithErrorWebAuth(OrangeAPIException exception) {
        Intent newResult = new Intent();
        newResult.putExtra(EXTRA_EXCEPTION_ERROR, (Serializable) exception);
        result = newResult;
        finish();
    }

    /**
     * Extend WebViewClient class in order to catch Orange connection information.
     */
    private class AuthWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.startsWith(AuthActivity.this.appRedirectURI)) {
                String authorizationCode = getAuthorizationCodeFromURL(url);
                if (authorizationCode != null) {
                    // Return the authorization code
                    finishWebAuth(authorizationCode);
                } else {
                    // Find error and return
                    finishWithErrorWebAuth(getErrorCodeFromURL(url));
                }
                return true;
            }
            return false;
        }

        /**
         * Extract the authorization code in the url parameter.
         * @param url the redirect uri with arguments
         * @return the authorization code.
         */
        private String getAuthorizationCodeFromURL(String url) {
            // TODO: manage Exception
            String[] arguments = url.split("\\?");
            if (arguments != null && arguments.length > 1) {
                String[] parameters = arguments[1].split("&");
                String prefix = "code=";
                for (String parameter : parameters) {
                    if (parameter.startsWith(prefix)) {
                        String code = parameter.substring(prefix.length(),
                                parameter.length());
                        return code;
                    }
                }
            }
            return null;
        }

        /**
         * Extract an exception with arguments in url parameter
         * @param url the redirect uri with arguments
         * @return an exception
         */
        private OrangeAPIException getErrorCodeFromURL(String url) {
            String[] arguments = url.split("\\?");
            String[] parameters = arguments[1].split("&");
            String prefix;
            String code = "";
            String description = "";
            for (String parameter : parameters) {
                prefix = "error=";
                if (parameter.startsWith(prefix)) {
                    code = parameter.substring(prefix.length(), parameter.length())
                            .replace("%20", " ");
                }
                prefix = "error_description=";
                if (parameter.startsWith(prefix)) {
                    description = parameter.substring(prefix.length(), parameter.length())
                            .replace("%20", " ");
                }
            }
            // TODO: choose an HTTP code
            return new OrangeAPIException(403, code, code, description);
        }
    }
}
