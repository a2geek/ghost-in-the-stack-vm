option strict
option heap

uses "text"
uses "strings"

dim a$,b$,c$

' chr$/strreverse/str$
a$ = chr$(0xc5)
b$ = strings.strreverse("sdrawkcab")
c$ = str$(-12345)

print "Actuals:"
print "A$='";a$;"', expected:'E'"
print "B$='";b$;"', expected:'backwards'"
print "C$='";c$;"', expected:'-12345'"

inverse
if a$="E" and b$="backwards" and c$="-12345" then
    print "SUCCESS"
else
    print "FAIL"
end if
normal
end
