/*
 *  StrangeFind, an anomaly detection system for the OpenDiamond Platform
 *
 *  Copyright (c) 2006-2008 Carnegie Mellon University
 *  All rights reserved.
 *
 *  StrangeFind is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, version 2.
 *
 *  StrangeFind is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with StrangeFind. If not, see <http://www.gnu.org/licenses/>.
 *
 *  Linking StrangeFind statically or dynamically with other modules is
 *  making a combined work based on StrangeFind. Thus, the terms and
 *  conditions of the GNU General Public License cover the whole
 *  combination.
 *
 *  In addition, as a special exception, the copyright holders of
 *  StrangeFind give you permission to combine StrangeFind with free software
 *  programs or libraries that are released under the GNU LGPL or the
 *  Eclipse Public License 1.0. You may copy and distribute such a system
 *  following the terms of the GNU GPL for StrangeFind and the licenses of
 *  the other code concerned, provided that you include the source code of
 *  that other code when and as the GNU GPL requires distribution of source
 *  code.
 *
 *  Note that people who make modified versions of StrangeFind are not
 *  obligated to grant this special exception for their modified versions;
 *  it is their choice whether to do so. The GNU General Public License
 *  gives permission to release a modified version without this exception;
 *  this exception also makes it possible to release a modified version
 *  which carries forward this exception.
 */

#include "ltiRGBPixel.h"
#include "ltiScaling.h"
#include "ltiDraw.h"                  // drawing tool
#include "ltiALLFunctor.h"            // image loader
#include "ltiCannyEdges.h"            // edge detector
#include "ltiFastEllipseExtraction.h" // ellipse detector
#include "ltiGaussianPyramid.h"
#include "ltiSplitImageToHSV.h"

#include "circles4.h"
#include "lib_filter.h"
#include "util.h"

#include <cstring>

#include <sys/time.h>
#include <time.h>
#include <math.h>


typedef struct {
  double minRadius;
  double maxRadius;
  double maxEccentricity;
  double minSharpness;
} circles_state_t;


// helper functions for glist
static void free_1_circle(gpointer data, gpointer user_data) {
  g_free((circle_type *) data);
}

static gint circle_radius_compare(gconstpointer a,
				  gconstpointer b) {
  const circle_type *c1 = (circle_type *) a;
  const circle_type *c2 = (circle_type *) b;

  // assume a and b are positive
  double c1_radius = quadratic_mean_radius(c1->a, c1->b);
  double c2_radius = quadratic_mean_radius(c2->a, c2->b);
  if (c1_radius < c2_radius) {
    return 1;
  } else if (c2_radius < c1_radius) {
    return -1;
  } else {
    return 0;
  }
}


static void do_canny(lti::gaussianPyramid<lti::image> &imgPyramid,
		     std::vector<lti::channel8*> &edges,
		     double minSharpness) {
  uint64_t start_time_in_ms;
  uint64_t end_time_in_ms;

  struct timeval tv;

  gettimeofday(&tv, NULL);
  start_time_in_ms = tv.tv_sec * 1000 + tv.tv_usec / 1000;

  // params
  lti::cannyEdges::parameters cannyParam;
  if (minSharpness == 0) {
    cannyParam.thresholdMax = 0.04;
  } else {
    cannyParam.thresholdMax = 0.10 + 0.90 * ((minSharpness - 1) / 4.0);
  }
  cannyParam.thresholdMin = 0.04;
  cannyParam.kernelSize = 7;

  // run
  lti::cannyEdges canny(cannyParam);
  g_assert(edges.size() == 0);

  for (int i = 0; i < imgPyramid.size(); i++) {
    printf("canny[%d]: %dx%d\n", i, imgPyramid[i].columns(),
	   imgPyramid[i].rows());
    lti::channel8 *c8 = new lti::channel8;
    canny.apply(imgPyramid[i], *c8);
    edges.push_back(c8);
  }

  gettimeofday(&tv, NULL);
  end_time_in_ms = tv.tv_sec * 1000 + tv.tv_usec / 1000;
  printf("canny done in %lld ms\n", end_time_in_ms - start_time_in_ms);
}

static GList *do_fee(std::vector<lti::channel8*> &edges,
		     circles_state_t *ct) {
  GList *result = NULL;

  uint64_t start_time_in_ms;
  uint64_t end_time_in_ms;

  struct timeval tv;

  gettimeofday(&tv, NULL);
  start_time_in_ms = tv.tv_sec * 1000 + tv.tv_usec / 1000;

  // create FEE functor
  lti::fastEllipseExtraction::parameters feeParam;
  feeParam.maxArcGap = 120;
  feeParam.minLineLen = 3;

  for (unsigned int i = 0; i < edges.size(); i++) {
    double pyrScale = pow(2, i);

    lti::fastEllipseExtraction fee(feeParam);

    // extract some ellipses
    printf("extracting from pyramid %d\n", i);
    fee.apply(*edges[i]);

    // build list
    std::vector<lti::fastEllipseExtraction::ellipseEntry>
      &ellipses = fee.getEllipseList();

    for(unsigned int j=0; j<ellipses.size(); j++) {
      float a = ellipses[j].a * pyrScale;
      float b = ellipses[j].b * pyrScale;
      float r = quadratic_mean_radius(a, b);
      gboolean in_result = TRUE;

      // determine if it should go in by radius
      if (!(ct->minRadius < 0 || r >= ct->minRadius)) {
	in_result = FALSE;
      } else if (!(ct->maxRadius < 0 || r <= ct->maxRadius)) {
	in_result = FALSE;
      }

      // compute eccentricity
      float e = compute_eccentricity(a, b);

      // if over 0.9, drop completely
      if (e > 0.9) {
	continue;
      }

      // otherwise, maybe mark
      if (e > ct->maxEccentricity) {
	in_result = FALSE;
      }

      // all set
      circle_type *c = (circle_type *)g_malloc(sizeof(circle_type));

      c->x = ellipses[j].x * pyrScale;
      c->y = ellipses[j].y * pyrScale;
      c->a = ellipses[j].a * pyrScale;
      c->b = ellipses[j].b * pyrScale;
      c->t = ellipses[j].t;
      c->in_result = in_result;

      result = g_list_prepend(result, c);

      // print ellipse data
      printf(" ellipse[%i]: center=(%.0f,%.0f) semiaxis=(%.0f,%.0f) "
	     "angle=%.1f coverage=%.1f%% \n",
	     j, c->x, c->y, c->a, c->b, c->t*180/M_PI,
	     ellipses[j].coverage*100);
    }
  }

  gettimeofday(&tv, NULL);
  end_time_in_ms = tv.tv_sec * 1000 + tv.tv_usec / 1000;
  printf("fee done in %lld ms\n", end_time_in_ms - start_time_in_ms);

  return result;
}

static circles_state_t staticState = {-1, -1, 0.4, 1};
static GList *circlesFromImage2(circles_state_t *ct,
				const int width, const int height,
				const int stride, const int bytesPerPixel,
				void *data) {
  uint64_t start_time_in_ms;
  uint64_t end_time_in_ms;

  struct timeval tv;

  gettimeofday(&tv, NULL);
  start_time_in_ms = tv.tv_sec * 1000 + tv.tv_usec / 1000;

  // load image
  g_assert(bytesPerPixel >= 3);
  lti::image img(false, height, width);
  for (int y = 0; y < height; y++) {
    for (int x = 0; x < width; x++) {
      guchar *d = ((guchar *) data) + (x * bytesPerPixel + y * stride);
      lti::rgbPixel p(d[0], d[1], d[2]);
      img[y][x] = p;
    }
  }

  // make pyramid
  int levels = (int)log2(MIN(img.rows(), img.columns()));
  lti::gaussianPyramid<lti::image> imgPyramid(levels);
  imgPyramid.generate(img);

  gettimeofday(&tv, NULL);
  end_time_in_ms = tv.tv_sec * 1000 + tv.tv_usec / 1000;
  printf("load/pyramid done in %lld ms\n", end_time_in_ms - start_time_in_ms);

  // make vector
  std::vector<lti::channel8*> edges;

  // run
  do_canny(imgPyramid, edges, ct->minSharpness);
  GList *result = do_fee(edges, ct);

  // clear vector
  for (unsigned int i = 0; i < edges.size(); i++) {
    delete edges[i];
    edges[i] = NULL;
  }

  // overlap suppression
  printf("overlap suppression ");
  fflush(stdout);

  gettimeofday(&tv, NULL);
  start_time_in_ms = tv.tv_sec * 1000 + tv.tv_usec / 1000;

  result = g_list_sort(result, circle_radius_compare);  // sort

  GList *iter;
  for (iter = result; iter != NULL; iter = g_list_next(iter)) {
    // find other centers within this circle (assume no eccentricity)
    circle_type *c1 = (circle_type *) iter->data;
    double c1_radius = quadratic_mean_radius(c1->a, c1->b);

    GList *iter2;

    // don't bother if large eccentricity
    double c1_e = compute_eccentricity(c1->a, c1->b);
    if (c1_e > 0.4) {
      continue;
    }

    iter2 = g_list_next(iter);
    while (iter2 != NULL) {
      circle_type *c2 = (circle_type *) iter2->data;
      double c2_radius = quadratic_mean_radius(c2->a, c2->b);

      // is c2 within c1?
      double xd = c2->x - c1->x;
      double yd = c2->y - c1->y;
      double dist = sqrt((xd * xd) + (yd * yd));

      // maybe delete?
      GList *next = g_list_next(iter2);
      //      printf(" dist: %g, c1_radius: %g, c2_radius: %g\n",
      //	     dist, c1_radius, c2_radius);

      // XXX fudge
      if ((dist + c2_radius) < (c1_radius + 100)) {
	printf("x");
	fflush(stdout);
	result = g_list_delete_link(result, iter2);
      }

      iter2 = next;
    }
    printf(".");
    fflush(stdout);
  }
  printf("\n");

  gettimeofday(&tv, NULL);
  end_time_in_ms = tv.tv_sec * 1000 + tv.tv_usec / 1000;
  printf("overlap suppression done in %lld ms\n", end_time_in_ms - start_time_in_ms);

  return result;
}


// inspired by
// http://www.csee.usf.edu/~christen/tools/moments.c
static void compute_moments(double *data_array, int len,
			    double *mean,
			    double *variance,
			    double *skewness,
			    double *kurtosis) {
  *mean = 0.0;
  for (int i = 0; i < len; i++) {
    printf("%g\n", data_array[i]);
    *mean += data_array[i] / (double) len;
  }

  double raw_moments[4] = {*mean, 0, 0, 0};
  double central_moments[4] = {0, 0, 0, 0};

  for (int i = 0; i < len; i++) {
    for (int j = 1; j < 4; j++) {
      raw_moments[j] += (pow(data_array[i], j + 1.0) / len);
      central_moments[j] += (pow((data_array[i] - *mean), j + 1.0) / len);
    }
  }

  printf("\nr: %g\n%g\n%g\n%g\n", raw_moments[0], raw_moments[1], raw_moments[2], raw_moments[3] );
  printf("c: %g\n%g\n%g\n%g\n\n", central_moments[0], central_moments[1], central_moments[2], central_moments[3] );

  *variance = central_moments[1];
  *skewness = central_moments[2];
  *kurtosis = central_moments[3];
}

// called from GUI
GList *circlesFromImage(const int width, const int height,
			const int stride, const int bytesPerPixel,
			void *data, double minSharpness) {
  circles_state_t cs = staticState;
  cs.minSharpness = minSharpness;
  return circlesFromImage2(&cs, width,
			   height, stride, bytesPerPixel, data);
}



// 3 functions for diamond filter interface

extern "C" {
  int f_init_circles (int num_arg, char **args,
		      int bloblen, void *blob_data,
		      const char *filter_name,
		      void **filter_args) {
    circles_state_t *cr;

    // check parameters
    if (num_arg != 4) {
      return -1;
    }

    // init state
    cr = (circles_state_t *)g_malloc(sizeof(circles_state_t));

    cr->minRadius = g_ascii_strtod(args[0], NULL);
    cr->maxRadius = g_ascii_strtod(args[1], NULL);
    cr->maxEccentricity = g_ascii_strtod(args[2], NULL);
    cr->minSharpness = g_ascii_strtod(args[3], NULL);

    // we're good
    *filter_args = cr;
    return 0;
  }



  int f_eval_circles (lf_obj_handle_t ohandle, void *filter_args) {
    // circles
    GList *clist;
    circles_state_t *cr = (circles_state_t *) filter_args;
    int num_circles;
    int num_circles_in_result = 0;

    char buf[G_ASCII_DTOSTR_BUF_SIZE];

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
    clist = circlesFromImage2(cr, w, h, w * 4, 4, data);

    data = NULL;

    // add the list of circles to the cache and the object
    num_circles = g_list_length(clist);
    double *areas = (double *) g_malloc(sizeof(double) * num_circles);
    double *eccentricities = (double *) g_malloc(sizeof(double) * num_circles);
    double total_area = 0.0;
    if (num_circles > 0) {
      GList *l = clist;
      int i = 0;

      int size_of_entry = sizeof(float) * 5 + 1;
      data = (unsigned char *) g_malloc(size_of_entry * num_circles);

      // pack in and count
      while (l != NULL) {
	float *f = (float *) (data + i * size_of_entry);
	unsigned char *in_result = data + ((i + 1) * size_of_entry) - 1;
	circle_type *c = (circle_type *) l->data;

	f[0] = c->x;
	f[1] = c->y;
	f[2] = c->a;
	f[3] = c->b;
	f[4] = c->t;
	*in_result = c->in_result;

	if (c->in_result) {
	  total_area += areas[num_circles_in_result] = M_PI * c->a * c->b;
	  eccentricities[num_circles_in_result] = compute_eccentricity(c->a, c->b);
	  num_circles_in_result++;
	}

	i++;
	l = g_list_next(l);
      }

      lf_write_attr(ohandle, "circle-data", size_of_entry * num_circles, data);

      /*
      printf(" ***\n");
      for (i = 0; i < size_of_entry * num_circles; i++) {
	printf(" %.2x", data[i]);
      }
      printf("\n");
      fflush(stdout);
      exit(0);
      */
    }

    // compute aggregate stats
    double n = num_circles_in_result;
    g_ascii_dtostr (buf, sizeof (buf), n);
    lf_write_attr(ohandle, "circle-count", strlen(buf) + 1, (unsigned char *) buf);


    double area_fraction = total_area / (w * h);
    g_ascii_dtostr (buf, sizeof (buf), area_fraction);
    lf_write_attr(ohandle, "circle-area-fraction", strlen(buf) + 1, (unsigned char *) buf);

    double area_m1, area_cm2, area_cm3, area_cm4;
    compute_moments(areas, num_circles_in_result, &area_m1, &area_cm2, &area_cm3, &area_cm4);
    g_ascii_dtostr (buf, sizeof (buf), area_m1);
    lf_write_attr(ohandle, "circle-area-m1", strlen(buf) + 1, (unsigned char *) buf);
    g_ascii_dtostr (buf, sizeof (buf), area_cm2);
    lf_write_attr(ohandle, "circle-area-cm2", strlen(buf) + 1, (unsigned char *) buf);
    g_ascii_dtostr (buf, sizeof (buf), area_cm3);
    lf_write_attr(ohandle, "circle-area-cm3", strlen(buf) + 1, (unsigned char *) buf);
    g_ascii_dtostr (buf, sizeof (buf), area_cm4);
    lf_write_attr(ohandle, "circle-area-cm4", strlen(buf) + 1, (unsigned char *) buf);

    double eccentricity_m1, eccentricity_cm2, eccentricity_cm3, eccentricity_cm4;
    compute_moments(eccentricities, num_circles_in_result,
		    &eccentricity_m1, &eccentricity_cm2,
		    &eccentricity_cm3, &eccentricity_cm4);
    g_ascii_dtostr (buf, sizeof (buf), eccentricity_m1);
    lf_write_attr(ohandle, "circle-eccentricity-m1", strlen(buf) + 1, (unsigned char *) buf);
    g_ascii_dtostr (buf, sizeof (buf), eccentricity_cm2);
    lf_write_attr(ohandle, "circle-eccentricity-cm2", strlen(buf) + 1, (unsigned char *) buf);
    g_ascii_dtostr (buf, sizeof (buf), eccentricity_cm3);
    lf_write_attr(ohandle, "circle-eccentricity-cm3", strlen(buf) + 1, (unsigned char *) buf);
    g_ascii_dtostr (buf, sizeof (buf), eccentricity_cm4);
    lf_write_attr(ohandle, "circle-eccentricity-cm4", strlen(buf) + 1, (unsigned char *) buf);

    printf("area_fraction: %g\n", area_fraction);
    printf("area moments: %g %g %g %g\n",
	   area_m1, area_cm2, area_cm3, area_cm4);
    printf("ecc  moments: %g %g %g %g\n",
	   eccentricity_m1, eccentricity_cm2,
	   eccentricity_cm3, eccentricity_cm4);

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

    // return number of circles
    return num_circles_in_result;
  }



  int f_fini_circles (void *filter_args) {
    g_free((circles_state_t *) filter_args);

    return 0;
  }
}