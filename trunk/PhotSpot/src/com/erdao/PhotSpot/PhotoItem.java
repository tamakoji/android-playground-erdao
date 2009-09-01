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

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.maps.GeoPoint;

/* Class for PhotoItem */
public class PhotoItem implements Parcelable {
	
    public static final String EXT_PHOTOITEM = "photoitem";

    /* variables */
	private long id_;
	private String title_;
	private String author_;
	private String thumbUrl_;
	private String photoUrl_;
	private GeoPoint location_;
	private int isSelected_;
	private Bitmap bitmap_;
	private long labelId_;
	
	/* constructor */
	public PhotoItem(long id, String thumbUrl, int latitudeE6, int longitudeE6,
			String title, String photoUrl, String author) {
		id_ = id;
		location_ = new GeoPoint(latitudeE6, longitudeE6);
		title_ = title;
		thumbUrl_ = thumbUrl;
		photoUrl_ = photoUrl;
		author_ = author;
		isSelected_ = 0;
		bitmap_ = null;
		labelId_ = 1;
	}

	/* constructor (Parcel) */
	public PhotoItem(Parcel in) {
		id_ = in.readLong();
		title_ = in.readString();
		author_ = in.readString();
		thumbUrl_ = in.readString();
		photoUrl_ = in.readString();
		location_ = new GeoPoint(in.readInt(), in.readInt());
		isSelected_ = 0;
		bitmap_ = Bitmap.CREATOR.createFromParcel(in);
		labelId_ = in.readLong();
	}

	/* describeContents */
	public int describeContents() {
		return 0;
	}

	/* getId */
	public long getId() {
		return id_;
	}
	
	/* setId */
	public void setId(long id) {
		id_ = id;;
	}

	/* getLocation */
	public GeoPoint getLocation() {
		return location_;
	}

	/* isSelected */
	public boolean isSelected() {
		return (isSelected_ == 1);
	}

	/* setSelect */
	public void setSelect() {
		isSelected_ = 1;
	}

	/* clearSelect */
	public void clearSelect() {
		isSelected_ = 0;
	}

	/* getTitle */
	public String getTitle() {
		return title_;
	}
	
	/* getauthor */
	public String getAuthor() {
		return author_;	   
	}

	/* getThumbUrl */
	public String getThumbUrl() {
		return thumbUrl_;
	}

	/* getPhotoUrl */
	public String getPhotoUrl() {
		return photoUrl_;
	}

	/* getBitmap */
	public Bitmap getBitmap() {
		return bitmap_;
	}

	/* setBitmap */
	public void setBitmap(Bitmap bmp) {
		bitmap_ = bmp;
	}

	/* getLabelId */
	public long getLabelId() {
		return labelId_;
	}

	/* setLabelId */
	public void setLabelId(long id) {
		labelId_ = id;
	}

	/* Parcelable.Creator */
	public static final Parcelable.Creator<PhotoItem> CREATOR =
		new Parcelable.Creator<PhotoItem>() {
		public PhotoItem createFromParcel(Parcel in) {
			return new PhotoItem(in);
		}
		public PhotoItem[] newArray(int size) {
			return new PhotoItem[size];
		}
	};

	/* writeToParcel */
	public void writeToParcel(Parcel parcel, int flags) {
		parcel.writeLong(id_);
		parcel.writeString(title_);
		parcel.writeString(author_);
		parcel.writeString(thumbUrl_);
		parcel.writeString(photoUrl_);
		parcel.writeInt(location_.getLatitudeE6());
		parcel.writeInt(location_.getLongitudeE6());
		bitmap_.writeToParcel(parcel, flags);
		parcel.writeLong(labelId_);
   }

}