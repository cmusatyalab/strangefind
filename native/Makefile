all: fil_anomaly fil_xquery

clean:
	$(RM) fil_anomaly fil_xquery

fil_anomaly: anomaly-filter.c logic-stack-machine.h logic-stack-machine.c
	gcc -std=gnu99 ${LDFLAGS} ${CPPFLAGS} `pkg-config opendiamond glib-2.0 --cflags --libs` -lm -O2 -g -m32 -Wall -o $@ anomaly-filter.c logic-stack-machine.c
	strip $@

fil_xquery: xquery-filter.cpp
	g++ ${LDFLAGS} ${CPPFLAGS} -I/opt/diamond-filter-kit/include -L/opt/diamond-filter-kit/lib `pkg-config opendiamond --cflags --libs` -O2 -g -m32 -Wall -o $@ -Wp,-D_FORTIFY_SOURCE=2 -fstack-protector --param=ssp-buffer-size=4 xquery-filter.cpp -lxqilla -lxerces-c -pthread
	strip $@

.PHONY: all clean
