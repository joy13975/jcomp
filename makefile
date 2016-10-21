APP=JComp

%.class: %.java
	javac -cp ./bin:${CLASSPATH} $^ -d ./bin

%.java: %.g
	antlr $^

all: $(APP).jar
	jar cvf $(APP).jar ./bin/*

$(APP).jar: src/Lex.class src/Syn.class src/IRTree.class src/TokenConv.class src/Memory.class src/Irt.class src/Cg.class src/Camle.class

disptree: src/disptree.c
	gcc $^ -o $@

.PHONY: clean

clean:
	rm -rf *.jar *.tokens bin/* src/Lex.java src/Syn.java disptree