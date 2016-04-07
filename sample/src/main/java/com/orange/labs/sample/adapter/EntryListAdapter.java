/*
 * Copyright (c) 2016 Orange.
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
 * Created by Erwan Morvillez on 02/02/16.
 */
package com.orange.labs.sample.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.orange.labs.sample.R;
import com.orange.labs.sdk.OrangeCloudAPI;
import com.orange.labs.sdk.OrangeListener;
import com.orange.labs.sdk.exception.OrangeAPIException;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class EntryListAdapter extends EntryAdapter {

    private SimpleDateFormat mDateFormat;

    private View.OnClickListener mMoreInfoClickListener;

    public EntryListAdapter(Context context) {
        super(context);
        mDateFormat = new SimpleDateFormat("dd MM yyyy HH:mm", Locale.US);
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final EntryListViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_entry_list, parent, false);
            viewHolder = new EntryListViewHolder();
            viewHolder.thumbView = (ImageView) convertView.findViewById(R.id.thumb);
            viewHolder.nameView = (TextView) convertView.findViewById(R.id.name);
            viewHolder.dateView = (TextView) convertView.findViewById(R.id.date);
            viewHolder.extraView = (TextView) convertView.findViewById(R.id.extra);
            viewHolder.moreButton = (ImageButton) convertView.findViewById(R.id.more_button);
            convertView.setTag(viewHolder);
            // Add click listener on More button
            if (mMoreInfoClickListener != null) {
                viewHolder.moreButton.setOnClickListener(mMoreInfoClickListener);
            }
        } else {
            viewHolder = (EntryListViewHolder)convertView.getTag();
        }
        final OrangeCloudAPI.Entry entry = getItem(position);
        if (entry != null) {
            viewHolder.nameView.setText(entry.name);

            if (entry.type != OrangeCloudAPI.Entry.Type.DIRECTORY) {
                viewHolder.dateView.setVisibility(View.VISIBLE);
                viewHolder.extraView.setVisibility(View.VISIBLE);
                // Change default icon:
                switch (entry.type) {
                    case FILE:
                        viewHolder.thumbView.setImageResource(R.mipmap.file);
                        break;
                    case IMAGE:
                        viewHolder.thumbView.setImageResource(R.mipmap.picture);
                        break;
                    case VIDEO:
                        viewHolder.thumbView.setImageResource(R.mipmap.video);
                        break;
                    case MUSIC:
                        viewHolder.thumbView.setImageResource(R.mipmap.music);
                        break;
                }

                if (entry.thumbnailURL != null) {
                    mApi.thumbnail(entry, new OrangeListener.Success<Bitmap>() {
                        @Override
                        public void onResponse(Bitmap response) {
                            viewHolder.thumbView.setImageBitmap(response);
                        }
                    }, new OrangeListener.Error() {
                        @Override
                        public void onErrorResponse(OrangeAPIException error) {
                            // do nothing !
                        }
                    });
                }

                if (entry.extraInfoAvailable == false) {

                    mApi.fileInfo(entry, new OrangeListener.Success<OrangeCloudAPI.Entry>() {
                        @Override
                        public void onResponse(OrangeCloudAPI.Entry entry) {
                            if (entry.equals(entry)) {
                                setExtraInfo(viewHolder, entry);
                            }
                        }
                    }, new OrangeListener.Error() {
                        @Override
                        public void onErrorResponse(OrangeAPIException error) {

                        }
                    });
                } else {
                    setExtraInfo(viewHolder, entry);
                }
            } else {
                viewHolder.thumbView.setImageResource(R.drawable.ic_folder);
                viewHolder.dateView.setVisibility(View.GONE);
                viewHolder.extraView.setVisibility(View.GONE);
            }
        }
        return convertView;
    }

    private void setExtraInfo(EntryListViewHolder view, final OrangeCloudAPI.Entry entry) {

        // Add Date
        view.dateView.setText(mDateFormat.format(entry.creationDate));

        // Add size
        view.extraView.setText(entry.size);

    }

    public void setMoreInfoClickListener(View.OnClickListener mMoreInfoClickListener) {
        this.mMoreInfoClickListener = mMoreInfoClickListener;
    }

    private static class EntryListViewHolder {
        ImageView thumbView;
        TextView nameView;
        TextView dateView;
        TextView extraView;
        ImageButton moreButton;
    }

}
