package io.github.sgpublic.genshinguide.action.task

import com.google.gson.annotations.SerializedName
import io.github.sgpublic.kotlin.core.util.fromGson
import io.github.sgpublic.kotlin.core.util.toGson
import io.github.sgpublic.kotlin.util.log
import io.github.sgpublic.genshinguide.Config
import io.github.sgpublic.genshinguide.action.rss.GuideRSS
import io.github.sgpublic.genshinguide.action.rss.RoleRSS
import io.github.sgpublic.genshinguide.core.AbsConfig
import io.github.sgpublic.genshinguide.core.applyAuth
import io.github.sgpublic.genshinguide.core.util.checkSum
import io.github.sgpublic.genshinguide.core.util.getSum
import org.eclipse.jgit.api.CommitCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.errors.RepositoryNotFoundException
import java.io.File

/**
 * @author sgpublic
 * @Date 2023/7/29 14:56
 */
interface RepoAction: AutoCloseable {
    val id: String

    fun needUpdate(info: GuideInfo): Boolean
    fun saveGuide(file: File, info: GuideInfo)

    companion object {
        fun of(id: String, force: Boolean, repo: AbsConfig.Repository): RepoAction {
            return RepoActionImpl(id, force, repo)
        }
    }
}

class RepoActionImpl internal constructor(
    override val id: String,
    force: Boolean,
    private val repo: AbsConfig.Repository
): RepoAction {
    private val tempDir: File = File(Config.tempDir, "git/$id")
    private val repository: File = File(tempDir, "repository")
    private val repositoryGit = checkout(repo, repository, repo.branch)

    init {
        if (force) {
            File(repository, "plugins").deleteRecursively()
        }
    }

    private fun checkout(repo: AbsConfig.Repository, target: File, branch: String? = null): Git {
        val open: Git = try {
            Git.open(target).also { git ->
                val remote = git.repository.config.getString("remote", "origin", "url")
                if (remote != repo.gitUrl) {
                    git.close()
                    throw IllegalStateException("此目录不是目标仓库，而是：$remote")
                }
                if (branch != null) {
                    git.checkout()
                        .setName(branch)
                        .setStartPoint("origin/$branch")
                        .call()
                }
                git.fetch().call()
                git.reset().setRef("HEAD").setMode(ResetCommand.ResetType.HARD).call()
            }
        } catch (e: Exception) {
            when (e) {
                is RepositoryNotFoundException -> {
                    log.debug("仓库 $id（${repo.gitUrl}）不存在，重新 clone")
                }
                is IllegalStateException -> {
                    log.warn("目标仓库不匹配，重新 clone")
                }
                else -> {
                    log.warn("仓库 $id（${repo.gitUrl}）检查失败，重新 clone")
                    log.debug("错误信息：${e.message}", e)
                }
            }
            target.deleteRecursively()
            target.mkdirs()
            Git.cloneRepository()
                .setURI(repo.gitUrl)
                .setDirectory(target)
                .also {
                    if (branch != null) {
                        it.setBranch(branch)
                    }
                }
                .applyAuth(repo.auth)
                .call()
        }
        return open
    }

    private val filePrefix = "./genshin_guide/guide"
    override fun needUpdate(info: GuideInfo): Boolean {
        val root = File(repository, "$filePrefix/")
        val guide = File(root, "./${info.role.name}.png")
        val json = File(root, "./${info.role.name}.json")
        if (!guide.exists() || !json.exists()) {
            return true
        }
        val existInfo = GuideInfo::class.fromGson(json.readText())
        if (!guide.checkSum(existInfo.sum)) {
            return true
        }
        return existInfo.guide == info.guide && existInfo.role == info.role
    }

    override fun saveGuide(file: File, info: GuideInfo) {
        val root = File(repository, "$filePrefix/")
        val guide = File(root, "./${info.role.name}.png")
        val json = File(root, "./${info.role.name}.json")
        info.sum = file.getSum()
        file.copyTo(guide, true)
        json.writeText(info.toGson())
    }

    override fun close() {
        repositoryGit.autoClose()
    }

    private fun Git.autoClose() {
        add().addFilepattern(".").call()
        if (status().call().hasUncommittedChanges()) {
            commit().applyAuthor().setMessage("auto update").call()
        }
        push().setForce(true).call()
        close()
    }

    private fun CommitCommand.applyAuthor() = apply {
        Config.vcs?.let { vcs ->
            setAuthor(vcs.name, vcs.email)
        }
    }
}

data class GuideInfo(
    @SerializedName("role")
    val role: RoleRSS.RoleItem,
    @SerializedName("guide")
    val guide: GuideRSS.GuideItem,
    @SerializedName("sum")
    var sum: String? = null,
)