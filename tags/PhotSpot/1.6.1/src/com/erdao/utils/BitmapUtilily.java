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

package com.erdao.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

/**
 * Utility Class to handle Bitmaps
 * @author Huan Erdao
 */
public class BitmapUtilily {

	/** IO BUFFER SIZE = 2M */
	private static final int IO_BUFFER_SIZE = 2 * 1024;

	/** Debug Tag */
	private static final String TAG = "PhotSpot";

	/**
	 * Load Bitmap from a url
	 * @param url	bitmap url to be loaded
	 * @return Bitmap object, null if could not be loaded
	 */
	public static Bitmap loadBitmap(String url) {
		Bitmap bitmap = null;
		InputStream is = null;
        BufferedOutputStream bos = null;
		try {
			is = new BufferedInputStream(new URL(url).openStream(), IO_BUFFER_SIZE);
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bos = new BufferedOutputStream(baos, IO_BUFFER_SIZE);
            copyStream(is, bos);
            bos.flush();
            final byte[] data = baos.toByteArray();
            bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Log.e(TAG,"IOException in loadBitmap");
			e.printStackTrace();
		} catch (OutOfMemoryError e) {
			// TODO Auto-generated catch block
			Log.e(TAG,"OutOfMemoryError in loadBitmap");
			e.printStackTrace();
		} finally {
			closeStream(is);
			closeStream(bos);
		}
		return bitmap;	}

	public static Bitmap loadBitmap(String url, BitmapFactory.Options opt) {
		Bitmap bitmap = null;
		InputStream is = null;
        BufferedOutputStream bos = null;
		try {
			is = new BufferedInputStream(new URL(url).openStream(), IO_BUFFER_SIZE);
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bos = new BufferedOutputStream(baos, IO_BUFFER_SIZE);
            copyStream(is, bos);
            bos.flush();
            final byte[] data = baos.toByteArray();
            bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, opt);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Log.e(TAG,"IOException in loadBitmap");
			e.printStackTrace();
		} catch (OutOfMemoryError e) {
			// TODO Auto-generated catch block
			Log.e(TAG,"OutOfMemoryError in loadBitmap");
			e.printStackTrace();
		} finally {
			closeStream(is);
			closeStream(bos);
		}
		return bitmap;
	}

	public static Bitmap loadBitmapDirectStream(String url, BitmapFactory.Options opt) {
		Bitmap bitmap = null;
		InputStream is = null;
		try {
			is = new BufferedInputStream(new URL(url).openStream(), IO_BUFFER_SIZE);
            bitmap = BitmapFactory.decodeStream(is,null,opt);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Log.e(TAG,"IOException in loadBitmap");
			e.printStackTrace();
		} catch (OutOfMemoryError e) {
			// TODO Auto-generated catch block
			Log.e(TAG,"OutOfMemoryError in loadBitmap");
			e.printStackTrace();
		} finally {
			closeStream(is);
		}
		return bitmap;
	}

    /**
     * @param in The input stream to copy from.
     * @param out The output stream to copy to.
     * @throws IOException If any error occurs during the copy.
     */
	private static void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] b = new byte[IO_BUFFER_SIZE];
        int read;
        while ((read = in.read(b)) != -1) {
            out.write(b, 0, read);
        }
    }

    /**
     * Closes the specified stream.
     * @param stream The stream to close.
     */
    private static void closeStream(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
				e.printStackTrace();
            }
        }
    }
    
}