package com.techblue.appyhightask

fun main() {

    val input = "AAAAACCCCCAAAAACCCCCCAAAAAGGGGGTTTTTGGGGGTTTTTTGGGGGBBBCCC"

    val charArray = input.toCharArray()

    for (index in charArray.indices) {

        if (index + 10 < charArray.size - 1) {

            val subString = input.substring(index, index + 10)
            var count = 0
            for (innerIndex in index..input.toCharArray().size) {

                val innerString = input.substring(innerIndex)

                if (innerString.contains(subString)) {
                    count += 1
                } else {
                    break
                }
            }
            if (count > 1) {
                System.out.println(subString)
            }
        }
    }
}