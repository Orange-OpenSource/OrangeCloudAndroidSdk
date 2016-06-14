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

import android.graphics.Bitmap;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.android.volley.Request.Method;
import com.android.volley.Response;
import com.android.volley.toolbox.ImageLoader.ImageCache;
import com.orange.labs.sdk.exception.OrangeAPIException;
import com.orange.labs.sdk.session.Session;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Location of the Orange Cloud API functions.
 *
 * The class is parameterized with the type of session it uses. This will be
 * the same as the type of session you pass into the constructor.
 */
public final class OrangeCloudAPI<SESS_T extends Session> {

    /**
     * The version of this Orange SDK.
     */
    public static final String SDK_VERSION = "1.0.6";
    // Server information
    private static String API_URL = "https://api.orange.com/cloud/";
    private static String API_CONTENT_URL = "https://cloudapi.orange.com/cloud/";

    private static String API_VERSION = "v1";
    // Parameters linked to OAuth
    private static String CLOUDAPI_DEFAULT_SCOPE = "cloud";
    // Internal
    private SESS_T session;


    /**
     * Create an instance of Orange Cloud Api
     *
     * @param session Orange Session used to authenticate client on Cloud Api
     */
    public OrangeCloudAPI(SESS_T session) {

        if (session == null) {
            throw new IllegalArgumentException("Session must not be null.");
        }
        this.session = session;

        // Need to add CloudAPI default scope
        session.addScope(CLOUDAPI_DEFAULT_SCOPE);
    }

    /**
     * Add additional scope to OpenIDConnect.
     * To be called before session.startAuthentication();
     * For example, if you have right, you can add cloudfullread
     * to have a full access to the Orange cloud.
     *
     * @param scope OIDC scope to add.
     */
    public void addScope(String scope) {
        if (scope != null && scope.length() > 0) {
            session.addScope(scope.trim());
        }
    }

    /**
     * @return the current session
     */
    public SESS_T getSession() {
        return session;
    }

    /**
     * Set an image cache
     * @param imageCache ImageCache Object (see sample code)
     */
    public void setImageCache(ImageCache imageCache) {
        session.getRestClient().setCache(imageCache);
    }

    /**
     * Unlink session and Cloud Api.
     */
    public void unlink() {
        getSession().unlink();
    }

    /**
     * Get the available space of the current account.
     *
     * @param success callback returning the CloudItem (folders and its elements)
     * @param failure callback when error occurred
     */
    public void freespace(final OrangeListener.Success<Long> success,
                          final OrangeListener.Error failure) {

        // Create Tag used to cancel the request
        final String tag = "Cloud/freespace/";

        // Prepare URL
        final String url = API_URL + API_VERSION + "/freespace";

        session.getRestClient().jsonRequest(tag, Method.GET, url, null, getHeaders(),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        long freeSpace = response.optLong("freespace");
                        success.onResponse(new Long(freeSpace));
                    }
                }, new OrangeListener.Error() {
                    @Override
                    public void onErrorResponse(OrangeAPIException error) {
                        checkSession(error, new OrangeListener.Success<String>() {
                            @Override
                            public void onResponse(String response) {
                                // retry request
                                freespace(success, failure);
                            }
                        }, failure);

                    }
                });
    }

    /**
     * List the content of a folder. You must call this method first to be able
     * to access to cloud information with null to obtain root folder.
     *
     * @param entry   folder object to list. Can be null to get the root access.
     * @param success callback returning a Entry (folders and its contents)
     * @param failure callback when error occurred
     *
     * @deprecated
     *    Replaced by {@link #listEntries(com.orange.labs.sdk.OrangeCloudAPI.Entry, JSONObject, com.orange.labs.sdk.OrangeListener.Success, com.orange.labs.sdk.OrangeListener.Error)}
     */
    @Deprecated
    public void listFolder(final Entry entry,
                           final OrangeListener.Success<Entry> success,
                           final OrangeListener.Error failure) {

        String entryIdentifier = "";
        if (entry != null) {
            entryIdentifier = entry.identifier;
        }

        // Create Tag used to cancel the request
        final String tag = "Cloud/folder/list/" + entryIdentifier;

        // Prepare URL
        final String url = API_URL + API_VERSION + "/folders/" + entryIdentifier;

        session.getRestClient().jsonRequest(tag, Method.GET, url, null, getHeaders(),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        success.onResponse(new Entry(response));
                    }
                }, new OrangeListener.Error() {
                    @Override
                    public void onErrorResponse(OrangeAPIException error) {

                        checkSession(error, new OrangeListener.Success<String>() {
                            @Override
                            public void onResponse(String response) {
                                // retry request
                                listFolder(entry, success, failure);
                            }
                        }, failure);
                    }
                });
    }

    /**
     * List the content of a entries function of parameters.
     * see https://developer.orange.com/apis/cloud-france/api-reference
     *
     * @param entry         folder object to list. Can be null to get the root access.
     * @param parameters    Add parameters (see documentation)
     * @param success       callback returning a Entry (folders and its contents)
     * @param failure       callback when error occurred
     */

    public void listEntries(final Entry entry,
                           final JSONObject parameters,
                           final OrangeListener.Success<Entry> success,
                           final OrangeListener.Error failure) {

        String entryIdentifier = "";
        if (entry != null) {
            entryIdentifier = entry.identifier;
        }

        // Create Tag used to cancel the request
        final String tag = "Cloud/entries/list/" + entryIdentifier;

        // Prepare URL
        String url = API_URL + API_VERSION + "/folders/" + entryIdentifier;
        if (parameters != null) {
            try {
                url += "?";
                Iterator<String> keys = parameters.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    url += key + "=" + parameters.get(key) + "&";
                }
            } catch (JSONException e) {
                // TODO:
                e.printStackTrace();
            }
        }
        session.getRestClient().jsonRequest(tag, Method.GET, url, null, getHeaders(),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        success.onResponse(new Entry(response));
                    }
                }, new OrangeListener.Error() {
                    @Override
                    public void onErrorResponse(OrangeAPIException error) {
                        checkSession(error, new OrangeListener.Success<String>() {
                            @Override
                            public void onResponse(String response) {
                                // retry request
                                listEntries(entry, parameters, success, failure);
                            }
                        }, failure);
                    }
                });
    }

    /**
     * Create a new folder.
     *
     * @param entry   the entry item where the folder has to be created. If null, folder will be
     *                created in the root folder.
     * @param name    the name of the folder to be created.
     * @param success callback returning the new created Entry
     * @param failure callback when error occurred
     */
    public void createFolder(final Entry entry,
                             final String name,
                             final OrangeListener.Success<Entry> success,
                             final OrangeListener.Error failure) {

        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("name must not be null or empty.");
        }

        // Create Tag used to cancel the request
        final String tag = "Cloud/folder/add/" + name;

        // Prepare params
        final JSONObject params = new JSONObject();
        try {
            params.put("name", name);
            if (entry != null) {
                params.put("parentFolderId", entry.identifier);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Prepare URL
        final String url = API_URL + API_VERSION + "/folders/";

        session.getRestClient().jsonRequest(tag, Method.POST, url, params, getHeaders(), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                success.onResponse(new Entry(response));
            }
        }, new OrangeListener.Error() {
            @Override
            public void onErrorResponse(OrangeAPIException error) {
                checkSession(error, new OrangeListener.Success<String>() {
                    @Override
                    public void onResponse(String response) {
                        createFolder(entry, name, success, failure);
                    }
                }, failure);
            }
        });
    }

    /**
     * Rename an Entry (files or folders)
     *
     * @param entry   entry to rename
     * @param name    new name of entry
     * @param success callback when delete is completed
     * @param failure callback when error occurred
     */
    public void rename(final Entry entry,
                             final String name,
                             final OrangeListener.Success<Entry> success,
                             final OrangeListener.Error failure) {

        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("name must not be null or empty.");
        }

        // Create Tag used to cancel the request
        final String tag = "Cloud/rename/" + name;

        // Prepare params
        final JSONObject params = new JSONObject();
        try {
            params.put("name", name);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Prepare URL
        final String url = API_URL + API_VERSION
                + ((entry.type == Entry.Type.DIRECTORY)
                ? "/folders/"
                : "/files/")
                + entry.identifier;
        session.getRestClient().jsonRequest(tag, Method.POST, url, params, getHeaders(), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                success.onResponse(new Entry(response));
            }
        }, new OrangeListener.Error() {
            @Override
            public void onErrorResponse(OrangeAPIException error) {
                checkSession(error, new OrangeListener.Success<String>() {
                    @Override
                    public void onResponse(String response) {
                        rename(entry, name, success, failure);
                    }
                }, failure);
            }
        });
    }

    /**
     * Copy an Entry (files or folders) in a new folder
     *
     * @param entry   entry to copy
     * @param destination  the folder to copy
     * @param success callback when delete is completed
     * @param failure callback when error occurred
     */
    public void copy(final Entry entry,
                     final Entry destination,
                     final OrangeListener.Success<Entry> success,
                     final OrangeListener.Error failure) {


        // Create Tag used to cancel the request
        final String tag = "Cloud/copy/" + entry.name;

        // Prepare params
        final JSONObject params = new JSONObject();
        try {
            params.put("parentFolderId", destination.identifier);
            params.put("clone", true);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        // Prepare URL
        final String url = API_URL + API_VERSION
                + ((entry.type == Entry.Type.DIRECTORY)
                ? "/folders/"
                : "/files/")
                + entry.identifier;
        session.getRestClient().jsonRequest(tag, Method.POST, url, params, getHeaders(), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                success.onResponse(new Entry(response));
            }
        }, new OrangeListener.Error() {
            @Override
            public void onErrorResponse(OrangeAPIException error) {
                checkSession(error, new OrangeListener.Success<String>() {
                    @Override
                    public void onResponse(String response) {
                        copy(entry, destination, success, failure);
                    }
                }, failure);
            }
        });
    }

    /**
     * Move an Entry (files or folders) in a new folder
     *
     * @param entry   entry to copy
     * @param destination  the folder to copy
     * @param success callback when delete is completed
     * @param failure callback when error occurred
     */
    public void move(final Entry entry,
                     final Entry destination,
                     final OrangeListener.Success<Entry> success,
                     final OrangeListener.Error failure) {


        // Create Tag used to cancel the request
        final String tag = "Cloud/copy/" + entry.name;

        // Prepare params
        final JSONObject params = new JSONObject();
        try {
            params.put("parentFolderId", destination.identifier);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        // Prepare URL
        final String url = API_URL + API_VERSION
                + ((entry.type == Entry.Type.DIRECTORY)
                ? "/folders/"
                : "/files/")
                + entry.identifier;
        session.getRestClient().jsonRequest(tag, Method.POST, url, params, getHeaders(), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                success.onResponse(new Entry(response));
            }
        }, new OrangeListener.Error() {
            @Override
            public void onErrorResponse(OrangeAPIException error) {
                checkSession(error, new OrangeListener.Success<String>() {
                    @Override
                    public void onResponse(String response) {
                        move(entry, destination, success, failure);
                    }
                }, failure);
            }
        });
    }


    /**
     * Delete an Entry (files or folders)
     *
     * @param entry   entry to delete
     * @param success callback when delete is completed
     * @param failure callback when error occurred
     */
    public void delete(final Entry entry,
                       final OrangeListener.Success<String> success,
                       final OrangeListener.Error failure) {

        if (entry == null) {
            throw new IllegalArgumentException("entry must not be null or empty.");
        }

        // Create Tag used to cancel the request
        final String tag = "Cloud/delete/" + entry.identifier;

        // Prepare URL
        final String url = API_URL + API_VERSION
                + ((entry.type == Entry.Type.DIRECTORY)
                ? "/folders/"
                : "/files/")
                + entry.identifier;

        session.getRestClient().stringRequest(tag, Method.DELETE, url, null, getHeaders(),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        success.onResponse(response);
                    }
                }, new OrangeListener.Error() {
                    @Override
                    public void onErrorResponse(OrangeAPIException error) {
                        checkSession(error, new OrangeListener.Success<String>() {
                            @Override
                            public void onResponse(String response) {
                                delete(entry, success, failure);
                            }
                        }, failure);
                    }
                });
    }

    /**
     * Get more information about a file. In particular, the following information is returned:
     * size, creation time, thumbnail and download URL.
     *
     * @param entry   must be a file
     * @param success callback returning the Entry with more information
     * @param failure callback when error occurred
     */
    public void fileInfo(final Entry entry,
                         final OrangeListener.Success<Entry> success,
                         final OrangeListener.Error failure) {

        if (entry == null) {
            throw new IllegalArgumentException("entry must not be null.");
        }

        // Create Tag used to cancel the request
        final String tag = "Cloud/fileInfo/" + entry.identifier;

        // Prepare URL
        final String url = API_URL + API_VERSION + "/files/" + entry.identifier;

        session.getRestClient().jsonRequest(tag, Method.GET, url, null, getHeaders(),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        entry.setExtraInfos(response);
                        success.onResponse(entry);
                    }
                }, new OrangeListener.Error() {
                    @Override
                    public void onErrorResponse(OrangeAPIException error) {
                        checkSession(error, new OrangeListener.Success<String>() {
                            @Override
                            public void onResponse(String response) {
                                fileInfo(entry, success, failure);
                            }
                        }, failure);
                    }
                });
    }

    /**
     * Download a thumbnail from Orange Cloud, copying it to a Bitmap object.
     *
     * @param entry   the entry metadata. Must be a file.
     * @param success callback returning a Bitmap
     * @param failure callback when error occurred
     */
    public void thumbnail(final Entry entry,
                          final OrangeListener.Success<Bitmap> success,
                          final OrangeListener.Error failure) {

        if (entry == null) {
            throw new IllegalArgumentException("entry must not be null.");
        }
        if (entry.thumbnailURL == null) {
            throw new IllegalArgumentException("thumbnailURL must not be null. Make sure that a " +
                    "fileinfo() has been called before.");
        }

        final String tag = "Cloud/thumbnail/" + entry.identifier;
        final String url = entry.thumbnailURL;
        session.getRestClient().imageRequest(tag, url, getHeaders(),
                success,
                new OrangeListener.Error() {
                    @Override
                    public void onErrorResponse(OrangeAPIException error) {
                        checkSession(error, new OrangeListener.Success<String>() {
                            @Override
                            public void onResponse(String response) {
                                thumbnail(entry, success, failure);
                            }
                        }, failure);
                    }
                }, true);
    }

    /**
     * Download a preview from Orange Cloud, copying it to a Bitmap object.
     *
     * @param entry   the entry metadata. Must be a file.
     * @param success callback returning a Bitmap
     * @param failure callback when error occurred
     */
    public void preview(final Entry entry,
                        final OrangeListener.Success<Bitmap> success,
                        final OrangeListener.Error failure) {

        if (entry == null) {
            throw new IllegalArgumentException("entry must not be null.");
        }
        if (entry.previewURL == null) {
            throw new IllegalArgumentException("previewURL must not be null. Make sure that a " +
                    "fileinfo() has been called before.");
        }

        final String tag = "Cloud/preview/" + entry.identifier;
        final String url = entry.previewURL;

        session.getRestClient().imageRequest(tag, url, getHeaders(),
                success,
                new OrangeListener.Error() {
                    @Override
                    public void onErrorResponse(OrangeAPIException error) {
                        checkSession(error, new OrangeListener.Success<String>() {
                            @Override
                            public void onResponse(String response) {
                                preview(entry, success, failure);
                            }
                        }, failure);
                    }
                }, true);
    }

    /**
     * Download image content from Orange Cloud, copying it to a Bitmap object.
     *
     * @param entry   the entry metadata. Must be a file.
     * @param success callback returning a Bitmap
     * @param failure callback when error occurred
     */
    public void imageContent(final Entry entry,
                             final OrangeListener.Success<Bitmap> success,
                             final OrangeListener.Error failure) {

        if (entry == null) {
            throw new IllegalArgumentException("entry must not be null.");
        }
        if (entry.downloadURL == null) {
            throw new IllegalArgumentException("downloadURL must not be null. Make sure that a " +
                    "fileinfo() has been called before.");
        }


        final String tag = "Cloud/content/" + entry.identifier;
        final String url = entry.downloadURL;

        session.getRestClient().imageRequest(tag, url, getHeaders(),
                success,
                new OrangeListener.Error() {
                    @Override
                    public void onErrorResponse(OrangeAPIException error) {
                        checkSession(error, new OrangeListener.Success<String>() {
                            @Override
                            public void onResponse(String response) {
                                imageContent(entry, success, failure);
                            }
                        }, failure);
                    }
                }, false);
    }

    /**
     * Upload a file to the Orange Cloud
     *
     * @param fileUri  Uri of file to upload
     * @param filename name of file to upload
     * @param entry    unique identifier of the parent folder
     * @param success  callback if upload is ok
     * @param progress callback to notify upload progress
     * @param failure  callback to notify error
     */
    public void upload(final Uri fileUri, final String filename, final Entry entry, final OrangeListener.Success<JSONObject> success,
                       final OrangeListener.Progress progress, final OrangeListener.Error failure) {

        URL url;
        try {
            url = new URL(API_CONTENT_URL + API_VERSION + "/files/content?name=" + filename
                    + "&folder=" + entry.identifier);

            session.getRestClient().uploadRequest(url, fileUri, getHeaders(),
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            success.onResponse(response);
                        }
                    }, progress,
                    new OrangeListener.Error() {
                        @Override
                        public void onErrorResponse(OrangeAPIException error) {
                            checkSession(error, new OrangeListener.Success<String>() {
                                @Override
                                public void onResponse(String response) {
                                    upload(fileUri, filename, entry, success, progress, failure);
                                }
                            }, failure);
                        }
                    });
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method is called if an Api call returned an error. It checks if the error is a session
     * problem. If true, method checks if the OAuth access token is still valid (refresh if needed)
     * and re-open a session.
     * Normally it will be simplified and removed in future version.
     *
     * @param error   The error exception returned by an api call.
     * @param success callback when session is re-opened without error
     * @param failure callback when error occurred
     */
    private void checkSession(final OrangeAPIException error,
                              final OrangeListener.Success<String> success,
                              final OrangeListener.Error failure) {

        // Check error code to know if SESSION_EXPIRED
        if (error.getStatusCode() == 401) {
            session.refresh(new OrangeListener.Success<String>() {
                @Override
                public void onResponse(String response) {
                    success.onResponse(null);
                }
            }, failure);
        } else {
            // Normal error
            failure.onErrorResponse(error);
        }
    }

    /**
     * Generate a Map containing the HTTP headers needed to Cloud Api.
     *
     * @return a Map containing minimal headers needed to Cloud Api
     */
    public Map<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", "Bearer " + getSession().getAccessToken());
        Log.v("headers", "Bearer " + getSession().getAccessToken());
        return headers;
    }

    /**
     * A metadata entry that describes a file or folder.
     */
    public static class Entry {

        /**
         * Date format returned by Server. Use to convert creationDate
         */
        private static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZ");
        /**
         * The unique identifier for the file or folder
         */
        public String identifier;

        /**
         * The unique identifier of the parent directory containing the file or folder
         */
        public String parentIdentifier;

        /**
         * The readable name of file or folder
         */
        public String name;

        /**
         * The item type of metadata (IMAGE, FILE, VIDEO, DIRECTORY)
         */
        public Type type;

        /**
         * The size, in bytes, of the file, only available for plain files
         */
        public long bytes;

        /**
         * Human-readable description of the file size.
         */
        public String size;

        /**
         * The creation date of the file
         */
        public Date creationDate;

        /**
         * The URL to download a preview thumbnail, only available for plain files
         */
        public String downloadURL;

        /**
         * The URL to download a graphical representation suitable to be displayed in full screen
         * on a mobile device, only available for some file type (photo, pdf, ...)
         */
        public String previewURL;

        /**
         * The URL to download the file itself, only available for plain files
         */
        public String thumbnailURL;

        /**
         * A flag set to {@code true} when extra information (i.e. bytes, urls, creation time)
         * has been fetched
         */
        public boolean extraInfoAvailable;

        /**
         * A list of immediate children if this is a directory.
         */
        public List<Entry> contents;

        /**
         * A metadata information.
         */
        public Metadata metadata;

        /**
         * Empty constructor
         */
        public Entry() {}

        /**
         * Creates an entry from a json Object, usually received from the list folder or
         * an explicit file info on a file
         *
         * @param jsonObject the jsonObject representation of the JSON received from the
         *                   list folder call, which should look like this:
         */
        public Entry(JSONObject jsonObject) {

            identifier = jsonObject.optString("id");
            name = jsonObject.optString("name");
            parentIdentifier = jsonObject.optString("parentId");

            // Define type of metatdata entry
            String entryType = jsonObject.optString("type");
            if (entryType == null || TextUtils.isEmpty(entryType)) {
                type = Type.DIRECTORY;
            } else if (entryType.equals("FILE")) {
                type = Type.FILE;
            } else if (entryType.equals("PICTURE")) {
                type = Type.IMAGE;
            } else if (entryType.equals("VIDEO")) {
                type = Type.VIDEO;
            } else if (entryType.equals("MUSIC")) {
                type = Type.MUSIC;
            }

            downloadURL = jsonObject.optString("downloadUrl");
            previewURL = jsonObject.optString("previewUrl");
            thumbnailURL = jsonObject.optString("thumbUrl");

            // If it is folder check if directory contains sub folders and files.
            if (this.type == Type.DIRECTORY) {
                contents = new ArrayList<Entry>();

                // Add sub folders
                JSONArray subfolders = jsonObject.optJSONArray("subfolders");
                if (subfolders != null) {
                    for (int i = 0; i < subfolders.length(); i++) {
                        JSONObject obj = subfolders.optJSONObject(i);
                        if (obj != null) {
                            contents.add(new Entry(obj));
                        }
                    }
                }
                // And add files
                JSONArray files = jsonObject.optJSONArray("files");
                if (files != null) {
                    for (int i = 0; i < files.length(); i++) {
                        JSONObject obj = files.optJSONObject(i);
                        if (obj != null) {
                            contents.add(new Entry(obj));
                        }
                    }
                }
            }
            //metadata = new Metadata(jsonObject.optJSONObject("metadata"));
        }

        private static String humanReadableByteCount(long bytes, boolean si) {
            int unit = si ? 1000 : 1024;
            if (bytes < unit) return bytes + " B";
            int exp = (int) (Math.log(bytes) / Math.log(unit));
            String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
            return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
        }

        /**
         * Add extra information getted by file info request.
         *
         * @param info JSONObject returned by file info request
         */
        public void setExtraInfos(JSONObject info) {
            //Log.v("extra Info", info.toString());
            bytes = info.optLong("size");
            size = humanReadableByteCount(bytes, true);

            downloadURL = info.optString("downloadUrl");
            previewURL = info.optString("previewUrl");
            thumbnailURL = info.optString("thumbUrl");

            String date = info.optString("creationDate");
            if (date != "") {
                try {
                    creationDate = formatter.parse(date);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            metadata = new Metadata(info.optJSONObject("metadata"));
            extraInfoAvailable = true;

        }

        /**
         * List of available item types
         */
        public static enum Type {
            IMAGE, FILE, VIDEO, MUSIC, DIRECTORY
        }
    }

    public static class Metadata {

        public String height;
        public String width;
        public Date shootingDate;

        /**
         * Creates metadata entry from a json Object, usually received from the list folder or
         * an explicit file info on a file
         *
         * @param jsonObject the jsonObject representation of the JSON received from the
         *                   list folder call, which should look like this:
         */
        public Metadata(JSONObject jsonObject) {
            this.height = jsonObject.optString("height");
            this.width = jsonObject.optString("width");
            String date = jsonObject.optString("shootingDate");
            if (date != "") {
                try {
                    shootingDate = Entry.formatter.parse(date);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
