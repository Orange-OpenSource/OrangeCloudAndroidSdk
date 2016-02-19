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

public class CloudAPIException extends OrangeAPIException {

    private static String TAG = "CloudException";

    public CloudAPIException() {
        super();
    }

    public CloudAPIException(int statusCode, String code, String message, String description) {
        super(statusCode, code, message, description);
    }

    public CloudAPIException(Throwable throwable) {
        super(throwable);
        try {
            VolleyError error = (VolleyError)throwable;
            if (error != null && error.networkResponse != null) {
                this.statusCode = error.networkResponse.statusCode;
                String str = new String(error.networkResponse.data, "UTF-8");
                JSONObject errorJson = new JSONObject(str);
                if (errorJson != null) {
                    JSONObject errorJsonLv1 = errorJson.optJSONObject("error");
                    if (errorJsonLv1 != null) {
                        this.code = errorJsonLv1.optString("code");

                        this.message = errorJsonLv1.optString("message");
                        if (TextUtils.isEmpty(this.message)) {
                            this.message = errorJsonLv1.optString("label");
                        }
                        this.description = errorJsonLv1.optString("description");
                        if (TextUtils.isEmpty(this.description)) {
                            this.description = errorJsonLv1.optString("details");
                        }
                    } else {
                        String err = errorJson.optString("error");
                        if (!err.isEmpty()) {
                            // Identity Error
                            this.code = errorJson.optString("error");
                            this.message = errorJson.optString("error");
                            this.description = errorJson.optString("error_description");
                        } else {
                            this.code = errorJson.optString("code");

                            this.message = errorJson.optString("message");
                            if (TextUtils.isEmpty(this.message)) {
                                this.message = errorJson.optString("label");
                            }
                            this.description = errorJson.optString("description");
                            if (TextUtils.isEmpty(this.description)) {
                                this.description = errorJson.optString("details");
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public CloudAPIException(int statusCode, JSONObject errorJson) {
        super();
        this.statusCode = statusCode;
        errorJson = errorJson.optJSONObject("error");
        if (errorJson != null) {
            this.code = errorJson.optString("code");

            this.message = errorJson.optString("message");
            if (TextUtils.isEmpty(this.message)) {
                this.message = errorJson.optString("label");
            }
            this.description = errorJson.optString("description");
            if (TextUtils.isEmpty(this.description)) {
                this.description = errorJson.optString("details");
            }

        } else {
            this.message = "";
            this.description = "";
            this.code = "";
        }
    }
}
