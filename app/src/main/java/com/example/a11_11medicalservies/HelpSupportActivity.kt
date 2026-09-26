package com.example.a11_11medicalservies

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HelpSupportActivity : AppCompatActivity() {

    private lateinit var rvChat: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var adapter: ChatAdapter
    private val messagesList = mutableListOf<AppChatMessage>()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance()
    private var chatId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_help_support)

        rvChat = findViewById(R.id.rvChat)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)

        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar).setNavigationOnClickListener {
            finish()
        }

        val phone = auth.currentUser?.phoneNumber?.replace("+91", "") ?: ""
        chatId = "Chat_$phone"

        setupRecyclerView()
        listenForMessages()

        btnSend.setOnClickListener {
            sendMessage()
        }
    }

    private fun setupRecyclerView() {
        adapter = ChatAdapter(messagesList)
        rvChat.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        rvChat.adapter = adapter
    }

    private fun listenForMessages() {
        db.getReference("SupportChats").child(chatId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    messagesList.clear()
                    for (snap in snapshot.children) {
                        val msg = snap.getValue(AppChatMessage::class.java)
                        if (msg != null) messagesList.add(msg)
                    }
                    adapter.notifyDataSetChanged()
                    if (messagesList.isNotEmpty()) {
                        rvChat.smoothScrollToPosition(messagesList.size - 1)
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun sendMessage() {
        val text = etMessage.text.toString().trim()
        if (text.isEmpty()) return

        val msg = AppChatMessage(
            senderId = auth.currentUser?.uid ?: "",
            message = text,
            timestamp = System.currentTimeMillis(),
            isFromCustomer = true
        )

        db.getReference("SupportChats").child(chatId).push().setValue(msg)
            .addOnSuccessListener {
                etMessage.setText("")
            }
    }

    inner class ChatAdapter(private val list: List<AppChatMessage>) :
        RecyclerView.Adapter<ChatAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val layout = if (viewType == 1) R.layout.item_chat_customer else R.layout.item_chat_support
            val v = LayoutInflater.from(parent.context).inflate(layout, parent, false)
            return ViewHolder(v)
        }

        override fun getItemViewType(position: Int): Int {
            return if (list[position].isFromCustomer) 1 else 0
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.tvMessage.text = list[position].message
        }

        override fun getItemCount() = list.size

        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val tvMessage: TextView = v.findViewById(R.id.tvChatMessage)
        }
    }
}