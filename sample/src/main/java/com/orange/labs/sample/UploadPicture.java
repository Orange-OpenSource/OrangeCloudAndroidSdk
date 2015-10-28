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
 * Created by Erwan Morvillez on 23/10/14.
 */
package com.orange.labs.sample;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.OpenableColumns;
import android.widget.Toast;

import com.android.volley.Response;
import com.orange.labs.sdk.OrangeCloudAPI;
import com.orange.labs.sdk.OrangeListener;
import com.orange.labs.sdk.exception.OrangeAPIException;

import org.json.JSONObject;

import java.io.File;
import java.util.Map;

public class UploadPicture extends AsyncTask<Void, Integer, Boolean> {

    private OrangeCloudAPI<?> mApi;
    private OrangeCloudAPI.Entry mEntry;
    private Uri mUri;

    private boolean mresult;

    private long mFileLen;
    //    private UploadRequest mRequest;
    private Context mContext;
    private final ProgressDialog mDialog;

    private String mErrorMsg;

    public UploadPicture(Context context, OrangeCloudAPI<?> api, OrangeCloudAPI.Entry entry, Uri uri) {
        mContext = context;
        mApi = api;
        mEntry = entry;
        mUri = uri;

        mDialog = new ProgressDialog(context);
        mDialog.setMax(100);
        mDialog.setMessage(mContext.getResources().getString(R.string.upload_image_title) +
                getFilename());
        mDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mDialog.setProgress(0);
        mDialog.show();
    }

    private ContentResolver getContentResolver(){
        return mContext.getContentResolver();
    }

    @Override
    protected Boolean doInBackground(Void... voids) {

        mApi.upload(mUri, getFilename(), mEntry, new OrangeListener.Success<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                mresult = true;

            }
        }, new OrangeListener.Progress() {
            @Override
            public void onProgress(float ratio) {
                publishProgress(new Integer((int) (ratio * 100)));
            }
        }, new OrangeListener.Error() {
            @Override
            public void onErrorResponse(OrangeAPIException error) {
                mresult = false;
                showToast(error.description);
            }
        });
        return mresult;
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        mDialog.setProgress(progress[0].intValue());
    }

    @Override
    protected void onPostExecute(Boolean result) {
        mDialog.dismiss();
        if (result) {
            showToast(mContext.getResources().getString(R.string.upload_file_success));
            if (mContext != null) {
                ((MainActivity)mContext).refreshData();
            }
        } else {
            showToast(mErrorMsg);
        }
    }

    private void showToast(String msg) {
        Toast error = Toast.makeText(mContext, msg, Toast.LENGTH_LONG);
        error.show();
    }

    /**
     * Try to get the filename of an uri (used to upload)
     *
     * @return the filename or 'default_file' name
     */
    private String getFilename() {
        String result = "default_file";
        if (mUri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(mUri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = mUri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }
}
