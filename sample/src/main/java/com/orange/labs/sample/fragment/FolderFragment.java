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
package com.orange.labs.sample.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.orange.labs.sample.CloudContextMenu;
import com.orange.labs.sample.MainActivity;
import com.orange.labs.sample.R;
import com.orange.labs.sample.adapter.EntryListAdapter;
import com.orange.labs.sdk.OrangeCloudAPI;
import com.orange.labs.sdk.OrangeListener;
import com.orange.labs.sdk.exception.OrangeAPIException;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class FolderFragment extends Fragment
        implements SwipeRefreshLayout.OnRefreshListener,
        OrangeListener.Error {

    private static List<String> breadcrumbs = new ArrayList<>();

    public enum ActionType {
        NONE, COPY, MOVE
    }
    private static ActionType action = ActionType.NONE;
    private static OrangeCloudAPI.Entry copiedEntry;

    private LinearLayout mPasteViewContainer;
    private Button mPasteButton;
    private Button mPasteCancelButton;

    private OrangeCloudAPI mApi;

    private boolean isAlreadyLoaded = false;

    private ListView mListView;
    private EntryListAdapter mAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private ProgressBar mProgressBar;

    private OnFolderFragmentInteractionListener mListener;

    private OrangeCloudAPI.Entry mEntry;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_folder, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mApi = ((MainActivity)getActivity()).getOrangeCloudApi();

        mListView = (ListView) view.findViewById(R.id.list_view);
        registerForContextMenu(mListView);

        TextView emptyText = (TextView) view.findViewById(R.id.empty_text);
        mListView.setEmptyView(emptyText);

        mAdapter = new EntryListAdapter(getContext());
        mAdapter.setCloudApi(mApi);
        mAdapter.setMoreInfoClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //getActivity().openContextMenu(v);

                int position = mListView.getPositionForView(v);
                OrangeCloudAPI.Entry entry = mAdapter.getItem(position);

                if (entry != null) {
                    CloudContextMenu contextMenu = new CloudContextMenu(getContext(), entry, mApi);
                    contextMenu.show();
                }
            }
        });

        mListView.setAdapter(mAdapter);

        mSwipeRefreshLayout = (SwipeRefreshLayout) getView().findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setOnRefreshListener(this);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.v("VIEW", view.toString());
                if (mListener != null) {
                    // Get element at position and browse if it is a folder
                    OrangeCloudAPI.Entry entry = mAdapter.getItem(position);
                    if (entry.type == OrangeCloudAPI.Entry.Type.DIRECTORY) {
                        mListener.onSelectFolder(entry.identifier, entry.name);
                    } else if (entry.type == OrangeCloudAPI.Entry.Type.IMAGE) {
                        previewImage(entry);
                    }
                }
            }
        });

        mProgressBar = (ProgressBar) view.findViewById(R.id.progress_bar);

        mPasteViewContainer = (LinearLayout) view.findViewById(R.id.ribbon_view);
        if (action == ActionType.NONE) {
            mPasteViewContainer.setVisibility(View.GONE);
        } else {
            mPasteViewContainer.setVisibility(View.VISIBLE);
        }

        mPasteButton = (Button) view.findViewById(R.id.paste);
        mPasteCancelButton = (Button) view.findViewById(R.id.cancel);

        mPasteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*if (copiedEntry.parentIdentifier.equals(mEntry.identifier)) {
                    OrangeAPIException exception = new OrangeAPIException(0, "", "FORBIDDEN_COPY_MOVE", "");
                    onErrorResponse(exception);
                } else {*/
                if (action == ActionType.COPY) {
                    copy();
                } else if (action == ActionType.MOVE) {
                        move();
                    }
                /*}*/
            }
        });

        mPasteCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                action = ActionType.NONE;
                copiedEntry = null;
                mPasteViewContainer.setVisibility(View.GONE);
            }
        });

        refreshData();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (OnFolderFragmentInteractionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnFolderFragmentInteractionListener");
        }
    }

    @Override
    public void onDestroy() {
        if (breadcrumbs.size() > 0) {
            breadcrumbs.remove(breadcrumbs.size()-1);
            setSubtitle();
        }
        super.onDestroy();
    }

    @Override
    public void onRefresh() {
        refreshData();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_main_folder, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_add_folder) {
            createFolder();
            return true;
        } else if (id == R.id.action_upload) {
            uploadFile();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        getActivity().getMenuInflater().inflate(R.menu.menu_context_folder, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        OrangeCloudAPI.Entry entry = mAdapter.getItem(info.position);
        boolean done = true;
        switch (item.getItemId()) {
            case R.id.action_rename:
                rename(entry);
                break;
            case R.id.action_delete:
                delete(entry);
                break;
            case R.id.action_copy:
                action = ActionType.COPY;
                copiedEntry = entry;
                mPasteViewContainer.setVisibility(View.VISIBLE);
                break;
            case R.id.action_move:
                action = ActionType.MOVE;
                copiedEntry = entry;
                mPasteViewContainer.setVisibility(View.VISIBLE);
                break;
            default:
                done = super.onContextItemSelected(item);
                break;
        }
        return done;
    }

    /**
     * Refresh the data of folder displayed
     */
    public void refreshData() {
        Bundle bundle = getArguments();
        if (bundle != null) {
            String identifier = bundle.getString("FOLDER_ID");
            OrangeCloudAPI.Entry entry = new OrangeCloudAPI.Entry();
            entry.identifier = identifier;
            browseFolders(entry);
        } else {
            browseFolders(null);
        }
    }

    /**
     * Method to browse folders
     *
     * @param entry an entry Object or null
     */
    private void browseFolders(OrangeCloudAPI.Entry entry) {
        mProgressBar.setVisibility(View.VISIBLE);
        final JSONObject params = new JSONObject();
        try {
            params.put("showthumbnails", "nocall");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mApi.listEntries(entry, params, new OrangeListener.Success<OrangeCloudAPI.Entry>() {
            @Override
            public void onResponse(OrangeCloudAPI.Entry entry) {
                mEntry = entry;
                // Add new result in stack and display data in the listView
                mSwipeRefreshLayout.setRefreshing(false);
                mProgressBar.setVisibility(View.GONE);
                mAdapter.setEntries(entry.contents);

                if (!isAlreadyLoaded) {
                    breadcrumbs.add(entry.name);
                    setSubtitle();
                    isAlreadyLoaded = true;
                }
            }
        }, this);
    }

    /**
     * Create folder inside current folder
     */
    public void createFolder() {
        AlertDialog.Builder alert = new AlertDialog.Builder(getContext());

        alert.setTitle(R.string.action_add_folder);
        final EditText input = new EditText(getContext());
        alert.setView(input);

        alert.setPositiveButton(R.string.alert_add_folder_button_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                Editable value = input.getText();
                String folderName = value.toString();
                if (!TextUtils.isEmpty(folderName)) {
                    mProgressBar.setVisibility(View.VISIBLE);
                    mApi.createFolder(mEntry, folderName, new OrangeListener.Success<OrangeCloudAPI.Entry>() {
                        @Override
                        public void onResponse(OrangeCloudAPI.Entry response) {
                            mPasteViewContainer.setVisibility(View.GONE);
                            browseFolders(mEntry);
                        }
                    }, FolderFragment.this);
                }
            }
        });
        alert.setNegativeButton(R.string.alert_add_folder_button_cancel, null);
        alert.show();
    }

    /**
     * Copy a folder of file in another folder
     */
    private void copy() {
        mApi.copy(copiedEntry, mEntry, new OrangeListener.Success<OrangeCloudAPI.Entry>() {
            @Override
            public void onResponse(OrangeCloudAPI.Entry response) {
                mPasteViewContainer.setVisibility(View.GONE);
                browseFolders(mEntry);
            }
        }, this);
    }

    /**
     * Move a folder of file in another folder
     */
    private void move() {
        mApi.move(copiedEntry, mEntry, new OrangeListener.Success<OrangeCloudAPI.Entry>() {
            @Override
            public void onResponse(OrangeCloudAPI.Entry response) {
                mPasteViewContainer.setVisibility(View.GONE);
                browseFolders(mEntry);
            }
        }, this);
    }

    /**
     * Rename folder or file
     */
    public void rename(final OrangeCloudAPI.Entry entry) {
        AlertDialog.Builder alert = new AlertDialog.Builder(getContext());

        alert.setTitle(R.string.action_rename);
        final EditText input = new EditText(getContext());
        alert.setView(input);

        alert.setPositiveButton(R.string.alert_rename_folder_button_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                Editable value = input.getText();
                String name = value.toString();
                if (!TextUtils.isEmpty(name)) {
                    mProgressBar.setVisibility(View.VISIBLE);
                    mApi.rename(entry, name, new OrangeListener.Success<OrangeCloudAPI.Entry>() {
                        @Override
                        public void onResponse(OrangeCloudAPI.Entry response) {
                            browseFolders(mEntry);
                        }
                    }, FolderFragment.this);
                }
            }
        });
        alert.setNegativeButton(R.string.alert_rename_folder_button_cancel, null);
        alert.show();
    }

    /**
     * Delete current folder and associated files.
     */
    public void delete(final OrangeCloudAPI.Entry entry) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMessage(R.string.alert_delete_message)
                .setTitle(R.string.alert_delete_title);

        builder.setPositiveButton(R.string.alert_delete_button_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
                mProgressBar.setVisibility(View.VISIBLE);
                mApi.delete(entry, new OrangeListener.Success<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Need to refresh
                        browseFolders(mEntry);
                    }
                }, FolderFragment.this);
            }
        });
        builder.setNegativeButton(R.string.alert_delete_button_cancel, null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Display Activity to choose image content.
     */
    public void uploadFile() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, getResources().getString(R.string.upload_file_title)), 1);
    }

    /**
     * Download and display image
     *
     * @param entry the image file to download
     */
    public void previewImage(final OrangeCloudAPI.Entry entry) {
        mProgressBar.setVisibility(View.VISIBLE);
        mApi.imageContent(entry, new OrangeListener.Success<Bitmap>() {
            @Override
            public void onResponse(Bitmap response) {
                mProgressBar.setVisibility(View.GONE);
                ImageView imageView = new ImageView(getActivity());
                imageView.setImageBitmap(response);
                new AlertDialog.Builder(getContext())
                        .setTitle(entry.name)
                        .setView(imageView)
                        .setPositiveButton(R.string.ok, null)
                        .show();
            }
        }, this);
    }

    /**
     * Method called when an error occured on Orange Cloud APi
     * @param error the Orange Api Cloud error
     */
    @Override
    public void onErrorResponse(OrangeAPIException error) {
        error.printStackTrace();
        mSwipeRefreshLayout.setRefreshing(false);
        mProgressBar.setVisibility(View.GONE);
        MainActivity activity = (MainActivity)getActivity();
        if (activity != null) {
            activity.failure(error, false);
        }
    }

    /**
     * set subtitle of toolbar function of current path
     */
    private void setSubtitle() {
        String path = "/" + TextUtils.join("/", breadcrumbs);
        if (getActivity() != null) {
            ((MainActivity) getActivity()).getSupportActionBar().setSubtitle(path);
        }
    }

    public OrangeCloudAPI.Entry getEntry() {
        return mEntry;
    }

    /**
     * Interface method called when user clicks on a item of listView
     * Implemented by MainActivity
     */
    public interface OnFolderFragmentInteractionListener {
        void onSelectFolder(String identifier, String name);
    }
}
