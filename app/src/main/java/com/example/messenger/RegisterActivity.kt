package com.example.messenger

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.example.messenger.databinding.ActivityRegisterBinding
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit

class RegisterActivity : AppCompatActivity() {

    lateinit var binding: ActivityRegisterBinding

    lateinit var userName: EditText
    lateinit var email: EditText
    lateinit var registerBtn: Button

//    var imageUrl: String = "https://thumbs.dreamstime.com/b/default-avatar-profile-vector-user-profile-default-avatar-profile-vector-user-profile-profile-179376714.jpg"
    var imageUri: Uri? = null
    var user: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_register)
        supportActionBar?.title = "Register Account"

        userName = binding.usernameEdit
        registerBtn = binding.registerBtn
        email = binding.emailEdit
        binding.selectPhotoBtn.alpha = 0F
        binding.selectPhotoText.visibility = View.GONE

        Glide.with(this)
            .load("https://thumbs.dreamstime.com/b/default-avatar-profile-vector-user-profile-default-avatar-profile-vector-user-profile-profile-179376714.jpg")
            .placeholder(R.drawable.ic_placeholder_person_24)
            .into(binding.circleImage)

        binding.signinText.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
//            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }

        binding.selectPhotoBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 0)
        }

        registerBtn.setOnClickListener {
            doRegister()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0 && resultCode == Activity.RESULT_OK && data != null){
            Timber.i("scheme is ${data.data?.scheme}")
            imageUri = data.data
            Timber.i("imageUri is $imageUri")
            Glide.with(this)
                .load(imageUri)
                .into(binding.circleImage)
            binding.selectPhotoBtn.alpha = 0F
            binding.selectPhotoText.visibility = View.GONE
        }
    }

    private fun doRegister(){
        if (userName.text.isEmpty()){
            Toast.makeText(this, "Invalid input", Toast.LENGTH_SHORT).show()
            return
        }
        else if (imageUri == null){
            Toast.makeText(applicationContext, "Enter a photo to continue.", Toast.LENGTH_LONG).show()
            return
        }

        val emailText = email.text.toString()
        Toast.makeText(this, "Processing input...", Toast.LENGTH_SHORT).show()


        firebasePhoneAuth.verifyPhoneNumber(emailText, 60, TimeUnit.SECONDS, this, object: PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                firebaseAuth.signInWithCredential(credential)
                    .addOnSuccessListener {
                        Timber.d("Success.")
                        val displayName = it.user?.displayName
                        Timber.i("firebaseAuth.currentUser?.displayName is ${firebaseAuth.currentUser?.displayName} and it.user?.displayName is ${it.user?.displayName}")
                        val uid = it.user?.uid
                        Timber.i("firebaseAuth.uid is ${firebaseAuth.uid} and it.user?.uid is ${it.user?.uid}")
                        Timber.i("displayName is $displayName and uid is $uid")
                        uploadImageToFireStoreStorage()
                        Toast.makeText(this@RegisterActivity, "Success. You have signed up.", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Timber.e(it)
                        Toast.makeText(this@RegisterActivity, "Error occurred.", Toast.LENGTH_SHORT).show()
                    }
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                storedVerificationId = verificationId
                resendingToken = token
            }

            override fun onVerificationFailed(error: FirebaseException) {
                Timber.e(error)
            }
        })
    }

    private fun uploadImageToFireStoreStorage(){
        if (imageUri == null)return

        val fileName = UUID.randomUUID().toString()
        val ref = firebaseStorage.getReference("/images/$fileName")

        ref.putFile(imageUri!!).addOnSuccessListener {
            ref.downloadUrl.addOnSuccessListener {
                Timber.i("downloadUrl is $it")
                saveUserToFirebaseDatabase(it.toString())
            }
        }
    }

    private fun saveUserToFirebaseDatabase(onlineImageUrl: String){
        val uid = firebaseAuth.uid
        val ref = firebaseDatabase.getReference("/users/${uid}")
        Timber.i("ref is $ref")
        val user = uid?.let { User(binding.usernameEdit.text.toString(), it, onlineImageUrl, null, binding.emailEdit.text.toString()) }
        ref.setValue(user)
        val intent = Intent(this, LatestMessagesActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        Toast.makeText(this, "Successfully signed up.", Toast.LENGTH_LONG).show()
    }
}