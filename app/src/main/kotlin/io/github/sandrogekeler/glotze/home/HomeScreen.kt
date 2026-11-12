package io.github.sandrogekeler.glotze.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import androidx.tv.material3.Text

@Composable
fun HomeScreen(
    apps: List<TvApp>,
    onOpen: (TvApp) -> Unit,
    art: ArtSource = NoArt,
) {
    // The remote has no pointer: without a focused item, the first key press is lost. Only
    // once, so the first card does not take focus back when it scrolls into view again.
    var focusPlaced by remember { mutableStateOf(false) }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 200.dp),
        contentPadding = PaddingValues(48.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        itemsIndexed(apps, key = { _, app -> app.packageName }) { index, app ->
            AppCard(
                app = app,
                art = art,
                onClick = { onOpen(app) },
                takeFocus = index == 0 && !focusPlaced,
                onFocusTaken = { focusPlaced = true },
            )
        }
    }
}

@Composable
private fun AppCard(
    app: TvApp,
    art: ArtSource,
    onClick: () -> Unit,
    takeFocus: Boolean,
    onFocusTaken: () -> Unit,
) {
    // Requested from inside the item: a lazy item exists only once the grid has measured it,
    // which can be after an effect in HomeScreen has already run.
    val focus = remember { FocusRequester() }
    if (takeFocus) {
        LaunchedEffect(Unit) {
            focus.requestFocus()
            onFocusTaken()
        }
    }
    var size by remember { mutableStateOf(IntSize.Zero) }
    // A card scrolled back into view draws its art in the first frame; a new one shows its
    // label until the art is decoded at the card's size.
    var loaded by remember(app) { mutableStateOf(art.cached(app)) }
    LaunchedEffect(app, size) {
        if (loaded == null && size != IntSize.Zero) loaded = art.load(app, size.width, size.height)
    }
    Card(
        onClick = onClick,
        modifier =
            Modifier
                .focusRequester(focus)
                .testTag(app.packageName)
                .aspectRatio(16f / 9f)
                .onSizeChanged { size = it },
    ) {
        val shown = loaded
        if (shown == null) {
            Text(text = app.label, modifier = Modifier.padding(16.dp))
        } else if (shown.isBanner) {
            Image(
                bitmap = shown.image,
                contentDescription = app.label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Image(bitmap = shown.image, contentDescription = app.label, contentScale = ContentScale.Fit)
            }
        }
    }
}

@Preview(device = "id:tv_1080p")
@Composable
private fun HomeScreenPreview() {
    HomeScreen(
        apps = listOf("Netflix", "YouTube", "Prime Video", "Plex").map { TvApp(it, it, activityName = "$it.Main") },
        onOpen = {},
    )
}
