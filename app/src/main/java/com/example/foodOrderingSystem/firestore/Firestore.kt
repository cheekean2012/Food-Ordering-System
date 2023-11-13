package com.example.foodOrderingSystem.firestore

import android.content.ContentValues
import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.example.foodOrderingSystem.MenuItemDetailFragment
import com.example.foodOrderingSystem.adapters.MenuItemListAdapter
import com.example.foodOrderingSystem.adapters.MenuTypeListAdapter
import com.example.foodOrderingSystem.menu.MenuItemListFragment
import com.example.foodOrderingSystem.menu.MenuTypeFragment
import com.example.foodOrderingSystem.models.MenuItem
import com.example.foodOrderingSystem.models.MenuItemViewModel
import com.example.foodOrderingSystem.models.MenuType
import com.example.foodOrderingSystem.models.MenuTypeViewModel
import com.example.foodOrderingSystem.utils.Constants
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.storageMetadata

class Firestore {

    private val mFirestore = FirebaseFirestore.getInstance()

    fun uploadMenuType(
        activity: Fragment,
        context: Context,
        menuType: MenuType,
        recyclerView: RecyclerView,
        menuTypeList: MutableLiveData<MutableList<MenuType>>
    ) {

        // Generate a document id
        val document = mFirestore.collection(Constants.MENUTYPES).document()

        when (activity) {
            is MenuTypeFragment -> {
                mFirestore.collection(Constants.MENUTYPES)
                    .document(menuType.id!!)
                    .set(menuType, SetOptions.merge())
                    .addOnSuccessListener { documentReference ->
                        Log.d(ContentValues.TAG, "DocumentSnapshot added with ID: $documentReference")
                        recyclerView.adapter = MenuTypeListAdapter(activity, context, menuTypeList)
                        // Call function from fragment for transfer toast message and forward to login screen
//                activity.userSignUpSuccess()
                    }
                    .addOnFailureListener { e ->
                        Log.w(ContentValues.TAG, "Error adding document", e)
                    }
            }
        }
    }

    fun getMenuType(
        activity: Fragment,
        recyclerView: RecyclerView,
        menuTypeViewModel: MenuTypeViewModel
    ) {

        mFirestore.collection(Constants.MENUTYPES)

            .get()
            .addOnSuccessListener { result ->

                /**
                Custom object to return the data field of the document in QuerySnapShot
                Adapter's MutableList can works in MutableList and ArrayList
                But if Adapter is ArrayList, it doesn't works MutableList
                 */
                val menuTypeList = result.toObjects(MenuType::class.java)
                menuTypeViewModel.setMenuTypes(menuTypeList)

                // Create LiveData to observe changes
                val menuTypeLiveData = MutableLiveData<MutableList<MenuType>>()
                menuTypeLiveData.value = menuTypeList

                Log.d("load menu type", menuTypeList.toString())

                recyclerView.adapter = MenuTypeListAdapter(activity, activity.requireContext(), menuTypeLiveData)
            }
            .addOnFailureListener { exception ->
                Log.d(ContentValues.TAG, "Error getting documents: ", exception)
            }
    }

    fun updateMenuType(activity: Fragment, context: Context, menuType: MenuType) {

        when (activity) {
            is MenuTypeFragment -> {
                mFirestore.collection(Constants.MENUTYPES)
                    .document(menuType.id!!)
                    .update(mapOf(
                        "menuType" to menuType.menuType,
                    ))
            }
        }
    }

    fun deleteMenuType(id: String?) {
        mFirestore.collection(Constants.MENUTYPES)
            .document(id!!)
            .delete()
    }

    fun loadSpinnerMenuType(menuTypeViewModel: MenuTypeViewModel) {
        mFirestore.collection(Constants.MENUTYPES)

            .get()
            .addOnSuccessListener { result ->

                /**
                Custom object to return the data field of the document in QuerySnapShot
                Adapter's MutableList can works in MutableList and ArrayList
                But if Adapter is ArrayList, it doesn't works MutableList
                 */
                val menuTypeList = result.toObjects(MenuType::class.java)
                menuTypeViewModel.setMenuTypes(menuTypeList)

                // Create LiveData to observe changes
                val menuTypeLiveData = MutableLiveData<MutableList<MenuType>>()
                menuTypeLiveData.value = menuTypeList

            }
            .addOnFailureListener { exception ->
                Log.d(ContentValues.TAG, "Error getting documents: ", exception)
            }
    }

    fun uploadMenuItem(
        activity: Fragment,
        context: Context,
        menuItem: MenuItem,
        recyclerView: RecyclerView,
        menuItemList: LiveData<MutableList<MenuItem>>
    ) {

        // Set image path file
        val storageReference = FirebaseStorage.getInstance().reference.child(
            "menu_picture/" + menuItem.id  + "menu.jpg"
        )

        // Set image mime type
        val metadata = storageMetadata {
            contentType = "image/jpeg"
        }

        when (activity) {
            is MenuItemListFragment -> {
//                activity.showProgress()
                storageReference.putFile(menuItem.image.toUri()!!, metadata).addOnSuccessListener { result ->
                    Log.d("Image URL: ", result.metadata!!.reference!!.downloadUrl.toString() )

                    result.metadata!!.reference!!.downloadUrl.addOnSuccessListener { uri ->
                        Log.d("Downloadable image URL ", uri.toString())
                        menuItem.image = uri.toString()

                        mFirestore.collection(Constants.MENUITEMS).document(menuItem.id!!)
                            .set(menuItem, SetOptions.merge())
                            .addOnSuccessListener {
//                                activity.closeProgress()
                                recyclerView.adapter = MenuItemListAdapter(activity, context, menuItemList)
                            }.addOnFailureListener {
//                                activity.closeProgress()
                            }
                    }
                }
            }
        }
    }

    fun getMenuItem(
        activity: Fragment,
        recyclerView: RecyclerView,
        menuItemViewModel: MenuItemViewModel
    ) {

        mFirestore.collection(Constants.MENUITEMS)
            .orderBy("type", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { result ->

                /**
                Custom object to return the data field of the document in QuerySnapShot
                Adapter's MutableList can works in MutableList and ArrayList
                But if Adapter is ArrayList, it doesn't works MutableList
                 */
                val menuItemList = result.toObjects(MenuItem::class.java)

                menuItemViewModel.setMenuItems(menuItemList)

                // Create LiveData to observe changes
                val menuItemLiveData = MutableLiveData<MutableList<MenuItem>>()
                menuItemLiveData.value = menuItemList

                recyclerView.adapter = MenuItemListAdapter(activity, activity.requireContext(), menuItemLiveData)
            }
            .addOnFailureListener { exception ->
                Log.d(ContentValues.TAG, "Error getting documents: ", exception)
            }
    }

    fun updateMenuItem(activity: Fragment, menuItem: MenuItem) {

        val image = menuItem.image.toUri()

        when (activity) {
            is MenuItemDetailFragment -> {
                if (image != null) {
                    mFirestore.collection(Constants.MENUITEMS)
                        .document(menuItem.id!!)
                        .update(mapOf(
                            "image" to menuItem.image,
                            "itemName" to menuItem.itemName,
                            "type" to menuItem.type,
                            "price" to menuItem.price,
                            "ingredient" to menuItem.ingredient,
                            "available" to menuItem.available
                        ))
                } else {
                    // Set image path file
                    val storageReference = FirebaseStorage.getInstance().reference.child(
                        "menu_picture/" + menuItem.id  + "menu.jpg"
                    )

                    // Set image mime type
                    val metadata = storageMetadata {
                        contentType = "image/jpeg"
                    }

                    // Upload picture to storage
                    storageReference.putFile(menuItem.image.toUri()!!, metadata).addOnSuccessListener { result ->
                        Log.d("Image URL: ", result.metadata!!.reference!!.downloadUrl.toString() )

                        result.metadata!!.reference!!.downloadUrl.addOnSuccessListener { uri ->
                            Log.d("Downloadable image URL ", uri.toString())
                            menuItem.image = uri.toString()

                            mFirestore.collection(Constants.MENUITEMS).document(menuItem.id!!)
                                .update(mapOf(
                                    "image" to menuItem.image,
                                    "itemName" to menuItem.itemName,
                                    "type" to menuItem.type,
                                    "price" to menuItem.price,
                                    "ingredient" to menuItem.ingredient,
                                    "available" to menuItem.available
                                ))
                        }
                    }
                }
            }
        }
    }

    fun deleteMenuItem(id: String?) {
        mFirestore.collection(Constants.MENUITEMS)
            .document(id!!)
            .delete()
    }

}