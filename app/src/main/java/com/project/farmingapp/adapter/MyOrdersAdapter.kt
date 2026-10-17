package com.project.farmingapp.adapter

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.project.farmingapp.R
import com.project.farmingapp.utilities.CartItemBuy
import com.project.farmingapp.utilities.CellClickListener
import com.project.farmingapp.utilities.formatLkr
import com.project.farmingapp.view.ecommerce.MyOrdersFragment
import com.project.farmingapp.view.ecommerce.RazorPayActivity
import com.project.farmingapp.viewmodel.EcommViewModel
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.HashMap

class MyOrdersAdapter(
    val context: MyOrdersFragment,
    val allData: HashMap<String, Object>,
    val cellClickListener: CellClickListener,
    val cartItemBuy: CartItemBuy
) : RecyclerView.Adapter<MyOrdersAdapter.MyOrdersViewHolder>() {
    class MyOrdersViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val pinCode: TextView = itemView.findViewById(R.id.myOrderPinCode)
        val itemName: TextView = itemView.findViewById(R.id.myOrderItemName)
        val itemPrice: TextView = itemView.findViewById(R.id.myOrderItemPrice)
        val deliveryCharge: TextView = itemView.findViewById(R.id.myOderDeliveryCharge)
        val deliveryStatus: TextView = itemView.findViewById(R.id.myOrderDeliveryStatus)
        val image: ImageView = itemView.findViewById(R.id.myOderItemImage)
        val timeStamp: TextView = itemView.findViewById(R.id.myOrderTimeStamp)
        val purchaseAgain: View = itemView.findViewById(R.id.myOrderPurchaseAgain)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyOrdersViewHolder {
        val view = LayoutInflater.from(context.context)
            .inflate(R.layout.single_myorder_item, parent, false)
        return MyOrdersViewHolder(view)
    }

    override fun getItemCount(): Int {
        return allData.size
    }

    override fun onBindViewHolder(holder: MyOrdersViewHolder, position: Int) {
        val entries = allData.entries.toList()
        val raw = entries.getOrNull(position)?.value as? Map<*, *> ?: return
        fun field(key: String): String = raw[key]?.toString().orEmpty()
        fun number(key: String): Int =
            raw[key]?.toString()?.trim()?.toDoubleOrNull()?.toInt() ?: 0

        val productId = field("productId")
        val quantity = number("quantity").coerceAtLeast(1)
        val itemCost = number("itemCost")
        val deliveryCost = number("deliveryCost")

        holder.pinCode.text = "Pin Code: " + field("pincode")
        holder.itemPrice.text = formatLkr(quantity * itemCost + deliveryCost)
        holder.deliveryCharge.text = deliveryCost.toString()
        holder.deliveryStatus.text = field("deliveryStatus")
        holder.timeStamp.text = field("time").split(" ").firstOrNull().orEmpty()
        holder.itemName.text = field("name").ifBlank { "Product" }
        holder.image.setImageResource(R.drawable.ic_launcher_foreground)

        // Product title/image: offline catalogue items are resolved locally, others from the server.
        val appContext = holder.itemView.context
        if (productId.startsWith("offline_")) {
            val offline = try {
                com.project.farmingapp.view.ecommerce.OfflineEcommerceData.getItems(appContext)
                    .firstOrNull { it.id == productId }
            } catch (_: Exception) { null }
            if (offline != null) {
                holder.itemName.text = offline.title
                if (!offline.imageAssetName.isNullOrBlank()) {
                    Glide.with(appContext).load("file:///android_asset/${offline.imageAssetName}").into(holder.image)
                } else {
                    holder.image.setImageResource(offline.imageRes)
                }
            }
        } else if (productId.isNotBlank()) {
            try {
                val owner = context.viewLifecycleOwner
                EcommViewModel().getSpecificItem(productId).observe(owner, androidx.lifecycle.Observer { doc ->
                    if (doc == null) return@Observer
                    // The holder may have been recycled for another order in the meantime.
                    if (holder.bindingAdapterPosition != position) return@Observer
                    try {
                        doc.getString("title")?.takeIf { it.isNotBlank() }?.let { holder.itemName.text = it }
                        val firstImage = (doc.get("imageUrl") as? List<*>)?.firstOrNull()?.toString()
                        if (!firstImage.isNullOrBlank()) {
                            Glide.with(appContext).load(firstImage).into(holder.image)
                        }
                    } catch (e: Exception) {
                        Log.w("MyOrdersAdapter", "Could not show product details", e)
                    }
                })
            } catch (e: Exception) {
                Log.w("MyOrdersAdapter", "Could not load product details", e)
            }
        }

        holder.purchaseAgain.setOnClickListener {
            cartItemBuy.addToOrders(productId, quantity, itemCost, deliveryCost)
        }
        holder.itemView.setOnClickListener {
            cellClickListener.onCellClickListener(productId)
        }
    }
}