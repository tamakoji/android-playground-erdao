/**
 * @name GeoClusterer
 * @version 1.0
 * @author Huan Erdao
 * @copyright (c) 2009 Huan Erdao
 * @fileoverview
 * This javascript library comes from markerclusterer by Xiaoxi Wu
 * which partially ported to work with google maps v3 apis.
 */

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Projection overlay helper class
 * came from : http://snippets.aktagon.com/snippets/377-How-to-find-a-Google-Map-marker-s-exact-position
 * @constructor
 * @param {google.maps.Map} map The map that the markers should be added to.
 */
function ProjectionHelperOverlay(map) {
	this.setMap(map);
}
ProjectionHelperOverlay.prototype = new google.maps.OverlayView();
ProjectionHelperOverlay.prototype.draw = function() {
	if (!this.ready){
		this.ready = true;
		google.maps.event.trigger(this, 'ready');
	}
};

/**
 * GeoCluster class
 * @constructor
 * @param {google.maps.Map} map The map that the markers should be added to.
 */
function GeoClusterer(map) {
	// private members
	var clusters_ = [];
	var map_ = map;
	var maxZoom_ = null;
	var me_ = this;
	var gridSize_ = 90;
	var leftItems_ = [];
	var mcfn_ = null;
	var projectionHelper_ = new ProjectionHelperOverlay(map_);

	/**
	 * addItem.
	 */
	this.addItem = function (item) {
		if (!isItemInViewport_(item)) {
			leftItems_.push(item);
			return;
		}
		var projection = projectionHelper_.getProjection();
		var pos = projection.fromLatLngToDivPixel(item.latlng);
		var length = clusters_.length;
		var cluster = null;
		for (var i = length - 1; i >= 0; i--) {
			cluster = clusters_[i];
			var gpCenter = cluster.getCenter();
			if (gpCenter === null) {
				continue;
			}
			var ptCenter = projection.fromLatLngToDivPixel(gpCenter);
			// Found a cluster which contains the marker.
			if (pos.x >= ptCenter.x - gridSize_ && pos.x <= ptCenter.x + gridSize_ &&
							pos.y >= ptCenter.y - gridSize_ && pos.y <= ptCenter.y + gridSize_) {
				cluster.addItem(item);
				return;
			}
	    }
		// No cluster contain the marker, create a new cluster.
		createCluster(item);
	};
	
	/**
	 * Create Cluster Object.
	 * override this method, if you want to use custom GeoCluster class.
	 * @param item GeoItem to be set to cluster.
	 */
	 function createCluster(item){
	 	 cluster = new Cluster(me_, map);
		 cluster.addItem(item);
		 clusters_.push(cluster);
	 }
	 
	/**
	 * Redraw all clusters in viewport.
	 */
	this.redraw_ = function () {
		for (var i = 0; i < clusters_.length; ++i) {
			clusters_[i].redraw_(true);
		}
	};
	
	this.getProjectionHelper = function() {
		return projectionHelper_;
	};
	
	/**
	 * Get all clusters in viewport.
	 */
	this.getClustersInViewport_ = function () {
		var clusters = [];
		var curBounds = map_.getBounds();
		for (var i = 0; i < clusters_.length; i ++) {
			if (clusters_[i].isInBounds(curBounds)) {
				clusters.push(clusters_[i]);
			}
		}
		return clusters;
	};

	/**
	 * addleftItems_
	 */
	function addleftItems_() {
		if (leftItems_.length === 0) {
			return;
		}
		var leftItems = leftItems_.slice(0);
		leftItems_.length = 0;
		for (i = 0; i < leftItems.length; ++i) {
			me_.addItem(leftItems[i]);
		}
	}

	/**
	 * reAddItems_
	 */
	function reAddItems_(items) {
		var len = items.length;
		for (var i = len - 1; i >= 0; --i) {
			me_.addItem(items[i]);
		}
		addleftItems_();
	}

	/**
	 * Remove all markers from MarkerClusterer.
	 */
	this.clear = function () {
		for (var i = 0; i < clusters_.length; ++i) {
			if (typeof clusters_[i] !== "undefined" && clusters_[i] !== null) {
				clusters_[i].clear();
			}
		}
		clusters_ = [];
		leftItems_ = [];
		if(mcfn_){
			google.maps.event.removeListener(mcfn_);
			mcfn_ = null;
		}
	};

	/**
	 * addItems
	 */
	this.addItems = function (items) {
		for (var i = 0; i < items.length; ++i) {
			this.addItem(items[i]);
		}
		this.redraw_();
		// when map move end, regroup.
		mcfn_ = google.maps.event.addListener(map_, "bounds_changed", function () {
			me_.resetViewport();
		});
	};

	/**
	 * isItemInViewport_
	 */
	function isItemInViewport_(item) {
		return map_.getBounds().contains(item.latlng);
	}

	/**
	 * Get grid size
	 */
	this.getGridSize_ = function () {
		return gridSize_;
	};

	/**
	 * Collect all markers of clusters in viewport and regroup them.
	*/
	this.resetViewport = function () {
		var clusters = this.getClustersInViewport_();
		var tmpItems = [];
		var removed = 0;
		for (var i = 0; i < clusters.length; ++i) {
			var cluster = clusters[i];
			var oldZoom = cluster.getCurrentZoom();
			if (oldZoom === null) {
				continue;
			}
			var curZoom = map_.getZoom();
			if (curZoom !== oldZoom) {
				// If the cluster zoom level changed then destroy the cluster
				// and collect its markers.
				var citms = cluster.getItems();
				for (j = 0; j < citms.length; ++j) {
					tmpItems.push(citms[j]);
				}
				cluster.clear();
				removed++;
				for (j = 0; j < clusters_.length; ++j) {
					if (cluster === clusters_[j]) {
						clusters_.splice(j, 1);
					}
				}
			}
		}
		// Add the markers collected into marker cluster to reset
		reAddItems_(tmpItems);
		this.redraw_();
	};

	this.onClickMarker = function (cluster){
	};

}

/**
 Cluster 
 */
function Cluster(geoClusterer,map) {
	var center_ = null;
	var items_ = [];
	var geoClusterer_ = geoClusterer;
	var map_ = map;
	var clusterMarker_ = null;
	var zoom_ = map_.getZoom();

	this.getItems = function () {
		return items_;
	};

	this.isInBounds = function (bounds) {
		if (center_ === null) {
			return false;
		}
		if (!bounds) {
			bounds = map_.getBounds();
		}
		var ph = geoClusterer_.getProjectionHelper();
		var projection = ph.getProjection();
		var sw = projection.fromLatLngToDivPixel(bounds.getSouthWest());
		var ne = projection.fromLatLngToDivPixel(bounds.getNorthEast());
		var mapcenter = map_.getCenter();
		var centerxy = projection.fromLatLngToDivPixel(center_);
		var inViewport = true;
		var gridSize = geoClusterer_.getGridSize_();
		if (zoom_ !== map_.getZoom()) {
			var dl = map_.getZoom() - zoom_;
			gridSize = Math.pow(2, dl) * gridSize;
		}
		if (ne.x !== sw.x && (centerxy.x + gridSize < sw.x || centerxy.x - gridSize > ne.x)) {
			inViewport = false;
		}
		if (inViewport && (centerxy.y + gridSize < ne.y || centerxy.y - gridSize > sw.y)) {
			inViewport = false;
		}
		return inViewport;
	};

	/**
	 * Get cluster center.
	 */
	this.getCenter = function () {
		return center_;
	};

	/**
	 * Add a marker.
	 */
	this.addItem = function (item) {
		if (center_ === null) {
			center_ = item.latlng;
		}
		items_.push(item);
	};

	/**
	 * Get current zoom level of this cluster.
	 */
	this.getCurrentZoom = function () {
		return zoom_;
	};

	this.redraw_ = function(){
		if(!this.isInBounds()) {
			return;
		}
		if(clusterMarker_ == null) {
			clusterMarker_ = new ClusterMarker_(map_,this,geoClusterer_.onClickMarker);
		}
	}

	/**
	 * Remove all the markers from this cluster.
	*/
	this.clear = function () {
		if (clusterMarker_ !== null) {
			clusterMarker_.setMap(null);
			clusterMarker_ = null;
		}
		items_ = [];
	};

	/**
	 * Get number of markers.
	 */
	this.getItemSize = function () {
		return items_.length;
	};
}

/**
 * ClusterMarker_ creates a marker that shows the number of markers that
 */
function ClusterMarker_(map, cluster, clickCallback) {
	this.cluster_ = cluster;
	this.latlng_ = cluster.getCenter();
	var count = cluster.getItemSize();
	this.count_ = count;
	this.icon_ = count >= 10 ? "http://gmaps-utility-library.googlecode.com/svn/trunk/markerclusterer/images/m3.png" : "http://gmaps-utility-library.googlecode.com/svn/trunk/markerclusterer/images/m1.png";
	this.width_ = count >= 10 ? 66 : 53;
	this.height_ = count >= 10 ? 65 : 52;
	this.mstyle_ = "background-image:url(" + this.icon_ + ");";
	this.mstyle_ += 'height:' + this.height_ + 'px;line-height:' + this.height_ + 'px;';
	this.mstyle_ += 'width:' + this.width_ + 'px;text-align:center;';
	this.clickCallback_ = clickCallback;
	this.setMap(map);
}

ClusterMarker_.prototype = new google.maps.OverlayView();

ClusterMarker_.prototype.draw = function() {
	this.projection_ = this.getProjection();
	var map = this.getMap();
	var pos = this.projection_.fromLatLngToDivPixel(this.latlng_);
	pos.x -= parseInt(this.width_ / 2, 10);
	pos.y -= parseInt(this.height_ / 2, 10);
	this.div_.style.left = pos.x + "px";
	this.div_.style.top = pos.y + "px";
	var txtColor = 'white';
	this.div_.style.cssText = this.mstyle_ + 'cursor:pointer; top:' + pos.y + "px;left:"
		+ pos.x + "px; color:" + txtColor +  ";position:absolute;font-size:11px;"
		+ 'font-family:Arial,sans-serif;font-weight:bold';
	this.div_.innerHTML = this.count_;
};

ClusterMarker_.prototype.onAdd = function() {
	var map = this.getMap();
	var div = document.createElement("div");
	div.style.visibility = 'visible';
	var cluster = this.cluster_;
	var callback = this.clickCallback_;
	google.maps.event.addDomListener(div, "click", function () {
		callback(cluster);
	});
	this.div_ = div;
	this.getPanes().mapPane.appendChild(this.div_);
};

ClusterMarker_.prototype.remove = function() {
	this.div_.parentNode.removeChild(this.div_);
};

