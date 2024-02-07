

<?php
$servername = "localhost";
$username = "root";
$password = "";
$database = "employeeattendance";

// Create connection
$conn = new mysqli($servername, $username, $password, $database);

if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
}

$employeeName = $_POST['employeeName'];
$date = $_POST['date'];
$checkInTime = $_POST['checkInTime'];
$checkInLocation = $_POST['checkInLocation'];
$checkOutTime = $_POST['checkOutTime'];
$checkOutLocation = $_POST['checkOutLocation'];

$sql = "INSERT INTO attendance1 (Employee_Name, Date, Check_In_Time, Check_In_Location, Check_Out_Time, Check_Out_Location)
        VALUES ('$employeeName', '$date', '$checkInTime', '$checkInLocation', '$checkOutTime', '$checkOutLocation')";

if ($conn->query($sql) === TRUE) {
    echo "Attendance record inserted successfully";
} else {
    echo "Error inserting attendance record: " . $conn->error;
}

$conn->close();
?>
