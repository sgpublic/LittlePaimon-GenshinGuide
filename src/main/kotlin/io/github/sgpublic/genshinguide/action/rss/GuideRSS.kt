package io.github.sgpublic.genshinguide.action.rss

import com.dtflys.forest.Forest
import com.dtflys.forest.callback.RetryWhen
import com.dtflys.forest.http.ForestRequest
import com.dtflys.forest.http.ForestResponse
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import io.github.sgpublic.kotlin.core.util.fromGson
import io.github.sgpublic.kotlin.util.log
import io.github.sgpublic.genshinguide.Config
import io.github.sgpublic.genshinguide.action.rss.RoleRSS.RoleItem
import io.github.sgpublic.genshinguide.core.AbsConfig
import kotlinx.serialization.Serializable
import java.util.*
import kotlin.reflect.KProperty


object GuideRSS: RetryWhen {
    override fun retryWhen(req: ForestRequest<*>?, res: ForestResponse<*>?): Boolean {
        val content = res?.content ?: return false
        return JsonObject::class.fromGson(content).get("retcode").asInt != 0
    }

    operator fun getValue(thisRef: Any, prop: KProperty<*>): EnumMap<AbsConfig.Element, LinkedList<GuideItem>>? {
        try {
            val result = EnumMap<AbsConfig.Element, LinkedList<GuideItem>>(AbsConfig.Element::class.java)

            for (element in AbsConfig.Element.entries) {
                val json = Forest.request(String::class.java)
                    .url(element.rss)
                    .retryWhen(this)
                    .maxRetryCount(5)
                    .maxRetryInterval(10_000)
                    .sync()
                    .execute(String::class.java)

                val list = JsonObject::class.fromGson(json)
                    .getAsJsonObject("data")
                    .getAsJsonArray("posts")

                for (item in list) {
                    item.asJsonObject.run {
                        val post = getAsJsonObject("post")
                        val subject = post.get("subject").asString
                        val updatedAt = post.get("updated_at").asLong
                        val image = GuideItem.Image::class.fromGson(
                            getAsJsonArray("image_list")
                                .get(0).asJsonObject
                        )
                        result[element] = result.getOrDefault(element, LinkedList()).also {
                            it.add(GuideItem(
                                updatedAt = updatedAt,
                                subject = subject,
                                element = element,
                                image = image,
                            ))
                        }
                    }
                }
            }

            return result
        } catch (e: Exception) {
            log.warn("角色攻略列表获取失败", e)
            return null
        }
    }

    @Serializable
    data class GuideItem(
        /** 1717211745 */
        @SerializedName("updated_at")
        val updatedAt: Long,
        /** 【千织】武器圣遗物配队详解+视频(培养图鉴) */
        @SerializedName("subject")
        val subject: String,
        /** Pyro */
        @SerializedName("element")
        val element: AbsConfig.Element,
        @SerializedName("image")
        val image: Image,
    ): Comparable<GuideItem> {
        override fun compareTo(other: GuideItem): Int {
            return updatedAt.compareTo(other.updatedAt)
        }

        @Serializable
        data class Image(
            @SerializedName("url")
            val url: String,
            @SerializedName("height")
            val height: Int,
            @SerializedName("width")
            val width: Int,
            @SerializedName("size")
            val size: Int,
            @SerializedName("image_id")
            val imageId: String,
        )
    }
}