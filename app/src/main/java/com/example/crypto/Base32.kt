package com.example.crypto

object Base32 {
  private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

  fun sanitize(input: String): String {
    return input.uppercase().replace(" ", "").replace("-", "").trimEnd('=')
  }

  fun isValid(input: String): Boolean {
    val sanitized = sanitize(input)
    if (sanitized.isEmpty()) return false
    for (char in sanitized) {
      if (!ALPHABET.contains(char)) {
        return false
      }
    }
    return true
  }

  fun decode(input: String): ByteArray {
    val sanitized = sanitize(input)
    if (sanitized.isEmpty()) return ByteArray(0)

    val output = ArrayList<Byte>()
    var buffer = 0
    var bitsLeft = 0

    for (char in sanitized) {
      val value = ALPHABET.indexOf(char)
      if (value == -1) {
        throw IllegalArgumentException("Illegal Base32 character: '$char'")
      }

      buffer = (buffer shl 5) or value
      bitsLeft += 5

      if (bitsLeft >= 8) {
        bitsLeft -= 8
        output.add(((buffer shr bitsLeft) and 0xFF).toByte())
      }
    }

    return output.toByteArray()
  }
}
