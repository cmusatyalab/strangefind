/*
 *  StrangeFind, an anomaly detection system for the OpenDiamond Platform
 *
 *  Copyright (c) 2008 Carnegie Mellon University
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

#include <iostream>
#include <xqilla/xqilla-simple.hpp>
#include <xercesc/framework/MemBufInputSource.hpp>

#include "lib_filter.h"

extern "C" {
  int f_init_xquery (int num_arg, char **args,
		     int bloblen, void *blob_data,
		     const char *filter_name,
		     void **filter_args);
  int f_eval_xquery (lf_obj_handle_t ohandle, void *filter_args);
  int f_fini_xquery (void *filter_args);
}


struct ctx {
  XQilla xqilla;
  XQQuery *query;
  XQQuery *post_query;
};

int f_init_xquery (int num_arg, const char * const *args,
		   int bloblen, const void *blob_data,
		   const char *filter_name,
		   void **filter_args) {
  // check args
  if (num_arg != 0) {
    return -1;
  }

  // context object
  struct ctx *ctx = new struct ctx;
  *filter_args = ctx;

  // convert blob into char*
  char *query_str = (char *) malloc(bloblen + 1);
  strncpy((char *) query_str, (const char *) blob_data, bloblen);
  query_str[bloblen] = '\0';

  // parse the blob into XQuery
  ctx->query = ctx->xqilla.parse(X(query_str));

  // parse the normalizing query
  ctx->post_query = ctx->xqilla.parse(X("for $a in /attributes/attribute[@name and @value] return (xs:string($a/@name), xs:string($a/@value))"));

  // clean up
  free(query_str);
  return 0;
}



int f_eval_xquery (lf_obj_handle_t ohandle, void *filter_args) {
  XQQuery *query = ((struct ctx *) filter_args)->query;
  XQQuery *post_query = ((struct ctx *) filter_args)->post_query;

  // create context objects
  AutoDelete<DynamicContext> context(query->createDynamicContext());
  AutoDelete<DynamicContext> post_context(post_query->createDynamicContext());


  // slurp in the entire object
  size_t len;
  const void *data;
  lf_ref_attr(ohandle, "", &len, &data);

  // parse the document, set it as context item
  xercesc::MemBufInputSource input_source((const XMLByte *) data, len, X("diamond"));
  Node::Ptr doc = context->parseDocument(input_source);
  context->setContextItem(doc);
  context->setContextPosition(1);
  context->setContextSize(1);

  // execute user query
  Result result = query->execute(context);

  // convert into diamond attributes, by executing our "post_query"
  post_context->setContextItem(result->toSequence(context).first());
  post_context->setContextPosition(1);
  post_context->setContextSize(1);

  bool settingName = true;
  char *attributeName = NULL;
  try {
    Result post_result = post_query->execute(post_context);
    Item::Ptr item;
    while(item = post_result->next(post_context)) {
      char *str = strdup(UTF8(item->asString(post_context)));
      if (settingName) {
	attributeName = strdup(str);
      } else {
	//std::cout << "writing attribute '" << attributeName << "':'" << str << "'" << std::endl;
	lf_write_attr(ohandle, attributeName,
		      strlen(str) + 1, (unsigned char *) str);
	free(attributeName);
      }
      free(str);
      settingName = !settingName;
    }
  } catch(XQException &e) {
    std::cerr << "XQException: " << UTF8(e.getError()) << std::endl;
    return 0;
  }

  return 1;
}



int f_fini_xquery (void *filter_args) {
  struct ctx *ctx = (struct ctx *) filter_args;

  delete ctx->query;
  delete ctx->post_query;
  delete ctx;
  return 0;
}
