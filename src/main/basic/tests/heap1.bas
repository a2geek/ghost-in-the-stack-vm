option strict
option heap

dim addr(10) as address, size(10) as integer
dim n as integer    ' iteration
dim i as integer    ' array index
dim total as integer

while True
    if addr(i) <> 0 then
        print "FREEING ";size(i);" BYTES AT ";addr(i)
        memory.heapfree(addr(i))
        total = total - size(i)
    end if
    size(i) = math.rnd(400)
    addr(i) = memory.heapalloc(size(i))
    total = total + size(i)
    n = n + 1
    print "ADDR=";addr(i);", SIZE=";size(i);", N=";n;", FREE=";memory.memfree();", TOTAL=";total
    i = i + 1
    if i > ubound(addr) then
        i = 0
    end if
end while
