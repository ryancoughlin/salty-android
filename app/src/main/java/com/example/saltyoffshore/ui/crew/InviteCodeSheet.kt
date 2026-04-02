package com.example.saltyoffshore.ui.crew

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saltyoffshore.data.waypoint.Crew
import com.example.saltyoffshore.ui.theme.SaltyColors
import com.example.saltyoffshore.ui.theme.SaltyType
import com.example.saltyoffshore.ui.theme.Spacing
import com.example.saltyoffshore.ui.theme.SplineSansMono
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteCodeSheet(
    crew: Crew,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(),
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var showCopied by remember { mutableStateOf(false) }

    LaunchedEffect(showCopied) {
        if (showCopied) {
            delay(2000)
            showCopied = false
        }
    }

    val formattedCode = crew.inviteCode.take(3) + " \u2013 " + crew.inviteCode.drop(3)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SaltyColors.overlay,
        dragHandle = {
            Box(
                Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
            )
        },
    ) {
        Column(
            modifier = Modifier.padding(Spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = crew.name,
                style = SaltyType.heading,
                color = SaltyColors.textPrimary,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Share this code to invite members",
                style = SaltyType.bodySmall,
                color = SaltyColors.textSecondary,
            )

            Spacer(Modifier.height(32.dp))

            Text(
                text = formattedCode,
                style = TextStyle(
                    fontFamily = SplineSansMono,
                    fontWeight = FontWeight.Normal,
                    fontSize = 28.sp,
                    letterSpacing = 4.sp,
                ),
                color = SaltyColors.textPrimary,
            )

            Spacer(Modifier.height(32.dp))

            Row {
                FilledTonalButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(crew.inviteCode))
                        showCopied = true
                    },
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text(if (showCopied) "Copied!" else "Copy Code")
                }

                Spacer(Modifier.width(12.dp))

                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Join my crew on Salty! Code: ${crew.inviteCode}",
                            )
                        }
                        context.startActivity(
                            Intent.createChooser(intent, "Share Invite Code"),
                        )
                    },
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text("Share")
                }
            }

            Spacer(Modifier.height(Spacing.large))
        }
    }
}
