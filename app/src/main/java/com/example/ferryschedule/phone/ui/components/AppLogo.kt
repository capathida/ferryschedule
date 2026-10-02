package com.example.ferryschedule.phone.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ferryschedule.R

/**
 * ----------------------------------------------------------------------------
 * CENTRAL LOGO CONFIGURATION & ASSET REPLACEMENT
 * ----------------------------------------------------------------------------
 * To replace the application logo, choose either:
 *
 * 1. ZERO-CODE REPLACEMENT:
 *    Simply replace the image file at:
 *    `app/src/main/res/drawable/app_logo.png`
 *    All in-app displays and launcher adaptive icons will immediately update.
 *    Optional: run `./scripts/generate_launcher_icons.ps1` to update legacy mipmaps.
 *
 * 2. CODE-BASED REPLACEMENT:
 *    Add your image to `app/src/main/res/drawable/` and update [LOGO_RES_ID] below.
 * ----------------------------------------------------------------------------
 */
object AppLogoConfig {
    /**
     * Resource ID of the application logo.
     * Change this to point to any other drawable resource if desired.
     */
    @DrawableRes
    val LOGO_RES_ID: Int = R.drawable.app_logo

    const val BRAND_NAME: String = "Westcoast Mobility"
    const val APP_TITLE: String = "Färjetidtabell"
    const val APP_VERSION: String = "1.0.0"
    const val CONTENT_DESCRIPTION: String = "Westcoast Mobility Logo"
}

/**
 * Standard application logo composable.
 */
@Composable
fun AppLogo(
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    contentDescription: String? = AppLogoConfig.CONTENT_DESCRIPTION,
    onClick: (() -> Unit)? = null
) {
    val clickableModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }

    Image(
        painter = painterResource(id = AppLogoConfig.LOGO_RES_ID),
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = clickableModifier
    )
}

/**
 * Compact branded badge showing the app logo emblem in a styled container.
 * Ideal for TopAppBars, navigation bars, and list headers.
 */
@Composable
fun AppLogoBadge(
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    shape: Shape = RoundedCornerShape(10.dp),
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(Color(0xFF061928))
            .border(1.dp, Color(0xFF0098A6).copy(alpha = 0.5f), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = AppLogoConfig.LOGO_RES_ID),
            contentDescription = AppLogoConfig.CONTENT_DESCRIPTION,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Full branded card displaying the logo, title, and optional subtitle.
 */
@Composable
fun AppLogoCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF061928)
        ),
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = AppLogoConfig.LOGO_RES_ID),
                contentDescription = AppLogoConfig.CONTENT_DESCRIPTION,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = AppLogoConfig.BRAND_NAME,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Realtidsinformation & färjetidtabeller",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF38BDF8)
            )
        }
    }
}

/**
 * Modern Material 3 About Dialog highlighting the logo, branding, and application details.
 */
@Composable
fun AppAboutDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header row with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Om appen",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Stäng",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Prominent high-res logo showcase
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF061928))
                        .border(1.dp, Color(0xFF0098A6).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = AppLogoConfig.LOGO_RES_ID),
                        contentDescription = AppLogoConfig.CONTENT_DESCRIPTION,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = AppLogoConfig.BRAND_NAME,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${AppLogoConfig.APP_TITLE} • v${AppLogoConfig.APP_VERSION}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Smart reseguide för västkustens bilfärjor med direkta avgångstider, GPS-beräknad körtid & färjematchning, köprognoser samt live-kameror från Trafikverket.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Spacer(modifier = Modifier.height(12.dp))

                // Supported Routes Summary
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Stödda färjeleder:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "• Hönöleden (Hönö Pinan ⇄ Lilla Varholmen)\n• Björköleden (Björkö Grönevik ⇄ Lilla Varholmen)\n• Svanesundsleden (Kolhättan ⇄ Svanesund)\n• Gullmarsleden (Finnsbo ⇄ Skår)",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Stäng")
                }
            }
        }
    }
}
