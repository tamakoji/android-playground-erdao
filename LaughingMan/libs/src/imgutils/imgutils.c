#include "imgutils.h"

void gray8toRGB32(unsigned char* src, unsigned int* dst, int len)
{
	int i,Y;
	for(i=0; i<len; i++){
		Y = *src & 0xff; 
		*dst = 0xff000000 + (Y << 16) + (Y << 8) + Y;
		src++;
		dst++;
	}
}
