package com.rotd.havenpad.web

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rotd.havenpad.CommunityStory
import com.rotd.havenpad.PlatformStats
import com.rotd.havenpad.PostStoryRequest
import com.rotd.havenpad.PropertyListing
import com.rotd.havenpad.User
import com.rotd.havenpad.UserRole
import com.rotd.havenpad.data.CommunityRepository
import com.rotd.havenpad.data.ListingRepository
import com.rotd.havenpad.error.userMessage
import com.rotd.havenpad.network.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Everything the public landing page shows, all of it fetched from the API. */
data class LandingUiState(
    val stats: PlatformStats? = null,
    val listings: List<PropertyListing> = emptyList(),
    val stories: List<CommunityStory> = emptyList(),
    val loadingListings: Boolean = true,
    val loadingStories: Boolean = true,
    val listingsError: String? = null,
    val storiesError: String? = null
)

sealed interface StoryPostState {
    data object Idle : StoryPostState
    data object Submitting : StoryPostState
    data object Posted : StoryPostState
    data class Failed(val message: String) : StoryPostState
}

class LandingViewModel(
    private val listingRepository: ListingRepository,
    private val communityRepository: CommunityRepository
) : ViewModel() {
    private val _state = MutableStateFlow(LandingUiState())
    val state: StateFlow<LandingUiState> = _state.asStateFlow()

    private val _postState = MutableStateFlow<StoryPostState>(StoryPostState.Idle)
    val postState: StateFlow<StoryPostState> = _postState.asStateFlow()

    val currentUser: StateFlow<User?> = SessionManager.currentUser

    init {
        refresh()
    }

    fun refresh() {
        loadStats()
        loadListings()
        loadStories()
    }

    private fun loadStats() {
        viewModelScope.launch {
            communityRepository.getStats(
                onCacheHit = { cached -> _state.value = _state.value.copy(stats = cached) }
            ).onSuccess { data ->
                _state.value = _state.value.copy(stats = data.value)
            }
        }
    }

    private fun loadListings() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loadingListings = true, listingsError = null)
            listingRepository.getBrowseListings(
                onCacheHit = { cached ->
                    _state.value = _state.value.copy(listings = cached, loadingListings = false)
                }
            ).fold(
                onSuccess = { data ->
                    _state.value = _state.value.copy(
                        listings = data.value,
                        loadingListings = false,
                        listingsError = null
                    )
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        loadingListings = false,
                        listingsError = error.userMessage()
                    )
                }
            )
        }
    }

    private fun loadStories() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loadingStories = true, storiesError = null)
            communityRepository.getStories(
                limit = STORY_FEED_LIMIT,
                onCacheHit = { cached ->
                    _state.value = _state.value.copy(stories = cached, loadingStories = false)
                }
            ).fold(
                onSuccess = { data ->
                    _state.value = _state.value.copy(
                        stories = data.value,
                        loadingStories = false,
                        storiesError = null
                    )
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        loadingStories = false,
                        storiesError = error.userMessage()
                    )
                }
            )
        }
    }

    /** Students and landlords publish their own experience; the feed refreshes on success. */
    fun postStory(content: String, rating: Int) {
        val trimmed = content.trim()
        if (trimmed.length < MIN_STORY_LENGTH) {
            _postState.value = StoryPostState.Failed(
                "Tell us a bit more — at least $MIN_STORY_LENGTH characters."
            )
            return
        }

        viewModelScope.launch {
            _postState.value = StoryPostState.Submitting
            communityRepository.postStory(PostStoryRequest(content = trimmed, rating = rating)).fold(
                onSuccess = { story ->
                    _state.value = _state.value.copy(
                        stories = listOf(story) + _state.value.stories.filter { it.id != story.id },
                        storiesError = null
                    )
                    _postState.value = StoryPostState.Posted
                    loadStats()
                },
                onFailure = { error ->
                    _postState.value = StoryPostState.Failed(error.userMessage())
                }
            )
        }
    }

    fun deleteStory(storyId: String) {
        viewModelScope.launch {
            communityRepository.deleteStory(storyId).onSuccess {
                _state.value = _state.value.copy(
                    stories = _state.value.stories.filterNot { it.id == storyId }
                )
                loadStats()
            }
        }
    }

    fun consumePostState() {
        _postState.value = StoryPostState.Idle
    }

    companion object {
        const val MIN_STORY_LENGTH = 40
        const val MAX_STORY_LENGTH = 600
        private const val STORY_FEED_LIMIT = 12
    }
}

/** Admins moderate rather than testify, matching the server rule. */
fun User?.canPostStory(): Boolean =
    this != null && (role == UserRole.STUDENT || role == UserRole.LANDLORD)
