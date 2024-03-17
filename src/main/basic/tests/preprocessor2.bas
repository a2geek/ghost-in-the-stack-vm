#define testvalue 1
option strict

uses "text"

print "CODE CHANGES BASED ON DEFINED VALUE OF TESTVALUE"
#if testvalue = 1
print "TESTVALUE WAS ONE"
#elseif testvalue = 2
print "TESTVALUE WAS TWO"
#else
print "TESTVALUE SOME OTHER VALUE THAN 1 OR 2"
#endif
print "AT END"

end
