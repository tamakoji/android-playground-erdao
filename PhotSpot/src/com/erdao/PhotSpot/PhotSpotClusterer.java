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

import java.util.List;

import android.view.View;
import android.widget.FrameLayout;

import com.erdao.android.mapviewutil.GeoItem;
import com.erdao.android.mapviewutil.markerclusterer.GeoClusterer;
import com.erdao.android.mapviewutil.markerclusterer.MarkerBitmap;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

/**
 * Custom GeoClusterer class for PhotSpot.
 * adding gallery and extra actions.
 * @author Huan Erdao
 */
public class PhotSpotClusterer extends GeoClusterer {
	
	/** acvitity handle for posting ToastMessage. */
	private PhotSpotActivity activityHndl_;
	/** MapView object. */
	private final MapView mapView_;
	/** FrameLayout object for gallery. */
	private final FrameLayout imageFrame_;
	/** Bitmap objects for marker icons */
	private final List<MarkerBitmap> markerIconBmps_;
	
	/**
	 * @param activityHndl		activity object.
	 * @param markerIconBmps	Bitmap objects for marker icons.
	 * @param mapView			MapView object.
	 * @param imageFrame		FrameLayout for gallery view.
	 */
	public PhotSpotClusterer(PhotSpotActivity activityHndl,
			List<MarkerBitmap> markerIconBmps, float screenDensity, MapView mapView, FrameLayout imageFrame){
		super(mapView, markerIconBmps, screenDensity);
		activityHndl_ = activityHndl;
		markerIconBmps_ = markerIconBmps;
		mapView_ = mapView;
		imageFrame_ = imageFrame;
		// override GRIDSIZE
		GRIDSIZE_ = 74;
	}

	/**
	 * Custom CreateCluster. Create PhotSpotGeoCluster object.
	 * @param item			GeoItem to be set.
	 */
	@Override
	public void createCluster(GeoItem item){
		PhotSpotGeoCluster cluster = new PhotSpotGeoCluster(this);
		cluster.addItem(item);
		clusters_.add(cluster);
	}

	/**
	 * Clears Gallery
	 */
	public void ClearGallery() {
		super.clearSelect();
		selcluster_ = null;
		imageFrame_.setVisibility(View.GONE);
	}
	
	/**
	 * Custom resetViewport.
	 * @return	current selected cluster.
	 */
	@Override
	public PhotSpotGeoCluster resetViewport() {
		PhotSpotGeoCluster cluster = (PhotSpotGeoCluster)super.resetViewport();
		if(cluster == null){
			imageFrame_.setVisibility(View.GONE);
//			TextView txtView = (TextView)imageFrame_.findViewById(R.id.copyright);
//			txtView.setVisibility(View.GONE);
		}
		else if( cluster != selcluster_ ){
			selcluster_ = cluster;
			cluster.showGallery();
			MapController mapCtrl = mapView_.getController();
			mapCtrl.animateTo(cluster.getLocation());
		}
		mapView_.invalidate();
		return cluster;
	}

	/**
	 * Custom GeoCluster class for PhotSpot.
	 * adding gallery and extra actions.
	 * @author Huan Erdao
	 */
	public class PhotSpotGeoCluster extends GeoCluster{
		/** PhotSpotClusterer object */
		private final PhotSpotClusterer clusterer_;

		/**
		 * @param clusterer PhotSpotClusterer object.
		 */
		public PhotSpotGeoCluster(PhotSpotClusterer clusterer){
			super(clusterer);
			clusterer_ = clusterer;
		}

		/**
		 * Show Gallery View
		 */
		public void showGallery(){
			((PhotSpotClusterMarker)clusterMarker_).showGallery();
		}

		/**
		 * Custom redraw function. Create PhotSpotClusterMarker here.
		 */
		@Override
		public void redraw(){
			if(!isInBounds(clusterer_.getCurBounds())) {
				return;
			}
			if(clusterMarker_ == null) {
				clusterMarker_ = new PhotSpotClusterMarker(activityHndl_,this,markerIconBmps_,screenDensity_,mapView_,imageFrame_);
				List<Overlay> mapOverlays = mapView_.getOverlays();
				mapOverlays.add(clusterMarker_);
			}
		}
	};
}
