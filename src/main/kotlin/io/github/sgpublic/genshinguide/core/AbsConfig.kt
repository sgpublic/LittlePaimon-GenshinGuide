package io.github.sgpublic.genshinguide.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.eclipse.jgit.api.GitCommand
import org.eclipse.jgit.api.TransportCommand
import org.eclipse.jgit.transport.*

@Serializable
data class AbsConfig(
    @SerialName("debug")
    val debug: Boolean = false,
    @SerialName("cron")
    val cron: String = "0 0 2 * * ?",
    @SerialName("temp-dir")
    val tempDir: String = "/tmp/little-paimon-genshin-guide",
    @SerialName("log-dir")
    val logDir: String = "/var/log/little-paimon-genshin-guide",
    @SerialName("role-rss")
    val roleRss: String = "https://genshin-builds.com/cn/characters",
    @SerialName("guide-rss")
    val guideRss: GuideRSS = GuideRSS(),
    @SerialName("download-retry")
    val downloadRetry: Int = 3,
    @SerialName("repos")
    val repos: Map<String, Repository> = mapOf(),
    @SerialName("vcs")
    val vcs: Vcs? = null,
) {
    @Serializable
    enum class Element(
        private val rssUrl: () -> String
    ) {
        Pyro({ io.github.sgpublic.genshinguide.Config.guideRss.pyro }),
        Hydro({ io.github.sgpublic.genshinguide.Config.guideRss.hydro }),
        Anemo({ io.github.sgpublic.genshinguide.Config.guideRss.anemo }),
        Electro({ io.github.sgpublic.genshinguide.Config.guideRss.electro }),
        Dendro({ io.github.sgpublic.genshinguide.Config.guideRss.dendro }),
        Cryo({ io.github.sgpublic.genshinguide.Config.guideRss.cryo }),
        Geo({ io.github.sgpublic.genshinguide.Config.guideRss.geo }),
        Default({ io.github.sgpublic.genshinguide.Config.guideRss.default }),
        ;

        val rss: String by lazy { rssUrl() }
    }

    @Serializable
    data class GuideRSS(
        // 火
        @SerialName("pyro")
        val pyro: String = "https://bbs-api.mihoyo.com/post/wapi/getPostFullInCollection?gids=2&order_type=2&collection_id=2319292",
        // 水
        @SerialName("hydro")
        val hydro: String = "https://bbs-api.mihoyo.com/post/wapi/getPostFullInCollection?gids=2&order_type=2&collection_id=2319293",
        // 风
        @SerialName("anemo")
        val anemo: String = "https://bbs-api.mihoyo.com/post/wapi/getPostFullInCollection?gids=2&order_type=2&collection_id=2319295",
        // 雷
        @SerialName("electro")
        val electro: String = "https://bbs-api.mihoyo.com/post/wapi/getPostFullInCollection?gids=2&order_type=2&collection_id=2319296",
        // 草
        @SerialName("dendro")
        val dendro: String = "https://bbs-api.mihoyo.com/post/wapi/getPostFullInCollection?gids=2&order_type=2&collection_id=2319299",
        // 冰
        @SerialName("cryo")
        val cryo: String = "https://bbs-api.mihoyo.com/post/wapi/getPostFullInCollection?gids=2&order_type=2&collection_id=2319294",
        // 岩
        @SerialName("geo")
        val geo: String = "https://bbs-api.mihoyo.com/post/wapi/getPostFullInCollection?gids=2&order_type=2&collection_id=2319298",
        // 默认合集
        @SerialName("default")
        val default: String = "https://bbs-api.mihoyo.com/post/wapi/getPostFullInCollection?gids=2&order_type=2&collection_id=642956",
    )

    @Serializable
    data class Repository(
        @SerialName("branch")
        val branch: String,
        @SerialName("git-url")
        val gitUrl: String,
        @SerialName("auth")
        val auth: PasswordAuth? = null,
    ) {
        @Serializable
        data class PasswordAuth(
            @SerialName("username")
            val username: String,
            @SerialName("token")
            val token: String,
        )
    }

    @Serializable
    data class Vcs(
        @SerialName("name")
        val name: String,
        @SerialName("email")
        val email: String,
    )
}

fun <T: GitCommand<R>, R> TransportCommand<T, R>.applyAuth(auth: AbsConfig.Repository.PasswordAuth?): T {
    if (auth != null) {
        setCredentialsProvider(
            UsernamePasswordCredentialsProvider(auth.username, auth.token)
        )
    }
    @Suppress("UNCHECKED_CAST")
    return this as T
}