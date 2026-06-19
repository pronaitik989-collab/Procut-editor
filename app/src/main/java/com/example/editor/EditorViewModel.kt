package com.example.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import android.net.Uri

class EditorViewModel : ViewModel() {

    // Firebase Backend References
    private val auth: FirebaseAuth? by lazy {
        try { FirebaseAuth.getInstance() } catch (e: Exception) { null }
    }
    private val db: FirebaseFirestore? by lazy {
        try { FirebaseFirestore.getInstance() } catch (e: Exception) { null }
    }

    // Is there any active project?
    private val _project = MutableStateFlow<ProjectDraft>(createNewProject("Untitled Draft"))
    val project: StateFlow<ProjectDraft> = _project.asStateFlow()

    // Playlist drafts history
    private val _draftsList = MutableStateFlow<List<ProjectDraft>>(emptyList())
    val draftsList: StateFlow<List<ProjectDraft>> = _draftsList.asStateFlow()

    // Interactive playback states
    private val _currentTimeMs = MutableStateFlow<Long>(0L)
    val currentTimeMs: StateFlow<Long> = _currentTimeMs.asStateFlow()

    private val _isPlaying = MutableStateFlow<Boolean>(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    // Selection states
    var selectedClipId by mutableStateOf<String?>(null)
    var selectedEffectId by mutableStateOf<String?>(null)
    var selectedAudioId by mutableStateOf<String?>(null)
    var selectedTextId by mutableStateOf<String?>(null)
    var selectedStickerId by mutableStateOf<String?>(null)

    // Editing mode selectors
    var activeTab by mutableStateOf("edit") // edit, audio, text, effects, filters, templates, ratio

    // Main screen navigator view: true to display listed projects landing page, false to enter CapCut Studio Editor
    var showHomeScreen by mutableStateOf(true)

    // Media asset picker database and states
    val availableMediaAssets = listOf(
        MediaAsset("vid_neon_street", "Neon Street Cruise", true, 4000L, SceneType.NEON_STREET, "🎥", "4K Video", Color(0xFFFF0055)),
        MediaAsset("vid_tokyo_synthwave", "Tokyo Cyber Skyline", true, 5000L, SceneType.TOKYO_SYNTHWAVE, "🎥", "120FPS Video", Color(0xFF00F0FF)),
        MediaAsset("vid_glitch_matrix", "Glitch Code Flow", true, 3000L, SceneType.GLITCH_MATRIX, "🎥", "Pro Video", Color(0xFF00FF66)),
        MediaAsset("vid_cosmos_nebula", "Nebula Warp Speed", true, 6000L, SceneType.COSMOS_NEBULA, "🎥", "Space Video", Color(0xFFB000FF)),
        MediaAsset("vid_cinematic_sunset", "Dreamy Golden Sunset", true, 4500L, SceneType.CINEMATIC_SUNSET, "🎥", "HDR Video", Color(0xFFFF9900)),
        MediaAsset("pic_tokyo_night", "Tokyo Cyber Tower Pic", false, 3000L, SceneType.TOKYO_SYNTHWAVE, "📸", "HD Photo", Color(0xFFE040FB)),
        MediaAsset("pic_space_dust", "Milkyway Clusters Pic", false, 3000L, SceneType.COSMOS_NEBULA, "📸", "Astro Photo", Color(0xFF536DFE)),
        MediaAsset("pic_neon_car", "Retro Racer Silhouette Pic", false, 3000L, SceneType.NEON_STREET, "📸", "Synth Photo", Color(0xFFFF4081)),
        MediaAsset("pic_matrix_core", "System Offline Error Pic", false, 3000L, SceneType.GLITCH_MATRIX, "📸", "Glitch Photo", Color(0xFF64FFDA)),
        MediaAsset("pic_golden_beach", "Cinematic Sunset Shore Pic", false, 3000L, SceneType.CINEMATIC_SUNSET, "📸", "Sunset Photo", Color(0xFFFFD740))
    )

    var customMediaAssets by mutableStateOf<List<MediaAsset>>(emptyList())

    val allMediaAssets: List<MediaAsset>
        get() = availableMediaAssets + customMediaAssets

    var isMediaSelectorOpen by mutableStateOf(false)
    var selectedMediaIds by mutableStateOf<Set<String>>(emptySet())
    var pendingProjectName by mutableStateOf("")
    var pendingAspectRatio by mutableStateOf(AspectRatioPreset.TIKTOK_9_16)

    fun openMediaSelectorForNewProject(name: String, ratio: AspectRatioPreset) {
        pendingProjectName = name
        pendingAspectRatio = ratio
        // Pre-select some assets to be helpful
        selectedMediaIds = setOf("vid_neon_street", "pic_tokyo_night")
        isMediaSelectorOpen = true
    }

    fun addCustomDeviceMedia(uri: Uri, isVideo: Boolean, name: String) {
        val id = uri.toString()
        if (customMediaAssets.any { it.id == id }) return

        val sceneTypes = SceneType.values()
        val sceneType = sceneTypes[customMediaAssets.size % sceneTypes.size]

        val newAsset = MediaAsset(
            id = id,
            name = name,
            isVideo = isVideo,
            durationMs = if (isVideo) 6000L else 3000L,
            sceneType = sceneType,
            emoji = if (isVideo) "🎬" else "🖼️",
            tag = "Device Media",
            color = if (isVideo) Color(0xFF00FF66) else Color(0xFFFF4081)
        )
        customMediaAssets = customMediaAssets + newAsset
        selectedMediaIds = selectedMediaIds + id
    }

    fun toggleMediaAssetSelection(id: String) {
        selectedMediaIds = if (selectedMediaIds.contains(id)) {
            selectedMediaIds - id
        } else {
            selectedMediaIds + id
        }
    }

    fun confirmMediaSelectionAndCreateProject() {
        if (selectedMediaIds.isEmpty()) {
            showToast("Please select at least one photo or video!")
            return
        }
        
        val clips = selectedMediaIds.map { id ->
            val asset = allMediaAssets.find { it.id == id } ?: allMediaAssets[0]
            val isFromLocalFile = asset.id.startsWith("content://") || asset.id.startsWith("file://")
            VideoClip(
                id = generateId(),
                name = asset.name + if (asset.isVideo) " (Video)" else " (Photo)",
                sceneType = asset.sceneType,
                durationMs = asset.durationMs,
                sourceDurationMs = asset.durationMs,
                trimStartMs = 0L,
                speed = 1.0f,
                opacity = 1.0f,
                adjustments = ColorAdjustments(),
                localUri = if (isFromLocalFile) asset.id else null
            )
        }
        
        val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
        val name = if (pendingProjectName.isBlank()) "My Visual Draft" else pendingProjectName
        val newProj = ProjectDraft(
            id = generateId(),
            name = name,
            lastModified = sdf.format(Date()),
            clips = clips,
            effects = emptyList(),
            audios = emptyList(),
            texts = emptyList(),
            stickers = emptyList(),
            aspectRatio = pendingAspectRatio
        )
        
        _draftsList.value = _draftsList.value + newProj
        selectProject(newProj)
        
        isMediaSelectorOpen = false
        showHomeScreen = false
        showToast("Created project '${name}' with ${clips.size} media clips!")
    }


    // Export overlay state (all in-line, no popups!)
    var isExporting by mutableStateOf(false)
    var exportProgress by mutableStateOf(0f)
    var exportResolution by mutableStateOf("1080p")
    var exportFps by mutableStateOf(60)
    var exportDone by mutableStateOf(false)

    // Current split warning / info toast messages
    var toastMessage by mutableStateOf<String?>(null)

    // --- AUTHENTICATION & PROFILE STATE ---
    var userEmail by mutableStateOf<String?>(null)
    var userName by mutableStateOf<String?>(null)
    var isProMember by mutableStateOf(true)
    var isAuthSheetOpen by mutableStateOf(false)

    fun attemptSignIn(email: String, name: String) {
        if (email.contains("@") && name.isNotBlank()) {
            signInWithEmailPassword(email, registrationName = name)
        } else {
            showToast("Please enter a valid name & email address!")
        }
    }

    fun signInWithEmailPassword(email: String, password: String = "password123", registrationName: String = "") {
        val authRef = auth ?: run {
            // If Firebase is unavailable in environment, fallback to offline sandbox
            userEmail = email
            userName = registrationName.takeIf { it.isNotBlank() } ?: email.substringBefore("@")
            isProMember = true
            isAuthSheetOpen = false
            showToast("Offline sandbox: Profile locked & activated!")
            return
        }
        
        showToast("Accessing secure cloud auth...")
        authRef.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = authRef.currentUser
                    if (user != null) {
                        userEmail = user.email
                        userName = user.displayName ?: registrationName.takeIf { it.isNotBlank() } ?: user.email?.substringBefore("@") ?: "Creator"
                        isProMember = true
                        isAuthSheetOpen = false
                        showToast("Welcome back, $userName! Sync complete.")
                        loadDraftsFromFirestore()
                    }
                } else {
                    // Account not found or wrong password, let's auto-create cloud profile (extremely fluid user experience)
                    showToast("Registering secure cloud profile...")
                    authRef.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { createDoc ->
                            if (createDoc.isSuccessful) {
                                val user = authRef.currentUser
                                if (user != null) {
                                    val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                                        displayName = registrationName.takeIf { it.isNotBlank() } ?: email.substringBefore("@")
                                    }
                                    user.updateProfile(profileUpdates).addOnCompleteListener {
                                        userEmail = user.email
                                        userName = registrationName.takeIf { it.isNotBlank() } ?: email.substringBefore("@")
                                        isProMember = true
                                        isAuthSheetOpen = false
                                        showToast("Cloud profile established successfully!")
                                        uploadCurrentDraftsToCloud()
                                    }
                                }
                            } else {
                                // If actual firebase project is not connected/valid at runtime, fallback gracefully to sandbox offline flow
                                userEmail = email
                                userName = registrationName.takeIf { it.isNotBlank() } ?: email.substringBefore("@")
                                isProMember = true
                                isAuthSheetOpen = false
                                showToast("Authenticated! Offline Sandbox Mode active.")
                            }
                        }
                }
            }
    }

    fun signInWithGoogle(idToken: String) {
        val authRef = auth ?: run {
            showToast("Firebase unavailable: Google Auth inactive.")
            return
        }
        showToast("Verifying Google Auth Token...")
        
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        authRef.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = authRef.currentUser
                    if (user != null) {
                        userEmail = user.email
                        userName = user.displayName ?: user.email?.substringBefore("@") ?: "Google Creator"
                        isProMember = true
                        isAuthSheetOpen = false
                        showToast("Signed in securely with Google Auth!")
                        loadDraftsFromFirestore()
                    }
                } else {
                    showToast("Google Authentication failed: ${task.exception?.localizedMessage}")
                }
            }
    }

    fun performSignOut() {
        val oldName = userName ?: "Creator"
        try {
            auth?.signOut()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        userEmail = null
        userName = null
        isProMember = false
        showToast("Signed out of $oldName")
        
        // Reset drafts list to baseline presets on logout
        val initialPresets = listOf(
            createTokyoMidnightPreset(),
            createCosmicOdysseyPreset(),
            createRetroVhsDraftPreset()
        )
        _draftsList.value = initialPresets
        _project.value = initialPresets[0]
    }

    fun loadDraftsFromFirestore() {
        val user = auth?.currentUser ?: return
        val dbRef = db ?: return
        
        dbRef.collection("users")
            .document(user.uid)
            .collection("drafts")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val list = mutableListOf<ProjectDraft>()
                for (doc in querySnapshot.documents) {
                    try {
                        val docData = doc.data
                        if (docData != null) {
                            val draft = ProjectDraft_fromMap(docData)
                            list.add(draft)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                if (list.isNotEmpty()) {
                    _draftsList.value = list
                    // If no active project, select first
                    _project.value = list[0]
                    showToast("Cloud Synchronized: Loaded ${list.size} cloud projects!")
                } else {
                    // Upload presets if user cloud is completely empty
                    uploadCurrentDraftsToCloud()
                }
            }
            .addOnFailureListener { e ->
                // fail silently, user might be offline or no firestore rule configured yet
            }
    }

    fun saveDraftToFirestore(draft: ProjectDraft) {
        val user = auth?.currentUser ?: return
        val dbRef = db ?: return
        
        dbRef.collection("users")
            .document(user.uid)
            .collection("drafts")
            .document(draft.id)
            .set(draft.toMap())
            .addOnSuccessListener {
                // Draft backed up
            }
            .addOnFailureListener {
                // Draft save error
            }
    }

    fun deleteDraftFromFirestore(draftId: String) {
        val user = auth?.currentUser ?: return
        val dbRef = db ?: return
        
        dbRef.collection("users")
            .document(user.uid)
            .collection("drafts")
            .document(draftId)
            .delete()
    }

    fun uploadCurrentDraftsToCloud() {
        val user = auth?.currentUser ?: return
        _draftsList.value.forEach {
            saveDraftToFirestore(it)
        }
    }

    // --- INTERACTIVE TUTORIAL GUIDE STATE ---
    var currentTutorialStep by mutableStateOf(0) // 0 means closed, 1..5 for sequential steps
    
    fun startTutorial() {
        currentTutorialStep = 1
        showToast("Welcome to CapCut Studio! Let's start the tour.")
    }

    fun nextTutorialStep() {
        if (currentTutorialStep in 1..4) {
            currentTutorialStep++
        } else {
            currentTutorialStep = 0
            showToast("Tour completed! You are ready to edit like a PRO.")
        }
    }

    fun dismissTutorial() {
        currentTutorialStep = 0
        showToast("Tutorial skipped")
    }

    fun showToastPublic(msg: String) {
        showToast(msg)
    }

    private var playbackJob: Job? = null

    init {
        // Preload standard cool preset projects for user to choose or edit
        val initialPresets = listOf(
            createTokyoMidnightPreset(),
            createCosmicOdysseyPreset(),
            createRetroVhsDraftPreset()
        )
        _draftsList.value = initialPresets
        // Set the active project to the first preset (Tokyo Cyberpunk) for immediate amazing wow factor!
        _project.value = initialPresets[0]

        // 1. Reactive loop to sync working active changes to local list & Firebase Firestore in real-time
        viewModelScope.launch {
            _project.collect { active ->
                _draftsList.value = _draftsList.value.map {
                    if (it.id == active.id) active else it
                }
                
                // Firestore Cloud synchronization
                val currentUser = auth?.currentUser
                if (currentUser != null) {
                    saveDraftToFirestore(active)
                }
            }
        }

        // 2. Continuous check if there is an existing signed-in Firebase session to resume
        try {
            val sessionUser = auth?.currentUser
            if (sessionUser != null) {
                userEmail = sessionUser.email
                userName = sessionUser.displayName ?: sessionUser.email?.substringBefore("@") ?: "Creator"
                isProMember = true // Grant premium PRO access immediately to logged-in Firebase users
                loadDraftsFromFirestore()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Starts or pauses the timeline playback loop
    fun togglePlay() {
        if (_isPlaying.value) {
            playbackJob?.cancel()
            _isPlaying.value = false
        } else {
            _isPlaying.value = true
            val totalDuration = calculateTotalDurationMs()
            if (_currentTimeMs.value >= totalDuration) {
                _currentTimeMs.value = 0L // Loop back to start
            }
            startPlaybackLoop()
        }
    }

    private fun startPlaybackLoop() {
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            var lastTime = System.currentTimeMillis()
            while (_isPlaying.value) {
                delay(16) // ~60fps refresh tick
                val now = System.currentTimeMillis()
                val delta = now - lastTime
                lastTime = now

                val totalDuration = calculateTotalDurationMs()
                if (totalDuration == 0L) {
                    _isPlaying.value = false
                    break
                }

                val nextTime = _currentTimeMs.value + delta
                if (nextTime >= totalDuration) {
                    _currentTimeMs.value = totalDuration
                    _isPlaying.value = false
                } else {
                    _currentTimeMs.value = nextTime
                }
            }
        }
    }

    fun seekTo(timeMs: Long) {
        val total = calculateTotalDurationMs()
        _currentTimeMs.value = timeMs.coerceIn(0L, maxOf(total, 1L))
    }

    fun selectProject(draft: ProjectDraft) {
        playbackJob?.cancel()
        _isPlaying.value = false
        _project.value = draft
        _currentTimeMs.value = 0L
        clearSelection()
        showHomeScreen = false
    }

    fun deleteProjectDraft(id: String) {
        val currentList = _draftsList.value
        val updatedList = currentList.filterNot { it.id == id }
        _draftsList.value = updatedList
        
        // Delete from Cloud Firestore if signed in
        if (auth?.currentUser != null) {
            deleteDraftFromFirestore(id)
        }
        
        // If the deleted project was active, auto-select another one or make a fresh one
        if (_project.value.id == id) {
            if (updatedList.isNotEmpty()) {
                _project.value = updatedList[0]
            } else {
                val fresh = createNewProject("My First CapCut Draft")
                _draftsList.value = listOf(fresh)
                _project.value = fresh
            }
        }
        showToast("Project draft deleted successfully")
    }

    fun createAndSelectNewProject(name: String) {
        val newProj = createNewProject(name)
        _draftsList.value = _draftsList.value + newProj
        selectProject(newProj)
    }

    fun changeAspectRatio(ratio: AspectRatioPreset) {
        _project.value = _project.value.copy(aspectRatio = ratio)
    }

    private fun clearSelection() {
        selectedClipId = null
        selectedEffectId = null
        selectedAudioId = null
        selectedTextId = null
        selectedStickerId = null
    }

    // App name tracking helper
    private fun generateId(): String = UUID.randomUUID().toString()

    fun calculateTotalDurationMs(): Long {
        return _project.value.clips.fold(0L) { acc, clip -> acc + clip.getEffectiveDurationMs() }
    }

    // Resolves what clip is playing at a relative timeline millisecond, plus gets local time
    fun getResolvedClipAt(timelineTimeMs: Long): ResolvedClipResult? {
        val clips = _project.value.clips
        var accStartMs = 0L
        for (clip in clips) {
            val clipTimelineDuration = clip.getEffectiveDurationMs()
            val accEndMs = accStartMs + clipTimelineDuration
            if (timelineTimeMs in accStartMs..accEndMs) {
                val offsetInClipMs = timelineTimeMs - accStartMs
                val localTimeInClipMs = clip.trimStartMs + (offsetInClipMs * clip.speed).toLong()
                return ResolvedClipResult(clip, accStartMs, localTimeInClipMs)
            }
            accStartMs = accEndMs
        }
        return clips.lastOrNull()?.let { ResolvedClipResult(it, accStartMs - it.getEffectiveDurationMs(), it.trimStartMs + it.durationMs) }
    }

    data class ResolvedClipResult(
        val clip: VideoClip,
        val timelineStartMs: Long,
        val localTimeInClipMs: Long
    )

    // Splitting active clip at current playhead position
    fun splitClipAtPlayhead() {
        val playhead = _currentTimeMs.value
        val resolved = getResolvedClipAt(playhead) ?: run {
            showToast("No clip found at playhead!")
            return
        }

        val originalClip = resolved.clip
        val offsetInsideTimelineClip = playhead - resolved.timelineStartMs
        
        // Ensure split point is not too close to the edges of the clip
        val splitMinMs = 200L
        val originalEffectiveDuration = originalClip.getEffectiveDurationMs()

        if (offsetInsideTimelineClip < splitMinMs || (originalEffectiveDuration - offsetInsideTimelineClip) < splitMinMs) {
            showToast("Can't split too close to clip boundary!")
            return
        }

        // Divide the clip!
        // Clip 1 takes from trimStart to trimStart + relative split boundary
        val localSplitMs = (offsetInsideTimelineClip * originalClip.speed).toLong()
        
        val clip1 = originalClip.copy(
            id = generateId(),
            name = "${originalClip.name} (Part 1)",
            durationMs = localSplitMs
        )

        // Clip 2 takes from trimStart + relative split onwards
        val clip2 = originalClip.copy(
            id = generateId(),
            name = "${originalClip.name} (Part 2)",
            trimStartMs = originalClip.trimStartMs + localSplitMs,
            durationMs = originalClip.durationMs - localSplitMs
        )

        // Reassemble the clip list with original replaced by clip1 and clip2
        val clips = _project.value.clips.toMutableList()
        val index = clips.indexOfFirst { it.id == originalClip.id }
        if (index != -1) {
            clips.removeAt(index)
            clips.add(index, clip1)
            clips.add(index + 1, clip2)
            _project.value = _project.value.copy(clips = clips)
            showToast("Split clip into two segments!")
            selectedClipId = clip1.id
        }
    }

    // Delete selected clip
    fun deleteSelectedClip() {
        val clipId = selectedClipId ?: return
        val clips = _project.value.clips.toMutableList()
        if (clips.size <= 1) {
            showToast("Timeline must have at least 1 video clip!")
            return
        }
        clips.removeAll { it.id == clipId }
        _project.value = _project.value.copy(clips = clips)
        selectedClipId = clips.firstOrNull()?.id
        showToast("Deleted clip segment")
        seekTo(0)
    }

    // Duplicate selected clip
    fun duplicateSelectedClip() {
        val clipId = selectedClipId ?: return
        val clips = _project.value.clips.toMutableList()
        val index = clips.indexOfFirst { it.id == clipId }
        if (index != -1) {
            val original = clips[index]
            val duplicate = original.copy(
                id = generateId(),
                name = "${original.name} Copy"
            )
            clips.add(index + 1, duplicate)
            _project.value = _project.value.copy(clips = clips)
            showToast("Duplicated clip segment")
            selectedClipId = duplicate.id
        }
    }

    // Add random new clip scene to end of timeline
    fun addNewClipToEnd(sceneType: SceneType) {
        val clips = _project.value.clips.toMutableList()
        val label = when (sceneType) {
            SceneType.NEON_STREET -> "Neon Cyber Grid"
            SceneType.COSMOS_NEBULA -> "Comet Nebula"
            SceneType.TOKYO_SYNTHWAVE -> "Synth Horizon"
            SceneType.GLITCH_MATRIX -> "Binary Rain"
            SceneType.CINEMATIC_SUNSET -> "Widescreen Dusk"
        }
        val newClip = VideoClip(
            id = generateId(),
            name = "$label ${clips.size + 1}",
            sceneType = sceneType,
            durationMs = 4000L, // 4 seconds duration
            sourceDurationMs = 10000L
        )
        clips.add(newClip)
        _project.value = _project.value.copy(clips = clips)
        selectedClipId = newClip.id
        showToast("Added $label clip to timeline")
    }

    // Update active color adjustment
    fun updateSelectedClipAdjustment(adjustments: ColorAdjustments) {
        val clipId = selectedClipId ?: return
        val clips = _project.value.clips.map {
            if (it.id == clipId) it.copy(adjustments = adjustments) else it
        }
        _project.value = _project.value.copy(clips = clips)
    }

    // Update clip speed parameter
    fun updateSelectedClipSpeed(speed: Float) {
        val clipId = selectedClipId ?: return
        val clips = _project.value.clips.map {
            if (it.id == clipId) it.copy(speed = speed) else it
        }
        _project.value = _project.value.copy(clips = clips)
    }

    // Update clip opacity parameter
    fun updateSelectedClipOpacity(opacity: Float) {
        val clipId = selectedClipId ?: return
        val clips = _project.value.clips.map {
            if (it.id == clipId) it.copy(opacity = opacity) else it
        }
        _project.value = _project.value.copy(clips = clips)
    }

    // Effects Manager: Adds effect block to timeline
    fun addEffectToTimeline(type: EffectType) {
        val startMs = _currentTimeMs.value
        val newEffect = EffectTrackItem(
            id = generateId(),
            type = type,
            timelineStartMs = startMs,
            durationMs = 2500L // 2.5 seconds block by default
        )
        val list = _project.value.effects + newEffect
        _project.value = _project.value.copy(effects = list)
        selectedEffectId = newEffect.id
        showToast("Applied ${type.label} block!")
    }

    fun deleteSelectedEffect() {
        val effectId = selectedEffectId ?: return
        val list = _project.value.effects.filterNot { it.id == effectId }
        _project.value = _project.value.copy(effects = list)
        selectedEffectId = null
        showToast("Removed effect block")
    }

    fun moveSelectedEffect(newStartMs: Long) {
        val id = selectedEffectId ?: return
        val total = calculateTotalDurationMs()
        val list = _project.value.effects.map {
            if (it.id == id) {
                val clampedStart = newStartMs.coerceIn(0L, maxOf(0L, total - it.durationMs))
                it.copy(timelineStartMs = clampedStart)
            } else it
        }
        _project.value = _project.value.copy(effects = list)
    }

    fun resizeSelectedEffect(newDurationMs: Long) {
        val id = selectedEffectId ?: return
        val clampedDuration = newDurationMs.coerceIn(500L, 10000L)
        val list = _project.value.effects.map {
            if (it.id == id) it.copy(durationMs = clampedDuration) else it
        }
        _project.value = _project.value.copy(effects = list)
    }

    // Audio Track Manager for sound overlays
    fun addAudioToTimeline(type: AudioType) {
        val startMs = _currentTimeMs.value
        val isSfx = type.ordinal >= 4
        val duration = if (isSfx) 1500L else 5000L // SFX are short sounds, loops are 5s
        val newAudio = AudioTrackItem(
            id = generateId(),
            type = type,
            timelineStartMs = startMs,
            durationMs = duration,
            volume = 0.8f
        )
        val list = _project.value.audios + newAudio
        _project.value = _project.value.copy(audios = list)
        selectedAudioId = newAudio.id
        showToast("Added ${type.label} sound track")
    }

    fun deleteSelectedAudio() {
        val audioId = selectedAudioId ?: return
        val list = _project.value.audios.filterNot { it.id == audioId }
        _project.value = _project.value.copy(audios = list)
        selectedAudioId = null
        showToast("Deleted audio component")
    }

    fun moveSelectedAudio(newStartMs: Long) {
        val id = selectedAudioId ?: return
        val total = calculateTotalDurationMs()
        val list = _project.value.audios.map {
            if (it.id == id) {
                val clamped = newStartMs.coerceIn(0L, maxOf(0L, total - it.durationMs))
                it.copy(timelineStartMs = clamped)
            } else it
        }
        _project.value = _project.value.copy(audios = list)
    }

    fun updateAudioVolume(vol: Float) {
        val id = selectedAudioId ?: return
        val list = _project.value.audios.map {
            if (it.id == id) it.copy(volume = vol) else it
        }
        _project.value = _project.value.copy(audios = list)
    }

    // --- ADVANCED AI AUDIO & SPEECH FUNCTIONS ---

    // 1. VOICE CHANGER: Applies a preset vocal modulation to the active sound segment
    fun updateAudioVoiceEffect(effect: String?) {
        val id = selectedAudioId ?: return
        val list = _project.value.audios.map {
            if (it.id == id) it.copy(voiceChangerEffect = effect) else it
        }
        _project.value = _project.value.copy(audios = list)
        if (effect != null) {
            showToast("Vocal voice effect set: $effect")
        } else {
            showToast("Cleared voice filter")
        }
    }

    // AI speech task processing indicator states
    var isAiSpeechGenerating by mutableStateOf(false)
    var isAiTranscribing by mutableStateOf(false)

    // 2. TEXT TO SPEECH (TTS): Synthesizes audio clips from custom strings with a digital vocal model
    fun generateTextToSpeech(text: String, voiceName: String) {
        if (text.isBlank()) {
            showToast("Please enter text for synthesis!")
            return
        }
        isAiSpeechGenerating = true
        showToast("Generating neural voice synthesized audio...")
        
        viewModelScope.launch {
            delay(1600) // Mimic complex wave synthesis
            val startMs = _currentTimeMs.value
            val simulatedDuration = (text.length * 85L + 1200L).coerceIn(1500L, 8000L)
            
            val newTtsTrack = AudioTrackItem(
                id = generateId(),
                type = AudioType.TTS_GENERATED,
                timelineStartMs = startMs,
                durationMs = simulatedDuration,
                volume = 1.0f,
                isTts = true,
                ttsText = text,
                ttsVoiceName = voiceName
            )
            
            val list = _project.value.audios + newTtsTrack
            _project.value = _project.value.copy(audios = list)
            selectedAudioId = newTtsTrack.id
            isAiSpeechGenerating = false
            showToast("AI TTS Voice added to playhead timeline!")
        }
    }

    // 3. SPEECH TO TEXT TRANSCRIBER: Auto parses tracks to create timeline captions!
    fun transcribeSelectedAudio() {
        val id = selectedAudioId ?: return
        val currentAudio = _project.value.audios.find { it.id == id } ?: return
        
        isAiTranscribing = true
        showToast("AI Transcriber is analyzing vocal frequencies...")
        
        viewModelScope.launch {
            delay(2000) // Simulated complex speech model analyzer
            
            // Form appropriate high caliber transcripts
            val textContent = when {
                currentAudio.isTts && !currentAudio.ttsText.isNullOrEmpty() -> currentAudio.ttsText ?: ""
                currentAudio.type == AudioType.NEON_GRID -> "Initializing Cyber Core. Connection established. Tokyo Grid is live."
                currentAudio.type == AudioType.MIDNIGHT_LOFI -> "Relax your mind. Feel the vintage chill vibe flowing."
                currentAudio.type == AudioType.CYBERPUNK_BEAT -> "Rebel forces alert. Grid lock active. Speed up the pace."
                currentAudio.type == AudioType.CINEMATIC_RISE -> "In a world of neon lights and space orbits, a star is born."
                else -> "Recording high fidelity viral content with CapCut Studio PRO edit."
            }

            val segmentLengthMs = 2800L
            val words = textContent.split(" ")
            val createdCaptions = mutableListOf<TextTrackItem>()
            
            if (words.size <= 4) {
                createdCaptions.add(
                    TextTrackItem(
                        id = generateId(),
                        text = textContent,
                        timelineStartMs = currentAudio.timelineStartMs,
                        durationMs = currentAudio.durationMs.coerceAtMost(3200L),
                        color = Color(0xFF00FFE0),
                        preset = TextStylePreset.NEON_CYAN,
                        scale = 1.2f,
                        posY = 0.85f
                    )
                )
            } else {
                val chunks = words.chunked(3)
                chunks.forEachIndexed { idx, chunk ->
                    val startOffset = idx * segmentLengthMs
                    if (startOffset < currentAudio.durationMs) {
                        createdCaptions.add(
                            TextTrackItem(
                                id = generateId(),
                                text = chunk.joinToString(" "),
                                timelineStartMs = currentAudio.timelineStartMs + startOffset,
                                durationMs = segmentLengthMs.coerceAtMost(currentAudio.durationMs - startOffset),
                                color = Color(0xFFFFCC00),
                                preset = TextStylePreset.RETRO_AMBER,
                                scale = 1.15f,
                                posY = 0.85f
                            )
                        )
                    }
                }
            }

            // Apply analyzed state properties back to the model list
            val updatedAudios = _project.value.audios.map {
                if (it.id == id) it.copy(isTranscribed = true, transcription = textContent) else it
            }
            val updatedTexts = _project.value.texts + createdCaptions

            _project.value = _project.value.copy(
                audios = updatedAudios,
                texts = updatedTexts
            )
            
            isAiTranscribing = false
            showToast("Transcribed! Synced ${createdCaptions.size} subtitles to timeline.")
        }
    }

    // --- AI PROMPT-TO-EFFECT VECTOR GENERATOR ---
    var aiEffectPrompt by mutableStateOf("")
    var isGeneratingAiEffect by mutableStateOf(false)

    fun generateAiVisualEffect(promptText: String) {
        if (promptText.isBlank()) {
            showToast("Please write a vision style prompt first!")
            return
        }
        isGeneratingAiEffect = true
        showToast("Synthesizing prompt into procedural neural layers...")
        
        viewModelScope.launch {
            delay(1800) // Simulated complex generative model computation
            
            // Analyze the aesthetic keywords to map to actual outstanding canvas shaders!
            val targetEffect = when {
                promptText.contains("scan", ignoreCase = true) || promptText.contains("laser", ignoreCase = true) || promptText.contains("line", ignoreCase = true) -> {
                    EffectType.LASER_SCAN
                }
                promptText.contains("neon", ignoreCase = true) || promptText.contains("glow", ignoreCase = true) || promptText.contains("edge", ignoreCase = true) || promptText.contains("border", ignoreCase = true) -> {
                    EffectType.NEON_EDGE
                }
                promptText.contains("matrix", ignoreCase = true) || promptText.contains("code", ignoreCase = true) || promptText.contains("dissolve", ignoreCase = true) || promptText.contains("disintegrate", ignoreCase = true) || promptText.contains("rain", ignoreCase = true) -> {
                    EffectType.DISINTEGRATE
                }
                promptText.contains("mirror", ignoreCase = true) || promptText.contains("scope", ignoreCase = true) || promptText.contains("kaleido", ignoreCase = true) || promptText.contains("symmetric", ignoreCase = true) -> {
                    EffectType.KALEIDOSCOPE
                }
                else -> {
                    // Fallback to choosing a randomized core style from the entire array
                    EffectType.values().random()
                }
            }
            
            addEffectToTimeline(targetEffect)
            isGeneratingAiEffect = false
            aiEffectPrompt = "" // Reset
            showToast("AI Effect Compiled: Applied ${targetEffect.label}!")
        }
    }

    // Text & Subtitles Manager
    fun addTextToTimeline(string: String) {
        if (string.isBlank()) return
        val startMs = _currentTimeMs.value
        val newText = TextTrackItem(
            id = generateId(),
            text = string,
            timelineStartMs = startMs,
            durationMs = 3000L, // 3 seconds overlay
            color = Color.White,
            preset = TextStylePreset.OUTLINE_WHITE
        )
        val list = _project.value.texts + newText
        _project.value = _project.value.copy(texts = list)
        selectedTextId = newText.id
        showToast("Added text overlay: \"$string\"")
    }

    fun updateSelectedTextContent(content: String) {
        val id = selectedTextId ?: return
        val list = _project.value.texts.map {
            if (it.id == id) it.copy(text = content) else it
        }
        _project.value = _project.value.copy(texts = list)
    }

    fun updateSelectedTextColor(col: Color) {
        val id = selectedTextId ?: return
        val list = _project.value.texts.map {
            if (it.id == id) it.copy(color = col) else it
        }
        _project.value = _project.value.copy(texts = list)
    }

    fun updateSelectedTextPreset(preset: TextStylePreset) {
        val id = selectedTextId ?: return
        val list = _project.value.texts.map {
            if (it.id == id) it.copy(preset = preset) else it
        }
        _project.value = _project.value.copy(texts = list)
    }

    fun moveSelectedText(newStartMs: Long) {
        val id = selectedTextId ?: return
        val total = calculateTotalDurationMs()
        val list = _project.value.texts.map {
            if (it.id == id) {
                val clamped = newStartMs.coerceIn(0L, maxOf(0L, total - it.durationMs))
                it.copy(timelineStartMs = clamped)
            } else it
        }
        _project.value = _project.value.copy(texts = list)
    }

    fun deleteSelectedText() {
        val textId = selectedTextId ?: return
        val list = _project.value.texts.filterNot { it.id == textId }
        _project.value = _project.value.copy(texts = list)
        selectedTextId = null
        showToast("Deleted text block")
    }

    // Stickers Manager
    fun addStickerToTimeline(type: StickerType) {
        val startMs = _currentTimeMs.value
        val newSticker = StickerTrackItem(
            id = generateId(),
            type = type,
            timelineStartMs = startMs,
            durationMs = 3000L // 3s
        )
        val list = _project.value.stickers + newSticker
        _project.value = _project.value.copy(stickers = list)
        selectedStickerId = newSticker.id
        showToast("Applied ${type.label} overlay sticker!")
    }

    fun addCustomStickerToTimeline(type: StickerType, emoji: String, label: String) {
        val startMs = _currentTimeMs.value
        val newSticker = StickerTrackItem(
            id = generateId(),
            type = type,
            timelineStartMs = startMs,
            durationMs = 3000L,
            customEmoji = emoji,
            customLabel = label
        )
        val list = _project.value.stickers + newSticker
        _project.value = _project.value.copy(stickers = list)
        selectedStickerId = newSticker.id
        showToast("Added sticker icon: $emoji $label")
    }

    fun deleteSelectedSticker() {
        val stickerId = selectedStickerId ?: return
        val list = _project.value.stickers.filterNot { it.id == stickerId }
        _project.value = _project.value.copy(stickers = list)
        selectedStickerId = null
        showToast("Removed sticker overlay")
    }

    fun moveSelectedSticker(newStartMs: Long) {
        val id = selectedStickerId ?: return
        val total = calculateTotalDurationMs()
        val list = _project.value.stickers.map {
            if (it.id == id) {
                val clamped = newStartMs.coerceIn(0L, maxOf(0L, total - it.durationMs))
                it.copy(timelineStartMs = clamped)
            } else it
        }
        _project.value = _project.value.copy(stickers = list)
    }

    // Helper to display a neat toast notification
    private fun showToast(msg: String) {
        toastMessage = msg
        viewModelScope.launch {
            delay(2200)
            if (toastMessage == msg) {
                toastMessage = null
            }
        }
    }

    // Full export render simulator
    fun triggerExport() {
        playbackJob?.cancel()
        _isPlaying.value = false
        isExporting = true
        exportProgress = 0f
        exportDone = false

        viewModelScope.launch {
            val segments = 25
            for (i in 0..segments) {
                delay(120) // Encode delay ticks
                exportProgress = i.toFloat() / segments
                // Scrub playhead forward to visually simulate actual frames baking!
                val simTime = (exportProgress * calculateTotalDurationMs()).toLong()
                _currentTimeMs.value = simTime
            }
            delay(500)
            exportDone = true
        }
    }

    fun cancelExport() {
        isExporting = false
        exportProgress = 0f
        exportDone = false
        seekTo(0)
    }

    // Initializer Factory templates
    private fun createNewProject(title: String): ProjectDraft {
        val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
        return ProjectDraft(
            id = generateId(),
            name = title,
            lastModified = sdf.format(Date()),
            clips = listOf(
                VideoClip(generateId(), "Intro Scene", SceneType.TOKYO_SYNTHWAVE, 3000L),
                VideoClip(generateId(), "Outro Scene", SceneType.CINEMATIC_SUNSET, 3000L)
            ),
            effects = emptyList(),
            audios = emptyList(),
            texts = emptyList(),
            stickers = emptyList()
        )
    }

    private fun createTokyoMidnightPreset(): ProjectDraft {
        val introId = generateId()
        val matrixId = generateId()
        val retroId = generateId()
        return ProjectDraft(
            id = generateId(),
            name = "🗼 Tokyo Cyberwave 4K",
            lastModified = "Just Now",
            clips = listOf(
                VideoClip(introId, "Neon Tokyo Grid", SceneType.TOKYO_SYNTHWAVE, 4200L, speed = 1.0f),
                VideoClip(matrixId, "Matrix Core", SceneType.GLITCH_MATRIX, 3500L, speed = 1.25f, adjustments = ColorAdjustments(saturation = 1.3f)),
                VideoClip(retroId, "Cyber Synth Street", SceneType.NEON_STREET, 4500L, adjustments = ColorAdjustments(vignette = 0.5f))
            ),
            effects = listOf(
                EffectTrackItem(generateId(), EffectType.GLITCH, 3800L, 2000L),
                EffectTrackItem(generateId(), EffectType.RGB_STROBE, 4200L, 1000L),
                EffectTrackItem(generateId(), EffectType.SHAKE_DISTORT, 3000L, 2500L),
                EffectTrackItem(generateId(), EffectType.CINEMATIC_CROP, 0L, 12200L)
            ),
            audios = listOf(
                AudioTrackItem(generateId(), AudioType.NEON_GRID, 0L, 12200L, volume = 0.9f)
            ),
            texts = listOf(
                TextTrackItem(generateId(), "TOKYO 2099", 500L, 3000L, Color(0xFF00FFE0), TextStylePreset.NEON_CYAN, scale = 1.4f, posX = 0.5f, posY = 0.4f),
                TextTrackItem(generateId(), "COMPILING DATA...", 4500L, 2500L, Color(0xFF00FF55), TextStylePreset.LCD_GREEN, scale = 1.0f, posX = 0.5f, posY = 0.75f),
                TextTrackItem(generateId(), "THE END", 9500L, 2000L, Color.White, TextStylePreset.OUTLINE_WHITE, scale = 1.6f, posX = 0.5f, posY = 0.5f)
            ),
            stickers = listOf(
                StickerTrackItem(generateId(), StickerType.VHS_PLAY_ICON, 100L, 2500L, 0.7f, 0.2f, 0.15f),
                StickerTrackItem(generateId(), StickerType.GLITCH_CAM, 0L, 12200L, 1.0f, 0.5f, 0.5f)
            ),
            aspectRatio = AspectRatioPreset.TIKTOK_9_16
        )
    }

    private fun createCosmicOdysseyPreset(): ProjectDraft {
        val scene1 = generateId()
        val scene2 = generateId()
        return ProjectDraft(
            id = generateId(),
            name = "🪐 Deep Cosmic Orbit",
            lastModified = "10 mins ago",
            clips = listOf(
                VideoClip(scene1, "Gravity Nebula Shift", SceneType.COSMOS_NEBULA, 6000L),
                VideoClip(scene2, "Event Horizon sunset", SceneType.CINEMATIC_SUNSET, 5000L, adjustments = ColorAdjustments(temperature = 0.4f, brightness = 0.08f))
            ),
            effects = listOf(
                EffectTrackItem(generateId(), EffectType.RADIAL_GLOW, 0L, 6000L),
                EffectTrackItem(generateId(), EffectType.RAINBOW_BOKEH, 2000L, 4000L),
                EffectTrackItem(generateId(), EffectType.ZOOM_BLUR, 5500L, 1500L)
            ),
            audios = listOf(
                AudioTrackItem(generateId(), AudioType.CINEMATIC_RISE, 0L, 11000L, volume = 0.85f),
                AudioTrackItem(generateId(), AudioType.SPARKLE_SFX, 5800L, 1500L)
            ),
            texts = listOf(
                TextTrackItem(generateId(), "ASTRAL DRIFT", 1500L, 4000L, Color(0xFFFFD700), TextStylePreset.VINTAGE_GOLD, scale = 1.3f)
            ),
            stickers = emptyList(),
            aspectRatio = AspectRatioPreset.YOUTUBE_16_9
        )
    }

    private fun createRetroVhsDraftPreset(): ProjectDraft {
        val scene1 = generateId()
        return ProjectDraft(
            id = generateId(),
            name = "📼 1996 Skate Tape",
            lastModified = "Yesterday",
            clips = listOf(
                VideoClip(scene1, "VHS Street Cruise", SceneType.NEON_STREET, 8000L, adjustments = ColorAdjustments(brightness = -0.05f, contrast = 0.85f, temperature = 0.25f))
            ),
            effects = listOf(
                EffectTrackItem(generateId(), EffectType.VHS, 0L, 8000L),
                EffectTrackItem(generateId(), EffectType.SHAKE_DISTORT, 1500L, 3000L)
            ),
            audios = listOf(
                AudioTrackItem(generateId(), AudioType.MIDNIGHT_LOFI, 0L, 8000L, volume = 0.95f),
                AudioTrackItem(generateId(), AudioType.REWIND_SFX, 0L, 1000L)
            ),
            texts = listOf(
                TextTrackItem(generateId(), "CRUISIN'", 1000L, 4500L, Color(0xFFFFA500), TextStylePreset.RETRO_AMBER, scale = 1.2f)
            ),
            stickers = listOf(
                StickerTrackItem(generateId(), StickerType.VHS_PLAY_ICON, 0L, 8000L, 0.8f, 0.2f, 0.15f)
            ),
            aspectRatio = AspectRatioPreset.TIKTOK_9_16
        )
    }

    // --- PRO MODERATION & HIGH-END INTEGRATED AI CAPABILITIES ---

    var selectedVoiceActor by mutableStateOf("Aria Dusk (Warm Storyteller)")
    val voiceActorsList = listOf(
        "Aria Dusk (Warm Storyteller)",
        "Marcus Prime (Deep Cinema)",
        "Synth Nova (Cyberpunk Synth)",
        "Chloe Breeze (Energetic Vlogger)"
    )

    // Image Analyzer States
    var activeAnalysisClipId by mutableStateOf<String?>(null)
    var isAnalyzingImage by mutableStateOf(false)
    var imageAnalysisResult by mutableStateOf<ImageAnalysis?>(null)

    fun runImageAnalysis(clip: VideoClip) {
        activeAnalysisClipId = clip.id
        isAnalyzingImage = true
        imageAnalysisResult = null
        viewModelScope.launch {
            delay(1500)
            val brightness = when(clip.sceneType) {
                SceneType.CINEMATIC_SUNSET -> "78% (Perfect HDR Golden Hour exposure)"
                SceneType.COSMOS_NEBULA -> "32% (Moody low-key space spectrum)"
                SceneType.GLITCH_MATRIX -> "54% (Balanced industrial matrix levels)"
                else -> "65% (Optimal daylight calibration)"
            }
            val balance = when(clip.sceneType) {
                SceneType.CINEMATIC_SUNSET -> "89% Symmetry (Rule of thirds center aligned)"
                SceneType.NEON_STREET -> "94% Linear (Dynamic cyberpunk lead lines)"
                else -> "91% Composition (Balanced foreground subject)"
            }
            val colors = when(clip.sceneType) {
                SceneType.CINEMATIC_SUNSET -> listOf("Sunset Amber (50%)", "M3 Coral Orange (30%)", "Soft Dusk Lavender (20%)")
                SceneType.TOKYO_SYNTHWAVE -> listOf("Cyberpunk Cyan (45%)", "Acid Magenta (40%)", "Deep Purple Shadow (15%)")
                SceneType.GLITCH_MATRIX -> listOf("Matrix Green (75%)", "Terminal Slate (20%)", "White Code Glow (5%)")
                else -> listOf("Classic Electric Cyan (60%)", "Slate Dark Accent (30%)", "Signal Orange (10%)")
            }
            val recommendation = when(clip.sceneType) {
                SceneType.CINEMATIC_SUNSET -> "Apply our 'Cinematic Crop' for wide-angle scope, and match with the 'Dynamic Shutter SFX' on start."
                SceneType.NEON_STREET -> "Activate the 'VHS' effect immediately or layer the 'Laser Scan' shader to amplify retro synthwaves."
                else -> "Combine with a custom styled neon title and use the 'Radial Glow' to saturate shadows beautifully."
            }
            val desc = when(clip.sceneType) {
                SceneType.COSMOS_NEBULA -> "A galactic exploration frame displaying dense star nurseries and volumetric stellar dust trails."
                SceneType.TOKYO_SYNTHWAVE -> "High density cityscape detailing vibrant towers, glass reflections, and nostalgic 80s arcade aesthetics."
                else -> "A visual composition demonstrating professional lighting contrast, strong focal symmetry, and deep atmospheric depth."
            }

            imageAnalysisResult = ImageAnalysis(
                assetName = clip.name,
                brightnessStr = brightness,
                balanceStr = balance,
                colorsList = colors,
                recommendation = recommendation,
                aestheticDesc = desc
            )
            isAnalyzingImage = false
            showToast("AI Vision analysis completed successfully!")
        }
    }

    // Audio Extractions
    fun extractAudioFromClip(clip: VideoClip) {
        var startMs = 0L
        for (item in _project.value.clips) {
            if (item.id == clip.id) break
            startMs += item.getEffectiveDurationMs()
        }
        val duration = clip.getEffectiveDurationMs()
        
        val newExtractedAudio = AudioTrackItem(
            id = generateId(),
            type = AudioType.EXTRACTED_AUDIO,
            timelineStartMs = startMs,
            durationMs = duration,
            volume = 0.90f,
            ttsText = "Extracted sound stream from ${clip.name}"
        )
        
        val list = _project.value.audios + newExtractedAudio
        _project.value = _project.value.copy(audios = list)
        selectedAudioId = newExtractedAudio.id
        showToast("Extracted native soundtrack from ${clip.name}!")
    }

    // AI Animated Video Generator States
    var aiVideoPrompt by mutableStateOf("")
    var isGeneratingAiVideo by mutableStateOf(false)
    var aiVideoGenerationProgress by mutableStateOf(0f)

    fun generateAiAnimatedVideo(prompt: String) {
        if (prompt.isBlank()) {
            showToast("Please write down what you want to animate!")
            return
        }
        isGeneratingAiVideo = true
        aiVideoGenerationProgress = 0f
        
        viewModelScope.launch {
            for (p in 1..10) {
                delay(200)
                aiVideoGenerationProgress = p / 10f
            }
            
            val randomScene = SceneType.values().random()
            val newClipId = generateId()
            val newClip = VideoClip(
                id = newClipId,
                name = "AI Gen: $prompt",
                sceneType = randomScene,
                durationMs = 4000L,
                sourceDurationMs = 4000L,
                speed = 1.0f,
                opacity = 1.0f
            )
            
            val list = _project.value.clips + newClip
            _project.value = _project.value.copy(clips = list)
            selectedClipId = newClipId
            
            isGeneratingAiVideo = false
            aiVideoGenerationProgress = 1f
            aiVideoPrompt = ""
            showToast("AI Animated Video compiled successfully and placed on timeline!")
        }
    }
}
