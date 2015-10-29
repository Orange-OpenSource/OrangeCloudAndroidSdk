package com.orange.labs.sample;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.orange.labs.sample.cache.LruBitmapCache;
import com.orange.labs.sample.entry.EntryAdapter;
import com.orange.labs.sdk.OrangeCloudAPI;
import com.orange.labs.sdk.OrangeListener;
import com.orange.labs.sdk.exception.OrangeAPIException;
import com.orange.labs.sdk.session.AuthSession;

import org.json.JSONObject;

import java.util.Stack;


public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    ///////////////////////////////////////////////////////////////////////////
    //                      Your app-specific settings.                      //
    ///////////////////////////////////////////////////////////////////////////

    // Replace this with your app key and secret assigned by Orange and
    // the redirect Uri.
    // Note: Obfuscation is good.
    final static private String APP_KEY = "your client app key";
    final static private String APP_SECRET = "your client app secret";
    final static private String APP_REDIRECT_URI = "your client redirect uri";

    ///////////////////////////////////////////////////////////////////////////
    //                      End app-specific settings.                       //
    ///////////////////////////////////////////////////////////////////////////

    private OrangeCloudAPI<AuthSession> mApi;

    // Use to keep cache
    private Stack<OrangeCloudAPI.Entry> mStack;

    // UI:
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private ProgressBar progressBar;
    private ListView list;
    private TextView emptyText;
    private EntryAdapter adapter;

    // Stored in savedInstanceState to track an ongoing auth attempt, which
    // must include a locally-generated nonce in the response.
    private String mainStateNonce = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.v("MainActivity", "CLOUD SDK VERSION = " + OrangeCloudAPI.SDK_VERSION);

        if (savedInstanceState == null) {
            mStack = new Stack<OrangeCloudAPI.Entry>();

            setContentView(R.layout.activity_main);

            mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
            mSwipeRefreshLayout.setOnRefreshListener(this);

            progressBar = (ProgressBar) findViewById(R.id.progress_bar);
            emptyText = (TextView) findViewById(R.id.empty_text);

            list = (ListView) findViewById(R.id.list);
            list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                    // Get element at position and browse if it is a folder
                    OrangeCloudAPI.Entry entry = (OrangeCloudAPI.Entry) adapter.getItem(i);
                    if (entry.type == OrangeCloudAPI.Entry.Type.DIRECTORY) {
                        browseFolders(entry);
                    } else if (entry.type == OrangeCloudAPI.Entry.Type.IMAGE) {
                        previewImage(entry);
                    }
                }
            });

            // Add ContextMenu on List (LongPress)
            registerForContextMenu(list);

            ///////////////////////////////////////////////////////////////////////////
            //
            // We create a new AuthSession so that we can use the Orange Cloud API.
            //
            ///////////////////////////////////////////////////////////////////////////
            AuthSession session = new AuthSession(MainActivity.this, APP_KEY, APP_SECRET, APP_REDIRECT_URI);

            mApi = new OrangeCloudAPI<AuthSession>(session);

            // An you can set a Image cache policy
            mApi.setImageCache(new LruBitmapCache());

            // Add scope for example cloudfullread
            // (see https://www.orangepartner.com/content/api-reference-cloud)
            //mApi.addScope("cloudfullread");

            // Start session
            session.startAuthentication();
        } else {
            mainStateNonce = savedInstanceState.getString("mainStateNonce");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        ///////////////////////////////////////////////////////////////////////////
        // The next part must be inserted in the onResume() method of the
        // activity from which session.startAuthentication() was called, so
        // that Orange authentication completes properly.
        ///////////////////////////////////////////////////////////////////////////
        final AuthSession session = mApi.getSession();

        // need to be sure that is first loading!
        if (mainStateNonce == null) {
            session.checkAuthentication(new OrangeListener.Success<String>() {
                @Override
                public void onResponse(String response) {
                    // Have a valid session:
                    // Browse the root folder and display its contents
                    // null -> because we need root folder of app!
                    browseFolders(null);
                    mainStateNonce = "mainStateNonce";

                }
            }, new OrangeListener.Error() {
                @Override
                public void onErrorResponse(OrangeAPIException error) {
                    failure(error, true);
                }
            });
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("mainStateNonce", mainStateNonce);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {

        if (resultCode == RESULT_OK) {
            if (requestCode == 1) {
                Uri uri = intent.getData();

                UploadPicture upload = new UploadPicture(MainActivity.this, mApi, mStack.lastElement(), uri);
                upload.execute();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_logout) {
            logout();
            return true;
        } else if (id == R.id.menu_add_folder) {
            createFolder();
            return true;
        } else if (id == R.id.menu_add_file) {
            uploadFile();
            return true;
        }else if (id == R.id.menu_freespace) {
            freespace();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        getMenuInflater().inflate(R.menu.context, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int id = item.getItemId();
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        OrangeCloudAPI.Entry entry = (OrangeCloudAPI.Entry) adapter.getItem(info.position);
        if (id == R.id.action_delete) {
            delete(entry);
            return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mStack.size() == 1) {
            super.onBackPressed();
        } else {
            // Pop stack
            mStack.pop();
            OrangeCloudAPI.Entry item = mStack.pop();
            browseFolders(item);
        }
    }

    @Override
    public void onRefresh() {
        refreshData();
    }

    /**
     * Method called when an API error occurred
     *
     * @param error               an OrangeAPIException
     * @param forceAuthentication if true, restart authentication
     */
    public void failure(OrangeAPIException error, final boolean forceAuthentication) {
        // Display popup alert and retry connect !
        progressBar.setVisibility(View.GONE);
        mSwipeRefreshLayout.setRefreshing(false);

        AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
        alert.setTitle(R.string.error);
        alert.setMessage(error.description);
        alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                if (forceAuthentication) {
                    // For sample retry authentication if error
                    mApi.getSession().startAuthentication();
                }
            }
        });
        alert.show();
    }

    /**
     * Method to browse folders
     *
     * @param entry an entry Object or null
     */
    public void browseFolders(OrangeCloudAPI.Entry entry) {

        // Display loader and make Cloud API request
        progressBar.setVisibility(View.VISIBLE);
        mApi.listFolder(entry, new OrangeListener.Success<OrangeCloudAPI.Entry>() {
            @Override
            public void onResponse(OrangeCloudAPI.Entry response) {
                // Add new result in stack and display data in the listView
                mSwipeRefreshLayout.setRefreshing(false);
                mStack.push(response);
                displayData();
            }
        }, new OrangeListener.Error() {
            @Override
            public void onErrorResponse(OrangeAPIException error) {
                failure(error, false);
            }
        });
    }

    /**
     * Delete current folder and associated files.
     */
    public void delete(final OrangeCloudAPI.Entry entry) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(R.string.alert_delete_message)
                .setTitle(R.string.alert_delete_title);

        builder.setPositiveButton(R.string.alert_delete_button_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
                mApi.delete(entry, new OrangeListener.Success<String>() {
                    @Override
                    public void onResponse(String response) {
                        if (mStack.size() > 0) {
                            browseFolders(mStack.lastElement());
                        } else {
                            browseFolders(null);
                        }
                    }
                }, new OrangeListener.Error() {
                    @Override
                    public void onErrorResponse(OrangeAPIException error) {
                        failure(error, false);
                    }
                });
            }
        });
        builder.setNegativeButton(R.string.alert_delete_button_cancel, null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Create folder inside current folder
     */
    public void createFolder() {
        AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);

        alert.setTitle(R.string.action_add_folder);
        final EditText input = new EditText(MainActivity.this);
        alert.setView(input);

        alert.setPositiveButton(R.string.alert_add_folder_button_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                Editable value = input.getText();
                String folderName = value.toString();
                if (!TextUtils.isEmpty(folderName)) {
                    progressBar.setVisibility(View.VISIBLE);
                    mApi.createFolder(mStack.lastElement(), folderName, new OrangeListener.Success<OrangeCloudAPI.Entry>() {
                        @Override
                        public void onResponse(OrangeCloudAPI.Entry response) {
                            browseFolders(mStack.lastElement());
                        }
                    }, new OrangeListener.Error() {
                        @Override
                        public void onErrorResponse(OrangeAPIException error) {
                            failure(error, false);
                        }
                    });
                }
            }
        });
        alert.setNegativeButton(R.string.alert_add_folder_button_cancel, null);
        alert.show();
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
        progressBar.setVisibility(View.VISIBLE);
        mApi.imageContent(entry, new OrangeListener.Success<Bitmap>() {
            @Override
            public void onResponse(Bitmap response) {
                progressBar.setVisibility(View.GONE);
                ImageView imageView = new ImageView(MainActivity.this);
                imageView.setImageBitmap(response);
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(entry.name)
                        .setView(imageView)
                        .setPositiveButton(R.string.ok, null)
                        .show();
            }
        }, new OrangeListener.Error() {
            @Override
            public void onErrorResponse(OrangeAPIException error) {
                failure(error, false);
            }
        });

    }

    /**
     * Display the last element of stack in listView
     */
    private void displayData() {
        OrangeCloudAPI.Entry entry = mStack.lastElement();
        if (entry != null) {
            progressBar.setVisibility(View.GONE);
            if (entry.type == OrangeCloudAPI.Entry.Type.DIRECTORY) {
                setTitle(entry.name);

                // Load in adapter the List of subfolders and files
                adapter = new EntryAdapter(MainActivity.this, entry.contents);
                adapter.setCloudApi(mApi);

                list.setAdapter(adapter);
                adapter.notifyDataSetChanged();

                if (adapter.isEmpty()) {
                    emptyText.setVisibility(View.VISIBLE);
                } else {
                    emptyText.setVisibility(View.GONE);
                }
            }
        } else {
            emptyText.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Refresh the data of folder displayed
     */
    public void refreshData() {
        OrangeCloudAPI.Entry item = null;
        if (mStack != null && mStack.size() > 0) {
            item = mStack.pop();
        }
        browseFolders(item);
    }

    /**
     * Clean Data (stack) and refresh layout (listView)
     */
    private void cleanData() {
        mStack = new Stack<OrangeCloudAPI.Entry>();
        list.setAdapter(null);
        progressBar.setVisibility(View.VISIBLE);
        emptyText.setVisibility(View.GONE);
    }

    /**
     * Called when users clicks on logout Menu
     * Logout user and display authentication Activity.
     */
    public void logout() {
        // Clean session
        AuthSession session = mApi.getSession();
        // To force reload of ListFragment
        cleanData();
        mainStateNonce = null;
        mApi.unlink();
        session.startAuthentication();
    }

    /**
     * Method to display freespace
     */
    public void freespace() {
        mApi.freespace(new OrangeListener.Success<Long>() {
            @Override
            public void onResponse(Long response) {
                String freespace = humanReadableByteCount(response, false);
                String msg = getApplicationContext().getString(R.string.available_freespace) + freespace;
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
            }
        }, new OrangeListener.Error() {
            @Override
            public void onErrorResponse(OrangeAPIException error) {
                failure(error, false);
            }
        });
    }

    private  String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }
}
