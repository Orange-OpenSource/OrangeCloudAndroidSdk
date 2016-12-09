Orange Cloud SDK for Android
============================

Orange API : Register your app
------------------------------
At this time, the downloadable projects are designed for use with Gradle and Android Studio. If you haven't already, first follow the steps at [Google's Android Studio installation guide](http://developer.android.com/sdk/installing/index.html?pkg=studio).

First,  you have to **register your app** on the [Orange developer portal](http://api.orange.com) in order to get needed informations to identify your app (App Key, app Secret, redirect uri...).

Adding SDK to existing projects with JitPack.io
-----------------------------------------------

We use JitPack.io to deliver an Android library for [Orange Cloud Sdk Android](https://jitpack.io/#Orange-OpenSource/OrangeCloudAndroidSdk/1.0.6)

Add it to your build.gradle with:

```gradle
repositories {   
    jcenter()
    maven { url "https://jitpack.io" }
}
```
and:

```gradle
dependencies {
    // Orange Cloud Android Sdk
    compile 'com.github.Orange-OpenSource:OrangeCloudAndroidSdk:1.0.7'
}
```

Sample app 
----------------
Sample app is a very basic Android app that authenticates Orange user and then offers basic actions (browse, delete, create, rename, copy, move folder or files and upload files). 
You can import sample project into **Android Studio** by clicking in **File > Import Projects...** and select **sample** directory.

You'll need to edit the code to enter your app key, your app secret and redirect uri where indicated in the MainActivity.java file.

```Java
final static private String APP_KEY = "your client app key";
final static private String APP_SECRET = "your client app secret";
final static private String APP_REDIRECT_URI = "your client redirect uri";
```



Authenticating your app
----------------------------------
You need to enter the following snippet in your **AndroidManifest.xml** in order to authenticate the Orange user. Insert the following code under the `<application>` section.

```XML
<activity
	android:name="com.orange.labs.sdk.activity.AuthActivity"
	android:launchMode="singleTask"
	android:configChanges="orientation|keyboard"/>
```
Make sure that your app has the internet permission. Insert the following code under the `<manifest>` section
```XML
<uses-permission android:name="android.permission.INTERNET"></uses-permission>
```

In your `Activity.java`, you need to define contants get on [Orange developer portal](https://developer.orange.com)
```Java
final static private String APP_KEY = "your client app key";
final static private String APP_SECRET = "your client app secret";
final static private String APP_REDIRECT_URI = "your client redirect uri";
```
Pass all three values to the new OrangeCloudAPI object.
```Java
// In the class declaration section:
public OrangeCloudAPI<AuthSession> mApi;

// and for example, in onCreate function:
AuthSession session = new AuthSession(Activity.this, APP_KEY, APP_SECRET, APP_REDIRECT_URI);
mApi = new OrangeCloudAPI<AuthSession>(session);
```
Now,  you can start authentication process. If it is the first connection for Orange user, method opens the **AuthActivity** declared in **AndroidManifest.xml** in order to authenticate user and authorize the access of Orange Cloud service.

```Java
mApi.getSession().startAuthentication();
```

Upon authentication, users are returned your own Activity.java. To finish authentication after the user returns to your app, you have to put the following code in your **onResume** function.
```Java
@Override
protected void onResume() {
	super.onResume();
	// Get the session and check the authentication state. 
	//!\ Have a asynchronous request here to check or refresh access token.
	final AuthSession session = mApi.getSession();
	session.checkAuthentication(new OrangeListener.Success<String>() {
		@Override
		public void onResponse(String response) {
			// Have a valid session, you can begin to use Cloud functions  
		}
	}, new OrangeListener.Error() {
		@Override
		public void onErrorResponse(OrangeAPIException error) {
			// An error occurred
		}
	});
}
```

Add scopes
----------

Before to call `mApi.getSession().startAuthentication();`, you can add new scopes. For example, if you have access to Cloud Full Read API (see documentation on [Cloud Api Reference](https://developer.orange.com/apis/cloud-france/api-reference)), you can add this scope like :

```Java
// Add scope "cloudfullread"
mApi.addScope("cloudfullread");

// Start session
session.startAuthentication();
``` 


Listing contents
--------------------
To list folder and contents, you have to pass an **Entry** object (`null`to get the root). An entry is a representation of file or folder.
```Java
mApi.listEntries(anEntry, parameters, new OrangeListener.Success<OrangeCloudAPI.Entry>() {
	Override
	public void onResponse(OrangeCloudAPI.Entry response) {
		// success
	}
}, new OrangeListener.Error() {
	@Override
	public void onErrorResponse(OrangeAPIException error) {
		// Error occurred
	}
});
```

**parameters** is an JSONObject parameters. You can enable pagination or other parameters (see [Cloud Api Reference](https://developer.orange.com/apis/cloud-france/api-reference))

**listContents()** function returns just the unique identifier and name of files. If you want more informations about file, you have to call **fileInfo()** function to get the creation date, size and thumbnail and content URLs
```Java
mApi.fileInfo(fileEntry, new OrangeListener.Success<OrangeCloudAPI.Entry>() {
	@Override
	public void onResponse(OrangeCloudAPI.Entry entry) {
		// Returns the entry file with extra informations.
	}
}, new OrangeListener.Error() {
	@Override
	public void onErrorResponse(OrangeAPIException error) {
		// Error occurred
	}
});
```
Create folder
--------------------
Create a folder as a Entry child. Name of folder has to be unique. Returns an Entry folder
```Java
mApi.createFolder(anEntry, folderName, new OrangeListener.Success<OrangeCloudAPI.Entry>() {
	@Override
	public void onResponse(OrangeCloudAPI.Entry newFolder) {
		// folder has been created
	}
}, new OrangeListener.Error() {
	@Override
	public void onErrorResponse(OrangeAPIException error) {
		// Error occurred
	}
});
```

Delete folder or file
------------------------------
You can delete a file or a folder and its contents by calling delete function.
```Java
mApi.delete(entryToRemove, new OrangeListener.Success<String>() {
	@Override
	public void onResponse(String response) {
		// File or folder entry has been deleted                   
	}
}, new OrangeListener.Error() {
	@Override
	public void onErrorResponse(OrangeAPIException error) {
		// Error occurred
	}
});
```

Rename folder or file [new]
------------------------------
You can rename a file or a folder by calling rename function.
```Java
mApi.rename(entryToRename, name, new OrangeListener.Success<String>() {
	@Override
	public void onResponse(String response) {
		// File or folder entry has been deleted                   
	}
}, new OrangeListener.Error() {
	@Override
	public void onErrorResponse(OrangeAPIException error) {
		// Error occurred
	}
});
```
Copy folder or file [new]
------------------------------
You can copy a file or a folder by calling copy function.
```Java
mApi.copy(entryToCopy, destination, new OrangeListener.Success<String>() {
	@Override
	public void onResponse(String response) {
		// File or folder entry has been deleted                   
	}
}, new OrangeListener.Error() {
	@Override
	public void onErrorResponse(OrangeAPIException error) {
		// Error occurred
	}
});
```
**Note:** You can not copy a folder in the same parent.

Move folder or file [new]
------------------------------
You can copy a file or a folder by calling copy function.
```Java
mApi.copy(entryToCopy, destination, new OrangeListener.Success<String>() {
	@Override
	public void onResponse(String response) {
		// File or folder entry has been deleted                   
	}
}, new OrangeListener.Error() {
	@Override
	public void onErrorResponse(OrangeAPIException error) {
		// Error occurred
	}
});
```
**Note:** You can not copy a folder in the same parent.

Upload a File
-------------------
**/!\\** This function has to be called as a background task, for example an AsyncTask or maybe an IntentService.

```Java
mApi.upload(fileUri, filename, entryToUpload, new OrangeListener.Success<JSONObject>() {
	@Override
	public void onResponse(JSONObject response) {
		// File has been uploaded
	}
}, new OrangeListener.Progress() {
	@Override
	public void onProgress(float ratio) {
		// Listener to create progress bar (ratio = [0,1])
	}
}, new OrangeListener.Error() {
	@Override
	public void onErrorResponse(OrangeAPIException error) {
		// Error occurred
	}
});
```
To get the File Object, you can use an Intent and get the result in the onActivityResult (See sample)
```Java
Intent intent = new Intent();
intent.setType("image/*");
intent.setAction(Intent.ACTION_GET_CONTENT);
startActivityForResult(Intent.createChooser(intent, "Select Picture"), 1);
```
File content features
-----------------------------
SDK not offers basic methods to download and save file on the disk but you are able to develop thanks to Entry properties (thumbnailURL, previewURL, downloadURL) and getHeaders() from OrangeCloudAPI.
**/!\\** Theses properties are available after a fileInfo request.

You can display easily the thumbnail of an entry file. It is a very small graphical representation of the file, only available for some file type (photo, pdf, ...)
```Java
mApi.thumbnail(entry, new OrangeListener.Success<Bitmap>() {
	@Override
	public void onResponse(Bitmap response) {
	    // Have a Bitmap            
	}
}, new OrangeListener.Error() {
	@Override
	public void onErrorResponse(OrangeAPIException error) {
		// Error occurred
	}
});
```
You can display the preview of an entry file. It is a graphical representation suitable to be displayed in full screen on a mobile device, only available for some file type (photo, pdf, ...)
```Java
mApi.preview(entry, new OrangeListener.Success<Bitmap>() {
	@Override
	public void onResponse(Bitmap response) {
	    // Have a Bitmap            
	}
}, new OrangeListener.Error() {
	@Override
	public void onErrorResponse(OrangeAPIException error) {
		// Error occurred
	}
});
```
Or display the real content of entry image. 
```Java
mApi.imageContent(entry, new OrangeListener.Success<Bitmap>() {
	@Override
	public void onResponse(Bitmap response) {
	    // Have a Bitmap            
	}
}, new OrangeListener.Error() {
	@Override
	public void onErrorResponse(OrangeAPIException error) {
		// Error occurred
	}
});
```
Image cache policy
--------------------------
Methods **thumbnail** and **preview** can keep data in a cache. For that you have to declare a **ImageCache** thanks to [Volley image cache](http://developer.android.com/training/volley/request.html) just after to create Api object. 
```Java
mApi = new OrangeCloudAPI<AuthSession>(session);
// An you can set a Image cache policy
mApi.setImageCache(new LruBitmapCache());
```
 No newline at end of file
