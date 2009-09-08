/*
 * Copyright (C) 2008 Google Inc.
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
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;

import com.google.android.maps.GeoPoint;

/**
 * Activity which displays the list of images.
 */
public class FavoritesGalleryActivity extends Activity {
	
	private PhotSpotDBHelper dbHelper_ = null;
    private List<PhotoItem> photoItemList_ = new ArrayList<PhotoItem>();
    private List<Bitmap> bitmapsList_ = new ArrayList<Bitmap>();
	private Context context_;
	private ImageAdapter imageAdapter_ = null;
	private int selItem_;
	
	private static final int EXT_ACTION_SHOWONMAP			= 0;
	private static final int EXT_ACTION_NAVTOPLACE			= 1;
	private static final int EXT_ACTION_OPENBROWSER			= 2;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context_ = this;
		selItem_ = 0;
		dbHelper_ = new PhotSpotDBHelper(this);
		final SQLiteDatabase db = dbHelper_.getReadableDatabase();
		Cursor c = dbHelper_.queryAllItems(db);
		c.moveToFirst();
		for (int i = 0; i < c.getCount(); i++) {
            long id = c.getLong(PhotSpotDBHelper.Spots.IDX_ID);
			String title = c.getString(PhotSpotDBHelper.Spots.IDX_TITLE);
			String author = c.getString(PhotSpotDBHelper.Spots.IDX_AUTHOR);
			String thumbUurl = c.getString(PhotSpotDBHelper.Spots.IDX_THUMB_URL);
			String photoUrl = c.getString(PhotSpotDBHelper.Spots.IDX_PHOTO_URL);
			double lat = c.getDouble(PhotSpotDBHelper.Spots.IDX_LATITUDE);
			double lng = c.getDouble(PhotSpotDBHelper.Spots.IDX_LONGITUDE);
			long labelId = c.getLong(PhotSpotDBHelper.Spots.IDX_LABEL);
			PhotoItem item =
				new PhotoItem(id,thumbUurl,(int)(lat*1E6),(int)(lng*1E6),title,photoUrl,author);
			item.setLabelId(labelId);
			photoItemList_.add(item);
 			Bitmap bitmap = null;
			byte[] bmpStream = c.getBlob(PhotSpotDBHelper.Spots.IDX_THUMBDATA);
			if(bmpStream!=null){
				bitmap = BitmapFactory.decodeByteArray(bmpStream, 0, bmpStream.length);
				bitmapsList_.add(bitmap);
			}
			else{
				bitmapsList_.add(null);
			}
			c.moveToNext();
		}
		c.close();
		db.close();
		if(photoItemList_.isEmpty())
			setContentView(R.layout.fav_noitem);
		else{
			setContentView(R.layout.fav_gallery);
			imageAdapter_ = new ImageAdapter(context_);
			Gallery gallery = (Gallery)findViewById(R.id.fav_gallery);
			gallery.setAdapter(imageAdapter_);
			gallery.setCallbackDuringFling(true);
			TextView txtView = (TextView)findViewById(R.id.fav_imgdesc);
			PhotoItem item = photoItemList_.get(selItem_);
			String desc = item.getTitle();
			if(item.getAuthor()!=null){
				desc += " (by "+item.getAuthor()+")";
			}
			txtView.setText(desc);
			item.setSelect();
			gallery.setSelection(selItem_);
			gallery.setOnItemSelectedListener(new OnItemSelectedListener() {
				public void onItemSelected (AdapterView<?> parent, View view, int position, long id){
					TextView txtView = (TextView)findViewById(R.id.fav_imgdesc);
					PhotoItem item = photoItemList_.get(selItem_);
					String desc = item.getTitle();
					if(item.getAuthor()!=null){
						desc += " (by "+item.getAuthor()+")";
					}
					txtView.setText(desc);
					item.clearSelect();
					photoItemList_.get(position).setSelect();
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
					.setItems(R.array.favorite_gallery_action, new DialogInterface.OnClickListener() {
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
	}

	
	/* option tasks for long pressing item */
	public void onItemAction(int cmd){
		switch(cmd){
			case EXT_ACTION_SHOWONMAP:{
				PhotoItem item = photoItemList_.get(selItem_);
	            Intent intent = new Intent(this, PhotSpotActivity.class);
	            intent.setAction(Intent.ACTION_VIEW);
	            item.setBitmap(bitmapsList_.get(selItem_));
	            intent.putExtra(PhotoItem.EXT_PHOTOITEM, item);
	            startActivity(intent);
//	            finish();
	            break;
			}
			case EXT_ACTION_NAVTOPLACE:{
				PhotoItem item = photoItemList_.get(selItem_);
				GeoPoint location = item.getLocation();
				double lat = location.getLatitudeE6()/1E6;
				double lng = location.getLongitudeE6()/1E6;
				String url = "http://maps.google.com/maps?daddr="+lat+","+lng;
				Intent i = new Intent(Intent.ACTION_VIEW,Uri.parse(url));
				i.addCategory(Intent.CATEGORY_BROWSABLE);
				i.setFlags(0x2800000);
				context_.startActivity(i);
				break;
			}
			case EXT_ACTION_OPENBROWSER:{
				PhotoItem item = photoItemList_.get(selItem_);
				String url = item.getPhotoUrl();
				Intent i = new Intent(Intent.ACTION_VIEW,Uri.parse(url));
				context_.startActivity(i);
				break;
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuItem menu_Revert = menu.add(0,R.id.menu_Revert,0,R.string.menu_Revert);
		menu_Revert.setIcon(android.R.drawable.ic_menu_revert);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_Revert: {
				finish();
				break;
			}
		}
		return true;
	}

	/* Image Adapter class for gallery */
	public class ImageAdapter extends BaseAdapter {
		/* variables */
		int galleryItemBackground_;
		private final Context context_;

		/* constructor */
		public ImageAdapter(Context c) {
			context_ = c;
			TypedArray a = context_.obtainStyledAttributes(R.styleable.ThumbGallery);
			galleryItemBackground_ = a.getResourceId(
					R.styleable.ThumbGallery_android_galleryItemBackground, 0);
			a.recycle();
		}

		/* getCount */
		public int getCount() {
			return photoItemList_.size();
		}

		/* getItem */
		public Object getItem(int position) {
			return photoItemList_.get(position);
		}

		/* getItemId */
		public long getItemId(int position) {
			return position;
		}

		/* getView */
		public View getView(int position, View convertView, ViewGroup parent) {
			ImageView imgView = new ImageView(context_);
			Bitmap bmp = bitmapsList_.get(position);
			imgView.setImageBitmap(bmp);
			imgView.setScaleType(ImageView.ScaleType.FIT_CENTER);//.CENTER_INSIDE);
			imgView.setLayoutParams(new Gallery.LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT));
			imgView.setBackgroundResource(galleryItemBackground_);		 
			return imgView;
		}

	};

}