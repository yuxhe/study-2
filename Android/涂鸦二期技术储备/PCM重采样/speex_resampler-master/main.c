

#ifdef __cplusplus
extern "C" {
#endif

#include "timing.h"
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
//ref https://github.com/mackron/dr_libs/blob/master/dr_wav.h
#define DR_WAV_IMPLEMENTATION

#include "dr_wav.h"
#include "speex_resampler.h"

void resampler(char *in_file, char *out_file);

void wavWrite_int16(char *filename, int16_t *buffer, int sampleRate, uint32_t totalSampleCount) {
    drwav_data_format format;
    format.container = drwav_container_riff;     // <-- drwav_container_riff = normal WAV files, drwav_container_w64 = Sony Wave64.
    format.format = DR_WAVE_FORMAT_PCM;          // <-- Any of the DR_WAVE_FORMAT_* codes.
    format.channels = 1;
    format.sampleRate = (drwav_uint32) sampleRate;
    format.bitsPerSample = 16;
    drwav *pWav = drwav_open_file_write(filename, &format);
    if (pWav) {
        drwav_uint64 samplesWritten = drwav_write(pWav, totalSampleCount, buffer);
        drwav_uninit(pWav);
        if (samplesWritten != totalSampleCount) {
            fprintf(stderr, "write file error.\n");
            exit(1);
        }
    }
}

int16_t *wavRead_int16(char *filename, uint32_t *sampleRate, uint64_t *totalSampleCount) {
    unsigned int channels;
    int16_t *buffer = drwav_open_and_read_file_s16(filename, &channels, sampleRate, totalSampleCount);
    if (buffer == NULL) {
        printf("read file error.\n");
    }
    if (channels != 1) {
        drwav_free(buffer);
        buffer = NULL;
        *sampleRate = 0;
        *totalSampleCount = 0;
    }
    return buffer;
}

void splitpath(const char *path, char *drv, char *dir, char *name, char *ext) {
    const char *end;
    const char *p;
    const char *s;
    if (path[0] && path[1] == ':') {
        if (drv) {
            *drv++ = *path++;
            *drv++ = *path++;
            *drv = '\0';
        }
    } else if (drv)
        *drv = '\0';
    for (end = path; *end && *end != ':';)
        end++;
    for (p = end; p > path && *--p != '\\' && *p != '/';)
        if (*p == '.') {
            end = p;
            break;
        }
    if (ext)
        for (s = end; (*ext = *s++);)
            ext++;
    for (p = end; p > path;)
        if (*--p == '\\' || *p == '/') {
            p++;
            break;
        }
    if (name) {
        for (s = p; s < end;)
            *name++ = *s++;
        *name = '\0';
    }
    if (dir) {
        for (s = path; s < p;)
            *dir++ = *s++;
        *dir = '\0';
    }
}


#ifndef MIN
#define MIN(a, b) ((a) < (b) ? (a) : (b))
#endif


void resampler(char *in_file, char *out_file) {
    uint32_t sampleRate = 0;
    uint64_t totalSampleCount = 0;
    int16_t *data_in = wavRead_int16(in_file, &sampleRate, &totalSampleCount);
    uint32_t out_sampleRate = 16000;
    uint32_t out_size = (uint32_t) (totalSampleCount * ((float) out_sampleRate / sampleRate));
    int16_t *data_out = (int16_t *) malloc(out_size * sizeof(int16_t));

    if (data_in != NULL && data_out != NULL) {
        int errcode = 0;
        int quality = SPEEX_RESAMPLER_QUALITY_MAX;
        SpeexResamplerState *resamplerState = speex_resampler_init(1, sampleRate, out_sampleRate, quality, &errcode);
        if (errcode == RESAMPLER_ERR_SUCCESS) {
            spx_uint32 in_len = (spx_uint32) totalSampleCount;
            spx_uint32 out_len = out_size;
            double startTime = now();
            errcode = speex_resampler_process_int(resamplerState, 0, data_in, &in_len, data_out, &out_len);
            double time_interval = calcElapsed(startTime, now());
            printf("process: %s \n", speex_resampler_strerror(errcode));
            speex_resampler_destroy(resamplerState);
            printf("time interval: %d ms\n ", (int) (time_interval * 1000));
            wavWrite_int16(out_file, data_out, out_sampleRate, (uint32_t) out_size);
        } else {
            printf("error: %s \n", speex_resampler_strerror(errcode));
        }
        free(data_in);
        free(data_out);
    } else {
        if (data_in) free(data_in);
        if (data_out) free(data_out);
    }
}

int main(int argc, char *argv[]) {
    printf("Audio Processing\n");
    printf("speex resampler\n");
    printf("blog:http://cpuimage.cnblogs.com/\n");
    if (argc < 2)
        return -1;

    char *in_file = argv[1];
    char drive[3];
    char dir[256];
    char fname[256];
    char ext[256];
    char out_file[1024];
    splitpath(in_file, drive, dir, fname, ext);
    sprintf(out_file, "%s%s%s_out%s", drive, dir, fname, ext);
    resampler(in_file, out_file);

    printf("press any key to exit.\n");
    getchar();
    return 0;
}

#ifdef __cplusplus
}
#endif

