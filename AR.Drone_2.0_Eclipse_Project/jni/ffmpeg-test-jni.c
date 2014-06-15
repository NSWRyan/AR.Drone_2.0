/**
 this is the wrapper of the native functions
 **/
/*android specific headers*/
#include <jni.h>
#include <android/log.h>
#include <android/bitmap.h>
/*standard library*/
 #include <time.h>
 #include <math.h>
 #include <limits.h>
 #include <stdio.h>
 #include <stdlib.h>
 #include <inttypes.h>
 #include <unistd.h>
 #include <assert.h>

/*ffmpeg headers*/

#include <libavformat/avformat.h>
#include <libswscale/swscale.h>


static void fill_bitmap(AndroidBitmapInfo* info, void *pixels, AVFrame *pFrame) {
	uint8_t *frameLine;

	int yy;
	for (yy = 0; yy < info->height; yy++) {
		uint8_t* line = (uint8_t*) pixels;
		frameLine = (uint8_t *) pFrame->data[0] + (yy * pFrame->linesize[0]);

		int xx;
		for (xx = 0; xx < info->width; xx++) {
			int out_offset = xx * 4;
			int in_offset = xx * 3;

			line[out_offset] = frameLine[in_offset];
			line[out_offset + 1] = frameLine[in_offset + 1];
			line[out_offset + 2] = frameLine[in_offset + 2];
			line[out_offset + 3] = 0;
		}
		pixels = (char*) pixels + info->stride;
	}
}
void SaveFrame(AVFrame *pFrame, int width, int height, int iFrame) {
	FILE *pFile;
	char szFilename[32];
	int y;

	// Open file
	sprintf(szFilename, "/sdcard/frame%d.ppm", iFrame);
	pFile = fopen(szFilename, "wb");
	if (pFile == NULL)
		return;

	// Write header
	fprintf(pFile, "P6\n%d %d\n255\n", width, height);

	// Write pixel data
	for (y = 0; y < height; y++)
		fwrite(pFrame->data[0] + y * pFrame->linesize[0], 1, width * 3, pFile);

	// Close file
	fclose(pFile);
}
jstring Java_id_r5xscn_ardrone_ARDrone_naInit(
		JNIEnv *pEnv, jobject pObj, jstring bitmap) {
	av_register_all();

	AVFormatContext *pFormatCtx;

	if (av_open_input_file(&pFormatCtx, "tcp://127.0.0.1:1234", NULL, 0, NULL)
			!= 0)
		return (*pEnv)->NewStringUTF(pEnv, "TCP error.");
	if (av_find_stream_info(pFormatCtx) < 0)
		return (*pEnv)->NewStringUTF(pEnv, "no stream info");

	dump_format(pFormatCtx, 0, "tcp://127.0.0.1:1234", 0);
	int i;
	AVCodecContext *pCodecCtx;

	// Find the first video stream
	int videoStream = -1;
	for (i = 0; i < pFormatCtx->nb_streams; i++)
		if (pFormatCtx->streams[i]->codec->codec_type == AVMEDIA_TYPE_VIDEO) {
			videoStream = i;
			break;
		}
	if (videoStream == -1)
		return (*pEnv)->NewStringUTF(pEnv, "no Video stream");

	pCodecCtx = pFormatCtx->streams[videoStream]->codec;

	AVCodec *pCodec;

	// Find the decoder for the video stream
	pCodec = avcodec_find_decoder(pCodecCtx->codec_id);
	if (pCodec == NULL) {
		fprintf(stderr, "Unsupported codec!\n");
		return (*pEnv)->NewStringUTF(pEnv, "Codec error");
	}

	// Open codec
	if (avcodec_open(pCodecCtx, pCodec) < 0)
		return (*pEnv)->NewStringUTF(pEnv, "Couldn't open codec."); // Could not open codec

	AVFrame *pFrame;
	AVFrame *pFrameRGB;
	// Allocate video frame
	pFrame = avcodec_alloc_frame();

	// Allocate an AVFrame structure
	pFrameRGB = avcodec_alloc_frame();
	if (pFrameRGB == NULL)
		return (*pEnv)->NewStringUTF(pEnv, "RGB null");

	uint8_t *buffer;
	int numBytes;
	// Determine required buffer size and allocate buffer
	numBytes = avpicture_get_size(PIX_FMT_RGB24, pCodecCtx->width,
			pCodecCtx->height);
	buffer = (uint8_t *) av_malloc(numBytes * sizeof(uint8_t));
	avpicture_fill((AVPicture *) pFrameRGB, buffer, PIX_FMT_RGB24,
			pCodecCtx->width, pCodecCtx->height);

	AndroidBitmapInfo info;
	void* pixels;
	int ret;

	int frameFinished;
	AVPacket packet;

	if ((ret = AndroidBitmap_getInfo(pEnv, bitmap, &info)) < 0) {
		return "error0";
	}

	if ((ret = AndroidBitmap_lockPixels(pEnv, bitmap, &pixels)) < 0) {
		return "error1";
	}

	i = 0;
	if (av_read_frame(pFormatCtx, &packet) >= 0) {
		// Is this a packet from the video stream?
		if (packet.stream_index == videoStream) {
			// Decode video frame
			avcodec_decode_video2(pCodecCtx, pFrame, &frameFinished, &packet);
			int twidth=640;
			int theight=360;
			// Did we get a video frame?
			if (frameFinished) {
				// Convert the image from its native format to RGB
				static struct SwsContext * img_convert_ctx;

				img_convert_ctx = sws_getContext(pCodecCtx->width,
						pCodecCtx->height, pCodecCtx->pix_fmt, twidth,
						theight, PIX_FMT_RGB24, SWS_BICUBIC, NULL,
						NULL, NULL);
				sws_scale(img_convert_ctx,
						(const uint8_t * const *) pFrame->data,
						pFrame->linesize, 0, pCodecCtx->height, pFrameRGB->data,
						pFrameRGB->linesize);

				fill_bitmap(&info, pixels, pFrameRGB);
			}
		}

		// Free the packet that was allocated by av_read_frame
		av_free_packet(&packet);
	}
// Free the RGB image
	av_free(buffer);
	av_free(pFrameRGB);

// Free the YUV frame
	av_free(pFrame);

// Close the codec
	avcodec_close(pCodecCtx);

// Close the video file
	av_close_input_file(pFormatCtx);
	return (*pEnv)->NewStringUTF(pEnv, "Complete");

}

