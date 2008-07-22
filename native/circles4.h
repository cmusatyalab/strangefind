/*
 *  FatFind
 *  A Diamond application for adipocyte image exploration
 *  Version 1
 *
 *  Copyright (c) 2006 Carnegie Mellon University
 *  All Rights Reserved.
 *
 *  This software is distributed under the terms of the Eclipse Public
 *  License, Version 1.0 which can be found in the file named LICENSE.
 *  ANY USE, REPRODUCTION OR DISTRIBUTION OF THIS SOFTWARE CONSTITUTES
 *  RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT
 */

#ifndef CIRCLES4_H
#define CIRCLES4_H

#include <glib.h>

typedef struct {
  float x;
  float y;
  float a;
  float b;
  float t;
  gboolean in_result;
} circle_type;

#ifdef __cplusplus
extern "C" {
#endif
  GList *circlesFromImage(const int width, const int height, const int stride,
			  const int bytesPerPixel,
			  void *data, double minSharpness);
#ifdef __cplusplus
}
#endif

#endif
