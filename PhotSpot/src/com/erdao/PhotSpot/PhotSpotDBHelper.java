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

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.graphics.Bitmap;
import android.provider.BaseColumns;

import com.google.android.maps.GeoPoint;


public class PhotSpotDBHelper extends SQLiteOpenHelper {

    public final class Spots implements BaseColumns {
        public Spots() {}
        public static final String DEFAULT_SORT_ORDER	= "modified DESC";
        public static final String TITLE				= "title";
        public static final String AUTHOR				= "author";
        public static final String THUMB_URL			= "thumb_url";
        public static final String PHOTO_URL			= "photo_url";
        public static final String LATITUDE				= "latitude";
        public static final String LONGITUDE			= "longitude";
        public static final String THUMBDATA			= "thumbdata";
        public static final String LABEL				= "label";
        public static final String REGION				= "region";
        public static final int IDX_ID			= 0;
        public static final int IDX_TITLE		= 1;
        public static final int IDX_AUTHOR		= 2;
        public static final int IDX_THUMB_URL	= 3;
        public static final int IDX_PHOTO_URL	= 4;
        public static final int IDX_LATITUDE	= 5;
        public static final int IDX_LONGITUDE	= 6;
        public static final int IDX_THUMBDATA	= 7;
        public static final int IDX_LABEL		= 8;
        public static final int IDX_REGIOIN		= 9;
        public long id_;
        public long labelId_;
        /*
        public String title_;
        public String author_;
        public String thumbUrl;
        public String photoUrl;
        public double lat_;
        public double lng_;
        public byte[] thumbStream_;
        public String region_;
        */
    }
    public final class Labels implements BaseColumns {
        public Labels() {}
        public static final String UNDEFINED_LABEL		= "undefined";
        public static final String DEFAULT_SORT_ORDER	= "modified DESC";
        public static final String LABEL				= "label";
        public static final String COUNTS				= "counts";
        public static final int IDX_ID			= 0;
        public static final int IDX_LABEL		= 1;
        public static final int IDX_COUNTS		= 2;
        public long id_;
        public String label_;
        public long counts_;
    }
    
    public static final int DB_SUCCESS			= 0;
    public static final int DB_FAILED			= -1;
    public static final int DB_EXISTS			= -2;
    public static final int DB_FULL				= -3;
    public static final int DB_UNKNOWN_ERROR	= -4;

    
    private static final String DATABASE_NAME = "photspotfv.db";
    private static final int DATABASE_VERSION = 1;
    private static final String SPOTS_TABLE_NAME = "spots";
    private static final String LABEL_TABLE_NAME = "labels";
	
	private static final int IO_BUFFER_SIZE = 4 * 1024;
	
	private static final int MAX_RECORDS = 256;	// MAX 256 

    PhotSpotDBHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE " + SPOTS_TABLE_NAME + " ("
				+ Spots._ID			+ " INTEGER PRIMARY KEY,"
				+ Spots.TITLE		+ " TEXT,"
				+ Spots.AUTHOR		+ " TEXT,"
				+ Spots.THUMB_URL	+ " TEXT,"
				+ Spots.PHOTO_URL	+ " TEXT,"
				+ Spots.LATITUDE	+ " REAL,"
				+ Spots.LONGITUDE	+ " REAL,"
				+ Spots.THUMBDATA	+ " BLOB,"
				+ Spots.LABEL		+ " INTEGER,"
				+ Spots.REGION		+ " TEXT"				
				+ ");");
		db.execSQL("CREATE TABLE " + LABEL_TABLE_NAME + " ("
				+ Labels._ID		+ " INTEGER PRIMARY KEY,"
				+ Labels.LABEL		+ " TEXT,"
				+ Labels.COUNTS		+ " INTEGER"
				+ ");");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS"+SPOTS_TABLE_NAME);
		onCreate(db);
	}
	
	// query a PhotoItem from THUMB_URL
	private Spots queryItemByThumbURL(SQLiteDatabase db, String thumb_url ){
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(SPOTS_TABLE_NAME);
        qb.appendWhere(Spots.THUMB_URL + "='"+thumb_url+"'");
        Cursor c = qb.query(db, null, null, null, null, null, null);
        if(c.getCount()==0)
        	return null;
        c.moveToFirst();
        Spots spots = new Spots();
        spots.id_ = c.getLong(Spots.IDX_ID);
		spots.labelId_ = c.getLong(Spots.IDX_LABEL);
        /*
        spots.title_ = c.getString(Spots.IDX_TITLE);
        spots.author_ = c.getString(Spots.IDX_AUTHOR);
		spots.thumbUrl = c.getString(Spots.IDX_THUMB_URL);
		spots.photoUrl = c.getString(Spots.IDX_PHOTO_URL);
		spots.lat_ = c.getDouble(Spots.IDX_LATITUDE);
		spots.lng_ = c.getDouble(Spots.IDX_LONGITUDE);
        spots.region_ = c.getString(Spots.IDX_REGIOIN);
        */
        return spots;
	}

	// query a PhotoItem
	private Spots queryItemById(SQLiteDatabase db, long id){
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(SPOTS_TABLE_NAME);
        qb.appendWhere(Spots._ID+ "="+id);
        Cursor c = qb.query(db, null, null, null, null, null, null);
        if(c.getCount()==0)
        	return null;
        c.moveToFirst();
        Spots spots = new Spots();
        spots.id_ = c.getLong(Spots.IDX_ID);
		spots.labelId_ = c.getLong(Spots.IDX_LABEL);
        /*
        spots.title_ = c.getString(Spots.IDX_TITLE);
        spots.author_ = c.getString(Spots.IDX_AUTHOR);
		spots.thumbUrl = c.getString(Spots.IDX_THUMB_URL);
		spots.photoUrl = c.getString(Spots.IDX_PHOTO_URL);
		spots.lat_ = c.getDouble(Spots.IDX_LATITUDE);
		spots.lng_ = c.getDouble(Spots.IDX_LONGITUDE);
        spots.region_ = c.getString(Spots.IDX_REGIOIN);
        */
        return spots;
	}

	// query all PhotoItem. returns cursor
	public Cursor queryAll(SQLiteDatabase db){
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(SPOTS_TABLE_NAME);
        return qb.query(db, null, null, null, null, null, null);
	}

	// query for a Label. if undefined, return null
	public String queryLabel(PhotoItem item){
		final SQLiteDatabase db = this.getWritableDatabase();
		Spots spots = queryItemById(db, item.getId());
		if(spots==null){
			db.close();
			return null;
		}
		Labels labels = this.queryLabel(db, spots.labelId_);
		if(labels==null){
			db.close();
			return null;
		}
		if( labels.label_.equals(Labels.UNDEFINED_LABEL) ){
			db.close();
			return null;
		}
		db.close();
		return labels.label_;
	}
	
	// insert a PhotoItem. 1:success, -1:exception 0:record exists
	public int insertItem(PhotoItem item, Bitmap bmp){
		final SQLiteDatabase db = this.getWritableDatabase();
		Cursor c = this.queryAll(db);
		if(c.getCount()>=MAX_RECORDS){
			db.close();
			return DB_FULL;
		}
		Spots spots = this.queryItemByThumbURL(db,item.getThumbUrl());
		if( spots != null ){
			db.close();
			return DB_EXISTS;
		}
		GeoPoint location = item.getLocation();
		double lat = location.getLatitudeE6()/1E6;
		double lng = location.getLongitudeE6()/1E6;
		BufferedOutputStream bufferedOS = null;
        byte[] data = null;
		if(bmp!=null){
	        try {
				final ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
				bufferedOS = new BufferedOutputStream(dataStream, IO_BUFFER_SIZE);
		        bmp.compress(Bitmap.CompressFormat.JPEG, 60, bufferedOS);
		        bufferedOS.flush();
		        data = dataStream.toByteArray();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				db.close();
				return DB_UNKNOWN_ERROR;
			}
		}
		ContentValues cv = new ContentValues();
		cv.put(Spots.TITLE,item.getTitle());
		cv.put(Spots.AUTHOR,item.getAuthor());
		cv.put(Spots.THUMB_URL,item.getThumbUrl());
		cv.put(Spots.PHOTO_URL,item.getPhotoUrl());
		cv.put(Spots.LATITUDE,lat);
		cv.put(Spots.LONGITUDE,lng);
		cv.put(Spots.THUMBDATA,data);
		long id = insertLabel(db,Labels.UNDEFINED_LABEL);
		cv.put(Spots.LABEL,id);
		cv.put(Spots.REGION,"");
		long newId = 0;
		try {
			newId = db.insertOrThrow(PhotSpotDBHelper.SPOTS_TABLE_NAME, null, cv);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			db.close();
			return DB_UNKNOWN_ERROR;
		}
    	if(bufferedOS!=null){
	        try {
	        	bufferedOS.close();
	        } catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
	        }
    	}
    	item.setId(newId);
		db.close();
		return DB_SUCCESS;
	}
	
	// query a Label from Label Name. returns Labels
	private Labels queryLabel(SQLiteDatabase db, String label){
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(LABEL_TABLE_NAME);
        qb.appendWhere(Labels.LABEL + "='"+label+"'");
        Cursor c = qb.query(db, null, null, null, null, null, null);
        if(c.getCount()==0)
        	return null;
        c.moveToFirst();
        Labels labels = new Labels();
        labels.id_ = c.getLong(Labels.IDX_ID);
        labels.label_ = c.getString(Labels.IDX_LABEL);
        labels.counts_ = c.getLong(Labels.IDX_COUNTS);
        return labels;
	}

	// query a Label from Id. returns current counts
	private Labels queryLabel(SQLiteDatabase db, long id){
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(LABEL_TABLE_NAME);
        qb.appendWhere(Labels._ID+ "="+id);
        Cursor c = qb.query(db, null, null, null, null, null, null);
        if(c.getCount()==0)
        	return null;
        c.moveToFirst();
        Labels labels = new Labels();
        labels.id_ = c.getLong(Labels.IDX_ID);
        labels.label_ = c.getString(Labels.IDX_LABEL);
        labels.counts_ = c.getLong(Labels.IDX_COUNTS);
        return labels;
	}

	// query a Label from Id
	private void updateLabelCount(SQLiteDatabase db, long id, long count){
		String whereClause = Labels._ID + "="+id;
		if(count == 0){
    		db.delete(LABEL_TABLE_NAME, whereClause, null);
		}
		else{
			ContentValues cv = new ContentValues();
			cv.put(Labels.COUNTS,count);
			db.update(LABEL_TABLE_NAME, cv, whereClause, null);
		}
	}
	
	// insert a Label
	public long insertLabel(SQLiteDatabase db, String newLabelName){
		Labels labels = this.queryLabel(db,newLabelName);
		long id = 0;
		if(labels != null){						// if exists
			id = labels.id_;
			updateLabelCount(db,id,labels.counts_+1);
		}
		else{
			ContentValues cv = new ContentValues();
			cv.put(Labels.LABEL,newLabelName);
			cv.put(Labels.COUNTS,1);
			try {
				id = db.insertOrThrow(LABEL_TABLE_NAME, null, cv);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return DB_UNKNOWN_ERROR;
			}
		}
		return id;
	}

	// update item with a new Label
	public int updateLabel(PhotoItem item, String newLabelName){
		final SQLiteDatabase db = this.getWritableDatabase();
		Spots spots = this.queryItemById(db,item.getId());
		if( spots == null ){
			db.close();
			return DB_FAILED;
		}
		long labelId = spots.labelId_;
		// query for current label
		Labels curLabels = this.queryLabel(db,labelId);
		if(curLabels==null){
			db.close();
			return DB_FAILED;
		}
		long newLabelId = 0;
		if(newLabelName.equals(curLabels.label_)){
			db.close();
			return DB_EXISTS;
		}
		// update old label
		updateLabelCount(db,curLabels.id_,curLabels.counts_-1);
		// insert new label
		newLabelId = insertLabel(db,newLabelName);
		// update spot table with new label id
		ContentValues cv = new ContentValues();
		cv.put(Spots.LABEL,newLabelId);
		String whereClause = Spots._ID + "="+item.getId();
		db.update(SPOTS_TABLE_NAME, cv, whereClause, null);
		db.close();
		return DB_SUCCESS;
	}

	
	// delete a PhotoItem from table
	public int deleteItem(PhotoItem item){
		final SQLiteDatabase db = this.getWritableDatabase();
		Spots spots = this.queryItemById(db, item.getId());
		if(spots == null){
			db.close();
			return DB_FAILED;
		}
		Labels curLabels = this.queryLabel(db,spots.labelId_);
		if(curLabels!=null)
			updateLabelCount(db,curLabels.id_,curLabels.counts_-1);
		try {
			String whereClause = Spots._ID + "="+spots.id_;
			db.delete(SPOTS_TABLE_NAME, whereClause, null);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			db.close();
			return DB_UNKNOWN_ERROR;
		}
		db.close();
		return DB_SUCCESS;
	}
	
	// delete all PhotoItem from table
	public int deleteAll(){
		final SQLiteDatabase db = this.getWritableDatabase();
		try {
			db.delete(SPOTS_TABLE_NAME, null, null);
			db.delete(LABEL_TABLE_NAME, null, null);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			db.close();
			return DB_UNKNOWN_ERROR;
		}
		db.close();
		return DB_SUCCESS;
	}
   
}

