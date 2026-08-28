package com.pira.darknama.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pira.darknama.R

private const val WEBSITE_URL = "https://darknama.pages.dev/"
private const val BANNER_IMAGE_URL = "https://raw.githubusercontent.com/DaknamaTv/DarkNama/main/darknama.jpg"

/**
 * Small circular network logo shown on the home screen.
 * Clicking it opens a small popup dialog with the DarkNama banner and website link.
 */
@Composable
fun NetworkLogoButton(
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(R.drawable.splash_logo)
            .crossfade(true)
            .build(),
        contentDescription = "DarkNama",
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable { showDialog = true },
        contentScale = ContentScale.Crop
    )

    if (showDialog) {
        WebsitePromoDialog(onDismiss = { showDialog = false })
    }
}

/**
 * Small popup dialog showing the DarkNama banner image and the website address.
 * Clicking the image or the address opens the website in the browser.
 */
@Composable
fun WebsitePromoDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val openWebsite = {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(WEBSITE_URL))
            context.startActivity(intent)
        } catch (e: Exception) {
            // Handle error if needed
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Banner image - opens website when clicked
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(BANNER_IMAGE_URL)
                        .crossfade(true)
                        .build(),
                    contentDescription = "DarkNama Website",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { openWebsite() },
                    contentScale = ContentScale.FillWidth
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Website address - opens website when clicked
                Text(
                    text = WEBSITE_URL,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clickable { openWebsite() }
                        .padding(4.dp)
                )
            }
        }
    }
}
