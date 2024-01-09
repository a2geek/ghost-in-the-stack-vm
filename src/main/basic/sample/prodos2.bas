uses "prodos"

sub prhex2(n as integer)
    cpu.register.a = n
    call 0xfdda
end sub

sub prhex4(n as integer)
    cpu.register.a = n / 256
    call 0xfdda
    cpu.register.a = n
    call 0xfdda
end sub

dim devnum as integer
dim block as integer = 2
dim buffer as address = 0x8000
dim i as integer, j as integer
dim ch as integer

devnum = lastDevice()

print "Device = $";
prhex2(devnum)
print
print "Block =  $";
prhex2(block)
print
print "Buffer = $";
prhex4(buffer)
print

readBlock(devnum, block, buffer)

for i = buffer to buffer+0x200-1 step 8
    prhex4(i)
    print "- ";
    for j = 0 to 7
        ch = peek(i+j)
        prhex2(ch)
        print " ";
        ' cheap stunt to put text out there
        poke peekw(0x28)+32+j,ch
    next j
    print
next i
