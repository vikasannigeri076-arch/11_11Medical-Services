package com.example.a11_11medicalservies

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.ByteArrayOutputStream
import java.io.InputStream

class ChatbotActivity : AppCompatActivity() {

    private lateinit var rvChatbot: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var btnAttach: ImageButton
    private lateinit var adapter: ChatbotAdapter
    private val messagesList = mutableListOf<AppChatMessage>()

    private var selectedBitmap: Bitmap? = null

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val bitmap = uriToBitmap(it)
            if (bitmap != null) {
                selectedBitmap = bitmap
                Toast.makeText(this, "Photo Selected.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as? Bitmap
            if (imageBitmap != null) {
                selectedBitmap = imageBitmap
                Toast.makeText(this, "Photo Captured.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chatbot)

        rvChatbot = findViewById(R.id.rvChatbot)
        etMessage = findViewById(R.id.etChatbotMessage)
        btnSend = findViewById(R.id.btnChatbotSend)
        btnAttach = findViewById(R.id.btnAttach)

        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar).setNavigationOnClickListener {
            finish()
        }

        setupRecyclerView()

        btnSend.setOnClickListener {
            sendMessage()
        }

        btnAttach.setOnClickListener {
            showAttachOptions()
        }
        
        if (messagesList.isEmpty()) {
            addBotMessage("Hello! I am your 11:11 Assistant. How can I help you today?")
        }
    }

    private fun showAttachOptions() {
        val options = arrayOf("1. Camera", "2. Upload Photo")
        AlertDialog.Builder(this)
            .setTitle("Attach Image")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> cameraLauncher.launch(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
                    1 -> galleryLauncher.launch("image/*")
                }
            }
            .show()
    }

    private fun setupRecyclerView() {
        adapter = ChatbotAdapter(messagesList)
        rvChatbot.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        rvChatbot.adapter = adapter
    }

    private fun sendMessage() {
        val text = etMessage.text.toString().trim()
        if (text.isEmpty() && selectedBitmap == null) return

        val base64Image = selectedBitmap?.let { bitmapToBase64(it) }
        
        val userMsg = AppChatMessage(
            senderId = "User",
            message = text,
            isFromCustomer = true,
            timestamp = System.currentTimeMillis(),
            imageData = base64Image
        )
        
        messagesList.add(userMsg)
        adapter.notifyItemInserted(messagesList.size - 1)
        rvChatbot.smoothScrollToPosition(messagesList.size - 1)
        etMessage.setText("")

        // Mock AI response since API is removed
        addBotMessage("I have received your message. (Note: AI backend is currently offline)")
        
        selectedBitmap = null
    }

    private fun addBotMessage(text: String) {
        val botMsg = AppChatMessage(
            senderId = "AI",
            message = text,
            isFromCustomer = false,
            timestamp = System.currentTimeMillis()
        )
        messagesList.add(botMsg)
        adapter.notifyItemInserted(messagesList.size - 1)
        rvChatbot.smoothScrollToPosition(messagesList.size - 1)
    }

    private fun uriToBitmap(uri: Uri): Bitmap? {
        return try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            null
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos)
        val byteArray = baos.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT).replace("\n", "")
    }

    inner class ChatbotAdapter(private val list: List<AppChatMessage>) :
        RecyclerView.Adapter<ChatbotAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val layout = if (viewType == 1) R.layout.item_chat_customer else R.layout.item_chat_support
            val v = LayoutInflater.from(parent.context).inflate(layout, parent, false)
            return ViewHolder(v)
        }

        override fun getItemViewType(position: Int): Int {
            return if (list[position].isFromCustomer) 1 else 0
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val msg = list[position]
            holder.tvMessage.text = msg.message
            
            if (msg.imageData != null) {
                holder.ivImage.visibility = View.VISIBLE
                try {
                    val decodedString = Base64.decode(msg.imageData, Base64.DEFAULT)
                    val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                    holder.ivImage.setImageBitmap(decodedByte)
                } catch (e: Exception) {
                    holder.ivImage.visibility = View.GONE
                }
            } else {
                holder.ivImage.visibility = View.GONE
            }
        }

        override fun getItemCount() = list.size

        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val tvMessage: TextView = v.findViewById(R.id.tvChatMessage)
            val ivImage: ImageView = v.findViewById(R.id.ivChatImage)
        }
    }
}
