package com.example.foodOrderingSystem.adapters

import android.app.Dialog
import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.foodOrderingSystem.R
import com.example.foodOrderingSystem.databinding.ListMenuItemBinding
import com.example.foodOrderingSystem.models.MenuItem
import com.example.foodOrderingSystem.models.MenuItemViewModel
import com.example.foodOrderingSystem.utils.Utils
import java.text.DecimalFormat

class MenuItemListAdapter (
    private val activity: Fragment,
    private val context: Context,
    private val menuItemList: LiveData<MutableList<MenuItem>>
): RecyclerView.Adapter<MenuItemListAdapter.ItemViewHolder>() {

    private val menuItemViewModel: MenuItemViewModel by activity.activityViewModels()
    private var mImageUri: Uri? = null
    private lateinit var dialog: Dialog



    inner class ItemViewHolder(private val view: ListMenuItemBinding) :
        RecyclerView.ViewHolder(view.root) {
        var image: ImageView = view.menuImage
        var itemName: TextView = view.menuItemTextview
        var itemType: TextView = view.menuType
        var itemPrice: TextView = view.priceMenuItem

        private var cardContainer: ConstraintLayout = view.menuItemContainer

        init {
            cardContainer.setOnClickListener { openMenuItemDetail() }
        }

        private fun openMenuItemDetail() {
            val getPosition = menuItemList.value!![adapterPosition]
            Log.d("get position: ", getPosition.toString())

            menuItemViewModel.setId(getPosition.id!!)
            menuItemViewModel.setImage(getPosition.image.toUri())
            menuItemViewModel.setMenuItemName(getPosition.itemName)
            menuItemViewModel.setType(getPosition.type)
            menuItemViewModel.setIngredient(getPosition.ingredient)
            menuItemViewModel.setPrice(getPosition.price)
            menuItemViewModel.setMenuAvailable(getPosition.available!!)

            Utils().goToNextNavigate(activity, R.id.action_menuItemListFragment_to_menuItemDetailFragment)
        }

    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MenuItemListAdapter.ItemViewHolder {
        val itemView: ListMenuItemBinding =
            ListMenuItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return ItemViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MenuItemListAdapter.ItemViewHolder, position: Int) {
        val item = menuItemList.value!![position]
        // Convert the image string to URI
        val imageUri = item.image
        Log.d("get image uri", imageUri)

        Glide.with(context)
            .load(item.image)
            .centerCrop()
            .into(holder.image)

        holder.itemName.text = item.itemName
        holder.itemType.text = item.type
        val priceFormat = DecimalFormat("RM0.00")
        val formattedPrice = priceFormat.format(item.price.toDouble())
        holder.itemPrice.text = formattedPrice


    }

    override fun getItemCount(): Int {
        return menuItemList.value!!.size;
    }
}