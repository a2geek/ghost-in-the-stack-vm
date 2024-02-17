module err
    dim number as integer
    dim message as string
    dim linenum as integer
    dim source as string
    dim context as string

    sub clear()
        number = 0
        message = ""
        linenum = -1
        source = ""
        context = ""
    end sub
end module
