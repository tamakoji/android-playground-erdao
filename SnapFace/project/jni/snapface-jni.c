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

#include "./imgutils/imgutils.h"
#include <jni.h>

void
Java_com_erdao_android_SnapFace_PreviewView_grayToRgb( JNIEnv*  env,
                                      jobject  this,
                                      jbyteArray src,
                                      jintArray  dst )
{
	jboolean b;
	jint len;
	jbyte* srcPtr=(*env)->GetByteArrayElements(env,src,&b);
	jint* dstPtr=(*env)->GetIntArrayElements(env,dst,&b);
	len = (*env)->GetArrayLength(env,src);
	gray8toRGB32(srcPtr, dstPtr, len);
	(*env)->ReleaseByteArrayElements(env, src, srcPtr, 0);
	(*env)->ReleaseIntArrayElements(env, dst, dstPtr, 0);
}
