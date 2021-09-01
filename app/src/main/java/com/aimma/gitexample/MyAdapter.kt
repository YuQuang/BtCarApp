package com.aimma.gitexample

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aimma.gitexample.databinding.ListItemBinding
import java.util.ArrayList

class MyAdapter: RecyclerView.Adapter<MyAdapter.ViewHolder>() {
    private lateinit var dataList: ArrayList<BtInfo>
    private lateinit var cellClickListener: CellClickListener


    interface CellClickListener {
        fun onCellClickListener(data: BtInfo, position: Int)
    }

    inner class ViewHolder(listItemBinding: ListItemBinding): RecyclerView.ViewHolder(listItemBinding.root){
        private val name = listItemBinding.btName
        private val mac = listItemBinding.btMac
        private val rrsi = listItemBinding.btRRSI

        fun bind(data: BtInfo) {
            name.text = data.getBtDeviceName()
            mac.text = data.getBtDeviceAddress()
            rrsi.text = data.getBtDeviceRRSI().toString()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val listItemBinding = ListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(listItemBinding)
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataList[position])
        holder.itemView.setOnClickListener {
            cellClickListener.onCellClickListener(dataList[position], position)
        }
    }
    override fun getItemCount(): Int {
        return dataList.size
    }

    fun updateList(dataList: ArrayList<BtInfo>){
        this.dataList = dataList
    }
    fun addData(data: BtInfo){
        this.dataList.add(data)
    }
    fun setCellClickListener(cellClickListener: CellClickListener){
        this.cellClickListener = cellClickListener
    }
}