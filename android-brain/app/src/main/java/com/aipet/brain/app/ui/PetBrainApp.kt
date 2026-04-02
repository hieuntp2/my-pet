package com.aipet.brain.app.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Base64
import android.util.Log
import com.aipet.brain.app.BuildConfig
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.room.Room
import com.aipet.brain.app.audio.AudioCaptureLifecycleEventPublisher
import com.aipet.brain.app.audio.AudioResponseDispatcher
import com.aipet.brain.app.audio.LocalAudioIntentCommandRule
import com.aipet.brain.app.animation.DefaultPetAnimatorFactory
import com.aipet.brain.app.animation.PetAnimationInputMapper
import com.aipet.brain.app.animation.PetAnimationSource
import com.aipet.brain.app.animation.PetAnimator
import com.aipet.brain.app.behavior.experience.BehaviorExperienceBinder
import com.aipet.brain.app.behavior.experience.BehaviorExperienceDebugState
import com.aipet.brain.app.behavior.experience.ChannelDecision
import com.aipet.brain.app.behavior.experience.ChannelExecutionResult
import com.aipet.brain.app.behavior.experience.PetIntentionExecutor
import com.aipet.brain.app.behavior.experience.TalkDirective
import com.aipet.brain.app.gameplay.PetGameplayAction
import com.aipet.brain.app.gameplay.PetGameplayAudioMapper
import com.aipet.brain.app.gameplay.PetGameplayCooldownGate
import com.aipet.brain.app.gameplay.PetGameplayFeedback
import com.aipet.brain.app.gameplay.PetGameplayFeedbackFormatter
import com.aipet.brain.app.onboarding.PetNamingOnboardingScreen
import com.aipet.brain.app.onboarding.PetOnboardingStore
import com.aipet.brain.app.reactions.OwnerSeenReactionEngine
import com.aipet.brain.app.reactions.PersonSeenEventPublisher
import com.aipet.brain.app.ui.audio.AudioDebugScreen
import com.aipet.brain.app.ui.audio.AudioPlaybackEngine
import com.aipet.brain.app.ui.camera.CameraScreen
import com.aipet.brain.app.ui.debug.AvatarAnimationDebugScreen
import com.aipet.brain.app.ui.debug.BehaviorIntelligenceDebugScreen
import com.aipet.brain.brain.attention.AttentionArbitrator
import com.aipet.brain.brain.attention.AttentionEngine
import com.aipet.brain.brain.attention.AttentionFatigueTracker
import com.aipet.brain.brain.attention.AttentionModeResolver
import com.aipet.brain.brain.attention.AttentionStateRepository
import com.aipet.brain.brain.attention.AttentionTargetEvaluator
import com.aipet.brain.brain.b2.BehaviorEngine
import com.aipet.brain.brain.b2.BehaviorPlanner
import com.aipet.brain.brain.b2.CooldownTracker
import com.aipet.brain.brain.b2.EmotionMomentumEngine
import com.aipet.brain.brain.b2.IntentionArbitrator
import com.aipet.brain.brain.b2.IntentionScorer
import com.aipet.brain.brain.b2.RelationshipStateBuilder
import com.aipet.brain.brain.b2.domain.RecentMemorySummary
import com.aipet.brain.brain.b2.domain.SessionContext
import com.aipet.brain.brain.fusion.AudioInterpreter
import com.aipet.brain.brain.fusion.InMemoryPerceptionFusionRepository
import com.aipet.brain.brain.fusion.PerceptionFusionCoordinator
import com.aipet.brain.brain.fusion.PresenceInterpreter
import com.aipet.brain.brain.fusion.TouchInterpreter
import com.aipet.brain.brain.fusion.VoiceCommandType
import com.aipet.brain.brain.fusion.VoiceInterpreter
import com.aipet.brain.brain.attention.SalienceScorer
import com.aipet.brain.brain.b2.domain.BehaviorPlan
import com.aipet.brain.brain.b2.WorkingContextBuilder
import com.aipet.brain.brain.fusion.PerceptionFusionSnapshot
import kotlinx.coroutines.flow.collectLatest
import com.aipet.brain.app.ui.debug.DebugScreen
import com.aipet.brain.app.ui.debug.FaceAutoEnrollDebugScreen
import com.aipet.brain.app.ui.diary.DiaryScreen
import com.aipet.brain.app.ui.debug.EventViewerScreen
import com.aipet.brain.app.ui.debug.ObservationViewerScreen
import com.aipet.brain.app.ui.debug.EvolutionSystemDebugScreen
import com.aipet.brain.app.ui.debug.WorkingMemoryDebugScreen
import com.aipet.brain.app.ui.home.ActivityReactionPresentationMapper
import com.aipet.brain.app.ui.home.GreetingPresentationMapper
import com.aipet.brain.app.ui.home.HomeInteractionUiState
import com.aipet.brain.app.ui.home.TapReactionPresentationMapper
import com.aipet.brain.app.ui.home.HomeKnownEntityCount
import com.aipet.brain.app.ui.home.HomeScreen
import com.aipet.brain.app.ui.home.HomeTodaySummaryResolver
import com.aipet.brain.app.ui.home.HomeUiModelBuilder
import com.aipet.brain.app.avatar.RealPixelPetBridgeStateAdapter
import com.aipet.brain.brain.activity.FeedPetUseCase
import com.aipet.brain.brain.activity.LetPetRestUseCase
import com.aipet.brain.brain.activity.PetActivityResult
import com.aipet.brain.brain.activity.PetActivityUseCase
import com.aipet.brain.brain.activity.PetActivityType
import com.aipet.brain.brain.activity.PlayWithPetUseCase
import com.aipet.brain.app.ui.persons.PersonDetailScreen
import com.aipet.brain.app.ui.persons.PersonEditorScreen
import com.aipet.brain.app.ui.persons.PersonsScreen
import com.aipet.brain.app.ui.persons.TeachPersonCapturedObservation
import com.aipet.brain.app.ui.persons.TeachSampleFaceCropExtractor
import com.aipet.brain.app.ui.persons.FaceCropExtractionResult
import com.aipet.brain.app.ui.persons.TeachSampleImageStorage
import com.aipet.brain.app.ui.persons.TeachPersonSaveController
import com.aipet.brain.app.ui.persons.TeachPersonScreen
import com.aipet.brain.app.ui.profiles.ProfileAssociationsScreen
import com.aipet.brain.app.ui.settings.SettingsScreen
import com.aipet.brain.app.ui.traits.TraitsScreen
import com.aipet.brain.app.settings.CameraSelection
import com.aipet.brain.app.settings.CameraSelectionStore
import com.aipet.brain.app.settings.KeywordSpottingConfigStore
import com.aipet.brain.app.settings.PetSoundSettingsStore
import com.aipet.brain.brain.events.CameraFrameReceivedPayload
import com.aipet.brain.brain.events.CandidatePersonReadyForTeachPayload
import com.aipet.brain.brain.events.CareActionAppliedPayload
import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.events.EventType
import com.aipet.brain.brain.events.InMemoryEventBus
import com.aipet.brain.brain.events.ObjectDetectedEventPayload
import com.aipet.brain.brain.events.PetActivityAppliedEventPayload
import com.aipet.brain.brain.events.PetGreetedEventPayload
import com.aipet.brain.brain.events.UserInteractedPetEventPayload
import com.aipet.brain.brain.events.UserTaughtPersonEventPayload
import com.aipet.brain.brain.events.audio.AudioResponseRequestPayload
import com.aipet.brain.brain.events.audio.AudioIntent
import com.aipet.brain.brain.events.vision.FaceBoundingBoxPayload
import com.aipet.brain.brain.events.vision.FacesDetectedEventPayload
import com.aipet.brain.brain.interaction.PetInteractionStateReducer
import com.aipet.brain.brain.interaction.PetInteractionType
import com.aipet.brain.brain.logic.audio.AudioResponseRequestEmitter
import com.aipet.brain.brain.logic.audio.AudioResponseRequestInput
import com.aipet.brain.brain.logic.audio.AudioStimulusObserver
import com.aipet.brain.brain.logic.audio.LoudSoundReactionRule
import com.aipet.brain.brain.logic.audio.VoiceActivityAcknowledgmentRule
import com.aipet.brain.brain.logic.audio.WakeWordAcknowledgmentRule
import com.aipet.brain.brain.logic.intent.KeywordIntentCommandRule
import com.aipet.brain.brain.behavior.PetBehaviorDecision
import com.aipet.brain.brain.behavior.PetBehaviorContext
import com.aipet.brain.brain.behavior.PetBehaviorWeightResolver
import com.aipet.brain.brain.logic.intent.KeywordIntentMapper
import com.aipet.brain.brain.pet.PetConditionResolver
import com.aipet.brain.brain.pet.CareActionResult
import com.aipet.brain.brain.pet.CareActionType
import com.aipet.brain.brain.pet.PetDayBoundaryResolver
import com.aipet.brain.brain.pet.PetDayBoundaryType
import com.aipet.brain.brain.pet.PetEmotion
import com.aipet.brain.brain.pet.PetEmotionResolver
import com.aipet.brain.brain.pet.PetGreetingReaction
import com.aipet.brain.brain.pet.PetCondition
import com.aipet.brain.brain.pet.PetGreetingResolver
import com.aipet.brain.brain.pet.PetProfile
import com.aipet.brain.brain.personality.PetPersonalitySummaryResolver
import com.aipet.brain.brain.personality.PetTraitEvolutionEngine
import com.aipet.brain.brain.personality.PetTraitRepository
import com.aipet.brain.brain.pet.PetProfileRepository
import com.aipet.brain.brain.personality.PetTrait
import com.aipet.brain.brain.pet.PetState
import com.aipet.brain.brain.pet.PetStateDecayEngine
import com.aipet.brain.brain.pet.PetStateRepository
import com.aipet.brain.brain.memory.WorkingMemoryStore
import com.aipet.brain.brain.memory.WorkingMemoryUpdater
import com.aipet.brain.brain.observations.ObservationRecorder
import com.aipet.brain.brain.observations.ObservationSource
import com.aipet.brain.brain.relationship.FamiliarityEngine
import com.aipet.brain.brain.relationship.FamiliarityIncreaseResult
import com.aipet.brain.brain.relationship.FamiliarityStore
import com.aipet.brain.brain.recognition.PersonRecognitionService
import com.aipet.brain.brain.recognition.RecognitionDecisionEventPublisher
import com.aipet.brain.brain.recognition.RecognitionMemoryStatsUpdate
import com.aipet.brain.brain.recognition.RecognitionMemoryStatsUpdater
import com.aipet.brain.brain.state.BrainInteractionLoop
import com.aipet.brain.brain.state.BrainStateStore
import com.aipet.brain.brain.traits.TraitsEngine
import com.aipet.brain.memory.db.AppDatabase
import com.aipet.brain.memory.events.EventStore
import com.aipet.brain.memory.daily.EventDrivenDailySummaryGenerator
import com.aipet.brain.memory.events.RoomEventStore
import com.aipet.brain.memory.memorycards.EventToMemoryCardMapper
import com.aipet.brain.memory.objects.ObjectRepository
import com.aipet.brain.memory.persons.PersonStore
import com.aipet.brain.memory.personality.RoomPetTraitStore
import com.aipet.brain.memory.pet.RoomPetProfileStore
import com.aipet.brain.memory.pet.RoomPetStateStore
import com.aipet.brain.memory.persons.RoomPersonStore
import com.aipet.brain.memory.profiles.FaceProfileStore
import com.aipet.brain.memory.profiles.RoomFaceProfileStore
import com.aipet.brain.memory.recognition.RoomKnownPersonEmbeddingsSource
import com.aipet.brain.memory.teachsessioncompletion.RoomTeachSessionCompletionStore
import com.aipet.brain.memory.teachsessioncompletion.TeachSessionCompletionStore
import com.aipet.brain.memory.teachsamples.RoomTeachSampleStore
import com.aipet.brain.memory.teachsamples.TeachSampleStore
import com.aipet.brain.memory.traits.RoomTraitsSnapshotRepository
import com.aipet.brain.memory.unknownfaces.RoomUnknownFaceCandidateStore
import com.aipet.brain.memory.unknownfaces.UnknownFaceCandidateStore
import com.aipet.brain.perception.camera.FrameDiagnostics
import com.aipet.brain.perception.vision.face.embedding.FaceEmbeddingEngine
import com.aipet.brain.perception.vision.face.embedding.TfliteFaceEmbeddingEngine
import com.aipet.brain.perception.vision.face.embedding.UnavailableFaceEmbeddingEngine
import com.aipet.brain.perception.vision.model.DetectedFace
import com.aipet.brain.perception.vision.model.FaceDetectionResult
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.aipet.brain.app.ui.audio.model.AudioCategory
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.aipet.brain.app.perception.BackgroundPerceptionOrchestrator
import com.aipet.brain.app.ui.perception.TeachUnknownDialog
import com.aipet.brain.app.ui.perception.TeachUnknownTarget
import com.aipet.brain.perception.vision.objectdetection.RealObjectDetectionEngine
import com.aipet.brain.memory.persons.PersonRecord

private enum class AppScreen {
    Home,
    Onboarding,
    Debug,
    AvatarDebug,
    AudioDebug,
    Settings,
    EventViewer,
    Diary,
    ObservationViewer,
    ProfileAssociations,
    WorkingMemoryDebug,
    Camera,
    Persons,
    Traits,
    TeachPerson,
    PersonEditor,
    PersonDetail,
    FaceAutoEnroll,
    BehaviorIntelligenceDebug,
    EmotionalSystemsDebug,
    EvolutionSystemDebug
}

@Composable
fun PetBrainApp() {
    val appContext = LocalContext.current.applicationContext
    var currentScreenName by rememberSaveable { mutableStateOf(AppScreen.Home.name) }
    var editingPersonId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedPersonId by rememberSaveable { mutableStateOf<String?>(null) }
    var hasRequestedCameraPermission by rememberSaveable { mutableStateOf(false) }
    var hasRequestedMicrophonePermission by rememberSaveable { mutableStateOf(false) }
    var hasAppliedAppOpenLifecycle by rememberSaveable { mutableStateOf(false) }
    var teachSessionId by rememberSaveable { mutableStateOf(UUID.randomUUID().toString()) }
    var petNameDraft by rememberSaveable { mutableStateOf(PetProfileRepository.DEFAULT_PET_NAME) }
    var isResettingPetData by rememberSaveable { mutableStateOf(false) }
    val currentScreen = currentScreenName.toAppScreen()
    val avatarDebugEnabled = BuildConfig.DEBUG
    val resolvedScreen = if (currentScreen == AppScreen.AvatarDebug && !avatarDebugEnabled) {
        AppScreen.Debug
    } else {
        currentScreen
    }
    var latestEvent by remember { mutableStateOf<EventEnvelope?>(null) }
    var latestOwnerSeenEvent by remember { mutableStateOf<EventEnvelope?>(null) }
    var latestOwnerGreetingEvent by remember { mutableStateOf<EventEnvelope?>(null) }
    var lastPublishedFaceCount by remember { mutableStateOf(0) }
    var recognizedPersonLabel by remember { mutableStateOf<String?>(null) }
    var recognitionProbeSummary by remember { mutableStateOf("not_run") }
    // Unknown entity dialog state
    var activeTeachTarget by remember { mutableStateOf<TeachUnknownTarget?>(null) }
    var perceptionLookingUntilMs by remember { mutableStateOf(0L) }
    var perceptionAskingUntilMs by remember { mutableStateOf(0L) }
    val database = remember(appContext) {
        Room.databaseBuilder(appContext, AppDatabase::class.java, AppDatabase.DB_NAME)
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6,
                AppDatabase.MIGRATION_6_7,
                AppDatabase.MIGRATION_7_8,
                AppDatabase.MIGRATION_8_9,
                AppDatabase.MIGRATION_9_10,
                AppDatabase.MIGRATION_10_11,
                AppDatabase.MIGRATION_11_12,
                AppDatabase.MIGRATION_12_13,
                AppDatabase.MIGRATION_13_14,
                AppDatabase.MIGRATION_14_15,
                AppDatabase.MIGRATION_15_16,
                AppDatabase.MIGRATION_16_17,
                AppDatabase.MIGRATION_17_18,
                AppDatabase.MIGRATION_18_19,
                AppDatabase.MIGRATION_19_20,
                AppDatabase.MIGRATION_20_21,
                AppDatabase.MIGRATION_21_22,
                AppDatabase.MIGRATION_22_23
            )
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }
    val personDao = remember(database) { database.personDao() }
    val objectDao = remember(database) { database.objectDao() }
    val eventStore: EventStore = remember(database) {
        RoomEventStore(database.eventDao())
    }
    val personStore: PersonStore = remember(personDao) {
        RoomPersonStore(personDao)
    }
    val objectRepository = remember(objectDao) {
        ObjectRepository(objectDao)
    }
    val faceProfileStore: FaceProfileStore = remember(database, personStore) {
        RoomFaceProfileStore(
            faceProfileDao = database.faceProfileDao(),
            personStore = personStore
        )
    }
    val unknownFaceCandidateStore: UnknownFaceCandidateStore = remember(database) {
        RoomUnknownFaceCandidateStore(database.unknownFaceCandidateDao())
    }
    val knownPersonEmbeddingsSource = remember(database) {
        RoomKnownPersonEmbeddingsSource(
            personDao = database.personDao(),
            faceProfileDao = database.faceProfileDao()
        )
    }
    val personRecognitionService = remember(knownPersonEmbeddingsSource) {
        PersonRecognitionService(
            knownPersonEmbeddingsSource = knownPersonEmbeddingsSource,
            decisionLogger = { message ->
                Log.i(DEBUG_RECOGNITION_TAG, message)
            }
        )
    }
    val teachSampleStore: TeachSampleStore = remember(database) {
        RoomTeachSampleStore(database.teachSampleDao())
    }
    val teachSessionCompletionStore: TeachSessionCompletionStore = remember(database) {
        RoomTeachSessionCompletionStore(database.teachSessionCompletionDao())
    }
    val persistedTeachSessionCompletion by teachSessionCompletionStore.observeBySessionId(
        teachSessionId = teachSessionId
    ).collectAsState(initial = null)
    val teachSampleImageStorage = remember(appContext) {
        TeachSampleImageStorage(appContext)
    }
    val teachSampleFaceCropExtractor = remember(appContext, teachSampleImageStorage) {
        TeachSampleFaceCropExtractor(
            appContext = appContext,
            teachSampleImageStorage = teachSampleImageStorage
        )
    }
    val eventBus = remember(eventStore) {
        InMemoryEventBus(persistEvent = { event -> eventStore.save(event) })
    }
    val eventToMemoryCardMapper = remember {
        EventToMemoryCardMapper()
    }
    val dailySummaryGenerator = remember {
        EventDrivenDailySummaryGenerator()
    }
    val homeTodaySummaryResolver = remember {
        HomeTodaySummaryResolver()
    }
    val knownPersonEntities by personDao.observeBySeenCount().collectAsState(initial = emptyList())
    val knownObjectEntities by objectDao.observeBySeenCount().collectAsState(initial = emptyList())
    val homeKnownPersons = remember(knownPersonEntities) {
        knownPersonEntities.map { entity ->
            HomeKnownEntityCount(
                name = entity.displayName.ifBlank { "Unknown person" },
                seenCount = entity.seenCount
            )
        }
    }
    val homeKnownObjects = remember(knownObjectEntities) {
        knownObjectEntities.map { entity ->
            HomeKnownEntityCount(
                name = entity.name.ifBlank { "Unnamed object" },
                seenCount = entity.seenCount
            )
        }
    }
    val diaryEvents by eventStore.observeLatest(limit = 400).collectAsState(initial = emptyList())
    val diaryMemoryCards = remember(diaryEvents, eventToMemoryCardMapper) {
        eventToMemoryCardMapper.mapEvents(diaryEvents)
    }
    val recognitionMemoryStatsUpdater = remember(personStore) {
        object : RecognitionMemoryStatsUpdater {
            override suspend fun updatePersonSeenStats(
                personId: String,
                timestampMs: Long
            ): RecognitionMemoryStatsUpdate? {
                return withContext(Dispatchers.IO) {
                    val updatedPerson = personStore.updatePersonSeenStats(
                        personId = personId,
                        timestampMs = timestampMs
                    ) ?: return@withContext null
                    RecognitionMemoryStatsUpdate(
                        personId = updatedPerson.personId,
                        timestampMs = updatedPerson.lastSeenAtMs ?: timestampMs,
                        seenCount = updatedPerson.seenCount
                    )
                }
            }
        }
    }
    val recognitionDecisionEventPublisher = remember(eventBus, recognitionMemoryStatsUpdater) {
        RecognitionDecisionEventPublisher(
            eventBus = eventBus,
            recognitionMemoryStatsUpdater = recognitionMemoryStatsUpdater,
            updateLogger = { message ->
                Log.i(DEBUG_RECOGNITION_TAG, message)
            }
        )
    }
    val faceEmbeddingEngine: FaceEmbeddingEngine = remember(appContext) {
        runCatching {
            TfliteFaceEmbeddingEngine(appContext.assets)
        }.onFailure { error ->
            Log.e(
                DEBUG_STARTUP_TAG,
                "Face embedding engine failed to initialize. Teach-person embedding is unavailable.",
                error
            )
        }.getOrElse { error ->
            val reason = error.message?.takeIf { it.isNotBlank() } ?: "initialization_failed"
            UnavailableFaceEmbeddingEngine(reason = reason)
        }
    }
    val teachPersonSaveController = remember(database, appContext, personStore, faceProfileStore, eventBus, faceEmbeddingEngine) {
        TeachPersonSaveController(
            database = database,
            contentResolver = appContext.contentResolver,
            personStore = personStore,
            faceProfileStore = faceProfileStore,
            faceEmbeddingEngine = faceEmbeddingEngine,
            eventBus = eventBus
        )
    }
    val petSoundSettingsStore = remember(appContext) {
        PetSoundSettingsStore.create(appContext)
    }
    val isPetSoundEnabled by petSoundSettingsStore.soundEnabled.collectAsState(initial = true)
    val latestSoundEnabled by rememberUpdatedState(isPetSoundEnabled)
    val audioPlaybackEngine = remember(appContext, eventBus) {
        AudioPlaybackEngine(
            context = appContext,
            eventBus = eventBus
        )
    }
    val audioResponseDispatcher = remember(eventBus, audioPlaybackEngine) {
        AudioResponseDispatcher(
            eventBus = eventBus,
            playbackEngine = audioPlaybackEngine,
            isSoundEnabled = { latestSoundEnabled }
        )
    }
    val personSeenEventPublisher = remember(eventBus) {
        PersonSeenEventPublisher(eventBus)
    }
    val ownerSeenReactionEngine = remember(eventBus) {
        OwnerSeenReactionEngine(eventBus)
    }
    val brainStateStore = remember(eventBus) {
        BrainStateStore(eventBus = eventBus)
    }
    val brainInteractionLoop = remember(eventBus, brainStateStore) {
        BrainInteractionLoop(
            eventBus = eventBus,
            brainStateStore = brainStateStore
        )
    }
    val brainStateSnapshot by brainStateStore.observe().collectAsState(
        initial = brainStateStore.currentSnapshot()
    )
    val cameraSelectionStore = remember(appContext) {
        CameraSelectionStore.create(appContext)
    }
    val keywordSpottingConfigStore = remember(appContext) {
        KeywordSpottingConfigStore.create(appContext)
    }
    val petOnboardingStore = remember(appContext) {
        PetOnboardingStore.create(appContext)
    }
    val selectedCamera by cameraSelectionStore.selectedCamera.collectAsState(
        initial = CameraSelection.FRONT
    )
    val observationRecorder = remember(eventBus) {
        ObservationRecorder(eventBus)
    }
    val traitsRepository = remember(database) {
        RoomTraitsSnapshotRepository(database.traitsSnapshotDao())
    }
    val petStateStore = remember(database) {
        RoomPetStateStore(database.petStateDao())
    }
    val petProfileStore = remember(database) {
        RoomPetProfileStore(database.petProfileDao())
    }
    val petTraitStore = remember(database) {
        RoomPetTraitStore(database.petTraitDao())
    }
    val petStateRepository = remember(petStateStore) {
        PetStateRepository(store = petStateStore)
    }
    val petProfileRepository = remember(petProfileStore) {
        PetProfileRepository(store = petProfileStore)
    }
    val petTraitRepository = remember(petTraitStore) {
        PetTraitRepository(store = petTraitStore)
    }
    // ── Evolution system repositories ────────────────────────────────────────
    val episodeRepository = remember(database) {
        com.aipet.brain.memory.evolution.RoomEpisodeRepository(database.memoryEpisodeDao())
    }
    val semanticMemoryRepository = remember(database) {
        com.aipet.brain.memory.semantics.RoomSemanticMemoryRepository(database.semanticMemoryFactDao())
    }
    val bondRepositoryV2 = remember(database) {
        com.aipet.brain.memory.bond.RoomBondRepositoryV2(database.bondStateV2Dao())
    }
    val habitRepository = remember(database) {
        com.aipet.brain.memory.habit.RoomUserHabitRepository(database.userHabitProfileDao())
    }
    val evolutionCoordinator = remember(
        episodeRepository, semanticMemoryRepository, bondRepositoryV2,
        habitRepository, petTraitRepository, eventBus
    ) {
        com.aipet.brain.brain.evolution.EvolutionCoordinator(
            episodeRepository = episodeRepository,
            semanticRepository = semanticMemoryRepository,
            bondRepository = bondRepositoryV2,
            habitRepository = habitRepository,
            traitRepository = petTraitRepository,
            eventBus = eventBus
        )
    }
    val evolutionContext by evolutionCoordinator.evolutionContext.collectAsState()
    val petStateDecayEngine = remember {
        PetStateDecayEngine()
    }
    val petConditionResolver = remember {
        PetConditionResolver()
    }
    val petEmotionResolver = remember {
        PetEmotionResolver()
    }
    val petGreetingResolver = remember {
        PetGreetingResolver()
    }
    val invitationContextResolver = remember {
        com.aipet.brain.brain.evolution.InvitationContextResolver()
    }
    val petDayBoundaryResolver = remember {
        PetDayBoundaryResolver()
    }
    val petBehaviorWeightResolver = remember {
        PetBehaviorWeightResolver()
    }
    val petInteractionStateReducer = remember {
        PetInteractionStateReducer()
    }
    val feedPetUseCase = remember {
        FeedPetUseCase()
    }
    val playWithPetUseCase = remember {
        PlayWithPetUseCase()
    }
    val letPetRestUseCase = remember {
        LetPetRestUseCase()
    }
    val petGameplayCooldownGate = remember {
        PetGameplayCooldownGate()
    }
    val petTraitEvolutionEngine = remember {
        PetTraitEvolutionEngine()
    }
    val petPersonalitySummaryResolver = remember {
        PetPersonalitySummaryResolver()
    }
    val traitsEngine = remember(eventBus, traitsRepository) {
        TraitsEngine(
            repository = traitsRepository,
            eventBus = eventBus
        )
    }
    val familiarityStore = remember(personStore) {
        object : FamiliarityStore {
            override suspend fun increaseFamiliarity(
                personId: String,
                delta: Float,
                updatedAtMs: Long
            ): FamiliarityIncreaseResult? {
                val normalizedPersonId = personId.trim()
                if (normalizedPersonId.isBlank()) {
                    return null
                }
                val previous = personStore.getById(normalizedPersonId) ?: return null
                val updated = personStore.increaseFamiliarity(
                    personId = normalizedPersonId,
                    delta = delta,
                    updatedAtMs = updatedAtMs
                ) ?: return null
                return FamiliarityIncreaseResult(
                    personId = updated.personId,
                    previousFamiliarityScore = previous.familiarityScore,
                    updatedFamiliarityScore = updated.familiarityScore,
                    updatedAtMs = updated.updatedAtMs
                )
            }
        }
    }
    val familiarityEngine = remember(eventBus, familiarityStore) {
        FamiliarityEngine(
            eventBus = eventBus,
            familiarityStore = familiarityStore
        )
    }
    val workingMemoryStore = remember {
        WorkingMemoryStore()
    }
    val workingMemoryUpdater = remember(eventBus, workingMemoryStore) {
        WorkingMemoryUpdater(
            eventBus = eventBus,
            workingMemoryStore = workingMemoryStore
        )
    }
    val audioStimulusObserver = remember(eventBus) {
        AudioStimulusObserver(eventBus)
    }
    val audioResponseRequestEmitter = remember(eventBus) {
        AudioResponseRequestEmitter(eventBus)
    }
    val loudSoundReactionRule = remember(audioStimulusObserver, audioResponseRequestEmitter) {
        LoudSoundReactionRule(
            audioStimulusObserver = audioStimulusObserver,
            audioResponseRequestEmitter = audioResponseRequestEmitter
        )
    }
    val voiceActivityAcknowledgmentRule = remember(audioStimulusObserver, audioResponseRequestEmitter) {
        VoiceActivityAcknowledgmentRule(
            audioStimulusObserver = audioStimulusObserver,
            audioResponseRequestEmitter = audioResponseRequestEmitter
        )
    }
    val wakeWordAcknowledgmentRule = remember(
        audioStimulusObserver,
        audioResponseRequestEmitter,
        brainStateStore
    ) {
        WakeWordAcknowledgmentRule(
            audioStimulusObserver = audioStimulusObserver,
            audioResponseRequestEmitter = audioResponseRequestEmitter,
            brainStateStore = brainStateStore
        )
    }
    val keywordIntentMapper = remember {
        KeywordIntentMapper()
    }
    val isBehaviorExperienceAuthoritative = true
    val keywordIntentCommandRule = remember(
        audioStimulusObserver,
        keywordIntentMapper,
        audioResponseRequestEmitter,
        brainStateStore
    ) {
        KeywordIntentCommandRule(
            audioStimulusObserver = audioStimulusObserver,
            keywordIntentMapper = keywordIntentMapper,
            audioResponseRequestEmitter = audioResponseRequestEmitter,
            brainStateStore = brainStateStore,
            shouldEmitAudioResponse = { !isBehaviorExperienceAuthoritative }
        )
    }
    val currentWorkingMemory by workingMemoryStore.observe().collectAsState(
        initial = workingMemoryStore.currentSnapshot()
    )
    val latestAudioStimulus by audioStimulusObserver.observeLatestStimulus().collectAsState(
        initial = audioStimulusObserver.currentLatestStimulus()
    )
    val currentTraits by traitsEngine.observeTraits().collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()
    val audioCaptureLifecycleEventPublisher = remember(eventBus, coroutineScope) {
        AudioCaptureLifecycleEventPublisher(
            eventBus = eventBus,
            coroutineScope = coroutineScope
        )
    }
    val animationInputMapper = remember {
        PetAnimationInputMapper()
    }
    val petAnimatorFactory = remember {
        DefaultPetAnimatorFactory()
    }
    val petAnimator: PetAnimator = remember(coroutineScope, petAnimatorFactory) {
        petAnimatorFactory.create(scope = coroutineScope)
    }
    val audioRuntimeDebugState by audioCaptureLifecycleEventPublisher.observeRuntimeDebugState().collectAsState(
        initial = audioCaptureLifecycleEventPublisher.currentRuntimeDebugState()
    )

    // ── Behavior Intelligence v2 ─────────────────────────────────────────────
    val perceptionFusionRepo = remember { InMemoryPerceptionFusionRepository() }
    val presenceInterpreter = remember { PresenceInterpreter() }
    val audioInterpreter = remember { AudioInterpreter() }
    val voiceInterpreter = remember { VoiceInterpreter() }
    val touchInterpreter = remember { TouchInterpreter() }
    val perceptionFusionCoordinator = remember(
        perceptionFusionRepo, presenceInterpreter, audioInterpreter,
        voiceInterpreter, touchInterpreter
    ) {
        PerceptionFusionCoordinator(
            repository = perceptionFusionRepo,
            presenceInterpreter = presenceInterpreter,
            audioInterpreter = audioInterpreter,
            voiceInterpreter = voiceInterpreter,
            touchInterpreter = touchInterpreter
        )
    }
    val attentionStateRepository = remember { AttentionStateRepository() }
    val emotionMomentumEngine = remember { EmotionMomentumEngine() }
    val cooldownTracker = remember { CooldownTracker() }
    val relationshipStateBuilder = remember { RelationshipStateBuilder() }
    val attentionEngine = remember(attentionStateRepository) {
        AttentionEngine(
            repository = attentionStateRepository,
            evaluator = AttentionTargetEvaluator(),
            scorer = SalienceScorer(),
            arbitrator = AttentionArbitrator(),
            modeResolver = AttentionModeResolver(),
            fatigueTracker = AttentionFatigueTracker()
        )
    }
    val behaviorEngine = remember(
        perceptionFusionRepo, attentionStateRepository, emotionMomentumEngine,
        cooldownTracker, relationshipStateBuilder
    ) {
        BehaviorEngine(
            fusionRepository = perceptionFusionRepo,
            attentionStateRepository = attentionStateRepository,
            emotionMomentumEngine = emotionMomentumEngine,
            cooldownTracker = cooldownTracker,
            relationshipStateBuilder = relationshipStateBuilder,
            scorer = IntentionScorer(),
            arbitrator = IntentionArbitrator(),
            planner = BehaviorPlanner()
        )
    }
    val behaviorExperienceBinder = remember {
        BehaviorExperienceBinder()
    }
    val petIntentionExecutor = remember {
        PetIntentionExecutor()
    }
    val behaviorDebugState by behaviorEngine.debugState.collectAsState()
    val attentionDebugState by attentionEngine.debugState.collectAsState()
    val fusionSnapshot by perceptionFusionRepo.observeFusionSnapshot().collectAsState()
    // RecentMemorySummary tracks short-term history updated from events
    var recentMemorySummary by remember { mutableStateOf(RecentMemorySummary.DEFAULT) }
    val audioPlaybackDebugState by audioPlaybackEngine.observeDebugState().collectAsState(
        initial = audioPlaybackEngine.currentDebugState()
    )
    var currentPetEmotion by remember { mutableStateOf(PetEmotion.IDLE) }
    var currentPetState by remember { mutableStateOf<PetState?>(null) }
    var currentDayBoundaryType by remember { mutableStateOf<PetDayBoundaryType?>(null) }
    var currentSummaryDate by remember { mutableStateOf(java.time.LocalDate.now()) }
    var appOpenGreeting by remember { mutableStateOf<PetGreetingReaction?>(null) }
    var activeAppOpenGreeting by remember { mutableStateOf<PetGreetingReaction?>(null) }
    var activeAppOpenGreetingExpiresAtMs by remember { mutableStateOf(0L) }
    var activePetProfile by remember { mutableStateOf<PetProfile?>(null) }
    var currentPetTraits by remember { mutableStateOf<PetTrait?>(null) }
    var currentPetConditions by remember { mutableStateOf(emptySet<PetCondition>()) }
    var latestBehaviorDecisionSource by remember { mutableStateOf<String?>(null) }
    var latestBehaviorDecision by remember { mutableStateOf<PetBehaviorDecision<PetEmotion>?>(null) }
    // NX emotional systems state
    var currentAbsenceBucket by remember { mutableStateOf<com.aipet.brain.brain.pet.AbsenceBucket?>(null) }
    var currentGreetingStyle by remember { mutableStateOf<com.aipet.brain.brain.pet.PetGreetingStyle?>(null) }
    var currentRelationshipStage by remember { mutableStateOf<com.aipet.brain.brain.pet.RelationshipStage?>(null) }
    var lastBehaviorScoringResult by remember { mutableStateOf<com.aipet.brain.brain.pet.PetBehaviorScoringEngine.ScoringResult?>(null) }
    val sessionInteractionTracker = remember { com.aipet.brain.brain.pet.SessionInteractionTracker() }
    val absenceClassifier = remember { com.aipet.brain.brain.pet.AbsenceClassifier() }
    val neglectTracker = remember { com.aipet.brain.brain.pet.NeglectTracker() }
    val relationshipStageResolver = remember { com.aipet.brain.brain.pet.RelationshipStageResolver() }
    val careActionProcessor = remember { com.aipet.brain.brain.pet.CareActionProcessor() }
    var homeInteractionFeedback by remember { mutableStateOf<PetGameplayFeedback?>(null) }
    var behaviorDrivenIntent by remember { mutableStateOf<com.aipet.brain.ui.avatar.pixel.bridge.PixelPetAvatarIntent?>(null) }
    var behaviorTalkDirective by remember { mutableStateOf<TalkDirective?>(null) }
    var behaviorExperienceDebugState by remember { mutableStateOf(BehaviorExperienceDebugState.EMPTY) }
    var transientReactionIntent by remember { mutableStateOf<com.aipet.brain.ui.avatar.pixel.bridge.PixelPetAvatarIntent?>(null) }
    // Invitation system state
    var lastInvitationMs by remember { mutableStateOf(0L) }
    var invitationIgnoredCount by remember { mutableStateOf(0) }
    var pendingInvitationUntilMs by remember { mutableStateOf(0L) }
    var invitationUiTriggerToken by remember { mutableStateOf(0L) }
    var lastLifecycleStopAtMs by remember { mutableStateOf(0L) }
    var lastAudioRequestDispatchAtMs by remember { mutableStateOf(0L) }
    var lastAudioRequestDispatchCategory by remember { mutableStateOf<AudioCategory?>(null) }
    var latestAudioOutputStartedAtMs by remember { mutableStateOf(0L) }
    var transientReactionActiveUntilMs by remember { mutableStateOf(0L) }
    val transientClearJobHolder = remember { arrayOfNulls<kotlinx.coroutines.Job>(1) }
    var runtimeOrchestratorDiagnostics by remember { mutableStateOf<com.aipet.brain.ui.avatar.pixel.bridge.PixelAnimationOrchestratorDiagnostics?>(null) }
    val diaryDailySummaries = remember(
        diaryEvents,
        currentPetState,
        currentDayBoundaryType,
        currentSummaryDate,
        dailySummaryGenerator
    ) {
        dailySummaryGenerator.generateAll(
            persistedEvents = diaryEvents,
            petStateSnapshot = currentPetState,
            currentDate = currentSummaryDate,
            continuityLabelForCurrentDay = currentDayBoundaryType.toDailySummaryLabel()
        )
    }
    val homeTodaySummary = remember(diaryMemoryCards, diaryDailySummaries, currentSummaryDate, homeTodaySummaryResolver) {
        homeTodaySummaryResolver.resolve(
            memoryCards = diaryMemoryCards,
            dailySummaries = diaryDailySummaries,
            targetDate = currentSummaryDate
        )
    }
    val homeUiModel = remember(
        activePetProfile?.name,
        currentPetState,
        currentPetEmotion,
        currentPetConditions,
        currentPetTraits,
        homeTodaySummary,
        homeKnownPersons,
        homeKnownObjects
    ) {
        HomeUiModelBuilder.build(
            petName = activePetProfile?.name ?: PetProfileRepository.DEFAULT_PET_NAME,
            petState = currentPetState,
            emotion = currentPetEmotion,
            conditions = currentPetConditions,
            traits = currentPetTraits,
            todaySummary = homeTodaySummary,
            knownPersons = homeKnownPersons,
            knownObjects = homeKnownObjects
        )
    }
    val homeInteractionUiState = remember(
        homeInteractionFeedback,
        isBehaviorExperienceAuthoritative
    ) {
        // In behavior-authoritative mode, normal interaction copy is owned by behavior talk.
        // Keep blocked/cooldown feedback visible so rejected interactions remain explainable.
        val shouldSurfaceFeedbackBubble = !isBehaviorExperienceAuthoritative ||
            (homeInteractionFeedback?.isBlocked == true)
        val feedbackMessage = if (shouldSurfaceFeedbackBubble) {
            homeInteractionFeedback?.text
        } else {
            null
        }
        HomeInteractionUiState(
            feedbackMessage = feedbackMessage,
            feedbackToken = if (feedbackMessage != null) System.currentTimeMillis() else 0L,
            feedbackIsBlocked = homeInteractionFeedback?.isBlocked ?: false,
            careHint = "Feed, play, or rest with one tap from here.",
            interactionHint = "Tap for a quick hello or hold for a longer cuddle.",
            cooldownHint = "Repeated taps and care actions pause briefly so each reaction stays readable."
        )
    }
    val isPerceptionLooking = perceptionLookingUntilMs > System.currentTimeMillis()
    val isPerceptionAsking = perceptionAskingUntilMs > System.currentTimeMillis()
    val startupGreetingForVisual = activeAppOpenGreeting?.takeIf {
        System.currentTimeMillis() <= activeAppOpenGreetingExpiresAtMs
    }
    val homePixelPetAvatarSignal = remember(
        currentPetEmotion,
        currentPetConditions,
        brainStateSnapshot.currentState,
        latestAudioStimulus,
        isPerceptionLooking,
        isPerceptionAsking,
        isBehaviorExperienceAuthoritative,
        behaviorDrivenIntent,
        startupGreetingForVisual,
        transientReactionIntent
    ) {
        com.aipet.brain.app.avatar.HomePixelPetAvatarSignal(
            petEmotion = currentPetEmotion,
            conditions = currentPetConditions,
            brainState = brainStateSnapshot.currentState,
            latestAudioStimulus = latestAudioStimulus,
            isPerceptionLooking = isPerceptionLooking,
            isPerceptionAsking = isPerceptionAsking,
            behaviorDrivenIntent = if (isBehaviorExperienceAuthoritative) {
                behaviorDrivenIntent
            } else {
                null
            },
            greetingBoostIntent = if (!isBehaviorExperienceAuthoritative || behaviorDrivenIntent == null) {
                startupGreetingForVisual?.let {
                    GreetingPresentationMapper.mapToAvatarIntent(it.emotion)
                }
            } else {
                null
            },
            // In behavior-authoritative mode, one-shot touch feedback is injected through
            // the bridge transient channel; non-authoritative mode uses HomeScreen-owned
            // reaction controller to avoid duplicate reaction ownership.
            transientReactionIntent = if (isBehaviorExperienceAuthoritative) {
                transientReactionIntent
            } else {
                null
            },
            // Sound reaction ownership is consolidated away from the bridge path to prevent
            // competing triggers with HomeScreen and behavior execution channels.
            soundReactionIntent = null
        )
    }
    LaunchedEffect(activeAppOpenGreeting, activeAppOpenGreetingExpiresAtMs) {
        val deadline = activeAppOpenGreetingExpiresAtMs
        if (activeAppOpenGreeting == null || deadline <= 0L) {
            return@LaunchedEffect
        }
        val remainingMs = deadline - System.currentTimeMillis()
        if (remainingMs > 0L) {
            kotlinx.coroutines.delay(remainingMs)
        }
        if (activeAppOpenGreetingExpiresAtMs == deadline) {
            activeAppOpenGreeting = null
            activeAppOpenGreetingExpiresAtMs = 0L
        }
    }
    val debugPixelPetBridgeAdapter = remember { RealPixelPetBridgeStateAdapter() }
    val homePixelPetDebugBridgeState = remember(homePixelPetAvatarSignal) {
        debugPixelPetBridgeAdapter.map(homePixelPetAvatarSignal)
    }
    val homePixelPetBridgeState = remember(homePixelPetDebugBridgeState) {
        homePixelPetDebugBridgeState.copy(debugMetadata = null)
    }

    fun queueTransientReaction(
        intent: com.aipet.brain.ui.avatar.pixel.bridge.PixelPetAvatarIntent,
        durationMs: Long = TapReactionPresentationMapper.REACTION_DURATION_MS
    ) {
        val safeDurationMs = durationMs.coerceAtLeast(0L)
        transientClearJobHolder[0]?.cancel()
        transientReactionIntent = intent
        transientReactionActiveUntilMs = System.currentTimeMillis() + safeDurationMs
        transientClearJobHolder[0] = coroutineScope.launch {
            if (safeDurationMs > 0L) {
                kotlinx.coroutines.delay(safeDurationMs)
            }
            if (transientReactionIntent == intent && System.currentTimeMillis() >= transientReactionActiveUntilMs) {
                transientReactionIntent = null
                transientReactionActiveUntilMs = 0L
            }
        }
    }

    suspend fun publishAudioResponseRequestWithRuntimeGuards(
        category: AudioCategory,
        timestampMs: Long,
        cooldownKey: String,
        priority: Int,
        source: String
    ): String? {
        val elapsedSinceLastDispatch = if (lastAudioRequestDispatchAtMs > 0L) {
            timestampMs - lastAudioRequestDispatchAtMs
        } else {
            Long.MAX_VALUE
        }
        val hitsGlobalRateGuard =
            elapsedSinceLastDispatch < RUNTIME_AUDIO_GLOBAL_MIN_INTERVAL_MS
        val hitsCategoryRateGuard =
            lastAudioRequestDispatchCategory == category &&
                elapsedSinceLastDispatch < RUNTIME_AUDIO_SAME_CATEGORY_MIN_INTERVAL_MS
        if (hitsGlobalRateGuard || hitsCategoryRateGuard) {
            val suppressionReason = if (hitsCategoryRateGuard) {
                "runtime_same_category_guard_${RUNTIME_AUDIO_SAME_CATEGORY_MIN_INTERVAL_MS}ms"
            } else {
                "runtime_global_guard_${RUNTIME_AUDIO_GLOBAL_MIN_INTERVAL_MS}ms"
            }
            eventBus.publish(
                EventEnvelope.create(
                    type = EventType.AUDIO_RESPONSE_SKIPPED,
                    timestampMs = timestampMs,
                    payloadJson = com.aipet.brain.brain.events.audio.AudioResponsePayload(
                        category = category.label,
                        clipId = null,
                        durationMs = 0L,
                        priority = 0,
                        timestamp = timestampMs,
                        reason = suppressionReason
                    ).toJson()
                )
            )
            Log.d(
                AUDIO_RUNTIME_TAG,
                "Suppressed audio request. source=$source, category=${category.label}, reason=$suppressionReason"
            )
            return suppressionReason
        }
        eventBus.publish(
            EventEnvelope.create(
                type = EventType.AUDIO_RESPONSE_REQUESTED,
                timestampMs = timestampMs,
                payloadJson = AudioResponseRequestPayload(
                    category = category.label,
                    priority = priority,
                    interruptPolicy = "INTERRUPT_NONE",
                    cooldownKey = cooldownKey,
                    timestamp = timestampMs
                ).toJson()
            )
        )
        lastAudioRequestDispatchAtMs = timestampMs
        lastAudioRequestDispatchCategory = category
        Log.d(
            AUDIO_RUNTIME_TAG,
            "Queued audio request. source=$source, category=${category.label}, cooldownKey=$cooldownKey"
        )
        return null
    }

    LaunchedEffect(
        petStateRepository,
        petProfileRepository,
        petOnboardingStore,
        petStateDecayEngine,
        petConditionResolver,
        petEmotionResolver,
        petGreetingResolver,
        petDayBoundaryResolver,
        eventBus,
        hasAppliedAppOpenLifecycle,
        isResettingPetData
    ) {
        if (isResettingPetData) {
            return@LaunchedEffect
        }
        val hasHydratedRuntimeState =
            currentPetState != null &&
                activePetProfile != null &&
                currentPetTraits != null
        if (hasAppliedAppOpenLifecycle && hasHydratedRuntimeState) {
            return@LaunchedEffect
        }
        if (hasAppliedAppOpenLifecycle && !hasHydratedRuntimeState) {
            Log.w(
                DEBUG_STARTUP_TAG,
                "Rehydrating app-open lifecycle because runtime state is missing."
            )
            val restoredSnapshot = withContext(Dispatchers.IO) {
                val profile = petProfileRepository.getOrCreateActiveProfile()
                val traits = petTraitRepository.getOrCreateForPet(profile.id)
                val persistedState = petStateRepository.getOrCreateState()
                val nowMs = System.currentTimeMillis()
                val dayBoundary = petDayBoundaryResolver.resolve(
                    previousUpdatedAt = persistedState.lastUpdatedAt,
                    now = nowMs
                )
                val conditions = petConditionResolver.resolve(persistedState)
                val emotion = petEmotionResolver.resolve(persistedState, conditions)
                val absenceBucket = absenceClassifier.classify(persistedState, nowMs)
                val relationshipStage = relationshipStageResolver.resolve(persistedState)
                RuntimeRehydrateSnapshot(
                    profile = profile,
                    state = persistedState,
                    traits = traits,
                    conditions = conditions,
                    emotion = emotion,
                    absenceBucket = absenceBucket,
                    relationshipStage = relationshipStage,
                    dayBoundaryType = dayBoundary.type,
                    summaryDate = dayBoundary.currentDate
                )
            }
            activePetProfile = restoredSnapshot.profile
            petNameDraft = restoredSnapshot.profile.name
            currentPetState = restoredSnapshot.state
            currentPetEmotion = restoredSnapshot.emotion
            currentPetTraits = restoredSnapshot.traits
            currentPetConditions = restoredSnapshot.conditions
            currentAbsenceBucket = restoredSnapshot.absenceBucket
            currentRelationshipStage = restoredSnapshot.relationshipStage
            currentDayBoundaryType = restoredSnapshot.dayBoundaryType
            currentSummaryDate = restoredSnapshot.summaryDate
            currentGreetingStyle = null
            homeInteractionFeedback = null
            appOpenGreeting = null
            activeAppOpenGreeting = null
            activeAppOpenGreetingExpiresAtMs = 0L
            latestBehaviorDecisionSource = "runtime_rehydrate"
            latestBehaviorDecision = null
            sessionInteractionTracker.resetForNewSession()
            val nowMs = System.currentTimeMillis()
            val rehydrateCalendar = java.util.Calendar.getInstance().apply {
                timeInMillis = nowMs
            }
            val lastDirectInteractionAt = restoredSnapshot.state.lastMeaningfulInteractionAt
                .takeIf { it > 0L } ?: restoredSnapshot.state.lastUpdatedAt
            behaviorEngine.setSessionContext(
                SessionContext(
                    sessionStartMs = nowMs,
                    sessionAgeMs = 0L,
                    hourOfDay = rehydrateCalendar.get(java.util.Calendar.HOUR_OF_DAY),
                    isFirstGreetToday = false,
                    sessionInteractionCount = 0,
                    msSinceLastDirectInteraction = (nowMs - lastDirectInteractionAt).coerceAtLeast(0L),
                    isReturningAfterLongAbsence = restoredSnapshot.absenceBucket == com.aipet.brain.brain.pet.AbsenceBucket.LONG_RETURN ||
                        restoredSnapshot.absenceBucket == com.aipet.brain.brain.pet.AbsenceBucket.NEGLECT_RETURN,
                    nowMs = nowMs
                )
            )
            return@LaunchedEffect
        }
        val resolvedState = withContext(Dispatchers.IO) {
            val profile = petProfileRepository.getOrCreateActiveProfile()
            val traits = petTraitRepository.getOrCreateForPet(profile.id)
            val currentState = petStateRepository.getOrCreateState()
            val startupNow = System.currentTimeMillis()
            val dayBoundary = petDayBoundaryResolver.resolve(
                previousUpdatedAt = currentState.lastUpdatedAt,
                now = startupNow
            )
            val decayedState = petStateDecayEngine.applyDecay(
                currentState = currentState,
                now = startupNow
            )
            // NX2: Classify absence and apply neglect if warranted
            val absenceBucket = absenceClassifier.classify(decayedState, startupNow)
            val stateAfterNeglect = neglectTracker.applyIfNeeded(
                state = decayedState,
                absenceBucket = absenceBucket
            )
            val absenceMsBeforeOpen = if (stateAfterNeglect.lastOpenAt > 0L) {
                (startupNow - stateAfterNeglect.lastOpenAt).coerceAtLeast(0L)
            } else {
                0L
            }
            val appOpenEvolutionContext = evolutionCoordinator.previewAppOpenContext(
                petState = stateAfterNeglect,
                petTraits = traits,
                absenceMsOverride = absenceMsBeforeOpen
            )
            // Update lastOpenAt so future absences can be classified correctly
            val stateWithOpenTimestamp = stateAfterNeglect.copy(lastOpenAt = startupNow)
            val persistedState = petStateRepository.updateState(stateWithOpenTimestamp)

            val conditions = petConditionResolver.resolve(persistedState)
            val emotion = petEmotionResolver.resolve(persistedState, conditions)
            val greetingContext = com.aipet.brain.brain.pet.PetGreetingContext(
                absenceBucket = absenceBucket,
                state = persistedState,
                conditions = conditions,
                traits = traits,
                evolutionReunionType = appOpenEvolutionContext.reunionType.name,
                evolutionBondAffection = appOpenEvolutionContext.bond.affection,
                evolutionBondTrust = appOpenEvolutionContext.bond.trust
            )
            val greetingResolution = petGreetingResolver.resolveWithContext(
                context = greetingContext,
                emotion = emotion
            )
            val relationshipStage = relationshipStageResolver.resolve(persistedState)
            StartupPetSnapshot(
                profile = profile,
                state = persistedState,
                emotion = emotion,
                greeting = greetingResolution.reaction,
                traits = traits,
                conditions = conditions,
                greetingDecision = greetingResolution.decision,
                greetedAtMs = startupNow,
                dayBoundaryType = dayBoundary.type,
                summaryDate = dayBoundary.currentDate,
                absenceBucket = absenceBucket,
                relationshipStage = relationshipStage,
                evolutionAbsenceMs = absenceMsBeforeOpen
            )
        }
        val shouldShowNamingOnboarding = withContext(Dispatchers.IO) {
            !petOnboardingStore.isNamingCompletedForProfile(resolvedState.profile.id)
        }
        activePetProfile = resolvedState.profile
        petNameDraft = resolvedState.profile.name
        currentPetState = resolvedState.state
        currentPetEmotion = resolvedState.emotion
        currentDayBoundaryType = resolvedState.dayBoundaryType
        currentSummaryDate = resolvedState.summaryDate
        appOpenGreeting = resolvedState.greeting
        activeAppOpenGreeting = resolvedState.greeting
        activeAppOpenGreetingExpiresAtMs = resolvedState.greetedAtMs + APP_OPEN_GREETING_WINDOW_MS
        currentPetTraits = resolvedState.traits
        currentPetConditions = resolvedState.conditions
        homeInteractionFeedback = null
        latestBehaviorDecisionSource = "greeting"
        latestBehaviorDecision = resolvedState.greetingDecision
        // NX: update emotional systems state
        currentAbsenceBucket = resolvedState.absenceBucket
        currentGreetingStyle = resolvedState.greeting.greetingStyle
        currentRelationshipStage = resolvedState.relationshipStage
        sessionInteractionTracker.resetForNewSession()
        val startupCalendar = java.util.Calendar.getInstance().apply {
            timeInMillis = resolvedState.greetedAtMs
        }
        val lastDirectInteractionAt = resolvedState.state.lastMeaningfulInteractionAt
            .takeIf { it > 0L } ?: resolvedState.state.lastUpdatedAt
        behaviorEngine.setSessionContext(
            SessionContext(
                sessionStartMs = resolvedState.greetedAtMs,
                sessionAgeMs = 0L,
                hourOfDay = startupCalendar.get(java.util.Calendar.HOUR_OF_DAY),
                isFirstGreetToday = resolvedState.dayBoundaryType == PetDayBoundaryType.NEW_DAY_RETURN,
                sessionInteractionCount = 0,
                msSinceLastDirectInteraction = (resolvedState.greetedAtMs - lastDirectInteractionAt)
                    .coerceAtLeast(0L),
                isReturningAfterLongAbsence = resolvedState.absenceBucket == com.aipet.brain.brain.pet.AbsenceBucket.LONG_RETURN ||
                    resolvedState.absenceBucket == com.aipet.brain.brain.pet.AbsenceBucket.NEGLECT_RETURN,
                nowMs = resolvedState.greetedAtMs
            )
        )
        if (!isBehaviorExperienceAuthoritative) {
            petAnimator.syncInputFrame(
                animationInputMapper.mapFrame(
                    state = resolvedState.state,
                    emotion = resolvedState.emotion,
                    conditions = resolvedState.conditions,
                    traits = resolvedState.traits
                )
            )
            petAnimator.playTrigger(
                animationInputMapper.mapGreetingTrigger(
                    greeting = resolvedState.greeting,
                    conditions = resolvedState.conditions,
                    decision = resolvedState.greetingDecision
                ),
                durationMs = PetAnimator.DEFAULT_GREETING_DURATION_MS
            )
        }
        eventBus.publish(
            EventEnvelope.create(
                type = EventType.PET_STATE_DECAY_APPLIED,
                payloadJson = com.aipet.brain.brain.events.PetStateDecayAppliedPayload(
                    appliedAtMs = resolvedState.greetedAtMs,
                    absenceBucket = resolvedState.absenceBucket.name,
                    elapsedMinutes = 0L,
                    energyDelta = 0,
                    hungerDelta = 0,
                    sleepinessDelta = 0,
                    socialDelta = 0,
                    comfortDelta = 0,
                    stimulationDelta = 0,
                    trustDelta = 0
                ).toJson(),
                timestampMs = resolvedState.greetedAtMs
            )
        )
        eventBus.publish(
            EventEnvelope.create(
                type = EventType.PET_GREETED,
                payloadJson = PetGreetedEventPayload(
                    greetedAtMs = resolvedState.greetedAtMs,
                    emotion = resolvedState.greeting.emotion.name,
                    reason = resolvedState.greeting.reason,
                    message = resolvedState.greeting.message
                ).toJson(),
                timestampMs = resolvedState.greetedAtMs
            )
        )
        if (shouldShowNamingOnboarding) {
            currentScreenName = AppScreen.Onboarding.name
        }
        transientClearJobHolder[0]?.cancel()
        transientReactionIntent = null
        transientReactionActiveUntilMs = 0L
        behaviorDrivenIntent = null
        behaviorTalkDirective = null
        behaviorExperienceDebugState = BehaviorExperienceDebugState.EMPTY
        lastAudioRequestDispatchAtMs = 0L
        lastAudioRequestDispatchCategory = null
        petIntentionExecutor.resetRuntimeState()
        hasAppliedAppOpenLifecycle = true
        // Notify evolution coordinator of app open
        evolutionCoordinator.onAppOpened(
            petState = resolvedState.state,
            petTraits = resolvedState.traits,
            absenceMsOverride = resolvedState.evolutionAbsenceMs
        )
    }

    // Auto-expire perception flags so the avatar doesn't stay stuck in LOOKING/ASKING
    // when no recomposition occurs after the hold window ends.
    LaunchedEffect(perceptionLookingUntilMs) {
        val deadline = perceptionLookingUntilMs
        if (deadline <= 0L) return@LaunchedEffect
        val remaining = deadline - System.currentTimeMillis()
        if (remaining > 0) kotlinx.coroutines.delay(remaining)
        if (perceptionLookingUntilMs == deadline) perceptionLookingUntilMs = 0L
    }
    LaunchedEffect(perceptionAskingUntilMs) {
        val deadline = perceptionAskingUntilMs
        if (deadline <= 0L) return@LaunchedEffect
        val remaining = deadline - System.currentTimeMillis()
        if (remaining > 0) kotlinx.coroutines.delay(remaining)
        if (perceptionAskingUntilMs == deadline) perceptionAskingUntilMs = 0L
    }

    LaunchedEffect(activePetProfile?.id, petTraitRepository) {
        val profileId = activePetProfile?.id ?: return@LaunchedEffect
        petTraitRepository.observeForPet(profileId).collect { observedTraits ->
            currentPetTraits = observedTraits
        }
    }

    LaunchedEffect(eventBus) {
        eventBus.observe().collect { event ->
            latestEvent = event
            when (event.type) {
                EventType.OWNER_SEEN_DETECTED -> latestOwnerSeenEvent = event
                EventType.ROBOT_GREETING_OWNER_TRIGGERED -> latestOwnerGreetingEvent = event
                EventType.CANDIDATE_PERSON_READY_FOR_TEACH -> {
                    perceptionLookingUntilMs = maxOf(perceptionLookingUntilMs, event.timestampMs + PERCEPTION_LOOKING_HOLD_MS)
                    perceptionAskingUntilMs = maxOf(perceptionAskingUntilMs, event.timestampMs + PERCEPTION_ASKING_HOLD_MS)
                    if (activeTeachTarget == null) {
                        val payload = CandidatePersonReadyForTeachPayload.fromJson(event.payloadJson)
                        if (payload != null) {
                            val previewBitmap = payload.previewImageBase64?.toBitmapFromBase64()
                            activeTeachTarget = TeachUnknownTarget.UnknownFace(
                                candidateId = payload.candidateId,
                                centroidEmbedding = payload.centroidEmbedding.toFiniteEmbedding(),
                                sampleCount = payload.sampleCount,
                                stableScore = payload.stableScore,
                                seenFrameCount = payload.seenFrameCount,
                                seenEncounterCount = payload.seenEncounterCount,
                                averageQualityScore = payload.averageQualityScore,
                                closestKnownPersonId = payload.closestKnownPersonId,
                                closestKnownSimilarity = payload.closestKnownSimilarity,
                                thumbnail = previewBitmap
                            )
                        }
                    }
                }
                EventType.PERSON_RECOGNIZED,
                EventType.PERSON_UNKNOWN,
                EventType.FACE_DETECTED,
                EventType.FACES_DETECTED,
                EventType.OBJECT_DETECTED -> {
                    perceptionLookingUntilMs = maxOf(perceptionLookingUntilMs, event.timestampMs + PERCEPTION_LOOKING_HOLD_MS)
                }
                EventType.UNKNOWN_FACE_CANDIDATE_READY_TO_ASK,
                EventType.UNKNOWN_OBJECT_DETECTED -> {
                    perceptionLookingUntilMs = maxOf(perceptionLookingUntilMs, event.timestampMs + PERCEPTION_LOOKING_HOLD_MS)
                    perceptionAskingUntilMs = maxOf(perceptionAskingUntilMs, event.timestampMs + PERCEPTION_ASKING_HOLD_MS)
                }
                EventType.PET_INVITATION_EMITTED -> {
                    invitationUiTriggerToken = if (event.timestampMs > 0L) {
                        event.timestampMs
                    } else {
                        System.currentTimeMillis()
                    }
                }
                // If the user interacts while an invitation is pending, accept it
                EventType.USER_INTERACTED_PET,
                EventType.AFFECTION_INTERACTION,
                EventType.PET_LONG_PRESSED -> {
                    if (pendingInvitationUntilMs > 0L && event.timestampMs <= pendingInvitationUntilMs) {
                        coroutineScope.launch {
                            eventBus.publish(
                                com.aipet.brain.brain.events.EventEnvelope.create(
                                    type = EventType.PET_INVITATION_ACCEPTED
                                )
                            )
                        }
                        invitationIgnoredCount = 0
                        pendingInvitationUntilMs = 0L
                    }
                }
                EventType.AUDIO_RESPONSE_STARTED -> {
                    val payload = com.aipet.brain.brain.events.audio.AudioResponsePayload
                        .fromJson(event.payloadJson)
                    latestAudioOutputStartedAtMs = event.timestampMs
                        .takeIf { it > 0L }
                        ?: System.currentTimeMillis()
                    if (!isBehaviorExperienceAuthoritative && payload != null) {
                        val category = try {
                            AudioCategory.valueOf(payload.category)
                        } catch (e: Exception) {
                            AudioCategory.ACKNOWLEDGMENT
                        }
                        petAnimator.playTrigger(animationInputMapper.mapSoundTrigger(category))
                    }
                    coroutineScope.launch { perceptionFusionCoordinator.onSelfPlaybackStarted() }
                }
                EventType.AUDIO_RESPONSE_COMPLETED -> {
                    coroutineScope.launch { perceptionFusionCoordinator.onSelfPlaybackStopped() }
                }
                EventType.AUDIO_RESPONSE_REQUESTED -> Unit
                else -> Unit
            }
        }
    }

    LaunchedEffect(ownerSeenReactionEngine) {
        ownerSeenReactionEngine.observePersonSeenUpdates()
    }

    LaunchedEffect(audioResponseDispatcher) {
        audioResponseDispatcher.observeRequestsAndDispatch()
    }

    LaunchedEffect(brainInteractionLoop) {
        brainInteractionLoop.observeEventsAndApplyTransitions()
    }

    LaunchedEffect(brainInteractionLoop, "inactivity_loop") {
        brainInteractionLoop.runInactivityMonitor()
    }

    // ── Behavior Intelligence v2 loop (750 ms tick) ───────────────────────────
    LaunchedEffect(
        behaviorEngine,
        attentionEngine,
        behaviorExperienceBinder,
        petIntentionExecutor,
        isBehaviorExperienceAuthoritative
    ) {
        while (true) {
            // Keep behavior execution foreground-bound; ON_START clears this marker after resume sync.
            if (lastLifecycleStopAtMs > 0L) {
                kotlinx.coroutines.delay(750L)
                continue
            }
            val ps = currentPetState
            if (ps != null) {
                val nowMs = System.currentTimeMillis()
                if (transientReactionIntent != null && nowMs >= transientReactionActiveUntilMs) {
                    transientReactionIntent = null
                    transientReactionActiveUntilMs = 0L
                }
                val appOpenGreetingForExecution = activeAppOpenGreeting?.takeIf {
                    nowMs <= activeAppOpenGreetingExpiresAtMs
                }
                if (activeAppOpenGreeting != null && appOpenGreetingForExecution == null) {
                    activeAppOpenGreeting = null
                    activeAppOpenGreetingExpiresAtMs = 0L
                }
                val ctx = WorkingContextBuilder(
                    fusionRepository = perceptionFusionRepo,
                    attentionStateRepository = attentionStateRepository,
                    emotionMomentumEngine = emotionMomentumEngine,
                    cooldownTracker = cooldownTracker,
                    relationshipStateBuilder = relationshipStateBuilder
                ).build(
                    petState = ps,
                    conditions = currentPetConditions,
                    traits = currentPetTraits,
                    session = behaviorEngine.getSessionContext(nowMs),
                    currentBehavior = null,
                    recognizedPersonFamiliarity = null,
                    recentInteractionCount = 0,
                    sessionAbsenceMs = 0L,
                    recentMemory = recentMemorySummary,
                    nowMs = nowMs,
                    evolutionContext = evolutionContext
                )
                attentionEngine.update(ctx)
                val resolvedPlan = behaviorEngine.runDecisionCycle(
                    petState = ps,
                    conditions = currentPetConditions,
                    traits = currentPetTraits,
                    recognizedPersonFamiliarity = null,
                    recentInteractionCount = 0,
                    sessionAbsenceMs = 0L,
                    recentMemory = recentMemorySummary,
                    evolutionContext = evolutionContext
                )
                if (isBehaviorExperienceAuthoritative) {
                    val experienceBundle = behaviorExperienceBinder.resolve(
                        plan = resolvedPlan,
                        currentEmotion = currentPetEmotion,
                        currentConditions = currentPetConditions,
                        appOpenGreeting = appOpenGreetingForExecution
                    )
                    val execution = petIntentionExecutor.execute(
                        bundle = experienceBundle,
                        nowMs = nowMs
                    )
                    var executionDebugState = execution.debugState
                    val transientVisualActive =
                        transientReactionIntent != null && nowMs < transientReactionActiveUntilMs
                    behaviorDrivenIntent = if (transientVisualActive) {
                        null
                    } else {
                        execution.activeVisualIntent
                    }
                    if (transientVisualActive && executionDebugState.visualResult.decision == ChannelDecision.EXECUTED) {
                        executionDebugState = executionDebugState.copy(
                            visualResult = ChannelExecutionResult(
                                decision = ChannelDecision.SUPPRESSED,
                                reason = "transient_visual_override_active"
                            )
                        )
                    }
                    behaviorTalkDirective = execution.talkDirective
                    execution.audioDirective?.let { audioDirective ->
                        eventBus.publish(
                            EventEnvelope.create(
                                type = EventType.AUDIO_RESPONSE_REQUESTED,
                                timestampMs = nowMs,
                                payloadJson = AudioResponseRequestPayload(
                                    category = audioDirective.category.label,
                                    priority = 3,
                                    interruptPolicy = "INTERRUPT_NONE",
                                    cooldownKey = audioDirective.cooldownKey,
                                    timestamp = nowMs
                                ).toJson()
                            )
                        )
                    }
                    if (execution.audioDirective == null &&
                        executionDebugState.audioResult.decision == ChannelDecision.SUPPRESSED &&
                        executionDebugState.mappedAudioCategory != null
                    ) {
                        val suppressionReason = executionDebugState.audioResult.reason
                        val isAudioRateOrCooldownSuppression =
                            suppressionReason?.startsWith("global_interval_active_") == true ||
                                suppressionReason?.startsWith("same_category_interval_active_") == true ||
                                suppressionReason?.startsWith("cooldown_active_") == true
                        if (isAudioRateOrCooldownSuppression) {
                            eventBus.publish(
                                EventEnvelope.create(
                                    type = EventType.AUDIO_RESPONSE_SKIPPED,
                                    timestampMs = nowMs,
                                    payloadJson = com.aipet.brain.brain.events.audio.AudioResponsePayload(
                                        category = executionDebugState.mappedAudioCategory.label,
                                        clipId = null,
                                        durationMs = 0L,
                                        priority = 0,
                                        timestamp = nowMs,
                                        reason = suppressionReason
                                    ).toJson()
                                )
                            )
                        }
                    }
                    if (appOpenGreetingForExecution != null && executionDebugState.accepted) {
                        val consumedByTalk = executionDebugState.mappedTalkDedupeKey == APP_OPEN_GREETING_DEDUPE_KEY &&
                            executionDebugState.talkResult.decision == ChannelDecision.EXECUTED
                        val consumedByAudio = executionDebugState.mappedAudioCategory == AudioCategory.GREETING &&
                            executionDebugState.audioResult.decision == ChannelDecision.EXECUTED
                        if (consumedByTalk || consumedByAudio) {
                            activeAppOpenGreeting = null
                            activeAppOpenGreetingExpiresAtMs = 0L
                        }
                    }
                    behaviorExperienceDebugState = executionDebugState
                } else {
                    behaviorDrivenIntent = null
                    behaviorTalkDirective = null
                    behaviorExperienceDebugState = BehaviorExperienceDebugState.EMPTY
                }

                // Invitation system: expire pending invitations and emit new ones
                if (pendingInvitationUntilMs > 0L && nowMs > pendingInvitationUntilMs) {
                    eventBus.publish(
                        com.aipet.brain.brain.events.EventEnvelope.create(
                            type = EventType.PET_INVITATION_IGNORED,
                            timestampMs = nowMs
                        )
                    )
                    invitationIgnoredCount++
                    pendingInvitationUntilMs = 0L
                }
                val evo = evolutionContext
                if (evo != null && pendingInvitationUntilMs == 0L) {
                    val decision = invitationContextResolver.resolve(
                        state = ps,
                        conditions = currentPetConditions,
                        traits = currentPetTraits,
                        bond = evo.bond,
                        dayPhase = evo.dayPhase,
                        reunionType = evo.reunionType,
                        expectationState = evo.expectationState,
                        ignoredCount = invitationIgnoredCount,
                        msSinceLastInvitation = nowMs - lastInvitationMs
                    )
                    val intentType = decision.intentType
                    if (decision.shouldInvite && intentType != null) {
                        eventBus.publish(
                            com.aipet.brain.brain.events.EventEnvelope.create(
                                type = EventType.PET_INVITATION_EMITTED,
                                payloadJson = """{"intentType":"${intentType.name}","confidence":${decision.confidenceScore}}""",
                                timestampMs = nowMs
                            )
                        )
                        lastInvitationMs = nowMs
                        pendingInvitationUntilMs = nowMs + INVITATION_RESPONSE_WINDOW_MS
                    }
                }
                evolutionCoordinator.finalizeEpisodeIfInactive(
                    petState = ps,
                    traits = currentPetTraits
                )
            }
            kotlinx.coroutines.delay(750L)
        }
    }

    LaunchedEffect(traitsEngine) {
        traitsEngine.initializeIfNeeded()
    }

    LaunchedEffect(traitsEngine, "traits_rules") {
        traitsEngine.observeEventsAndApplyRules()
    }

    LaunchedEffect(familiarityEngine) {
        familiarityEngine.observeEventsAndApplyRules()
    }

    LaunchedEffect(workingMemoryUpdater) {
        workingMemoryUpdater.observeEventsAndUpdateMemory()
    }

    LaunchedEffect(audioStimulusObserver) {
        audioStimulusObserver.observeEventsAndMapStimuli()
    }

    LaunchedEffect(loudSoundReactionRule, isBehaviorExperienceAuthoritative) {
        if (isBehaviorExperienceAuthoritative) {
            return@LaunchedEffect
        }
        loudSoundReactionRule.observeStimuliAndReact()
    }

    LaunchedEffect(voiceActivityAcknowledgmentRule, isBehaviorExperienceAuthoritative) {
        if (isBehaviorExperienceAuthoritative) {
            return@LaunchedEffect
        }
        voiceActivityAcknowledgmentRule.observeStimuliAndReact()
    }

    LaunchedEffect(wakeWordAcknowledgmentRule, isBehaviorExperienceAuthoritative) {
        if (isBehaviorExperienceAuthoritative) {
            return@LaunchedEffect
        }
        wakeWordAcknowledgmentRule.observeStimuliAndReact()
    }

    LaunchedEffect(keywordIntentCommandRule) {
        keywordIntentCommandRule.observeStimuliAndReact()
    }

    LaunchedEffect(eventBus, "app_started") {
        eventBus.publish(EventEnvelope.create(type = EventType.APP_STARTED))
    }

    LaunchedEffect(currentScreen) {
        if (currentScreen != AppScreen.Camera) {
            lastPublishedFaceCount = 0
            recognizedPersonLabel = null
        }
    }

    DisposableEffect(audioPlaybackEngine, faceEmbeddingEngine) {
        onDispose {
            audioPlaybackEngine.release()
            faceEmbeddingEngine.close()
        }
    }

    // Start evolution event observation for episode auto-recording
    LaunchedEffect(evolutionCoordinator) {
        evolutionCoordinator.startObservingEvents()
    }

    suspend fun syncRuntimeStateOnResume(
        resumedAtMs: Long,
        absenceMs: Long
    ): ResumeLifecycleSnapshot {
        val snapshot = withContext(Dispatchers.IO) {
            val profile = activePetProfile ?: petProfileRepository.getOrCreateActiveProfile()
            val traits = currentPetTraits ?: petTraitRepository.getOrCreateForPet(profile.id)
            val previousState = petStateRepository.getOrCreateState()
            val dayBoundary = petDayBoundaryResolver.resolve(
                previousUpdatedAt = previousState.lastUpdatedAt,
                now = resumedAtMs
            )
            val decayedState = petStateDecayEngine.applyDecay(
                currentState = previousState,
                now = resumedAtMs
            )
            val absenceBucket = absenceClassifier.classify(decayedState, resumedAtMs)
            val stateAfterNeglect = neglectTracker.applyIfNeeded(
                state = decayedState,
                absenceBucket = absenceBucket
            )
            val stateWithOpenTimestamp = stateAfterNeglect.copy(lastOpenAt = resumedAtMs)
            val persistedState = petStateRepository.updateState(stateWithOpenTimestamp)
            val conditions = petConditionResolver.resolve(persistedState)
            val emotion = petEmotionResolver.resolve(persistedState, conditions)
            val relationshipStage = relationshipStageResolver.resolve(persistedState)
            val elapsedMinutes = (absenceMs / 60_000L).coerceAtLeast(0L)
            val decayPayload = com.aipet.brain.brain.events.PetStateDecayAppliedPayload(
                appliedAtMs = resumedAtMs,
                absenceBucket = absenceBucket.name,
                elapsedMinutes = elapsedMinutes,
                energyDelta = persistedState.energy - previousState.energy,
                hungerDelta = persistedState.hunger - previousState.hunger,
                sleepinessDelta = persistedState.sleepiness - previousState.sleepiness,
                socialDelta = persistedState.social - previousState.social,
                comfortDelta = persistedState.comfort - previousState.comfort,
                stimulationDelta = persistedState.stimulation - previousState.stimulation,
                trustDelta = persistedState.trustScore - previousState.trustScore
            )
            ResumeLifecycleSnapshot(
                profile = profile,
                state = persistedState,
                emotion = emotion,
                traits = traits,
                conditions = conditions,
                absenceBucket = absenceBucket,
                relationshipStage = relationshipStage,
                dayBoundaryType = dayBoundary.type,
                summaryDate = dayBoundary.currentDate,
                decayPayload = decayPayload
            )
        }
        activePetProfile = snapshot.profile
        currentPetState = snapshot.state
        currentPetEmotion = snapshot.emotion
        currentPetTraits = snapshot.traits
        currentPetConditions = snapshot.conditions
        currentAbsenceBucket = snapshot.absenceBucket
        currentRelationshipStage = snapshot.relationshipStage
        currentDayBoundaryType = snapshot.dayBoundaryType
        currentSummaryDate = snapshot.summaryDate
        pendingInvitationUntilMs = 0L
        invitationUiTriggerToken = 0L
        homeInteractionFeedback = null
        latestBehaviorDecisionSource = "resume_sync"
        latestBehaviorDecision = null
        currentGreetingStyle = null
        appOpenGreeting = null
        activeAppOpenGreeting = null
        activeAppOpenGreetingExpiresAtMs = 0L
        transientClearJobHolder[0]?.cancel()
        transientReactionIntent = null
        transientReactionActiveUntilMs = 0L
        behaviorDrivenIntent = null
        behaviorTalkDirective = null
        behaviorExperienceDebugState = BehaviorExperienceDebugState.EMPTY
        lastAudioRequestDispatchAtMs = 0L
        lastAudioRequestDispatchCategory = null
        petIntentionExecutor.resetRuntimeState()
        sessionInteractionTracker.resetForNewSession()
        val lastDirectInteractionAt = snapshot.state.lastMeaningfulInteractionAt
            .takeIf { it > 0L } ?: snapshot.state.lastUpdatedAt
        val startupCalendar = java.util.Calendar.getInstance().apply {
            timeInMillis = resumedAtMs
        }
        behaviorEngine.setSessionContext(
            SessionContext(
                sessionStartMs = resumedAtMs,
                sessionAgeMs = 0L,
                hourOfDay = startupCalendar.get(java.util.Calendar.HOUR_OF_DAY),
                isFirstGreetToday = snapshot.dayBoundaryType == PetDayBoundaryType.NEW_DAY_RETURN,
                sessionInteractionCount = 0,
                msSinceLastDirectInteraction = (resumedAtMs - lastDirectInteractionAt)
                    .coerceAtLeast(0L),
                isReturningAfterLongAbsence = snapshot.absenceBucket == com.aipet.brain.brain.pet.AbsenceBucket.LONG_RETURN ||
                    snapshot.absenceBucket == com.aipet.brain.brain.pet.AbsenceBucket.NEGLECT_RETURN,
                nowMs = resumedAtMs
            )
        )
        eventBus.publish(
            EventEnvelope.create(
                type = EventType.PET_STATE_DECAY_APPLIED,
                payloadJson = snapshot.decayPayload.toJson(),
                timestampMs = resumedAtMs
            )
        )
        return snapshot
    }

    // Finalize episode on app background (ON_STOP) for reliable session capture
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, evolutionCoordinator) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                lastLifecycleStopAtMs = System.currentTimeMillis()
                transientClearJobHolder[0]?.cancel()
                transientReactionIntent = null
                transientReactionActiveUntilMs = 0L
                behaviorDrivenIntent = null
                behaviorTalkDirective = null
                behaviorExperienceDebugState = BehaviorExperienceDebugState.EMPTY
                appOpenGreeting = null
                activeAppOpenGreeting = null
                activeAppOpenGreetingExpiresAtMs = 0L
                latestAudioOutputStartedAtMs = 0L
                lastAudioRequestDispatchAtMs = 0L
                lastAudioRequestDispatchCategory = null
                pendingInvitationUntilMs = 0L
                invitationUiTriggerToken = 0L
                petIntentionExecutor.resetRuntimeState()
                val state = currentPetState
                if (state != null) {
                    evolutionCoordinator.onSessionEnded(petState = state, traits = currentPetTraits)
                }
            } else if (event == androidx.lifecycle.Lifecycle.Event.ON_START) {
                if (!hasAppliedAppOpenLifecycle) {
                    lastLifecycleStopAtMs = 0L
                    return@LifecycleEventObserver
                }
                val stoppedAtMs = lastLifecycleStopAtMs
                if (stoppedAtMs <= 0L) {
                    return@LifecycleEventObserver
                }
                val nowMs = System.currentTimeMillis()
                val absenceMs = (nowMs - stoppedAtMs).coerceAtLeast(0L)
                if (absenceMs < RESUME_EVOLUTION_MIN_ABSENCE_MS) {
                    lastLifecycleStopAtMs = 0L
                    return@LifecycleEventObserver
                }
                coroutineScope.launch {
                    runCatching {
                        val resumeSnapshot = syncRuntimeStateOnResume(
                            resumedAtMs = nowMs,
                            absenceMs = absenceMs
                        )
                        evolutionCoordinator.onAppOpened(
                            petState = resumeSnapshot.state,
                            petTraits = resumeSnapshot.traits,
                            absenceMsOverride = absenceMs
                        )
                    }.onFailure { error ->
                        Log.w(
                            DEBUG_STARTUP_TAG,
                            "Resume runtime sync failed; keeping existing state. absenceMs=$absenceMs",
                            error
                        )
                    }
                    // Consume this stop marker so ON_START can only trigger one reopen lifecycle pass.
                    if (lastLifecycleStopAtMs == stoppedAtMs) {
                        lastLifecycleStopAtMs = 0L
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val objectDetectionEngineForBackground = remember(appContext) {
        runCatching { RealObjectDetectionEngine(appContext.assets) }.getOrNull()
    }
    val backgroundOrchestrator = remember(
        appContext,
        lifecycleOwner,
        coroutineScope,
        eventBus,
        faceEmbeddingEngine,
        personRecognitionService,
        recognitionDecisionEventPublisher,
        objectDetectionEngineForBackground,
        objectRepository,
        unknownFaceCandidateStore,
        faceProfileStore
    ) {
        BackgroundPerceptionOrchestrator(
            context = appContext,
            lifecycleOwner = lifecycleOwner,
            coroutineScope = coroutineScope,
            eventBus = eventBus,
            faceEmbeddingEngine = faceEmbeddingEngine,
            personRecognitionService = personRecognitionService,
            recognitionDecisionEventPublisher = recognitionDecisionEventPublisher,
            objectDetectionEngine = objectDetectionEngineForBackground,
            objectRepository = objectRepository,
            unknownFaceCandidateStore = unknownFaceCandidateStore,
            faceProfileStore = faceProfileStore,
            onUnknownObjectDetected = { thumbnail, label, confidence, _ ->
                if (activeTeachTarget == null) {
                    activeTeachTarget = TeachUnknownTarget.UnknownObject(label, confidence, thumbnail)
                }
            }
        )
    }

    LaunchedEffect(hasRequestedCameraPermission) {
        backgroundOrchestrator.start()
    }

    DisposableEffect(backgroundOrchestrator, objectDetectionEngineForBackground) {
        onDispose {
            backgroundOrchestrator.release()
            objectDetectionEngineForBackground?.close()
        }
    }

    suspend fun publishGameplayAudioResponse(
        category: com.aipet.brain.app.ui.audio.model.AudioCategory,
        timestampMs: Long,
        cooldownKey: String
    ) {
        publishAudioResponseRequestWithRuntimeGuards(
            category = category,
            timestampMs = timestampMs,
            cooldownKey = cooldownKey,
            priority = 2,
            source = "gameplay_action"
        )
    }

    suspend fun resetPetDataToFreshState() {
        if (isResettingPetData) {
            return
        }
        isResettingPetData = true
        try {
            transientClearJobHolder[0]?.cancel()
            transientReactionIntent = null
            transientReactionActiveUntilMs = 0L
            behaviorDrivenIntent = null
            behaviorTalkDirective = null
            behaviorExperienceDebugState = BehaviorExperienceDebugState.EMPTY
            appOpenGreeting = null
            activeAppOpenGreeting = null
            activeAppOpenGreetingExpiresAtMs = 0L
            latestAudioOutputStartedAtMs = 0L
            lastAudioRequestDispatchAtMs = 0L
            lastAudioRequestDispatchCategory = null
            pendingInvitationUntilMs = 0L
            invitationIgnoredCount = 0
            invitationUiTriggerToken = 0L
            lastInvitationMs = 0L
            lastLifecycleStopAtMs = 0L
            latestEvent = null
            latestOwnerSeenEvent = null
            latestOwnerGreetingEvent = null
            recognizedPersonLabel = null
            recognitionProbeSummary = "not_run"
            activeTeachTarget = null
            perceptionLookingUntilMs = 0L
            perceptionAskingUntilMs = 0L
            currentPetState = null
            currentPetEmotion = PetEmotion.IDLE
            currentPetTraits = null
            currentPetConditions = emptySet()
            currentAbsenceBucket = null
            currentGreetingStyle = null
            currentRelationshipStage = null
            currentDayBoundaryType = null
            currentSummaryDate = java.time.LocalDate.now()
            homeInteractionFeedback = null
            latestBehaviorDecisionSource = null
            latestBehaviorDecision = null
            petNameDraft = PetProfileRepository.DEFAULT_PET_NAME
            activePetProfile = null
            editingPersonId = null
            selectedPersonId = null
            currentScreenName = AppScreen.Home.name
            hasAppliedAppOpenLifecycle = false
            sessionInteractionTracker.resetForNewSession()
            petIntentionExecutor.resetRuntimeState()
            withContext(Dispatchers.IO) {
                database.clearAllTables()
                petOnboardingStore.clear()
            }
            teachSessionId = UUID.randomUUID().toString()
        } finally {
            isResettingPetData = false
        }
    }

    suspend fun markPendingInvitationIgnored(
        timestampMs: Long = System.currentTimeMillis()
    ) {
        if (pendingInvitationUntilMs <= 0L) {
            return
        }
        eventBus.publish(
            EventEnvelope.create(
                type = EventType.PET_INVITATION_IGNORED,
                timestampMs = timestampMs
            )
        )
        invitationIgnoredCount++
        pendingInvitationUntilMs = 0L
    }

    suspend fun publishVoiceCommandAudioFeedback(intent: AudioIntent): Boolean {
        if (isBehaviorExperienceAuthoritative) {
            Log.d(
                VOICE_FEEDBACK_TAG,
                "Skipped direct voice audio feedback in behavior-authoritative mode; " +
                    "behavior loop owns audio reaction. intent=${intent.name}"
            )
            return false
        }
        val category = when (intent) {
            AudioIntent.WAKE_UP -> AudioCategory.ACKNOWLEDGMENT
            AudioIntent.LEARN_PERSON -> AudioCategory.ACKNOWLEDGMENT
            AudioIntent.LEARN_OBJECT -> AudioCategory.ACKNOWLEDGMENT
            AudioIntent.PLAY_RANDOM -> AudioCategory.HAPPY
            AudioIntent.UNKNOWN -> null
        } ?: return false

        val timestampMs = System.currentTimeMillis()
        val suppressionReason = runCatching {
            publishAudioResponseRequestWithRuntimeGuards(
                category = category,
                timestampMs = timestampMs,
                cooldownKey = "voice_cmd_${intent.name.lowercase()}",
                priority = 3,
                source = "voice_command"
            )
        }.getOrElse { error ->
            Log.w(
                VOICE_FEEDBACK_TAG,
                "Voice feedback failed. intent=${intent.name}, category=${category.label}, reason=${error.message}"
            )
            return false
        }
        return if (suppressionReason == null) {
            Log.d(
                VOICE_FEEDBACK_TAG,
                "Voice feedback queued. intent=${intent.name}, category=${category.label}"
            )
            true
        } else {
            Log.d(
                VOICE_FEEDBACK_TAG,
                "Voice feedback suppressed. intent=${intent.name}, category=${category.label}, reason=$suppressionReason"
            )
            false
        }
    }

    suspend fun playVoiceCommandAvatarReaction(intent: AudioIntent): Boolean {
        if (activeTeachTarget != null && intent != AudioIntent.WAKE_UP) {
            Log.d(
                VOICE_AVATAR_TAG,
                "Skipped voice avatar reaction due to active teach flow. intent=${intent.name}"
            )
            return false
        }
        if (isBehaviorExperienceAuthoritative) {
            Log.d(
                VOICE_AVATAR_TAG,
                "Skipped direct voice avatar reaction in behavior-authoritative mode; " +
                    "behavior loop owns avatar reaction. intent=${intent.name}"
            )
            return false
        }
        val emotion = when (intent) {
            AudioIntent.WAKE_UP -> PetEmotion.CURIOUS
            AudioIntent.LEARN_PERSON -> PetEmotion.CURIOUS
            AudioIntent.LEARN_OBJECT -> PetEmotion.CURIOUS
            AudioIntent.PLAY_RANDOM -> PetEmotion.EXCITED
            AudioIntent.UNKNOWN -> null
        } ?: return false

        val trigger = animationInputMapper.mapGenericReaction(
            emotion = emotion,
            source = PetAnimationSource.REACTION
        )
        return runCatching {
            petAnimator.playTrigger(trigger, durationMs = 900L)
        }.onSuccess {
            Log.d(
                VOICE_AVATAR_TAG,
                "Voice avatar reaction played. intent=${intent.name}, emotion=${emotion.name}"
            )
        }.onFailure { error ->
            Log.w(
                VOICE_AVATAR_TAG,
                "Voice avatar reaction skipped. intent=${intent.name}, reason=${error.message}"
            )
        }.isSuccess
    }

    suspend fun handlePetInteraction(
        interactionType: PetInteractionType,
        source: String
    ) {
        val interactedAtMs = System.currentTimeMillis()
        val gameplayAction = PetGameplayAction.fromInteractionType(interactionType)
        val activePetName = activePetProfile?.name ?: PetProfileRepository.DEFAULT_PET_NAME
        val cooldownDecision = petGameplayCooldownGate.evaluate(
            action = gameplayAction,
            attemptedAtMs = interactedAtMs
        )
        if (!cooldownDecision.isAllowed) {
            homeInteractionFeedback = PetGameplayFeedbackFormatter.blocked(
                petName = activePetName,
                action = gameplayAction,
                remainingMs = cooldownDecision.remainingMs
            )
            return
        }
        val resolvedInteraction = withContext(Dispatchers.IO) {
            val profile = petProfileRepository.getOrCreateActiveProfile()
            val traits = petTraitRepository.getOrCreateForPet(profile.id)
            val currentState = petStateRepository.getOrCreateState()
            val nextState = petInteractionStateReducer.apply(
                currentState = currentState,
                interactionType = interactionType,
                interactedAtMs = interactedAtMs
            )
            // NX3: apply care action effects on top of the old reducer for NX state fields
            val careActionType = if (interactionType == PetInteractionType.LONG_PRESS) {
                com.aipet.brain.brain.pet.CareActionType.LONG_PRESS
            } else {
                com.aipet.brain.brain.pet.CareActionType.TAP
            }
            val careResult = careActionProcessor.process(careActionType, currentState, traits, interactedAtMs)
            val mergedState = nextState.copy(
                comfort = careResult.stateAfter.comfort,
                stimulation = careResult.stateAfter.stimulation,
                moodValence = careResult.stateAfter.moodValence,
                moodArousal = careResult.stateAfter.moodArousal,
                trustScore = careResult.stateAfter.trustScore,
                attachmentScore = careResult.stateAfter.attachmentScore,
                neglectStreak = careResult.stateAfter.neglectStreak,
                careStreak = careResult.stateAfter.careStreak,
                lastMeaningfulInteractionAt = careResult.stateAfter.lastMeaningfulInteractionAt
            )
            val persistedState = petStateRepository.updateState(mergedState)
            val evolvedTraits = petTraitEvolutionEngine.applyInteraction(
                current = traits,
                interactionType = interactionType,
                appliedAtMs = interactedAtMs
            )
            val persistedTraits = petTraitRepository.save(evolvedTraits)
            val conditions = petConditionResolver.resolve(persistedState)
            val emotion = petEmotionResolver.resolve(persistedState, conditions)
            val decision = petBehaviorWeightResolver.resolveInteractionEmotion(
                interactionType = interactionType,
                context = PetBehaviorContext(
                    state = persistedState,
                    conditions = conditions,
                    traits = persistedTraits
                )
            )
            InteractionAppliedSnapshot(
                previousState = currentState,
                state = persistedState,
                emotion = emotion,
                traits = persistedTraits,
                conditions = conditions,
                decision = decision,
                interactedAtMs = interactedAtMs,
                feedback = PetGameplayFeedbackFormatter.forInteraction(
                    petName = profile.name,
                    interactionType = interactionType,
                    previousState = currentState,
                    updatedState = persistedState
                )
            )
        }
        petGameplayCooldownGate.recordSuccess(gameplayAction, interactedAtMs)
        currentPetState = resolvedInteraction.state
        currentPetEmotion = resolvedInteraction.emotion
        currentPetTraits = resolvedInteraction.traits
        currentPetConditions = resolvedInteraction.conditions
        homeInteractionFeedback = resolvedInteraction.feedback
        latestBehaviorDecisionSource = interactionType.name.lowercase()
        latestBehaviorDecision = resolvedInteraction.decision
        if (isBehaviorExperienceAuthoritative) {
            behaviorDrivenIntent = null
        }
        queueTransientReaction(
            intent = TapReactionPresentationMapper.mapToAvatarIntent(
                resultingEmotion = resolvedInteraction.emotion,
                interactionType = interactionType
            )
        )
        if (!isBehaviorExperienceAuthoritative) {
            petAnimator.syncInputFrame(
                animationInputMapper.mapFrame(
                    state = resolvedInteraction.state,
                    emotion = resolvedInteraction.emotion,
                    conditions = resolvedInteraction.conditions,
                    traits = resolvedInteraction.traits
                )
            )
            petAnimator.playTrigger(
                animationInputMapper.mapInteractionTrigger(
                    interactionType = interactionType,
                    decision = resolvedInteraction.decision
                )
            )
        }
        if (!isBehaviorExperienceAuthoritative) {
            publishGameplayAudioResponse(
                category = PetGameplayAudioMapper.categoryForInteraction(interactionType),
                timestampMs = resolvedInteraction.interactedAtMs,
                cooldownKey = "pet_${interactionType.name.lowercase()}"
            )
        }
        appOpenGreeting = null
        activeAppOpenGreeting = null
        activeAppOpenGreetingExpiresAtMs = 0L
        sessionInteractionTracker.recordInteraction()
        eventBus.publish(
            EventEnvelope.create(
                type = if (interactionType == PetInteractionType.LONG_PRESS) {
                    EventType.PET_LONG_PRESSED
                } else {
                    EventType.USER_INTERACTED_PET
                },
                payloadJson = UserInteractedPetEventPayload(
                    interactedAtMs = resolvedInteraction.interactedAtMs,
                    source = source,
                    interactionType = interactionType.name,
                    resultingMood = resolvedInteraction.state.mood.name,
                    socialDelta = resolvedInteraction.state.social - resolvedInteraction.previousState.social,
                    bondDelta = resolvedInteraction.state.bond - resolvedInteraction.previousState.bond,
                    feedbackText = resolvedInteraction.feedback.text
                ).toJson(),
                timestampMs = resolvedInteraction.interactedAtMs
            )
        )
        // Feed touch to perception fusion
        coroutineScope.launch {
            if (interactionType == PetInteractionType.LONG_PRESS) {
                perceptionFusionCoordinator.onLongPress()
            } else {
                perceptionFusionCoordinator.onTap()
            }
        }
    }

    suspend fun handlePetActivity(
        useCase: PetActivityUseCase
    ) {
        val actedAtMs = System.currentTimeMillis()
        val activityType = when (useCase) {
            is FeedPetUseCase -> PetActivityType.FEED
            is PlayWithPetUseCase -> PetActivityType.PLAY
            is LetPetRestUseCase -> PetActivityType.REST
            else -> null
        }
        val gameplayAction = activityType?.let(PetGameplayAction::fromActivityType)
        val activePetName = activePetProfile?.name ?: PetProfileRepository.DEFAULT_PET_NAME
        if (gameplayAction != null) {
            val cooldownDecision = petGameplayCooldownGate.evaluate(
                action = gameplayAction,
                attemptedAtMs = actedAtMs
            )
            if (!cooldownDecision.isAllowed) {
                homeInteractionFeedback = PetGameplayFeedbackFormatter.blocked(
                    petName = activePetName,
                    action = gameplayAction,
                    remainingMs = cooldownDecision.remainingMs
                )
                return
            }
        }
        val resolvedActivity = withContext(Dispatchers.IO) {
            val profile = petProfileRepository.getOrCreateActiveProfile()
            val traits = petTraitRepository.getOrCreateForPet(profile.id)
            val currentState = petStateRepository.getOrCreateState()
            val activityResult = useCase.execute(
                currentState = currentState,
                actedAtMs = actedAtMs
            )
            // NX3: augment activity state with care action processor NX fields
            val careActionType = when (activityResult.activityType) {
                PetActivityType.FEED -> CareActionType.FEED
                PetActivityType.PLAY -> CareActionType.PLAY
                PetActivityType.REST -> CareActionType.REST
            }
            val careResult = careActionProcessor.process(careActionType, currentState, traits, actedAtMs)
            val careEnrichedState = activityResult.updatedState.copy(
                comfort = careResult.stateAfter.comfort,
                stimulation = careResult.stateAfter.stimulation,
                moodValence = careResult.stateAfter.moodValence,
                moodArousal = careResult.stateAfter.moodArousal,
                trustScore = careResult.stateAfter.trustScore,
                attachmentScore = careResult.stateAfter.attachmentScore,
                neglectStreak = careResult.stateAfter.neglectStreak,
                careStreak = careResult.stateAfter.careStreak,
                lastMeaningfulInteractionAt = careResult.stateAfter.lastMeaningfulInteractionAt
            )
            val persistedState = petStateRepository.updateState(careEnrichedState)
            val evolvedTraits = petTraitEvolutionEngine.applyActivity(
                current = traits,
                activityType = activityResult.activityType,
                appliedAtMs = actedAtMs
            )
            val persistedTraits = petTraitRepository.save(evolvedTraits)
            val conditions = petConditionResolver.resolve(persistedState)
            val emotion = petEmotionResolver.resolve(persistedState, conditions)
            val decision = petBehaviorWeightResolver.resolveActivityEmotion(
                activityType = activityResult.activityType,
                context = PetBehaviorContext(
                    state = persistedState,
                    conditions = conditions,
                    traits = persistedTraits
                )
            )
            ActivityAppliedSnapshot(
                result = activityResult.copy(updatedState = persistedState),
                emotion = emotion,
                traits = persistedTraits,
                conditions = conditions,
                decision = decision,
                feedback = PetGameplayFeedbackFormatter.forActivity(
                    petName = profile.name,
                    result = activityResult.copy(updatedState = persistedState)
                ),
                careActionType = careActionType,
                careResult = careResult
            )
        }
        gameplayAction?.let { petGameplayCooldownGate.recordSuccess(it, actedAtMs) }
        currentPetState = resolvedActivity.result.updatedState
        currentPetEmotion = resolvedActivity.emotion
        currentPetTraits = resolvedActivity.traits
        currentPetConditions = resolvedActivity.conditions
        homeInteractionFeedback = resolvedActivity.feedback
        latestBehaviorDecisionSource = resolvedActivity.result.activityType.name.lowercase()
        latestBehaviorDecision = resolvedActivity.decision
        if (isBehaviorExperienceAuthoritative) {
            behaviorDrivenIntent = null
        }
        sessionInteractionTracker.recordInteraction()
        queueTransientReaction(
            intent = ActivityReactionPresentationMapper.mapToAvatarIntent(
                activityType = resolvedActivity.result.activityType,
                resultingEmotion = resolvedActivity.emotion
            )
        )
        if (!isBehaviorExperienceAuthoritative) {
            petAnimator.syncInputFrame(
                animationInputMapper.mapFrame(
                    state = resolvedActivity.result.updatedState,
                    emotion = resolvedActivity.emotion,
                    conditions = resolvedActivity.conditions,
                    traits = resolvedActivity.traits
                )
            )
            petAnimator.playTrigger(
                animationInputMapper.mapActivityTrigger(
                    activityType = resolvedActivity.result.activityType,
                    decision = resolvedActivity.decision
                )
            )
        }
        if (!isBehaviorExperienceAuthoritative) {
            publishGameplayAudioResponse(
                category = PetGameplayAudioMapper.categoryForActivity(resolvedActivity.result),
                timestampMs = actedAtMs,
                cooldownKey = "pet_${resolvedActivity.result.activityType.name.lowercase()}"
            )
        }
        appOpenGreeting = null
        activeAppOpenGreeting = null
        activeAppOpenGreetingExpiresAtMs = 0L
        eventBus.publish(
            EventEnvelope.create(
                type = resolvedActivity.result.activityType.toEventType(),
                payloadJson = PetActivityAppliedEventPayload(
                    activityType = resolvedActivity.result.activityType.name,
                    actedAtMs = actedAtMs,
                    reason = resolvedActivity.result.reason,
                    resultingMood = resolvedActivity.result.updatedState.mood.name,
                    energyDelta = resolvedActivity.result.delta.energyDelta,
                    hungerDelta = resolvedActivity.result.delta.hungerDelta,
                    sleepinessDelta = resolvedActivity.result.delta.sleepinessDelta,
                    socialDelta = resolvedActivity.result.delta.socialDelta,
                    bondDelta = resolvedActivity.result.delta.bondDelta,
                    feedbackText = resolvedActivity.feedback.text
                ).toJson(),
                timestampMs = actedAtMs
            )
        )
        eventBus.publish(
            EventEnvelope.create(
                type = EventType.PET_CARE_ACTION_APPLIED,
                payloadJson = CareActionAppliedPayload(
                    actionType = resolvedActivity.careActionType.name,
                    appliedAtMs = actedAtMs,
                    resultingEmotion = resolvedActivity.careResult.resultingEmotion.name,
                    didDiminishingReturnsApply = resolvedActivity.careResult.didDiminishingReturnsApply,
                    isRepairAction = resolvedActivity.careResult.isRepairAction,
                    burstCount = resolvedActivity.careResult.burstCount,
                    comfortDelta = resolvedActivity.careResult.comfortDelta,
                    trustDelta = resolvedActivity.careResult.trustDelta,
                    socialDelta = resolvedActivity.careResult.socialDelta,
                    moodValenceDelta = resolvedActivity.careResult.moodValenceDelta,
                    bondDelta = resolvedActivity.careResult.bondDelta,
                    neglectStreakDelta = resolvedActivity.careResult.neglectStreakDelta
                ).toJson(),
                timestampMs = actedAtMs
            )
        )
    }

    suspend fun completePetNamingOnboarding(
        requestedName: String?,
        shouldKeepCurrentName: Boolean
    ) {
        val currentProfile = activePetProfile ?: withContext(Dispatchers.IO) {
            petProfileRepository.getOrCreateActiveProfile()
        }
        val normalizedRequestedName = requestedName
            ?.trim()
            ?.take(MAX_PET_NAME_LENGTH)
            .orEmpty()
        val finalName = if (shouldKeepCurrentName || normalizedRequestedName.isBlank()) {
            currentProfile.name
        } else {
            normalizedRequestedName
        }
        val savedProfile = withContext(Dispatchers.IO) {
            val updatedProfile = if (finalName == currentProfile.name) {
                currentProfile
            } else {
                petProfileRepository.saveActiveProfile(
                    currentProfile.copy(name = finalName)
                )
            }
            petOnboardingStore.markNamingCompleted(updatedProfile.id)
            updatedProfile
        }
        activePetProfile = savedProfile
        petNameDraft = savedProfile.name
        petAnimator.playTrigger(
            animationInputMapper.mapGenericReaction(
                emotion = PetEmotion.HAPPY,
                source = PetAnimationSource.REACTION
            ),
            durationMs = 1_000L
        )
        currentScreenName = AppScreen.Home.name
    }

    // Placed here so onPlayRandom/onAcceptedAudioFeedback/onAcceptedAvatarReaction lambdas can
    // reference the local suspend functions defined above (Kotlin local functions require
    // declaration before use at the call site).
    val latestCurrentScreenForAudioRule by rememberUpdatedState(currentScreen)
    val latestTeachTargetForAudioRule by rememberUpdatedState(activeTeachTarget)
    val localAudioIntentCommandRule = remember(
        eventBus,
        brainInteractionLoop,
        playWithPetUseCase
        // brainStateSnapshot.currentState is intentionally excluded: currentBrainState reads
        // live state at call time, so including it would recreate and restart the observer
        // coroutine on every SLEEPY↔CURIOUS transition unnecessarily.
    ) {
        LocalAudioIntentCommandRule(
            eventBus = eventBus,
            currentBrainState = {
                brainStateStore.currentSnapshot().currentState
            },
            isTeachPersonFlowActive = {
                latestCurrentScreenForAudioRule == AppScreen.TeachPerson ||
                    latestTeachTargetForAudioRule is TeachUnknownTarget.UnknownFace
            },
            isTeachObjectFlowActive = {
                latestTeachTargetForAudioRule is TeachUnknownTarget.UnknownObject
            },
            isExclusiveFlowActive = {
                latestTeachTargetForAudioRule != null
            },
            isPlayRandomEntryAvailable = { true },
            onWakeUp = { timestampMs ->
                brainInteractionLoop.forceWake(timestampMs)
            },
            onLearnPerson = {
                currentScreenName = AppScreen.TeachPerson.name
            },
            onLearnObject = {
                currentScreenName = AppScreen.Camera.name
            },
            onPlayRandom = {
                handlePetActivity(playWithPetUseCase)
            },
            onAcceptedAudioFeedback = { intent ->
                publishVoiceCommandAudioFeedback(intent)
            },
            onAcceptedAvatarReaction = { intent ->
                playVoiceCommandAvatarReaction(intent)
            }
        )
    }
    LaunchedEffect(localAudioIntentCommandRule) {
        localAudioIntentCommandRule.observeEventsAndRoute()
    }

    MaterialTheme {
        Surface {
            when (resolvedScreen) {
                AppScreen.Home -> HomeScreen(
                    homeUiModel = homeUiModel,
                    homeInteractionUiState = homeInteractionUiState,
                    avatarBridgeState = homePixelPetBridgeState,
                    behaviorTalkDirective = if (isBehaviorExperienceAuthoritative) {
                        behaviorTalkDirective
                    } else {
                        null
                    },
                    isBehaviorExperienceAuthoritative = isBehaviorExperienceAuthoritative,
                    appOpenGreeting = if (isBehaviorExperienceAuthoritative) {
                        null
                    } else {
                        startupGreetingForVisual
                    },
                    latestAudioStimulus = latestAudioStimulus,
                    latestAudioOutputStartedAtMs = latestAudioOutputStartedAtMs,
                    invitationTriggerToken = invitationUiTriggerToken,
                    petState = currentPetState,
                    brainState = brainStateSnapshot.currentState,
                    onPetTap = {
                        coroutineScope.launch {
                            handlePetInteraction(
                                interactionType = PetInteractionType.TAP,
                                source = "home_pet_tap"
                            )
                        }
                    },
                    onPetLongPress = {
                        coroutineScope.launch {
                            handlePetInteraction(
                                interactionType = PetInteractionType.LONG_PRESS,
                                source = "home_pet_avatar_long_press"
                            )
                        }
                    },
                    onFeedPet = {
                        coroutineScope.launch {
                            handlePetActivity(feedPetUseCase)
                        }
                    },
                    onPlayWithPet = {
                        coroutineScope.launch {
                            handlePetActivity(playWithPetUseCase)
                        }
                    },
                    onLetPetRest = {
                        coroutineScope.launch {
                            handlePetActivity(letPetRestUseCase)
                        }
                    },
                    onMiniGameCelebrate = {
                        queueTransientReaction(
                            intent = com.aipet.brain.ui.avatar.pixel.bridge.PixelPetAvatarIntent.GAME_CELEBRATE
                        )
                    },
                    onMiniGameFail = {
                        queueTransientReaction(
                            intent = com.aipet.brain.ui.avatar.pixel.bridge.PixelPetAvatarIntent.GAME_FAIL
                        )
                    },
                    onInvitationIgnored = {
                        coroutineScope.launch {
                            markPendingInvitationIgnored()
                        }
                    },
                    onNavigateToDebug = { currentScreenName = AppScreen.Debug.name },
                    onNavigateToDiary = { currentScreenName = AppScreen.Diary.name }
                )

                AppScreen.Onboarding -> PetNamingOnboardingScreen(
                    currentName = activePetProfile?.name ?: PetProfileRepository.DEFAULT_PET_NAME,
                    draftName = petNameDraft,
                    onDraftNameChanged = { updatedName ->
                        petNameDraft = updatedName.take(MAX_PET_NAME_LENGTH)
                    },
                    onConfirmName = {
                        coroutineScope.launch {
                            completePetNamingOnboarding(
                                requestedName = petNameDraft,
                                shouldKeepCurrentName = false
                            )
                        }
                    },
                    onKeepCurrentName = {
                        coroutineScope.launch {
                            completePetNamingOnboarding(
                                requestedName = activePetProfile?.name,
                                shouldKeepCurrentName = true
                            )
                        }
                    }
                )

                AppScreen.Debug -> DebugScreen(
                    latestEvent = latestEvent,
                    latestOwnerSeenEvent = latestOwnerSeenEvent,
                    latestOwnerGreetingEvent = latestOwnerGreetingEvent,
                    latestAudioStimulusSummary = buildAudioStimulusDebugSummary(latestAudioStimulus),
                    audioRuntimeDebugState = audioRuntimeDebugState,
                    audioPlaybackDebugState = audioPlaybackDebugState,
                    currentBrainState = brainStateSnapshot.currentState.name,
                    currentWorkingMemory = currentWorkingMemory,
                    currentPetState = currentPetState,
                    currentPetTraits = currentPetTraits,
                    currentPetPersonalitySummary = petPersonalitySummaryResolver.resolve(currentPetTraits),
                    currentPetConditions = currentPetConditions,
                    latestBehaviorDecisionSource = latestBehaviorDecisionSource,
                    latestBehaviorDecision = latestBehaviorDecision,
                    onNavigateToHome = { currentScreenName = AppScreen.Home.name },
                    onNavigateToSettings = { currentScreenName = AppScreen.Settings.name },
                    onNavigateToEventViewer = { currentScreenName = AppScreen.EventViewer.name },
                    onNavigateToDiary = { currentScreenName = AppScreen.Diary.name },
                    onNavigateToObservationViewer = { currentScreenName = AppScreen.ObservationViewer.name },
                    onNavigateToProfileAssociations = { currentScreenName = AppScreen.ProfileAssociations.name },
                    onNavigateToPersons = { currentScreenName = AppScreen.Persons.name },
                    onNavigateToTraits = { currentScreenName = AppScreen.Traits.name },
                    onNavigateToWorkingMemoryDebug = { currentScreenName = AppScreen.WorkingMemoryDebug.name },
                    onNavigateToCamera = { currentScreenName = AppScreen.Camera.name },
                    onNavigateToAudioDebug = { currentScreenName = AppScreen.AudioDebug.name },
                    onNavigateToFaceAutoEnroll = { currentScreenName = AppScreen.FaceAutoEnroll.name },
                    showAvatarDebugAction = avatarDebugEnabled,
                    onNavigateToAvatarDebug = {
                        if (avatarDebugEnabled) {
                            currentScreenName = AppScreen.AvatarDebug.name
                        }
                    },
                    onForceSleep = {
                        coroutineScope.launch {
                            brainInteractionLoop.forceSleep()
                        }
                    },
                    onForceWake = {
                        coroutineScope.launch {
                            brainInteractionLoop.forceWake()
                        }
                    },
                    onEmitAudioResponseRequestFromStimulus = {
                        coroutineScope.launch {
                            audioResponseRequestEmitter.emitFromStimulus(
                                AudioResponseRequestInput(
                                    stimulus = latestAudioStimulus,
                                    category = DEBUG_AUDIO_REQUEST_CATEGORY,
                                    cooldownKey = DEBUG_AUDIO_REQUEST_COOLDOWN_KEY
                                )
                            )
                        }
                    },
                    onEmitTestEvent = {
                        coroutineScope.launch {
                            eventBus.publish(
                                EventEnvelope.create(
                                    type = EventType.TEST_EVENT,
                                    payloadJson = "{\"source\":\"debug_screen\"}"
                                )
                            )
                        }
                    },
                    onCreateObject = { objectName ->
                        val normalizedName = objectName.trim()
                        if (normalizedName.isBlank()) {
                            Result.failure(IllegalArgumentException("Object name is required."))
                        } else {
                            val createdObject = objectRepository.createObject(normalizedName)
                            if (createdObject == null) {
                                Result.failure(IllegalStateException("Unable to create object."))
                            } else {
                                Log.i(
                                    DEBUG_OBJECT_CREATE_TAG,
                                    "Manual object created: objectId=${createdObject.objectId}, " +
                                        "name='${createdObject.name}', createdAtMs=${createdObject.createdAtMs}"
                                )
                                Result.success(
                                    "Object created: ${createdObject.name} (${createdObject.objectId})"
                                )
                            }
                        }
                    },
                    recognitionProbeSummary = recognitionProbeSummary,
                    onRunRecognitionProbe = {
                        coroutineScope.launch {
                            val probePerson = knownPersonEmbeddingsSource.loadKnownPersonEmbeddings()
                                .firstOrNull { person ->
                                    person.embeddings.isNotEmpty()
                                }
                            if (probePerson == null) {
                                val emptyRecognitionResult = personRecognitionService.recognize(
                                    currentEmbedding = floatArrayOf(1f)
                                )
                                recognitionDecisionEventPublisher.publish(
                                    recognitionResult = emptyRecognitionResult
                                )
                                recognitionProbeSummary = buildString {
                                    append("classification=${emptyRecognitionResult.classification.name}")
                                    append(",accepted=${emptyRecognitionResult.accepted}")
                                    append(",bestScore=${emptyRecognitionResult.bestScore}")
                                    append(",threshold=${emptyRecognitionResult.threshold}")
                                    append(",bestPersonId=${emptyRecognitionResult.bestPersonId ?: "none"}")
                                    append(",evaluatedCandidates=${emptyRecognitionResult.evaluatedCandidates}")
                                }
                                Log.i(
                                    DEBUG_RECOGNITION_TAG,
                                    "Recognition probe result: $recognitionProbeSummary"
                                )
                                return@launch
                            }

                            val recognitionResult = personRecognitionService.recognize(
                                currentEmbedding = probePerson.embeddings.first().values
                            )
                            recognitionDecisionEventPublisher.publish(
                                recognitionResult = recognitionResult
                            )
                            recognitionProbeSummary = buildString {
                                append(",classification=${recognitionResult.classification.name}")
                                append(",accepted=${recognitionResult.accepted}")
                                append(",queryPersonId=${probePerson.personId}")
                                append(",bestPersonId=${recognitionResult.bestPersonId ?: "none"}")
                                append(",bestScore=${recognitionResult.bestScore}")
                                append(",threshold=${recognitionResult.threshold}")
                                append(",evaluatedCandidates=${recognitionResult.evaluatedCandidates}")
                            }
                            Log.i(
                                DEBUG_RECOGNITION_TAG,
                                "Recognition probe result: $recognitionProbeSummary"
                            )
                        }
                    },
                    onNavigateToBehaviorIntelligence = {
                        currentScreenName = AppScreen.BehaviorIntelligenceDebug.name
                    },
                    onNavigateToEmotionalSystems = {
                        currentScreenName = AppScreen.EmotionalSystemsDebug.name
                    },
                    onNavigateToEvolutionDebug = {
                        currentScreenName = AppScreen.EvolutionSystemDebug.name
                    }
                )

                AppScreen.EvolutionSystemDebug -> {
                    val evCtx = evolutionContext
                    val recentEpisodes = evCtx?.recentEpisodes ?: emptyList()
                    val bond = evCtx?.bond
                    val habit = evCtx?.habitProfile
                    var latestFacts by remember { mutableStateOf<List<com.aipet.brain.brain.evolution.domain.SemanticMemoryFact>>(emptyList()) }
                    LaunchedEffect(Unit) {
                        latestFacts = withContext(Dispatchers.IO) {
                            semanticMemoryRepository.getAll()
                        }
                    }
                    EvolutionSystemDebugScreen(
                        evolutionContext = evCtx,
                        recentEpisodes = recentEpisodes,
                        semanticFacts = latestFacts,
                        bond = bond,
                        habitProfile = habit,
                        onNavigateBack = { currentScreenName = AppScreen.Debug.name }
                    )
                }

                AppScreen.AvatarDebug -> AvatarAnimationDebugScreen(
                    runtimeBridgeState = homePixelPetDebugBridgeState,
                    runtimeOrchestratorDiagnostics = runtimeOrchestratorDiagnostics,
                    onNavigateBack = { currentScreenName = AppScreen.Debug.name }
                )

                AppScreen.AudioDebug -> AudioDebugScreen(
                    hasRequestedPermission = hasRequestedMicrophonePermission,
                    onPermissionRequestTracked = { hasRequestedMicrophonePermission = true },
                    onNavigateBack = { currentScreenName = AppScreen.Debug.name },
                    audioEventBus = eventBus,
                    audioPlaybackEngine = audioPlaybackEngine,
                    audioCaptureLifecycleListener = audioCaptureLifecycleEventPublisher,
                    audioEnergyMetricsListener = audioCaptureLifecycleEventPublisher,
                    audioVadResultListener = audioCaptureLifecycleEventPublisher,
                    audioKeywordDetectionListener = audioCaptureLifecycleEventPublisher,
                    audioRuntimeDebugStateProvider = audioCaptureLifecycleEventPublisher,
                    keywordSpottingConfigStore = keywordSpottingConfigStore
                )

                AppScreen.WorkingMemoryDebug -> WorkingMemoryDebugScreen(
                    currentWorkingMemory = currentWorkingMemory,
                    onNavigateBack = { currentScreenName = AppScreen.Debug.name }
                )

                AppScreen.Traits -> TraitsScreen(
                    currentTraits = currentTraits,
                    onNavigateBack = { currentScreenName = AppScreen.Debug.name }
                )

                AppScreen.Settings -> SettingsScreen(
                    selectedCamera = selectedCamera,
                    soundEnabled = isPetSoundEnabled,
                    isResetInProgress = isResettingPetData,
                    onSelectCamera = { selection ->
                        coroutineScope.launch {
                            cameraSelectionStore.setSelectedCamera(selection)
                        }
                    },
                    onSoundEnabledChange = { enabled ->
                        coroutineScope.launch {
                            petSoundSettingsStore.setSoundEnabled(enabled)
                        }
                    },
                    onResetPetData = {
                        coroutineScope.launch {
                            resetPetDataToFreshState()
                        }
                    },
                    onNavigateBack = { currentScreenName = AppScreen.Debug.name }
                )

                AppScreen.EventViewer -> EventViewerScreen(
                    eventStore = eventStore,
                    onNavigateBack = { currentScreenName = AppScreen.Debug.name }
                )

                AppScreen.Diary -> DiaryScreen(
                    memoryCards = diaryMemoryCards,
                    dailySummaries = diaryDailySummaries,
                    onNavigateHome = { currentScreenName = AppScreen.Home.name },
                    onNavigateDiary = { currentScreenName = AppScreen.Diary.name },
                    onNavigateDebug = { currentScreenName = AppScreen.Debug.name }
                )

                AppScreen.ObservationViewer -> ObservationViewerScreen(
                    eventStore = eventStore,
                    onNavigateBack = { currentScreenName = AppScreen.Debug.name },
                    onRecordDebugObservation = {
                        try {
                            observationRecorder.recordPersonLikeObservation(
                                source = ObservationSource.DEBUG,
                                note = "manual_debug_observation"
                            )
                            Result.success(Unit)
                        } catch (error: Throwable) {
                            Result.failure(error)
                        }
                    }
                )

                AppScreen.ProfileAssociations -> ProfileAssociationsScreen(
                    faceProfileStore = faceProfileStore,
                    personStore = personStore,
                    personSeenEventPublisher = personSeenEventPublisher,
                    eventStore = eventStore,
                    onNavigateBack = { currentScreenName = AppScreen.Debug.name }
                )

                AppScreen.Camera -> CameraScreen(
                    hasRequestedPermission = hasRequestedCameraPermission,
                    selectedCamera = selectedCamera,
                    onPermissionRequestTracked = { hasRequestedCameraPermission = true },
                    onFrameDiagnostics = { diagnostics ->
                        coroutineScope.launch {
                            eventBus.publish(
                                EventEnvelope.create(
                                    type = EventType.CAMERA_FRAME_RECEIVED,
                                    payloadJson = diagnostics.toCameraFramePayloadJson()
                                )
                            )
                        }
                    },
                    onFaceDetectionResult = { detectionResult ->
                        val currentFaceCount = detectionResult.faces.size
                        if (currentFaceCount <= 0) {
                            recognizedPersonLabel = null
                        } else {
                            perceptionLookingUntilMs = maxOf(
                                perceptionLookingUntilMs,
                                detectionResult.timestampMs + PERCEPTION_LOOKING_HOLD_MS
                            )
                        }
                        coroutineScope.launch {
                            perceptionFusionCoordinator.onCameraFrame(
                                faceCount = currentFaceCount,
                                recognizedPersonId = null,
                                faceConfidence = if (currentFaceCount > 0) 0.7f else 0f
                            )
                        }
                        if (currentFaceCount == lastPublishedFaceCount) {
                            return@CameraScreen
                        }
                        lastPublishedFaceCount = currentFaceCount
                        coroutineScope.launch {
                            eventBus.publish(
                                EventEnvelope.create(
                                    type = EventType.FACE_DETECTED,
                                    timestampMs = detectionResult.timestampMs,
                                    payloadJson = detectionResult.toFacesDetectedPayloadJson()
                                )
                            )
                        }
                    },
                    onObjectDetected = { label, confidence, detectedAtMs ->
                        perceptionLookingUntilMs = maxOf(perceptionLookingUntilMs, detectedAtMs + PERCEPTION_LOOKING_HOLD_MS)
                        coroutineScope.launch {
                            val seenUpdateResult = runCatching {
                                objectRepository.recordKnownObjectSeen(
                                    label = label,
                                    seenAtMs = detectedAtMs
                                )
                            }.onFailure { error ->
                                Log.w(
                                    DEBUG_OBJECT_STATS_TAG,
                                    "Known-object stats update failed for label='$label'.",
                                    error
                                )
                            }.getOrNull()
                            when {
                                seenUpdateResult?.wasUpdated == true -> {
                                    val updatedObject = seenUpdateResult.objectRecord
                                    Log.i(
                                        DEBUG_OBJECT_STATS_TAG,
                                        "Known-object stats updated: objectId=${updatedObject.objectId}, " +
                                            "name='${updatedObject.name}', seenCount=${updatedObject.seenCount}, " +
                                            "lastSeenAtMs=${updatedObject.lastSeenAtMs}"
                                    )
                                }
                                seenUpdateResult != null -> {
                                    val matchedObject = seenUpdateResult.objectRecord
                                    Log.d(
                                        DEBUG_OBJECT_STATS_TAG,
                                        "Known-object stats throttled: objectId=${matchedObject.objectId}, " +
                                            "name='${matchedObject.name}', seenCount=${matchedObject.seenCount}, " +
                                            "lastSeenAtMs=${matchedObject.lastSeenAtMs}"
                                    )
                                }
                                else -> Unit
                            }
                            eventBus.publish(
                                EventEnvelope.create(
                                    type = EventType.OBJECT_DETECTED,
                                    timestampMs = detectedAtMs,
                                    payloadJson = ObjectDetectedEventPayload(
                                        objectId = seenUpdateResult?.objectRecord?.objectId,
                                        label = label,
                                        confidence = confidence,
                                        detectedAtMs = detectedAtMs
                                    ).toJson()
                                )
                            )
                            Log.i(
                                DEBUG_OBJECT_EVENT_TAG,
                                "Published OBJECT_DETECTED: objectId=${seenUpdateResult?.objectRecord?.objectId ?: "unknown"}, " +
                                    "label='$label', confidence=$confidence, detectedAtMs=$detectedAtMs"
                            )
                        }
                    },
                    onResolveKnownObjectLabel = { canonicalLabel ->
                        val resolvedDisplayName = objectRepository.resolveKnownObjectDisplayName(
                            detectedCanonicalLabel = canonicalLabel
                        )
                        if (resolvedDisplayName != null) {
                            Log.d(
                                DEBUG_OBJECT_ALIAS_TAG,
                                "Resolved camera object label: canonicalLabel='$canonicalLabel', " +
                                    "displayLabel='$resolvedDisplayName'"
                            )
                        } else {
                            Log.d(
                                DEBUG_OBJECT_ALIAS_TAG,
                                "No known alias for camera label: canonicalLabel='$canonicalLabel'"
                            )
                        }
                        resolvedDisplayName
                    },
                    onInspectUnknownObjectPromptSuppression = { canonicalLabel ->
                        backgroundOrchestrator.inspectUnknownObjectPromptSuppression(canonicalLabel)
                    },
                    onRecordPersonLikeObservation = { note ->
                        try {
                            observationRecorder.recordPersonLikeObservation(
                                source = ObservationSource.CAMERA,
                                note = note
                            )
                            Result.success(Unit)
                        } catch (error: Throwable) {
                            Result.failure(error)
                        }
                    },
                    recognizedPersonLabel = recognizedPersonLabel,
                    perceptionLoopState = when {
                        isPerceptionAsking -> "asking"
                        isPerceptionLooking -> "looking"
                        else -> "idle"
                    },
                    avatarPerceptionState = homePixelPetDebugBridgeState.intent.name.lowercase(),
                    onLiveFaceCropReady = { rawBitmap, _, cameraRotation ->
                        // If the phone is in portrait orientation, the face crop from FaceCropper
                        // is upright but the stored embeddings were generated from landscape-oriented
                        // (raw sensor) frames. Rotate 90° to match that stored orientation.
                        val bitmap = rotateBitmapForPortrait(rawBitmap, cameraRotation)
                        coroutineScope.launch(Dispatchers.Default) {
                            try {
                                faceEmbeddingEngine.generateEmbedding(bitmap)
                                    .onSuccess { embedding ->
                                        val result = personRecognitionService.recognize(embedding)
                                        recognitionDecisionEventPublisher.publish(result)
                                        val personId = if (result.accepted) result.bestPersonId else null

                                        // Auto-learn: silently reinforce the face profile every time
                                        // recognition is confident enough. This expands the profile
                                        // over time without requiring manual re-teaching.
                                        if (personId != null && result.bestScore >= AUTO_LEARN_MIN_SCORE) {
                                            withContext(Dispatchers.IO) {
                                                runCatching {
                                                    autoLearnEmbeddingIfNeeded(
                                                        personId = personId,
                                                        embedding = embedding,
                                                        faceProfileStore = faceProfileStore
                                                    )
                                                }.onFailure { error ->
                                                    Log.w(
                                                        CAMERA_RECOGNITION_TAG,
                                                        "Auto-learn failed for personId=$personId: ${error.message}"
                                                    )
                                                }
                                            }
                                        }

                                        val label: String? = when {
                                            personId != null -> {
                                                val name = withContext(Dispatchers.IO) {
                                                    personStore.getById(personId)?.displayName ?: personId
                                                }
                                                "$name (${String.format(java.util.Locale.US, "%.2f", result.bestScore)})"
                                            }
                                            result.bestScore >= RECOGNITION_NEAR_MISS_THRESHOLD -> {
                                                // Below acceptance threshold but possibly the right person.
                                                // Prefix "?" signals the overlay to use amber colour.
                                                "? (${String.format(java.util.Locale.US, "%.2f", result.bestScore)})"
                                            }
                                            else -> null
                                        }
                                        withContext(Dispatchers.Main) {
                                            recognizedPersonLabel = label
                                        }
                                    }
                                    .onFailure { error ->
                                        Log.w(
                                            CAMERA_RECOGNITION_TAG,
                                            "Live face embedding failed: ${error.message}",
                                            error
                                        )
                                    }
                            } finally {
                                bitmap.recycle()
                            }
                        }
                    },
                    onNavigateBack = { currentScreenName = AppScreen.Home.name }
                )

                AppScreen.Persons -> PersonsScreen(
                    personStore = personStore,
                    personSeenEventPublisher = personSeenEventPublisher,
                    faceProfileStore = faceProfileStore,
                    eventBus = eventBus,
                    onNavigateBack = { currentScreenName = AppScreen.Debug.name },
                    onNavigateToTeachPerson = {
                        currentScreenName = AppScreen.TeachPerson.name
                    },
                    onNavigateToCreatePerson = {
                        editingPersonId = null
                        currentScreenName = AppScreen.PersonEditor.name
                    },
                    onNavigateToEditPerson = { personId ->
                        editingPersonId = personId
                        currentScreenName = AppScreen.PersonEditor.name
                    },
                    onNavigateToPersonDetail = { personId ->
                        selectedPersonId = personId
                        currentScreenName = AppScreen.PersonDetail.name
                    }
                )

                AppScreen.TeachPerson -> TeachPersonScreen(
                    teachSessionId = teachSessionId,
                    teachSampleStore = teachSampleStore,
                    teachSampleImageStorage = teachSampleImageStorage,
                    personStore = personStore,
                    teachPersonSaveController = teachPersonSaveController,
                    onCaptureSample = { note, imageUri ->
                        runCatching {
                            val observation = observationRecorder.recordPersonLikeObservation(
                                source = ObservationSource.CAMERA,
                                note = note
                            )
                            val faceCropResult = teachSampleFaceCropExtractor.extractFaceCrop(
                                sourceImageUri = imageUri,
                                sampleCaptureId = observation.observationId
                            )
                            val faceCropUri = when (faceCropResult) {
                                is FaceCropExtractionResult.Success -> faceCropResult.faceCropUri
                                FaceCropExtractionResult.NoFaceDetected -> null
                                is FaceCropExtractionResult.Failed -> null
                            }
                            val noteWithCropStatus = appendTeachSampleCropStatusToNote(
                                baseNote = observation.note,
                                faceCropResult = faceCropResult
                            )
                            TeachPersonCapturedObservation(
                                observationId = observation.observationId,
                                observedAtMs = observation.observedAtMs,
                                source = observation.source.name,
                                note = noteWithCropStatus,
                                imageUri = imageUri,
                                faceCropUri = faceCropUri
                            )
                        }
                    },
                    initialCompletionConfirmedAtMs = persistedTeachSessionCompletion
                        ?.takeIf { completion -> completion.isCompletedConfirmed }
                        ?.confirmedAtMs,
                    onTeachSessionCompletionConfirmed = { completedAtMs ->
                        teachSessionCompletionStore.confirmCompletion(
                            teachSessionId = teachSessionId,
                            confirmedAtMs = completedAtMs
                        )
                    },
                    onTeachSessionCompletionCleared = {
                        teachSessionCompletionStore.clearCompletion(
                            teachSessionId = teachSessionId
                        )
                    },
                    onNavigateBack = { currentScreenName = AppScreen.Persons.name },
                    onPersonSaved = { personId ->
                        teachSessionId = UUID.randomUUID().toString()
                        selectedPersonId = personId
                        currentScreenName = AppScreen.PersonDetail.name
                    }
                )

                AppScreen.PersonEditor -> PersonEditorScreen(
                    personStore = personStore,
                    personId = editingPersonId,
                    onNavigateBack = { currentScreenName = AppScreen.Persons.name },
                    onPersonSaved = { personId ->
                        selectedPersonId = personId
                        currentScreenName = AppScreen.PersonDetail.name
                    }
                )

                AppScreen.PersonDetail -> PersonDetailScreen(
                    personId = selectedPersonId,
                    personStore = personStore,
                    faceProfileStore = faceProfileStore,
                    onNavigateBack = { currentScreenName = AppScreen.Persons.name }
                )
                AppScreen.FaceAutoEnroll -> FaceAutoEnrollDebugScreen(
                    knownPersonEmbeddingsSource = knownPersonEmbeddingsSource,
                    personRecognitionService = personRecognitionService,
                    faceEmbeddingEngine = faceEmbeddingEngine,
                    faceProfileStore = faceProfileStore,
                    onNavigateBack = { currentScreenName = AppScreen.Debug.name },
                    hasRequestedCameraPermission = hasRequestedCameraPermission,
                    onPermissionRequestTracked = { hasRequestedCameraPermission = true }
                )

                AppScreen.BehaviorIntelligenceDebug -> BehaviorIntelligenceDebugScreen(
                    behaviorDebugState = behaviorDebugState,
                    behaviorExperienceDebugState = behaviorExperienceDebugState,
                    attentionDebugState = attentionDebugState,
                    fusionSnapshot = fusionSnapshot,
                    onNavigateToHome = { currentScreenName = AppScreen.Home.name },
                    onNavigateToDiary = { currentScreenName = AppScreen.Diary.name },
                    onNavigateToDebug = { currentScreenName = AppScreen.Debug.name }
                )

                AppScreen.EmotionalSystemsDebug -> com.aipet.brain.app.ui.debug.EmotionalSystemsDebugScreen(
                    petState = currentPetState,
                    petTraits = currentPetTraits,
                    petConditions = currentPetConditions,
                    currentAbsenceBucket = currentAbsenceBucket,
                    currentGreetingStyle = currentGreetingStyle,
                    currentRelationshipStage = currentRelationshipStage,
                    sessionState = sessionInteractionTracker.state,
                    lastBehaviorScoringResult = lastBehaviorScoringResult,
                    onNavigateBack = { currentScreenName = AppScreen.Debug.name },
                    onInjectTiredPet = {
                        coroutineScope.launch {
                            val state = currentPetState ?: return@launch
                            val injected = state.copy(
                                sleepiness = 85,
                                energy = 15,
                                lastUpdatedAt = System.currentTimeMillis()
                            ).withClampedValues()
                            withContext(Dispatchers.IO) { petStateRepository.updateState(injected) }
                            currentPetState = injected
                            currentPetConditions = petConditionResolver.resolve(injected)
                        }
                    },
                    onInjectHungryPet = {
                        coroutineScope.launch {
                            val state = currentPetState ?: return@launch
                            val injected = state.copy(
                                hunger = 90,
                                lastUpdatedAt = System.currentTimeMillis()
                            ).withClampedValues()
                            withContext(Dispatchers.IO) { petStateRepository.updateState(injected) }
                            currentPetState = injected
                            currentPetConditions = petConditionResolver.resolve(injected)
                        }
                    },
                    onInjectNeglectedPet = {
                        coroutineScope.launch {
                            val state = currentPetState ?: return@launch
                            val injected = state.copy(
                                neglectStreak = 4,
                                trustScore = 15,
                                careStreak = 0,
                                moodValence = -30,
                                lastUpdatedAt = System.currentTimeMillis()
                            ).withClampedValues()
                            withContext(Dispatchers.IO) { petStateRepository.updateState(injected) }
                            currentPetState = injected
                            currentPetConditions = petConditionResolver.resolve(injected)
                            currentRelationshipStage = relationshipStageResolver.resolve(injected)
                        }
                    },
                    onInjectBondedPet = {
                        coroutineScope.launch {
                            val state = currentPetState ?: return@launch
                            val injected = state.copy(
                                bond = 70,
                                trustScore = 65,
                                attachmentScore = 55,
                                careStreak = 5,
                                neglectStreak = 0,
                                moodValence = 40,
                                lastUpdatedAt = System.currentTimeMillis()
                            ).withClampedValues()
                            withContext(Dispatchers.IO) { petStateRepository.updateState(injected) }
                            currentPetState = injected
                            currentPetConditions = petConditionResolver.resolve(injected)
                            currentRelationshipStage = relationshipStageResolver.resolve(injected)
                        }
                    },
                    onInjectOverstimulatedPet = {
                        coroutineScope.launch {
                            val state = currentPetState ?: return@launch
                            val injected = state.copy(
                                stimulation = 90,
                                comfort = 35,
                                lastUpdatedAt = System.currentTimeMillis()
                            ).withClampedValues()
                            withContext(Dispatchers.IO) { petStateRepository.updateState(injected) }
                            currentPetState = injected
                            currentPetConditions = petConditionResolver.resolve(injected)
                        }
                    }
                )
            }

            // ── Unknown entity teach dialog — floats over all screens ──────────
            activeTeachTarget?.let { target ->
                TeachUnknownDialog(
                    target = target,
                    onSave = { name ->
                        coroutineScope.launch {
                            when (target) {
                                is TeachUnknownTarget.UnknownFace -> {
                                    withContext(Dispatchers.IO) {
                                        val now = System.currentTimeMillis()
                                        val trimmedName = name.trim().take(MAX_PET_NAME_LENGTH)
                                        val personId = UUID.randomUUID().toString()
                                        personStore.insert(
                                            PersonRecord(
                                                personId = personId,
                                                displayName = trimmedName,
                                                nickname = null,
                                                isOwner = false,
                                                createdAtMs = now,
                                                updatedAtMs = now,
                                                lastSeenAtMs = null,
                                                seenCount = 0,
                                                familiarityScore = 0f
                                            )
                                        )

                                        val profile = faceProfileStore.createProfileCandidate(
                                            label = trimmedName,
                                            note = "unknown_candidate:${target.candidateId}"
                                        )
                                        faceProfileStore.linkProfileToPerson(
                                            profileId = profile.profileId,
                                            personId = personId
                                        )
                                        val seedEmbedding = target.centroidEmbedding.toFiniteEmbedding()
                                        if (seedEmbedding.isNotEmpty()) {
                                            faceProfileStore.addEmbeddingToProfile(
                                                profileId = profile.profileId,
                                                values = seedEmbedding,
                                                metadata = "unknown_candidate_seed"
                                            )
                                        }
                                        eventBus.publish(
                                            EventEnvelope.create(
                                                type = EventType.USER_TAUGHT_PERSON,
                                                timestampMs = now,
                                                payloadJson = UserTaughtPersonEventPayload(
                                                    personId = personId,
                                                    displayName = trimmedName,
                                                    sampleCount = 1
                                                ).toJson()
                                            )
                                        )
                                        backgroundOrchestrator
                                            .resolveUnknownFaceCandidateAndStartAutoCapture(
                                                candidateId = target.candidateId,
                                                personId = personId,
                                                profileId = profile.profileId,
                                                seedEmbedding = seedEmbedding
                                            )
                                    }
                                }
                                is TeachUnknownTarget.UnknownObject -> {
                                    withContext(Dispatchers.IO) {
                                        objectRepository.createObject(name.trim())
                                    }
                                }
                            }
                            activeTeachTarget = null
                        }
                    },
                    onDismiss = {
                        if (target is TeachUnknownTarget.UnknownFace) {
                            backgroundOrchestrator.suppressUnknownFaceCandidate(
                                candidateId = target.candidateId,
                                reason = "user_skipped_dialog"
                            )
                        }
                        activeTeachTarget = null
                    }
                )
            }
        }
    }
}

private fun String.toAppScreen(): AppScreen {
    return AppScreen.entries.firstOrNull { it.name == this } ?: AppScreen.Home
}

private fun String.toBitmapFromBase64(): Bitmap? {
    return runCatching {
        val bytes = Base64.decode(this, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()
}

private fun List<Float>.toFiniteEmbedding(): List<Float> {
    if (isEmpty()) {
        return emptyList()
    }
    if (!all { value -> value.isFinite() }) {
        return emptyList()
    }
    return toList()
}

private fun FrameDiagnostics.toCameraFramePayloadJson(): String {
    return CameraFrameReceivedPayload(
        width = width,
        height = height,
        analyzedAtMs = timestampMs,
        rotationDegrees = rotationDegrees,
        processingDurationMs = processingDurationMs
    ).toJson()
}

private fun FaceDetectionResult.toFacesDetectedPayloadJson(): String {
    return FacesDetectedEventPayload(
        frameId = frameId,
        timestampMs = timestampMs,
        faceCount = faces.size,
        boundingBoxes = faces.map { face ->
            face.toBoundingBoxPayload()
        }
    ).toJson()
}

private fun DetectedFace.toBoundingBoxPayload(): FaceBoundingBoxPayload {
    return FaceBoundingBoxPayload(
        left = boundingBox.left,
        top = boundingBox.top,
        right = boundingBox.right,
        bottom = boundingBox.bottom
    )
}

private fun appendTeachSampleCropStatusToNote(
    baseNote: String?,
    faceCropResult: FaceCropExtractionResult
): String {
    val notePrefix = baseNote?.trim().orEmpty()
    val cropStatus = when (faceCropResult) {
        is FaceCropExtractionResult.Success -> "face_crop=available"
        FaceCropExtractionResult.NoFaceDetected -> "face_crop=not_detected"
        is FaceCropExtractionResult.Failed -> "face_crop=failed"
    }
    return if (notePrefix.isBlank()) {
        cropStatus
    } else {
        "$notePrefix;$cropStatus"
    }
}

private fun buildAudioStimulusDebugSummary(
    stimulus: com.aipet.brain.brain.logic.audio.AudioStimulus?
): String {
    val currentStimulus = stimulus ?: return "-"
    return "source=${currentStimulus.sourceEventType.name}, ts=${currentStimulus.timestampMs}"
}

private const val DEBUG_AUDIO_REQUEST_CATEGORY = "ACKNOWLEDGMENT"
private const val DEBUG_AUDIO_REQUEST_COOLDOWN_KEY = "debug_audio_stimulus_request"
private const val DEBUG_RECOGNITION_TAG = "RecognitionProbe"
private const val CAMERA_RECOGNITION_TAG = "LiveRecognition"
private const val DEBUG_OBJECT_EVENT_TAG = "ObjectEventPublisher"
private const val DEBUG_OBJECT_CREATE_TAG = "ObjectCreateDebug"
private const val DEBUG_OBJECT_STATS_TAG = "ObjectSeenStats"
private const val DEBUG_OBJECT_ALIAS_TAG = "ObjectAliasResolver"
private const val DEBUG_STARTUP_TAG = "PetBrainStartup"
private const val VOICE_FEEDBACK_TAG = "VoiceCmdFeedback"
private const val VOICE_AVATAR_TAG = "VoiceCmdAvatar"
private const val AUDIO_RUNTIME_TAG = "HomeAudioRuntime"
private const val MAX_PET_NAME_LENGTH = 24
private const val APP_OPEN_GREETING_WINDOW_MS = 3_000L
private const val APP_OPEN_GREETING_DEDUPE_KEY = "app_open_greeting"
private const val RESUME_EVOLUTION_MIN_ABSENCE_MS = 1_000L
private const val RUNTIME_AUDIO_GLOBAL_MIN_INTERVAL_MS = 1_200L
private const val RUNTIME_AUDIO_SAME_CATEGORY_MIN_INTERVAL_MS = 2_200L

// Auto-learning: minimum recognition score (cosine similarity) before a live frame
// embedding is added to the person's profile automatically.
private const val AUTO_LEARN_MIN_SCORE = 0.82f
private const val PERCEPTION_LOOKING_HOLD_MS = 2_500L
private const val PERCEPTION_ASKING_HOLD_MS = 4_500L
// Time the user has to interact after an invitation before it is counted as ignored
private const val INVITATION_RESPONSE_WINDOW_MS = 30_000L

// Cap total auto-learned embeddings per-person to avoid unbounded DB growth.
private const val AUTO_LEARN_MAX_EMBEDDINGS_PER_PERSON = 20

// Score at or above this threshold (but below acceptance) is shown as a near-miss
// in the diagnostics overlay so the threshold can be tuned visually.
private const val RECOGNITION_NEAR_MISS_THRESHOLD = 0.65f

private suspend fun autoLearnEmbeddingIfNeeded(
    personId: String,
    embedding: FloatArray,
    faceProfileStore: com.aipet.brain.memory.profiles.FaceProfileStore
) {
    val profiles = faceProfileStore.listProfilesForPerson(personId)
    if (profiles.isEmpty()) return
    val primaryProfile = profiles.first()
    val existingEmbeddings = faceProfileStore.listProfileEmbeddings(primaryProfile.profileId)
    if (existingEmbeddings.size >= AUTO_LEARN_MAX_EMBEDDINGS_PER_PERSON) return
    faceProfileStore.addEmbeddingToProfile(
        profileId = primaryProfile.profileId,
        values = embedding.toList(),
        metadata = "auto_learn_live_recognition"
    )
    android.util.Log.d(
        CAMERA_RECOGNITION_TAG,
        "Auto-learned face embedding for personId=$personId, total=${existingEmbeddings.size + 1}"
    )
}

/**
 * When the phone is portrait (cameraRotation 90° or 270°), FaceCropper produces an upright face
 * crop that doesn't match embeddings trained/stored from landscape-oriented (raw sensor) captures.
 * Rotate 90° CW to bring the crop back to the landscape-equivalent orientation.
 *
 * The original bitmap is recycled; callers must use the returned value.
 */
private fun rotateBitmapForPortrait(bitmap: Bitmap, cameraRotation: Int): Bitmap {
    if (cameraRotation != 90 && cameraRotation != 270) return bitmap
    val matrix = Matrix().apply { postRotate(90f) }
    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    bitmap.recycle()
    return rotated
}

private data class RuntimeRehydrateSnapshot(
    val profile: PetProfile,
    val state: PetState,
    val emotion: PetEmotion,
    val traits: PetTrait,
    val conditions: Set<PetCondition>,
    val absenceBucket: com.aipet.brain.brain.pet.AbsenceBucket,
    val relationshipStage: com.aipet.brain.brain.pet.RelationshipStage,
    val dayBoundaryType: PetDayBoundaryType,
    val summaryDate: java.time.LocalDate
)

private data class ResumeLifecycleSnapshot(
    val profile: PetProfile,
    val state: PetState,
    val emotion: PetEmotion,
    val traits: PetTrait,
    val conditions: Set<PetCondition>,
    val absenceBucket: com.aipet.brain.brain.pet.AbsenceBucket,
    val relationshipStage: com.aipet.brain.brain.pet.RelationshipStage,
    val dayBoundaryType: PetDayBoundaryType,
    val summaryDate: java.time.LocalDate,
    val decayPayload: com.aipet.brain.brain.events.PetStateDecayAppliedPayload
)

private data class StartupPetSnapshot(
    val profile: PetProfile,
    val state: PetState,
    val emotion: PetEmotion,
    val greeting: PetGreetingReaction,
    val traits: PetTrait,
    val conditions: Set<PetCondition>,
    val greetingDecision: PetBehaviorDecision<PetEmotion>,
    val greetedAtMs: Long,
    val dayBoundaryType: PetDayBoundaryType,
    val summaryDate: java.time.LocalDate,
    val absenceBucket: com.aipet.brain.brain.pet.AbsenceBucket = com.aipet.brain.brain.pet.AbsenceBucket.SHORT_RETURN,
    val relationshipStage: com.aipet.brain.brain.pet.RelationshipStage = com.aipet.brain.brain.pet.RelationshipStage.STRANGER,
    val evolutionAbsenceMs: Long = 0L
)

private data class InteractionAppliedSnapshot(
    val previousState: PetState,
    val state: PetState,
    val emotion: PetEmotion,
    val traits: PetTrait,
    val conditions: Set<PetCondition>,
    val decision: PetBehaviorDecision<PetEmotion>,
    val interactedAtMs: Long,
    val feedback: PetGameplayFeedback
)

private data class ActivityAppliedSnapshot(
    val result: PetActivityResult,
    val emotion: PetEmotion,
    val traits: PetTrait,
    val conditions: Set<PetCondition>,
    val decision: PetBehaviorDecision<PetEmotion>,
    val feedback: PetGameplayFeedback,
    val careActionType: CareActionType,
    val careResult: CareActionResult
)

private fun PetDayBoundaryType?.toDailySummaryLabel(): String? {
    return when (this) {
        PetDayBoundaryType.SAME_DAY_RETURN -> "Same-day return"
        PetDayBoundaryType.NEW_DAY_RETURN -> "New day"
        null -> null
    }
}

private fun PetActivityType.toEventType(): EventType {
    return when (this) {
        PetActivityType.FEED -> EventType.PET_FED
        PetActivityType.PLAY -> EventType.PET_PLAYED
        PetActivityType.REST -> EventType.PET_RESTED
    }
}
