package com.lpavs.caliinda.feature.settings.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.LeakRemove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.ui.theme.CaliindaTheme
import com.lpavs.caliinda.core.ui.theme.cuid

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AboutScreen(onNavigateBack: () -> Unit, title: String) {
  val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

  Scaffold(
      snackbarHost = { SnackbarHost(snackbarHostState) },
      topBar = {
        TopAppBar(
            title = { Text(title) },
            navigationIcon = {
              IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back))
              }
            })
      }) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .padding(paddingValues)
                    .padding(16.dp) // Дополнительные отступы для контента
                    .fillMaxWidth() // Занимаем всю ширину
            ) {
            SettingsItem(
                modifier = Modifier.padding(vertical = cuid.ItemVerticalPadding),
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.doc),
                        tint = colorScheme.onPrimaryContainer,
                        contentDescription = "About")
                },
                title = stringResource(R.string.privacy_policy),
                onClick = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        "https://www.lpavs.com/caliinda/privacy-policy".toUri()
                    )
                    context.startActivity(intent)
                },
                shape = MaterialShapes.Bun.toShape())
            SettingsItem(
                modifier = Modifier.padding(vertical = cuid.ItemVerticalPadding),
                icon = {
                    Icon(
                        Icons.Rounded.DeleteSweep,
                        tint = colorScheme.onPrimaryContainer,
                        contentDescription = stringResource(R.string.terms))
                },
                title = stringResource(R.string.delete_data),
                onClick = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        "https://www.lpavs.com/caliinda/delete-data".toUri()
                    )
                    context.startActivity(intent)
                },
                shape = MaterialShapes.Burst.toShape()
            )
            SettingsItem(
                modifier = Modifier.padding(vertical = cuid.ItemVerticalPadding),
                icon = {
                    Icon(
                        Icons.Rounded.LeakRemove,
                        tint = colorScheme.onPrimaryContainer,
                        contentDescription = "revoke_Access")
                },
                title = stringResource(R.string.revoke_access), //TODO
                onClick = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        "https://myaccount.google.com/connections".toUri()
                    )
                    context.startActivity(intent)
                },
                shape = MaterialShapes.Burst.toShape()
            )
            }
      }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BoxOfInformation(
    modifier: Modifier
){
    val cornerRadius = cuid.SettingsItemCornerRadius
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(cornerRadius))
                .background(color = colorScheme.surfaceContainer)
                .height(90.dp)) {
        Text(
            text = "Text", modifier = Modifier
                .padding(16.dp)
                .align(Alignment.Center)
        )
    }
}

@Preview(showBackground = true, wallpaper = Wallpapers.RED_DOMINATED_EXAMPLE)
@Composable
fun BoxOfInformationPreview() {
    CaliindaTheme {
        BoxOfInformation(
            modifier = Modifier
        )
    }
}