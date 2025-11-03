package com.bzk9x.slatt

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Rect
import android.icu.util.Calendar
import android.icu.util.TimeZone
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.TouchDelegate
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bzk9x.slatt.utils.ErrorVibrationUtil
import com.bzk9x.slatt.utils.ShakeUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale
import androidx.core.content.edit

class CreateAccountActivity : AppCompatActivity() {

    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var createAccountButton: ConstraintLayout
    private lateinit var buttonText: TextView
    private lateinit var buttonProgress: ProgressBar
    private lateinit var togglePasswordMode: TextView

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var errorVibrationUtil: ErrorVibrationUtil

    private var isPasswordVisible = false

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_account)

        setupWindowInsets()
        initializeViews()
        setupDarkMode()
        setupPasswordToggle()
        setupCreateAccountButton()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun initializeViews() {
        emailInput = findViewById(R.id.email_input)
        passwordInput = findViewById(R.id.password_input)
        createAccountButton = findViewById(R.id.create_account_button)
        buttonText = findViewById(R.id.button_text)
        buttonProgress = findViewById(R.id.button_progress)
        togglePasswordMode = findViewById(R.id.toggle_password_mode)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        errorVibrationUtil = ErrorVibrationUtil(this)
    }

    private fun setupDarkMode() {
        val isDarkMode = resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES

        if (isDarkMode) {
            val slatt: ImageView = findViewById(R.id.slatt)
            val colorMatrix = ColorMatrix(floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f
            ))
            slatt.colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupPasswordToggle() {
        togglePasswordMode.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            passwordInput.transformationMethod = if (isPasswordVisible) {
                togglePasswordMode.text = "Show"
                HideReturnsTransformationMethod.getInstance()
            } else {
                togglePasswordMode.text = "Hide"
                PasswordTransformationMethod.getInstance()
            }
        }

        val togglePasswordModeView = togglePasswordMode.parent as View
        togglePasswordModeView.post {
            val rect = Rect()
            togglePasswordMode.getHitRect(rect)
            val extraPadding = 40
            rect.apply {
                top -= extraPadding
                bottom += extraPadding
                left -= extraPadding
                right += extraPadding
            }
            togglePasswordModeView.touchDelegate = TouchDelegate(rect, togglePasswordMode)
        }
    }

    private fun setupCreateAccountButton() {
        createAccountButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            emailInput.error = null
            passwordInput.error = null

            if (!validateEmail(email)) return@setOnClickListener
            if (!validatePassword(password)) return@setOnClickListener

            setButtonLoading(true)
            createAccount(email, password)
        }
    }

    private fun validateEmail(email: String): Boolean {
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.error = "Enter a valid email address"
            errorVibrationUtil.doubleVibrate()
            ShakeUtil.shake(emailInput)
            return false
        }
        return true
    }

    private fun validatePassword(password: String): Boolean {
        if (password.length < 8 ||
            !password.any { it.isUpperCase() } ||
            !password.any { it.isDigit() } ||
            !password.any { !it.isLetterOrDigit() }) {
            passwordInput.error = "Password must be 8+ characters with an uppercase letter, a digit, and a special character"
            errorVibrationUtil.doubleVibrate()
            ShakeUtil.shake(passwordInput)
            return false
        }
        return true
    }

    private fun setButtonLoading(isLoading: Boolean) {
        createAccountButton.isEnabled = !isLoading
        createAccountButton.setBackgroundResource(
            if (isLoading) R.drawable.button_disabled else R.drawable.button
        )
        buttonText.visibility = if (isLoading) View.INVISIBLE else View.VISIBLE
        buttonProgress.visibility = if (isLoading) View.VISIBLE else View.INVISIBLE
    }

    private fun createAccount(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    auth.currentUser?.let { user ->
                        saveUserData(user.uid, email)
                    }
                } else {
                    handleAccountCreationError(task.exception?.message)
                }
            }
    }

    private fun saveUserData(uid: String, email: String) {
        val now = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val userData = hashMapOf(
            "uid" to uid,
            "email" to email,
            "dateJoined" to String.format(Locale.US, "%04d-%02d-%02d",
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH) + 1,
                now.get(Calendar.DAY_OF_MONTH)
            ),
            "timeJoined" to String.format(Locale.US, "%02d:%02d:%02d",
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
                now.get(Calendar.SECOND)
            )
        )

        db.collection("users").document(uid)
            .set(userData)
            .addOnSuccessListener {
                val sharedPrefs = getSharedPreferences("Slatt", MODE_PRIVATE)
                sharedPrefs.edit {
                    putBoolean("hasCreatedUsername", false)
                }
                val createUsernameActivity = Intent(this, CreateUsernameActivity::class.java)
                createUsernameActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(createUsernameActivity)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error saving user data: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun handleAccountCreationError(message: String?) {
        errorVibrationUtil.doubleVibrate()
        Toast.makeText(this, "Account creation failed: $message", Toast.LENGTH_LONG).show()
        setButtonLoading(false)
    }
}