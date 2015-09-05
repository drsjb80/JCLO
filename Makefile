BASE	= $*
TARGET	= $@
DEPENDS	= $<
NEWER	= $?

DIR	= edu/mscd/cs/jclo

VERSION = $(shell cat VERSION)

SOURCES	= $(shell ls $(DIR)/*.java)
CLASSES = $(SOURCES:.java=.class)

PACKAGE	= edu.mscd.cs.jclo

.SUFFIXES: .java .class .jar

.java.class : 
	javac $(DEPENDS)

JCLO-$(VERSION).jar : $(CLASSES)
	jar -cMf $(TARGET) $(CLASSES)

index.html : index Makefile
	sed -e "s/VERSION/$(VERSION)/" < $(DEPENDS) > $(TARGET)

docs : JCLO.java
	javadoc -quiet -d $(TARGET) $(DEPENDS)

$(DIR)/Version.java : VERSION
	echo "package edu.mscd.cs.jclo; public class Version { public static String getVersion() { return (\"$(VERSION)\"); }}" > $(TARGET)

clean :
	rm -f JCLO-$(VERSION).jar $(CLASSES) index.html
	rm -rf docs

test : JCLO.class JCLOTests.class Main.class Example.class
	java -classpath $(ROOT) $(PACKAGE).JCLOTests | diff - GOOD.1
	java -classpath $(ROOT):. Main -a 5 -b | diff - GOOD.2
	java -classpath $(ROOT):. Example --a=5 --b --c=3.141592 --d=this \
	    that theother | diff - GOOD.3
	- java -classpath $(ROOT):. Example --e=5 2>&1 | \
	    sed -e 's/java:[0-9][0-9]*/java:#/' | diff - GOOD.4
	- java -classpath $(ROOT):. Example --c=true 2>&1 | \
	    sed -e 's/java:[0-9][0-9]*/java:#/' | diff - GOOD.5
	java -classpath $(ROOT) $(PACKAGE).JCLO --version

tag:
	git tag -a v$(VERSION) -m "my version $(VERSION)"
	# cvs tag -R jclo`echo $(VERSION) | sed -e 's/\.//g'`
