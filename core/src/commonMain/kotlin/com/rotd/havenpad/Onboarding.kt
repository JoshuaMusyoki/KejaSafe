package com.rotd.havenpad

/**
 * Role-specific details collected before an account is created.
 * Student and landlord accounts stay unverified until document KYC.
 * Agent accounts are submitted for review and are not auto-verified.
 */
fun RegistrationRequest.onboardingError(): String? {
    if (role == UserRole.ADMIN) return "Choose student, landlord, or agent."
    if (fullName.isBlank() || username.isBlank() || email.isBlank()) {
        return "Name, username, and email are required."
    }
    if (!email.contains("@") || email.substringAfter("@").isBlank()) {
        return "Enter a valid email."
    }
    if (password.length < 8) return "Password must be at least 8 characters."
    if (phone.isBlank()) return "Phone number is required."
    return when (role) {
        UserRole.STUDENT -> when {
            university.isBlank() -> "University is required."
            studentNumber.isBlank() -> "Student number is required."
            major.isBlank() -> "Major is required."
            yearOfStudy == null || yearOfStudy !in 1..8 -> "Year of study is required."
            else -> null
        }
        UserRole.LANDLORD -> when {
            businessName.isBlank() -> "Property or business name is required."
            residentialAddress.isBlank() -> "Residential address is required."
            else -> null
        }
        UserRole.AGENT -> when {
            businessName.isBlank() -> "Agency name is required."
            licenseNumber.isBlank() -> "Agent licence number is required."
            serviceArea.isBlank() -> "Service area is required."
            nationalIdNumber.isBlank() -> "National ID number is required."
            else -> null
        }
        UserRole.ADMIN -> "Choose student, landlord, or agent."
    }
}
