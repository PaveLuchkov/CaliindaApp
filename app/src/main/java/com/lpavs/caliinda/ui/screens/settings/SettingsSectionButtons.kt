package com.lpavs.caliinda.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lpavs.caliinda.ui.screens.main.MainViewModel
import com.lpavs.caliinda.ui.screens.main.components.UIDefaults.cuid
import com.lpavs.caliinda.ui.theme.Typography


@Composable
fun GoogleAccountSection(
   viewModel: MainViewModel,
   onSignInClick: () -> Unit,
){
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val email = uiState.userEmail ?: "Google account"
    val displayName = uiState.displayName ?: email.substringBefore("@")
    val cornerRadius = cuid.SettingsItemCornerRadius
    Box(
        modifier = Modifier

            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .background(color= colorScheme.surfaceContainer)
            .height(60.dp)

    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .size(40.dp)
                    .background(color= colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ){
                Icon(Icons.Rounded.AccountCircle,
                    tint=colorScheme.onPrimaryContainer,
                    contentDescription = "Account")
            }
           Spacer(modifier = Modifier.width(16.dp))
           Text(text = displayName, style = Typography.bodyLarge)
           Spacer(Modifier.weight(1f))
           Box(
               modifier = Modifier
                   .padding(6.dp)
           ){

               Box() {
                   if (!uiState.isSignedIn) {
                       Button(
                           onClick = onSignInClick, // Вызываем лямбду
                       //    enabled = !uiState.isLoading // Блокируем кнопку во время входа
                       ) {
                           Text("Log-in")
                       }
                   } else {
                       Button(
                           onClick = { viewModel.signOut() },
                           enabled = !uiState.isLoading
                       ) {
                           Text("Log Out")
                       }
                   }
               }

           }
        }

    }
}

@Composable
fun SettingsItem(
    icon: @Composable () -> Unit,
    title: String,
    onClick: () -> Unit
){
    val cornerRadius = cuid.SettingsItemCornerRadius
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .background(color= colorScheme.surfaceContainer)
            .height(60.dp)
            .clickable(onClick = onClick)

    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp)
                .clip(CircleShape)
                .size(40.dp)
                .background(color = colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Text(
            text = title,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.Center)
        )
    }
}
