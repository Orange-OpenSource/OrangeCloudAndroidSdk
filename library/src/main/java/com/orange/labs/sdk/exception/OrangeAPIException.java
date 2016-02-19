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
package com.orange.labs.sdk.exception;

import android.text.TextUtils;
import android.util.Log;

import com.android.volley.VolleyError;

import org.json.JSONObject;

public class OrangeAPIException extends Exception {

    protected int statusCode = 0;

    protected String code = "";

    protected String message = "";

    protected String description = "";


    public OrangeAPIException() {

    }

    public OrangeAPIException(Throwable throwable) {
        super(throwable);
        try {
            VolleyError error = (VolleyError)throwable;
            if (error != null && error.networkResponse != null) {
                this.statusCode = error.networkResponse.statusCode;
                String str = new String(error.networkResponse.data, "UTF-8");

                JSONObject errorJson = new JSONObject(str);
                if (errorJson != null && errorJson.optJSONObject("error") != null) {
                    // Default JSON error :
                    //       {"error":{"code":40,"message":"Missing credentials","description":"The
                    //          requested service needs credentials, but none were provided."}}

                    this.code = errorJson.optString("error");
                    this.message = errorJson.optString("error");
                    this.description = errorJson.optString("error_description");

                    errorJson = errorJson.optJSONObject("error");

                    this.code = errorJson.optString("code");
                    this.message = errorJson.optString("message");
                    this.description = errorJson.optString("description");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public OrangeAPIException(int statusCode, String code, String message, String description) {
        this.statusCode = statusCode;
        this.code = code;
        this.message = message;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public int getStatusCode() {
        return statusCode;
    }

    @Override
    public String getMessage() {
        if (!TextUtils.isEmpty(message)) {
            return message;
        } else {
            return super.getMessage();
        }
    }

    // TODO: Localized message
}
