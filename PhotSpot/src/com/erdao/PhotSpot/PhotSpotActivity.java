/*
 * Copyright (C) 2009 Huan Erdao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.erdao.PhotSpot;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.SearchRecentSuggestions;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

/* Main Activity Class for PhotSpot */
public class PhotSpotActivity extends MapActivity {

	/* google map variables */
	private MapView mapView_;
	private MapController mapCtrl_;
	private LocationManager locationMgr_;
	private Criteria criteria_;
	private MyLocationOverlay myLocationOverlay_ = null;
	private List<Overlay> mapOverlays_;
	private boolean mylocationEnabled_;
	private List<Address> addresses_;
	
	/* for mylocation thread */
	private Handler handler_;

	/* photo feed task */
	private GetPhotoFeedTask getPhotoFeedTask_;
	private int contentProvider_;

	/* Clusterer */
	private GeoClusterer clusterer_ = null;

	/** Called when the activity is first created. */
	protected boolean isRouteDisplayed() {
		return false;
	}
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);
		resetViews();
		handler_ = new Handler();

		mapView_ = (MapView)findViewById(R.id.mapview);
		mapCtrl_ = mapView_.getController();
		mapCtrl_.setZoom(15);
		/* since built-in zoom control cannot hook event, set button for zoom control */
		mapView_.setBuiltInZoomControls(false);
		mapView_.displayZoomControls(false);
		Button zoBtn = (Button)findViewById(R.id.zoomoutbtn);
		zoBtn.setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				Button zoBtn = (Button)findViewById(R.id.zoomoutbtn);
				if(event.getAction() == MotionEvent.ACTION_DOWN) {
					zoBtn.setBackgroundResource(R.drawable.btn_zoom_out_pressed);
					mapCtrl_.zoomOut();
				}
				else if(event.getAction() == MotionEvent.ACTION_UP){
					zoBtn.setBackgroundResource(R.drawable.btn_zoom_out_normal);
				}
				return false;
			}	
		});
		Button ziBtn = (Button)findViewById(R.id.zoominbtn);
		ziBtn.setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				Button zoBtn = (Button)findViewById(R.id.zoominbtn);
				if(event.getAction() == MotionEvent.ACTION_DOWN) {
					zoBtn.setBackgroundResource(R.drawable.btn_zoom_in_pressed);
					if(clusterer_ != null){
						clusterer_.zoomInFixing();
					}else{
						mapCtrl_.zoomIn();
					}
				}
				else if(event.getAction() == MotionEvent.ACTION_UP){
					zoBtn.setBackgroundResource(R.drawable.btn_zoom_in_normal);
				}
				return false;
			}	
		});
		
		/* setup location manager */
		locationMgr_ = (LocationManager)getSystemService(Context.LOCATION_SERVICE); 
		criteria_ = new Criteria();
		criteria_.setAccuracy(Criteria.ACCURACY_FINE);
		criteria_.setAltitudeRequired(false);
		criteria_.setBearingRequired(false);
		criteria_.setCostAllowed(true);
		criteria_.setPowerRequirement(Criteria.POWER_LOW);
		setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

		/* Restore preferences */
		SharedPreferences settings = getSharedPreferences(getString(R.string.PhotSpotPreference), 0);
		boolean mapmode = settings.getBoolean(getString(R.string.PrefMapMode), false);
		mapView_.setSatellite(mapmode);
		contentProvider_ = settings.getInt(getString(R.string.PrefContentProvider), 0);

		/* get and process search query here */
		final Intent queryIntent = getIntent();
		final String queryAction = queryIntent.getAction();
		if (Intent.ACTION_SEARCH.equals(queryAction)) {
			Toast.makeText(this, R.string.ToastRevGeoCodeRun, Toast.LENGTH_SHORT).show();
			mylocationEnabled_ = false;
			doSearchQuery(queryIntent);
		}
		else{
			Toast.makeText(this, R.string.ToastInstructionNav, Toast.LENGTH_LONG).show();
			mylocationEnabled_ = true;
		}
	}

	/* resetViews */
	private void resetViews(){
		// hide imageframe
		FrameLayout frameLayout = (FrameLayout)findViewById(R.id.imageframe);
		if(frameLayout.getVisibility()!=View.GONE)
			frameLayout.setVisibility(View.GONE);
	}

	/* onStart */
	@Override
	public void onStart() {
		super.onStart();
//		Log.i("DEBUG", "onStart");
		if(clusterer_ == null && mylocationEnabled_){
			String provider = locationMgr_.getBestProvider(criteria_, true);
			Location location = locationMgr_.getLastKnownLocation(provider);
			if(location != null){
				Double geoLat = location.getLatitude()*1E6;
				Double geoLng = location.getLongitude()*1E6;
				GeoPoint point = new GeoPoint(geoLat.intValue(), geoLng.intValue());
				resetViews();
				mapCtrl_.animateTo(point);
			}
			startListening();
		}
	}

	/* onStop */
	@Override 
	public void onStop() {
//		Log.i("DEBUG", "onStop"); 
		stopListening();
		super.onStop();
	}

	/* startListening */
	private void startListening() {
//		Log.i("DEBUG", "startListeningGPS"); 
		if( myLocationOverlay_ == null ){
			myLocationOverlay_ = new MyLocationOverlay(this, mapView_);
		}
		List<Overlay> mapOverlays = mapView_.getOverlays();
		if(!mapOverlays.contains(mapOverlays)){
			mapOverlays.add(myLocationOverlay_);
		}
		myLocationOverlay_.enableMyLocation();
		myLocationOverlay_.runOnFirstFix(new Runnable() {
			public void run() {
				handler_.post(new Runnable() {
					public void run() {
						resetViews();
					}
				});
				mapCtrl_.animateTo(myLocationOverlay_.getMyLocation());
			}
		});
	}

	/* stopListening */
	private void stopListening() {
		if(myLocationOverlay_!= null)
			myLocationOverlay_.disableMyLocation();
	}

	/* create Menu */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		menu.clear();
		if(clusterer_==null){
				MenuItem menu_SearchPhotSpot = menu.add(0,R.id.menu_FindSpots,0,R.string.menu_SearchPhotSpot);
			menu_SearchPhotSpot.setIcon(R.drawable.ic_menu_searchspot);
		}else{
			MenuItem menu_SearchPhotSpot = menu.add(0,R.id.menu_FindSpots,0,R.string.menu_RefreshPhotSpot);
			menu_SearchPhotSpot.setIcon(R.drawable.ic_menu_refreshspot);
		}
		MenuItem menu_SearchPlace = menu.add(0,R.id.menu_SearchPlace,0,R.string.menu_SearchPlace);
		menu_SearchPlace.setIcon(R.drawable.ic_menu_searchplace);
		MenuItem menu_MyLocation = menu.add(0,R.id.menu_MyLocation,0,R.string.menu_MyLocation);
		menu_MyLocation.setIcon(R.drawable.ic_menu_mylocation);
		MenuItem menu_MapMode = menu.add(0,R.id.menu_MapMode,0,R.string.menu_MapMode);
		menu_MapMode.setIcon(android.R.drawable.ic_menu_mapmode);
		MenuItem menu_Preferences = menu.add(0,R.id.menu_Preferences,0,R.string.menu_Preferences);
		menu_Preferences.setIcon(android.R.drawable.ic_menu_preferences);
		MenuItem menu_ClearSuggest = menu.add(0,R.id.menu_ClearSuggest,0,R.string.menu_ClearSuggest);
		menu_ClearSuggest.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
		MenuItem menu_Help = menu.add(0,R.id.menu_Help,0,R.string.menu_Help);
		menu_Help.setIcon(android.R.drawable.ic_menu_help);
		MenuItem menu_About = menu.add(0,R.id.menu_About,0,R.string.menu_About);
		menu_About.setIcon(android.R.drawable.ic_menu_info_details);
		return true;
	}

	/* Menu handling */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		String uri = "";
		Projection proj = mapView_.getProjection();
		GeoPoint nw = proj.fromPixels(0,0);
		GeoPoint se = proj.fromPixels(mapView_.getWidth(),mapView_.getHeight());
		Double nwlat = nw.getLatitudeE6()/1E6;
		Double nwlng = nw.getLongitudeE6()/1E6;
		Double selat = se.getLatitudeE6()/1E6;
		Double selng = se.getLongitudeE6()/1E6;
		switch (item.getItemId()) {
			case R.id.menu_FindSpots: {
				showDialog(R.id.QuerySearchDlg);
				uri = "http://photspotcloud.appspot.com/photspotcloud?nwlng="+nwlng+"&selat="+selat+"&nwlat="+nwlat+"&selng="+selng;
				String debugstr = Locale.getDefault().getDisplayName()+","+Build.MODEL+","+Build.VERSION.RELEASE;
				try {
					debugstr = URLEncoder.encode(debugstr,"UTF-8");
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				switch( contentProvider_ ){
					case ContentProvider.Panoramio:
					default:{
							uri += "&svc=panoramio";
						break;
					}
					case ContentProvider.PicasaWeb:{
						uri += "&svc=picasa";
						break;
					}
					case ContentProvider.Flickr:{
						uri += "&svc=flickr";
						break;
					}
				}
				uri += "&dbg="+debugstr;
//				Log.i("DEBUG",uri);
				getPhotoFeedTask_ = new GetPhotoFeedTask(this,contentProvider_);
				getPhotoFeedTask_.execute(uri);
				break;
			}
			case R.id.menu_SearchPlace:{
				onSearchRequested();
				break;
			}
			case R.id.menu_MyLocation:{
				mylocationEnabled_ = true;
				startListening();
				break;
			}
			case R.id.menu_MapMode:{
				showDialog(R.id.MapModeDlg);
				break;
			}
			case R.id.menu_Preferences:{
				showDialog(R.id.PreferencesDlg);
				break;
			}
			case R.id.menu_ClearSuggest:{
				clearSearchHistory();
				break;
			}
			case R.id.menu_Help:{
				Intent i = new Intent(this, HelpActivity.class);
				startActivity(i);
				break;
			}
			case R.id.menu_About:{
				showDialog(R.id.AboutDlg);
				break;
			}
		}
		return true;
	}

	/* onCreateDialog */
	protected Dialog onCreateDialog(int id) {
		switch (id) {
			// create progress dialog for search
			case R.id.QuerySearchDlg: {
				OnCancelListener cancelListener = new OnCancelListener() {
					public void onCancel(DialogInterface dialog) {
						getPhotoFeedTask_.cancel(true);
					}
				};
				ProgressDialog dialog = new ProgressDialog(this);
				dialog.setMessage(this.getText(R.string.QuerySearching));
				dialog.setIndeterminate(true);
				dialog.setCancelable(true);
				dialog.setOnCancelListener(cancelListener);
				return dialog;
			}
			// create map selection dialog and change modes
			case R.id.MapModeDlg: {
				int choice = mapView_.isSatellite() ? 1 : 0;
				return new AlertDialog.Builder(this)
				.setTitle(R.string.MapModeDlgTitle)
				.setSingleChoiceItems(R.array.select_map_mode, choice, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						boolean isSatelite = whichButton == 0 ? false : true;
						mapView_.setSatellite(isSatelite);
						SharedPreferences settings = getSharedPreferences(getString(R.string.PhotSpotPreference), 0);
						SharedPreferences.Editor editor = settings.edit();
						editor.putBoolean(getString(R.string.PrefMapMode), isSatelite);
						editor.commit();
						dismissDialog(R.id.MapModeDlg);
					}
				})
				.create();
			}
			// create map selection dialog and change modes
			case R.id.PreferencesDlg: {
				return new AlertDialog.Builder(this)
				.setTitle(R.string.PreferencesDlgTitle)
				.setSingleChoiceItems(R.array.select_service, contentProvider_, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						contentProvider_ = whichButton;
						SharedPreferences settings = getSharedPreferences(getString(R.string.PhotSpotPreference), 0);
						SharedPreferences.Editor editor = settings.edit();
						editor.putInt(getString(R.string.PrefContentProvider),contentProvider_);
						editor.commit();
						dismissDialog(R.id.PreferencesDlg);
					}
				})
				.create();
			}
			case R.id.AboutDlg:{
				return new AlertDialog.Builder(this)
				.setIcon(R.drawable.icon)
				.setTitle(R.string.AboutDlgTitle)
				.setMessage(R.string.AboutDlgContent)
				.setPositiveButton(R.string.AboutDlgOK, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
					}
				})
				.create();
			}
		}
		return null;
	}

	/* clearSearchHistory */
	private void clearSearchHistory() {
		SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this, 
				PhotSpotSearchSuggestionsProvider.AUTHORITY, PhotSpotSearchSuggestionsProvider.MODE);
		suggestions.clearHistory();
	}
	
	/* onAsyncTaskComplete */
	protected void onAsyncTaskComplete(Integer code){
		dismissDialog(R.id.QuerySearchDlg);
		if(code==PhotoFeedGetter.CODE_HTTPERROR){
			AlertDialog.Builder ad = new AlertDialog.Builder(this);
			ad.setMessage(R.string.httpErrorMsg);
			ad.setPositiveButton(android.R.string.ok,null);
			ad.setTitle(R.string.app_name);
			ad.create();
			ad.show();
		}
		else if(code==PhotoFeedGetter.CODE_NORESULT){
			AlertDialog.Builder ad = new AlertDialog.Builder(this);
			ad.setMessage(R.string.noResultErrorMsg);
			ad.setPositiveButton(android.R.string.ok,null);
			ad.setTitle(R.string.app_name);
			ad.create();
			ad.show();
		}
		else{
			resetViews();
		}
	}

	/* onSearchRequested */
	@Override
	public boolean onSearchRequested() {
//		Log.i("DEBUG","onSearchRequested");
		Bundle appData = new Bundle();
		startSearch( null, false, appData, false);
		return true;
	}

	/* doSearchQuery */
	private void doSearchQuery(final Intent queryIntent) {
		final String queryString = queryIntent.getStringExtra(SearchManager.QUERY);
//		Log.i("DEBUG","doSearchQuery:"+queryString);
		SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this, 
				PhotSpotSearchSuggestionsProvider.AUTHORITY, PhotSpotSearchSuggestionsProvider.MODE);
		suggestions.saveRecentQuery(queryString, null);
		Geocoder geoCoder = new Geocoder(this);	
		try {
				addresses_ = geoCoder.getFromLocationName(queryString,10);
				if(addresses_.size()>0){
					CharSequence[] places = new CharSequence[addresses_.size()];
					for(int i=0;i<addresses_.size();i++){
						Address addr = addresses_.get(i);
						String place = "";
						int linesize = addr.getMaxAddressLineIndex();
						if(linesize!=-1){
							for(int j=0;j<=linesize;j++){
								place += (addr.getAddressLine(j)+", ");
							}
						}else{
							if(addr.getFeatureName()!=null)
								place += (addr.getFeatureName()+", ");
							else
								place += (queryString+", ");
							if(addr.getAdminArea()!=null)
								place += (addr.getAdminArea()+", ");
							if(addr.getCountryName()!=null)
								place += addr.getCountryName();
						}
						places[i] = place;
					}
					new AlertDialog.Builder(this)
					.setTitle(R.string.ReverseGeoResult)
					.setItems(places, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							GeoPoint p = new GeoPoint(
									(int) (addresses_.get(which).getLatitude() * 1E6), 
									(int) (addresses_.get(which).getLongitude() * 1E6));
							stopListening();
							mapCtrl_.animateTo(p);	
							mapView_.invalidate();
							dialog.dismiss();
						}
					})
					.create()
					.show();
				}	
			} catch (IOException e) {
				Toast.makeText(this, R.string.ToastRevGeoCodeFail, Toast.LENGTH_SHORT).show();
//				Log.i("DEBUG","IOException");
				e.printStackTrace();
			}
	}
	
	/* GetPhotoFeedTask - AsyncTask */
	private class GetPhotoFeedTask extends AsyncTask<String, Integer, Integer> {
		PhotoFeedGetter getter_;
		Context context_;
		/* constructor */
		public GetPhotoFeedTask(Context c, int svc) {
			context_ = c;
			getter_ = new PhotoFeedGetter(svc,context_);
		}

		/* doInBackground */
		@Override
		protected Integer doInBackground(String... uris) {
//			Log.i("DEBUG","doInBackground");
			return getter_.getFeed(uris[0]);
		}

		/* onPostExecute */
		@Override
		protected void onPostExecute(Integer code) {
			onAsyncTaskComplete(code);
			if(code!=PhotoFeedGetter.CODE_HTTPERROR){
				ArrayList<PhotoItem> photoItems = getter_.getPhotoItemList();
//				Log.i("DEBUG","result size:"+photoItems.size());
				mapOverlays_ = mapView_.getOverlays();
				mapOverlays_.clear();
				if(myLocationOverlay_!=null&&mylocationEnabled_){
					mapOverlays_.add(myLocationOverlay_);
				}
				FrameLayout imageFrame = (FrameLayout)findViewById(R.id.imageframe);
				clusterer_ = new GeoClusterer(context_, mapView_,imageFrame);
				for(int i=0; i<photoItems.size(); i++) {
					PhotoItem item = photoItems.get(i);
					clusterer_.addItem(item);
				}
				clusterer_.redraw();
				mapView_.invalidate();
			}
		}
		
		/* onCancelled */
		@Override
		protected void onCancelled() {
//			Log.i("DEBUG","onCancelled");
		}
	};

}