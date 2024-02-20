option strict
option heap

' this way we don't have to fiddle with text width
if prodos.has80Cols() then
    misc.prnum(3)
end if

dim addr(10) as address, size(10) as integer
dim n as integer = 10000   ' iterations for "stress" test
dim i as integer           ' array index
dim total as integer
dim startmem as integer

print "Running for ";n;" iterations. Set to max speed for this one!"

startmem = memory.memfree()
while n <> 0
    if addr(i) <> 0 then
        print "FREEING ";size(i);" BYTES AT ";addr(i)
        memory.heapfree(addr(i))
        total = total - size(i)
    end if
    size(i) = math.rnd(400)
    addr(i) = memory.heapalloc(size(i))
    total = total + size(i)
    print "ADDR=";addr(i);", SIZE=";size(i);", N=";n;", FREE=";memory.memfree();", TOTAL=";total
    i = i + 1
    if i > ubound(addr) then
        i = 0
    end if
    n = n - 1
end while

for i = 0 to ubound(addr)
    memory.heapfree(addr(i))
next i

if startmem <> memory.memfree() then
    print "** ERROR **"
    print "EXPECTING ";startmem;" BYTES FREE BUT HAVE ";memory.memfree()
else
    print "** GOOD **"
    print "STARTING AND ENDING MEMORY SIZES MATCH AT ";startmem;" BYTES."
end if

