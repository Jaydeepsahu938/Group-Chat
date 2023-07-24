package com.example.mychatapp

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

@Suppress("DEPRECATION")
class SigningActivity : AppCompatActivity() {

    companion object {
        private const val TAG: String = ""
        private const val RC_SIGN_IN = 1
    }

    private var googleSignInClient: GoogleSignInClient? = null
    private var firebaseAuth: FirebaseAuth? = null
    private lateinit var signInBtn: SignInButton
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signing)

        firebaseAuth = FirebaseAuth.getInstance()

        val gso: GoogleSignInOptions =
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        signInBtn= findViewById(R.id.Signing_button)
        signInBtn.setOnClickListener {
            signIn()
        }

    }


    private fun signIn() {
        val signInIntent: Intent = googleSignInClient!!.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }


    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            if (resultCode == Activity.RESULT_OK) {
                val task: Task<GoogleSignInAccount> =
                    GoogleSignIn.getSignedInAccountFromIntent(data)
                try {
                    val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
                    firebaseWithGoogle(account)
                } catch (e: ApiException) {
                    Log.e(TAG, "Google Sign In Failed $e")
                    Toast.makeText(this, "Google Sign In Failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun firebaseWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)

        firebaseAuth!!.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Authentication Failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

}