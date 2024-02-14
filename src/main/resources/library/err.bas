module err
    dim number as integer
    dim message as string
    dim linenum as integer
    dim source as string

    sub clear()
        number = 0
        message = ""
        linenum = -1
        source = ""
    end sub
end module
