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

package com.erdao.LaughingMan;

import java.io.IOException;
import java.nio.ByteBuffer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.media.FaceDetector;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

class PreviewView extends SurfaceView implements SurfaceHolder.Callback, PreviewCallback, AutoFocusCallback {
	private SurfaceHolder surfacehldr_;
	private Camera camera_;
	private boolean lockPreview_ = false;
	private FaceDetector.Face faces_[];
	private final int MAX_FACE = 5;
	private FaceDetector fdet_;
	private boolean isThreadWorking_ = false;
	private Handler handler_;
	private int previewWidth_;
	private int previewHeight_;
	private OverlayLayer layer_;
	private Context context_;
	
	public PreviewView(Context context) {
		super(context);
		init(context);
	}

	private void init(Context context){
		context_ = context;
		previewWidth_ = previewHeight_ = 1;
		faces_ = new FaceDetector.Face[MAX_FACE];
		layer_ = new OverlayLayer(context);
		surfacehldr_ = getHolder();
		surfacehldr_.addCallback(this);
		surfacehldr_.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		handler_ = new Handler();
        System.loadLibrary("laughingman-jni");
	}
	
	public OverlayLayer getOverlay(){
		return layer_;
	}

	public native int grayToRgb(byte src[],int dst[]);

	public void surfaceCreated(SurfaceHolder holder) {
		try {
			camera_ = Camera.open();
			camera_.setPreviewDisplay(holder);
	        this.setOnClickListener(new OnClickListener() {
				public void onClick(View v){
					camera_.autoFocus((PreviewView)v);
				}
			});
		} catch (IOException exception) {
			camera_.release();
			camera_ = null;
		}
		setKeepScreenOn(true);
	}

	public void onPreviewFrame(byte[] _data, Camera _camera) {
		if(lockPreview_)
			return;
		// run only one analysis thread at one time
		if(!isThreadWorking_){
			isThreadWorking_ = true;
			// copy only Y buffer
			int bufflen = previewWidth_*previewHeight_;
			ByteBuffer grayBuff = ByteBuffer.allocate(bufflen);
			grayBuff.put(_data, 0, bufflen);
			// start thread
			new FaceDetectThread(handler_,grayBuff.array()).start();
		}
	}

	public void onAutoFocus(boolean success, Camera camera){
		// keep on autofocus
//		camera_.autoFocus(this);
	}

   public void surfaceDestroyed(SurfaceHolder holder) {
		lockPreview_ = true;
		camera_.stopPreview();
		camera_.release();
		camera_ = null;
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {

 		Camera.Parameters params = camera_.getParameters();
 		// set preview size small for fast analysis. let say QQVGA
 		// by setting smaller image size, small faces will not be detected.
 		params.setPreviewSize(160, 120);
// 		params.setPreviewSize(w, h);
		camera_.setParameters(params);
		// need to re-get for real size... why?
 		params = camera_.getParameters();
 		Size size = params.getPreviewSize();
		previewWidth_ = size.width;
		previewHeight_ = size.height;
		Log.i("DEBUG","preview size, w:"+previewWidth_+",h:"+previewHeight_);
		fdet_ = new FaceDetector( previewWidth_,previewHeight_, MAX_FACE ); 
		camera_.startPreview();
		/* not sure but setting preview callback sometime app dies when
		 * you stop the app even if the callback is just an empty method  */
		camera_.setPreviewCallback(this);
		camera_.autoFocus(this);
		Toast.makeText(context_, "Tap screen to adjust focus", Toast.LENGTH_LONG).show();
	}

	public class OverlayLayer extends View { 
		private Paint paint_ = new Paint(Paint.ANTI_ALIAS_FLAG); 
		private Bitmap laughingman_;
		public OverlayLayer(Context context) { 
        	super(context); 
    		paint_.setStyle(Paint.Style.STROKE); 
    		paint_.setColor(Color.RED);
    		paint_.setStrokeWidth(1);
    		laughingman_ = BitmapFactory.decodeResource(context.getResources(), R.drawable.laughingman);
        } 
        @Override 
        protected void onDraw(Canvas canvas) {
        	super.onDraw(canvas);
			float xRatio = canvas.getWidth()*1.0f / previewWidth_; 
			float yRatio = canvas.getHeight()*1.0f / previewHeight_; 
			for(int i=0; i<MAX_FACE; i++){
				FaceDetector.Face face = faces_[i];
				if(face!=null){
					PointF midEyes = new PointF(); 
		        	face.getMidPoint(midEyes); 
		        	float dist = face.eyesDistance()*xRatio;
		        	if (midEyes != null) {
		        		PointF lt = new PointF(midEyes.x*xRatio-dist*1.2f,midEyes.y*yRatio-dist*1.2f);
		        		//canvas.drawRect(lt.x, lt.y, lt.x+dist*2.4, lt.y+dist*2.4, paint_); 
		        		canvas.drawBitmap(laughingman_, null , new Rect((int)lt.x, (int)lt.y,(int)(lt.x+dist*2.4f),(int)(lt.y+dist*2.4f)),paint_);
		        	}
				}
			}
        }
	}
	
	// Thread Class to Detect Face
	private class FaceDetectThread extends Thread {
		private Handler handler_;
		private byte[] graybuff_;

		public FaceDetectThread(Handler handler, byte[] graybuff){
			handler_ = handler;
			graybuff_ = graybuff;
		}

		@Override
		public void run() {
			long t1 = System.currentTimeMillis();
			int[] rgbs = new int[previewWidth_*previewHeight_];
			// use jni for image process(faster)
			grayToRgb(graybuff_,rgbs);										// jni method
//			gray8toRGB32(graybuff_,previewWidth_,previewHeight_,rgbs);		// java method
			Log.i("DEBUG","decode time:"+(System.currentTimeMillis()-t1));
			Bitmap bmp = Bitmap.createBitmap(rgbs,previewWidth_,previewHeight_,Bitmap.Config.RGB_565);
			t1 = System.currentTimeMillis();
			int found = fdet_.findFaces(bmp, faces_); 
			Log.i("DEBUG","detect time:"+(System.currentTimeMillis()-t1));
			if(found==0){
				for(int i=0;i<MAX_FACE;i++)
					faces_[i] = null;
			}
			handler_.post(new Runnable() {
				public void run() {
					layer_.postInvalidate();
					// turn off thread lock
					isThreadWorking_ = false;
				}
			});
		}

		// convert 8bit grayscale to RGB32bit (fill R,G,B with Y)
		// process may take time and differs according to OS load. (100-1000ms)
		@SuppressWarnings("unused")
		private void gray8toRGB32(byte[] gray8, int width, int height, int[] rgb_32s) {
		    final int endPtr = width * height;
		    int ptr = 0;
		    while (true) {
	            if (ptr == endPtr)
	            	break;
		        final int Y = gray8[ptr] & 0xff; 
		        rgb_32s[ptr] = 0xff000000 + (Y << 16) + (Y << 8) + Y;
		        ptr++;
		    }
		}

	}

}