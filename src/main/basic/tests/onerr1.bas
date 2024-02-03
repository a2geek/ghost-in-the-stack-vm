on error goto handleError

print "BEFORE ERROR"
raise error 1, "DID SOMETHING STUPID"

print "NEVER GETS HERE"
end

handleError:
    print "OOPS - ERROR CODE ";err.number
    print "       ERROR MSG  ";err.message
