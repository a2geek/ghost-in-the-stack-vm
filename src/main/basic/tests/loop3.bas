dim ch as integer

ch = 0xB1
print "CH=";ch;"  0=";ASC("0");"  9=";ASC("9")
if ch >= ASC("0") and ch <= ASC("9") then
    print ">= 0 AND <= 9"
end if
if ch >= ASC("0") then
    print ">= 0"
end if
if ch <= ASC("9") then
    print "<= 9"
end if
end
