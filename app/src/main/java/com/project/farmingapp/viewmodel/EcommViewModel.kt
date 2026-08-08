package com.project.farmingapp.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.project.farmingapp.supabase.RemoteDocument
import com.project.farmingapp.supabase.SupabaseDb
import java.util.concurrent.Executors

class EcommViewModel : ViewModel() {

    private val executor = Executors.newSingleThreadExecutor()

    var ecommLiveData = MutableLiveData<List<RemoteDocument>>()
    var specificCategoryItems = MutableLiveData<List<RemoteDocument>>()
    var specificItem = MutableLiveData<RemoteDocument>()

    fun loadAllEcommItems(): MutableLiveData<List<RemoteDocument>> {
        executor.execute {
            try {
                val docs = SupabaseDb.select("products")
                ecommLiveData.postValue(docs)
            } catch (e: Exception) {
                Log.d("ecommviewmodel", e.message ?: "Unknown error")
                ecommLiveData.postValue(emptyList())
            }
        }
        return ecommLiveData
    }

    fun loadSpecificTypeEcomItem(itemType: String) {
        executor.execute {
            try {
                val docs = SupabaseDb.select("products", SupabaseDb.eq("type", itemType))
                ecommLiveData.postValue(docs)
            } catch (e: Exception) {
                Log.d("ecommviewmodel", e.message ?: "Unknown error")
                ecommLiveData.postValue(emptyList())
            }
        }
    }

    fun getSpecificCategoryItems(itemType: String): MutableLiveData<List<RemoteDocument>> {
        executor.execute {
            try {
                val docs = SupabaseDb.select("products", SupabaseDb.eq("type", itemType))
                specificCategoryItems.postValue(docs)
            } catch (e: Exception) {
                Log.e("EcommViewModel", "Error Loading Specific Category Items", e)
                specificCategoryItems.postValue(emptyList())
            }
        }
        return specificCategoryItems
    }

    fun getSpecificItem(itemID: String): MutableLiveData<RemoteDocument> {
        executor.execute {
            try {
                val doc = SupabaseDb.selectOne("products", SupabaseDb.eq("id", itemID))
                if (doc != null) {
                    specificItem.postValue(doc)
                } else {
                    Log.e("EcommViewModel", "Failed Getting Data")
                }
            } catch (e: Exception) {
                Log.e("EcommViewModel", "Failed Getting Data", e)
            }
        }
        return specificItem
    }
}
