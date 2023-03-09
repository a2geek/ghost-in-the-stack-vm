CL65 = cl65
CL65_ARGS = -v -vm -t apple2 -C apple2-asm.cfg -u __EXEHDR__ apple2.lib
OUTPUT = ../resources

all: interp.as interp-debug.as

interp.as: interp.asm
	cl65 -l interp.lst  $(CL65_ARGS) interp.asm -o $(OUTPUT)/interp-base.as

interp-debug.as: interp.asm
	cl65 --asm-define DEBUG -l interp-debug.lst  $(CL65_ARGS) interp.asm -o $(OUTPUT)/interp-debug-base.as

clean:
	rm -f *.as *.bin *.lst *.o interp.po
