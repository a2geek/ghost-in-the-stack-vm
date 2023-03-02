JAVA=/opt/homebrew/Cellar/openjdk@11/11.0.18/libexec/openjdk.jdk/Contents/Home/bin/java
AC=/Users/rob/Documents/Source/AppleCommander/app/cli-ac/build/libs/AppleCommander-ac-1.8.0-SNAPSHOT.jar

all: interp.bin

interp.bin: interp.asm
	cl65 -l interp.lst -v -vm -C interp.config interp.asm -o interp-base.bin
	cat interp-base.bin ../../../output.bin > interp.bin
	cp template.po interp.po
	$(JAVA) -jar $(AC) -p interp.po interp BIN 0x2000 < interp.bin

clean:
	rm -f *.bin *.lst *.o interp.po