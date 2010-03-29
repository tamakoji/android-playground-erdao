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

package com.erdao.android.LaughingMan;

import java.io.IOException;
import java.nio.ByteBuffer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.media.FaceDetector;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;


/* Class PreviewView for camera surface
 */
class PreviewView extends SurfaceView implements SurfaceHolder.Callback, PreviewCallback, AutoFocusCallback {
	
    private static final String TAG = "LaughingMan";
    
	private final static int PREVIEW_WIDTH_FINE		= 320;
	private final static int PREVIEW_WIDTH_NORMAL	= 240;

	/* local members */
	private Context context_;
	private SurfaceHolder surfacehldr_;
	private Camera camera_;
	private boolean lockPreview_ = true;
	private int prevSettingWidth_;
	private int prevSettingHeight_;
	private int previewWidth_;
	private int previewHeight_;
	private int captureWidth_ = 0;
	private int captureHeight_ = 0;
	private boolean takingPicture_ = false;
	private Bitmap fdtmodeBitmap_ = null;
	private boolean isSDCardPresent_ = false;
	private int fdetLevel_ = 1;
	private int appMode_ = 0;

	/* Overlay Layer for additional graphics overlay */
	private Bitmap overlayBitmap_;
	private OverlayLayer overlayLayer_;

	/* Face Detection */
	private FaceResult faces_[];
	private static final int MAX_FACE = 5;
	private FaceDetector fdet_;
	private PointF selFacePt_ = null;

	/* Face Detection Threads */
	private boolean isThreadWorking_ = false;
	private Handler handler_;
	private FaceDetectThread detectThread_ = null;
	
	/* buffers for vision analysis */
	private byte[] grayBuff_;
	private int bufflen_;
	private int[] rgbs_;

	/* Constructor */
	public PreviewView(Context context) {
		super(context);
		context_ = context;
		previewWidth_ = previewHeight_ = 1;
		faces_ = new FaceResult[MAX_FACE];
		for(int i=0;i<MAX_FACE;i++)
			faces_[i] = new FaceResult();
		overlayLayer_ = new OverlayLayer(context);
		surfacehldr_ = getHolder();
		surfacehldr_.addCallback(this);
		surfacehldr_.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		handler_ = new Handler();
		System.loadLibrary("laughingman-jni");
		isSDCardPresent_ = android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
	}

	/* Overlay instance access method for Activity */
	public OverlayLayer getOverlay(){
		return overlayLayer_;
	}

	/* surfaceCreated */
	public void surfaceCreated(SurfaceHolder holder) {
		setKeepScreenOn(true);
		setupCamera();
		/*
		if(!isSDCardPresent_)
			Toast.makeText(context_, R.string.SDCardNotPresentAlert, Toast.LENGTH_LONG).show();
		else
			Toast.makeText(context_, R.string.TapInstructionAlert, Toast.LENGTH_LONG).show();
			*/
	}

	/* surfaceDestroyed */
	public void surfaceDestroyed(SurfaceHolder holder) {
		setKeepScreenOn(false);
		releaseCamera();
	}

	/* onAutoFocus */
	public void onAutoFocus(boolean success, Camera camera){
	}

	/* surfaceChanged */
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		resetCameraSettings();
		camera_.setPreviewCallback(this);
		camera_.startPreview();
	}

	/* onPreviewFrame */
	public void onPreviewFrame(byte[] _data, Camera _camera) {
		if(lockPreview_||takingPicture_)
			return;
		if(_data.length < bufflen_)
			return;
		// run only one analysis thread at one time
		if(!isThreadWorking_){
			isThreadWorking_ = true;
			// copy only Y buffer
			ByteBuffer bbuffer = ByteBuffer.wrap(_data);
			bbuffer.get(grayBuff_, 0, bufflen_);
			// make sure to wait for previous thread completes
			waitForFdetThreadComplete();
			// start thread
			detectThread_ = new FaceDetectThread(handler_);
			detectThread_.setBuffer(grayBuff_);
			detectThread_.start();
		}
	}

	/* onTouchEvent */
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		/* do not handle if already going to take picture */
		if(takingPicture_)
			return false;
		/* detect touch down */
		if(event.getAction()==MotionEvent.ACTION_DOWN){
			/* CAUTION : touch point need to be same aspect to jpeg bitmap size */
			int w = this.getWidth();
			int h = this.getHeight();
			Point touchPt = new Point((int)event.getRawX(),(int)event.getRawY());
			float xRatio = (float)w / previewWidth_; 
			float yRatio = (float)h / previewHeight_;
			selFacePt_ = null;
			for(int i=0; i<MAX_FACE; i++){
				FaceResult face = faces_[i];
				float eyedist = face.eyesDistance()*xRatio;
				if(eyedist==0.0f)
					continue;
				PointF midEyes = new PointF();
				face.getMidPoint(midEyes);
				/* assume face rect is x3 size of eye distance each side */
				PointF lt = new PointF(midEyes.x*xRatio-eyedist*1.5f,midEyes.y*yRatio-eyedist*1.5f);
				Rect rect = new Rect((int)(lt.x),(int)(lt.y),(int)(lt.x+eyedist*3.0f),(int)(lt.y+eyedist*3.0f));
				/*
				if( rect.contains(touchPt.x,touchPt.y)){
					if(!isSDCardPresent_)
					Toast.makeText(context_, R.string.SDCardNotPresentAlert, Toast.LENGTH_LONG).show();
					else{
						takingPicture_ = true;
						Toast.makeText(context_, R.string.CapturingAlert, Toast.LENGTH_SHORT).show();
						selFacePt_ = new PointF((float)touchPt.x/w,(float)touchPt.y/h);
					}
					break;
				}
				*/
			}
			/* call autofocus. if takingPicture_ == true, take picture upon completion */
			camera_.autoFocus(this);
			return true;
		}
		return false;
	}
	
	/* setResolution */
	public void setfdetLevel(int level, boolean silent){
		if(fdetLevel_ == level)
			return;
		fdetLevel_ = level;
		if(silent)
			return;
		camera_.stopPreview();
		resetCameraSettings();
		camera_.startPreview();
	}

	/* setAppMode */
	public void setAppMode(int mode){
		if(appMode_ == mode)
			return;
		appMode_ = mode;
		switch(appMode_)
		{
			/*
			case 0:{
				overlayBitmap_ = null;
				break;
			}
			case 1:{
				overlayBitmap_ = BitmapFactory.decodeResource(context_.getResources(), R.drawable.mask_vader);
				break;
			}
			*/
			default:
			case 2:
			{
				overlayBitmap_ = BitmapFactory.decodeResource(context_.getResources(), R.drawable.laughingman);
				break;
			}
		}
	}

	/* setupCamera */
	private void setupCamera(){
		try {
			camera_ = Camera.open();
			camera_.setPreviewDisplay(surfacehldr_);
		} catch (IOException exception) {
			camera_.release();
			camera_ = null;
		}
	}

	/* releaseCamera */
	private void releaseCamera(){
		/* release camera object */
 		Log.i(TAG,"releaseCamera");
		lockPreview_ = true;
		camera_.setPreviewCallback(null);
		camera_.stopPreview();
		waitForFdetThreadComplete();
		camera_.release();
		camera_ = null;
	}
	
	/* resetCameraSettings */
	private void resetCameraSettings(){
		lockPreview_ = true;
		waitForFdetThreadComplete();
		for(int i=0;i<MAX_FACE;i++)
			faces_[i].clear();
		/* set parameters for onPreviewFrame */
 		Camera.Parameters params = camera_.getParameters();
// 		Log.i(TAG,"camera params"+params.flatten());
		String strPrevSizesVals = params.get("preview-size-values");
 		String strCapSizesVals = params.get("picture-size-values");
 		int previewHeightFine = 0;
 		int previewHeightNorm = 0;
 		if(strPrevSizesVals!=null){
	 		String tokens[] = strPrevSizesVals.split(",");
	 		for( int i=0; i < tokens.length; i++ ){
	 	 		String tokens2[] = tokens[i].split("x");
	 			if( tokens[i].contains(Integer.toString(PREVIEW_WIDTH_FINE)) )
	 				previewHeightFine = Integer.parseInt(tokens2[1]);
	 			if( tokens[i].contains(Integer.toString(PREVIEW_WIDTH_NORMAL)) )
	 				previewHeightNorm = Integer.parseInt(tokens2[1]);
	 		}
 		}
 		else{
 			previewHeightFine = 240;
 			previewHeightNorm = 160;
 		}
		if( fdetLevel_ == 0 ){
			prevSettingWidth_ = PREVIEW_WIDTH_FINE;
			prevSettingHeight_ = previewHeightFine;
			//fdtmodeBitmap_ = BitmapFactory.decodeResource(context_.getResources(), R.drawable.fdt_fine);
		}else{
			prevSettingWidth_ = PREVIEW_WIDTH_NORMAL;
			prevSettingHeight_ = previewHeightNorm;
			//fdtmodeBitmap_ = BitmapFactory.decodeResource(context_.getResources(), R.drawable.fdt_norm);
		}
 		if(strCapSizesVals!=null){
 			String capTokens1[] = strCapSizesVals.split(",");
 			String capTokens2[] = capTokens1[capTokens1.length-1].split("x");
 	 		captureWidth_ = Integer.parseInt(capTokens2[0]);
 	 		captureHeight_ = Integer.parseInt(capTokens2[1]);
 		}
 		/* set preview size small for fast analysis. let say QQVGA
 		 * by setting smaller image size, small faces will not be detected. */
 		if(prevSettingHeight_!=0)
 			params.setPreviewSize(prevSettingWidth_, prevSettingHeight_);
 		if(captureWidth_!=0)
 			params.setPictureSize(captureWidth_, captureHeight_);
// 		params.setPreviewFrameRate(5);
		camera_.setParameters(params);
		/* setParameters do not work well */
 		params = camera_.getParameters();
 		Size size = params.getPreviewSize();
		previewWidth_ = size.width;
		previewHeight_ = size.height;
		Log.i(TAG,"preview size, w:"+previewWidth_+",h:"+previewHeight_);
 		size = params.getPictureSize();
		Log.i(TAG,"picture size, w:"+size.width+",h:"+size.height);
		// allocate work memory for analysis
		bufflen_ = previewWidth_*previewHeight_;
		grayBuff_ = new byte[bufflen_];
		rgbs_ = new int[bufflen_];
		float aspect = (float)previewHeight_/(float)previewWidth_;
		fdet_ = new FaceDetector( prevSettingWidth_,(int)(prevSettingWidth_*aspect), MAX_FACE ); 
		lockPreview_ = false;
	}
	
	/* waitForFdetThreadComplete */
	private void waitForFdetThreadComplete(){
		if(detectThread_ == null)
			return;
		if( detectThread_.isAlive() ){
			try {
				detectThread_.join();
				Log.i(TAG,"thread deleted.");
				detectThread_ = null;
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	/* Jni entry point */
	private native int grayToRgb(byte src[],int dst[]);
	
	/* Overlay Layer class */
	public class OverlayLayer extends View { 
		private Paint paint_ = new Paint(Paint.ANTI_ALIAS_FLAG); 

		/* Constructor */
		public OverlayLayer(Context context) { 
			super(context); 
			paint_.setStyle(Paint.Style.STROKE); 
			paint_.setColor(0xFF33FF33);
			paint_.setStrokeWidth(3);
		} 

		/* onDraw - Draw Face rect */
		@Override 
		protected void onDraw(Canvas canvas) {
			super.onDraw(canvas);
			int w = canvas.getWidth();
			int h = canvas.getHeight();
			if(fdtmodeBitmap_!=null){
				int x = w-100;
				int y = 10;
        		canvas.drawBitmap(fdtmodeBitmap_, null , new Rect(x,y,x+70,y+20),paint_);
			}
			float xRatio = (float)w / previewWidth_; 
			float yRatio = (float)h / previewHeight_;
			for(int i=0; i<MAX_FACE; i++){
				FaceResult face = faces_[i];
				float eyedist = face.eyesDistance()*xRatio;
				if(eyedist==0.0f)
					continue;
				PointF midEyes = new PointF();
				face.getMidPoint(midEyes);
				if(appMode_==0){
					PointF lt = new PointF(midEyes.x*xRatio-eyedist*1.5f,midEyes.y*yRatio-eyedist*1.5f);
					canvas.drawRect((int)(lt.x),(int)(lt.y),(int)(lt.x+eyedist*3.0f),(int)(lt.y+eyedist*3.0f), paint_); 
				}
				else if(overlayBitmap_!=null){
					PointF lt = new PointF(midEyes.x*xRatio-eyedist*1.75f,midEyes.y*yRatio-eyedist*1.75f);
	        		canvas.drawBitmap(overlayBitmap_, null , new Rect((int)lt.x, (int)lt.y,(int)(lt.x+eyedist*3.5f),(int)(lt.y+eyedist*3.5f)),paint_);
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
			/* face detector only needs grayscale image */
			grayToRgb(graybuff_,rgbs_);										// jni method
//			gray8toRGB32(graybuff_,previewWidth_,previewHeight_,rgbs);		// java method
			float aspect = (float)previewHeight_/(float)previewWidth_;
			int w = prevSettingWidth_;
			int h = (int)(prevSettingWidth_*aspect);
			float xScale = (float)previewWidth_/(float)prevSettingWidth_;
			float yScale = (float)previewHeight_/(float)prevSettingHeight_;
			Bitmap bmp = Bitmap.createScaledBitmap(
						Bitmap.createBitmap(rgbs_,previewWidth_,previewHeight_,Bitmap.Config.RGB_565),
						w,h,false);
//			Log.i(TAG,"downscale w="+bmp.getWidth()+",h="+bmp.getHeight());
			int prevfound=0,trackfound=0;
			for(int i=0; i<MAX_FACE; i++){
				FaceResult face = faces_[i];
				float eyedist = face.eyesDistance();
				if(eyedist==0.0f)
					continue;
				PointF midEyes = new PointF(); 
				face.getMidPoint(midEyes);
				prevfound++;
				PointF lt = new PointF(midEyes.x-eyedist*2.5f,midEyes.y-eyedist*2.5f);
				Rect rect = new Rect((int)(lt.x),(int)(lt.y),(int)(lt.x+eyedist*5.0f),(int)(lt.y+eyedist*5.0f));
				/* fix to fit */
				rect.left = rect.left < 0 ? 0 : rect.left;
				rect.right = rect.right > w ? w : rect.right;
				rect.top = rect.top < 0 ? 0 : rect.top;
				rect.bottom = rect.bottom > h ? h : rect.bottom;
				if(rect.left >= rect.right || rect.top >= rect.bottom )
					continue;
				/* crop */
				Bitmap facebmp = Bitmap.createBitmap(bmp,rect.left,rect.top,rect.width(),rect.height());
				FaceDetector.Face[] trackface = new FaceDetector.Face[1];
				FaceDetector tracker = new FaceDetector( facebmp.getWidth(),facebmp.getHeight(),1); 
				int found = tracker.findFaces(facebmp, trackface);
				if(found!=0){
					PointF ptTrack = new PointF();
					trackface[0].getMidPoint(ptTrack);
					ptTrack.x += (float)rect.left;
					ptTrack.y += (float)rect.top;
					ptTrack.x *= xScale;
					ptTrack.y *= yScale;
					float trkEyedist = trackface[0].eyesDistance()*xScale;
					faces_[i].setFace(ptTrack,trkEyedist);
					trackfound++;
				}
			}
			if(prevfound==0||prevfound!=trackfound){
				FaceDetector.Face[] fullResults = new FaceDetector.Face[MAX_FACE];
				fdet_.findFaces(bmp, fullResults);
				/* copy result */
				for(int i=0; i<MAX_FACE; i++){
					if(fullResults[i]==null)
						faces_[i].clear();
					else{
						PointF mid = new PointF();
						fullResults[i].getMidPoint(mid);
						mid.x *= xScale;
						mid.y *= yScale;
						float eyedist = fullResults[i].eyesDistance()*xScale;
						faces_[i].setFace(mid,eyedist);
					}
				}
			}
			/* post message to UI */
			handler_.post(new Runnable() {
				public void run() {
					overlayLayer_.postInvalidate();
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

	/* Face Result Class */
	private class FaceResult extends Object {
		private PointF midEye_;
		private float eyeDist_;
		public FaceResult(){
			midEye_ = new PointF(0.0f,0.0f);
			eyeDist_ = 0.0f;
		}
		public void setFace(PointF midEye, float eyeDist){
			set_(midEye,eyeDist);
		}
		public void clear(){
			set_(new PointF(0.0f,0.0f),0.0f);
		}
		private synchronized void set_(PointF midEye, float eyeDist){
			midEye_.set(midEye);
			eyeDist_ = eyeDist;
		}
		public float eyesDistance(){
			return eyeDist_;
		}
		public void getMidPoint(PointF pt){
			pt.set(midEye_);
		}
	};
}
