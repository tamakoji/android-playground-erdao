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
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;

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
	private final List<Bitmap> bitmaps_;
	/** image width */
	private int width_ = 160;
	/** image height */
	private int height_ = 120;

	/**
	 * @param c				Context object
	 * @param photoItems 	PhotoItem list object
	 * @param bitmaps		Bitmap list object
	 */
	public ImageListAdapter(Context c, List<PhotoItem> photoItems, List<Bitmap> bitmaps) {
		context_ = c;
		photoItems_ = photoItems;
		bitmaps_ = bitmaps;
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
		return bitmaps_.get(pos);
	}

	/**
	 * @param pos position of list
	 * @return	Formatted description string.
	 */
	public String getDescription(int pos) {
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
		Bitmap bmp = bitmaps_.get(pos);
		if( bmp == null ){
			imgView.setImageResource(R.drawable.imgloading);
			Handler handler = new Handler();
			PhotoItem item = photoItems_.get(pos);
			new BitmapLoadThread(handler,pos,item.getThumbUrl()).start();
		}
		else{
			imgView.setImageBitmap(bmp);
		}
		imgView.setScaleType(ImageView.ScaleType.FIT_CENTER);//.CENTER_INSIDE);
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
		/**
		 * @param handler	Handler for UI refresh posting
		 * @param pos		list position
		 * @param url		url for bitmap
		 */
		public BitmapLoadThread(Handler handler, int pos, String url ){
			pos_ = pos;
			url_ = url;
			handler_ = handler;
		}
		/**
		 * Thread main routine
		 */
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
}