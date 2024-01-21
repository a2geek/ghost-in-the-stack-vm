module hires
    ' ZP locations - trying to pick unique names to not collide with preferred Basic names
    const SHAPE_ADDR = 0x1a     ' address of individual shape
    const HGR_COLOR = 0xe4
    const SHAPE_SCALE = 0xe7
    const SHAPE_TABLE = 0xe8    ' address of shape table
    const SHAPE_ROT = 0xf9

    ' HGR/Shape routines - also trying to pick unique names
    const HGR_INIT = 0xf3e2
    const HGR_HPOSN = 0xf411
    const HGR_HPLOT = 0xf457
    const HGR_HLIN = 0xf53a
    const SHAPE_DRAW = 0xf605
    const SHAPE_XDRAW = 0xf661
    const HGR_SETHCOL = 0xf6f0

    export inline sub hgr()
        call HGR_INIT
    end sub

    export sub hcolor(color as integer)
        cpu.register.x = color mod 8
        call HGR_SETHCOL
    end sub

    export sub hplotAt(x as integer, y as integer)
        cpu.register.y = x / 256
        cpu.register.x = x
        cpu.register.a = y
        call HGR_HPLOT
    end sub

    export sub hplotTo(x as integer, y as integer)
        cpu.register.x = x / 256
        cpu.register.a = x
        cpu.register.y = y
        call HGR_HLIN
    end sub

    export sub hplot(x0 as integer, y0 as integer, x1 as integer, y1 as integer)
        hplotAt(x0,y0)
        hplotTo(x1,y1)
    end sub

    export inline sub shapeTable(ptr as address)
        pokew SHAPE_TABLE, ptr
    end sub

    export inline sub scale(n as integer)
        poke SHAPE_SCALE, n
    end sub

    export inline sub rot(n as integer)
        poke SHAPE_ROT, n
    end sub

    ' extracting common code
    sub setupShape(shapenum as integer, x as integer, y as integer)
        dim addr as address = peekw(SHAPE_TABLE)
        if shapenum > peek(addr) then
            print "SHAPE#";shapenum
            print "TOTAL#";peek(addr)
            print "ADDR=";addr
            raise error 53, "ILLEGAL SHAPE NUMBER"
        end if
        pokew SHAPE_ADDR, addr + peekw(addr + shapenum * 2)
        cpu.register.y = x / 256
        cpu.register.x = x
        cpu.register.a = y
        call HGR_HPOSN
        cpu.register.a = peek(SHAPE_ROT)
    end sub

    export sub draw(shapenum as integer, x as integer, y as integer)
        setupShape(shapenum, x, y)
        call SHAPE_DRAW
    end sub

    export sub xdraw(shapenum as integer, x as integer, y as integer)
        setupShape(shapenum, x, y)
        call SHAPE_XDRAW
    end sub

end module
