package com.gracetv.iptv.ui.navigation

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.gracetv.iptv.data.local.UserPreferences
import com.gracetv.iptv.di.AppContainer
import com.gracetv.iptv.domain.model.ContentType
import com.gracetv.iptv.ui.components.GraceTvWordmark
import com.gracetv.iptv.ui.screens.epg.EpgScreen
import com.gracetv.iptv.ui.screens.epg.EpgViewModel
import com.gracetv.iptv.ui.screens.favorites.FavoritesScreen
import com.gracetv.iptv.ui.screens.favorites.FavoritesViewModel
import com.gracetv.iptv.ui.screens.home.HomeScreen
import com.gracetv.iptv.ui.screens.home.HomeViewModel
import com.gracetv.iptv.ui.screens.livetv.LiveTvScreen
import com.gracetv.iptv.ui.screens.livetv.LiveTvViewModel
import com.gracetv.iptv.ui.screens.movies.MovieDetailScreen
import com.gracetv.iptv.ui.screens.movies.MoviesScreen
import com.gracetv.iptv.ui.screens.movies.MoviesViewModel
import com.gracetv.iptv.ui.screens.player.ActiveMediaDescriptor
import com.gracetv.iptv.ui.screens.player.PlayerScreen
import com.gracetv.iptv.ui.screens.player.PlayerViewModel
import com.gracetv.iptv.ui.screens.playlist.PlaylistScreen
import com.gracetv.iptv.ui.screens.playlist.PlaylistViewModel
import com.gracetv.iptv.ui.screens.search.SearchScreen
import com.gracetv.iptv.ui.screens.search.SearchViewModel
import com.gracetv.iptv.ui.screens.series.SeriesDetailScreen
import com.gracetv.iptv.ui.screens.series.SeriesScreen
import com.gracetv.iptv.ui.screens.series.SeriesViewModel
import com.gracetv.iptv.ui.screens.settings.SettingsScreen
import com.gracetv.iptv.ui.screens.settings.SettingsViewModel
import com.gracetv.iptv.ui.screens.splash.SplashScreen
import com.gracetv.iptv.ui.screens.support.SupportScreen
import com.gracetv.iptv.ui.theme.GraceRed
import com.gracetv.iptv.ui.theme.GraceTvTheme
import com.gracetv.iptv.ui.theme.GraceYellow
import com.gracetv.iptv.ui.theme.GraceYellowContainer
import kotlinx.coroutines.launch

private data class NavSection(
    val label: String,
    val icon: ImageVector,
    val route: GraceRoute,
    val testTag: String
)

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraceTvApp(container: AppContainer) {
    val prefs by container.preferencesManager.preferences.collectAsStateWithLifecycle(
        initialValue = UserPreferences()
    )

    GraceTvTheme(themeMode = prefs.themeMode) {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = backStackEntry?.destination
        val scope = rememberCoroutineScope()
        val drawerState = rememberDrawerState(DrawerValue.Closed)

        val homeViewModel: HomeViewModel = viewModel(
            factory = HomeViewModel.Factory(
                container.playlistRepository,
                container.contentRepository,
                container.preferencesManager
            )
        )
        val playlistViewModel: PlaylistViewModel = viewModel(
            factory = PlaylistViewModel.Factory(container.playlistRepository)
        )
        val liveTvViewModel: LiveTvViewModel = viewModel(
            factory = LiveTvViewModel.Factory(
                container.playlistRepository,
                container.contentRepository,
                container.epgRepository,
                container.preferencesManager
            )
        )
        val moviesViewModel: MoviesViewModel = viewModel(
            factory = MoviesViewModel.Factory(
                container.playlistRepository,
                container.contentRepository,
                container.preferencesManager
            )
        )
        val seriesViewModel: SeriesViewModel = viewModel(
            factory = SeriesViewModel.Factory(
                container.playlistRepository,
                container.contentRepository,
                container.preferencesManager
            )
        )
        val favoritesViewModel: FavoritesViewModel = viewModel(
            factory = FavoritesViewModel.Factory(container.contentRepository)
        )
        val searchViewModel: SearchViewModel = viewModel(
            factory = SearchViewModel.Factory(
                container.playlistRepository,
                container.contentRepository,
                container.preferencesManager
            )
        )
        val epgViewModel: EpgViewModel = viewModel(
            factory = EpgViewModel.Factory(
                container.playlistRepository,
                container.contentRepository,
                container.epgRepository,
                container.preferencesManager
            )
        )
        val settingsViewModel: SettingsViewModel = viewModel(
            factory = SettingsViewModel.Factory(
                container.preferencesManager,
                container.playlistRepository,
                container.contentRepository,
                container.epgRepository
            )
        )

        val isFullScreenRoute = currentDestination?.hasRoute<GraceRoute.Splash>() == true ||
            currentDestination?.hasRoute<GraceRoute.Player>() == true

        val drawerSections = listOf(
            NavSection("Home", Icons.Filled.Home, GraceRoute.Home, "nav_home"),
            NavSection("Live TV", Icons.Filled.LiveTv, GraceRoute.LiveTv, "nav_live_tv"),
            NavSection("Movies", Icons.Filled.Movie, GraceRoute.Movies, "nav_movies"),
            NavSection("Series", Icons.Filled.Tv, GraceRoute.Series, "nav_series"),
            NavSection("Favorites", Icons.Filled.Favorite, GraceRoute.Favorites, "nav_favorites"),
            NavSection("Search", Icons.Filled.Search, GraceRoute.Search, "nav_search"),
            NavSection("TV Guide (EPG)", Icons.Filled.CalendarMonth, GraceRoute.Epg(), "nav_epg"),
            NavSection("Playlists", Icons.Filled.PlaylistPlay, GraceRoute.Playlists, "nav_playlists"),
            NavSection("Settings", Icons.Filled.Settings, GraceRoute.Settings, "nav_settings"),
            NavSection("Contact & Support", Icons.Filled.SupportAgent, GraceRoute.Support, "nav_support")
        )

        val bottomBarSections = drawerSections.take(5)

        fun navigateTopLevel(route: GraceRoute) {
            navController.navigate(route) {
                popUpTo(GraceRoute.Home) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }

        fun launchPlayer(
            playlistId: Long,
            type: ContentType,
            itemId: Long,
            remoteId: String,
            title: String,
            subtitle: String,
            imageUrl: String,
            streamUrl: String,
            seriesRemoteId: String = ""
        ) {
            navController.navigate(
                GraceRoute.Player(
                    playlistId = playlistId,
                    contentType = type,
                    itemId = itemId,
                    remoteId = remoteId,
                    title = title,
                    subtitle = subtitle,
                    imageUrl = imageUrl,
                    streamUrl = streamUrl,
                    seriesRemoteId = seriesRemoteId
                )
            )
        }

        val navHostContent: @Composable (Modifier) -> Unit = { hostModifier ->
            NavHost(
                navController = navController,
                startDestination = GraceRoute.Splash,
                modifier = hostModifier
            ) {
                composable<GraceRoute.Splash> {
                    SplashScreen(
                        onFinished = {
                            scope.launch {
                                val autoChannel = homeViewModel.getAutoStartChannel()
                                navController.navigate(GraceRoute.Home) {
                                    popUpTo(GraceRoute.Splash) { inclusive = true }
                                }
                                if (autoChannel != null) {
                                    launchPlayer(
                                        playlistId = autoChannel.playlistId,
                                        type = ContentType.LIVE,
                                        itemId = autoChannel.id,
                                        remoteId = autoChannel.remoteId,
                                        title = autoChannel.name,
                                        subtitle = autoChannel.categoryName,
                                        imageUrl = autoChannel.logoUrl,
                                        streamUrl = autoChannel.streamUrl
                                    )
                                }
                            }
                        }
                    )
                }

                composable<GraceRoute.Home> {
                    HomeScreen(
                        viewModel = homeViewModel,
                        onOpenPlaylists = { navigateTopLevel(GraceRoute.Playlists) },
                        onOpenLiveTv = { navigateTopLevel(GraceRoute.LiveTv) },
                        onOpenMovies = { navigateTopLevel(GraceRoute.Movies) },
                        onOpenSeries = { navigateTopLevel(GraceRoute.Series) },
                        onOpenFavorites = { navigateTopLevel(GraceRoute.Favorites) },
                        onOpenEpg = { navigateTopLevel(GraceRoute.Epg()) },
                        onOpenMovieDetail = { id -> navController.navigate(GraceRoute.MovieDetail(id)) },
                        onOpenSeriesDetail = { id -> navController.navigate(GraceRoute.SeriesDetail(id)) },
                        onPlayProgress = { prog ->
                            launchPlayer(
                                playlistId = prog.playlistId,
                                type = prog.itemType,
                                itemId = prog.itemId,
                                remoteId = "",
                                title = prog.title,
                                subtitle = prog.subtitle,
                                imageUrl = prog.imageUrl,
                                streamUrl = prog.streamUrl
                            )
                        },
                        onPlayHistory = { hist ->
                            if (hist.itemType == ContentType.SERIES) {
                                navController.navigate(GraceRoute.SeriesDetail(hist.itemId))
                            } else {
                                launchPlayer(
                                    playlistId = hist.playlistId,
                                    type = hist.itemType,
                                    itemId = hist.itemId,
                                    remoteId = hist.remoteId,
                                    title = hist.title,
                                    subtitle = hist.subtitle,
                                    imageUrl = hist.imageUrl,
                                    streamUrl = hist.streamUrl
                                )
                            }
                        }
                    )
                }

                composable<GraceRoute.LiveTv> {
                    LiveTvScreen(
                        viewModel = liveTvViewModel,
                        onOpenPlaylists = { navigateTopLevel(GraceRoute.Playlists) },
                        onPlayChannel = { ch ->
                            launchPlayer(
                                playlistId = ch.playlistId,
                                type = ContentType.LIVE,
                                itemId = ch.id,
                                remoteId = ch.remoteId,
                                title = ch.name,
                                subtitle = ch.categoryName,
                                imageUrl = ch.logoUrl,
                                streamUrl = ch.streamUrl
                            )
                        }
                    )
                }

                composable<GraceRoute.Movies> {
                    MoviesScreen(
                        viewModel = moviesViewModel,
                        onOpenPlaylists = { navigateTopLevel(GraceRoute.Playlists) },
                        onOpenMovieDetail = { id -> navController.navigate(GraceRoute.MovieDetail(id)) }
                    )
                }

                composable<GraceRoute.MovieDetail> { entry ->
                    val args = entry.toRoute<GraceRoute.MovieDetail>()
                    MovieDetailScreen(
                        movieId = args.movieId,
                        viewModel = moviesViewModel,
                        onBack = { navController.popBackStack() },
                        onPlayMovie = { mv ->
                            launchPlayer(
                                playlistId = mv.playlistId,
                                type = ContentType.MOVIE,
                                itemId = mv.id,
                                remoteId = mv.remoteId,
                                title = mv.name,
                                subtitle = mv.categoryName,
                                imageUrl = mv.posterUrl,
                                streamUrl = mv.streamUrl
                            )
                        }
                    )
                }

                composable<GraceRoute.Series> {
                    SeriesScreen(
                        viewModel = seriesViewModel,
                        onOpenPlaylists = { navigateTopLevel(GraceRoute.Playlists) },
                        onOpenSeriesDetail = { id -> navController.navigate(GraceRoute.SeriesDetail(id)) }
                    )
                }

                composable<GraceRoute.SeriesDetail> { entry ->
                    val args = entry.toRoute<GraceRoute.SeriesDetail>()
                    SeriesDetailScreen(
                        seriesId = args.seriesId,
                        viewModel = seriesViewModel,
                        onBack = { navController.popBackStack() },
                        onPlayEpisode = { series, ep ->
                            launchPlayer(
                                playlistId = ep.playlistId,
                                type = ContentType.EPISODE,
                                itemId = ep.id,
                                remoteId = ep.remoteId,
                                title = "S${ep.seasonNumber}E${ep.episodeNumber} • ${ep.title}",
                                subtitle = series.name,
                                imageUrl = ep.thumbnailUrl.ifBlank { series.coverUrl },
                                streamUrl = ep.streamUrl,
                                seriesRemoteId = series.remoteId
                            )
                        }
                    )
                }

                composable<GraceRoute.Favorites> {
                    FavoritesScreen(
                        viewModel = favoritesViewModel,
                        onOpenFavorite = { fav ->
                            when (fav.itemType) {
                                ContentType.SERIES -> navController.navigate(GraceRoute.SeriesDetail(fav.itemId))
                                ContentType.MOVIE -> navController.navigate(GraceRoute.MovieDetail(fav.itemId))
                                else -> launchPlayer(
                                    playlistId = fav.playlistId,
                                    type = fav.itemType,
                                    itemId = fav.itemId,
                                    remoteId = fav.remoteId,
                                    title = fav.title,
                                    subtitle = fav.subtitle,
                                    imageUrl = fav.imageUrl,
                                    streamUrl = fav.streamUrl
                                )
                            }
                        }
                    )
                }

                composable<GraceRoute.Search> {
                    SearchScreen(
                        viewModel = searchViewModel,
                        onPlayChannel = { ch ->
                            launchPlayer(
                                playlistId = ch.playlistId,
                                type = ContentType.LIVE,
                                itemId = ch.id,
                                remoteId = ch.remoteId,
                                title = ch.name,
                                subtitle = ch.categoryName,
                                imageUrl = ch.logoUrl,
                                streamUrl = ch.streamUrl
                            )
                        },
                        onOpenMovieDetail = { mv -> navController.navigate(GraceRoute.MovieDetail(mv.id)) },
                        onOpenSeriesDetail = { sr -> navController.navigate(GraceRoute.SeriesDetail(sr.id)) },
                        onOpenPlaylists = { navigateTopLevel(GraceRoute.Playlists) }
                    )
                }

                composable<GraceRoute.Playlists> {
                    PlaylistScreen(viewModel = playlistViewModel)
                }

                composable<GraceRoute.Epg> { entry ->
                    val args = entry.toRoute<GraceRoute.Epg>()
                    EpgScreen(
                        initialChannelId = args.initialChannelId,
                        viewModel = epgViewModel,
                        onPlayChannel = { ch ->
                            launchPlayer(
                                playlistId = ch.playlistId,
                                type = ContentType.LIVE,
                                itemId = ch.id,
                                remoteId = ch.remoteId,
                                title = ch.name,
                                subtitle = ch.categoryName,
                                imageUrl = ch.logoUrl,
                                streamUrl = ch.streamUrl
                            )
                        }
                    )
                }

                composable<GraceRoute.Settings> {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onOpenSupport = { navigateTopLevel(GraceRoute.Support) }
                    )
                }

                composable<GraceRoute.Support> {
                    SupportScreen()
                }

                composable<GraceRoute.Player> { entry ->
                    val args = entry.toRoute<GraceRoute.Player>()
                    val context = LocalContext.current
                    val playerViewModel: PlayerViewModel = viewModel(
                        factory = PlayerViewModel.Factory(
                            context,
                            container.contentRepository,
                            container.preferencesManager
                        )
                    )
                    PlayerScreen(
                        descriptor = ActiveMediaDescriptor(
                            playlistId = args.playlistId,
                            contentType = args.contentType,
                            itemId = args.itemId,
                            remoteId = args.remoteId,
                            title = args.title,
                            subtitle = args.subtitle,
                            imageUrl = args.imageUrl,
                            streamUrl = args.streamUrl,
                            seriesRemoteId = args.seriesRemoteId
                        ),
                        viewModel = playerViewModel,
                        onExit = { navController.popBackStack() }
                    )
                }
            }
        }

        if (isFullScreenRoute) {
            navHostContent(Modifier.fillMaxSize())
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isWideScreen = maxWidth >= 840.dp

                val drawerItems: @Composable (Boolean) -> Unit = { closeOnTap ->
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 16.dp)
                    ) {
                        Spacer(Modifier.height(16.dp))
                        Box(modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) {
                            GraceTvWordmark()
                        }
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.height(8.dp))
                        drawerSections.forEach { sec ->
                            val selected = isRouteSelected(currentDestination, sec.route)
                            NavigationDrawerItem(
                                selected = selected,
                                onClick = {
                                    navigateTopLevel(sec.route)
                                    if (closeOnTap) scope.launch { drawerState.close() }
                                },
                                icon = { Icon(sec.icon, contentDescription = null) },
                                label = { Text(sec.label) },
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = GraceYellowContainer,
                                    selectedIconColor = GraceYellow,
                                    selectedTextColor = GraceYellow
                                ),
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 2.dp)
                                    .testTag("drawer_${sec.testTag}")
                            )
                        }
                    }
                }

                if (isWideScreen) {
                    PermanentNavigationDrawer(
                        drawerContent = {
                            PermanentDrawerSheet(
                                drawerContainerColor = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.width(260.dp)
                            ) {
                                drawerItems(false)
                            }
                        }
                    ) {
                        Scaffold(
                            contentWindowInsets = WindowInsets(0, 0, 0, 0)
                        ) { innerPadding ->
                            navHostContent(
                                Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            )
                        }
                    }
                } else {
                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            ModalDrawerSheet(
                                drawerContainerColor = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.width(280.dp)
                            ) {
                                drawerItems(true)
                            }
                        }
                    ) {
                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    title = { GraceTvWordmark() },
                                    navigationIcon = {
                                        IconButton(
                                            onClick = { scope.launch { drawerState.open() } },
                                            modifier = Modifier.testTag("open_drawer_button")
                                        ) {
                                            Icon(Icons.Filled.Menu, contentDescription = "Open menu")
                                        }
                                    },
                                    actions = {
                                        IconButton(
                                            onClick = { navigateTopLevel(GraceRoute.Search) },
                                            modifier = Modifier.testTag("topbar_search_button")
                                        ) {
                                            Icon(Icons.Filled.Search, contentDescription = "Search", tint = GraceYellow)
                                        }
                                        IconButton(
                                            onClick = { navigateTopLevel(GraceRoute.Playlists) },
                                            modifier = Modifier.testTag("topbar_playlists_button")
                                        ) {
                                            Icon(Icons.Filled.PlaylistPlay, contentDescription = "Playlists")
                                        }
                                        IconButton(
                                            onClick = { navigateTopLevel(GraceRoute.Settings) },
                                            modifier = Modifier.testTag("topbar_settings_button")
                                        ) {
                                            Icon(Icons.Filled.Settings, contentDescription = "Settings")
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                )
                            },
                            bottomBar = {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ) {
                                    bottomBarSections.forEach { sec ->
                                        val selected = isRouteSelected(currentDestination, sec.route)
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = { navigateTopLevel(sec.route) },
                                            icon = { Icon(sec.icon, contentDescription = sec.label) },
                                            label = { Text(sec.label) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = GraceYellow,
                                                selectedTextColor = GraceYellow,
                                                indicatorColor = GraceRed.copy(alpha = 0.22f)
                                            ),
                                            modifier = Modifier.testTag("bottom_${sec.testTag}")
                                        )
                                    }
                                }
                            }
                        ) { innerPadding ->
                            navHostContent(
                                Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun isRouteSelected(
    destination: androidx.navigation.NavDestination?,
    route: GraceRoute
): Boolean {
    if (destination == null) return false
    return when (route) {
        is GraceRoute.Home -> destination.hasRoute<GraceRoute.Home>()
        is GraceRoute.LiveTv -> destination.hasRoute<GraceRoute.LiveTv>()
        is GraceRoute.Movies -> destination.hasRoute<GraceRoute.Movies>() || destination.hasRoute<GraceRoute.MovieDetail>()
        is GraceRoute.Series -> destination.hasRoute<GraceRoute.Series>() || destination.hasRoute<GraceRoute.SeriesDetail>()
        is GraceRoute.Favorites -> destination.hasRoute<GraceRoute.Favorites>()
        is GraceRoute.Search -> destination.hasRoute<GraceRoute.Search>()
        is GraceRoute.Epg -> destination.hasRoute<GraceRoute.Epg>()
        is GraceRoute.Playlists -> destination.hasRoute<GraceRoute.Playlists>()
        is GraceRoute.Settings -> destination.hasRoute<GraceRoute.Settings>()
        is GraceRoute.Support -> destination.hasRoute<GraceRoute.Support>()
        else -> false
    }
}
