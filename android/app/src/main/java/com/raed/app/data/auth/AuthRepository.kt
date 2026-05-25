package com.raed.app.data.auth

import android.app.Activity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.raed.app.data.api.RaedApi
import com.raed.app.data.api.models.VerifyFirebaseTokenRequest
import com.raed.app.data.local.SessionDataStore
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

sealed class AuthResult {
    data class Success(
        val isNewUser: Boolean,
        val userType: String,
        val hasProfile: Boolean,
    ) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

@Singleton
class AuthRepository @Inject constructor(
    private val api: RaedApi,
    private val sessionDataStore: SessionDataStore,
    private val firebaseAuth: FirebaseAuth,
) {
    fun sendPhoneOtp(
        phoneNumber: String,
        activity: Activity,
        onCodeSent: (verificationId: String) -> Unit,
        onAutoVerified: (PhoneAuthCredential) -> Unit,
        onError: (String) -> Unit,
    ) {
        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    onAutoVerified(credential)
                }
                override fun onVerificationFailed(e: FirebaseException) {
                    onError(e.localizedMessage ?: "فشل إرسال رمز التحقق")
                }
                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken,
                ) {
                    onCodeSent(verificationId)
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    suspend fun verifyPhoneCode(verificationId: String, code: String): AuthResult {
        return try {
            val credential = PhoneAuthProvider.getCredential(verificationId, code)
            firebaseAuth.signInWithCredential(credential).await()
            exchangeToken(userType = null)
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "رمز التحقق غير صحيح")
        }
    }

    suspend fun signInWithPhoneCredential(credential: PhoneAuthCredential): AuthResult {
        return try {
            firebaseAuth.signInWithCredential(credential).await()
            exchangeToken(userType = null)
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "فشل التحقق")
        }
    }

    suspend fun exchangeToken(userType: String?): AuthResult {
        val idToken = firebaseAuth.currentUser?.getIdToken(false)?.await()?.token
            ?: return AuthResult.Error("فشل الحصول على رمز المصادقة")
        return try {
            val response = api.verifyFirebaseToken(
                VerifyFirebaseTokenRequest(idToken = idToken, userType = userType)
            )
            if (!response.isSuccessful) {
                return if (response.code() == 400) AuthResult.Error("NEW_USER_NEEDS_TYPE")
                else AuthResult.Error("خطأ في الخادم: ${response.code()}")
            }
            val body = response.body()!!
            sessionDataStore.saveSession(
                accessToken = body.accessToken,
                refreshToken = body.refreshToken,
                userId = body.user.id,
                userType = body.user.userType,
            )
            AuthResult.Success(
                isNewUser = body.user.isNewUser,
                userType = body.user.userType,
                hasProfile = body.user.fullName != null,
            )
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "خطأ غير متوقع")
        }
    }

    suspend fun clearSession() {
        firebaseAuth.signOut()
        sessionDataStore.clearSession()
    }
}
