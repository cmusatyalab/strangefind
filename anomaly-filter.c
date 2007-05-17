#include <stdlib.h>
#include <stdio.h>
#include <math.h>
#include <string.h>

#include "lib_filter.h"

typedef struct {
  uint64_t count;
  double sum;
  double sum_of_squares;
} stats_t;

typedef struct {
  int size;
  char **name_array;
  stats_t *stats;
} context_t;


// 3 functions for diamond filter interface
int f_init_afilter (int num_arg, char **args,
		    int bloblen, void *blob_data,
		    const char *filter_name,
		    void **filter_args) {
  int i;

  // make space for things to examine
  context_t *ctx = (context_t *) malloc(sizeof(context_t));

  // fill in
  ctx->size = num_arg;
  ctx->name_array = (char **) calloc(ctx->size, sizeof(char *));
  ctx->stats = (stats_t *) calloc(ctx->size, sizeof(stats_t));

  // fill in names
  for (i = 0; i < ctx->size; i++) {
    ctx->name_array[i] = strdup(args[i]);
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
    double sum = ctx->stats[i].sum;
    int count = ctx->stats[i].count;
    double mean = sum / count;
    double variance = (ctx->stats[i].sum_of_squares - mean * sum) / count;
    double stddev = sqrt(variance);

    if (d > mean + (2 * stddev) || d < mean - (2 * stddev)) {
      // flag it
      printf(" *** %s is anomalous: %g (mean: %g, stddev: %g)\n",
	     ctx->name_array[i], d, mean, stddev);
      if (!result) {
	result = 100;
	lf_write_attr(ohandle, "anomalous-value.int", sizeof(int),
		      (unsigned char *) &i);
      }
    }

    // add to sum
    printf("%s: %g\n", ctx->name_array[i], d);
    ctx->stats[i].count++;
    ctx->stats[i].sum += d;
    ctx->stats[i].sum_of_squares += d * d;
  }

  return result;
}



int f_fini_afilter (void *filter_args) {
  context_t *ctx = (context_t *) filter_args;

  int i;
  for (i = 0; i < ctx->size; i++) {
    free(ctx->name_array[i]);
  }
  free(ctx->name_array);
  free(ctx->stats);

  free(ctx);
  return 0;
}
