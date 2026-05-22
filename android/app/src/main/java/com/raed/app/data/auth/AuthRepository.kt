package com.raed.app.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.raed.app.data.api.RaedApi
import com.raed.app.data.api.models.VerifyFirebaseTokenRequest
import com.raed.app.data.local.SessionDataStore
import kotlinx.coroutines.tasks.await
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
    suspend fun signInWithEmail(email: String, password: String): AuthResult {
        return try {
            firebaseAuth.signInWithEmailAndPassword(email, password).await()
            exchangeToken(userType = null)
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "فشل تسجيل الدخول")
        }
    }

    suspend fun createAccountWithEmail(email: String, password: String): AuthResult {
        return try {
            firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            exchangeToken(userType = null)
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "فشل إنشاء الحساب")
        }
    }

    suspend fun signInWithGoogle(googleIdToken: String): AuthResult {
        return try {
            val credential = GoogleAuthProvider.getCredential(googleIdToken, null)
            firebaseAuth.signInWithCredential(credential).await()
            exchangeToken(userType = null)
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "فشل تسجيل الدخول بـ Google")
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
