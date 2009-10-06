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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.app.ExpandableListActivity;
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
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.erdao.utils.BitmapUtilily;
import com.google.android.maps.GeoPoint;

/**
 * Activity Class for Displaying Favorites.
 * @author Huan Erdao
 */
public class FavoritesActivity extends ExpandableListActivity {
	
	/** PhotSpotDBHelper object */
	private PhotSpotDBHelper dbHelper_ = null;
	/** label list */
	private List<String> groups_ = new ArrayList<String>();
	/** PhotoItem full list */
    private List<PhotoItem> photoItems_ = new ArrayList<PhotoItem>();
	/** Bitmap full list */
    private List<Bitmap> bitmaps_ = new ArrayList<Bitmap>();
	/** Double-Array of PhotoItem for ExpandableList*/
    private List<List<PhotoItem>> photoItemNodes_ = new ArrayList<List<PhotoItem>>();
	/** Double-Array of Bitmap for ExpandableList*/
    private List<List<Bitmap>> bitmapNodes_ = new ArrayList<List<Bitmap>>();
	/** Context object */
	private Context context_;
	/** selected GroupPos */
	private int groupPos_;
	/** selected ChildPos */
	private int childPos_;
	/** Image Adapter */
	private ImageExpandableListAdapter imageAdapter_ = null;
	/** Adapter for Auto Complete Text */
	ArrayAdapter<String> autoCompleteAdapter_;
	/** Auto Complete TextView */
    AutoCompleteTextView autoCompleteTextView_;
	
	/** Extra Action - Show on Map */
	private static final int EXT_ACTION_SHOWONMAP			= 0;
	/** Extra Action - Navigate to Place */
	private static final int EXT_ACTION_NAVTOPLACE			= 1;
	/** Extra Action - Edit Label */
	private static final int EXT_ACTION_EDITLABEL			= 2;
	/** Extra Action - Remove from favorites */
	private static final int EXT_ACTION_REMOVE_FAVORITES	= 3;
	/** Extra Action - Open with browser */
	private static final int EXT_ACTION_OPENBROWSER			= 4;


	/**
	 * onCreate handler
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context_ = this;
		dbHelper_ = new PhotSpotDBHelper(this);
		updateGroupList();
		if(photoItems_.isEmpty())
			setContentView(R.layout.fav_noitem);
		else{
			ExpandableListView expListView = getExpandableListView();
			LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View footer = inflater.inflate(R.layout.fav_footer, expListView, false);
			expListView.addFooterView(footer, null, false);
			imageAdapter_ = new ImageExpandableListAdapter(this);
			setListAdapter(imageAdapter_);
			expListView.setBackgroundDrawable(null);
			expListView.setDivider(this.getResources().getDrawable(android.R.drawable.divider_horizontal_bright));
			expListView.setChildDivider(this.getResources().getDrawable(android.R.drawable.divider_horizontal_bright));
		}
	}
	
	/**
	 * remove Item from ExpandableList
	 * @param groupPos	ExpandableList group position
	 * @param childPos	ExpandableList child position
	 */
	private void removeItem(int groupPos, int childPos) {
		photoItems_.remove(photoItemNodes_.get(groupPos).get(childPos));
		photoItemNodes_.get(groupPos).remove(childPos);
		bitmaps_.remove(bitmapNodes_.get(groupPos).get(childPos));
		bitmapNodes_.get(groupPos).remove(childPos);
		if(photoItemNodes_.get(groupPos).isEmpty()){
			photoItemNodes_.remove(groupPos);
			bitmapNodes_.remove(groupPos);
			groups_.remove(groupPos);
		}
	}

	/**
	 * remove All Item from ExpandableList
	 */
	private void removeAll() {
		while(!photoItemNodes_.isEmpty()) {
			photoItemNodes_.get(0).removeAll(photoItemNodes_.get(0));
			photoItemNodes_.remove(0);
		}
		while(!bitmapNodes_.isEmpty()) {
			bitmapNodes_.get(0).removeAll(bitmapNodes_.get(0));
			bitmapNodes_.remove(0);
		}
		groups_.removeAll(groups_);
		photoItems_.removeAll(photoItems_);
		bitmaps_.removeAll(bitmaps_);
	}

	/**
	 * remove All Node lists
	 */
	private void removeAllNodes() {
		while(!photoItemNodes_.isEmpty()) {
			photoItemNodes_.get(0).removeAll(photoItemNodes_.get(0));
			photoItemNodes_.remove(0);
		}
		while(!bitmapNodes_.isEmpty()) {
			bitmapNodes_.get(0).removeAll(bitmapNodes_.get(0));
			bitmapNodes_.remove(0);
		}
		groups_.removeAll(groups_);
	}

	/**
	 * Update Group List and reflect to ExpandableList
	 */
	private void updateGroupList(){
		removeAllNodes();
		final SQLiteDatabase db = dbHelper_.getReadableDatabase();
		Cursor c = dbHelper_.queryAllLabels(db);
		c.moveToFirst();
		Map<Long,Integer> labelMap = new HashMap<Long,Integer>();
		for (int i = 0; i < c.getCount(); i++) {
            long id = c.getLong(PhotSpotDBHelper.Labels.IDX_ID);
			String label = c.getString(PhotSpotDBHelper.Labels.IDX_LABEL);
			labelMap.put(id, i);
			if(label.equals(PhotSpotDBHelper.Labels.UNDEFINED_LABEL))
				label = getString(R.string.UndefinedFavLabel);
			groups_.add(label);
			List<PhotoItem> childPhotoItems = new ArrayList<PhotoItem>();
			List<Bitmap> childBitmapItems = new ArrayList<Bitmap>();
			photoItemNodes_.add(childPhotoItems);
			bitmapNodes_.add(childBitmapItems);
			c.moveToNext();
		}
		if(photoItems_.isEmpty()){
			BitmapFactory.Options opt = new BitmapFactory.Options();
			opt.inSampleSize = 6;
			c = dbHelper_.queryAllItems(db);
			c.moveToFirst();
			for (int i = 0; i < c.getCount(); i++) {
	            long id = c.getLong(PhotSpotDBHelper.Spots.IDX_ID);
				String title = c.getString(PhotSpotDBHelper.Spots.IDX_TITLE);
				String author = c.getString(PhotSpotDBHelper.Spots.IDX_AUTHOR);
				String thumbUrl = c.getString(PhotSpotDBHelper.Spots.IDX_THUMB_URL);
				String photoUrl = c.getString(PhotSpotDBHelper.Spots.IDX_PHOTO_URL);
				double lat = c.getDouble(PhotSpotDBHelper.Spots.IDX_LATITUDE);
				double lng = c.getDouble(PhotSpotDBHelper.Spots.IDX_LONGITUDE);
				long labelId = c.getLong(PhotSpotDBHelper.Spots.IDX_LABEL);
				PhotoItem item =
					new PhotoItem(id,(int)(lat*1E6),(int)(lng*1E6),title,author,thumbUrl,photoUrl);
				item.setLabelId(labelId);
				int pos = labelMap.get(labelId);
				photoItems_.add(item);
				photoItemNodes_.get(pos).add(item);
	 			Bitmap bitmap = null;
				byte[] bmpStream = c.getBlob(PhotSpotDBHelper.Spots.IDX_THUMBDATA);
				if(bmpStream!=null){
					bitmap = BitmapFactory.decodeByteArray(bmpStream, 0, bmpStream.length, opt);
					bitmapNodes_.get(pos).add(bitmap);
					bitmaps_.add(bitmap);
				}
				else{
					bitmapNodes_.get(pos).add(null);
					bitmaps_.add(null);
				}
				c.moveToNext();
			}
		}
		else{
			for (int i = 0; i < photoItems_.size(); i++) {
				PhotoItem item = photoItems_.get(i);
				int pos = labelMap.get(item.getLabelId());
				photoItemNodes_.get(pos).add(item);
				bitmapNodes_.get(pos).add(bitmaps_.get(i));
			}
		}
		c.close();
		db.close();
	}

	/**
	 * onCreateOptionsMenu handler
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuItem menu_Revert = menu.add(0,R.id.menu_Revert,0,R.string.menu_Revert);
		menu_Revert.setIcon(android.R.drawable.ic_menu_revert);
		MenuItem menu_GalleryView = menu.add(0,R.id.menu_GalleryView,0,R.string.menu_GalleryView);
		menu_GalleryView.setIcon(R.drawable.ic_menu_galleryview);
		MenuItem menu_DeleteAll = menu.add(0,R.id.menu_DeleteAll,0,R.string.menu_DeleteAll);
		menu_DeleteAll.setIcon(android.R.drawable.ic_menu_delete);
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
			case R.id.menu_GalleryView: {
				Intent i = new Intent(context_, FavoritesGalleryActivity.class);
				startActivity(i);
				break;
			}
			case R.id.menu_DeleteAll: {
				new AlertDialog.Builder(context_)
				.setTitle(R.string.menu_DeleteAll)
				.setMessage(R.string.DeleteAllConfirm)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						dbHelper_.deleteAll();
						removeAll();
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
	
	/**
	 * onChildClick handler
	 */
	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPos, int childPos, long id){
		groupPos_ = groupPos;
		childPos_ = childPos;
		new AlertDialog.Builder(context_)
		.setTitle(R.string.ExtActionDlgTitle)
		.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				dialog.dismiss();
			}
		})
		.setItems(R.array.favorite_extaction, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				onItemAction(which,groupPos_,childPos_);
			}
		})
	   .create()
	   .show();
		return true;
    }
	
	/**
	 * Toast Message
	 * @param messageId message resource id
	 * @param duration Toast duration
	 */
	public void ToastMessage(int messageId, int duration){
		Toast.makeText(this, messageId, duration).show();
	}

	/**
	 * Extra Action Handler
	 * @param cmd action command
	 * @param groupPos	ExpandableList group position
	 * @param childPos	ExpandableList child position
	 */
	public void onItemAction(int cmd, int groupPos, int childPos){
		switch(cmd){
			case EXT_ACTION_SHOWONMAP:{
				PhotoItem item = photoItemNodes_.get(groupPos).get(childPos);
	            Intent intent = new Intent(this, PhotSpotActivity.class);
	            intent.setAction(Intent.ACTION_VIEW);
	            item.setBitmap(bitmapNodes_.get(groupPos).get(childPos));
	            intent.putExtra(PhotoItem.EXT_PHOTOITEM, item);
	            startActivity(intent);
//	            finish();
	            break;
			}
			case EXT_ACTION_NAVTOPLACE:{
				GeoPoint location = photoItemNodes_.get(groupPos).get(childPos).getLocation();
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
				String url = photoItemNodes_.get(groupPos).get(childPos).getPhotoUrl();
				Intent i = new Intent(Intent.ACTION_VIEW,Uri.parse(url));
				context_.startActivity(i);
				break;
			}
			case EXT_ACTION_EDITLABEL:{
		        setTheme(android.R.style.Theme_Black);
		        autoCompleteAdapter_ = new ArrayAdapter<String>(this,
		                 android.R.layout.simple_dropdown_item_1line, groups_);
		        autoCompleteTextView_ = new AutoCompleteTextView(this);
		        autoCompleteTextView_.setSingleLine();
		        autoCompleteTextView_.setWidth(50);
		        autoCompleteTextView_.setAdapter(autoCompleteAdapter_);
				String label = dbHelper_.queryLabel(photoItemNodes_.get(groupPos).get(childPos));
				if(label!=null){
					autoCompleteTextView_.setText(label);
				}
				new AlertDialog.Builder(this)
				.setTitle(R.string.EditLabel)
				.setView(autoCompleteTextView_)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String label = autoCompleteTextView_.getText().toString();
						if(label != null){
							PhotoItem item = photoItemNodes_.get(groupPos_).get(childPos_);
							long newLabelId = dbHelper_.updateLabel(item,label);
							if(newLabelId>0)
								item.setLabelId(newLabelId);
							updateGroupList();
							imageAdapter_.notifyDataSetChanged();
						}
				        context_.setTheme(android.R.style.Theme_Light);
						dialog.dismiss();
					}
				})
				.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
				        context_.setTheme(android.R.style.Theme_Light);
						dialog.dismiss();
					}
				})
			   .create()
			   .show();
				break;
			}
			case EXT_ACTION_REMOVE_FAVORITES:{
				new AlertDialog.Builder(context_)
				.setTitle(R.string.Delete)
				.setMessage(R.string.DeleteFavoriteConfirm)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						if( dbHelper_.deleteItem(photoItemNodes_.get(groupPos_).get(childPos_)) == PhotSpotDBHelper.DB_SUCCESS ){
							ToastMessage(R.string.ToastRemovedFavorites, Toast.LENGTH_SHORT);
							removeItem(groupPos_,childPos_);
							if(photoItems_.isEmpty())
								setContentView(R.layout.fav_noitem);
							else{
								updateGroupList();
								imageAdapter_.notifyDataSetChanged();
							}
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
		}
	}

	/**
	 * Image Adapter class for ExpandableList
	 * @author Huan Erdao
	 */
	private class ImageExpandableListAdapter extends BaseExpandableListAdapter {
		/* variables */
		private final Context context_;
		int galleryItemBackground_;

		/**
		 * @param c Context object
		 */
		public ImageExpandableListAdapter(Context c) {
			context_ = c;
			TypedArray a = context_.obtainStyledAttributes(R.styleable.ThumbGallery);
			galleryItemBackground_ = a.getResourceId(
					R.styleable.ThumbGallery_android_galleryItemBackground, 0);
			a.recycle();
		}

		/**
		 * hasStableIds
		 */
        public boolean hasStableIds() {
            return true;
        }
        
		/**
		 * getChild
		 */
        public Object getChild(int groupPos, int childPos) {
            return photoItemNodes_.get(groupPos).get(childPos);
        }
        
		/**
		 * getChildId
		 */
		@Override
		public long getChildId(int groupPos, int childPos) {
			return photoItemNodes_.get(groupPos).get(childPos).getId();
		}

		/**
		 * getChildrenCount
		 */
		@Override
		public int getChildrenCount(int groupPos) {
			return photoItemNodes_.get(groupPos).size();
		}

		/**
		 * getGroup
		 */
		@Override
		public Object getGroup(int groupPos) {
			return photoItemNodes_.get(groupPos);
		}

		/**
		 * getGroupCount
		 */
		@Override
		public int getGroupCount() {
			return photoItemNodes_.size();
		}

		/**
		 * getGroupId
		 */
		@Override
		public long getGroupId(int groupPos) {
			return groupPos;
		}

		/**
		 * getChildView
		 */
		@Override
		public View getChildView(int groupPos, int childPos, boolean isLastChild, View convertView, ViewGroup parent) {
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
			PhotoItem item = photoItemNodes_.get(groupPos).get(childPos);
			Bitmap bmp = bitmapNodes_.get(groupPos).get(childPos);
			if( bmp == null ){
				imgView.setImageResource(R.drawable.imgloading);
				Handler handler = new Handler();
				new BitmapLoadThread(handler,groupPos,childPos,item.getThumbUrl()).start();
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

		/**
		 * getGenericView
		 */
		public TextView getGenericView() {
            // Layout parameters for the ExpandableListView
            AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
                    ViewGroup.LayoutParams.FILL_PARENT, 64);
            TextView textView = new TextView(context_);
            textView.setLayoutParams(lp);
            // Center the text vertically
            textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
            // Set the text starting position
            textView.setPadding(36, 0, 0, 0);
            return textView;
        }

		/**
		 * getGroupView
		 */        
        @Override
		public View getGroupView(int groupPos, boolean isExpanded, View convertView, ViewGroup parent) {
            TextView textView = getGenericView();
            textView.setText(groups_.get(groupPos));
            return textView;
		}

		/**
		 * isChildSelectable
		 */        
		@Override
		public boolean isChildSelectable(int groupPos, int childPos) {
			return true;
		}

		/**
		 * Thread Class to load Bitmap
		 * @author Huan Erdao
		 */
		private class BitmapLoadThread extends Thread {
			/** ExpandableList group position */
			private int groupPos_;
			/** ExpandableList child position */
			private int childPos_;
			/** url of bitmap */
			private String url_;
			/** Handler object for UI refresh posting */
			private Handler handler_;
			/**
			 * @param handler	Handler for UI refresh posting
			 * @param groupPos	ExpandableList group position
			 * @param childPos	ExpandableList child position
			 * @param url		url for bitmap
			 */
			public BitmapLoadThread(Handler handler, int groupPos, int childPos, String url ){
				groupPos_ = groupPos;
				childPos_ = childPos;
				url_ = url;
				handler_ = handler;
			}
			/**
			 * thread main routine
			 */
			@Override
			public void run() {
				Bitmap bmp = BitmapUtilily.loadBitmap(url_);
				bitmapNodes_.get(groupPos_).set(childPos_,bmp);
				handler_.post(new Runnable() {
					public void run() {
						notifyDataSetChanged();
					}
				});
			}
		}

	};	
}