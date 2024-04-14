option strict

uses "text"

dim n as integer, i as integer

sub check(actual as integer, expected as integer)
    i = i + 1
    print "STEP #";i;": ";actual;" - ";
    if actual = expected then
        print "MATCH"
    else
        inverse : print "FAIL" : normal
    end if
end sub

n = 10
check(n, 10)
n ^= 4
check(n, 10000)
n += 5
check(n, 10005)
n -= 2
check(n, 10003)
n /= 3
check(n, 3334)
n *= 6
check(n, 20004)
n >>= 3
check(n, 2500)
n <<= 2
check(n, 10000)

print "** END **"
end
