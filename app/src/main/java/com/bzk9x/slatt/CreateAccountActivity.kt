package com.bzk9x.slatt

import android.annotation.SuppressLint
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

class CreateAccountActivity : AppCompatActivity() {
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_account)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val isDarkMode = when (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
            android.content.res.Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }

        val slatt: ImageView = findViewById(R.id.slatt)

        if (isDarkMode) {
            val colorMatrix = android.graphics.ColorMatrix(floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f
            ))

            val invert = android.graphics.ColorMatrixColorFilter(colorMatrix)
            slatt.colorFilter = invert
        }

        val emailInput: EditText = findViewById(R.id.email_input)
        val passwordInput: EditText = findViewById(R.id.password_input)
        val createAccountButton: ConstraintLayout = findViewById(R.id.create_account_button)
        val buttonText: TextView = findViewById(R.id.button_text)
        val buttonProgress: ProgressBar = findViewById(R.id.button_progress)
        val togglePasswordMode: TextView = findViewById(R.id.toggle_password_mode)
        val togglePasswordModeView = togglePasswordMode.parent as View

        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        val errorVibrationUtil = ErrorVibrationUtil(this)

        var isPasswordVisible = false

        togglePasswordMode.setOnClickListener {
            if (isPasswordVisible) {
                passwordInput.transformationMethod = PasswordTransformationMethod.getInstance()
                togglePasswordMode.text = "Hide"
            } else {
                passwordInput.transformationMethod = HideReturnsTransformationMethod.getInstance()
                togglePasswordMode.text = "Show"
            }
            isPasswordVisible = !isPasswordVisible
        }

        togglePasswordModeView.post {
            val rect = Rect()
            togglePasswordMode.getHitRect(rect)

            val extraPadding = 40
            rect.top -= extraPadding
            rect.bottom += extraPadding
            rect.left -= extraPadding
            rect.right += extraPadding

            togglePasswordModeView.touchDelegate = TouchDelegate(rect, togglePasswordMode)
        }

        createAccountButton.setOnClickListener { it ->
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            // Clear previous errors
            emailInput.error = null
            passwordInput.error = null

            // Validate email
            if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailInput.error = "Enter a valid email address"
                errorVibrationUtil.doubleVibrate()
                ShakeUtil.shake(emailInput)
                return@setOnClickListener
            }

            // Validate password
            if (password.length < 8 || !password.any { it.isUpperCase() } || !password.any { it.isDigit() } || !password.any { !it.isLetterOrDigit() }) {
                passwordInput.error = "Password must be 8+ characters with an uppercase letter, a digit, and a special character"
                errorVibrationUtil.doubleVibrate()
                ShakeUtil.shake(passwordInput)
                return@setOnClickListener
            }

            it.isEnabled = false
            it.setBackgroundResource(R.drawable.button_disabled)
            buttonText.visibility = View.INVISIBLE
            buttonProgress.visibility = View.VISIBLE

            // Create account
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        if (user != null) {
                            val uid = user.uid
                            val now = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                            val dateJoined = String.format(Locale.US, "%04d-%02d-%02d",
                                now.get(Calendar.YEAR),
                                now.get(Calendar.MONTH) + 1,
                                now.get(Calendar.DAY_OF_MONTH)
                            )
                            val timeJoined = String.format(Locale.US, "%02d:%02d:%02d",
                                now.get(Calendar.HOUR_OF_DAY),
                                now.get(Calendar.MINUTE),
                                now.get(Calendar.SECOND)
                            )

                            val userData = hashMapOf(
                                "uid" to uid,
                                "email" to email,
                                "dateJoined" to dateJoined,
                                "timeJoined" to timeJoined
                            )

                            db.collection("users").document(uid)
                                .set(userData)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Error saving user data: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                        }
                    } else {
                        errorVibrationUtil.doubleVibrate()
                        Toast.makeText(this, "Account creation failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        it.isEnabled = true
                        it.setBackgroundResource(R.drawable.button)
                        buttonText.visibility = View.VISIBLE
                        buttonProgress.visibility = View.INVISIBLE
                    }
                }
        }
    }
}