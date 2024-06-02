option strict
option default integer

uses "text"
uses "lores"

' Player Type
const COMPUTER = 1
const HUMAN = 2

' Cell Type
const UNASSIGNED = 0
const PLAYER_X = 1
const PLAYER_O = 2

dim playerX = HUMAN
dim playerO = COMPUTER
dim cell1, cell2, cell3, cell4, cell5, cell6, cell7, cell8, cell9

' lores (GR) but all 48 lines
sub fullScreenGR()
    poke -16298,0 : poke -16300,0 : poke -16302,0 : poke -16304,0 : call 0xf832
end sub

function waitForKey() as integer
    dim ch
    do
        ch = Math.random()  ' randomize
        ch = peek(0xc000)
    loop while ch < 128
    poke 0xc010,0
    return ch
end function


sub drawLogoA(x, y)
    hlin(x+5,x+6,y)
    hlin(x+4,x+7,y+1)
    hlin(x+3,x+8,y+2)
    hlin(x+2,x+4,y+3) : hlin(x+7,x+9,y+3)
    hlin(x+1,x+3,y+4) : hlin(x+8,x+10,y+4)
    hlin(x,x+2,y+5) : hlin(x+9,x+11,y+5)
    hlin(x,x+11,y+8) : hlin(x,x+11,y+9)
    vlin(y+6,y+13,x) : vlin(y+6,y+13,x+1)
    vlin(y+6,y+13,x+9) : vlin(y+6,y+13,x+10) : vlin(y+6,y+13,x+11)
end sub

sub drawLogoC(x, y)
    hlin(x+3,x+9,y)
    hlin(x+2,x+10,y+1)
    hlin(x+1,x+3,y+2) : hlin(x+9,x+11,y+2)
    plot(x+2,y+3) : hlin(x+10,x+11,y+3)
    vlin(y+3,y+10,x)
    vlin(y+3,y+10,x+1)
    plot(x+2,y+10) : hlin(x+9,x+11,y+10)
    hlin(x+1,x+3,y+11) : hlin(x+8,x+11,y+11)
    hlin(x+2,x+10,y+12)
    hlin(x+3,x+9,y+13)
end sub

sub drawLogoE(x, y)
    hlin(x,x+11,y)
    hlin(x,x+11,y+1)
    hlin(x+10,x+11,y+2)
    vlin(y+2,y+11,x)
    vlin(y+2,y+11,x+1)
    vlin(y+6,y+11,x+2)
    hlin(x+6,x+7,y+5)
    hlin(x+2,x+7,y+6)
    hlin(x+2,x+7,y+7)
    hlin(x+6,x+7,y+8)
    hlin(x+10,x+11,y+11)
    hlin(x,x+11,y+12)
    hlin(x,x+11,y+13)
end sub

sub drawLogoI(x, y)
    hlin(x+1,x+10,y)
    hlin(x+1,x+10,y+1)
    vlin(y+8,y+11,x+4)
    vlin(y+2,y+11,x+5)
    vlin(y+2,y+11,x+6)
    hlin(x+1,x+10,y+12)
    hlin(x+1,x+10,y+13)
end sub

sub drawLogoO(x, y)
    hlin(x+4,x+7,y)
    hlin(x+3,x+8,y+1)
    hlin(x+2,x+4,y+2) : hlin(x+7,x+9,y+2)
    hlin(x+1,x+3,y+3) : hlin(x+8,x+10,y+3)
    hlin(x,x+2,y+4) : hlin(x+9,x+11,y+4)
    vlin(y+5,y+8,x) : vlin(y+5,y+8,x+1)
    vlin(y+5,y+8,x+9) : vlin(y+5,y+8,x+10) : vlin(y+5,y+8,x+11)
    hlin(x,x+2,y+9) : hlin(x+9,x+11,y+9)
    hlin(x+1,x+3,y+10) : hlin(x+8,x+10,y+10)
    hlin(x+2,x+4,y+11) : hlin(x+7,x+9,y+11)
    hlin(x+3,x+8,y+12)
    hlin(x+4,x+7,y+13)
end sub

sub drawLogoT(x, y)
    hlin(x,x+11,y)
    hlin(x,x+11,y+1)
    hlin(x,x+1,y+2) : hlin(x+10,x+11,y+2)
    vlin(y+8,y+13,x+4)
    vlin(y+2,y+13,x+5)
    vlin(y+2,y+13,x+6)
end sub


sub drawCell1(x, y)
    vlin(y+1,y+8,x+4)
    vlin(y+1,y+8,x+5)
    plot(x+3,y+2)
    hlin(x+2,x+7,y+8)
end sub

sub drawCell2(x, y)
    hlin(x+2,x+7,y+1)
    hlin(x+1,x+2,y+2) : hlin(x+7,x+8,y+2)
    hlin(x+7,x+8,y+3)
    hlin(x+4,x+7,y+4)
    hlin(x+2,x+5,y+5)
    hlin(x+1,x+2,y+6)
    hlin(x+1,x+2,y+7)
    hlin(x+1,x+8,y+8)
end sub

sub drawCell3(x, y)
    hlin(x+2,x+7,y+1)
    hlin(x+1,x+2,y+2) : hlin(x+7,x+8,y+2)
    hlin(x+7,x+8,y+3)
    hlin(x+5,x+7,y+4)
    hlin(x+7,x+8,y+5)
    hlin(x+7,x+8,y+6)
    hlin(x+1,x+2,y+7) : hlin(x+7,x+8,y+7)
    hlin(x+2,x+7,y+8)
end sub

sub drawCell4(x, y)
    hlin(x+5,x+6,y+1)
    hlin(x+4,x+6,y+2)
    hlin(x+3,x+6,y+3)
    hlin(x+2,x+3,y+4) : hlin(x+5,x+6,y+4)
    hlin(x+1,x+2,y+5) : hlin(x+5,x+6,y+5)
    hlin(x+1,x+7,y+6)
    hlin(x+5,x+6,y+7)
    hlin(x+5,x+6,y+8)
end sub

sub drawCell5(x, y)
    hlin(x+1,x+8,y+1)
    hlin(x+1,x+2,y+2)
    hlin(x+1,x+2,y+3)
    hlin(x+1,x+7,y+4)
    hlin(x+7,x+8,y+5)
    hlin(x+7,x+8,y+6)
    hlin(x+1,x+2,y+7) : hlin(x+7,x+8,y+7)
    hlin(x+2,x+7,y+8)
end sub

sub drawCell6(x, y)
    hlin(x+3,x+7,y+1)
    hlin(x+2,x+3,y+2)
    hlin(x+1,x+2,y+3)
    hlin(x+1,x+7,y+4)
    hlin(x+1,x+2,y+5) : hlin(x+7,x+8,y+5)
    hlin(x+1,x+2,y+6) : hlin(x+7,x+8,y+6)
    hlin(x+1,x+2,y+7) : hlin(x+7,x+8,y+7)
    hlin(x+2,x+7,y+8)
end sub

sub drawCell7(x, y)
    hlin(x+1,x+8,y+1)
    hlin(x+1,x+2,y+2) : hlin(x+7,x+8,y+2)
    hlin(x+6,x+8,y+3)
    hlin(x+5,x+7,y+4)
    hlin(x+4,x+6,y+5)
    hlin(x+4,x+5,y+6)
    hlin(x+4,x+5,y+7)
    hlin(x+4,x+5,y+8)
end sub

sub drawCell8(x, y)
    hlin(x+2,x+7,y+1)
    hlin(x+1,x+2,y+2) : hlin(x+7,x+8,y+2)
    hlin(x+1,x+2,y+3) : hlin(x+7,x+8,y+3)
    hlin(x+2,x+7,y+4)
    hlin(x+1,x+2,y+5) : hlin(x+7,x+8,y+5)
    hlin(x+1,x+2,y+6) : hlin(x+7,x+8,y+6)
    hlin(x+1,x+2,y+7) : hlin(x+7,x+8,y+7)
    hlin(x+2,x+7,y+8)
end sub

sub drawCell9(x, y)
    hlin(x+2,x+7,y+1)
    hlin(x+1,x+2,y+2) : hlin(x+7,x+8,y+2)
    hlin(x+1,x+2,y+3) : hlin(x+7,x+8,y+3)
    hlin(x+1,x+2,y+4) : hlin(x+7,x+8,y+4)
    hlin(x+1,x+8,y+5)
    hlin(x+7,x+8,y+6)
    hlin(x+6,x+7,y+7)
    hlin(x+2,x+6,y+8)
end sub

sub drawCellX(x, y)
    hlin(x+1,x+2,y+1) : hlin(x+7,x+8,y+1)
    hlin(x+2,x+3,y+2) : hlin(x+6,x+7,y+2)
    hlin(x+3,x+6,y+3)
    hlin(x+4,x+5,y+4)
    hlin(x+4,x+5,y+5)
    hlin(x+3,x+6,y+6)
    hlin(x+2,x+3,y+7) : hlin(x+6,x+7,y+7)
    hlin(x+1,x+2,y+8) : hlin(x+7,x+8,y+8)
end sub

sub drawCellO(x, y)
    hlin(x+3,x+6,y+1)
    hlin(x+2,x+3,y+2) : hlin(x+6,x+7,y+2)
    hlin(x+1,x+2,y+3) : hlin(x+7,x+8,y+3)
    hlin(x+1,x+2,y+4) : hlin(x+7,x+8,y+4)
    hlin(x+1,x+2,y+5) : hlin(x+7,x+8,y+5)
    hlin(x+1,x+2,y+6) : hlin(x+7,x+8,y+6)
    hlin(x+2,x+3,y+7) : hlin(x+6,x+7,y+7)
    hlin(x+3,x+6,y+8)
end sub


sub drawFontA(x,y)
    hlin(x+1,x+3,y)
    hlin(x+1,x+3,y+2)
    vlin(y+1,y+4,x)
    vlin(y+1,y+4,x+4)
end sub

sub drawFontE(x,y)
    hlin(x,x+4,y)
    hlin(x,x+3,y+2)
    hlin(x,x+4,y+4)
    vlin(y,y+4,x)
end sub

sub drawFontG(x,y)
    hlin(x+1,x+4,y)
    hlin(x+2,x+4,y+2)
    hlin(x+1,x+4,y+4)
    vlin(y+1,y+3,x)
end sub

sub drawFontI(x,y)
    hlin(x+1,x+3,y)
    hlin(x+1,x+3,y+4)
    vlin(y,y+4,x+2)
end sub

sub drawFontM(x,y)
    vlin(y,y+4,x)
    vlin(y,y+4,x+4)
    plot(x+1,y+1)
    plot(x+2,y+2)
    plot(x+3,y+1)
end sub

sub drawFontN(x,y)
    vlin(y,y+4,x)
    vlin(y,y+4,x+4)
    plot(x+1,y+1)
    plot(x+2,y+2)
    plot(x+3,y+3)
end sub

sub drawFontO(x,y)
    hlin(x+1,x+3,y)
    vlin(y+1,y+3,x)
    vlin(y+1,y+3,x+4)
    hlin(x+1,x+3,y+4)
end sub

sub drawFontS(x,y)
    hlin(x+1,x+4,y)
    hlin(x+1,x+3,y+2)
    hlin(x,x+3,y+4)
    plot(x,y+1)
    plot(x+4,y+3)
end sub

sub drawFontT(x,y)
    hlin(x,x+4,y)
    vlin(y,y+4,x+2)
end sub

sub drawFontW(x,y)
    vlin(y,y+4,x)
    vlin(y,y+4,x+4)
    plot(x+1,y+3)
    plot(x+2,y+2)
    plot(x+3,y+3)
end sub

sub drawFontX(x,y)
    plot(x,y)
    plot(x+4,y)
    plot(x+1,y+1)
    plot(x+3,y+1)
    plot(x+2,y+2)
    plot(x+3,y+3)
    plot(x+1,y+3)
    plot(x+4,y+4)
    plot(x,y+4)
end sub


sub title()
    dim y

    fullScreenGR()
    color(lores.PURPLE)
    drawLogoT(1,y)
    drawLogoI(14,y)
    drawLogoC(27,y)
    y += 16
    drawLogoT(1,y)
    drawLogoA(14,y)
    drawLogoC(27,y)
    y += 16
    drawLogoT(1,y)
    drawLogoO(14,y)
    drawLogoE(27,y)

    y = waitForKey()
end sub

function menu() as integer
    dim ch

    text : home
    htab(15) : print "_____________"
    htab(15) : inverse : print " TIC-TAC-TOE " : normal

    do
        vtab(5) : htab(1)
        print
        print "PLAYER [X]: ";("HUMAN   " if playerX = HUMAN else "COMPUTER")
        print
        print "PLAYER [O]: ";("HUMAN   " if playerO = HUMAN else "COMPUTER")
        print
        print "USE 'X' OR 'O' TO TOGGLE,"
        print "'G' TO START GAME, OR 'Q' TO QUIT."

        ch = waitForKey()
        if ch = asc("X") or ch = asc("x") then
            playerX = HUMAN if playerX = COMPUTER else COMPUTER
        elseif ch = asc("O") or ch = asc("o") then
            playerO = HUMAN if playerO = COMPUTER else COMPUTER
        end if

    loop until ch = asc("Q") or ch = asc("q") or ch = asc("G") or ch = asc("g")
    return ch
end function

sub drawCell(cellNumber, cell, x, y)
    select case cell
    case UNASSIGNED
        color(lores.DARK_BLUE)
        select case cellNumber
        case 1
            drawCell1(x,y)
        case 2
            drawCell2(x,y)
        case 3
            drawCell3(x,y)
        case 4
            drawCell4(x,y)
        case 5
            drawCell5(x,y)
        case 6
            drawCell6(x,y)
        case 7
            drawCell7(x,y)
        case 8
            drawCell8(x,y)
        case 9
            drawCell9(x,y)
        end select
    case PLAYER_X
        color(lores.MAGENTA)
        drawCellX(x,y)
    case PLAYER_O
        color(lores.GREEN)
        drawCellO(x,y)
    end select
end sub

sub drawGrid()
    fullScreenGR()
    color(lores.DARK_GRAY)
    hlin(4,36,19)
    hlin(4,36,30)
    vlin(8,40,15)
    vlin(8,40,26)
    drawCell(1, cell1, 5, 9)
    drawCell(2, cell2, 16, 9)
    drawCell(3, cell3, 27, 9)
    drawCell(4, cell4, 5, 20)
    drawCell(5, cell5, 16, 20)
    drawCell(6, cell6, 27, 20)
    drawCell(7, cell7, 5, 31)
    drawCell(8, cell8, 16, 31)
    drawCell(9, cell9, 27, 31)
end sub

sub setCell(playerType, byref cell, byref ch)
    if cell = UNASSIGNED then
        cell = playerType
    else
        ch = 0
    end if
end sub

sub playHuman(playerType)
    dim ch
    do
        ch = waitForKey()
        select case ch - asc("0")
        case 1
            setCell(playerType, cell1, ch)
        case 2
            setCell(playerType, cell2, ch)
        case 3
            setCell(playerType, cell3, ch)
        case 4
            setCell(playerType, cell4, ch)
        case 5
            setCell(playerType, cell5, ch)
        case 6
            setCell(playerType, cell6, ch)
        case 7
            setCell(playerType, cell7, ch)
        case 8
            setCell(playerType, cell8, ch)
        case 9
            setCell(playerType, cell9, ch)
        case else
            ch = 0  ' try it again
        end select
    loop until ch <> 0
end sub

function checkAndSet(testPlayer, setPlayer, byref cellA, byref cellB, byref cellC) as boolean
    dim count
    count += 1 if cellA = testPlayer else 0
    count += 1 if cellB = testPlayer else 0
    count += 1 if cellC = testPlayer else 0
    if count = 2 then
        if cellA = UNASSIGNED then
            cellA = setPlayer
            return true
        elseif cellB = UNASSIGNED then
            cellB = setPlayer
            return true
        elseif cellC = UNASSIGNED then
            cellC = setPlayer
            return true
        end if
    end if
    return false
end function

sub playComputer(currentPlayer)
    dim otherPlayer = PLAYER_X if currentPlayer = PLAYER_O else PLAYER_O
    ' check if we are about to win and place
    if checkAndSet(currentPlayer, currentPlayer, cell1, cell2, cell3) then return
    if checkAndSet(currentPlayer, currentPlayer, cell4, cell5, cell6) then return
    if checkAndSet(currentPlayer, currentPlayer, cell7, cell8, cell9) then return
    if checkAndSet(currentPlayer, currentPlayer, cell1, cell4, cell7) then return
    if checkAndSet(currentPlayer, currentPlayer, cell2, cell5, cell8) then return
    if checkAndSet(currentPlayer, currentPlayer, cell3, cell6, cell9) then return
    if checkAndSet(currentPlayer, currentPlayer, cell1, cell5, cell9) then return
    if checkAndSet(currentPlayer, currentPlayer, cell3, cell5, cell7) then return
    ' check if other player is about to win and place
    if checkAndSet(otherPlayer, currentPlayer, cell1, cell2, cell3) then return
    if checkAndSet(otherPlayer, currentPlayer, cell4, cell5, cell6) then return
    if checkAndSet(otherPlayer, currentPlayer, cell7, cell8, cell9) then return
    if checkAndSet(otherPlayer, currentPlayer, cell1, cell4, cell7) then return
    if checkAndSet(otherPlayer, currentPlayer, cell2, cell5, cell8) then return
    if checkAndSet(otherPlayer, currentPlayer, cell3, cell6, cell9) then return
    if checkAndSet(otherPlayer, currentPlayer, cell1, cell5, cell9) then return
    if checkAndSet(otherPlayer, currentPlayer, cell3, cell5, cell7) then return
    ' if other player took a corner, take the center
    if (cell1 = otherPlayer or cell3 = otherPlayer or cell7 = otherPlayer or cell9 = otherPlayer) _
            and cell5 = UNASSIGNED then
        cell5 = currentPlayer : return
    end if
    ' GAP..? not super smart when placing here... play a bit and it will be clear...
    ' pick a cell
    if cell1 = UNASSIGNED then
        cell1 = currentPlayer : return
    elseif cell3 = UNASSIGNED then
        cell3 = currentPlayer : return
    elseif cell7 = UNASSIGNED then
        cell7 = currentPlayer : return
    elseif cell9 = UNASSIGNED then
        cell9 = currentPlayer : return
    elseif cell2 = UNASSIGNED then
        cell2 = currentPlayer : return
    elseif cell4 = UNASSIGNED then
        cell4 = currentPlayer : return
    elseif cell6 = UNASSIGNED then
        cell6 = currentPlayer : return
    elseif cell8 = UNASSIGNED then
        cell8 = currentPlayer : return
    else
        cell5 = currentPlayer : return
    end if
end sub

function checkCells(player, cellA, cellB, cellC) as boolean
    return cellA = player AND cellB = player and cellC = player
end function

function checkForWinner(player)
    if checkCells(player, cell1, cell2, cell3) _
            or checkCells(player, cell4, cell5, cell6) _
            or checkCells(player, cell7, cell8, cell9) _
            or checkCells(player, cell1, cell4, cell7) _
            or checkCells(player, cell2, cell5, cell8) _
            or checkCells(player, cell3, cell6, cell9) _
            or checkCells(player, cell1, cell5, cell9) _
            or checkCells(player, cell3, cell5, cell7) then
        return player
    elseif cell1 <> 0 and cell2 <> 0 and cell3 <> 0 and cell4 <> 0 and _
            cell5 <> 0 and cell6 <> 0 and cell7 <> 0 and cell8 <> 0 and _
            cell9 <> 0 then
        return -1   ' tie/no more moves
    else
        return 0
    end if
end function

sub playGame()
    dim winner

    cell1 = UNASSIGNED : cell2 = UNASSIGNED : cell3 = UNASSIGNED
    cell4 = UNASSIGNED : cell5 = UNASSIGNED : cell6 = UNASSIGNED
    cell7 = UNASSIGNED : cell8 = UNASSIGNED : cell9 = UNASSIGNED

    drawGrid()
    do
        ' player X
        if playerX = HUMAN then
            playHuman(PLAYER_X)
        else
            playComputer(PLAYER_X)
        end if
        drawGrid()
        winner = checkForWinner(PLAYER_X)
        ' player O
        if winner = 0 then
            if playerO = HUMAN then
                playHuman(PLAYER_O)
            else
                playComputer(PLAYER_O)
            end if
            drawGrid()
            winner = checkForWinner(PLAYER_O)
        end if
    loop until winner <> 0

    ' stupid win screen
    color(lores.YELLOW)
    if winner = -1 then
        ' TIE
        drawFontT(13,0)
        drawFontI(18,0)
        drawFontE(23,0)
    else
        ' X|O WINS
        if winner = PLAYER_X then
            drawFontX(7,0)
        else
            drawFontO(7,0)
        end if
        drawFontW(15,0)
        drawFontI(20,0)
        drawFontN(25,0)
        drawFontS(31,0)
    end if
    winner = waitForKey()
end sub


dim ch
repeat
    title()
    ch = menu()
    if ch = asc("G") or ch = asc("g") then
        playGame()
    end if
until ch = asc("Q") or ch = asc("q")

text : home
end