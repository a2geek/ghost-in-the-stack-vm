uses "prodos"
uses "strings"

dim pfx as string

print "*** PREFIX ***"

if isPrefixActive() then
    print "HAS ACTIVE PREFIX"
else
    print "NO ACTIVE PREFIX"
end if

pfx = getprefix()

print "PREFIX: ";
if len(pfx) = 0 then
    print "(UNSET)"
else
    print pfx
end if

print "*** LAST DEVICE ***"
print "LAST USED DEVICE = ";lastDevice()

print "*** MACHID TESTS ***"
print "APPLE II.....";isAppleII()
print "APPLE II+....";isAppleIIplus()
print "APPLE //E....";isAppleIIe()
print "APPLE III....";isAppleIII()
print "APPLE //C....";isAppleIIc()
print "48K MEMORY...";has48K()
print "64K MEMORY...";has64K()
print "128K MEMORY..";has128K()
print "80 COLUMNS...";has80Cols()
print "CLOCK........";hasClock()

print "*** END ***"
