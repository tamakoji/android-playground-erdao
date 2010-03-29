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

import com.erdao.android.mapviewutil.GeoItem;

/**
 * Class for managing Photo item
 * @author Huan Erdao
 */
public class PhotoItem extends GeoItem {
	
	/** for intent handling */
    public static final String EXT_PHOTOITEM = "photoitem";

	/** title of the item */
	private String title_;
	/** author of the item */
	private String author_;
	/** full thumbnail url of the item */
	private String fullThumbUrl_;
	/** compact thumbnail url of the item - for lazyload */
	private String cmpThumbUrl_;
	/** orginal link of the item */
	private String originalUrl_;
	/** bitmap object of the item */
	private Bitmap bitmap_;
	/** label if of the item */
	private long labelId_;
	
	/**
	 * @param id			id of item.
	 * @param latitudeE6	latitude of the item in microdegrees (degrees * 1E6).
	 * @param longitudeE6	longitude of the item in microdegrees (degrees * 1E6).
	 * @param title			title of item.
	 * @param author		author of item.
	 * @param fullThumbUrl	full thumbnail url of item.
	 * @param cmpThumbUrl	compact thumbnail url of item.
	 * @param origUrl		original url of item.
	 */
	public PhotoItem(long id, int latitudeE6, int longitudeE6,
			String title, String author, String fullThumbUrl, String cmpThumbUrl, String origUrl ) {
		super(id,latitudeE6,longitudeE6);
		title_ = title;
		author_ = author;
		fullThumbUrl_ = fullThumbUrl;
		cmpThumbUrl_ = cmpThumbUrl;
		originalUrl_ = origUrl;
		bitmap_ = null;
		labelId_ = 1;
	}

	/**
	 * @param src			source PhotoItem.
	 */
	public PhotoItem(PhotoItem src) {
		super(src);
		title_ = src.title_;
		fullThumbUrl_ = src.fullThumbUrl_;
		cmpThumbUrl_ = src.cmpThumbUrl_;
		originalUrl_ = src.originalUrl_;
		author_ = src.author_;
		bitmap_ = src.bitmap_;
		labelId_ = src.labelId_;
	}

	/**
	 * @param src			source Parcel.
	 */
	public PhotoItem(Parcel src) {
		super(src);
		title_ = src.readString();
		author_ = src.readString();
		fullThumbUrl_ = src.readString();
		cmpThumbUrl_ = src.readString();
		originalUrl_ = src.readString();
		bitmap_ = Bitmap.CREATOR.createFromParcel(src);
		labelId_ = src.readLong();
	}

	/**
	 * describeContents
	 */
	public int describeContents() {
		return 0;
	}

	/**
	 * get title
	 * @return title of item.
	 */
	public String getTitle() {
		return title_;
	}
	
	/**
	 * get author
	 * @return author of item.
	 */
	public String getAuthor() {
		return author_;	   
	}

	/**
	 * get full thumbnail url
	 * @return fullsize thumbnail url of item.
	 */
	public String getFullThumbUrl() {
		return fullThumbUrl_;
	}

	/**
	 * get compact thumbnail url
	 * @return compact thumbnail url of item.
	 */
	public String getCompactThumbUrl() {
		return cmpThumbUrl_;
	}
	
	/**
	 * get original url
	 * @return original url of item.
	 */
	public String getOriginalUrl() {
		return originalUrl_;
	}

	/**
	 * get bitmap object
	 * @return bitmap object of item.
	 */
	public Bitmap getBitmap() {
		return bitmap_;
	}

	/**
	 * set bitmap object
	 * @param bmp bitmap object of item.
	 */
	public void setBitmap(Bitmap bmp) {
		bitmap_ = bmp;
	}

	/**
	 * get label id
	 * @return label id of item.
	 */
	public long getLabelId() {
		return labelId_;
	}

	/**
	 * set label id
	 * @param id label id of item.
	 */
	public void setLabelId(long id) {
		labelId_ = id;
	}

	/**
	 * Parcelable.Creator
	 */
	public static final Parcelable.Creator<PhotoItem> CREATOR =
		new Parcelable.Creator<PhotoItem>() {
		public PhotoItem createFromParcel(Parcel in) {
			return new PhotoItem(in);
		}
		public PhotoItem[] newArray(int size) {
			return new PhotoItem[size];
		}
	};

	/**
	 * writeToParcel
	 */
	public void writeToParcel(Parcel parcel, int flags) {
		super.writeToParcel(parcel, flags);
		parcel.writeString(title_);
		parcel.writeString(author_);
		parcel.writeString(fullThumbUrl_);
		parcel.writeString(cmpThumbUrl_);
		parcel.writeString(originalUrl_);
		bitmap_.writeToParcel(parcel, flags);
		parcel.writeLong(labelId_);
   }

}