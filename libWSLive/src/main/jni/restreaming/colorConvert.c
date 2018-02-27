#include "colorConvert.h"
#include <string.h>
#include "log.h"

void NV21TOYUV420SP(const unsigned char *src,const unsigned char *dst,int ySize)
{
	memcpy(dst,src,ySize);
	int uvSize = ySize>>1;
	int uSize = uvSize>>1;
	memcpy(dst+ySize,src+ySize+1,uvSize-1);
	unsigned char *nvcur = src+ySize;
	unsigned char *yuvcur = dst+ySize+1;
	int i=0;
	while(i<uSize)
	{
		(*yuvcur)=(*nvcur);
		yuvcur+=2;
		nvcur+=2;
		++i;
	}
}
void NV21TOYUV420P(const unsigned char *src,const unsigned char *dst,int ySize)
{
	memcpy(dst,src,ySize);
	int uSize = ySize>>2;
	unsigned char *srcucur = src+ySize+1;
	unsigned char *srcvcur = src+ySize;
	unsigned char *dstucur = dst+ySize;
	unsigned char *dstvcur = dst+ySize+uSize;
	int i=0;
	while(i<uSize)
	{
		(*dstucur)=(*srcucur);
		(*dstvcur)=(*srcvcur);
		srcucur+=2;
		srcvcur+=2;
		++dstucur;
		++dstvcur;
		++i;
	}
}
void YUV420SPTOYUV420P(const unsigned char *src,const unsigned char *dst,int ySize)
{
	memcpy(dst,src,ySize);
	int uSize = ySize>>2;
	unsigned char *srcucur = src+ySize;
	unsigned char *srcvcur = src+ySize+1;
	unsigned char *dstucur = dst+ySize;
	unsigned char *dstvcur = dst+ySize+uSize;
	int i=0;
	while(i<uSize)
	{
		(*dstucur)=(*srcucur);
		(*dstvcur)=(*srcvcur);
		srcucur+=2;
		srcvcur+=2;
		++dstucur;
		++dstvcur;
		++i;
	}
}
void NV21TOARGB(const unsigned char *src,const unsigned int *dst,int width,int height)
{
	int frameSize = width * height;

	int i = 0, j = 0,yp = 0;
	int uvp = 0, u = 0, v = 0;
	int y1192 = 0, r = 0, g = 0, b = 0;
	unsigned int *target=dst;
	for (j = 0, yp = 0; j < height; j++)
	{
		uvp = frameSize + (j >> 1) * width;
		u = 0;
		v = 0;
		for (i = 0; i < width; i++, yp++)
		{
			int y = (0xff & ((int) src[yp])) - 16;
			if (y < 0)
				y = 0;
			if ((i & 1) == 0)
			{
				v = (0xff & src[uvp++]) - 128;
				u = (0xff & src[uvp++]) - 128;
			}

			y1192 = 1192 * y;
			r = (y1192 + 1634 * v);
			g = (y1192 - 833 * v - 400 * u);
			b = (y1192 + 2066 * u);

			if (r < 0) r = 0; else if (r > 262143) r = 262143;
			if (g < 0) g = 0; else if (g > 262143) g = 262143;
			if (b < 0) b = 0; else if (b > 262143) b = 262143;
			target[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
		}
	}
}
#define IS_FLIP_H ((FLAG_DIRECTION_FLIP_HORIZONTAL&directionFlag)!=0)
#define IS_FLIP_V ((FLAG_DIRECTION_FLIP_VERTICAL&directionFlag)!=0)
void NV21Transform(const unsigned char *src,const unsigned char *dst,int srcWidth,int srcHeight,int directionFlag)
{
	unsigned char *cdst=dst;
	unsigned char *csrc=src;
	int rotate=0;
	int hflip=0;
	int vflip=0;
	if((FLAG_DIRECTION_ROATATION_0&directionFlag)!=0 || (FLAG_DIRECTION_ROATATION_180&directionFlag)!=0){
		rotate =0;
	}else{
		rotate =1;
	}

	if((FLAG_DIRECTION_ROATATION_0&directionFlag)!=0 || (FLAG_DIRECTION_ROATATION_90&directionFlag)!=0){
		hflip = IS_FLIP_H?1:0;
		vflip = IS_FLIP_V?1:0;
	}else{
		if(IS_FLIP_V){
			hflip = IS_FLIP_H?0:1;
			vflip = IS_FLIP_H?0:0;
		}else{
			hflip = IS_FLIP_H?0:1;
			vflip = IS_FLIP_H?1:1;
		}
	}
	int ySize=srcHeight*srcWidth;
	int totalSize = ySize*3 / 2;
	int yStart,yStep,xStep;
	if(rotate==0 && hflip==0 && vflip==0){
		memcpy(cdst,csrc,totalSize);
		return;
	}
	int srcX,srcY,srcCurr;
	int dstX,dstY,dstCurr;
	int halfHeight=srcHeight>>1,halfWidth=srcWidth>>1;
	if(rotate==1){
		//transformY
		if(hflip==1){
			yStart=vflip==1?ySize-srcHeight:ySize-1;
			yStep=vflip==1?1:-1;
			xStep=-srcHeight;
		}else{
			yStart=vflip==1?0:srcHeight-1;
			yStep=vflip==1?1:-1;
			xStep=srcHeight;
		}
		srcCurr=-1;
		for(srcY=0;srcY<srcHeight;++srcY){
			dstCurr = yStart;
			for(srcX=0;srcX<srcWidth;++srcX){
				cdst[dstCurr]=csrc[++srcCurr];
				dstCurr+=xStep;
			}
			yStart+=yStep;
		}
		//transformVU
		if(hflip==1){
			yStart=vflip==1?totalSize-srcHeight:totalSize-2;
			yStep=vflip==1?2:-2;
			xStep=-srcHeight;
		}else{
			yStart=vflip==1?ySize:ySize+srcHeight-2;
			yStep=vflip==1?2:-2;
			xStep=srcHeight;
		}
		srcCurr=ySize-1;
		for(srcY=0;srcY<halfHeight;++srcY){
			dstCurr = yStart;
			for(srcX=0;srcX<halfWidth;++srcX){
				cdst[dstCurr]=csrc[++srcCurr];
				cdst[dstCurr+1]=csrc[++srcCurr];
				dstCurr+=xStep;
			}
			yStart+=yStep;
		}
	}else{
		if(vflip==1 && hflip==0){
			//transformY
			yStart = ySize-srcWidth;
			srcCurr=-1;
			for(srcY=0;srcY<srcHeight;++srcY){
				dstCurr = yStart-1;
				for(srcX=0;srcX<srcWidth;++srcX){
					cdst[++dstCurr]=csrc[++srcCurr];
				}
				yStart-=srcWidth;
			}
			//transformVU
			yStart=totalSize-srcWidth;
			for(srcY=0;srcY<halfHeight;++srcY){
				dstCurr = yStart-1;
				for(srcX=0;srcX<halfWidth;++srcX){
					cdst[++dstCurr]=csrc[++srcCurr];
					cdst[++dstCurr]=csrc[++srcCurr];
				}
				yStart-=srcWidth;
			}
		}else{
			yStep=vflip==1?-srcWidth:srcWidth;
			yStart=vflip==1?ySize-1:srcWidth-1;
			//transformY
			srcCurr=-1;
			for(srcY=0;srcY<srcHeight;++srcY){
				dstCurr = yStart+1;
				for(srcX=0;srcX<srcWidth;++srcX){
					cdst[--dstCurr]=csrc[++srcCurr];
				}
				yStart+=yStep;
			}
			//transformVU
			yStart=vflip==1?totalSize-1:ySize+srcWidth-1;
			for(srcY=0;srcY<halfHeight;++srcY){
				dstCurr = yStart;
				for(srcX=0;srcX<halfWidth;++srcX){
					cdst[dstCurr-1]=csrc[++srcCurr];
					cdst[dstCurr]=csrc[++srcCurr];
					dstCurr-=2;
				}
				yStart+=yStep;
			}
		}
	}

}
void NV21TOYUV(const unsigned char *src,const unsigned char *dstY,const unsigned char *dstU,const unsigned char *dstV,int width,int height)
{
	int ySize=width*height;
	int uvSize=ySize>>1;
	int uSize = uvSize>>1;
	//y
	memcpy(dstY,src,ySize);
	//uv
	unsigned char *srcucur = src+ySize+1;
	unsigned char *srcvcur = src+ySize;
	unsigned char *dstucur = dstU;
	unsigned char *dstvcur = dstV;
	int i=0;
	while(i<uSize)
	{
		(*dstucur)=(*srcucur);
		(*dstvcur)=(*srcvcur);
		srcucur+=2;
		srcvcur+=2;
		++dstucur;
		++dstvcur;
		++i;
	}
}
void FIXGLPIXEL(const unsigned int *src,unsigned int *dst,int width,int height)
{
    int i=0;
    int x,y;
    unsigned char temp;
    unsigned char *srcucur;
    unsigned char *dstucur;
    unsigned char *dstu=dst;
    unsigned char *srcu=src;
    for(y=0;y<height;y++)
    {
        srcucur=(srcu+y*width*4);
        int step=(height-y-1)*width*4;
        dstucur=(dstu+step);
        dstucur+=3;
        for(x=0;x<width;x++){
            (*dstucur)=(unsigned char)(*(srcucur+3));
            (*(dstucur+1))=(unsigned char)(*(srcucur+2));
            (*(dstucur+2))=(unsigned char)(*(srcucur+1));
            (*(dstucur+3))=(unsigned char)(*(srcucur));
            srcucur+=4;
            dstucur+=4;
        }
    }
}