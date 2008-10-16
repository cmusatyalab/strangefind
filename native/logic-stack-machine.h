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


#ifndef LOGIC_STACK_MACHINE_H
#define LOGIC_STACK_MACHINE_H

#include <stdbool.h>

typedef struct _logic_stack_machine logic_stack_machine_t;

logic_stack_machine_t *lsm_create(void);

void lsm_destroy(logic_stack_machine_t *lsmr);

void lsm_push(logic_stack_machine_t *lsmr, bool value);

/* pop, and, or, not return -1 in case of underflow.
 * When -1 is returned, the stack will be empty. */

// returns value at top of stack
int lsm_pop(logic_stack_machine_t *lsmr);

// pops 2 values and pushes logical AND
int lsm_and(logic_stack_machine_t *lsmr);

// pops 2 values and pushes logical OR
int lsm_or(logic_stack_machine_t *lsmr);

// pops 1 value and pushes logical NOT
int lsm_not(logic_stack_machine_t *lsmr);

#endif
