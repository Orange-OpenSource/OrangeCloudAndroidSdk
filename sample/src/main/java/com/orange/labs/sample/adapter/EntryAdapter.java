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
package com.orange.labs.sample.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.orange.labs.sdk.OrangeCloudAPI;
import com.orange.labs.sdk.session.AuthSession;

import java.util.ArrayList;
import java.util.List;

public class EntryAdapter extends BaseAdapter {

    Context context;

    List<OrangeCloudAPI.Entry> mEntries = new ArrayList<>();

    protected OrangeCloudAPI<AuthSession> mApi;

    public EntryAdapter(Context context) {
        this.context = context;
        notifyDataSetChanged();
    }

    public List<OrangeCloudAPI.Entry> getEntries() {
        return mEntries;
    }

    public void setEntries(List<OrangeCloudAPI.Entry> entries) {
        if (mEntries != null) {
            mEntries.clear();
        }
        addEntries(entries);
    }

    public void addEntries(List<OrangeCloudAPI.Entry> entries) {
        mEntries.addAll(entries);
        notifyDataSetChanged();
    }



    public void setCloudApi(OrangeCloudAPI<AuthSession> api) {
        mApi = api;
    }

    @Override
    public int getCount() {
        return mEntries.size();
    }

    @Override
    public OrangeCloudAPI.Entry getItem(int position) {
        return mEntries.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mEntries.indexOf(getItem(position));
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        return convertView;
    }
}
