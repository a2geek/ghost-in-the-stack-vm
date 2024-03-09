option strict
option heap

uses "prodos"

function frompstring(p as address) as string
    dim i as integer, n as integer, s as address
    n = peek(p) AND 0x0F
    s = 0x280
    poke s, n
    for i = 1 to n
        p = p + 1
        s = s + 1
        poke s, peek(p)
    next i
    s = s + 1
    poke s,0
    return 0x280
end function

dim buffer as address = 0x9000
dim refnum as integer, n as integer
dim prefix as string
dim block(512) as byte, i as integer
dim entrylength as integer, filecount as integer

prefix = getPrefix()
print "CAT: ";prefix

refnum = open(prefix, buffer)
n = read(refnum, addrof(block(0)), 512)
entrylength = block(0x23)
filecount = peekw(addrof(block(0x25)))
while filecount > 0
    for i = 4 to 512-entrylength step entrylength
        if block(i) <> 0 then
            print frompstring(addrof(block(i)))
            filecount = filecount - 1
        end if
    next i
    if block(2) = 0 and block(3) = 0 then
        exit while
    end if
    n = read(refnum, addrof(block(0)), 512)
end while
close(refnum)
