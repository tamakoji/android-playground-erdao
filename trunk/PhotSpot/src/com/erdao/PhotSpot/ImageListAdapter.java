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

import java.util.List;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;

import com.erdao.utils.BitmapUtilily;
import com.erdao.utils.LazyLoadBitmap;
import com.erdao.utils.LazyLoadBitmap.LoadState;

/**
 * ImageListAdapter Class for Gallery View in list.
 * @author Huan Erdao
 */
public class ImageListAdapter extends BaseAdapter {
	/** gallery background resource */
	int galleryItemBackground_;
	/** Context object */
	private final Context context_;
	/** PhotoItem list */
	private final List<PhotoItem> photoItems_;
	/** Bitmap list */
	private final List<LazyLoadBitmap> bitmaps_;
	/** Current position */
	private int curPos_;
	/** default gallery width. */
	private static final int GALLERY_DEFAULT_WIDTH = 160;
	/** default gallery height. */
	private static final int GALLERY_DEFAULT_HEIGHT = 120;
	/** image width */
	private int width_ = GALLERY_DEFAULT_WIDTH;
	/** image height */
	private int height_ = GALLERY_DEFAULT_HEIGHT;
	/** screen density */
	private float screenDensity_ = 1.0f;

	/**
	 * @param c				Context object
	 * @param photoItems 	PhotoItem list object
	 * @param bitmaps		Bitmap list object
	 */
	public ImageListAdapter(Context c, List<PhotoItem> photoItems, List<LazyLoadBitmap> bitmaps) {
		context_ = c;
		screenDensity_ = context_.getResources().getDisplayMetrics().density;
		width_ = getDefaultWidth();
		height_ = getDefaultHeight();
		photoItems_ = photoItems;
		bitmaps_ = bitmaps;
		curPos_ = 0;
		TypedArray a = context_.obtainStyledAttributes(R.styleable.ThumbGallery);
		galleryItemBackground_ = a.getResourceId(
				R.styleable.ThumbGallery_android_galleryItemBackground, 0);
		a.recycle();
	}

	/**
	 * @return	photoitem list size
	 */
	public int getCount() {
		return photoItems_.size();
	}

	/**
	 * @return	default gallery width
	 */
	public int getDefaultWidth(){
		return (int)(screenDensity_*GALLERY_DEFAULT_WIDTH+0.5f);
	}
	
	/**
	 * @return	default gallery height
	 */
	public int getDefaultHeight(){
		return (int)(screenDensity_*GALLERY_DEFAULT_HEIGHT+0.5f);
	}

	/**
	 * @param pos position of list
	 * @return	PhotoItem object.
	 */
	public Object getItem(int pos) {
		if(pos>photoItems_.size())
			return null;
		return photoItems_.get(pos);
	}

	/**
	 * @param pos position of list
	 * @return	position as id
	 */
	public long getItemId(int pos) {
		return pos;
	}

	/**
	 * @param pos position of list
	 * @return	Bitmap object
	 */
	public Bitmap getBitmap(int pos) {
		if(pos>bitmaps_.size())
			return null;
		return bitmaps_.get(pos).getBitmap();
	}

	/**
	 * @param pos position of list
	 * @return	Formatted description string.
	 */
	public String getDescription(int pos) {
		curPos_ = pos;
		if(pos>photoItems_.size())
			return null;
		PhotoItem item = photoItems_.get(pos);
		String desc = item.getTitle();
		if(item.getAuthor()!=null){
			desc += "\nby "+item.getAuthor();
		}
		return desc;
	}
	
	/**
	 * @param w		new Image width
	 * @param h		new Image height
	 */
	public void setSize(int w, int h){
		width_ = w;
		height_ = h;
		notifyDataSetChanged();			
	}
	
	/**
	 * getView
	 */
	public View getView(int pos, View convertView, ViewGroup parent) {
		ImageView imgView = new ImageView(context_);
		LazyLoadBitmap lzybmp = bitmaps_.get(pos);
		Bitmap bmp = lzybmp.getBitmap();
		int loadState = lzybmp.getState();
		if( loadState < LoadState.BITMAP_STATE_PRE_LOADING ){
			lzybmp.setState(LoadState.BITMAP_STATE_PRE_LOADING);
			bitmaps_.set(pos,lzybmp);
			imgView.setImageResource(R.drawable.imgloading);
			Handler handler = new Handler();
			PhotoItem item = photoItems_.get(pos);
			new BitmapLoadThread(handler,pos,item.getCompactThumbUrl(),true).start();
		}
		else if( loadState == LoadState.BITMAP_STATE_PRE_LOADING ){
			imgView.setImageResource(R.drawable.imgloading);
		}
		else if( loadState >= LoadState.BITMAP_STATE_PRE_LOADED && loadState < LoadState.BITMAP_STATE_FULL_LOADING ){
			lzybmp.setState(LoadState.BITMAP_STATE_FULL_LOADING);
			bitmaps_.set(pos,lzybmp);
			imgView.setImageBitmap(bmp);
			Handler handler = new Handler();
			PhotoItem item = photoItems_.get(pos);
			new BitmapLoadThread(handler,pos,item.getFullThumbUrl(),false).start();
		}
		else{
			imgView.setImageBitmap(bmp);
		}
		imgView.setScaleType(ImageView.ScaleType.FIT_CENTER);//.CENTER_INSIDE);
//		float scale = context_.getResources().getDisplayMetrics().density;
//		imgView.setLayoutParams(new Gallery.LayoutParams((int)(width_*scale+0.5f), (int)(height_*scale+0.5f)));
		imgView.setLayoutParams(new Gallery.LayoutParams(width_, height_));
		imgView.setBackgroundResource(galleryItemBackground_);		 
		return imgView;
	}

	/**
	 * Thread Class to load Bitmap
	 * @author Huan Erdao
	 */
	private class BitmapLoadThread extends Thread {
		/** position of list */
		private int pos_;
		/** url of bitmap */
		private String url_;
		/** Handler object for UI refresh posting */
		private Handler handler_;
		/** Preload state */
		private boolean isPreLoad_;

		/**
		 * @param handler	Handler for UI refresh posting
		 * @param pos		list position
		 * @param url		url for bitmap
		 * @param preLoad	true if want to preload, false will load full image.
		 */
		public BitmapLoadThread(Handler handler, int pos, String url, boolean preLoad ){
			pos_ = pos;
			url_ = url;
			handler_ = handler;
			isPreLoad_ = preLoad;
		}
		/**
		 * Thread main routine
		 */
		@Override
		public synchronized void run() {
			Bitmap bmp = null;
			int loadState = LoadState.BITMAP_STATE_NULL;
			if(isPreLoad_){
				BitmapFactory.Options opt = new BitmapFactory.Options();
				opt.inSampleSize = 8;
				// flickr stream cannot decode from InputStream...
				if(url_.contains("flickr"))
					bmp = BitmapUtilily.loadBitmap(url_,opt);
				else
					bmp = BitmapUtilily.loadBitmapDirectStream(url_,opt);
				loadState = LoadState.BITMAP_STATE_PRE_LOADED;
			}else{
				bmp = BitmapUtilily.loadBitmap(url_);
				loadState = LoadState.BITMAP_STATE_FULL_LOADED;
			}
			if(bmp == null){
				loadState = LoadState.BITMAP_STATE_NULL;
				// try to dispose bitmap far away from current point.
				if( bitmaps_.size()-curPos_ > curPos_ ){
					for(int i = bitmaps_.size()-1; i > curPos_+3; i-- ){
						if( bitmaps_.get(i).recycle() )
							break;
					}
				}
				else{
					for(int i = 0; i < curPos_-3; i++ ){
						if( bitmaps_.get(i).recycle() )
							break;
					}
				}
			}
			LazyLoadBitmap lzybmp = bitmaps_.get(pos_);
			lzybmp.setBitmap(bmp);
			lzybmp.setState(loadState);
			bitmaps_.set(pos_,lzybmp);
			handler_.post(new Runnable() {
				public void run() {
					notifyDataSetChanged();
				}
			});
		}
	}
}