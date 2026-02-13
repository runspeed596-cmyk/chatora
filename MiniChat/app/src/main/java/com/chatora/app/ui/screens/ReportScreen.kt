package com.chatora.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.chatora.app.ui.components.PrimaryButton

import androidx.compose.ui.res.stringResource
import com.chatora.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(navController: NavController) {
    var reason by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    val options = listOf(
        stringResource(R.string.report_reason_inappropriate),
        stringResource(R.string.report_reason_spam),
        stringResource(R.string.report_reason_harassment),
        stringResource(R.string.report_reason_other)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.report_user)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text(stringResource(R.string.report_reason_prompt), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))

            options.forEach { option ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (reason == option),
                        onClick = { reason = option }
                    )
                    Text(option)
                }
            }

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.report_details_label)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5
            )

            Spacer(Modifier.weight(1f))
            PrimaryButton(
                text = stringResource(R.string.submit_report),
                enabled = reason.isNotEmpty(),
                onClick = {
                    // TODO: Send to API
                    navController.popBackStack()
                }
            )
        }
    }
}
