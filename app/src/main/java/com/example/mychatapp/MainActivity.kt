@file:Suppress("DEPRECATION")

package com.example.mychatapp

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.firebase.ui.database.SnapshotParser
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import de.hdodenhof.circleimageview.CircleImageView

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener{

    companion object{
      const val TAG:String="MainActivity"
        const val ANONYMOUS="anonymous"
        const val MESSAGE_CHILD="messages"
        const val REQUEST_IMAGE=1
        const val LOADING_IMAGE_URL="https://upload.wikimedia.org/wikipedia/commons/b/b1/Loading_icon.gif"
    }
    override fun onConnectionFailed(p0: ConnectionResult) {
       Log.e(TAG,"OnConnectionFailedListener $p0")
        Toast.makeText(this,"Google Play Service Error",Toast.LENGTH_SHORT).show()
    }

    private var userName:String?=null
    private var userPhotoUrl:String?=null
    private var progressBar:ProgressBar?=null
     lateinit var recyclerView:RecyclerView
     lateinit var sendBtn:Button
     lateinit var text_message_edit_text:TextView
     lateinit var send_image:ImageView


    private var fireBaseAuth:FirebaseAuth?=null
    private var fireBaseUser:FirebaseUser?=null
    private var googleApiClient:GoogleApiClient?=null
    private var firebaseDatabaseReference:DatabaseReference?= null
    private var firebaseAdapter:FirebaseRecyclerAdapter<Message,MessageViewHolder>?=null
    private var googleSignInClient: GoogleSignInClient?=null

    lateinit var linearLayoutManager: LinearLayoutManager
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sendBtn=findViewById(R.id.send_button)
        text_message_edit_text=findViewById(R.id.text_message_edit_text)
        progressBar=findViewById(R.id.progress_bar)
        send_image=findViewById(R.id.add_image_image_view)


         recyclerView=findViewById(R.id.recycler_view)
         linearLayoutManager= LinearLayoutManager(this)
        linearLayoutManager.stackFromEnd=true
        firebaseDatabaseReference=FirebaseDatabase.getInstance().reference

        googleApiClient=GoogleApiClient.Builder(this)
            .enableAutoManage(this,this)
            .addApi(Auth.GOOGLE_SIGN_IN_API)
            .build()

        userName= ANONYMOUS

        fireBaseAuth=FirebaseAuth.getInstance()
        fireBaseUser=fireBaseAuth!!.currentUser
       if(fireBaseUser==null)
       {
           startActivity(Intent(this,SigningActivity::class.java))
           finish()
       }
        else
       {
           userName=fireBaseUser!!.displayName
           if(fireBaseUser!!.photoUrl != null)
           {
               userPhotoUrl=fireBaseUser!!.photoUrl!!.toString()
           }

       }

        val gso=GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient=GoogleSignIn.getClient(this,gso)
        val parser = SnapshotParser<Message>{
            snapshot:DataSnapshot ->
            val chatMessage = snapshot.getValue(Message::class.java)
            if(chatMessage!=null)
            {
                chatMessage.Id=snapshot.key
            }
            chatMessage!!
        }
        val messageRefs=firebaseDatabaseReference!!.child(MESSAGE_CHILD)
        val option= FirebaseRecyclerOptions.Builder<Message>()
            .setQuery(messageRefs,parser)
            .build()
        firebaseAdapter=object :FirebaseRecyclerAdapter<Message,MessageViewHolder>(option){
            override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): MessageViewHolder {
               val inflater=LayoutInflater.from(viewGroup.context)
                return MessageViewHolder(inflater.inflate(R.layout.item_message,viewGroup,false))
            }

            override fun onBindViewHolder(holder: MessageViewHolder, p1: Int, model: Message) {
                progressBar!!.visibility=ProgressBar.INVISIBLE
                holder.bind(model)
            }

        }
        firebaseAdapter!!.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)

                val messageCount:Int=firebaseAdapter!!.itemCount
                val lastVisiblePosition=linearLayoutManager.findLastCompletelyVisibleItemPosition()


                if(lastVisiblePosition == -1 || positionStart >=messageCount-1 && lastVisiblePosition==positionStart-1)
                    recyclerView.scrollToPosition(positionStart)
            }
        })

        recyclerView.layoutManager=linearLayoutManager
        recyclerView.adapter=firebaseAdapter

        sendBtn.setOnClickListener{
            val message=Message(text_message_edit_text.text.toString(),userName!!,userPhotoUrl,null)
            firebaseDatabaseReference!!.child(MESSAGE_CHILD).push().setValue(message)
        }

        send_image.setOnClickListener{
            val intent=Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)

            intent.type="image/*"
            startActivityForResult(intent, REQUEST_IMAGE)

        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode== REQUEST_IMAGE)
            if(resultCode==Activity.RESULT_OK){
                if(data!=null)
                {
                    val uri =data.data
                    val tempMessage=Message(null,userName, userPhotoUrl,uri.toString())
                    firebaseDatabaseReference!!.child(MESSAGE_CHILD).push().setValue(tempMessage){
                        databaseError,databaseReference->
                        if(databaseError == null){
                            val key=databaseReference.key
                            val storageReference=FirebaseStorage.getInstance()
                                .getReference(fireBaseUser!!.uid)
                                .child(key!!)
                                .child(uri!!.lastPathSegment!!)
                            putImageInStorage(storageReference,uri,key)
                        }
                        else{
                            Log.e(TAG,"Unable to write message to database ${databaseError.toException()}")
                        }
                    }
                }
            }
    }

    private fun putImageInStorage(storageReference: StorageReference,uri: Uri?,key:String?){
        val uploadTask=storageReference.putFile(uri!!)
        uploadTask.continueWithTask{task->
            if(!task.isSuccessful) {
                throw task.exception!!
            }
            storageReference.downloadUrl
        }.addOnCompleteListener{
            task->
            if(task.isSuccessful){
                val downloadUrl =task.result!!.toString()
                val message=Message(null,userName,userPhotoUrl,downloadUrl)
                firebaseDatabaseReference!!.child(MESSAGE_CHILD).child(key!!).setValue(message)
            }else{
                Log.e(TAG,"Image Upload Task is not Successful ${task.exception}")
            }
        }

    }
    class MessageViewHolder(v: View):RecyclerView.ViewHolder(v){

       lateinit var message:Message
        var messageTextView:TextView
        var messageImageView:ImageView
        var nameTextView:TextView
        var userImage:CircleImageView

        init {
              messageTextView=itemView.findViewById(R.id.message_text_view)
              messageImageView=itemView.findViewById(R.id.message_image_view)
              nameTextView=itemView.findViewById(R.id.name_text_view)
              userImage=itemView.findViewById(R.id.user_image_view)
        }
        fun bind(message: Message){
            this.message=message
            if(message.text!=null)
            {
                messageTextView.text=message.text
                messageTextView.visibility=View.VISIBLE
                messageImageView.visibility=View.GONE
            }else if(message.imageUrl!=null){
                messageTextView.visibility=View.GONE
                messageImageView.visibility=View.VISIBLE

                val imageUrl = message.imageUrl
                if(imageUrl!!.startsWith("gs://")){
                    val storageReference=FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)

                    storageReference.downloadUrl.addOnCompleteListener{task->
                        if(task.isSuccessful) {
                            val downlaodUrl = task.result!!.toString()
                            Glide.with(messageImageView.context)
                                .load(downlaodUrl)
                                .into(messageImageView)
                        }else{
                            Log.e(TAG,"Getting download url is not successful ${task.exception}")
                        }
                    }
                }
                else{
                    Glide.with(messageImageView.context)
                        .load(Uri.parse(message.imageUrl))
                        .into(messageImageView)
                    }
            }

            nameTextView.text=message.name
            if(message.photoUrl==null){
                userImage.setImageDrawable(ContextCompat.getDrawable(userImage.context,R.drawable.ic_account_circle))
            }else
            {
                Glide.with(userImage.context)
                    .load(Uri.parse(message.photoUrl))
                    .into(userImage)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        firebaseAdapter!!.startListening()
    }

    override fun onResume() {
        super.onResume()
        firebaseAdapter!!.startListening()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
      val inflater=menuInflater
        inflater.inflate(R.menu.menu,menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item?.itemId) {
            R.id.sign_out_item -> {
                fireBaseAuth!!.signOut()
                fireBaseAuth = null
                userName = ANONYMOUS
                userPhotoUrl = null

                googleSignInClient!!.revokeAccess().addOnCompleteListener(this) {
                    startActivity(Intent(this, SigningActivity::class.java))
                    finish()
                }
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }

    }
}