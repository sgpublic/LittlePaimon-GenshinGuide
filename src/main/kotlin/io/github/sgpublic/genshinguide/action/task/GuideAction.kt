package io.github.sgpublic.genshinguide.action.task

import io.github.sgpublic.genshinguide.action.rss.GuideRSS
import io.github.sgpublic.genshinguide.core.AbsConfig
import io.github.sgpublic.kotlin.util.log
import java.io.File
import java.util.*

/**
 * @author sgpublic
 * @Date 2023/7/29 15:16
 */
interface GuideAction: AutoCloseable {
    fun needUpdate(info: GuideInfo): Boolean
    fun saveGuide(info: GuideInfo)

    companion object {
        fun create(list: List<RepoAction>): GuideAction {
            return GuideActionImpl(list)
        }
    }
}

class GuideActionImpl(
    private val list: List<RepoAction>
): GuideAction {
    override fun needUpdate(
        info: GuideInfo
    ): Boolean {
        for (item in list) {
            if (item.needUpdate(info)) {
                return true
            }
        }
        return false
    }

    override fun saveGuide(
        info: GuideInfo,
    ) {
        val guide: File? = DownloadAction.of(info).download()
        if (guide == null) {
            log.warn("攻略 ${info.role.name} 下载失败")
            return
        }
        for (item in list) {
            if (!item.needUpdate(info)) {
                continue
            }
            try {
                item.saveGuide(guide, info)
                log.info("攻略导出到 ${item.id} 成功：${info.role.name}")
            } catch (e: Exception) {
                log.warn("攻略 ${info.role.name} 保存失败，仓库 ID：${item.id}", e)
            }
        }
    }

    override fun close() {
        for (repoAction in list) {
            repoAction.close()
        }
    }
}