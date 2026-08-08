package com.project.farmingapp.view.ecommerce

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.project.farmingapp.databinding.ActivityRazorPayBinding
import com.project.farmingapp.supabase.SupabaseDb
import com.project.farmingapp.utilities.AuthHelper
import com.project.farmingapp.utilities.LocalOrderStore
import com.project.farmingapp.utilities.formatLkr
import com.project.farmingapp.utilities.parseAmount
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

class RazorPayActivity : AppCompatActivity() {
    val sdf = SimpleDateFormat("dd/M/yyyy hh:mm:ss")
    private lateinit var binding: ActivityRazorPayBinding

    var postId: UUID? = null
    var name: String = ""
    var locality: String = ""
    var city: String = ""
    var state: String = ""
    var pincode: String = ""
    var mobile: String = ""
    var currentDate = sdf.format(Date())
    var productId: String? = null
    var totalPrice = 0
    var itemCost: Int? = null
    var quantity: Int? = null
    var deliveryCost: Int? = null
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRazorPayBinding.inflate(layoutInflater)
        setContentView(binding.root)
        postId = UUID.randomUUID()

        productId = intent.getStringExtra("productId") ?: ""
        itemCost = parseAmount(intent.getStringExtra("itemCost").orEmpty())
        quantity = intent.getStringExtra("quantity")?.toIntOrNull() ?: 1
        deliveryCost = parseAmount(intent.getStringExtra("deliveryCost").orEmpty())
        if (productId.isNullOrBlank() || productId == "null") {
            Toast.makeText(this, "Unable to open product checkout.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        Log.d("tp", productId.toString())
        Log.d("tp", itemCost.toString())
        Log.d("tp", quantity.toString())
        Log.d("tp", deliveryCost.toString())

        binding.orderNowBtn.setOnClickListener {
            name = binding.fullNamePrePay.text.toString()
            locality = binding.localityPrePay.text.toString()
            city = binding.cityPrePay.text.toString()
            state = binding.statePrePay.text.toString()
            pincode = binding.pincodePrePay.text.toString()
            mobile = binding.mobileNumberPrePay.text.toString()
            if (name.isNullOrEmpty() ||
                locality.isNullOrEmpty() ||
                city.isNullOrEmpty() ||
                state.isNullOrEmpty() ||
                pincode.isNullOrEmpty() ||
                mobile.isNullOrEmpty()
            ) {
                Toast.makeText(this, "Please Add all Fields", Toast.LENGTH_LONG).show()
            } else {
                placeOrder()
            }
        }

        binding.netValue.text =
            "Net Value: " + formatLkr((itemCost ?: 0) * (quantity ?: 1) + (deliveryCost ?: 0))
    }

    private fun placeOrder() {
        var date1 = Date()
        val calendar = Calendar.getInstance()
        calendar.time = date1

        val randomDay = (0..12).shuffled().first()
        val displayDateFormat = SimpleDateFormat("dd/MM/yyyy")
        calendar.add(Calendar.DATE, randomDay)
        date1 = calendar.time
        val deliveryStatus = "Arriving By: " + displayDateFormat.format(date1)

        val orderFields = linkedMapOf<String, Any?>(
            "name" to name,
            "locality" to locality,
            "city" to city,
            "state" to state,
            "pincode" to pincode,
            "mobile" to mobile,
            "time" to currentDate,
            "productId" to productId,
            "itemCost" to itemCost,
            "quantity" to quantity,
            "deliveryCost" to deliveryCost,
            "deliveryStatus" to deliveryStatus
        )

        if (AuthHelper.guestMode) {
            LocalOrderStore.add(this, postId.toString(), orderFields)
            Toast.makeText(this, "Order placed (offline)", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val uid = AuthHelper.requireUid()
        val totalCost = (itemCost ?: 0) * (quantity ?: 1) + (deliveryCost ?: 0)
        val address = "$locality, $city, $state - $pincode"
        val raw = JSONObject()
        orderFields.forEach { (k, v) -> raw.put(k, v ?: JSONObject.NULL) }

        executor.execute {
            try {
                val row = JSONObject()
                    .put("uid", uid)
                    .put("product_id", productId)
                    .put("quantity", quantity)
                    .put("cost", totalCost.toString())
                    .put("status", deliveryStatus)
                    .put("address", address)
                    .put("phone", mobile)
                    .put("time_ms", System.currentTimeMillis())
                    .put("raw", raw)
                SupabaseDb.insert("orders", row)
                runOnUiThread {
                    Toast.makeText(this, "Order placed", Toast.LENGTH_LONG).show()
                    finish()
                }
            } catch (_: Exception) {
                LocalOrderStore.add(this, postId.toString(), orderFields)
                runOnUiThread {
                    Toast.makeText(this, "Order saved offline.", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }
}
