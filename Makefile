all: fil_circle.so fil_anomaly.so

clean:
	$(RM) fil_circle.so fil_anomaly.so

fil_circle.so: circles4.cpp circles4.h util.h util.c
	g++ -fPIC ${LDFLAGS} ${CPPFLAGS} -L/opt/diamond-filter-kit/lib `pkg-config opendiamond glib-2.0 --cflags` `lti-config nogtk --cxxflags` -O2 -g -m32 -Wall -shared  -o $@ -Wl,-Bstatic circles4.cpp util.c `lti-config nogtk --libs` `pkg-config glib-2.0 --libs` -Wl,-Bdynamic
	strip $@

fil_anomaly.so: anomaly-filter.c
	gcc -fPIC ${LDFLAGS} ${CPPFLAGS} -L/opt/diamond-filter-kit/lib `pkg-config opendiamond glib-2.0 --cflags` -O2 -g -m32 -Wall -shared  -o $@ -Wl,-Bstatic anomaly-filter.c -Wl,-Bdynamic
	strip $@

.PHONY: all clean
