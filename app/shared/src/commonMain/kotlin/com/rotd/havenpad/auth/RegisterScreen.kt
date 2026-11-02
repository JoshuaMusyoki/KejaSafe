package com.rotd.havenpad.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rotd.havenpad.UserRole
import com.rotd.havenpad.components.HavenPadBody
import com.rotd.havenpad.components.HavenPadHeader
import com.rotd.havenpad.components.HavenPadHeadline
import com.rotd.havenpad.components.HavenPadPrimaryButton
import com.rotd.havenpad.components.HavenPadTextField
import com.rotd.havenpad.network.SessionManager
import com.rotd.havenpad.theme.HavenPadTheme
import com.rotd.havenpad.theme.ScreenPreviews
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RegisterScreen(
    role: UserRole,
    onNavigateToLogin: () -> Unit,
    onRegistrationSuccess: () -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: RegisterViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val fullName by viewModel.fullName.collectAsStateWithLifecycle()
    val username by viewModel.username.collectAsStateWithLifecycle()
    val email by viewModel.email.collectAsStateWithLifecycle()
    val phone by viewModel.phone.collectAsStateWithLifecycle()
    val password by viewModel.password.collectAsStateWithLifecycle()
    val confirmPassword by viewModel.confirmPassword.collectAsStateWithLifecycle()
    val university by viewModel.university.collectAsStateWithLifecycle()
    val studentNumber by viewModel.studentNumber.collectAsStateWithLifecycle()
    val major by viewModel.major.collectAsStateWithLifecycle()
    val yearOfStudy by viewModel.yearOfStudy.collectAsStateWithLifecycle()
    val businessName by viewModel.businessName.collectAsStateWithLifecycle()
    val nationalIdNumber by viewModel.nationalIdNumber.collectAsStateWithLifecycle()
    val residentialAddress by viewModel.residentialAddress.collectAsStateWithLifecycle()
    val licenseNumber by viewModel.licenseNumber.collectAsStateWithLifecycle()
    val serviceArea by viewModel.serviceArea.collectAsStateWithLifecycle()

    LaunchedEffect(role) {
        viewModel.prepare(role)
    }

    RegisterScreenContent(
        state = state,
        role = role,
        fullName = fullName,
        username = username,
        email = email,
        phone = phone,
        password = password,
        confirmPassword = confirmPassword,
        university = university,
        studentNumber = studentNumber,
        major = major,
        yearOfStudy = yearOfStudy,
        businessName = businessName,
        nationalIdNumber = nationalIdNumber,
        residentialAddress = residentialAddress,
        licenseNumber = licenseNumber,
        serviceArea = serviceArea,
        onNavigateToLogin = onNavigateToLogin,
        onBack = onBack,
        onFullNameChange = { viewModel.fullName.value = it },
        onUsernameChange = { viewModel.username.value = it },
        onEmailChange = { viewModel.email.value = it },
        onPhoneChange = { viewModel.phone.value = it },
        onPasswordChange = { viewModel.password.value = it },
        onConfirmPasswordChange = { viewModel.confirmPassword.value = it },
        onUniversityChange = { viewModel.university.value = it },
        onStudentNumberChange = { viewModel.studentNumber.value = it },
        onMajorChange = { viewModel.major.value = it },
        onYearChange = { viewModel.yearOfStudy.value = it.filter(Char::isDigit).take(1) },
        onBusinessNameChange = { viewModel.businessName.value = it },
        onNationalIdChange = { viewModel.nationalIdNumber.value = it },
        onAddressChange = { viewModel.residentialAddress.value = it },
        onLicenseChange = { viewModel.licenseNumber.value = it },
        onServiceAreaChange = { viewModel.serviceArea.value = it },
        onRegister = { viewModel.register() }
    )

    LaunchedEffect(state) {
        if (state is RegisterState.Success) {
            if (SessionManager.isLogged()) {
                onRegistrationSuccess()
            }
            viewModel.consumeSuccess()
        }
    }
}

private enum class RegisterStep {
    Name,
    Contact,
    Identity,
    Study,
    Password,
}

private val registerSteps = listOf(
    RegisterStep.Name,
    RegisterStep.Contact,
    RegisterStep.Identity,
    RegisterStep.Study,
    RegisterStep.Password,
)

@Composable
private fun RegisterScreenContent(
    state: RegisterState,
    role: UserRole,
    fullName: String,
    username: String,
    email: String,
    phone: String,
    password: String,
    confirmPassword: String,
    university: String,
    studentNumber: String,
    major: String,
    yearOfStudy: String,
    businessName: String,
    nationalIdNumber: String,
    residentialAddress: String,
    licenseNumber: String,
    serviceArea: String,
    onNavigateToLogin: () -> Unit,
    onBack: (() -> Unit)?,
    onFullNameChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onUniversityChange: (String) -> Unit,
    onStudentNumberChange: (String) -> Unit,
    onMajorChange: (String) -> Unit,
    onYearChange: (String) -> Unit,
    onBusinessNameChange: (String) -> Unit,
    onNationalIdChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onLicenseChange: (String) -> Unit,
    onServiceAreaChange: (String) -> Unit,
    onRegister: () -> Unit
) {
    val steps = registerSteps
    var stepIndex by remember { mutableIntStateOf(0) }
    var stepError by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    val step = steps[stepIndex.coerceIn(steps.indices)]
    val screenTitle = when (role) {
        UserRole.LANDLORD -> "Landlord account"
        UserRole.AGENT -> "Agent account"
        else -> "Student account"
    }

    fun leave() {
        (onBack ?: onNavigateToLogin)()
    }

    fun goBack() {
        stepError = null
        if (stepIndex > 0) stepIndex -= 1 else leave()
    }

    fun goNext() {
        val message = stepProblem(
            step = step,
            role = role,
            fullName = fullName,
            username = username,
            email = email,
            phone = phone,
            password = password,
            confirmPassword = confirmPassword,
            university = university,
            studentNumber = studentNumber,
            major = major,
            yearOfStudy = yearOfStudy,
            businessName = businessName,
            nationalIdNumber = nationalIdNumber,
            residentialAddress = residentialAddress,
            licenseNumber = licenseNumber,
            serviceArea = serviceArea,
        )
        if (message != null) {
            stepError = message
            return
        }
        stepError = null
        if (stepIndex < steps.lastIndex) stepIndex += 1 else onRegister()
    }

    LaunchedEffect(role) {
        stepIndex = 0
        stepError = null
    }

    Scaffold(
        topBar = {
            HavenPadHeader(title = screenTitle, onBackClick = { goBack() })
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 440.dp)
                    .fillMaxWidth()
                    .heightIn(max = maxHeight)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "HavenPad",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        StepProgress(count = steps.size, current = stepIndex)
                        AnimatedContent(
                            targetState = step,
                            transitionSpec = {
                                val forward = targetState.ordinal > initialState.ordinal
                                val enter = if (forward) {
                                    slideInHorizontally { it / 4 } + fadeIn()
                                } else {
                                    slideInHorizontally { -it / 4 } + fadeIn()
                                }
                                val exit = if (forward) {
                                    slideOutHorizontally { -it / 4 } + fadeOut()
                                } else {
                                    slideOutHorizontally { it / 4 } + fadeOut()
                                }
                                enter.togetherWith(exit)
                            },
                            label = "register-step",
                        ) { current ->
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                val copy = stepCopy(role, current)
                                HavenPadHeadline(text = copy.title)
                                HavenPadBody(text = copy.body)
                                StepFields(
                                    step = current,
                                    role = role,
                                    fullName = fullName,
                                    username = username,
                                    email = email,
                                    phone = phone,
                                    password = password,
                                    confirmPassword = confirmPassword,
                                    university = university,
                                    studentNumber = studentNumber,
                                    major = major,
                                    yearOfStudy = yearOfStudy,
                                    businessName = businessName,
                                    nationalIdNumber = nationalIdNumber,
                                    residentialAddress = residentialAddress,
                                    licenseNumber = licenseNumber,
                                    serviceArea = serviceArea,
                                    passwordVisible = passwordVisible,
                                    confirmPasswordVisible = confirmPasswordVisible,
                                    onFullNameChange = onFullNameChange,
                                    onUsernameChange = onUsernameChange,
                                    onEmailChange = onEmailChange,
                                    onPhoneChange = onPhoneChange,
                                    onPasswordChange = onPasswordChange,
                                    onConfirmPasswordChange = onConfirmPasswordChange,
                                    onUniversityChange = onUniversityChange,
                                    onStudentNumberChange = onStudentNumberChange,
                                    onMajorChange = onMajorChange,
                                    onYearChange = onYearChange,
                                    onBusinessNameChange = onBusinessNameChange,
                                    onNationalIdChange = onNationalIdChange,
                                    onAddressChange = onAddressChange,
                                    onLicenseChange = onLicenseChange,
                                    onServiceAreaChange = onServiceAreaChange,
                                    onTogglePassword = { passwordVisible = !passwordVisible },
                                    onToggleConfirm = { confirmPasswordVisible = !confirmPasswordVisible },
                                    onDone = { goNext() },
                                )
                            }
                        }
                        val message = stepError ?: (state as? RegisterState.Error)?.error
                        if (message != null) {
                            Text(
                                text = message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        val last = stepIndex == steps.lastIndex
                        HavenPadPrimaryButton(
                            text = when {
                                state is RegisterState.Loading -> "Creating account..."
                                last -> "Create account"
                                else -> "Continue"
                            },
                            onClick = { goNext() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = state !is RegisterState.Loading,
                        )
                    }
                }
                TextButton(onClick = onNavigateToLogin) {
                    Text("Already have an account? ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Log in", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun StepProgress(count: Int, current: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(count) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(
                        if (index <= current) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                        }
                    )
            )
        }
    }
}

private data class StepCopy(val title: String, val body: String)

private fun stepCopy(role: UserRole, step: RegisterStep): StepCopy = when (step) {
    RegisterStep.Name -> StepCopy(
        "What should we call you?",
        "This is the name other people on HavenPad will see.",
    )
    RegisterStep.Contact -> StepCopy(
        "How can we reach you?",
        "We'll use this email and phone for your account.",
    )
    RegisterStep.Identity -> when (role) {
        UserRole.LANDLORD -> StepCopy(
            "Your property",
            "Tell us the name on your listings.",
        )
        UserRole.AGENT -> StepCopy(
            "Your agency",
            "Share the agency you work with and your licence number.",
        )
        else -> StepCopy(
            "Your campus",
            "These details start your student KYC.",
        )
    }
    RegisterStep.Study -> when (role) {
        UserRole.LANDLORD -> StepCopy(
            "Where do you live?",
            "We use this address when we review your documents.",
        )
        UserRole.AGENT -> StepCopy(
            "Where do you work?",
            "We'll review this area and your ID. Nothing is verified automatically.",
        )
        else -> StepCopy(
            "What do you study?",
            "You can add your student ID document on the next screen.",
        )
    }
    RegisterStep.Password -> StepCopy(
        "Choose a password",
        "Use at least 8 characters.",
    )
}

@Composable
private fun StepFields(
    step: RegisterStep,
    role: UserRole,
    fullName: String,
    username: String,
    email: String,
    phone: String,
    password: String,
    confirmPassword: String,
    university: String,
    studentNumber: String,
    major: String,
    yearOfStudy: String,
    businessName: String,
    nationalIdNumber: String,
    residentialAddress: String,
    licenseNumber: String,
    serviceArea: String,
    passwordVisible: Boolean,
    confirmPasswordVisible: Boolean,
    onFullNameChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onUniversityChange: (String) -> Unit,
    onStudentNumberChange: (String) -> Unit,
    onMajorChange: (String) -> Unit,
    onYearChange: (String) -> Unit,
    onBusinessNameChange: (String) -> Unit,
    onNationalIdChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onLicenseChange: (String) -> Unit,
    onServiceAreaChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onToggleConfirm: () -> Unit,
    onDone: () -> Unit,
) {
    when (step) {
        RegisterStep.Name -> {
            StepField("Full name", fullName, onFullNameChange)
            StepField("Username", username, onUsernameChange, onDone = onDone)
        }
        RegisterStep.Contact -> {
            StepField(
                label = "Email",
                value = email,
                onValueChange = onEmailChange,
                keyboardType = KeyboardType.Email,
            )
            StepField(
                label = "Phone number",
                value = phone,
                onValueChange = onPhoneChange,
                keyboardType = KeyboardType.Phone,
                onDone = onDone,
            )
        }
        RegisterStep.Identity -> when (role) {
            UserRole.LANDLORD -> {
                StepField("Property or business name", businessName, onBusinessNameChange, onDone = onDone)
            }
            UserRole.AGENT -> {
                StepField("Agency name", businessName, onBusinessNameChange)
                StepField("Agent licence number", licenseNumber, onLicenseChange, onDone = onDone)
            }
            else -> {
                StepField("University", university, onUniversityChange)
                StepField("Student number", studentNumber, onStudentNumberChange, onDone = onDone)
            }
        }
        RegisterStep.Study -> when (role) {
            UserRole.LANDLORD -> {
                StepField("Residential address", residentialAddress, onAddressChange, onDone = onDone)
            }
            UserRole.AGENT -> {
                StepField("Service area", serviceArea, onServiceAreaChange)
                StepField("National ID number", nationalIdNumber, onNationalIdChange, onDone = onDone)
            }
            else -> {
                StepField("Major", major, onMajorChange)
                StepField(
                    label = "Year of study",
                    value = yearOfStudy,
                    onValueChange = onYearChange,
                    keyboardType = KeyboardType.Number,
                    onDone = onDone,
                )
            }
        }
        RegisterStep.Password -> {
            StepField(
                label = "Password",
                value = password,
                onValueChange = onPasswordChange,
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(onClick = onTogglePassword) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        )
                    }
                },
            )
            StepField(
                label = "Confirm password",
                value = confirmPassword,
                onValueChange = onConfirmPasswordChange,
                visualTransformation = if (confirmPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(onClick = onToggleConfirm) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password",
                        )
                    }
                },
                onDone = onDone,
            )
        }
    }
}

@Composable
private fun StepField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    onDone: (() -> Unit)? = null,
) {
    HavenPadTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = if (onDone != null) ImeAction.Done else ImeAction.Next,
        ),
        keyboardActions = if (onDone != null) {
            KeyboardActions(onDone = { onDone() })
        } else {
            KeyboardActions.Default
        },
    )
}

private fun stepProblem(
    step: RegisterStep,
    role: UserRole,
    fullName: String,
    username: String,
    email: String,
    phone: String,
    password: String,
    confirmPassword: String,
    university: String,
    studentNumber: String,
    major: String,
    yearOfStudy: String,
    businessName: String,
    nationalIdNumber: String,
    residentialAddress: String,
    licenseNumber: String,
    serviceArea: String,
): String? = when (step) {
    RegisterStep.Name -> when {
        fullName.isBlank() -> "Full name is required."
        username.isBlank() -> "Username is required."
        else -> null
    }
    RegisterStep.Contact -> when {
        !email.contains("@") || email.substringAfter("@").isBlank() -> "Enter a valid email."
        phone.isBlank() -> "Phone number is required."
        else -> null
    }
    RegisterStep.Identity -> when (role) {
        UserRole.LANDLORD -> when {
            businessName.isBlank() -> "Property or business name is required."
            else -> null
        }
        UserRole.AGENT -> when {
            businessName.isBlank() -> "Agency name is required."
            licenseNumber.isBlank() -> "Agent licence number is required."
            else -> null
        }
        else -> when {
            university.isBlank() -> "University is required."
            studentNumber.isBlank() -> "Student number is required."
            else -> null
        }
    }
    RegisterStep.Study -> when (role) {
        UserRole.LANDLORD -> when {
            residentialAddress.isBlank() -> "Residential address is required."
            else -> null
        }
        UserRole.AGENT -> when {
            serviceArea.isBlank() -> "Service area is required."
            nationalIdNumber.isBlank() -> "National ID number is required."
            else -> null
        }
        else -> when {
            major.isBlank() -> "Major is required."
            yearOfStudy.toIntOrNull().let { it == null || it < 1 || it > 8 } -> "Year of study is required."
            else -> null
        }
    }
    RegisterStep.Password -> when {
        password.length < 8 -> "Password must be at least 8 characters."
        password != confirmPassword -> "Passwords do not match."
        else -> null
    }
}

@ScreenPreviews
@Composable
fun RegisterScreenPreview() {
    HavenPadTheme {
        RegisterScreenContent(
            state = RegisterState.Idle,
            role = UserRole.STUDENT,
            fullName = "Amina Otieno",
            username = "amina",
            email = "amina@example.com",
            phone = "0712345678",
            password = "",
            confirmPassword = "",
            university = "University of Nairobi",
            studentNumber = "P15/1234/2022",
            major = "Computer Science",
            yearOfStudy = "3",
            businessName = "",
            nationalIdNumber = "",
            residentialAddress = "",
            licenseNumber = "",
            serviceArea = "",
            onNavigateToLogin = {},
            onBack = {},
            onFullNameChange = {},
            onUsernameChange = {},
            onEmailChange = {},
            onPhoneChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onUniversityChange = {},
            onStudentNumberChange = {},
            onMajorChange = {},
            onYearChange = {},
            onBusinessNameChange = {},
            onNationalIdChange = {},
            onAddressChange = {},
            onLicenseChange = {},
            onServiceAreaChange = {},
            onRegister = {}
        )
    }
}
