package io.github.sgpublic.genshinguide.core.util

import io.github.sgpublic.kotlin.util.Loggable
import org.apache.commons.codec.digest.DigestUtils
import org.slf4j.LoggerFactory
import java.io.File

/**
 * @author sgpublic
 * @Date 2023/7/30 12:14
 */

private val log = LoggerFactory.getLogger("_Resource.kt")

fun File.checkSum(targetSha256: String?): Boolean {
    if (targetSha256 == null) {
        return false
    }
    log.info("文件校验中...")
    return if (getSum() == targetSha256) {
        log.info("文件校验成功：{}", this)
        true
    } else {
        log.info("文件校验失败：{}", this)
        false
    }
}

fun File.getSum(): String {
    return this.inputStream().use {
        DigestUtils.sha256Hex(it)
    }
}
