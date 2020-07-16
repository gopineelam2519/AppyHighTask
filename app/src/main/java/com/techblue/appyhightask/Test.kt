package com.techblue.appyhightask

fun main() {
    val arr = intArrayOf(1, 1, 0, 1, 1, 1, 0, 1, 1, 1, 1, 0, 0, 1, 1, 1, 1, 1)
    var maxLength = 0

    if (arr.size < 10000) {
        for (index in arr.indices) {
            if (arr[index] == 1) {
                maxLength += 1
            } else {
                if (index < arr.size - 1)
                    maxLength = 0
            }
        }

        System.out.println("$maxLength")
    } else {
        System.out.println("Length of the array exceeds 10000")
    }

}