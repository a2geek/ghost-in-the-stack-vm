option strict

sub testing(a as integer, b as integer = -1)
    print "TESTING: A=";a;", B=";b
end sub

print "TESTING DEFAULT PARAMETER VALUE (-1)"
testing(1)
testing(2,3)
testing(4)
testing(5,6)
testing(7)
print "END"

end
