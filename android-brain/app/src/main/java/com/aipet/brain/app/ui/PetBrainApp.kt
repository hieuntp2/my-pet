package com.aipet.brain.app.ui

import android.util.Log
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.room.Room
import com.aipet.brain.app.audio.AudioCaptureLifecycleEventPublisher
import com.aipet.brain.app.audio.AudioResponseDispatcher
import com.aipet.brain.app.animation.DefaultPetAnimatorFactory
import com.aipet.brain.app.animation.PetAnimationInputMapper
import com.aipet.brain.app.animation.PetAnimationSource
import com.aipet.brain.app.animation.PetAnimator
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
import com.aipet.brain.app.ui.debug.DebugScreen
import com.aipet.brain.app.ui.diary.DiaryScreen
import com.aipet.brain.app.ui.debug.EventViewerScreen
import com.aipet.brain.app.ui.debug.ObservationViewerScreen
import com.aipet.brain.app.ui.debug.WorkingMemoryDebugScreen
import com.aipet.brain.app.ui.home.HomeInteractionUiState
import com.aipet.brain.app.ui.home.HomeScreen
import com.aipet.brain.app.ui.home.HomeTodaySummaryResolver
import com.aipet.brain.app.ui.home.HomeUiModelBuilder
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
import com.aipet.brain.brain.events.CameraFrameReceivedPayload
import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.events.EventType
import com.aipet.brain.brain.events.InMemoryEventBus
import com.aipet.brain.brain.events.ObjectDetectedEventPayload
import com.aipet.brain.brain.events.PetActivityAppliedEventPayload
import com.aipet.brain.brain.events.PetGreetedEventPayload
import com.aipet.brain.brain.events.UserInteractedPetEventPayload
import com.aipet.brain.brain.events.audio.AudioResponseRequestPayload
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

private enum class AppScreen {
    Home,
    Onboarding,
    Debug,
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
    PersonDetail
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
    val currentScreen = currentScreenName.toAppScreen()
    var latestEvent by remember { mutableStateOf<EventEnvelope?>(null) }
    var latestOwnerSeenEvent by remember { mutableStateOf<EventEnvelope?>(null) }
    var latestOwnerGreetingEvent by remember { mutableStateOf<EventEnvelope?>(null) }
    var lastPublishedFaceCount by remember { mutableStateOf(0) }
    var recognitionProbeSummary by remember { mutableStateOf("not_run") }
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
                AppDatabase.MIGRATION_18_19
            )
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }
    val eventStore: EventStore = remember(database) {
        RoomEventStore(database.eventDao())
    }
    val personStore: PersonStore = remember(database) {
        RoomPersonStore(database.personDao())
    }
    val objectRepository = remember(database) {
        ObjectRepository(database.objectDao())
    }
    val faceProfileStore: FaceProfileStore = remember(database, personStore) {
        RoomFaceProfileStore(
            faceProfileDao = database.faceProfileDao(),
            personStore = personStore
        )
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
    val audioPlaybackEngine = remember(appContext, eventBus) {
        AudioPlaybackEngine(
            context = appContext,
            eventBus = eventBus
        )
    }
    val audioResponseDispatcher = remember(eventBus, audioPlaybackEngine) {
        AudioResponseDispatcher(
            eventBus = eventBus,
            playbackEngine = audioPlaybackEngine
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
            brainStateStore = brainStateStore
        )
    }
    val currentWorkingMemory by workingMemoryStore.observe().collectAsState(
        initial = workingMemoryStore.currentSnapshot()
    )
    val latestAudioStimulus by audioStimulusObserver.observeLatestStimulus().collectAsState(
        initial = audioStimulusObserver.currentLatestStimulus()
    )
    val recentInteractions by eventStore.observeLatest(limit = 24).collectAsState(initial = emptyList())
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
    val petAnimationState by petAnimator.state.collectAsState()
    val audioPlaybackDebugState by audioPlaybackEngine.observeDebugState().collectAsState(
        initial = audioPlaybackEngine.currentDebugState()
    )
    var currentPetEmotion by remember { mutableStateOf(PetEmotion.IDLE) }
    var currentPetState by remember { mutableStateOf<PetState?>(null) }
    var currentDayBoundaryType by remember { mutableStateOf<PetDayBoundaryType?>(null) }
    var currentSummaryDate by remember { mutableStateOf(java.time.LocalDate.now()) }
    var appOpenGreeting by remember { mutableStateOf<PetGreetingReaction?>(null) }
    var activePetProfile by remember { mutableStateOf<PetProfile?>(null) }
    var currentPetTraits by remember { mutableStateOf<PetTrait?>(null) }
    var currentPetConditions by remember { mutableStateOf(emptySet<PetCondition>()) }
    var latestBehaviorDecisionSource by remember { mutableStateOf<String?>(null) }
    var latestBehaviorDecision by remember { mutableStateOf<PetBehaviorDecision<PetEmotion>?>(null) }
    var homeInteractionFeedback by remember { mutableStateOf<PetGameplayFeedback?>(null) }
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
        homeTodaySummary
    ) {
        HomeUiModelBuilder.build(
            petName = activePetProfile?.name ?: PetProfileRepository.DEFAULT_PET_NAME,
            petState = currentPetState,
            emotion = currentPetEmotion,
            conditions = currentPetConditions,
            traits = currentPetTraits,
            todaySummary = homeTodaySummary
        )
    }
    val homeInteractionUiState = remember(homeInteractionFeedback) {
        HomeInteractionUiState(
            feedbackMessage = homeInteractionFeedback?.text,
            feedbackIsBlocked = homeInteractionFeedback?.isBlocked ?: false,
            careHint = "Feed, play, or rest with one tap from here.",
            interactionHint = "Tap for a quick hello or hold for a longer cuddle.",
            cooldownHint = "Repeated taps and care actions pause briefly so each reaction stays readable."
        )
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
        hasAppliedAppOpenLifecycle
    ) {
        if (hasAppliedAppOpenLifecycle) {
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
            val persistedState = if (decayedState != currentState) {
                petStateRepository.updateState(decayedState)
            } else {
                currentState
            }
            val conditions = petConditionResolver.resolve(persistedState)
            val emotion = petEmotionResolver.resolve(persistedState, conditions)
            val greetingResolution = petGreetingResolver.resolveDetailed(
                state = persistedState,
                emotion = emotion,
                conditions = conditions,
                traits = traits
            )
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
                summaryDate = dayBoundary.currentDate
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
        currentPetTraits = resolvedState.traits
        currentPetConditions = resolvedState.conditions
        homeInteractionFeedback = null
        latestBehaviorDecisionSource = "greeting"
        latestBehaviorDecision = resolvedState.greetingDecision
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
        hasAppliedAppOpenLifecycle = true
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
                EventType.AUDIO_RESPONSE_STARTED -> {
                    val payload = com.aipet.brain.brain.events.audio.AudioResponsePayload
                        .fromJson(event.payloadJson)
                    if (payload != null) {
                        val category = try {
                            AudioCategory.valueOf(payload.category)
                        } catch (e: Exception) {
                            AudioCategory.ACKNOWLEDGMENT
                        }
                        petAnimator.playTrigger(animationInputMapper.mapSoundTrigger(category))
                    }
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

    LaunchedEffect(loudSoundReactionRule) {
        loudSoundReactionRule.observeStimuliAndReact()
    }

    LaunchedEffect(voiceActivityAcknowledgmentRule) {
        voiceActivityAcknowledgmentRule.observeStimuliAndReact()
    }

    LaunchedEffect(wakeWordAcknowledgmentRule) {
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
        }
    }

    DisposableEffect(audioPlaybackEngine, faceEmbeddingEngine) {
        onDispose {
            audioPlaybackEngine.release()
            faceEmbeddingEngine.close()
        }
    }

    suspend fun publishGameplayAudioResponse(
        category: com.aipet.brain.app.ui.audio.model.AudioCategory,
        timestampMs: Long,
        cooldownKey: String
    ) {
        eventBus.publish(
            EventEnvelope.create(
                type = EventType.AUDIO_RESPONSE_REQUESTED,
                timestampMs = timestampMs,
                payloadJson = AudioResponseRequestPayload(
                    category = category.label,
                    priority = 2,
                    interruptPolicy = "INTERRUPT_NONE",
                    cooldownKey = cooldownKey,
                    timestamp = timestampMs
                ).toJson()
            )
        )
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
            val persistedState = petStateRepository.updateState(nextState)
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
        publishGameplayAudioResponse(
            category = PetGameplayAudioMapper.categoryForInteraction(interactionType),
            timestampMs = resolvedInteraction.interactedAtMs,
            cooldownKey = "pet_${interactionType.name.lowercase()}"
        )
        appOpenGreeting = null
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
            val persistedState = petStateRepository.updateState(activityResult.updatedState)
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
                )
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
        publishGameplayAudioResponse(
            category = PetGameplayAudioMapper.categoryForActivity(resolvedActivity.result),
            timestampMs = actedAtMs,
            cooldownKey = "pet_${resolvedActivity.result.activityType.name.lowercase()}"
        )
        appOpenGreeting = null
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

    MaterialTheme {
        Surface {
            when (currentScreen) {
                AppScreen.Home -> HomeScreen(
                    homeUiModel = homeUiModel,
                    homeInteractionUiState = homeInteractionUiState,
                    petAnimationState = petAnimationState,
                    appOpenGreeting = appOpenGreeting,
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
                    onNavigateToHome = { currentScreenName = AppScreen.Home.name },
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
                    }
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
                    onSelectCamera = { selection ->
                        coroutineScope.launch {
                            cameraSelectionStore.setSelectedCamera(selection)
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
                    onNavigateBack = { currentScreenName = AppScreen.Home.name }
                )

                AppScreen.Persons -> PersonsScreen(
                    personStore = personStore,
                    personSeenEventPublisher = personSeenEventPublisher,
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
            }
        }
    }
}

private fun String.toAppScreen(): AppScreen {
    return AppScreen.entries.firstOrNull { it.name == this } ?: AppScreen.Home
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
private const val DEBUG_OBJECT_EVENT_TAG = "ObjectEventPublisher"
private const val DEBUG_OBJECT_CREATE_TAG = "ObjectCreateDebug"
private const val DEBUG_OBJECT_STATS_TAG = "ObjectSeenStats"
private const val DEBUG_OBJECT_ALIAS_TAG = "ObjectAliasResolver"
private const val DEBUG_STARTUP_TAG = "PetBrainStartup"
private const val MAX_PET_NAME_LENGTH = 24

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
    val summaryDate: java.time.LocalDate
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
    val feedback: PetGameplayFeedback
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
