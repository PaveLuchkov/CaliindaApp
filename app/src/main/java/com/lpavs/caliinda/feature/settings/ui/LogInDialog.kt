package com.lpavs.caliinda.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lpavs.caliinda.R

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LogInScreenDialog(
    onDismissRequest: () -> Unit,
    onSignInClick: () -> Unit,
) {


  Dialog(
      onDismissRequest = { onDismissRequest() }, // ,
      properties =
          DialogProperties(
              dismissOnBackPress = true,
              dismissOnClickOutside = true,
              usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.9f).wrapContentHeight(),
            shape = RoundedCornerShape(25.dp),
            color = colorScheme.primaryContainer,
            tonalElevation = 8.dp) {
              val onDialog = colorScheme.onPrimaryContainer
              Box(modifier = Modifier.fillMaxWidth()) {
                val shape1 = MaterialShapes.Flower.toShape()
                val shape2 = MaterialShapes.Cookie12Sided.toShape()
                Box(
                    modifier =
                        Modifier.align(Alignment.BottomEnd)
                            .size(250.dp)
                            .rotate(75f)
                            .offset(y = (-50).dp, x = 50.dp)
                            .clip(shape1)
                            .border(
                                width = 2.dp, color = onDialog.copy(alpha = 0.2f), shape = shape1)
                            .background(onDialog.copy(alpha = 0f)))
                Box(
                    modifier =
                        Modifier.align(Alignment.TopStart)
                            .size(250.dp)
                            .offset(y = (-100).dp, x = (-80).dp)
                            .rotate(30f)
                            .clip(shape2)
                            .border(
                                width = 2.dp, color = onDialog.copy(alpha = 0.2f), shape = shape2)
                            .background(onDialog.copy(alpha = 0f)))
                Column(
                    modifier =
                        Modifier.padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 12.dp),
                    horizontalAlignment = Alignment.Start) {
                      Text(
                          text = stringResource(R.string.app_name),
                          style = typography.displaySmall.copy(fontWeight = FontWeight.SemiBold),
                          color = onDialog)
                      Spacer(modifier = Modifier.height(2.dp))
                      Text(
                          text = stringResource(R.string.log_to_continue),
                          style = typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                          color = onDialog)
                      Spacer(modifier = Modifier.height(50.dp))
                      Row(
                          modifier = Modifier.fillMaxWidth(),
                          verticalAlignment = Alignment.Bottom,
                          horizontalArrangement = Arrangement.Center) {
                            val expandedSize = ButtonDefaults.MediumContainerHeight
                            Button(
                                onClick = {
                                  onSignInClick()
                                },
                                colors =
                                    ButtonColors(
                                        contentColor = colorScheme.onTertiary,
                                        containerColor = colorScheme.tertiary,
                                        disabledContentColor = colorScheme.inverseSurface,
                                        disabledContainerColor = colorScheme.inverseSurface),
                                modifier = Modifier.heightIn(expandedSize),
                                contentPadding = ButtonDefaults.contentPaddingFor(expandedSize)) {
                                  Text(
                                      text = stringResource(R.string.google_login),
                                      style = ButtonDefaults.textStyleFor(expandedSize))
                                }
                          }
                    }
              }
            }
      }
}

@Preview(showBackground = true) @Composable fun IconAndTextButtonGroupScreenPreview() {}
