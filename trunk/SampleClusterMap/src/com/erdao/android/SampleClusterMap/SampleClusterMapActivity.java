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

package com.erdao.android.SampleClusterMap;

import java.util.ArrayList;
import java.util.List;

import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.erdao.android.mapviewutil.GeoItem;
import com.erdao.android.mapviewutil.markerclusterer.GeoClusterer;
import com.erdao.android.mapviewutil.markerclusterer.MarkerBitmap;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;


public class SampleClusterMapActivity extends MapActivity {
	/* google map variables */
	/** MapView object */
	private MapView mapView_;
	/** MapController object */
	private MapController mapCtrl_;
	
	// sample geo items
	private final GeoItem[] geoItems_ = {
		new GeoItem(0,(int)(37.448743*1E6),(int)(-122.171938*1E6)),
		new GeoItem(1,(int)(37.427205999999998*1E6),(int)(-122.16911399999999*1E6)),
		new GeoItem(2,(int)(37.45919*1E6),(int)(-122.105645*1E6)),
		new GeoItem(3,(int)(37.447453000000003*1E6),(int)(-122.104304*1E6)),
		new GeoItem(4,(int)(37.414738*1E6),(int)(-122.18315*1E6)),
		new GeoItem(5,(int)(37.429670000000002*1E6),(int)(-122.173258*1E6)),
		new GeoItem(6,(int)(37.427536000000003*1E6),(int)(-122.16689599999999*1E6)),
		new GeoItem(7,(int)(37.423411999999999*1E6),(int)(-122.169127*1E6)),
	};

	// marker icons
	private List<MarkerBitmap> markerIconBmps_ = new ArrayList<MarkerBitmap>();

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // set up  map view and location
		mapView_ = (MapView)findViewById(R.id.mapview);
		mapCtrl_ = mapView_.getController();
		mapCtrl_.setZoom(13);
		mapView_.setBuiltInZoomControls(true);
		mapView_.displayZoomControls(true);
		int lat = (int)(37.443633512358176*1E6);
		int lng = (int)(-122.1658268152832*1E6);
		mapCtrl_.setCenter(new GeoPoint(lat,lng));
		mapView_.invalidate();

		// prepare for marker icons.
		// small icon for maximum 10 items
		markerIconBmps_.add(
			new MarkerBitmap(
					BitmapFactory.decodeResource(getResources(), R.drawable.balloon_s_n),
					BitmapFactory.decodeResource(getResources(), R.drawable.balloon_s_s),
					new Point(20,20),
					14,
					10)
			);
		// large icon. 100 will be ignored.
		markerIconBmps_.add(
				new MarkerBitmap(
						BitmapFactory.decodeResource(getResources(), R.drawable.balloon_l_n),
						BitmapFactory.decodeResource(getResources(), R.drawable.balloon_l_s),
						new Point(28,28),
						16,
						100)
				);
    }
	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * onCreateOptionsMenu handler
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuItem menu_Revert = menu.add(0,1,0,"Display GeoItems");
		menu_Revert.setIcon(android.R.drawable.ic_menu_myplaces);
		return true;
	}

	/**
	 * onOptionsItemSelected handler
	 * since clustering need MapView to be created and visible,
	 * this sample do clustering here.
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case 1: {
				// create clusterer instance
				float screenDensity = this.getResources().getDisplayMetrics().density;
				GeoClusterer clusterer = new GeoClusterer(mapView_,markerIconBmps_,screenDensity);
				// add geoitems for clustering
				for(int i=0; i<geoItems_.length; i++) {
					clusterer.addItem(geoItems_[i]);
				}
				// now redraw the cluster. it will create markers.
				clusterer.redraw();
				mapView_.invalidate();
				// now you can see items clustered on the map.
				// zoom in/out to see how icons change.
				break;
			}
		}
		return true;
	}
	
}