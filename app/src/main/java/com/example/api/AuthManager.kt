package com.example.api

import android.content.Context
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.security.MessageDigest

object AuthManager {
    private const val PREFS_NAME = "report_bot_auth_prefs"
    private const val KEY_LOGGED_IN_EMAIL = "logged_in_email"
    
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    private var database: AppDatabase? = null
    private var context: Context? = null

    fun initialize(appContext: Context) {
        try {
            context = appContext
            database = AppDatabase.getDatabase(appContext)

            // Load active logged-in session on startup
            val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val email = prefs.getString(KEY_LOGGED_IN_EMAIL, null)
            if (email != null) {
                // Retrieve user details from database in background
                kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                    try {
                        val dbUser = database?.userDao?.getUserByEmail(email)
                        if (dbUser != null) {
                            _currentUser.value = dbUser
                        } else {
                            // Fallback to clear stale session
                            prefs.edit().remove(KEY_LOGGED_IN_EMAIL).apply()
                        }
                    } catch (t: Throwable) {
                        Log.e("AuthManager", "Gagal mengambil user session dari database: ${t.localizedMessage}", t)
                    }
                }
            }
        } catch (t: Throwable) {
            Log.e("AuthManager", "Gagal inisialisasi AuthManager: ${t.localizedMessage}", t)
        }
    }

    suspend fun signUp(email: String, password: String, fullName: String): AuthResult {
        return withContext(Dispatchers.IO) {
            val db = database ?: return@withContext AuthResult.Error("Database tidak siap")
            
            val trimmedEmail = email.trim().lowercase()
            if (trimmedEmail.isEmpty() || password.isEmpty() || fullName.trim().isEmpty()) {
                return@withContext AuthResult.Error("Mohon lengkapi semua bidang forms secara benar.")
            }

            val existing = db.userDao.getUserByEmail(trimmedEmail)
            if (existing != null) {
                return@withContext AuthResult.Error("Akun dengan email ini sudah terdaftar.")
            }

            val pwdHash = hashPassword(password)
            val newUser = User(
                email = trimmedEmail,
                passwordHash = pwdHash,
                fullName = fullName.trim(),
                isGoogleUser = false
            )
            
            val id = db.userDao.insertUser(newUser)
            val finalUser = newUser.copy(id = id)
            
            saveSession(trimmedEmail)
            _currentUser.value = finalUser
            AuthResult.Success(finalUser)
        }
    }

    suspend fun signIn(email: String, password: String): AuthResult {
        return withContext(Dispatchers.IO) {
            val db = database ?: return@withContext AuthResult.Error("Database tidak siap")
            
            val trimmedEmail = email.trim().lowercase()
            if (trimmedEmail.isEmpty() || password.isEmpty()) {
                return@withContext AuthResult.Error("Email dan password tidak boleh kosong.")
            }

            val user = db.userDao.getUserByEmail(trimmedEmail) ?: return@withContext AuthResult.Error("Akun tidak ditemukan. Silakan mendaftar terlebih dahulu.")
            
            if (user.isGoogleUser) {
                return@withContext AuthResult.Error("Akun ini terdaftar lewat Google Sign-In. Gunakan tombol Google Sign-In.")
            }

            val pwdHash = hashPassword(password)
            if (user.passwordHash == pwdHash) {
                saveSession(trimmedEmail)
                _currentUser.value = user
                AuthResult.Success(user)
            } else {
                AuthResult.Error("Password yang Anda masukkan salah.")
            }
        }
    }

    suspend fun signInWithGoogle(email: String, fullName: String): AuthResult {
        return withContext(Dispatchers.IO) {
            val db = database ?: return@withContext AuthResult.Error("Database tidak siap")
            
            val trimmedEmail = email.trim().lowercase()
            val existing = db.userDao.getUserByEmail(trimmedEmail)
            
            if (existing != null) {
                saveSession(trimmedEmail)
                _currentUser.value = existing
                return@withContext AuthResult.Success(existing)
            }

            // Create a new Google sign in account profile in local DB
            val newUser = User(
                email = trimmedEmail,
                passwordHash = "",
                fullName = fullName.trim(),
                isGoogleUser = true
            )
            val id = db.userDao.insertUser(newUser)
            val finalUser = newUser.copy(id = id)
            
            saveSession(trimmedEmail)
            _currentUser.value = finalUser
            AuthResult.Success(finalUser)
        }
    }

    suspend fun signInWithFacebook(email: String, fullName: String): AuthResult {
        return withContext(Dispatchers.IO) {
            val db = database ?: return@withContext AuthResult.Error("Database tidak siap")
            
            val trimmedEmail = email.trim().lowercase()
            val existing = db.userDao.getUserByEmail(trimmedEmail)
            
            if (existing != null) {
                saveSession(trimmedEmail)
                _currentUser.value = existing
                return@withContext AuthResult.Success(existing)
            }

            // Create a new Facebook sign in account profile in local DB
            val newUser = User(
                email = trimmedEmail,
                passwordHash = "",
                fullName = fullName.trim(),
                isGoogleUser = false,
                isFacebookUser = true
            )
            val id = db.userDao.insertUser(newUser)
            val finalUser = newUser.copy(id = id)
            
            saveSession(trimmedEmail)
            _currentUser.value = finalUser
            AuthResult.Success(finalUser)
        }
    }

    suspend fun resetPassword(email: String, newPassword: String): AuthResult {
        return withContext(Dispatchers.IO) {
            val db = database ?: return@withContext AuthResult.Error("Database tidak siap")
            
            val trimmedEmail = email.trim().lowercase()
            val existing = db.userDao.getUserByEmail(trimmedEmail) ?: return@withContext AuthResult.Error("Email tidak terdaftar.")
            
            if (existing.isGoogleUser || existing.isFacebookUser) {
                return@withContext AuthResult.Error("Gagal mereset: Akun ini terdaftar lewat Media Sosial.")
            }

            if (newPassword.length < 6) {
                return@withContext AuthResult.Error("Kata sandi harus minimal 6 karakter.")
            }

            val pwdHash = hashPassword(newPassword)
            val updatedUser = existing.copy(passwordHash = pwdHash)
            db.userDao.insertUser(updatedUser)
            AuthResult.Success(updatedUser)
        }
    }

    fun signOut() {
        val prefs = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs?.edit()?.remove(KEY_LOGGED_IN_EMAIL)?.apply()
        _currentUser.value = null
    }

    private fun saveSession(email: String) {
        val prefs = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs?.edit()?.putString(KEY_LOGGED_IN_EMAIL, email)?.apply()
    }

    // SHA-256 secure hashing algorithm
    private fun hashPassword(password: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = digest.digest(password.toByteArray(Charsets.UTF_8))
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            // Fallback safe representation
            password.hashCode().toString()
        }
    }
}

sealed class AuthResult {
    data class Success(val user: User) : AuthResult()
    data class Error(val message: String) : AuthResult()
}
