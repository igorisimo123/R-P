package com.example.rp.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.rp.data.*
import kotlinx.coroutines.launch
import java.io.File

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

    fun refreshData() {
        pantryItems = db.getAllPantryItems()
        recipes = db.getAllRecipes()
    }

    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (selectedTab) {
                            0 -> "🍲 Mis Recetas y Platillos"
                            1 -> "🧺 Mi Despensa"
                            else -> "🤖 Gemini IA y Ajustes"
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
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Text("🍲", fontSize = 20.sp) },
                    label = { Text("Recetas") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Text("🧺", fontSize = 20.sp) },
                    label = { Text("Despensa") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Text("🤖", fontSize = 20.sp) },
                    label = { Text("IA / Ajustes") }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> RecipesScreen(
                    recipes = recipes,
                    pantryItems = pantryItems,
                    onSaveRecipe = { recipe ->
                        db.saveRecipe(recipe)
                        refreshData()
                    },
                    onDeleteRecipe = { id ->
                        db.deleteRecipe(id)
                        refreshData()
                    }
                )
                1 -> PantryScreen(
                    pantryItems = pantryItems,
                    geminiApiKey = geminiApiKey,
                    onSaveItem = { item ->
                        db.savePantryItem(item)
                        refreshData()
                    },
                    onDeleteItem = { id ->
                        db.deletePantryItem(id)
                        refreshData()
                    }
                )
                2 -> SettingsScreen(
                    currentApiKey = geminiApiKey,
                    onSaveApiKey = { newKey ->
                        db.setGeminiApiKey(newKey)
                        geminiApiKey = newKey.trim()
                    }
                )
            }
        }
    }
}

// =========================================================================
// PESTAÑA 1: RECETAS, PORCENTAJE PANADERO (GRAMOS O PIEZAS) Y PLATILLOS
// =========================================================================

@Composable
fun RecipesScreen(
    recipes: List<Recipe>,
    pantryItems: List<PantryItem>,
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
                text = { Text("Nueva Receta / Platillo") }
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
                    "Aún no tienes recetas guardadas. Pulsa '+ Nueva Receta / Platillo' para crear una receta normal, un pan con porcentaje panadero o un platillo combinado.",
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
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    // 1. Toggle para mostrar u ocultar el desglose general de esta receta
    var showRecipeBreakdown by remember { mutableStateOf(true) }

    // 2. Estado para recordar qué sub-recetas dentro de este platillo tienen el desglose activado (toggle)
    val expandedSubRecipes = remember { mutableStateMapOf<Int, Boolean>() }

    // 3. Modo de escalado en Panadería: por Gramos Totales de mezcla (false) o por Cantidad de Piezas (true)
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

    val summary = remember(recipe, targetTotalGrams, activeGramsPerPiece, pantryMap, recipeMap) {
        RecipeCalculator.calculateRecipe(
            recipe = recipe.copy(gramsPerPiece = activeGramsPerPiece),
            overrideTargetWeightGrams = if (recipe.isBakersPercentage) targetTotalGrams else null,
            pantryMap = pantryMap,
            recipeMap = recipeMap
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
                        if (summary.anyHasPrice) Text("💰", fontSize = 16.sp)
                        if (summary.anyHasNutrition) Text("🧠", fontSize = 16.sp)
                    }
                    Text(
                        text = if (recipe.isBakersPercentage) {
                            "🥖 Panadería (% sobre Harina = 100% | 1 pz = ${RecipeCalculator.formatNumber(activeGramsPerPiece)}g)"
                        } else {
                            "🍽️ Platillo / Receta Estándar"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row {
                    TextButton(onClick = onEdit) { Text("Editar") }
                    TextButton(onClick = onDelete) {
                        Text("Borrar", color = MaterialTheme.colorScheme.error)
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

            // Si es Receta de Panadería con Porcentaje Panadero, mostramos la calculadora por Gramos o por Piezas
            if (recipe.isBakersPercentage) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "⚖️ ¿Cómo quieres calcular la masa hoy?",
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
                                label = { Text("Por Gramos de Mezcla") }
                            )
                            FilterChip(
                                selected = scaleByPieces,
                                onClick = {
                                    scaleByPieces = true
                                    val pz = targetTotalGrams / activeGramsPerPiece
                                    customPieceCountText = RecipeCalculator.formatNumber(pz)
                                },
                                label = { Text("🥐 Por Cantidad de Piezas") }
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
                                    label = { Text("Gramos de mezcla (g)") },
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
                                text = "Equivale a ~${RecipeCalculator.formatNumber(summary.totalPieces)} piezas de ${RecipeCalculator.formatNumber(activeGramsPerPiece)}g cada una",
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
                                    label = { Text("¿Cuántas piezas?") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = customGramsPerPieceText,
                                    onValueChange = { customGramsPerPieceText = it },
                                    label = { Text("Gramos por 1 pz (g)") },
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
                                text = "👉 Mezcla total a preparar: ${RecipeCalculator.formatNumber(targetTotalGrams)} g (${RecipeCalculator.formatNumber(activePieceCount)} pz × ${RecipeCalculator.formatNumber(activeGramsPerPiece)}g)",
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

            // Botón / Switch para Togglear si se desglosa o no esta receta
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Desglose de ingredientes (${summary.rows.size}):",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                FilterChip(
                    selected = showRecipeBreakdown,
                    onClick = { showRecipeBreakdown = !showRecipeBreakdown },
                    label = {
                        Text(if (showRecipeBreakdown) "🔽 Ocultar desglose" else "▶️ Desglosar receta")
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
                                    if (row.hasPriceIcon) Text("💰", fontSize = 14.sp)
                                    if (row.hasNutritionIcon) {
                                        Spacer(Modifier.width(2.dp))
                                        Text("🧠", fontSize = 14.sp)
                                    }
                                }
                                if (row.storeName.isNotBlank()) {
                                    Text(
                                        text = "🛒 Súper: ${row.storeName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                // Si este renglón es una sub-receta, botón para togglear el desglose de esa sub-receta
                                if (row.isSubRecipe && row.subBreakdownRows.isNotEmpty()) {
                                    TextButton(
                                        onClick = { expandedSubRecipes[index] = !isSubExpanded },
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                    ) {
                                        Text(
                                            text = if (isSubExpanded) {
                                                "🔼 Ocultar ingredientes de esta receta"
                                            } else {
                                                "🔽 Desglosar ingredientes de esta receta (${row.subBreakdownRows.size})"
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

                        // Sub-desglose cuando el usuario activa el toggle de una sub-receta
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
                                        text = "Desglose proporcional para ${RecipeCalculator.formatNumber(row.exactGramsOrMl)}g de ${row.displayName}:",
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
                                                if (subRow.hasPriceIcon) Text(" 💰", fontSize = 11.sp)
                                                if (subRow.hasNutritionIcon) Text(" 🧠", fontSize = 11.sp)
                                                if (subRow.storeName.isNotBlank()) {
                                                    Text(
                                                        text = " (🛒 ${subRow.storeName})",
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
            // Resumen de Peso, Piezas y Costo Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Peso total: ${RecipeCalculator.formatNumber(summary.totalWeightGramsOrMl)} g",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (recipe.isBakersPercentage && summary.totalPieces > 0) {
                        Text(
                            text = "Piezas: ${RecipeCalculator.formatNumber(summary.totalPieces)} pz ($${RecipeCalculator.formatNumber(summary.costPerPiece)} / pz)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                Text(
                    text = "💰 Costo aprox: $${RecipeCalculator.formatNumber(summary.totalCost)}",
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
                    if (showNutritionTable) "Ocultar Tabla Nutrimental 🧠"
                    else "Ver Tabla Nutrimental Aproximada 🧠"
                )
            }

            if (showNutritionTable) {
                Spacer(Modifier.height(8.dp))
                NutritionTableView(
                    totalNutrition = summary.totalNutrition,
                    per100gNutrition = summary.nutritionPer100g,
                    totalWeightGrams = summary.totalWeightGramsOrMl
                )
            }
        }
    }
}

@Composable
fun NutritionTableView(
    totalNutrition: NutritionInfo,
    per100gNutrition: NutritionInfo,
    totalWeightGrams: Double
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "🧠 Información Nutrimental Aproximada",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Calculada con los ingredientes que tienen 🧠 registrado",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Nutriente", modifier = Modifier.weight(1.4f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Por 100g", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(
                    "Total (${RecipeCalculator.formatNumber(totalWeightGrams)}g)",
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            val orderedRows = listOf(
                Triple("1. Energía (Calorías)", "${RecipeCalculator.formatNumber(per100gNutrition.caloriesKcal)} kcal", "${RecipeCalculator.formatNumber(totalNutrition.caloriesKcal)} kcal"),
                Triple("2. Proteínas", "${RecipeCalculator.formatNumber(per100gNutrition.proteinG)} g", "${RecipeCalculator.formatNumber(totalNutrition.proteinG)} g"),
                Triple("3. Grasas / Lípidos Totales", "${RecipeCalculator.formatNumber(per100gNutrition.totalFatG)} g", "${RecipeCalculator.formatNumber(totalNutrition.totalFatG)} g"),
                Triple("   • Grasas Saturadas", "${RecipeCalculator.formatNumber(per100gNutrition.saturatedFatG)} g", "${RecipeCalculator.formatNumber(totalNutrition.saturatedFatG)} g"),
                Triple("4. Carbohidratos", "${RecipeCalculator.formatNumber(per100gNutrition.carbsG)} g", "${RecipeCalculator.formatNumber(totalNutrition.carbsG)} g"),
                Triple("   • Azúcares", "${RecipeCalculator.formatNumber(per100gNutrition.sugarsG)} g", "${RecipeCalculator.formatNumber(totalNutrition.sugarsG)} g"),
                Triple("5. Fibra Dietética", "${RecipeCalculator.formatNumber(per100gNutrition.fiberG)} g", "${RecipeCalculator.formatNumber(totalNutrition.fiberG)} g"),
                Triple("6. Sodio", "${RecipeCalculator.formatNumber(per100gNutrition.sodiumMg)} mg", "${RecipeCalculator.formatNumber(totalNutrition.sodiumMg)} mg")
            )

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeEditorDialog(
    initialRecipe: Recipe?,
    allRecipes: List<Recipe>,
    pantryItems: List<PantryItem>,
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialRecipe == null) "Nueva Receta o Platillo" else "Editar Receta o Platillo")
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
                    label = { Text("Nombre de la receta o platillo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas / Preparación (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Selector de Modo: Normal vs Porcentaje Panadero + Peso por pieza
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
                                Text("🥖 Modo Porcentaje Panadero", fontWeight = FontWeight.Bold)
                                Text(
                                    "Harina base = 100%, los demás ingredientes van en % respecto a la harina.",
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
                                    label = { Text("Mezcla base (g)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            OutlinedTextField(
                                value = gramsPerPiece,
                                onValueChange = { gramsPerPiece = it },
                                label = { Text("🥐 Gramos por 1 pieza (g)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Text(
                            text = "Indicar cuántos gramos pesa 1 pieza (ej. 80g o 100g) te permite cocinar calculando por número de piezas.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                HorizontalDivider()
                Text("Agregar ingrediente o sub-receta:", fontWeight = FontWeight.Bold)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = addingSourceType == ComponentSourceType.PANTRY_INGREDIENT,
                        onClick = { addingSourceType = ComponentSourceType.PANTRY_INGREDIENT },
                        label = { Text("🧺 De Despensa") }
                    )
                    FilterChip(
                        selected = addingSourceType == ComponentSourceType.SUB_RECIPE,
                        onClick = {
                            addingSourceType = ComponentSourceType.SUB_RECIPE
                            if (selectedSubRecipeId == 0L && availableSubRecipes.isNotEmpty()) {
                                selectedSubRecipeId = availableSubRecipes.first().id
                            }
                        },
                        label = { Text("🍲 De otra Receta") }
                    )
                }

                if (addingSourceType == ComponentSourceType.PANTRY_INGREDIENT) {
                    if (pantryItems.isEmpty()) {
                        Text(
                            "Primero agrega ingredientes en la pestaña Despensa.",
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text("Selecciona el ingrediente:", style = MaterialTheme.typography.labelMedium)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            pantryItems.forEach { item ->
                                val icons = buildString {
                                    if (item.hasPrice) append("💰")
                                    if (item.hasNutrition) append("🧠")
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
                        Text("Selecciona la receta a incluir:", style = MaterialTheme.typography.labelMedium)
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
                                        Text("🍲 ${sub.name} (1 pz=${RecipeCalculator.formatNumber(sub.gramsPerPiece)}g)")
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
                            if (isBakersMode) "Porcentaje respecto a harina (%) - Ej. 100 harina, 65 agua"
                            else "Cantidad (${selectedUnit.label})"
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

                if (isBakersMode && addingSourceType == ComponentSourceType.PANTRY_INGREDIENT) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isBaseFlour,
                            onCheckedChange = {
                                isBaseFlour = it
                                if (it) amountInput = "100"
                            }
                        )
                        Text("Es la harina base (100%)", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Button(
                    onClick = {
                        val amt = amountInput.toDoubleOrNull() ?: return@Button
                        if (amt <= 0.0) return@Button
                        if (addingSourceType == ComponentSourceType.PANTRY_INGREDIENT && selectedPantryId != 0L) {
                            components = components + RecipeComponent(
                                sourceType = ComponentSourceType.PANTRY_INGREDIENT,
                                referenceId = selectedPantryId,
                                amountOrPercentage = amt,
                                unit = if (isBakersMode) MeasureUnit.GRAMS else selectedUnit,
                                isBaseFlourInBakersMode = isBakersMode && isBaseFlour
                            )
                            isBaseFlour = false
                        } else if (addingSourceType == ComponentSourceType.SUB_RECIPE && selectedSubRecipeId != 0L) {
                            val subUnit = if (isBakersMode) MeasureUnit.GRAMS else selectedUnit
                            components = components + RecipeComponent(
                                sourceType = ComponentSourceType.SUB_RECIPE,
                                referenceId = selectedSubRecipeId,
                                amountOrPercentage = amt,
                                unit = subUnit
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("➕ Agregar a la lista de esta receta")
                }

                HorizontalDivider()
                Text("Componentes actuales (${components.size}):", fontWeight = FontWeight.Bold)

                components.forEachIndexed { idx, comp ->
                    val label = if (comp.sourceType == ComponentSourceType.PANTRY_INGREDIENT) {
                        val item = pantryItems.find { it.id == comp.referenceId }
                        val icons = buildString {
                            if (item?.hasPrice == true) append("💰")
                            if (item?.hasNutrition == true) append("🧠")
                        }
                        "${item?.name ?: "Ingrediente"} $icons"
                    } else {
                        val sub = allRecipes.find { it.id == comp.referenceId }
                        "🍲 ${sub?.name ?: "Receta"}"
                    }
                    val unitTxt = if (isBakersMode) "%" else comp.unit.symbol
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "• $label: ${RecipeCalculator.formatNumber(comp.amountOrPercentage)} $unitTxt",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall
                        )
                        TextButton(
                            onClick = {
                                components = components.toMutableList().also { it.removeAt(idx) }
                            }
                        ) {
                            Text("Quitar", color = MaterialTheme.colorScheme.error)
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
                Text("Guardar Receta")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// =========================================================================
// PESTAÑA 2: DESPENSA (CON SUPERMERCADO 🛒, PRECIOS 💰, NUTRICIÓN 🧠 Y PIEZAS)
// =========================================================================

@Composable
fun PantryScreen(
    pantryItems: List<PantryItem>,
    geminiApiKey: String,
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
                        Text("📦 Importar de Catálogo (Verduras/Básicos)")
                    }
                }
                ExtendedFloatingActionButton(
                    onClick = {
                        editingItem = null
                        showEditor = true
                    },
                    icon = { Text("➕", fontSize = 18.sp) },
                    text = { Text("Nuevo Ingrediente") }
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
                    text = "Leyenda: 💰 = Precio | 🧠 = Tabla nutrimental | 🛒 = Súper donde lo compras",
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
                            label = { Text("Todos (${pantryItems.size})") }
                        )
                        availableStores.forEach { store ->
                            FilterChip(
                                selected = selectedStoreFilter == store,
                                onClick = {
                                    selectedStoreFilter = if (selectedStoreFilter == store) null else store
                                },
                                label = { Text("🛒 $store") }
                            )
                        }
                    }
                }
            }
            items(filteredItems, key = { it.id }) { item ->
                PantryItemCard(
                    item = item,
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
                    if (item.hasPrice) Text("💰", fontSize = 16.sp)
                    if (item.hasNutrition) {
                        Spacer(Modifier.width(2.dp))
                        Text("🧠", fontSize = 16.sp)
                    }
                }

                if (item.storeName.isNotBlank()) {
                    Text(
                        text = "🛒 Súper: ${item.storeName}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                val unitInfo = if (item.defaultUnit == MeasureUnit.PIECES) {
                    "Se mide por Piezas (1 pz ≈ ${RecipeCalculator.formatNumber(item.amountPerPiece)} ${item.pieceEquivalenceUnit.symbol})"
                } else {
                    "Unidad base: ${item.defaultUnit.label} (1 pz ≈ ${RecipeCalculator.formatNumber(item.amountPerPiece)} ${item.pieceEquivalenceUnit.symbol})"
                }
                Text(text = unitInfo, style = MaterialTheme.typography.bodySmall)

                if (item.hasPrice) {
                    Text(
                        text = "💰 Costo: $${RecipeCalculator.formatNumber(item.priceCost)} por cada ${RecipeCalculator.formatNumber(item.priceReferenceAmount)} ${item.priceReferenceUnit.symbol}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (item.hasNutrition) {
                    val n = item.nutritionPer100gOrMl
                    Text(
                        text = "🧠 Por 100${item.pieceEquivalenceUnit.symbol}: ${RecipeCalculator.formatNumber(n.caloriesKcal)} kcal | Prot: ${RecipeCalculator.formatNumber(n.proteinG)}g | Carb: ${RecipeCalculator.formatNumber(n.carbsG)}g | Grasa: ${RecipeCalculator.formatNumber(n.totalFatG)}g",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Column {
                TextButton(onClick = onEdit) { Text("Editar") }
                TextButton(onClick = onDelete) {
                    Text("Borrar", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun PantryItemEditorDialog(
    initialItem: PantryItem?,
    geminiApiKey: String,
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
            Text(if (initialItem == null) "Nuevo Ingrediente en Despensa" else "Editar Ingrediente")
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
                    label = { Text("Nombre del ingrediente (ej. Harina, Huevo, Carne)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Campo de Supermercado / Tienda donde lo compra
                OutlinedTextField(
                    value = storeName,
                    onValueChange = { storeName = it },
                    label = { Text("🛒 ¿En qué súper o tienda lo compras? (Opcional)") },
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

                Text("¿Cómo se mide normalmente?", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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

                Surface(
                    color = if (defaultUnit == MeasureUnit.PIECES) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (defaultUnit == MeasureUnit.PIECES) {
                                "⚖️ Equivalencia obligatoria por Pieza (¿cuánto pesa o qué volumen ocupa 1 pieza en promedio?)"
                            } else {
                                "⚖️ Equivalencia opcional si llegas a usarlo por Pieza:"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = amountPerPiece,
                            onValueChange = { amountPerPiece = it },
                            label = { Text("1 pieza equivale a:") },
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
                        Text(if (photoPath == null) "🖼️ Agregar Foto (Opcional)" else "🖼️ Cambiar Foto")
                    }
                    if (photoPath != null) {
                        Text("✅ Foto guardada", style = MaterialTheme.typography.bodySmall)
                    }
                }

                HorizontalDivider()

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("💰 Registrar Precio", fontWeight = FontWeight.Bold)
                    Switch(checked = hasPrice, onCheckedChange = { hasPrice = it })
                }

                if (hasPrice) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = priceCost,
                            onValueChange = { priceCost = it },
                            label = { Text("Costo ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = priceRefAmount,
                            onValueChange = { priceRefAmount = it },
                            label = { Text("Por cantidad") },
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
                    Text("🧠 Información Nutrimental (por 100g/ml)", fontWeight = FontWeight.Bold)
                    Switch(checked = hasNutrition, onCheckedChange = { hasNutrition = it })
                }

                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            "🤖 Autocompletar con Gemini IA (baja la resolución a 768px para ahorrar créditos):",
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
                                Text("🖼️ Foto de Galería", fontSize = 12.sp)
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
                                Text("📷 Tomar Foto", fontSize = 12.sp)
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
                        "Valores por cada 100g o 100ml:",
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
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun CatalogImportDialog(
    existingNames: Set<String>,
    onImportItem: (PantryItem) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("📦 Catálogo Integrado de Ingredientes") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(BuiltInCatalog.items) { catalogItem ->
                    val alreadyAdded = existingNames.contains(catalogItem.name.lowercase())
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
                                    "💰 🧠 | 1 pz ≈ ${RecipeCalculator.formatNumber(catalogItem.amountPerPiece)}${catalogItem.pieceEquivalenceUnit.symbol} | ${RecipeCalculator.formatNumber(catalogItem.nutritionPer100gOrMl.caloriesKcal)} kcal/100g",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Button(
                                onClick = { onImportItem(catalogItem) },
                                enabled = !alreadyAdded
                            ) {
                                Text(if (alreadyAdded) "Agregado" else "Importar")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

// =========================================================================
// PESTAÑA 3: CONFIGURACIÓN DE GEMINI API KEY
// =========================================================================

@Composable
fun SettingsScreen(
    currentApiKey: String,
    onSaveApiKey: (String) -> Unit
) {
    var apiKeyInput by remember(currentApiKey) { mutableStateOf(currentApiKey) }
    var savedBanner by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "🤖 Escáner de Etiquetas con Gemini API",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Aquí puedes guardar tu API Key de Google AI Studio (Gemini). Cuando escanees una etiqueta nutricional en la Despensa:\n" +
                    "1. La app reduce primero la resolución de la foto a un máximo de 768 píxeles y la comprime en JPEG.\n" +
                    "2. Envía esa imagen ligera a Gemini para consumir la menor cantidad posible de créditos/tokens.\n" +
                    "3. Rellena automáticamente la tabla nutrimental ordenada por cada 100g/100ml.",
            style = MaterialTheme.typography.bodyMedium
        )

        OutlinedTextField(
            value = apiKeyInput,
            onValueChange = {
                apiKeyInput = it
                savedBanner = false
            },
            label = { Text("Tu Gemini API Key (AIza...)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                onSaveApiKey(apiKeyInput)
                savedBanner = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Guardar API Key en el teléfono")
        }

        if (savedBanner) {
            Text(
                text = "✅ API Key guardada localmente en el teléfono.",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
