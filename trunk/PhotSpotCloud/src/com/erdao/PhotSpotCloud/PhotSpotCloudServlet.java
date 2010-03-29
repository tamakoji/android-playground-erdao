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
import java.net.MalformedURLException;
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

	/** constants */
	private static final int MODE_PANORAMIO		= 0;
	private static final int MODE_PICASA		= 1;
	private static final int MODE_FLICKR		= 2;
	private static final int MODE_PICASA_WUSER	= 3;
	private static final int MODE_FLICKR_WUSER	= 4;
	private static final int MODE_LOCALSEARCH	= 5;
	private static final int MAX_FEED_PUB_SPOTS	= 100;
	private static final int MAX_FEED_MY_SPOTS	= 200;
	private static final int MAX_RETRY			= 2;
	private static final int PICASA_MAX_RESULTS	= 400;

	/** for Logging */
	private static final Logger log_ = Logger.getLogger(PhotSpotCloudServlet.class.getName());

	/** IO BUFFER SIZE = 2M */
	private static final int IO_BUFFER_SIZE = 2 * 1024;

	/** other variables */
	private int maximumFeedSize_ = MAX_FEED_PUB_SPOTS;
	private int totalFeedSize_= 0;
	private int compactFeedSize_= 0;
	private double appVerDbl_ = 0.0;
	private String jsoncb_ = null;
	private int svcMode_ = MODE_PANORAMIO;
	private boolean openfeedSuccess_ = false;

	/** photo search variables */
	private String nwlat_ = null;
	private String nwlng_ = null;
	private String selat_ = null;
	private String selng_ = null;
	private String service_ = null;
	private String userid_ = null;
	
	/** local search variables */
	private String lat_ = null;
	private String lng_ = null;

	/**
	 * do Get, main entry for this servlet
	 */
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		// get request params
		String qMode = req.getParameter("q");
		String debugstr = req.getParameter("dbg");
		if(debugstr!=null){
			try {
				debugstr = URLDecoder.decode(debugstr,"UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		jsoncb_ = req.getParameter("callback");
		String appVer = req.getParameter("appver");
		appVerDbl_ = 0.0;
		if( appVer!=null )
			appVerDbl_ = Double.parseDouble(appVer.substring(0,3));

		String compactJson = null;
		totalFeedSize_ = 0;
		// request branch
		if(qMode==null||qMode.equals("searchspot")){
			compactJson = ProcessPhotoSearch(req);
		}
		else if(qMode.equals("localsearch")){
			svcMode_ = MODE_LOCALSEARCH;
			compactJson = ProcessLocalSearch(req);
		}
		if( compactJson == null )
			return;

		// Compact JSON and format it
		resp.setContentType("text/plain");
		resp.setCharacterEncoding("utf-8");
		if(jsoncb_!=null)
			resp.getWriter().println(jsoncb_+"("+compactJson+")");
		else
			resp.getWriter().println(compactJson);
		// debug log
		if(svcMode_==MODE_LOCALSEARCH)
			log_.info("localsearch from="+req.getRemoteAddr()+",user-agent="+req.getHeader("user-agent")+",latlng="+lat_+","+lng_+",dbg="+debugstr+",openfeed="+openfeedSuccess_+",feedcount="+compactFeedSize_);
		else
			log_.info("spotsearch from="+req.getRemoteAddr()+",user-agent="+req.getHeader("user-agent")+",bbox="+nwlat_+","+nwlng_+","+selat_+","+selng_+",svc="+service_+",dbg="+debugstr+",openfeed="+openfeedSuccess_+",totalfeed="+totalFeedSize_+",compactfeed="+compactFeedSize_);
		
	}
	
	/**
	 * Process Local Search query
	 * @param req HttpServletRequest
	 * @return compact formatted JSON String
	 */
	private String ProcessLocalSearch(HttpServletRequest req){
		String requestUrl = null;
		String compactJson = "";
		String latlng = req.getParameter("latlng");
		if(latlng==null)
			return null;
		String token[] = latlng.split(",");
		lat_ = token[0];
		lng_ = token[1];
		if(lat_==null||lng_==null)
			return null;
		requestUrl = "http://ajax.googleapis.com/ajax/services/search/local?v=1.0&q=*&key="+APIKeys.google_ajax_key+"&sll="+lat_+","+lng_;
		// debug log
		log_.info("requestUrl raw="+requestUrl);
		StringBuilder strbuilder = null;
		strbuilder = openFeed(requestUrl);
		if( strbuilder != null ){
			compactJson = compactLocalSearchFeed(strbuilder.toString());
		}
		return compactJson;
	}

	/**
	 * Process Photo Search query
	 * @param req HttpServletRequest
	 * @return compact formatted JSON String
	 */
	private String ProcessPhotoSearch(HttpServletRequest req){
		String requestUrl = null;
		String compactJson = "";
		nwlat_ = req.getParameter("nwlat");
		nwlng_ = req.getParameter("nwlng");
		selat_ = req.getParameter("selat");
		selng_ = req.getParameter("selng");
		service_ = req.getParameter("svc");
		userid_ = req.getParameter("userid");
		maximumFeedSize_ = userid_==null ? MAX_FEED_PUB_SPOTS : MAX_FEED_MY_SPOTS;
		if(nwlat_==null||nwlng_==null||selat_==null||selng_==null||service_==null)
			return null;
		if(service_.equals("picasa")){
			if(userid_!=null)
				svcMode_ = MODE_PICASA_WUSER;
			else
				svcMode_ = MODE_PICASA;
		}
		else if(service_.equals("flickr")){
			if(userid_!=null)
				svcMode_ = MODE_FLICKR_WUSER;
			else
				svcMode_ = MODE_FLICKR;
		}
		else
			svcMode_ = MODE_PANORAMIO;
		requestUrl = createPhotoFeedUrl();
		if(requestUrl==null)
			return null;
		// debug log
		log_.info("requestUrl raw="+requestUrl);
		StringBuilder strbuilder = null;
		JSONArray compactArray = new JSONArray();
		int index = 1;
		if(service_.equals("flickr")){
			while(compactArray.length()<maximumFeedSize_){
				strbuilder = openFeed(requestUrl+"&page="+index);
				if( strbuilder == null )
					break;
				int curlen = compactArray.length();
				createCompactJSONPhotoArray(strbuilder.toString(),compactArray);
				log_.info("index:"+index+", arraysize="+compactArray.length()+",totalFeedSize_="+totalFeedSize_+",maximumFeedSize_="+maximumFeedSize_);
				if(curlen==compactArray.length())
					break;
				if(totalFeedSize_<maximumFeedSize_||compactArray.length()>maximumFeedSize_/2)
					break;
				if(++index>=3)
					break;
			}
			compactJson =  formatJSONString(compactArray);
		}
		else if(service_.equals("picasa")){
			while(compactArray.length()<maximumFeedSize_){
				strbuilder = openFeed(requestUrl+"&start-index="+index);
				if( strbuilder == null )
					break;
				int curlen = compactArray.length();
				createCompactJSONPhotoArray(strbuilder.toString(),compactArray);
				log_.info("index:"+index+", arraysize="+compactArray.length()+",totalFeedSize_="+totalFeedSize_+",maximumFeedSize_="+maximumFeedSize_);
				if(curlen==compactArray.length())
					break;
				if(totalFeedSize_<maximumFeedSize_||compactArray.length()>maximumFeedSize_/2)
					break;
				index+=PICASA_MAX_RESULTS;
				if(index>PICASA_MAX_RESULTS*2)
					break;
			}
			compactJson =  formatJSONString(compactArray);
		}
		else{
			strbuilder = openFeed(requestUrl);
			if( strbuilder != null ){
				createCompactJSONPhotoArray(strbuilder.toString(),compactArray);
				compactJson =  formatJSONString(compactArray);
			}
		}
		return compactJson;
	}
	
	
	/**
	 * Open specific url and set to StringBuilder
	 * @param url
	 * @return StringBuilder
	 */
	private StringBuilder openFeed(String requestUrl){
		openfeedSuccess_ = false;
		HttpURLConnection connection;
		StringBuilder strbuilder = null;
		BufferedReader reader;
		URL url = null;
		try{
			url = new URL(requestUrl);
		} catch (MalformedURLException e) {
			return null;
		}
		for(int i = 0; i < MAX_RETRY; i++ ){
			try {
				connection = (HttpURLConnection) url.openConnection();
				connection.setUseCaches(false);
				connection.setDoInput(true);
				connection.setRequestProperty("Content-type","text/plain");
				connection.setRequestMethod("GET");
				if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
					InputStream is = connection.getInputStream();
					InputStreamReader isr = new InputStreamReader(is,"UTF-8");
					reader = new BufferedReader(isr,IO_BUFFER_SIZE);
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
			if(strbuilder!=null){
				openfeedSuccess_ = true;
				break;
			}
		}
		return strbuilder;
	}

	/**
	 * Create Photo Feed Url for various services
	 * @return String for request
	 */
	private String createPhotoFeedUrl() {
		String url = "";
		switch(svcMode_){
			default:
			case MODE_PANORAMIO:{
				url = "http://www.panoramio.com/map/get_panoramas.php?order=popularity&set=full&size=small&minx="+nwlng_+"&miny="+selat_+"&maxx="+selng_+"&maxy="+nwlat_+"&from=0&to=100";
				break;
			}
			case MODE_PICASA:{
				url = "http://picasaweb.google.com/data/feed/api/all?alt=jsonc&kind=photo&bbox="+nwlng_+","+selat_+","+selng_+","+nwlat_+"&max-results="+PICASA_MAX_RESULTS;
				break;
			}
			case MODE_PICASA_WUSER:{
				url = "http://picasaweb.google.com/data/feed/api/user/"+userid_+"/?alt=jsonc&kind=photo&bbox="+nwlng_+","+selat_+","+selng_+","+nwlat_+"&max-results="+PICASA_MAX_RESULTS;
				break;
			}
			case MODE_FLICKR:{
				url = "http://api.flickr.com/services/rest/?method=flickr.photos.search&format=json&api_key="+APIKeys.flickr_key+"&content_type=1&sort=date-taken-desc&extras=geo&min_taken_date=2005-1-1+00%3A00%3A00&bbox="+nwlng_+","+selat_+","+selng_+","+nwlat_;
				break;
			}
			case MODE_FLICKR_WUSER:{
				url = "http://api.flickr.com/services/rest/?method=flickr.photos.search&format=json&api_key="+APIKeys.flickr_key+"&sort=date-taken-desc&extras=geo&user_id="+userid_+"&min_taken_date=1980-1-1+00%3A00%3A00&bbox="+nwlng_+","+selat_+","+selng_+","+nwlat_;
				break;
			}
		}
		return url;
	}
	
	/**
	 * create JSONArray for photo search
	 * @param srcJson		source json string from services
	 * @param compactArray	JSONArray object
	 */
	private void createCompactJSONPhotoArray(String srcJson, JSONArray compactArray){
		JSONObject jsonobj = null;
		JSONArray srcArray = null;
		try {
			switch( svcMode_ ){
				default:
				case MODE_PANORAMIO:{
					jsonobj = new JSONObject(srcJson);
					srcArray = jsonobj.getJSONArray("photos");
					totalFeedSize_ += srcArray.length();
					break;
				}
				case MODE_PICASA:
				case MODE_PICASA_WUSER:{
					jsonobj = new JSONObject(srcJson);
					totalFeedSize_ += jsonobj.getJSONObject("data").getInt("itemsPerPage");
					srcArray = jsonobj.getJSONObject("data").getJSONArray("photos");
					break;
				}
				case MODE_FLICKR:
				case MODE_FLICKR_WUSER:{
					srcJson = srcJson.substring(14, srcJson.length());
					jsonobj = new JSONObject(srcJson);
					totalFeedSize_ += jsonobj.getJSONObject("photos").getInt("perpage");
					srcArray = jsonobj.getJSONObject("photos").getJSONArray("photo");
					break;
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
			return;
		}
//		totalFeedSize_ += srcArray.length();
		String author_p = "";
		double lat_p = 0.0;
		double lng_p = 0.0;
		for (int i = 0; i < srcArray.length(); i++) {
			JSONObject obj = null;
			try{
				obj = srcArray.getJSONObject(i);
			}catch (JSONException e) {
				continue;
			}
			long id = 0;
			String title = null, fullThumbUrl = null, cmpThumbUrl = null, origUrl = null, author= null;
			double lat = 0.0,lng = 0.0;
			try{
				switch( svcMode_ ){
					default:
					case MODE_PANORAMIO:{
						id = obj.getLong("photo_id");
						title = obj.getString("photo_title");
						fullThumbUrl = obj.getString("photo_file_url");
						cmpThumbUrl = fullThumbUrl.replace("/small/","/thumbnail/");
						origUrl = obj.getString("photo_url");
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
						fullThumbUrl = mediaobj.getJSONArray("thumbnails").getString(2);
						cmpThumbUrl = mediaobj.getJSONArray("thumbnails").getString(1);
						origUrl = linkobj.getString("alternate");
						String latlng = obj.getJSONObject("geo").getString("point");
						String token[] = latlng.split(" ");
						lat = Double.valueOf(token[0]);
						lng = Double.valueOf(token[1]);
						if(svcMode_==MODE_PICASA_WUSER)
							author = userid_;
						else
							author = obj.getString("author");
						break;
					}
					case MODE_FLICKR:
					case MODE_FLICKR_WUSER:{
						String server = obj.getString("server");
						String secret = obj.getString("secret");
						id = obj.getLong("id");
						title = obj.getString("title");
						fullThumbUrl = "http://static.flickr.com/"+server+"/"+id+"_"+secret+"_m.jpg";
						cmpThumbUrl = "http://static.flickr.com/"+server+"/"+id+"_"+secret+"_t.jpg";
						origUrl = "http://www.flickr.com/photos/"+obj.getString("owner")+"/"+id;
						lat = obj.getDouble("latitude");
						lng = obj.getDouble("longitude");
						author = obj.getString("owner"); // todo:get real user name..
						break;
					}
				}
			}catch (JSONException e) {
				continue;
			}
			// eliminate same author with same location.
			// TODO: do more intelligent filtering.
			if(userid_==null){
				if(author.contentEquals(author_p)){
					if((lat>(lat_p-0.01))&&(lat<(lat_p+0.01))||(lng>(lng_p-0.01))&&(lng<(lng_p+0.01))){
						continue;
					}
				}
			}
			JSONObject comactObj = new JSONObject();
			try{
				comactObj.put("id", id);
				comactObj.put("title", title);
				comactObj.put("author", author);
				if(appVerDbl_>=1.6){
					comactObj.put("fullThumbUrl", fullThumbUrl);
					comactObj.put("cmpThumbUrl", cmpThumbUrl);
					comactObj.put("origUrl", origUrl);
				}
				else{
					comactObj.put("photoUrl", origUrl);
					comactObj.put("thumbUrl", fullThumbUrl);
				}
				comactObj.put("lat", lat);
				comactObj.put("lng", lng);
				compactArray.put(comactObj);
			} catch (JSONException e) {
				e.printStackTrace();
			}		
			if(compactArray.length()>=maximumFeedSize_)
				break;
			author_p = author;
			lat_p = lat;
			lng_p = lng;
		}
	}
	
	/**
	 * Compact Local Search JSON Feed
	 * @param fullJson
	 * @return compact Json Feed String
	 */
	private String compactLocalSearchFeed(String fullJson){
		JSONObject jsonobj = null;
		JSONArray compactArray = new JSONArray();
		try {
			JSONArray array = null;
			jsonobj = new JSONObject(fullJson);
			array = jsonobj.getJSONObject("responseData").getJSONArray("results");
			totalFeedSize_ = array.length();
			for (int i = 0; i < totalFeedSize_; i++) {
				JSONObject obj = array.getJSONObject(i);
				String title = null;
				title = obj.getString("titleNoFormatting");
				JSONObject comactObj = new JSONObject();
				comactObj.put("title", title);
				compactArray.put(comactObj);
			}
		} catch (JSONException e) {
			e.printStackTrace();
			compactFeedSize_ = 0;
			return "{\"count\":0}";
		}
		return formatJSONString(compactArray);
	}

	/**
	 * format JSONArray to JSON String
	 * @param jsonArray
	 * @return formatted JSON String
	 */
	private String formatJSONString(JSONArray jsonArray){
		if(jsonArray==null)
			return null;
		if(jsonArray.length()==0)
			return "{\"count\":0}";
		String jsonString = null;
		try {
			JSONObject result = new JSONObject();
			compactFeedSize_ = jsonArray.length();
			result.put("count", compactFeedSize_);
			if(svcMode_ == MODE_LOCALSEARCH)
				result.put("result", jsonArray);
			else
				result.put("photo", jsonArray);
			jsonString = result.toString();
		} catch (JSONException e) {
			e.printStackTrace();
			jsonString = "{\"count\":0}";
		}
		return jsonString;
	}

}
