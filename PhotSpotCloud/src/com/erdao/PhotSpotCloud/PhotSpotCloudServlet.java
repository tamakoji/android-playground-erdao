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
import java.net.HttpURLConnection;
import java.net.URL;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@SuppressWarnings("serial")
public class PhotSpotCloudServlet extends HttpServlet {
	private final int MODE_PANORAMIO = 0;
	private final int MODE_PICASA = 1;
	private final int MODE_FLICKR = 2;

	private String service_;
	private int svcMode_ = MODE_PANORAMIO;
	private String nwlat_;
	private String nwlng_;
	private String selat_;
	private String selng_;
	private String debugstr_;
	private String author_p = "";
	private double lat_p = 0.0;
	private double lng_p = 0.0;
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		nwlat_ = req.getParameter("nwlat");
		nwlng_ = req.getParameter("nwlng");
		selat_ = req.getParameter("selat");
		selng_ = req.getParameter("selng");
		debugstr_ = req.getParameter("dbg");
		service_ = req.getParameter("svc");
		if(service_.equals("picasa"))
			svcMode_ = MODE_PICASA;
		else if(service_.equals("panoramio"))
			svcMode_ = MODE_PANORAMIO;
		else
			svcMode_ = MODE_FLICKR;
		resp.setContentType("text/plain");
		resp.setCharacterEncoding("utf-8");
		String compactJson = "";
		StringBuilder strbuilder = null;
		URL url = new URL(createUrl());
		final int MAX_RETRY = 2;
		for(int i = 0; i < MAX_RETRY; i++ ){
			strbuilder = openFeed(url);
			if(strbuilder!=null)
				break;
		}
		if( strbuilder != null ){
			compactJson = compactJsonFeed(strbuilder.toString());
			if(compactJson!=null){
				resp.getWriter().println(compactJson);
			}
		}
	}
	
	private StringBuilder openFeed(URL url){
		HttpURLConnection connection;
		StringBuilder strbuilder = null;
		BufferedReader reader;
		try {
			connection = (HttpURLConnection) url.openConnection();
			connection.setDoOutput(false);
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
	
	private String createUrl() {
		String url = "";
		switch(svcMode_){
			default:
			case MODE_PANORAMIO:{
				url = "http://www.panoramio.com/map/get_panoramas.php?order=popularity&set=full&size=small&minx="+nwlng_+"&miny="+selat_+"&maxx="+selng_+"&maxy="+nwlat_+"&from=0&to=100";
				break;
			}
			case MODE_PICASA:{
				url = "http://picasaweb.google.com/data/feed/api/all?alt=jsonc&kind=photo&bbox="+nwlng_+","+selat_+","+selng_+","+nwlat_+"&max-results=400";
				break;
			}
			case MODE_FLICKR:{
				url = "http://api.flickr.com/services/rest/?method=flickr.photos.search&format=json&api_key="+APIKeys.flickr_key+"&per_page=400&extras=geo&min_taken_date=2005-1-1+00%3A00%3A00&bbox="+nwlng_+","+selat_+","+selng_+","+nwlat_;
				break;
			}
		}
		return url;
	}

	
	/* compactJsonFeed - compact json feed
	 */
	private String compactJsonFeed(String fullJson){
		String compactJson = "";
		JSONObject jsonobj = null;
		JSONArray compactArray = new JSONArray();
		try {
			JSONArray array = null;
			switch( svcMode_ ){
				default:
				case MODE_PANORAMIO:{
					jsonobj = new JSONObject(fullJson);
					array = jsonobj.getJSONArray("photos");
					break;
				}
				case MODE_PICASA: {
					jsonobj = new JSONObject(fullJson);
					array = jsonobj.getJSONArray("photos");
					break;
				}
				case MODE_FLICKR: {
					fullJson = fullJson.substring(14, fullJson.length());
					jsonobj = new JSONObject(fullJson);
					array = jsonobj.getJSONObject("photos").getJSONArray("photo");
					break;
				}
			}
			int total = array.length();
			int compactArrayCount = 0;
			for (int i = 0; i < total; i++) {
				JSONObject obj = array.getJSONObject(i);
				long id = 0;
				String title = null, thumbUrl = null, photoUrl = null, author= null;
				double lat = 0.0,lng = 0.0;
				switch( svcMode_ ){
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
					case MODE_PICASA:{
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
				}
				if(author.contentEquals(author_p)){
					if((lat>(lat_p-0.0001))&&(lat<(lat_p+0.001))||(lng>(lng_p-0.001))&&(lng<(lng_p+0.001))){
						continue;
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
				if(++compactArrayCount>100)
					break;
				author_p = author;
				lat_p = lat;
				lng_p = lng;
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			compactJson = "{\"count\":0}";
			return compactJson;
		}
		try {
			JSONObject result = new JSONObject();
			result.put("count", compactArray.length());
			result.put("photo", compactArray);
			compactJson = result.toString();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return compactJson;
	}
	
}
