package com.example.kazanmaintenenceapp

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.kazaninventoryapp.Models.Asset
import com.example.kazaninventoryapp.httpservice.httpgetassets
import com.example.kazanmaintenenceapp.API.httpcreatetasks
import com.example.kazanmaintenenceapp.API.httpgettasks
import com.example.kazanmaintenenceapp.API.httpposttask
import com.example.kazanmaintenenceapp.Models.CreateTask
import com.example.kazanmaintenenceapp.Models.Task
import com.example.kazanmaintenenceapp.ui.theme.KazanMaintenenceAppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.intellij.lang.annotations.JdkConstants.HorizontalAlignment
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val colorScheme = lightColorScheme(
            primary = Color(0xFF005CB9),
            onPrimary = Color.White,
            // Add other color customizations if needed
        )

        setContent {
            MaterialTheme(
                colorScheme = colorScheme,
                typography = androidx.compose.material3.Typography(),
                content = {
                    val navController = rememberNavController()
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = "taskScreen",
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable("taskScreen") {
                                TaskScreen(navController, this@MainActivity)
                            }
                            composable("registerNewTask") {
                                RegisteringNewPreventiveMaintenanceTasksScreen(
                                    navController,
                                    this@MainActivity
                                )
                            }
                        }
                    }
                })
            }
        }
    }








@Composable
fun TaskScreen(navController: NavController, context: Context) {
    var activeDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var tasksList by remember { mutableStateOf<List<Task>>(emptyList()) }
    var checkedTasks by remember { mutableStateOf(mutableSetOf<Task>()) }
    val httpgettasks by remember { mutableStateOf(httpgettasks()) }
    val httpgetassets by remember { mutableStateOf(httpgetassets()) }
    var filteredTaskList by remember { mutableStateOf<List<Task>>(emptyList()) }
    var selectedAssetName by remember { mutableStateOf("") }
    var selectedTaskName by remember { mutableStateOf("") }
    var refreshScreen by remember { mutableStateOf(false) }
    var activeDateParsed by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(activeDate)) }
    var fourDaysLater by remember { mutableStateOf(Calendar.getInstance().apply {
        time = activeDateParsed
        add(Calendar.DAY_OF_YEAR, 5)
    }.time) }
    val taskMap = mapOf(
        1 to "Get Tires Rotated and Balanced.",
        2 to "Check Engine Oil",
        3 to "Check Air Filter",
        4 to "Check Battery",
        5 to "Inspect for any damage to paint on pump",
        6 to "Inspect cord and cord placement",
        7 to "Pull each pump and reset"
    )
    var assetsList = remember { mutableStateOf<List<Asset>>(emptyList()) }

    // Function to refresh tasks based on activeDate, selectedAssetName, and selectedTaskName

    fun filterTasks() {
        activeDateParsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(activeDate)
        fourDaysLater=Calendar.getInstance().apply {
            time = activeDateParsed
            add(Calendar.DAY_OF_YEAR, 5)
        }.time
        try {
            val runBasedList = tasksList.filter {
                it.scheduleType == "By Milage"
            }
            val notDoneRunBasedList = runBasedList.filter { !it.taskDone }
            val doneRunBasedList = runBasedList.filter { it.taskDone }

            filteredTaskList = tasksList.filter { task ->
                (selectedAssetName.isEmpty() || task.assetName == selectedAssetName) &&
                        (selectedTaskName.isEmpty() || task.taskName == selectedTaskName) &&
                        (task.scheduleDate == activeDate || task.scheduleKilometer == null ) &&
                        (SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(task.scheduleDate)?.before(fourDaysLater) == true)
            }.sortedBy {
                when {
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it.scheduleDate).before(activeDateParsed) && !it.taskDone -> 0
                    it.scheduleDate == activeDate && !it.taskDone -> 1
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it.scheduleDate)?.after(activeDateParsed) == true &&
                            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it.scheduleDate)?.before(fourDaysLater) == true && !it.taskDone -> 2
                    it.taskDone -> 3
                    else -> 4
                }
            }
            filteredTaskList = notDoneRunBasedList + filteredTaskList + doneRunBasedList
        }catch (e: Exception) {
            e.printStackTrace()
        }

    }


    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val fetchedTasks = httpgettasks.getTasks()
            tasksList = fetchedTasks ?: listOf()

            val fetchedAssets = httpgetassets.getAssets()
            assetsList.value = fetchedAssets ?: listOf()
            filteredTaskList = tasksList
        }
        filterTasks()
    }

    if (refreshScreen) {
        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                val fetchedTasks = httpgettasks.getTasks()
                tasksList = fetchedTasks ?: listOf()

                val fetchedAssets = httpgetassets.getAssets()
                assetsList.value = fetchedAssets ?: listOf()
                filteredTaskList = tasksList
            }
            filterTasks()
            refreshScreen = false
        }
    }


    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Active Date Entry

        Row (
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ){
            Text("Active Date",
                modifier = Modifier.align(Alignment.CenterVertically)

            )
            Spacer(modifier = Modifier.width(16.dp))
            DatePickerDocked(
                identifier = "activeDate",
                selectedDate = activeDate,
                label = activeDate,
                onDateSelected = {
                    activeDate = it
                    filterTasks()
                }
            )
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Kazan Logo",
                modifier = Modifier
                    .size(75.dp)
                    .padding(10.dp)
            )
        }




        Spacer(modifier = Modifier.height(16.dp))




        // Scrollable list of active tasks
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(filteredTaskList) { task ->
                val taskColor = when {
                    task.scheduleKilometer != null && !task.taskDone -> Color.Black
                    task.scheduleKilometer != null && task.taskDone -> Color.Gray
                    task.scheduleDate?.let { date -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date) }?.before(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(activeDate)) == true && !task.taskDone -> Color.Red
                    task.scheduleDate?.let { date -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date) }?.before(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(activeDate)) == true && task.taskDone -> Color(0xFFFFA500)
                    task.scheduleDate == activeDate && !task.taskDone -> Color.Black
                    task.scheduleDate == activeDate && task.taskDone -> Color.Green
                    task.scheduleDate?.let { date -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date) }?.after(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(activeDate)) == true && task.scheduleDate?.let { date -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date) }?.before(fourDaysLater) == true && !task.taskDone -> Color(0xFF800080)
                    task.scheduleDate?.let { date -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date) }?.after(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(activeDate)) == true && task.scheduleDate?.let { date -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date) }?.before(fourDaysLater) == true && task.taskDone -> Color.Black
                    else -> Color.Black
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    TaskCard(task, navController, context, taskColor)
                    {
                        refreshScreen = true
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Button to navigate to Registering New Preventive Maintenance Tasks Screen
        FloatingActionButton(onClick = {
            navController.navigate("registerNewTask")
        },
            modifier = Modifier
                .align(Alignment.End)
                .padding(16.dp)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Register New Task")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DropDownMenu(items = assetsList.value.map { it.AssetName}.toList(), name = "Select Asset", selectedItem = selectedAssetName, 130) { selectedAsset ->
                selectedAssetName = selectedAsset
                filterTasks()
            }
            DropDownMenu(items = taskMap.values.toList(), name = "Select Task", selectedItem = selectedTaskName, 130) { selectedTask ->
                selectedTaskName = selectedTask
                filterTasks()
            }
            Button(onClick = {
                selectedAssetName = ""
                selectedTaskName = ""
                activeDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                filterTasks()

            },
                modifier = Modifier
                    .height(50.dp).align(Alignment.CenterVertically)
                ) {
                Text("Clear")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Clear filter button

    }
}

@Composable
fun TaskCard(task: Task, navController: NavController, context: Context, taskColor: Color, onCheck: (Task) -> Unit) {
    var selectedTask by remember { mutableStateOf(task) }
    var checked by remember { mutableStateOf(selectedTask.taskDone) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(25.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Column(modifier = Modifier.padding(5.dp)
            ) {
                Text(text = "Asset Name: ${selectedTask.assetName}", color = taskColor)
                Text(text = "Asset SN: ${selectedTask.assetSN}", color = taskColor)
                Text(text = "Task Name: ${selectedTask.taskName}", color = taskColor)
                Text(text = "Schedule Type: ${selectedTask.scheduleType}", color = taskColor)
                selectedTask.scheduleDate?.let {
                    Text(text = "Schedule Date: $it", color = taskColor)
                }
                selectedTask.scheduleKilometer?.let {
                    Text(text = "Schedule Kilometer: $it", color = taskColor)
                }
            }
            Checkbox(
                checked = checked,
                onCheckedChange = {
                    checked = it
                    CoroutineScope(Dispatchers.IO).launch {
                        val httpPostTask = httpposttask()
                        onCheck(task)
                        task.taskDone = checked
                        httpPostTask.postTask(task, {
                            // Success
                            //task.taskDone = isChecked

                        }, {
                            // Failure
                            //checked = !checked
                        })
                    }
                }
            )

        }



    }
}


@Composable
fun RegisteringNewPreventiveMaintenanceTasksScreen(navController: NavController, context: Context) {
    var assetName by remember { mutableStateOf("") }
    var taskName by remember { mutableStateOf("") }
    var taskStartDate by remember { mutableStateOf("2023-02-01") }
    var taskEndDate by remember { mutableStateOf("") }
    var selectedScheduleModel by remember { mutableStateOf("") }
    var scheduleParameter by remember { mutableStateOf("") }
    val scheduleModels = listOf("Daily", "Weekly", "Monthly", "Run-based")
    var selectedAssetName by remember { mutableStateOf("") }
    var selectedTaskName by remember { mutableStateOf("") }
    var taskList by remember { mutableStateOf<List<CreateTask>>(emptyList()) }
    val taskMap = mapOf(
        1 to "Get Tires Rotated and Balanced.",
        2 to "Check Engine Oil",
        3 to "Check Air Filter",
        4 to "Check Battery",
        5 to "Inspect for any damage to paint on pump",
        6 to "Inspect cord and cord placement",
        7 to "Pull each pump and reset"
    )
    val scheduleTypeMap = mapOf(
        1 to Pair("Daily", 2),
        2 to Pair("Weekly", 2),
        3 to Pair("Monthly", 2),
        4 to Pair("Run-based", 1)
    )
    var assetsList = remember { mutableStateOf<List<Asset>>(emptyList()) }
    val httpgetassets by remember { mutableStateOf(httpgetassets()) }
    var odometerReading by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val fetchedAssets = httpgetassets.getAssets()
            assetsList.value = fetchedAssets ?: listOf()
        }
    }
    var addedAssetList by remember { mutableStateOf<List<Asset>>(emptyList()) }
    var gapParameter by remember { mutableStateOf("") }
    var checkEndDate by remember { mutableStateOf(false) }

    if (checkEndDate) {

        taskEndDate = ""

        checkEndDate = false
    }


    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Register New Preventive Maintenance Task", style = MaterialTheme.typography.bodyLarge)

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            DropDownMenu(items = taskMap.values.toList(), name = "Select Task", selectedItem = selectedTaskName,330) { selectedTask ->
                selectedTaskName = selectedTask
            }
//            Button(onClick = {
//                selectedAssetName = ""
//                selectedTaskName = ""
//                taskStartDate = "2023-02-01"
//                taskEndDate = ""
//                scheduleParameter = ""
//                selectedScheduleModel = ""
//            },
//                modifier = Modifier
//                    .height(50.dp).align(Alignment.CenterVertically)
//            ) {
//                Text("Clear")
//            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Row (
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ){
            DropDownMenu(items = assetsList.value.map { it.AssetName}.toList(), name = "Select Asset", selectedItem = selectedAssetName,230) { selectedAsset ->
                selectedAssetName = selectedAsset
            }
            Spacer(modifier = Modifier.width(10.dp))
            Button(onClick = {
                addedAssetList = addedAssetList + assetsList.value.find { it.AssetName == selectedAssetName }!!
                selectedAssetName = ""
            },
                modifier = Modifier
                    .height(50.dp).align(Alignment.CenterVertically)
            ) {
                Text("Add to List")
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().height(100.dp).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(addedAssetList) { asset ->
                Text(text = asset.AssetName)
            }
        }





        DropDownMenu(items = scheduleModels.toList(), name = "Schedule Model", selectedItem = selectedScheduleModel,350) { selectedSchedule ->
            selectedScheduleModel = selectedSchedule
        }



        Spacer(modifier = Modifier.height(16.dp))


        when (selectedScheduleModel) {
            "Daily" -> {

                Row (
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ){
                    Text("Start Date",
                        modifier = Modifier.align(Alignment.CenterVertically)

                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    DatePickerDocked(
                        identifier = "startDate",
                        selectedDate = taskStartDate,
                        label = taskStartDate,
                        onDateSelected = {
                            taskStartDate = it
                        }
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))

                Row (
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ){
                    Text("End Date",
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    DatePickerDocked(
                        identifier = "endDate",
                        selectedDate = taskEndDate,
                        label = taskEndDate,
                        onDateSelected = {
                            var previousEndDate = taskEndDate
                            taskEndDate = it
                            if (taskEndDate >= taskStartDate) {
                            } else {
                                Toast.makeText(context, "End Date must be after Start Date", Toast.LENGTH_SHORT).show()
                                taskEndDate = previousEndDate
                                // Handle the case where end date is before start date
                            }

                        }
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))


                OutlinedTextField(
                    value = gapParameter,
                    onValueChange = {
                        if (it.toIntOrNull() != null) {
                            gapParameter = it
                        }
                    },
                    label = { Text("Gap in Days") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            "Weekly" -> {
                Row (
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ){
                    Text("Start Date",
                        modifier = Modifier.align(Alignment.CenterVertically)

                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    DatePickerDocked(
                        identifier = "startDate",
                        selectedDate = taskStartDate,
                        label = taskStartDate,
                        onDateSelected = {
                            taskStartDate = it
                        }
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))

                Row (
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ){
                    Text("End Date",
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    DatePickerDocked(
                        identifier = "endDate",
                        selectedDate = taskEndDate,
                        label = taskEndDate,
                        onDateSelected = {
                            var previousEndDate = taskEndDate
                            taskEndDate = it
                            if (taskEndDate >= taskStartDate) {
                            } else {
                                Toast.makeText(context, "End Date must be after Start Date", Toast.LENGTH_SHORT).show()
                                taskEndDate = previousEndDate
                                // Handle the case where end date is before start date
                            }

                        }
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                OutlinedTextField(
                    value = scheduleParameter,
                    onValueChange = { scheduleParameter = it },
                    label = { Text("Day of Week") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(2.dp))

                OutlinedTextField(
                    value = gapParameter,
                    onValueChange = {
                        if (it.toIntOrNull() != null) {
                            gapParameter = it
                        }
                    },
                    label = { Text("Gap in Weeks") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            "Monthly" -> {
                Row (
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ){
                    Text("Start Date",
                        modifier = Modifier.align(Alignment.CenterVertically)

                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    DatePickerDocked(
                        identifier = "startDate",
                        selectedDate = taskStartDate,
                        label = taskStartDate,
                        onDateSelected = {
                            taskStartDate = it
                        }
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))

                Row (
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ){
                    Text("End Date",
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    DatePickerDocked(
                        identifier = "endDate",
                        selectedDate = taskEndDate,
                        label = taskEndDate,
                        onDateSelected = {
                            var previousEndDate = taskEndDate
                            taskEndDate = it
                            if (taskEndDate >= taskStartDate) {
                            } else {
                                Toast.makeText(context, "End Date must be after Start Date", Toast.LENGTH_SHORT).show()
                                taskEndDate = previousEndDate
                                // Handle the case where end date is before start date
                            }

                        }
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                OutlinedTextField(
                    value = scheduleParameter,
                    onValueChange = { scheduleParameter = it },
                    label = { Text("Day of Month ") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(2.dp))

                OutlinedTextField(
                    value = gapParameter,
                    onValueChange = {
                        if (it.toIntOrNull() != null) {
                            gapParameter = it
                        }
                    },
                    label = { Text("Gap in Months") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            "Run-based" -> {
                OutlinedTextField(
                    value = scheduleParameter,
                    onValueChange = {
                        if (it.toIntOrNull() != null) {
                            scheduleParameter = it
                        }
                    },
                    label = { Text("Start Range") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = odometerReading,
                    onValueChange = {
                        if (it.toIntOrNull() != null) {
                            odometerReading = it
                        }
                    },
                    label = { Text("End Range") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = gapParameter,
                    onValueChange = {
                        if (it.toIntOrNull() != null) {
                            gapParameter = it
                        }
                    },
                    label = { Text("Gap") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            "" -> {}
        }

        Spacer(modifier = Modifier.height(16.dp))
        Box (modifier = Modifier.weight(1f).padding(16.dp)){
            Row (
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ){
                LazyColumn (
                    modifier = Modifier.fillMaxSize()
                ){

                    if (taskList.isNotEmpty()) {

                        items(taskList) { task ->
                            Text(text = assetsList.value.find { it.ID == task.assetID }?.AssetName ?: "Asset Name Not Found")
                        }
                    } }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {

                }) {
                    Text("Add Task")
                }
            }

        }

        Spacer(modifier = Modifier.height(16.dp))
        Toast.LENGTH_SHORT
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {


            Button(onClick = { navController.navigate("taskScreen") }) {
                Text("Cancel")
            }

            Button(onClick = {
                val newTasks = mutableListOf<CreateTask>()
                val scheduleType = scheduleTypeMap.entries.first { it.value.first == selectedScheduleModel }.value.second
                if (addedAssetList.isEmpty()) {
                    Toast.makeText(context, "No assets selected", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                try {
                    addedAssetList.forEach { asset ->
                        val assetID = asset.ID
                        val taskID = taskMap.filterValues { it == selectedTaskName }.keys.first()

                        when (selectedScheduleModel) {
                            "Daily" -> {
                                val intervalInDays = gapParameter.toIntOrNull() ?: 0
                                val startDate =
                                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(
                                        taskStartDate
                                    )
                                val endDate =
                                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(
                                        taskEndDate
                                    )
                                val calendar = Calendar.getInstance()
                                calendar.time = startDate

                                while (calendar.time.before(endDate) || calendar.time == endDate) {
                                    val scheduleDate =
                                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
                                            calendar.time
                                        )


                                    newTasks.add(
                                        CreateTask(
                                            assetID = assetID,
                                            taskId = taskID,
                                            scheduleType = scheduleType,
                                            scheduleDate = scheduleDate,
                                            scheduleKilometer = null,
                                            taskDone = false,
                                            odometerReading = null
                                        )
                                    )
                                    Log.d("calendar.time", calendar.time.toString())
                                    var checkDate = calendar.time.toString()
                                    calendar.add(Calendar.DAY_OF_YEAR, intervalInDays)
                                }
                            }

                            "Weekly" -> {
                                var dayOfWeek = scheduleParameter.toIntOrNull() ?: 0
                                val intervalInWeeks = gapParameter.toIntOrNull() ?: 0
                                val startDate =
                                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(
                                        taskStartDate
                                    )
                                val endDate =
                                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(
                                        taskEndDate
                                    )
                                val calendar = Calendar.getInstance()
                                calendar.time = startDate

                                while (calendar.time.before(endDate) || calendar.time == endDate) {
                                    calendar.set(Calendar.DAY_OF_WEEK, dayOfWeek + 1)
                                    if (calendar.time.before(startDate)) {
                                        calendar.add(Calendar.WEEK_OF_YEAR, 1)
                                    }
                                    val scheduleDate =
                                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
                                            calendar.time
                                        )
                                    newTasks.add(
                                        CreateTask(
                                            assetID = assetID,
                                            taskId = taskID,
                                            scheduleType = scheduleType,
                                            scheduleDate = scheduleDate,
                                            scheduleKilometer = null,
                                            taskDone = false,
                                            odometerReading = null
                                        )
                                    )
                                    calendar.add(Calendar.WEEK_OF_YEAR, intervalInWeeks)
                                }
                            }

                            "Monthly" -> {
                                var dayOfMonth = scheduleParameter.toIntOrNull() ?: 0
                                val intervalInMonths = gapParameter.toIntOrNull() ?: 0
                                val startDate =
                                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(
                                        taskStartDate
                                    )
                                val endDate =
                                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(
                                        taskEndDate
                                    )
                                val calendar = Calendar.getInstance()
                                calendar.time = startDate

                                while (calendar.time.before(endDate) || calendar.time == endDate) {
                                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                    val scheduleDate =
                                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
                                            calendar.time
                                        )
                                    newTasks.add(
                                        CreateTask(
                                            assetID = assetID,
                                            taskId = taskID,
                                            scheduleType = scheduleType,
                                            scheduleDate = scheduleDate,
                                            scheduleKilometer = null,
                                            taskDone = false,
                                            odometerReading = null
                                        )
                                    )
                                    calendar.add(Calendar.MONTH, intervalInMonths)
                                }
                            }

                            "Run-based" -> {
                                var start = scheduleParameter.toIntOrNull() ?: 0
                                val end = odometerReading.toIntOrNull() ?: 0

                                while (start <= end) {
                                    newTasks.add(
                                        CreateTask(
                                            assetID = assetID,
                                            taskId = taskID,
                                            scheduleType = scheduleType,
                                            scheduleDate = null,
                                            scheduleKilometer = start,
                                            taskDone = false,
                                            odometerReading = null
                                        )
                                    )
                                    start += gapParameter.toIntOrNull() ?: 0
                                }


                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                // Submit the tasks
                CoroutineScope(Dispatchers.IO).launch {
                    val httpcreatetasks = httpcreatetasks()
                    httpcreatetasks.postTask(newTasks, {
                        Toast.makeText(context, "Tasks added successfully", Toast.LENGTH_SHORT).show()
                        navController.navigate("taskScreen")
                    }, {
                        Toast.makeText(context, "Failed to add tasks", Toast.LENGTH_SHORT).show()
                    })
                }
            }) {
                Text("Submit")
            }

        }

    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDocked(
    identifier: String,
    selectedDate: String,
    label: String,
    onDateSelected: (String) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    val selectedDate = datePickerState.selectedDateMillis?.let {
        convertMillisToDate(it)
    } ?: ""
    var selectedDateText by remember { mutableStateOf(selectedDate) }

    Box(
        modifier = Modifier
            .width(200.dp)
            .padding(5.dp)
    ) {
        OutlinedTextField(
            value = selectedDateText,
            onValueChange = { },
            label = { Text(label) },
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { showDatePicker = !showDatePicker }) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Select date"
                    )
                }
            },
            modifier = Modifier
                .width(200.dp)
                .height(64.dp)
        )

        if (showDatePicker) {
            Popup(
                onDismissRequest = { showDatePicker = false },
                alignment = Alignment.TopStart,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = 64.dp)
                        .shadow(elevation = 4.dp)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp)
                ) {
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    val selectedDateMillis = datePickerState.selectedDateMillis
                                    if (selectedDateMillis != null) {
                                        onDateSelected(convertMillisToDate(selectedDateMillis))
                                    }
                                    showDatePicker = false
                                }
                            ) {
                                Text("OK")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) {
                                Text("Cancel")
                            }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }

            }
        }
    }
}

fun convertMillisToDate(millis: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return formatter.format(Date(millis))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropDownMenu(items: List<String>, name: String, selectedItem: String,width: Int, onItemSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier
            .width(width.dp)
            .padding(5.dp) // Adjust the width as needed
    ) {
        TextField(
            value = selectedItem,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(),
            label = { Text(name) }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(text = item) },
                    onClick = {
                        expanded = false
                        onItemSelected(item)
                    }
                )
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    KazanMaintenenceAppTheme {
        //Greeting("Android")
    }
}