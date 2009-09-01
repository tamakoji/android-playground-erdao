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

import android.app.AlertDialog;
import android.app.ListActivity;
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
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;

/**
 * Activity which displays the list of images.
 */
public class FavoritesActivity extends ListActivity {
	
	private PhotSpotDBHelper dbHelper_ = null;
	private ArrayList<PhotoItem> photoItems_ = new ArrayList<PhotoItem>();
	private ArrayList<Bitmap> bitmaps_ = new ArrayList<Bitmap>();
	private Context context_;
	private int selItem_;
	private ImageAdapter imageAdapter_ = null;
    private EditText edit_;
	
	private static final int EXT_ACTION_SHOWONMAP			= 0;
	private static final int EXT_ACTION_NAVTOPLACE			= 1;
	private static final int EXT_ACTION_OPENBROWSER			= 2;
	private static final int EXT_ACTION_EDITLABEL			= 3;
	private static final int EXT_ACTION_REMOVE_FAVORITES	= 4;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context_ = this;
		dbHelper_ = new PhotSpotDBHelper(this);
		final SQLiteDatabase db = dbHelper_.getReadableDatabase();
		Cursor cursor = dbHelper_.queryAll(db);
		cursor.moveToFirst();
		for (int i = 0; i < cursor.getCount(); i++) {
			long id = cursor.getLong(PhotSpotDBHelper.Spots.IDX_ID);
			String title = cursor.getString(PhotSpotDBHelper.Spots.IDX_TITLE);
			String author = cursor.getString(PhotSpotDBHelper.Spots.IDX_AUTHOR);
			String thumbUurl = cursor.getString(PhotSpotDBHelper.Spots.IDX_THUMB_URL);
			String photoUrl = cursor.getString(PhotSpotDBHelper.Spots.IDX_PHOTO_URL);
			double lat = cursor.getDouble(PhotSpotDBHelper.Spots.IDX_LATITUDE);
			double lng = cursor.getDouble(PhotSpotDBHelper.Spots.IDX_LONGITUDE);
			PhotoItem item =
				new PhotoItem(id,thumbUurl,(int)(lat*1E6),(int)(lng*1E6),title,photoUrl,author);
			photoItems_.add(item);
 			Bitmap bitmap = null;
			byte[] bmpStream = cursor.getBlob(PhotSpotDBHelper.Spots.IDX_THUMBDATA);
			if(bmpStream!=null){
				bitmap = BitmapFactory.decodeByteArray(bmpStream, 0, bmpStream.length);
				bitmaps_.add(bitmap);
			}
			else
				bitmaps_.add(null);
			cursor.moveToNext();
		}
		cursor.close();
		db.close();
		if(photoItems_.size()==0)
			setContentView(R.layout.fav_noitem);
		else{
			ListView listView = getListView();
			LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View footer = inflater.inflate(R.layout.fav_footer, listView, false);
			listView.addFooterView(footer, null, false);
			imageAdapter_ = new ImageAdapter(this);
			setListAdapter(imageAdapter_);
			listView.setBackgroundColor(0xFFFFFFFF);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuItem menu_Revert = menu.add(0,R.id.menu_Revert,0,R.string.menu_Revert);
		menu_Revert.setIcon(android.R.drawable.ic_menu_revert);
		MenuItem menu_DeleteAll = menu.add(0,R.id.menu_DeleteAll,0,R.string.menu_DeleteAll);
		menu_DeleteAll.setIcon(android.R.drawable.ic_menu_delete);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_Revert: {
				finish();
				break;
			}
			case R.id.menu_DeleteAll: {
				new AlertDialog.Builder(context_)
				.setTitle(R.string.menu_DeleteAll)
				.setMessage(R.string.DeleteAllConfirm)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						dbHelper_.deleteAll();
						imageAdapter_.removeAll();
						setContentView(R.layout.fav_noitem);
						dialog.dismiss();
					}
				})
				.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						dialog.dismiss();
					}
				})
			   .create()
			   .show();
				break;
			}
		}
		return true;
	}
	
	@Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
		selItem_ = position;
		new AlertDialog.Builder(context_)
		.setTitle(R.string.ExtActionDlg)
		.setPositiveButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				dialog.dismiss();
			}
		})
		.setItems(R.array.favorite_extaction, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				onItemAction(which,selItem_);
				dialog.dismiss();
			}
		})
	   .create()
	   .show();
    }
	
	/* option tasks for long pressing item */
	public void onItemAction(int cmd, int pos){
		selItem_ = pos;
		switch(cmd){
			case EXT_ACTION_SHOWONMAP:{
				PhotoItem item = photoItems_.get(pos);
	            Intent intent = new Intent(this, PhotSpotActivity.class);
	            intent.setAction(Intent.ACTION_VIEW);
	            item.setBitmap(bitmaps_.get(pos));
	            intent.putExtra(PhotoItem.EXT_PHOTOITEM, item);
	            startActivity(intent);
	            break;
			}
			case EXT_ACTION_NAVTOPLACE:{
				GeoPoint location = photoItems_.get(pos).getLocation();
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
				String url = photoItems_.get(pos).getPhotoUrl();
				Intent i = new Intent(Intent.ACTION_VIEW,Uri.parse(url));
				context_.startActivity(i);
				break;
			}
			case EXT_ACTION_EDITLABEL:{
		        edit_ = new EditText(this);
		        edit_.setWidth(50);
				String label = dbHelper_.queryLabel(photoItems_.get(pos));
				if(label!=null){
					edit_.setText(label);
				}
				new AlertDialog.Builder(this)
				.setTitle("Edit Label")
				.setView(edit_)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String label = edit_.getText().toString();
						if(label != null){
							dbHelper_.updateLabel(photoItems_.get(selItem_),label);
						}
						dialog.dismiss();
					}
				})
				.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						dialog.dismiss();
					}
				})
			   .create()
			   .show();
				break;
			}
			case EXT_ACTION_REMOVE_FAVORITES:{
				if( dbHelper_.deleteItem(photoItems_.get(pos)) == PhotSpotDBHelper.DB_SUCCESS ){
					Toast.makeText(context_, R.string.ToastRemovedFavorites, Toast.LENGTH_SHORT);
					imageAdapter_.removeItem(pos);
					if(imageAdapter_.getCount()==0)
						setContentView(R.layout.fav_noitem);
				}
				break;
			}
		}
	}

	/* Image Adapter class for FavoriteActivity */
	private class ImageAdapter extends BaseAdapter {
		/* variables */
		private final Context context_;
		int galleryItemBackground_;

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

		/* removeItem */
		public void removeItem(int position) {
			photoItems_.remove(position);
			bitmaps_.remove(position);
			notifyDataSetChanged();
		}
		
		public void removeAll() {
			int start = photoItems_.size()-1;
			for(int i=start; i >=0; i--){
				photoItems_.remove(i);
			}
			start = bitmaps_.size()-1;
			for(int i=start; i >=0; i--){
				bitmaps_.remove(i);
			}
			notifyDataSetChanged();
		}

		/* getView */
		public View getView(int position, View convertView, ViewGroup parent) {
			View view;
			if (convertView == null) {
				// Make up a new view
				LayoutInflater inflater = (LayoutInflater) context_.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = inflater.inflate(R.layout.fav_item, null);
			} else {
				// Use convertView if it is available
				view = convertView;
			}
			ImageView imgView = (ImageView) view.findViewById(R.id.favimage);
			PhotoItem item = photoItems_.get(position);
			Bitmap bmp = bitmaps_.get(position);
			if( bmp == null ){
				imgView.setImageResource(R.drawable.imgloading);
				Handler handler = new Handler();
				new BitmapLoadThread(handler,position,item.getThumbUrl()).start();
			}
			else{
				imgView.setImageBitmap(bmp);
			}
			imgView.setBackgroundResource(galleryItemBackground_); 
			TextView t = (TextView) view.findViewById(R.id.fav_title);
			t.setText(item.getTitle());
			t = (TextView) view.findViewById(R.id.fav_author);
			t.setText("by "+item.getAuthor());
			t = (TextView) view.findViewById(R.id.fav_location);
			GeoPoint location = item.getLocation();
			double lat = location.getLatitudeE6()/1E6;
			double lng = location.getLongitudeE6()/1E6;
			t.setText("("+lat+","+lng+")");
			return view;
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