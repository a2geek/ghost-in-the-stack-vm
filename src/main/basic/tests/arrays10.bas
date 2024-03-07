option strict

uses "text"

dim data(5) as byte
dim good as integer, bad as integer

sub check(i as integer, expected as integer)
    print "Index ";i;": ";data(i);" = ";cbyte(expected);": ";
    if (data(i) = cbyte(expected)) then
        print "Ok."
        good = good + 1
    else
        print "Incorrect."
        bad = bad + 1
    end if
end sub

text:home
print "Setting BYTE values..."
data(0) = 0xef
data(1) = 0xee
data(2) = 0xed
data(3) = 0xec
data(4) = 0xeb
data(5) = 0xea

print "Checking BYTE values..."
check(0, 0xef)
check(1, 0xee)
check(2, 0xed)
check(3, 0xec)
check(4, 0xeb)
check(5, 0xea)

inverse
if bad = 0 then
    print "SUCCESS"
else
    print "FAILURE; GOOD=";good;", BAD=";bad
end if
normal
end
