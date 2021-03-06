package app.luisramos.ler.domain

import app.luisramos.ler.data.model.FeedModel
import app.luisramos.ler.data.model.FeedUpdateMode
import java.io.IOException
import java.util.*

interface SaveFeedUseCase {
    suspend fun saveFeed(feedModel: FeedModel): Result<Boolean>
}

class DefaultSaveFeedUseCase(
    private val db: Db
) : SaveFeedUseCase {
    override suspend fun saveFeed(feedModel: FeedModel): Result<Boolean> {
        val (title, link) = feedModel

        if (title.isEmpty() || link.isEmpty()) {
            return Result.failure(IOException("No title or link for channel"))
        }

        val feed = db.findFeedByUpdateLink(feedModel.feedLink)

        val feedId = if (feed != null) {
            db.updateFeed(
                id = feed.id,
                title = title,
                link = link,
                description = feedModel.description,
                updateLink = feedModel.feedLink,
                updateMode = FeedUpdateMode.NONE
            )
        } else {
            db.insertFeed(
                title = title,
                link = link,
                description = feedModel.description,
                updateLink = feedModel.feedLink,
                updateMode = FeedUpdateMode.NONE,
                updatedAt = feedModel.updated
            )
        }

        val idLinkPairs = db.findFeedItemsIdsByFeedId(feedId)
        val idMap: Map<String, Long> = idLinkPairs.fold(mutableMapOf()) { acc, value ->
            acc[value.link] = value.id
            acc
        }
        val links = idLinkPairs.map { it.link }

        val itemsToUpdate = feedModel.items.filter { links.contains(it.link) }
        val itemsToInsert = feedModel.items - itemsToUpdate

        itemsToInsert.forEach {
            db.insertFeedItem(
                title = it.title,
                description = it.description,
                link = it.link,
                publishedAt = it.published,
                updatedAt = it.updated ?: Date(),
                feedId = feedId
            )
        }
        itemsToUpdate.forEach {
            idMap[it.link]?.let { id ->
                db.updateFeedItem(
                    id = id,
                    title = it.title,
                    description = it.description,
                    link = it.link,
                    publishedAt = it.published,
                    updatedAt = it.updated ?: Date()
                )
            }
        }

        return Result.success(true)
    }
}

class FakeSaveFeedUseCase: SaveFeedUseCase {
    var mockResult = Result.success(true)
    override suspend fun saveFeed(feedModel: FeedModel): Result<Boolean> = mockResult
}