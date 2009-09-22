package com.erdao.SampleClusterMap;

import android.os.Bundle;

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
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
		mapView_ = (MapView)findViewById(R.id.mapview);
		mapCtrl_ = mapView_.getController();
		mapCtrl_.setZoom(15);
		mapView_.setBuiltInZoomControls(true);
		mapView_.displayZoomControls(true);
		int lat = (int)(37.443633512358176*1E6);
		int lng = (int)(-122.1658268152832*1E6);
		mapCtrl_.setCenter(new GeoPoint(lat,lng));
		mapView_.invalidate();

    }
	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}
}