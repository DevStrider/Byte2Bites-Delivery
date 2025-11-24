package com.byte2bites.delivery

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.byte2bites.delivery.databinding.ActivityWelcomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class WelcomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWelcomeBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // If already logged in as Buyer, go directly to DeliveryOrdersActivity
        val currentUser = auth.currentUser
        if (currentUser != null) {
            database.reference.child("Buyers").child(currentUser.uid).get()
                .addOnSuccessListener { snap ->
                    if (snap.exists()) {
                        startActivity(Intent(this, DeliveriesActivity::class.java))
                        finish()
                    } else {
                        // Not a buyer -> sign out
                        auth.signOut()
                        setupClicks()
                    }
                }
                .addOnFailureListener {
                    setupClicks()
                }
        } else {
            setupClicks()
        }
    }

    private fun setupClicks() {
        // LOGIN BUTTON
        binding.btnLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}
