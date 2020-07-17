package com.techblue.appyhightask

fun main() {

    //finding max no.of consecutive 1s

    val arr = intArrayOf(1, 1, 0, 1, 1, 1, 0, 1, 1, 1, 1, 0, 0, 1, 1, 1, 1, 1, 0, 1, 1)
    var maxLength = 0
    var globalMax = 0

    if (arr.size < 10000) {
        for (index in arr.indices) {
            if (arr[index] == 1) {
                maxLength += 1
            } else {
                if (index < arr.size - 1) {
                    globalMax = maxLength
                    maxLength = 0
                }
            }
        }

        System.out.println("$globalMax")
    } else {
        System.out.println("Length of the array exceeds 10000")
    }

}