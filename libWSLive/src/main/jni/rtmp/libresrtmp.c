#include <malloc.h>
#include "libresrtmp.h"
#include "rtmp.h"
/*
 * Class:     me_lake_librestreaming_rtmp_RtmpClient
 * Method:    open
 * Signature: (Ljava/lang/String;)I
 */
 JNIEXPORT jlong JNICALL Java_me_lake_librestreaming_rtmp_RtmpClient_open
 (JNIEnv * env, jobject thiz, jstring url_, jboolean isPublishMode) {
 	const char *url = (*env)->GetStringUTFChars(env, url_, 0);
 	LOGD("RTMP_OPENING:%s",url);
 	RTMP* rtmp = RTMP_Alloc();
 	if (rtmp == NULL) {
 		LOGD("RTMP_Alloc=NULL");
 		return NULL;
 	}

 	RTMP_Init(rtmp);
 	int ret = RTMP_SetupURL(rtmp, url);

 	if (!ret) {
 		RTMP_Free(rtmp);
 		rtmp=NULL;
 		LOGD("RTMP_SetupURL=ret");
 		return NULL;
 	}
 	if (isPublishMode) {
 		RTMP_EnableWrite(rtmp);
 	}

 	ret = RTMP_Connect(rtmp, NULL);
 	if (!ret) {
 		RTMP_Free(rtmp);
 		rtmp=NULL;
 		LOGD("RTMP_Connect=ret");
 		return NULL;
 	}
 	ret = RTMP_ConnectStream(rtmp, 0);

 	if (!ret) {
 		ret = RTMP_ConnectStream(rtmp, 0);
 		RTMP_Close(rtmp);
 		RTMP_Free(rtmp);
 		rtmp=NULL;
 		LOGD("RTMP_ConnectStream=ret");
 		return NULL;
 	}
 	(*env)->ReleaseStringUTFChars(env, url_, url);
 	LOGD("RTMP_OPENED");
 	return rtmp;
 }



/*
 * Class:     me_lake_librestreaming_rtmp_RtmpClient
 * Method:    read
 * Signature: ([CI)I
 */
 JNIEXPORT jint JNICALL Java_me_lake_librestreaming_rtmp_RtmpClient_read
 (JNIEnv * env, jobject thiz,jlong rtmp, jbyteArray data_, jint offset, jint size) {

 	char* data = malloc(size*sizeof(char));

 	int readCount = RTMP_Read((RTMP*)rtmp, data, size);

 	if (readCount > 0) {
        (*env)->SetByteArrayRegion(env, data_, offset, readCount, data);  // copy
    }
    free(data);

    return readCount;
}

/*
 * Class:     me_lake_librestreaming_rtmp_RtmpClient
 * Method:    write
 * Signature: ([CI)I
 */
 JNIEXPORT jint JNICALL Java_me_lake_librestreaming_rtmp_RtmpClient_write
 (JNIEnv * env, jobject thiz,jlong rtmp, jbyteArray data, jint size, jint type, jint ts) {
 	LOGD("start write");
 	jbyte *buffer = (*env)->GetByteArrayElements(env, data, NULL);
 	RTMPPacket *packet = (RTMPPacket*)malloc(sizeof(RTMPPacket));
 	RTMPPacket_Alloc(packet, size);
 	RTMPPacket_Reset(packet);
    if (type == RTMP_PACKET_TYPE_INFO) { // metadata
    	packet->m_nChannel = 0x03;
    } else if (type == RTMP_PACKET_TYPE_VIDEO) { // video
    	packet->m_nChannel = 0x04;
    } else if (type == RTMP_PACKET_TYPE_AUDIO) { //audio
    	packet->m_nChannel = 0x05;
    } else {
    	packet->m_nChannel = -1;
    }

    packet->m_nInfoField2  =  ((RTMP*)rtmp)->m_stream_id;

    LOGD("write data type: %d, ts %d", type, ts);

    memcpy(packet->m_body,  buffer,  size);
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    packet->m_hasAbsTimestamp = FALSE;
    packet->m_nTimeStamp = ts;
    packet->m_packetType = type;
    packet->m_nBodySize  = size;
    int ret = RTMP_SendPacket((RTMP*)rtmp, packet, 0);
    RTMPPacket_Free(packet);
    free(packet);
    (*env)->ReleaseByteArrayElements(env, data, buffer, 0);
    if (!ret) {
    	LOGD("end write error %d", sockerr);
		return sockerr;
    }else
    {
    	LOGD("end write success");
		return 0;
    }
}

/*
 * Class:     me_lake_librestreaming_rtmp_RtmpClient
 * Method:    close
 * Signature: ()I
 */
 JNIEXPORT jint JNICALL Java_me_lake_librestreaming_rtmp_RtmpClient_close
 (JNIEnv * env,jlong rtmp, jobject thiz) {
 	RTMP_Close((RTMP*)rtmp);
 	RTMP_Free((RTMP*)rtmp);
 	return 0;
 }

JNIEXPORT jstring JNICALL Java_me_lake_librestreaming_rtmp_RtmpClient_getIpAddr
		(JNIEnv * env,jobject thiz,jlong rtmp) {
	if(rtmp!=0){
		RTMP* r= (RTMP*)rtmp;
		return (*env)->NewStringUTF(env, r->ipaddr);
	}else {
		return (*env)->NewStringUTF(env, "");
	}
}