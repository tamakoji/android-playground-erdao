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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

/**
 * JSON Feed get and parse utility
 * @author Huan Erdao
 */
public class JsonFeedGetter {
	/* constant */
	public static final int MODE_SPOTSEARCH		= 0;
	public static final int MODE_LOCALSEARCH	= 1;
	public static final int MODE_FLICKR_ID		= 2;
	public static final int MODE_PICASA_ID		= 3;

	public final static int CODE_HTTPERROR		= -2;
	public final static int CODE_JSONERROR		= -1;
	public final static int CODE_NORESULT		= 0;
	public final static int CODE_OK				= 1;
	/* variables */
	private HttpClient httpClient_;
	private final int connection_Timeout = 10000;
	private final Context context_;
	int mode_;
	private List<PhotoItem> photoItems_ = new ArrayList<PhotoItem>();
	private List<CharSequence> localSpots_ = new ArrayList<CharSequence>();
	private String userid_ = "";

	/* constructor */
	public JsonFeedGetter(Context c, int mode){
		mode_ = mode;
		context_ = c;
		final HttpParams httpParams = new BasicHttpParams();
		HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setContentCharset(httpParams, "UTF-8");
		HttpConnectionParams.setConnectionTimeout(httpParams,connection_Timeout); 
		HttpConnectionParams.setSoTimeout(httpParams,connection_Timeout); 
		final SchemeRegistry schemeRegistry = new SchemeRegistry();
		schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
		httpClient_ = new DefaultHttpClient(
				new ThreadSafeClientConnManager(httpParams, schemeRegistry),
				httpParams);
		
	}

	/* getFeed */
	public Integer getFeed(String uri){
		final HttpGet get = new HttpGet(uri);
		HttpEntity entity = null;
		HttpResponse response;
		StringBuilder strbuilder = null;
		InputStream is = null;
		boolean isSuccess = false;
		try {
			response = httpClient_.execute(get);
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				entity = response.getEntity();
				is = entity.getContent();
				InputStreamReader isr = new InputStreamReader(is,"UTF-8");
				BufferedReader reader = new BufferedReader(isr,1024);
				strbuilder = new StringBuilder();
				String line = null;
				while ((line = reader.readLine()) != null) {
					strbuilder.append(line + "\n");
				}
				isSuccess = true;
			}
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			try {
				if(is!=null)
					is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if( strbuilder == null ){
			if(mode_==MODE_PICASA_ID&&!isSuccess)
				return CODE_JSONERROR;
			return CODE_HTTPERROR;
		}

		String result = strbuilder.toString();
		JSONObject jsonobj = null;
		try {
			JSONArray array = null;
			switch(mode_){
				default:
				case MODE_SPOTSEARCH:{
					jsonobj = new JSONObject(result);
					array = jsonobj.getJSONArray("photo");
					break;
				}
				case MODE_LOCALSEARCH:{
					jsonobj = new JSONObject(result);
					array = jsonobj.getJSONArray("result");
					break;
				}
				case MODE_FLICKR_ID:{
					result = result.substring(14, result.length());
					jsonobj = new JSONObject(result);
					jsonobj = jsonobj.getJSONObject("user");
					userid_ = jsonobj.getString("nsid");
					return CODE_OK; 
				}
				case MODE_PICASA_ID:{
					return CODE_OK; 
				}
			}
			long feedcount = jsonobj.getLong("count");
			if(feedcount==0)
				return CODE_NORESULT;
			int count = array.length();
			for (int i = 0; i < count; i++) {
				JSONObject obj = array.getJSONObject(i);
				long id = 0;
				String title = null, thumbUrl = null, photoUrl = null, author= null;
				double lat = 0.0,lng = 0.0;
				if(mode_==MODE_SPOTSEARCH){
					id = obj.getLong("id");
					title = obj.getString("title");
					thumbUrl = obj.getString("thumbUrl");
					photoUrl = obj.getString("photoUrl");
					lat = obj.getDouble("lat");
					lng = obj.getDouble("lng");
					author = obj.getString("author");
					if(title == null || title.length() == 0) {
						title = context_.getString(R.string.no_title);
					}
					PhotoItem item =
						new PhotoItem(id,(int)(lat*1E6),(int)(lng*1E6),title,author,thumbUrl,photoUrl);
					photoItems_.add(item);
				}
				else{
					title = obj.getString("title");
					localSpots_.add(title);
				}
			}
			return CODE_OK; 
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return CODE_JSONERROR;
	}
	
	/* getPhotoItemList */
	public List<PhotoItem> getPhotoItemList(){
		return photoItems_;
	}

	/* getLocalSpotsList */
	public List<CharSequence> getLocalSpotsList(){
		return localSpots_;
	}

	/* getUserId */
	public final String getUserId(){
		return userid_;
	}

}
