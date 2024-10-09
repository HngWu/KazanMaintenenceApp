package com.example.kazanmaintenenceapp

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.kazaninventoryapp.Models.Asset
import com.example.kazaninventoryapp.httpservice.httpgetassets
import com.example.kazanmaintenenceapp.API.httpgettasks
import com.example.kazanmaintenenceapp.Models.Task
import com.example.kazanmaintenenceapp.ui.theme.KazanMaintenenceAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KazanMaintenenceAppTheme {
                val navController = rememberNavController()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "registerNewTask",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("taskScreen") {
                            TaskScreen(navController, this@MainActivity)
                        }
                        composable("registerNewTask") {
                            RegisteringNewPreventiveMaintenanceTasksScreen(navController, this@MainActivity)
                        }
                    }
                }
            }
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
    var activeDateParsed by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(activeDate)) }
    var fourDaysLater by remember { mutableStateOf(Calendar.getInstance().apply {
        time = activeDateParsed
        add(Calendar.DAY_OF_YEAR, 4)
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

    fun tryParse(dateString: String?): Date? {
        return try {
            dateString?.let {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it)
            }
        } catch (e: ParseException) {
            null // Return null if parsing fails
        }
    }

    // Function to refresh tasks based on activeDate, selectedAssetName, and selectedTaskName

    fun filterTasks() {
        activeDateParsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(activeDate)

        try {
            val runBasedList = tasksList.filter {
                it.scheduleKilometer != null
            }

            val notDoneRunBasedList = runBasedList.filter { !it.taskDone }
            val doneRunBasedList = runBasedList.filter { it.taskDone }
            filteredTaskList = tasksList.filter { task ->
                (selectedAssetName.isEmpty() || task.assetName == selectedAssetName) &&
                        (selectedTaskName.isEmpty() || task.taskName == selectedTaskName) &&
                        (task.scheduleDate == activeDate || task.scheduleKilometer == null )
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

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Active Date Entry
        DatePickerDocked(
            identifier = "activeDate",
            selectedDate = activeDate,
            label = activeDate,
            onDateSelected = {
                activeDate = it
                filterTasks()
            }
        )

        Spacer(modifier = Modifier.height(16.dp))




        // Scrollable list of active tasks
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(filteredTaskList) { task ->
                val taskColor = when {
                    task.scheduleKilometer != null && !task.taskDone -> Color.Black
                    task.scheduleKilometer != null && task.taskDone -> Color.Gray
                    task.scheduleDate?.let { date -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date) }?.before(activeDateParsed) == true && !task.taskDone -> Color.Red
                    task.scheduleDate?.let { date -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date) }?.before(activeDateParsed) == true && task.taskDone -> Color(0xFFFFA500)
                    task.scheduleDate == activeDate && !task.taskDone -> Color.Black
                    task.scheduleDate == activeDate && task.taskDone -> Color.Green
                    task.scheduleDate?.let { date -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date) }?.after(activeDateParsed) == true && task.scheduleDate?.let { date -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date) }?.before(fourDaysLater) == true && !task.taskDone -> Color(0xFF800080)
                    task.scheduleDate?.let { date -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date) }?.after(activeDateParsed) == true && task.scheduleDate?.let { date -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date) }?.before(fourDaysLater) == true && task.taskDone -> Color.Black
                    else -> Color.Black
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    TaskCard(task, navController, context, taskColor)
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
            DropDownMenu(items = assetsList.value.map { it.AssetName}.toList(), name = "Select Asset", selectedItem = selectedAssetName) { selectedAsset ->
                selectedAssetName = selectedAsset
                filterTasks()
            }
            DropDownMenu(items = taskMap.values.toList(), name = "Select Task", selectedItem = selectedTaskName) { selectedTask ->
                selectedTaskName = selectedTask
                filterTasks()
            }
            Button(onClick = {
                selectedAssetName = ""
                selectedTaskName = ""
                activeDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                filterTasks()
                filteredTaskList = tasksList
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
fun TaskCard(task: Task, navController: NavController, context: Context,taskColor: Color) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(modifier = Modifier.padding(16.dp)
            ) {
                Text(text = "Asset Name: ${task.assetName}", color = taskColor)
                Text(text = "Asset SN: ${task.assetSN}", color = taskColor)
                Text(text = "Task Name: ${task.taskName}", color = taskColor)
                Text(text = "Schedule Type: ${task.scheduleType}", color = taskColor)
                task.scheduleDate?.let {
                    Text(text = "Schedule Date: $it", color = taskColor)
                }
                task.scheduleKilometer?.let {
                    Text(text = "Schedule Kilometer: $it", color = taskColor)
                }
            }
            Checkbox(
                checked = task.taskDone,
                onCheckedChange = {
                    task.taskDone = it
                }
            )

        }



    }
}


@Composable
fun RegisteringNewPreventiveMaintenanceTasksScreen(navController: NavController, context: Context) {
    var assetName by remember { mutableStateOf("") }
    var taskName by remember { mutableStateOf("") }
    var taskStartDate by remember { mutableStateOf("") }
    var taskEndDate by remember { mutableStateOf("") }
    var selectedScheduleModel by remember { mutableStateOf("") }
    var scheduleParameter by remember { mutableStateOf("") }
    val scheduleModels = listOf("Daily", "Weekly", "Monthly", "Run-based")

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Register New Preventive Maintenance Task", style = MaterialTheme.typography.bodyLarge)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = assetName,
            onValueChange = { assetName = it },
            label = { Text("Asset Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = taskName,
            onValueChange = { taskName = it },
            label = { Text("Task Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = taskStartDate,
            onValueChange = { taskStartDate = it },
            label = { Text("Task Start Date") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = taskEndDate,
            onValueChange = { taskEndDate = it },
            label = { Text("Task End Date") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        DropDownMenu(
            items = scheduleModels,
            name = "Schedule Model",
            selectedItem = selectedScheduleModel,
            onItemSelected = { selectedScheduleModel = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedScheduleModel) {
            "Daily" -> {
                OutlinedTextField(
                    value = scheduleParameter,
                    onValueChange = { scheduleParameter = it },
                    label = { Text("Interval in Days") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            "Weekly" -> {
                OutlinedTextField(
                    value = scheduleParameter,
                    onValueChange = { scheduleParameter = it },
                    label = { Text("Day of Week and Interval in Weeks") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            "Monthly" -> {
                OutlinedTextField(
                    value = scheduleParameter,
                    onValueChange = { scheduleParameter = it },
                    label = { Text("Day of Month and Interval in Months") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            "Run-based" -> {
                OutlinedTextField(
                    value = scheduleParameter,
                    onValueChange = { scheduleParameter = it },
                    label = { Text("Odometer Reading Range") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = { navController.navigateUp() }) {
                Text("Back")
            }
            Button(onClick = { /* Handle form submission */ }) {
                Text("Submit")
            }
            Button(onClick = { navController.navigateUp() }) {
                Text("Cancel")
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

    Box(
        modifier = Modifier
            .padding(5.dp)
    ) {
        OutlinedTextField(
            value = selectedDate,
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
fun DropDownMenu(items: List<String>, name: String, selectedItem: String, onItemSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier
            .width(130.dp)
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