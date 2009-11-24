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
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.Gallery;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;

import com.erdao.utils.LazyLoadBitmap;
import com.erdao.utils.LazyLoadBitmap.LoadState;
import com.google.android.maps.GeoPoint;

/**
 * Activity Class for Displaying Favorites in Gallery View.
 * @author Huan Erdao
 */
public class FavoritesGalleryActivity extends Activity {
	
	/** PhotSpotDBHelper object */
	private PhotSpotDBHelper dbHelper_ = null;
	/** PhotoItem list */
    private List<PhotoItem> photoItems_ = new ArrayList<PhotoItem>();
	/** Bitmap list */
    private List<LazyLoadBitmap> bitmaps_ = new ArrayList<LazyLoadBitmap>();
	/** Context object */
	private Context context_;
	/** ImageListAdapter for gallery */
	private ImageListAdapter imageAdapter_ = null;
	/** current selected item */
	private int selItem_;
	
	/** Extra Action - Show on Map */
	private static final int EXT_ACTION_SHOWONMAP			= 0;
	/** Extra Action - Navigate to place */
	private static final int EXT_ACTION_NAVTOPLACE			= 1;
	/** Extra Action - Open with Browser */
	private static final int EXT_ACTION_OPENBROWSER			= 2;


	/**
	 * onCreate handler.
	 */
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
			String fullThumbUrl = c.getString(PhotSpotDBHelper.Spots.IDX_THUMB_URL);
			String cmpThumbUrl = fullThumbUrl;
			String origUrl = c.getString(PhotSpotDBHelper.Spots.IDX_ORIGINAL_URL);
			double lat = c.getDouble(PhotSpotDBHelper.Spots.IDX_LATITUDE);
			double lng = c.getDouble(PhotSpotDBHelper.Spots.IDX_LONGITUDE);
			long labelId = c.getLong(PhotSpotDBHelper.Spots.IDX_LABEL);
			PhotoItem item =
				new PhotoItem(id,(int)(lat*1E6),(int)(lng*1E6),title,author,fullThumbUrl,cmpThumbUrl,origUrl);
			item.setLabelId(labelId);
			photoItems_.add(item);
 			Bitmap bitmap = null;
			byte[] bmpStream = c.getBlob(PhotSpotDBHelper.Spots.IDX_THUMBDATA);
			if(bmpStream!=null){
				bitmap = BitmapFactory.decodeByteArray(bmpStream, 0, bmpStream.length);
				bitmaps_.add(new LazyLoadBitmap(bitmap,LoadState.BITMAP_STATE_FULL_LOADED));
			}
			else{
				bitmaps_.add(new LazyLoadBitmap());
			}
			c.moveToNext();
		}
		c.close();
		db.close();
		if(photoItems_.isEmpty())
			setContentView(R.layout.fav_noitem);
		else{
			setContentView(R.layout.fav_gallery);
			imageAdapter_ = new ImageListAdapter(context_,photoItems_,bitmaps_);
			imageAdapter_.setSize(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT);
			Gallery gallery = (Gallery)findViewById(R.id.fav_gallery);
			gallery.setAdapter(imageAdapter_);
			gallery.setCallbackDuringFling(true);
			TextView txtView = (TextView)findViewById(R.id.fav_imgdesc);
			txtView.setText(imageAdapter_.getDescription(selItem_));
			photoItems_.get(selItem_).setSelect(true);
			gallery.setSelection(selItem_);
			gallery.setOnItemSelectedListener(new OnItemSelectedListener() {
				public void onItemSelected (AdapterView<?> parent, View view, int position, long id){
					photoItems_.get(selItem_).setSelect(false);
					photoItems_.get(position).setSelect(true);
					selItem_ = position;
					TextView txtView = (TextView)findViewById(R.id.fav_imgdesc);
					txtView.setText(imageAdapter_.getDescription(position));
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

	
	/**
	 * option tasks for long pressing item
	 * @param cmd action command
	 */
	public void onItemAction(int cmd){
		switch(cmd){
			case EXT_ACTION_SHOWONMAP:{
				PhotoItem item = photoItems_.get(selItem_);
	            Intent intent = new Intent(this, PhotSpotActivity.class);
	            intent.setAction(Intent.ACTION_VIEW);
	            item.setBitmap(bitmaps_.get(selItem_).getBitmap());
	            intent.putExtra(PhotoItem.EXT_PHOTOITEM, item);
	            startActivity(intent);
//	            finish();
	            break;
			}
			case EXT_ACTION_NAVTOPLACE:{
				PhotoItem item = photoItems_.get(selItem_);
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
				PhotoItem item = photoItems_.get(selItem_);
				String url = item.getOriginalUrl();
				Intent i = new Intent(Intent.ACTION_VIEW,Uri.parse(url));
				context_.startActivity(i);
				break;
			}
		}
	}

	/**
	 * onCreateOptionsMenu handler
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuItem menu_Revert = menu.add(0,R.id.menu_Revert,0,R.string.menu_Revert);
		menu_Revert.setIcon(android.R.drawable.ic_menu_revert);
		return true;
	}

	/**
	 * onOptionsItemSelected handler
	 */
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

}