option strict
option heap

dim addr as address
dim n as integer

while True
    print "ADDR=";addr;", N=";n;", FREE=";memory.memfree()
    addr = memory.heapalloc(400)
    n = n + 1
end while
