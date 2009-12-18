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

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;

/**
 * Activity Class for Help Document.
 * @author Huan Erdao
 */
public class HelpActivity extends Activity {

	/**
	 * onCreate handler.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.help);
		final String mimeType = "text/html";
		final String encoding = "utf-8";
		WebView wv;
		wv = (WebView) findViewById(R.id.help);
		WebSettings setting = wv.getSettings();
		setting.setTextSize(WebSettings.TextSize.SMALLER);
		setting.setDefaultTextEncodingName(encoding);
		// there is bug on loadData, if containing 2byte chars it displays corrupted text.
//		wv.loadData(getString(R.string.helpcontent), mimeType, encoding);
		wv.loadDataWithBaseURL("dummy",getString(R.string.helpcontent), mimeType, encoding, null);
	}
	
	/**
	 * onCreateOptionsMenu handler.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuItem menu_Revert = menu.add(0,R.id.menu_Revert,0,R.string.menu_Revert);
		menu_Revert.setIcon(android.R.drawable.ic_menu_revert);
		return true;
	}

	/**
	 * onOptionsItemSelected handler.
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_Revert: {
				finish();
				break;
			}
		}
		return true;
	}
}
