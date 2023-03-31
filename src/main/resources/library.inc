
sub color(c)
    cpu.register.a = c
    call 0xf864
end sub

sub home
    call 0xfc58
end sub

sub print_comma
    ' FIXME
    cpu.register.a = 0xa0
    call 0xfded
end sub

sub print_integer(i)
    cpu.register.y = i
    cpu.register.a = i mod 256
    call 0xe2f2
    call 0xed34
    call 0xdb3a
end sub

sub print_string(s)
    cpu.register.a = s
    cpu.register.y = s mod 256
    call 0xdb3a
end sub

sub print_newline
    call 0xfd8e
end sub

sub lores_gr
    call 0xfb40
end sub

sub lores_plot(x,y)
    cpu.register.y = x
    cpu.register.a = y
    call 0xf800
end sub

sub lores_hlin(x0,x1,y)
    cpu.register.y = x0
    poke 0x2c, x1
    cpu.register.a = y
    call 0xf819
end sub

sub lores_vlin(y0,y1,x)
    cpu.register.a = y0
    poke 0x2d, y1
    cpu.register.y = x
    call 0xf828
end sub

sub text
    call 0xfb2f
end sub

sub htab(h)
    poke 0x24, h-1
end sub

sub vtab(v)
    cpu.register.a = v
    call 0xfb5b
end sub

function lores_scrn(x,y)
    cpu.register.y = x
    cpu.register.a = y
    call 0xf871
    return cpu.register.a
end function
