    uses "prodos"

    print "BEFORE CREATE FILE"
    createFile("TEST1", 0x04, 0)
    print "AFTER CREATE FILE"

    on error goto handleError
    unlock("TEST1B")
    destroy("TEST1B")

domore:
    rename("TEST1", "TEST1B")
    lock("TEST1B")
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
