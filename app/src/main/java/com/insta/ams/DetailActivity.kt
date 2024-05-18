package com.insta.ams

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DetailActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)
        val recordsButton = findViewById<Button>(R.id.recordButton)
        recordsButton.setOnClickListener {
            val intent = Intent(this, AttendanceRecords::class.java)
            startActivity(intent)
        }

        //floating button
        val floatingActionButton = findViewById<FloatingActionButton>(R.id.backtohome)

        floatingActionButton.setOnClickListener {
            val intent = Intent(this, homeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }

        //logo image
        val imageView=findViewById<ImageView>(R.id.imageView2)
        imageView.setOnClickListener {
            val intent=Intent(this,preLogActivity::class.java)
            startActivity(intent)
        }

        fetchEmployeeDetails()

        val buttonLogout=findViewById<Button>(R.id.btnLogout)
        buttonLogout.setOnClickListener{
            logoutUser()
        }

        val deleteattendanceRecord=findViewById<TextView>(R.id.textView5)
        deleteattendanceRecord.setOnClickListener {
            showAlertDialogAttendance()
        }

        val deleteuser=findViewById<TextView>(R.id.textView4)
        deleteuser.setOnClickListener {
            showAlertDialog()
        }
    }

    private fun logoutUser() {
        val auth=FirebaseAuth.getInstance()
        auth.signOut()
        val intent = Intent(this, loginActivity::class.java)
        startActivity(intent)
    }

    //fetching employee details
    private fun fetchEmployeeDetails() {

        val firestore=FirebaseFirestore.getInstance()
        val auth=FirebaseAuth.getInstance()

        val employeeNameTextView = findViewById<TextView>(R.id.employeeNameTextView1)
        val employeeEmailTextView = findViewById<TextView>(R.id.employeeEmailTextView)
        val employeePhoneTextView = findViewById<TextView>(R.id.employeePhoneTextView)
        val employeeCircleTextView = findViewById<TextView>(R.id.circleEditTextView)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)



        val uid = auth.currentUser?.uid

        if (uid != null) {
            firestore.collection("employeeDetails").document(uid)
                .get()
                .addOnSuccessListener { document ->
                    progressBar.visibility= View.GONE
                    if (document != null && document.exists()) {
                        val employeeName = document.getString("username")
                        //val employeeEmail = document.getString("email")
                        val employeePhone = document.getString("phoneNumber")
                        val employeeCircle = document.getString("Circle")
                        employeeNameTextView.text = "Employee Name: $employeeName"
                       // employeeEmailTextView.text = "Employee Email: $employeeEmail"
                        employeePhoneTextView.text = "Phone No.: $employeePhone"
                        employeeCircleTextView.text = "Employment: $employeeCircle"


                    } else {
                        employeeNameTextView.text = "Employee Name: Not Found"
                        employeeEmailTextView.text = "Employee Email: Not Found"
                        employeePhoneTextView.text = "Phone No.: Not Found"
                        employeeCircleTextView.text = "Employment: Not Found"
                    }
                }
                .addOnFailureListener { exception ->
                    exception.printStackTrace()
                }
        }
    }

    private fun deleteUser() {
        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()

        fetchDeleteRequest(auth.currentUser?.uid){permission->

            if (permission=="true"){
                fetchEmployeeName(auth.currentUser?.uid) { employeeName ->
                    if (employeeName != null) {
                        auth.currentUser?.let { firestore.collection("employeeDetails").document(it.uid).delete() }
                        firestore.collection("attendance")
                            .whereEqualTo("Employee Name", employeeName)
                            .get()
                            .addOnSuccessListener { querySnapshot ->
                                for (document in querySnapshot.documents) {
                                    document.reference.delete()
                                }

                                // Now, after deleting attendance records, proceed to delete user account
                                auth.currentUser?.delete()?.addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        // User account deletion successful
                                        Toast.makeText(this, "Account Deleted Successfully", Toast.LENGTH_LONG).show()
                                        val intent = Intent(this, loginActivity::class.java)
                                        startActivity(intent)
                                    } else {
                                        // User account deletion failed
                                        Toast.makeText(this, "Failed to delete account", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, it.toString(), Toast.LENGTH_LONG).show()
                            }
                    }
                }

            } else{
                showRequestAlertDialog("You don't have permission to delete your account. You can still send request  to admin")
            }

        }
    }

    private fun fetchEmployeeName(uid: String?, callback: (String?) -> Unit) {
        val db = FirebaseFirestore.getInstance()

        db.collection("employeeDetails")
            .document(uid!!)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val username = documentSnapshot.getString("username")
                    callback.invoke(username)
                } else {

                    callback.invoke(null)
                }
            }
            .addOnFailureListener {
                callback.invoke(null)
            }
    }

    private fun fetchDeleteRequest(uid: String?, callback: (String?) -> Unit) {
        val db = FirebaseFirestore.getInstance()

        db.collection("employeeDetails")
            .document(uid!!)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val permission = documentSnapshot.getString("deletePermission")
                    callback.invoke(permission)
                } else {

                    callback.invoke(null)
                }
            }
            .addOnFailureListener {
                callback.invoke(null)
            }
    }

    private  fun deleteAttendanceRecord(){
        val auth=FirebaseAuth.getInstance()
        val firestore=FirebaseFirestore.getInstance()
        // auth.currentUser?.let { firestore.collection("employeeDetails").document(it.uid).delete() }
        // auth.currentUser?.delete()

        fetchEmployeeName(auth.currentUser?.uid){
                employeeName->
            if (employeeName!=null){
                firestore.collection("attendance")
                    .whereEqualTo("Employee Name", employeeName)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        for (document in querySnapshot.documents) {
                            document.reference.delete()
                        }
                        Toast.makeText(this,"Success",Toast.LENGTH_LONG).show()
                    }.addOnFailureListener{
                        Toast.makeText(this,it.toString(),Toast.LENGTH_LONG).show()
                    }
            }
        }

        auth.currentUser?.let { firestore.collection("attendance") }
    }
    private fun showRequestAlertDialog(message:String){
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Request Delete")
            .setMessage(message)
            .setCancelable(true)
            .setPositiveButton("Send Request") { dialogInterface,it->
                updateRequest()
            }
            .setNegativeButton("Cancel") { dialogInterface,it->
                dialogInterface.cancel()
            }
            .create()
            .show()

    }
    private fun showAlertDialog(){
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Confirmation")
            .setMessage("Are you sure you want to delete")
            .setCancelable(true)
            .setPositiveButton("delete") { dialogInterface,it->
                deleteUser()
            }
            .setNegativeButton("Cancel") { dialogInterface,it->
                dialogInterface.cancel()
            }
            .create()
            .show()

    }

    private fun showAlertDialogAttendance(){
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Confirmation")
            .setMessage("Are you sure you want to delete?")
            .setCancelable(true)
            .setPositiveButton("delete") { dialogInterface,it->
                deleteAttendanceRecord()
            }
            .setNegativeButton("Cancel") { dialogInterface,it->
                dialogInterface.cancel()
            }
            .create()
            .show()

    }



    private fun updateRequest(){
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        val db = FirebaseFirestore.getInstance()
        if (uid != null) {
            db.collection("employeeDetails")
                .document(uid).update("deletePermission", "false")
                .addOnSuccessListener {

                }
        }
    }


}