package com.dark.darknama.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.dark.darknama.VideoPlayerActivity

object DownloadUtils {
    fun openUrl(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            // Show error message when URL cannot be opened
            android.widget.Toast.makeText(context, "Unable to open URL: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Video URL", text)
        clipboard.setPrimaryClip(clip)
        
        // Show a toast or snackbar to indicate success
        android.widget.Toast.makeText(context, "Link copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
    }

    fun openWithADM(context: Context, url: String) {
        try {
            // Try multiple common ADM package names and intent actions
            val packages = arrayOf(
                "com.dv.adm",
                "com.dv.adm.pay",
                "com.dv.get",
                "com.dv.adm.old"
            )
            
            var success = false
            for (pkg in packages) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    intent.`package` = pkg
                    context.startActivity(intent)
                    success = true
                    break
                } catch (e: Exception) {
                    // Try next package
                }
            }
            
            // If none of the specific packages work, try the general approach
            if (!success) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("adm://$url"))
                    context.startActivity(intent)
                    success = true
                } catch (e: Exception) {
                    // Continue to fallback
                }
            }
            
            // If all else fails, show error message
            if (!success) {
                android.widget.Toast.makeText(context, "ADM is not installed on this device", android.widget.Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            // If ADM is not installed, show error message
            android.widget.Toast.makeText(context, "ADM is not installed on this device", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    fun openWithVLC(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(Uri.parse(url), "video/*")
            intent.setPackage("org.videolan.vlc")
            context.startActivity(intent)
        } catch (e: Exception) {
            // If VLC is not installed, show a message
            android.widget.Toast.makeText(context, "VLC Player not installed", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    fun openWithMXPlayer(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(Uri.parse(url), "video/*")
            intent.setPackage("com.mxtech.videoplayer.ad") // MX Player Free
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                // Try pro version
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(Uri.parse(url), "video/*")
                intent.setPackage("com.mxtech.videoplayer.pro") // MX Player Pro
                context.startActivity(intent)
            } catch (e2: Exception) {
                // If MX Player is not installed, show a message
                android.widget.Toast.makeText(context, "MX Player not installed", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    fun openWithKMPlayer(context: Context, url: String) {
        // Try multiple KM Player package names
        val packages = arrayOf(
            "com.kmplayer", // KM Player
            "com.kmplayerpro",
            "com.kmplayer.google",
            "com.kmplayer.d"
        )
        
        var success = false
        for (pkg in packages) {
            try {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(Uri.parse(url), "video/*")
                intent.setPackage(pkg)
                context.startActivity(intent)
                success = true
                break
            } catch (e: Exception) {
                // Try next package
            }
        }
        
        // If none of the specific packages work, try a general approach
        if (!success) {
            try {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(Uri.parse(url), "video/*")
                context.startActivity(intent)
                success = true
            } catch (e: Exception) {
                // If all else fails, show error message
                android.widget.Toast.makeText(context, "KM Player not installed", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
}