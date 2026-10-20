package com.example.rp.data

/**
 * Catálogo integrado en la app para importar rápidamente verduras, carnes,
 * harinas, lácteos y básicos con su peso promedio por pieza e información nutrimental por 100g/ml.
 */
object BuiltInCatalog {

    val items: List<PantryItem> = listOf(
        PantryItem(
            name = "Harina de Trigo (Panadera / Todo uso)",
            defaultUnit = MeasureUnit.GRAMS,
            amountPerPiece = 100.0,
            pieceEquivalenceUnit = PieceEquivalenceUnit.GRAMS,
            hasPrice = true,
            priceCost = 24.0,
            priceReferenceAmount = 1000.0,
            priceReferenceUnit = MeasureUnit.GRAMS,
            hasNutrition = true,
            nutritionPer100gOrMl = NutritionInfo(
                caloriesKcal = 364.0,
                proteinG = 10.3,
                carbsG = 76.3,
                sugarsG = 0.3,
                totalFatG = 1.0,
                saturatedFatG = 0.2,
                fiberG = 2.7,
                sodiumMg = 2.0
            )
        ),
        PantryItem(
            name = "Agua",
            defaultUnit = MeasureUnit.MILLILITERS,
            amountPerPiece = 250.0,
            pieceEquivalenceUnit = PieceEquivalenceUnit.MILLILITERS,
            hasPrice = false,
            hasNutrition = true,
            nutritionPer100gOrMl = NutritionInfo()
        ),
        PantryItem(
            name = "Sal Fina",
            defaultUnit = MeasureUnit.GRAMS,
            amountPerPiece = 5.0,
            pieceEquivalenceUnit = PieceEquivalenceUnit.GRAMS,
            hasPrice = true,
            priceCost = 18.0,
            priceReferenceAmount = 1000.0,
            priceReferenceUnit = MeasureUnit.GRAMS,
            hasNutrition = true,
            nutritionPer100gOrMl = NutritionInfo(
                sodiumMg = 38758.0
            )
        ),
        PantryItem(
            name = "Levadura Seca Instantánea",
            defaultUnit = MeasureUnit.GRAMS,
            amountPerPiece = 11.0,
            pieceEquivalenceUnit = PieceEquivalenceUnit.GRAMS,
            hasPrice = true,
            priceCost = 35.0,
            priceReferenceAmount = 100.0,
            priceReferenceUnit = MeasureUnit.GRAMS,
            hasNutrition = true,
            nutritionPer100gOrMl = NutritionInfo(
                caloriesKcal = 325.0,
                proteinG = 40.4,
                carbsG = 41.2,
                sugarsG = 0.0,
                totalFatG = 7.6,
                saturatedFatG = 1.0,
                fiberG = 26.9,
                sodiumMg = 51.0
            )
        ),
        PantryItem(
            name = "Huevo de Gallina",
            defaultUnit = MeasureUnit.PIECES,
            amountPerPiece = 55.0, // 1 huevo mediano/grande pesa ~55g sin cáscara
            pieceEquivalenceUnit = PieceEquivalenceUnit.GRAMS,
            hasPrice = true,
            priceCost = 45.0,
            priceReferenceAmount = 12.0,
            priceReferenceUnit = MeasureUnit.PIECES,
            hasNutrition = true,
            nutritionPer100gOrMl = NutritionInfo(
                caloriesKcal = 143.0,
                proteinG = 12.6,
                carbsG = 0.7,
                sugarsG = 0.4,
                totalFatG = 9.5,
                saturatedFatG = 3.1,
                fiberG = 0.0,
                sodiumMg = 142.0
            )
        ),
        PantryItem(
            name = "Queso Manchego / Gouda (Rebanada)",
            defaultUnit = MeasureUnit.PIECES,
            amountPerPiece = 28.0, // 1 rebanada promedio pesa 28g
            pieceEquivalenceUnit = PieceEquivalenceUnit.GRAMS,
            hasPrice = true,
            priceCost = 95.0,
            priceReferenceAmount = 400.0,
            priceReferenceUnit = MeasureUnit.GRAMS,
            hasNutrition = true,
            nutritionPer100gOrMl = NutritionInfo(
                caloriesKcal = 356.0,
                proteinG = 24.9,
                carbsG = 2.2,
                sugarsG = 2.2,
                totalFatG = 27.4,
                saturatedFatG = 17.6,
                fiberG = 0.0,
                sodiumMg = 819.0
            )
        ),
        PantryItem(
            name = "Carne de Res (Molida / Bistec)",
            defaultUnit = MeasureUnit.GRAMS,
            amountPerPiece = 150.0,
            pieceEquivalenceUnit = PieceEquivalenceUnit.GRAMS,
            hasPrice = true,
            priceCost = 185.0,
            priceReferenceAmount = 1000.0,
            priceReferenceUnit = MeasureUnit.GRAMS,
            hasNutrition = true,
            nutritionPer100gOrMl = NutritionInfo(
                caloriesKcal = 250.0,
                proteinG = 26.0,
                carbsG = 0.0,
                sugarsG = 0.0,
                totalFatG = 15.0,
                saturatedFatG = 6.0,
                fiberG = 0.0,
                sodiumMg = 72.0
            )
        ),
        PantryItem(
            name = "Pechuga de Pollo",
            defaultUnit = MeasureUnit.GRAMS,
            amountPerPiece = 200.0,
            pieceEquivalenceUnit = PieceEquivalenceUnit.GRAMS,
            hasPrice = true,
            priceCost = 145.0,
            priceReferenceAmount = 1000.0,
            priceReferenceUnit = MeasureUnit.GRAMS,
            hasNutrition = true,
            nutritionPer100gOrMl = NutritionInfo(
                caloriesKcal = 165.0,
                proteinG = 31.0,
                carbsG = 0.0,
                sugarsG = 0.0,
                totalFatG = 3.6,
                saturatedFatG = 1.0,
                fiberG = 0.0,
                sodiumMg = 74.0
            )
        ),
        PantryItem(
            name = "Jitomate / Tomate Saladet",
            defaultUnit = MeasureUnit.PIECES,
            amountPerPiece = 120.0, // 1 jitomate mediano pesa ~120g
            pieceEquivalenceUnit = PieceEquivalenceUnit.GRAMS,
            hasPrice = true,
            priceCost = 32.0,
            priceReferenceAmount = 1000.0,
            priceReferenceUnit = MeasureUnit.GRAMS,
            hasNutrition = true,
            nutritionPer100gOrMl = NutritionInfo(
                caloriesKcal = 18.0,
                proteinG = 0.9,
                carbsG = 3.9,
                sugarsG = 2.6,
                totalFatG = 0.2,
                saturatedFatG = 0.0,
                fiberG = 1.2,
                sodiumMg = 5.0
            )
        ),
        PantryItem(
            name = "Cebolla Blanca",
            defaultUnit = MeasureUnit.PIECES,
            amountPerPiece = 150.0,
            pieceEquivalenceUnit = PieceEquivalenceUnit.GRAMS,
            hasPrice = true,
            priceCost = 28.0,
            priceReferenceAmount = 1000.0,
            priceReferenceUnit = MeasureUnit.GRAMS,
            hasNutrition = true,
            nutritionPer100gOrMl = NutritionInfo(
                caloriesKcal = 40.0,
                proteinG = 1.1,
                carbsG = 9.3,
                sugarsG = 4.2,
                totalFatG = 0.1,
                saturatedFatG = 0.0,
                fiberG = 1.7,
                sodiumMg = 4.0
            )
        ),
        PantryItem(
            name = "Aguacate Hass",
            defaultUnit = MeasureUnit.PIECES,
            amountPerPiece = 170.0, // Peso neto promedio de pulpa
            pieceEquivalenceUnit = PieceEquivalenceUnit.GRAMS,
            hasPrice = true,
            priceCost = 75.0,
            priceReferenceAmount = 1000.0,
            priceReferenceUnit = MeasureUnit.GRAMS,
            hasNutrition = true,
            nutritionPer100gOrMl = NutritionInfo(
                caloriesKcal = 160.0,
                proteinG = 2.0,
                carbsG = 8.5,
                sugarsG = 0.7,
                totalFatG = 14.7,
                saturatedFatG = 2.1,
                fiberG = 6.7,
                sodiumMg = 7.0
            )
        ),
        PantryItem(
            name = "Mantequilla sin Sal",
            defaultUnit = MeasureUnit.GRAMS,
            amountPerPiece = 90.0, // 1 barra estándar = 90g
            pieceEquivalenceUnit = PieceEquivalenceUnit.GRAMS,
            hasPrice = true,
            priceCost = 26.0,
            priceReferenceAmount = 90.0,
            priceReferenceUnit = MeasureUnit.GRAMS,
            hasNutrition = true,
            nutritionPer100gOrMl = NutritionInfo(
                caloriesKcal = 717.0,
                proteinG = 0.9,
                carbsG = 0.1,
                sugarsG = 0.1,
                totalFatG = 81.1,
                saturatedFatG = 51.4,
                fiberG = 0.0,
                sodiumMg = 11.0
            )
        ),
        PantryItem(
            name = "Leche Entera",
            defaultUnit = MeasureUnit.MILLILITERS,
            amountPerPiece = 250.0,
            pieceEquivalenceUnit = PieceEquivalenceUnit.MILLILITERS,
            hasPrice = true,
            priceCost = 29.0,
            priceReferenceAmount = 1000.0,
            priceReferenceUnit = MeasureUnit.MILLILITERS,
            hasNutrition = true,
            nutritionPer100gOrMl = NutritionInfo(
                caloriesKcal = 61.0,
                proteinG = 3.2,
                carbsG = 4.8,
                sugarsG = 5.1,
                totalFatG = 3.3,
                saturatedFatG = 1.9,
                fiberG = 0.0,
                sodiumMg = 43.0
            )
        )
    )
}
