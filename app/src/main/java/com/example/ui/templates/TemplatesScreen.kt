package com.example.ui.templates

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.IronLogRepository
import com.example.model.Template
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(
    repository: IronLogRepository,
    onNewTemplate: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var templates by remember { mutableStateOf<List<Template>>(emptyList()) }

    LaunchedEffect(Unit) {
        repository.getTemplates().collect { templates = it }
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { Text("TEMPLATES", color = Color.White, fontWeight = FontWeight.Black, fontSize = 28.sp, letterSpacing = 2.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewTemplate,
                containerColor = Color.White,
                contentColor = Color.Black,
                shape = RoundedCornerShape(0.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "New Template")
            }
        }
    ) { padding ->
        LazyColumn(
            contentPadding = padding,
            modifier = Modifier.fillMaxSize()
        ) {
            items(templates) { template ->
                TemplateCard(
                    template = template,
                    onDelete = {
                        coroutineScope.launch { repository.deleteTemplate(template.id) }
                    }
                )
            }
        }
    }
}

@Composable
fun TemplateCard(template: Template, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        border = BorderStroke(1.dp, Color(0xFF333333))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(template.name.uppercase(), fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color.White)
                Spacer(modifier = Modifier.height(4.dp))
                Text("${template.exercises.size} EXERCISES", color = Color(0xFFA0A0A0), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color(0xFF888888))
            }
        }
    }
}
