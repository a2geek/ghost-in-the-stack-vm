'
' Runtime support
'
module runtime
    ' See https://6502disassembly.com/a2-rom/Applesoft.html
    ' and https://6502disassembly.com/a2-rom/AutoF8ROM.html
    const STROUT = 0xdb3a
    const GIVAYF = 0xe2f2
    const FOUT = 0xed34
    const MON_CROUT = 0xfd8e
    const MON_COUT = 0xfded
    const MON_PRNTAX = 0xf941
    const INPTR = 0x7f
    const BUFFER = 0x200
    const GETLN1 = 0xfd6f

    sub print_comma
        ' FIXME
        cpu.register.a = 0xa0   ' space
        call MON_COUT
    end sub

    sub print_integer(i as integer)
        cpu.register.y = i
        cpu.register.a = i >> 8
        call GIVAYF
        call FOUT
        call STROUT
    end sub


    sub print_string(s as string)
        ' String starts with max length byte
        cpu.register.a = s+1
        cpu.register.y = (s+1) >> 8
        call STROUT
    end sub

    sub print_boolean(b as boolean)
        if b then
            print "True";
        else
            print "False";
        end if
    end sub

    sub print_newline
        call MON_CROUT
    end sub

    sub print_address(a as address)
        cpu.register.a = a >> 8
        cpu.register.x = a
        call MON_PRNTAX
    end sub

    sub input_readline()
        call GETLN1
        pokew INPTR,BUFFER
    end sub

    function input_scaninteger as integer
        dim n as integer, i as integer, ch as integer
        dim p as address
        dim good as boolean

        while not good
            p = peekw(INPTR)
            do
                ch = peek(p)
                if ch = 0xac then       ' comma
                    p = p + 1           ' skip the comma
                    exit do
                elseif ch = 0x8d then   '  need CHR$!!
                    exit do
                end if
                ' test digits 0..9
                if ch >= 0xb0 and ch <= 0xb9 then
                    n = n*10 + ch-0xb0
                    good = true
                end if
                p = p + 1
                i = i + 1
            loop while i < 240
            pokew INPTR,p
            if not good then
                print "?";
                input_readline()
            end if
        end while
        return n
    end function

    sub input_scanstring(s as string)
        dim i as integer, ch as integer
        dim p as address
        dim maxlen as integer

        maxlen = peek(s)
        p = peekw(INPTR)
        while i < 240 and i < maxlen
            ch = peek(p)
            if ch = 0x8d then
                exit while
            end if
            poke s+1+i,ch
            p = p + 1
            i = i + 1
        end while
        poke s+1+i,0
        pokew INPTR,p
    end sub

    ' Find maximum value <= line number. We return index+1 for response to fit with
    ' ON...GOTO/GUSUB code.
    function line_index(lineno as integer, numbers() as integer) as integer
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

    ' abusing a subroutine for the error handler; note the END terminates the application
    sub defaultErrorHandler
        print "ERROR #";err.number;" - ";err.message
        end
    end sub

end module
