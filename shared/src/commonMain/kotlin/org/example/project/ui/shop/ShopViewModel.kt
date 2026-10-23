package org.example.project.ui.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.example.project.data.AppSettings
import org.example.project.data.cache.AssetCache

/** Something on its way from the server. */
internal sealed interface ShopLoad<out T> {
    data object Loading : ShopLoad<Nothing>
    data object Failed : ShopLoad<Nothing>
    data class Ready<T>(val value: T) : ShopLoad<T>
}

/** The id of the New pill, beside the categories' own. */
internal const val NewPillId = "#new"

/**
 * One library tab: its pills, the picked one ([selectedId]: a category's id or [NewPillId]; null for
 * the first category), each pill's packs as they come in, and the search.
 */
internal data class ShopTabState(
    val categories: ShopLoad<List<ShopCategory>> = ShopLoad.Loading,
    val selectedId: String? = null,
    val packs: Map<String, ShopLoad<List<ShopPack>>> = emptyMap(),
    val query: String = "",
) {
    val selectedPillId: String?
        get() = selectedId ?: (categories as? ShopLoad.Ready)?.value?.firstOrNull()?.id

    /** What the list shows: the search's matches while there is a query, else the picked pill's packs. */
    val shown: ShopLoad<List<ShopPack>>
        get() {
            val categoryList = when (val c = categories) {
                is ShopLoad.Ready -> c.value
                ShopLoad.Loading -> return ShopLoad.Loading
                ShopLoad.Failed -> return ShopLoad.Failed
            }
            if (query.isBlank()) return selectedPillId?.let { packs[it] } ?: ShopLoad.Ready(emptyList())
            val loaded = categoryList.mapNotNull { category -> (packs[category.id] as? ShopLoad.Ready)?.let { category to it.value } }
            val matches = searchShopPacks(loaded, query)
            // Every category is read for a search; the matches show as they come, the spinner until the first.
            val stillLoading = categoryList.any { packs[it.id] !is ShopLoad.Ready && packs[it.id] != ShopLoad.Failed }
            return if (matches.isEmpty() && stillLoading) ShopLoad.Loading else ShopLoad.Ready(matches)
        }
}

internal data class ShopUiState(
    val tab: ShopTab,
    val tabs: Map<ShopTab, ShopTabState> = ShopTab.entries.filter { it != ShopTab.Premium }.associateWith { ShopTabState() },
    /** Packs being downloaded, and how far (0-1). */
    val downloading: Map<String, Float> = emptyMap(),
    /** Packs every item of which is kept on the disk. */
    val downloaded: Set<String> = emptySet(),
    val isSubscribed: Boolean = false,
) {
    val current: ShopTabState? get() = tabs[tab]
}

/**
 * The Shop: one [ShopSource] per library tab, each read when its tab is first shown. A pill's packs
 * are read when it is picked; a search reads every category of the tab.
 *
 * Download keeps every item of a pack in [AssetCache] under the key its tool reads it by, so the tool
 * then has it without the network. A premium pack is a subscriber's to download: for anyone else
 * the Shop turns to its Premium tab instead.
 */
class ShopViewModel internal constructor(
    initialTab: ShopTab,
    private val sources: Map<ShopTab, ShopSource>,
    private val assetCache: AssetCache,
    private val picks: ShopPicks,
    settings: AppSettings,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShopUiState(tab = initialTab))
    internal val uiState: StateFlow<ShopUiState> = _uiState.asStateFlow()

    // Tabs whose categories are being read; a tab's state reads Loading before its first read too.
    private val loading = mutableSetOf<ShopTab>()

    init {
        viewModelScope.launch { settings.isPremium.collect { premium -> _uiState.update { it.copy(isSubscribed = premium) } } }
        selectTab(initialTab)
    }

    internal fun selectTab(tab: ShopTab) {
        _uiState.update { it.copy(tab = tab) }
        val state = _uiState.value.tabs[tab] ?: return
        if (state.categories == ShopLoad.Failed || (state.categories == ShopLoad.Loading && tab !in loading)) loadCategories(tab)
    }

    private fun loadCategories(tab: ShopTab) {
        val source = sources[tab] ?: return
        loading += tab
        updateTab(tab) { it.copy(categories = ShopLoad.Loading) }
        viewModelScope.launch {
            val categories = load { source.categories() }
            loading -= tab
            updateTab(tab) { it.copy(categories = categories) }
            val state = _uiState.value.tabs[tab] ?: return@launch
            if (state.query.isNotBlank()) loadAllPacks(tab) else state.selectedPillId?.let { loadPacks(tab, it) }
        }
    }

    internal fun selectPill(id: String) {
        val tab = _uiState.value.tab
        updateTab(tab) { it.copy(selectedId = id) }
        loadPacks(tab, id)
    }

    internal fun search(query: String) {
        val tab = _uiState.value.tab
        updateTab(tab) { it.copy(query = query) }
        if (query.isNotBlank()) loadAllPacks(tab)
    }

    /** Retry: whatever the current tab failed to read. */
    internal fun retry() {
        val tab = _uiState.value.tab
        val state = _uiState.value.tabs[tab] ?: return
        when {
            state.categories == ShopLoad.Failed -> loadCategories(tab)
            state.query.isNotBlank() -> loadAllPacks(tab, retryFailed = true)
            else -> state.selectedPillId?.let { loadPacks(tab, it, retryFailed = true) }
        }
    }

    private fun loadAllPacks(tab: ShopTab, retryFailed: Boolean = false) {
        val categories = (_uiState.value.tabs[tab]?.categories as? ShopLoad.Ready)?.value ?: return
        categories.forEach { loadPacks(tab, it.id, retryFailed) }
    }

    private fun loadPacks(tab: ShopTab, pillId: String, retryFailed: Boolean = false) {
        val source = sources[tab] ?: return
        val state = _uiState.value.tabs[tab] ?: return
        val categories = (state.categories as? ShopLoad.Ready)?.value ?: return
        when (state.packs[pillId]) {
            is ShopLoad.Ready, ShopLoad.Loading -> return
            ShopLoad.Failed -> if (!retryFailed) return
            null -> Unit
        }
        updateTab(tab) { it.copy(packs = it.packs + (pillId to ShopLoad.Loading)) }
        viewModelScope.launch {
            val packs = load {
                if (pillId == NewPillId) source.newPacks(categories) else source.packs(categories.first { it.id == pillId })
            }
            updateTab(tab) { it.copy(packs = it.packs + (pillId to packs)) }
            if (packs is ShopLoad.Ready) checkDownloaded(packs.value)
        }
    }

    /** Marks the [packs] whose every item is already kept, e.g. by an earlier visit or by using them in the tool. */
    private suspend fun checkDownloaded(packs: List<ShopPack>) {
        val kept = withContext(Dispatchers.IO) {
            packs.filter { pack -> pack.items.all { assetCache.isKept(it.fileUrl) } }.map { it.id }
        }
        if (kept.isNotEmpty()) _uiState.update { it.copy(downloaded = it.downloaded + kept) }
    }

    internal fun download(pack: ShopPack) {
        val state = _uiState.value
        if (pack.id in state.downloading || pack.id in state.downloaded) return
        if (pack.isPremium && !state.isSubscribed) {
            selectTab(ShopTab.Premium)
            return
        }
        _uiState.update { it.copy(downloading = it.downloading + (pack.id to 0f)) }
        viewModelScope.launch {
            var done = 0
            val ok = try {
                pack.items.map { item ->
                    async {
                        assetCache.bytes(item.fileUrl, keep = true)
                        done++
                        _uiState.update { it.copy(downloading = it.downloading + (pack.id to done.toFloat() / pack.items.size)) }
                    }
                }.awaitAll()
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                false
            }
            _uiState.update {
                it.copy(
                    downloading = it.downloading - pack.id,
                    downloaded = if (ok) it.downloaded + pack.id else it.downloaded,
                )
            }
        }
    }

    /** "Use": hands [pack] to the tool under the Shop, which opens on it once the Shop is gone. */
    internal fun use(pack: ShopPack) {
        picks.post(ShopPick(_uiState.value.tab, pack.categoryId, pack.id))
    }

    private fun updateTab(tab: ShopTab, change: (ShopTabState) -> ShopTabState) {
        _uiState.update { state -> state.tabs[tab]?.let { state.copy(tabs = state.tabs + (tab to change(it))) } ?: state }
    }

    private suspend fun <T> load(block: suspend () -> T): ShopLoad<T> = try {
        ShopLoad.Ready(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        ShopLoad.Failed
    }
}
