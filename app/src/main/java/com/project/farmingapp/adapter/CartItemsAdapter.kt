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
import com.project.farmingapp.R
import com.project.farmingapp.supabase.SupabaseDb
import com.project.farmingapp.utilities.AuthHelper
import com.project.farmingapp.utilities.CartItemBuy
import com.project.farmingapp.utilities.LocalCartStore
import com.project.farmingapp.utilities.formatLkr
import com.project.farmingapp.utilities.parseAmount
import com.project.farmingapp.view.ecommerce.CartFragment
import com.project.farmingapp.view.ecommerce.OfflineEcommerceData
import org.json.JSONObject
import java.util.concurrent.Executors

class CartItemsAdapter(
    val context: CartFragment,
    val allData: HashMap<String, Any>,
    val cartitembuy: CartItemBuy,
    private val useLocalStore: Boolean = false
) : RecyclerView.Adapter<CartItemsAdapter.CartItemsViewHolder>() {

    var itemCost = 0
    var deliveryCharge = 0
    var quantity = 0
    private val executor = Executors.newSingleThreadExecutor()

    /** Runs [block] on the UI thread only if the cart screen is still showing; never throws. */
    private fun ui(block: () -> Unit) {
        val act = context.activity ?: return
        act.runOnUiThread {
            if (context.isAdded && context.view != null) {
                try { block() } catch (e: Exception) { Log.w("CartItemsAdapter", "UI update failed", e) }
            }
        }
    }

    private fun appContext(): android.content.Context? = context.context?.applicationContext

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

    override fun getItemCount(): Int = allData.size

    override fun onBindViewHolder(holder: CartItemsViewHolder, position: Int) {
        val currentData = allData.entries.toTypedArray()[position]
        val itemId = currentData.key.toString()
        val uid = try {
            AuthHelper.requireUid()
        } catch (_: Exception) {
            AuthHelper.GUEST_UID
        }

        holder.buyBtn.setOnClickListener {
            // Text is empty until the product details finish loading; do not crash on early taps.
            val qty = holder.quantityText.text.toString().toIntOrNull() ?: return@setOnClickListener
            val itemPrice = parseAmount(holder.itemPrice.text.toString())
            val deliveryCharge = holder.deliveryCharge.text.toString().toIntOrNull() ?: 0
            Log.d("totalPrice", quantity.toString())
            Log.d("totalPrice", itemCost.toString())
            Log.d("totalPrice", deliveryCharge.toString())
            cartitembuy.addToOrders("${currentData.key}", qty, itemPrice, deliveryCharge)
        }

        holder.removeBtn.setOnClickListener {
            if (useLocalStore) {
                appContext()?.let { LocalCartStore.remove(it, itemId) }
                allData.remove(itemId)
                notifyDataSetChanged()
            } else {
                executor.execute {
                    try {
                        SupabaseDb.delete(
                            "cart_items",
                            "${SupabaseDb.eq("uid", uid)}&${SupabaseDb.eq("product_id", itemId)}"
                        )
                        ui {
                            allData.remove(itemId)
                            notifyDataSetChanged()
                        }
                    } catch (_: Exception) {
                        appContext()?.let { LocalCartStore.remove(it, itemId) }
                        ui {
                            allData.remove(itemId)
                            notifyDataSetChanged()
                        }
                    }
                }
            }
        }

        holder.increaseQtyBtn.setOnClickListener {
            val current = holder.quantityText.text.toString().toIntOrNull() ?: return@setOnClickListener
            val updatedQty = current + 1
            holder.quantityText.text = updatedQty.toString()
            updateQuantity(itemId, uid, updatedQty)
        }

        holder.decreaseQtyBtn.setOnClickListener {
            val current = holder.quantityText.text.toString().toIntOrNull() ?: return@setOnClickListener
            if (current > 1) {
                val updatedQty = current - 1
                holder.quantityText.text = updatedQty.toString()
                updateQuantity(itemId, uid, updatedQty)
            }
        }

        @Suppress("UNCHECKED_CAST")
        val curr = currentData.value as Map<String, Any>

        val offlineItem = if (itemId.startsWith("offline_")) {
            appContext()?.let { OfflineEcommerceData.getItems(it).firstOrNull { item -> item.id == itemId } }
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

        executor.execute {
            try {
                val product = SupabaseDb.selectOne("products", SupabaseDb.eq("id", itemId))
                ui {
                    if (product == null || !product.exists()) {
                        bindUnavailable(holder, curr)
                        return@ui
                    }
                    itemCost = product.get("price")?.toString()?.toIntOrNull() ?: 0
                    deliveryCharge = product.get("delCharge")?.toString()?.toIntOrNull()
                        ?: product.get("del_charge")?.toString()?.toIntOrNull()
                        ?: 0
                    quantity = curr["quantity"].toString().toIntOrNull() ?: 1
                    holder.itemName.text = product.getString("title").orEmpty().ifBlank { "Product" }
                    holder.itemPrice.text = formatLkr(itemCost)
                    holder.quantityText.text = quantity.toString()
                    holder.deliveryCharge.text = deliveryCharge.toString()
                    holder.firm.text = product.get("retailer").toString()
                    holder.availability.text = product.get("availability").toString()

                    @Suppress("UNCHECKED_CAST")
                    val allImages = product.get("imageUrl") as? List<*> ?: emptyList<Any>()
                    if (allImages.isNotEmpty()) {
                        Glide.with(context).load(allImages[0].toString()).into(holder.image)
                    } else {
                        holder.image.setImageResource(R.drawable.ic_launcher_foreground)
                    }
                    holder.buyBtn.text = "Buy Now: " + formatLkr(itemCost * quantity + deliveryCharge)
                }
            } catch (_: Exception) {
                ui { bindUnavailable(holder, curr) }
            }
        }
    }

    private fun bindUnavailable(holder: CartItemsViewHolder, curr: Map<String, Any>) {
        holder.itemName.text = "Unavailable Item"
        holder.itemPrice.text = formatLkr(0)
        holder.quantityText.text = (curr["quantity"].toString().toIntOrNull() ?: 1).toString()
        holder.deliveryCharge.text = "0"
        holder.firm.text = "Unknown"
        holder.availability.text = "Unavailable"
        holder.buyBtn.text = "Buy Now: " + formatLkr(0)
        holder.image.setImageResource(R.drawable.ic_launcher_foreground)
    }

    private fun updateQuantity(itemId: String, uid: String, updatedQty: Int) {
        if (useLocalStore) {
            appContext()?.let { LocalCartStore.updateQuantity(it, itemId, updatedQty) }
        } else {
            executor.execute {
                try {
                    SupabaseDb.update(
                        "cart_items",
                        "${SupabaseDb.eq("uid", uid)}&${SupabaseDb.eq("product_id", itemId)}",
                        JSONObject().put("quantity", updatedQty)
                    )
                } catch (_: Exception) {
                    appContext()?.let { LocalCartStore.updateQuantity(it, itemId, updatedQty) }
                }
            }
        }
    }
}
