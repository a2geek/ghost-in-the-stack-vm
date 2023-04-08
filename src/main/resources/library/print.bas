'
' Print statement routines
'

' See https://6502disassembly.com/a2-rom/Applesoft.html
' and https://6502disassembly.com/a2-rom/AutoF8ROM.html
const STROUT = 0xdb3a
const GIVAYF = 0xe2f2
const FOUT = 0xed34
const MON_CROUT = 0xfd8e
const MON_COUT = 0xfded

sub print_comma
    ' FIXME
    cpu.register.a = asc(" ")
    call MON_COUT
end sub

sub print_integer(i as integer)
    cpu.register.y = i
    cpu.register.a = i >> 8
    call GIVAYF
    call FOUT
    call STROUT
end sub

sub print_boolean(b as boolean)
    if b then
        print "True"
    else
        print "False"
    end if
end sub

sub print_string(s as string)
    cpu.register.a = s
    cpu.register.y = s >> 8
    call STROUT
end sub

sub print_newline
    call MON_CROUT
end sub
