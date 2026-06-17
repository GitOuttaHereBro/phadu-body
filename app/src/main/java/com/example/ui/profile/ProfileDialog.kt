package com.example.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.example.data.IronLogRepository
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import coil.compose.AsyncImage
import java.io.File
import java.io.FileOutputStream
import android.net.Uri
import android.content.Context
import com.example.model.UserProfile
import com.example.model.ProgressPhoto
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Alignment

fun copyUriToLocalAndReturnPath(context: Context, uri: Uri): String? {
    try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val fileName = "progress_${System.currentTimeMillis()}.jpg"
        val file = File(context.filesDir, fileName)
        val outputStream = FileOutputStream(file)
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()
        return file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDialog(
    repository: IronLogRepository,
    onDismiss: () -> Unit,
    onSignOut: () -> Unit,
    onLoginClick: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val coroutineScope = rememberCoroutineScope()
    
    var profile by remember { mutableStateOf(UserProfile()) }
    var isLoading by remember { mutableStateOf(true) }
    
    // UI states
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Not specified") }
    var progressPhotos by remember { mutableStateOf(listOf<ProgressPhoto>()) }
    
    val context = LocalContext.current
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                coroutineScope.launch {
                    val localPath = copyUriToLocalAndReturnPath(context, uri)
                    if (localPath != null) {
                        val newPhoto = ProgressPhoto(
                            id = java.util.UUID.randomUUID().toString(),
                            date = System.currentTimeMillis(),
                            localUri = localPath,
                            weightKg = weight.toDoubleOrNull() ?: 0.0
                        )
                        progressPhotos = progressPhotos + newPhoto
                    }
                }
            }
        }
    )
    
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            repository.getUserProfile().collect { fetchedProfile ->
                if (fetchedProfile != null) {
                    profile = fetchedProfile
                    name = profile.name
                    age = if (profile.age > 0) profile.age.toString() else ""
                    weight = if (profile.weightKg > 0) profile.weightKg.toString() else ""
                    height = if (profile.heightCm > 0) profile.heightCm.toString() else ""
                    gender = profile.gender
                    progressPhotos = profile.progressPhotos
                }
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        containerColor = com.example.ui.theme.GrayDark,
        titleContentColor = Color.White,
        textContentColor = com.example.ui.theme.GrayMedium,
        shape = RoundedCornerShape(24.dp),
        title = { Text("PROFILE", fontWeight = FontWeight.Black) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
                if (currentUser != null) {
                    Text("Logged in as: ${currentUser.email}", color = Color.Gray, modifier = Modifier.padding(bottom = 16.dp))
                    
                    if (isLoading) {
                        CircularProgressIndicator(color = com.example.ui.theme.AccentGreen)
                    } else {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Name", color = Color.Gray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = com.example.ui.theme.AccentGreen,
                                unfocusedBorderColor = com.example.ui.theme.GrayMedium,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = age,
                                onValueChange = { age = it },
                                label = { Text("Age", color = Color.Gray) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = com.example.ui.theme.AccentGreen,
                                    unfocusedBorderColor = com.example.ui.theme.GrayMedium,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                singleLine = true,
                                modifier = Modifier.weight(1f).padding(bottom = 8.dp)
                            )
                            
                            OutlinedTextField(
                                value = gender,
                                onValueChange = { gender = it },
                                label = { Text("Gender", color = Color.Gray) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = com.example.ui.theme.AccentGreen,
                                    unfocusedBorderColor = com.example.ui.theme.GrayMedium,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                singleLine = true,
                                modifier = Modifier.weight(1f).padding(bottom = 8.dp)
                            )
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = weight,
                                onValueChange = { weight = it },
                                label = { Text("Weight (kg)", color = Color.Gray) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = com.example.ui.theme.AccentGreen,
                                    unfocusedBorderColor = com.example.ui.theme.GrayMedium,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                singleLine = true,
                                modifier = Modifier.weight(1f).padding(bottom = 16.dp)
                            )
                            
                            OutlinedTextField(
                                value = height,
                                onValueChange = { height = it },
                                label = { Text("Height (cm)", color = Color.Gray) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = com.example.ui.theme.AccentGreen,
                                    unfocusedBorderColor = com.example.ui.theme.GrayMedium,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                singleLine = true,
                                modifier = Modifier.weight(1f).padding(bottom = 16.dp)
                            )
                        }
                        
                        val w = weight.toDoubleOrNull() ?: 0.0
                        val h = height.toDoubleOrNull() ?: 0.0
                        if (w > 0 && h > 0) {
                            val hm = h / 100.0
                            val bmi = w / (hm * hm)
                            val bmiText = String.format("%.1f", bmi)
                            
                            val category = when {
                                bmi < 18.5 -> "Underweight"
                                bmi < 25.0 -> "Normal"
                                bmi < 30.0 -> "Overweight"
                                else -> "Obese"
                            }
                            
                            val color = when {
                                bmi < 18.5 -> Color(0xFF3498db)
                                bmi < 25.0 -> com.example.ui.theme.AccentGreen
                                bmi < 30.0 -> Color(0xFFf39c12)
                                else -> com.example.ui.theme.ErrorColor
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth().background(com.example.ui.theme.GlassDark, RoundedCornerShape(12.dp)).padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("BMI", color = Color.Gray)
                                    Text(bmiText, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                                }
                                Text(category, color = color, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            Text("PROGRESS PHOTOS", color = Color.White, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                                Icon(Icons.Default.Add, contentDescription = "Add Photo", tint = com.example.ui.theme.AccentGreen)
                            }
                        }
                        
                        if (progressPhotos.isEmpty()) {
                            Text("No progress photos added yet.", color = Color.Gray, modifier = Modifier.padding(bottom = 16.dp))
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(progressPhotos) { photo ->
                                    Box(
                                        modifier = Modifier
                                            .size(100.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(com.example.ui.theme.GrayMedium)
                                    ) {
                                        AsyncImage(
                                            model = File(photo.localUri),
                                            contentDescription = "Progress Photo",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .background(Color(0x99000000), RoundedCornerShape(topStart = 4.dp))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text("${photo.weightKg} kg", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text("You are not logged in. Sign in to save your profile and track your workouts.", modifier = Modifier.padding(vertical = 16.dp))
                }
            }
        },
        confirmButton = {
            if (currentUser != null) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val newProfile = UserProfile(
                                name = name.trim(),
                                age = age.toIntOrNull() ?: 0,
                                weightKg = weight.toDoubleOrNull() ?: 0.0,
                                heightCm = height.toDoubleOrNull() ?: 0.0,
                                gender = gender.trim(),
                                progressPhotos = progressPhotos
                            )
                            repository.saveUserProfile(newProfile)
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.AccentGreen, contentColor = Color.White)
                ) {
                    Text("SAVE")
                }
            } else {
                Button(
                    onClick = {
                        onDismiss()
                        onLoginClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.AccentGreen, contentColor = Color.White)
                ) {
                    Text("LOG IN")
                }
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (currentUser != null) {
                    TextButton(onClick = { onDismiss(); onSignOut() }) {
                        Text("SIGN OUT", color = com.example.ui.theme.ErrorColor)
                    }
                }
                TextButton(onClick = { onDismiss() }) {
                    Text("CANCEL", color = Color.White)
                }
            }
        }
    )
}
