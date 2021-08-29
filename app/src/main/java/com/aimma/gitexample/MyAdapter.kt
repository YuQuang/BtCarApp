package com.aimma.gitexample

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aimma.gitexample.databinding.ListItemBinding
import java.util.ArrayList


class MyAdapter: RecyclerView.Adapter<MyAdapter.ViewHolder>() {
    private lateinit var datalist: ArrayList<BtInfo>
    private lateinit var cellClickListener: CellClickListener


    interface CellClickListener {
        fun onCellClickListener(data: BtInfo)
    }

    inner class ViewHolder(listItemBinding: ListItemBinding): RecyclerView.ViewHolder(listItemBinding.root){
        private val name = listItemBinding.btName
        private val mac = listItemBinding.btMac
        private val linear = listItemBinding.linear

        fun bind(data: BtInfo) {
            name.text = data.getBtDeviceName()
            mac.text = data.getBtDeviceAddress()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val listItemBinding = ListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(listItemBinding)
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(datalist[position])
        holder.itemView.setOnClickListener {
            cellClickListener.onCellClickListener(datalist[position])
        }
    }
    override fun getItemCount(): Int {
        return datalist.size
    }

    fun updateList(datalist: ArrayList<BtInfo>){
        this.datalist = datalist
    }
    fun addData(data: BtInfo){
        this.datalist.add(data)
    }
    fun setCellClickListener(cellClickListener: CellClickListener){
        this.cellClickListener = cellClickListener
    }
}