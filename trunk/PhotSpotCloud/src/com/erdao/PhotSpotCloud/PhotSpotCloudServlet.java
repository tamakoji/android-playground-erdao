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

package com.erdao.PhotSpotCloud;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@SuppressWarnings("serial")
public class PhotSpotCloudServlet extends HttpServlet {
	private static final int MODE_PANORAMIO		= 0;
	private static final int MODE_PICASA		= 1;
	private static final int MODE_FLICKR		= 2;
	private static final int MODE_PICASA_WUSER	= 3;
	private static final int MODE_FLICKR_WUSER	= 4;
	private static final int MODE_LOCALSEARCH	= 5;
	private int feedCount_ = 0;
	private static final Logger log = Logger.getLogger(PhotSpotCloudServlet.class.getName());
	private static final int MAX_FEED_PUB_SPOTS = 100;
	private static final int MAX_FEED_MY_SPOTS = 200;

	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		URL requestUrl = null;
		String compactJson = "";
		StringBuilder strbuilder = null;
		String lat = null;
		String lng = null;
		String service = null;
		int svcMode = MODE_PANORAMIO;
		String nwlat = null;
		String nwlng = null;
		String selat = null;
		String selng = null;
		String userid = null;

		// common params
		String qMode = req.getParameter("q");
		String debugstr = req.getParameter("dbg");
		if(debugstr!=null){
			try {
				debugstr = URLDecoder.decode(debugstr,"UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		String jsoncb = req.getParameter("callback");
		// request branch
		if(qMode==null||qMode.equals("searchspot")){
			nwlat = req.getParameter("nwlat");
			nwlng = req.getParameter("nwlng");
			selat = req.getParameter("selat");
			selng = req.getParameter("selng");
			service = req.getParameter("svc");
			userid = req.getParameter("userid");
			if(nwlat==null||nwlng==null||selat==null||selng==null||service==null)
				return;
			if(service.equals("picasa")){
				if(userid!=null)
					svcMode = MODE_PICASA_WUSER;
				else
					svcMode = MODE_PICASA;
			}
			else if(service.equals("panoramio"))
				svcMode = MODE_PANORAMIO;
			else{
				if(userid!=null)
					svcMode = MODE_FLICKR_WUSER;
				else
					svcMode = MODE_FLICKR;
			}
			requestUrl = new URL(createPhotoFeedUrl(svcMode,nwlat,nwlng,selat,selng,userid));
		}
		else if(qMode.equals("localsearch")){
			String latlng = req.getParameter("latlng");
			if(latlng==null)
				return;
			String token[] = latlng.split(",");
			lat = token[0];
			lng = token[1];
			if(lat==null||lng==null)
				return;
			svcMode = MODE_LOCALSEARCH;
			requestUrl = new URL("http://ajax.googleapis.com/ajax/services/search/local?v=1.0&q=*&key="+APIKeys.google_ajax_key+"&sll="+lat+","+lng);
			log.info("requestUrl:"+requestUrl);
		}
		if(requestUrl==null)
			return;
		resp.setContentType("text/plain");
		resp.setCharacterEncoding("utf-8");
		final int MAX_RETRY = 2;
		boolean openfeedSuccess = false;
		for(int i = 0; i < MAX_RETRY; i++ ){
			strbuilder = openFeed(requestUrl);
			if(strbuilder!=null){
				openfeedSuccess = true;
				break;
			}
		}
		if( strbuilder != null ){
			compactJson = compactJsonFeed(svcMode,strbuilder.toString(),userid);
			if(compactJson!=null){
				if(jsoncb!=null)
					resp.getWriter().println(jsoncb+"("+compactJson+")");
				else
					resp.getWriter().println(compactJson);
			}
		}
		if(svcMode==MODE_LOCALSEARCH)
			log.info("localsearch from="+req.getRemoteAddr()+",user-agent="+req.getHeader("user-agent")+",latlng="+lat+","+lng+",dbg="+debugstr+",openfeed="+openfeedSuccess+",feedcount="+feedCount_);
		else
			log.info("spotsearch from="+req.getRemoteAddr()+",user-agent="+req.getHeader("user-agent")+",bbox="+nwlat+","+nwlng+","+selat+","+selng+",svc="+service+",dbg="+debugstr+",openfeed="+openfeedSuccess+",feedcount="+feedCount_);
	}
	
	private StringBuilder openFeed(URL url){
		HttpURLConnection connection;
		StringBuilder strbuilder = null;
		BufferedReader reader;
		try {
			connection = (HttpURLConnection) url.openConnection();
			connection.setUseCaches(false);
			connection.setDoInput(true);
			connection.setRequestProperty("Content-type","text/plain");
			connection.setRequestMethod("GET");
			if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
				InputStream is = connection.getInputStream();
				InputStreamReader isr = new InputStreamReader(is,"UTF-8");
				reader = new BufferedReader(isr,2048);
				strbuilder = new StringBuilder();
				String line = null;
				while ((line = reader.readLine()) != null) {
					strbuilder.append(line + "\n");
				}
				is.close();
			}
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		return strbuilder;
	}
	
	private String createPhotoFeedUrl(int svcMode, String nwlat, String nwlng, String selat, String selng, String userid) {
		String url = "";
		switch(svcMode){
			default:
			case MODE_PANORAMIO:{
				url = "http://www.panoramio.com/map/get_panoramas.php?order=popularity&set=full&size=small&minx="+nwlng+"&miny="+selat+"&maxx="+selng+"&maxy="+nwlat+"&from=0&to=100";
				break;
			}
			case MODE_PICASA:{
				url = "http://picasaweb.google.com/data/feed/api/all?alt=jsonc&kind=photo&bbox="+nwlng+","+selat+","+selng+","+nwlat+"&max-results=400";
				break;
			}
			case MODE_PICASA_WUSER:{
				url = "http://picasaweb.google.com/data/feed/api/user/"+userid+"/?alt=jsonc&kind=photo&bbox="+nwlng+","+selat+","+selng+","+nwlat+"&max-results=400";
				break;
			}
			case MODE_FLICKR:{
				url = "http://api.flickr.com/services/rest/?method=flickr.photos.search&format=json&api_key="+APIKeys.flickr_key+"&per_page=400&extras=geo&min_taken_date=2005-1-1+00%3A00%3A00&bbox="+nwlng+","+selat+","+selng+","+nwlat;
				break;
			}
			case MODE_FLICKR_WUSER:{
				url = "http://api.flickr.com/services/rest/?method=flickr.photos.search&format=json&api_key="+APIKeys.flickr_key+"&per_page=400&extras=geo&user_id="+userid+"&min_taken_date=1980-1-1+00%3A00%3A00&bbox="+nwlng+","+selat+","+selng+","+nwlat;
				break;
			}
		}
		log.info("spotsearch raw="+url);
		return url;
	}

	
	/* compactJsonFeed - compact json feed
	 */
	private String compactJsonFeed(int svcMode, String fullJson, String userid){
		String compactJson = "";
		JSONObject jsonobj = null;
		JSONArray compactArray = new JSONArray();
		try {
			JSONArray array = null;
			switch( svcMode ){
				default:
				case MODE_PANORAMIO:{
					jsonobj = new JSONObject(fullJson);
					array = jsonobj.getJSONArray("photos");
					break;
				}
				case MODE_PICASA:
				case MODE_PICASA_WUSER:{
					jsonobj = new JSONObject(fullJson);
					array = jsonobj.getJSONArray("photos");
					break;
				}
				case MODE_FLICKR:
				case MODE_FLICKR_WUSER:{
					fullJson = fullJson.substring(14, fullJson.length());
					jsonobj = new JSONObject(fullJson);
					array = jsonobj.getJSONObject("photos").getJSONArray("photo");
					break;
				}
				case MODE_LOCALSEARCH: {
					jsonobj = new JSONObject(fullJson);
					array = jsonobj.getJSONObject("responseData").getJSONArray("results");
					break;
				}
			}
			int total = array.length();
			int compactArrayCount = 0;
			String author_p = "";
			double lat_p = 0.0;
			double lng_p = 0.0;
			for (int i = 0; i < total; i++) {
				JSONObject obj = array.getJSONObject(i);
				long id = 0;
				String title = null, thumbUrl = null, photoUrl = null, author= null;
				double lat = 0.0,lng = 0.0;
				switch( svcMode ){
					default:
					case MODE_PANORAMIO:{
						id = obj.getLong("photo_id");
						title = obj.getString("photo_title");
						thumbUrl = obj.getString("photo_file_url");
						photoUrl = obj.getString("photo_url");
						lat = obj.getDouble("latitude");
						lng = obj.getDouble("longitude");
						author = obj.getString("owner_name");
						break;
					}
					case MODE_PICASA:
					case MODE_PICASA_WUSER:{
						id = obj.getLong("id");
						title = obj.getString("title");
						JSONObject mediaobj = obj.getJSONObject("media");
						JSONObject linkobj = obj.getJSONObject("links");
						thumbUrl = mediaobj.getJSONArray("thumbnails").getString(2);
						photoUrl = linkobj.getString("alternate");
						String latlng = obj.getJSONObject("geo").getString("point");
						String token[] = latlng.split(" ");
						lat = Double.valueOf(token[0]);
						lng = Double.valueOf(token[1]);
						if(svcMode==MODE_PICASA_WUSER)
							author = userid;
						else
							author = obj.getString("author");
						break;
					}
					case MODE_FLICKR: {
						String server = obj.getString("server");
						String secret = obj.getString("secret");
						id = obj.getLong("id");
						title = obj.getString("title");
						thumbUrl = "http://static.flickr.com/"+server+"/"+id+"_"+secret+"_m.jpg";
						photoUrl = "http://www.flickr.com/photos/"+obj.getString("owner")+"/"+id;
						lat = obj.getDouble("latitude");
						lng = obj.getDouble("longitude");
						author = obj.getString("owner"); // todo:get real user name..
						break;
					}
					case MODE_LOCALSEARCH: {
						title = obj.getString("titleNoFormatting");
						break;
					}
				}
				if(svcMode==MODE_LOCALSEARCH){
					JSONObject comactObj = new JSONObject();
					comactObj.put("title", title);
					compactArray.put(comactObj);
				}
				else{
					// eliminate same author with same location.
					// TODO: do more intelligent filtering.
					if(userid==null){
						if(author.contentEquals(author_p)){
							if((lat>(lat_p-0.0001))&&(lat<(lat_p+0.001))||(lng>(lng_p-0.001))&&(lng<(lng_p+0.001))){
								continue;
							}
						}
					}
					JSONObject comactObj = new JSONObject();
					comactObj.put("id", id);
					comactObj.put("title", title);
					comactObj.put("author", author);
					comactObj.put("thumbUrl", thumbUrl);
					comactObj.put("photoUrl", photoUrl);
					comactObj.put("lat", lat);
					comactObj.put("lng", lng);
					compactArray.put(comactObj);
					if(userid==null){
						if(++compactArrayCount>MAX_FEED_PUB_SPOTS)
							break;
					}else{
						if(++compactArrayCount>MAX_FEED_MY_SPOTS)
							break;
					}
					author_p = author;
					lat_p = lat;
					lng_p = lng;
				}
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			compactJson = "{\"count\":0}";
			feedCount_ = 0;
			return null;
		}
		try {
			JSONObject result = new JSONObject();
			feedCount_ = compactArray.length();
			result.put("count", feedCount_);
			if(svcMode==MODE_LOCALSEARCH)
				result.put("result", compactArray);
			else
				result.put("photo", compactArray);
			compactJson = result.toString();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			compactJson = "{\"count\":0}";
			e.printStackTrace();
		}
		return compactJson;
	}
	
}
