package com.grupo2.ashley.dashboard.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries

@Composable
fun VicoLineChart(
    data: List<Pair<String, Int>>,
    title: String,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF6200EE)
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (data.isEmpty() || data.all { it.second == 0 }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay datos disponibles",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
                val modelProducer = remember { CartesianChartModelProducer() }
                
                LaunchedEffect(data) {
                    modelProducer.runTransaction {
                        lineSeries { series(data.map { it.second }) }
                    }
                }

                CartesianChartHost(
                    chart = rememberCartesianChart(
                        rememberLineCartesianLayer(),
                        startAxis = rememberStartAxis(
                            label = rememberAxisLabelComponent(
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        ),
                        bottomAxis = rememberBottomAxis(
                            label = rememberAxisLabelComponent(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            valueFormatter = { value, _, _ ->
                                data.getOrNull(value.toInt())?.first?.takeLast(5) ?: ""
                            }
                        )
                    ),
                    modelProducer = modelProducer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
        }
    }
}

@Composable
fun VicoBarChart(
    categories: Map<String, Int>,
    title: String,
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(
        Color(0xFF6200EE),
        Color(0xFF03DAC5),
        Color(0xFFFF5722),
        Color(0xFF4CAF50),
        Color(0xFFFFC107),
        Color(0xFF9C27B0),
        Color(0xFF00BCD4),
        Color(0xFFFF9800)
    )
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (categories.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay datos de categorías",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
                val modelProducer = remember { CartesianChartModelProducer() }
                
                // Preparar datos - tomar máximo 8 categorías
                val sortedCategories = categories.entries
                    .sortedByDescending { it.value }
                    .take(8)
                
                val labels = sortedCategories.map { it.key }
                
                LaunchedEffect(categories) {
                    modelProducer.runTransaction {
                        columnSeries { series(sortedCategories.map { it.value }) }
                    }
                }

                CartesianChartHost(
                    chart = rememberCartesianChart(
                        rememberColumnCartesianLayer(),
                        startAxis = rememberStartAxis(
                            label = rememberAxisLabelComponent(
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        ),
                        bottomAxis = rememberBottomAxis(
                            label = rememberAxisLabelComponent(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            valueFormatter = { value, _, _ ->
                                // Truncar nombres largos
                                labels.getOrNull(value.toInt())?.take(10) ?: ""
                            }
                        )
                    ),
                    modelProducer = modelProducer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                )

                // Leyenda con porcentajes
                Spacer(modifier = Modifier.height(16.dp))
                val total = categories.values.sum()
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sortedCategories.forEachIndexed { index, (category, count) ->
                        val percentage = (count.toFloat() / total * 100).toInt()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                ) {
                                    Surface(
                                        color = colors[index % colors.size],
                                        shape = RoundedCornerShape(2.dp),
                                        modifier = Modifier.size(12.dp)
                                    ) {}
                                }
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "$count ($percentage%)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VicoPieChart(
    data: Map<String, Int>,
    title: String,
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(
        Color(0xFF6200EE),
        Color(0xFF03DAC5),
        Color(0xFFFF5722),
        Color(0xFF4CAF50),
        Color(0xFFFFC107)
    )
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (data.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay datos disponibles",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
                val total = data.values.sum()
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    data.entries.take(5).forEachIndexed { index, (label, value) ->
                        val percentage = (value.toFloat() / total * 100).toInt()
                        
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = colors[index % colors.size],
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.size(16.dp)
                                    ) {}
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "$value ($percentage%)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors[index % colors.size]
                                )
                            }
                            
                            LinearProgressIndicator(
                                progress = { percentage / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                                color = colors[index % colors.size],
                                trackColor = colors[index % colors.size].copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VicoMultiLineChart(
    viewsData: List<Pair<String, Int>>,
    favoritesData: List<Pair<String, Int>>,
    title: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Leyenda
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LegendItem(
                    color = Color(0xFF2196F3),
                    label = "Vistas"
                )
                LegendItem(
                    color = Color(0xFFE91E63),
                    label = "Favoritos"
                )
            }

            if (viewsData.isEmpty() || viewsData.all { it.second == 0 }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay datos para mostrar",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
                val modelProducer = remember { CartesianChartModelProducer() }
                
                LaunchedEffect(viewsData, favoritesData) {
                    modelProducer.runTransaction {
                        lineSeries {
                            series(viewsData.map { it.second })
                            series(favoritesData.map { it.second })
                        }
                    }
                }

                CartesianChartHost(
                    chart = rememberCartesianChart(
                        rememberLineCartesianLayer(),
                        startAxis = rememberStartAxis(
                            label = rememberAxisLabelComponent(
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        ),
                        bottomAxis = rememberBottomAxis(
                            label = rememberAxisLabelComponent(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            valueFormatter = { value, _, _ ->
                                viewsData.getOrNull(value.toInt())?.first?.takeLast(5) ?: ""
                            }
                        )
                    ),
                    modelProducer = modelProducer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )
            }
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
        ) {
            Surface(
                color = color,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.size(12.dp)
            ) {}
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
    }
}
