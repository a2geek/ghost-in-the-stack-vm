option strict
option heap lomem=0x8000

uses "text"

print "OPTION HEAP WAS DECLARED"
#if defined(option.heap)
print "DETECTED!"
inverse : print "SUCCESS" : normal
#else
print "THE OPTION WAS NOT DETECTED"
inverse : print "ERROR" : normal
#endif

end
