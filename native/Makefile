all: fil_anomaly fil_xquery

clean:
	$(RM) fil_anomaly fil_xquery

fil_anomaly: anomaly-filter.c logic-stack-machine.h logic-stack-machine.c
	gcc -std=gnu99 ${LDFLAGS} ${CPPFLAGS} -O2 -g -Wall -o $@ anomaly-filter.c logic-stack-machine.c `pkg-config opendiamond glib-2.0 --cflags --libs` -lm
	strip $@

fil_xquery: xquery-filter.cpp
	g++ ${LDFLAGS} ${CPPFLAGS} -O2 -g -Wall -o $@ -Wp,-D_FORTIFY_SOURCE=2 -fstack-protector --param=ssp-buffer-size=4 xquery-filter.cpp -lxqilla -lxerces-c `pkg-config opendiamond --cflags --libs` -pthread
	strip $@

.PHONY: all clean
