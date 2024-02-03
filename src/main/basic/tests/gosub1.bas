' Test of GOSUB/RETURN

main:
    print "BEFORE"
    gosub sub1
    print "AFTER"
    end

sub1:
    print "SUB"
    return
