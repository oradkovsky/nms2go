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
import androidx.compose.runtime.LaunchedEffect
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
import com.ror.nms2go.data.SenderOverview
import com.ror.nms2go.data.SenderEntity

@Composable
fun OverviewScreen(
    senders: List<SenderEntity>,
    loading: Boolean,
    statusText: String,
    overviewResults: List<SenderOverview>,
    onLoad: () -> Unit,
    onParseItem: (SenderOverview) -> Unit,
    onNavigateToConfig: () -> Unit
) {
    LaunchedEffect(senders, overviewResults) {
        if (senders.isNotEmpty() && overviewResults.isEmpty() && !loading) {
            onLoad()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        if (senders.isNotEmpty()) {
            Text(
                text = stringResource(R.string.overview_intro),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(20.dp))
        }

        if (loading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (senders.isEmpty()) {
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
            OverviewList(senders = senders, results = overviewResults, onParseItem = onParseItem)
        }

        if (senders.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun OverviewList(
    senders: List<SenderEntity>,
    results: List<SenderOverview>,
    onParseItem: (SenderOverview) -> Unit
) {
    if (results.isEmpty()) {
        // Don't show "Завантаження останніх листів…" when not loading – statusText at the bottom
        // already communicates idle / auth error. This prevents stuck loading hint after user
        // dismisses the Google account chooser (auth cancelled) while keeping the error label visible.
        return
    }

    LazyColumn {
        items(results, key = { it.senderQuery }) { result ->
            val companyName = senders.firstOrNull { it.email == result.senderQuery }?.companyName
            OverviewRow(result = result, companyName = companyName, onParseItem = onParseItem)
        }
    }
}

@Composable
private fun OverviewRow(
    result: SenderOverview,
    companyName: String?,
    onParseItem: (SenderOverview) -> Unit
) {
    val onClick: (() -> Unit)? = if (result.status == SenderOverview.Status.FOUND) {
        { onParseItem(result) }
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
            OverviewRowContent(result, companyName)
        }
    } else {
        OutlinedCard(modifier = cardModifier) {
            OverviewRowContent(result, companyName)
        }
    }
}

@Composable
private fun OverviewRowContent(result: SenderOverview, companyName: String?) {
    Column(modifier = Modifier.padding(16.dp)) {
        if (companyName != null) {
            Text(
                text = companyName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = result.senderQuery,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            Text(
                text = result.senderQuery,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(4.dp))
        when (result.status) {
            SenderOverview.Status.FOUND -> {
                Text(
                    text = result.subject?.let { displaySubject(it, result.date) }
                        ?: stringResource(R.string.overview_no_subject),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                result.date?.let { date ->
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

            SenderOverview.Status.NO_ATTACHMENTS -> {
                Text(
                    text = stringResource(R.string.overview_no_attachments),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SenderOverview.Status.NO_MESSAGES -> {
                Text(
                    text = stringResource(R.string.overview_no_messages),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * We no longer append subject with date in " (date)" because date/time is already shown
 * in the separate Text below subject. Strip trailing date-like parentheses to avoid duplication.
 */
private fun displaySubject(subject: String, date: String?): String {
    if (date.isNullOrBlank()) return subject
    // Exact match: subject ends with " (date)" where date is the same as displayed below
    val exactSuffix = " ($date)"
    if (subject.endsWith(exactSuffix)) {
        return subject.removeSuffix(exactSuffix)
    }
    // Date below is "dd.MM.yyyy HH:mm" but subject may contain only "dd.MM.yyyy"
    val dateOnly = date.substringBefore(" ")
    if (dateOnly != date && subject.endsWith(" ($dateOnly)")) {
        return subject.removeSuffix(" ($dateOnly)")
    }
    // Generic fallback: strip trailing "(dd.MM.yyyy...)" or "(dd-MM-yyyy...)" – any date-like parentheses
    return subject.replace(Regex("""\s*\(\s*\d{2}[.\-]\d{2}[.\-]\d{4}[^)]*\)\s*$"""), "")
}
