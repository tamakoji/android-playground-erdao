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
	private int mode_;
	private final int PREVIEW_WIDTH_FINE = 320;
	private final int PREVIEW_HEIGHT_FINE = 240;
	private final int PREVIEW_WIDTH_NORMAL = 160;
	private final int PREVIEW_HEIGHT_NORMAL= 120;
	private int prevSettingWidth_;
	private int prevSettingHeight_;
	private int previewWidth_;
	private int previewHeight_;
	private boolean takingPicture_ = false;

	/* Overlay Layer for additional graphics overlay */
	private OverlayLayer layer_;

	/* Face Detection */
	private FaceDetector.Face faces_[];
	private final int MAX_FACE = 5;
	private FaceDetector fdet_;
	private PointF selFacePt_ = null;

	/* Face Detection Threads */
	private boolean isThreadWorking_ = false;
	private Handler handler_;
	
	/* buffers for vision analysis */
	private byte[] grayBuff_;
	private int bufflen_;
	private int[] rgbs_;

	/* Constructor */
	public PreviewView(Context context, int mode) {
		super(context);
		context_ = context;
		mode_ = mode;
		previewWidth_ = previewHeight_ = 1;
		faces_ = new FaceDetector.Face[MAX_FACE];
		layer_ = new OverlayLayer(context);
		surfacehldr_ = getHolder();
		surfacehldr_.addCallback(this);
		surfacehldr_.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		handler_ = new Handler();
		System.loadLibrary("snapface-jni");
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
			Toast.makeText(context_, "Tap deteced face rect to capture.", Toast.LENGTH_LONG).show();
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
		if(success&&takingPicture_)
			takePicture();
	}

	/* surfaceChanged */
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
//		Log.i("DEBUG","surfaceChanged");
		resetCameraSettings();
	}

	/* onPreviewFrame */
	public void onPreviewFrame(byte[] _data, Camera _camera) {
		if(lockPreview_)
			return;
		if(_data.length < bufflen_)
			return;
		// run only one analysis thread at one time
		if(!isThreadWorking_){
			isThreadWorking_ = true;
			// copy only Y buffer
			ByteBuffer bbuffer = ByteBuffer.wrap(_data);
			bbuffer.get(grayBuff_, 0, bufflen_);
			// start thread
			FaceDetectThread detectThread = new FaceDetectThread(handler_);
			detectThread.setBuffer(grayBuff_);
			detectThread.start();
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
			synchronized(context_){
				for(int i=0; i<MAX_FACE; i++){
					FaceDetector.Face face = faces_[i];
					if(face==null)
						continue;
					PointF midEyes = new PointF(); 
					face.getMidPoint(midEyes); 
					float eyedist = face.eyesDistance()*xRatio;
					if(midEyes==null)
						continue;
					/* assume face rect is x3 size of eye distance each side */
					PointF lt = new PointF(midEyes.x*xRatio-eyedist*1.5f,midEyes.y*yRatio-eyedist*1.5f);
					Rect rect = new Rect((int)(lt.x),(int)(lt.y),(int)(lt.x+eyedist*3.0f),(int)(lt.y+eyedist*3.0f));
					if( rect.contains(touchPt.x,touchPt.y)){
						Toast.makeText(context_, "Capturing... hold still.", Toast.LENGTH_SHORT).show();
						selFacePt_ = new PointF((float)touchPt.x/w,(float)touchPt.y/h);
						takingPicture_ = true;
						break;
					}
				}
			}
			/* call autofocus. if takingPicture_ == true, take picture upon completion */
			camera_.autoFocus(this);
			return true;
		}
		return false;
	}
	
	public void setResolution(int which){
		mode_ = which;
		lockPreview_ = true;
		camera_.stopPreview();
		resetCameraSettings();
	}
	
	/* Jni entry point */
	private native int grayToRgb(byte src[],int dst[]);
	
	/* resetCameraSettings */
	private void resetCameraSettings(){
		if( mode_ == 0 ){
			prevSettingWidth_ = PREVIEW_WIDTH_FINE;
			prevSettingHeight_ = PREVIEW_HEIGHT_FINE;
		}else{
			prevSettingWidth_ = PREVIEW_WIDTH_NORMAL;
			prevSettingHeight_ = PREVIEW_HEIGHT_NORMAL;
		}
		/* set parameters for onPreviewFrame */
 		Camera.Parameters params = camera_.getParameters();
 		/* set preview size small for fast analysis. let say QQVGA
 		 * by setting smaller image size, small faces will not be detected. */
 		params.setPreviewSize(prevSettingWidth_, prevSettingHeight_);
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
		grayBuff_ = new byte[bufflen_];
		rgbs_ = new int[bufflen_];
		fdet_ = new FaceDetector( previewWidth_,previewHeight_, MAX_FACE ); 
		/* start Preview */
		camera_.startPreview();
		camera_.setPreviewCallback(this);
		/* one-shot autofoucs */
		camera_.autoFocus(this);
		lockPreview_ = false;
	}
	
	/* takePicture */
	private void takePicture() {
//		Log.i("DEBUG","takePicture");
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
//			Log.i("DEBUG","image size: w="+w+",h="+h);
			/* CAUTION
			 * takepicture callback image differs not just aspect
			 * but also the view angle. need to fix them.
			 */
			float orgRatio = (float)previewWidth_/previewHeight_;
			float offset_w = (w - h*orgRatio)/2.0f;
			float xRatio = (float)(h*orgRatio) / previewWidth_; 
			float yRatio = (float)h / previewHeight_;
			/* restore touch point to bitmap size */
			Point touchPt = new Point((int)(selFacePt_.x*w),(int)(selFacePt_.y*h));
			synchronized(context_){
				for(int i=0; i<MAX_FACE; i++){
					FaceDetector.Face face = faces_[i];
					if(face==null)
						continue;
					PointF midEyes = new PointF(); 
					face.getMidPoint(midEyes); 
					float eyedist = face.eyesDistance()*xRatio;
					if (midEyes == null)
						continue;
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
					/* Save bitmap to file */
					Uri uri = SaveBitmapToFile(facebmp);
					if(uri!=null){
						/* Open up Picture Viewer for further actions (i.e. set as contact icon ) */
						//Intent intent = new Intent(Intent.ACTION_VIEW, uri);
						//context_.startActivity(intent);
						Intent intent = new Intent(Intent.ACTION_ATTACH_DATA, uri);
						try {
							context_.startActivity(Intent.createChooser(intent,context_.getString(R.string.SetPictureAsIntent_Name)));
						} catch (android.content.ActivityNotFoundException ex) {
							Toast.makeText(context_, R.string.SetPictureAsIntent_FailCreate, Toast.LENGTH_SHORT).show();
						}
						return;
					}
					break;
				}
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
			return uri;
		} catch (IOException e) {
			Toast.makeText(context_, "Image Capture Failed.", Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}
		return null;
	}

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
						canvas.drawRect((int)(lt.x),(int)(lt.y),(int)(lt.x+eyedist*3.0f),(int)(lt.y+eyedist*3.0f), paint_); 
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
