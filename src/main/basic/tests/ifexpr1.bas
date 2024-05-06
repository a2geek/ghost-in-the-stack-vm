option strict

uses "text"

dim a as integer, b as boolean, c as string
dim r as integer

text : home

r = 6

a = 10 if r < 5 else 20
b = True if r > 5 else False
c = "Exactly 5" if r = 5 else "Other than 5"

print "A=";a;" (expecting 20)"
print "B=";b;" (expecting True)"
print "C='";c;"' (expecting 'Other than 5')"

inverse
if a = 20 AND b = True and c = "Other than 5" then
    print "SUCCESS"
else
    print "FAILURE"
end if
normal
end
