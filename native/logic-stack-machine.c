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


#include "logic-stack-machine.h"

#include <glib.h>

struct _logic_stack_machine {
  GSList *stack;
};

logic_stack_machine_t *lsm_create(void) {
  return g_slice_new0(struct _logic_stack_machine);
}

void lsm_destroy(logic_stack_machine_t *lsmr) {
  g_slist_free(lsmr->stack);
  g_slice_free(struct _logic_stack_machine, lsmr);
}

void lsm_push(logic_stack_machine_t *lsmr, bool value) {
  lsmr->stack = g_slist_prepend(lsmr->stack, (gpointer) value);
}

int lsm_pop(logic_stack_machine_t *lsmr) {
  if (lsmr->stack == NULL) {
    return -1;
  }

  bool val = (bool) lsmr->stack->data;
  lsmr->stack = g_slist_delete_link(lsmr->stack, lsmr->stack);

  return val;
}

int lsm_and(logic_stack_machine_t *lsmr) {
  int val1 = lsm_pop(lsmr);
  int val2 = lsm_pop(lsmr);

  if (val1 == -1 || val2 == -1) {
    return -1;
  }

  bool val = val1 && val2;
  lsm_push(lsmr, val);

  return val;
}

int lsm_or(logic_stack_machine_t *lsmr) {
  int val1 = lsm_pop(lsmr);
  int val2 = lsm_pop(lsmr);

  if (val1 == -1 || val2 == -1) {
    return -1;
  }

  bool val = val1 || val2;
  lsm_push(lsmr, val);

  return val;
}

int lsm_not(logic_stack_machine_t *lsmr) {
  int val1 = lsm_pop(lsmr);

  if (val1 == -1) {
    return -1;
  }

  bool val = !val1;
  lsm_push(lsmr, val);

  return val;
}
