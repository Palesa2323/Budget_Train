package com.example.budgettrain.feature.rewards

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardsScreen(viewModel: RewardsViewModel = viewModel()) {
    val rewards by viewModel.rewards.collectAsState()
    val badges by viewModel.badges.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Rewards & Badges") })
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Your Rewards", style = MaterialTheme.typography.titleLarge)
            }

            items(rewards.size) { index ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = rewards[index].title, style = MaterialTheme.typography.titleMedium)
                        Text(text = rewards[index].description, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            item {
                Spacer(Modifier.height(20.dp))
                Text("Your Badges", style = MaterialTheme.typography.titleLarge)
            }

            items(badges.size) { index ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = badges[index].name, style = MaterialTheme.typography.titleMedium)
                        Text(text = badges[index].criteria, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
