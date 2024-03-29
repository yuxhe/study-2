#define FP_DIGITS 15
#define FP_FACTOR (1 << FP_DIGITS)
#define FP_MASK (FP_FACTOR - 1)

#define MAX_HWORD (32767)
#define MIN_HWORD (-32768)

#ifndef MAX
#define MAX(x,y) ((x)>(y) ?(x):(y))
#endif
#ifndef MIN
#define MIN(x,y) ((x)<(y) ?(x):(y))
#endif

struct rs_data {
	double factor;
	/* time is 32-bit fixed-point using 2^FP_FACTOR as factor */
	unsigned int time;
	int in_buf_ptr;
	int out_buf_ptr;
	int in_buf_read;
	int in_buf_offset;
	int in_buf_used;
	int in_buf_size;
	int out_buf_size;
	short *in_buf;
	short *out_buf;
};

int resample_simple(double factor, short *in_buf, short *out_buf, int buffer_size);

struct rs_data *resample_init(int in_rate, int out_rate, int buffer_size);

int resample(struct rs_data *rs, short *in_buf, int in_buf_size, short *out_buf,
	     int out_buf_size, int last);

void resample_close(struct rs_data *rs);