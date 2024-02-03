' Perform comparison within IF statements

print "COMPARISONS"
print
print "<  <= >  >= =  <> A B"

a = 1
b = 2
gosub compare

a = 2
gosub compare

a = 3
gosub compare

end

compare:
    if a < b then
        print "T  ";
    else
        print "F  ";
    end if

    if a <= b then
        print "T  ";
    else
        print "F  ";
    end if

    if a > b then
        print "T  ";
    else
        print "F  ";
    end if

    if a >= b then
        print "T  ";
    else
        print "F  ";
    end if

    if a = b then
        print "T  ";
    else
        print "F  ";
    end if

    if a <> b then
        print "T  ";
    else
        print "F  ";
    end if

    print A,B
    return