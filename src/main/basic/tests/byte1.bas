' The premise here is that if bytes are not allocated correctly, they
' should intefere with one of the other values.

option strict

uses "text"

dim good as integer, bad as integer

sub check(label as string, actual as byte, expected as byte)
    print label;"( ";actual; " = ";expected;"): ";
    if actual = expected then
        print "Ok."
        good = good + 1
    else
        print "Fail."
        bad = bad + 1
    end if
end sub

dim b1 as byte
dim b2 as byte
dim b3 as byte

text:home
print "Setting up byte values..."
b1 = 0xfe
b2 = 0x99
b3 = 0x00

print "Checking byte values..."
check("b1", b1, 0xfe)
check("b2", b2, 0x99)
check("b3", b3, 0x00)

inverse
if bad = 0 then
    print "SUCCESS"
else
    print "FAILURE; GOOD=";good;", BAD=";bad
end if
normal
end
