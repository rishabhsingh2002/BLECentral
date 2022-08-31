package com.rpt11.bleproofcentral

import android.Manifest
import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import com.google.android.material.switchmaterial.SwitchMaterial
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.Viewport
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import com.jjoe64.graphview.series.PointsGraphSeries
import java.text.SimpleDateFormat
import java.util.*


private const val ENABLE_BLUETOOTH_REQUEST_CODE = 1
private const val LOCATION_PERMISSION_REQUEST_CODE = 2
private const val BLUETOOTH_ALL_PERMISSIONS_REQUEST_CODE = 3
private const val SERVICE_UUID = "25AE1441-05D3-4C5B-8281-93D4E07420CF"
private const val CHAR_FOR_READ_UUID = "25AE1442-05D3-4C5B-8281-93D4E07420CF"
private const val CHAR_FOR_WRITE_UUID = "25AE1443-05D3-4C5B-8281-93D4E07420CF"
private const val CHAR_FOR_INDICATE_UUID = "25AE1444-05D3-4C5B-8281-93D4E07420CF"
private const val CCC_DESCRIPTOR_UUID = "00002902-0000-1000-8000-00805f9b34fb"
private const val CHAR_FOR_WRITE_UUID_START = "25AE1445-05D3-4C5B-8281-93D4E07420CF"
private const val CHAR_FOR_READ_UUID_ANGLE = "25AE1446-05D3-4C5B-8281-93D4E07420CF"
private const val CHAR_FOR_READ_UUID_MAX_ANGLE = "25AE1447-05D3-4C5B-8281-93D4E07420CF"
private const val CHAR_FOR_READ_UUID_TIME = "25AE1448-05D3-4C5B-8281-93D4E07420CF"
private const val CHAR_FOR_READ_UUID_NUMBER_OF_REPS = "25AE1449-05D3-4C5B-8281-93D4E07420CF"
private const val CHAR_FOR_READ_UUID_SCANNED_EXERCISE = "25AE1450-05D3-4C5B-8281-93D4E07420CF"
private const val CHAR_FOR_READ_UUID_SUMMARY = "25AE1451-05D3-4C5B-8281-93D4E07420CF"
private const val CHAR_FOR_READ_UUID_SUMMARY_X_AXIS = "25AE1452-05D3-4C5B-8281-93D4E07420CF"
private const val CHAR_FOR_READ_UUID_SUMMARY_Y_AXIS = "25AE1453-05D3-4C5B-8281-93D4E07420CF"


class MainActivity : AppCompatActivity() {
    enum class BLELifecycleState {
        Disconnected,
        Scanning,
        Connecting,
        ConnectedDiscovering,
        ConnectedSubscribing,
        Connected
    }

    private var lifecycleState = BLELifecycleState.Disconnected
        set(value) {
            field = value
            appendLog("status = $value")
            runOnUiThread {
                textViewLifecycleState.text = "State: ${value.name}"
                if (value != BLELifecycleState.Connected) {
                    textViewSubscription.text = getString(R.string.text_not_subscribed)
                }
            }
        }

    private val switchConnect: SwitchMaterial
        get() = findViewById<SwitchMaterial>(R.id.switchConnect)
    private val textViewLifecycleState: TextView
        get() = findViewById<TextView>(R.id.textViewLifecycleState)
    private val textViewReadValue: TextView
        get() = findViewById<TextView>(R.id.textViewReadValue)
    private val editTextWriteValue: TextView
        get() = findViewById<EditText>(R.id.editTextWriteValue)
    private val editTextZero: TextView
        get() = findViewById<EditText>(R.id.edit_zero)
    private val editTextOne: TextView
        get() = findViewById<EditText>(R.id.edit_one)
    private val textViewIndicateValue: TextView
        get() = findViewById<TextView>(R.id.textViewIndicateValue)
    private val textViewSubscription: TextView
        get() = findViewById<TextView>(R.id.textViewSubscription)
    private val textViewLog: TextView
        get() = findViewById<TextView>(R.id.textViewLog)
    private val scrollViewLog: ScrollView
        get() = findViewById<ScrollView>(R.id.scrollViewLog)
    private val btnNext: Button get() = findViewById(R.id.btn_next)
    private val cl_first: ConstraintLayout get() = findViewById(R.id.firstScreen)
    private val cl_second: ConstraintLayout get() = findViewById(R.id.second_screen)
    private val cl_third: ConstraintLayout get() = findViewById(R.id.third_screen)
    private val btnWrite: Button get() = findViewById(R.id.buttonWrite)
    private val btnStart: Button get() = findViewById(R.id.btn_start)
    private val btnEnd: Button get() = findViewById(R.id.btn_end)
    private val cl_read: ConstraintLayout get() = findViewById(R.id.cl_read)

    private val clUser: ConstraintLayout get() = findViewById(R.id.ll_user)
    private val btnUser: Button get() = findViewById(R.id.btn_user)

    //Doctor Screen
    private val btnDoctor: Button get() = findViewById(R.id.btn_doctor)
    private val clDoctor: ConstraintLayout get() = findViewById(R.id.cl_doctor)

    private val TAG = "MainActivity"

    //add PointsGraphSeries of DataPoint type
    var xySeries: PointsGraphSeries<DataPoint>? = null



    private val tvXAxis: TextView
        get() = findViewById<TextView>(R.id.tv_x_axis)
    private val tvYAxis: TextView
        get() = findViewById<TextView>(R.id.tv_y_axis)

    var graph: GraphView? = null


    //make xyValueArray global
    private var xyValueArray: ArrayList<XYValues>? = null





    private val tvAngle: TextView
        get() = findViewById<TextView>(R.id.tv_angle)
    private val tvMaxAngle: TextView
        get() = findViewById<TextView>(R.id.tv_max_angle)
    private val tvTime: TextView
        get() = findViewById<TextView>(R.id.tv_time)
    private val tvNoOfReps: TextView
        get() = findViewById<TextView>(R.id.tv_no_of_reps)
    private val tvScannedExercise: TextView
        get() = findViewById<TextView>(R.id.tv_scanned_exercise)
    private val tvSummary: TextView
        get() = findViewById<TextView>(R.id.tv_summary)

    private val tvShowAngle: TextView
        get() = findViewById<TextView>(R.id.tv_show_angle)
    private val tvShowTime: TextView
        get() = findViewById<TextView>(R.id.tv_show_time)
    private val tvShowMaxAngle: TextView
        get() = findViewById<TextView>(R.id.tv_show_max_angle)
    private val tvShowNoOfReps: TextView
        get() = findViewById<TextView>(R.id.tv_show_number_of_reps)
    private val tvShowScannedExercise: TextView
        get() = findViewById<TextView>(R.id.tv_show_scanned_exercise)
    private val tvShowSummary: TextView
        get() = findViewById<TextView>(R.id.tv_show_summery)

    private val ll_five_details: LinearLayout
        get() = findViewById<LinearLayout>(R.id.ll_five_details)

    private val btnShowX :Button get() = findViewById<Button>(R.id.btn_show_x)
    private val btnShowY :Button get() = findViewById<Button>(R.id.btn_show_y)


    private val userWantsToScanAndConnect: Boolean get() = switchConnect.isChecked
    private var isScanning = false
    private var connectedGatt: BluetoothGatt? = null
    private var characteristicForRead: BluetoothGattCharacteristic? = null
    private var characteristicForReadAngle: BluetoothGattCharacteristic? = null
    private var characteristicForReadMaxAngle: BluetoothGattCharacteristic? = null
    private var characteristicForReadTime: BluetoothGattCharacteristic? = null
    private var characteristicForReadNoOfReps: BluetoothGattCharacteristic? = null
    private var characteristicForReadScannedExercise: BluetoothGattCharacteristic? = null
    private var characteristicForReadSummary: BluetoothGattCharacteristic? = null
    private var characteristicForWrite: BluetoothGattCharacteristic? = null
    private var characteristicForStart: BluetoothGattCharacteristic? = null
    private var characteristicForIndicate: BluetoothGattCharacteristic? = null
    //graph
    private var characteristicForReadXAxis: BluetoothGattCharacteristic? = null
    private var characteristicForReadYAxis: BluetoothGattCharacteristic? = null

    //TODO: graph -2
    private val RANDOM: Random? = Random()
    private var series: LineGraphSeries<DataPoint>? = null
    private var lastX = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //TODO: did find view by id -1
        graph = findViewById<View>(R.id.lineChart) as GraphView
        xyValueArray = ArrayList()

        // data
        series = LineGraphSeries<DataPoint>();
        graph!!.addSeries(series);
        // customize a little bit viewport
        var viewport:Viewport = graph!!.getViewport();
        viewport.setYAxisBoundsManual(true);
        viewport.setMinY(0.0);
        viewport.setMinX(0.0);
        viewport.setMaxY(150.0);
        viewport.setMaxX(150.0)
        viewport.setScrollable(true);




        //end

      btnUser.setOnClickListener {
          clUser.visibility = View.VISIBLE
          clDoctor.visibility = View.GONE
      }

        btnDoctor.setOnClickListener {
            clUser.visibility = View.GONE
            clDoctor.visibility = View.VISIBLE
        }
//        if (tvXAxis.text.isEmpty() && tvYAxis.text.isEmpty()){
//
//        }else{
//            //init()
//        }

        btnShowX.setOnClickListener {
            readXAxis()
        }
        btnShowY.setOnClickListener {
            readYAxis()
        }

//        btnShowXY.setOnClickListener {
////            readXAxis()
////            Log.d("###","XAxis appeared")
////            readYAxis()
//        }



        switchConnect.setOnCheckedChangeListener { _, isChecked ->
            when (isChecked) {
                true -> {
                    val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
                    registerReceiver(bleOnOffListener, filter)
                }
                false -> {
                    unregisterReceiver(bleOnOffListener)
                }
            }
            bleRestartLifecycle()
        }
        appendLog("MainActivity.onCreate")

        btnNext.setOnClickListener {
            cl_second.visibility = View.VISIBLE
            cl_first.visibility = View.INVISIBLE


        }

        btnStart.setOnClickListener {
            onButtonStart()
        }
        btnEnd.setOnClickListener {
            onButtonEnd()

        }
        //read values


        tvShowAngle.setOnClickListener {
            readAngle()
        }
        tvShowTime.setOnClickListener {
            readTime()
            tvShowTime.isPressed = true
        }

        tvShowMaxAngle.setOnClickListener {
            readMaxAngle()
        }
        tvShowNoOfReps.setOnClickListener {
            readNoOfReps()
        }
        tvShowScannedExercise.setOnClickListener {
            readScannedExercises()
        }
        tvShowSummary.setOnClickListener {
            tvSummary.visibility = View.VISIBLE
            readSummary()
          //  ll_five_details.visibility = View.INVISIBLE
        }


        //graph 1
        val timer5 = object: CountDownTimer(300, 10) {
            override fun onTick(millisUntilFinished: Long) {}

            override fun onFinish() {
                btnShowX.performClick()
                this.start()
            }
        }
        timer5.start()

        val timer6 = object: CountDownTimer(300, 10) {
            override fun onTick(millisUntilFinished: Long) {}

            override fun onFinish() {
                btnShowY.performClick()
                this.start()
            }
        }
        timer6.start()



        //angle
        val timer1 = object: CountDownTimer(300, 10) {
            override fun onTick(millisUntilFinished: Long) {}

            override fun onFinish() {
                tvShowAngle.performClick()
                this.start()
            }
        }
        timer1.start()


        //numperofreps
        val timer3 = object: CountDownTimer(500, 10) {
            override fun onTick(millisUntilFinished: Long) {}

            override fun onFinish() {
                tvShowNoOfReps.performClick()
                this.start()
            }
        }
        timer3.start()

        //time
        val timer4 = object: CountDownTimer(500, 10) {
            override fun onTick(millisUntilFinished: Long) {}

            override fun onFinish() {
                tvShowTime.performClick()
                this.start()
            }
        }
        timer4.start()

    }

    //TODO: -3
    override fun onResume() {
        super.onResume()

        // we're going to simulate real time with thread that append data to the graph
        // we're going to simulate real time with thread that append data to the graph
        Thread {
            // we add 100 new entries
            for (i in 0..99) {
                runOnUiThread { addEntry() }

                // sleep to slow down the add of entries
                try {
                    Thread.sleep(600)
                } catch (e: InterruptedException) {
                    // manage error ...
                }
            }
        }.start()

    }

    //TODO -4
    // add random data to graph
    private  fun addEntry() {
        // here, we choose to display max 10 points on the viewport and we scroll to end
        var x = tvXAxis.text.toString().toDouble()
        var y = tvYAxis.text.toString().toDouble()

            return series!!.appendData(DataPoint(x, y), true, 10)


    }


    private fun readScannedExercises() {
        var gatt = connectedGatt ?: run {
            appendLog("ERROR: read failed, no connected device")
            return
        }
        var characteristic = characteristicForReadScannedExercise ?: run {
            appendLog("ERROR: read failed, characteristic unavailable $CHAR_FOR_READ_UUID_SCANNED_EXERCISE")
            return
        }
        if (!characteristic.isReadable()) {
            appendLog("ERROR: read failed, characteristic not readable $CHAR_FOR_READ_UUID_SCANNED_EXERCISE")
            return
        }
        gatt.readCharacteristic(characteristic)
    }

    private fun readNoOfReps() {
        var gatt = connectedGatt ?: run {
            appendLog("ERROR: read failed, no connected device")
            return
        }
        var characteristic = characteristicForReadNoOfReps ?: run {
            appendLog("ERROR: read failed, characteristic unavailable $CHAR_FOR_READ_UUID_NUMBER_OF_REPS")
            return
        }
        if (!characteristic.isReadable()) {
            appendLog("ERROR: read failed, characteristic not readable $CHAR_FOR_READ_UUID_NUMBER_OF_REPS")
            return
        }
        gatt.readCharacteristic(characteristic)
    }

    private fun readMaxAngle() {
        var gatt = connectedGatt ?: run {
            appendLog("ERROR: read failed, no connected device")
            return
        }
        var characteristic = characteristicForReadMaxAngle ?: run {
            appendLog("ERROR: read failed, characteristic unavailable $CHAR_FOR_READ_UUID_MAX_ANGLE")
            return
        }
        if (!characteristic.isReadable()) {
            appendLog("ERROR: read failed, characteristic not readable $CHAR_FOR_READ_UUID_MAX_ANGLE")
            return
        }
        gatt.readCharacteristic(characteristic)
    }

    private fun readTime() {
        //time
        var gatt = connectedGatt ?: run {
            appendLog("ERROR: read failed, no connected device")
            return
        }
        var characteristic = characteristicForReadTime ?: run {
            appendLog("ERROR: read failed, characteristic unavailable $CHAR_FOR_READ_UUID_TIME")
            return
        }
        if (!characteristic.isReadable()) {
            appendLog("ERROR: read failed, characteristic not readable $CHAR_FOR_READ_UUID_TIME")
            return
        }
        gatt.readCharacteristic(characteristic)
    }

    private fun onButtonEnd() {
        var gatt = connectedGatt ?: run {
            appendLog("ERROR: write failed, no connected device")
            return
        }
        var characteristic = characteristicForStart ?: run {
            appendLog("ERROR: write failed, characteristic unavailable $CHAR_FOR_WRITE_UUID_START")
            return
        }
        if (!characteristic.isWriteable()) {
            appendLog("ERROR: write failed, characteristic not writeable $CHAR_FOR_WRITE_UUID_START")
            return
        }
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        characteristic.value = editTextOne.text.toString().toByteArray(Charsets.UTF_8)
        gatt.writeCharacteristic(characteristic)


        tvShowSummary.visibility = View.VISIBLE
        tvShowAngle.visibility = View.INVISIBLE
        tvShowMaxAngle.visibility = View.INVISIBLE
        tvShowTime.visibility = View.INVISIBLE
        tvShowNoOfReps.visibility = View.INVISIBLE
        tvShowScannedExercise.visibility = View.INVISIBLE
        tvAngle.visibility = View.INVISIBLE
        tvMaxAngle.visibility = View.INVISIBLE
        tvTime.visibility = View.INVISIBLE
        tvNoOfReps.visibility = View.INVISIBLE
        tvScannedExercise.visibility = View.INVISIBLE

    }

    private fun onButtonStart() {
        var gatt = connectedGatt ?: run {
            appendLog("ERROR: write failed, no connected device")
            return
        }
        var characteristic = characteristicForStart ?: run {
            appendLog("ERROR: write failed, characteristic unavailable $CHAR_FOR_WRITE_UUID_START")
            return
        }
        if (!characteristic.isWriteable()) {
            appendLog("ERROR: write failed, characteristic not writeable $CHAR_FOR_WRITE_UUID_START")
            return
        }
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        characteristic.value = editTextZero.text.toString().toByteArray(Charsets.UTF_8)
        gatt.writeCharacteristic(characteristic)


        tvShowSummary.visibility = View.INVISIBLE
        tvSummary.visibility = View.INVISIBLE
        tvShowAngle.visibility = View.VISIBLE
        tvShowMaxAngle.visibility = View.VISIBLE
        tvShowTime.visibility = View.VISIBLE
        tvShowNoOfReps.visibility = View.VISIBLE
        tvShowScannedExercise.visibility = View.VISIBLE
        tvAngle.visibility = View.VISIBLE
        tvMaxAngle.visibility = View.VISIBLE
        tvTime.visibility = View.VISIBLE
        tvNoOfReps.visibility = View.VISIBLE
        tvScannedExercise.visibility = View.VISIBLE
    }

    private fun readAngle() {

        //angle
        var gatt = connectedGatt ?: run {
            appendLog("ERROR: read failed, no connected device")
            return
        }
        var characteristic = characteristicForReadAngle ?: run {
            appendLog("ERROR: read failed, characteristic unavailable $CHAR_FOR_READ_UUID_ANGLE")
            return
        }
        if (!characteristic.isReadable()) {
            appendLog("ERROR: read failed, characteristic not readable $CHAR_FOR_READ_UUID_ANGLE")
            return
        }
        gatt.readCharacteristic(characteristic)

    }

    private fun readSummary(){
        var gatt = connectedGatt ?: run {
            appendLog("ERROR: read failed, no connected device")
            return
        }
        var characteristic = characteristicForReadSummary ?: run {
            appendLog("ERROR: read failed, characteristic unavailable $CHAR_FOR_READ_UUID_SUMMARY")
            return
        }
        if (!characteristic.isReadable()) {
            appendLog("ERROR: read failed, characteristic not readable $CHAR_FOR_READ_UUID_SUMMARY")
            return
        }
        gatt.readCharacteristic(characteristic)
    }



    override fun onDestroy() {
        bleEndLifecycle()
        super.onDestroy()
    }

    fun onTapRead(view: View) {
        var gatt = connectedGatt ?: run {
            appendLog("ERROR: read failed, no connected device")
            return
        }
        var characteristic = characteristicForReadMaxAngle ?: run {
            appendLog("ERROR: read failed, characteristic unavailable $CHAR_FOR_READ_UUID")
            return
        }
        if (!characteristic.isReadable()) {
            appendLog("ERROR: read failed, characteristic not readable $CHAR_FOR_READ_UUID")
            return
        }
        gatt.readCharacteristic(characteristic)

    }

    //graph
    fun readXAxis(){
        var gatt = connectedGatt ?: run {
            appendLog("ERROR: read failed, no connected device")
            return
        }
        var characteristic = characteristicForReadXAxis ?: run {
            appendLog("ERROR: read failed, characteristic unavailable $CHAR_FOR_READ_UUID_SUMMARY_X_AXIS")
            return
        }
        if (!characteristic.isReadable()) {
            appendLog("ERROR: read failed, characteristic not readable $CHAR_FOR_READ_UUID_SUMMARY_X_AXIS")
            return
        }
        gatt.readCharacteristic(characteristic)
    }
    fun readYAxis(){
        var gatt = connectedGatt ?: run {
            appendLog("ERROR: read failed, no connected device")
            return
        }
        var characteristic = characteristicForReadYAxis ?: run {
            appendLog("ERROR: read failed, characteristic unavailable $CHAR_FOR_READ_UUID_SUMMARY_Y_AXIS")
            return
        }
        if (!characteristic.isReadable()) {
            appendLog("ERROR: read failed, characteristic not readable $CHAR_FOR_READ_UUID_SUMMARY_Y_AXIS")
            return
        }
        gatt.readCharacteristic(characteristic)
    }

    fun onTapWrite(view: View) {
        cl_third.visibility = View.VISIBLE
        cl_second.visibility = View.INVISIBLE
        cl_first.visibility = View.INVISIBLE

        var gatt = connectedGatt ?: run {
            appendLog("ERROR: write failed, no connected device")
            return
        }
        var characteristic = characteristicForWrite ?: run {
            appendLog("ERROR: write failed, characteristic unavailable $CHAR_FOR_WRITE_UUID")
            return
        }
        if (!characteristic.isWriteable()) {
            appendLog("ERROR: write failed, characteristic not writeable $CHAR_FOR_WRITE_UUID")
            return
        }
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        characteristic.value = editTextWriteValue.text.toString().toByteArray(Charsets.UTF_8)
        gatt.writeCharacteristic(characteristic)
    }

    fun onTapClearLog(view: View) {
        textViewLog.text = "Logs:"
        appendLog("log cleared")
    }

    private fun appendLog(message: String) {
        Log.d("appendLog", message)
        runOnUiThread {
            val strTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            textViewLog.text = textViewLog.text.toString() + "\n$strTime $message"

            // scroll after delay, because textView has to be updated first
            Handler().postDelayed({
                scrollViewLog.fullScroll(View.FOCUS_DOWN)
            }, 16)
        }
    }

    private fun bleEndLifecycle() {
        safeStopBleScan()
        connectedGatt?.close()
        setConnectedGattToNull()
        lifecycleState = BLELifecycleState.Disconnected
    }

    private fun setConnectedGattToNull() {
        connectedGatt = null
        characteristicForRead = null
        characteristicForReadAngle = null
        characteristicForReadMaxAngle = null
        characteristicForReadTime= null
        characteristicForReadNoOfReps = null
        characteristicForReadScannedExercise = null
        characteristicForReadSummary = null
        characteristicForWrite = null
        characteristicForStart = null
        characteristicForIndicate = null
        //graph
        characteristicForReadXAxis = null
        characteristicForReadYAxis = null
    }

    private fun bleRestartLifecycle() {
        runOnUiThread {
            if (userWantsToScanAndConnect) {
                if (connectedGatt == null) {
                    prepareAndStartBleScan()
                } else {
                    connectedGatt?.disconnect()
                }
            } else {
                bleEndLifecycle()
            }
        }
    }

    private fun prepareAndStartBleScan() {
        ensureBluetoothCanBeUsed { isSuccess, message ->
            appendLog(message)
            if (isSuccess) {
                safeStartBleScan()
            }
        }
    }

    private fun safeStartBleScan() {
        if (isScanning) {
            appendLog("Already scanning")
            return
        }

        val serviceFilter = scanFilter.serviceUuid?.uuid.toString()
        appendLog("Starting BLE scan, filter: $serviceFilter")

        isScanning = true
        lifecycleState = BLELifecycleState.Scanning
        bleScanner.startScan(mutableListOf(scanFilter), scanSettings, scanCallback)
    }

    private fun safeStopBleScan() {
        if (!isScanning) {
            appendLog("Already stopped")
            return
        }

        appendLog("Stopping BLE scan")
        isScanning = false
        bleScanner.stopScan(scanCallback)
    }

    private fun subscribeToIndications(
        characteristic: BluetoothGattCharacteristic,
        gatt: BluetoothGatt
    ) {
        val cccdUuid = UUID.fromString(CCC_DESCRIPTOR_UUID)
        characteristic.getDescriptor(cccdUuid)?.let { cccDescriptor ->
            if (!gatt.setCharacteristicNotification(characteristic, true)) {
                appendLog("ERROR: setNotification(true) failed for ${characteristic.uuid}")
                return
            }
            cccDescriptor.value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            gatt.writeDescriptor(cccDescriptor)
        }
    }

    private fun unsubscribeFromCharacteristic(characteristic: BluetoothGattCharacteristic) {
        val gatt = connectedGatt ?: return

        val cccdUuid = UUID.fromString(CCC_DESCRIPTOR_UUID)
        characteristic.getDescriptor(cccdUuid)?.let { cccDescriptor ->
            if (!gatt.setCharacteristicNotification(characteristic, false)) {
                appendLog("ERROR: setNotification(false) failed for ${characteristic.uuid}")
                return
            }
            cccDescriptor.value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(cccDescriptor)
        }
    }

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    //region BLE Scanning
    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    private val scanFilter = ScanFilter.Builder()
        .setServiceUuid(ParcelUuid(UUID.fromString(SERVICE_UUID)))
        .build()

    private val scanSettings: ScanSettings
        get() {
            return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                scanSettingsSinceM
            } else {
                scanSettingsBeforeM
            }
        }

    private val scanSettingsBeforeM = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
        .setReportDelay(0)
        .build()

    @RequiresApi(Build.VERSION_CODES.M)
    private val scanSettingsSinceM = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
        .setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH)
        .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
        .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
        .setReportDelay(0)
        .build()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val name: String? = result.scanRecord?.deviceName ?: result.device.name
            appendLog("onScanResult name=$name address= ${result.device?.address}")
            safeStopBleScan()
            lifecycleState = BLELifecycleState.Connecting
            result.device.connectGatt(this@MainActivity, false, gattCallback)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            appendLog("onBatchScanResults, ignoring")
        }

        override fun onScanFailed(errorCode: Int) {
            appendLog("onScanFailed errorCode=$errorCode")
            safeStopBleScan()
            lifecycleState = BLELifecycleState.Disconnected
            bleRestartLifecycle()
        }
    }
    //endregion

    //region BLE events, when connected
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            // TODO: timeout timer: if this callback not called - disconnect(), wait 120ms, close()

            val deviceAddress = gatt.device.address

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    appendLog("Connected to $deviceAddress")

                    // TODO: bonding state

                    // recommended on UI thread https://punchthrough.com/android-ble-guide/
                    Handler(Looper.getMainLooper()).post {
                        lifecycleState = BLELifecycleState.ConnectedDiscovering
                        gatt.discoverServices()
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    appendLog("Disconnected from $deviceAddress")
                    setConnectedGattToNull()
                    gatt.close()
                    lifecycleState = BLELifecycleState.Disconnected
                    bleRestartLifecycle()
                }
            } else {
                // TODO: random error 133 - close() and try reconnect

                appendLog("ERROR: onConnectionStateChange status=$status deviceAddress=$deviceAddress, disconnecting")

                setConnectedGattToNull()
                gatt.close()
                lifecycleState = BLELifecycleState.Disconnected
                bleRestartLifecycle()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            appendLog("onServicesDiscovered services.count=${gatt.services.size} status=$status")

            if (status == 129 /*GATT_INTERNAL_ERROR*/) {
                // it should be a rare case, this article recommends to disconnect:
                // https://medium.com/@martijn.van.welie/making-android-ble-work-part-2-47a3cdaade07
                appendLog("ERROR: status=129 (GATT_INTERNAL_ERROR), disconnecting")
                gatt.disconnect()
                return
            }

            val service = gatt.getService(UUID.fromString(SERVICE_UUID)) ?: run {
                appendLog("ERROR: Service not found $SERVICE_UUID, disconnecting")
                gatt.disconnect()
                return
            }

            connectedGatt = gatt
            //graph
            characteristicForReadXAxis = service.getCharacteristic(UUID.fromString(
                CHAR_FOR_READ_UUID_SUMMARY_X_AXIS))
            characteristicForReadYAxis = service.getCharacteristic(UUID.fromString(
                CHAR_FOR_READ_UUID_SUMMARY_Y_AXIS))
            //end
            characteristicForRead = service.getCharacteristic(UUID.fromString(CHAR_FOR_READ_UUID))
            characteristicForReadAngle = service.getCharacteristic(UUID.fromString(
                CHAR_FOR_READ_UUID_ANGLE))
            characteristicForReadMaxAngle = service.getCharacteristic(UUID.fromString(
                CHAR_FOR_READ_UUID_MAX_ANGLE))
            characteristicForReadTime = service.getCharacteristic(UUID.fromString(
                CHAR_FOR_READ_UUID_TIME))
            characteristicForReadScannedExercise = service.getCharacteristic(UUID.fromString(
                CHAR_FOR_READ_UUID_SCANNED_EXERCISE))
            characteristicForReadSummary = service.getCharacteristic(UUID.fromString(
                CHAR_FOR_READ_UUID_SUMMARY))
            characteristicForReadNoOfReps = service.getCharacteristic(UUID.fromString(
                CHAR_FOR_READ_UUID_NUMBER_OF_REPS))
            characteristicForWrite = service.getCharacteristic(UUID.fromString(CHAR_FOR_WRITE_UUID))
            characteristicForStart = service.getCharacteristic(UUID.fromString(
                CHAR_FOR_WRITE_UUID_START))
            characteristicForIndicate =
                service.getCharacteristic(UUID.fromString(CHAR_FOR_INDICATE_UUID))

            characteristicForIndicate?.let {
                lifecycleState = BLELifecycleState.ConnectedSubscribing
                subscribeToIndications(it, gatt)
            } ?: run {
                appendLog("WARN: characteristic not found $CHAR_FOR_INDICATE_UUID")
                lifecycleState = BLELifecycleState.Connected
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            when (characteristic.uuid) {
                UUID.fromString(CHAR_FOR_READ_UUID_MAX_ANGLE) -> {
                    val strValue = characteristic.value.toString(Charsets.UTF_8)
                    val log = "onCharacteristicRead " + when (status) {
                        BluetoothGatt.GATT_SUCCESS -> "OK, value=\"$strValue\""
                        BluetoothGatt.GATT_READ_NOT_PERMITTED -> "not allowed"
                        else -> "error $status"
                    }
                    appendLog(log)
                    runOnUiThread {
                        tvMaxAngle.text = strValue
                    }
                }
                UUID.fromString(CHAR_FOR_READ_UUID_ANGLE) -> {
                    val strValue = characteristic.value.toString(Charsets.UTF_8)
                    val log = "onCharacteristicRead " + when (status) {
                        BluetoothGatt.GATT_SUCCESS -> "OK, value=\"$strValue\""
                        BluetoothGatt.GATT_READ_NOT_PERMITTED -> "not allowed"
                        else -> "error $status"
                    }
                    appendLog(log)
                    runOnUiThread {
                        tvAngle.text = strValue
                    }
                }
                UUID.fromString(CHAR_FOR_READ_UUID_MAX_ANGLE) -> {
                    val strValue = characteristic.value.toString(Charsets.UTF_8)
                    val log = "onCharacteristicRead " + when (status) {
                        BluetoothGatt.GATT_SUCCESS -> "OK, value=\"$strValue\""
                        BluetoothGatt.GATT_READ_NOT_PERMITTED -> "not allowed"
                        else -> "error $status"
                    }
                    appendLog(log)
                    runOnUiThread {
                        tvMaxAngle.text = strValue
                    }
                }
                UUID.fromString(CHAR_FOR_READ_UUID_TIME) -> {
                    val strValue = characteristic.value.toString(Charsets.UTF_8)
                    val log = "onCharacteristicRead " + when (status) {
                        BluetoothGatt.GATT_SUCCESS -> "OK, value=\"$strValue\""
                        BluetoothGatt.GATT_READ_NOT_PERMITTED -> "not allowed"
                        else -> "error $status"
                    }
                    appendLog(log)
                    runOnUiThread {
                        tvTime.text = strValue
                    }
                }
                UUID.fromString(CHAR_FOR_READ_UUID_NUMBER_OF_REPS) -> {
                    val strValue = characteristic.value.toString(Charsets.UTF_8)
                    val log = "onCharacteristicRead " + when (status) {
                        BluetoothGatt.GATT_SUCCESS -> "OK, value=\"$strValue\""
                        BluetoothGatt.GATT_READ_NOT_PERMITTED -> "not allowed"
                        else -> "error $status"
                    }
                    appendLog(log)
                    runOnUiThread {
                        tvNoOfReps.text = strValue
                    }
                }
                UUID.fromString(CHAR_FOR_READ_UUID_SCANNED_EXERCISE) -> {
                    val strValue = characteristic.value.toString(Charsets.UTF_8)
                    val log = "onCharacteristicRead " + when (status) {
                        BluetoothGatt.GATT_SUCCESS -> "OK, value=\"$strValue\""
                        BluetoothGatt.GATT_READ_NOT_PERMITTED -> "not allowed"
                        else -> "error $status"
                    }
                    appendLog(log)
                    runOnUiThread {
                        tvScannedExercise.text = strValue
                    }
                }
                UUID.fromString(CHAR_FOR_READ_UUID_SUMMARY) -> {
                    val strValue = characteristic.value.toString(kotlin.text.Charsets.UTF_8)
                    val log = "onCharacteristicRead " + when (status) {
                        android.bluetooth.BluetoothGatt.GATT_SUCCESS -> "OK, value=\"$strValue\""
                        android.bluetooth.BluetoothGatt.GATT_READ_NOT_PERMITTED -> "not allowed"
                        else -> "error $status"
                    }
                    appendLog(log)
                    runOnUiThread {
                        tvSummary.text = strValue
                    }
                }
                //graph
                UUID.fromString(CHAR_FOR_READ_UUID_SUMMARY_X_AXIS) -> {
                    val strValue = characteristic.value.toString(Charsets.UTF_8)
                    val log = "onCharacteristicRead " + when (status) {
                        BluetoothGatt.GATT_SUCCESS -> "OK, value=\"$strValue\""
                        BluetoothGatt.GATT_READ_NOT_PERMITTED -> "not allowed"
                        else -> "error $status"
                    }
                    appendLog(log)
                    runOnUiThread {
                        tvXAxis.text = strValue
                    }
                }
                UUID.fromString(CHAR_FOR_READ_UUID_SUMMARY_Y_AXIS) -> {
                    val strValue = characteristic.value.toString(Charsets.UTF_8)
                    val log = "onCharacteristicRead " + when (status) {
                        BluetoothGatt.GATT_SUCCESS -> "OK, value=\"$strValue\""
                        BluetoothGatt.GATT_READ_NOT_PERMITTED -> "not allowed"
                        else -> "error $status"
                    }
                    appendLog(log)
                    runOnUiThread {
                        tvYAxis.text = strValue
                    }
                }
                else -> {
                    appendLog("onCharacteristicRead unknown uuid $characteristic.uuid")
                }
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (characteristic.uuid == UUID.fromString(CHAR_FOR_WRITE_UUID)) {
                val log: String = "onCharacteristicWrite " + when (status) {
                    BluetoothGatt.GATT_SUCCESS -> "OK"
                    BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> "not allowed"
                    BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH -> "invalid length"
                    else -> "error $status"
                }
                appendLog(log)
            }else  if (characteristic.uuid == UUID.fromString(CHAR_FOR_WRITE_UUID_START)) {
                val log: String = "onCharacteristicWrite " + when (status) {
                    BluetoothGatt.GATT_SUCCESS -> "OK"
                    BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> "not allowed"
                    BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH -> "invalid length"
                    else -> "error $status"
                }
                appendLog(log)
            }
            else {
                appendLog("onCharacteristicWrite unknown uuid $characteristic.uuid")
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (characteristic.uuid == UUID.fromString(CHAR_FOR_INDICATE_UUID)) {
                val strValue = characteristic.value.toString(Charsets.UTF_8)
                appendLog("onCharacteristicChanged value=\"$strValue\"")
                runOnUiThread {
                    textViewIndicateValue.text = strValue
                }
            } else {
                appendLog("onCharacteristicChanged unknown uuid $characteristic.uuid")
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            if (descriptor.characteristic.uuid == UUID.fromString(CHAR_FOR_INDICATE_UUID)) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val value = descriptor.value
                    val isSubscribed = value.isNotEmpty() && value[0].toInt() != 0
                    val subscriptionText = when (isSubscribed) {
                        true -> getString(R.string.text_subscribed)
                        false -> getString(R.string.text_not_subscribed)
                    }
                    appendLog("onDescriptorWrite $subscriptionText")
                    runOnUiThread {
                        textViewSubscription.text = subscriptionText
                    }
                } else {
                    appendLog("ERROR: onDescriptorWrite status=$status uuid=${descriptor.uuid} char=${descriptor.characteristic.uuid}")
                }

                // subscription processed, consider connection is ready for use
                lifecycleState = BLELifecycleState.Connected
            } else {
                appendLog("onDescriptorWrite unknown uuid $descriptor.characteristic.uuid")
            }
        }
    }
    //endregion

    //region BluetoothGattCharacteristic extension
    fun BluetoothGattCharacteristic.isReadable(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_READ)

    fun BluetoothGattCharacteristic.isWriteable(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE)

    fun BluetoothGattCharacteristic.isWriteableWithoutResponse(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)

    fun BluetoothGattCharacteristic.isNotifiable(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_NOTIFY)

    fun BluetoothGattCharacteristic.isIndicatable(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_INDICATE)

    private fun BluetoothGattCharacteristic.containsProperty(property: Int): Boolean {
        return (properties and property) != 0
    }
    //endregion

    //region Permissions and Settings management
    enum class AskType {
        AskOnce,
        InsistUntilSuccess
    }

    private var activityResultHandlers = mutableMapOf<Int, (Int) -> Unit>()
    private var permissionResultHandlers =
        mutableMapOf<Int, (Array<out String>, IntArray) -> Unit>()
    private var bleOnOffListener = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)) {
                BluetoothAdapter.STATE_ON -> {
                    appendLog("onReceive: Bluetooth ON")
                    if (lifecycleState == BLELifecycleState.Disconnected) {
                        bleRestartLifecycle()
                    }
                }
                BluetoothAdapter.STATE_OFF -> {
                    appendLog("onReceive: Bluetooth OFF")
                    bleEndLifecycle()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        activityResultHandlers[requestCode]?.let { handler ->
            handler(resultCode)
        } ?: runOnUiThread {
            appendLog("ERROR: onActivityResult requestCode=$requestCode result=$resultCode not handled")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionResultHandlers[requestCode]?.let { handler ->
            handler(permissions, grantResults)
        } ?: runOnUiThread {
            appendLog("ERROR: onRequestPermissionsResult requestCode=$requestCode not handled")
        }
    }

    private fun ensureBluetoothCanBeUsed(completion: (Boolean, String) -> Unit) {
        grantBluetoothCentralPermissions(AskType.AskOnce) { isGranted ->
            if (!isGranted) {
                completion(false, "Bluetooth permissions denied")
                return@grantBluetoothCentralPermissions
            }

            enableBluetooth(AskType.AskOnce) { isEnabled ->
                if (!isEnabled) {
                    completion(false, "Bluetooth OFF")
                    return@enableBluetooth
                }

                grantLocationPermissionIfRequired(AskType.AskOnce) { isGranted ->
                    if (!isGranted) {
                        completion(false, "Location permission denied")
                        return@grantLocationPermissionIfRequired
                    }

                    completion(true, "Bluetooth ON, permissions OK, ready")
                }
            }
        }
    }

    private fun enableBluetooth(askType: AskType, completion: (Boolean) -> Unit) {
        if (bluetoothAdapter.isEnabled) {
            completion(true)
        } else {
            val intentString = BluetoothAdapter.ACTION_REQUEST_ENABLE
            val requestCode = ENABLE_BLUETOOTH_REQUEST_CODE

            // set activity result handler
            activityResultHandlers[requestCode] = { result ->
                Unit
                val isSuccess = result == Activity.RESULT_OK
                if (isSuccess || askType != AskType.InsistUntilSuccess) {
                    activityResultHandlers.remove(requestCode)
                    completion(isSuccess)
                } else {
                    // start activity for the request again
                    startActivityForResult(Intent(intentString), requestCode)
                }
            }

            // start activity for the request
            startActivityForResult(Intent(intentString), requestCode)
        }
    }

    private fun grantLocationPermissionIfRequired(askType: AskType, completion: (Boolean) -> Unit) {
        val wantedPermissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // BLUETOOTH_SCAN permission has flag "neverForLocation", so location not needed
            completion(true)
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || hasPermissions(wantedPermissions)) {
            completion(true)
        } else {
            runOnUiThread {
                val requestCode = LOCATION_PERMISSION_REQUEST_CODE

                // prepare motivation message
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Location permission required")
                builder.setMessage("BLE advertising requires location access, starting from Android 6.0")
                builder.setPositiveButton(android.R.string.ok) { _, _ ->
                    requestPermissionArray(wantedPermissions, requestCode)
                }
                builder.setCancelable(false)

                // set permission result handler
                permissionResultHandlers[requestCode] = { permissions, grantResults ->
                    val isSuccess = grantResults.firstOrNull() != PackageManager.PERMISSION_DENIED
                    if (isSuccess || askType != AskType.InsistUntilSuccess) {
                        permissionResultHandlers.remove(requestCode)
                        completion(isSuccess)
                    } else {
                        // show motivation message again
                        builder.create().show()
                    }
                }

                // show motivation message
                builder.create().show()
            }
        }
    }

    private fun grantBluetoothCentralPermissions(askType: AskType, completion: (Boolean) -> Unit) {
        val wantedPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
            )
        } else {
            emptyArray()
        }

        if (wantedPermissions.isEmpty() || hasPermissions(wantedPermissions)) {
            completion(true)
        } else {
            runOnUiThread {
                val requestCode = BLUETOOTH_ALL_PERMISSIONS_REQUEST_CODE

                // set permission result handler
                permissionResultHandlers[requestCode] = { _ /*permissions*/, grantResults ->
                    val isSuccess = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
                    if (isSuccess || askType != AskType.InsistUntilSuccess) {
                        permissionResultHandlers.remove(requestCode)
                        completion(isSuccess)
                    } else {
                        // request again
                        requestPermissionArray(wantedPermissions, requestCode)
                    }
                }

                requestPermissionArray(wantedPermissions, requestCode)
            }
        }
    }

    private fun Context.hasPermissions(permissions: Array<String>): Boolean = permissions.all {
        ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun Activity.requestPermissionArray(permissions: Array<String>, requestCode: Int) {
        ActivityCompat.requestPermissions(this, permissions, requestCode)
    }
    //endregion


//    private fun init() {
//        //declare the xySeries Object
//        xySeries = PointsGraphSeries()
//        btnShowXY!!.setOnClickListener {
//            if (tvXAxis!!.text.toString() != "" && tvYAxis!!.text.toString() != "") {
//                val x = tvXAxis!!.text.toString().toDouble()
//                val y = tvYAxis!!.text.toString().toDouble()
//                Log.d(TAG, "onClick: Adding a new point. (x,y): ($x,$y)")
//                xyValueArray!!.add(XYValues(x, y))
//                init()
//            } else {
//                toastMessage("You must fill out both fields!")
//            }
//        }

//        //little bit of exception handling for if there is no data.
//        if (xyValueArray!!.size != 0) {
//            createScatterPlot()
//        } else {
//            Log.d(TAG, "onCreate: No data to plot.")
//        }
//    }

//    private fun createScatterPlot() {
//        Log.d(TAG, "createScatterPlot: Creating scatter plot.")
//
//        //sort the array of xy values
//        xyValueArray = sortArray(xyValueArray!!)
//
//        //add the data to the series
//        for (i in xyValueArray!!.indices) {
//            try {
//                val x = xyValueArray!![i].getX()
//                val y = xyValueArray!![i].getY()
//                xySeries!!.appendData(DataPoint(x, y), true, 1000)
//            } catch (e: IllegalArgumentException) {
//                Log.e(TAG, "createScatterPlot: IllegalArgumentException: " + e.message)
//            }
//        }
//
//        //set some properties
//        xySeries!!.shape = PointsGraphSeries.Shape.POINT
//        xySeries!!.color = Color.BLUE
//        xySeries!!.size = 4f
//
//        //set Scrollable and Scaleable
//        mScatterPlot!!.viewport.isScalable = true
//        mScatterPlot!!.viewport.setScalableY(true)
//        mScatterPlot!!.viewport.isScrollable = true
//        mScatterPlot!!.viewport.setScrollableY(true)
//
//        //set manual x bounds
//        mScatterPlot!!.viewport.isYAxisBoundsManual = true
//        mScatterPlot!!.viewport.setMaxY(150.0)
//        mScatterPlot!!.viewport.setMinY(-150.0)
//
//        //set manual y bounds
//        mScatterPlot!!.viewport.isXAxisBoundsManual = true
//        mScatterPlot!!.viewport.setMaxX(150.0)
//        mScatterPlot!!.viewport.setMinX(-150.0)
//        mScatterPlot!!.addSeries(xySeries)
//    }

    /**
     * Sorts an ArrayList<XYValue> with respect to the x values.
     * @param array
     * @return
    </XYValue> */
    private fun sortArray(array: ArrayList<XYValues>): ArrayList<XYValues> {
        /*
        //Sorts the xyValues in Ascending order to prepare them for the PointsGraphSeries<DataSet>
         */
        val factor = Math.round(Math.pow(array.size.toDouble(), 2.0)).toString().toInt()
        var m: Int = array.size - 1
        var count = 0
        Log.d(TAG, "sortArray: Sorting the XYArray.")
        while (true) {
            m--
            if (m <= 0) {
                m = array.size - 1
            }
            Log.d(TAG, "sortArray: m = $m")
            try {
                //print out the y entrys so we know what the order looks like
                //Log.d(TAG, "sortArray: Order:");
                //for(int n = 0;n < array.size();n++){
                //Log.d(TAG, "sortArray: " + array.get(n).getY());
                //}
                val tempY: Double = array[m - 1].getY()
                val tempX: Double = array[m - 1].getX()
                if (tempX > array[m].getX()) {
                    array[m - 1].setY(array[m].getY())
                    array[m].setY(tempY)
                    array[m - 1].setX(array[m].getX())
                    array[m].setX(tempX)
                } else if (tempX == array[m].getX()) {
                    count++
                    Log.d(TAG, "sortArray: count = $count")
                } else if (array[m].getX() > array[m - 1].getX()) {
                    count++
                    Log.d(TAG, "sortArray: count = $count")
                }
                //break when factorial is done
                if (count == factor) {
                    break
                }
            } catch (e: ArrayIndexOutOfBoundsException) {
                Log.e(
                    TAG,
                    "sortArray: ArrayIndexOutOfBoundsException. Need more than 1 data point to create Plot." +
                            e.message
                )
                break
            }
        }
        return array
    }

    /**
     * customizable toast
     * @param message
     */
    private fun toastMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

}
