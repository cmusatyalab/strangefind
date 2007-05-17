#include "lib_filter.h"

// inspired by
// http://www.csee.usf.edu/~christen/tools/moments.c
static void compute_moments(double *data_array, int len,
			    double *m0,
			    double *m1,
			    double *m2,
			    double *m3) {
  double mean = 0.0;
  for (int i = 0; i < len; i++) {
    printf("%g\n", data_array[i]);
    mean += data_array[i] / (double) len;
  }

  double raw_moments[4] = {0, 0, 0, 0};
  double central_moments[4] = {0, 0, 0, 0};

  for (int i = 0; i < len; i++) {
    for (int j = 0; j < 4; j++) {
      raw_moments[j] += (pow(data_array[i], j + 1.0) / len);
      central_moments[j] += (pow((data_array[i] - mean), j + 1.0) / len);
    }
  }

  printf("\nr: %g\n%g\n%g\n%g\n", raw_moments[0], raw_moments[1], raw_moments[2], raw_moments[3] );
  printf("c: %g\n%g\n%g\n%g\n\n", central_moments[0], central_moments[1], central_moments[2], central_moments[3] );

  *m0 = raw_moments[0];
  *m1 = raw_moments[1];
  *m2 = raw_moments[2];
  *m3 = raw_moments[3];
}



typedef struct {
  int size;
  double *table_array;
  char **name_array;
} context_t;


// 3 functions for diamond filter interface
int f_init_afilter (int num_arg, char **args,
		    int bloblen, void *blob_data,
		    const char *filter_name,
		    void **filter_args) {
  // make space for things to examine
  context_t *ctx = (context_t *) malloc(sizeof(context_t));
  ctx->size = num_arg;
  ctx->table_array = (double *) malloc(sizeof(context_t) * ctx->size);
  ctx->name_array = (
  for (int i = 0; i < ctx->size; i++) {

  }


  *filter_args = ctx;
  return 0;
}



  int f_eval_afilter (lf_obj_handle_t ohandle, void *filter_args) {
    // afilter
    GList *clist;
    afilter_state_t *cr = (afilter_state_t *) filter_args;
    int num_afilter;
    int num_afilter_in_result = 0;

    // for attributes from diamond
    size_t len;
    unsigned char *data;



    // get the data from the ohandle, convert to IplImage
    int w;
    int h;

    // width
    lf_ref_attr(ohandle, "_rows.int", &len, &data);
    h = *((int *) data);

    // height
    lf_ref_attr(ohandle, "_cols.int", &len, &data);
    w = *((int *) data);

    // image data
    lf_ref_attr(ohandle, "_rgb_image.rgbimage", &len, &data);
    lf_omit_attr(ohandle, "_rgb_image.rgbimage");

    // feed it to our processor
    clist = afilterFromImage2(cr, w, h, w * 4, 4, data);

    data = NULL;

    // add the list of afilter to the cache and the object
    // XXX !

    num_afilter = g_list_length(clist);
    double *areas = (double *) g_malloc(sizeof(double) * num_afilter);
    double *eccentricities = (double *) g_malloc(sizeof(double) * num_afilter);
    double total_area = 0.0;
    if (num_afilter > 0) {
      GList *l = clist;
      int i = 0;
      data = (unsigned char *) g_malloc(sizeof(circle_type) * num_afilter);

      // pack in and count
      while (l != NULL) {
	circle_type *p = ((circle_type *) data) + i;
	circle_type *c = (circle_type *) l->data;
	*p = *c;

	if (c->in_result) {
	  total_area += areas[num_afilter_in_result] = M_PI * c->a * c->b;
	  eccentricities[num_afilter_in_result] = compute_eccentricity(c->a, c->b);
	  num_afilter_in_result++;
	}

	i++;
	l = g_list_next(l);
      }
      lf_write_attr(ohandle, "circle-data", sizeof(circle_type) * num_afilter, data);
    }

    // compute aggregate stats
    lf_write_attr(ohandle, "circle-count", sizeof(int), (unsigned char *) &num_afilter_in_result);

    double area_fraction = total_area / (w * h);
    lf_write_attr(ohandle, "circle-area-fraction", sizeof(double), (unsigned char *) &area_fraction);

    double area_m0, area_m1, area_m2, area_m3;
    compute_moments(areas, num_afilter_in_result, &area_m0, &area_m1, &area_m2, &area_m3);
    lf_write_attr(ohandle, "circle-area-m0", sizeof(double), (unsigned char *) &area_m0);
    lf_write_attr(ohandle, "circle-area-m1", sizeof(double), (unsigned char *) &area_m1);
    lf_write_attr(ohandle, "circle-area-m2", sizeof(double), (unsigned char *) &area_m2);
    lf_write_attr(ohandle, "circle-area-m3", sizeof(double), (unsigned char *) &area_m3);

    double eccentricity_m0, eccentricity_m1, eccentricity_m2, eccentricity_m3;
    compute_moments(eccentricities, num_afilter_in_result,
		    &eccentricity_m0, &eccentricity_m1,
		    &eccentricity_m2, &eccentricity_m3);
    lf_write_attr(ohandle, "circle-eccentricity-m0", sizeof(double), (unsigned char *) &eccentricity_m0);
    lf_write_attr(ohandle, "circle-eccentricity-m1", sizeof(double), (unsigned char *) &eccentricity_m1);
    lf_write_attr(ohandle, "circle-eccentricity-m2", sizeof(double), (unsigned char *) &eccentricity_m2);
    lf_write_attr(ohandle, "circle-eccentricity-m3", sizeof(double), (unsigned char *) &eccentricity_m3);

    printf("area_fraction: %g\n", area_fraction);
    printf("area moments: %g %g %g %g\n", area_m0, area_m1, area_m2, area_m3);
    printf("ecc  moments: %g %g %g %g\n", eccentricity_m0, eccentricity_m1, eccentricity_m2, eccentricity_m3);

    printf("area_fraction hex: %llx\n", area_fraction);

    // free others
    g_list_foreach(clist, free_1_circle, NULL);
    g_list_free(clist);
    clist = NULL;
    g_free(data);
    data = NULL;
    g_free(areas);
    areas = NULL;
    g_free(eccentricities);
    eccentricities = NULL;

    // return number of afilter
    return num_afilter_in_result;
  }



  int f_fini_afilter (void *filter_args) {
    g_free((afilter_state_t *) filter_args);

    return 0;
  }
