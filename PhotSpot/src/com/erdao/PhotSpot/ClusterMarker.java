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
import java.util.Locale;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
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
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;

import com.erdao.PhotSpot.GeoClusterer.GeoCluster;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
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
	private final ArrayList<PhotoItem> photoItems_;
	private Bitmap bmp_;
	private ImageAdapter imgAdapter_;
	private Point balloonGrid_;
	private Point bmpSize_;
	private final int sizeThresh_ = 10;
	private boolean isSelected_ = false;
	private long tapCheckTime_;
	private int selItem_;
	private ArrayList<CharSequence> localSpots_;

	private static final int EXT_ACTION_OPENBROWSER = 0;
	private static final int EXT_ACTION_ZOOM_IN= 1;
	private static final int EXT_ACTION_WHATS_HERE = 2;
	private static final int EXT_ACTION_NAVTOPLACE = 3;
	
	/* constructor */
	public ClusterMarker(GeoCluster cluster, MapView mapView, Context context, FrameLayout imageFrame) {
		cluster_ = cluster;
		mapView_ = mapView;
		context_ = context;
		imageFrame_ = imageFrame;
		center_ = cluster_.getLocation();
		photoItems_ = cluster_.getItems();
		selItem_ = 0;
		paint_ = new Paint();
		paint_.setStyle(Paint.Style.STROKE);
		paint_.setColor(Color.WHITE);
		paint_.setTextSize(15);
		paint_.setTypeface(Typeface.DEFAULT_BOLD);
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
		imgAdapter_ = new ImageAdapter(context_, photoItems_);
		Gallery gallery = (Gallery)imageFrame_.findViewById(R.id.gallery);
		gallery.setAdapter(imgAdapter_);
		gallery.setCallbackDuringFling(true);
		TextView txtView = (TextView)imageFrame_.findViewById(R.id.imgdesc);
		txtView.setText(imgAdapter_.getDescription(selItem_));
		photoItems_.get(selItem_).setSelect();
		gallery.setSelection(selItem_);
		Button button = (Button)imageFrame_.findViewById(R.id.closebtn);
		button.setOnClickListener(new OnClickListener() {
			public void onClick(View v){
				hideImageFrame();
				clearSelect();
			}
		});
		Animation anim = AnimationUtils.loadAnimation(context_, R.anim.stretch);
		gallery.startAnimation(anim);
		showImageFrame();
		gallery.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected (AdapterView<?> parent, View view, int position, long id){
				TextView txtView = (TextView)imageFrame_.findViewById(R.id.imgdesc);
				txtView.setText(imgAdapter_.getDescription(position));
				photoItems_.get(selItem_).clearSelect();
				photoItems_.get(position).setSelect();
				selItem_ = position;
			}
			public void onNothingSelected (AdapterView<?> parent){
			}
		});
		gallery.setOnItemLongClickListener(new OnItemLongClickListener() {
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				new AlertDialog.Builder(context_)
				.setTitle(R.string.ThumbActionDlg)
				.setPositiveButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						dialog.dismiss();
					}
				})
				.setItems(R.array.thumbnail_action, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						onThumbAction(which);
						dialog.dismiss();
					}
				})
			   .create()
			   .show();
				return true;
			}
		});
	}
	
	/* option tasks for long pressing thumbnail */
	public void onThumbAction(int cmd){
		switch(cmd){
			case EXT_ACTION_OPENBROWSER:{
				clearSelect();
				String url = photoItems_.get(selItem_).getPhotoUrl();
				Intent i = new Intent(Intent.ACTION_VIEW,Uri.parse(url));
				context_.startActivity(i);
				break;
			}
			case EXT_ACTION_ZOOM_IN:{
				MapController mapCtrl = mapView_.getController();
				GeoPoint location = photoItems_.get(selItem_).getLocation();
				Projection pro = mapView_.getProjection();
				Point pt = pro.toPixels(location, null);
				mapCtrl.zoomInFixing(pt.x, pt.y);
				break;
			}
			case EXT_ACTION_WHATS_HERE:{
				GeoPoint location = photoItems_.get(selItem_).getLocation();
				double lat = location.getLatitudeE6()/1E6;
				double lng = location.getLongitudeE6()/1E6;
				String debugstr = Locale.getDefault().getDisplayName()+","+Build.MODEL+","+Build.VERSION.RELEASE;
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
				GeoPoint location = photoItems_.get(selItem_).getLocation();
				double lat = location.getLatitudeE6()/1E6;
				double lng = location.getLongitudeE6()/1E6;
				String url = "http://maps.google.com/maps?daddr="+lat+","+lng;
				Intent i = new Intent(Intent.ACTION_VIEW,Uri.parse(url));
				i.addCategory(Intent.CATEGORY_BROWSABLE);
				i.setFlags(0x2800000);
				context_.startActivity(i);
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

	/* Image Adapter class for gallery */
	public class ImageAdapter extends BaseAdapter {
		/* variables */
		int galleryItemBackground_;
		private final Context context_;
		private final ArrayList<PhotoItem> photoItems_;
		private ArrayList<Bitmap> bitmaps_ = new ArrayList<Bitmap>();

		/* constructor */
		public ImageAdapter(Context c, ArrayList<PhotoItem> list) {
			context_ = c;
			photoItems_ = list;
			for(int i=0; i< list.size();i++)
				bitmaps_.add(null);
			TypedArray a = context_.obtainStyledAttributes(R.styleable.ThumbGallery);
			galleryItemBackground_ = a.getResourceId(
					R.styleable.ThumbGallery_android_galleryItemBackground, 0);
			a.recycle();
		}

		/* getCount */
		public int getCount() {
			return photoItems_.size();
		}

		/* getItem */
		public Object getItem(int position) {
			return photoItems_.get(position);
		}

		/* getItemId */
		public long getItemId(int position) {
			return position;
		}

		/* getBitmap */
		public Bitmap getBitmap(int position) {
			return bitmaps_.get(position);
		}

		/* getDescription */
		public String getDescription(int position) {
			PhotoItem item = photoItems_.get(position);
			String desc = item.getTitle();
			if(item.getOwner()!=null){
				desc += " (by "+item.getOwner()+")";
			}
			return desc;
		}
		
		/* getView */
		public View getView(int position, View convertView, ViewGroup parent) {
			ImageView imgView = new ImageView(context_);
			Bitmap bmp = bitmaps_.get(position);
			if( bmp == null ){
				imgView.setImageResource(R.drawable.imgloading);
				Handler handler = new Handler();
				PhotoItem item = photoItems_.get(position);
				new BitmapLoadThread(handler,position,item.getThumbUrl()).start();
			}
			else{
				imgView.setImageBitmap(bmp);
			}
			imgView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
			imgView.setLayoutParams(new Gallery.LayoutParams(160, 120));
			imgView.setBackgroundResource(galleryItemBackground_);		 
			return imgView;
		}

		/* Thread Class to load Bitmap */
		private class BitmapLoadThread extends Thread {
			private int pos_;
			private String url_;
			private Handler handler_;
			public BitmapLoadThread(Handler handler, int position, String url ){
				pos_ = position;
				url_ = url;
				handler_ = handler;
			}
			@Override
			public void run() {
				Bitmap bmp = BitmapUtils.loadBitmap(url_);
				bitmaps_.set(pos_,bmp);
				handler_.post(new Runnable() {
					public void run() {
						notifyDataSetChanged();
					}
				});
			}
		}
	};

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
					Toast.makeText(context_, R.string.ToastLocalSearchFail, Toast.LENGTH_SHORT);
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
