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
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.graphics.Point;
import android.os.Handler;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

/* Class for Clustering geotagged content 
 * this clustering came from "markerclusterer" which is available as opensource at
 * http://code.google.com/p/gmaps-utility-library/
 * this is android ported version with modification to fit to the application
 */
public class GeoClusterer {
	
	/* variables */
	private final Context context_;
	private final MapView mapView_;
	private final FrameLayout imageFrame_;
	private ArrayList<PhotoItem> items_ = new ArrayList<PhotoItem>();
	private ArrayList<PhotoItem> leftItems_ = new ArrayList<PhotoItem>();
	private ArrayList<GeoCluster> clusters_ = new ArrayList<GeoCluster>();
	private int gridSize_ = 40;
	private GeoCluster selcluster_ = null;
	private int checkcnt_ = 0;
	private GeoBounds checkBound_;
	private boolean isMoving_;
	private Handler handler_;

	/* constructor */
	public GeoClusterer(Context context, MapView mapView, FrameLayout imageFrame){
		context_ = context;
		mapView_ = mapView;
		imageFrame_ = imageFrame;
		handler_ = new Handler();
	}

	/* addItem */
	public void addItem(PhotoItem item) {
		// if not in viewport, add to leftItems_
		if(!isItemInViewport(item)) {
			leftItems_.add(item);
			return;
		}
		// else add to items_;
		items_.add(item);
		int length = clusters_.size();
		GeoCluster cluster = null;
		Projection proj = mapView_.getProjection();
		Point pos = proj.toPixels(item.getLocation(), null);
		// check existing cluster
		for(int i=length-1; i>=0; i--) {
			  cluster = clusters_.get(i);
			  GeoPoint gpCenter = cluster.getLocation();
			  if(gpCenter == null)
				  continue;
			  Point ptCenter = proj.toPixels(gpCenter,null);
			  // find a cluster which contains the marker.
			  if(pos.x >= ptCenter.x - gridSize_ && pos.x <= ptCenter.x + gridSize_ &&
				  pos.y >= ptCenter.y - gridSize_ && pos.y <= ptCenter.y + gridSize_) {
				  cluster.addItem(item);
//				  cluster.redraw();
				  return;
			  }
		}
		// No cluster contain the marker, create a new cluster.
		cluster = new GeoCluster(this);
		cluster.addItem(item);
		// Add Cluster to ArrayList
		clusters_.add(cluster);
//		cluster.redraw();
	}

	/* redraw */
	public void redraw(){
		for(int i=0; i<clusters_.size(); i++) {
			clusters_.get(i).redraw();
		}
	}

	/* zoomInFixing */
	public void zoomInFixing(){
		if(selcluster_!=null){
			GeoPoint gpt = selcluster_.getSelectedItemLocation();
			if(getCurBounds().isInBounds(gpt)){
				Projection pro = mapView_.getProjection();
				Point ppt = pro.toPixels(gpt, null);
				MapController mapCtrl = mapView_.getController();
				mapCtrl.zoomInFixing(ppt.x, ppt.y);
			}
			else{
				MapController mapCtrl = mapView_.getController();
				mapCtrl.zoomIn();
			}
		}
		else{
			MapController mapCtrl = mapView_.getController();
			mapCtrl.zoomIn();
		}
	}
	
	/* isItemInViewport */
	private boolean isItemInViewport(PhotoItem item){
		checkBound_ = getCurBounds();
		isMoving_ = false;
		return checkBound_.isInBounds(item.getLocation());
	}

	/* getCurBounds */
	private GeoBounds getCurBounds(){
		Projection proj = mapView_.getProjection();
		return new GeoBounds(proj.fromPixels(0,0),proj.fromPixels(mapView_.getWidth(),mapView_.getHeight()));
	}

	/* getMapView */
	private MapView getMapView(){
		return mapView_;
	}

	/* getGridSize */
	private int getGridSize(){
		return gridSize_;
	}

	/* getClustersInViewport */
	private ArrayList<GeoCluster> getClustersInViewport() {
		GeoBounds curBounds = getCurBounds();
		ArrayList<GeoCluster> clusters = new ArrayList<GeoCluster>();
		for(int i=0; i<clusters_.size(); i++) {
			GeoCluster cluster = clusters_.get(i);
			  if(cluster.isInBounds(curBounds)) {
				  clusters.add(cluster);
			  }
		}
		return clusters;
	}

	/* addLeftItems */
	private void addLeftItems() {
		if(leftItems_.size()==0){
			return;
		}
		ArrayList<PhotoItem> leftItems = new ArrayList<PhotoItem>();
		leftItems.addAll(leftItems_);
		leftItems_.clear();
		for(int i=0; i<leftItems.size(); i++) {
			addItem(leftItems.get(i));
		}
	}

	/* reAddItems */
	private void reAddItems(ArrayList<PhotoItem> items) {
		int len = items.size();
		for(int i=len-1; i>=0; i--) {
			addItem(items.get(i));
		}
		addLeftItems();
	}

	/* resetViewport */
	private void resetViewport() {
//		Log.i("DEBUG","resetViewport");
		ArrayList<GeoCluster> clusters = getClustersInViewport();
		ArrayList<PhotoItem> tmpItems = new ArrayList<PhotoItem>();
		int removed = 0;
		for(int i=0; i<clusters.size(); i++) {
			GeoCluster cluster = clusters.get(i);
			int oldZoom = cluster.getCurrentZoom();
			int curZoom = mapView_.getZoomLevel();
			// If the cluster zoom level changed then destroy the cluster and collect its markers.
			if(curZoom != oldZoom) {
				tmpItems.addAll(cluster.getItems());
				cluster.clearMarker();
				removed++;
				for(int j=0; j<clusters_.size(); j++) {
					if(cluster == clusters_.get(j)) {
						clusters_.remove(j);
					}
				}
			}
		}
		reAddItems(tmpItems);
		redraw();
		// Add the markers collected into marker cluster to reset
		if(removed>0){
			boolean dismiss = true;
			for(int i=0; i<clusters_.size(); i++) {
				GeoCluster cluster = clusters_.get(i);
				if( cluster.isSelected() ){
					dismiss = false;
					selcluster_ = cluster;
					cluster.showGallery();
					MapController mapCtrl = mapView_.getController();
					mapCtrl.animateTo(cluster.getLocation());
					break;
				}
			}
			if(dismiss){
				imageFrame_.setVisibility(View.GONE);
				TextView txtView = (TextView)imageFrame_.findViewById(R.id.copyright);
				txtView.setVisibility(View.GONE);
			}
		}
		mapView_.invalidate();
	}

	/* onNotifyDraw
	 * Little tweek to enable catching zoom/move event when markers are present.
	 * hope there will be event notification for android equivalent to javascriptin the future....
	 */
	public void onNotifyDraw(){
		if(!isMoving_){
			GeoBounds curBnd = getCurBounds();
			if( !checkBound_.isEqual(curBnd) ){
				isMoving_ = true;
				checkBound_ = curBnd;
//				Log.i("DEBUG","movestart");
				Timer timer = new Timer(true);
				timer.schedule(
					new TimerTask() {
						public void run() {
							//Log.i("DEBUG","timertask");
							GeoBounds curBnd = getCurBounds();
							if( checkBound_.isEqual(curBnd) ){
								isMoving_ = false;
								//Log.i("DEBUG","moveend");
								this.cancel();
								handler_.post( new Runnable() {
									public void run() {
										resetViewport();
									}
								});
							}
							checkBound_ = curBnd;
						}
					}, 500, 500
				);
			}
		}
	}

	/* onTap, not usual onTap method. called from Cluster Layer */
	public void onTap(GeoCluster caller,boolean tapped) {
		if(tapped){
			if(selcluster_ == caller)
				return;
			for(int i=0; i<clusters_.size(); i++) {
				if(selcluster_==clusters_.get(i)) {
					clusters_.get(i).clearSelect();
				}
			}
			selcluster_ = caller;
		}
		else{
			checkcnt_++;
			if( checkcnt_ == clusters_.size() ){
				checkcnt_ = 0;
				/* TODO : do something if no marker was tapped
				 */
//				selcluster_.clearSelect();
			}
		}
	}

	/* getCluster */
	public GeoCluster getCluster(int id) {
		return clusters_.get(id);
	}
	
	/* Cluster class - GeoCluster
	 * contains single marker object(ClusterMarker). mostly wraps methods in ClusterMarker.
	 */
	public class GeoCluster {
		/* variables */
		private final GeoClusterer clusterer_;
		private final MapView mapView_;
		private GeoPoint center_;
		private ArrayList<PhotoItem> items_ = new ArrayList<PhotoItem>();
		private ClusterMarker clusterMarker_;
		private int zoom_;

		/* constructor */
		public GeoCluster(GeoClusterer clusterer){
			clusterer_ = clusterer;
			mapView_ = clusterer_.getMapView();
			zoom_ = mapView_.getZoomLevel();
		}
		
		/* addItem */
		public void addItem(PhotoItem item){
			if(center_ == null){
				center_ = item.getLocation();
			}
			items_.add(item);
		}
		
		/* getLocation */
		public GeoPoint getLocation(){
			return center_;
		}

		/* getSelectedItemLocation */
		public GeoPoint getSelectedItemLocation(){
			return clusterMarker_.getSelectedItemLocation();
		}

		/* showGallery */
		public void showGallery(){
			clusterMarker_.showGallery();
		}

		/* clearSelect */
		public void clearSelect(){
			clusterMarker_.clearSelect();
		}

		/* isSelected */
		public boolean isSelected(){
			return clusterMarker_.isSelected();
		}

		/* getCurrentZoom */
		public int getCurrentZoom(){
			return zoom_;
		}
		
		/* getItems */
		public ArrayList<PhotoItem> getItems(){
			return items_;
		}

		/* onNotifyDraw */
		public void onNotifyDraw(){
			clusterer_.onNotifyDraw();
		}

		/* onTap - not usual onTap. called from ClusterMarker */
		public void onTap(boolean flg) {
			clusterer_.onTap(this,flg);
		}
		
		/* clearMarker */
		public void clearMarker() {
			if(clusterMarker_ != null) {
				List<Overlay> mapOverlays = mapView_.getOverlays();
				if(mapOverlays.contains(clusterMarker_)){
					mapOverlays.remove(clusterMarker_);
				}
			}
			items_ = null;
		}

		/* redraw */
		public void redraw(){
			if(!isInBounds(clusterer_.getCurBounds())) {
				return;
			}
			if(clusterMarker_ == null) {
				clusterMarker_ = new ClusterMarker(this,mapView_,context_,imageFrame_);
				List<Overlay> mapOverlays = mapView_.getOverlays();
				mapOverlays.add(clusterMarker_);
			}
		}

		/* isInBounds */
		private boolean isInBounds(GeoBounds bounds) {
			if(center_ == null) {
				return false;
			}
			Projection pro = mapView_.getProjection();
			Point nw = pro.toPixels(bounds.getNorthWest(),null);
			Point se = pro.toPixels(bounds.getSouthEast(),null);
			Point centxy = pro.toPixels(center_,null);
			boolean inViewport = true;
			int gridSize = clusterer_.getGridSize();
			if(zoom_ != mapView_.getZoomLevel()) {
				int diff = mapView_.getZoomLevel() - zoom_;
				gridSize = (int) (Math.pow(2, diff) * gridSize);
			}
			if(nw.x != se.x && (centxy.x + gridSize < nw.x || centxy.x - gridSize > se.x)) {
				inViewport = false;
			}
			if(inViewport && (centxy.y + gridSize < nw.y || centxy.y - gridSize > se.y)) {
				inViewport = false;
			}
			return inViewport;
		}
	};
}
