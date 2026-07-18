package com.project.farmingapp.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.project.farmingapp.utilities.AuthHelper
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.project.farmingapp.R
import com.project.farmingapp.utilities.CartItemBuy
import com.project.farmingapp.utilities.LocalCartStore
import com.project.farmingapp.utilities.formatLkr
import com.project.farmingapp.utilities.parseAmount
import com.project.farmingapp.view.ecommerce.CartFragment
import com.project.farmingapp.view.ecommerce.OfflineEcommerceData

class CartItemsAdapter(
    val context: CartFragment,
    val allData: HashMap<String, Any>,
    val cartitembuy: CartItemBuy,
    private val useLocalStore: Boolean = false
) :
    RecyclerView.Adapter<CartItemsAdapter.CartItemsViewHolder>() {
    var itemCost = 0
    var deliveryCharge = 0
    var quantity = 0
    class CartItemsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val buyBtn: com.google.android.material.button.MaterialButton = itemView.findViewById(R.id.cartItemBuyBtn)
        val removeBtn: ImageView = itemView.findViewById(R.id.removeCartBtn)
        val increaseQtyBtn: ImageButton = itemView.findViewById(R.id.increaseQtyBtn)
        val decreaseQtyBtn: ImageButton = itemView.findViewById(R.id.decreaseQtyBtn)

        val quantityText: TextView = itemView.findViewById(R.id.quantityCountEcomm)
        val itemName: TextView = itemView.findViewById(R.id.itemNameCart)
        val itemPrice: TextView = itemView.findViewById(R.id.itemPriceCart)
        val deliveryCharge: TextView = itemView.findViewById(R.id.deliveryChargeCart)
        val firm: TextView = itemView.findViewById(R.id.cartItemFirm)
        val availability: TextView = itemView.findViewById(R.id.cartItemAvailability)
        val image: ImageView = itemView.findViewById(R.id.cartItemImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartItemsViewHolder {
        val view = LayoutInflater.from(context.context).inflate(R.layout.single_cart_item, parent, false)
        return CartItemsAdapter.CartItemsViewHolder(view)
    }

    override fun getItemCount(): Int {
        return allData.size
    }

    override fun onBindViewHolder(holder: CartItemsViewHolder, position: Int) {
        val currentData = allData.entries.toTypedArray()[position]
        val itemId = currentData.key.toString()
        val firebaseDatabase = FirebaseDatabase.getInstance()
        val itemQtyRef = if (!useLocalStore) {
            firebaseDatabase.getReference(AuthHelper.requireUid()).child("cart")
                .child(itemId).child("quantity")
        } else null

        val itemRef = if (!useLocalStore) {
            firebaseDatabase.getReference(AuthHelper.requireUid()).child("cart")
                .child(itemId)
        } else null

        holder.buyBtn.setOnClickListener {
            val qty = holder.quantityText.text.toString().toInt()
            val itemPrice = parseAmount(holder.itemPrice.text.toString())
            val deliveryCharge = holder.deliveryCharge.text.toString().toInt()
            Log.d("totalPrice", quantity.toString())
            Log.d("totalPrice", itemCost.toString())
            Log.d("totalPrice", deliveryCharge.toString())
            cartitembuy.addToOrders("${currentData.key}", qty, itemPrice, deliveryCharge)
        }

        holder.removeBtn.setOnClickListener {
            if (useLocalStore) {
                LocalCartStore.remove(context.requireContext(), itemId)
                allData.remove(itemId)
                notifyDataSetChanged()
            } else {
                itemRef?.removeValue()
            }
        }

        holder.increaseQtyBtn.setOnClickListener {

            holder.quantityText.text =
                (holder.quantityText.text.toString().toInt() + 1).toString()
            val updatedQty = holder.quantityText.text.toString().toInt()
            if (useLocalStore) {
                LocalCartStore.updateQuantity(context.requireContext(), itemId, updatedQty)
            } else {
                itemQtyRef?.setValue(updatedQty)
            }
        }

        holder.decreaseQtyBtn.setOnClickListener {
            if (holder.quantityText.text.toString().toInt() != 1) {
                holder.quantityText.text =
                    (holder.quantityText.text.toString().toInt() - 1).toString()
                val updatedQty = holder.quantityText.text.toString().toInt()
                if (useLocalStore) {
                    LocalCartStore.updateQuantity(context.requireContext(), itemId, updatedQty)
                } else {
                    itemQtyRef?.setValue(updatedQty)
                }
            }
        }

        val curr = currentData.value as Map<String, Any>

        val offlineItem = if (itemId.startsWith("offline_")) {
            OfflineEcommerceData.getItems(context.requireContext()).firstOrNull { it.id == itemId }
        } else null

        if (offlineItem != null) {
            quantity = curr["quantity"].toString().toIntOrNull() ?: 1
            itemCost = offlineItem.price
            deliveryCharge = offlineItem.deliveryCharge
            holder.itemName.text = offlineItem.title
            holder.itemPrice.text = formatLkr(itemCost)
            holder.quantityText.text = quantity.toString()
            holder.deliveryCharge.text = deliveryCharge.toString()
            holder.firm.text = offlineItem.retailer
            holder.availability.text = offlineItem.availability

            if (!offlineItem.imageAssetName.isNullOrBlank()) {
                Glide.with(context).load("file:///android_asset/${offlineItem.imageAssetName}").into(holder.image)
            } else {
                Glide.with(context).load(offlineItem.imageRes).into(holder.image)
            }
            holder.buyBtn.text = "Buy Now: " + formatLkr(itemCost * quantity + deliveryCharge)
            return
        }

        FirebaseFirestore.getInstance().collection("products").document(itemId).get()
            .addOnSuccessListener { it ->
                if (it == null || !it.exists()) {
                    holder.itemName.text = "Unavailable Item"
                    holder.itemPrice.text = formatLkr(0)
                    holder.quantityText.text = (curr["quantity"].toString().toIntOrNull() ?: 1).toString()
                    holder.deliveryCharge.text = "0"
                    holder.firm.text = "Unknown"
                    holder.availability.text = "Unavailable"
                    holder.buyBtn.text = "Buy Now: " + formatLkr(0)
                    holder.image.setImageResource(R.drawable.ic_launcher_foreground)
                    return@addOnSuccessListener
                }

                itemCost = it.get("price").toString().toIntOrNull() ?: 0
                deliveryCharge = it.get("delCharge").toString().toIntOrNull() ?: 0
                quantity = curr["quantity"].toString().toIntOrNull() ?: 1
                holder.itemName.text = it.getString("title").orEmpty().ifBlank { "Product" }
                holder.itemPrice.text = formatLkr(itemCost)
                holder.quantityText.text = quantity.toString()
                holder.deliveryCharge.text = deliveryCharge.toString()
                holder.firm.text = it.get("retailer").toString()
                holder.availability.text = it.get("availability").toString()

                val allImages = it.get("imageUrl") as? ArrayList<String> ?: arrayListOf()
                if (allImages.isNotEmpty()) {
                    Glide.with(context).load(allImages[0]).into(holder.image)
                } else {
                    holder.image.setImageResource(R.drawable.ic_launcher_foreground)
                }
                holder.buyBtn.text = "Buy Now: " + formatLkr(itemCost * quantity + deliveryCharge)
            }
            .addOnFailureListener {
                holder.itemName.text = "Unavailable Item"
                holder.itemPrice.text = formatLkr(0)
                holder.buyBtn.text = "Buy Now: " + formatLkr(0)
                holder.image.setImageResource(R.drawable.ic_launcher_foreground)
            }
    }
}