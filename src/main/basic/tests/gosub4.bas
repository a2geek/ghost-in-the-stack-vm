dim n() as integer = { 10, 20, 30 }
dim i as integer

i = 10
on runtime.line_index(i, n) gosub one, two, three
end

one:
    print "ONE"
    return

two:
    print "TWO"
    return

three:
    print "THREE"
    return
