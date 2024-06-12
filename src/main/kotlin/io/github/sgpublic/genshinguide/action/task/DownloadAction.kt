package io.github.sgpublic.genshinguide.action.task

import io.github.sgpublic.genshinguide.Config
import io.github.sgpublic.genshinguide.core.util.checkSum
import io.github.sgpublic.kotlin.util.Loggable
import kotlinx.coroutines.*
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import java.io.BufferedOutputStream
import java.io.File
import java.net.URI
import kotlin.math.max

interface DownloadAction {
    fun download(): File?

    companion object {
        fun of(info: GuideInfo, retry: Int = 3): DownloadAction {
            return DownloadActionImp(info, retry)
        }
    }
}

class DownloadActionImp(
    private val info: GuideInfo,
    private val retry: Int,
): DownloadAction, Loggable {
    override fun download(): File? {
        log.info("开始下攻略：${info.role.name}（${info.guide.image.url}）")
        try {
            if (tempFile.exists() && tempFile.isFile && tempFile.checkSum(info.sum)) {
                return tempFile
            }
            for (index in 0 until max(1, retry)) {
                try {
                    realDownload()
                    log.info("第 ${index + 1} 次尝试下载完成，校验文件完整性...")
                    if (tempFile.checkSum(info.sum)) {
                        return tempFile
                    } else {
                        throw IllegalStateException("文件校验不完整")
                    }
                } catch (e: Exception) {
                    log.warn("第 ${index + 1} 次尝试下载失败", e)
                }
            }
            log.warn("下载任务失败")
            return null
        } finally {
            log.info("下载任务结束")
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun realDownload() {
        tempFile.deleteRecursively()
        tempFile.parentFile.mkdirs()
        tempFile.createNewFile()
        val url = URI.create(info.guide.image.url)
        val resp = HttpClients.createDefault().execute(HttpGet(url))
        runBlocking {
            var downloaded = 0
            val downloadJob = GlobalScope.async {
                val buffer = ByteArray(4096)
                resp.entity.content.use { input ->
                    BufferedOutputStream(tempFile.outputStream()).use { output ->
                        var length: Int
                        while (input.read(buffer).also { length = it } > 0) {
                            downloaded += length
                            output.write(buffer, 0, length)
                        }
                    }
                }
            }
            val tickJob = GlobalScope.async {
                var debugFlag = 0
                while (downloadJob.isActive) {
                    val message = "下载中（${info.role.name}）：${String.format(
                        "%.2f", downloaded.toFloat() / info.guide.image.size * 100
                    )}%"
                    if (debugFlag == 0) {
                        log.info(message)
                    } else {
                        log.debug(message)
                    }
                    debugFlag = (debugFlag + 1) % 3
                    delay(500)
                }
            }
            downloadJob.await()
            tickJob.cancel()
        }
    }

    private val tempFile: File by lazy {
        File(Config.tempDir, "${info.role.element}/${info.role.name}.png")
    }
}
