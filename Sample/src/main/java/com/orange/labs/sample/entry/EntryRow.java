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
package com.orange.labs.sample.entry;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.orange.labs.sample.R;
import com.orange.labs.sdk.OrangeCloudAPI;
import com.orange.labs.sdk.OrangeListener;
import com.orange.labs.sdk.exception.OrangeAPIException;
import com.orange.labs.sdk.session.AuthSession;

import java.text.Format;
import java.text.SimpleDateFormat;

//import org.apache.http.impl.cookie.DateUtils;

public class EntryRow extends RelativeLayout {

    private OrangeCloudAPI<AuthSession> mApi;

    private final static String PATTERN_ASCTIME = "EEE MMM d HH:mm:ss yyyy";

    // UI
    private TextView mTitleView;
    private TextView mDateView;
    private TextView mExtraView;
    private ImageView mImageView;

    // Data
    private OrangeCloudAPI.Entry mEntry;

    public EntryRow(Context context) {
        super(context);
    }

    public EntryRow(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EntryRow(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setCloudApi(OrangeCloudAPI<AuthSession> api) {
        mApi = api;
    }

    @Override
    protected void onFinishInflate() {
        mTitleView = (TextView) findViewById(R.id.title_row);
        mDateView = (TextView) findViewById(R.id.date_row);
        mExtraView = (TextView) findViewById(R.id.extra_row);
        mImageView = (ImageView) findViewById(R.id.icon_row);
    }

    public void setCloudItem(OrangeCloudAPI.Entry entry) {
        mEntry = entry;
        mTitleView.setText(entry.name);
        mDateView.setText("...");
        mExtraView.setText("...");

        if (entry.type != OrangeCloudAPI.Entry.Type.DIRECTORY) {
            mDateView.setVisibility(View.VISIBLE);
            mExtraView.setVisibility(View.VISIBLE);
            // Change default icon:
            switch (entry.type) {
                case FILE:
                    mImageView.setImageResource(R.drawable.ic_file);
                    break;
                case IMAGE:
                    mImageView.setImageResource(R.drawable.ic_image);
                    break;
                case VIDEO:
                    mImageView.setImageResource(R.drawable.ic_video);
                    break;
            }

            if (entry.extraInfoAvailable == false) {

                mApi.fileInfo(entry, new OrangeListener.Success<OrangeCloudAPI.Entry>() {
                    @Override
                    public void onResponse(OrangeCloudAPI.Entry entry) {
                        if (entry.equals(mEntry)) {
                            setExtraInfo(entry);
                        }
                    }
                }, new OrangeListener.Error() {
                    @Override
                    public void onErrorResponse(OrangeAPIException error) {

                    }
                });
            } else {
                setExtraInfo(entry);
            }
        } else {
            mImageView.setImageResource(R.drawable.ic_folder);
            mDateView.setVisibility(View.GONE);
            mExtraView.setVisibility(View.GONE);
        }
    }

    public void setExtraInfo(final OrangeCloudAPI.Entry entry) {

        // Add Date
        Format formatter = new SimpleDateFormat(PATTERN_ASCTIME);
        String creationDate = formatter.format(entry.creationDate);
        mDateView.setText(creationDate);

        // Add size
        mExtraView.setText(entry.size);

        // ask thumbnail
        mApi.thumbnail(entry, new OrangeListener.Success<Bitmap>() {
            @Override
            public void onResponse(Bitmap response) {
                if (mEntry.equals(entry)) {
                    mImageView.setImageBitmap(response);
                }
            }
        }, new OrangeListener.Error() {
            @Override
            public void onErrorResponse(OrangeAPIException error) {
                // do nothing !
            }
        });

    }
}
