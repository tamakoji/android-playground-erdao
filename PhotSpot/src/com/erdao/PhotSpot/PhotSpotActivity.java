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
import android.graphics.Canvas;
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

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

/* Main Activity Class for PhotSpot */
public class PhotSpotActivity extends MapActivity {

	private PhotSpotActivity me_;
	private Context context_;

	/* google map variables */
	private MapView mapView_;
	private MapController mapCtrl_;
	private LocationManager locationMgr_;
	private Criteria criteria_;
	private MyLocationOverlay myLocationOverlay_ = null;
	private List<Overlay> mapOverlays_;
	private boolean mylocationEnabled_;
	private boolean mylocationSetFocus_;
	private List<Address> addresses_;

	/* settings */
	private SharedPreferences settings_;
	private ScrollView msgFrame_;
	private TextView msgTxtView_;

	/* for mylocation thread */
	private Handler handler_;

	/* photo feed task */
	private GetPhotoFeedTask getPhotoFeedTask_;
	private int contentProvider_;

	/* Clusterer */
	private GeoClusterer clusterer_ = null;

	/* favorite ovelay */
	private boolean favoriteOverlayed_;
	private Handler favUpdateTimer_ = new Handler();
	
	/** Called when the activity is first created. */
	protected boolean isRouteDisplayed() {
		return false;
	}
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		me_ = this;
		context_ = this;
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);
		resetViews();
		handler_ = new Handler();

		/* Read preferences */
		settings_ = getSharedPreferences(getString(R.string.PhotSpotPreference), 0);

		onCreateMain();

		String version = settings_.getString(getString(R.string.PrefVersionInfo), "");
		boolean initialBoot = !version.equals(getString(R.string.app_ver));
		SharedPreferences.Editor editor = settings_.edit();
		editor.putString(getString(R.string.PrefVersionInfo),getString(R.string.app_ver));
		editor.commit();
		if(initialBoot){
			msgTxtView_ = new TextView(this);
			msgTxtView_.setTextSize(14);
			msgTxtView_.setText(R.string.InitialMessage);
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
			.setNegativeButton(R.string.menu_Help, new DialogInterface.OnClickListener() {
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
	
	private void onCreateMain(){
//		Log.i("DEBUG", "onCreateMain");
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

		/* Restore preferences */
		boolean mapmode = settings_.getBoolean(getString(R.string.PrefMapMode), false);
		mapView_.setSatellite(mapmode);
		contentProvider_ = settings_.getInt(getString(R.string.PrefContentProvider), 0);

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
			mylocationEnabled_ = true;
			mylocationSetFocus_ = false;
			favoriteOverlayed_ = true;
			PhotoItem item = intent.getParcelableExtra(PhotoItem.EXT_PHOTOITEM);
			mapCtrl_.animateTo(item.getLocation());
			final List<Overlay> overlays = mapView_.getOverlays();
			overlays.add(new ImageOverlay(this,item));
			mapView_.invalidate();
		}
		else{
			mylocationEnabled_ = true;
			mylocationSetFocus_ = true;
			favoriteOverlayed_ = false;
		}
        final Object data = getLastNonConfigurationInstance();
        if (data != null) {
        	final PhotoItem[] items = (PhotoItem[]) data;
			FrameLayout imageFrame = (FrameLayout)findViewById(R.id.imageframe);
			clusterer_ = new GeoClusterer(me_,context_, mapView_,imageFrame);
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

	/* for orientation */
	@Override
	public Object onRetainNonConfigurationInstance() {
		if(clusterer_!=null){
			List<PhotoItem> items = clusterer_.getPhotoItems();
			final PhotoItem[] list = new PhotoItem[items.size()];
			for(int i = 0; i<items.size();i++){
				list[i] = new PhotoItem(items.get(i));
			}
			return list;
		}
		return null;
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

	/* onStop */
	@Override 
	public void onStop() {
//		Log.i("DEBUG", "onStop"); 
		if(favoriteOverlayed_)
			favUpdateTimer_.removeCallbacks(favOverlayUpdateTask_);
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
			menu_SearchPhotSpot.setIcon(R.drawable.ic_menu_searchspot);
		}
		MenuItem menu_SearchPlace = menu.add(0,R.id.menu_SearchPlace,0,R.string.menu_SearchPlace);
		menu_SearchPlace.setIcon(R.drawable.ic_menu_searchplace);
		MenuItem menu_MyLocation = menu.add(0,R.id.menu_MyLocation,0,R.string.menu_MyLocation);
		menu_MyLocation.setIcon(R.drawable.ic_menu_mylocation);
		MenuItem menu_Favorites = menu.add(0,R.id.menu_Favorites,0,R.string.menu_Favorites);
		menu_Favorites.setIcon(android.R.drawable.star_big_on);
		MenuItem menu_Preferences = menu.add(0,R.id.menu_Preferences,0,R.string.menu_Preferences);
		menu_Preferences.setIcon(android.R.drawable.ic_menu_preferences);
		MenuItem menu_MapMode = menu.add(0,R.id.menu_MapMode,0,R.string.menu_MapMode);
		menu_MapMode.setIcon(android.R.drawable.ic_menu_mapmode);
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
				uri = "http://photspotcloud.appspot.com/photspotcloud?q=searchspot&nwlng="+nwlng+"&selat="+selat+"&nwlat="+nwlat+"&selng="+selng;
				String debugstr = Locale.getDefault().getDisplayName()+","+Build.MODEL+","+Build.VERSION.RELEASE+","+context_.getString(R.string.app_ver);
				try {
					debugstr = URLEncoder.encode(debugstr,"UTF-8");
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				switch( contentProvider_ ){
					case TypesService.Panoramio:
					default:{
							uri += "&svc=panoramio";
						break;
					}
					case TypesService.PicasaWeb:{
						uri += "&svc=picasa";
						break;
					}
					case TypesService.Flickr:{
						uri += "&svc=flickr";
						break;
					}
				}
				uri += "&dbg="+debugstr+",android";
//				Log.i("DEBUG",uri);
				favUpdateTimer_.removeCallbacks(favOverlayUpdateTask_);
				getPhotoFeedTask_ = new GetPhotoFeedTask(this);
				getPhotoFeedTask_.execute(uri);
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
				Intent i = new Intent(this, FavoritesActivity.class);
				startActivity(i);
				break;
			}
			case R.id.menu_Preferences:{
				showDialog(R.id.PreferencesDlg);
				break;
			}
			case R.id.menu_MapMode:{
				showDialog(R.id.MapModeDlg);
				break;
			}
			case R.id.menu_ClearSuggest:{
				clearSearchHistory();
				break;
			}
			case R.id.menu_Help:{
				favUpdateTimer_.removeCallbacks(favOverlayUpdateTask_);
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
						SharedPreferences.Editor editor = settings_.edit();
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
						SharedPreferences.Editor editor = settings_.edit();
						editor.putInt(getString(R.string.PrefContentProvider),contentProvider_);
						editor.commit();
						dismissDialog(R.id.PreferencesDlg);
					}
				})
				.create();
			}
			case R.id.AboutDlg:{
				msgTxtView_ = new TextView(this);
				msgTxtView_.setTextSize(14);
				msgTxtView_.setText(R.string.AboutDlgContent);
				msgTxtView_.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
				msgFrame_ = new ScrollView(this);
				msgFrame_.addView(msgTxtView_);
				return new AlertDialog.Builder(this)
				.setIcon(R.drawable.icon)
				.setTitle(R.string.AboutDlgTitle)
				.setView(msgFrame_)
				.setPositiveButton(R.string.Dlg_OK, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
					}
				})
				.create();
			}
		}
		return null;
	}

	/* Toast Message */
	public void ToastMessage(int messageId, int duration){
		Toast.makeText(this, messageId, duration).show();
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
	
	/* timer task for updating favorite overlay */
	private Runnable favOverlayUpdateTask_ = new Runnable() {
		public void run() {
			long current = SystemClock.uptimeMillis();
			mapView_.invalidate();
			favUpdateTimer_.postAtTime(this,current+1000);
		}
	};
	
	/* GetPhotoFeedTask - AsyncTask */
	private class GetPhotoFeedTask extends AsyncTask<String, Integer, Integer> {
		JsonFeedGetter getter_;
		Context context_;
		/* constructor */
		public GetPhotoFeedTask(Context c) {
			context_ = c;
			getter_ = new JsonFeedGetter(JsonFeedGetter.MODE_SPOTSEARCH,context_);
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
			if(code!=JsonFeedGetter.CODE_HTTPERROR){
				List<PhotoItem> photoItems = getter_.getPhotoItemList();
//				Log.i("DEBUG","result size:"+photoItems.size());
				mapOverlays_ = mapView_.getOverlays();
				mapOverlays_.clear();
				if(myLocationOverlay_!=null&&mylocationEnabled_){
					mapOverlays_.add(myLocationOverlay_);
				}
				FrameLayout imageFrame = (FrameLayout)findViewById(R.id.imageframe);
				clusterer_ = new GeoClusterer(me_,context_, mapView_,imageFrame);
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

	public class ImageOverlay extends Overlay {
		private final PhotoItem item_;
		private final Drawable frame_;
		private final Bitmap thumbnail_;
		private final Rect frmRect_;
		private final Rect thmRect_;
		private final Context context_;
		private Paint paint_;
		private String title_;
		private String subtitle_;
		
		private static final int EXT_ACTION_NAVTOPLACE		= 0;
		private static final int EXT_ACTION_OPENBROWSER		= 1;

		public ImageOverlay(Context c, PhotoItem item) { 
			context_ = c;
			item_ = item;
			frame_ = c.getResources().getDrawable(R.drawable.balloon_fv);
			thumbnail_ = item.getBitmap();
			final int frmWidth = frame_.getIntrinsicWidth();	// 75
			final int frmHeight = frame_.getIntrinsicHeight();	// 75
			frame_.setBounds(0, 0, frmWidth, frmHeight);
			int ltx = -(frmWidth/2);
			int lty = -frmHeight;
			frmRect_ = new Rect(ltx,lty,ltx+frmWidth,lty+frmHeight);
			int thmbWidth = 57;
			int thmbHeight= 57;
			ltx += 6;
			lty += 6;
			thmRect_ = new Rect(ltx,lty,ltx+thmbWidth,lty+thmbHeight);
			paint_ = new Paint();
			paint_.setAntiAlias(true);
			paint_.setTextSize(14);
			paint_.setTypeface(Typeface.DEFAULT_BOLD);
			title_ = item_.getTitle();
			if(title_.length()>25)
				title_ = title_.substring(0, 24);
			subtitle_ = item_.getAuthor();
			if(subtitle_.length()>25)
				subtitle_ = subtitle_.substring(0, 24);
			subtitle_ = "by: "+subtitle_;
		}

		/* onTouchEvent */
		@Override
		public boolean onTap(GeoPoint p, MapView mapView){
			Projection pro = mapView.getProjection();
			Point ct = pro.toPixels(item_.getLocation(), null);
			Point pt = pro.toPixels(p, null);
			/* check if this marker was tapped */
			if( pt.x > ct.x-frmRect_.width()/2 && pt.x < ct.x+frmRect_.width()/2 && pt.y > ct.y-frmRect_.height()/2 && pt.y < ct.y+frmRect_.height()/2 ){
				new AlertDialog.Builder(context_)
				.setTitle(R.string.ExtActionDlg)
				.setPositiveButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						dialog.dismiss();
					}
				})
				.setItems(R.array.showmap_extaction, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						onItemAction(which,item_);
						dialog.dismiss();
					}
				})
			   .create()
			   .show();
				return true;
			}
			return false;
		}

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
				paint_.setTextSize(14);
				paint_.setTypeface(Typeface.DEFAULT_BOLD);
				int x = r+5;
				int y = (int) (t+paint_.getTextSize()+3);
				canvas.drawText(title_,x,y,paint_);
				paint_.setTextSize(11);
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

		/* option tasks for long pressing item */
		public void onItemAction(int cmd, PhotoItem item){
			switch(cmd){
				case EXT_ACTION_OPENBROWSER:{
					String url = item.getPhotoUrl();
					Intent i = new Intent(Intent.ACTION_VIEW,Uri.parse(url));
					context_.startActivity(i);
					break;
				}
				case EXT_ACTION_NAVTOPLACE:{
					GeoPoint location = item.getLocation();
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