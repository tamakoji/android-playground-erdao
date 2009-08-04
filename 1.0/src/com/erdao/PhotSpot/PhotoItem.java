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

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.maps.GeoPoint;

/* Class for PhotoItem */
public class PhotoItem implements Parcelable {
	
	/* variables */
	private long id_;
	private GeoPoint location_;
	private String title_;
	private String thumbUrl_;
	private String photoUrl_;
	private String owner_;
	private int isSelected_;
	
	/* constructor */
	public PhotoItem(long id, String thumbUrl, int latitudeE6, int longitudeE6,
			String title, String photoUrl, String owner) {
		location_ = new GeoPoint(latitudeE6, longitudeE6);
		title_ = title;
		thumbUrl_ = thumbUrl;
		photoUrl_ = photoUrl;
		owner_ = owner;
		isSelected_ = 0;
	}

	/* constructor (Parcel) */
	public PhotoItem(Parcel in) {
		id_ = in.readLong();
		location_ = new GeoPoint(in.readInt(), in.readInt());
		title_ = in.readString();
		thumbUrl_ = in.readString();
		photoUrl_ = in.readString();
		owner_ = in.readString();
		isSelected_ = in.readInt();
	}

	/* describeContents */
	public int describeContents() {
		return 0;
	}

	/* getId */
	public long getId() {
		return id_;
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
	
	/* getOwner */
	public String getOwner() {
		return owner_;	   
	}

	/* getThumbUrl */
	public String getThumbUrl() {
		return thumbUrl_;
	}

	/* getPhotoUrl */
	public String getPhotoUrl() {
		return photoUrl_;
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
		parcel.writeInt(location_.getLatitudeE6());
		parcel.writeInt(location_.getLongitudeE6());
		parcel.writeString(title_);
		parcel.writeString(thumbUrl_);
		parcel.writeString(photoUrl_);
		parcel.writeString(owner_);
   }

}