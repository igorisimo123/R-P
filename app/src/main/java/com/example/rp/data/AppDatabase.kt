package com.example.rp.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject

/**
 * ============================================================================
 * ARCHIVO: AppDatabase.kt
 * ============================================================================
 * ¿QUÉ HACE ESTA CLASE?
 * Es el manejador de la base de datos local SQLite y de las configuraciones persistentes (`SharedPreferences`).
 */
class AppDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    private val prefs = context.getSharedPreferences("rp_settings", Context.MODE_PRIVATE)

    companion object {
        private const val DATABASE_NAME = "recetas_despensa.db"
        private const val DATABASE_VERSION = 3
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE pantry_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                store_name TEXT NOT NULL DEFAULT '',
                default_unit TEXT NOT NULL,
                amount_per_piece REAL NOT NULL,
                piece_equiv_unit TEXT NOT NULL,
                has_price INTEGER NOT NULL,
                price_cost REAL NOT NULL,
                price_ref_amount REAL NOT NULL,
                price_ref_unit TEXT NOT NULL,
                has_nutrition INTEGER NOT NULL,
                calories_kcal REAL NOT NULL,
                protein_g REAL NOT NULL,
                carbs_g REAL NOT NULL,
                sugars_g REAL NOT NULL,
                total_fat_g REAL NOT NULL,
                saturated_fat_g REAL NOT NULL,
                fiber_g REAL NOT NULL,
                sodium_mg REAL NOT NULL,
                photo_path TEXT
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE recipes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                notes TEXT NOT NULL,
                is_bakers_percentage INTEGER NOT NULL,
                default_target_weight_grams REAL NOT NULL,
                grams_per_piece REAL NOT NULL DEFAULT 100.0,
                components_json TEXT NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < DATABASE_VERSION) {
            runCatching {
                db.execSQL("ALTER TABLE pantry_items ADD COLUMN store_name TEXT NOT NULL DEFAULT ''")
            }
            runCatching {
                db.execSQL("ALTER TABLE recipes ADD COLUMN grams_per_piece REAL NOT NULL DEFAULT 100.0")
            }
        }
    }

    fun seedInitialDataIfEmpty() {
        if (prefs.getBoolean("initial_seed_done", false)) return
        if (getAllPantryItems().isNotEmpty()) {
            prefs.edit().putBoolean("initial_seed_done", true).apply()
            return
        }

        val insertedIdsByName = mutableMapOf<String, Long>()
        for (item in BuiltInCatalog.items.take(8)) {
            val id = savePantryItem(item)
            insertedIdsByName[item.name] = id
        }

        val flourId = insertedIdsByName["Harina de Trigo (Panadera / Todo uso)"] ?: 1L
        val waterId = insertedIdsByName["Agua"] ?: 2L
        val saltId = insertedIdsByName["Sal Fina"] ?: 3L
        val yeastId = insertedIdsByName["Levadura Seca Instantánea"] ?: 4L
        val cheeseId = insertedIdsByName["Queso Manchego / Gouda (Rebanada)"] ?: 6L
        val beefId = insertedIdsByName["Carne de Res (Molida / Bistec)"] ?: 7L

        val breadRecipeId = saveRecipe(
            Recipe(
                name = "Pan Artesanal (Base 100% Harina)",
                notes = "Receta en porcentaje panadero. Puedes cocinar por gramos de mezcla (ej. 1000g o 2000g) o por cantidad de piezas de pan.",
                isBakersPercentage = true,
                defaultTargetWeightGrams = 1000.0,
                gramsPerPiece = 100.0,
                components = listOf(
                    RecipeComponent(
                        sourceType = ComponentSourceType.PANTRY_INGREDIENT,
                        referenceId = flourId,
                        amountOrPercentage = 100.0,
                        unit = MeasureUnit.GRAMS,
                        isBaseFlourInBakersMode = true
                    ),
                    RecipeComponent(
                        sourceType = ComponentSourceType.PANTRY_INGREDIENT,
                        referenceId = waterId,
                        amountOrPercentage = 65.0,
                        unit = MeasureUnit.MILLILITERS
                    ),
                    RecipeComponent(
                        sourceType = ComponentSourceType.PANTRY_INGREDIENT,
                        referenceId = saltId,
                        amountOrPercentage = 2.0,
                        unit = MeasureUnit.GRAMS
                    ),
                    RecipeComponent(
                        sourceType = ComponentSourceType.PANTRY_INGREDIENT,
                        referenceId = yeastId,
                        amountOrPercentage = 1.0,
                        unit = MeasureUnit.GRAMS
                    )
                )
            )
        )

        saveRecipe(
            Recipe(
                name = "Platillo con Carne, Queso y Pan",
                notes = "Ejemplo combinando 100g de carne + 1 pieza de queso + 100g (o 1 pieza) de la receta de Pan Artesanal.",
                isBakersPercentage = false,
                defaultTargetWeightGrams = 1000.0,
                gramsPerPiece = 228.0,
                components = listOf(
                    RecipeComponent(
                        sourceType = ComponentSourceType.PANTRY_INGREDIENT,
                        referenceId = beefId,
                        amountOrPercentage = 100.0,
                        unit = MeasureUnit.GRAMS
                    ),
                    RecipeComponent(
                        sourceType = ComponentSourceType.PANTRY_INGREDIENT,
                        referenceId = cheeseId,
                        amountOrPercentage = 1.0,
                        unit = MeasureUnit.PIECES
                    ),
                    RecipeComponent(
                        sourceType = ComponentSourceType.SUB_RECIPE,
                        referenceId = breadRecipeId,
                        amountOrPercentage = 100.0,
                        unit = MeasureUnit.GRAMS
                    )
                )
            )
        )

        prefs.edit().putBoolean("initial_seed_done", true).apply()
    }

    // ========================================================================
    // CRUD DE DESPENSA
    // ========================================================================

    fun getAllPantryItems(): List<PantryItem> {
        val list = mutableListOf<PantryItem>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM pantry_items ORDER BY name COLLATE NOCASE ASC", null)
        cursor.use { c ->
            val storeColIdx = c.getColumnIndex("store_name")
            while (c.moveToNext()) {
                list.add(
                    PantryItem(
                        id = c.getLong(c.getColumnIndexOrThrow("id")),
                        name = c.getString(c.getColumnIndexOrThrow("name")),
                        storeName = if (storeColIdx >= 0) (c.getString(storeColIdx) ?: "") else "",
                        defaultUnit = runCatching {
                            MeasureUnit.valueOf(c.getString(c.getColumnIndexOrThrow("default_unit")))
                        }.getOrDefault(MeasureUnit.GRAMS),
                        amountPerPiece = c.getDouble(c.getColumnIndexOrThrow("amount_per_piece")),
                        pieceEquivalenceUnit = runCatching {
                            PieceEquivalenceUnit.valueOf(c.getString(c.getColumnIndexOrThrow("piece_equiv_unit")))
                        }.getOrDefault(PieceEquivalenceUnit.GRAMS),
                        hasPrice = c.getInt(c.getColumnIndexOrThrow("has_price")) == 1,
                        priceCost = c.getDouble(c.getColumnIndexOrThrow("price_cost")),
                        priceReferenceAmount = c.getDouble(c.getColumnIndexOrThrow("price_ref_amount")),
                        priceReferenceUnit = runCatching {
                            MeasureUnit.valueOf(c.getString(c.getColumnIndexOrThrow("price_ref_unit")))
                        }.getOrDefault(MeasureUnit.GRAMS),
                        hasNutrition = c.getInt(c.getColumnIndexOrThrow("has_nutrition")) == 1,
                        nutritionPer100gOrMl = NutritionInfo(
                            caloriesKcal = c.getDouble(c.getColumnIndexOrThrow("calories_kcal")),
                            proteinG = c.getDouble(c.getColumnIndexOrThrow("protein_g")),
                            carbsG = c.getDouble(c.getColumnIndexOrThrow("carbs_g")),
                            sugarsG = c.getDouble(c.getColumnIndexOrThrow("sugars_g")),
                            totalFatG = c.getDouble(c.getColumnIndexOrThrow("total_fat_g")),
                            saturatedFatG = c.getDouble(c.getColumnIndexOrThrow("saturated_fat_g")),
                            fiberG = c.getDouble(c.getColumnIndexOrThrow("fiber_g")),
                            sodiumMg = c.getDouble(c.getColumnIndexOrThrow("sodium_mg"))
                        ),
                        photoPath = c.getString(c.getColumnIndexOrThrow("photo_path"))
                    )
                )
            }
        }
        return list
    }

    fun savePantryItem(item: PantryItem): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("name", item.name)
            put("store_name", item.storeName)
            put("default_unit", item.defaultUnit.name)
            put("amount_per_piece", item.amountPerPiece)
            put("piece_equiv_unit", item.pieceEquivalenceUnit.name)
            put("has_price", if (item.hasPrice) 1 else 0)
            put("price_cost", item.priceCost)
            put("price_ref_amount", item.priceReferenceAmount)
            put("price_ref_unit", item.priceReferenceUnit.name)
            put("has_nutrition", if (item.hasNutrition) 1 else 0)
            put("calories_kcal", item.nutritionPer100gOrMl.caloriesKcal)
            put("protein_g", item.nutritionPer100gOrMl.proteinG)
            put("carbs_g", item.nutritionPer100gOrMl.carbsG)
            put("sugars_g", item.nutritionPer100gOrMl.sugarsG)
            put("total_fat_g", item.nutritionPer100gOrMl.totalFatG)
            put("saturated_fat_g", item.nutritionPer100gOrMl.saturatedFatG)
            put("fiber_g", item.nutritionPer100gOrMl.fiberG)
            put("sodium_mg", item.nutritionPer100gOrMl.sodiumMg)
            put("photo_path", item.photoPath)
        }
        return if (item.id == 0L) {
            db.insert("pantry_items", null, values)
        } else {
            db.update("pantry_items", values, "id = ?", arrayOf(item.id.toString()))
            item.id
        }
    }

    fun deletePantryItem(id: Long) {
        writableDatabase.delete("pantry_items", "id = ?", arrayOf(id.toString()))
    }

    // ========================================================================
    // CRUD DE RECETAS Y PLATILLOS
    // ========================================================================

    fun getAllRecipes(): List<Recipe> {
        val list = mutableListOf<Recipe>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM recipes ORDER BY name COLLATE NOCASE ASC", null)
        cursor.use { c ->
            val pieceColIdx = c.getColumnIndex("grams_per_piece")
            while (c.moveToNext()) {
                val componentsJson = c.getString(c.getColumnIndexOrThrow("components_json"))
                list.add(
                    Recipe(
                        id = c.getLong(c.getColumnIndexOrThrow("id")),
                        name = c.getString(c.getColumnIndexOrThrow("name")),
                        notes = c.getString(c.getColumnIndexOrThrow("notes")),
                        isBakersPercentage = c.getInt(c.getColumnIndexOrThrow("is_bakers_percentage")) == 1,
                        defaultTargetWeightGrams = c.getDouble(c.getColumnIndexOrThrow("default_target_weight_grams")),
                        gramsPerPiece = if (pieceColIdx >= 0) c.getDouble(pieceColIdx).coerceAtLeast(1.0) else 100.0,
                        components = parseComponentsJson(componentsJson)
                    )
                )
            }
        }
        return list
    }

    fun saveRecipe(recipe: Recipe): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("name", recipe.name)
            put("notes", recipe.notes)
            put("is_bakers_percentage", if (recipe.isBakersPercentage) 1 else 0)
            put("default_target_weight_grams", recipe.defaultTargetWeightGrams)
            put("grams_per_piece", recipe.gramsPerPiece)
            put("components_json", serializeComponentsJson(recipe.components))
        }
        return if (recipe.id == 0L) {
            db.insert("recipes", null, values)
        } else {
            db.update("recipes", values, "id = ?", arrayOf(recipe.id.toString()))
            recipe.id
        }
    }

    fun deleteRecipe(id: Long) {
        writableDatabase.delete("recipes", "id = ?", arrayOf(id.toString()))
    }

    private fun serializeComponentsJson(components: List<RecipeComponent>): String {
        val arr = JSONArray()
        for (comp in components) {
            val obj = JSONObject().apply {
                put("sourceType", comp.sourceType.name)
                put("referenceId", comp.referenceId)
                put("amountOrPercentage", comp.amountOrPercentage)
                put("unit", comp.unit.name)
                put("isBaseFlourInBakersMode", comp.isBaseFlourInBakersMode)
                put("weightRetentionPercent", comp.weightRetentionPercent)
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun parseComponentsJson(jsonStr: String): List<RecipeComponent> {
        val result = mutableListOf<RecipeComponent>()
        runCatching {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                result.add(
                    RecipeComponent(
                        sourceType = runCatching {
                            ComponentSourceType.valueOf(obj.getString("sourceType"))
                        }.getOrDefault(ComponentSourceType.PANTRY_INGREDIENT),
                        referenceId = obj.getLong("referenceId"),
                        amountOrPercentage = obj.getDouble("amountOrPercentage"),
                        unit = runCatching {
                            MeasureUnit.valueOf(obj.optString("unit", MeasureUnit.GRAMS.name))
                        }.getOrDefault(MeasureUnit.GRAMS),
                        isBaseFlourInBakersMode = obj.optBoolean("isBaseFlourInBakersMode", false),
                        weightRetentionPercent = obj.optDouble("weightRetentionPercent", 100.0)
                    )
                )
            }
        }
        return result
    }

    // ========================================================================
    // CONFIGURACIÓN: GEMINI API KEY Y PERSONALIZACIÓN AVANZADA DE LA APP
    // ========================================================================

    fun getGeminiApiKey(): String {
        return prefs.getString("gemini_api_key", "") ?: ""
    }

    fun setGeminiApiKey(apiKey: String) {
        prefs.edit().putString("gemini_api_key", apiKey.trim()).apply()
    }

    /**
     * Lee la configuración visual personalizada (`AppUiConfig`) desde `SharedPreferences`.
     */
    fun getUiConfig(): AppUiConfig {
        val raw = prefs.getString("ui_config_json", null) ?: return AppUiConfig()
        return runCatching {
            val obj = JSONObject(raw)
            val defaultCfg = AppUiConfig()

            val enabledSet = mutableSetOf<String>()
            val secArr = obj.optJSONArray("enabledSectionIds")
            if (secArr != null) {
                for (i in 0 until secArr.length()) {
                    enabledSet.add(secArr.getString(i))
                }
            } else {
                enabledSet.addAll(defaultCfg.enabledSectionIds)
            }
            // La pestaña de Configuración NUNCA puede desactivarse
            enabledSet.add(AppSection.SETTINGS.id)

            // Leemos el diccionario de textos personalizados (`customTexts`)
            val customMap = mutableMapOf<String, String>()
            val textsObj = obj.optJSONObject("customTexts")
            if (textsObj != null) {
                val keys = textsObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    customMap[k] = textsObj.getString(k)
                }
            }

            AppUiConfig(
                enabledSectionIds = enabledSet,
                maxItemsPerBottomRow = obj.optInt("maxItemsPerBottomRow", defaultCfg.maxItemsPerBottomRow).coerceIn(2, 6),
                customTexts = customMap,
                showCalories = obj.optBoolean("showCalories", true),
                showProtein = obj.optBoolean("showProtein", true),
                showTotalFat = obj.optBoolean("showTotalFat", true),
                showSaturatedFat = obj.optBoolean("showSaturatedFat", true),
                showCarbs = obj.optBoolean("showCarbs", true),
                showSugars = obj.optBoolean("showSugars", true),
                showFiber = obj.optBoolean("showFiber", true),
                showSodium = obj.optBoolean("showSodium", true)
            )
        }.getOrDefault(AppUiConfig())
    }

    /**
     * Guarda la configuración visual personalizada (`AppUiConfig`) en `SharedPreferences`.
     */
    fun saveUiConfig(config: AppUiConfig) {
        val safeEnabled = config.enabledSectionIds.toMutableSet().apply {
            add(AppSection.SETTINGS.id)
        }
        val textsObj = JSONObject()
        for ((k, v) in config.customTexts) {
            textsObj.put(k, v)
        }

        val obj = JSONObject().apply {
            put("enabledSectionIds", JSONArray(safeEnabled.toList()))
            put("maxItemsPerBottomRow", config.maxItemsPerBottomRow.coerceIn(2, 6))
            put("customTexts", textsObj)
            put("showCalories", config.showCalories)
            put("showProtein", config.showProtein)
            put("showTotalFat", config.showTotalFat)
            put("showSaturatedFat", config.showSaturatedFat)
            put("showCarbs", config.showCarbs)
            put("showSugars", config.showSugars)
            put("showFiber", config.showFiber)
            put("showSodium", config.showSodium)
        }
        prefs.edit().putString("ui_config_json", obj.toString()).apply()
    }
}
