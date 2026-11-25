package com.gracetv.iptv.ui.screens.support

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.gracetv.iptv.ui.theme.GraceGreen
import com.gracetv.iptv.ui.theme.GraceRed
import com.gracetv.iptv.ui.theme.GraceYellow

@Composable
fun SupportScreen() {
    val context = LocalContext.current
    val phone = stringResource(R.string.support_phone)
    val whatsapp = stringResource(R.string.support_whatsapp)
    val email = stringResource(R.string.support_email)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.SupportAgent,
                    contentDescription = null,
                    tint = GraceYellow,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(10.dp))
                Text("Grace TV Contact & Support", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Need assistance with Grace TV? Reach out directly using the channels below.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        SupportActionCard(
            title = "Phone",
            value = phone,
            buttonLabel = "Call Support",
            icon = Icons.Filled.Call,
            accent = GraceRed,
            testTag = "support_phone_button",
            onClick = {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                runCatching { context.startActivity(intent) }.onFailure {
                    Toast.makeText(context, "No phone dialer app found.", Toast.LENGTH_SHORT).show()
                }
            }
        )

        SupportActionCard(
            title = "WhatsApp",
            value = whatsapp,
            buttonLabel = "Message on WhatsApp",
            icon = Icons.Filled.Chat,
            accent = GraceGreen,
            testTag = "support_whatsapp_button",
            onClick = {
                val international = if (whatsapp.startsWith("0")) "92${whatsapp.drop(1)}" else whatsapp
                val waWeb = Uri.parse("https://wa.me/$international")
                val intent = Intent(Intent.ACTION_VIEW, waWeb)
                runCatching { context.startActivity(intent) }.onFailure {
                    Toast.makeText(context, "Unable to open WhatsApp or browser.", Toast.LENGTH_SHORT).show()
                }
            }
        )

        SupportActionCard(
            title = "Email",
            value = email,
            buttonLabel = "Send Email",
            icon = Icons.Filled.Email,
            accent = GraceYellow,
            testTag = "support_email_button",
            onClick = {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:$email")
                    putExtra(Intent.EXTRA_SUBJECT, "Grace TV IPTV Support")
                }
                runCatching { context.startActivity(intent) }.onFailure {
                    Toast.makeText(context, "No email app found.", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
private fun SupportActionCard(
    title: String,
    value: String,
    buttonLabel: String,
    icon: ImageVector,
    accent: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(28.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(value, style = MaterialTheme.typography.labelLarge, color = accent)
                }
            }
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = if (accent == GraceYellow) Color.Black else Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag(testTag)
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(buttonLabel)
            }
        }
    }
}
