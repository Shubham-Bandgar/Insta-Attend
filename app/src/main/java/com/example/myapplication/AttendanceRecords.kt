package com.example.myapplication

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class AttendanceRecords : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var userList: ArrayList<User>
    private var db = Firebase.firestore
    private lateinit var searchView: SearchView // Use androidx.appcompat.widget.SearchView

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendance_records)
        fetchEmployeeDetails()
    }

    @SuppressLint("SetTextI18n")
    private fun fetchEmployeeDetails() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val firestore = FirebaseFirestore.getInstance()
        if (uid != null) {
            firestore.collection("employeeDetails").document(uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val employeeName = document.getString("username")
                        val tvname = findViewById<TextView>(R.id.tvemployeeName)
                        tvname.text = "$employeeName"
                        Toast.makeText(this, employeeName, Toast.LENGTH_LONG).show()
                        recyclerView = findViewById(R.id.recyclerView)
                        searchView = findViewById<SearchView>(R.id.searchView) // Use androidx.appcompat.widget.SearchView
                        searchView.clearFocus()
                        recyclerView.layoutManager = LinearLayoutManager(this)
                        userList = arrayListOf()
                        db = FirebaseFirestore.getInstance()
                        db.collection("attendance").whereEqualTo("Employee Name", employeeName).get()
                            .addOnSuccessListener { result ->
                                if (!result.isEmpty) {
                                    for (data in result.documents) {
                                        val user: User? = data.toObject(User::class.java)
                                        if (user != null) {
                                            userList.add(user)
                                        }
                                    }
                                    recyclerView.adapter = MyAdapter(userList)
                                }
                            }
                            .addOnFailureListener { exception ->
                                Toast.makeText(this, exception.toString(), Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(this, "No entry found for you", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    exception.printStackTrace()
                }
        }
    }

    private fun filterList(query: String?) {
        if (query != null) {
            val filteredList = ArrayList<User>()
            for (user in userList) {
                if (user.Date!!.toLowerCase().contains(query.toLowerCase()) ||
                    user.Duration!!.toLowerCase().contains(query.toLowerCase())||user.CheckIn_Time!!.toLowerCase().contains(query.toLowerCase()) ||
                    user.CheckOut_Time!!.toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(user)
                }
            }
            recyclerView.adapter = MyAdapter(filteredList)
        }
    }
}
