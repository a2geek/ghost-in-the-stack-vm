    uses "prodos"
    uses "text"

    print "BEFORE CREATE FILE"
    createFile("TEST1", 0x04, 0)
    print "AFTER CREATE FILE"

    on error goto handleError
    print "BEFORE UNLOCK"
    unlock("TEST1B")
    print "AFTER UNLOCK, BEFORE DESTROY"
    destroy("TEST1B")
    print "AFTER DESTROY"

domore:
    print "BEFORE RENAME"
    rename("TEST1", "TEST1B")
    print "AFTER RENAME; BEFORE LOCK"
    lock("TEST1B")
    print "AFTER LOCK"
    inverse
    print "SUCCESS"
    normal
    end

handleError:
    if err.number = 0x46 then
        print "FILE DOES NOT EXIST"
        on error disable
        goto domore
    end if
    print "PRODOS ERROR!"
    print "NUMBER:  ";err.number
    print "MESSAGE: ";err.message
    end
