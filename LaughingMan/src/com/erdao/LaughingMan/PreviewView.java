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
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.media.FaceDetector;
import android.os.Handler;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;


/* Class PreviewView for camera surface
 */
class PreviewView extends SurfaceView implements SurfaceHolder.Callback, PreviewCallback, AutoFocusCallback {
	
	/* local members */
	private Context context_;
	private SurfaceHolder surfacehldr_;
	private Camera camera_;
	private boolean lockPreview_ = true;
	private int previewWidth_;
	private int previewHeight_;

	/* Overlay Layer for additional graphics overlay */
	private OverlayLayer layer_;

	/* Face Detection */
	private FaceDetector.Face faces_[];
	private final int MAX_FACE = 5;
	private FaceDetector fdet_;

	/* Face Detection Threads */
	private FaceDetectThread detectThread_ = null;
	private boolean isThreadWorking_ = false;
	private Handler handler_;
	
	/* buffers for vision analysis */
	private ByteBuffer grayBuff_;
	private int bufflen_;
	private int[] rgbs_;

	/* Constructor */
	public PreviewView(Context context) {
		super(context);
		context_ = context;
		previewWidth_ = previewHeight_ = 1;
		faces_ = new FaceDetector.Face[MAX_FACE];
		layer_ = new OverlayLayer(context);
		surfacehldr_ = getHolder();
		surfacehldr_.addCallback(this);
		surfacehldr_.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		handler_ = new Handler();
        System.loadLibrary("laughingman-jni");
		detectThread_ = new FaceDetectThread(handler_);
	}

	/* Overlay instance access method for Activity */
	public OverlayLayer getOverlay(){
		return layer_;
	}

	/* surfaceCreated */
	public void surfaceCreated(SurfaceHolder holder) {
//		Log.i("DEBUG","surfaceCreated");
		/* open camera setup preview */
		try {
			camera_ = Camera.open();
			camera_.setPreviewDisplay(holder);
			setKeepScreenOn(true);
		} catch (IOException exception) {
			camera_.release();
			camera_ = null;
		}
	}

	/* surfaceDestroyed */
	public void surfaceDestroyed(SurfaceHolder holder) {
//		Log.i("DEBUG","surfaceDestroyed");
		/* release camera object */
		lockPreview_ = true;
		camera_.stopPreview();
		camera_.release();
		camera_ = null;
	}

	/* onAutoFocus */
	public void onAutoFocus(boolean success, Camera camera){
	}

	/* surfaceChanged */
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
//		Log.i("DEBUG","surfaceChanged");
		/* set parameters for onPreviewFrame */
 		Camera.Parameters params = camera_.getParameters();
 		/* set preview size small for fast analysis. let say QQVGA
 		 * by setting smaller image size, small faces will not be detected. */
 		params.setPreviewSize(160, 120);
 		params.setPictureSize(640, 480);
		camera_.setParameters(params);
		/* need to re-get for real size... why?
		 * (actual preview size will be 176x144 wich is 11:9) */
 		params = camera_.getParameters();
 		Size size = params.getPreviewSize();
		previewWidth_ = size.width;
		previewHeight_ = size.height;
//		Log.i("DEBUG","preview size, w:"+previewWidth_+",h:"+previewHeight_);
		// allocate work memory for analysis
		bufflen_ = previewWidth_*previewHeight_;
		grayBuff_ = ByteBuffer.allocate(bufflen_);
		rgbs_ = new int[bufflen_];
		fdet_ = new FaceDetector( previewWidth_,previewHeight_, MAX_FACE ); 
		/* start Preview */
		camera_.startPreview();
		camera_.setPreviewCallback(this);
		/* one-shot autofoucs */
		camera_.autoFocus(this);
		lockPreview_ = false;
	}

	/* onPreviewFrame */
	public void onPreviewFrame(byte[] _data, Camera _camera) {
		if(lockPreview_)
			return;
		// run only one analysis thread at one time
		if(!isThreadWorking_){
			isThreadWorking_ = true;
			// copy only Y buffer
			grayBuff_.clear();
			grayBuff_.put(_data, 0, bufflen_);
			// start thread
			detectThread_.setBuffer(grayBuff_.array());
			detectThread_.run();
		}
	}


	/* Jni entry point */
	private native int grayToRgb(byte src[],int dst[]);


	/* Overlay Layer class */
	public class OverlayLayer extends View { 
		private Paint paint_ = new Paint(Paint.ANTI_ALIAS_FLAG); 
		private Bitmap laughingmanBitmap_;

		/* Constructor */
		public OverlayLayer(Context context) { 
			super(context); 
			paint_.setStyle(Paint.Style.STROKE); 
			paint_.setColor(0xFF33FF33);
			paint_.setStrokeWidth(3);
			laughingmanBitmap_ = BitmapFactory.decodeResource(context.getResources(), R.drawable.laughingman);
		} 

		/* onDraw - Draw Face rect */
		@Override 
		protected void onDraw(Canvas canvas) {
			super.onDraw(canvas);
			int w = canvas.getWidth();
			int h = canvas.getHeight();
			float xRatio = (float)w / previewWidth_; 
			float yRatio = (float)h / previewHeight_;
			for(int i=0; i<MAX_FACE; i++){
				FaceDetector.Face face = faces_[i];
				if(face!=null){
					PointF midEyes = new PointF(); 
					face.getMidPoint(midEyes); 
					float eyedist = face.eyesDistance()*xRatio;
					if (midEyes != null) {
						PointF lt = new PointF(midEyes.x*xRatio-eyedist*1.5f,midEyes.y*yRatio-eyedist*1.5f);
//						canvas.drawRect((int)(lt.x),(int)(lt.y),(int)(lt.x+eyedist*3.0f),(int)(lt.y+eyedist*3.0f), paint_); 
		        		canvas.drawBitmap(laughingmanBitmap_, null , new Rect((int)lt.x, (int)lt.y,(int)(lt.x+eyedist*3.0f),(int)(lt.y+eyedist*3.0f)),paint_);
					}
				}
			}
		}
	};
	
	/* Thread Class for Face Detection */
	private class FaceDetectThread extends Thread {
		/* variables */
		private Handler handler_;
		private byte[] graybuff_ = null;

		/* Constructor */
		public FaceDetectThread(Handler handler){
			handler_ = handler;
		}
		
		/* set buffer */
		public void setBuffer(byte[] graybuff){
			graybuff_ = graybuff;
		}

		/* run the thread */
		@Override
		public void run() {
//			long t1 = System.currentTimeMillis();
			/* face detector only needs grayscale image */
			grayToRgb(graybuff_,rgbs_);										// jni method
//			gray8toRGB32(graybuff_,previewWidth_,previewHeight_,rgbs);		// java method
//			Log.i("DEBUG","decode time:"+(System.currentTimeMillis()-t1));
			Bitmap bmp = Bitmap.createBitmap(rgbs_,previewWidth_,previewHeight_,Bitmap.Config.RGB_565);
//			t1 = System.currentTimeMillis();
			FaceDetector.Face[] local = new FaceDetector.Face[MAX_FACE];
			fdet_.findFaces(bmp, local);
			/* copy result */
			synchronized (context_) {
				faces_ = local.clone();
			}
//			Log.i("DEBUG","detect time:"+(System.currentTimeMillis()-t1));
			/* post message to UI */
			handler_.post(new Runnable() {
				public void run() {
					layer_.postInvalidate();
					// turn off thread lock
					isThreadWorking_ = false;
				}
			});
		}
		
		/* convert 8bit grayscale to RGB32bit (fill R,G,B with Y)
		 * process may take time and differs according to OS load. (100-1000ms) */
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
	};

}
