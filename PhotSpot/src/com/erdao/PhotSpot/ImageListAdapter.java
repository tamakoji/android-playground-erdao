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

/* Image Adapter class for gallery */
public class ImageListAdapter extends BaseAdapter {
	/* variables */
	int galleryItemBackground_;
	private final Context context_;
	private final List<PhotoItem> photoItems_;
	private final List<Bitmap> bitmaps_;
	private int width_ = 160;
	private int height_ = 120;

	/* constructor */
	public ImageListAdapter(Context c, List<PhotoItem> photoItems, List<Bitmap> bitmaps) {
		context_ = c;
		photoItems_ = photoItems;
		bitmaps_ = bitmaps;
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
	public Object getItem(int pos) {
		return photoItems_.get(pos);
	}

	/* getItemId */
	public long getItemId(int pos) {
		return pos;
	}

	/* getBitmap */
	public Bitmap getBitmap(int pos) {
		return bitmaps_.get(pos);
	}

	/* getDescription */
	public String getDescription(int pos) {
		PhotoItem item = photoItems_.get(pos);
		String desc = item.getTitle();
		if(item.getAuthor()!=null){
			desc += "\nby "+item.getAuthor();
		}
		return desc;
	}
	
	public void setSize(int w, int h){
		width_ = w;
		height_ = h;
		notifyDataSetChanged();			
	}
	
	/* getView */
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

	/* Thread Class to load Bitmap */
	private class BitmapLoadThread extends Thread {
		private int pos_;
		private String url_;
		private Handler handler_;
		public BitmapLoadThread(Handler handler, int pos, String url ){
			pos_ = pos;
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
}