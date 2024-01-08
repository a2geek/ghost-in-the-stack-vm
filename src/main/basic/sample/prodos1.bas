uses "prodos"
uses "strings"

dim pfx as string

pfx = getprefix()

print "PREFIX: ";
if len(pfx) = 0 then
    print "(UNSET)"
else
    print pfx
end if
