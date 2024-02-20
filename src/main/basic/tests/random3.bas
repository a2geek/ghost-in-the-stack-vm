option strict
option heap lomem=0x2000

print "PLEASE WAIT, ALLOCATING MEMORY"

dim n(4096) as integer, i as integer
dim done as boolean = False
dim counterlo as integer, counterhi as integer, x as integer
dim r as integer, bit as integer

print "... AND IT BEGINS."
print "LOOKING FOR RANDOM REPETITION..."
repeat
    r = math.random()
    bit = 1 << (r mod 16)
    i = r / 16
    done = (n(i) AND bit) <> 0
    n(i) = n(i) OR bit
    x = x + 1
    counterlo = counterlo + 1
    if counterlo > 10000 then
        counterhi = counterhi + 1
        counterlo = 0
    end if
    if x > 1000 then
        print "COUNTER = ";counterhi;",";counterlo;" (FIX ZEROS)"
        x = 0
    end if
until done

print "DUPLICATE OCCURRED AT REPETITION ";counterhi;",";counterlo
print "(SHOULD BE AROUND MAX UNSIGNED INT16 - 65536)"
end