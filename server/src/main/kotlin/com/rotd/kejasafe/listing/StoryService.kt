package com.rotd.kejasafe.listing

import com.rotd.kejasafe.CommunityStory
import com.rotd.kejasafe.PostStoryRequest
import com.rotd.kejasafe.UserRole
import com.rotd.kejasafe.db.CommunityStoriesTable
import com.rotd.kejasafe.db.UsersTable
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

object StoryService {
    const val MIN_CONTENT_LENGTH = 40
    const val MAX_CONTENT_LENGTH = 600
    private const val DEFAULT_LIMIT = 12
    private const val MAX_LIMIT = 50

    /** Newest stories written by verified students and landlords. Public feed. */
    fun getStories(limit: Int = DEFAULT_LIMIT): List<CommunityStory> = transaction {
        (CommunityStoriesTable innerJoin UsersTable)
            .selectAll()
            .orderBy(CommunityStoriesTable.createdAt to SortOrder.DESC)
            .limit(limit.coerceIn(1, MAX_LIMIT))
            .map { it.toStory() }
    }

    fun getStoriesByAuthor(userId: String): List<CommunityStory> = transaction {
        (CommunityStoriesTable innerJoin UsersTable)
            .selectAll()
            .where { CommunityStoriesTable.authorId eq userId }
            .orderBy(CommunityStoriesTable.createdAt to SortOrder.DESC)
            .map { it.toStory() }
    }

    /**
     * Publishes a story for [userId]. Only students and landlords can post — admins
     * moderate rather than testify.
     */
    fun postStory(userId: String, request: PostStoryRequest): CommunityStory = transaction {
        val author = UsersTable.selectAll()
            .where { UsersTable.id eq userId }
            .singleOrNull()
            ?: throw IllegalArgumentException("Sign in again to share your story")

        val role = author[UsersTable.role]
        require(role == UserRole.STUDENT || role == UserRole.LANDLORD) {
            "Only students and landlords can post community stories"
        }

        val content = request.content.trim()
        require(content.length >= MIN_CONTENT_LENGTH) {
            "Tell us a bit more — at least $MIN_CONTENT_LENGTH characters"
        }
        require(content.length <= MAX_CONTENT_LENGTH) {
            "Stories are capped at $MAX_CONTENT_LENGTH characters"
        }

        val rating = request.rating.coerceIn(1, 5)
        val id = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        CommunityStoriesTable.insert {
            it[CommunityStoriesTable.id] = id
            it[CommunityStoriesTable.authorId] = userId
            it[CommunityStoriesTable.content] = content
            it[CommunityStoriesTable.rating] = rating
            it[CommunityStoriesTable.createdAt] = timestamp
        }

        CommunityStory(
            id = id,
            authorId = userId,
            authorName = author[UsersTable.fullName],
            authorRole = role,
            authorProfilePictureUrl = author[UsersTable.profilePictureUrl],
            content = content,
            rating = rating,
            createdAt = timestamp
        )
    }

    /** Authors can retract their own story. Returns false when it is not theirs. */
    fun deleteStory(storyId: String, userId: String): Boolean = transaction {
        CommunityStoriesTable.deleteWhere {
            (CommunityStoriesTable.id eq storyId) and (CommunityStoriesTable.authorId eq userId)
        } > 0
    }

    private fun ResultRow.toStory() = CommunityStory(
        id = this[CommunityStoriesTable.id],
        authorId = this[CommunityStoriesTable.authorId],
        authorName = this[UsersTable.fullName],
        authorRole = this[UsersTable.role],
        authorProfilePictureUrl = this[UsersTable.profilePictureUrl],
        content = this[CommunityStoriesTable.content],
        rating = this[CommunityStoriesTable.rating],
        createdAt = this[CommunityStoriesTable.createdAt]
    )
}
