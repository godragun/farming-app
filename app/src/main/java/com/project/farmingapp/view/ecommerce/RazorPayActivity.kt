package com.project.farmingapp.view.ecommerce

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.project.farmingapp.utilities.AuthHelper
import com.google.firebase.database.FirebaseDatabase
import com.project.farmingapp.R
import com.project.farmingapp.databinding.ActivityRazorPayBinding
import com.project.farmingapp.model.data.orders
import com.project.farmingapp.utilities.formatLkr
import com.project.farmingapp.utilities.parseAmount
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

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
    lateinit var realtimeDatabase: FirebaseDatabase
    var productId: String? = null
    var totalPrice = 0
    var itemCost: Int? = null
    var quantity: Int? = null
    var deliveryCost: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRazorPayBinding.inflate(layoutInflater)
        setContentView(binding.root)
        postId = UUID.randomUUID()

        //val totalCost=intent.getStringExtra("tp")
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
        // Toast.makeText(this,abc,Toast.LENGTH_LONG).show()
//Log.d("tp",abc.toString())
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
            }

            //add inputfield validation

            else {
                // Razorpay SDK removed in this build; place the order directly.
                placeOrder()
            }
        }

        binding.netValue.text =
            "Net Value: " + formatLkr((itemCost ?: 0) * (quantity ?: 1) + (deliveryCost ?: 0))
    }

    private fun placeOrder() {
        realtimeDatabase = FirebaseDatabase.getInstance()

        val orderRef =
            realtimeDatabase.getReference(AuthHelper.requireUid()).child("orders")
                .child("${postId}")

        var date1 = Date()
        val calendar = Calendar.getInstance()
        calendar.time = date1

        val randomDay = (0..12).shuffled().first()
        val displayDateFormat = SimpleDateFormat("dd/MM/yyyy")
        calendar.add(Calendar.DATE, randomDay)
        date1 = calendar.time

        orderRef.setValue(
            orders(
                name,
                locality,
                city,
                state,
                pincode,
                mobile,
                currentDate,
                productId!!,
                itemCost!!,
                quantity!!,
                deliveryCost!!,
                "Arriving By: " + displayDateFormat.format(date1)
            )
        ).addOnCompleteListener {
            Toast.makeText(this, "Order placed", Toast.LENGTH_LONG).show()
            finish()
        }.addOnFailureListener {
            Toast.makeText(this, "Order failed. Please try again.", Toast.LENGTH_LONG).show()
        }
    }


}