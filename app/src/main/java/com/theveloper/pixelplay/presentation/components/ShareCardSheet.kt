package com.theveloper.pixelplay.presentation.components

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.theveloper.pixelplay.R
import com.theveloper.pixelplay.data.model.Song
import com.theveloper.pixelplay.ui.theme.GoogleSansRounded
import com.theveloper.pixelplay.ui.theme.ShapeCache
import com.theveloper.pixelplay.utils.ShareCardAspect
import com.theveloper.pixelplay.utils.ShareCardRenderer
import com.theveloper.pixelplay.utils.ShareCardSharing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Bottom sheet that previews the generated "music card" for [song] and lets the user pick
 * the aspect ratio, share it, or save it to the device.
 *
 * The preview shows the exact bitmap that gets shared. Switching the aspect ratio animates the
 * card into its new shape (Material's default AnimatedContent morph) while the freshly rendered
 * bitmap fades in, and a small indicator marks the background render.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ShareCardSheet(
    song: Song,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var aspect by remember(song.id) { mutableStateOf(ShareCardAspect.SQUARE) }
    var renderedAspect by remember(song.id) { mutableStateOf(ShareCardAspect.SQUARE) }
    var cardBitmap by remember(song.id) { mutableStateOf<Bitmap?>(null) }
    var isRendering by remember(song.id) { mutableStateOf(true) }
    var isSaving by remember(song.id) { mutableStateOf(false) }

    LaunchedEffect(song.id, aspect) {
        isRendering = true
        val result = withContext(Dispatchers.Default) {
            runCatching { ShareCardRenderer.render(context, song, aspect) }.getOrNull()
        }
        if (result != null) {
            cardBitmap = result
            renderedAspect = aspect
        }
        isRendering = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colorScheme.surfaceContainerHigh,
        tonalElevation = 8.dp,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = stringResource(R.string.share_card_title),
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = GoogleSansRounded,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ToggleSegmentButton(
                    modifier = Modifier.weight(1f),
                    active = aspect == ShareCardAspect.SQUARE,
                    activeColor = colorScheme.primary,
                    inactiveColor = colorScheme.surfaceVariant,
                    activeContentColor = colorScheme.onPrimary,
                    inactiveContentColor = colorScheme.onSurfaceVariant,
                    activeCornerRadius = 32.dp,
                    onClick = { aspect = ShareCardAspect.SQUARE },
                    text = stringResource(R.string.share_card_ratio_square)
                )
                ToggleSegmentButton(
                    modifier = Modifier.weight(1f),
                    active = aspect == ShareCardAspect.STORY,
                    activeColor = colorScheme.primary,
                    inactiveColor = colorScheme.surfaceVariant,
                    activeContentColor = colorScheme.onPrimary,
                    inactiveContentColor = colorScheme.onSurfaceVariant,
                    activeCornerRadius = 32.dp,
                    onClick = { aspect = ShareCardAspect.STORY },
                    text = stringResource(R.string.share_card_ratio_story)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = cardBitmap,
                    contentAlignment = Alignment.Center,
                    label = "ShareCardPreview"
                ) { bitmap ->
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = stringResource(R.string.share_card_preview_cd),
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .height(300.dp)
                                .aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat())
                                .shadow(8.dp, ShapeCache.smooth20)
                                .clip(ShapeCache.smooth20)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .height(300.dp)
                                .aspectRatio(ShareCardAspect.SQUARE.ratio),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingIndicator(modifier = Modifier.size(44.dp))
                        }
                    }
                }

                ShareCardRenderingBadge(
                    visible = isRendering && cardBitmap != null,
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = colorScheme.surfaceContainerHighest,
                        contentColor = colorScheme.onSurfaceVariant
                    ),
                    enabled = cardBitmap != null && !isRendering && !isSaving,
                    onClick = {
                        val bitmap = cardBitmap ?: return@FilledTonalButton
                        isSaving = true
                        val targetAspect = renderedAspect
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                ShareCardSharing.saveToGallery(
                                    context,
                                    bitmap,
                                    ShareCardSharing.fileNameFor(song, targetAspect)
                                )
                            }
                            isSaving = false
                            Toast.makeText(
                                context,
                                context.getString(
                                    if (result.isSuccess) R.string.share_card_saved
                                    else R.string.share_card_save_error
                                ),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                ) {
                    if (isSaving) {
                        LoadingIndicator(modifier = Modifier.size(18.dp))
                    } else {
                        Icon(imageVector = Icons.Rounded.Download, contentDescription = null)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.share_card_action_save))
                }

                Button(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp),
                    shape = CircleShape,
                    enabled = cardBitmap != null && !isRendering,
                    onClick = {
                        val bitmap = cardBitmap ?: return@Button
                        val result = ShareCardSharing.share(
                            context,
                            bitmap,
                            ShareCardSharing.fileNameFor(song, renderedAspect),
                            context.getString(R.string.share_card_chooser_title)
                        )
                        if (result.isFailure) {
                            Toast.makeText(context, R.string.share_card_share_error, Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Icon(imageVector = Icons.Rounded.IosShare, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.share_card_action_share))
                }
            }
        }
    }
}

/**
 * Small badge that fades in while a fresh card bitmap is being rendered. It lives in its own
 * composable so [AnimatedVisibility] resolves to the plain overload instead of the
 * `ColumnScope` one, which cannot be called with an implicit receiver from inside the preview
 * `Box`.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ShareCardRenderingBadge(visible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(150)),
        exit = fadeOut(tween(220)),
        modifier = modifier
    ) {
        LoadingIndicator(modifier = Modifier.size(22.dp))
    }
}
