package com.grupo2.ashley.home.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grupo2.ashley.home.models.Category
import com.grupo2.ashley.home.models.CategoryIcon
import com.grupo2.ashley.ui.theme.AnimationConstants

@Composable
fun CategoriesRow(
    categories: List<Category>, selectedCategory: String, onCategoryClick: (String) -> Unit
) {
    androidx.compose.foundation.lazy.LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(categories.size) { index ->
            val category = categories[index]
            CategoryItem(
                category = category,
                isSelected = selectedCategory == category.id,
                onClick = { onCategoryClick(category.id) })
        }
    }
}

@Composable
fun CategoryItem(
    category: Category, isSelected: Boolean, onClick: () -> Unit
) {
    // Animación de escala cuando está seleccionado
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = tween(AnimationConstants.FLUID_DURATION),
        label = "category_scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(scale)
            .clickable { onClick() }
            .padding(vertical = 8.dp)
            .animateContentSize(animationSpec = tween(AnimationConstants.FLUID_DURATION))) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = MaterialTheme.shapes.medium,
            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
            border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            else null,
            tonalElevation = if (isSelected) 4.dp else 0.dp
        ) {
            Box(
                contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = when (category.icon) {
                        CategoryIcon.ALL -> Icons.Default.Apps
                        CategoryIcon.ELECTRONICS -> Icons.Default.Smartphone
                        CategoryIcon.FASHION -> Icons.Default.Checkroom
                        CategoryIcon.HOME -> Icons.Default.Home
                        CategoryIcon.SPORTS -> Icons.Default.SportsBasketball
                        CategoryIcon.BOOKS -> Icons.Default.MenuBook
                        CategoryIcon.TOYS -> Icons.Default.Toys
                        CategoryIcon.VEHICLES -> Icons.Default.DirectionsCar
                        CategoryIcon.OTHERS -> Icons.Default.MoreHoriz
                    },
                    contentDescription = stringResource(category.labelResId),
                    modifier = Modifier.size(32.dp),
                    tint = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(category.labelResId),
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
