
// Script for Web Worker for use in Timed Sort on xSortLab.html

var jobNum;
var arrayCt;
var arraySize;
var algorithm;
var arraysSorted;
var comparisonCt;
var copyCt;
var startTime;
var elapsedTime;
var comparisonsSinceLastCheck;
var timeSinceLastSend;
var done;
var A,B;

function send() {
    postMessage( [jobNum, arraysSorted, comparisonCt, copyCt, elapsedTime, done] );
}
       
onmessage = function(msg) {
    var data = msg.data;
    jobNum = data[0];
    arrayCt = data[1];
    arraySize = data[2];
    algorithm = data[3];
    arraysSorted = 0;
    comparisonCt = 0;
    copyCt = 0;
    elapsedTime = 0;
    done = false;
    comparisonsSinceLastCheck = 0;
    var size = arrayCt*arraySize;
    try {
        A = new Float32Array(size);
        if (algorithm == 4) {
            B = new Float32Array(arraySize);
        }
        for (var i = 0; i < A.length; i++) {
            A[i] = Math.random();
        }
    }
    catch (e) {
        postMessage([jobNum,"Error","Can't create arrays (probably not enough memory)."]);
        return;
    }
    postMessage([jobNum,"Initialized"]);
    startTime = new Date().getTime();
    timeSinceLastSend = startTime;
    var start = 0;
    var end = arraySize - 1;
    var sort;
    switch (algorithm) {
        case 1: sort = bubbleSort; break;
        case 2: sort = selectionSort; break;
        case 3: sort = insertionSort; break;
        case 4: sort = mergeSort; break;
        case 5: sort = quickSort; break;
    }
    try {
        while (start < size) {
            sort(start,end);
            //for (var i = start; i < end-1; i++) { // test that sorting worked.
            //    if (A[i] >= A[i+1]) {
            //        throw "not sorted at " + i;
            //    }
            //}
            //if (elapsedTime > 2000) { // test that errors are handled correctly.
            //   throw "Test error";
            //}
            start = end+1;
            end += arraySize;
            arraysSorted++;
        }
        elapsedTime = new Date().getTime() - startTime;
        done = true;
        send();
    }
    catch (e) {
        postMessage([jobNum,"Error",""+e]);
    }
}


function compare(q) {
   comparisonCt++;
   comparisonsSinceLastCheck++;
   if (comparisonsSinceLastCheck == 1000000) {
      var time = new Date().getTime();
      elapsedTime = time - startTime;
      if (time - timeSinceLastSend >= 200) {
          send();
          timeSinceLastSend = time;
      }
      comparisonsSinceLastCheck = 0;
   }
   return q;
}

function swap(loc1, loc2) { // swaps within array A
   var temp = A[loc1];
   A[loc1] = A[loc2];
   A[loc2] = temp;
   copyCt += 3;
}

function bubbleSort(start, end) {
   for (var top = end; top > start; top--)
      for (var i = start; i < top; i++)
         if (compare(A[i] > A[i+1]))
            swap(i, i+1);
}

function selectionSort(start, end) {
   for (var top = end; top > start; top--) {
      var max = start;
      for (var i = start+1; i <= top; i++)
         if (compare(A[i] > A[max]))
            max = i;
      swap(max,top);
   }
}

function insertionSort(start, end) {
   for (var insert = start+1; insert <= end; insert++) {
      var temp = A[insert];
      copyCt++;
      var i = insert-1;
      while (i >= start && compare(A[i] > temp)) {
         A[i+1] = A[i];
         copyCt++;
         i--;
      }
      A[i+1] = temp;
      copyCt++;
   }
}

function doMerge(from1, to1, from2, to2, count, posInB) {
   for (var i = 0; i < count; i++) {
     if (from2 > to2)
        B[posInB++] = A[from1++];
     else if (from1 > to1)
        B[posInB++] = A[from2++];
     else if (compare(A[from1] < A[from2]))
        B[posInB++] = A[from1++];
     else
        B[posInB++] = A[from2++];
     copyCt++;
   }
}

function mergeSort(start, end) {
   var length = end - start + 1;
   var sortLength = 1;
   while (sortLength < length) {
      var from1 = start;
      while (from1 <= end) {
         var from2 = from1 + sortLength;
         var to1 = from2 - 1;
         var to2 = from2 + sortLength - 1;
         if (to1 >= end)
            doMerge(from1,end,0,-1,end-from1+1,from1-start);
         else if (to2 >= end)
            doMerge(from1,to1,from2,end,end-from1+1,from1-start);
         else
            doMerge(from1,to1,from2,to2,2*sortLength,from1-start);
         from1 = to2 + 1;
      }
      for (var i = 0; i < length; i++)
         A[start+i] = B[i];
      copyCt += length;
      sortLength *= 2;
   }
}

function quickSortStep(lo, hi) {
   var temp = A[hi];
   copyCt++;
   while (hi > lo) {
      while (hi > lo && compare(A[lo] <= temp))
        lo++;
      if (hi > lo) {
         A[hi] = A[lo];
         copyCt++;
         hi--;
         while (hi > lo && compare(A[hi] >= temp))
            hi--;
         if (hi > lo) {
            A[lo] = A[hi];
            copyCt++;
            lo++;
         }
      }
   }
   A[hi] = temp;
   copyCt++;
   return hi;
}

function quickSort(start, end) {
   if (end > start) {
       var mid = quickSortStep(start, end);
       quickSort(start,mid-1);
       quickSort(mid+1,end);
   }
}

