package com.example.foodOrderingSystem.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.RecyclerView
import com.example.foodOrderingSystem.R
import com.example.foodOrderingSystem.databinding.ListMenuTypeBinding
import com.example.foodOrderingSystem.firestore.Firestore
import com.example.foodOrderingSystem.models.MenuType
import com.example.foodOrderingSystem.models.MenuTypeViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MenuTypeListAdapter (
    private val activity: Fragment,
    private val context: Context,
    private val menuTypeList: LiveData<MutableList<MenuType>>
): RecyclerView.Adapter<MenuTypeListAdapter.ItemViewHolder>() {

    private val menuTypeViewModel: MenuTypeViewModel by activity.activityViewModels()

    inner class ItemViewHolder(private val view: ListMenuTypeBinding) :
        RecyclerView.ViewHolder(view.root) {
        var menuTypeTextView: TextView = view.menuTypeTextview
        private var editMenuType: ImageView = view.editMenuType
        private var deleteMenuType: ImageView = view.deleteMenuType

        init {
            editMenuType.setOnClickListener { openEditDialog() }
            deleteMenuType.setOnClickListener{ deleteMenuType() }
        }

        private fun openEditDialog() {
            val getPosition = menuTypeList.value!![adapterPosition]
            Log.d("get position: ", getPosition.toString())

            val inflater = LayoutInflater.from(context)
                .inflate(R.layout.dialog_menu_type, null)

            val menuTypeEditText: EditText = inflater.findViewById(R.id.new_menu_type_editText)

            MaterialAlertDialogBuilder(context)
                .setView(inflater)
                .setNegativeButton(activity.resources.getString(R.string.cancel)) { dialog, which ->
                    // Respond to negative button press
                    dialog.dismiss()
                }
                .setPositiveButton(activity.resources.getString(R.string.ok)) { dialog, which ->
                    val inputText = menuTypeEditText.text.toString().uppercase() // Get the user input from the EditText
                    val updatedItem = getPosition.copy(menuType = inputText)

                    if (inputText.isNotEmpty()) {
                        menuTypeViewModel.updateMenuType(updatedItem)

                        // Specify the item when data has been changed and display
                        notifyItemChanged(adapterPosition)
                        Firestore().updateMenuType(activity, context, updatedItem)

                        dialog.dismiss()
                    } else {
                        // Handle case where user didn't enter any text
                        Toast.makeText(context, "Please enter the menu type", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
                .show()
        }

        private fun deleteMenuType() {
            val getPosition = menuTypeList.value!![adapterPosition]
            Log.d("get position: ", getPosition.toString())

            MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(R.string.delete_menu_type))
                .setMessage(context.getString(R.string.confirm_delete_menu_type))
                .setNegativeButton(activity.resources.getString(R.string.cancel)) { dialog, _ ->
                    // Respond to negative button press
                    dialog.dismiss()
                }
                .setPositiveButton(activity.resources.getString(R.string.ok)) { _, _ ->
                    // Delete data from menu view model
                    menuTypeViewModel.deleteMenuType(getPosition)

                    // Notify the adapter that an item has been removed
                    notifyItemRemoved(adapterPosition)
                    Firestore().deleteMenuType(getPosition.id)
                }
                .show()
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MenuTypeListAdapter.ItemViewHolder {
        val itemView: ListMenuTypeBinding =
            ListMenuTypeBinding.inflate(LayoutInflater.from(context), parent, false)
        return ItemViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MenuTypeListAdapter.ItemViewHolder, position: Int) {
        val item = menuTypeList.value!![position]
        holder.menuTypeTextView.text = item.menuType
    }

    override fun getItemCount(): Int {
        return menuTypeList.value!!.size;
    }
}