package com.example.rp.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.rp.data.*
import kotlinx.coroutines.launch
import java.io.File

/**
 * ============================================================================
 * ARCHIVO: RecetasApp.kt
 * ============================================================================
 * Contiene toda la interfaz gráfica (UI) construida con Jetpack Compose:
 * - Todos los textos y símbolos fuera de la pestaña de Configuración se leen dinámicamente
 *   desde `uiConfig.text(UiTextKey.XXX)` para que el cliente pueda modificarlos en Opciones Avanzadas.
 * - Los textos e íconos de la pestaña de Configuración (`⚙️ Configuración`) son fijos y no modificables.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecetasApp() {
    val context = LocalContext.current

    val db = remember {
        AppDatabase(context).also { it.seedInitialDataIfEmpty() }
    }

    var pantryItems by remember { mutableStateOf(db.getAllPantryItems()) }
    var recipes by remember { mutableStateOf(db.getAllRecipes()) }
    var geminiApiKey by remember { mutableStateOf(db.getGeminiApiKey()) }
    var uiConfig by remember { mutableStateOf(db.getUiConfig()) }

    fun refreshData() {
        pantryItems = db.getAllPantryItems()
        recipes = db.getAllRecipes()
    }

    // `SETTINGS` (Configuración) siempre va al final de la lista para quedar fija en la esquina inferior derecha
    val activeSections = remember(uiConfig.enabledSectionIds) {
        val nonSettings = AppSection.entries.filter {
            it != AppSection.SETTINGS && uiConfig.enabledSectionIds.contains(it.id)
        }
        nonSettings + AppSection.SETTINGS
    }

    var selectedSection by remember { mutableStateOf(activeSections.first()) }

    LaunchedEffect(activeSections) {
        if (!activeSections.contains(selectedSection)) {
            selectedSection = AppSection.SETTINGS
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (selectedSection) {
                            AppSection.RECIPES -> uiConfig.text(UiTextKey.RECIPES_TOP_TITLE)
                            AppSection.PANTRY -> uiConfig.text(UiTextKey.PANTRY_TOP_TITLE)
                            // El título de Configuración es fijo y no se puede modificar
                            AppSection.SETTINGS -> AppSection.SETTINGS.defaultTopTitle
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            MultiRowBottomBar(
                activeSections = activeSections,
                selectedSection = selectedSection,
                uiConfig = uiConfig,
                onSelectSection = { selectedSection = it }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedSection) {
                AppSection.RECIPES -> RecipesScreen(
                    recipes = recipes,
                    pantryItems = pantryItems,
                    uiConfig = uiConfig,
                    onSaveRecipe = { recipe ->
                        db.saveRecipe(recipe)
                        refreshData()
                    },
                    onDeleteRecipe = { id ->
                        db.deleteRecipe(id)
                        refreshData()
                    }
                )
                AppSection.PANTRY -> PantryScreen(
                    pantryItems = pantryItems,
                    geminiApiKey = geminiApiKey,
                    uiConfig = uiConfig,
                    onSaveItem = { item ->
                        db.savePantryItem(item)
                        refreshData()
                    },
                    onDeleteItem = { id ->
                        db.deletePantryItem(id)
                        refreshData()
                    }
                )
                AppSection.SETTINGS -> SettingsScreen(
                    currentApiKey = geminiApiKey,
                    uiConfig = uiConfig,
                    onSaveApiKey = { newKey ->
                        db.setGeminiApiKey(newKey)
                        geminiApiKey = newKey.trim()
                    },
                    onUpdateUiConfig = { newConfig ->
                        db.saveUiConfig(newConfig)
                        uiConfig = db.getUiConfig()
                    }
                )
            }
        }
    }
}

/**
 * Barra de navegación inferior multi-fila donde Configuración (`⚙️ Configuración`)
 * tiene ícono y texto fijos y siempre queda en la esquina inferior derecha.
 */
@Composable
fun MultiRowBottomBar(
    activeSections: List<AppSection>,
    selectedSection: AppSection,
    uiConfig: AppUiConfig,
    onSelectSection: (AppSection) -> Unit
) {
    val maxPerRow = uiConfig.maxItemsPerBottomRow.coerceIn(2, 6)
    val rows = remember(activeSections, maxPerRow) {
        activeSections.chunked(maxPerRow)
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 4.dp)
        ) {
            rows.forEachIndexed { rowIndex, rowSections ->
                val isLastRow = rowIndex == rows.lastIndex
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (rows.size > 1 && isLastRow && rowSections.size < maxPerRow) {
                        val emptySlots = maxPerRow - rowSections.size
                        repeat(emptySlots) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }

                    rowSections.forEach { section ->
                        val isSelected = section == selectedSection
                        val iconText = when (section) {
                            AppSection.RECIPES -> uiConfig.text(UiTextKey.RECIPES_TAB_ICON)
                            AppSection.PANTRY -> uiConfig.text(UiTextKey.PANTRY_TAB_ICON)
                            // Configuración siempre mantiene su ícono fijo
                            AppSection.SETTINGS -> AppSection.SETTINGS.defaultTabIcon
                        }
                        val labelText = when (section) {
                            AppSection.RECIPES -> uiConfig.text(UiTextKey.RECIPES_TAB_LABEL)
                            AppSection.PANTRY -> uiConfig.text(UiTextKey.PANTRY_TAB_LABEL)
                            // Configuración siempre mantiene su texto fijo
                            AppSection.SETTINGS -> AppSection.SETTINGS.defaultTabLabel
                        }

                        val bgColor = if (isSelected) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        }
                        val contentColor = if (isSelected) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(bgColor)
                                .clickable { onSelectSection(section) }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = iconText, fontSize = 18.sp)
                            Text(
                                text = labelText,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = contentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// APARTADO 1: RECETAS, PORCENTAJE PANADERO, PIEZAS Y ABSORCIÓN DE PESO
// =========================================================================

@Composable
fun RecipesScreen(
    recipes: List<Recipe>,
    pantryItems: List<PantryItem>,
    uiConfig: AppUiConfig,
    onSaveRecipe: (Recipe) -> Unit,
    onDeleteRecipe: (Long) -> Unit
) {
    val pantryMap = remember(pantryItems) { pantryItems.associateBy { it.id } }
    val recipeMap = remember(recipes) { recipes.associateBy { it.id } }

    var editingRecipe by remember { mutableStateOf<Recipe?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    if (showEditor) {
        RecipeEditorDialog(
            initialRecipe = editingRecipe,
            allRecipes = recipes,
            pantryItems = pantryItems,
            uiConfig = uiConfig,
            onDismiss = { showEditor = false },
            onConfirm = { saved ->
                onSaveRecipe(saved)
                showEditor = false
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingRecipe = null
                    showEditor = true
                },
                icon = { Text("➕", fontSize = 18.sp) },
                text = { Text(uiConfig.text(UiTextKey.RECIPES_FAB_NEW)) }
            )
        }
    ) { padding ->
        if (recipes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    uiConfig.text(UiTextKey.RECIPES_EMPTY_MSG),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(recipes, key = { it.id }) { recipe ->
                    RecipeCard(
                        recipe = recipe,
                        pantryMap = pantryMap,
                        recipeMap = recipeMap,
                        uiConfig = uiConfig,
                        onEdit = {
                            editingRecipe = recipe
                            showEditor = true
                        },
                        onDelete = { onDeleteRecipe(recipe.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun RecipeCard(
    recipe: Recipe,
    pantryMap: Map<Long, PantryItem>,
    recipeMap: Map<Long, Recipe>,
    uiConfig: AppUiConfig,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    // Oculto por defecto como configuraste en la línea 360
    var showRecipeBreakdown by remember { mutableStateOf(false) }
    val expandedSubRecipes = remember { mutableStateMapOf<Int, Boolean>() }
    var scaleByPieces by remember(recipe.id) { mutableStateOf(false) }

    var customWeightText by remember(recipe.id, recipe.defaultTargetWeightGrams) {
        mutableStateOf(RecipeCalculator.formatNumber(recipe.defaultTargetWeightGrams))
    }
    var customGramsPerPieceText by remember(recipe.id, recipe.gramsPerPiece) {
        mutableStateOf(RecipeCalculator.formatNumber(recipe.gramsPerPiece))
    }
    val initialPieces = remember(recipe.id, recipe.defaultTargetWeightGrams, recipe.gramsPerPiece) {
        val pzW = recipe.gramsPerPiece.coerceAtLeast(1.0)
        (recipe.defaultTargetWeightGrams / pzW).coerceAtLeast(1.0)
    }
    var customPieceCountText by remember(recipe.id) {
        mutableStateOf(RecipeCalculator.formatNumber(initialPieces))
    }

    var showNutritionTable by remember { mutableStateOf(false) }

    val activeGramsPerPiece = customGramsPerPieceText.toDoubleOrNull()?.coerceAtLeast(1.0)
        ?: recipe.gramsPerPiece.coerceAtLeast(1.0)
    val activePieceCount = customPieceCountText.toDoubleOrNull()?.coerceAtLeast(0.1) ?: 1.0

    val targetTotalGrams = if (scaleByPieces) {
        activePieceCount * activeGramsPerPiece
    } else {
        customWeightText.toDoubleOrNull()?.coerceAtLeast(1.0) ?: recipe.defaultTargetWeightGrams
    }

    val summary = remember(
        recipe,
        targetTotalGrams,
        activeGramsPerPiece,
        pantryMap,
        recipeMap,
        uiConfig.subRecipeSymbol,
        uiConfig.text(UiTextKey.SYMBOL_SCALE)
    ) {
        RecipeCalculator.calculateRecipe(
            recipe = recipe.copy(gramsPerPiece = activeGramsPerPiece),
            overrideTargetWeightGrams = if (recipe.isBakersPercentage) targetTotalGrams else null,
            pantryMap = pantryMap,
            recipeMap = recipeMap,
            subRecipeSymbol = uiConfig.subRecipeSymbol,
            scaleSymbol = uiConfig.text(UiTextKey.SYMBOL_SCALE)
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = recipe.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(6.dp))
                        if (summary.anyHasPrice) Text(uiConfig.priceSymbol, fontSize = 16.sp)
                        if (summary.anyHasNutrition) Text(uiConfig.nutritionSymbol, fontSize = 16.sp)
                    }
                    Text(
                        text = if (recipe.isBakersPercentage) {
                            "${uiConfig.bakerySymbol} ${uiConfig.text(UiTextKey.RECIPE_SUBTITLE_BAKERY)} | 1 pz = ${RecipeCalculator.formatNumber(activeGramsPerPiece)}g"
                        } else {
                            "${uiConfig.dishSymbol} ${uiConfig.text(UiTextKey.RECIPE_SUBTITLE_STANDARD)}"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row {
                    TextButton(onClick = onEdit) { Text(uiConfig.text(UiTextKey.BTN_EDIT)) }
                    TextButton(onClick = onDelete) {
                        Text(uiConfig.text(UiTextKey.BTN_DELETE), color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            if (recipe.notes.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = recipe.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (recipe.isBakersPercentage) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "${uiConfig.text(UiTextKey.SYMBOL_SCALE)} ${uiConfig.text(UiTextKey.RECIPE_SCALE_TITLE)}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = !scaleByPieces,
                                onClick = {
                                    scaleByPieces = false
                                    customWeightText = RecipeCalculator.formatNumber(targetTotalGrams)
                                },
                                label = { Text(uiConfig.text(UiTextKey.RECIPE_SCALE_BY_GRAMS_CHIP)) }
                            )
                            FilterChip(
                                selected = scaleByPieces,
                                onClick = {
                                    scaleByPieces = true
                                    val pz = targetTotalGrams / activeGramsPerPiece
                                    customPieceCountText = RecipeCalculator.formatNumber(pz)
                                },
                                label = {
                                    Text("${uiConfig.text(UiTextKey.SYMBOL_PIECE)} ${uiConfig.text(UiTextKey.RECIPE_SCALE_BY_PIECES_CHIP)}")
                                }
                            )
                        }

                        Spacer(Modifier.height(8.dp))
                        if (!scaleByPieces) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = customWeightText,
                                    onValueChange = { customWeightText = it },
                                    label = { Text(uiConfig.text(UiTextKey.RECIPE_MIX_GRAMS_LABEL)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                AssistChip(
                                    onClick = { customWeightText = "1000" },
                                    label = { Text("1000g") }
                                )
                                AssistChip(
                                    onClick = { customWeightText = "2000" },
                                    label = { Text("2000g") }
                                )
                            }
                            Text(
                                text = "Equivale a ~${RecipeCalculator.formatNumber(summary.totalPieces)} pz de ${RecipeCalculator.formatNumber(activeGramsPerPiece)}g",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = customPieceCountText,
                                    onValueChange = { customPieceCountText = it },
                                    label = { Text(uiConfig.text(UiTextKey.RECIPE_HOW_MANY_PIECES_LABEL)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = customGramsPerPieceText,
                                    onValueChange = { customGramsPerPieceText = it },
                                    label = { Text(uiConfig.text(UiTextKey.RECIPE_GRAMS_PER_PIECE_LABEL)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                AssistChip(
                                    onClick = { customPieceCountText = "6" },
                                    label = { Text("6 pz") }
                                )
                                AssistChip(
                                    onClick = { customPieceCountText = "12" },
                                    label = { Text("12 pz") }
                                )
                                AssistChip(
                                    onClick = { customPieceCountText = "20" },
                                    label = { Text("20 pz") }
                                )
                            }
                            Text(
                                text = "${uiConfig.text(UiTextKey.RECIPE_TOTAL_MIX_PREFIX)} ${RecipeCalculator.formatNumber(targetTotalGrams)} g (${RecipeCalculator.formatNumber(activePieceCount)} pz × ${RecipeCalculator.formatNumber(activeGramsPerPiece)}g)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${uiConfig.text(UiTextKey.RECIPE_INGREDIENTS_HEADER)} (${summary.rows.size}):",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                FilterChip(
                    selected = showRecipeBreakdown,
                    onClick = { showRecipeBreakdown = !showRecipeBreakdown },
                    label = {
                        Text(
                            if (showRecipeBreakdown) uiConfig.text(UiTextKey.RECIPE_COLLAPSE_BREAKDOWN)
                            else uiConfig.text(UiTextKey.RECIPE_EXPAND_BREAKDOWN)
                        )
                    }
                )
            }

            if (showRecipeBreakdown) {
                Spacer(Modifier.height(4.dp))
                summary.rows.forEachIndexed { index, row ->
                    val isSubExpanded = expandedSubRecipes[index] ?: false
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "• ${row.displayName}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (row.component.isBaseFlourInBakersMode || row.isSubRecipe) {
                                            FontWeight.Bold
                                        } else {
                                            FontWeight.Normal
                                        }
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    if (row.hasPriceIcon) Text(uiConfig.priceSymbol, fontSize = 14.sp)
                                    if (row.hasNutritionIcon) {
                                        Spacer(Modifier.width(2.dp))
                                        Text(uiConfig.nutritionSymbol, fontSize = 14.sp)
                                    }
                                }
                                if (row.storeName.isNotBlank()) {
                                    Text(
                                        text = "${uiConfig.storeSymbol} ${uiConfig.text(UiTextKey.PANTRY_STORE_PREFIX)} ${row.storeName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                if (row.retentionNoteText.isNotBlank()) {
                                    Text(
                                        text = row.retentionNoteText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                if (row.isSubRecipe && row.subBreakdownRows.isNotEmpty()) {
                                    TextButton(
                                        onClick = { expandedSubRecipes[index] = !isSubExpanded },
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                    ) {
                                        Text(
                                            text = if (isSubExpanded) {
                                                uiConfig.text(UiTextKey.RECIPE_SUB_COLLAPSE)
                                            } else {
                                                "${uiConfig.text(UiTextKey.RECIPE_SUB_EXPAND)} (${row.subBreakdownRows.size})"
                                            },
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = row.displayAmountText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (row.hasPriceIcon) {
                                    Text(
                                        text = "$${RecipeCalculator.formatNumber(row.calculatedCost)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        if (row.isSubRecipe && isSubExpanded && row.subBreakdownRows.isNotEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, top = 4.dp, bottom = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = "${uiConfig.text(UiTextKey.RECIPE_SUB_PROPORTIONAL_HEADER)} ${RecipeCalculator.formatNumber(row.exactGramsOrMl)}g de ${row.displayName}:",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    row.subBreakdownRows.forEach { subRow ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "↳ ${subRow.displayName}",
                                                    fontSize = 12.sp
                                                )
                                                if (subRow.hasPriceIcon) Text(" ${uiConfig.priceSymbol}", fontSize = 11.sp)
                                                if (subRow.hasNutritionIcon) Text(" ${uiConfig.nutritionSymbol}", fontSize = 11.sp)
                                                if (subRow.storeName.isNotBlank()) {
                                                    Text(
                                                        text = " (${uiConfig.storeSymbol} ${subRow.storeName})",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.secondary
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "${subRow.displayAmountText} ($${RecipeCalculator.formatNumber(subRow.calculatedCost)})",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${uiConfig.text(UiTextKey.RECIPE_FINAL_WEIGHT_LABEL)} ${RecipeCalculator.formatNumber(summary.totalWeightGramsOrMl)} g",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (recipe.isBakersPercentage && summary.totalPieces > 0) {
                        Text(
                            text = "${uiConfig.text(UiTextKey.RECIPE_PIECES_SUMMARY_LABEL)} ${RecipeCalculator.formatNumber(summary.totalPieces)} pz ($${RecipeCalculator.formatNumber(summary.costPerPiece)} / pz)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                Text(
                    text = "${uiConfig.priceSymbol} ${uiConfig.text(UiTextKey.RECIPE_APPROX_COST_LABEL)} $${RecipeCalculator.formatNumber(summary.totalCost)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showNutritionTable = !showNutritionTable },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (showNutritionTable) "${uiConfig.text(UiTextKey.RECIPE_HIDE_NUTRITION_BTN)} ${uiConfig.nutritionSymbol}"
                    else "${uiConfig.text(UiTextKey.RECIPE_SHOW_NUTRITION_BTN)} ${uiConfig.nutritionSymbol}"
                )
            }

            if (showNutritionTable) {
                Spacer(Modifier.height(8.dp))
                NutritionTableView(
                    totalNutrition = summary.totalNutrition,
                    per100gNutrition = summary.nutritionPer100g,
                    totalWeightGrams = summary.totalWeightGramsOrMl,
                    uiConfig = uiConfig
                )
            }
        }
    }
}

@Composable
fun NutritionTableView(
    totalNutrition: NutritionInfo,
    per100gNutrition: NutritionInfo,
    totalWeightGrams: Double,
    uiConfig: AppUiConfig
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "${uiConfig.nutritionSymbol} ${uiConfig.text(UiTextKey.NUTRITION_TABLE_TITLE)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = uiConfig.text(UiTextKey.NUTRITION_TABLE_SUBTITLE),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Text(uiConfig.text(UiTextKey.NUTRITION_COL_NUTRIENT), modifier = Modifier.weight(1.4f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(uiConfig.text(UiTextKey.NUTRITION_COL_PER100G), modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(
                    "${uiConfig.text(UiTextKey.NUTRITION_COL_TOTAL)} (${RecipeCalculator.formatNumber(totalWeightGrams)}g)",
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            val orderedRows = buildList {
                if (uiConfig.showCalories) {
                    add(Triple("Energía (Calorías)", "${RecipeCalculator.formatNumber(per100gNutrition.caloriesKcal)} kcal", "${RecipeCalculator.formatNumber(totalNutrition.caloriesKcal)} kcal"))
                }
                if (uiConfig.showProtein) {
                    add(Triple("Proteínas", "${RecipeCalculator.formatNumber(per100gNutrition.proteinG)} g", "${RecipeCalculator.formatNumber(totalNutrition.proteinG)} g"))
                }
                if (uiConfig.showTotalFat) {
                    add(Triple("Grasas / Lípidos Totales", "${RecipeCalculator.formatNumber(per100gNutrition.totalFatG)} g", "${RecipeCalculator.formatNumber(totalNutrition.totalFatG)} g"))
                }
                if (uiConfig.showSaturatedFat) {
                    add(Triple("   • Grasas Saturadas", "${RecipeCalculator.formatNumber(per100gNutrition.saturatedFatG)} g", "${RecipeCalculator.formatNumber(totalNutrition.saturatedFatG)} g"))
                }
                if (uiConfig.showCarbs) {
                    add(Triple("Carbohidratos", "${RecipeCalculator.formatNumber(per100gNutrition.carbsG)} g", "${RecipeCalculator.formatNumber(totalNutrition.carbsG)} g"))
                }
                if (uiConfig.showSugars) {
                    add(Triple("   • Azúcares", "${RecipeCalculator.formatNumber(per100gNutrition.sugarsG)} g", "${RecipeCalculator.formatNumber(totalNutrition.sugarsG)} g"))
                }
                if (uiConfig.showFiber) {
                    add(Triple("Fibra Dietética", "${RecipeCalculator.formatNumber(per100gNutrition.fiberG)} g", "${RecipeCalculator.formatNumber(totalNutrition.fiberG)} g"))
                }
                if (uiConfig.showSodium) {
                    add(Triple("Sodio", "${RecipeCalculator.formatNumber(per100gNutrition.sodiumMg)} mg", "${RecipeCalculator.formatNumber(totalNutrition.sodiumMg)} mg"))
                }
            }

            if (orderedRows.isEmpty()) {
                Text(
                    "Todos los renglones de la tabla nutrimental están desactivados en Configuración.",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                orderedRows.forEach { (label, per100, total) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Text(label, modifier = Modifier.weight(1.4f), fontSize = 12.sp)
                        Text(per100, modifier = Modifier.weight(1f), fontSize = 12.sp)
                        Text(total, modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeEditorDialog(
    initialRecipe: Recipe?,
    allRecipes: List<Recipe>,
    pantryItems: List<PantryItem>,
    uiConfig: AppUiConfig,
    onDismiss: () -> Unit,
    onConfirm: (Recipe) -> Unit
) {
    var name by remember { mutableStateOf(initialRecipe?.name ?: "") }
    var notes by remember { mutableStateOf(initialRecipe?.notes ?: "") }
    var isBakersMode by remember { mutableStateOf(initialRecipe?.isBakersPercentage ?: false) }
    var defaultTargetWeight by remember {
        mutableStateOf(RecipeCalculator.formatNumber(initialRecipe?.defaultTargetWeightGrams ?: 1000.0))
    }
    var gramsPerPiece by remember {
        mutableStateOf(RecipeCalculator.formatNumber(initialRecipe?.gramsPerPiece ?: 100.0))
    }
    var components by remember { mutableStateOf(initialRecipe?.components ?: emptyList()) }

    var addingSourceType by remember { mutableStateOf(ComponentSourceType.PANTRY_INGREDIENT) }
    var selectedPantryId by remember { mutableLongStateOf(pantryItems.firstOrNull()?.id ?: 0L) }
    val availableSubRecipes = remember(allRecipes, initialRecipe) {
        allRecipes.filter { it.id != (initialRecipe?.id ?: -1L) }
    }
    var selectedSubRecipeId by remember {
        mutableLongStateOf(availableSubRecipes.firstOrNull()?.id ?: 0L)
    }
    var amountInput by remember { mutableStateOf("100") }
    var selectedUnit by remember { mutableStateOf(MeasureUnit.GRAMS) }
    var isBaseFlour by remember { mutableStateOf(false) }
    var weightRetentionInput by remember { mutableStateOf("100") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initialRecipe == null) uiConfig.text(UiTextKey.RECIPE_DIALOG_NEW_TITLE)
                else uiConfig.text(UiTextKey.RECIPE_DIALOG_EDIT_TITLE)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(uiConfig.text(UiTextKey.RECIPE_FIELD_NAME)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(uiConfig.text(UiTextKey.RECIPE_FIELD_NOTES)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${uiConfig.bakerySymbol} ${uiConfig.text(UiTextKey.RECIPE_BAKERS_MODE_TITLE)}",
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    uiConfig.text(UiTextKey.RECIPE_BAKERS_MODE_DESC),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Switch(
                                checked = isBakersMode,
                                onCheckedChange = { isBakersMode = it }
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (isBakersMode) {
                                OutlinedTextField(
                                    value = defaultTargetWeight,
                                    onValueChange = { defaultTargetWeight = it },
                                    label = { Text(uiConfig.text(UiTextKey.RECIPE_BASE_MIX_FIELD)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            OutlinedTextField(
                                value = gramsPerPiece,
                                onValueChange = { gramsPerPiece = it },
                                label = {
                                    Text("${uiConfig.text(UiTextKey.SYMBOL_PIECE)} ${uiConfig.text(UiTextKey.RECIPE_GRAMS_PER_PIECE_LABEL)}")
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                HorizontalDivider()
                Text(uiConfig.text(UiTextKey.RECIPE_ADD_COMPONENT_HEADER), fontWeight = FontWeight.Bold)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = addingSourceType == ComponentSourceType.PANTRY_INGREDIENT,
                        onClick = { addingSourceType = ComponentSourceType.PANTRY_INGREDIENT },
                        label = {
                            Text("${uiConfig.text(UiTextKey.PANTRY_TAB_ICON)} ${uiConfig.text(UiTextKey.RECIPE_FROM_PANTRY_CHIP)}")
                        }
                    )
                    FilterChip(
                        selected = addingSourceType == ComponentSourceType.SUB_RECIPE,
                        onClick = {
                            addingSourceType = ComponentSourceType.SUB_RECIPE
                            if (selectedSubRecipeId == 0L && availableSubRecipes.isNotEmpty()) {
                                selectedSubRecipeId = availableSubRecipes.first().id
                            }
                        },
                        label = {
                            Text("${uiConfig.subRecipeSymbol} ${uiConfig.text(UiTextKey.RECIPE_FROM_SUBRECIPE_CHIP)}")
                        }
                    )
                }

                if (addingSourceType == ComponentSourceType.PANTRY_INGREDIENT) {
                    if (pantryItems.isEmpty()) {
                        Text(
                            "Primero agrega ingredientes en la pestaña Despensa.",
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text(uiConfig.text(UiTextKey.RECIPE_SELECT_INGREDIENT_LABEL), style = MaterialTheme.typography.labelMedium)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            pantryItems.forEach { item ->
                                val icons = buildString {
                                    if (item.hasPrice) append(uiConfig.priceSymbol)
                                    if (item.hasNutrition) append(uiConfig.nutritionSymbol)
                                }
                                FilterChip(
                                    selected = selectedPantryId == item.id,
                                    onClick = {
                                        selectedPantryId = item.id
                                        selectedUnit = item.defaultUnit
                                    },
                                    label = { Text("${item.name} $icons") }
                                )
                            }
                        }
                    }
                } else {
                    if (availableSubRecipes.isEmpty()) {
                        Text(
                            "Aún no hay otras recetas guardadas para combinar.",
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text(uiConfig.text(UiTextKey.RECIPE_SELECT_SUBRECIPE_LABEL), style = MaterialTheme.typography.labelMedium)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            availableSubRecipes.forEach { sub ->
                                FilterChip(
                                    selected = selectedSubRecipeId == sub.id,
                                    onClick = { selectedSubRecipeId = sub.id },
                                    label = {
                                        Text("${uiConfig.subRecipeSymbol} ${sub.name} (1 pz=${RecipeCalculator.formatNumber(sub.gramsPerPiece)}g)")
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = {
                        Text(
                            if (isBakersMode) uiConfig.text(UiTextKey.RECIPE_PERCENTAGE_FIELD_LABEL)
                            else "${uiConfig.text(UiTextKey.RECIPE_AMOUNT_FIELD_LABEL)} (${selectedUnit.label})"
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (!isBakersMode) {
                    val unitsToShow = if (addingSourceType == ComponentSourceType.SUB_RECIPE) {
                        listOf(MeasureUnit.GRAMS, MeasureUnit.PIECES)
                    } else {
                        MeasureUnit.entries
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        unitsToShow.forEach { u ->
                            FilterChip(
                                selected = selectedUnit == u,
                                onClick = { selectedUnit = u },
                                label = { Text(u.label) }
                            )
                        }
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "${uiConfig.text(UiTextKey.SYMBOL_RETENTION)} ${uiConfig.text(UiTextKey.RECIPE_RETENTION_TITLE)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = uiConfig.text(UiTextKey.RECIPE_RETENTION_DESC),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = weightRetentionInput,
                            onValueChange = { weightRetentionInput = it },
                            label = { Text(uiConfig.text(UiTextKey.RECIPE_RETENTION_FIELD_LABEL)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AssistChip(
                                onClick = { weightRetentionInput = "100" },
                                label = { Text("100%") }
                            )
                            AssistChip(
                                onClick = { weightRetentionInput = "50" },
                                label = { Text("50%") }
                            )
                            AssistChip(
                                onClick = { weightRetentionInput = "10" },
                                label = { Text("10%") }
                            )
                            AssistChip(
                                onClick = { weightRetentionInput = "0" },
                                label = { Text("0%") }
                            )
                        }
                    }
                }

                if (isBakersMode && addingSourceType == ComponentSourceType.PANTRY_INGREDIENT) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isBaseFlour,
                            onCheckedChange = {
                                isBaseFlour = it
                                if (it) amountInput = "100"
                            }
                        )
                        Text(uiConfig.text(UiTextKey.RECIPE_BASE_FLOUR_CHECKBOX), style = MaterialTheme.typography.bodySmall)
                    }
                }

                Button(
                    onClick = {
                        val amt = amountInput.toDoubleOrNull() ?: return@Button
                        if (amt <= 0.0) return@Button
                        val retPct = weightRetentionInput.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 100.0

                        if (addingSourceType == ComponentSourceType.PANTRY_INGREDIENT && selectedPantryId != 0L) {
                            components = components + RecipeComponent(
                                sourceType = ComponentSourceType.PANTRY_INGREDIENT,
                                referenceId = selectedPantryId,
                                amountOrPercentage = amt,
                                unit = if (isBakersMode) MeasureUnit.GRAMS else selectedUnit,
                                isBaseFlourInBakersMode = isBakersMode && isBaseFlour,
                                weightRetentionPercent = retPct
                            )
                            isBaseFlour = false
                            weightRetentionInput = "100"
                        } else if (addingSourceType == ComponentSourceType.SUB_RECIPE && selectedSubRecipeId != 0L) {
                            val subUnit = if (isBakersMode) MeasureUnit.GRAMS else selectedUnit
                            components = components + RecipeComponent(
                                sourceType = ComponentSourceType.SUB_RECIPE,
                                referenceId = selectedSubRecipeId,
                                amountOrPercentage = amt,
                                unit = subUnit,
                                weightRetentionPercent = retPct
                            )
                            weightRetentionInput = "100"
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(uiConfig.text(UiTextKey.RECIPE_ADD_TO_LIST_BTN))
                }

                HorizontalDivider()
                Text(
                    "${uiConfig.text(UiTextKey.RECIPE_CURRENT_COMPONENTS_HEADER)} (${components.size}):",
                    fontWeight = FontWeight.Bold
                )

                components.forEachIndexed { idx, comp ->
                    val label = if (comp.sourceType == ComponentSourceType.PANTRY_INGREDIENT) {
                        val item = pantryItems.find { it.id == comp.referenceId }
                        val icons = buildString {
                            if (item?.hasPrice == true) append(uiConfig.priceSymbol)
                            if (item?.hasNutrition == true) append(uiConfig.nutritionSymbol)
                        }
                        "${item?.name ?: "Ingrediente"} $icons"
                    } else {
                        val sub = allRecipes.find { it.id == comp.referenceId }
                        "${uiConfig.subRecipeSymbol} ${sub?.name ?: "Receta"}"
                    }
                    val unitTxt = if (isBakersMode) "%" else comp.unit.symbol
                    val retTxt = if (comp.weightRetentionPercent != 100.0) {
                        " [${RecipeCalculator.formatNumber(comp.weightRetentionPercent)}%]"
                    } else ""
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "• $label: ${RecipeCalculator.formatNumber(comp.amountOrPercentage)} $unitTxt$retTxt",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall
                        )
                        TextButton(
                            onClick = {
                                components = components.toMutableList().also { it.removeAt(idx) }
                            }
                        ) {
                            Text(uiConfig.text(UiTextKey.BTN_REMOVE), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    onConfirm(
                        Recipe(
                            id = initialRecipe?.id ?: 0L,
                            name = name.trim(),
                            notes = notes.trim(),
                            isBakersPercentage = isBakersMode,
                            defaultTargetWeightGrams = defaultTargetWeight.toDoubleOrNull() ?: 1000.0,
                            gramsPerPiece = gramsPerPiece.toDoubleOrNull()?.coerceAtLeast(1.0) ?: 100.0,
                            components = components
                        )
                    )
                }
            ) {
                Text(uiConfig.text(UiTextKey.BTN_SAVE))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(uiConfig.text(UiTextKey.BTN_CANCEL)) }
        }
    )
}

// =========================================================================
// APARTADO 2: DESPENSA
// =========================================================================

@Composable
fun PantryScreen(
    pantryItems: List<PantryItem>,
    geminiApiKey: String,
    uiConfig: AppUiConfig,
    onSaveItem: (PantryItem) -> Unit,
    onDeleteItem: (Long) -> Unit
) {
    var editingItem by remember { mutableStateOf<PantryItem?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var showCatalogDialog by remember { mutableStateOf(false) }
    var selectedStoreFilter by remember { mutableStateOf<String?>(null) }

    val availableStores = remember(pantryItems) {
        pantryItems.map { it.storeName.trim() }.filter { it.isNotEmpty() }.distinct().sorted()
    }

    val filteredItems = remember(pantryItems, selectedStoreFilter) {
        if (selectedStoreFilter == null) pantryItems
        else pantryItems.filter { it.storeName.equals(selectedStoreFilter, ignoreCase = true) }
    }

    if (showEditor) {
        PantryItemEditorDialog(
            initialItem = editingItem,
            geminiApiKey = geminiApiKey,
            uiConfig = uiConfig,
            onDismiss = { showEditor = false },
            onConfirm = { saved ->
                onSaveItem(saved)
                showEditor = false
            }
        )
    }

    if (showCatalogDialog) {
        CatalogImportDialog(
            existingNames = pantryItems.map { it.name.lowercase() }.toSet(),
            uiConfig = uiConfig,
            onImportItem = { item -> onSaveItem(item) },
            onDismiss = { showCatalogDialog = false }
        )
    }

    Scaffold(
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SmallFloatingActionButton(
                    onClick = { showCatalogDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp)) {
                        Text("${uiConfig.text(UiTextKey.SYMBOL_CATALOG)} ${uiConfig.text(UiTextKey.PANTRY_FAB_CATALOG)}")
                    }
                }
                ExtendedFloatingActionButton(
                    onClick = {
                        editingItem = null
                        showEditor = true
                    },
                    icon = { Text("➕", fontSize = 18.sp) },
                    text = { Text(uiConfig.text(UiTextKey.PANTRY_FAB_NEW)) }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "${uiConfig.text(UiTextKey.PANTRY_LEGEND_PREFIX)} ${uiConfig.priceSymbol} = Precio | ${uiConfig.nutritionSymbol} = Nutrición | ${uiConfig.storeSymbol} = Súper",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (availableStores.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = selectedStoreFilter == null,
                            onClick = { selectedStoreFilter = null },
                            label = { Text("${uiConfig.text(UiTextKey.PANTRY_FILTER_ALL)} (${pantryItems.size})") }
                        )
                        availableStores.forEach { store ->
                            FilterChip(
                                selected = selectedStoreFilter == store,
                                onClick = {
                                    selectedStoreFilter = if (selectedStoreFilter == store) null else store
                                },
                                label = { Text("${uiConfig.storeSymbol} $store") }
                            )
                        }
                    }
                }
            }
            items(filteredItems, key = { it.id }) { item ->
                PantryItemCard(
                    item = item,
                    uiConfig = uiConfig,
                    onEdit = {
                        editingItem = item
                        showEditor = true
                    },
                    onDelete = { onDeleteItem(item.id) }
                )
            }
        }
    }
}

@Composable
fun PantryItemCard(
    item: PantryItem,
    uiConfig: AppUiConfig,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val bitmap = remember(item.photoPath) {
        item.photoPath?.let { path ->
            if (File(path).exists()) BitmapFactory.decodeFile(path) else null
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = item.name,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(6.dp))
                    if (item.hasPrice) Text(uiConfig.priceSymbol, fontSize = 16.sp)
                    if (item.hasNutrition) {
                        Spacer(Modifier.width(2.dp))
                        Text(uiConfig.nutritionSymbol, fontSize = 16.sp)
                    }
                }

                if (item.storeName.isNotBlank()) {
                    Text(
                        text = "${uiConfig.storeSymbol} ${uiConfig.text(UiTextKey.PANTRY_STORE_PREFIX)} ${item.storeName}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Solo se muestra la equivalencia de piezas si `defaultUnit == MeasureUnit.PIECES`
                val unitInfo = if (item.defaultUnit == MeasureUnit.PIECES) {
                    "${uiConfig.text(UiTextKey.PANTRY_UNIT_PIECES_PREFIX)} (1 pz ≈ ${RecipeCalculator.formatNumber(item.amountPerPiece)} ${item.pieceEquivalenceUnit.symbol})"
                } else {
                    "${uiConfig.text(UiTextKey.PANTRY_UNIT_BASE_PREFIX)} ${item.defaultUnit.label}"
                }
                Text(text = unitInfo, style = MaterialTheme.typography.bodySmall)

                if (item.hasPrice) {
                    Text(
                        text = "${uiConfig.priceSymbol} ${uiConfig.text(UiTextKey.PANTRY_COST_PREFIX)} $${RecipeCalculator.formatNumber(item.priceCost)} / ${RecipeCalculator.formatNumber(item.priceReferenceAmount)} ${item.priceReferenceUnit.symbol}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (item.hasNutrition) {
                    val n = item.nutritionPer100gOrMl
                    val baseSym = if (item.defaultUnit == MeasureUnit.MILLILITERS) "ml" else "g"
                    Text(
                        text = "${uiConfig.nutritionSymbol} 100$baseSym: ${RecipeCalculator.formatNumber(n.caloriesKcal)} kcal | Prot: ${RecipeCalculator.formatNumber(n.proteinG)}g | Carb: ${RecipeCalculator.formatNumber(n.carbsG)}g | Grasa: ${RecipeCalculator.formatNumber(n.totalFatG)}g",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Column {
                TextButton(onClick = onEdit) { Text(uiConfig.text(UiTextKey.BTN_EDIT)) }
                TextButton(onClick = onDelete) {
                    Text(uiConfig.text(UiTextKey.BTN_DELETE), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun PantryItemEditorDialog(
    initialItem: PantryItem?,
    geminiApiKey: String,
    uiConfig: AppUiConfig,
    onDismiss: () -> Unit,
    onConfirm: (PantryItem) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(initialItem?.name ?: "") }
    var storeName by remember { mutableStateOf(initialItem?.storeName ?: "") }
    var defaultUnit by remember { mutableStateOf(initialItem?.defaultUnit ?: MeasureUnit.GRAMS) }

    var amountPerPiece by remember {
        mutableStateOf(RecipeCalculator.formatNumber(initialItem?.amountPerPiece ?: 100.0))
    }
    var pieceEquivUnit by remember {
        mutableStateOf(initialItem?.pieceEquivalenceUnit ?: PieceEquivalenceUnit.GRAMS)
    }

    var hasPrice by remember { mutableStateOf(initialItem?.hasPrice ?: true) }
    var priceCost by remember {
        mutableStateOf(RecipeCalculator.formatNumber(initialItem?.priceCost ?: 0.0))
    }
    var priceRefAmount by remember {
        mutableStateOf(RecipeCalculator.formatNumber(initialItem?.priceReferenceAmount ?: 1000.0))
    }
    var priceRefUnit by remember {
        mutableStateOf(initialItem?.priceReferenceUnit ?: MeasureUnit.GRAMS)
    }

    var hasNutrition by remember { mutableStateOf(initialItem?.hasNutrition ?: false) }
    var calories by remember {
        mutableStateOf(RecipeCalculator.formatNumber(initialItem?.nutritionPer100gOrMl?.caloriesKcal ?: 0.0))
    }
    var protein by remember {
        mutableStateOf(RecipeCalculator.formatNumber(initialItem?.nutritionPer100gOrMl?.proteinG ?: 0.0))
    }
    var carbs by remember {
        mutableStateOf(RecipeCalculator.formatNumber(initialItem?.nutritionPer100gOrMl?.carbsG ?: 0.0))
    }
    var sugars by remember {
        mutableStateOf(RecipeCalculator.formatNumber(initialItem?.nutritionPer100gOrMl?.sugarsG ?: 0.0))
    }
    var totalFat by remember {
        mutableStateOf(RecipeCalculator.formatNumber(initialItem?.nutritionPer100gOrMl?.totalFatG ?: 0.0))
    }
    var satFat by remember {
        mutableStateOf(RecipeCalculator.formatNumber(initialItem?.nutritionPer100gOrMl?.saturatedFatG ?: 0.0))
    }
    var fiber by remember {
        mutableStateOf(RecipeCalculator.formatNumber(initialItem?.nutritionPer100gOrMl?.fiberG ?: 0.0))
    }
    var sodium by remember {
        mutableStateOf(RecipeCalculator.formatNumber(initialItem?.nutritionPer100gOrMl?.sodiumMg ?: 0.0))
    }

    var photoPath by remember { mutableStateOf(initialItem?.photoPath) }
    var isScanningAi by remember { mutableStateOf(false) }
    var aiStatusMessage by remember { mutableStateOf<String?>(null) }

    val commonStores = listOf("Walmart", "Costco", "Soriana", "Chedraui", "HEB", "Sam's", "Mercado", "Bodega Aurrera")

    fun applyScannedNutrition(info: NutritionInfo) {
        hasNutrition = true
        calories = RecipeCalculator.formatNumber(info.caloriesKcal)
        protein = RecipeCalculator.formatNumber(info.proteinG)
        carbs = RecipeCalculator.formatNumber(info.carbsG)
        sugars = RecipeCalculator.formatNumber(info.sugarsG)
        totalFat = RecipeCalculator.formatNumber(info.totalFatG)
        satFat = RecipeCalculator.formatNumber(info.saturatedFatG)
        fiber = RecipeCalculator.formatNumber(info.fiberG)
        sodium = RecipeCalculator.formatNumber(info.sodiumMg)
    }

    val productPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val bmp = GeminiNutritionScanner.loadAndDownscaleFromUri(context, uri, 768)
            if (bmp != null) {
                photoPath = GeminiNutritionScanner.saveBitmapToInternalStorage(context, bmp)
            }
        }
    }

    val labelGalleryScanner = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val lowResBmp = GeminiNutritionScanner.loadAndDownscaleFromUri(context, uri, 768)
            if (lowResBmp != null) {
                isScanningAi = true
                aiStatusMessage = "Reduciendo resolución y analizando etiqueta con Gemini..."
                scope.launch {
                    val res = GeminiNutritionScanner.scanNutritionLabel(lowResBmp, geminiApiKey)
                    isScanningAi = false
                    res.onSuccess { info ->
                        applyScannedNutrition(info)
                        aiStatusMessage = "✅ ¡Tabla nutrimental extraída con éxito!"
                    }.onFailure { err ->
                        aiStatusMessage = "❌ ${err.message}"
                    }
                }
            }
        }
    }

    val labelCameraScanner = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp: Bitmap? ->
        if (bmp != null) {
            isScanningAi = true
            aiStatusMessage = "Reduciendo resolución y analizando etiqueta con Gemini..."
            scope.launch {
                val res = GeminiNutritionScanner.scanNutritionLabel(bmp, geminiApiKey)
                isScanningAi = false
                res.onSuccess { info ->
                    applyScannedNutrition(info)
                    aiStatusMessage = "✅ ¡Tabla nutrimental extraída con éxito!"
                }.onFailure { err ->
                    aiStatusMessage = "❌ ${err.message}"
                }
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            labelCameraScanner.launch(null)
        } else {
            aiStatusMessage = "Se requiere permiso de cámara para tomar la foto directa."
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initialItem == null) uiConfig.text(UiTextKey.PANTRY_DIALOG_NEW_TITLE)
                else uiConfig.text(UiTextKey.PANTRY_DIALOG_EDIT_TITLE)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(uiConfig.text(UiTextKey.PANTRY_FIELD_NAME)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = storeName,
                    onValueChange = { storeName = it },
                    label = { Text("${uiConfig.storeSymbol} ${uiConfig.text(UiTextKey.PANTRY_FIELD_STORE)}") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    commonStores.forEach { store ->
                        SuggestionChip(
                            onClick = { storeName = store },
                            label = { Text(store, fontSize = 12.sp) }
                        )
                    }
                }

                Text(uiConfig.text(UiTextKey.PANTRY_HOW_MEASURED_LABEL), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MeasureUnit.entries.forEach { u ->
                        FilterChip(
                            selected = defaultUnit == u,
                            onClick = {
                                defaultUnit = u
                                priceRefUnit = u
                                if (u == MeasureUnit.PIECES && priceRefAmount == "1000") {
                                    priceRefAmount = "1"
                                }
                            },
                            label = { Text(u.label) }
                        )
                    }
                }

                if (defaultUnit == MeasureUnit.PIECES) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "${uiConfig.text(UiTextKey.SYMBOL_SCALE)} ${uiConfig.text(UiTextKey.PANTRY_PIECE_EQUIV_TITLE)}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(6.dp))
                            OutlinedTextField(
                                value = amountPerPiece,
                                onValueChange = { amountPerPiece = it },
                                label = { Text(uiConfig.text(UiTextKey.PANTRY_PIECE_EQUIV_FIELD)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PieceEquivalenceUnit.entries.forEach { pu ->
                                    FilterChip(
                                        selected = pieceEquivUnit == pu,
                                        onClick = { pieceEquivUnit = pu },
                                        label = { Text(pu.label) }
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = {
                            productPhotoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    ) {
                        Text(
                            if (photoPath == null) uiConfig.text(UiTextKey.PANTRY_ADD_PHOTO_BTN)
                            else uiConfig.text(UiTextKey.PANTRY_CHANGE_PHOTO_BTN)
                        )
                    }
                    if (photoPath != null) {
                        Text(uiConfig.text(UiTextKey.PANTRY_PHOTO_SAVED_LABEL), style = MaterialTheme.typography.bodySmall)
                    }
                }

                HorizontalDivider()

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "${uiConfig.priceSymbol} ${uiConfig.text(UiTextKey.PANTRY_REGISTER_PRICE_TITLE)}",
                        fontWeight = FontWeight.Bold
                    )
                    Switch(checked = hasPrice, onCheckedChange = { hasPrice = it })
                }

                if (hasPrice) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = priceCost,
                            onValueChange = { priceCost = it },
                            label = { Text(uiConfig.text(UiTextKey.PANTRY_PRICE_COST_FIELD)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = priceRefAmount,
                            onValueChange = { priceRefAmount = it },
                            label = { Text(uiConfig.text(UiTextKey.PANTRY_PRICE_REF_FIELD)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MeasureUnit.entries.forEach { u ->
                            FilterChip(
                                selected = priceRefUnit == u,
                                onClick = { priceRefUnit = u },
                                label = { Text("Por ${u.symbol}") }
                            )
                        }
                    }
                }

                HorizontalDivider()

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "${uiConfig.nutritionSymbol} ${uiConfig.text(UiTextKey.PANTRY_REGISTER_NUTRITION_TITLE)}",
                        fontWeight = FontWeight.Bold
                    )
                    Switch(checked = hasNutrition, onCheckedChange = { hasNutrition = it })
                }

                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            uiConfig.text(UiTextKey.PANTRY_GEMINI_SCAN_HEADER),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    labelGalleryScanner.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                enabled = !isScanningAi,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(uiConfig.text(UiTextKey.PANTRY_SCAN_GALLERY_BTN), fontSize = 12.sp)
                            }
                            Button(
                                onClick = {
                                    val hasCamPerm = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.CAMERA
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (hasCamPerm) {
                                        labelCameraScanner.launch(null)
                                    } else {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                },
                                enabled = !isScanningAi,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(uiConfig.text(UiTextKey.PANTRY_SCAN_CAMERA_BTN), fontSize = 12.sp)
                            }
                        }
                        if (isScanningAi) {
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        if (aiStatusMessage != null) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = aiStatusMessage!!,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                if (hasNutrition) {
                    Text(
                        uiConfig.text(UiTextKey.PANTRY_NUTRITION_VALUES_HEADER),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = calories,
                            onValueChange = { calories = it },
                            label = { Text("1. Calorías (kcal)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = protein,
                            onValueChange = { protein = it },
                            label = { Text("2. Proteínas (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = totalFat,
                            onValueChange = { totalFat = it },
                            label = { Text("3. Grasas tot. (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = satFat,
                            onValueChange = { satFat = it },
                            label = { Text("• Saturadas (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = carbs,
                            onValueChange = { carbs = it },
                            label = { Text("4. Carbohidratos (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = sugars,
                            onValueChange = { sugars = it },
                            label = { Text("• Azúcares (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = fiber,
                            onValueChange = { fiber = it },
                            label = { Text("5. Fibra (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = sodium,
                            onValueChange = { sodium = it },
                            label = { Text("6. Sodio (mg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    onConfirm(
                        PantryItem(
                            id = initialItem?.id ?: 0L,
                            name = name.trim(),
                            storeName = storeName.trim(),
                            defaultUnit = defaultUnit,
                            amountPerPiece = amountPerPiece.toDoubleOrNull()?.coerceAtLeast(0.1) ?: 100.0,
                            pieceEquivalenceUnit = pieceEquivUnit,
                            hasPrice = hasPrice,
                            priceCost = priceCost.toDoubleOrNull() ?: 0.0,
                            priceReferenceAmount = priceRefAmount.toDoubleOrNull()?.coerceAtLeast(0.1) ?: 1000.0,
                            priceReferenceUnit = priceRefUnit,
                            hasNutrition = hasNutrition,
                            nutritionPer100gOrMl = NutritionInfo(
                                caloriesKcal = calories.toDoubleOrNull() ?: 0.0,
                                proteinG = protein.toDoubleOrNull() ?: 0.0,
                                carbsG = carbs.toDoubleOrNull() ?: 0.0,
                                sugarsG = sugars.toDoubleOrNull() ?: 0.0,
                                totalFatG = totalFat.toDoubleOrNull() ?: 0.0,
                                saturatedFatG = satFat.toDoubleOrNull() ?: 0.0,
                                fiberG = fiber.toDoubleOrNull() ?: 0.0,
                                sodiumMg = sodium.toDoubleOrNull() ?: 0.0
                            ),
                            photoPath = photoPath
                        )
                    )
                }
            ) {
                Text(uiConfig.text(UiTextKey.BTN_SAVE))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(uiConfig.text(UiTextKey.BTN_CANCEL)) }
        }
    )
}

@Composable
fun CatalogImportDialog(
    existingNames: Set<String>,
    uiConfig: AppUiConfig,
    onImportItem: (PantryItem) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("${uiConfig.text(UiTextKey.SYMBOL_CATALOG)} ${uiConfig.text(UiTextKey.PANTRY_CATALOG_DIALOG_TITLE)}")
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(BuiltInCatalog.items) { catalogItem ->
                    val alreadyAdded = existingNames.contains(catalogItem.name.lowercase())
                    val unitDetail = if (catalogItem.defaultUnit == MeasureUnit.PIECES) {
                        "1 pz ≈ ${RecipeCalculator.formatNumber(catalogItem.amountPerPiece)}${catalogItem.pieceEquivalenceUnit.symbol} | "
                    } else ""
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(catalogItem.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    "${uiConfig.priceSymbol} ${uiConfig.nutritionSymbol} | $unitDetail${RecipeCalculator.formatNumber(catalogItem.nutritionPer100gOrMl.caloriesKcal)} kcal/100g",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Button(
                                onClick = { onImportItem(catalogItem) },
                                enabled = !alreadyAdded
                            ) {
                                Text(
                                    if (alreadyAdded) uiConfig.text(UiTextKey.BTN_ALREADY_ADDED)
                                    else uiConfig.text(UiTextKey.BTN_IMPORT)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(uiConfig.text(UiTextKey.BTN_CLOSE)) }
        }
    )
}

// =========================================================================
// APARTADO 3: CONFIGURACIÓN (CON TEXTOS FIJOS Y OPCIONES AVANZADAS COMPLETAS)
// =========================================================================

@Composable
fun SettingsScreen(
    currentApiKey: String,
    uiConfig: AppUiConfig,
    onSaveApiKey: (String) -> Unit,
    onUpdateUiConfig: (AppUiConfig) -> Unit
) {
    var apiKeyInput by remember(currentApiKey) { mutableStateOf(currentApiKey) }
    var savedApiKeyBanner by remember { mutableStateOf(false) }
    var showAdvancedOptions by remember { mutableStateOf(false) }

    // Filtros para el editor de todos los textos default en Opciones Avanzadas
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Todos") }

    val categories = remember {
        listOf("Todos") + UiTextKey.entries.map { it.category }.distinct()
    }

    val filteredTextKeys = remember(searchQuery, selectedCategory) {
        UiTextKey.entries.filter { key ->
            val matchesCat = selectedCategory == "Todos" || key.category == selectedCategory
            val q = searchQuery.trim().lowercase()
            val matchesSearch = q.isEmpty() ||
                key.description.lowercase().contains(q) ||
                key.defaultText.lowercase().contains(q)
            matchesCat && matchesSearch
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. TARJETA DE GEMINI API KEY (Textos fijos de Configuración)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "🤖 API de Gemini (Escáner de Etiquetas)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Reduce las fotos a 768px antes de enviarlas para ahorrar tokens y rellenar la tabla nutrimental.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = {
                        apiKeyInput = it
                        savedApiKeyBanner = false
                    },
                    label = { Text("Tu Gemini API Key (AIza...)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        onSaveApiKey(apiKeyInput)
                        savedApiKeyBanner = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Guardar API Key")
                }
                if (savedApiKeyBanner) {
                    Text(
                        text = "✅ API Key guardada en el teléfono.",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // 2. ACTIVAR / DESACTIVAR SECCIONES DE LA BOTTOM BAR Y FILAS
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "🧭 Secciones de la Barra Inferior (Bottom Bar)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Activa o desactiva apartados. 'Configuración' no se puede desactivar y siempre queda fija en la esquina inferior derecha.",
                    style = MaterialTheme.typography.bodySmall
                )

                AppSection.entries.forEach { section ->
                    val isEnabled = section == AppSection.SETTINGS || uiConfig.enabledSectionIds.contains(section.id)
                    val secTitle = when (section) {
                        AppSection.RECIPES -> "${uiConfig.text(UiTextKey.RECIPES_TAB_ICON)} ${uiConfig.text(UiTextKey.RECIPES_TAB_LABEL)}"
                        AppSection.PANTRY -> "${uiConfig.text(UiTextKey.PANTRY_TAB_ICON)} ${uiConfig.text(UiTextKey.PANTRY_TAB_LABEL)}"
                        AppSection.SETTINGS -> "⚙️ Configuración (Fija en esquina inf. derecha)"
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(secTitle, style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = isEnabled,
                            enabled = section.canBeDisabled,
                            onCheckedChange = { checked ->
                                if (section.canBeDisabled) {
                                    val nextSet = uiConfig.enabledSectionIds.toMutableSet()
                                    if (checked) nextSet.add(section.id) else nextSet.remove(section.id)
                                    nextSet.add(AppSection.SETTINGS.id)
                                    onUpdateUiConfig(uiConfig.copy(enabledSectionIds = nextSet))
                                }
                            }
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    text = "Máximo de secciones por fila antes de ampliar la Bottom Bar en más filas:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(2, 3, 4, 5).forEach { count ->
                        FilterChip(
                            selected = uiConfig.maxItemsPerBottomRow == count,
                            onClick = {
                                onUpdateUiConfig(uiConfig.copy(maxItemsPerBottomRow = count))
                            },
                            label = { Text("$count por fila") }
                        )
                    }
                }
            }
        }

        // 3. PARTES VISIBLES DE LA TABLA NUTRIMENTAL
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "🧠 Elementos visibles en la Tabla Nutrimental",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Desactiva las partes de la tabla nutrimental que no quieras que se muestren en tus recetas:",
                    style = MaterialTheme.typography.bodySmall
                )

                NutritionToggleRow("1. Energía (Calorías kcal)", uiConfig.showCalories) {
                    onUpdateUiConfig(uiConfig.copy(showCalories = it))
                }
                NutritionToggleRow("2. Proteínas (g)", uiConfig.showProtein) {
                    onUpdateUiConfig(uiConfig.copy(showProtein = it))
                }
                NutritionToggleRow("3. Grasas / Lípidos Totales (g)", uiConfig.showTotalFat) {
                    onUpdateUiConfig(uiConfig.copy(showTotalFat = it))
                }
                NutritionToggleRow("   • Grasas Saturadas (g)", uiConfig.showSaturatedFat) {
                    onUpdateUiConfig(uiConfig.copy(showSaturatedFat = it))
                }
                NutritionToggleRow("4. Carbohidratos (g)", uiConfig.showCarbs) {
                    onUpdateUiConfig(uiConfig.copy(showCarbs = it))
                }
                NutritionToggleRow("   • Azúcares (g)", uiConfig.showSugars) {
                    onUpdateUiConfig(uiConfig.copy(showSugars = it))
                }
                NutritionToggleRow("5. Fibra Dietética (g)", uiConfig.showFiber) {
                    onUpdateUiConfig(uiConfig.copy(showFiber = it))
                }
                NutritionToggleRow("6. Sodio (mg)", uiConfig.showSodium) {
                    onUpdateUiConfig(uiConfig.copy(showSodium = it))
                }
            }
        }

        // 4. OPCIONES AVANZADAS: MODIFICAR TODOS LOS TEXTOS Y SÍMBOLOS DEFAULT DE LA APP
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🛠️ Opciones Avanzadas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Modifica cualquiera de los ${UiTextKey.entries.size} símbolos y textos por defecto de la app (excepto los de esta pestaña de Configuración).",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Button(onClick = { showAdvancedOptions = !showAdvancedOptions }) {
                        Text(if (showAdvancedOptions) "Ocultar" else "Abrir")
                    }
                }

                if (showAdvancedOptions) {
                    HorizontalDivider()

                    // Buscador de textos y símbolos
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("🔍 Buscar texto o símbolo a modificar...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Filtro por categoría
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat) }
                            )
                        }
                    }

                    // Botón para restaurar todos los textos si alguno fue cambiado
                    if (uiConfig.customTexts.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { onUpdateUiConfig(uiConfig.copy(customTexts = emptyMap())) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("🔄 Restaurar los ${uiConfig.customTexts.size} textos modificados a sus valores de fábrica")
                        }
                    }

                    // Lista de campos editables para cada texto/símbolo
                    filteredTextKeys.forEach { key ->
                        val currentValue = uiConfig.text(key)
                        val isModified = uiConfig.customTexts.containsKey(key.name)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedTextField(
                                    value = currentValue,
                                    onValueChange = { newVal ->
                                        onUpdateUiConfig(uiConfig.withCustomText(key, newVal))
                                    },
                                    label = {
                                        Text("${key.category}: ${key.description}")
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                if (isModified) {
                                    TextButton(
                                        onClick = {
                                            onUpdateUiConfig(uiConfig.withCustomText(key, key.defaultText))
                                        }
                                    ) {
                                        Text("↺")
                                    }
                                }
                            }
                            if (isModified) {
                                Text(
                                    text = "Default original: \"${key.defaultText}\"",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NutritionToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
