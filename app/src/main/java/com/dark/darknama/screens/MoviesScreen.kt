package com.dark.darknama.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.dark.darknama.components.GenreFilterSection
import com.dark.darknama.components.NetworkLogoButton
import com.dark.darknama.data.model.Genre
import com.dark.darknama.data.model.Movie
import com.dark.darknama.ui.movies.MoviesViewModel
import com.dark.darknama.utils.DeviceUtils
import com.dark.darknama.utils.StorageUtils

@Composable
fun MoviesScreen(
    viewModel: MoviesViewModel = viewModel(),
    navController: NavController? = null
) {
    val movies = viewModel.movies
    val isLoading = viewModel.isLoading
    val isLoadingMore = viewModel.isLoadingMore
    val errorMessage = viewModel.errorMessage
    val genres = viewModel.genres
    val selectedGenreId = viewModel.selectedGenreId
    val selectedFilterType = viewModel.selectedFilterType
    
    LaunchedEffect(Unit) {
        if (movies.isEmpty()) {
            viewModel.loadMovies()
        }
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Top header with network logo (opens website popup)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "DarkNama",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            NetworkLogoButton()
        }
        
        // Genre filter section
        GenreFilterSection(
            genres = genres,
            selectedGenreId = selectedGenreId,
            selectedFilterType = selectedFilterType,
            onGenreSelected = { genreId -> viewModel.selectGenre(genreId) },
            onFilterTypeSelected = { filterType -> viewModel.selectFilterType(filterType) }
        )
        
        when {
            isLoading && movies.isEmpty() -> {
                // Show modern loading animation when initial movies are loading
                LoadingScreen()
            }
            errorMessage != null && movies.isEmpty() -> {
                ErrorScreen(
                    errorMessage = errorMessage,
                    onRetry = { viewModel.retry() }
                )
            }
            else -> {
                MovieGrid(
                    movies = movies,
                    isLoading = isLoading,
                    isLoadingMore = isLoadingMore,
                    errorMessage = errorMessage,
                    onRetry = { viewModel.retry() },
                    onRefresh = { viewModel.refresh() },
                    onLoadMore = { viewModel.loadMoreMovies() },
                    navController = navController
                )
            }
        }
    }
}

@Composable
fun LoadingScreen() {
    val shimmerColor = MaterialTheme.colorScheme.surfaceVariant
    val shimmerColorShade = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
    
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Add a title while loading
        Text(
            text = "Loading Movies...",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(16.dp),
            fontWeight = FontWeight.Bold
        )
        
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(initialAlpha = 0.3f),
            exit = fadeOut()
        ) {
            val columns = DeviceUtils.getGridColumns(LocalContext.current.resources)
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(6) { // Show 6 loading placeholders
                    ShimmerMovieItem(shimmerColor, shimmerColorShade)
                }
            }
        }
    }
}

@Composable
fun ShimmerMovieItem(
    shimmerColor: Color,
    shimmerColorShade: Color
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = -1000f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200)
        ), label = "shimmer_translate"
    )
    
    val brush = Brush.linearGradient(
        colors = listOf(
            shimmerColor,
            shimmerColorShade,
            shimmerColor,
            shimmerColorShade,
            shimmerColor
        ),
        start = Offset.Zero,
        end = Offset(x = translateAnim, y = translateAnim)
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Movie poster shimmer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(brush)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Title shimmer
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(20.dp)
                    .background(brush)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Year shimmer
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(16.dp)
                    .background(brush)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Genres shimmer
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(16.dp)
                    .background(brush)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Rating shimmer
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(brush)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(16.dp)
                        .background(brush)
                )
            }
        }
    }
}

@Composable
fun MovieGrid(
    movies: List<Movie>,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    navController: NavController? = null
) {
    val moviesList = movies.toList()
    val context = LocalContext.current
    
    val columns = DeviceUtils.getGridColumns(LocalContext.current.resources)
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(moviesList) { index, movie ->
            MovieItem(
                movie = movie,
                onClick = {
                    // Save movie to storage
                    StorageUtils.saveMovieToFile(context, movie)
                    // Navigate to single movie screen
                    navController?.navigate("single_movie/${movie.id}")
                }
            )
            
            // Load more when we're near the end of the list
            if (index >= moviesList.size - 3) {
                LaunchedEffect(Unit) {
                    onLoadMore()
                }
            }
        }
        
        if (isLoadingMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Modern animated loading indicator
                    ModernCircularProgressIndicator()
                }
            }
        }
        
        if (errorMessage != null) {
            item {
                ErrorItem(
                    errorMessage = errorMessage,
                    onRetry = onRetry
                )
            }
        }
        
        // Add a small spacer at the bottom to avoid excessive padding
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun ModernCircularProgressIndicator() {
    val transition = rememberInfiniteTransition(label = "progress")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = androidx.compose.animation.core.FastOutSlowInEasing
            )
        ), label = "progress_anim"
    )
    
    // Add rotation animation for a more dynamic effect
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2000,
                easing = androidx.compose.animation.core.LinearEasing
            )
        ), label = "rotation_anim"
    )
    
    CircularProgressIndicator(
        progress = progress,
        modifier = Modifier
            .size(48.dp)
            .rotate(rotation), // Add rotation
        strokeWidth = 4.dp,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun MovieItem(
    movie: Movie,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(310.dp) // Fixed height for all cards
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Movie poster with rating overlay
            Box {
                Image(
                    painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(LocalContext.current)
                            .data(movie.image)
                            .crossfade(true)
                            .build()
                    ),
                    contentDescription = movie.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                
                // Rating overlay at top-right corner
                Card(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    shape = RoundedCornerShape(50.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Black.copy(alpha = 0.7f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = Color.Yellow,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format("%.1f", movie.imdb),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Movie details with weight to fill remaining space
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = movie.year.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Genres
                if (movie.genres.isNotEmpty()) {
                    Text(
                        text = movie.genres.joinToString(", ") { it.title },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun ErrorScreen(
    errorMessage: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        Text(
            text = "Failed to load movies",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Button(
            onClick = onRetry,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Retry",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Retry")
        }
    }
}

@Composable
fun ErrorItem(
    errorMessage: String,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Failed to load more movies",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Button(
                onClick = onRetry,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onErrorContainer,
                    contentColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Retry",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retry")
            }
        }
    }
}