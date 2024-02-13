option strict

uses "prodos"
uses "hires"
uses "strings"
uses "math"

const CHARWIDTH = 7
const CHARHEIGHT = 8
const SUBSHAPENUM = 1
const DESTROYERSHAPE = 9
const DEPTHCHARGESHAPE = 11
const CHARBASE = 15
const RUBSHAPE = CHARBASE + 95
const WATERLINE = 40
const SHIPY = WATERLINE - 3
const CHARGESTARTY = WATERLINE + 2
const CHARGEMAX = 5

' Game just uses a pile of local variables...
dim hiscore as integer, score as integer
dim shipx as integer, shipdirection as integer, shipshape as integer
dim oldshipx as integer, oldshipshape as integer
dim subshape as integer, subx as integer, suby as integer, subdirection as integer
dim oldsubshape as integer, oldsubx as integer, oldsuby as integer
dim explosionshape as integer, explosionx as integer, explosiony as integer
dim oldexplosionshape as integer, oldexplosionx as integer, oldexplosiony as integer
dim chargeindex as integer, remainingcharges as integer
dim chargex(CHARGEMAX),chargey(CHARGEMAX),chargeshape(CHARGEMAX)
dim priorchargex(CHARGEMAX),priorchargey(CHARGEMAX),priorchargeshape(CHARGEMAX)

sub initialize()
    bload("DESTROYER.BIN", 0x6000)
    shapeTable(0x6000)
    scale(1)
    rot(0)
end sub

' Display string at X,Y location. Note the erase done with the rub character; assumption is color is black!
sub drawTextXY(x as integer, y as integer, txt as string)
    dim i as integer, l as integer, ch as integer

    l = len(txt)-1      ' compute length only once
    for i = 0 to l
        ch = CHARBASE + ascn(txt,i)-asc(" ")
        draw(RUBSHAPE,x,y)
        xdraw(ch,x,y)
        x = x + CHARWIDTH
    next i
end sub

' VTAB/HTAB positioning. Order is due to original source (did VTAB first).
sub drawTextVH(cv as integer, ch as integer, txt as string)
    drawTextXY((ch-1)*CHARWIDTH, (cv-1)*CHARHEIGHT, txt)
end sub

inline sub updateScore()
    drawTextVH(2,37,strn(score,4))
end sub

inline sub updateCharges()
    drawTextVH(3,37,strn(remainingcharges,4))
end sub

sub updateGameStats()
    drawTextVH(1,37,strn(hiscore,4))
    updateScore()
    updateCharges()
end sub

sub title()
    dim y as integer

    hgr()
    poke -16302,0
    hcolor(0)
    drawTextVH(1,1,"Destroyer!")
    drawTextVH(22,4,"| Left, } Right, Spacebar to fire")
    drawTextVH(23,15,"Q to quit")
    drawTextVH(1,28,"Hiscore:")
    drawTextVH(2,28,"Score:")
    drawTextVH(3,28,"Charges:")
    updateGameStats()

    y = WATERLINE
    hcolor(6)
    hplot(0,y,279,y)
    hplot(0,159,279,159)
    hcolor(0)
    y = y + 15
    xdraw(DESTROYERSHAPE,45,y+3)
    drawTextXY(70,y,"Your destroyer")
    y = y + 10
    xdraw(SUBSHAPENUM,45,y+3)
    drawTextXY(70,y,"Enemy submarine (1 point)")
    y = y + 10
    xdraw(DEPTHCHARGESHAPE+0,40,y+3)
    xdraw(DEPTHCHARGESHAPE+1,45,y+3)
    xdraw(DEPTHCHARGESHAPE+2,50,y+3)
    xdraw(DEPTHCHARGESHAPE+3,55,y+3)
    drawTextXY(70,y,"Depth charges (hit = +5!)")
    y = y + 20
    drawTextXY(40,y,"Demo code for 'ghostvm'")
    y = y + 10
    drawTextXY(40,y,"Visit ghost-in-the-stack-vm")
    y = y + 20
    drawTextXY(40,y,"PRESS ANY KEY TO BEGIN!")
    while peek(-16384)<128
        ' wait for keypress
    end while
    poke -16368,0
    hcolor(0)
    for y = WATERLINE+15 to 150
        hplot(0,y,279,y)
    next y
end sub

' Game initialization
sub resetGame()
    dim i as integer

    shipx = 140
    shipdirection = 0
    shipshape = DESTROYERSHAPE
    score = 0
    remainingcharges = 30
    chargeindex = 1
    for i=1 to CHARGEMAX
        chargex(i) = 0
        chargey(i) = 0
        chargeshape(i) = 0
    next i
    subshape = 0
    subx = 0
    suby = 0
    subdirection = 0
    explosionshape = 0
    explosionx = 0
    explosiony = 0
    updateGameStats()
end sub

' Draw all active shapes (based on X coordinate)
sub drawShapes
    xdraw(shipshape,shipx,shipy)
    oldshipx = shipx
    oldshipshape = shipshape
    if subx then
        xdraw(subshape,subx,suby)
        oldsubshape = subshape
        oldsubx = subx
        oldsuby = suby
    end if
    priorchargex(chargeindex) = chargex(chargeindex)
    priorchargey(chargeindex) = chargey(chargeindex)
    priorchargeshape(chargeindex) = chargeshape(chargeindex)
    if chargex(chargeindex) then
        xdraw(chargeshape(chargeindex),chargex(chargeindex),chargey(chargeindex))
    end if
    if explosionx then
        xdraw(explosionshape,explosionx,explosiony)
        oldexplosionshape = explosionshape
        oldexplosionx = explosionx
        oldexplosiony = explosiony
    end if
end sub

sub releaseDepthCharge
    dim i as integer
    for i=1 to CHARGEMAX
        if chargex(i) = 0 then
            chargex(i) = shipx
            chargey(i) = chargestarty
            chargeshape(i) = DEPTHCHARGESHAPE
            remainingcharges = remainingcharges-1
            updateCharges()
            exit for
        end if
    next i
end sub

' Handle keyboard
sub handleKeyboad
    dim keypress as integer
    keypress = peek(-16384)
    if keypress < 128 then
        return
    end if
    poke -16368,0
    if keypress=136 then
        shipshape = DESTROYERSHAPE
        shipdirection = shipdirection-1
    elseif keypress=149 then
        shipshape = DESTROYERSHAPE+1
        shipdirection = shipdirection+1
    elseif keypress=160 and remainingcharges > 0 then
        releaseDepthCharge()
    elseif keypress=asc("Q") or keypress=asc("q") then
        remainingcharges = 0
    end if
    if shipdirection > 3 then shipdirection=3
    if shipdirection < -3 then shipdirection=-3
end sub

' Submarine. Only one at a time. If it doesn't exist, pick a random number to see if one shows up!
sub triggerSubmarine
    dim r as integer
    if subx = 0 then
        r = rnd(10)     ' grab only one random value
        select case r
        case 1
            subx = 10
            suby = WATERLINE+10+rnd(80)
            subdirection = 2
            subshape = SUBSHAPENUM+1
        case 2
            subx = 270
            suby = WATERLINE+10+rnd(80)
            subdirection = -2
            subshape = SUBSHAPENUM
        end select
    else
        subx = subx+subdirection
        if subx < 10 or subx > 270 then
            subshape = 0
            subx = 0
            suby = 0
            subdirection = 0
        end if
    end if
end sub

sub moveShapes
    ' Move the destroyer/ship
    shipx = shipx+shipdirection
    if shipx < 10 then shipx=10
    if shipx > 270 then shipx=270

    ' Make the explosion all explody
    if explosionshape then
        explosionshape = explosionshape+2
        if explosionshape >= DESTROYERSHAPE then explosionx=0
    end if

    ' Move one of the depth charges
    if chargex(chargeindex) then
        chargeshape(chargeindex) = chargeshape(chargeindex)+1
        if chargeshape(chargeindex) > DEPTHCHARGESHAPE+3 then
           chargeshape(chargeindex) = DEPTHCHARGESHAPE
        end if
        chargey(chargeindex) = chargey(chargeindex)+5
        if chargey(chargeindex) > 155 then
           chargex(chargeindex) = 0
           chargey(chargeindex) = 0
           chargeshape(chargeindex) = 0
        end if
    end if

    ' Setup for next depth charge
    chargeindex=chargeindex+1
    if chargeindex > CHARGEMAX then chargeindex = 1
end sub

sub eraseShapes
    if oldsubx then
        xdraw(oldsubshape,oldsubx,oldsuby)
        ' We hit a sub!
        if peek(234) then
            explosionshape = oldsubshape+2
            explosionx = oldsubx
            explosiony = oldsuby
            subx = 0
            score = score+1
            remainingcharges = remainingcharges+5
            updateCharges()
            updateScore()
        end if
        oldsubx=0
    end if
    if priorchargex(chargeindex) then
        xdraw(priorchargeshape(chargeindex), _
              priorchargex(chargeindex), _
              priorchargey(chargeindex))
        if peek(234) then
            chargex(chargeindex) = 0
            chargey(chargeindex) = 0
            chargeshape(chargeindex) = 0
        end if
    end if
    if oldexplosionx then
        xdraw(oldexplosionshape,oldexplosionx,oldexplosiony)
        oldexplosionx = 0
    end if
    xdraw(oldshipshape,oldshipx,shipy)
end sub

sub game()
    resetGame()
    while remainingcharges > 0
        drawShapes()
        handleKeyboad()
        triggerSubmarine()
        moveShapes()
        eraseShapes()
    end while

    if score > hiscore then hiscore = score
end sub

initialize()
while True
    title()
    game()
end while

