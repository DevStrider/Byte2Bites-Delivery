package com.byte2bites.delivery

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.byte2bites.delivery.databinding.ActivityProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

data class DeliveryUser(
    val fullName: String? = null,
    val email: String? = null,
    val photoUrl: String? = null,
    val deliveryPoints: Int? = null
)

class ProfileActivity : AppCompatActivity() {

    private lateinit var b: ActivityProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(b.root)

        supportActionBar?.hide()

        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance()

        // Back button (top-left)
        b.ivBack.setOnClickListener { finish() }

        val user = auth.currentUser
        if (user == null) {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
            return
        }

        loadProfile()
        setupButtons()
    }

    private fun loadProfile() {
        val uid = auth.currentUser?.uid ?: return

        val userRef = db.reference.child("Buyers").child(uid)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return

                val fullName = snapshot.child("fullName").getValue(String::class.java)
                val email = snapshot.child("email").getValue(String::class.java)
                val photoUrl = snapshot.child("photoUrl").getValue(String::class.java)
                val points = snapshot.child("points").getValue(Int::class.java) ?: 0

                b.tvUserName.text = fullName ?: "Delivery User"
                b.tvUserEmail.text = email ?: ""
                b.tvPoints.text = "$points pts"

                if (!photoUrl.isNullOrEmpty()) {
                    Glide.with(this@ProfileActivity)
                        .load(photoUrl)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .error(R.drawable.ic_profile_placeholder)
                        .circleCrop()
                        .into(b.ivProfilePicture)
                } else {
                    b.ivProfilePicture.setImageResource(R.drawable.ic_profile_placeholder)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@ProfileActivity,
                    "Failed to load profile: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun setupButtons() {
        // Logout → back to Welcome
        b.btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, WelcomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
