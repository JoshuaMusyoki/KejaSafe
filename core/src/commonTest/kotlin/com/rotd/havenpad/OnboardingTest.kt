package com.rotd.havenpad

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OnboardingTest {
    private fun base(role: UserRole) = RegistrationRequest(
        email = "amina@havenpad.app",
        username = "amina",
        fullName = "Amina Otieno",
        password = "placeholder_secure_pass_123",
        role = role,
        phone = "0712345678",
    )

    @Test
    fun studentNeedsUniversityDetails() {
        val missing = base(UserRole.STUDENT)
        assertEquals("University is required.", missing.onboardingError())
        val complete = missing.copy(
            university = "University of Nairobi",
            studentNumber = "P15/1234/2022",
            major = "Computer Science",
            yearOfStudy = 3,
        )
        println("Testing student complete details error: ${complete.onboardingError()}")
        assertNull(complete.onboardingError())
    }

    @Test
    fun landlordNeedsIdentityAndAddress() {
        val missing = base(UserRole.LANDLORD)
        assertEquals("Property or business name is required.", missing.onboardingError())
        val complete = missing.copy(
            businessName = "Joymat Homes",
            residentialAddress = "Kasarani, Nairobi",
        )
        assertNull(complete.onboardingError())
    }

    @Test
    fun agentNeedsLicenceAndArea() {
        val missing = base(UserRole.AGENT).copy(
            businessName = "Campus Keys",
            licenseNumber = "EAL-204",
            nationalIdNumber = "12345678",
        )
        assertEquals("Service area is required.", missing.onboardingError())
        assertNull(missing.copy(serviceArea = "Rongai").onboardingError())
        assertEquals("Choose student, landlord, or agent.", base(UserRole.ADMIN).onboardingError())
    }
}
