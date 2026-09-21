package com.ror.nms2go.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ror.nms2go.R
import com.ror.nms2go.utils.debugTestTag

@Composable
fun OverviewScreen(
    uiModel: OverviewUiModel,
    onParseItem: (String) -> Unit,
    onNavigateToConfig: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        if (uiModel.hasConfiguredSenders) {
            Text(
                text = stringResource(R.string.overview_intro),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(20.dp))
        }

        if (uiModel.isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiModel.showEmptyState) {
            val prefix = stringResource(R.string.overview_empty_prefix)
            val configuration = stringResource(R.string.menu_configuration)
            val linkColor = MaterialTheme.colorScheme.primary
            val annotatedString = buildAnnotatedString {
                append(prefix)
                append(" ")
                val start = length
                append(configuration)
                addStyle(
                    SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline,
                        fontWeight = FontWeight.SemiBold
                    ),
                    start = start,
                    end = length
                )
                addStringAnnotation("menu_configuration", "menu_configuration", start, length)
            }
            ClickableText(
                text = annotatedString,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier
                    .padding(end = 2.dp)
                    .debugTestTag("emptyStateLink"),
                onClick = { onNavigateToConfig() }
            )
        } else {
            OverviewList(items = uiModel.items, onParseItem = onParseItem)
        }

        if (uiModel.showStatus) {
            Spacer(Modifier.height(20.dp))

            Text(
                text = uiModel.statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun OverviewList(
    items: List<OverviewUiItem>,
    onParseItem: (String) -> Unit
) {
    if (items.isEmpty()) {
        // Don't show "Завантаження останніх листів…" when not loading – statusText at the bottom
        // already communicates idle / auth error. This prevents stuck loading hint after user
        // dismisses the Google account chooser (auth cancelled) while keeping the error label visible.
        return
    }

    LazyColumn {
        items(items, key = { it.senderQuery }) { item ->
            OverviewRow(item = item, onParseItem = onParseItem)
        }
    }
}

@Composable
private fun OverviewRow(
    item: OverviewUiItem,
    onParseItem: (String) -> Unit
) {
    val onClick: (() -> Unit)? = if (item.isParseable) {
        { onParseItem(item.senderQuery) }
    } else {
        null
    }
    val cardModifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 12.dp)
    if (onClick != null) {
        OutlinedCard(
            onClick = onClick,
            modifier = cardModifier
        ) {
            OverviewRowContent(item)
        }
    } else {
        OutlinedCard(modifier = cardModifier) {
            OverviewRowContent(item)
        }
    }
}

@Composable
private fun OverviewRowContent(item: OverviewUiItem) {
    Column(modifier = Modifier.padding(16.dp)) {
        if (item.companyName != null) {
            Text(
                text = item.companyName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = item.senderQuery,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            Text(
                text = item.senderQuery,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(4.dp))
        when (item.status) {
            OverviewUiItemStatus.FOUND -> {
                Text(
                    text = item.subject
                        ?: stringResource(R.string.overview_no_subject),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                item.date?.let { date ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = date,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.overview_tap_to_parse),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            OverviewUiItemStatus.NO_ATTACHMENTS -> {
                Text(
                    text = stringResource(R.string.overview_no_attachments),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OverviewUiItemStatus.NO_MESSAGES -> {
                Text(
                    text = stringResource(R.string.overview_no_messages),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
