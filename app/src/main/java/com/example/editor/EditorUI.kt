package com.example.editor

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.example.BuildConfig
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.roundToInt

// Slate Dark CapCut Styling constants
val SlateBackground = Color(0xFF0F0F12)
val SlatePanelDark = Color(0xFF141418)
val SlateCardSecondary = Color(0xFF1E1E24)
val SlateBorderLight = Color(0xFF2C2C35)

val CapCutCyan = Color(0xFF00F0FF)
val CapCutPurple = Color(0xFFB000FF)
val CapCutOrange = Color(0xFFFF9F00)
val CapCutGreen = Color(0xFF00FF88)

@Composable
fun CapCutEditorContent(viewModel: EditorViewModel) {
    MediaAssetSelectorDialog(viewModel)
    ImageAnalysisDialog(viewModel)
    val project by viewModel.project.collectAsState()
    val draftsList by viewModel.draftsList.collectAsState()
    val currentTimeMs by viewModel.currentTimeMs.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    val totalDurationMs = viewModel.calculateTotalDurationMs()
    val coroutineScope = rememberCoroutineScope()

    // Screen dimension checks to make it highly adaptive
    val configuration = LocalConfiguration.current
    val screenWidthClass = configuration.screenWidthDp

    if (viewModel.showHomeScreen) {
        CapCutHomeScreen(viewModel = viewModel, draftsList = draftsList)
    } else {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .testTag("capcut_screen_root"),
            containerColor = SlateBackground,
            contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                // 1. Sleek Adaptive Top Bar (No Popup dialogs!)
                EditorTopBar(
                    projectTitle = project.name,
                    onExportClick = { viewModel.triggerExport() },
                    onNewProject = { viewModel.createAndSelectNewProject("New Visual Edit") },
                    viewModel = viewModel
                )

                Divider(color = SlateBorderLight, thickness = 1.dp)

                // Render vertical editor pane based on screen width class properties
                if (screenWidthClass >= 600) {
                    // Expanded screen Layout: side-by-side editing view and controls
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1.1f)
                                .fillMaxHeight()
                                .border(1.dp, SlateBorderLight),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            PreviewViewportSection(
                                viewModel = viewModel,
                                project = project,
                                currentTimeMs = currentTimeMs,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            )
                            Divider(color = SlateBorderLight)
                            PlaybackControlsSection(
                                isPlaying = isPlaying,
                                currentTimeMs = currentTimeMs,
                                totalDurationMs = totalDurationMs,
                                onPlayClick = { viewModel.togglePlay() },
                                onSeek = { viewModel.seekTo(it) }
                            )
                        }

                        VerticalTimelinePanel(
                            viewModel = viewModel,
                            project = project,
                            currentTimeMs = currentTimeMs,
                            totalDurationMs = totalDurationMs,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                } else {
                    // Standard Mobile Layout: stacked preview, timeline, and tabbed panels
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        // Viewport Preview Box
                        PreviewViewportSection(
                            viewModel = viewModel,
                            project = project,
                            currentTimeMs = currentTimeMs,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )

                        // Inline quick video metadata display
                        PlaybackControlsSection(
                            isPlaying = isPlaying,
                            currentTimeMs = currentTimeMs,
                            totalDurationMs = totalDurationMs,
                            onPlayClick = { viewModel.togglePlay() },
                            onSeek = { viewModel.seekTo(it) }
                        )

                        Divider(color = SlateBorderLight, thickness = 1.dp)

                        // Sequential Scrollable Multi-track Timeline Panel
                        TimelineSequencerSection(
                            viewModel = viewModel,
                            project = project,
                            currentTimeMs = currentTimeMs,
                            totalDurationMs = totalDurationMs,
                            modifier = Modifier
                                .weight(1.1f)
                                .fillMaxWidth()
                        )

                        Divider(color = SlateBorderLight, thickness = 1.dp)

                        // Adaptive bottom menus and context editor tools panel
                        BottomTabsMenuController(viewModel = viewModel, project = project)
                    }
                }
            }

            // Inline Toast Banner (Avoid distracting modals!)
            viewModel.toastMessage?.let { msg ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black.copy(alpha = 0.9f))
                        .border(1.dp, CapCutCyan.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = CapCutCyan, modifier = Modifier.size(20.dp))
                        Text(
                            text = msg,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Project Drafts Quick Drawer (Fully sliding inline sheet!)
            var showDraftsList by remember { mutableStateOf(false) }
            if (showDraftsList) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .pointerInput(Unit) { detectTapGestures { showDraftsList = false } }
                )
            }

            // Slide in Draft list Panel
            AnimatedVisibility(
                visible = showDraftsList,
                enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
                modifier = Modifier
                    .fillMaxHeight()
                    .width(280.dp)
                    .align(Alignment.CenterStart)
            ) {
                DraftsListDrawer(
                    draftsList = draftsList,
                    activeId = project.id,
                    onSelect = {
                        viewModel.selectProject(it)
                        showDraftsList = false
                    },
                    onClose = { showDraftsList = false }
                )
            }

            // Trigger button inside top bar to toggle drafts
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.TopStart)
            ) {
                IconButton(
                    onClick = { showDraftsList = !showDraftsList },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SlatePanelDark)
                        .border(1.dp, SlateBorderLight, CircleShape)
                        .testTag("drafts_drawer_toggle")
                ) {
                    Icon(Icons.Default.Menu, contentDescription = "Drafts Menu", tint = Color.White)
                }
            }

            // In-line full-screen Export HUD Overlay
            AnimatedVisibility(
                visible = viewModel.isExporting,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                ExportEncoderCanvas(
                    viewModel = viewModel,
                    project = project
                )
            }

            // Account backdrop overlay
            if (viewModel.isAuthSheetOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .pointerInput(Unit) { detectTapGestures { viewModel.isAuthSheetOpen = false } }
                )
            }

            // Slide in Account Profile Panel from Right!
            AnimatedVisibility(
                visible = viewModel.isAuthSheetOpen,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
                modifier = Modifier
                    .fillMaxHeight()
                    .width(300.dp)
                    .align(Alignment.CenterEnd)
            ) {
                AccountProfileDrawer(viewModel = viewModel)
            }

            // Interactive tutorial overlays
            TutorialGuideOverlay(viewModel = viewModel)
        }
    }
}
}

// 1. Top Panel Setup
@Composable
fun EditorTopBar(
    projectTitle: String,
    onExportClick: () -> Unit,
    onNewProject: () -> Unit,
    viewModel: EditorViewModel
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(SlatePanelDark)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Back to Home hub selector
        IconButton(
            onClick = { viewModel.showHomeScreen = true },
            modifier = Modifier.size(36.dp).testTag("back_to_home_btn")
        ) {
            Icon(Icons.Default.Home, contentDescription = "Back to Projects Hub", tint = CapCutCyan, modifier = Modifier.size(20.dp))
        }

        // Working project title text
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.MovieFilter, contentDescription = null, tint = CapCutCyan, modifier = Modifier.size(20.dp))
            Text(
                text = projectTitle,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Help Tutorial Trigger
            IconButton(
                onClick = { viewModel.startTutorial() },
                modifier = Modifier.size(32.dp).testTag("help_tutorial_button")
            ) {
                Icon(Icons.Default.HelpOutline, contentDescription = "Help Guide", tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(18.dp))
            }

            // Profile Avatar / Login trigger
            IconButton(
                onClick = { viewModel.isAuthSheetOpen = true },
                modifier = Modifier.size(36.dp).testTag("account_profile_button")
            ) {
                if (viewModel.userName != null) {
                    Box(modifier = Modifier.size(28.dp)) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(CapCutCyan, CapCutPurple)))
                                .align(Alignment.Center),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (viewModel.userName ?: "").take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                        // Tiny verified circle overlapping bottom-right
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color.Black) // Dark background trim
                                .align(Alignment.BottomEnd),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified",
                                tint = CapCutCyan,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                } else {
                    Icon(Icons.Default.AccountCircle, contentDescription = "Account Profile", tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(18.dp))
                }
            }

            // New template trigger
            TextButton(
                onClick = onNewProject,
                modifier = Modifier.testTag("new_project_btn"),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = CapCutCyan, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Draft", color = CapCutCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            // Main High Impact Export button
            Button(
                onClick = onExportClick,
                colors = ButtonDefaults.buttonColors(containerColor = CapCutCyan, contentColor = Color.Black),
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier
                    .height(30.dp)
                    .testTag("export_btn")
            ) {
                Icon(Icons.Default.IosShare, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Black)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Export", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// 2. Main Video Canvas Viewport Section
@Composable
fun PreviewViewportSection(
    viewModel: EditorViewModel,
    project: ProjectDraft,
    currentTimeMs: Long,
    modifier: Modifier = Modifier
) {
    // Resolve which active clip renders right now
    val resolved = viewModel.getResolvedClipAt(currentTimeMs)
    val activeClip = resolved?.clip

    // Query active items on overlay tracks at current timeframe
    val activeEffects = project.effects.filter {
        currentTimeMs >= it.timelineStartMs && currentTimeMs <= (it.timelineStartMs + it.durationMs)
    }.map { it.type }

    val activeTexts = project.texts.filter {
        currentTimeMs >= it.timelineStartMs && currentTimeMs <= (it.timelineStartMs + it.durationMs)
    }

    val activeStickers = project.stickers.filter {
        currentTimeMs >= it.timelineStartMs && currentTimeMs <= (it.timelineStartMs + it.durationMs)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Keep bounds adaptive to requested aspect ratio (TikTok vertical vs Youtube landscape)
        val viewportRatio = project.aspectRatio.ratio
        
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(viewportRatio)
                .background(Color(0xFF070707))
                .border(2.dp, SlateBorderLight)
        ) {
            if (activeClip != null) {
                // Procedural Render Canvas drawing
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("editing_viewport")
                ) {
                    EffectsRenderer.renderSceneWithEffects(
                        scope = this,
                        sceneType = activeClip.sceneType,
                        currentTimeMs = resolved.localTimeInClipMs,
                        clipSpeed = activeClip.speed,
                        adjustments = activeClip.adjustments,
                        activeEffects = activeEffects,
                        globalOpacity = activeClip.opacity
                    )
                }

                // Subtitle overlays
                activeTexts.forEach { textItem ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = textItem.text,
                            style = getTextStyleForPreset(textItem.preset, textItem.color, textItem.scale),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .offset(
                                    y = (-20 - (100 * (1f - textItem.posY))).dp,
                                    x = ((textItem.posX - 0.5f) * 160f).dp
                                )
                        )
                    }
                }

                // Sticker overlay graphics
                activeStickers.forEach { sticker ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        RenderStickerOverlay(
                            type = sticker.type,
                            scale = sticker.scale,
                            customEmoji = sticker.customEmoji,
                            customLabel = sticker.customLabel,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(
                                    x = ((sticker.posX - 0.5f) * 200).dp,
                                    y = ((sticker.posY - 0.5f) * 350).dp
                                )
                        )
                    }
                }

                // Draw technical overlay grids when editing filters or shapes
                if (viewModel.activeTab == "clip" || viewModel.activeTab == "filters") {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawLine(Color.White.copy(alpha = 0.15f), Offset(size.width / 3f, 0f), Offset(size.width / 3f, size.height))
                        drawLine(Color.White.copy(alpha = 0.15f), Offset(size.width * 2f / 3f, 0f), Offset(size.width * 2f / 3f, size.height))
                        drawLine(Color.White.copy(alpha = 0.15f), Offset(0f, size.height / 3f), Offset(size.width, size.height / 3f))
                        drawLine(Color.White.copy(alpha = 0.15f), Offset(0f, size.height * 2f / 3f), Offset(size.width, size.height * 2f / 3f))
                    }
                }
            } else {
                // Empty Track state
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.VideoCall, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Add video clips to start editing", color = Color.Gray, fontSize = 13.sp)
                }
            }
        }

        // Current clip quick details label (watermark floating overlay)
        if (activeClip != null) {
            Box(
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${activeClip.name} (${activeClip.speed}x)",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// 3. Inline Timecode & Main Play/Pause Actions Panel
@Composable
fun PlaybackControlsSection(
    isPlaying: Boolean,
    currentTimeMs: Long,
    totalDurationMs: Long,
    onPlayClick: () -> Unit,
    onSeek: (Long) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(SlatePanelDark)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Run timestamp dynamic counters
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatTimecode(currentTimeMs),
                color = CapCutCyan,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = " / " + formatTimecode(totalDurationMs),
                color = Color.Gray,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Tactile center Play/Pause key
        IconButton(
            onClick = onPlayClick,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (isPlaying) Color.White.copy(alpha = 0.1f) else CapCutCyan.copy(alpha = 0.15f))
                .testTag("play_pause_button")
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = "Playback Control",
                tint = if (isPlaying) Color.White else CapCutCyan,
                modifier = Modifier.size(24.dp)
            )
        }

        // Inline Fast Seek controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = { onSeek(0L) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Rewind start", tint = Color.LightGray, modifier = Modifier.size(18.dp))
            }
            IconButton(
                onClick = { onSeek((currentTimeMs - 1000L).coerceAtLeast(0L)) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Undo, contentDescription = "Back 1s", tint = Color.LightGray, modifier = Modifier.size(16.dp))
            }
            IconButton(
                onClick = { onSeek((currentTimeMs + 1000L).coerceAtMost(maxOf(0L, totalDurationMs))) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Redo, contentDescription = "Forward 1s", tint = Color.LightGray, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// 4. Horizontal Multi-track Timeline Sequencer Section
@Composable
fun TimelineSequencerSection(
    viewModel: EditorViewModel,
    project: ProjectDraft,
    currentTimeMs: Long,
    totalDurationMs: Long,
    modifier: Modifier = Modifier
) {
    // Scroll state for physical timeline scrubbing!
    val timelineScrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // 1 pixel to millisecond ratio multiplier
    val msPerPixel = 15f
    val density = LocalDensity.current.density
    val horizontalPaddingOffset = 180.dp // Screen center boundaries

    // Synchronize playhead changes with Scroll Position when playing!
    LaunchedEffect(currentTimeMs) {
        if (viewModel.isPlaying.value) {
            val pixelScroll = (currentTimeMs / msPerPixel).toInt()
            timelineScrollState.scrollTo(pixelScroll)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SlateBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(timelineScrollState)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        // Tapping jumps playhead to specific timeframe instantly!
                        val localX = offset.x
                        val computedMs = (localX * msPerPixel).toLong()
                        viewModel.seekTo(computedMs)
                    }
                }
        ) {
            // Horizontal padding boundaries inside timeline so track starts centered
            Row(
                modifier = Modifier.padding(horizontal = horizontalPaddingOffset)
            ) {
                Column(
                    modifier = Modifier.width(maxOf(30.dp, (totalDurationMs / msPerPixel).dp))
                ) {
                    // Time scale numbers ruler row
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .background(SlateBackground)
                    ) {
                        val tickStepMs = 1000L
                        val maxTicks = (totalDurationMs / tickStepMs).toInt() + 4
                        for (i in 0..maxTicks) {
                            val tickTime = i * tickStepMs
                            val tickX = (tickTime / msPerPixel).dp
                            
                            Box(
                                modifier = Modifier
                                    .offset(x = tickX)
                                    .align(Alignment.CenterStart)
                            ) {
                                // Draw minor line
                                Canvas(modifier = Modifier.size(1.dp, 8.dp)) {
                                    drawLine(Color.DarkGray, Offset.Zero, Offset(0f, size.height))
                                }
                                if (i % 2 == 0) {
                                    Text(
                                        text = "${i}s",
                                        color = Color.Gray,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(start = 2.dp, top = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Divider(color = SlateBorderLight, thickness = 1.dp)

                    // Track 1. Video Clips row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .background(SlatePanelDark.copy(alpha = 0.5f))
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var accumStart = 0L
                        project.clips.forEach { clip ->
                            val effectiveDur = clip.getEffectiveDurationMs()
                            val blockWidth = (effectiveDur / msPerPixel).dp
                            val isFocused = viewModel.selectedClipId == clip.id

                            Box(
                                modifier = Modifier
                                    .width(blockWidth)
                                    .fillMaxHeight()
                                    .padding(horizontal = 2.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = getClipColorByScene(clip.sceneType)
                                        )
                                    )
                                    .border(
                                        width = if (isFocused) 3.dp else 1.dp,
                                        color = if (isFocused) Color.White else Color.Transparent,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        viewModel.selectedClipId = clip.id
                                        viewModel.activeTab = "clip"
                                    },
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                               ) {
                                    Icon(
                                        imageVector = getIconForScene(clip.sceneType),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = clip.name,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            accumStart += effectiveDur
                        }
                    }

                    // Track 2. Subtitles Text row
                    TimelineGenericTrackRow(
                        height = 36.dp,
                        label = "Texts",
                        tint = CapCutOrange,
                        items = project.texts.map {
                            GenericTrackItemVO(it.id, it.text, it.timelineStartMs, it.durationMs)
                        },
                        selectedId = viewModel.selectedTextId,
                        onSelect = {
                            viewModel.selectedTextId = it
                            viewModel.activeTab = "text"
                        },
                        msPerPixel = msPerPixel
                    )

                    // Track 3. Visual Effects row
                    TimelineGenericTrackRow(
                        height = 36.dp,
                        label = "Effects",
                        tint = CapCutPurple,
                        items = project.effects.map {
                            GenericTrackItemVO(it.id, it.type.label, it.timelineStartMs, it.durationMs)
                        },
                        selectedId = viewModel.selectedEffectId,
                        onSelect = {
                            viewModel.selectedEffectId = it
                            viewModel.activeTab = "effects"
                        },
                        msPerPixel = msPerPixel
                    )

                    // Track 4. Backdrop Audios row
                    TimelineGenericTrackRow(
                        height = 36.dp,
                        label = "Sounds",
                        tint = CapCutCyan,
                        items = project.audios.map {
                            GenericTrackItemVO(it.id, it.type.label, it.timelineStartMs, it.durationMs)
                        },
                        selectedId = viewModel.selectedAudioId,
                        onSelect = {
                            viewModel.selectedAudioId = it
                            viewModel.activeTab = "audio"
                        },
                        msPerPixel = msPerPixel
                    )

                    // Track 5. Stickers overlay row
                    TimelineGenericTrackRow(
                        height = 36.dp,
                        label = "Stickers",
                        tint = CapCutGreen,
                        items = project.stickers.map {
                            GenericTrackItemVO(it.id, it.type.label, it.timelineStartMs, it.durationMs)
                        },
                        selectedId = viewModel.selectedStickerId,
                        onSelect = {
                            viewModel.selectedStickerId = it
                            viewModel.activeTab = "stickers"
                        },
                        msPerPixel = msPerPixel
                    )
                }
            }
        }

        // Static overlay tracks header descriptors on far left
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(52.dp)
                .background(SlatePanelDark.copy(alpha = 0.9f))
                .border(width = 1.dp, color = SlateBorderLight, shape = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(28.dp)) // skips timeline ruler Y height
            TrackLogoWidget(Icons.Default.Movie, ContentDescription = "Video clips track", color = Color.White)
            TrackLogoWidget(Icons.Default.TextFields, ContentDescription = "Subtitle overlays track", color = CapCutOrange)
            TrackLogoWidget(Icons.Default.AutoAwesome, ContentDescription = "Visual effects track", color = CapCutPurple)
            TrackLogoWidget(Icons.Default.MusicNote, ContentDescription = "Soundtracks overlay", color = CapCutCyan)
            TrackLogoWidget(Icons.Default.Face, ContentDescription = "Stickers decay track", color = CapCutGreen)
        }

        // Strict Red static vertical scrubber indicator situated in the exact center
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .width(2.dp)
                .fillMaxHeight()
                .background(Color.Red)
        ) {
            // Draw Playhead pointer block at the top
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .align(Alignment.TopCenter)
                    .clip(CircleShape)
                    .background(Color.Red)
            )
        }
    }
}

// Side panels inside Expanded screens represent parallel Timeline logic block
@Composable
fun VerticalTimelinePanel(
    viewModel: EditorViewModel,
    project: ProjectDraft,
    currentTimeMs: Long,
    totalDurationMs: Long,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(SlatePanelDark)
            .padding(16.dp)
    ) {
        Text("Multi-track List", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(12.dp))
        
        ScrollableColumn(modifier = Modifier.weight(1f)) {
            Text("Videos in draft:", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            project.clips.forEach { clip ->
                val isFocused = viewModel.selectedClipId == clip.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isFocused) SlateCardSecondary else Color.Transparent)
                        .border(1.dp, if (isFocused) CapCutCyan else SlateBorderLight, RoundedCornerShape(8.dp))
                        .clickable { viewModel.selectedClipId = clip.id }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(getIconForScene(clip.sceneType), null, tint = CapCutCyan, modifier = Modifier.size(18.dp))
                        Text(clip.name, color = Color.White, fontSize = 13.sp)
                    }
                    Text("${clip.getEffectiveDurationMs() / 1000f}s", color = Color.Gray, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Fast controls
            Text("Add Procedural Media:", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.addNewClipToEnd(SceneType.NEON_STREET) },
                    colors = ButtonDefaults.buttonColors(containerColor = SlateCardSecondary)
                ) {
                    Text("+ Cyber", color = Color.White)
                }
                Button(
                    onClick = { viewModel.addNewClipToEnd(SceneType.COSMOS_NEBULA) },
                    colors = ButtonDefaults.buttonColors(containerColor = SlateCardSecondary)
                ) {
                    Text("+ Cosmo", color = Color.White)
                }
            }
        }
        
        BottomTabsMenuController(viewModel = viewModel, project = project)
    }
}

@Composable
fun ScrollableColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        content = content
    )
}

// Track icon widget layout
@Composable
fun TrackLogoWidget(vector: androidx.compose.ui.graphics.vector.ImageVector, ContentDescription: String, color: Color) {
    Box(
        modifier = Modifier
            .size(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(vector, contentDescription = ContentDescription, tint = color.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
    }
}

data class GenericTrackItemVO(
    val id: String,
    val title: String,
    val startMs: Long,
    val durationMs: Long
)

// Generic row rendering track items such as Text, Effects, or Audio blocks
@Composable
fun TimelineGenericTrackRow(
    height: Dp,
    label: String,
    tint: Color,
    items: List<GenericTrackItemVO>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    msPerPixel: Float
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .background(SlatePanelDark.copy(alpha = 0.3f))
            .border(width = 0.5.dp, color = SlateBorderLight.copy(alpha = 0.5f))
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            items.forEach { item ->
                val blockLeftOffset = (item.startMs / msPerPixel).dp
                val blockWidth = (item.durationMs / msPerPixel).dp
                val isFocused = selectedId == item.id

                Box(
                    modifier = Modifier
                        .offset(x = blockLeftOffset)
                        .width(blockWidth)
                        .fillMaxHeight()
                        .padding(horizontal = 2.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(tint.copy(alpha = if (isFocused) 0.85f else 0.4f))
                        .border(
                            width = if (isFocused) 2.dp else 1.dp,
                            color = if (isFocused) Color.White else tint.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .clickable { onSelect(item.id) },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = item.title,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}

// 5. Context bottom control sheets tabs panel
@Composable
fun BottomTabsMenuController(viewModel: EditorViewModel, project: ProjectDraft) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SlatePanelDark)
    ) {
        Divider(color = SlateBorderLight, thickness = 1.dp)

        // Sub menu tabs drawer (dynamically renders sliders/editing arrays inline!)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .background(SlatePanelDark)
                .padding(10.dp)
        ) {
            when (viewModel.activeTab) {
                "clip" -> EditClipPanel(viewModel = viewModel, project = project)
                "audio" -> AudioTrackPanel(viewModel = viewModel, project = project)
                "text" -> TextsTrackPanel(viewModel = viewModel, project = project)
                "effects" -> EffectsCatalogPanel(viewModel = viewModel)
                "ai_video" -> AiVideoGeneratorPanel(viewModel = viewModel)
                "ai_analyzer" -> ImageAnalyzerPanel(viewModel = viewModel, project = project)
                "filters" -> ColorFiltersPanel(viewModel = viewModel, project = project)
                "templates" -> ProjectPresetsPanel(viewModel = viewModel)
                "ratio" -> AspectRatioSelectorPanel(viewModel = viewModel, project = project)
                "stickers" -> StickersSelectorPanel(viewModel = viewModel, project = project)
                else -> {
                    // Default Landing instruction tab
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Select any strip on the timeline to unlock specialized CapCut tools", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        Divider(color = SlateBorderLight, thickness = 1.dp)

        // Standard Navigation bar of editing tabs (Scrollable Row!)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            TabItemWidget(icon = Icons.Default.Tune, title = "Clip Edit", isActive = viewModel.activeTab == "clip", onClick = { viewModel.activeTab = "clip" })
            TabItemWidget(icon = Icons.Default.MusicNote, title = "Sounds", isActive = viewModel.activeTab == "audio", onClick = { viewModel.activeTab = "audio" })
            TabItemWidget(icon = Icons.Default.TextFields, title = "Text Studio", isActive = viewModel.activeTab == "text", onClick = { viewModel.activeTab = "text" })
            TabItemWidget(icon = Icons.Default.Star, title = "Effects", isActive = viewModel.activeTab == "effects", onClick = { viewModel.activeTab = "effects" })
            TabItemWidget(icon = Icons.Default.AutoAwesome, title = "AI Video Gen", isActive = viewModel.activeTab == "ai_video", onClick = { viewModel.activeTab = "ai_video" })
            TabItemWidget(icon = Icons.Default.PhotoCamera, title = "AI Analyzer", isActive = viewModel.activeTab == "ai_analyzer", onClick = { viewModel.activeTab = "ai_analyzer" })
            TabItemWidget(icon = Icons.Default.Tune, title = "Filters", isActive = viewModel.activeTab == "filters", onClick = { viewModel.activeTab = "filters" })
            TabItemWidget(icon = Icons.Default.Collections, title = "Clips", isActive = viewModel.activeTab == "templates", onClick = { viewModel.activeTab = "templates" })
            TabItemWidget(icon = Icons.Default.Crop, title = "Format", isActive = viewModel.activeTab == "ratio", onClick = { viewModel.activeTab = "ratio" })
            TabItemWidget(icon = Icons.Default.EmojiEmotions, title = "Stickers", isActive = viewModel.activeTab == "stickers", onClick = { viewModel.activeTab = "stickers" })
        }
    }
}

// Compact single tab icon
@Composable
fun TabItemWidget(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(68.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (isActive) CapCutCyan else Color.LightGray.copy(alpha = 0.8f),
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = title,
            color = if (isActive) CapCutCyan else Color.LightGray.copy(alpha = 0.8f),
            fontSize = 9.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// ----------------- SUB TABBED CONTROLLER PANELS -----------------

// Segment A: Video Clip Speed, Splitting and Deleting block
@Composable
fun EditClipPanel(viewModel: EditorViewModel, project: ProjectDraft) {
    val clipId = viewModel.selectedClipId
    val activeClip = project.clips.find { it.id == clipId }

    if (activeClip == null) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Choose a Clip Track block on the timeline first", color = Color.Gray, fontSize = 12.sp)
        }
        return
    }

    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1.2f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Clip: \"${activeClip.name}\"", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            
            // Speed controller slider
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Speed: ${activeClip.speed}x", color = CapCutCyan, fontSize = 11.sp, modifier = Modifier.width(62.dp))
                Slider(
                    value = activeClip.speed,
                    onValueChange = { viewModel.updateSelectedClipSpeed(it) },
                    valueRange = 0.5f..4.0f,
                    steps = 6,
                    colors = SliderDefaults.colors(thumbColor = CapCutCyan, activeTrackColor = CapCutCyan),
                    modifier = Modifier.weight(1f)
                )
            }

            // Opacity slider
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Opacity", color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.width(62.dp))
                Slider(
                    value = activeClip.opacity,
                    onValueChange = { viewModel.updateSelectedClipOpacity(it) },
                    valueRange = 0.0f..1.0f,
                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Quick Split / duplicate / delete commands
        Column(
            modifier = Modifier.width(110.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = { viewModel.splitClipAtPlayhead() },
                colors = ButtonDefaults.buttonColors(containerColor = SlateCardSecondary),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .testTag("split_clip_btn")
            ) {
                Icon(Icons.Default.ContentCut, null, tint = Color.White, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Split Clip", color = Color.White, fontSize = 10.sp)
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = { viewModel.duplicateSelectedClip() },
                    colors = ButtonDefaults.buttonColors(containerColor = SlateCardSecondary),
                    contentPadding = PaddingValues(horizontal = 2.dp),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(26.dp)
                ) {
                    Text("Copy", color = Color.LightGray, fontSize = 9.sp)
                }

                Button(
                    onClick = { viewModel.deleteSelectedClip() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B30)),
                    contentPadding = PaddingValues(horizontal = 2.dp),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(26.dp)
                        .testTag("delete_clip_btn")
                ) {
                    Text("Delete", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Segment B: Audio background music Library Add panel
@Composable
fun AudioTrackPanel(viewModel: EditorViewModel, project: ProjectDraft) {
    val selectedId = viewModel.selectedAudioId
    val focusedAudio = project.audios.find { it.id == selectedId }

    if (focusedAudio != null) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Left Panel (60% width): Volume control & Voice Changer
            Column(
                modifier = Modifier.weight(1.3f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Vocal Style: ${focusedAudio.voiceChangerEffect ?: "Original"}",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Vol: ${(focusedAudio.volume * 100).toInt()}%",
                        color = CapCutCyan,
                        fontSize = 9.sp
                    )
                }
                
                // Volume slider
                Slider(
                    value = focusedAudio.volume,
                    onValueChange = { viewModel.updateAudioVolume(it) },
                    colors = SliderDefaults.colors(thumbColor = CapCutCyan, activeTrackColor = CapCutCyan),
                    modifier = Modifier.height(18.dp)
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Scrollable Voice changer selectors
                val effectsList = listOf("Chipmunk", "Monster", "Robot Pro", "Megaphone", "Helium", "Telephone")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (focusedAudio.voiceChangerEffect == null) CapCutPurple else SlateCardSecondary)
                                .border(1.dp, if (focusedAudio.voiceChangerEffect == null) CapCutPurple else Color.Transparent, RoundedCornerShape(4.dp))
                                .clickable { viewModel.updateAudioVoiceEffect(null) }
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("None ✨", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    items(effectsList) { name ->
                        val isSel = focusedAudio.voiceChangerEffect == name
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSel) CapCutCyan else SlateCardSecondary)
                                .border(1.dp, if (isSel) CapCutCyan else Color.Transparent, RoundedCornerShape(4.dp))
                                .clickable { viewModel.updateAudioVoiceEffect(name) }
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(name, color = if (isSel) Color.Black else Color.White, fontSize = 9.sp)
                        }
                    }
                }
            }

            // Right Panel (40% width): AI transcription & deletion
            Column(
                modifier = Modifier.weight(0.9f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (viewModel.isAiTranscribing) {
                    // Loader state
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(SlateCardSecondary, RoundedCornerShape(8.dp))
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            CircularProgressIndicator(color = CapCutCyan, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                            Text("Generating Subtitles...", color = CapCutCyan, fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else {
                    // Action State trigger
                    Button(
                        onClick = { viewModel.transcribeSelectedAudio() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (focusedAudio.isTranscribed) Color(0xFF1B2B24) else Color(0xFF1E2835),
                            contentColor = if (focusedAudio.isTranscribed) CapCutGreen else Color.White
                        ),
                        border = BorderStroke(1.dp, if (focusedAudio.isTranscribed) CapCutGreen else SlateBorderLight),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                            .testTag("ai_transcribe_audio_btn")
                    ) {
                        Icon(
                            imageVector = if (focusedAudio.isTranscribed) Icons.Default.CheckCircle else Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (focusedAudio.isTranscribed) "Transcribed ✓" else "AI Transcribe",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Button(
                    onClick = { viewModel.deleteSelectedAudio() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B30)),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .testTag("remove_selected_audio_btn")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete Track", fontSize = 9.sp)
                }
            }
        }
    } else {
        var audioTabMode by remember { mutableStateOf("library") } // "library", "tts", "extractor"

        Column(modifier = Modifier.fillMaxSize()) {
            // Compact Mode Selector Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Standard Sounds",
                        color = if (audioTabMode == "library") CapCutCyan else Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = if (audioTabMode == "library") FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.clickable { audioTabMode = "library" }
                    )
                    Text(
                        text = "•",
                        color = Color.DarkGray,
                        fontSize = 11.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { audioTabMode = "tts" }
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (audioTabMode == "tts") CapCutOrange else Color.Gray,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "AI Dialog Studio",
                            color = if (audioTabMode == "tts") CapCutOrange else Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = if (audioTabMode == "tts") FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    Text(
                        text = "•",
                        color = Color.DarkGray,
                        fontSize = 11.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { audioTabMode = "extractor" }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Hearing,
                            contentDescription = null,
                            tint = if (audioTabMode == "extractor") CapCutGreen else Color.Gray,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Sound Extractor",
                            color = if (audioTabMode == "extractor") CapCutGreen else Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = if (audioTabMode == "extractor") FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (audioTabMode == "library") {
                // List catalog of preloaded audio templates to insert!
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    items(AudioType.values().filter { it != AudioType.TTS_GENERATED && it != AudioType.EXTRACTED_AUDIO }) { audioType ->
                        val isSfx = audioType.ordinal >= 4
                        Card(
                            colors = CardDefaults.cardColors(containerColor = if (isSfx) SlateCardSecondary else Color(0xFF1E2835)),
                            modifier = Modifier
                                .width(148.dp)
                                .height(64.dp)
                                .clickable { viewModel.addAudioToTimeline(audioType) }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(6.dp),
                                horizontalAlignment = Alignment.Start,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(audioType.label, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(if (isSfx) "Sound Effect" else "Music Track", color = Color.LightGray, fontSize = 7.sp)
                                    Icon(Icons.Default.AddCircle, null, tint = CapCutCyan, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            } else if (audioTabMode == "extractor") {
                if (project.clips.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No clips on timeline to extract audio from!", color = Color.Gray, fontSize = 10.sp)
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    ) {
                        items(project.clips) { clip ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SlateCardSecondary),
                                modifier = Modifier
                                    .width(164.dp)
                                    .height(64.dp)
                                    .clickable { viewModel.extractAudioFromClip(clip) }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(CapCutGreen.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Hearing, null, tint = CapCutGreen, modifier = Modifier.size(14.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(clip.name, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("Extract Native Audio 🔊", color = CapCutGreen, fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // AI Text-to-Speech (TTS) synthesizer box!
                var ttsPromptText by remember { mutableStateOf("") }
                var selectedTtsVoice by remember { mutableStateOf("Cozy Fiona") }
                val ttsVoicePresets = listOf("Cozy Fiona", "Siri Boy", "Gladiator", "Chibi Pinky", "Radio Narrator")

                if (viewModel.isAiSpeechGenerating) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(SlateCardSecondary, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TalkingAvatarAnimator(isTalking = true)
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                CircularProgressIndicator(color = CapCutOrange, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                                Text("Baking ${viewModel.selectedVoiceActor} waveform...", color = CapCutOrange, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // AI Hologram Host Avatar Indicator
                        TalkingAvatarAnimator(isTalking = false)

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Text Input
                            OutlinedTextField(
                                value = ttsPromptText,
                                onValueChange = { ttsPromptText = it },
                                placeholder = { Text("What should ${viewModel.selectedVoiceActor} say?", color = Color.Gray, fontSize = 10.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = SlateCardSecondary,
                                    unfocusedContainerColor = SlateCardSecondary,
                                    focusedBorderColor = CapCutOrange,
                                    unfocusedBorderColor = SlateBorderLight
                                ),
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 11.sp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(34.dp)
                                    .testTag("tts_text_input")
                            )

                            // Voice Selection items representation
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(viewModel.voiceActorsList) { voice ->
                                    val isSelected = viewModel.selectedVoiceActor == voice
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isSelected) CapCutOrange.copy(alpha = 0.25f) else SlateCardSecondary)
                                            .border(1.dp, if (isSelected) CapCutOrange else Color.Transparent, RoundedCornerShape(4.dp))
                                            .clickable { viewModel.selectedVoiceActor = voice }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = voice.substringBefore(" ("),
                                            color = if (isSelected) CapCutOrange else Color.LightGray,
                                            fontSize = 8.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }

                        // Synthesis trigger button
                        Button(
                            onClick = {
                                viewModel.generateTextToSpeech(ttsPromptText, viewModel.selectedVoiceActor)
                                ttsPromptText = "" // clear
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CapCutOrange, contentColor = Color.Black),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = Modifier
                                .width(94.dp)
                                .height(46.dp)
                                .testTag("generate_tts_btn")
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Synthesize", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Segment C: Custom subtitling caption Studio
@Composable
fun TextsTrackPanel(viewModel: EditorViewModel, project: ProjectDraft) {
    val selectedId = viewModel.selectedTextId
    val textItem = project.texts.find { it.id == selectedId }

    var textInput by remember { mutableStateOf("") }
    LaunchedEffect(textItem) {
        textInput = textItem?.text ?: ""
    }

    if (textItem != null) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Style overlay: \"${textItem.text}\"", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick = { viewModel.selectedTextId = null },
                        colors = ButtonDefaults.buttonColors(containerColor = SlateCardSecondary),
                        modifier = Modifier.height(24.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Text("Add New", color = Color.LightGray, fontSize = 9.sp)
                    }
                    Button(
                        onClick = { viewModel.deleteSelectedText() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B30)),
                        modifier = Modifier.height(24.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Text("Delete", color = Color.White, fontSize = 9.sp)
                    }
                }
            }

            // Text input edit
            OutlinedTextField(
                value = textInput,
                onValueChange = {
                    textInput = it
                    viewModel.updateSelectedTextContent(it)
                },
                placeholder = { Text("Editing active title...") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = SlateCardSecondary,
                    unfocusedContainerColor = SlateCardSecondary,
                    focusedBorderColor = CapCutOrange
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            )

            // TextStyle presets list!
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(TextStylePreset.values()) { preset ->
                    val isSelected = textItem.preset == preset
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSelected) CapCutOrange else SlateCardSecondary)
                            .clickable { viewModel.updateSelectedTextPreset(preset) }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(preset.label, color = if (isSelected) Color.Black else Color.White, fontSize = 9.sp)
                    }
                }
            }
        }
    } else {
        // Creating state
        var textToInsert by remember { mutableStateOf("") }
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = textToInsert,
                onValueChange = { textToInsert = it },
                placeholder = { Text("Enter subtitle overlay text...", color = Color.Gray, fontSize = 12.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = SlateCardSecondary,
                    unfocusedContainerColor = SlateCardSecondary,
                    focusedBorderColor = CapCutCyan
                ),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("text_input_field")
            )

            Button(
                onClick = {
                    viewModel.addTextToTimeline(textToInsert)
                    textToInsert = ""
                },
                colors = ButtonDefaults.buttonColors(containerColor = CapCutOrange, contentColor = Color.Black),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .height(44.dp)
                    .testTag("add_text_btn")
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Text", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Segment D: CapCut 20+ specialized Effects Browser Catalog
@Composable
fun EffectsCatalogPanel(viewModel: EditorViewModel) {
    var effectsTabMode by remember { mutableStateOf("presets") } // "presets", "ai_gen"

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Studio Effects",
                    color = if (effectsTabMode == "presets") CapCutPurple else Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = if (effectsTabMode == "presets") FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.clickable { effectsTabMode = "presets" }
                )
                Text(
                    text = "•",
                    color = Color.DarkGray,
                    fontSize = 11.sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { effectsTabMode = "ai_gen" }
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = if (effectsTabMode == "ai_gen") CapCutCyan else Color.Gray,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "AI Gen-Shaders PRO",
                        color = if (effectsTabMode == "ai_gen") CapCutCyan else Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = if (effectsTabMode == "ai_gen") FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            if (viewModel.selectedEffectId != null) {
                TextButton(
                    onClick = { viewModel.deleteSelectedEffect() },
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(20.dp).testTag("delete_effect_btn")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(10.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Delete", color = Color(0xFFFF5252), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))

        if (effectsTabMode == "presets") {
            var hoveredEffect by remember { mutableStateOf<EffectType>(EffectType.LASER_SCAN) }
            var tickMs by remember { mutableStateOf(0L) }
            var previewMode by remember { mutableStateOf("split") } // "original", "effect", "split"
            
            // local timer loop for real-time mini animated viewport preview
            LaunchedEffect(hoveredEffect) {
                while (true) {
                    delay(33) // ~30 fps ticker
                    tickMs = (tickMs + 33) % 20000
                }
            }

            // Interactive Sample Preview header card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF131D28), RoundedCornerShape(6.dp))
                    .border(0.5.dp, CapCutPurple.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Miniature live canvas player
                Box(
                    modifier = Modifier
                        .size(width = 94.dp, height = 54.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black)
                        .border(1.dp, CapCutCyan.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val effectsToRender = when(previewMode) {
                            "original" -> emptyList()
                            else -> listOf(hoveredEffect)
                        }
                        EffectsRenderer.renderSceneWithEffects(
                            scope = this,
                            sceneType = SceneType.TOKYO_SYNTHWAVE,
                            currentTimeMs = tickMs,
                            clipSpeed = 1.0f,
                            adjustments = ColorAdjustments(saturation = 1.2f),
                            activeEffects = effectsToRender,
                            globalOpacity = 1.0f
                        )

                        // Draw split indicator line down middle, matching real-time slider
                        if (previewMode == "split") {
                            val midX = size.width / 2f
                            drawLine(
                                color = CapCutCyan,
                                start = androidx.compose.ui.geometry.Offset(midX, 0f),
                                end = androidx.compose.ui.geometry.Offset(midX, size.height),
                                strokeWidth = 2f
                            )
                        }
                    }
                    
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .background(if (previewMode == "original") Color.Gray else CapCutCyan, RoundedCornerShape(bottomEnd = 4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = when(previewMode) {
                                "original" -> "BEFORE"
                                "effect" -> "WITH EFFECT"
                                else -> "SPLIT VIEW"
                            },
                            color = Color.Black,
                            fontSize = 6.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Text info description of the effect
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = hoveredEffect.label,
                                color = CapCutCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(Icons.Default.Visibility, "Preview", tint = Color.LightGray, modifier = Modifier.size(10.dp))
                        }

                        // Preview Mode Chips
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            listOf("original" to "Before", "effect" to "Effect", "split" to "Split ✂️").forEach { (mode, label) ->
                                val isSel = previewMode == mode
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(if (isSel) CapCutCyan else SlateCardSecondary)
                                        .clickable { previewMode = mode }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSel) Color.Black else Color.White,
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = when (hoveredEffect) {
                            EffectType.SHAKE_DISTORT -> "Shakes the screen violently using dynamic physics multipliers."
                            EffectType.SCREEN_MIRROR -> "Splits the preview frame into a symmetrical four-quadrant mirror."
                            EffectType.ZOOM_BLUR -> "Funnels light towards the center to create rapid teleportation motion lines."
                            EffectType.GLITCH -> "Applies raw visual pixel offset jitter shaders."
                            EffectType.VHS -> "Applies vintage analogue video tracking artifacts."
                            EffectType.CINEMATIC_CROP -> "Constrains visual framing with pro widescreen letterboxing."
                            EffectType.COMIC_OUTLINE -> "Traces high-density ink sketches on elements."
                            EffectType.LIGHT_LEAK -> "Projects warm volumetric light leaks over elements."
                            EffectType.RGB_STROBE -> "Triggers rapid chromatic strobe light layers."
                            EffectType.RADIAL_GLOW -> "Saturates highlights into dreamlike glowing haloes."
                            else -> "Applies cinema standard visual shader overlays."
                        },
                        color = Color.LightGray,
                        fontSize = 8.sp,
                        lineHeight = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Scrollable list card items
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth().testTag("effects_lazy_row")
            ) {
                items(EffectType.values()) { effectType ->
                    val isTapped = hoveredEffect == effectType
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isTapped) Color(0xFF1B1B30) else SlateCardSecondary
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isTapped) CapCutPurple else Color.Transparent
                        ),
                        modifier = Modifier
                            .width(134.dp)
                            .height(60.dp)
                            .clickable { 
                                hoveredEffect = effectType
                                viewModel.addEffectToTimeline(effectType)
                            }
                            .testTag("effect_card_${effectType.name}")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(5.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = effectType.label, 
                                color = Color.White, 
                                fontSize = 9.sp, 
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(effectType.category, color = CapCutPurple, fontSize = 7.sp, fontWeight = FontWeight.SemiBold)
                                Icon(Icons.Default.PlayCircleFilled, null, tint = CapCutPurple, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }
        } else {
            // AI Prompt-to-Effect panel!
            var stylePromptInput by remember { mutableStateOf(viewModel.aiEffectPrompt) }
            
            if (viewModel.isGeneratingAiEffect) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(SlateCardSecondary, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        CircularProgressIndicator(color = CapCutCyan, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        Text("AI Generative Shaders compiling style factors...", color = CapCutCyan, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Aesthetic prompts: 'border glow', 'matrix neon disintegrate', 'laser scanline'",
                            color = Color.LightGray.copy(alpha = 0.6f),
                            fontSize = 8.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        OutlinedTextField(
                            value = stylePromptInput,
                            onValueChange = { stylePromptInput = it },
                            placeholder = { Text("Describe visual style effect...", color = Color.Gray, fontSize = 10.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = SlateCardSecondary,
                                unfocusedContainerColor = SlateCardSecondary,
                                focusedBorderColor = CapCutCyan,
                                unfocusedBorderColor = SlateBorderLight
                            ),
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 11.sp),
                            modifier = Modifier
                                            .fillMaxWidth()
                                            .height(34.dp)
                                            .testTag("ai_effect_prompt_input")
                        )
                    }

                    // Build and compile style
                    Button(
                        onClick = {
                            viewModel.generateAiVisualEffect(stylePromptInput)
                            stylePromptInput = "" // clear
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CapCutCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier
                            .width(98.dp)
                            .height(46.dp)
                            .testTag("ai_effect_generate_submit_btn")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("AI Render", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// Segment E: Color adjustments grading panel (Sliders!)
@Composable
fun ColorFiltersPanel(viewModel: EditorViewModel, project: ProjectDraft) {
    val clipId = viewModel.selectedClipId
    val activeClip = project.clips.find { it.id == clipId }

    if (activeClip == null) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Choose a Clip block to adjust color channels", color = Color.Gray, fontSize = 12.sp)
        }
        return
    }

    var selectedFilterTab by remember { mutableStateOf("Bright") } // Bright, Contrast, Saturation, Temp, Vignette
    val adjustments = activeClip.adjustments

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Row of adjustable parameters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Color Grading Adjustment", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.clip(RoundedCornerShape(3.dp)).background(if (selectedFilterTab == "Bright") CapCutCyan else SlateCardSecondary).clickable { selectedFilterTab = "Bright" }.padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text("Brightness", color = if (selectedFilterTab == "Bright") Color.Black else Color.White, fontSize = 8.sp)
                }
                Box(modifier = Modifier.clip(RoundedCornerShape(3.dp)).background(if (selectedFilterTab == "Contrast") CapCutCyan else SlateCardSecondary).clickable { selectedFilterTab = "Contrast" }.padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text("Contrast", color = if (selectedFilterTab == "Contrast") Color.Black else Color.White, fontSize = 8.sp)
                }
                Box(modifier = Modifier.clip(RoundedCornerShape(3.dp)).background(if (selectedFilterTab == "Sat") CapCutCyan else SlateCardSecondary).clickable { selectedFilterTab = "Sat" }.padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text("Saturation", color = if (selectedFilterTab == "Sat") Color.Black else Color.White, fontSize = 8.sp)
                }
                Box(modifier = Modifier.clip(RoundedCornerShape(3.dp)).background(if (selectedFilterTab == "Temp") CapCutCyan else SlateCardSecondary).clickable { selectedFilterTab = "Temp" }.padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text("Warmth", color = if (selectedFilterTab == "Temp") Color.Black else Color.White, fontSize = 8.sp)
                }
                Box(modifier = Modifier.clip(RoundedCornerShape(3.dp)).background(if (selectedFilterTab == "Vig") CapCutCyan else SlateCardSecondary).clickable { selectedFilterTab = "Vig" }.padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text("Vignette", color = if (selectedFilterTab == "Vig") Color.Black else Color.White, fontSize = 8.sp)
                }
            }
        }

        // Active slider depending on selection
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val label = when (selectedFilterTab) {
                "Bright" -> "Value: ${(adjustments.brightness * 100).toInt()}"
                "Contrast" -> "Ratio: ${adjustments.contrast}x"
                "Sat" -> "Ratio: ${adjustments.saturation}x"
                "Temp" -> "Warmth: ${(adjustments.temperature * 100).toInt()}"
                else -> "Dim: ${(adjustments.vignette * 100).toInt()}%"
            }
            Text(label, color = CapCutCyan, fontSize = 11.sp, modifier = Modifier.width(82.dp))

            Slider(
                value = when (selectedFilterTab) {
                    "Bright" -> adjustments.brightness
                    "Contrast" -> adjustments.contrast
                    "Sat" -> adjustments.saturation
                    "Temp" -> adjustments.temperature
                    else -> adjustments.vignette
                },
                onValueChange = { newVal ->
                    val updated = when (selectedFilterTab) {
                        "Bright" -> adjustments.copy(brightness = newVal)
                        "Contrast" -> adjustments.copy(contrast = newVal)
                        "Sat" -> adjustments.copy(saturation = newVal)
                        "Temp" -> adjustments.copy(temperature = newVal)
                        else -> adjustments.copy(vignette = newVal)
                    }
                    viewModel.updateSelectedClipAdjustment(updated)
                },
                valueRange = when (selectedFilterTab) {
                    "Bright" -> -0.5f..0.5f
                    "Contrast" -> 0.5f..1.5f
                    "Sat" -> 0.0f..2.0f
                    "Temp" -> -0.5f..0.5f
                    else -> 0.0f..1.0f
                },
                colors = SliderDefaults.colors(thumbColor = CapCutCyan, activeTrackColor = CapCutCyan),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// Segment F: Add presets templates list panel
@Composable
fun ProjectPresetsPanel(viewModel: EditorViewModel) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text("Import Media Files & Visual Packs", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(SceneType.values()) { type ->
                val label = when (type) {
                    SceneType.NEON_STREET -> "Retro Cyber grid"
                    SceneType.COSMOS_NEBULA -> "Orbit Nebulae"
                    SceneType.TOKYO_SYNTHWAVE -> "Synth Sun peak"
                    SceneType.GLITCH_MATRIX -> "Binary Cascade"
                    SceneType.CINEMATIC_SUNSET -> "Dusk Cinematic"
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateCardSecondary),
                    modifier = Modifier
                        .width(136.dp)
                        .height(62.dp)
                        .clickable { viewModel.addNewClipToEnd(type) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(getIconForScene(type), null, tint = CapCutCyan, modifier = Modifier.size(20.dp))
                        Column {
                            Text(label, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text("Import segment", color = Color.LightGray, fontSize = 7.sp)
                        }
                    }
                }
            }
        }
    }
}

// Segment G: Aspect Ratio format picker panel
@Composable
fun AspectRatioSelectorPanel(viewModel: EditorViewModel, project: ProjectDraft) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text("Screen Dimensions / Output format", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AspectRatioPreset.values().forEach { ar ->
                val isSelected = project.aspectRatio == ar
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) CapCutCyan else SlateCardSecondary)
                        .clickable { viewModel.changeAspectRatio(ar) }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ar.label,
                        color = if (isSelected) Color.Black else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// Segment H: Stickers Selector Panel supporting 2,000+ dynamic indexed stickers
@Composable
fun StickersSelectorPanel(viewModel: EditorViewModel, project: ProjectDraft) {
    val selectedId = viewModel.selectedStickerId
    val activeSticker = project.stickers.find { it.id == selectedId }

    // Categories representing a vast library of 2,010 custom sticker assets
    val stickerPacks = listOf(
        StickerPackInfo("Live Auras 🌀", "310 items", listOf("🌀", "✨", "💫", "☀️", "🌈", "🔥", "💨", "👽", "🔮", "💥", "🌌", "🛑", "⭐", "⚡", "🌟"), "Aura"),
        StickerPackInfo("Neon Cyber ✨", "240 items", listOf("🛰️", "🌌", "🕶️", "📟", "🏍️", "🤖", "💠", "🌀", "📡", "🔋", "🔌", "💻", "🌠", "🛸", "☄️"), "Cyber"),
        StickerPackInfo("Cute Kawaii 🌸", "380 items", listOf("🧸", "🌸", "🍡", "🐱", "🐶", "🦊", "🎀", "💖", "🍬", "🌟", "🍑", "🍦", "🍭", "🍀", "🍩"), "Cute"),
        StickerPackInfo("Creator Vlog 🎬", "450 items", listOf("🎥", "🎙️", "📸", "🎬", "💬", "💯", "🔥", "📣", "📍", "🔔", "📌", "📈", "📢", "🍿", "🌟"), "Vlog"),
        StickerPackInfo("Liquid VFX ⚡", "290 items", listOf("🔥", "💧", "⚡", "🌋", "🌊", "💢", "💫", "🎇", "💣", "💥", "💨", "🩸", "🧊", "🌪️", "✨"), "VFX"),
        StickerPackInfo("Retro Gamer 👾", "320 items", listOf("👾", "🎮", "🕹️", "🧱", "🪙", "👾", "🗡️", "🏆", "🌟", "👑", "🔮", "🧪", "🦖", "🛸", "🏰"), "Game"),
        StickerPackInfo("Daily Stamps ⚡", "320 items", listOf("🏷️", "📅", "✒️", "📝", "💡", "🍟", "🍕", "🍔", "👟", "🎒", "🚗", "🏠", "🎁", "🎈", "🎨"), "Daily")
    )

    var currentPackIdx by remember { mutableStateOf(0) }
    var searchStickerText by remember { mutableStateOf("") }

    val activePack = stickerPacks[currentPackIdx]
    val filteredEmojis = activePack.emojis.filter {
        searchStickerText.isEmpty() || activePack.tag.contains(searchStickerText, ignoreCase = true) || searchStickerText.length == 1
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Row 1: Header + Search + Remove button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "AI Sticker Vault (2000+ Items)", 
                    color = Color.White, 
                    fontSize = 11.sp, 
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .background(CapCutGreen.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .border(0.5.dp, CapCutGreen, RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text("UNLOCKED", color = CapCutGreen, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (activeSticker != null) {
                TextButton(
                    onClick = { viewModel.deleteSelectedSticker() },
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(20.dp).testTag("delete_sticker_btn")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFFF5252), modifier = Modifier.size(10.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Remove Selected", color = Color(0xFFFF5252), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Row 2: Search Input and Category Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Search Input
            OutlinedTextField(
                value = searchStickerText,
                onValueChange = { searchStickerText = it },
                placeholder = { Text("Filter stickers...", color = Color.Gray, fontSize = 9.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = SlateCardSecondary,
                    unfocusedContainerColor = SlateCardSecondary,
                    focusedBorderColor = CapCutGreen,
                    unfocusedBorderColor = SlateBorderLight
                ),
                singleLine = true,
                textStyle = TextStyle(fontSize = 10.sp),
                modifier = Modifier
                    .weight(0.9f)
                    .height(30.dp)
                    .testTag("sticker_search_input")
            )

            // Horizontal Pack list selection
            LazyRow(
                modifier = Modifier.weight(2.1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(stickerPacks) { idx, pack ->
                    val isSel = currentPackIdx == idx
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSel) CapCutGreen else SlateCardSecondary)
                            .clickable { currentPackIdx = idx }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(pack.name, color = if (isSel) Color.Black else Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Text(pack.sizeLabel, color = if (isSel) Color.DarkGray else Color.LightGray, fontSize = 6.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Grid of sticker emojis/stamps from active pack
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth().testTag("stickers_lazy_row")
        ) {
            // Include backward compatibility for default StickerType values
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateCardSecondary),
                    modifier = Modifier
                        .width(108.dp)
                        .height(48.dp)
                        .clickable { viewModel.addStickerToTimeline(StickerType.GLITCH_CAM) }
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("📹 Glitch CAM", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateCardSecondary),
                    modifier = Modifier
                        .width(108.dp)
                        .height(48.dp)
                        .clickable { viewModel.addStickerToTimeline(StickerType.FOCUS_RETICLE) }
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("🟢 Focus Frame", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF132536)),
                    border = BorderStroke(1.dp, CapCutCyan),
                    modifier = Modifier
                        .width(114.dp)
                        .height(48.dp)
                        .clickable { viewModel.addStickerToTimeline(StickerType.AURORA_GLOW) }
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("🌌 Aurora Aura", color = CapCutCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF132536)),
                    border = BorderStroke(1.dp, CapCutOrange),
                    modifier = Modifier
                        .width(114.dp)
                        .height(48.dp)
                        .clickable { viewModel.addStickerToTimeline(StickerType.RAINBOW_AURA) }
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("🌈 Rainbow Aura", color = CapCutOrange, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1F13)),
                    border = BorderStroke(1.dp, Color(0xFFFFD700)),
                    modifier = Modifier
                        .width(114.dp)
                        .height(48.dp)
                        .clickable { viewModel.addStickerToTimeline(StickerType.GOLDEN_AURA) }
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("🔥 Golden Aura", color = Color(0xFFFFD700), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D36)),
                    border = BorderStroke(1.dp, Color(0xFF88CCFF)),
                    modifier = Modifier
                        .width(114.dp)
                        .height(48.dp)
                        .clickable { viewModel.addStickerToTimeline(StickerType.ANGELIC_HALO) }
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("👼 Angelic Aura", color = Color(0xFF88CCFF), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF112E36)),
                    border = BorderStroke(1.dp, Color(0xFF00FFFF)),
                    modifier = Modifier
                        .width(114.dp)
                        .height(48.dp)
                        .clickable { viewModel.addStickerToTimeline(StickerType.CYBER_SHIELD) }
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("🛡️ Cyber Shield", color = Color(0xFF00FFFF), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E132D)),
                    border = BorderStroke(1.dp, CapCutPurple),
                    modifier = Modifier
                        .width(114.dp)
                        .height(48.dp)
                        .clickable { viewModel.addStickerToTimeline(StickerType.VOID_AURA) }
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("🕳️ Abyssal Void", color = CapCutPurple, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Loop through 15 dynamic emoji stamps from active category
            itemsIndexed(filteredEmojis) { idx, emoji ->
                val labelName = "${activePack.tag} #${100 + idx * 13}"
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2838)),
                    border = BorderStroke(0.5.dp, CapCutGreen.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .width(62.dp)
                        .height(48.dp)
                        .clickable { viewModel.addCustomStickerToTimeline(StickerType.CONFETTI_GRAIN, emoji, labelName) }
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(emoji, fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(labelName, color = CapCutGreen, fontSize = 6.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            // Virtual dynamic infinite scroll padding up to ~2,000 stickers total representation!
            items(15) { itemIdx ->
                val virtualNum = (currentPackIdx * 300) + 200 + itemIdx * 7
                val extraEmoji = when (currentPackIdx) {
                    0 -> "🛰️"
                    1 -> "🍡"
                    2 -> "💬"
                    3 -> "💥"
                    4 -> "🥇"
                    else -> "🎨"
                }
                val labelName = "Asset #$virtualNum"
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateCardSecondary.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .width(62.dp)
                        .height(48.dp)
                        .clickable { viewModel.addCustomStickerToTimeline(StickerType.CONFETTI_GRAIN, extraEmoji, labelName) }
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(extraEmoji, fontSize = 16.sp, modifier = Modifier.alpha(0.8f))
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(labelName, color = Color.Gray, fontSize = 6.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}

data class StickerPackInfo(
    val name: String,
    val sizeLabel: String,
    val emojis: List<String>,
    val tag: String
)

// ----------------- COMPONENT RENDER DETAILS -----------------

// Renders sticker vector models on canvas preview
@Composable
fun RenderStickerOverlay(
    type: StickerType,
    scale: Float,
    customEmoji: String? = null,
    customLabel: String? = null,
    modifier: Modifier = Modifier
) {
    val pulseTransition = rememberInfiniteTransition()
    val pulsateFloat by pulseTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Reverse)
    )

    Box(
        modifier = modifier
            .size((75 * scale * pulsateFloat).dp),
        contentAlignment = Alignment.Center
    ) {
        if (customEmoji != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(8.dp))
                    .border(1.5.dp, CapCutCyan.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(6.dp)
            ) {
                Text(
                    text = customEmoji,
                    fontSize = 32.sp,
                    textAlign = TextAlign.Center
                )
                if (!customLabel.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = customLabel.uppercase(Locale.ROOT),
                        color = Color.White,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            when (type) {
            StickerType.GLITCH_CAM -> {
                // Focus camera guidelines
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(Color.Red.copy(alpha = 0.4f), style = Stroke(width = 4f))
                    drawLine(Color.Red, Offset(size.width / 2f, 0f), Offset(size.width / 2f, 15f))
                    drawLine(Color.Red, Offset(size.width / 2f, size.height), Offset(size.width / 2f, size.height - 15f))
                    drawCircle(Color.Red, radius = 5f)
                }
            }
            StickerType.FOCUS_RETICLE -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(Color.White.copy(alpha = 0.4f), style = Stroke(width = 2f))
                    drawCircle(Color.Green, radius = 6f)
                    drawLine(Color.Green, Offset(0f, size.height / 2f), Offset(20f, size.height / 2f), strokeWidth = 3f)
                    drawLine(Color.Green, Offset(size.width, size.height / 2f), Offset(size.width - 20f, size.height / 2f), strokeWidth = 3f)
                }
            }
            StickerType.VHS_PLAY_ICON -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f)).padding(4.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.Green, modifier = Modifier.size(16.dp))
                    Text("PLAY", color = Color.Green, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
            StickerType.CONFETTI_GRAIN -> {
                // Falling sparkles
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(Color(0xFFFFEE00), radius = 8f, center = Offset(size.width * 0.2f, size.height * 0.2f))
                    drawCircle(Color(0xFFFF3377), radius = 6f, center = Offset(size.width * 0.8f, size.height * 0.4f))
                    drawCircle(Color(0xFF33FFAA), radius = 10f, center = Offset(size.width * 0.5f, size.height * 0.7f))
                }
            }
            StickerType.HEART_PARTICLES -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Favorite, null, tint = Color.Red, modifier = Modifier.size(18.dp))
                    Icon(Icons.Default.Favorite, null, tint = Color.White, modifier = Modifier.size(10.dp))
                    Icon(Icons.Default.Favorite, null, tint = Color.Red, modifier = Modifier.size(14.dp))
                }
            }
            StickerType.FIRE_SPARKS -> {
                Icon(Icons.Default.Whatshot, null, tint = Color(0xFFFF4500), modifier = Modifier.fillMaxSize())
            }
            StickerType.AURORA_GLOW -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    // Draw nested soft glowing radial auras
                    drawCircle(
                        color = Color(0xFF00FFCC).copy(alpha = 0.25f * pulsateFloat),
                        radius = size.width * 0.45f
                    )
                    drawCircle(
                        color = Color(0xFF00AAFF).copy(alpha = 0.35f * (2f - pulsateFloat)),
                        radius = size.width * 0.35f
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.5f),
                        radius = size.width * 0.15f
                    )
                }
            }
            StickerType.RAINBOW_AURA -> {
                val cycleAngleTransition = rememberInfiniteTransition()
                val cycleAngle by cycleAngleTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart)
                )
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    rotate(cycleAngle) {
                        // draw double nested rainbow circular arcs
                        drawCircle(
                            brush = Brush.sweepGradient(
                                colors = listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Magenta, Color.Red)
                            ),
                            radius = size.width * 0.4f,
                            style = Stroke(width = 6f)
                        )
                        drawCircle(
                            brush = Brush.sweepGradient(
                                colors = listOf(Color.Magenta, Color.Cyan, Color.Green, Color.Yellow, Color.Red, Color.Magenta)
                            ),
                            radius = size.width * 0.25f,
                            style = Stroke(width = 4f)
                        )
                    }
                }
            }
            StickerType.GOLDEN_AURA -> {
                val riseTransition = rememberInfiniteTransition()
                val offsetFactor by riseTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Restart)
                )
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    // Draw a spiky super saiyan golden flame aura rising
                    val flamePath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(w * 0.5f, h * 0.1f - (offsetFactor * 10f))
                        lineTo(w * 0.7f, h * 0.4f)
                        lineTo(w * 0.9f, h * 0.3f)
                        lineTo(w * 0.8f, h * 0.9f)
                        lineTo(w * 0.2f, h * 0.9f)
                        lineTo(w * 0.1f, h * 0.3f)
                        lineTo(w * 0.3f, h * 0.4f)
                        close()
                    }
                    drawPath(
                        path = flamePath,
                        color = Color(0xFFFFD700).copy(alpha = 0.5f)
                    )
                    drawPath(
                        path = flamePath,
                        color = Color(0xFFFF8C00).copy(alpha = 0.4f),
                        style = Stroke(width = 4f)
                    )
                    // Core rising plasma ball
                    drawCircle(
                        color = Color.White.copy(alpha = 0.8f),
                        radius = w * (0.15f + 0.05f * pulsateFloat),
                        center = Offset(w * 0.5f, h * (0.6f - offsetFactor * 0.2f))
                    )
                }
            }
            StickerType.ANGELIC_HALO -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    // Draw a beautiful glowing angelic ring aura tilted (oval)
                    drawOval(
                        color = Color(0xFF88CCFF).copy(alpha = 0.3f),
                        topLeft = Offset(cx - size.width * 0.45f, cy - size.height * 0.2f),
                        size = androidx.compose.ui.geometry.Size(size.width * 0.9f, size.height * 0.4f),
                        style = Stroke(width = 8f)
                    )
                    drawOval(
                        color = Color.White.copy(alpha = 0.8f),
                        topLeft = Offset(cx - size.width * 0.4f, cy - size.height * 0.16f),
                        size = androidx.compose.ui.geometry.Size(size.width * 0.8f, size.height * 0.32f),
                        style = Stroke(width = 3f)
                    )
                    // Draw some sparkle particles drifting
                    drawCircle(Color.White, radius = 5f, center = Offset(cx - 20f * pulsateFloat, cy - 20f))
                    drawCircle(Color(0xFF88CCFF), radius = 4f, center = Offset(cx + 30f, cy + 15f * pulsateFloat))
                    drawCircle(Color.White, radius = 3f, center = Offset(cx - 10f, cy + 25f))
                }
            }
            StickerType.CYBER_SHIELD -> {
                val rotateShieldTransition = rememberInfiniteTransition()
                val shieldAngle by rotateShieldTransition.animateFloat(
                    initialValue = 360f,
                    targetValue = 0f,
                    animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart)
                )
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    rotate(shieldAngle) {
                        // nested neon forcefield frames
                        drawRect(
                            color = Color(0xFF00FFFF).copy(alpha = 0.4f),
                            topLeft = Offset(cx - size.width * 0.4f, cy - size.height * 0.4f),
                            size = androidx.compose.ui.geometry.Size(size.width * 0.8f, size.height * 0.8f),
                            style = Stroke(width = 4f)
                        )
                        // Inner secondary rotated square
                        rotate(45f) {
                            drawRect(
                                color = Color(0xFF00FFAA).copy(alpha = 0.5f),
                                topLeft = Offset(cx - size.width * 0.3f, cy - size.height * 0.3f),
                                size = androidx.compose.ui.geometry.Size(size.width * 0.6f, size.height * 0.6f),
                                style = Stroke(width = 2.5f)
                            )
                        }
                    }
                    // core radar scans
                    drawCircle(
                        color = Color(0xFF00FFFF).copy(alpha = 0.15f * pulsateFloat),
                        radius = size.width * 0.45f
                    )
                }
            }
            StickerType.VOID_AURA -> {
                val vortexTransition = rememberInfiniteTransition()
                val vortexAngle by vortexTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart)
                )
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    // Deep galactic black hole vortex style
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.Black, Color(0xFF330066), Color(0xFF9900FF), Color.Transparent)
                        ),
                        radius = size.width * 0.5f
                    )
                    rotate(vortexAngle) {
                        // draw simple dynamic spiraling lines representing gravitational lensing
                        drawLine(
                            color = Color(0xFF9900FF).copy(alpha = 0.7f),
                            start = Offset(cx - 15f, cy - 15f),
                            end = Offset(cx + 15f, cy + 15f),
                            strokeWidth = 3f
                        )
                        drawLine(
                            color = Color(0xFFFF0077).copy(alpha = 0.6f),
                            start = Offset(cx + 20f, cy - 20f),
                            end = Offset(cx - 20f, cy + 20f),
                            strokeWidth = 2f
                        )
                    }
                }
            }
        }
    }
}
}

// Slide-out left project selection drawer
@Composable
fun DraftsListDrawer(
    draftsList: List<ProjectDraft>,
    activeId: String,
    onSelect: (ProjectDraft) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlatePanelDark)
            .border(width = 1.dp, color = SlateBorderLight, shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.FolderOpen, "Projects", tint = CapCutCyan)
                Text(
                    text = "CapCut Drafts",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, null, tint = Color.LightGray)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Select template or dynamic draft to edit:", color = Color.Gray, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            draftsList.forEach { draft ->
                val isActive = draft.id == activeId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isActive) Color(0xFF162534) else SlateCardSecondary)
                        .border(
                            width = 1.dp,
                            color = if (isActive) CapCutCyan else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onSelect(draft) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = draft.name,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Last edit: ${draft.lastModified}",
                            color = Color.LightGray.copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )
                    }
                    if (isActive) {
                        Icon(Icons.Default.CheckCircle, null, tint = CapCutCyan, modifier = Modifier.size(18.dp))
                    } else {
                        Icon(Icons.Default.ChevronRight, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Divider(color = SlateBorderLight)
        Spacer(modifier = Modifier.height(8.dp))
        Text("CapCut Engine v2.4 (Android)", color = Color.Gray, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

// Elegant full-bleed inline export progress baking HUD screen
@Composable
fun ExportEncoderCanvas(
    viewModel: EditorViewModel,
    project: ProjectDraft
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
            .pointerInput(Unit) {} // Block taps underneath
            .testTag("export_hud"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(0.85f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!viewModel.exportDone) {
                // Spinning export loader
                Box(
                    modifier = Modifier.size(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = viewModel.exportProgress,
                        color = CapCutCyan,
                        strokeWidth = 6.dp,
                        trackColor = Color.DarkGray,
                        modifier = Modifier.fillMaxSize()
                    )
                    Text(
                        text = "${(viewModel.exportProgress * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Text(
                    text = "Baking Cinematic Timeline...",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                // Render detail simulation ticker
                val simFrame = (viewModel.exportProgress * 300).roundToInt()
                Text(
                    text = "Merging tracks: Clip Frame $simFrame/300\nApplying active glitch shaders & M3 dynamic layers\nCodec: MPEG-4 AVC H.264 High Profile",
                    color = Color.LightGray.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.cancelExport() },
                    colors = ButtonDefaults.buttonColors(containerColor = SlateCardSecondary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancel Rendering", color = Color.LightGray)
                }
            } else {
                // Success screen!
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = CapCutGreen,
                    modifier = Modifier.size(72.dp)
                )

                Text(
                    text = "Rendered Completed!",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )

                Text(
                    text = "Project \"${project.name}\" successfully compiled at ${viewModel.exportResolution} (${viewModel.exportFps} FPS) to /sdcard/Movies/CapCut",
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { viewModel.cancelExport() }, // return to timeline
                        colors = ButtonDefaults.buttonColors(containerColor = SlateCardSecondary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Back to Canvas", color = Color.White)
                    }

                    Button(
                        onClick = { viewModel.cancelExport() }, // simulate share
                        colors = ButtonDefaults.buttonColors(containerColor = CapCutCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Icon(Icons.Default.PhoneAndroid, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share to TikTok", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ----------------- MISC HELPER STYLINGS -----------------

fun formatTimecode(timeMs: Long): String {
    val totalSecs = timeMs / 1000L
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    val frames = ((timeMs % 1000L) / 33.3).toInt() // Standard 30fps frames representation
    return String.format("%02d:%02d.%02d", mins, secs, frames)
}

fun getIconForScene(type: SceneType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (type) {
        SceneType.NEON_STREET -> Icons.Default.Bolt
        SceneType.COSMOS_NEBULA -> Icons.Default.Star
        SceneType.TOKYO_SYNTHWAVE -> Icons.Default.WbSunny
        SceneType.GLITCH_MATRIX -> Icons.Default.Code
        SceneType.CINEMATIC_SUNSET -> Icons.Default.Filter
    }
}

fun getClipColorByScene(type: SceneType): List<Color> {
    return when (type) {
        SceneType.NEON_STREET -> listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
        SceneType.COSMOS_NEBULA -> listOf(Color(0xFF3A6073), Color(0xFF3A6073), Color(0xFF16222F))
        SceneType.TOKYO_SYNTHWAVE -> listOf(Color(0xFF654ea3), Color(0xFFeaac8b))
        SceneType.GLITCH_MATRIX -> listOf(Color(0xFF0D0D0D), Color(0xFF00FF33))
        SceneType.CINEMATIC_SUNSET -> listOf(Color(0xFFFF8C00), Color(0xFFFF00D4))
    }
}

@Composable
fun getTextStyleForPreset(preset: TextStylePreset, color: Color, scale: Float): TextStyle {
    val size = (24 * scale).sp
    return when (preset) {
        TextStylePreset.NEON_CYAN -> TextStyle(
            color = Color(0xFFD7FFFF),
            fontSize = size,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.SansSerif,
            shadow = Shadow(color = CapCutCyan, offset = Offset.Zero, blurRadius = 24f)
        )
        TextStylePreset.RETRO_AMBER -> TextStyle(
            color = Color(0xFFFFE082),
            fontSize = size,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            shadow = Shadow(color = Color(0xFFFF6F00), offset = Offset(4f, 4f))
        )
        TextStylePreset.CHROME_SILVER -> TextStyle(
            color = Color(0xFFEEEEEE),
            fontSize = size,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif
        )
        TextStylePreset.OUTLINE_WHITE -> TextStyle(
            color = Color.White,
            fontSize = size,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.SansSerif
        )
        TextStylePreset.VINTAGE_GOLD -> TextStyle(
            color = Color(0xFFFFDF00),
            fontSize = size,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            shadow = Shadow(color = Color.Black, offset = Offset(2f, 2f), blurRadius = 6f)
        )
        TextStylePreset.LCD_GREEN -> TextStyle(
            color = Color(0xFF00FF41),
            fontSize = size,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            shadow = Shadow(color = Color(0xFF003B00), offset = Offset.Zero, blurRadius = 12f)
        )
    }
}

// ----------------- COMPANION ONBOARDING ACCOUNT VIEWS -----------------

@Composable
fun TutorialGuideOverlay(viewModel: EditorViewModel) {
    val step = viewModel.currentTutorialStep
    if (step <= 0) return

    val title = when (step) {
        1 -> "1. Draft Manager 📂"
        2 -> "2. Procedural Viewport 🎬"
        3 -> "3. Multi-Track Timeline ⏱️"
        4 -> "4. Professional Studio Tools 🛠️"
        else -> "5. Cloud Sync Sync ID ☁️"
    }

    val desc = when (step) {
        1 -> "Click the menu icon in the top left to open your preloaded high-fidelity project edits. You can create a clean new draft with one click!"
        2 -> "Our state-of-the-art vector renderer compiles stunning, responsive canvas graphics. Touch or slide on it to preview!"
        3 -> "Drag or tap on individual clip, text, or sticker blocks here. Use the central 'Split' tool to carve video fragments precisely."
        4 -> "Navigate the Bottom Tabs matrix to apply retro VHS, 3D glimmers, custom text presets, or unleash pro-tier lasers and kaleidoscope shaders!"
        else -> "Sign in on the upper right panel to access cross-platform edits and sync your workspace with other devices!"
    }

    val alignment = when (step) {
        1 -> Alignment.TopStart
        2 -> Alignment.Center
        3 -> Alignment.Center
        4 -> Alignment.BottomCenter
        else -> Alignment.TopEnd
    }

    val offsetModifier = when (step) {
        1 -> Modifier.padding(start = 24.dp, top = 80.dp, end = 0.dp, bottom = 0.dp)
        2 -> Modifier.padding(start = 24.dp, top = 0.dp, end = 24.dp, bottom = 0.dp).offset(y = (-80).dp)
        3 -> Modifier.padding(start = 24.dp, top = 0.dp, end = 24.dp, bottom = 0.dp).offset(y = 120.dp)
        4 -> Modifier.padding(start = 24.dp, top = 0.dp, end = 24.dp, bottom = 84.dp)
        else -> Modifier.padding(start = 0.dp, top = 80.dp, end = 24.dp, bottom = 0.dp)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .pointerInput(Unit) { detectTapGestures { } } // Block background clicks
    ) {
        Card(
            modifier = offsetModifier
                .widthIn(max = 340.dp)
                .align(alignment)
                .testTag("tutorial_guide_card"),
            colors = CardDefaults.cardColors(containerColor = SlatePanelDark),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            border = BorderStroke(1.5.dp, CapCutCyan)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        color = CapCutCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "$step/5",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = desc,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { viewModel.dismissTutorial() }) {
                        Text("Skip Tour", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { viewModel.nextTutorialStep() },
                        colors = ButtonDefaults.buttonColors(containerColor = CapCutCyan),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (step < 5) "Next Step" else "Done!",
                            color = Color.Black,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AccountProfileDrawer(
    viewModel: EditorViewModel
) {
    var emailInput by remember { mutableStateOf(viewModel.userEmail ?: "") }
    var nameInput by remember { mutableStateOf(viewModel.userName ?: "") }

    val context = LocalContext.current
    
    // Google Sign-In Result Launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                viewModel.signInWithGoogle(idToken)
            } else {
                viewModel.showToastPublic("Google Sign-In failed: No ID token retrieved.")
            }
        } catch (e: Exception) {
            viewModel.showToastPublic("Google Sign-In error: ${e.localizedMessage ?: "Handshake failed"}")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlatePanelDark)
            .border(width = 1.dp, color = SlateBorderLight, shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
            .padding(16.dp)
            .testTag("account_profile_drawer"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Drawer Header with Close Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.ManageAccounts, contentDescription = null, tint = CapCutCyan)
                Text("CapCut Account", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            IconButton(onClick = { viewModel.isAuthSheetOpen = false }) {
                Icon(Icons.Default.Close, contentDescription = "Close Account Panel", tint = Color.White)
            }
        }

        Divider(color = SlateBorderLight)

        if (viewModel.userEmail == null) {
            // NOT SIGNED IN state: beautiful credential form
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Sign in to save cloud backups and access cross-device project syncing with the editor community.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                // Input Name Field
                Text("Your Nickname", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    placeholder = { Text("e.g. EditorPro", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CapCutCyan,
                        unfocusedBorderColor = SlateBorderLight,
                        focusedContainerColor = SlateBackground,
                        unfocusedContainerColor = SlateBackground
                    ),
                    textStyle = TextStyle(fontSize = 14.sp),
                    modifier = Modifier.fillMaxWidth().testTag("auth_name_field"),
                    singleLine = true
                )

                // Input Email Field
                Text("Email Address", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    placeholder = { Text("user@email.com", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CapCutCyan,
                        unfocusedBorderColor = SlateBorderLight,
                        focusedContainerColor = SlateBackground,
                        unfocusedContainerColor = SlateBackground
                    ),
                    textStyle = TextStyle(fontSize = 14.sp),
                    modifier = Modifier.fillMaxWidth().testTag("auth_email_field"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.attemptSignIn(emailInput, nameInput) },
                    colors = ButtonDefaults.buttonColors(containerColor = CapCutCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp).testTag("sign_in_submit_btn")
                ) {
                    Icon(Icons.Default.CloudQueue, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign In & Sync Profile", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                // Modern Google Sign-In Option
                Button(
                    onClick = {
                        try {
                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                                .requestEmail()
                                .build()
                            val client = GoogleSignIn.getClient(context, gso)
                            googleSignInLauncher.launch(client.signInIntent)
                        } catch (e: Exception) {
                            viewModel.showToastPublic("Google Auth setup: ${e.localizedMessage}")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp).testTag("google_sign_in_btn")
                ) {
                    Icon(Icons.Default.AccountCircle, contentDescription = "Google Logo", modifier = Modifier.size(18.dp), tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign in with Google", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                TextButton(
                    onClick = {
                        viewModel.attemptSignIn("guest@capcut.com", "Guest Editor")
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Continue as Guest Sandbox", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                }
            }
        } else {
            // SIGNED IN state: high performance user detail dashboard
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                // User Avatar and Header Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SlateCardSecondary),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CapCutPurple)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(CapCutCyan, CapCutPurple))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (viewModel.userName ?: "").take(2).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = viewModel.userName ?: "CapCut Creator",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified Creator",
                                tint = CapCutCyan,
                                modifier = Modifier.size(16.dp).testTag("verified_badge_account")
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(CapCutPurple.copy(alpha = 0.25f))
                                .border(1.dp, CapCutPurple, RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Text("LIFETIME PRO UNLOCKED", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Feature items
                AccountFeatureRow(icon = Icons.Default.CloudDone, label = "Cloud Draft Backups", status = "Synced")
                AccountFeatureRow(icon = Icons.Default.NoPhotography, label = "Render Watermarks", status = "Removed")
                AccountFeatureRow(icon = Icons.Default.Hd, label = "Encoder Exports", status = "4K 60FPS")
                AccountFeatureRow(icon = Icons.Default.Bolt, label = "PRO Procedural Shaders", status = "Enabled")

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { viewModel.performSignOut() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp).testTag("sign_out_btn")
                ) {
                    Text("Deauthenticate Account", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun AccountFeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    status: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SlateCardSecondary)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, contentDescription = null, tint = CapCutCyan, modifier = Modifier.size(18.dp))
            Text(label, color = Color.White, fontSize = 12.sp)
        }
        Text(status, color = CapCutGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

// End of CapCut Editor UI Content

@Composable
fun CapCutHomeScreen(viewModel: EditorViewModel, draftsList: List<ProjectDraft>) {
    var newProjectTitle by remember { mutableStateOf("") }
    var selectedRatio by remember { mutableStateOf(AspectRatioPreset.TIKTOK_9_16) }
    
    // Background gradient
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateBackground)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .testTag("capcut_homescreen_container")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 800.dp)
                .align(Alignment.TopCenter),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Section 1: Dashboard Cosmic Brand Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF131F30), Color(0xFF0F151B))
                        )
                    )
                    .border(1.dp, CapCutPurple.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "ProCut AI Studio",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Box(
                            modifier = Modifier
                                .background(CapCutPurple.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .border(0.5.dp, CapCutPurple, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("PRO CLIENT", color = CapCutCyan, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Created by moderators • Procedural Neural Shader Platform & Creative Timeline Engine",
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )
                }
                
                // Active status or sign-in profile avatar shortcut
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SlateCardSecondary)
                        .clickable { viewModel.isAuthSheetOpen = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(CapCutGreen)
                        )
                        Text(
                            text = viewModel.userName ?: "Anonymous Creator",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (viewModel.userName != null) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified Creator",
                                tint = CapCutCyan,
                                modifier = Modifier.size(11.dp).testTag("verified_creator_home_badge")
                            )
                        }
                    }
                }
            }

            // Section 2: Creator Studio - CREATE PROJECT CARD!
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SlateCardSecondary)
                    .border(1.dp, SlateBorderLight, RoundedCornerShape(12.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Start Creating",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.2f)) {
                        Text("Draft Name", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = newProjectTitle,
                            onValueChange = { newProjectTitle = it },
                            placeholder = { Text("Name your creative vision...", color = Color.Gray, fontSize = 11.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF131D28),
                                unfocusedContainerColor = Color(0xFF131D28),
                                focusedBorderColor = CapCutCyan,
                                unfocusedBorderColor = SlateBorderLight
                            ),
                            textStyle = TextStyle(fontSize = 12.sp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("homescreen_project_title_input")
                        )
                    }

                    Column(modifier = Modifier.weight(1.8f)) {
                        Text("Timeline Aspect Ratio", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AspectRatioPreset.values().forEach { ar ->
                                val isChosen = selectedRatio == ar
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isChosen) CapCutCyan else Color(0xFF131D28))
                                        .border(
                                            1.dp,
                                            if (isChosen) CapCutCyan else SlateBorderLight,
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable { selectedRatio = ar }
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = ar.label.substringBefore(" ("),
                                            color = if (isChosen) Color.Black else Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = when (ar) {
                                                AspectRatioPreset.TIKTOK_9_16 -> "9:16"
                                                AspectRatioPreset.YOUTUBE_16_9 -> "16:9"
                                                AspectRatioPreset.INSTAGRAM_1_1 -> "1:1"
                                                AspectRatioPreset.CINEMATIC_21_9 -> "Cine"
                                            },
                                            color = if (isChosen) Color.DarkGray else Color.Gray,
                                            fontSize = 7.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val name = if (newProjectTitle.isBlank()) "New Visual Edit" else newProjectTitle
                            viewModel.openMediaSelectorForNewProject(name, selectedRatio)
                            newProjectTitle = "" // reset input
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CapCutCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("homescreen_create_project_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                            Text("Create Project", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Section 3: Draft Hub Gallery List
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My Visual Projects (${draftsList.size})",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Offline Draft Save: Auto-Sync enabled",
                        color = Color.Gray,
                        fontSize = 9.sp
                    )
                }

                if (draftsList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(SlateCardSecondary, RoundedCornerShape(12.dp))
                            .border(1.dp, SlateBorderLight, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No active drafts found. Type a title and hit Create above!", color = Color.Gray, fontSize = 12.sp)
                    }
                } else {
                    draftsList.forEach { draft ->
                        ProjectDraftCard(
                            draft = draft,
                            isActive = viewModel.project.value.id == draft.id,
                            onSelect = { viewModel.selectProject(draft) },
                            onDelete = { viewModel.deleteProjectDraft(draft.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectDraftCard(
    draft: ProjectDraft,
    isActive: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val totalMs = draft.clips.fold(0L) { acc, clip -> acc + clip.getEffectiveDurationMs() }
    val durationSecStr = "${totalMs / 1000f}s"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isActive) Color(0xFF132230) else SlateCardSecondary)
            .border(
                1.dp,
                if (isActive) CapCutCyan.copy(alpha = 0.5f) else SlateBorderLight,
                RoundedCornerShape(10.dp)
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail stylized box representation based on Aspect Ratio
        Box(
            modifier = Modifier
                .size(width = 54.dp, height = 54.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Black)
                .border(0.5.dp, Color.DarkGray, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Draw a tiny procedural design of a film strip / aspect ratio boundaries
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    tint = if (isActive) CapCutCyan else CapCutPurple,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = draft.aspectRatio.label.substringBefore(" "),
                    color = Color.LightGray,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp)
                    .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
            )
        }

        // Project details core metadata columns
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = draft.name,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Duration: $durationSecStr",
                    color = CapCutCyan,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "•",
                    color = Color.DarkGray,
                    fontSize = 9.sp
                )
                Text(
                    text = "${draft.clips.size} clips • ${draft.effects.size} Shaders • ${draft.stickers.size} Stickers",
                    color = Color.LightGray,
                    fontSize = 9.sp
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Last Saved: ${draft.lastModified}",
                color = Color.Gray,
                fontSize = 8.sp
            )
        }

        // Action Buttons Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onSelect,
                colors = ButtonDefaults.buttonColors(containerColor = CapCutPurple, contentColor = Color.White),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                modifier = Modifier
                    .height(32.dp)
                    .testTag("edit_draft_btn_${draft.id}")
            ) {
                Text("Edit Project", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp).testTag("delete_draft_btn_${draft.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Draft",
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

val SimpleAlphaCyan = Color(0x2200F0FF)

@Composable
fun MediaAssetSelectorDialog(viewModel: EditorViewModel) {
    if (!viewModel.isMediaSelectorOpen) return

    val context = LocalContext.current
    val selectedIds = viewModel.selectedMediaIds
    var selectedTab by remember { mutableStateOf("all") } // "all", "video", "photo"

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris.forEach { uri ->
            val name = getFileName(context, uri) ?: "Local Photo"
            viewModel.addCustomDeviceMedia(uri, isVideo = false, name = name)
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris.forEach { uri ->
            val name = getFileName(context, uri) ?: "Local Video"
            viewModel.addCustomDeviceMedia(uri, isVideo = true, name = name)
        }
    }

    val filteredAssets = remember(selectedTab, viewModel.allMediaAssets) {
        when (selectedTab) {
            "video" -> viewModel.allMediaAssets.filter { it.isVideo }
            "photo" -> viewModel.allMediaAssets.filter { !it.isVideo }
            else -> viewModel.allMediaAssets
        }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = { viewModel.isMediaSelectorOpen = false },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 680.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0F151B))
                .border(1.dp, CapCutPurple.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.5f)) {
                        Text(
                            text = "Import Media Assets",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Choose local photos/videos from your device, or select our beautiful high-fidelity presets.",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(
                        onClick = { viewModel.isMediaSelectorOpen = false },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Selector",
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Device Pickers Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = SlateCardSecondary, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("device_pick_photos_btn")
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Add Photos", modifier = Modifier.size(16.dp), tint = CapCutCyan)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Photos", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = { videoPickerLauncher.launch("video/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = SlateCardSecondary, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("device_pick_videos_btn")
                    ) {
                        Icon(Icons.Default.VideoCall, contentDescription = "Add Videos", modifier = Modifier.size(16.dp), tint = CapCutOrange)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Videos", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Filters Tabs
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val tabs = listOf(
                        Triple("all", "All Assets", "🗂️"),
                        Triple("video", "Videos Only", "🎥"),
                        Triple("photo", "Photos Only", "📸")
                    )
                    tabs.forEach { (tabId, label, emoji) ->
                        val isSel = selectedTab == tabId
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(30.dp))
                                .background(if (isSel) CapCutCyan else SlateCardSecondary)
                                .border(1.dp, if (isSel) CapCutCyan else SlateBorderLight, RoundedCornerShape(30.dp))
                                .clickable { selectedTab = tabId }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(emoji, fontSize = 11.sp)
                                Text(
                                    text = label,
                                    color = if (isSel) Color.Black else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Media Assets Grid
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0A0F14))
                        .border(1.dp, SlateBorderLight, RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 130.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredAssets) { asset ->
                            val isChosen = selectedIds.contains(asset.id)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isChosen) SimpleAlphaCyan else SlateCardSecondary)
                                    .border(
                                        2.dp,
                                        if (isChosen) CapCutCyan else Color.Transparent,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { viewModel.toggleMediaAssetSelection(asset.id) }
                                    .padding(8.dp)
                                    .height(96.dp)
                                    .testTag("media_asset_${asset.id}")
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Top Row: Type indicator & Checkbox
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .background(asset.color.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(asset.emoji, fontSize = 8.sp)
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(
                                                    text = if (asset.isVideo) "VIDEO" else "PHOTO",
                                                    color = asset.color,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        // Selection Radio badge
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(if (isChosen) CapCutCyan else Color.Gray.copy(alpha = 0.3f))
                                                .border(1.dp, if (isChosen) CapCutCyan else Color.Gray, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isChosen) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = Color.Black,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Bottom Area: Title and tag
                                    Column {
                                        Text(
                                            text = asset.name,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = asset.tag,
                                                color = Color.Gray,
                                                fontSize = 8.sp
                                            )
                                            Text(
                                                text = "${asset.durationMs / 1000f}s",
                                                color = CapCutGreen,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Summary Row and Actions
                val videoCount = selectedIds.count { id -> viewModel.allMediaAssets.find { it.id == id }?.isVideo == true }
                val photoCount = selectedIds.count { id -> viewModel.allMediaAssets.find { it.id == id }?.isVideo == false }
                val totalDuration = selectedIds.fold(0L) { acc, id -> acc + (viewModel.allMediaAssets.find { it.id == id }?.durationMs ?: 0L) } / 1000f

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left summary stats
                    Column {
                        Text(
                            text = "Import Pack Details",
                            color = Color.Gray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (selectedIds.isEmpty()) "None Selected" else "$videoCount Videos, $photoCount Photos (${totalDuration}s)",
                            color = if (selectedIds.isEmpty()) Color.Red else CapCutGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Action buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = { viewModel.isMediaSelectorOpen = false },
                            modifier = Modifier.height(40.dp)
                        ) {
                            Text("Cancel", color = Color.Gray, fontSize = 12.sp)
                        }

                        Button(
                            onClick = { viewModel.confirmMediaSelectionAndCreateProject() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CapCutCyan,
                                contentColor = Color.Black,
                                disabledContainerColor = Color.Gray.copy(alpha = 0.3f),
                                disabledContentColor = Color.Gray
                            ),
                            shape = RoundedCornerShape(8.dp),
                            enabled = selectedIds.isNotEmpty(),
                            modifier = Modifier
                                .height(40.dp)
                                .testTag("media_selector_confirm_btn")
                        ) {
                            Text(
                                text = "Import Selected (${selectedIds.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

fun getFileName(context: android.content.Context, uri: android.net.Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val columnIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (columnIndex >= 0) {
                        result = it.getString(columnIndex)
                    }
                }
            }
        } catch (e: Exception) {
            // fallback
        }
    }
    if (result == null) {
        val path = uri.path
        val cut = path?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = path?.substring(cut + 1)
        } else {
            result = path
        }
    }
    return result
}

@Composable
fun TalkingAvatarAnimator(isTalking: Boolean) {
    var animateStep by remember { mutableStateOf(0f) }
    LaunchedEffect(isTalking) {
        if (isTalking) {
            while (true) {
                delay(90)
                animateStep = (animateStep + 1f) % 8f
            }
        } else {
            animateStep = 0f
        }
    }

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color(0xFF131D28))
            .border(1.5.dp, if (isTalking) CapCutOrange else CapCutCyan.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(32.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            
            // Outer circular face glow
            drawCircle(
                color = if (isTalking) CapCutOrange.copy(alpha = 0.15f) else CapCutCyan.copy(alpha = 0.1f),
                radius = size.width / 2f
            )
            
            // Holographic Eyes
            drawCircle(
                color = if (isTalking) CapCutOrange else CapCutCyan,
                radius = 2.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(cx - 5.dp.toPx(), cy - 3.dp.toPx())
            )
            drawCircle(
                color = if (isTalking) CapCutOrange else CapCutCyan,
                radius = 2.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(cx + 5.dp.toPx(), cy - 3.dp.toPx())
            )
            
            // Visor spectrum glow connection
            drawLine(
                color = if (isTalking) CapCutOrange.copy(alpha = 0.5f) else CapCutCyan.copy(alpha = 0.4f),
                start = androidx.compose.ui.geometry.Offset(cx - 5.dp.toPx(), cy - 3.dp.toPx()),
                end = androidx.compose.ui.geometry.Offset(cx + 5.dp.toPx(), cy - 3.dp.toPx()),
                strokeWidth = 1.dp.toPx()
            )
            
            // Mouth talking waves
            if (isTalking) {
                val waveHeight = 2.dp.toPx() + (animateStep % 3f) * 2.dp.toPx()
                drawLine(
                    color = CapCutOrange,
                    start = androidx.compose.ui.geometry.Offset(cx - 4.dp.toPx(), cy + 4.dp.toPx()),
                    end = androidx.compose.ui.geometry.Offset(cx + 4.dp.toPx(), cy + 4.dp.toPx()),
                    strokeWidth = waveHeight
                )
            } else {
                // static cute neutral line
                drawLine(
                    color = CapCutCyan.copy(alpha = 0.7f),
                    start = androidx.compose.ui.geometry.Offset(cx - 3.dp.toPx(), cy + 4.dp.toPx()),
                    end = androidx.compose.ui.geometry.Offset(cx + 3.dp.toPx(), cy + 4.dp.toPx()),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
        }
    }
}

@Composable
fun AiVideoGeneratorPanel(viewModel: EditorViewModel) {
    var prompt by remember { mutableStateOf(viewModel.aiVideoPrompt) }
    
    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (viewModel.isGeneratingAiVideo) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0D141C))
                    .border(1.dp, CapCutCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CircularProgressIndicator(
                        progress = viewModel.aiVideoGenerationProgress,
                        color = CapCutCyan,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "AI Generating Animation: ${(viewModel.aiVideoGenerationProgress * 100).toInt()}%",
                        color = CapCutCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when {
                            viewModel.aiVideoGenerationProgress < 0.3f -> "Parsing prompt tokens..."
                            viewModel.aiVideoGenerationProgress < 0.6f -> "Baking fluid keyframe layers..."
                            else -> "Injecting rendering noise filters..."
                        },
                        color = Color.Gray,
                        fontSize = 9.sp
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.weight(1.3f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "AI Text-to-Video Animator 🚀",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { 
                        prompt = it
                        viewModel.aiVideoPrompt = it
                    },
                    placeholder = { Text("E.g. Futuristic hypercar speeding under neon rain...", color = Color.Gray, fontSize = 9.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = SlateCardSecondary,
                        unfocusedContainerColor = SlateCardSecondary,
                        focusedBorderColor = CapCutCyan,
                        unfocusedBorderColor = SlateBorderLight
                    ),
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 10.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .testTag("ai_video_prompt_input")
                )

                // Quick presets
                val presets = listOf("Neon Cruise", "Cosmic Speed", "Glitch Code", "Cyber Sunset")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(presets) { p ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(SlateCardSecondary)
                                .clickable {
                                    val fullPrompt = when(p) {
                                        "Neon Cruise" -> "Cinematic neon street cruise with intense cyber reflections"
                                        "Cosmic Speed" -> "Warp-speed spaceship cruise inside space nebula clouds"
                                        "Glitch Code" -> "Raw code sequence raining on terminal background"
                                        else -> "Beautiful retro-futuristic horizon sunset behind grid lines"
                                    }
                                    prompt = fullPrompt
                                    viewModel.aiVideoPrompt = fullPrompt
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(p, color = Color.LightGray, fontSize = 7.sp)
                        }
                    }
                }
            }
            
            Button(
                onClick = { viewModel.generateAiAnimatedVideo(prompt) },
                colors = ButtonDefaults.buttonColors(containerColor = CapCutCyan, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .width(108.dp)
                    .height(52.dp)
                    .testTag("ai_video_generate_btn")
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Generate", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ImageAnalyzerPanel(viewModel: EditorViewModel, project: ProjectDraft) {
    if (project.clips.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No active timeline clips to analyze. Add clips first!", color = Color.Gray, fontSize = 11.sp)
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "AI Computer Vision Image Analyzer 👁️",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Select any timeline clip to run deep optical parameter audits, rule of thirds compositions, and color schemes:",
            color = Color.Gray,
            fontSize = 9.sp,
            lineHeight = 11.sp
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            items(project.clips) { clip ->
                val isBeingAnalyzed = viewModel.activeAnalysisClipId == clip.id && viewModel.isAnalyzingImage
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (viewModel.activeAnalysisClipId == clip.id) Color(0xFF13202E) else SlateCardSecondary
                    ),
                    border = BorderStroke(
                        0.5.dp,
                        if (viewModel.activeAnalysisClipId == clip.id) CapCutCyan else Color.Transparent
                    ),
                    modifier = Modifier
                        .width(168.dp)
                        .height(54.dp)
                        .clickable { viewModel.runImageAnalysis(clip) }
                        .testTag("analyze_clip_card_${clip.id}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "👁️",
                                fontSize = 11.sp
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(clip.name, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(modifier = Modifier.height(1.dp))
                            if (isBeingAnalyzed) {
                                Text("Analyzing...", color = CapCutCyan, fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
                            } else if (viewModel.activeAnalysisClipId == clip.id && viewModel.imageAnalysisResult != null) {
                                Text("Audit Complete ✓", color = CapCutGreen, fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
                            } else {
                                Text("Tap to Run AI Audit", color = Color.Gray, fontSize = 7.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImageAnalysisDialog(viewModel: EditorViewModel) {
    val analysis = viewModel.imageAnalysisResult
    if (analysis == null && !viewModel.isAnalyzingImage) return

    androidx.compose.ui.window.Dialog(
        onDismissRequest = { 
            viewModel.imageAnalysisResult = null
            viewModel.activeAnalysisClipId = null
        }
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SlatePanelDark),
            border = BorderStroke(1.dp, CapCutCyan.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("image_analysis_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, tint = CapCutCyan, modifier = Modifier.size(16.dp))
                        Text(
                            text = "AI Computer Vision Audit",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.Gray,
                        modifier = Modifier
                            .size(14.dp)
                            .clickable {
                                viewModel.imageAnalysisResult = null
                                viewModel.activeAnalysisClipId = null
                            }
                    )
                }

                Divider(color = SlateBorderLight, thickness = 0.5.dp)

                if (viewModel.isAnalyzingImage) {
                    // Scanning state
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = CapCutCyan, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Running Professional Pixel Analysis...", color = CapCutCyan, fontSize = 11.sp)
                        Text("Reading luminance histograms & rule of thirds grid...", color = Color.Gray, fontSize = 8.sp)
                    }
                } else if (analysis != null) {
                    // Results state!
                    Text(
                        text = "Source Clip: \"${analysis.assetName}\"",
                        color = Color.LightGray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Brightness Bar
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Luminance Calibration", color = Color.Gray, fontSize = 8.sp)
                            Text(analysis.brightnessStr, color = CapCutCyan, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                        // Draw a cool visual progress line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFF1E2835))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.75f)
                                    .fillMaxHeight()
                                    .background(CapCutCyan)
                            )
                        }
                    }

                    // Composition
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Composition balance", color = Color.Gray, fontSize = 8.sp)
                            Text(analysis.balanceStr, color = CapCutGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFF1E2835))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .fillMaxHeight()
                                    .background(CapCutGreen)
                            )
                        }
                    }

                    // Dominant Color pills
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Dominant Cinematic Color Scheme", color = Color.Gray, fontSize = 8.sp)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            analysis.colorsList.forEach { colName ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF1F1F27))
                                        .border(0.5.dp, Color.DarkGray, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(colName, color = Color.White, fontSize = 7.sp)
                                }
                            }
                        }
                    }

                    // Aesthetic Summary Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0D1621), RoundedCornerShape(6.dp))
                            .border(0.5.dp, CapCutCyan.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("ESTIMATED STYLE LAYERS", color = CapCutCyan, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = analysis.aestheticDesc,
                                color = Color.LightGray,
                                fontSize = 8.sp,
                                lineHeight = 10.sp
                            )
                        }
                    }

                    // AI Recommendation
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1A1323), RoundedCornerShape(6.dp))
                            .border(0.5.dp, CapCutPurple.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("PRO AUDIOVISUAL ENHANCEMENTS", color = CapCutPurple, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = analysis.recommendation,
                                color = Color.White,
                                fontSize = 8.sp,
                                lineHeight = 10.sp
                            )
                        }
                    }

                    // Apply Recommendations
                    Button(
                        onClick = {
                            viewModel.showToastPublic("Applied cinematic improvements to draft!")
                            viewModel.imageAnalysisResult = null
                            viewModel.activeAnalysisClipId = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CapCutCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                    ) {
                        Text("Apply Recommended Studio Enhancements", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

