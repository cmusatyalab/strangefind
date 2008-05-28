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
  int f_fini_afilter (void *filter_args);
}


struct ctx {
  XQilla xqilla;
  XQQuery *query;
};

int f_init_xquery (int num_arg, char **args,
		   int bloblen, void *blob_data,
		   const char *filter_name,
		   void **filter_args) {
  // check args
  if (num_arg != 0) {
    return -1;
  }

  // factory object
  struct ctx *ctx = new struct ctx;

  // convert blob into char*
  char *query_str = (char *) malloc(bloblen + 1);
  strncpy((char *) query_str, (const char *) blob_data, bloblen);
  query_str[bloblen] = '\0';

  // parse the blob into XQuery
  std::cout << query_str << std::endl;
  XQQuery *query(ctx->xqilla.parse(X(query_str)));
  std::cout << query << std::endl;

  // save query
  ctx->query = query;
  *filter_args = ctx;

  // clean up
  free(query_str);
  return 0;
}



int f_eval_xquery (lf_obj_handle_t ohandle, void *filter_args) {
  XQQuery *query = ((struct ctx *) filter_args)->query;
  std::cout << "hi" << std::endl << query << std::endl;

  // create context object
  AutoDelete<DynamicContext> context(query->createDynamicContext());
  std::cout << context << std::endl;

  // slurp in the entire object
  size_t len;
  unsigned char *data;
  lf_next_block(ohandle, INT_MAX, &len, &data);

  std::cout << "len: " << len << std::endl;

  // parse the document, set it as context item
  xercesc::MemBufInputSource input_source(data, len, X("diamond"));
  Node::Ptr doc = context->parseDocument(input_source);
  std::cout << "doc: " << doc;
  if (doc->isNode()) {
    context->setContextItem(doc);
    context->setContextPosition(1);
    context->setContextSize(1);
  }

  // execute query
  Result result = query->execute(context);

  // convert into diamond attributes
  Item::Ptr item;
  while(item = result->next(context)) {
    std::cout << UTF8(item->asString(context)) << std::endl;
  }

  return 1;
}



int f_fini_afilter (void *filter_args) {
  struct ctx *ctx = (struct ctx *) filter_args;

  delete ctx->query;
  delete ctx;
  return 0;
}
