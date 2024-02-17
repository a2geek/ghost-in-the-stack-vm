on error goto handleError

dim n as integer = 1

domore:
    print "N = ";n
    on n goto withnumber, withmessage, withcontext

    print "ALL DONE"
    end

withnumber:
    print "ERROR WITH JUST NUMBER"
    raise error 1
    print "NEVER GETS HERE (1)"
    end

withmessage:
    print "ERROR WITH NUMBER AND MESSAGE"
    raise error 2, "DID SOMETHING STUPID"
    print "NEVER GETS HERE (2)"
    end

withcontext:
    print "ERROR WITH NUMBER, MESSAGE, AND CONTEXT"
    raise error 3, "DID ANOTHER THING STUPID", "THIS IS THE CONTEXT"
    print "NEVER GETS HERE (3)"
    end

handleError:
    print "OOPS!"
    print "  ERROR CODE......";err.number
    print "  ERROR MSG.......";err.message
    print "  ERROR LINE#.....";err.linenum
    print "  ERROR SOURCE....";err.source
    print "  ERROR CONTEXT...";err.context
    n = n + 1
    goto domore
