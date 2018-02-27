#ifndef __COLORCONVERT_H__
#define __COLORCONVERT_H__

#define COLOR_FORMAT_NV21 17

#define FLAG_DIRECTION_FLIP_HORIZONTAL	0x01
#define FLAG_DIRECTION_FLIP_VERTICAL	0x02
#define FLAG_DIRECTION_ROATATION_0 		0x10
#define FLAG_DIRECTION_ROATATION_90		0x20
#define FLAG_DIRECTION_ROATATION_180	0x40
#define FLAG_DIRECTION_ROATATION_270	0x80

void NV21TOYUV420SP(const unsigned char *src,const unsigned char *dst,int ySize);
void YUV420SPTOYUV420P(const unsigned char *src,const unsigned char *dst,int ySize);
void NV21TOYUV420P(const unsigned char *src,const unsigned char *dst,int ySize);
void NV21TOARGB(const unsigned char *src,const unsigned int *dst,int width,int height);
void NV21Transform(const unsigned char *src,const unsigned char *dst,int dstWidth,int dstHeight,int directionFlag);
void NV21TOYUV(const unsigned char *src,const unsigned char *dstY,const unsigned char *dstU,const unsigned char *dstV,int width,int height);
void FIXGLPIXEL(const unsigned int *src,unsigned int *dst,int width,int height);
#endif