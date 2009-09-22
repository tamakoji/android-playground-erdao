/*
 * Copyright (C) 2009 Huan Erdao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.erdao.maps.markerclusterer;

import android.graphics.Bitmap;
import android.graphics.Point;

/**
 * Utility Class to handle MarkerBitmap
 * it handles grid offset to display on the map with offset
 * @author Huan Erdao
 */
public class MarkerBitmap {
	
	/**
	 * Enum Class for Marker Types.
	 */
	public static class MarkerIconTypes {
		/** this is static class. cannot call constructor. */
		private MarkerIconTypes(){};
		/** static value for small icon, normal state */
		public final static int ICON_SMALL_NORMAL		= 0;
		/** static value for small icon, select state */
		public final static int ICON_SMALL_SELECTED		= 1;
		/** static value for large icon, normal state */
		public final static int ICON_LARGE_NORMAL		= 2;
		/** static value for large icon, select state */
		public final static int ICON_LARGE_SELECTED		= 3;
		/** static value for total variation of icons */
		public final static int ICON_MAX				= 4;

		/** static value for changing SMALL/LARGE icon*/
		public final static int ICONSIZE_THRESH = 10;
	}
	
	/** bitmap object for icon */
	private final Bitmap iconBmp_;
	/** offset grid of icon in Point.
	 * if you are using symmetric icon image, it should be half size of width&height.
	 * adjust this parameter to offset the axis of the image. */
	private Point iconGrid_ = new Point();
	/** icon size in Point. x = width, y = height */
	private Point iconSize_ = new Point();

	/**
	 * @param src source Bitmap object
	 * @param grid grid point to be offset
	 */
	public MarkerBitmap( Bitmap src, Point grid ){
		iconBmp_ = src;
		iconGrid_ = grid;
		iconSize_.x = src.getWidth();
		iconSize_.y = src.getHeight();
	}

	/**
	 * @return bitmap object for icon
	 */
	public final Bitmap getBitmap(){
		return iconBmp_;
	}

	/**
	 * @return get offset grid
	 */
	public final Point getGrid(){
		return iconGrid_;
	}

	/**
	 * returns icon size in Point. x = width, y = height.
	 * @return get bitmap size in Point
	 */
	public final Point getSize(){
		return iconSize_;
	}
}
