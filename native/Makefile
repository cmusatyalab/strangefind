all: fil_circle.so fil_anomaly.so fil_xquery.so

clean:
	$(RM) fil_circle.so fil_anomaly.so fil_xquery.so

fil_circle.so: circles4.cpp circles4.h util.h util.c
	g++ -fPIC ${LDFLAGS} ${CPPFLAGS} -I/opt/diamond-filter-kit/include -L/opt/diamond-filter-kit/lib `pkg-config opendiamond glib-2.0 --cflags` `lti-config nogtk --cxxflags` -O2 -g -m32 -Wall -shared  -o $@ circles4.cpp util.c `lti-config nogtk --libs` `pkg-config glib-2.0 --libs` -Wl,--version-script=filters.map
	strip $@

fil_anomaly.so: anomaly-filter.c
	gcc -fPIC ${LDFLAGS} ${CPPFLAGS} -I/opt/diamond-filter-kit/include -L/opt/diamond-filter-kit/lib `pkg-config opendiamond glib-2.0 --cflags` -O2 -g -m32 -Wall -shared  -o $@ anomaly-filter.c -Wl,--version-script=filters.map
	strip $@

fil_xquery.so: xquery-filter.cpp
	g++ -fPIC ${LDFLAGS} ${CPPFLAGS} -I/opt/diamond-filter-kit/include -L/opt/diamond-filter-kit/lib `pkg-config opendiamond --cflags` -O2 -g -m32 -Wall -shared  -o $@ -Wp,-D_FORTIFY_SOURCE=2 -fstack-protector --param=ssp-buffer-size=4 xquery-filter.cpp -lxqilla -lxerces-c -Wl,--version-script=filters.map
	strip $@

.PHONY: all clean
