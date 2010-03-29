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
