all: fil_anomaly.so fil_xquery.so

clean:
	$(RM) fil_anomaly.so fil_xquery.so

fil_anomaly.so: anomaly-filter.c logic-stack-machine.h logic-stack-machine.c
	gcc -std=gnu99 -fPIC ${LDFLAGS} ${CPPFLAGS} -I/opt/diamond-filter-kit/include -L/opt/diamond-filter-kit/lib `pkg-config opendiamond glib-2.0 --cflags` -O2 -g -m32 -Wall -shared  -o $@ anomaly-filter.c logic-stack-machine.c -Wl,--version-script=filters.map
	strip $@

fil_xquery.so: xquery-filter.cpp
	g++ -fPIC ${LDFLAGS} ${CPPFLAGS} -I/opt/diamond-filter-kit/include -L/opt/diamond-filter-kit/lib `pkg-config opendiamond --cflags` -O2 -g -m32 -Wall -shared  -o $@ -Wp,-D_FORTIFY_SOURCE=2 -fstack-protector --param=ssp-buffer-size=4 xquery-filter.cpp -lxqilla -lxerces-c -Wl,--version-script=filters.map
	strip $@

.PHONY: all clean
