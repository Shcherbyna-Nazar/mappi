package com.example.mappi_kt

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mappi_kt.entity.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    private lateinit var usernameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var loginTextView: TextView

    private lateinit var mAuth: FirebaseAuth
    private lateinit var usersRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.register_activity)

        mAuth = FirebaseAuth.getInstance()
        usersRef = FirebaseDatabase.getInstance().reference.child("users")

        usernameEditText = findViewById(R.id.usernameEditText)
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        confirmPasswordEditText = findViewById(R.id.password2EditText)
        val registerButton: Button = findViewById(R.id.createAccButton)

        registerButton.setOnClickListener { registerUser() }

        loginTextView = findViewById(R.id.loginTextView)
        loginTextView.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun registerUser() {
        val username = usernameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString()
        val confirmPassword = confirmPasswordEditText.text.toString()

        if (username.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            Toast.makeText(this, "Please fill in all the fields.", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show()
            return
        }

        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user: FirebaseUser? = mAuth.currentUser
                    if (user != null) {
                        val userId = user.uid
                        val userData = User(username, email)
                        userData.userId = userId
                        usersRef.child(userId).setValue(userData)
                    }

                    Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Registration failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
