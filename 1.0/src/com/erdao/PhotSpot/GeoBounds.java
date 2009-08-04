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

package com.erdao.PhotSpot;

import com.google.android.maps.GeoPoint;

/* Utility Class to handle GeoBounds, which is not available for 1.5SDK */
public class GeoBounds {
	private GeoPoint nw_;
	private GeoPoint se_;

	/* constructor */
	public GeoBounds( GeoPoint nw, GeoPoint se ){
		nw_ = nw;
		se_ = se;
	}
	
	/* isInBounds */
	public boolean isInBounds( GeoPoint pt ){
		return (pt.getLatitudeE6()<=nw_.getLatitudeE6()
				&&pt.getLatitudeE6()>=se_.getLatitudeE6()
				&&pt.getLongitudeE6()>=nw_.getLongitudeE6()
				&&pt.getLongitudeE6()<=se_.getLongitudeE6());
	}

	/* getSouthEast */
	public GeoPoint getSouthEast(){
		return se_;
	}

	/* getNorthWest */
	public GeoPoint getNorthWest(){
		return nw_;
	}

	/* isEqual */
	public Boolean isEqual(GeoBounds b){
		return (nw_.getLatitudeE6()==b.nw_.getLatitudeE6()&&
				nw_.getLongitudeE6()==b.nw_.getLongitudeE6()&&
				se_.getLatitudeE6()==b.se_.getLatitudeE6()&&
				se_.getLongitudeE6()==b.se_.getLongitudeE6());
	}


}
