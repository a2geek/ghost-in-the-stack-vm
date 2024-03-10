option strict

uses "text"

dim a$,b$,c$,d$,expected$

' string concatenation
expected$ = "Hello, World!"
a$ = "Hello"
b$ = ", "
c$ = "World!"
print a$;b$;c$

d$ = a$ + b$ + c$
print d$

inverse
if d$ = expected$ and a$+b$+c$ = expected$ then
    print "SUCCESS"
else
    print "FAIL"
end if
normal
end
