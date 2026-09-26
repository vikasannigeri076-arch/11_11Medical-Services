package com.example.a11_11medicalservies

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.database.FirebaseDatabase
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.journeyapps.barcodescanner.BarcodeEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderLabelActivity : AppCompatActivity() {

    private lateinit var tvShopName: TextView
    private lateinit var tvShopAddress: TextView
    private lateinit var tvOrderId: TextView
    private lateinit var tvDateTime: TextView
    private lateinit var tvCustomerName: TextView
    private lateinit var tvCustomerPhone: TextView
    private lateinit var tvDeliveryAddress: TextView
    private lateinit var tvMedicineTotal: TextView
    private lateinit var tvDeliveryCharge: TextView
    private lateinit var tvGrandTotal: TextView
    private lateinit var tvPaymentMethod: TextView
    private lateinit var tvPaymentStatus: TextView
    private lateinit var ivQrCode: ImageView
    private lateinit var labelContainer: View

    private var orderId: String? = null
    private var shopPhone: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_order_label)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbar)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        orderId = intent.getStringExtra("orderId")
        shopPhone = intent.getStringExtra("shopPhone")

        if (orderId == null || shopPhone == null) {
            Toast.makeText(this, "Missing order or shop information", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        fetchData()

        findViewById<View>(R.id.btnCloseLabel).setOnClickListener { finish() }
        findViewById<View>(R.id.btnPrintLabel).setOnClickListener { doPrint() }
        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }
    }

    private fun initViews() {
        tvShopName = findViewById(R.id.tvLabelShopName)
        tvShopAddress = findViewById(R.id.tvLabelShopAddress)
        tvOrderId = findViewById(R.id.tvLabelOrderId)
        tvDateTime = findViewById(R.id.tvLabelDateTime)
        tvCustomerName = findViewById(R.id.tvLabelCustomerName)
        tvCustomerPhone = findViewById(R.id.tvLabelCustomerPhone)
        tvDeliveryAddress = findViewById(R.id.tvLabelDeliveryAddress)
        tvMedicineTotal = findViewById(R.id.tvLabelMedicineTotal)
        tvDeliveryCharge = findViewById(R.id.tvLabelDeliveryCharge)
        tvGrandTotal = findViewById(R.id.tvLabelGrandTotal)
        tvPaymentMethod = findViewById(R.id.tvLabelPaymentMethod)
        tvPaymentStatus = findViewById(R.id.tvLabelPaymentStatus)
        ivQrCode = findViewById(R.id.ivLabelQrCode)
        labelContainer = findViewById(R.id.labelContainer)
    }

    private fun fetchData() {
        val database = FirebaseDatabase.getInstance()

        // Fetch Shop Data
        database.getReference("Shops").child(shopPhone!!).get().addOnSuccessListener { snapshot ->
            val shop = snapshot.getValue(Shop::class.java)
            shop?.let {
                tvShopName.text = it.shopName
                tvShopAddress.text = "${it.address}, ${it.district}"
            }
        }

        // Fetch Order Data
        database.getReference("Orders").child(orderId!!).get().addOnSuccessListener { snapshot ->
            val order = snapshot.getValue(OrderRequest::class.java)
            order?.let {
                val shortId = if (it.id.length > 8) it.id.takeLast(8) else it.id
                tvOrderId.text = "Order ID: #$shortId"
                
                val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                tvDateTime.text = "Date: ${sdf.format(Date(it.timestamp))}"
                
                tvCustomerName.text = "Name: ${it.customerName}"
                tvCustomerPhone.text = "Phone: ${it.customerPhone}"
                tvDeliveryAddress.text = "Address: ${it.district} (Location Tagged)"

                // Fetch customer's full address
                fetchCustomerAddress(it.customerPhone)

                val shopResponse = it.responses[shopPhone]
                if (shopResponse != null) {
                    tvMedicineTotal.text = "₹ %.2f".format(shopResponse.medicineTotal)
                    tvDeliveryCharge.text = "₹ %.2f".format(shopResponse.deliveryCharge)
                    tvGrandTotal.text = "₹ %.2f".format(shopResponse.total)
                } else {
                    tvMedicineTotal.text = "₹ %.2f".format(it.medicineAmount)
                    tvDeliveryCharge.text = "₹ %.2f".format(it.deliveryCharge)
                    tvGrandTotal.text = "₹ %.2f".format(it.grandTotal)
                }

                tvPaymentMethod.text = "Payment: ${it.paymentMethod.ifEmpty { "N/A" }}"
                tvPaymentStatus.text = "Status: ${it.paymentStatus}"

                generateQrCode(it.id)
            }
        }
    }

    private fun fetchCustomerAddress(phone: String) {
        FirebaseDatabase.getInstance().getReference("Customers").child(phone)
            .child("address").get().addOnSuccessListener { snapshot ->
                val address = snapshot.getValue(String::class.java)
                if (!address.isNullOrEmpty()) {
                    tvDeliveryAddress.text = "Address: $address"
                }
            }
    }

    private fun generateQrCode(data: String) {
        try {
            val writer = MultiFormatWriter()
            val matrix = writer.encode(data, BarcodeFormat.QR_CODE, 512, 512)
            val encoder = BarcodeEncoder()
            val bitmap = encoder.createBitmap(matrix)
            ivQrCode.setImageBitmap(bitmap)
        } catch (e: Exception) {
            Log.e("OrderLabelActivity", "Error generating QR", e)
        }
    }

    private fun doPrint() {
        val printManager = getSystemService(Context.PRINT_SERVICE) as PrintManager
        val jobName = "${getString(R.string.app_name)} Document"
        
        // Convert view to bitmap for printing
        val bitmap = createBitmapFromView(labelContainer)
        
        printManager.print(jobName, object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: android.os.CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }

                val info = android.print.PrintDocumentInfo.Builder(jobName)
                    .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()

                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out android.print.PageRange>?,
                destination: android.os.ParcelFileDescriptor?,
                cancellationSignal: android.os.CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                val pdfDocument = android.graphics.pdf.PdfDocument()
                val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(
                    labelContainer.width,
                    labelContainer.height,
                    1
                ).create()
                
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                labelContainer.draw(canvas)
                pdfDocument.finishPage(page)

                try {
                    pdfDocument.writeTo(java.io.FileOutputStream(destination?.fileDescriptor))
                } catch (e: java.io.IOException) {
                    callback?.onWriteFailed(e.toString())
                    return
                } finally {
                    pdfDocument.close()
                }

                callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
            }
        }, null)
    }

    private fun createBitmapFromView(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        view.draw(canvas)
        return bitmap
    }
}
