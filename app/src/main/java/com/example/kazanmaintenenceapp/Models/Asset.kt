package com.example.kazaninventoryapp.Models
import kotlinx.serialization.Serializable


@Serializable
class Asset
    (
    val ID: Int,
    val AssetSN: String,
    val AssetName: String,
    val DepartmentID: Int,
    val EmployeeID: Int,
    val AssetGroupID: Int,
    val Description: String,
    val WarrantyDate: String,
    val readDate : String?,
    val odometerAmount : String?,
)