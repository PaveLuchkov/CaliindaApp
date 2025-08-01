package com.lpavs.caliinda.ui.screens.settings

import android.net.Uri
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.lpavs.caliinda.R
import com.lpavs.caliinda.ui.screens.main.MainViewModel
import com.lpavs.caliinda.ui.screens.main.shared.cuid


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GoogleAccountSection(
    viewModel: MainViewModel,
    onSignInClick: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val email = uiState.userEmail ?: stringResource(R.string.loginplease)
  val displayName = uiState.displayName ?: email.substringBefore("@")
  val photo: Uri? = uiState.photo
  val cornerRadius = cuid.SettingsItemCornerRadius
  Box(
      modifier =
          Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(cornerRadius))
              .background(color = colorScheme.surfaceContainer)
              .height(120.dp)) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier =
                        Modifier
                            .size(80.dp)
                            .clip(if (photo == null) MaterialShapes.Circle.toShape() else MaterialShapes.Cookie7Sided.toShape())
                            .background(color = colorScheme.primaryContainer)
                            ,
                    contentAlignment = Alignment.Center
                ) {
                        if (photo == null){
                            Icon(
                                Icons.Rounded.AccountCircle,
                                tint = colorScheme.onPrimaryContainer,
                                contentDescription = stringResource(R.string.account),
                                modifier = Modifier.size(40.dp)
                            )
                        } else {
                            AsyncImage(
                            modifier = Modifier.fillMaxSize(),
                                model = photo,
                                contentDescription = stringResource(R.string.account),
                                contentScale = ContentScale.Fit,
                                placeholder = rememberVectorPainter(image = Icons.Rounded.AccountCircle),
                                error = rememberVectorPainter(image = Icons.Rounded.Error),
                            )
                        }
                }
              Spacer(modifier = Modifier.width(16.dp))
            Box(modifier = Modifier.width(130.dp)) {  Text(text = displayName, style = typography.bodyLarge, maxLines = 2)}
              Spacer(Modifier.weight(1f))
              Box(modifier = Modifier.padding(6.dp)) {
                Box {
                  if (!uiState.isSignedIn) {
                    Button(
                        onClick = onSignInClick, // Вызываем лямбду
                        //    enabled = !uiState.isLoading // Блокируем кнопку во время входа
                    ) {
                      Text(stringResource(R.string.login))
                    }
                  } else {
                    Button(onClick = { viewModel.signOut() }, enabled = !uiState.isLoading) {
                        Icon(
                            Icons.AutoMirrored.Rounded.Logout,
                            tint = colorScheme.onPrimaryContainer,
                            contentDescription = stringResource(R.string.account),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                  }
                }
              }
            }
      }
}

@Composable
fun SettingsItem(icon: @Composable () -> Unit, title: String, onClick: () -> Unit, shape: Shape) {
  val cornerRadius = cuid.SettingsItemCornerRadius
  Box(
      modifier =
          Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(cornerRadius))
              .background(color = colorScheme.surfaceContainer)
              .height(60.dp)
              .clickable(onClick = onClick)) {
        Box(
            modifier =
                Modifier.align(Alignment.CenterStart)
                    .padding(start = 16.dp)
                    .clip(shape)
                    .size(40.dp)
                    .background(color = colorScheme.primaryContainer),
            contentAlignment = Alignment.Center) {
              icon()
            }
        Text(text = title, modifier = Modifier.padding(16.dp).align(Alignment.Center))
      }
}
