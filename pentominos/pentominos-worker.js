"use strict";

onmessage = function(msg) {
   if (msg.data == "continue") {
      solve();
      return;
   }
   board = msg.data[0];
   rows = board.length;
   cols = board[0].length;
   executionStack = msg.data[1];
   frame = msg.data[2];
   used = msg.data[3];
   usedCt = msg.data[4];
   moves = msg.data[5];
   solutions = msg.data[6];
   checkForBlocking = msg.data[7];
   extraEmptySpaces = msg.data[8];
   piece_data = msg.data[9];
   piecesForSolution = msg.data[10];
   if (checkForBlocking) {
       blockCheckData = new Array(rows);
       for (var i = 0; i < rows; i++) {
           blockCheckData[i] = new Array(cols);
           for (var j = 0; j < cols; j++) {
               blockCheckData[i][j] = 0;
           }
       }
   }
   solve();
}

function sendResults(done) {
    var data = [];
    data.push(board,executionStack,frame,used,usedCt,moves,solutions,done);
    postMessage(data);
}


var piece_data;

var rows, cols;   // board size
var board;        // 2D array containing contents of the board

var checkForBlocking;

var blockCheckData;
var blockCheckCt = 0;
var extraEmptySpaces;

var executionStack;  // Stack for the recursive backtracking solution function.
var frame;  // Data for current level of the recursion
var used;  // used[i] tells if piece #i is on the board, for 1 from 1 to 12
var usedCt;

var piecesForSolution;  // How many pieces fit on the board
var moves;  // How many moves?  A move means placing a piece on the board.
var solutions;  // How many solutions have been found?


function solutionStep() {
     if (frame[2] == 63) {  // empty space was left at (frame[0],frame[1])
         board[frame[0]][frame[1]] = -1;
         extraEmptySpaces++;
     }
     else {
         if (frame[2] >= 0) { 
             removePiece(piece_data[frame[2]], frame[0], frame[1]);
             used[ piece_data[frame[2]][0] ] = 0;
             usedCt--;
         }
         frame[2]++;
         while (frame[2] < piece_data.length) {  // find the next piece to place  
             if ( !used[piece_data[frame[2]][0]] && canPlay(piece_data[frame[2]], frame[0], frame[1])) {
                 if (checkForBlocking) {
                     playPiece(piece_data[frame[2]], frame[0], frame[1], false);
                     if (obviousBlockExists()) {
                         removePiece(piece_data[frame[2]], frame[0], frame[1], false);
                         frame[2]++;
                         continue;
                     }
                 }
                 else {
                     playPiece(piece_data[frame[2]], frame[0], frame[1]);
                 }
                 moves++;
                 used[ piece_data[frame[2]][0] ] = 1;
                 usedCt++;
                 if (usedCt == piecesForSolution) {
                     return;
                 }
                 executionStack.push(frame);
                 var row = frame[0];
                 var col = frame[1];
                 while (row < rows && board[row][col] != -1) {
                     col++;
                     if (col == cols) {
                         col = 0;
                         row++;
                     }
                 }
                 frame = [row,col,-1];
                 return;
             }
             frame[2]++;
         }
         if (extraEmptySpaces > 0) { // leave space empty
             extraEmptySpaces--;
             board[frame[0]][frame[1]] = -2;
             executionStack.push(frame);
             var row = frame[0];
             var col = frame[1];
             while (row < rows && board[row][col] != -1) {
                 col++;
                 if (col == cols) {
                     col = 0;
                     row++;
                 }
             }
             frame = [row,col,-1];
             return;
         }
     }
     if (executionStack.length == 0) {  // no solution
         return;
     }
     frame = executionStack.pop();
     return;
 }


function solve() {
    while (true) {
        if (executionStack.length == 0 && frame[2] == piece_data.length) {
            sendResults(true);
            break;
        }
        solutionStep();
        if (usedCt == piecesForSolution) {
            solutions++;
            sendResults(false);
            break;
        }
    }
}

function canPlay(pieceData, row, col) {
    if (board[row][col] != -1) {
        return false;
    }
    for (var i = 1; i < pieceData.length; i+= 2) {
        var r = row + pieceData[i];
        var c = col + pieceData[i+1];
        if (r < 0 || r >= rows || c < 0 || c >= cols || board[r][c] != -1) {
            return false;
        }
    }
    return true;
}

function playPiece(pieceData, row, col) { // assume the move has already been checked as legal!
    var p = pieceData[0];  // number of the piece
    board[row][col] = p;
    for (var i = 1; i < pieceData.length; i+= 2) {
        var r = row + pieceData[i];
        var c = col + pieceData[i+1];
        board[r][c] = p;
    }
}

function removePiece(pieceData, row, col) { // assume the is known to be there!
    board[row][col] = -1;
    for (var i = 1; i < pieceData.length; i+= 2) {
        var r = row + pieceData[i];
        var c = col + pieceData[i+1];
        board[r][c] = -1;
    }
}

function obviousBlockExists() { // Check whether the board has a region that can never be filled because of the number of squares it contains.
   blockCheckCt++;
   var forcedEmptyCt = 0;
   for (var r = 0; r < rows; r++)
      for (var c = 0; c < cols; c++) {
         var blockSize = countEmptyBlock(r,c);
         if (blockSize % 5 == 0)
            continue;
         forcedEmptyCt += blockSize % 5;
         if (forcedEmptyCt > extraEmptySpaces)
            return true;
      }
   return false;
}

function countEmptyBlock(r, c) {  // Find the size of one empty region on the board; recursive routine called by obviousBlockExists.
   if (blockCheckData[r][c] == blockCheckCt || board[r][c] != -1)
      return 0;
   var i, c1 = c, c2 = c;
   while (c1 > 0 && blockCheckData[r][c1-1] < blockCheckCt && board[r][c1-1] == -1)
      c1--;
   while (c2 < cols-1 && blockCheckData[r][c2+1] < blockCheckCt && board[r][c2+1] == -1)
      c2++;
   for (i = c1; i <= c2; i++)
      blockCheckData[r][i] = blockCheckCt;
   var ct = c2 - c1 + 1;
   if (r > 0)
      for (i = c1; i <= c2; i++)
         ct += countEmptyBlock(r-1,i);
   if (r < rows-1)
      for (i = c1; i <= c2; i++)
         ct += countEmptyBlock(r+1,i);
   return ct;
}
