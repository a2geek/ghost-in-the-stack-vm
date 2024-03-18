option strict
option heap

uses "text"
uses "strings"

dim a$,b$,c$,d$,expected$

' left$/right$/mid$
expected$ = "Hello, World!"

' get values
a$ = left$(expected$,5)
b$ = right$(expected$,6)
c$ = mid$(expected$,8)
d$ = mid$(expected$,4,6)

print "Actuals:"
print "A$='";a$;"', expected:'Hello'"
print "B$='";b$;"', expected:'World!'"
print "C$='";c$;"', expected:'World!'"
print "D$='";d$;"', expected:'lo, Wo'"

inverse
if a$="Hello" and b$="World!" and c$="World!" and d$="lo, Wo" then
    print "SUCCESS"
else
    print "FAIL"
end if
normal
end
