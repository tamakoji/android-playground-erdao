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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.SearchRecentSuggestions;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.erdao.android.mapviewutil.GeoItem;
import com.erdao.android.mapviewutil.markerclusterer.MarkerBitmap;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

/**
 * Main Activity Class for PhotSpot
 * @author Huan Erdao
 */
public class PhotSpotActivity extends MapActivity {

	/** self object */
	private PhotSpotActivity me_;
	/** Context object */
	private Context context_;
	/** screen density for multi-resolution
	 *	get from contenxt.getResources().getDisplayMetrics().density;  */
	private float screenDensity_ = 1.0f;

	/* google map variables */
	/** MapView object */
	private MapView mapView_;
	/** MapController object */
	private MapController mapCtrl_;
	/** LocationManager object */
	private LocationManager locationMgr_;
	/** Criteria object */
	private Criteria criteria_;
	/** MyLocationOverlay object */
	private MyLocationOverlay myLocationOverlay_ = null;
	/** list of Overlay object */
	private List<Overlay> mapOverlays_;
	/** flag if mylocation enabled */
	private boolean mylocationEnabled_;
	/** flag if mylocation centering enabled */
	private boolean mylocationSetFocus_;
	/** List of Address for search place */
	private List<Address> addresses_;

	/* settings */
	/** SharedPreferences object for saving preference */
	private SharedPreferences settings_;
	/** message frame for update notification */
	private ScrollView msgFrame_;
	/** message body update notification */
	private TextView msgTxtView_;
	/** flickr userid for retrieving user's content */
	private String userIdFlickr_;
	/** picasa userid for retrieving user's content */
	private String userIdPicasa_;

	/* for mylocation thread */
	/** Handler object to refresh UI from thread */
	private Handler handler_;

	/* photo feed task */
	/** Photo feed retrieving Async task object */
	private GetPhotoFeedTask getPhotoFeedTask_;
	/** service to retrieve photo */
	private int serviceProvider_;

	/* Clusterer */
	/** Clusterer object */
	private PhotSpotClusterer clusterer_ = null;
	/** Bitmap for marker icons */
	private List<MarkerBitmap> markerIconBmps_ = new ArrayList<MarkerBitmap>();

	/* favorite ovelay */
	/** flag if favorite is overlayed */
	private boolean favoriteOverlayed_;
	/** Handler to refresh favorite overlay(distance to current)*/
	private Handler favUpdateTimer_ = new Handler();
	
	/**
	 * isRouteDisplayed
	 */
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
	
	/**
	 * onCreate handler
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		me_ = this;
		context_ = this;
		screenDensity_ = this.getResources().getDisplayMetrics().density;
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);
		resetViews();
		handler_ = new Handler();

		/* Read preferences */
		settings_ = PreferenceManager.getDefaultSharedPreferences(this);
		//getSharedPreferences(getString(R.string.PhotSpotPreference), 0);

//		final float scale = this.getResources().getDisplayMetrics().density;
//		Log.i("DEBUG","scale = "+scale);
		

		onCreateMain();

		String ver_legal = settings_.getString(getString(R.string.prefkey_verlegal), "");
		boolean legalCheck = !ver_legal.equals(getString(R.string.legal_ver));
		if(legalCheck){
			msgTxtView_ = new TextView(this);
			msgTxtView_.setTextSize(13);
			msgTxtView_.setText(R.string.PrivacyAgreement);
			msgTxtView_.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
			msgTxtView_.setTextColor(Color.WHITE);
			msgTxtView_.setBackgroundColor(Color.DKGRAY);
			msgFrame_ = new ScrollView(this);
			msgFrame_.addView(msgTxtView_);
			new AlertDialog.Builder(this)
			.setTitle(R.string.PrivacyAgreementDlgTitle)
			.setIcon(R.drawable.icon)
			.setView(msgFrame_)
			.setPositiveButton(R.string.Dlg_Agree, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					SharedPreferences.Editor editor = settings_.edit();
					editor.putString(getString(R.string.prefkey_verlegal),getString(R.string.legal_ver));
					editor.commit();
					dialog.dismiss();
					InitialMessage();
				}
			})
			.setNegativeButton(R.string.Dlg_Disagree, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					finish();
				}
			})
		   .create()
		   .show();
		}
		else
			InitialMessage();
	}
	
	/**
	 * setup initial objects main routine
	 */
	private void InitialMessage(){	
		String version = settings_.getString(getString(R.string.prefkey_verinfo), "");
		boolean versionCheck = !version.equals(getString(R.string.app_ver));
		SharedPreferences.Editor editor = settings_.edit();
		editor.putString(getString(R.string.prefkey_verinfo),getString(R.string.app_ver));
		editor.commit();
		if(versionCheck){
			msgTxtView_ = new TextView(this);
			msgTxtView_.setTextSize(13);
			msgTxtView_.setText(R.string.InitialMessage);
			msgTxtView_.setTextColor(Color.WHITE);
			msgTxtView_.setBackgroundColor(Color.DKGRAY);
			msgTxtView_.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
			msgFrame_ = new ScrollView(this);
			msgFrame_.addView(msgTxtView_);
			new AlertDialog.Builder(this)
			.setTitle(R.string.AboutDlgTitle)
			.setIcon(R.drawable.icon)
			.setView(msgFrame_)
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					dialog.dismiss();
					ToastMessage(R.string.ToastInstructionNav, Toast.LENGTH_LONG);
				}
			})
			.setNegativeButton(R.string.setting_help, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					dialog.dismiss();
					Intent i = new Intent(context_, HelpActivity.class);
					startActivity(i);
				}
			})
		   .create()
		   .show();
		}
	}

	/**
	 * setup initial objects main routine
	 */
	private void onCreateMain(){
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
				Button ziBtn = (Button)findViewById(R.id.zoominbtn);
				if(event.getAction() == MotionEvent.ACTION_DOWN) {
					ziBtn.setBackgroundResource(R.drawable.btn_zoom_in_pressed);
					if(clusterer_ != null){
						clusterer_.zoomInFixing();
					}else{
						mapCtrl_.zoomIn();
					}
				}
				else if(event.getAction() == MotionEvent.ACTION_UP){
					ziBtn.setBackgroundResource(R.drawable.btn_zoom_in_normal);
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

		/* get and process search query here */
		final Intent intent = getIntent();
		final String action = intent.getAction();
		favUpdateTimer_.removeCallbacks(favOverlayUpdateTask_);
		if (Intent.ACTION_SEARCH.equals(action)) {
			Toast.makeText(this, R.string.ToastRevGeoCodeRun, Toast.LENGTH_SHORT).show();
			mylocationSetFocus_ = false;
			mylocationEnabled_ = false;
			favoriteOverlayed_ = false;
			doSearchQuery(intent);
		}
		if (Intent.ACTION_VIEW.equals(action)) {
			mylocationEnabled_ = false;
			mylocationSetFocus_ = false;
			favoriteOverlayed_ = true;
			PhotoItem item = intent.getParcelableExtra(PhotoItem.EXT_PHOTOITEM);
			mapCtrl_.animateTo(item.getLocation());
			final List<Overlay> overlays = mapView_.getOverlays();
			overlays.add(new FavoriteOverlay(this,item));
			mapView_.invalidate();
		}
		else{
			mylocationEnabled_ = false;
			mylocationSetFocus_ = true;
			favoriteOverlayed_ = false;
		}
        final Object data = getLastNonConfigurationInstance();
        if (data != null) {
        	final PhotoItem[] items = (PhotoItem[]) data;
			FrameLayout imageFrame = (FrameLayout)findViewById(R.id.imageframe);
			setupMarkerIcons();
			clusterer_ = new PhotSpotClusterer(me_,markerIconBmps_,screenDensity_,mapView_,imageFrame);
			for(int i=0; i<items.length;i++) {
				clusterer_.addItem(items[i]);
			}
			/* jesus,,, there is no way to know if the mapview gets ready...
			 * (if mapview isnt visible, all items will regarded as out of bounds)
			 * so that try to post re-clustering x ms after...
			 * setOnFocusChangedListner does not work......*/
			Handler handler = new Handler();
			handler.postDelayed(new Runnable (){
				public void run() {
					clusterer_.resetViewport();
				}
			},1000);
        }
	}

	/**
	 * set up marker Icons according to the service
	 */
	private void setupMarkerIcons(){
		// create marker bitmaps
		markerIconBmps_.clear();
		Bitmap bmp_s_n,bmp_s_s,bmp_l_n,bmp_l_s;
		switch( serviceProvider_ ){
			case TypesService.PANORAMIO:
			default:{
				bmp_s_n = BitmapFactory.decodeResource(getResources(), R.drawable.balloon_pano_s_n);
				bmp_s_s = BitmapFactory.decodeResource(getResources(), R.drawable.balloon_pano_s_s);
				bmp_l_n = BitmapFactory.decodeResource(getResources(), R.drawable.balloon_pano_l_n);
				bmp_l_s = BitmapFactory.decodeResource(getResources(), R.drawable.balloon_pano_l_s);
				break;
			}
			case TypesService.PICASAWEB:{
				bmp_s_n = BitmapFactory.decodeResource(getResources(), R.drawable.balloon_picasa_s_n);
				bmp_s_s = BitmapFactory.decodeResource(getResources(), R.drawable.balloon_picasa_s_s);
				bmp_l_n = BitmapFactory.decodeResource(getResources(), R.drawable.balloon_picasa_l_n);
				bmp_l_s = BitmapFactory.decodeResource(getResources(), R.drawable.balloon_picasa_l_s);
				break;
			}
			case TypesService.FLICKR:{
				bmp_s_n = BitmapFactory.decodeResource(getResources(), R.drawable.balloon_flickr_s_n);
				bmp_s_s = BitmapFactory.decodeResource(getResources(), R.drawable.balloon_flickr_s_s);
				bmp_l_n = BitmapFactory.decodeResource(getResources(), R.drawable.balloon_flickr_l_n);
				bmp_l_s = BitmapFactory.decodeResource(getResources(), R.drawable.balloon_flickr_l_s);
				break;
			}
		}
		MarkerBitmap markerBitmap = new MarkerBitmap(bmp_s_n,bmp_s_s,new Point(20,20),14,10);
		markerIconBmps_.add(markerBitmap);
		markerBitmap = new MarkerBitmap(bmp_l_n,bmp_l_s,new Point(28,28),16,100);
		markerIconBmps_.add(markerBitmap);
	}
	/**
	 * setup before orientation change
	 */
	@Override
	public Object onRetainNonConfigurationInstance() {
		if(clusterer_!=null){
			List<GeoItem> items = clusterer_.getItems();
			final PhotoItem[] list = new PhotoItem[items.size()];
			for(int i = 0; i<items.size();i++){
				PhotoItem item = (PhotoItem)items.get(i);
				list[i] = new PhotoItem(item);
			}
			return list;
		}
		return null;
	}

	/**
	 * reset View. hide gallery frames.
	 */
	private void resetViews(){
		// hide imageframe
		FrameLayout frameLayout = (FrameLayout)findViewById(R.id.imageframe);
		if(frameLayout.getVisibility()!=View.GONE)
			frameLayout.setVisibility(View.GONE);
	}

	/**
	 * onStart handler.
	 */
	@Override
	public void onStart() {
		super.onStart();

		/* Restore preferences */
		boolean mapmode = settings_.getBoolean(getString(R.string.prefkey_mapmode), false);
		mapView_.setSatellite(mapmode);
		String svcmode = settings_.getString(getString(R.string.prefkey_svcproc), "0");
		serviceProvider_ = Integer.valueOf(svcmode);
		userIdFlickr_ = settings_.getString(getString(R.string.prefkey_flickruserid),"");
		userIdPicasa_ = settings_.getString(getString(R.string.prefkey_picasauser),"");

		if(clusterer_ == null && mylocationEnabled_){
			String provider = locationMgr_.getBestProvider(criteria_, true);
			if(provider != null){
				Location location = locationMgr_.getLastKnownLocation(provider);
				if(location != null){
					Double geoLat = location.getLatitude()*1E6;
					Double geoLng = location.getLongitude()*1E6;
					GeoPoint point = new GeoPoint(geoLat.intValue(), geoLng.intValue());
					resetViews();
					if(mylocationSetFocus_)
						mapCtrl_.animateTo(point);
				}
				startListening();
			}
		}
		if(favoriteOverlayed_)
			favUpdateTimer_.postDelayed(favOverlayUpdateTask_, 1000);
	}

	/**
	 * onStop handler.
	 */
	@Override 
	public void onStop() {
		if(favoriteOverlayed_)
			favUpdateTimer_.removeCallbacks(favOverlayUpdateTask_);
		stopListening();
		super.onStop();
	}

	/**
	 * Start listening to location manager and
	 * enabling myLocation.
	 */
	private void startListening() {
		if( myLocationOverlay_ == null ){
			myLocationOverlay_ = new MyLocationOverlay(this, mapView_);
		}
		List<Overlay> mapOverlays = mapView_.getOverlays();
		if(!mapOverlays.contains(myLocationOverlay_)){
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
				if(mylocationSetFocus_)
					mapCtrl_.animateTo(myLocationOverlay_.getMyLocation());
			}
		});
	}

	/**
	 * Stop listening to location manager and
	 * disabling myLocation.
	 */
	private void stopListening() {
		if(myLocationOverlay_!= null)
			myLocationOverlay_.disableMyLocation();
	}

	/**
	 * onPrepareOptionsMenu handler
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		menu.clear();
		if(clusterer_==null){
				MenuItem menu_SearchPhotSpot = menu.add(0,R.id.menu_SearchSpots,0,R.string.menu_SearchSpots);
			menu_SearchPhotSpot.setIcon(R.drawable.ic_menu_searchspots);
		}else{
			MenuItem menu_SearchPhotSpot = menu.add(0,R.id.menu_SearchSpots,0,R.string.menu_RefreshPhotSpot);
			menu_SearchPhotSpot.setIcon(R.drawable.ic_menu_searchspots);
		}
		if(serviceProvider_==TypesService.FLICKR){
			if(!userIdFlickr_.equals("")){
				MenuItem menu_SearchMySpot = menu.add(0,R.id.menu_SearchMySpots,0,R.string.menu_SearchMySpots);
				menu_SearchMySpot.setIcon(R.drawable.ic_menu_searchmyspots);
			}
		}
		else if(serviceProvider_==TypesService.PICASAWEB){
			if(!userIdPicasa_.equals("")){
				MenuItem menu_SearchMySpot = menu.add(0,R.id.menu_SearchMySpots,0,R.string.menu_SearchMySpots);
				menu_SearchMySpot.setIcon(R.drawable.ic_menu_searchmyspots);
			}
		}
		MenuItem menu_SearchPlace = menu.add(0,R.id.menu_SearchPlace,0,R.string.menu_SearchPlace);
		menu_SearchPlace.setIcon(R.drawable.ic_menu_searchplace);
		MenuItem menu_MyLocation = menu.add(0,R.id.menu_MyLocation,0,R.string.menu_MyLocation);
		menu_MyLocation.setIcon(R.drawable.ic_menu_mylocation);
		MenuItem menu_Favorites = menu.add(0,R.id.menu_Favorites,0,R.string.menu_Favorites);
		menu_Favorites.setIcon(android.R.drawable.star_big_on);
		MenuItem menu_MapMode = menu.add(0,R.id.menu_MapMode,0,R.string.menu_MapMode);
		menu_MapMode.setIcon(android.R.drawable.ic_menu_mapmode);
		MenuItem menu_Preferences = menu.add(0,R.id.menu_Preferences,0,R.string.menu_Preferences);
		menu_Preferences.setIcon(android.R.drawable.ic_menu_preferences);
		return true;
	}

	/**
	 * onOptionsItemSelected handler
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_SearchSpots: {
				SearchSpot(false);
				break;
			}
			case R.id.menu_SearchMySpots: {
				SearchSpot(true);
				break;
			}
			case R.id.menu_SearchPlace:{
				onSearchRequested();
				break;
			}
			case R.id.menu_MyLocation:{
				mylocationEnabled_ = true;
				mylocationSetFocus_ = true;
				startListening();
				break;
			}
			case R.id.menu_Favorites:{
				favUpdateTimer_.removeCallbacks(favOverlayUpdateTask_);
				if(clusterer_!=null)
					clusterer_.ClearGallery();
				Intent i = new Intent(this, FavoritesActivity.class);
				startActivity(i);
				break;
			}
			case R.id.menu_Preferences:{
				favUpdateTimer_.removeCallbacks(favOverlayUpdateTask_);
				if(clusterer_!=null)
					clusterer_.ClearGallery();
				Intent i = new Intent(this, PhotSpotPreferenceActivity.class);
				startActivity(i);
//				showDialog(R.id.PreferencesDlg);
				break;
			}
			case R.id.menu_MapMode:{
				showDialog(R.id.MapModeDlg);
				break;
			}
		}
		return true;
	}

	/**
	 * SearchSpot
	 */
	protected void SearchSpot(boolean mySpot){
		String uri = "";
		Projection proj = mapView_.getProjection();
		GeoPoint nw = proj.fromPixels(0,0);
		GeoPoint se = proj.fromPixels(mapView_.getWidth(),mapView_.getHeight());
		Double nwlat = nw.getLatitudeE6()/1E6;
		Double nwlng = nw.getLongitudeE6()/1E6;
		Double selat = se.getLatitudeE6()/1E6;
		Double selng = se.getLongitudeE6()/1E6;
		showDialog(R.id.QuerySearchDlg);
		uri = context_.getString(R.string.photspotserver)+"/photspotcloud?q=searchspot&nwlng="+nwlng+"&selat="+selat+"&nwlat="+nwlat+"&selng="+selng;
		uri += "&appver="+context_.getString(R.string.app_ver);
		String debugstr = Locale.getDefault().getDisplayName()+","+Build.MODEL+","+Build.VERSION.RELEASE+",android";
		try {
			debugstr = URLEncoder.encode(debugstr,"UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		switch( serviceProvider_ ){
			case TypesService.PANORAMIO:
			default:{
					uri += "&svc=panoramio";
				break;
			}
			case TypesService.PICASAWEB:{
				uri += "&svc=picasa";
				if(mySpot){
					uri += ("&userid="+userIdPicasa_);
				}
				break;
			}
			case TypesService.FLICKR:{
				uri += "&svc=flickr";
				if(mySpot){
					uri += ("&userid="+userIdFlickr_);
				}
				break;
			}
		}
		uri += "&dbg="+debugstr;
		favUpdateTimer_.removeCallbacks(favOverlayUpdateTask_);
		getPhotoFeedTask_ = new GetPhotoFeedTask(this);
		getPhotoFeedTask_.execute(uri);
		
	}

	/**
	 * onCreateDialog handler
	 */
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
						SharedPreferences.Editor editor = settings_.edit();
						editor.putBoolean(getString(R.string.prefkey_mapmode), isSatelite);
						editor.commit();
						dismissDialog(R.id.MapModeDlg);
					}
				})
				.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						dialog.dismiss();
					}
				})
				.create();
			}
		}
		return null;
	}

	/**
	 * ToastMessage utility
	 * @param messageId		message resource id
	 * @param duration		Toast duration
	 */
	public void ToastMessage(int messageId, int duration){
		Toast.makeText(this, messageId, duration).show();
	}

	/**
	 * Callback for AsyncTask completion.
	 * @param code return code from AsyncTask
	 */
	protected void onAsyncTaskComplete(Integer code){
		dismissDialog(R.id.QuerySearchDlg);
		if(code==JsonFeedGetter.CODE_HTTPERROR){
			AlertDialog.Builder ad = new AlertDialog.Builder(this);
			ad.setMessage(R.string.httpErrorMsg);
			ad.setPositiveButton(android.R.string.ok,null);
			ad.setTitle(R.string.app_name);
			ad.create();
			ad.show();
		}
		else if(code==JsonFeedGetter.CODE_JSONERROR){
			AlertDialog.Builder ad = new AlertDialog.Builder(this);
			ad.setMessage(R.string.jsonErrorMsg);
			ad.setPositiveButton(android.R.string.ok,null);
			ad.setTitle(R.string.app_name);
			ad.create();
			ad.show();
		}
		else if(code==JsonFeedGetter.CODE_NORESULT){
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

	/**
	 * onSearchRequested handler.
	 */
	@Override
	public boolean onSearchRequested() {
		Bundle appData = new Bundle();
		startSearch( null, false, appData, false);
		return true;
	}

	/**
	 * query for a place main routine.
	 */
	private void doSearchQuery(final Intent queryIntent) {
		final String queryString = queryIntent.getStringExtra(SearchManager.QUERY);
		SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this, 
				PhotSpotSearchSuggestProvider.AUTHORITY, PhotSpotSearchSuggestProvider.MODE);
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
				e.printStackTrace();
			}
	}
	
	/**
	 * timer task for updating favorite overlay
	 */
	private Runnable favOverlayUpdateTask_ = new Runnable() {
		public void run() {
			long current = SystemClock.uptimeMillis();
			mapView_.invalidate();
			favUpdateTimer_.postAtTime(this,current+1000);
		}
	};
	
	/**
	 * Photo Feed Retrieving AsyncTask class
	 * @author Huan Erdao
	 */
	private class GetPhotoFeedTask extends AsyncTask<String, Integer, Integer> {
		/** JSON feed getter utility */
		JsonFeedGetter getter_;
		/** Context object */
		Context context_;

		/**
		 * @param c	Context object
		 */
		public GetPhotoFeedTask(Context c) {
			context_ = c;
			getter_ = new JsonFeedGetter(context_,JsonFeedGetter.MODE_SPOTSEARCH);
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
			if(code!=JsonFeedGetter.CODE_HTTPERROR){
				List<PhotoItem> photoItems = getter_.getPhotoItemList();
				mapOverlays_ = mapView_.getOverlays();
				mapOverlays_.clear();
				if(myLocationOverlay_!=null&&mylocationEnabled_){
					mapOverlays_.add(myLocationOverlay_);
				}
				FrameLayout imageFrame = (FrameLayout)findViewById(R.id.imageframe);
				setupMarkerIcons();
				clusterer_ = new PhotSpotClusterer(me_,markerIconBmps_,screenDensity_,mapView_,imageFrame);
				for(int i=0; i<photoItems.size(); i++) {
					PhotoItem item = photoItems.get(i);
					clusterer_.addItem(item);
				}
				clusterer_.redraw();
				mapView_.invalidate();
			}
		}
		
		/**
		 * onCancel handler.
		 */
		@Override
		protected void onCancelled() {
		}
	};

	/**
	 * Class for overlaying Favorite item on the map
	 * @author Huan Erdao
	 */
	public class FavoriteOverlay extends Overlay {
		/** PhotoItem object */
		private final PhotoItem item_;
		/** marker frame object */
		private final Drawable frame_;
		/** thumbnail object */
		private final Bitmap thumbnail_;
		/** Frame Rect object */
		private final Rect frmRect_;
		/** Thumbnail Rect object */
		private final Rect thmRect_;
		/** Context object */
		private final Context context_;
		/** Paint object for messages */
		private Paint paint_;
		/** Title string */
		private String title_;
		/** sub title string */
		private String subtitle_;
		/** screenDensity */
		private float screenDensity_ = 1.0f;
		
		/** Extra Action - Navigate to Place */
		private static final int EXT_ACTION_NAVTOPLACE		= 0;
		/** Extra Action - Open with Browser */
		private static final int EXT_ACTION_OPENBROWSER		= 1;

		private static final int THUMBPADDING				= 6;
		private static final int THUMBSIZE					= 57;
		
		/**
		 * @param c		Context object
		 * @param item	PhotoItem object
		 */
		public FavoriteOverlay(Context c, PhotoItem item) { 
			context_ = c;
			screenDensity_ = context_.getResources().getDisplayMetrics().density;
			item_ = item;
			frame_ = c.getResources().getDrawable(R.drawable.balloon_fv);
			thumbnail_ = item.getBitmap();
			final int frmWidth = frame_.getIntrinsicWidth();
			final int frmHeight = frame_.getIntrinsicHeight();
			frame_.setBounds(0, 0, frmWidth, frmHeight);
			int ltx = -(frmWidth/2);
			int lty = -frmHeight;
			frmRect_ = new Rect(ltx,lty,ltx+frmWidth,lty+frmHeight);
			int thmbWidth = (int)(THUMBSIZE*screenDensity_+0.5f);
			int thmbHeight= (int)(THUMBSIZE*screenDensity_+0.5f);
			ltx += (int)(THUMBPADDING*screenDensity_+0.5f);
			lty += (int)(THUMBPADDING*screenDensity_+0.5f);
			thmRect_ = new Rect(ltx,lty,ltx+thmbWidth,lty+thmbHeight);
			final int txtareaWidth = frmRect_.width()-(int)(thmRect_.width()+THUMBPADDING*screenDensity_+0.5f);
			paint_ = new Paint();
			paint_.setAntiAlias(true);
			paint_.setTextSize(14*screenDensity_);
			paint_.setTypeface(Typeface.DEFAULT_BOLD);
			title_ = item_.getTitle();
			int breaktxt = paint_.breakText(title_, true, txtareaWidth, null);
			if(title_.length()>breaktxt)
				title_ = title_.substring(0, breaktxt-1);
			subtitle_ = item_.getAuthor();
			breaktxt = paint_.breakText(subtitle_, true, txtareaWidth, null);
			if(subtitle_.length()>breaktxt)
				subtitle_ = subtitle_.substring(0, breaktxt-1);
			subtitle_ = "by: "+subtitle_;
		}

		/**
		 * onTap event handler
		 */
		@Override
		public boolean onTap(GeoPoint p, MapView mapView){
			Projection pro = mapView.getProjection();
			Point ct = pro.toPixels(item_.getLocation(), null);
			Point pt = pro.toPixels(p, null);
			/* check if this marker was tapped */
			if( pt.x > ct.x-frmRect_.width()/2 && pt.x < ct.x+frmRect_.width()/2 && pt.y > ct.y-frmRect_.height()/2 && pt.y < ct.y+frmRect_.height()/2 ){
				new AlertDialog.Builder(context_)
				.setTitle(R.string.ExtActionDlgTitle)
				.setPositiveButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						dialog.dismiss();
					}
				})
				.setItems(R.array.showmap_extaction, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						onItemAction(which);
						dialog.dismiss();
					}
				})
			   .create()
			   .show();
				return true;
			}
			return false;
		}

		/**
		 * draw Tap event handler. draw overlay
		 */
		@Override
		public void draw(Canvas canvas, MapView mapView, boolean shadow) {
			if (!shadow) {
				Point p = new Point();
				Projection proj = mapView.getProjection();
				proj.toPixels(item_.getLocation(), p);
				super.draw(canvas, mapView, shadow);
				drawAt(canvas, frame_, p.x+frmRect_.left, p.y+frmRect_.top, shadow);
				int l = p.x+thmRect_.left;
				int t = p.y+thmRect_.top;
				int r = l + thmRect_.width();
				int b = t + thmRect_.height();
				canvas.drawBitmap(thumbnail_, null , new Rect(l,t,r,b),paint_);
				paint_.setTextSize(14*screenDensity_);
				paint_.setTypeface(Typeface.DEFAULT_BOLD);
				int x = r+(int)(THUMBPADDING*screenDensity_+0.5f);
				int y = (int) (t+paint_.getTextSize()+3);
				canvas.drawText(title_,x,y,paint_);
				paint_.setTextSize(11*screenDensity_);
				paint_.setTypeface(Typeface.DEFAULT);
				y += (int) (paint_.getTextSize()+4);
				canvas.drawText(subtitle_,x,y,paint_);
				if(myLocationOverlay_!=null){
					GeoPoint myLoc = myLocationOverlay_.getMyLocation();
					if(myLoc!=null){
						GeoPoint ovLoc = item_.getLocation();
						final int EARTH_RADIUS_KM = 6371;
						double myLatRad = Math.toRadians(myLoc.getLatitudeE6()/(double)1E6);
						double ovLatRad = Math.toRadians(ovLoc.getLatitudeE6()/(double)1E6);
						double deltaLonRad = Math.toRadians((ovLoc.getLongitudeE6()-myLoc.getLongitudeE6())/(double)1E6);
						double distKm = Math.acos(Math.sin(myLatRad) * Math.sin(ovLatRad) + Math.cos(myLatRad) * Math.cos(ovLatRad)
								* Math.cos(deltaLonRad))
								* EARTH_RADIUS_KM;
						y += (int) (paint_.getTextSize()+4);
						String distMsg = String.format(":%6.2f[km]", distKm);
						canvas.drawText(getString(R.string.Distance)+distMsg,x,y,paint_);
					}
				}
			}
		}

		/**
		 * Extra Action handler
		 * @param cmd action command
		 * @param item PhotoItem object
		 */
		public void onItemAction(int cmd){
			switch(cmd){
				case EXT_ACTION_OPENBROWSER:{
					String url = item_.getOriginalUrl();
					Intent i = new Intent(Intent.ACTION_VIEW,Uri.parse(url));
					context_.startActivity(i);
					break;
				}
				case EXT_ACTION_NAVTOPLACE:{
					GeoPoint location = item_.getLocation();
					double lat = location.getLatitudeE6()/1E6;
					double lng = location.getLongitudeE6()/1E6;
					GeoPoint gp_saddr = null;
					if(myLocationOverlay_!=null)
						gp_saddr = myLocationOverlay_.getMyLocation();
					String url = "http://maps.google.com/maps?daddr="+lat+","+lng;
					if(gp_saddr!=null){
						url += ( "&saddr=" + gp_saddr.getLatitudeE6()/1E6 + "," + gp_saddr.getLongitudeE6()/1E6 );
					}
					Intent i = new Intent(Intent.ACTION_VIEW,Uri.parse(url));
					i.addCategory(Intent.CATEGORY_BROWSABLE);
					i.setFlags(0x2800000);
					context_.startActivity(i);
					break;
				}
			}
		}
	};

}