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

import com.orange.labs.sdk.OrangeListener;
import com.orange.labs.sdk.RestUtils;

import java.util.Date;
import java.util.Map;

public interface Session {

    /** Returns the App Key */
    public String getAppkey();

    /** Returns the App Secret */
    public String getAppSecret();

    /** Returns the Redirect Uri */
    public String getRedirectUri();

    /** Returns the current access token */
    public String getAccessToken();

    /** Returns the date of expire for access token */
    public Date getExpiresIn();

    /** Returns the session scope: scope separated by space */
    public String getScope();

    /** Returns the HTTP Rest Client */
    public RestUtils getRestClient();

    /** Returns headers for http request with session parameters */
    public Map<String, String> getHeaders();

    /**
     * Right to be granted by the user for an Api. Each Api can populate scope.
     * For example, Cloud Api add 'cloud' scope.
     *
     * @param scope scope for identity populate by an Api.
     */
    public void addScope(String scope);

    /**
     * Returns whether or not this session has a user's access token
     * is still valid.
     */
    public boolean isLinked();

    /**
     * Unlinks the session by removing any stored access token and secret.
     */
    public void unlink();

    /**
     * Refresh a session thanks to refresh token
     *
     * @param success Listener called when session has been refreshed
     * @param failure Listener called when error occurred
     */
    public void refresh(final OrangeListener.Success<String> success,
                        final OrangeListener.Error failure);
}
