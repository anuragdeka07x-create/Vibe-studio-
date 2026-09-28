package com.example

import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.ui.theme.VibeStudioTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      VibeStudioTheme {
        MediaEditorScreen()
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaEditorScreen() {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }

  val exoPlayer = remember(context) {
    ExoPlayer.Builder(context).build().apply {
      repeatMode = Player.REPEAT_MODE_ONE
    }
  }

  // Manage ExoPlayer lifecycle
  DisposableEffect(lifecycleOwner, exoPlayer) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
        Lifecycle.Event.ON_RESUME -> {
          if (selectedVideoUri != null) {
            exoPlayer.play()
          }
        }
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      exoPlayer.release()
    }
  }

  // File picker launcher for native Android media selection
  val filePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    uri?.let {
      selectedVideoUri = it
      exoPlayer.setMediaItem(MediaItem.fromUri(it))
      exoPlayer.prepare()
      exoPlayer.play()
    }
  }

  Scaffold(
    topBar = {
      CenterAlignedTopAppBar(
        title = {
          Text(
            text = stringResource(R.string.app_name),
            fontWeight = FontWeight.Bold
          )
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    },
    modifier = Modifier.fillMaxSize()
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Import Media Button at the top
      Button(
        onClick = { filePickerLauncher.launch("video/*") },
        modifier = Modifier
          .fillMaxWidth()
          .height(56.dp)
          .testTag("import_media_button"),
        shape = RoundedCornerShape(12.dp)
      ) {
        Icon(
          imageVector = Icons.Default.FileOpen,
          contentDescription = stringResource(R.string.import_media),
          modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = stringResource(R.string.import_media),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold
        )
      }

      // Status indicator for playback state
      if (selectedVideoUri != null) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Loop,
                contentDescription = stringResource(R.string.looping_playback),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = stringResource(R.string.looping_playback),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Medium
              )
            }
          }
        }
      }

      // ExoPlayer View area below button
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .testTag("video_player_view"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black)
      ) {
        Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.Center
        ) {
          if (selectedVideoUri != null) {
            AndroidView(
              modifier = Modifier.fillMaxSize(),
              factory = { ctx ->
                PlayerView(ctx).apply {
                  player = exoPlayer
                  useController = true
                  layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                  )
                }
              },
              update = { playerView ->
                playerView.player = exoPlayer
              }
            )
          } else {
            Column(
              modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center
            ) {
              Icon(
                imageVector = Icons.Default.VideoLibrary,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
              )
              Spacer(modifier = Modifier.height(16.dp))
              Text(
                text = stringResource(R.string.no_video_selected),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = stringResource(R.string.tap_import_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.LightGray,
                textAlign = TextAlign.Center
              )
            }
          }
        }
      }
    }
  }
}
