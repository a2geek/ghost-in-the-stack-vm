
uses "text"
uses "math"

' Generate a sequence of numbers based on MouseMaze.
' It was only moving in one direction; this helped confirm
' the random numbers were somewhat random.
' (yes, 136 never gets used, but the original program is
' setup that way)
text
home
for i = 1 to 500
    K = rnd(77)+141
    IF K = 141  THEN  print rnd(77)+141;", ";
    IF K = 175  THEN  print rnd(77)+141;", ";
    IF K = 136  THEN  print rnd(77)+141;", ";
    IF K = 149  THEN  print rnd(77)+141;", ";
    IF K = 196  THEN  print rnd(77)+141;", ";
    IF K = 197  THEN  print rnd(77)+141;", ";
    IF K = 193  THEN  print rnd(77)+141;", ";
    IF K = 195  THEN  print rnd(77)+141;", ";
    IF K = 209  THEN  print rnd(77)+141;", ";
    IF K = 218  THEN  print rnd(77)+141;", ";
    IF K = 215  THEN  print rnd(77)+141;", ";
    IF K = 216  THEN  print rnd(77)+141;", ";
next i
end
