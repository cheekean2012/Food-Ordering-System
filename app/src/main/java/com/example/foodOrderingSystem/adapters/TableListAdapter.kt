package com.example.foodOrderingSystem.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.RecyclerView
import com.example.foodOrderingSystem.R
import com.example.foodOrderingSystem.databinding.ListTableItemBinding
import com.example.foodOrderingSystem.models.TableViewModel
import com.example.foodOrderingSystem.models.Tables
import com.example.foodOrderingSystem.utils.Utils
import com.google.android.material.card.MaterialCardView

class TableListAdapter (
    private val activity: Fragment,
    private val context: Context,
    private val tableList: LiveData<MutableList<Tables>>
): RecyclerView.Adapter<TableListAdapter.ItemViewHolder>() {

    private val tableViewModel: TableViewModel by activity.activityViewModels()

    inner class ItemViewHolder(private val view: ListTableItemBinding): RecyclerView.ViewHolder(view.root) {
        val tableNumber: TextView = view.tableNumber
        private var card:MaterialCardView = view.cardContainer

        init {
            card.setOnClickListener { goToTableCustomerOrder()   }
        }

        private fun goToTableCustomerOrder() {
            val getPosition = tableList.value!![adapterPosition]
            Log.d("get position: ", getPosition.toString())
            tableViewModel.setTableId(getPosition.id!!)
            tableViewModel.setTableNumber(getPosition.tableNumber!!)

            Utils().goToNextNavigate(activity,R.id.action_navigation_table_to_navigation_table_customer_order)
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TableListAdapter.ItemViewHolder {
        val itemView: ListTableItemBinding = ListTableItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return ItemViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: TableListAdapter.ItemViewHolder, position: Int) {
        val item = tableList.value!![position]
        holder.tableNumber.text = "Table: " + item.tableNumber.toString()
    }

    override fun getItemCount(): Int {
        return tableList.value!!.size;
    }


}