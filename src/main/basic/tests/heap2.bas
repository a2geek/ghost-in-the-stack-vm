option strict
option heap lomem=0x8000

dim temp as address, addr1 as address, addr2 as address, addr3 as address

' this way we don't have to fiddle with text width
if prodos.has80Cols() then
    misc.prnum(3)
end if

print "HEAP ALLOCATION MEMORY TESTS"
print "============================"
print
print "These tests run through different scenarios and make an"
print "attempt to confuse the heap allocation and free routines."
print

' Check consolidation and allocation at front
addr1 = memory.heapalloc(100)
addr2 = memory.heapalloc(100)
memory.heapfree(addr1)
memory.heapfree(addr2)
addr3 = memory.heapalloc(200)
if addr3 <> 0x8006 then
    print "Test 1: Expecting $8006 but got ";addr3;"."
else
    print "Test 1: Passed."
end if

' Check middle with split
memory.heapinit()
temp = memory.heapalloc(100)
addr1 = memory.heapalloc(200)
temp = memory.heapalloc(100)
memory.heapfree(addr1)
addr2 = memory.heapalloc(50)
addr3 = memory.heapalloc(50)
' $8000+106(chunk1)+6 and $8000+106(chunk1)+56(chunk2)+6
if addr2 <> 0x8070 OR addr3 <> 0x80A8 then
    print "Test 2: Expecting $8070/$80A8 but got ";addr2;" and ";addr3;"."
else
    print "Test 2: Passed."
end if

' Now check that the recombination worked
' memory.heapinit() continuing from test 2, not initializing
memory.heapfree(addr2)
memory.heapfree(addr3)
addr1 = memory.heapalloc(200)
if addr1 <> 0x8070 then
    print "Test 3: Expecting $8070 but got ";addr1;"."
else
    print "Test 3: Passed."
end if

' Check end
memory.heapinit()
temp = memory.heapalloc(100)
addr1 = memory.heapalloc(100)
memory.heapfree(addr1)
addr2 = memory.heapalloc(100)
if addr2 <> addr1 AND addr2 <> 0x806C then
    print "Test 4: Expecting $806C but got ";addr2;"."
else
    print "Test 4: Passed."
end if

end
