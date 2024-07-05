package io.github.sgpublic.genshinguide.action.rss

import com.dtflys.forest.callback.RetryWhen
import com.dtflys.forest.http.ForestRequest
import com.dtflys.forest.http.ForestResponse
import com.google.gson.annotations.SerializedName
import io.github.sgpublic.kotlin.util.log
import io.github.sgpublic.genshinguide.Config
import io.github.sgpublic.genshinguide.core.AbsConfig
import kotlinx.serialization.Serializable
import org.htmlunit.BrowserVersion
import org.htmlunit.WebClient
import org.htmlunit.html.HtmlPage
import org.htmlunit.html.HtmlSpan
import java.util.EnumMap
import java.util.LinkedList
import kotlin.reflect.KProperty


object RoleRSS: RetryWhen {
    override fun retryWhen(req: ForestRequest<*>?, res: ForestResponse<*>?): Boolean {
        return false
    }

    private val client: WebClient = WebClient(BrowserVersion.CHROME).apply {
        options.isThrowExceptionOnScriptError = false
        options.isJavaScriptEnabled = false
        options.isCssEnabled = false
    }

    private val rarityReg = "genshin-bg-rarity-\\d".toRegex()
    operator fun getValue(thisRef: Any, prop: KProperty<*>): EnumMap<AbsConfig.Element, LinkedList<RoleItem>>? {
        try {
            val result = EnumMap<AbsConfig.Element, LinkedList<RoleItem>>(AbsConfig.Element::class.java)
            val page = client.getPage<HtmlPage>(Config.roleRss)
            for (a in page.getElementsByTagName("a")) {
                if (!a.hasAttribute("href")) {
                    continue
                }
                val href = a.getAttribute("href")
                if (!href.startsWith("/cn/character/")) {
                    continue
                }
                val id = href.substring(14)
                val name: String = try {
                    val spans = a.querySelectorAll(".text-white")
                    var name: String? = null
                    for (span in spans) {
                        if (span !is HtmlSpan) {
                            continue
                        }
                        if (span.getAttribute("class") != "text-white") {
                            continue
                        }
                        name = span.textContent
                        break
                    }
                    name ?: throw IllegalArgumentException("找不到 .text-white")
                } catch (e: Exception) {
                    log.warn("角色名称获取失败：$href")
                    continue
                }
                val rarity = try {
                    val clazz = a.getElementsByTagName("div")
                        .firstOrNull()?.getAttribute("class")
                        ?.takeIf { it.contains(rarityReg) }!!
                    rarityReg.find(clazz)
                        ?.value
                        ?.substring(18)
                        ?.toIntOrNull()!!
                } catch (e: Exception) {
                    log.warn("角色星级获取失败：$href")
                    continue
                }
                val element = try {
                    a.getElementsByTagName("img")
                        .find { it.hasAttribute("alt") && it.getAttribute("alt") != id }
                        ?.getAttribute("alt")?.let {
                            return@let AbsConfig.Element.valueOf(it)
                        }!!
                } catch (e: Exception) {
                    log.warn("角元素获取失败：$href")
                    continue
                }
                log.debug("识别到角色：$name")
                result[element] = result.getOrDefault(element, LinkedList()).also {
                    it.add(RoleItem(
                        rarity = rarity,
                        name = name,
                        element = element,
                    ))
                }
            }

            return result
        } catch (e: Exception) {
            log.warn("角色列表获取失败", e)
            return null
        }
    }

    @Serializable
    data class RoleItem(
        /** 4 */
        @SerializedName("rarity")
        val rarity: Int,
        /** 千织 */
        @SerializedName("name")
        val name: String,
        /** Pyro */
        @SerializedName("element")
        val element: AbsConfig.Element,
    ): Comparable<RoleItem> {
        override fun compareTo(other: RoleItem): Int {
            return name.compareTo(other.name)
        }
    }
}