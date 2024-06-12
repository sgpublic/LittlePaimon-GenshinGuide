package io.github.sgpublic.genshinguide.action

import io.github.sgpublic.kotlin.util.Loggable
import io.github.sgpublic.genshinguide.Config
import io.github.sgpublic.genshinguide.action.rss.GuideRSS
import io.github.sgpublic.genshinguide.action.rss.GuideRSS.GuideItem
import io.github.sgpublic.genshinguide.action.rss.RoleRSS
import io.github.sgpublic.genshinguide.action.task.GuideAction
import io.github.sgpublic.genshinguide.action.task.GuideInfo
import io.github.sgpublic.genshinguide.action.task.RepoAction
import io.github.sgpublic.genshinguide.core.AbsConfig
import org.quartz.Job
import org.quartz.JobExecutionContext
import java.util.*

class Action: Job {
    private val roleRss: EnumMap<AbsConfig.Element, LinkedList<RoleRSS.RoleItem>>? by RoleRSS
    private val guideRss: EnumMap<AbsConfig.Element, LinkedList<GuideItem>>? by GuideRSS

    fun realExecute(force: Boolean = false) {
        try {
            if (Config.repos.isEmpty()) {
                log.warn("没有配置仓库信息，跳过此次更新。")
                return
            }

            val roleRss = roleRss ?: return
            val guideRss = guideRss ?: return

            try {
                GuideAction.create(
                    Config.repos.map {
                        log.debug("检查仓库：${it.key}")
                        RepoAction.of(it.key, force, it.value)
                    }
                )
            } catch (e: Exception) {
                log.error("仓库检查出错！", e)
                return
            }.use { actions ->
                for ((element, roles) in roleRss) {
                    for (role in roles) {
                        log.debug("寻找角色攻略：$role")

                        val existGuide = guideRss[element]?.find {
                            val realName = if (role.name.startsWith("旅行者")) {
                                role.name.replace(" ", "")
                                    .replace("(", "")
                                    .replace("（", "")
                                    .replace(")", "")
                                    .replace("）", "")
                            } else {
                                role.name
                            }
                            it.subject.contains(realName)
                        }

                        if (existGuide == null) {
                            log.warn("没有找到角色 ${role.name} 的攻略")
                            continue
                        }

                        val target = GuideInfo(role, existGuide)
                        try {
                            if (!force && !actions.needUpdate(target)) {
                                continue
                            }
                            log.info("开始导出攻略：${role.name}")
                            actions.saveGuide(target)
                        } catch (e: Exception) {
                            log.warn("攻略导出失败：${role.name}", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            log.error("未捕获的错误！", e)
        } finally {
            log.info("更新结束")
        }
    }

    override fun execute(context: JobExecutionContext?) {
        realExecute()
    }

    companion object: Loggable
}