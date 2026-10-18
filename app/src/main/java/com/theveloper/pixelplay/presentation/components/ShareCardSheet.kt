package com.theveloper.pixelplay.presentation.components

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.theveloper.pixelplay.R
import com.theveloper.pixelplay.data.model.Song
import com.theveloper.pixelplay.utils.ShareCardAspect
import com.theveloper.pixelplay.utils.ShareCardRenderer
import com.theveloper.pixelplay.utils.ShareCardSharing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Bottom sheet that previews the generated "music card" for [song] and lets the user pick
 * the aspect ratio, share it, or save it to the device. The preview shows the exact bitmap
 * that gets shared.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareCardSheet(
    song: Song,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var aspect by remember(song.id) { mutableStateOf(ShareCardAspect.SQUARE) }

    val cardBitmap by produceState<Bitmap?>(initialValue = null, song.id, aspect) {
        value = withContext(Dispatchers.Default) {
            runCatching { ShareCardRenderer.render(context, song, aspect) }.getOrNull()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = stringResource(R.string.share_card_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            val options = ShareCardAspect.values()
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, option ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                        selected = aspect == option,
                        onClick = { aspect = option },
                        label = {
                            Text(
                                stringResource(
                                    if (option == ShareCardAspect.SQUARE) {
                                        R.string.share_card_ratio_square
                                    } else {
                                        R.string.share_card_ratio_story
                                    }
                                )
                            )
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                contentAlignment = Alignment.Center
            ) {
                val bitmap = cardBitmap
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.share_card_preview_cd),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .height(300.dp)
                            .aspectRatio(aspect.ratio)
                            .clip(RoundedCornerShape(18.dp))
                    )
                } else {
                    CircularProgressIndicator()
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalButton(
                    modifier = Modifier.weight(1f),
                    enabled = cardBitmap != null,
                    onClick = {
                        val bitmap = cardBitmap ?: return@FilledTonalButton
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                ShareCardSharing.saveToGallery(
                                    context,
                                    bitmap,
                                    ShareCardSharing.fileNameFor(song, aspect)
                                )
                            }
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
                    Icon(imageVector = Icons.Rounded.Download, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.share_card_action_save))
                }

                Button(
                    modifier = Modifier.weight(1f),
                    enabled = cardBitmap != null,
                    onClick = {
                        val bitmap = cardBitmap ?: return@Button
                        val result = ShareCardSharing.share(
                            context,
                            bitmap,
                            ShareCardSharing.fileNameFor(song, aspect),
                            context.getString(R.string.share_card_chooser_title)
                        )
                        if (result.isFailure) {
                            Toast.makeText(context, R.string.share_card_share_error, Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Icon(imageVector = Icons.Rounded.Share, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.share_card_action_share))
                }
            }
        }
    }
}
