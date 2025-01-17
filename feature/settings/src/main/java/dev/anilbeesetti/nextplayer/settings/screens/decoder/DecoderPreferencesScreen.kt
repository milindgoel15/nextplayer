package dev.anilbeesetti.nextplayer.settings.screens.decoder

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.anilbeesetti.nextplayer.core.model.DecoderPriority
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.components.ClickablePreferenceItem
import dev.anilbeesetti.nextplayer.core.ui.components.NextTopAppBar
import dev.anilbeesetti.nextplayer.core.ui.components.RadioTextButton
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import dev.anilbeesetti.nextplayer.settings.composables.OptionsDialog
import dev.anilbeesetti.nextplayer.settings.composables.PreferenceSubtitle
import dev.anilbeesetti.nextplayer.settings.extensions.name

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecoderPreferencesScreen(
    onNavigateUp: () -> Unit,
    viewModel: DecoderPreferencesViewModel = hiltViewModel()
) {
    val preferences by viewModel.preferencesFlow.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val scrollBehaviour = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehaviour.nestedScrollConnection),
        topBar = {
            NextTopAppBar(
                title = stringResource(id = R.string.decoder),
                scrollBehavior = scrollBehaviour,
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = NextIcons.ArrowBack,
                            contentDescription = stringResource(id = R.string.navigate_up)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = innerPadding,
            modifier = Modifier.fillMaxSize()
        ) {
            item { PreferenceSubtitle(text = stringResource(id = R.string.playback)) }
            decoderPrioritySetting(
                currentValue = preferences.decoderPriority,
                onClick = { viewModel.showDialog(DecoderPreferenceDialog.DecoderPriorityDialog) }
            )
        }

        when (uiState.showDialog) {
            DecoderPreferenceDialog.DecoderPriorityDialog -> {
                OptionsDialog(
                    text = stringResource(id = R.string.decoder_priority),
                    onDismissClick = viewModel::hideDialog
                ) {
                    items(DecoderPriority.values()) {
                        RadioTextButton(
                            text = it.name(),
                            selected = it == preferences.decoderPriority,
                            onClick = {
                                viewModel.updateDecoderPriority(it)
                                viewModel.hideDialog()
                            }
                        )
                    }
                }
            }
            DecoderPreferenceDialog.None -> Unit
        }
    }
}

fun LazyListScope.decoderPrioritySetting(
    currentValue: DecoderPriority,
    onClick: () -> Unit
) = item {
    ClickablePreferenceItem(
        title = stringResource(R.string.decoder_priority),
        description = currentValue.name(),
        icon = NextIcons.Priority,
        onClick = onClick
    )
}
