[back to PhotSpot](PhotSpot.md)
# Clustering Code and Samples #
Clustering Modules are now available.
## Module Source for com.erdao.android.mapviewutil ##
http://android-playground-erdao.googlecode.com/svn/trunk/SampleClusterMap/src/com/erdao/android/mapviewutil/
## Javadoc for com.erdao.android.mapviewutil ##
http://android-playground-erdao.googlecode.com/svn/trunk/SampleClusterMap/doc/index.html
## Full Sample Codes to use this classlib ##
http://android-playground-erdao.googlecode.com/svn/trunk/SampleClusterMap

<font color='red'>
NOTICE:<br />
You need to obtain YOUR API KEYs to run the application<br />
You have to press menu key and select "Display GeoItems" to invoke clustering, since there is no onReady kind of event for MapView.<br />
</font>


# Implementation Notes #
## Client (android) ##
For clustering, ported javascript version of [markerclusterer](http://code.google.com/p/gmaps-utility-library/) to java code.
There are other things in android SDK1.5r3 does not support and needed to tweak a bit.
Hope apis will get richer in the future.
  * There are no Bounds class
> like GLatLngBounds in javascript, there is no bound handling class for android. just made substitute class for it.
  * Map move/zoom event handling
> In javascript version of google maps, it can register event handler such as "moveend" and "zoomend" but not in android. for that I needed to hook the draw event of the marker to check the current screen projection (bounds) to know if it is moving or staying still.
  * ItemizedOverlay are easy but not customizable well enough
> there are plenty sample sources use ItemizedOverlay but I don't think this class is useful if you need to do many things with markers.
> ItemizedOverlay is good to overlay several "same-image" icons on the map, but like showing individual information like how many items within the cluster, needed to use Overlay class for this implementation.
  * Built-in zoom control can't hook events
> built-in zoom control are easy but can't hook press event, as well it only zoom to center of current screen so sometime marker in focus will get out of bounds. For that, I needed to implement zoom control as button and zoom manually.
  * Can't set zoom level directly
> apis given is ZoomIn or ZoomOut only zooming one level at a time, can't set directly to maximum zoom level.

## Server (appspot) ##
Since the 3 services differs in feeds that they provide, especially Picasa Web Albums JSON feed are huge compared to Panoramio. Picasa Web Albums now provide JSONC wich is much less data compared to full JSON feed, though it is still big enough for users to be irritated waiting for data to be loaded.
In addition, Picasa Web Albums and Flickr public feed contains photo sequences which are almost the same, people are taking photo of holiday and upload almost same shot to the site. Retrieving more than 100 feeds and then filtering out those similar image on the client takes time and not good solution for user experience.
Since appengine now supports Java, Java codes in android are quite easy to be ported to appengine servlet. There I will retrieve large amount of data from services and then filter them to be adequate to the client and provide the feed to client.
Currently, the filtering is really simple thing that checks same author's content with approximately same geo-coordinates.


# Source Codes #
  * PhotSpot(Client)
> http://code.google.com/p/android-playground-erdao/source/browse/#svn/trunk/PhotSpot
  * PhotSpotCloud(appEngine)
> http://code.google.com/p/android-playground-erdao/source/browse/#svn/trunk/PhotSpotCloud