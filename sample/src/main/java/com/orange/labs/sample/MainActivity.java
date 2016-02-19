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
package com.orange.labs.sample;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.Toast;

import com.orange.labs.sample.cache.LruBitmapCache;
import com.orange.labs.sample.fragment.FolderFragment;
import com.orange.labs.sdk.OrangeCloudAPI;
import com.orange.labs.sdk.OrangeListener;
import com.orange.labs.sdk.exception.OrangeAPIException;
import com.orange.labs.sdk.session.AuthSession;

public class MainActivity extends AppCompatActivity
        implements
            NavigationView.OnNavigationItemSelectedListener,
            FolderFragment.OnFolderFragmentInteractionListener {

    ///////////////////////////////////////////////////////////////////////////
    //                      Your app-specific settings.                      //
    ///////////////////////////////////////////////////////////////////////////
    //
    // Replace this with your app key and secret assigned by Orange and
    // the redirect Uri.
    // Note: Obfuscation is good.
    //
    // https://developer.orange.com/apis/cloud-france/api-reference
    //
    final static private String APP_KEY = "oW5zJ83QPJGGfOGGMmz35O8sqjeAaXBt";
    final static private String APP_SECRET = "bjoOI4NmTJLo9Qsw";
    final static private String APP_REDIRECT_URI = "filepicker://callback";

    ///////////////////////////////////////////////////////////////////////////
    //                      End app-specific settings.                       //
    ///////////////////////////////////////////////////////////////////////////

    /** Instance to access to the cloud API */
    private OrangeCloudAPI<AuthSession> mApi;

    /** Stored in savedInstanceState to track an ongoing auth attempt, which
     *  must include a locally-generated nonce in the response.
     */
    private String mainStateNonce = null;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            setContentView(R.layout.activity_main);
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);

            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawer.setDrawerListener(toggle);
            toggle.syncState();

            NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
            navigationView.setNavigationItemSelectedListener(this);

            ///////////////////////////////////////////////////////////////////////////
            //
            // We create a new AuthSession so that we can use the Orange Cloud API.
            //
            ///////////////////////////////////////////////////////////////////////////
            AuthSession session = new AuthSession(MainActivity.this, APP_KEY, APP_SECRET, APP_REDIRECT_URI);

            mApi = new OrangeCloudAPI<>(session);

            // An you can set a Image cache policy
            mApi.setImageCache(new LruBitmapCache());

            // Add scope for example cloudfullread
            // (see https://developer.orange.com/apis/cloud-france/api-reference)
            // mApi.addScope("cloudfullread");

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
                    mainStateNonce = "mainStateNonce";
                    // Load default fragment
                    loadFragment(new FolderFragment(), false);

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
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.v("onActivityResult", resultCode + " - " + requestCode);
        // Called when picture has been selected
        if (resultCode == RESULT_OK) {
            //if (requestCode == 1) {
                Uri uri = intent.getData();
                // Check that currentFragment is a FolderFragment and get the current folder to upload on it.
                Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                if (fragment instanceof FolderFragment) {
                    UploadPicture upload = new UploadPicture(this, mApi, ((FolderFragment) fragment).getEntry(), uri);
                    upload.execute();
                }
            //}
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("mainStateNonce", mainStateNonce);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_folders) {
            // Load root
            getSupportActionBar().setTitle(R.string.nav_folders);
            onSelectFolder(null, "");
        } else if (id == R.id.nav_freespace) {
            freespace();
        } else if (id == R.id.nav_logout) {
            // Logout user and display authentication Activity.
            logout();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Load a fragment in MainActivity
     * @param fragment  fragment to be loaded
     * @param addToBackStack true if it will be added in backstack
     */
    private void loadFragment(Fragment fragment, boolean addToBackStack) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        if (addToBackStack) {
            fragmentTransaction.addToBackStack(null);
        }
        fragmentTransaction.commit();
    }

    public OrangeCloudAPI<AuthSession> getOrangeCloudApi() {
        return mApi;
    }

    /**
     * Load a new folder fragment by passing identifier of folder to load.
     * @param identifier the unique id of folder
     */
    @Override
    public void onSelectFolder(String identifier, String name) {
        FolderFragment fragment = new FolderFragment();
        if (identifier != null) {
            Bundle bundle = new Bundle();
            bundle.putString("FOLDER_ID", identifier);
            fragment.setArguments(bundle);
        }

        loadFragment(fragment, (identifier != null));
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


    public void refreshData() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment instanceof FolderFragment) {
            ((FolderFragment) fragment).refreshData();
        }
    }


    /**
     * Called when users clicks on logout Menu
     * Logout user and display authentication Activity.
     */
    public void logout() {
        // Clean session
        AuthSession session = mApi.getSession();
        // To force reload of ListFragment
        //cleanData();
        mainStateNonce = null;
        mApi.unlink();
        session.startAuthentication();
    }

    private String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    /**
     * Method called when an API error occurred
     *
     * @param error               an OrangeAPIException
     * @param forceAuthentication if true, restart authentication
     */
    public void failure(OrangeAPIException error, final boolean forceAuthentication) {
        error.printStackTrace();

        // Display popup alert and retry connect !
        AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
        alert.setTitle(R.string.error);
        String message = error.getMessage();
        int id = getResources().getIdentifier(message, "string", getPackageName());
        if (id != 0) {
            message = getString(id);
        }

        alert.setMessage(message);
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
}