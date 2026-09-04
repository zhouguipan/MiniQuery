package com.marsz.miniquery.vm

import android.app.Application
import androidx.collection.LruCache
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.marsz.miniquery.data.model.DevData
import com.marsz.miniquery.data.model.FamilyDetail
import com.marsz.miniquery.data.model.FamilyMember
import com.marsz.miniquery.data.model.GiftItem
import com.marsz.miniquery.data.model.MapItem
import com.marsz.miniquery.data.model.MemberRole
import com.marsz.miniquery.data.model.MemberRow
import com.marsz.miniquery.data.model.NowProfile
import com.marsz.miniquery.data.model.RoomData
import com.marsz.miniquery.data.model.SkinItem
import com.marsz.miniquery.data.model.TrackingData
import com.marsz.miniquery.data.model.TrackingTeam
import com.marsz.miniquery.data.net.Api
import com.marsz.miniquery.data.prefs.AppPrefs
import com.marsz.miniquery.data.repo.ProfileRepository
import com.marsz.miniquery.data.repo.ProfileRepository.networkError
import com.marsz.miniquery.util.formatJoinTime
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 家族成员每页加载条数（滚动接近尾部时自动追加） */
const val MEMBER_PAGE_SIZE = 25

/** 批量资料接口单次最多携带的 uin 数量 */
private const val PROFILE_BATCH_SIZE = 80

/** 批量资料缓存上限，防止大量家族成员查询导致内存无限增长 */
private const val PROFILE_CACHE_MAX = 2000

class MainViewModel(app: Application) : AndroidViewModel(app) {

    /* ==================== 输入与当前查询对象 ==================== */

    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input.asStateFlow()

    private val _currentUin = MutableStateFlow<String?>(null)
    val currentUin: StateFlow<String?> = _currentUin.asStateFlow()

    private val _searching = MutableStateFlow(false)
    val searching: StateFlow<Boolean> = _searching.asStateFlow()

    private val _history = MutableStateFlow<List<String>>(emptyList())
    val history: StateFlow<List<String>> = _history.asStateFlow()

    /* ==================== 各页数据 ==================== */

    private val _profile = MutableStateFlow<NowProfile?>(null)
    val profile: StateFlow<NowProfile?> = _profile.asStateFlow()

    private val _tracking = MutableStateFlow<TrackingData?>(null)
    val tracking: StateFlow<TrackingData?> = _tracking.asStateFlow()

    private val _teamMembers = MutableStateFlow<List<MemberRow>>(emptyList())
    val teamMembers: StateFlow<List<MemberRow>> = _teamMembers.asStateFlow()

    private val _room = MutableStateFlow<RoomData?>(null)
    val room: StateFlow<RoomData?> = _room.asStateFlow()

    /** 房间接口是否已返回（无论成功失败），用于区分"加载中"与"确实不在房间" */
    private val _roomLoaded = MutableStateFlow(false)
    val roomLoaded: StateFlow<Boolean> = _roomLoaded.asStateFlow()

    private val _roomMembers = MutableStateFlow<List<MemberRow>>(emptyList())
    val roomMembers: StateFlow<List<MemberRow>> = _roomMembers.asStateFlow()

    private val _dev = MutableStateFlow<DevData?>(null)
    val dev: StateFlow<DevData?> = _dev.asStateFlow()

    private val _families = MutableStateFlow(TabState<List<FamilyDetail>>())
    val families: StateFlow<TabState<List<FamilyDetail>>> = _families.asStateFlow()

    private val _familyMembers = MutableStateFlow<Map<Long, FamilyMemberState>>(emptyMap())
    val familyMembers: StateFlow<Map<Long, FamilyMemberState>> = _familyMembers.asStateFlow()

    private val _skins = MutableStateFlow(TabState<List<SkinItem>>())
    val skins: StateFlow<TabState<List<SkinItem>>> = _skins.asStateFlow()

    private val _gifts = MutableStateFlow(TabState<List<GiftItem>>())
    val gifts: StateFlow<TabState<List<GiftItem>>> = _gifts.asStateFlow()

    private val _album = MutableStateFlow(TabState<List<String>>())
    val album: StateFlow<TabState<List<String>>> = _album.asStateFlow()

    private val _maps = MutableStateFlow(TabState<List<MapItem>>())
    val maps: StateFlow<TabState<List<MapItem>>> = _maps.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    /* ==================== 缓存 ==================== */

    /** uin -> 昵称 / 头像 / 头像框 */
    private data class ProfileLite(
        val name: String,
        val avatar: String,
        val headframe: String
    )

    private val profileCache = object : LruCache<String, ProfileLite>(PROFILE_CACHE_MAX) {}
    private val skinCache = object : LruCache<String, List<SkinItem>>(32) {}

    init {
        _history.value = AppPrefs.getHistory(app)
        // 上次查询的迷你号回填输入框，打开即用
        AppPrefs.lastUin(app)?.let { _input.value = it }
    }

    /* ==================== 输入 ==================== */

    fun onInputChanged(value: String) {
        // 迷你号只可能是数字，提前过滤掉误输入
        _input.value = value.filter { it.isDigit() }.take(20)
    }

    fun removeHistory(uin: String) {
        AppPrefs.removeHistory(getApplication(), uin)
        _history.value = AppPrefs.getHistory(getApplication())
    }

    fun clearHistory() {
        AppPrefs.clearHistory(getApplication())
        _history.value = emptyList()
    }

    /* ==================== 查询 ==================== */

    /** 未查询过就点功能入口时的提示 */
    fun notifyNeedQuery() {
        _events.tryEmit(UiEvent.Toast("请先查询一个迷你号"))
    }

    /** 点击查询 / 回车 */
    fun search(uinText: String? = null) {
        // 金标 / 个保法：未同意隐私政策前禁止发起任何联网查询
        if (!AppPrefs.isPrivacyAccepted(getApplication())) {
            _events.tryEmit(UiEvent.ConsentRequired)
            return
        }

        val uin = (uinText ?: _input.value).trim()
        if (uin.isEmpty()) {
            _events.tryEmit(UiEvent.Toast("请输入要查询的迷你号"))
            return
        }
        _input.value = uin
        resetAll()
        _currentUin.value = uin
        _searching.value = true

        val app = getApplication<Application>()
        AppPrefs.setLastUin(app, uin)
        AppPrefs.addHistory(app, uin)
        _history.value = AppPrefs.getHistory(app)

        viewModelScope.launch {
            try {
                val data = ProfileRepository.nowProfile(uin)
                _profile.value = data
                // 与网页一致：基础资料成功后并行加载追踪 / 房间 / 创作者信息
                val t = async { loadTrackingInternal(uin) }
                val r = async {
                    loadRoomInternal(uin)
                    _roomLoaded.value = true
                }
                val d = async { loadDevInternal(uin) }
                t.await(); r.await(); d.await()
            } catch (e: Exception) {
                _events.tryEmit(UiEvent.Toast(networkError(e)))
            } finally {
                _searching.value = false
            }
        }
    }

    private fun resetAll() {
        _profile.value = null
        _tracking.value = null
        _teamMembers.value = emptyList()
        _room.value = null
        _roomLoaded.value = false
        _roomMembers.value = emptyList()
        _dev.value = null
        _families.value = TabState()
        _familyMembers.value = emptyMap()
        _skins.value = TabState()
        _gifts.value = TabState()
        _album.value = TabState()
        _maps.value = TabState()
    }

    /* ==================== 状态 / 队伍 ==================== */

    private suspend fun loadTrackingInternal(uin: String) {
        val data = ProfileRepository.tracking(uin)?.data ?: return
        _tracking.value = data
        val team = data.status?.team_details
        if (team != null && !team.players.isNullOrEmpty()) {
            _teamMembers.value = buildTeamMembers(team, uin)
        }
    }

    private suspend fun buildTeamMembers(
        team: TrackingTeam,
        targetUin: String
    ): List<MemberRow> {
        val players = team.players ?: return emptyList()
        val profiles = fetchProfiles(players.map { it.uin.toString() })
        val leader = team.leader_uin?.toString()
        return players.map { p ->
            val uinStr = p.uin.toString()
            val lite = profiles[uinStr]
            val (roleText, role) = when {
                leader != null && uinStr == leader -> "队长" to MemberRole.LEADER
                uinStr == targetUin -> "本人" to MemberRole.SELF
                else -> "成员" to MemberRole.NORMAL
            }
            MemberRow(
                uin = uinStr,
                name = lite?.name ?: (p.name ?: "玩家 $uinStr"),
                avatarUrl = lite?.avatar ?: Api.defaultAvatar,
                headframeUrl = lite?.headframe ?: Api.headframe(null),
                roleText = roleText,
                role = role,
                joinTimeText = formatJoinTime(p.time),
                sortWeight = if (leader != null && uinStr == leader) 0
                else if (uinStr == targetUin) 1 else 2
            )
        }.sortedWith(compareBy { it.sortWeight })
    }

    /* ==================== 房间 ==================== */

    fun loadRoom(force: Boolean = false) {
        val uin = _currentUin.value ?: return
        // 首页查询时已经拉过一次，这里直接复用，不再重复请求
        if (!force && (_roomLoaded.value || _room.value != null)) return
        _roomLoaded.value = false
        viewModelScope.launch {
            loadRoomInternal(uin)
            // 无论成功失败都置位，避免界面一直停在加载中
            _roomLoaded.value = true
        }
    }

    private suspend fun loadRoomInternal(uin: String) {
        val data = ProfileRepository.room(uin)?.data ?: return
        _room.value = data
        val r = data.room_info
        if (r == null || r.members.isNullOrEmpty()) {
            _roomMembers.value = emptyList()
            return
        }
        val ownerUin = r.owner?.uin?.toString()
        val members = r.members
        val profiles = fetchProfiles(members.map { it.uin.toString() })
        _roomMembers.value = members.map { m ->
            val uinStr = m.uin.toString()
            val lite = profiles[uinStr]
            val isOwner = m.is_owner || (ownerUin != null && uinStr == ownerUin)
            MemberRow(
                uin = uinStr,
                name = lite?.name ?: (m.name ?: if (isOwner) (r.owner?.name ?: "房主") else "玩家 $uinStr"),
                avatarUrl = lite?.avatar ?: Api.defaultAvatar,
                headframeUrl = lite?.headframe ?: Api.headframe(null),
                roleText = if (isOwner) "房主" else "成员",
                role = if (isOwner) MemberRole.CREATOR else MemberRole.NORMAL,
                joinTimeText = "-",
                sortWeight = if (isOwner) 0 else 1
            )
        }.sortedWith(compareBy { it.sortWeight })
    }

    /* ==================== 创作者 ==================== */

    private suspend fun loadDevInternal(uin: String) {
        val resp = ProfileRepository.developer(uin)
        _dev.value = if (resp?.code == 0) resp.data else null
    }

    /* ==================== 家族 ==================== */

    /**
     * 加载用户加入的家族列表。
     *
     * 注意：这里**只加载家族本身**，不预取任何成员资料。
     * 成员留到进入家族详情页后再按滚动位置分页拉取，
     * 这样列表页的首屏时间只取决于一个接口，滑动也不会因为后台批量请求而卡顿。
     */
    fun loadFamilies(force: Boolean = false) {
        val uin = _currentUin.value
        if (uin == null) {
            _events.tryEmit(UiEvent.Toast("请先查询一个迷你号"))
            return
        }
        val st = _families.value
        if (!force && (st.loading || st.loaded)) return
        _families.value = st.copy(loading = true, error = null)

        viewModelScope.launch {
            runCatching {
                val ids = ProfileRepository.familyIds(uin)
                if (ids.isEmpty()) {
                    _families.value = TabState(loaded = true, data = emptyList())
                    return@launch
                }
                val details = ProfileRepository.familyDetails(ids)
                _families.value = TabState(loaded = true, data = details)
            }.onFailure {
                _families.value = TabState(error = networkError(it))
            }
        }
    }

    /**
     * 加载某个家族的下一页成员。
     * 由详情页在滚动接近尾部时调用；已在加载或已全部加载完时直接返回，可安全重复调用。
     */
    fun loadFamilyMemberPage(family: FamilyDetail) {
        val targetUin = _currentUin.value ?: return
        val state = _familyMembers.value[family.id]
        if (state?.loading == true || state?.allLoaded == true) return

        val all = sortMembers(family, targetUin)
        val offset = state?.offset ?: 0
        val batch = all.drop(offset).take(MEMBER_PAGE_SIZE)
        if (batch.isEmpty()) {
            _familyMembers.update { map ->
                map.toMutableMap().apply {
                    put(family.id, (state ?: FamilyMemberState()).copy(allLoaded = true, total = all.size))
                }
            }
            return
        }

        viewModelScope.launch {
            _familyMembers.update { map ->
                map.toMutableMap().apply {
                    put(family.id, (state ?: FamilyMemberState(total = all.size)).copy(loading = true))
                }
            }
            val rows = buildFamilyMembers(family, batch, targetUin)
            val newOffset = offset + batch.size
            // 多个家族可能并发加载，用 update 做原子合并，避免相互覆盖
            _familyMembers.update { map ->
                val prev = map[family.id]
                map.toMutableMap().apply {
                    put(
                        family.id,
                        FamilyMemberState(
                            rows = (prev?.rows ?: emptyList()) + rows,
                            offset = newOffset,
                            total = all.size,
                            loading = false,
                            allLoaded = newOffset >= all.size
                        )
                    )
                }
            }
        }
    }

    /** 成员排序：本人 → 族长 → 创始人 → 其他（保持原始相对顺序） */
    private fun sortMembers(family: FamilyDetail, targetUin: String): List<FamilyMember> {
        val list = family.member_list ?: return emptyList()
        return list.sortedWith(
            compareBy { m: FamilyMember ->
                val u = m.uin.toString()
                when {
                    u == targetUin -> 0
                    family.leader_uin?.toString() == u -> 1
                    family.creator_uin?.toString() == u -> 2
                    else -> 3
                }
            }
        )
    }

    private suspend fun buildFamilyMembers(
        family: FamilyDetail,
        batch: List<FamilyMember>,
        targetUin: String
    ): List<MemberRow> {
        if (batch.isEmpty()) return emptyList()
        val profiles = fetchProfiles(batch.map { it.uin.toString() })
        val leader = family.leader_uin?.toString()
        val creator = family.creator_uin?.toString()
        return batch.map { m ->
            val uinStr = m.uin.toString()
            val lite = profiles[uinStr]
            val (roleText, role) = when {
                leader != null && uinStr == leader -> "族长" to MemberRole.LEADER
                creator != null && uinStr == creator -> "创始人" to MemberRole.CREATOR
                uinStr == targetUin -> "本人" to MemberRole.SELF
                else -> "成员" to MemberRole.NORMAL
            }
            MemberRow(
                uin = uinStr,
                name = lite?.name ?: "查询失败",
                avatarUrl = lite?.avatar ?: Api.defaultAvatar,
                headframeUrl = lite?.headframe ?: Api.headframe(null),
                roleText = roleText,
                role = role,
                joinTimeText = formatJoinTime(m.time)
            )
        }
    }

    /* ==================== 皮肤 ==================== */

    fun loadSkins(force: Boolean = false) {
        val uin = _currentUin.value ?: return
        val st = _skins.value
        if (!force && (st.loading || st.loaded)) return

        skinCache[uin]?.let {
            _skins.value = TabState(loaded = true, data = it)
            return
        }
        _skins.value = st.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching {
                val list = ProfileRepository.skins(uin).skin_list ?: emptyList()
                skinCache.put(uin, list)
                _skins.value = TabState(loaded = true, data = list)
            }.onFailure {
                _skins.value = TabState(error = networkError(it))
            }
        }
    }

    /* ==================== 礼物 ==================== */

    fun loadGifts(force: Boolean = false) {
        val uin = _currentUin.value ?: return
        val st = _gifts.value
        if (!force && (st.loading || st.loaded)) return
        _gifts.value = st.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching {
                val list = ProfileRepository.gifts(uin).gift_list ?: emptyList()
                _gifts.value = TabState(loaded = true, data = list)
            }.onFailure {
                _gifts.value = TabState(error = networkError(it))
            }
        }
    }

    /* ==================== 相册 ==================== */

    fun loadAlbum(force: Boolean = false) {
        val uin = _currentUin.value ?: return
        val st = _album.value
        if (!force && (st.loading || st.loaded)) return
        _album.value = st.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching {
                val urls = ProfileRepository.album(uin).thumb_urls ?: emptyList()
                _album.value = TabState(loaded = true, data = urls)
            }.onFailure {
                _album.value = TabState(error = networkError(it))
            }
        }
    }

    /* ==================== 地图 ==================== */

    fun loadMaps(force: Boolean = false) {
        val uin = _currentUin.value ?: return
        val st = _maps.value
        if (!force && (st.loading || st.loaded)) return
        _maps.value = st.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching {
                val list = ProfileRepository.maps(uin).map_info_list?.map_info_list ?: emptyList()
                _maps.value = TabState(loaded = true, data = list)
            }.onFailure {
                _maps.value = TabState(error = networkError(it))
            }
        }
    }

    /* ==================== 批量资料（带缓存，每批最多 80 个 uin） ==================== */

    private suspend fun fetchProfiles(uins: List<String>): Map<String, ProfileLite> {
        val result = LinkedHashMap<String, ProfileLite>()
        val distinct = uins.distinct()
        val missing = distinct.filter { profileCache[it] == null }
        missing.chunked(PROFILE_BATCH_SIZE).forEach { chunk ->
            runCatching { ProfileRepository.profileList(chunk) }
                .getOrDefault(emptyMap())
                .forEach { (uin, item) ->
                    profileCache.put(
                        uin,
                        ProfileLite(
                            name = item.nickname ?: "未知",
                            avatar = item.avatar_url ?: Api.defaultAvatar,
                            headframe = Api.headframe(item.head_frame_id)
                        )
                    )
                }
        }
        distinct.forEach { uin -> profileCache[uin]?.let { result[uin] = it } }
        return result
    }

    /** 相册图片 URL 列表（供大图预览使用） */
    fun albumUrls(): List<String> = _album.value.data ?: emptyList()
}
