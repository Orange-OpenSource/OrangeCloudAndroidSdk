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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.orange.labs.sdk.exception.CloudAPIException;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class RestUtils {

    private RequestQueue mRequestQueue;
    private ImageLoader.ImageCache mImageCache;
    private int maxWidth;
    private int maxHeight;

    public RestUtils(Context context) {
        // Create Volley Request Queue thanks to context
        mRequestQueue = Volley.newRequestQueue(context);

        // Fix maxWidth & maxHeight of screen
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        maxWidth = size.x;
        maxHeight = size.y;
    }

    public void setCache(ImageLoader.ImageCache cache) {
        mImageCache = cache;
    }

    public void jsonRequest(final String tag,
                            final int method,
                            final String url,
                            final JSONObject params,
                            final Map<String, String> headers,
                            final Response.Listener<JSONObject> success,
                            final OrangeListener.Error failure) {

        JsonObjectRequest jsonObjReq = new JsonObjectRequest(method, url, params, success,
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        failure.onErrorResponse(new CloudAPIException(error));
                    }
                }) {

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return headers;
            }
        };
        jsonObjReq.setTag(tag);
        mRequestQueue.add(jsonObjReq);
    }


    public void stringRequest(final String tag,
                              final int method,
                              final String url,
                              final Map<String,String> params,
                              final Map<String, String> headers,
                              final Response.Listener<String> success,
                              final OrangeListener.Error failure) {

        StringRequest jsonObjReq = new StringRequest(method, url, success,
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        failure.onErrorResponse(new CloudAPIException(error));
                    }
                }) {
            @Override
            protected Map<String,String> getParams(){
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return headers;
            }
        };
        jsonObjReq.setTag(tag);
        mRequestQueue.add(jsonObjReq);
    }

    public void imageRequest(final String tag,
                             final String url,
                             final Map<String, String> headers,
                             final OrangeListener.Success<Bitmap> success,
                             final OrangeListener.Error failure,
                             final boolean useCache) {


        if (useCache && mImageCache != null) {
            Bitmap image = mImageCache.getBitmap(tag);
            if (image != null) {
                success.onResponse(image);
                return;
            }
        }

        ImageRequest request = new ImageRequest(url,
                new Response.Listener<Bitmap>() {
                    @Override
                    public void onResponse(Bitmap bitmap) {
                        if (useCache && mImageCache != null) {
                            mImageCache.putBitmap(tag, bitmap);
                        }
                        success.onResponse(bitmap);
                    }
                }, maxWidth, maxHeight, null,
                new Response.ErrorListener() {
                    public void onErrorResponse(VolleyError error) {
                        failure.onErrorResponse(new CloudAPIException(error));
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return headers;
            }
        };
        // Adding request to request queue
        request.setTag(tag);
        mRequestQueue.add(request);
    }


    public void uploadRequest(URL url,
                              File file,
                              String folderIdentifier,
                              final Map<String, String> headers,
                              final OrangeListener.Success<JSONObject> success,
                              final OrangeListener.Progress progress,
                              final OrangeListener.Error failure) {


        // open a URL connection to the Server
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(file);

            // Open a HTTP connection to the URL
            HttpURLConnection conn = (HttpsURLConnection) url.openConnection();

            // Allow Inputs & Outputs
            conn.setDoInput(true);
            conn.setDoOutput(true);

            // Don't use a Cached Copy
            conn.setUseCaches(false);

            conn.setRequestMethod("POST");

            //
            // Define headers
            //

            // Create an unique boundary
            String boundary = "UploadBoundary";

            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
            for (String key : headers.keySet()) {
                conn.setRequestProperty(key.toString(), headers.get(key));
            }

            //
            // Write body part
            //
            DataOutputStream dos = new DataOutputStream(conn.getOutputStream());

            int bytesAvailable = fileInputStream.available();

            String marker = "\r\n--" + boundary + "\r\n";

            dos.writeBytes(marker);
            dos.writeBytes("Content-Disposition: form-data; name=\"description\"\r\n\r\n");

            // Create JSonObject :
            JSONObject params = new JSONObject();
            params.put("name", file.getName());
            params.put("size", String.valueOf(bytesAvailable));
            params.put("folder", folderIdentifier);

            dos.writeBytes(params.toString());

            dos.writeBytes(marker);
            dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"\r\n");
            dos.writeBytes("Content-Type: image/jpeg\r\n\r\n");

            int progressValue = 0;
            int bytesRead = 0;
            byte buf[] = new byte[1024];
            BufferedInputStream bufInput = new BufferedInputStream(fileInputStream);
            while ((bytesRead = bufInput.read(buf)) != -1) {
                // write output
                dos.write(buf, 0, bytesRead);
                dos.flush();
                progressValue += bytesRead;
                // update progress bar
                progress.onProgress((float) progressValue / bytesAvailable);
            }

            dos.writeBytes(marker);

            //
            // Responses from the server (code and message)
            //
            int serverResponseCode = conn.getResponseCode();
            String serverResponseMessage = conn.getResponseMessage();

            // close streams
            fileInputStream.close();
            dos.flush();
            dos.close();

            if (serverResponseCode == 200 || serverResponseCode == 201) {
                BufferedReader rd = new BufferedReader(new InputStreamReader(
                        conn.getInputStream()));

                String response = "";
                String line;
                while ((line = rd.readLine()) != null) {
                    Log.i("FileUpload", "Response: " + line);
                    response += line;
                }
                rd.close();
                JSONObject object = new JSONObject(response);
                success.onResponse(object);
            } else {
                BufferedReader rd = new BufferedReader(new InputStreamReader(
                        conn.getErrorStream()));
                String response = "";
                String line;
                while ((line = rd.readLine()) != null) {
                    Log.i("FileUpload", "Error: " + line);
                    response += line;
                }
                rd.close();
                JSONObject errorResponse = new JSONObject(response);
                failure.onErrorResponse(new CloudAPIException(serverResponseCode, errorResponse));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
