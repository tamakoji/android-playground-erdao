/*
 * Copyright (C) 2010 Huan Erdao
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

package com.erdao.android.SnapFace;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

public class ImageViewActivity extends Activity {
	
	private Uri uri_;
	private Context context_;
	private Intent parentIntent_ = null;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		/* set Full Screen */
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);  
		/* set window with no title bar */
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.image_view);
		context_ = (Context)this;
		ImageView imgView = (ImageView)findViewById(R.id.imageview);
		parentIntent_ = getIntent();
        uri_ = parentIntent_.getData();
        imgView.setImageURI(uri_);
	}
	
	/* create Menu */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuItem menu_SetAs = menu.add(0,R.id.menu_SetAs,0,R.string.menu_SetAs);
		menu_SetAs.setIcon(android.R.drawable.ic_menu_set_as);
		MenuItem menu_Delete = menu.add(0,R.id.menu_Undo,0,R.string.menu_Undo);
		menu_Delete.setIcon(android.R.drawable.ic_menu_revert);
		return true;
	}

	/* Menu handling */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_SetAs:{
				// for standalone operation
				Intent intent = new Intent(Intent.ACTION_ATTACH_DATA, uri_);
				try {
					startActivity(Intent.createChooser(intent,getString(R.string.SetPictureAsIntent_Name)));
				} catch (android.content.ActivityNotFoundException ex) {
					Toast.makeText(this, R.string.SetPictureAsIntent_FailCreate, Toast.LENGTH_SHORT).show();
				}
				break;
			}
			case R.id.menu_Undo:{
				new AlertDialog.Builder(this)
				.setTitle(R.string.menu_Undo)
				.setMessage(R.string.delete_alert)
                .setPositiveButton(R.string.delete_alert_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    	context_.getContentResolver().delete(uri_, null, null);
    					setResult(RESULT_CANCELED);
                    	finish();
                    }
                })
                .setNegativeButton(R.string.delete_alert_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                })
				.create()
				.show();
				break;
			}
		}
		return true;
	}
	
}
