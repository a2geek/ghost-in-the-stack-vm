10 text : gr : home : print "Making Trees..."
20 for x0=0 to 39 step 10
30 for y0=0 to 39 step 10
40 for x=1 to 4
50 t=8-x*2+1
60 for y=8 to t step -1
70 color=(y <> t)*4 + (y = t)*13 : plot x0+x,y0+y :plot x0+8-x,y0+y
80 next y
90 next x
100 color= 7 : plot x0+4,y0+9
110 next y0
120 next x0
130 print "DONE!"
140 end
