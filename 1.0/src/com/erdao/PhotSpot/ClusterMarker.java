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

import java.util.ArrayList;

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

	/* constructor */
	public ClusterMarker(GeoCluster cluster, MapView mapView, Context context, FrameLayout imageFrame) {
		cluster_ = cluster;
		mapView_ = mapView;
		context_ = context;
		imageFrame_ = imageFrame;
		center_ = cluster_.getLocation();
		photoItems_ = cluster_.getItems();
		selItem_ = 0;
		/* check if we have selected item in cluster */
		for(int i=0; i<photoItems_.size(); i++) {
			if(photoItems_.get(i).isSelected()) {
				selItem_ = i;
				isSelected_ = true;
			}
		}
		loadMarkerBitmap();
		paint_ = new Paint();
		paint_.setStyle(Paint.Style.STROKE);
		paint_.setColor(Color.RED);
		paint_.setTextSize(12);
		paint_.setTypeface(Typeface.DEFAULT_BOLD);
	}
	
	/* load&change bitmap for cluster marker */
	private void loadMarkerBitmap(){
		if(photoItems_.size()>sizeThresh_){
			if( isSelected_ ){
				bmp_ = BitmapFactory.decodeResource(context_.getResources(), R.drawable.balloon_l_s);
			}else{
				bmp_ = BitmapFactory.decodeResource(context_.getResources(), R.drawable.balloon_l);
			}
			bmpSize_ = new Point(48,40);
			balloonGrid_ = new Point(43,37);
		}else{
			if( isSelected_ ){
				bmp_ = BitmapFactory.decodeResource(context_.getResources(), R.drawable.balloon_s_s);
			}else{
				bmp_ = BitmapFactory.decodeResource(context_.getResources(), R.drawable.balloon_s);
			}
			bmpSize_ = new Point(38,32);
			balloonGrid_ = new Point(34,30);
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
		int x = p.x-bmpSize_.x/2-(caption.length()-1)*2;
		int y = p.y-bmpSize_.y/2+4;
		canvas.drawText(caption,x,y,paint_);
	}
	
	/* onTap */
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
				.setItems(R.array.thumbnail_action, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						onThumbAction(which);
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
			case 0:{
				clearSelect();
				String url = photoItems_.get(selItem_).getPhotoUrl();
				//Log.i("DEBUG","Openurl="+url);
				Intent i = new Intent(Intent.ACTION_VIEW,Uri.parse(url));
				context_.startActivity(i);
				break;
			}
			case 1:{
				MapController mapCtrl = mapView_.getController();
				GeoPoint location = photoItems_.get(selItem_).getLocation();
				Projection pro = mapView_.getProjection();
				Point pt = pro.toPixels(location, null);
				mapCtrl.zoomInFixing(pt.x, pt.y);
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

}
