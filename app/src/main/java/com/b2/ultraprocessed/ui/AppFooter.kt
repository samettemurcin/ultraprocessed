package com.b2.ultraprocessed.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.b2.ultraprocessed.R

@Composable
fun AppFooter(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(AppTestTags.FOOTER),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.footer_copyright),
            modifier = Modifier.widthIn(max = 320.dp),
            color = Color.White.copy(alpha = 0.15f),
            fontSize = UiTextSizes.Caption,
            lineHeight = 12.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${stringResource(R.string.footer_built_with)} ",
                color = Color.White.copy(alpha = 0.16f),
                fontSize = UiTextSizes.Caption,
            )
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = Color.Red.copy(alpha = 0.4f),
                modifier = Modifier.size(10.dp),
            )
            Text(
                text = " ${stringResource(R.string.footer_humanity)}",
                color = Color.White.copy(alpha = 0.16f),
                fontSize = UiTextSizes.Caption,
            )
        }
    }
}
