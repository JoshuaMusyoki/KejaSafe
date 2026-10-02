package com.rotd.havenpad.data

import com.rotd.havenpad.CommunityStory
import com.rotd.havenpad.PlatformStats
import com.rotd.havenpad.PostStoryRequest
import com.rotd.havenpad.network.CommunityClient
import com.rotd.havenpad.storage.CacheKeys
import com.rotd.havenpad.storage.JsonCache

class CommunityRepository {
    suspend fun getStories(
        limit: Int = 12,
        onCacheHit: ((List<CommunityStory>) -> Unit)? = null
    ): Result<OfflineData<List<CommunityStory>>> =
        offlineFirst(
            readCache = { JsonCache.getList(CacheKeys.COMMUNITY_STORIES, CommunityStory.serializer()) },
            writeCache = { list ->
                JsonCache.putList(CacheKeys.COMMUNITY_STORIES, list, CommunityStory.serializer())
            },
            fetch = { CommunityClient.getStories(limit) },
            onCacheHit = onCacheHit
        )

    suspend fun getMyStories(): Result<List<CommunityStory>> = CommunityClient.getMyStories()

    suspend fun postStory(request: PostStoryRequest): Result<CommunityStory> =
        CommunityClient.postStory(request).onSuccess { story ->
            // Show the new story straight away on the next cache read.
            val current = JsonCache.getList(CacheKeys.COMMUNITY_STORIES, CommunityStory.serializer())
                .orEmpty()
                .filter { it.id != story.id }
            JsonCache.putList(
                CacheKeys.COMMUNITY_STORIES,
                listOf(story) + current,
                CommunityStory.serializer()
            )
        }

    suspend fun deleteStory(storyId: String): Result<Unit> =
        CommunityClient.deleteStory(storyId).onSuccess {
            val remaining = JsonCache.getList(CacheKeys.COMMUNITY_STORIES, CommunityStory.serializer())
                .orEmpty()
                .filter { it.id != storyId }
            JsonCache.putList(CacheKeys.COMMUNITY_STORIES, remaining, CommunityStory.serializer())
        }

    suspend fun getStats(
        onCacheHit: ((PlatformStats) -> Unit)? = null
    ): Result<OfflineData<PlatformStats>> =
        offlineFirst(
            readCache = { JsonCache.get(CacheKeys.PLATFORM_STATS, PlatformStats.serializer()) },
            writeCache = { JsonCache.put(CacheKeys.PLATFORM_STATS, it, PlatformStats.serializer()) },
            fetch = { CommunityClient.getStats() },
            onCacheHit = onCacheHit
        )
}
