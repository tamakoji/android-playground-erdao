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

package com.erdao.SnapFace;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Calendar;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.media.FaceDetector;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.Media;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;


/* Class PreviewView for camera surface
 */
class PreviewView extends SurfaceView implements SurfaceHolder.Callback, PreviewCallback, AutoFocusCallback {
	
	/* local members */
	private Context context_;
	private SurfaceHolder surfacehldr_;
	private Camera camera_;
	private boolean lockPreview_ = true;
	private final int PREVIEW_WIDTH_FINE = 320;
	private final int PREVIEW_HEIGHT_FINE = 240;
	private final int PREVIEW_WIDTH_NORMAL = 240;
	private final int PREVIEW_HEIGHT_NORMAL= 180;
	private int prevSettingWidth_;
	private int prevSettingHeight_;
	private int previewWidth_;
	private int previewHeight_;
	private boolean takingPicture_ = false;
	private Bitmap fdtmodeBitmap_ = null;
	private boolean isSDCardPresent_ = false;
	private int fdetLevel_ = 1;
	private int appMode_ = 0;

	/* Overlay Layer for additional graphics overlay */
	private Bitmap overlayBitmap_;
	private OverlayLayer layer_;

	/* Face Detection */
	private FaceResult faces_[];
	private final int MAX_FACE = 5;
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
		layer_ = new OverlayLayer(context);
		surfacehldr_ = getHolder();
		surfacehldr_.addCallback(this);
		surfacehldr_.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		handler_ = new Handler();
		System.loadLibrary("snapface-jni");
		isSDCardPresent_ = android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
	}

	/* Overlay instance access method for Activity */
	public OverlayLayer getOverlay(){
		return layer_;
	}

	/* surfaceCreated */
	public void surfaceCreated(SurfaceHolder holder) {
//		Log.i("DEBUG","surfaceCreated");
		setKeepScreenOn(true);
		setupCamera();
		if(!isSDCardPresent_)
			Toast.makeText(context_, R.string.SDCardNotPresentAlert, Toast.LENGTH_LONG).show();
		else
			Toast.makeText(context_, R.string.TapInstructionAlert, Toast.LENGTH_LONG).show();
	}

	/* surfaceDestroyed */
	public void surfaceDestroyed(SurfaceHolder holder) {
//		Log.i("DEBUG","surfaceDestroyed");
		setKeepScreenOn(false);
		releaseCamera();
	}

	/* onAutoFocus */
	public void onAutoFocus(boolean success, Camera camera){
		if(success&&takingPicture_)
			takePicture();
	}

	/* surfaceChanged */
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
//		Log.i("DEBUG","surfaceChanged");
		resetCameraSettings();
		startPreview();
	}

	/* onPreviewFrame */
	public void onPreviewFrame(byte[] _data, Camera _camera) {
		if(lockPreview_||takingPicture_)
			return;
//		Log.i("DEBUG","_data.length, bufflen_="+_data.length+","+bufflen_);
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
//			Log.i("DEBUG","onTouchDown");
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
		releaseCamera();
		setupCamera();
		resetCameraSettings();
		startPreview();
	}

	/* setAppMode */
	public void setAppMode(int mode){
		if(appMode_ == mode)
			return;
		appMode_ = mode;
		switch(appMode_)
		{
			case 0:{
				overlayBitmap_ = null;
				break;
			}
			case 1:{
				overlayBitmap_ = BitmapFactory.decodeResource(context_.getResources(), R.drawable.mask_vader);
				break;
			}
//			case 2:{
//				overlayBitmap_ = BitmapFactory.decodeResource(context_.getResources(), R.drawable.mask_laughingman);
//				break;
//			}
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
		lockPreview_ = true;
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
		if( fdetLevel_ == 0 ){
			prevSettingWidth_ = PREVIEW_WIDTH_FINE;
			prevSettingHeight_ = PREVIEW_HEIGHT_FINE;
			fdtmodeBitmap_ = BitmapFactory.decodeResource(context_.getResources(), R.drawable.fdt_fine);
		}else{
			prevSettingWidth_ = PREVIEW_WIDTH_NORMAL;
			prevSettingHeight_ = PREVIEW_HEIGHT_NORMAL;
			fdtmodeBitmap_ = BitmapFactory.decodeResource(context_.getResources(), R.drawable.fdt_norm);
		}
		/* set parameters for onPreviewFrame */
 		Camera.Parameters params = camera_.getParameters();
 		/* set preview size small for fast analysis. let say QQVGA
 		 * by setting smaller image size, small faces will not be detected. */
 		params.setPreviewSize(prevSettingWidth_, prevSettingHeight_);
 		params.setPictureSize(640, 480);
		camera_.setParameters(params);
		/* need to re-get for real size if set to 160x120... why?
		 * (actual preview size will be 176x144 wich is 11:9) */
 		params = camera_.getParameters();
 		Size size = params.getPreviewSize();
		previewWidth_ = size.width;
		previewHeight_ = size.height;
//		Log.i("DEBUG","preview size, w:"+previewWidth_+",h:"+previewHeight_);
		// allocate work memory for analysis
		bufflen_ = previewWidth_*previewHeight_;
		grayBuff_ = new byte[bufflen_];
		rgbs_ = new int[bufflen_];
		fdet_ = new FaceDetector( previewWidth_,previewHeight_, MAX_FACE ); 
		lockPreview_ = false;
	}
	
	/* startPreview */
	private void startPreview(){
		/* start Preview */
		camera_.setPreviewCallback(this);
		camera_.startPreview();
		/* one-shot autofoucs */
		camera_.autoFocus(this);
	}

	/* takePicture */
	private void takePicture() {
		waitForFdetThreadComplete();
		/* implement only jpeg callback */
		camera_.takePicture(null, null, pictureCallbackJpeg); 
	}
		 
	/* jpegCallback */
	private PictureCallback pictureCallbackJpeg = new PictureCallback() {
		public void onPictureTaken(byte[] _data, Camera _camera) {
//			Log.i("DEBUG","jpegCallback:"+_data);
			takingPicture_ = false;
			/* convert jpeg buffer to Bitmap */
			Bitmap fullbmp = BitmapFactory.decodeByteArray(_data, 0, _data.length);
			int w = fullbmp.getWidth();
			int h = fullbmp.getHeight();
			/* CAUTION
			 * takepicture callback image may differs not just aspect
			 * but also the view angle. need to fix them.
			 */
			float orgRatio = (float)previewWidth_/previewHeight_;
			float offset_w = (w - h*orgRatio)/2.0f;
			float xRatio = (float)(h*orgRatio) / previewWidth_; 
			float yRatio = (float)h / previewHeight_;
			/* restore touch point to bitmap size */
			Point touchPt = new Point((int)(selFacePt_.x*w),(int)(selFacePt_.y*h));
			for(int i=0; i<MAX_FACE; i++){
				FaceResult face = faces_[i];
				float eyedist = face.eyesDistance()*xRatio;
				if(eyedist==0.0f)
					continue;
				PointF midEyes = new PointF(); 
				face.getMidPoint(midEyes);
				/* don't want region to be overlapped, assume face region is x3 eyedist */
				PointF lt = new PointF(midEyes.x*xRatio-eyedist*1.5f+offset_w,midEyes.y*yRatio-eyedist*1.5f);
				Rect rect = new Rect((int)(lt.x),(int)(lt.y),(int)(lt.x+eyedist*3.0f),(int)(lt.y+eyedist*3.0f));
				if(!rect.contains(touchPt.x,touchPt.y))
					continue;
				/* expand region for cropping face */
				lt = new PointF(midEyes.x*xRatio-eyedist*2.0f+offset_w,midEyes.y*yRatio-eyedist*2.0f);
				rect = new Rect((int)(lt.x),(int)(lt.y),(int)(lt.x+eyedist*4.0f),(int)(lt.y+eyedist*4.0f));
				/* fix to fit within bitmap */
				rect.left = rect.left < 0 ? 0 : rect.left;
				rect.right = rect.right > w ? w : rect.right;
				rect.top = rect.top < 0 ? 0 : rect.top;
				rect.bottom = rect.bottom > h ? h : rect.bottom;
				/* crop */
				Bitmap facebmp = Bitmap.createBitmap(fullbmp,rect.left,rect.top,rect.width(),rect.height());
				if(appMode_!=0&&overlayBitmap_!=null){
					//todo: merge bitmap
					Canvas c = new Canvas(facebmp);
		            Paint p = new Paint();
		            float len = eyedist*3.5f;
					PointF lt_mask = new PointF((facebmp.getWidth()-len)/2.0f,(facebmp.getHeight()-len)/2.0f);
	        		c.drawBitmap(overlayBitmap_, null , new Rect((int)lt_mask.x, (int)lt_mask.y,(int)(lt_mask.x+len),(int)(lt_mask.y+len)),p);
				}
				/* Save bitmap to file */
				Uri uri = SaveBitmapToFile(facebmp);
				if(uri!=null){
					Intent intent = new Intent(Intent.ACTION_VIEW, uri, context_, ImageViewActivity.class);
					context_.startActivity(intent);
					return;
				}
				break;
			}
			/* restart preview */
			camera_.startPreview();
		}
	};
	
	/* Save bitmap to file */
	private Uri SaveBitmapToFile(Bitmap bmp) {
		/* get current time for file */
		Calendar calendar = Calendar.getInstance();
		int year = calendar.get(Calendar.YEAR);
		int month = calendar.get(Calendar.MONTH);
		int day = calendar.get(Calendar.DAY_OF_MONTH);
		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		int minute = calendar.get(Calendar.MINUTE);
		int second = calendar.get(Calendar.SECOND);
		String filename = String.format("%4d-%02d-%02d %02d.%02d.%02d", year,(month + 1),day,hour,minute,second);
		/* setup for storing file */
		ContentValues values = new ContentValues();
		values.put(Media.DISPLAY_NAME, filename);
		values.put(Media.TITLE, filename);
		String absFilePath = "/sdcard/DCIM/snapface/"+filename+".jpg";
		values.put(Media.DATA, absFilePath);
		values.put(Media.MIME_TYPE, "image/jpeg");
		Uri uri = context_.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
		try {
			/* save file */
			OutputStream outStream = context_.getContentResolver().openOutputStream(uri);
			bmp.compress(Bitmap.CompressFormat.JPEG, 90, outStream);
			outStream.flush();
			outStream.close();
			Toast.makeText(context_, context_.getString(R.string.SaveImageSuccessAlert)+absFilePath, Toast.LENGTH_LONG).show();
			return uri;
		} catch (IOException e) {
			Toast.makeText(context_, R.string.SaveImageFailureAlert, Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}
		return null;
	}

	/* waitForFdetThreadComplete */
	private void waitForFdetThreadComplete(){
		if(detectThread_ == null)
			return;
		if( detectThread_.isAlive() ){
			try {
				detectThread_.join();
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
//			long t1 = System.currentTimeMillis();
			/* face detector only needs grayscale image */
			grayToRgb(graybuff_,rgbs_);										// jni method
//			gray8toRGB32(graybuff_,previewWidth_,previewHeight_,rgbs);		// java method
//			Log.i("DEBUG","decode time:"+(System.currentTimeMillis()-t1));
			Bitmap bmp = Bitmap.createBitmap(rgbs_,previewWidth_,previewHeight_,Bitmap.Config.RGB_565);
//			Log.i("DEBUG","bmp w:"+bmp.getWidth()+",h:"+bmp.getHeight());
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
				rect.right = rect.right > previewWidth_ ? previewWidth_ : rect.right;
				rect.top = rect.top < 0 ? 0 : rect.top;
				rect.bottom = rect.bottom > previewHeight_ ? previewHeight_ : rect.bottom;
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
					faces_[i].setFace(ptTrack,trackface[0].eyesDistance());
					trackfound++;
				}
			}
//			Log.i("DEBUG","prevfound:trackfound="+prevfound+","+trackfound);
			if(prevfound==0||prevfound!=trackfound){
				FaceDetector.Face[] fullResults = new FaceDetector.Face[MAX_FACE];
//				t1 = System.currentTimeMillis();
				fdet_.findFaces(bmp, fullResults);
				/* copy result */
				for(int i=0; i<MAX_FACE; i++){
					if(fullResults[i]==null)
						faces_[i].clear();
					else{
						faces_[i].setFace(fullResults[i]);
					}
				}
			}
//			Log.i("DEBUG","loop time:"+(System.currentTimeMillis()-t1));
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
		public void setFace(FaceDetector.Face src){
			PointF midEye = new PointF();
			src.getMidPoint(midEye);
			set_(midEye,src.eyesDistance());
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
