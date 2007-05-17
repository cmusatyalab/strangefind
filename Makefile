fil_circle.so: circles4.cpp circles4.h util.h util.c
	g++ -fPIC ${LDFLAGS} ${CPPFLAGS} -Wl,--hash-style=sysv -L/opt/diamond-filter-kit/lib `pkg-config opendiamond glib-2.0 --cflags` `lti-config nogtk --cxxflags` -O2 -g -m32 -Wall -shared  -o $@ -Wl,-Bstatic circles4.cpp util.c `lti-config nogtk --libs` `pkg-config glib-2.0 --libs` -Wl,-Bdynamic
