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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Typeface;
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

import com.erdao.PhotSpot.GeoClusterer.GeoCluster;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

/* Cluster Marker class */
public class ClusterMarker extends Overlay {
	/* variables */
	private final Context context_;
	private final GeoCluster cluster_;
	private final MapView mapView_;
	private final FrameLayout imageFrame_;
	private final GeoPoint center_;
	private final Paint paint_;
	private final List<PhotoItem> photoItems_;
	private List<Bitmap> bitmaps_ = new ArrayList<Bitmap>();
	private Bitmap bmp_;
	private ImageListAdapter imageAdapter_;
	private Point balloonGrid_;
	private Point bmpSize_;
	private final int sizeThresh_ = 10;
	private boolean isSelected_ = false;
	private long tapCheckTime_;
	private int selItem_;
	private List<CharSequence> localSpots_;
	private PhotSpotActivity activityHndl_;
	private Point galleryActionPt_ = new Point();
	private int gallery_width_;
	private int gallery_height_;
	private static final int GALLERY_DEFAULT_WIDTH = 160;
	private static final int GALLERY_DEFAULT_HEIGHT = 120;
	private boolean onGalleryGesture_;

	private static final int EXT_ACTION_ADD_FAVORITES	= 0;
	private static final int EXT_ACTION_WHATS_HERE		= 1;
	private static final int EXT_ACTION_NAVTOPLACE		= 2;
	private static final int EXT_ACTION_OPENBROWSER		= 3;
	
	/* constructor */
	public ClusterMarker(PhotSpotActivity activityHndl, GeoCluster cluster, MapView mapView, Context context, FrameLayout imageFrame) {
		activityHndl_ = activityHndl;
		cluster_ = cluster;
		mapView_ = mapView;
		context_ = context;
		imageFrame_ = imageFrame;
		center_ = cluster_.getLocation();
		photoItems_ = cluster_.getItems();
		for(int i=0; i< photoItems_.size();i++)
			bitmaps_.add(null);
		selItem_ = 0;
		paint_ = new Paint();
		paint_.setStyle(Paint.Style.STROKE);
		paint_.setColor(Color.WHITE);
		paint_.setTextSize(15);
		paint_.setTypeface(Typeface.DEFAULT_BOLD);
		onGalleryGesture_ = false;
		/* check if we have selected item in cluster */
		for(int i=0; i<photoItems_.size(); i++) {
			if(photoItems_.get(i).isSelected()) {
				selItem_ = i;
				isSelected_ = true;
			}
		}
		loadMarkerBitmap();
	}
	
	/* load&change bitmap for cluster marker */
	private void loadMarkerBitmap(){
		if(photoItems_.size()>sizeThresh_){
			if( isSelected_ ){
				bmp_ = BitmapFactory.decodeResource(context_.getResources(), R.drawable.balloon_l_s);
			}else{
				bmp_ = BitmapFactory.decodeResource(context_.getResources(), R.drawable.balloon_l);
			}
			bmpSize_ = new Point(56,56);
			balloonGrid_ = new Point(28,28);
			paint_.setTextSize(16);
		}else{
			if( isSelected_ ){
				bmp_ = BitmapFactory.decodeResource(context_.getResources(), R.drawable.balloon_s_s);
			}else{
				bmp_ = BitmapFactory.decodeResource(context_.getResources(), R.drawable.balloon_s);
			}
			bmpSize_ = new Point(40,40);
			balloonGrid_ = new Point(20,20);
			paint_.setTextSize(14);
		}
	}

	/* draw marker icon */
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		cluster_.onNotifyDraw();
		Projection proj = mapView.getProjection();
		Point p = proj.toPixels(center_, null);
		if( p.x < 0 || p.x > mapView.getWidth() || p.y < 0 || p.y > mapView.getHeight() )
			return;
		canvas.drawBitmap(bmp_, p.x-balloonGrid_.x, p.y-balloonGrid_.y, null);
		String caption = String.valueOf(photoItems_.size());
		int x = p.x-caption.length()*4;
		int y = p.y+5;
		canvas.drawText(caption,x,y,paint_);
	}
	
	/* onTouchEvent */
	@Override
    public boolean onTap(GeoPoint p, MapView mapView){
		Projection pro = mapView.getProjection();
		Point ct = pro.toPixels(center_, null);
		Point pt = pro.toPixels(p, null);
		/* check if this marker was tapped */
		if( pt.x > ct.x-balloonGrid_.x && pt.x < ct.x+(bmpSize_.x-balloonGrid_.x) && pt.y > ct.y-balloonGrid_.y && pt.y < ct.y+(bmpSize_.y-balloonGrid_.y) ){
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
			loadMarkerBitmap();
			cluster_.onTap(true);
			showGallery();
			tapCheckTime_ = SystemClock.uptimeMillis();
			return true;
		}
		cluster_.onTap(false);
		return false;
	}
	
	/* show Gallery View */
	public void showGallery(){
		imageAdapter_ = new ImageListAdapter(context_, photoItems_, bitmaps_);
		Gallery gallery = (Gallery)imageFrame_.findViewById(R.id.gallery);
		gallery.setAdapter(imageAdapter_);
		gallery.setCallbackDuringFling(true);
		TextView txtView = (TextView)imageFrame_.findViewById(R.id.imgdesc);
		txtView.setText(imageAdapter_.getDescription(selItem_));
		photoItems_.get(selItem_).setSelect();
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
					int diffX = posX-galleryActionPt_.x;
					int diffY = posY-galleryActionPt_.y;
					galleryActionPt_.x = posX;
					galleryActionPt_.y = posY;
					/* if swipe to the edge then hide the gallery */
					if(posY<0){
						hideImageFrame();
						clearSelect();
						showZoomButtons();
						return true;
					}
					/* if x move is large then assume swiping left or right */
					if(diffX > 25 || diffX < -25)
						return false;
					/* if y move is small then assume as click */
					if(diffY < 10 && diffY > -10)
						return false;
					onGalleryGesture_ = true;
					gallery_height_ += (int)(diffY*1.0);
					gallery_width_ = (int)(gallery_height_/3.0*4.0);
					if( gallery_height_ > (mainframe.getHeight()-30) ){
						gallery_height_ = (mainframe.getHeight()-30);
						gallery_width_ = (int)(gallery_height_/3.0*4.0);
					}
					else if( gallery_width_ < GALLERY_DEFAULT_WIDTH ){
						gallery_width_ = GALLERY_DEFAULT_WIDTH;
						gallery_height_ = GALLERY_DEFAULT_HEIGHT;
					}
					if( gallery_width_ > mainframe.getWidth() )
						gallery_width_ = mainframe.getWidth();
					if(gallery_height_ == (mainframe.getHeight()-30))
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
				PhotoItem prev = photoItems_.get(selItem_);
				PhotoItem curr = photoItems_.get(position);
				TextView txtView = (TextView)imageFrame_.findViewById(R.id.imgdesc);
				txtView.setText(imageAdapter_.getDescription(position));
				prev.clearSelect();
				curr.setSelect();
				selItem_ = position;
			}
			public void onNothingSelected (AdapterView<?> parent){
			}
		});
		gallery.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				new AlertDialog.Builder(context_)
				.setTitle(R.string.ExtActionDlg)
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
	
	/* option tasks for long pressing thumbnail */
	public void onItemAction(int cmd){
		switch(cmd){
			case EXT_ACTION_OPENBROWSER:{
				clearSelect();
				String url = photoItems_.get(selItem_).getPhotoUrl();
				Intent i = new Intent(Intent.ACTION_VIEW,Uri.parse(url));
				context_.startActivity(i);
				break;
			}
			case EXT_ACTION_WHATS_HERE:{
				GeoPoint location = photoItems_.get(selItem_).getLocation();
				double lat = location.getLatitudeE6()/1E6;
				double lng = location.getLongitudeE6()/1E6;
				String debugstr = Locale.getDefault().getDisplayName()+","+Build.MODEL+","+Build.VERSION.RELEASE+","+context_.getString(R.string.app_ver);
				try {
					debugstr = URLEncoder.encode(debugstr,"UTF-8");
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				String uri = "http://photspotcloud.appspot.com/photspotcloud?q=localsearch&latlng="+lat+","+lng+"&dbg="+debugstr;
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
				GeoPoint location = photoItems_.get(selItem_).getLocation();
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
				PhotoItem item = photoItems_.get(selItem_);
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

	/* showImageFrame */
	public void showImageFrame(){
		if(imageFrame_.getVisibility() != View.VISIBLE){
			imageFrame_.setVisibility(View.VISIBLE);
		}
	}

	/* hideImageFrame */
	public void hideImageFrame(){
		imageFrame_.setVisibility(View.GONE);
	}

	/* hideZoomButtons */
	public void hideZoomButtons(){
		Button zoBtn = (Button)activityHndl_.findViewById(R.id.zoominbtn);
		zoBtn.setVisibility(View.GONE);
		Button ziBtn = (Button)activityHndl_.findViewById(R.id.zoomoutbtn);
		ziBtn.setVisibility(View.GONE);
	}

	/* showZoomButtons */
	public void showZoomButtons(){
		Button zoBtn = (Button)activityHndl_.findViewById(R.id.zoominbtn);
		zoBtn.setVisibility(View.VISIBLE);
		Button ziBtn = (Button)activityHndl_.findViewById(R.id.zoomoutbtn);
		ziBtn.setVisibility(View.VISIBLE);
	}

	/* isSelected */
	public boolean isSelected(){
		return isSelected_;
	}
	
	/* clearSelect */
	public void clearSelect(){
		isSelected_ = false;
		photoItems_.get(selItem_).clearSelect();
		loadMarkerBitmap();
	}

	/* getLocation */
	public GeoPoint getLocation(){
		return center_;
	}

	/* getSelectedItemLocation */
	public GeoPoint getSelectedItemLocation(){
		return photoItems_.get(selItem_).getLocation();
	}

	/* GetPhotoFeedTask - AsyncTask */
	private class LocalSearchTask extends AsyncTask<String, Integer, Integer> {
		JsonFeedGetter getter_;
		Context context_;
		/* constructor */
		public LocalSearchTask(Context c) {
			context_ = c;
			getter_ = new JsonFeedGetter(JsonFeedGetter.MODE_LOCALSEARCH,context_);
		}
		/* doInBackground */
		@Override
		protected Integer doInBackground(String... uris) {
			return getter_.getFeed(uris[0]);
		}

		/* onPostExecute */
		@Override
		protected void onPostExecute(Integer code) {
			if(code!=JsonFeedGetter.CODE_HTTPERROR){
				localSpots_ = getter_.getLocalSpotsList();
				if(localSpots_.size()==0)
					activityHndl_.ToastMessage(R.string.ToastLocalSearchFail, Toast.LENGTH_SHORT);
				else{
					CharSequence[] arry = (CharSequence[])localSpots_.toArray(new CharSequence[0]);
					new AlertDialog.Builder(context_)
					.setTitle(R.string.LocalSearchDlg)
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
	
		/* onCancelled */
		@Override
		protected void onCancelled() {
		}
	};
	
}
