package com.rotd.kejasafe.data.repoImpl

import com.rotd.kejasafe.data.UserProfileDataSource
import com.rotd.kejasafe.domain.repository.AuthRepository

expect fun createAuthRepository(profiles: UserProfileDataSource): AuthRepository
