package com.example.foodOrderingSystem.firestore

import android.content.ContentValues
import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.example.foodOrderingSystem.R
import com.example.foodOrderingSystem.ui.menus.MenuItemDetailFragment
import com.example.foodOrderingSystem.adapters.MenuItemListAdapter
import com.example.foodOrderingSystem.adapters.MenuTypeListAdapter
import com.example.foodOrderingSystem.adapters.OrderItemListAdapter
import com.example.foodOrderingSystem.adapters.ReportDetailItemListAdapter
import com.example.foodOrderingSystem.adapters.ReportListAdapter
import com.example.foodOrderingSystem.adapters.TableListAdapter
import com.example.foodOrderingSystem.ui.menus.MenuItemListFragment
import com.example.foodOrderingSystem.ui.menus.MenuTypeFragment
import com.example.foodOrderingSystem.models.MenuItem
import com.example.foodOrderingSystem.models.MenuItemViewModel
import com.example.foodOrderingSystem.models.MenuType
import com.example.foodOrderingSystem.models.MenuTypeViewModel
import com.example.foodOrderingSystem.models.OrderItem
import com.example.foodOrderingSystem.models.OrderItemViewModel
import com.example.foodOrderingSystem.models.PrintOrderItem
import com.example.foodOrderingSystem.models.Report
import com.example.foodOrderingSystem.models.ReportItemViewModel
import com.example.foodOrderingSystem.models.TableOrder
import com.example.foodOrderingSystem.models.TableViewModel
import com.example.foodOrderingSystem.models.Tables
import com.example.foodOrderingSystem.ui.reports.ReportFragment
import com.example.foodOrderingSystem.ui.reports.ReportItemFragment
import com.example.foodOrderingSystem.ui.table.TableCustomerOrderFragment
import com.example.foodOrderingSystem.ui.table.TableFragment
import com.example.foodOrderingSystem.utils.Constants
import com.example.foodOrderingSystem.utils.Utils
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

    fun addTableOrder(
        activity: Fragment,
        context: Context,
        tableOrder: TableOrder,
        recyclerView: RecyclerView,
        tableList: LiveData<MutableList<Tables>>
    ) {
        when(activity) {
            is TableFragment -> {
                mFirestore.collection(Constants.TABLEORDERS)
                    .document(tableOrder.id!!)
                    .set(tableOrder, SetOptions.merge())
                    .addOnSuccessListener { documentReference ->
                        Log.d(ContentValues.TAG, "DocumentSnapshot added with ID: $documentReference")
                        recyclerView.adapter = TableListAdapter(activity, context, tableList)
                    }
                    .addOnFailureListener { e ->
                        Log.w(ContentValues.TAG, "Error adding document", e)
                    }
            }
        }
    }

    fun updateTableOrder(activity: Fragment, id: String, startTime: String, token: String) {
        when(activity) {
            is TableCustomerOrderFragment -> {
                val documentRef = mFirestore.collection(Constants.TABLEORDERS).document(id)
                Log.d("table id", id)

                // Check if the start time exists in the document
                documentRef.get()
                    .addOnSuccessListener { documentSnapshot ->
                        if (documentSnapshot.exists()) {
                            // Document exists, now check if the start time exists
                            val existingStartTime = documentSnapshot.getString("startTime")

                            if (existingStartTime == "") {
                                // Start time doesn't exist, update the document
                                documentRef.update(mapOf(
                                    "startTime" to startTime,
                                    "token" to token
                                ))
                            } else {
                                documentRef.update(mapOf(
                                    "token" to token
                                ))
                            }
                        }
                    }
            }
        }
    }

    fun updateTableCancelReason(activity: Fragment, id: String, cancelReason: String) {
        when(activity) {
            is TableCustomerOrderFragment -> {
                mFirestore.collection(Constants.TABLEORDERS)
                    .document(id)
                    .update(mapOf(
                        "status" to "CANCELED",
                        "reason" to cancelReason
                    )).addOnSuccessListener {
                        Utils().backToPrevious(activity, R.id.navigation_table)
                    }
            }
        }
    }

    fun getCustomerOrdering(
        activity: Fragment,
        callback: (List<PrintOrderItem>?) -> Unit
    ) {
        when (activity) {
            is TableFragment -> {
                mFirestore.collection(Constants.TABLEORDERS)
                    .whereEqualTo("status", "PROCESS")
                    .get()
                    .addOnSuccessListener {result ->

                        val printOrderItems = result.toObjects(PrintOrderItem::class.java)

                        Log.d("get result", printOrderItems.toString())

                        // Now you can use printOrderItems to generate your print order
                        callback(printOrderItems)

                    }
                    .addOnFailureListener { e ->
                        Log.w(ContentValues.TAG, "Error getting documents", e)
                        callback(null)
                    }
            }
        }
    }

    fun getCustomerOrder(
        activity: Fragment,
        recyclerView: RecyclerView,
        orderItemViewModel: OrderItemViewModel,
        id: String
    ) {
        when (activity) {
            is TableCustomerOrderFragment -> {
                mFirestore.collection(Constants.TABLEORDERS)
                    .document(id)
                    .get()
                    .addOnSuccessListener { result ->

                        Log.d("id", id)
                        if (result.exists()) {
                            val customerOrderItem = result.toObject(PrintOrderItem::class.java)?.customerOrder
                            if (customerOrderItem != null) {
                                orderItemViewModel.setOrderItem(customerOrderItem)

                                // Create LiveData to observe changes
                                val orderItemLiveData = MutableLiveData<MutableList<OrderItem>>()
                                orderItemLiveData.value = (customerOrderItem)

                                Log.d("load order item", customerOrderItem.toString())

                                recyclerView.adapter = OrderItemListAdapter(activity, activity.requireContext(), orderItemLiveData)
                            } else {
                                Log.e("Firestore", "Document $id does not exist")
                            }
                        } else {
                            Log.e("Firestore", "Document $id does not exist")
                        }
                    }
            }
        }
    }

    fun getCustomerOrderForReceipt(
        activity: Fragment,
        tableId: String,
        callback: (List<PrintOrderItem>?) -> Unit
    ) {
        when (activity) {
            is TableCustomerOrderFragment -> {
                mFirestore.collection(Constants.TABLEORDERS)
                    .document(tableId)
                    .get()
                    .addOnSuccessListener {result ->

                        val printReceiptItem = result.toObject(PrintOrderItem::class.java)

                        Log.d("get result", printReceiptItem.toString())

                        // Convert the single item to a list
                        val printReceiptItems: List<PrintOrderItem>? = printReceiptItem?.let {
                            listOf(it)
                        }

                        // Now you can use printOrderItems to generate your print order
                        callback(printReceiptItems)


                    }
                    .addOnFailureListener { e ->
                        Log.w(ContentValues.TAG, "Error getting documents", e)
                        callback(null)
                    }
            }
        }
    }

    fun deleteCustomerOrderItem(id: String?, tableId: String) {
        val orderRef = mFirestore.collection(Constants.TABLEORDERS).document(tableId)

        // Get the current order document
        orderRef.get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                // Get the current customerOrder array
                val customerOrder = documentSnapshot["customerOrder"] as? MutableList<Map<String, Any>> ?: mutableListOf()

                // Find the index of the item to be deleted
                val indexToRemove = customerOrder.indexOfFirst { it["id"] == id }

                if (indexToRemove != -1) {
                    // Remove the item from the customerOrder array
                    customerOrder.removeAt(indexToRemove)

                    // Update the document with the modified customerOrder array
                    orderRef.update("customerOrder", customerOrder)
                        .addOnSuccessListener {
                            // Successfully updated the document
                            // You may want to handle this case appropriately
                            println("Item deleted successfully")
                        }
                        .addOnFailureListener { e ->
                            // Handle failure
                            println("Error deleting item: $e")
                        }
                } else {
                    // Item not found in the array
                    println("Item not found in the array")
                }
            } else {
                // Document not found
                println("Document not found")
            }
        }
            .addOnFailureListener { e ->
                // Handle failure
                println("Error getting document: $e")
            }
    }

    fun getTableOrder(
        activity: Fragment,
        recyclerView: RecyclerView,
        tableViewModel: TableViewModel
    ) {
        mFirestore.collection(Constants.TABLEORDERS)
            .whereEqualTo("status", "PROCESS")
            .get()
            .addOnSuccessListener { result ->

                val tableOrder = result.toObjects(Tables::class.java)
                tableViewModel.setTable(tableOrder)

                // Create LiveData to observe changes
                val tableOrderLivaData = MutableLiveData<MutableList<Tables>>()
                tableOrderLivaData.value = tableOrder

                Log.d("load table", tableOrder.toString())

                recyclerView.adapter = TableListAdapter(activity, activity.requireContext(), tableOrderLivaData)
            }
    }

    fun updateCustomerOrderingToCustomerOrder(
        activity: Fragment,
        orderItem: List<OrderItem>?,
        id: String?
    ) {
        when (activity) {
            is TableFragment -> {
                if (id != null) {
                    mFirestore.collection(Constants.TABLEORDERS)
                        .document(id)
                        .update(mapOf(
                            "customerOrder" to orderItem,
                            "customerOrdering" to null
                        ))
                        .addOnFailureListener { e ->
                            Log.w(ContentValues.TAG, "Error getting documents", e)
                        }
                } else {
                    Log.d("updateCustomerOrdering", "Document ID is null")
                }
            }
        }
    }

    fun updateCustomerPaymentMethod(activity: Fragment, tableId: String, paymentType: String) {
        when (activity) {
            is TableCustomerOrderFragment -> {
                mFirestore.collection(Constants.TABLEORDERS)
                    .document(tableId)
                    .update(mapOf(
                        "status" to "COMPLETED",
                        "paymentType" to paymentType
                    ))
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

    fun addReport(activity: Fragment, report: Report) {
        when (activity) {
            is TableCustomerOrderFragment -> {
                mFirestore.collection(Constants.REPORTS)
                    .document(report.id!!)
                    .set(report, SetOptions.merge())
                    .addOnSuccessListener { documentReference ->
                        Log.d(ContentValues.TAG, "DocumentSnapshot added with ID: $documentReference")
                    }
            }
        }
    }

    fun getReport(
        activity: Fragment,
        context: Context,
        recyclerView: RecyclerView,
        reportItemViewModel: ReportItemViewModel, ) {
        when (activity) {
            is ReportFragment -> {
                mFirestore.collection(Constants.REPORTS)
                    .orderBy("dateTime", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener { result ->

                        /**
                        Custom object to return the data field of the document in QuerySnapShot
                        Adapter's MutableList can works in MutableList and ArrayList
                        But if Adapter is ArrayList, it doesn't works MutableList
                         */
                        val reportItemList = result.toObjects(Report::class.java)
                        reportItemViewModel.setReportItem(reportItemList)

                        // Create LiveData to observe changes
                        val reportItemLiveData = MutableLiveData<MutableList<Report>>()
                        reportItemLiveData.value = reportItemList

                        Log.d("load report", reportItemList.toString())

                        recyclerView.adapter = ReportListAdapter(activity, context, reportItemLiveData)
                    }
                    .addOnFailureListener { exception ->
                        Log.d(ContentValues.TAG, "Error getting documents: ", exception)
                    }
            }
        }
    }

    fun getCustomerReportItemDetail(
        activity: Fragment,
        recyclerView: RecyclerView,
        reportId: String
    ) {
        mFirestore.collection(Constants.REPORTS)
            .document(reportId)
            .get()
            .addOnSuccessListener { result ->

                Log.d("id", reportId)
                if (result.exists()) {
                    val customerOrderItem = result.toObject(Report::class.java)?.orderItem
                    if (customerOrderItem != null) {

                        // Create LiveData to observe changes
                        val reportItemLiveData = MutableLiveData<MutableList<OrderItem>>()
                        reportItemLiveData.value = (customerOrderItem)

                        Log.d("load order item", customerOrderItem.toString())

                        recyclerView.adapter = ReportDetailItemListAdapter(activity, activity.requireContext(), reportItemLiveData)
                    } else {
                        Log.e("Firestore", "Document $reportId does not exist")
                    }
                } else {
                    Log.e("Firestore", "Document $reportId does not exist")
                }
            }
    }

}