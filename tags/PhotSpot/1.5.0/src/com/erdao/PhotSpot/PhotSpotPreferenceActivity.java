package com.erdao.PhotSpot;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.SearchRecentSuggestions;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup.LayoutParams;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class PhotSpotPreferenceActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	/** message frame for update notification */
	private ScrollView msgFrame_;
	/** message body update notification */
	private TextView msgTxtView_;
	/** context */
	private Context context_;
	/** SharedPreferences object for saving preference */
	private SharedPreferences settings_;
	/** fickr id get task */
	ConfirmUserIdTask userIdConfirmTask_;

	/**
	 * onCreate handler.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context_ = this;
		settings_ = PreferenceManager.getDefaultSharedPreferences(this);
		addPreferencesFromResource(R.xml.settings);
	}

	/**
	 * onResume handler.
	 */
	@Override
    protected void onResume() {
    	super.onResume();
    	getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

	/**
	 * onPause handler.
	 */
	@Override
    protected void onPause() {
    	super.onPause();
    	getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);

    }
   
	/**
	 * onSharedPreferenceChanged handler.
	 */
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    	if(key.equals(getString(R.string.prefkey_flickruser))){
    		String username = sharedPreferences.getString(key, "");
    		if(!username.equals(""))
    			ConfirmUserId(username,JsonFeedGetter.MODE_FLICKR_ID);
    	}
    	if(key.equals(getString(R.string.prefkey_picasauser))){
    		String username = sharedPreferences.getString(key, "");
    		if(!username.equals(""))
    			ConfirmUserId(username,JsonFeedGetter.MODE_PICASA_ID);
    	}
	};

	/**
	 * onCreateOptionsMenu handler.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuItem menu_Revert = menu.add(0,R.id.menu_Revert,0,R.string.menu_Revert);
		menu_Revert.setIcon(android.R.drawable.ic_menu_revert);
		return true;
	}

	/**
	 * onOptionsItemSelected handler.
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_Revert: {
				finish();
				break;
			}
		}
		return true;
	}
		
	/**
	 * onPreferenceTreeClick handler.
	 */
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference){
    	String title = (String) preference.getTitle();
    	if(title.equals(getString(R.string.setting_clearhistory))){
    		clearSearchHistory();
    		return true;
    	}
    	else if(title.equals(getString(R.string.setting_about))){
			showDialog(R.id.AboutDlg);
    		return true;
    	}
		return false;
    }

	/**
	 * onCreateDialog handler
	 */
	protected Dialog onCreateDialog(int id) {
		switch (id) {
			case R.id.QuerySearchDlg: {
				OnCancelListener cancelListener = new OnCancelListener() {
					public void onCancel(DialogInterface dialog) {
						userIdConfirmTask_.cancel(true);
					}
				};
				ProgressDialog dialog = new ProgressDialog(this);
				dialog.setMessage(this.getText(R.string.QueryConfirm));
				dialog.setIndeterminate(true);
				dialog.setCancelable(true);
				dialog.setOnCancelListener(cancelListener);
				return dialog;
			}
			case R.id.AboutDlg:{
				String msgStr = getString(R.string.AboutDlgContent);
				msgStr+=getString(R.string.PrivacyAgreement);
				msgTxtView_ = new TextView(this);
				msgTxtView_.setTextSize(14);
				msgTxtView_.setText(msgStr);
				msgTxtView_.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
				msgTxtView_.setTextColor(Color.WHITE);
				msgTxtView_.setBackgroundColor(Color.DKGRAY);
				msgFrame_ = new ScrollView(this);
				msgFrame_.addView(msgTxtView_);
				return new AlertDialog.Builder(this)
				.setIcon(R.drawable.icon)
				.setTitle(R.string.AboutDlgTitle)
				.setView(msgFrame_)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
					}
				})
				.create();
			}
		}
		return null;
	}
		
	/**
	 * Clears SearchHistory
	 */
	private void clearSearchHistory() {
		new AlertDialog.Builder(this)
		.setMessage(R.string.ClearHistoryConfirm)
		.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				dialog.dismiss();
				SearchRecentSuggestions suggestions = new SearchRecentSuggestions(context_, 
						PhotSpotSearchSuggestProvider.AUTHORITY, PhotSpotSearchSuggestProvider.MODE);
				suggestions.clearHistory();
			}
		})
		.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				dialog.dismiss();
			}
		})
	   .create()
	   .show();
	}
	
	/**
	 * Get Flickr Id
	 */
	private void ConfirmUserId(String username, int mode) {
		userIdConfirmTask_ = new ConfirmUserIdTask(context_,mode);
		// URLEncode
		try {
			username = URLEncoder.encode(username,"UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String uri;
		if(mode == JsonFeedGetter.MODE_FLICKR_ID)
			uri = "http://api.flickr.com/services/rest/?method=flickr.people.findByUsername&format=json&api_key="+getString(R.string.flickr_apikey)+"&username="+username;
		else if(mode == JsonFeedGetter.MODE_PICASA_ID)
			uri = "http://picasaweb.google.com/data/feed/api/user/"+username+"?alt=json";
		else
			return;
		showDialog(R.id.QuerySearchDlg);
		userIdConfirmTask_.execute(uri);
	}

	/**
	 * Callback for AsyncTask completion.
	 * @param code return code from AsyncTask
	 */
	protected void onAsyncTaskComplete(Integer code){
		dismissDialog(R.id.QuerySearchDlg);
		if(code==JsonFeedGetter.CODE_JSONERROR){
			AlertDialog.Builder ad = new AlertDialog.Builder(this);
			ad.setMessage(R.string.jsonErrorMsgConfirmUser);
			ad.setPositiveButton(android.R.string.ok,null);
			ad.setTitle(R.string.app_name);
			ad.create();
			ad.show();
		}
		else if(code==JsonFeedGetter.CODE_HTTPERROR){
			AlertDialog.Builder ad = new AlertDialog.Builder(this);
			ad.setMessage(R.string.httpErrorMsg);
			ad.setPositiveButton(android.R.string.ok,null);
			ad.setTitle(R.string.app_name);
			ad.create();
			ad.show();
		}
	}
	
	/**
	 * Local Search Task. Retrieve JSON local search and display it.
	 */
	private class ConfirmUserIdTask extends AsyncTask<String, Integer, Integer> {
		/** Json Utility object */
		private JsonFeedGetter getter_;
		/** Context */
		private Context context_;
		/** mode */
		private int mode_;
		

		/**
		 * @param c Context object
		 */
		public ConfirmUserIdTask(Context c, int mode) {
			context_ = c;
			mode_ = mode;
			getter_ = new JsonFeedGetter(context_,mode_);
		}

		/**
		 * execute AsyncTask to retrieve LocalSearch
		 * @param uris uri for retrieving feed.
		 */
		@Override
		protected Integer doInBackground(String... uris) {
			return getter_.getFeed(uris[0]);
		}

		/**
		 * callback from AsyncTask upon completion.
		 * @param code errorcode.
		 */
		@Override
		protected void onPostExecute(Integer code) {
			onAsyncTaskComplete(code);
			String userid = "";
			SharedPreferences.Editor editor = settings_.edit();
			if(mode_==JsonFeedGetter.MODE_FLICKR_ID){
				if(code==JsonFeedGetter.CODE_OK){
					userid = getter_.getUserId();
					Toast.makeText(context_, R.string.ToastUserConfirmSuccess, Toast.LENGTH_SHORT).show();
				}
				editor.putString(getString(R.string.prefkey_flickruserid), userid);
				editor.commit();
			}
			else if(mode_==JsonFeedGetter.MODE_PICASA_ID){
				if(code==JsonFeedGetter.CODE_OK){
					Toast.makeText(context_, R.string.ToastUserConfirmSuccess, Toast.LENGTH_SHORT).show();
				}
			}
		}
	
		/**
		 * onCancel handler.
		 */
		@Override
		protected void onCancelled() {
		}
	};

	
}
