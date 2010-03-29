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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Gallery;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;

import com.erdao.PhotSpot.PhotSpotClusterer.PhotSpotGeoCluster;
import com.erdao.android.mapviewutil.GeoItem;
import com.erdao.android.mapviewutil.markerclusterer.ClusterMarker;
import com.erdao.android.mapviewutil.markerclusterer.MarkerBitmap;
import com.erdao.utils.LazyLoadBitmap;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

/**
 * Custom ClusterMarker class for PhotSpot.
 * adding gallery and extra actions.
 * @author Huan Erdao
 */
public class PhotSpotClusterMarker extends ClusterMarker {
	/** acvitity handle for posting ToastMessage. */
	private PhotSpotActivity activityHndl_;
	/** Context object. */
	private final Context context_;
	/** PhotSpotGeoCluster object. */
	private final PhotSpotGeoCluster cluster_;
	/** MapView object. */
	private final MapView mapView_;
	/** FrameLayout for gallery. */
	private final FrameLayout imageFrame_;
	/** PhotoItem list for gallery. */
	private List<PhotoItem> photoItems_ = new ArrayList<PhotoItem>();
	/** Bitmap list for gallery. */
	private List<LazyLoadBitmap> bitmaps_ = new ArrayList<LazyLoadBitmap>();
	/** check time object for tapping. */
	private long tapCheckTime_;

	/** ImageListAdapter for gallery. */
	private ImageListAdapter imageAdapter_;

	/** List of localsearch result. */
	private List<CharSequence> localSpots_;

	/** For Swipe gesture. action point latch */
	private Point galleryActionPt_ = new Point();
	/** flag to detect if it is gesture mode. */
	private boolean onGalleryGesture_;

	/** parameter for gallery width. */
	private int gallery_width_;
	/** parameter for gallery height. */
	private int gallery_height_;

	/** Extra Action constants. */
	private static final int EXT_ACTION_ADD_FAVORITES	= 0;
	/** Extra Action constants. */
	private static final int EXT_ACTION_WHATS_HERE		= 1;
	/** Extra Action constants. */
	private static final int EXT_ACTION_NAVTOPLACE		= 2;
	/** Extra Action constants. */
	private static final int EXT_ACTION_OPENBROWSER		= 3;
	
	/**
	 * @param activityHndl		acvitity handle 
	 * @param cluster			cluster object
	 * @param markerIconBmps	icon objects for markers
	 * @param mapView			MapView object
	 * @param imageFrame		FrameLayout object for gallery
	 */
	public PhotSpotClusterMarker(PhotSpotActivity activityHndl, PhotSpotGeoCluster cluster,
			List<MarkerBitmap> markerIconBmps, float screenDensity, MapView mapView, FrameLayout imageFrame) {
		super(cluster, markerIconBmps,screenDensity);
		activityHndl_ = activityHndl;
		context_ = (Context)activityHndl_;
		cluster_ = cluster;
		mapView_ = mapView;
		imageFrame_ = imageFrame;
		imageAdapter_ = null;
		onGalleryGesture_ = false;
		tapCheckTime_ = SystemClock.uptimeMillis();
	}
	
	/**
	 * clears selected state.
	 */
	@Override
	public void clearSelect(){
		super.clearSelect();
		for(int i = 0; i < bitmaps_.size(); i++){
			bitmaps_.get(i).recycle();
		}
	}
	
	/**
	 * Touch Event
	 * @param p					touched point.
	 * @param mapView			MapView object
	 * @return true if touch within marker icon.
	 */
	@Override
    public boolean onTap(GeoPoint p, MapView mapView){
		Projection pro = mapView.getProjection();
		Point ct = pro.toPixels(center_, null);
		Point pt = pro.toPixels(p, null);
		/* check if this marker was tapped */
		MarkerBitmap bmp = markerIconBmps_.get(markerTypes);
		Point grid = bmp.getGrid();
		Point bmpSize = bmp.getSize();
		if( pt.x > ct.x-grid.x && pt.x < ct.x+(bmpSize.x-grid.x) && pt.y > ct.y-grid.y && pt.y < ct.y+(bmpSize.y-grid.y) ){
			if(isSelected_){ 
				/* double tap */
				long curTime = SystemClock.uptimeMillis();
				if( curTime < tapCheckTime_+1000 ){ // if within 1sec
					//Log.i("DEBUG","DoubleTapped");
					MapController mapCtrl = mapView.getController();
					mapCtrl.zoomInFixing(ct.x, ct.y);
				}
				tapCheckTime_ = SystemClock.uptimeMillis();
				return false;
			}
			isSelected_ = true;
			setMarkerBitmap();
			cluster_.onTapCalledFromMarker(true);
			showGallery();
			tapCheckTime_ = SystemClock.uptimeMillis();
			return true;
		}
		cluster_.onTapCalledFromMarker(false);
		return false;
	}
	
	/**
	 * Show Gallery View.
	 */
	public void showGallery(){
		if(imageAdapter_ == null){
			for(int i=0; i< GeoItems_.size();i++){
				PhotoItem item = (PhotoItem)GeoItems_.get(i);
				photoItems_.add(item);
				bitmaps_.add(new LazyLoadBitmap());
			}
			imageAdapter_ = new ImageListAdapter(context_, photoItems_, bitmaps_);
		}
		Gallery gallery = (Gallery)imageFrame_.findViewById(R.id.gallery);
		gallery.setAdapter(imageAdapter_);
		gallery.setCallbackDuringFling(true);
		TextView txtView = (TextView)imageFrame_.findViewById(R.id.imgdesc);
		txtView.setText(imageAdapter_.getDescription(selItem_));
		GeoItems_.get(selItem_).setSelect(true);
		gallery.setSelection(selItem_);
		gallery.setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				if(event.getAction() == MotionEvent.ACTION_DOWN) {
					galleryActionPt_.x = (int)event.getX();
					galleryActionPt_.y = (int)event.getY();
				}
				else if(event.getAction() == MotionEvent.ACTION_UP){
					if( onGalleryGesture_ ){
						onGalleryGesture_ = false;
						return true;
					}
					return false;
				}
				else if(event.getAction() == MotionEvent.ACTION_MOVE){
					int posX = (int)event.getX();
					int posY = (int)event.getY();
					RelativeLayout mainframe = (RelativeLayout)activityHndl_.findViewById(R.id.mainframe);
					TextView imgdesc = (TextView)activityHndl_.findViewById(R.id.imgdesc);
					int mainWidth = mainframe.getWidth();
					int mainHeight = mainframe.getHeight();
					int footerHeight = imgdesc.getHeight();
					int diffX = posX-galleryActionPt_.x;
					int diffY = posY-galleryActionPt_.y;
					galleryActionPt_.x = posX;
					galleryActionPt_.y = posY;
					/* if swipe to the edge then hide the gallery */
					if(posY<0){
						hideImageFrame();
						cluster_.onNotifyClearSelectFromMarker();
						showZoomButtons();
						return true;
					}
					int SWIPE_LR_THRESH = (int)(screenDensity_*25+0.5f);
					int SWIPE_UD_THRESH = (int)(screenDensity_*10+0.5f);
					/* if x move is large then assume swiping left or right */
					if(Math.abs(diffX) > SWIPE_LR_THRESH )
						return false;
					/* if y move is small then assume as click */
					if(Math.abs(diffY) < SWIPE_UD_THRESH)
						return false;
					onGalleryGesture_ = true;
					gallery_height_ += (int)(diffY*1.0);
					gallery_width_ = (int)(gallery_height_/3.0*4.0);
					if( gallery_height_ > (mainHeight-footerHeight) ){
						gallery_height_ = (mainHeight-footerHeight);
						gallery_width_ = (int)(gallery_height_/3.0*4.0);
					}
					else if( gallery_width_ < imageAdapter_.getDefaultWidth() ){
						gallery_width_ = imageAdapter_.getDefaultWidth();
						gallery_height_ = imageAdapter_.getDefaultHeight();
					}
					if( gallery_width_ > mainWidth )
						gallery_width_ = mainWidth;
					if(gallery_height_ == (mainHeight-footerHeight))
						hideZoomButtons();
					else
						showZoomButtons();
					imageAdapter_.setSize(gallery_width_,gallery_height_);
					return true;
				}
				return false;
			}	
		});
		/*
		Button favBtn = (Button)imageFrame_.findViewById(R.id.favbtn);
		favBtn.setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				Button favBtn = (Button)imageFrame_.findViewById(R.id.favbtn);
				if(event.getAction() == MotionEvent.ACTION_DOWN) {
					favBtn.setBackgroundResource(R.drawable.gallery_btn_fav_psh);
				}
				else if(event.getAction() == MotionEvent.ACTION_UP){
					favBtn.setBackgroundResource(R.drawable.gallery_btn_fav_nrm);
					onItemAction(EXT_ACTION_ADD_FAVORITES);
				}
				return false;
			}	
		});
		*/
		Animation anim = AnimationUtils.loadAnimation(context_, R.anim.stretch);
		gallery.startAnimation(anim);
		showImageFrame();
		gallery.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected (AdapterView<?> parent, View view, int position, long id){
				GeoItem prev = GeoItems_.get(selItem_);
				GeoItem curr = GeoItems_.get(position);
				TextView txtView = (TextView)imageFrame_.findViewById(R.id.imgdesc);
				txtView.setText(imageAdapter_.getDescription(position));
				prev.setSelect(false);
				curr.setSelect(true);
				selItem_ = position;
			}
			public void onNothingSelected (AdapterView<?> parent){
			}
		});
		gallery.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				new AlertDialog.Builder(context_)
				.setTitle(R.string.ExtActionDlgTitle)
				.setPositiveButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						dialog.dismiss();
					}
				})
				.setItems(R.array.gallery_extaction, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						onItemAction(which);
						dialog.dismiss();
					}
				})
			   .create()
			   .show();
			}
		});
	}
	
	/**
	 * Extra Actions.
	 * @param cmd command for extra action
	 */
	public void onItemAction(int cmd){
		switch(cmd){
			case EXT_ACTION_OPENBROWSER:{
				//clearSelect();
				String url = ((PhotoItem)GeoItems_.get(selItem_)).getOriginalUrl();
				Intent i = new Intent(Intent.ACTION_VIEW,Uri.parse(url));
				context_.startActivity(i);
				break;
			}
			case EXT_ACTION_WHATS_HERE:{
				GeoPoint location = ((PhotoItem)GeoItems_.get(selItem_)).getLocation();
				double lat = location.getLatitudeE6()/1E6;
				double lng = location.getLongitudeE6()/1E6;
				String uri = context_.getString(R.string.photspotserver)+"/photspotcloud?q=localsearch&latlng="+lat+","+lng;
				uri += "&appver="+context_.getString(R.string.app_ver);
				String debugstr = Locale.getDefault().getDisplayName()+","+Build.MODEL+","+Build.VERSION.RELEASE+","+context_.getString(R.string.app_ver);
				try {
					debugstr = URLEncoder.encode(debugstr,"UTF-8");
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				uri += "&dbg="+debugstr;
				LocalSearchTask task = new LocalSearchTask(context_);
				task.execute(uri);
				break;
			}
			case EXT_ACTION_NAVTOPLACE:{
				GeoPoint gp_saddr = null;
				List<Overlay> overlays = mapView_.getOverlays();
				for(int i = 0; i < overlays.size(); i++) {
					Overlay overlay = overlays.get(i);
					if(overlay.getClass().equals(MyLocationOverlay.class)){
						gp_saddr = ((MyLocationOverlay)overlay).getMyLocation();
						break;
					}
				}
				GeoPoint location = ((PhotoItem)GeoItems_.get(selItem_)).getLocation();
				double lat = location.getLatitudeE6()/1E6;
				double lng = location.getLongitudeE6()/1E6;
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
			case EXT_ACTION_ADD_FAVORITES:{
				PhotoItem item = ((PhotoItem)GeoItems_.get(selItem_));
				/* Database for favorite */
				PhotSpotDBHelper dbHelper = new PhotSpotDBHelper(context_);
				if(dbHelper==null)
					break;
				int ret = dbHelper.insertItem(item,imageAdapter_.getBitmap(selItem_));
				if(ret == PhotSpotDBHelper.DB_SUCCESS)
					activityHndl_.ToastMessage(R.string.ToastSavedFavorites, Toast.LENGTH_SHORT);
				else if(ret == PhotSpotDBHelper.DB_EXISTS)
					activityHndl_.ToastMessage(R.string.ToastAlreadyExistFavorites, Toast.LENGTH_SHORT);
				else if(ret == PhotSpotDBHelper.DB_FULL)
					activityHndl_.ToastMessage(R.string.ToastFavoritesFull, Toast.LENGTH_SHORT);
				dbHelper.close();
				break;
			}
		}
	}

	/**
	 * Show Image Frame.
	 */
	public void showImageFrame(){
		if(imageFrame_.getVisibility() != View.VISIBLE){
			imageFrame_.setVisibility(View.VISIBLE);
		}
	}

	/**
	 * Hide Image Frame.
	 */
	public void hideImageFrame(){
		imageFrame_.setVisibility(View.GONE);
	}

	/**
	 * Show Zoom Button.
	 * called if the gallery view exits from full screen.
	 */
	public void showZoomButtons(){
		Button zoBtn = (Button)activityHndl_.findViewById(R.id.zoominbtn);
		zoBtn.setVisibility(View.VISIBLE);
		Button ziBtn = (Button)activityHndl_.findViewById(R.id.zoomoutbtn);
		ziBtn.setVisibility(View.VISIBLE);
	}

	/**
	 * Hide Zoom Button.
	 * called if the gallery view is in full screen.
	 */
	public void hideZoomButtons(){
		Button zoBtn = (Button)activityHndl_.findViewById(R.id.zoominbtn);
		zoBtn.setVisibility(View.GONE);
		Button ziBtn = (Button)activityHndl_.findViewById(R.id.zoomoutbtn);
		ziBtn.setVisibility(View.GONE);
	}


	/**
	 * Local Search Task. Retrieve JSON local search and display it.
	 */
	private class LocalSearchTask extends AsyncTask<String, Integer, Integer> {
		/** Json Utility object */
		private JsonFeedGetter getter_;
		/** Context */
		private Context context_;

		/**
		 * @param c Context object
		 */
		public LocalSearchTask(Context c) {
			context_ = c;
			getter_ = new JsonFeedGetter(context_,JsonFeedGetter.MODE_LOCALSEARCH);
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
			if(code!=JsonFeedGetter.CODE_HTTPERROR){
				localSpots_ = getter_.getLocalSpotsList();
				if(localSpots_.size()==0)
					activityHndl_.ToastMessage(R.string.ToastLocalSearchFail, Toast.LENGTH_SHORT);
				else{
					CharSequence[] arry = (CharSequence[])localSpots_.toArray(new CharSequence[0]);
					new AlertDialog.Builder(context_)
					.setTitle(R.string.LocalSearchDlgTitle)
					.setPositiveButton(R.string.Dlg_Close, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							dialog.dismiss();
						}
					})
					.setItems(arry, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							String place = (String) localSpots_.get(which);
							try {
								place = URLEncoder.encode(place,"UTF-8");
							} catch (UnsupportedEncodingException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							String url = "http://www.google.com/m/search?hl="+Locale.getDefault().getCountry()+"&q="+place;
							Intent i = new Intent(Intent.ACTION_VIEW,Uri.parse(url));
							context_.startActivity(i);
							dialog.dismiss();
						}
					})
				   .create()
				   .show();
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
