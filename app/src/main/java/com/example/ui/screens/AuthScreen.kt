package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.R
import com.example.model.BuiltInAvatar
import androidx.compose.runtime.ReadOnlyComposable
import com.example.ui.theme.AppTheme
import com.example.ui.theme.CormorantSerifFamily
import com.example.ui.theme.InterUiFamily
import com.example.ui.theme.MicroUpperLabel
import com.example.ui.widgets.RoundAvatarCarousel
import com.example.ui.widgets.drawableRes
import com.example.ui.widgets.rememberReducedMotion
import com.example.util.ErrorSanitizer
import com.example.viewmodel.AnimeViewModel
import com.example.viewmodel.OnboardingStage

private val ObsidianBlack: Color
    @Composable @ReadOnlyComposable get() = AppTheme.colors.backgroundMain
private val ObsidianSurface: Color
    @Composable @ReadOnlyComposable get() = AppTheme.colors.cardBackground
private val ObsidianSurface2: Color
    @Composable @ReadOnlyComposable get() = AppTheme.colors.elevatedSurface
private val ChampagneGold: Color
    @Composable @ReadOnlyComposable get() = AppTheme.colors.accentPrimary
private val ChampagneGoldBright: Color
    @Composable @ReadOnlyComposable get() = AppTheme.colors.accentSecondary
private val ChampagneGoldSoft: Color
    @Composable @ReadOnlyComposable get() = AppTheme.colors.highlight
private val ChampagneOnGold: Color
    @Composable @ReadOnlyComposable get() = AppTheme.colors.onAccentPrimary
private val IvoryText: Color
    @Composable @ReadOnlyComposable get() = AppTheme.colors.textPrimary
private val AshText: Color
    @Composable @ReadOnlyComposable get() = AppTheme.colors.textSecondary
private val AshDim: Color
    @Composable @ReadOnlyComposable get() = AppTheme.colors.textMuted

private val AuthBgTop: Color
    @Composable @ReadOnlyComposable get() = AppTheme.colors.backgroundMain
private val AuthBgBottom: Color
    @Composable @ReadOnlyComposable get() = AppTheme.colors.backgroundSecondary
private val AuthGlassCard: Color
    @Composable @ReadOnlyComposable get() = AppTheme.colors.cardBackground.copy(alpha = 0.96f)
private val AuthFieldBg: Color
    @Composable @ReadOnlyComposable get() = AppTheme.colors.elevatedSurface.copy(alpha = 0.94f)
private val RoyalPlumGlow: Color
    @Composable @ReadOnlyComposable get() = AppTheme.colors.accentSecondary

@Composable
fun AuthScreen(
    viewModel: AnimeViewModel,
    onboardingStage: OnboardingStage = OnboardingStage.NONE,
    needsInviteOnlyGate: Boolean = false
) {
    val isLoading by viewModel.isAuthLoading.collectAsState()
    val authError by viewModel.authError.collectAsState()
    val pendingVerification by viewModel.pendingEmailVerification.collectAsState()
    val session by viewModel.authSession.collectAsState()
    val pendingOnboardingUsername by viewModel.pendingOnboardingUsername.collectAsState()
    val isCheckingUsername by viewModel.isCheckingUsername.collectAsState()
    val usernameAvailabilityError by viewModel.usernameAvailabilityError.collectAsState()

    var isSignUpMode by rememberSaveable { mutableStateOf(false) }
    var identifier by rememberSaveable { mutableStateOf("") }
    var signUpEmail by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var onboardingUsername by rememberSaveable(pendingOnboardingUsername) {
        mutableStateOf(pendingOnboardingUsername)
    }
    var selectedAvatarKey by rememberSaveable { mutableStateOf(BuiltInAvatar.AVATAR_1.key) }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var localValidationError by remember { mutableStateOf<String?>(null) }

    var showForgotPasswordDialog by rememberSaveable { mutableStateOf(false) }
    var resetIdentifier by rememberSaveable { mutableStateOf("") }

    val activeOnboardingStage = when {
        session != null && onboardingStage != OnboardingStage.NONE -> onboardingStage
        session != null && needsInviteOnlyGate -> OnboardingStage.USERNAME
        else -> OnboardingStage.NONE
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(AuthBgTop, AuthBgBottom)))
            .systemBarsPadding()
            .imePadding()
    ) {
        val topGlowColor = (if (activeOnboardingStage == OnboardingStage.AVATAR) RoyalPlumGlow else ChampagneGold)
            .copy(alpha = if (AppTheme.colors.isDark) 0.20f else 0.12f)
        val bottomGlowColor = ChampagneGoldBright.copy(alpha = if (AppTheme.colors.isDark) 0.10f else 0.08f)
        // Ambient theme accent aura
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(topGlowColor, Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.16f),
                    radius = size.maxDimension * 0.48f
                ),
                center = Offset(size.width * 0.5f, size.height * 0.16f),
                radius = size.maxDimension * 0.48f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(bottomGlowColor, Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.86f),
                    radius = size.maxDimension * 0.52f
                ),
                center = Offset(size.width * 0.5f, size.height * 0.86f),
                radius = size.maxDimension * 0.52f
            )
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = activeOnboardingStage,
                transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(200)) },
                label = "onboarding_stage_transition"
            ) { stage ->
                when (stage) {
                    OnboardingStage.USERNAME -> {
                        OnboardingUsernameStage(
                            username = onboardingUsername,
                            onUsernameChange = {
                                onboardingUsername = it
                                localValidationError = null
                                viewModel.clearUsernameError()
                            },
                            isLoading = isCheckingUsername || isLoading,
                            error = localValidationError ?: usernameAvailabilityError ?: authError,
                            onContinue = {
                                val clean = onboardingUsername.trim()
                                if (clean.length !in 2..32) {
                                    localValidationError = "Username must be between 2 and 32 characters."
                                } else {
                                    localValidationError = null
                                    viewModel.verifyAndAdvanceUsername(clean)
                                }
                            },
                            onSignOut = { viewModel.signOut() }
                        )
                    }

                    OnboardingStage.AVATAR -> {
                        RoyalAvatarSelectionStage(
                            username = pendingOnboardingUsername.ifBlank { onboardingUsername.trim().ifBlank { "Member" } },
                            selectedAvatarKey = selectedAvatarKey,
                            onSelectAvatar = { selectedAvatarKey = it.key },
                            isLoading = isLoading,
                            error = authError,
                            onBack = {
                                localValidationError = null
                                viewModel.backToUsernameStep()
                            },
                            onComplete = {
                                viewModel.completeRoyalAvatarOnboarding(selectedAvatarKey)
                            }
                        )
                    }

                    OnboardingStage.NONE -> {
                        Column(
                            modifier = Modifier
                                .widthIn(max = 430.dp)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 22.dp, vertical = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_login_mascot),
                                    contentDescription = "Anime Universe Mascot",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .height(176.dp)
                                        .align(Alignment.TopCenter)
                                        .zIndex(2f)
                                )

                                GlassAuthCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 146.dp)
                                ) {
                                    AnimatedContent(
                                        targetState = when {
                                            !isSignUpMode -> 0
                                            !pendingVerification.isNullOrBlank() -> 2
                                            else -> 1
                                        },
                                        transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
                                        label = "auth_card_mode"
                                    ) { cardMode ->
                                        when (cardMode) {
                                            0 -> {
                                                // SIGN IN (Email or Username + Password only)
                                                Column(
                                                    modifier = Modifier.padding(
                                                        top = 34.dp,
                                                        start = 22.dp,
                                                        end = 22.dp,
                                                        bottom = 22.dp
                                                    ),
                                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                                ) {
                                                    ObsidianLabeledField(
                                                        label = "Email or Username",
                                                        value = identifier,
                                                        onValueChange = {
                                                            identifier = it
                                                            localValidationError = null
                                                            viewModel.clearAuthMessages()
                                                        },
                                                        placeholder = "you@example.com or username",
                                                        leadingIcon = Icons.Filled.Person,
                                                        keyboardType = KeyboardType.Email,
                                                        modifier = Modifier.testTag("auth_email_input")
                                                    )

                                                    ObsidianLabeledField(
                                                        label = "Password",
                                                        value = password,
                                                        onValueChange = {
                                                            password = it
                                                            localValidationError = null
                                                        },
                                                        placeholder = "••••••••",
                                                        leadingIcon = Icons.Filled.Key,
                                                        keyboardType = KeyboardType.Password,
                                                        isPassword = true,
                                                        passwordVisible = passwordVisible,
                                                        onTogglePassword = { passwordVisible = !passwordVisible },
                                                        modifier = Modifier.testTag("auth_password_input")
                                                    )

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.End
                                                    ) {
                                                        Text(
                                                            text = "Forgot Password?",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            fontWeight = FontWeight.Medium,
                                                            color = AshText,
                                                            modifier = Modifier
                                                                .pointerHoverIcon(PointerIcon.Hand)
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .clickable {
                                                                    resetIdentifier = identifier
                                                                    viewModel.clearAuthMessages()
                                                                    showForgotPasswordDialog = true
                                                                }
                                                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                                                .testTag("forgot_password_button")
                                                        )
                                                    }

                                                    AuthFeedbackBanner(
                                                        error = localValidationError ?: authError,
                                                        info = pendingVerification
                                                    )

                                                    AnimatedVisibility(visible = !pendingVerification.isNullOrBlank()) {
                                                        VerificationLinkPromptCard(
                                                            targetEmail = identifier.takeIf { it.contains("@") },
                                                            isLoading = isLoading,
                                                            onResend = {
                                                                if (identifier.contains("@")) {
                                                                    viewModel.resendVerificationEmail(identifier)
                                                                }
                                                            },
                                                            onContinue = {
                                                                localValidationError = null
                                                                viewModel.signInWithIdentifier(identifier, password)
                                                            }
                                                        )
                                                    }

                                                    GoldCtaButton(
                                                        text = "SIGN IN",
                                                        isLoading = isLoading,
                                                        enabled = !isLoading,
                                                        onClick = {
                                                            val cleanId = identifier.trim()
                                                            when {
                                                                cleanId.isBlank() -> {
                                                                    localValidationError = "Enter your email or username."
                                                                }
                                                                password.isBlank() -> {
                                                                    localValidationError = "Enter your password."
                                                                }
                                                                else -> {
                                                                    localValidationError = null
                                                                    viewModel.signInWithIdentifier(cleanId, password)
                                                                }
                                                            }
                                                        },
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .testTag("auth_submit_button")
                                                    )
                                                }
                                            }

                                            1 -> {
                                                // SIGN UP (Email + Password only)
                                                Column(
                                                    modifier = Modifier.padding(
                                                        top = 34.dp,
                                                        start = 22.dp,
                                                        end = 22.dp,
                                                        bottom = 22.dp
                                                    ),
                                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    Text(
                                                        text = "Create Account",
                                                        fontFamily = CormorantSerifFamily,
                                                        fontSize = 24.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = IvoryText
                                                    )

                                                    ObsidianLabeledField(
                                                        label = "Email",
                                                        value = signUpEmail,
                                                        onValueChange = {
                                                            signUpEmail = it
                                                            localValidationError = null
                                                        },
                                                        placeholder = "you@example.com",
                                                        leadingIcon = Icons.Filled.Email,
                                                        keyboardType = KeyboardType.Email,
                                                        modifier = Modifier.testTag("auth_email_input")
                                                    )

                                                    ObsidianLabeledField(
                                                        label = "Password (6+ chars)",
                                                        value = password,
                                                        onValueChange = {
                                                            password = it
                                                            localValidationError = null
                                                        },
                                                        placeholder = "••••••••",
                                                        leadingIcon = Icons.Filled.Key,
                                                        keyboardType = KeyboardType.Password,
                                                        isPassword = true,
                                                        passwordVisible = passwordVisible,
                                                        onTogglePassword = { passwordVisible = !passwordVisible },
                                                        modifier = Modifier.testTag("auth_password_input")
                                                    )

                                                    ObsidianLabeledField(
                                                        label = "Confirm Password",
                                                        value = confirmPassword,
                                                        onValueChange = {
                                                            confirmPassword = it
                                                            localValidationError = null
                                                        },
                                                        placeholder = "••••••••",
                                                        leadingIcon = Icons.Filled.Lock,
                                                        keyboardType = KeyboardType.Password,
                                                        isPassword = true,
                                                        passwordVisible = passwordVisible,
                                                        onTogglePassword = null,
                                                        modifier = Modifier.testTag("auth_confirm_password_input")
                                                    )

                                                    AuthFeedbackBanner(
                                                        error = localValidationError ?: authError,
                                                        info = null
                                                    )

                                                    Spacer(modifier = Modifier.height(2.dp))

                                                    GoldCtaButton(
                                                        text = "CREATE ACCOUNT",
                                                        isLoading = isLoading,
                                                        enabled = !isLoading,
                                                        trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
                                                        onClick = {
                                                            val cleanEmail = signUpEmail.trim()
                                                            when {
                                                                !cleanEmail.contains("@") || !cleanEmail.contains(".") -> {
                                                                    localValidationError = "Enter a valid email address."
                                                                }
                                                                password.length < 6 -> {
                                                                    localValidationError = "Password must be at least 6 characters."
                                                                }
                                                                password != confirmPassword -> {
                                                                    localValidationError = "Passwords do not match."
                                                                }
                                                                else -> {
                                                                    localValidationError = null
                                                                    viewModel.clearAuthMessages()
                                                                    viewModel.signUpEmailOnly(cleanEmail, password)
                                                                }
                                                            }
                                                        },
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .testTag("auth_submit_button")
                                                    )
                                                }
                                            }

                                            else -> {
                                                // EMAIL VERIFICATION WAITING VIEW (Auto-detects verification in background + Link redirect)
                                                LaunchedEffect(signUpEmail, password, pendingVerification) {
                                                    if (signUpEmail.isNotBlank() && password.length >= 6) {
                                                        while (true) {
                                                            delay(3500L)
                                                            if (viewModel.checkEmailVerifiedSilently(signUpEmail, password)) {
                                                                break
                                                            }
                                                        }
                                                    }
                                                }
                                                Column(
                                                    modifier = Modifier.padding(
                                                        top = 34.dp,
                                                        start = 22.dp,
                                                        end = 22.dp,
                                                        bottom = 22.dp
                                                    ),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                                ) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = ChampagneGold.copy(alpha = 0.16f),
                                                        border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.5f)),
                                                        modifier = Modifier.size(60.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                imageVector = Icons.Filled.MarkEmailRead,
                                                                contentDescription = null,
                                                                tint = ChampagneGold,
                                                                modifier = Modifier.size(28.dp)
                                                            )
                                                        }
                                                    }

                                                    Text(
                                                        text = "Check Your Inbox",
                                                        fontFamily = CormorantSerifFamily,
                                                        fontSize = 26.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = IvoryText
                                                    )

                                                    Surface(
                                                        shape = CircleShape,
                                                        color = AuthFieldBg,
                                                        border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.3f))
                                                    ) {
                                                        Text(
                                                            text = ErrorSanitizer.maskEmail(signUpEmail).ifBlank { "Confirmation Sent" },
                                                            style = MaterialTheme.typography.labelMedium,
                                                            color = ChampagneGoldSoft,
                                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                                        )
                                                    }

                                                    var signUpOtpCode by rememberSaveable { mutableStateOf("") }

                                                    Text(
                                                        text = "Enter the 8-digit verification code sent to your email to continue.",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = AshText,
                                                        textAlign = TextAlign.Center
                                                    )

                                                    ObsidianLabeledField(
                                                        label = "8-Digit Verification Code",
                                                        value = signUpOtpCode,
                                                        onValueChange = { input ->
                                                            signUpOtpCode = input.filter { it.isDigit() }.take(8)
                                                            localValidationError = null
                                                        },
                                                        placeholder = "ex-12345678",
                                                        leadingIcon = Icons.Filled.Verified,
                                                        keyboardType = KeyboardType.Number,
                                                        modifier = Modifier.testTag("signup_verification_code_input")
                                                    )

                                                    AuthFeedbackBanner(error = authError, info = pendingVerification)

                                                    GoldCtaButton(
                                                        text = "CONFIRM & CONTINUE",
                                                        isLoading = isLoading,
                                                        enabled = !isLoading && signUpOtpCode.length == 8,
                                                        onClick = {
                                                            viewModel.verifySignUpOtp(signUpEmail, signUpOtpCode)
                                                        },
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .testTag("auth_confirmed_signin_button")
                                                    )

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        TextButton(
                                                            onClick = { viewModel.resendVerificationEmail(signUpEmail) },
                                                            enabled = !isLoading && signUpEmail.isNotBlank(),
                                                            modifier = Modifier.testTag("auth_resend_email_button")
                                                        ) {
                                                            Text("Resend Code", color = ChampagneGold)
                                                        }
                                                        TextButton(
                                                            onClick = {
                                                                localValidationError = null
                                                                viewModel.clearAuthMessages()
                                                            },
                                                            enabled = !isLoading,
                                                            modifier = Modifier.testTag("auth_edit_signup_email_button")
                                                        ) {
                                                            Text("Change Email", color = AshText)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 6.dp)
                            ) {
                                Text(
                                    text = if (!isSignUpMode) "Don't have an account? " else "Already have an account? ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AshText
                                )
                                Text(
                                    text = if (!isSignUpMode) "Sign Up" else "Sign In",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ChampagneGold,
                                    modifier = Modifier
                                        .pointerHoverIcon(PointerIcon.Hand)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            isSignUpMode = !isSignUpMode
                                            localValidationError = null
                                            viewModel.clearAuthMessages()
                                        }
                                        .padding(horizontal = 6.dp, vertical = 6.dp)
                                        .testTag(if (!isSignUpMode) "auth_tab_signup" else "auth_tab_signin")
                                )
                            }

                            TextButton(
                                onClick = { viewModel.enterLocalPreviewMode() },
                                modifier = Modifier
                                    .pointerHoverIcon(PointerIcon.Hand)
                                    .testTag("local_preview_button")
                            ) {
                                Text(
                                    text = "Continue without an account",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AshDim
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    var resetCodeSent by rememberSaveable { mutableStateOf(false) }
    var resetOtpOrLink by rememberSaveable { mutableStateOf("") }
    var resetNewPassword by rememberSaveable { mutableStateOf("") }

    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showForgotPasswordDialog = false
                resetCodeSent = false
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = ObsidianSurface,
            title = {
                Text(
                    text = if (resetCodeSent) "Enter Reset Code" else "Reset Password",
                    fontWeight = FontWeight.Bold,
                    color = IvoryText
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = resetIdentifier,
                        onValueChange = { resetIdentifier = it },
                        label = { Text("Email or Username") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("forgot_password_identifier_input")
                    )
                    if (!resetCodeSent) {
                        Text(
                            text = "We'll send an 8-digit reset code to your email.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AshText
                        )
                        TextButton(
                            onClick = { resetCodeSent = true },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Already have a reset code?", color = ChampagneGold, fontSize = 12.sp)
                        }
                    } else {
                        OutlinedTextField(
                            value = resetOtpOrLink,
                            onValueChange = { input ->
                                resetOtpOrLink = input.filter { it.isDigit() }.take(8)
                            },
                            label = { Text("8-Digit Reset Code") },
                            placeholder = { Text("ex-12345678") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("forgot_password_code_input")
                        )
                        OutlinedTextField(
                            value = resetNewPassword,
                            onValueChange = { resetNewPassword = it },
                            label = { Text("New Password (min 6 chars)") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("forgot_password_new_password_input")
                        )
                    }
                    if (!authError.isNullOrBlank()) {
                        Text(
                            text = authError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFF6B6B)
                        )
                    } else if (!pendingVerification.isNullOrBlank() && resetCodeSent) {
                        Text(
                            text = pendingVerification!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = ChampagneGoldSoft
                        )
                    }
                }
            },
            confirmButton = {
                val canConfirmReset = if (!resetCodeSent) {
                    resetIdentifier.isNotBlank()
                } else {
                    resetOtpOrLink.length == 8 && resetNewPassword.length >= 6
                }
                Button(
                    onClick = {
                        if (!resetCodeSent) {
                            viewModel.sendPasswordReset(resetIdentifier) {
                                resetCodeSent = true
                            }
                        } else {
                            viewModel.verifyRecoveryCodeAndSetPassword(
                                identifier = resetIdentifier,
                                otpOrLink = resetOtpOrLink,
                                newPassword = resetNewPassword
                            ) {
                                resetOtpOrLink = ""
                                resetNewPassword = ""
                                resetCodeSent = false
                                showForgotPasswordDialog = false
                            }
                        }
                    },
                    enabled = !isLoading && canConfirmReset,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ChampagneGold,
                        contentColor = ChampagneOnGold
                    )
                ) {
                    Text(
                        text = if (!resetCodeSent) "Send Code" else "Update Password",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showForgotPasswordDialog = false
                        resetCodeSent = false
                    }
                ) {
                    Text("Cancel", color = AshText)
                }
            }
        )
    }
}

@Composable
private fun OnboardingUsernameStage(
    username: String,
    onUsernameChange: (String) -> Unit,
    isLoading: Boolean,
    error: String?,
    onContinue: () -> Unit,
    onSignOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .widthIn(max = 440.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 20.dp)
            .testTag("onboarding_username_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_login_mascot),
                contentDescription = "Anime Universe Mascot",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .height(154.dp)
                    .align(Alignment.TopCenter)
                    .zIndex(2f)
            )
            GlassAuthCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 126.dp)
            ) {
                Column(
                    modifier = Modifier.padding(top = 34.dp, start = 22.dp, end = 22.dp, bottom = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    StepProgressHeader(
                        step = 1,
                        totalSteps = 2,
                        title = "Claim Your Identity"
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = null,
                            tint = ChampagneGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Email Verified",
                            style = MaterialTheme.typography.bodySmall,
                            color = ChampagneGoldSoft
                        )
                    }

                    ObsidianLabeledField(
                        label = "Unique Username",
                        value = username,
                        onValueChange = onUsernameChange,
                        placeholder = "2–32 characters",
                        leadingIcon = Icons.Filled.Person,
                        modifier = Modifier.testTag("onboarding_username_input")
                    )

                    AuthFeedbackBanner(error = error, info = null)

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = onSignOut,
                            shape = RoundedCornerShape(26.dp),
                            border = BorderStroke(1.dp, IvoryText.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .weight(0.7f)
                                .height(52.dp)
                                .pointerHoverIcon(PointerIcon.Hand)
                                .testTag("onboarding_signout_button")
                        ) {
                            Text("Sign Out", color = IvoryText, maxLines = 1)
                        }
                        GoldCtaButton(
                            text = "NEXT",
                            isLoading = isLoading,
                            enabled = !isLoading,
                            trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
                            onClick = onContinue,
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("onboarding_username_next_button")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoyalAvatarSelectionStage(
    username: String,
    selectedAvatarKey: String,
    onSelectAvatar: (BuiltInAvatar) -> Unit,
    isLoading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    val selectedPreset = remember(selectedAvatarKey) { BuiltInAvatar.fromKey(selectedAvatarKey) }
    val reducedMotion = rememberReducedMotion()

    val shimmerAlpha = if (reducedMotion) {
        0.72f
    } else {
        val transition = rememberInfiniteTransition(label = "royal_halo")
        val anim by transition.animateFloat(
            initialValue = 0.48f,
            targetValue = 0.92f,
            animationSpec = infiniteRepeatable(
                animation = tween(1800),
                repeatMode = RepeatMode.Reverse
            ),
            label = "halo_pulse"
        )
        anim
    }

    Column(
        modifier = Modifier
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp)
            .testTag("royal_pfp_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        GlassAuthCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Royal Step Header
                StepProgressHeader(
                    step = 2,
                    totalSteps = 2,
                    title = "Royal Portrait"
                )

                // Crown / Crest Pill
                Surface(
                    shape = CircleShape,
                    color = ChampagneGold.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = ChampagneGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "RESERVED FOR @$username",
                            style = MicroUpperLabel.copy(fontSize = 9.sp, letterSpacing = 1.8.sp),
                            color = ChampagneGoldSoft
                        )
                    }
                }

                // Grand Animated Royal Portrait Spotlight (Only available built-in PFPs)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(146.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        ChampagneGold.copy(alpha = shimmerAlpha * 0.42f),
                                        selectedPreset.primaryColor.copy(alpha = 0.18f),
                                        Color.Transparent
                                    )
                                ),
                                CircleShape
                            )
                    )
                    // Outer Metallic Gold Concentric Ring
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(124.dp)
                            .clip(CircleShape)
                            .border(
                                width = 2.5.dp,
                                brush = Brush.linearGradient(
                                    listOf(ChampagneGoldBright, ChampagneGoldSoft, ChampagneGold)
                                ),
                                shape = CircleShape
                            )
                            .padding(5.dp)
                            .border(
                                width = 1.dp,
                                color = ChampagneGold.copy(alpha = 0.45f),
                                shape = CircleShape
                            )
                            .padding(4.dp)
                    ) {
                        AnimatedContent(
                            targetState = selectedPreset,
                            transitionSpec = {
                                (fadeIn(tween(240)) + scaleIn(initialScale = 0.90f)) togetherWith
                                    (fadeOut(tween(180)) + scaleOut(targetScale = 1.06f))
                            },
                            label = "royal_spotlight_portrait"
                        ) { preset ->
                            Image(
                                painter = painterResource(id = preset.drawableRes()),
                                contentDescription = "${preset.displayName} Royal Portrait",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        }
                    }

                    // Bottom Sovereign Plaque
                    Surface(
                        shape = CircleShape,
                        color = ObsidianBlack,
                        border = BorderStroke(1.2.dp, ChampagneGold),
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                    ) {
                        Text(
                            text = selectedPreset.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = ChampagneGoldBright,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = "Select your official guild portrait from the Royal Collection below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AshText,
                    textAlign = TextAlign.Center
                )

                RoundAvatarCarousel(
                    selectedKey = selectedAvatarKey,
                    onSelectAvatar = onSelectAvatar,
                    avatarDiameter = 74.dp,
                    showLabels = true,
                    darkSurface = true
                )

                AuthFeedbackBanner(error = error, info = null)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        enabled = !isLoading,
                        shape = RoundedCornerShape(26.dp),
                        border = BorderStroke(1.dp, IvoryText.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .weight(0.42f)
                            .height(52.dp)
                            .pointerHoverIcon(PointerIcon.Hand)
                            .testTag("onboarding_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = IvoryText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    GoldCtaButton(
                        text = "ENTER REALM",
                        isLoading = isLoading,
                        enabled = !isLoading,
                        onClick = onComplete,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("onboarding_complete_button")
                    )
                }
            }
        }
    }
}


@Composable
private fun VerificationLinkPromptCard(
    targetEmail: String?,
    isLoading: Boolean,
    onResend: () -> Unit,
    onContinue: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ObsidianSurface2.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("email_verification_link_panel")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Tap the confirmation link in your email inbox, then press Continue below.",
                style = MaterialTheme.typography.bodySmall,
                color = AshText
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!targetEmail.isNullOrBlank()) {
                    TextButton(
                        onClick = onResend,
                        enabled = !isLoading,
                        modifier = Modifier.testTag("auth_resend_email_button")
                    ) {
                        Text("Resend Link", color = ChampagneGold)
                    }
                } else {
                    Spacer(modifier = Modifier.width(4.dp))
                }
                TextButton(
                    onClick = onContinue,
                    enabled = !isLoading,
                    modifier = Modifier.testTag("auth_confirmed_signin_button")
                ) {
                    Text("I've Confirmed — Continue", color = ChampagneGoldSoft)
                }
            }
        }
    }
}

@Composable
private fun GlassAuthCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = AuthGlassCard,
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    ChampagneGold.copy(alpha = 0.30f),
                    Color.White.copy(alpha = 0.08f)
                )
            )
        ),
        shadowElevation = 18.dp,
        modifier = modifier
    ) {
        content()
    }
}

@Composable
private fun ObsidianLabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePassword: (() -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = IvoryText.copy(alpha = 0.92f)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(AuthFieldBg)
                .border(
                    width = 1.dp,
                    color = ChampagneGold.copy(alpha = 0.42f),
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = ChampagneGold.copy(alpha = 0.85f),
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyMedium,
                            color = IvoryText.copy(alpha = 0.35f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        singleLine = true,
                        textStyle = TextStyle(
                            fontFamily = InterUiFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = IvoryText
                        ),
                        cursorBrush = SolidColor(ChampagneGold),
                        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                        visualTransformation = if (isPassword && !passwordVisible) {
                            PasswordVisualTransformation()
                        } else {
                            VisualTransformation.None
                        },
                        modifier = modifier.fillMaxWidth()
                    )
                }
                if (isPassword && onTogglePassword != null) {
                    IconButton(
                        onClick = onTogglePassword,
                        modifier = Modifier
                            .size(48.dp)
                            .pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = IvoryText.copy(alpha = 0.7f),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GoldCtaButton(
    text: String,
    isLoading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingIcon: ImageVector? = null
) {
    val gradient = Brush.horizontalGradient(
        colors = listOf(ChampagneGoldSoft, ChampagneGold, ChampagneGoldBright)
    )
    val disabledBg = AppTheme.colors.borderSubtle
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(26.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = disabledBg
        ),
        contentPadding = PaddingValues(0.dp),
        modifier = modifier
            .height(52.dp)
            .pointerHoverIcon(PointerIcon.Hand)
            .shadow(
                elevation = if (enabled) 10.dp else 0.dp,
                shape = RoundedCornerShape(26.dp),
                ambientColor = ChampagneGold.copy(alpha = 0.4f),
                spotColor = ChampagneGold.copy(alpha = 0.4f)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(26.dp))
                .then(
                    if (enabled) Modifier.background(gradient)
                    else Modifier.background(disabledBg)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = ChampagneOnGold
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = text,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        letterSpacing = 1.4.sp,
                        color = if (enabled) ChampagneOnGold else IvoryText
                    )
                    if (trailingIcon != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = trailingIcon,
                            contentDescription = null,
                            tint = ChampagneOnGold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepProgressHeader(
    step: Int,
    totalSteps: Int,
    title: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontFamily = CormorantSerifFamily,
                fontSize = 23.sp,
                fontWeight = FontWeight.SemiBold,
                color = IvoryText
            )
            Text(
                text = "Step $step of $totalSteps",
                style = MaterialTheme.typography.labelMedium,
                color = ChampagneGold
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (i in 1..totalSteps) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (i <= step) ChampagneGold
                            else AppTheme.colors.borderSubtle
                        )
                )
            }
        }
    }
}

@Composable
private fun AuthFeedbackBanner(
    error: String?,
    info: String?
) {
    val colors = AppTheme.colors
    AnimatedVisibility(visible = !error.isNullOrBlank()) {
        if (!error.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.error.copy(alpha = if (colors.isDark) 0.24f else 0.12f),
                border = BorderStroke(1.dp, colors.error.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = error,
                    color = colors.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
    AnimatedVisibility(visible = !info.isNullOrBlank()) {
        if (!info.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ChampagneGold.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = info,
                    color = ChampagneGoldSoft,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}
