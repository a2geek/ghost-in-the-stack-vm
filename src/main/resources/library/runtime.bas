'
' Runtime support
'

sub runtime_out_of_bounds(name as string, linenum as integer)
    print name;" INDEX OUT OF BOUNDS AT LINE ";linenum
    end
end sub

' Find maximum value <= line number. We return index+1 for response to fit with
' ON...GOTO/GUSUB code.
function runtime_line_index(lineno as integer, numbers() as integer) as integer
    dim i as integer

    for i = 0 to ubound(numbers)
        if numbers(i) > lineno then
            return i
        end if
    next i

    ' Check if final entry was an exact match
    i = ubound(numbers)
    if numbers(i) = lineno then
        return i+1
    end if

    return -1
end function
