#define _GNU_SOURCE
#include <stdlib.h>
#include <stdio.h>
#include <math.h>
#include <string.h>

#include "lib_filter.h"


static const char *COUNT_KEY = "count";
static const char *SUM_KEY = "sum";
static const char *SUM_OF_SQUARES_KEY = "sum_of_squares";

typedef struct {
  int size;
  char **name_array;
  double *stddev_array;
  lf_session_variable_t **stats;
  int min_count;
} context_t;

static double composer_sum(double a, double b) {
  return a + b;
}


// 3 functions for diamond filter interface
int f_init_afilter (int num_arg, char **args,
		    int bloblen, void *blob_data,
		    const char *filter_name,
		    void **filter_args) {
  int i;

  // check args
  if (num_arg < 2) {
    return -1;
  }

  // make space for things to examine
  context_t *ctx = (context_t *) malloc(sizeof(context_t));

  // args is:
  // 1. min_count
  // 2. random string to supress caching
  // rest. pairs of attribute names and standard deviations
  ctx->size = (num_arg - 2) / 2;

  printf("uuid: %s\n", args[1]);

  // fill in
  ctx->name_array = (char **) calloc(ctx->size, sizeof(char *));
  ctx->stddev_array = (double *) calloc(ctx->size, sizeof(double));
  ctx->min_count = strtol(args[0], NULL, 10);

  // null terminated stats list
  int stats_len = 3 * ctx->size;
  ctx->stats = calloc(stats_len + 1, sizeof(lf_session_variable_t *));
  for (i = 0; i < stats_len; i++) {
    ctx->stats[i] = malloc(sizeof(lf_session_variable_t));
    ctx->stats[i]->composer = composer_sum;
  }

  // fill in arrays
  for (i = 0; i < ctx->size; i++) {
    ctx->name_array[i] = strdup(args[(i*2)+2]);
    ctx->stddev_array[i] = strtod(args[(i*2)+3], NULL);

    char *name;
    asprintf(&name, "%s_%s", ctx->name_array[i], COUNT_KEY);
    ctx->stats[(i * 3) + 0]->name = name;
    asprintf(&name, "%s_%s", ctx->name_array[i], SUM_KEY);
    ctx->stats[(i * 3) + 1]->name = name;
    asprintf(&name, "%s_%s", ctx->name_array[i], SUM_OF_SQUARES_KEY);
    ctx->stats[(i * 3) + 2]->name = name;
  }

  // ready?
  *filter_args = ctx;
  return 0;
}



int f_eval_afilter (lf_obj_handle_t ohandle, void *filter_args) {
  // afilter
  context_t *ctx = (context_t *) filter_args;

  int i;
  int err;

  // for attributes from diamond
  size_t len;

  // get stats
  lf_get_session_variables(ohandle, ctx->stats);

  typedef union {
    double *d;
    unsigned char *c;
  } char_double_t;

  char_double_t val;

  int result = 0;
  // compute anomalousness for each thing
  // XXX stats done by non-statistician
  for (i = 0; i < ctx->size; i++) {
    // get each thing
    err = lf_ref_attr(ohandle, ctx->name_array[i], &len,
		      &val.c);
    double d = *(val.d);

    // check
    int count = ctx->stats[(i * 3) + 0]->value;
    double sum = ctx->stats[(i * 3) + 1]->value;
    double sum_sq = ctx->stats[(i * 3) + 2]->value;

    double mean = sum / count;
    double variance = (sum_sq - mean * sum) / count;
    double stddev = sqrt(variance);

    double num_stddev = ctx->stddev_array[i];
    if (count > ctx->min_count
	&& (d > mean + (num_stddev * stddev)
	    || d < mean - (num_stddev * stddev))) {
      // flag it
      printf(" *** %s is anomalous: %g (mean: %g, stddev: %g)\n",
	     ctx->name_array[i], d, mean, stddev);
      if (!result) {
	result = 100;
	lf_write_attr(ohandle, "anomalous-value.int", sizeof(int),
		      (unsigned char *) &i);
	lf_write_attr(ohandle, "anomalous-value-count.int", sizeof(int),
		      (unsigned char *) &count);
	lf_write_attr(ohandle, "anomalous-value-mean.double", sizeof(double),
		      (unsigned char *) &mean);
	lf_write_attr(ohandle, "anomalous-value-stddev.double", sizeof(double),
		      (unsigned char *) &stddev);
      }
    }

    // add to sum
    printf("%s: %g\n", ctx->name_array[i], d);
    ctx->stats[(i * 3) + 0]->value = 1;      // count += 1
    ctx->stats[(i * 3) + 1]->value = d;      // sum += d
    ctx->stats[(i * 3) + 2]->value = d * d;  // sum_sq += (d * d)
  }

  // update
  lf_update_session_variables(ohandle, ctx->stats);

  return result;
}



int f_fini_afilter (void *filter_args) {
  context_t *ctx = (context_t *) filter_args;

  int i;
  for (i = 0; i < ctx->size; i++) {
    free(ctx->name_array[i]);
  }

  for (i = 0; i < ctx->size * 3; i++) {
    free(ctx->stats[i]);
  }

  free(ctx->name_array);
  free(ctx->stddev_array);
  free(ctx->stats);

  free(ctx);
  return 0;
}
