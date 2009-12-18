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

package com.erdao.utils;

import android.graphics.Bitmap;

/**
 * Custom Bitmap Class with LazyLoad
 * @author Huan Erdao
 */
public class LazyLoadBitmap {
	
	public class LoadState{
		public static final int	BITMAP_STATE_NULL			= 0;
		public static final int BITMAP_STATE_PRE_LOADING	= 1;
		public static final int BITMAP_STATE_PRE_LOADED		= 2;
		public static final int BITMAP_STATE_FULL_LOADING	= 3;
		public static final int BITMAP_STATE_FULL_LOADED	= 4;
	};

	/** Bitmap object */
	protected Bitmap bitmap_;
	/** state for preloading */
	protected int state_;
	
	public LazyLoadBitmap(){
		bitmap_ = null;
		state_ = LoadState.BITMAP_STATE_NULL;
	}

	public LazyLoadBitmap(Bitmap bmp, int state){
		bitmap_ = bmp;
		state_ = state;
	}

	/**
	 * Get bitmap object
	 * @return	Bitmap object
	 */
	public final Bitmap getBitmap(){
		return bitmap_;
	}

	/**
	 * Set bitmap object
	 * @param	bmp		Bitmap object
	 */
	public void setBitmap(Bitmap bmp){
		bitmap_ = bmp;
	}

	/**
	 * Recycle bitmap object
	 * @return true if success, false if bitmap was null.
	 */
	public boolean recycle(){
		if(bitmap_!=null){
			bitmap_.recycle();
			bitmap_ = null;
			state_ = LoadState.BITMAP_STATE_NULL;
			return true;
		}
		return false;
	}

	
	/**
	 * Get Loading state
	 * @return	LoadState.
	 */
	public final int getState(){
		return state_;
	}

	/**
	 * Set PreLoad state
	 * @param	state LoadState.
	 */
	public void setState(int state){
		state_ = state;
	}
}