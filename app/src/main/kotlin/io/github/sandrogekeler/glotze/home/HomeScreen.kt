package io.github.sandrogekeler.glotze.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import androidx.tv.material3.Text

@Composable
fun HomeScreen(
    apps: List<TvApp>,
    onOpen: (TvApp) -> Unit,
) {
    val firstApp = remember { FocusRequester() }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 200.dp),
        contentPadding = PaddingValues(48.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        itemsIndexed(apps, key = { _, app -> app.packageName }) { index, app ->
            AppCard(
                app = app,
                onClick = { onOpen(app) },
                modifier = if (index == 0) Modifier.focusRequester(firstApp) else Modifier,
            )
        }
    }
    // The remote has no pointer: without a focused item, the first key press is lost.
    LaunchedEffect(apps.isNotEmpty()) {
        if (apps.isNotEmpty()) firstApp.requestFocus()
    }
}

@Composable
private fun AppCard(
    app: TvApp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(onClick = onClick, modifier = modifier.aspectRatio(16f / 9f)) {
        val banner = app.banner
        if (banner != null) {
            Image(
                bitmap = banner,
                contentDescription = app.label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(text = app.label, modifier = Modifier.padding(16.dp))
        }
    }
}

@Preview(device = "id:tv_1080p")
@Composable
private fun HomeScreenPreview() {
    HomeScreen(
        apps = listOf("Netflix", "YouTube", "Prime Video", "Plex").map { TvApp(it, it, banner = null) },
        onOpen = {},
    )
}
