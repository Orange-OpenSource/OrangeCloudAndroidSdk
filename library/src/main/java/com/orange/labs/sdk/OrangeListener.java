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
package com.orange.labs.sdk;

import com.orange.labs.sdk.exception.OrangeAPIException;


public final class OrangeListener {

    /**
     * Callback interface for delivering responses Cloud Api.
     */
    public interface Success<T> {
        /**
         * Called when a response is received.
         * @param response the response listener
         */
        public abstract void onResponse(T response);
    }

    /**
     * Callback interface for delivering error responses.
     */
    public interface Progress {
        /**
         * Callback method to notify progress upload or download.
         * @param ratio the upload progress [0,1]
         */
        public void onProgress(float ratio);
    }

    /**
     * Callback interface for delivering error responses.
     */
    public interface Error {
        /**
         * Callback method that an error has been occurred with the
         * provided error code and optional user-readable message.
         * @param error the Orange Api Cloud error
         */
        public void onErrorResponse(OrangeAPIException error);
    }
}
