APP=JComp

src/%.java: src/%.g
	antlr $^

%.class: %.java
	javac -cp ./bin:${CLASSPATH} $^ -d ./bin

$(APP): src/Lex.class src/Syn.class src/IRTree.class src/TokenConv.class src/Memory.class src/Irt.class src/Cg.class src/Camle.class

all: $(APP)
	jar cvfm $(APP).jar manifest.txt -C bin/ bin/*.class

run: all
	java -jar $(APP).jar

disptree: src/disptree.c
	gcc $^ -o $@

.PHONY: clean

clean:
	rm -rf *.jar *.tokens bin/* src/Lex.java src/Syn.java disptree