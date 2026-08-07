package com.example.auralarc.navidrome

import java.security.MessageDigest
import java.util.*

object NavidromeAuth {

    fun createSalt(): String {
        return UUID.randomUUID()
            .toString()
            .replace(
                "-",
                ""
            )
            .take(
                12
            )
    }

    fun createToken(
        password: String,
        salt: String
    ): String {
        val input =
            password + salt

        val bytes =
            MessageDigest
                .getInstance(
                    "MD5"
                )
                .digest(
                    input.toByteArray(
                        Charsets.UTF_8
                    )
                )

        return bytes.joinToString(
            separator = ""
        ) { byte ->
            "%02x".format(
                byte
            )
        }
    }
}