' Testing the register intrinsic variables!

' GR
call -1216          ' $fb40

' COLOR= 5
cpu.register.a = 5
call -1948          ' $f864

' PLOT 5,10
cpu.register.y = 5  ' X coordinate
cpu.register.a = 10 ' Y coordinate
call -2048          ' $f800

' C = SCRN(5,10)
cpu.register.y = 5  ' X coordinate
cpu.register.a = 10 ' Y coordinate
call -1935          ' $f871
c = cpu.register.a

' COLOR= C+1 (6!)
cpu.register.a = c+1
call -1948          ' $f864

' PLOT 6.11
cpu.register.y = 6  ' X coordinate
cpu.register.a = 11 ' Y coordinate
call -2048          ' $f800

end
