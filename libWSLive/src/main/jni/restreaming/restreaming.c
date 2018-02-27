#include "colorConvert.h"
#include "log.h"
#include "jni.h"
#include <string.h>
#include <android/native_window_jni.h>

JNIEXPORT void JNICALL Java_me_lake_librestreaming_core_ColorHelper_NV21TOYUV420SP
(JNIEnv * env, jobject thiz, jbyteArray srcarray,jbyteArray dstarray,jint ySize) {
	unsigned char *src = (unsigned char *)(*env)->GetByteArrayElements(env,srcarray, 0);
	unsigned char *dst = (unsigned char*)(*env)->GetByteArrayElements(env,dstarray, 0);
	NV21TOYUV420SP(src,dst,ySize);
	(*env)->ReleaseByteArrayElements(env,srcarray,src,JNI_ABORT);
	(*env)->ReleaseByteArrayElements(env,dstarray,dst,JNI_ABORT);
	return;
}
JNIEXPORT void JNICALL Java_me_lake_librestreaming_core_ColorHelper_YUV420SPTOYUV420P
(JNIEnv * env, jobject thiz, jbyteArray srcarray,jbyteArray dstarray,jint ySize) {
	unsigned char *src = (unsigned char *)(*env)->GetByteArrayElements(env,srcarray, 0);
	unsigned char *dst = (unsigned char*)(*env)->GetByteArrayElements(env,dstarray, 0);
	YUV420SPTOYUV420P(src,dst,ySize);
	(*env)->ReleaseByteArrayElements(env,srcarray,src,JNI_ABORT);
	(*env)->ReleaseByteArrayElements(env,dstarray,dst,JNI_ABORT);
	return;
}
JNIEXPORT void JNICALL Java_me_lake_librestreaming_core_ColorHelper_NV21TOYUV420P
(JNIEnv * env, jobject thiz, jbyteArray srcarray,jbyteArray dstarray,jint ySize) {
	unsigned char *src = (unsigned char *)(*env)->GetByteArrayElements(env,srcarray, 0);
	unsigned char *dst = (unsigned char*)(*env)->GetByteArrayElements(env,dstarray, 0);
	NV21TOYUV420P(src,dst,ySize);
	(*env)->ReleaseByteArrayElements(env,srcarray,src,JNI_ABORT);
	(*env)->ReleaseByteArrayElements(env,dstarray,dst,JNI_ABORT);
	return;
}
JNIEXPORT void JNICALL Java_me_lake_librestreaming_core_ColorHelper_NV21TOARGB
(JNIEnv *env, jobject thiz,jbyteArray srcarray,jintArray dstarray,jint width,jint height){
		unsigned char *src = (unsigned char *)(*env)->GetByteArrayElements(env,srcarray, 0);
		unsigned int *dst = (unsigned int*)(*env)->GetIntArrayElements(env,dstarray, 0);
		NV21TOARGB(src,dst,width,height);
		(*env)->ReleaseByteArrayElements(env,srcarray,src,JNI_ABORT);
		(*env)->ReleaseIntArrayElements(env,dstarray,dst,JNI_ABORT);
		return;
}

JNIEXPORT void JNICALL Java_me_lake_librestreaming_core_ColorHelper_NV21Transform
(JNIEnv * env, jobject thiz, jbyteArray srcarray,jbyteArray dstarray,jint srcwidth,jint srcheight,jint directionflag) {
	unsigned char *src = (unsigned char*)(*env)->GetByteArrayElements(env,srcarray, 0);
	unsigned char *dst = (unsigned char*)(*env)->GetByteArrayElements(env,dstarray, 0);
	NV21Transform(src,dst,srcwidth,srcheight,directionflag);
	(*env)->ReleaseByteArrayElements(env,srcarray,src,JNI_ABORT);
	(*env)->ReleaseByteArrayElements(env,dstarray,dst,JNI_ABORT);
	return;
}

JNIEXPORT void JNICALL Java_me_lake_librestreaming_render_GLESRender_NV21TOYUV
(JNIEnv *env, jobject thiz,jbyteArray srcarray,jbyteArray dstYarray,jbyteArray dstUarray,jbyteArray dstVarray,jint width,jint height){
		unsigned char *src = (unsigned char*)(*env)->GetByteArrayElements(env,srcarray, 0);
		unsigned char *dsty = (unsigned char*)(*env)->GetByteArrayElements(env,dstYarray, 0);
		unsigned char *dstu = (unsigned char*)(*env)->GetByteArrayElements(env,dstUarray, 0);
		unsigned char *dstv = (unsigned char*)(*env)->GetByteArrayElements(env,dstVarray, 0);
		NV21TOYUV(src,dsty,dstu,dstv,width,height);
		(*env)->ReleaseByteArrayElements(env,srcarray,src,JNI_ABORT);
		(*env)->ReleaseByteArrayElements(env,dstYarray,dsty,JNI_ABORT);
		(*env)->ReleaseByteArrayElements(env,dstUarray,dstu,JNI_ABORT);
		(*env)->ReleaseByteArrayElements(env,dstVarray,dstv,JNI_ABORT);
		return;
}
JNIEXPORT void JNICALL Java_me_lake_librestreaming_core_ColorHelper_FIXGLPIXEL
(JNIEnv * env, jobject thiz, jintArray srcarray,jintArray dstarray,jint w,jint h) {
        unsigned int *src = (unsigned int *)(*env)->GetIntArrayElements(env,srcarray, 0);
        unsigned int *dst = (unsigned int *)(*env)->GetIntArrayElements(env,dstarray, 0);
        FIXGLPIXEL(src,dst,w,h);
        (*env)->ReleaseIntArrayElements(env,srcarray,src,JNI_ABORT);
        (*env)->ReleaseIntArrayElements(env,dstarray,dst,JNI_ABORT);
        return;
}

//rendering
JNIEXPORT void JNICALL Java_me_lake_librestreaming_render_NativeRender_renderingSurface
(JNIEnv * env, jobject thiz,jobject javaSurface,jbyteArray pixelsArray,jint w,jint h,jint size) {
	ANativeWindow* window = ANativeWindow_fromSurface(env, javaSurface);
	if(window!=NULL)
	{
		ANativeWindow_setBuffersGeometry(window,w,h,COLOR_FORMAT_NV21);
		ANativeWindow_Buffer buffer;
		if (ANativeWindow_lock(window, &buffer, NULL) == 0) {
			unsigned char *pixels = (unsigned char*)(*env)->GetByteArrayElements(env,pixelsArray, 0);
			if(buffer.width==buffer.stride){
				memcpy(buffer.bits, pixels,  size);
			}else{
				int height = h*3/2;
				int width = w;
				int i=0;
				for(;i<height;++i)
					memcpy(buffer.bits +  buffer.stride * i
						, pixels + width * i
						, width);
			}
			(*env)->ReleaseByteArrayElements(env,pixelsArray,pixels,JNI_ABORT);
			ANativeWindow_unlockAndPost(window);
		}
		ANativeWindow_release(window);
	}
	return;
}