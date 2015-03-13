<img src='http://android-playground-erdao.googlecode.com/svn/wiki/img/PhotSpot/photspot_logo.png'>
<h1>Introduction</h1>
PhotSpot is a GoogleMaps and public geotagged photo content mashup app to discover popular places.<br>
It clusteres the geo information and show them on the map as balloon marker.<br>
Clustering the marker is essential for application those want to manage many geotagged content on the screen, though displaying all the markers makes UI slow and let user hard to see the information.<br>
<br>
<hr />
<h1>Alternative Resource</h1>
If you'd like to see only app info, you can also get them <a href='http://erdao.info/blog/justmemo/?page_id=275'>here</a>.<br>
<hr />
<h1>Screen Cast</h1>
<a href='http://www.youtube.com/watch?feature=player_embedded&v=tcO6uoy80tg' target='_blank'><img src='http://img.youtube.com/vi/tcO6uoy80tg/0.jpg' width='425' height=344 /></a><br>
<hr />
<h1>Screen Shots Gallery</h1>
<a href='ScreenShots.md'>Screen Shots Gallery</a>
<hr />
<h1>App Download</h1>
<b>from Android Market</b>
<blockquote><a href='http://market.android.com/search?q=com.erdao.PhotSpot'>click from android browser</a>
<hr /></blockquote>

<h1>Info</h1>
<font color='red'>
<b>2009/11/24 ver 1.6.1 released</b>
<ul><li>Support for multiple resolution(e.g. DROID)<br>
</li><li>Fix some FC bugs.<br>
</font>
<b>2009/10/05 ver 1.5.1 released</b>
</li><li>Fixing some Out of Memory issue upon opening lots of images.<br>
<blockquote>try to recycle old bitmaps<br>
</blockquote></li><li>Support Lazy-Loading Images<br>
<blockquote>to save memory<br>
<b>2009/10/03 ver 1.5.0 released</b>
</blockquote></li><li>MySpot Search function added.<br>
<b>2009/09/23 ver 1.4.2 released</b>
</li><li>Mainly Code Refactoring.<br>
</li><li>Balloon icon reflects current service using.<br>
</li><li>Marker Clusterer is now modularized. you can easily make android map application with clustering. for detail, see <a href='DeveloperResource.md'>Developer Resource</a>.<br>
<b>2009/09/09 ver 1.4.1 released</b>
</li><li>Bugfix:Gallery Veiw in Favorites does not sync with title<br>
<b>2009/09/07 ver 1.4.0 released</b>
</li><li>support gallery image size change by gesture.<br>
<ul><li>swipe up thumbnail to shrink, swipe down to enlarge gallery thumbnail size.<br>
</li><li>swipe up to the frame limit will hide gallery view.<br>
</li></ul></li><li>gallery view support on favorites<br>
</li><li>"long-press" thumbnail action been changed to "tap"<br>
</li><li>icon design change</li></ul>

<hr />
<h1>TODO List</h1>
I am not sure if I would really implement below functions:<br>
<ul><li>Time based filtering of the photo content<br>
</li><li>Data filtering and/or categorization for the spots<br>
<ul><li>food/people/buildings/pets/animals...?<br>
</li></ul></li><li>Route based search<br>
<blockquote>Not only based on location, letting user find what is on their way<br>
</blockquote></li><li>use k-means for clustering<br>
<blockquote>make k=10 to get top-10 spots?<br>
</blockquote></li><li>Invoke Radar?<br>
</li><li>Spots Sharing function(e-mail?)<br>
</li><li>Store Favorite db to SD?<br>
</li><li>Enrich Gallery UI?</li></ul>

<hr />
<h1>Developer Resource</h1>
<a href='DeveloperResource.md'>Developer Resource</a>
<hr />
<h1>Special Thanks</h1>
<ul><li>Thanks to reviewers in Android Market<br>
</li><li>Thanks to all users who upload geotagged photo content to public<br>
</li><li>Thanks to Dr. Andreas Paepcke at Stanford Univ. for mentoring<br>
</li><li>Thanks to Google for providing ION phone :-)<br>
</li><li>This project was done under affiliation with Stanford HCI Group until Jan/10