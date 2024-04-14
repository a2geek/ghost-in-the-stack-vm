option strict
option heap

uses "text"
uses "strings"

' Stupid way to create a border... but it creates a pile of strings
function MakeBorder(ch$ as string, n as integer) as string
    dim a$ as string
    repeat
        a$ = a$+ch$
    until len(a$) >= n
    return a$
end function

dim memtotal as integer
dim a$,b$,c$

memtotal = memory.memfree()
print "Starting with ";memtotal;" bytes free."

a$ = MakeBorder("<>",10)
print "A$=";a$
print "LEN(A$)=";len(a$)

b$ = MakeBorder("=",20)
print "B$=";b$
print "LEN(B$)=";len(b$)

c$ = MakeBorder("<::>",30)
print "C$=";c$
print "LEN(C$)=";len(c$)

' We need to clean up the last string as well
memory.heapfree(a$)
memory.heapfree(b$)
memory.heapfree(c$)
print "Ending with ";memory.memfree();" bytes free."

inverse
if memtotal = memory.memfree() then
    print "SUCCESS"
    normal
else
    print "FAIL"
    normal
    memory.MemReport()
end if
end
