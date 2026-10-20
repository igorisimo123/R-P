package com.example.rp.data

import kotlin.math.roundToInt

/**
 * En C++ esto sería un `enum class MeasureUnit { GRAMS, MILLILITERS, PIECES };`
 * En Python sería usar `class MeasureUnit(Enum):`
 */
enum class MeasureUnit(val symbol: String, val label: String) {
    GRAMS("g", "Gramos (g)"),
    MILLILITERS("ml", "Mililitros (ml)"),
    PIECES("pz", "Piezas (pz)")
}

/**
 * Si un ingrediente se mide por PIEZAS, este enum indica si su equivalencia
 * promedio por pieza está expresada en gramos (masa) o mililitros (volumen).
 */
enum class PieceEquivalenceUnit(val symbol: String, val label: String) {
    GRAMS("g", "Gramos por pieza"),
    MILLILITERS("ml", "Mililitros por pieza")
}

/**
 * Tabla nutrimental por cada 100g o 100ml del producto.
 */
data class NutritionInfo(
    val caloriesKcal: Double = 0.0,      // Energía (kcal)
    val proteinG: Double = 0.0,          // Proteínas (g)
    val carbsG: Double = 0.0,            // Hidratos de carbono / Carbohidratos (g)
    val sugarsG: Double = 0.0,           // Azúcares (g)
    val totalFatG: Double = 0.0,         // Grasas totales / Lípidos (g)
    val saturatedFatG: Double = 0.0,     // Grasas saturadas (g)
    val fiberG: Double = 0.0,            // Fibra dietética (g)
    val sodiumMg: Double = 0.0           // Sodio (mg)
) {
    operator fun plus(other: NutritionInfo): NutritionInfo {
        return NutritionInfo(
            caloriesKcal = this.caloriesKcal + other.caloriesKcal,
            proteinG = this.proteinG + other.proteinG,
            carbsG = this.carbsG + other.carbsG,
            sugarsG = this.sugarsG + other.sugarsG,
            totalFatG = this.totalFatG + other.totalFatG,
            saturatedFatG = this.saturatedFatG + other.saturatedFatG,
            fiberG = this.fiberG + other.fiberG,
            sodiumMg = this.sodiumMg + other.sodiumMg
        )
    }

    fun scale(factor: Double): NutritionInfo {
        return NutritionInfo(
            caloriesKcal = this.caloriesKcal * factor,
            proteinG = this.proteinG * factor,
            carbsG = this.carbsG * factor,
            sugarsG = this.sugarsG * factor,
            totalFatG = this.totalFatG * factor,
            saturatedFatG = this.saturatedFatG * factor,
            fiberG = this.fiberG * factor,
            sodiumMg = this.sodiumMg * factor
        )
    }
}

/**
 * Ingrediente guardado en la Despensa.
 */
data class PantryItem(
    val id: Long = 0L,
    val name: String,
    // Supermercado o tienda donde compras este ingrediente (ej. Costco, Walmart, Soriana, Mercado)
    val storeName: String = "",
    val defaultUnit: MeasureUnit = MeasureUnit.GRAMS,
    // Si se mide por piezas (o cuando se usa por pieza), ¿cuántos gramos o ml pesa/ocupa 1 pieza en promedio?
    val amountPerPiece: Double = 100.0,
    val pieceEquivalenceUnit: PieceEquivalenceUnit = PieceEquivalenceUnit.GRAMS,
    // Datos de precio opcionales (ej. $45 por cada 1000g, o $50 por 12 piezas)
    val hasPrice: Boolean = false,
    val priceCost: Double = 0.0,
    val priceReferenceAmount: Double = 1000.0,
    val priceReferenceUnit: MeasureUnit = MeasureUnit.GRAMS,
    // Datos nutrimentales opcionales (por cada 100g o 100ml)
    val hasNutrition: Boolean = false,
    val nutritionPer100gOrMl: NutritionInfo = NutritionInfo(),
    // Ruta local opcional de foto del ingrediente guardada en el teléfono
    val photoPath: String? = null
) {
    fun toGramsOrMl(amount: Double, unit: MeasureUnit): Double {
        return when (unit) {
            MeasureUnit.GRAMS, MeasureUnit.MILLILITERS -> amount
            MeasureUnit.PIECES -> amount * amountPerPiece
        }
    }

    fun gramsOrMlToPieces(gramsOrMl: Double): Double {
        if (amountPerPiece <= 0.0) return 0.0
        return gramsOrMl / amountPerPiece
    }

    fun calculateCost(amount: Double, unit: MeasureUnit): Double {
        if (!hasPrice || priceReferenceAmount <= 0.0) return 0.0
        val usedGramsOrMl = toGramsOrMl(amount, unit)
        val refGramsOrMl = toGramsOrMl(priceReferenceAmount, priceReferenceUnit)
        if (refGramsOrMl <= 0.0) return 0.0
        return (usedGramsOrMl / refGramsOrMl) * priceCost
    }

    fun calculateNutrition(amount: Double, unit: MeasureUnit): NutritionInfo {
        if (!hasNutrition) return NutritionInfo()
        val usedGramsOrMl = toGramsOrMl(amount, unit)
        return nutritionPer100gOrMl.scale(usedGramsOrMl / 100.0)
    }
}

enum class ComponentSourceType {
    PANTRY_INGREDIENT,
    SUB_RECIPE
}

/**
 * Renglón / ingrediente dentro de una receta o platillo.
 */
data class RecipeComponent(
    val sourceType: ComponentSourceType = ComponentSourceType.PANTRY_INGREDIENT,
    val referenceId: Long, // ID del PantryItem o ID de la Sub-Receta
    // En modo normal: cantidad en g, ml o pz.
    // En modo panadería (Porcentaje Panadero): porcentaje respecto a la harina (ej. Harina = 100.0, Agua = 65.0)
    val amountOrPercentage: Double,
    val unit: MeasureUnit = MeasureUnit.GRAMS,
    val isBaseFlourInBakersMode: Boolean = false
)

/**
 * Receta o Platillo guardado en el teléfono.
 * - `gramsPerPiece`: Cuántos gramos pesa 1 pieza de esta receta (ej. 1 bolillo/pan = 80g).
 *   Permite cocinar en base a cierta cantidad de piezas (ej. "Quiero hacer 12 piezas de 80g = 960g")
 *   y también usar esta receta por piezas dentro de otros platillos.
 */
data class Recipe(
    val id: Long = 0L,
    val name: String,
    val notes: String = "",
    val isBakersPercentage: Boolean = false,
    val defaultTargetWeightGrams: Double = 1000.0,
    val gramsPerPiece: Double = 100.0,
    val components: List<RecipeComponent> = emptyList()
)

/**
 * Resultado calculado para mostrar en pantalla (totales y desglose por ingrediente o sub-receta).
 */
data class CalculatedComponentRow(
    val component: RecipeComponent,
    val displayName: String,
    val storeName: String = "",
    val isSubRecipe: Boolean,
    val exactGramsOrMl: Double,
    val displayAmountText: String,
    val hasPriceIcon: Boolean,      // Muestra el símbolo de moneda 💰
    val hasNutritionIcon: Boolean,  // Muestra el símbolo de cerebro 🧠
    val calculatedCost: Double,
    val calculatedNutrition: NutritionInfo,
    // Si es una sub-receta, contiene sus ingredientes escalados proporcionalmente para poder hacer toggle del desglose
    val subBreakdownRows: List<CalculatedComponentRow> = emptyList()
)

data class CalculatedRecipeSummary(
    val totalWeightGramsOrMl: Double,
    val totalPieces: Double,
    val costPerPiece: Double,
    val totalCost: Double,
    val totalNutrition: NutritionInfo,
    val nutritionPer100g: NutritionInfo,
    val anyHasPrice: Boolean,
    val anyHasNutrition: Boolean,
    val rows: List<CalculatedComponentRow>
)

/**
 * Motor matemático de recetas (convierte piezas <-> gramos, escala porcentaje panadero por gramos o por piezas,
 * y resuelve sub-recetas recursivamente con su desglose interno).
 */
object RecipeCalculator {

    fun calculateRecipe(
        recipe: Recipe,
        overrideTargetWeightGrams: Double? = null,
        pantryMap: Map<Long, PantryItem>,
        recipeMap: Map<Long, Recipe>,
        visitedRecipeIds: Set<Long> = emptySet()
    ): CalculatedRecipeSummary {
        if (recipe.id != 0L && visitedRecipeIds.contains(recipe.id)) {
            return CalculatedRecipeSummary(
                totalWeightGramsOrMl = 0.0,
                totalPieces = 0.0,
                costPerPiece = 0.0,
                totalCost = 0.0,
                totalNutrition = NutritionInfo(),
                nutritionPer100g = NutritionInfo(),
                anyHasPrice = false,
                anyHasNutrition = false,
                rows = emptyList()
            )
        }
        val nextVisited = if (recipe.id != 0L) visitedRecipeIds + recipe.id else visitedRecipeIds

        val targetTotalGrams = overrideTargetWeightGrams ?: recipe.defaultTargetWeightGrams
        val totalPercentageSum = if (recipe.isBakersPercentage) {
            recipe.components.sumOf { it.amountOrPercentage }.coerceAtLeast(0.0001)
        } else {
            100.0
        }

        val rows = mutableListOf<CalculatedComponentRow>()
        var sumWeight = 0.0
        var sumCost = 0.0
        var sumNutrition = NutritionInfo()
        var anyPrice = false
        var anyNutrition = false

        for (comp in recipe.components) {
            when (comp.sourceType) {
                ComponentSourceType.PANTRY_INGREDIENT -> {
                    val item = pantryMap[comp.referenceId]
                    if (item != null) {
                        val gramsOrMl: Double
                        val displayAmount: String

                        if (recipe.isBakersPercentage) {
                            gramsOrMl = targetTotalGrams * (comp.amountOrPercentage / totalPercentageSum)
                            val pctStr = formatNumber(comp.amountOrPercentage)
                            val gStr = formatNumber(gramsOrMl)
                            val pieceHint = if (item.defaultUnit == MeasureUnit.PIECES && item.amountPerPiece > 0) {
                                " (~${formatNumber(item.gramsOrMlToPieces(gramsOrMl))} pz)"
                            } else ""
                            displayAmount = "$pctStr% → $gStr ${item.pieceEquivalenceUnit.symbol}$pieceHint"
                        } else {
                            gramsOrMl = item.toGramsOrMl(comp.amountOrPercentage, comp.unit)
                            val amtStr = formatNumber(comp.amountOrPercentage)
                            displayAmount = if (comp.unit == MeasureUnit.PIECES) {
                                "$amtStr pz (${formatNumber(gramsOrMl)} ${item.pieceEquivalenceUnit.symbol})"
                            } else {
                                "$amtStr ${comp.unit.symbol}"
                            }
                        }

                        val cost = item.calculateCost(gramsOrMl, MeasureUnit.GRAMS)
                        val nutrition = item.calculateNutrition(gramsOrMl, MeasureUnit.GRAMS)

                        if (item.hasPrice) anyPrice = true
                        if (item.hasNutrition) anyNutrition = true

                        sumWeight += gramsOrMl
                        sumCost += cost
                        sumNutrition = sumNutrition + nutrition

                        rows.add(
                            CalculatedComponentRow(
                                component = comp,
                                displayName = item.name,
                                storeName = item.storeName,
                                isSubRecipe = false,
                                exactGramsOrMl = gramsOrMl,
                                displayAmountText = displayAmount,
                                hasPriceIcon = item.hasPrice,
                                hasNutritionIcon = item.hasNutrition,
                                calculatedCost = cost,
                                calculatedNutrition = nutrition
                            )
                        )
                    }
                }

                ComponentSourceType.SUB_RECIPE -> {
                    val subRecipe = recipeMap[comp.referenceId]
                    if (subRecipe != null) {
                        val subSummary = calculateRecipe(
                            recipe = subRecipe,
                            overrideTargetWeightGrams = null,
                            pantryMap = pantryMap,
                            recipeMap = recipeMap,
                            visitedRecipeIds = nextVisited
                        )
                        val gramsUsed: Double
                        val displayAmount: String

                        if (recipe.isBakersPercentage) {
                            gramsUsed = targetTotalGrams * (comp.amountOrPercentage / totalPercentageSum)
                            displayAmount = "${formatNumber(comp.amountOrPercentage)}% → ${formatNumber(gramsUsed)} g"
                        } else if (comp.unit == MeasureUnit.PIECES) {
                            val pzWeight = subRecipe.gramsPerPiece.coerceAtLeast(1.0)
                            gramsUsed = comp.amountOrPercentage * pzWeight
                            displayAmount = "${formatNumber(comp.amountOrPercentage)} pz (${formatNumber(gramsUsed)} g)"
                        } else {
                            gramsUsed = comp.amountOrPercentage
                            displayAmount = "${formatNumber(gramsUsed)} g"
                        }

                        val ratio = if (subSummary.totalWeightGramsOrMl > 0.0) {
                            gramsUsed / subSummary.totalWeightGramsOrMl
                        } else 0.0

                        val subCost = subSummary.totalCost * ratio
                        val subNutrition = subSummary.totalNutrition.scale(ratio)

                        if (subSummary.anyHasPrice) anyPrice = true
                        if (subSummary.anyHasNutrition) anyNutrition = true

                        sumWeight += gramsUsed
                        sumCost += subCost
                        sumNutrition = sumNutrition + subNutrition

                        // Escalamos cada ingrediente interno de la sub-receta para mostrarlo cuando el usuario active el desglose
                        val scaledSubRows = subSummary.rows.map { childRow ->
                            val scaledGrams = childRow.exactGramsOrMl * ratio
                            CalculatedComponentRow(
                                component = childRow.component,
                                displayName = childRow.displayName,
                                storeName = childRow.storeName,
                                isSubRecipe = childRow.isSubRecipe,
                                exactGramsOrMl = scaledGrams,
                                displayAmountText = "${formatNumber(scaledGrams)} g",
                                hasPriceIcon = childRow.hasPriceIcon,
                                hasNutritionIcon = childRow.hasNutritionIcon,
                                calculatedCost = childRow.calculatedCost * ratio,
                                calculatedNutrition = childRow.calculatedNutrition.scale(ratio)
                            )
                        }

                        rows.add(
                            CalculatedComponentRow(
                                component = comp,
                                displayName = "🍲 ${subRecipe.name}",
                                storeName = "",
                                isSubRecipe = true,
                                exactGramsOrMl = gramsUsed,
                                displayAmountText = displayAmount,
                                hasPriceIcon = subSummary.anyHasPrice,
                                hasNutritionIcon = subSummary.anyHasNutrition,
                                calculatedCost = subCost,
                                calculatedNutrition = subNutrition,
                                subBreakdownRows = scaledSubRows
                            )
                        )
                    }
                }
            }
        }

        // Si no estamos en modo panadería pero tenemos componentes y nos pasan overrideTargetWeightGrams,
        // también podemos escalar un platillo normal si se desea
        val per100g = if (sumWeight > 0.0) {
            sumNutrition.scale(100.0 / sumWeight)
        } else {
            NutritionInfo()
        }

        val pieceWeight = recipe.gramsPerPiece.coerceAtLeast(0.1)
        val totalPieces = if (pieceWeight > 0.0) sumWeight / pieceWeight else 0.0
        val costPerPiece = if (totalPieces > 0.0) sumCost / totalPieces else 0.0

        return CalculatedRecipeSummary(
            totalWeightGramsOrMl = sumWeight,
            totalPieces = totalPieces,
            costPerPiece = costPerPiece,
            totalCost = sumCost,
            totalNutrition = sumNutrition,
            nutritionPer100g = per100g,
            anyHasPrice = anyPrice,
            anyHasNutrition = anyNutrition,
            rows = rows
        )
    }

    fun formatNumber(value: Double): String {
        val rounded = (value * 100.0).roundToInt() / 100.0
        return if (rounded % 1.0 == 0.0) {
            rounded.toLong().toString()
        } else {
            rounded.toString()
        }
    }
}
