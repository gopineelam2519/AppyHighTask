package com.techblue.appyhightask

fun main() {

    //matrix spiral problem 2d Matrix

//    val arr= Array(2) { IntArray(3) }
    val arr = arrayOf(intArrayOf(1, 2, 3, 4), intArrayOf(5, 6, 7, 8), intArrayOf(9, 10, 11, 12), intArrayOf(13, 14, 15, 16))
    val rowStart = 0
    val rowEnd = 4
    val columnStart = 0
    val columnEnd = 4


    iConstantIncrementJ(arr, rowStart, rowEnd, columnStart, columnEnd)
}

fun iConstantIncrementJ(arr: Array<IntArray>, rowStart: Int, rowEnd: Int, columnStart: Int, columnEnd: Int) {
    for (j in columnStart until columnEnd) {
        print(arr[rowStart][j])
        print(" ")
        if (j == columnEnd - 1) {
            //make j constant & increment i
            val innerRowStart = rowStart + 1
            jConstantIncrementI(arr, innerRowStart, rowEnd, columnStart, columnEnd)
        }
    }
}

fun jConstantIncrementI(arr: Array<IntArray>, rowStart: Int, rowEnd: Int, columnStart: Int, columnEnd: Int) {
    for (k in rowStart until rowEnd) {
        print(arr[k][columnEnd - 1])
        print(" ")
        if (k == rowEnd - 1) {
            val innerColumnEnd = columnEnd - 1
            iConstantDecrementJ(arr, rowStart, rowEnd, columnStart, innerColumnEnd)
        }
    }
}

fun iConstantDecrementJ(arr: Array<IntArray>, rowStart: Int, rowEnd: Int, columnStart: Int, columnEnd: Int) {
    for (k in columnEnd - 1 downTo columnStart) {
        print(arr[rowEnd - 1][k])
        print(" ")
        if (k == columnStart) {
            val innerRowEnd = rowEnd - 1
            // System.out.println("rowStart: ${rowStart}, rowEnd: ${innerRowEnd}, columnStart: ${columnStart}, columnEnd: ${columnEnd}")
            jConstantDecrementI(arr, rowStart, innerRowEnd, columnStart, columnEnd)
        }
    }
}

fun jConstantDecrementI(arr: Array<IntArray>, rowStart: Int, rowEnd: Int, columnStart: Int, columnEnd: Int) {
    for (k in rowEnd - 1 downTo rowStart) {
        print(arr[k][columnStart])
        print(" ")
        if (k == rowStart) {
            val innerColumnStart = columnStart + 1
            iConstantIncrementJ(arr, rowStart, rowEnd, innerColumnStart, columnEnd)
        }
    }
}

