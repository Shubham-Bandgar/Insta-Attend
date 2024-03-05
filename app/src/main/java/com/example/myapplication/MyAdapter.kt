package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MyAdapter(private val userList: ArrayList<User>): RecyclerView.Adapter<MyAdapter.MyViewHolder>(){
    class MyViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val date : TextView = itemView.findViewById(R.id.tvDate)
        val status: TextView = itemView.findViewById(R.id.tvStatus)
        val duration: TextView = itemView.findViewById(R.id.tvDuration)
        val checkInTime: TextView = itemView.findViewById(R.id.tvCheckIn)
        val checkOutTime: TextView = itemView.findViewById(R.id.tvCheckOut)

    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyAdapter.MyViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false)
        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.date.text = userList[position].Date
        holder.status.text = userList[position].Status
        holder.duration.text = userList[position].Duration
        holder.checkInTime.text = userList[position].CheckIn_Time
        holder.checkOutTime.text = userList[position].CheckOut_Time
    }

    override fun getItemCount(): Int {
       return userList.size
    }
}