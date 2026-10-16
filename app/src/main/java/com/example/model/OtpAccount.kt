package com.example.model

enum class OtpType {
  TOTP,
  HOTP
}

enum class OtpAlgorithm {
  SHA1,
  SHA256,
  SHA512
}

data class OtpAccount(
  val id: String,
  val accountName: String,
  val issuer: String = "",
  val secretKey: String,
  val type: OtpType = OtpType.TOTP,
  val algorithm: OtpAlgorithm = OtpAlgorithm.SHA1,
  val digits: Int = 6,
  val period: Int = 30,
  val counter: Long = 0L,
  val createdAt: Long = System.currentTimeMillis()
)
