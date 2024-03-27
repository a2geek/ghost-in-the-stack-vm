option strict
option heap

uses "text"
uses "strings"

' This test takes the simple case for deallocating strings.
' Note that HEAPFREE has code to ensure address is in range.

dim a$
dim n as integer

n = memory.memfree()
print "Starting with ";n;" bytes free."

' The quick brown fox jumps over the lazy dog.
a$ = "The "
a$ = a$ + "quick brown "
a$ = a$ + "fox jumps " + "over the" + " lazy dog."

print "Result: "
print a$

' We need to clean up the last string as well
memory.heapfree(a$)
print "Ending with ";memory.memfree();" bytes free."

inverse
if n = memory.memfree() then
    print "SUCCESS"
else
    print "FAIL"
end if
normal
end
