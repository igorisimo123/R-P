package com.example.rp.data

import kotlin.math.roundToInt

/**
 * ============================================================================
 * ARCHIVO: Models.kt
 * ============================================================================
 * Aquí definimos las estructuras de datos (`data class`, equivalentes a `struct` en C++
 * o `@dataclass` en Python), el diccionario completo de textos personalizables (`UiTextKey`),
 * la configuración personalizable de la interfaz (`AppUiConfig`), las secciones (`AppSection`)
 * y el motor matemático (`RecipeCalculator`).
 */

/**
 * Secciones (Apartados) disponibles en la aplicación.
 * `SETTINGS` (Configuración) es la única sección que NO se puede desactivar, sus textos
 * no se modifican y siempre se dibuja en la esquina inferior derecha de la barra inferior.
 */
enum class AppSection(
    val id: String,
    val defaultTabLabel: String,
    val defaultTabIcon: String,
    val defaultTopTitle: String,
    val canBeDisabled: Boolean = true
) {
    RECIPES(
        id = "RECIPES",
        defaultTabLabel = "Recetas",
        defaultTabIcon = "🍲",
        defaultTopTitle = "🍲 Mis Recetas y Platillos",
        canBeDisabled = true
    ),
    PANTRY(
        id = "PANTRY",
        defaultTabLabel = "Despensa",
        defaultTabIcon = "🧺",
        defaultTopTitle = "🧺 Mi Despensa",
        canBeDisabled = true
    ),
    SETTINGS(
        id = "SETTINGS",
        defaultTabLabel = "Configuración",
        defaultTabIcon = "⚙️",
        defaultTopTitle = "⚙️ Configuración",
        canBeDisabled = false // Siempre activa y fija en la esquina inferior derecha
    )
}

/**
 * Catálogo completo de TODOS los símbolos y textos por defecto de la aplicación
 * (excepto los de la pestaña de Configuración, que permanecen fijos por seguridad).
 *
 * En Python esto sería un diccionario global de textos `DEFAULT_TEXTS = { ... }`.
 * El cliente puede modificar cualquiera de estos textos desde Configuración -> Opciones Avanzadas.
 */
enum class UiTextKey(
    val category: String,
    val description: String,
    val defaultText: String
) {
    // --- CATEGORÍA 1: SÍMBOLOS E ÍCONOS ---
    SYMBOL_PRICE("Símbolos", "Símbolo de Precio registrado", "💰"),
    SYMBOL_NUTRITION("Símbolos", "Símbolo de Nutrición registrada", "🧠"),
    SYMBOL_STORE("Símbolos", "Símbolo de Supermercado / Tienda", "🛒"),
    SYMBOL_SUB_RECIPE("Símbolos", "Símbolo de Sub-receta", "🍲"),
    SYMBOL_BAKERY("Símbolos", "Símbolo de Modo Panadería", "🥖"),
    SYMBOL_DISH("Símbolos", "Símbolo de Platillo Estándar", "🍽️"),
    SYMBOL_PIECE("Símbolos", "Símbolo de Piezas / Pan", "🥐"),
    SYMBOL_SCALE("Símbolos", "Símbolo de Báscula / Peso", "⚖️"),
    SYMBOL_RETENTION("Símbolos", "Símbolo de Absorción / Cocción", "💧/🍟"),
    SYMBOL_CATALOG("Símbolos", "Símbolo de Catálogo", "📦"),

    // --- CATEGORÍA 2: PESTAÑAS Y TÍTULOS (EXCEPTO CONFIGURACIÓN) ---
    RECIPES_TAB_ICON("Pestañas y Títulos", "Ícono de pestaña Recetas", "🍲"),
    RECIPES_TAB_LABEL("Pestañas y Títulos", "Nombre de pestaña Recetas", "Recetas"),
    RECIPES_TOP_TITLE("Pestañas y Títulos", "Título superior de Recetas", "🍲 Mis Recetas y Platillos"),
    PANTRY_TAB_ICON("Pestañas y Títulos", "Ícono de pestaña Despensa", "🧺"),
    PANTRY_TAB_LABEL("Pestañas y Títulos", "Nombre de pestaña Despensa", "Despensa"),
    PANTRY_TOP_TITLE("Pestañas y Títulos", "Título superior de Despensa", "🧺 Mi Despensa"),

    // --- CATEGORÍA 3: BOTONES GENERALES ---
    BTN_SAVE("Botones Generales", "Botón Guardar", "Guardar"),
    BTN_EDIT("Botones Generales", "Botón Editar", "Editar"),
    BTN_DELETE("Botones Generales", "Botón Borrar", "Borrar"),
    BTN_CANCEL("Botones Generales", "Botón Cancelar", "Cancelar"),
    BTN_REMOVE("Botones Generales", "Botón Quitar de una lista", "Quitar"),
    BTN_CLOSE("Botones Generales", "Botón Cerrar ventana", "Cerrar"),
    BTN_IMPORT("Botones Generales", "Botón Importar del catálogo", "Importar"),
    BTN_ALREADY_ADDED("Botones Generales", "Texto cuando ya fue importado", "Agregado"),

    // --- CATEGORÍA 4: PANTALLA DE RECETAS Y TARJETAS ---
    RECIPES_FAB_NEW("Pantalla Recetas", "Botón flotante Nueva Receta", "Nueva Receta / Platillo"),
    RECIPES_EMPTY_MSG(
        "Pantalla Recetas",
        "Mensaje cuando no hay recetas guardadas",
        "Aún no tienes recetas guardadas. Pulsa '+ Nueva Receta / Platillo' para crear una receta normal, un pan con porcentaje panadero o un platillo combinado."
    ),
    RECIPE_SUBTITLE_BAKERY("Pantalla Recetas", "Subtítulo en tarjeta de Panadería", "Panadería (% sobre Harina = 100%)"),
    RECIPE_SUBTITLE_STANDARD("Pantalla Recetas", "Subtítulo en tarjeta de Platillo normal", "Platillo / Receta Estándar"),
    RECIPE_SCALE_TITLE("Pantalla Recetas", "Título de calculadora de masa", "¿Cómo quieres calcular la masa hoy?"),
    RECIPE_SCALE_BY_GRAMS_CHIP("Pantalla Recetas", "Botón escalar por gramos de mezcla", "Por Gramos de Mezcla"),
    RECIPE_SCALE_BY_PIECES_CHIP("Pantalla Recetas", "Botón escalar por cantidad de piezas", "Por Cantidad de Piezas"),
    RECIPE_MIX_GRAMS_LABEL("Pantalla Recetas", "Campo gramos de mezcla (g)", "Gramos de mezcla (g)"),
    RECIPE_HOW_MANY_PIECES_LABEL("Pantalla Recetas", "Campo ¿Cuántas piezas?", "¿Cuántas piezas?"),
    RECIPE_GRAMS_PER_PIECE_LABEL("Pantalla Recetas", "Campo Gramos por 1 pieza (g)", "Gramos por 1 pz (g)"),
    RECIPE_TOTAL_MIX_PREFIX("Pantalla Recetas", "Texto 'Mezcla total a preparar:'", "👉 Mezcla total a preparar:"),
    RECIPE_INGREDIENTS_HEADER("Pantalla Recetas", "Encabezado 'Ingredientes'", "Ingredientes"),
    RECIPE_EXPAND_BREAKDOWN("Pantalla Recetas", "Botón para mostrar desglose de receta", "▶️ Desglosar receta"),
    RECIPE_COLLAPSE_BREAKDOWN("Pantalla Recetas", "Botón para ocultar desglose de receta", "🔽 Ocultar desglose"),
    RECIPE_SUB_EXPAND("Pantalla Recetas", "Botón para desglosar una sub-receta", "🔽 Desglosar ingredientes de esta receta"),
    RECIPE_SUB_COLLAPSE("Pantalla Recetas", "Botón para ocultar desglose de sub-receta", "🔼 Ocultar ingredientes de esta receta"),
    RECIPE_SUB_PROPORTIONAL_HEADER("Pantalla Recetas", "Encabezado del desglose proporcional", "Desglose proporcional para"),
    RECIPE_FINAL_WEIGHT_LABEL("Pantalla Recetas", "Etiqueta 'Peso final:'", "Peso final:"),
    RECIPE_PIECES_SUMMARY_LABEL("Pantalla Recetas", "Etiqueta 'Piezas:' en resumen", "Piezas:"),
    RECIPE_APPROX_COST_LABEL("Pantalla Recetas", "Etiqueta 'Costo aprox:'", "Costo aprox:"),
    RECIPE_SHOW_NUTRITION_BTN("Pantalla Recetas", "Botón Ver Tabla Nutrimental", "Ver Tabla Nutrimental Aproximada"),
    RECIPE_HIDE_NUTRITION_BTN("Pantalla Recetas", "Botón Ocultar Tabla Nutrimental", "Ocultar Tabla Nutrimental"),
    NUTRITION_TABLE_TITLE("Pantalla Recetas", "Título de la Tabla Nutrimental", "Información Nutrimental Aproximada"),
    NUTRITION_TABLE_SUBTITLE("Pantalla Recetas", "Subtítulo de la Tabla Nutrimental", "Calculada según el peso final que queda en el platillo"),
    NUTRITION_COL_NUTRIENT("Pantalla Recetas", "Columna 'Nutriente'", "Nutriente"),
    NUTRITION_COL_PER100G("Pantalla Recetas", "Columna 'Por 100g'", "Por 100g"),
    NUTRITION_COL_TOTAL("Pantalla Recetas", "Columna 'Total'", "Total"),

    // --- CATEGORÍA 5: EDITOR DE RECETAS / PLATILLOS ---
    RECIPE_DIALOG_NEW_TITLE("Editor de Recetas", "Título ventana Nueva Receta", "Nueva Receta o Platillo"),
    RECIPE_DIALOG_EDIT_TITLE("Editor de Recetas", "Título ventana Editar Receta", "Editar Receta o Platillo"),
    RECIPE_FIELD_NAME("Editor de Recetas", "Campo nombre de la receta", "Nombre de la receta o platillo"),
    RECIPE_FIELD_NOTES("Editor de Recetas", "Campo notas / preparación", "Notas / Preparación (opcional)"),
    RECIPE_BAKERS_MODE_TITLE("Editor de Recetas", "Título switch Modo Porcentaje Panadero", "Modo Porcentaje Panadero"),
    RECIPE_BAKERS_MODE_DESC(
        "Editor de Recetas",
        "Descripción del Modo Porcentaje Panadero",
        "Harina base = 100%, los demás ingredientes van en % respecto a la harina."
    ),
    RECIPE_BASE_MIX_FIELD("Editor de Recetas", "Campo Mezcla base (g)", "Mezcla base (g)"),
    RECIPE_ADD_COMPONENT_HEADER("Editor de Recetas", "Encabezado agregar ingrediente o sub-receta", "Agregar ingrediente o sub-receta:"),
    RECIPE_FROM_PANTRY_CHIP("Editor de Recetas", "Chip elegir de Despensa", "De Despensa"),
    RECIPE_FROM_SUBRECIPE_CHIP("Editor de Recetas", "Chip elegir de otra Receta", "De otra Receta"),
    RECIPE_SELECT_INGREDIENT_LABEL("Editor de Recetas", "Texto 'Selecciona el ingrediente:'", "Selecciona el ingrediente:"),
    RECIPE_SELECT_SUBRECIPE_LABEL("Editor de Recetas", "Texto 'Selecciona la receta a incluir:'", "Selecciona la receta a incluir:"),
    RECIPE_PERCENTAGE_FIELD_LABEL(
        "Editor de Recetas",
        "Campo porcentaje en modo panadería",
        "Porcentaje respecto a harina (%) - Ej. 100 harina, 65 agua"
    ),
    RECIPE_AMOUNT_FIELD_LABEL("Editor de Recetas", "Campo cantidad a usar en modo normal", "Cantidad a usar"),
    RECIPE_RETENTION_TITLE(
        "Editor de Recetas",
        "Título porcentaje que influye en el peso final",
        "¿Cuánto influye en el peso final y nutrición? (%)"
    ),
    RECIPE_RETENTION_DESC(
        "Editor de Recetas",
        "Explicación de retención de peso (aceite/agua)",
        "Ej. Aceite para freír o agua para hervir papas: el precio cobra el 100% usado, pero en el peso final y tabla nutrimental solo cuenta este %."
    ),
    RECIPE_RETENTION_FIELD_LABEL("Editor de Recetas", "Campo % que queda en el platillo", "% que queda en el platillo (100% = todo)"),
    RECIPE_BASE_FLOUR_CHECKBOX("Editor de Recetas", "Checkbox 'Es la harina base (100%)'", "Es la harina base (100%)"),
    RECIPE_ADD_TO_LIST_BTN("Editor de Recetas", "Botón agregar componente a la receta", "➕ Agregar a la lista de esta receta"),
    RECIPE_CURRENT_COMPONENTS_HEADER("Editor de Recetas", "Encabezado 'Componentes actuales'", "Componentes actuales"),

    // --- CATEGORÍA 6: PANTALLA DE DESPENSA Y EDITOR DE INGREDIENTES ---
    PANTRY_FAB_CATALOG("Pantalla Despensa", "Botón flotante Importar de Catálogo", "Importar de Catálogo (Verduras/Básicos)"),
    PANTRY_FAB_NEW("Pantalla Despensa", "Botón flotante Nuevo Ingrediente", "Nuevo Ingrediente"),
    PANTRY_LEGEND_PREFIX("Pantalla Despensa", "Prefijo de la leyenda en Despensa", "Leyenda:"),
    PANTRY_FILTER_ALL("Pantalla Despensa", "Chip filtro 'Todos' en Despensa", "Todos"),
    PANTRY_STORE_PREFIX("Pantalla Despensa", "Prefijo 'Súper:' en tarjetas", "Súper:"),
    PANTRY_UNIT_PIECES_PREFIX("Pantalla Despensa", "Texto 'Se mide por Piezas'", "Se mide por Piezas"),
    PANTRY_UNIT_BASE_PREFIX("Pantalla Despensa", "Texto 'Unidad base:'", "Unidad base:"),
    PANTRY_COST_PREFIX("Pantalla Despensa", "Texto 'Costo:' en tarjeta de ingrediente", "Costo:"),
    PANTRY_DIALOG_NEW_TITLE("Editor de Despensa", "Título ventana Nuevo Ingrediente", "Nuevo Ingrediente en Despensa"),
    PANTRY_DIALOG_EDIT_TITLE("Editor de Despensa", "Título ventana Editar Ingrediente", "Editar Ingrediente"),
    PANTRY_FIELD_NAME("Editor de Despensa", "Campo nombre del ingrediente", "Nombre del ingrediente (ej. Harina, Huevo, Carne)"),
    PANTRY_FIELD_STORE("Editor de Despensa", "Campo en qué súper o tienda lo compras", "¿En qué súper o tienda lo compras? (Opcional)"),
    PANTRY_HOW_MEASURED_LABEL("Editor de Despensa", "Pregunta '¿Cómo se mide normalmente?'", "¿Cómo se mide normalmente?"),
    PANTRY_PIECE_EQUIV_TITLE(
        "Editor de Despensa",
        "Pregunta equivalencia de 1 pieza en gramos o ml",
        "¿Cuántos gramos o mililitros pesa/ocupa 1 pieza en promedio?"
    ),
    PANTRY_PIECE_EQUIV_FIELD("Editor de Despensa", "Campo '1 pieza equivale a:'", "1 pieza equivale a:"),
    PANTRY_ADD_PHOTO_BTN("Editor de Despensa", "Botón Agregar Foto (Opcional)", "🖼️ Agregar Foto (Opcional)"),
    PANTRY_CHANGE_PHOTO_BTN("Editor de Despensa", "Botón Cambiar Foto", "🖼️ Cambiar Foto"),
    PANTRY_PHOTO_SAVED_LABEL("Editor de Despensa", "Texto '✅ Foto guardada'", "✅ Foto guardada"),
    PANTRY_REGISTER_PRICE_TITLE("Editor de Despensa", "Switch Registrar Precio", "Registrar Precio"),
    PANTRY_PRICE_COST_FIELD("Editor de Despensa", "Campo Costo ($)", "Costo ($)"),
    PANTRY_PRICE_REF_FIELD("Editor de Despensa", "Campo Por cantidad", "Por cantidad"),
    PANTRY_REGISTER_NUTRITION_TITLE("Editor de Despensa", "Switch Información Nutrimental", "Información Nutrimental (por 100g/ml)"),
    PANTRY_GEMINI_SCAN_HEADER(
        "Editor de Despensa",
        "Encabezado escáner Gemini IA en ingrediente",
        "🤖 Autocompletar con Gemini IA (baja la resolución a 768px para ahorrar créditos):"
    ),
    PANTRY_SCAN_GALLERY_BTN("Editor de Despensa", "Botón Foto de Galería para Gemini", "🖼️ Foto de Galería"),
    PANTRY_SCAN_CAMERA_BTN("Editor de Despensa", "Botón Tomar Foto para Gemini", "📷 Tomar Foto"),
    PANTRY_NUTRITION_VALUES_HEADER("Editor de Despensa", "Texto 'Valores por cada 100g o 100ml:'", "Valores por cada 100g o 100ml:"),
    PANTRY_CATALOG_DIALOG_TITLE("Editor de Despensa", "Título ventana Catálogo Integrado", "Catálogo Integrado de Ingredientes")
}

/**
 * Configuración visual y de comportamiento de la app que el usuario puede modificar
 * desde el apartado de Configuración -> Opciones Avanzadas.
 */
data class AppUiConfig(
    val enabledSectionIds: Set<String> = setOf(
        AppSection.RECIPES.id,
        AppSection.PANTRY.id,
        AppSection.SETTINGS.id
    ),
    val maxItemsPerBottomRow: Int = 4,

    // Diccionario de textos personalizados (`Map<String, String>`).
    // Si una clave no está en `customTexts`, se usa su `defaultText` de `UiTextKey`.
    val customTexts: Map<String, String> = emptyMap(),

    // Switches para activar o desactivar partes de la Tabla Nutrimental
    val showCalories: Boolean = true,
    val showProtein: Boolean = true,
    val showTotalFat: Boolean = true,
    val showSaturatedFat: Boolean = true,
    val showCarbs: Boolean = true,
    val showSugars: Boolean = true,
    val showFiber: Boolean = true,
    val showSodium: Boolean = true
) {
    /**
     * Devuelve el texto personalizado por el usuario para `key`, o el texto por defecto si no lo ha modificado.
     * En Python equivale a: `custom_texts.get(key.name, key.default_text)`
     */
    fun text(key: UiTextKey): String {
        return customTexts[key.name] ?: key.defaultText
    }

    /**
     * Crea una copia de `AppUiConfig` actualizando un texto específico.
     */
    fun withCustomText(key: UiTextKey, newValue: String): AppUiConfig {
        val nextMap = customTexts.toMutableMap()
        if (newValue == key.defaultText) {
            nextMap.remove(key.name)
        } else {
            nextMap[key.name] = newValue
        }
        return copy(customTexts = nextMap)
    }

    // Propiedades de acceso rápido para los símbolos más usados
    val priceSymbol: String get() = text(UiTextKey.SYMBOL_PRICE)
    val nutritionSymbol: String get() = text(UiTextKey.SYMBOL_NUTRITION)
    val storeSymbol: String get() = text(UiTextKey.SYMBOL_STORE)
    val subRecipeSymbol: String get() = text(UiTextKey.SYMBOL_SUB_RECIPE)
    val bakerySymbol: String get() = text(UiTextKey.SYMBOL_BAKERY)
    val dishSymbol: String get() = text(UiTextKey.SYMBOL_DISH)
}

/**
 * Unidad de medida principal de un ingrediente.
 */
enum class MeasureUnit(val symbol: String, val label: String) {
    GRAMS("g", "Gramos (g)"),
    MILLILITERS("ml", "Mililitros (ml)"),
    PIECES("pz", "Piezas (pz)")
}

/**
 * Si un ingrediente se mide por PIEZAS, indica si su equivalencia promedio está en gramos o mililitros.
 */
enum class PieceEquivalenceUnit(val symbol: String, val label: String) {
    GRAMS("g", "Gramos por pieza"),
    MILLILITERS("ml", "Mililitros por pieza")
}

/**
 * Tabla nutrimental estandarizada por cada 100g o 100ml del producto.
 */
data class NutritionInfo(
    val caloriesKcal: Double = 0.0,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val sugarsG: Double = 0.0,
    val totalFatG: Double = 0.0,
    val saturatedFatG: Double = 0.0,
    val fiberG: Double = 0.0,
    val sodiumMg: Double = 0.0
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
 * Representa un ingrediente guardado en la Despensa.
 */
data class PantryItem(
    val id: Long = 0L,
    val name: String,
    val storeName: String = "",
    val defaultUnit: MeasureUnit = MeasureUnit.GRAMS,
    val amountPerPiece: Double = 100.0,
    val pieceEquivalenceUnit: PieceEquivalenceUnit = PieceEquivalenceUnit.GRAMS,
    val hasPrice: Boolean = false,
    val priceCost: Double = 0.0,
    val priceReferenceAmount: Double = 1000.0,
    val priceReferenceUnit: MeasureUnit = MeasureUnit.GRAMS,
    val hasNutrition: Boolean = false,
    val nutritionPer100gOrMl: NutritionInfo = NutritionInfo(),
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
 * Renglón individual dentro de una receta o platillo.
 */
data class RecipeComponent(
    val sourceType: ComponentSourceType = ComponentSourceType.PANTRY_INGREDIENT,
    val referenceId: Long,
    val amountOrPercentage: Double,
    val unit: MeasureUnit = MeasureUnit.GRAMS,
    val isBaseFlourInBakersMode: Boolean = false,
    val weightRetentionPercent: Double = 100.0
)

/**
 * Receta o Platillo completo.
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
 * Resultado ya calculado de un renglón para que la interfaz gráfica (`RecetasApp.kt`) solo lo dibuje.
 */
data class CalculatedComponentRow(
    val component: RecipeComponent,
    val displayName: String,
    val storeName: String = "",
    val isSubRecipe: Boolean,
    val rawGramsOrMl: Double,
    val exactGramsOrMl: Double,
    val displayAmountText: String,
    val retentionNoteText: String = "",
    val hasPriceIcon: Boolean,
    val hasNutritionIcon: Boolean,
    val calculatedCost: Double,
    val calculatedNutrition: NutritionInfo,
    val subBreakdownRows: List<CalculatedComponentRow> = emptyList()
)

/**
 * Resumen total calculado de toda una receta o platillo.
 */
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
 * Motor matemático de recetas.
 */
object RecipeCalculator {

    fun calculateRecipe(
        recipe: Recipe,
        overrideTargetWeightGrams: Double? = null,
        pantryMap: Map<Long, PantryItem>,
        recipeMap: Map<Long, Recipe>,
        subRecipeSymbol: String = "🍲",
        scaleSymbol: String = "⚖️",
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
            recipe.components.sumOf {
                it.amountOrPercentage * (it.weightRetentionPercent.coerceAtLeast(0.0) / 100.0)
            }.coerceAtLeast(0.0001)
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
            val retentionRatio = (comp.weightRetentionPercent.coerceAtLeast(0.0)) / 100.0

            when (comp.sourceType) {
                ComponentSourceType.PANTRY_INGREDIENT -> {
                    val item = pantryMap[comp.referenceId]
                    if (item != null) {
                        val rawGramsOrMl: Double
                        val displayAmount: String

                        if (recipe.isBakersPercentage) {
                            rawGramsOrMl = targetTotalGrams * (comp.amountOrPercentage / totalPercentageSum)
                            val pctStr = formatNumber(comp.amountOrPercentage)
                            val gStr = formatNumber(rawGramsOrMl)
                            val pieceHint = if (item.defaultUnit == MeasureUnit.PIECES && item.amountPerPiece > 0) {
                                " (~${formatNumber(item.gramsOrMlToPieces(rawGramsOrMl))} pz)"
                            } else ""
                            val unitSym = if (item.defaultUnit == MeasureUnit.PIECES) item.pieceEquivalenceUnit.symbol else item.defaultUnit.symbol
                            displayAmount = "$pctStr% → $gStr $unitSym$pieceHint"
                        } else {
                            rawGramsOrMl = item.toGramsOrMl(comp.amountOrPercentage, comp.unit)
                            val amtStr = formatNumber(comp.amountOrPercentage)
                            displayAmount = if (comp.unit == MeasureUnit.PIECES) {
                                "$amtStr pz (${formatNumber(rawGramsOrMl)} ${item.pieceEquivalenceUnit.symbol})"
                            } else {
                                "$amtStr ${comp.unit.symbol}"
                            }
                        }

                        val retainedGramsOrMl = rawGramsOrMl * retentionRatio
                        val cost = item.calculateCost(rawGramsOrMl, MeasureUnit.GRAMS)
                        val nutrition = item.calculateNutrition(retainedGramsOrMl, MeasureUnit.GRAMS)

                        if (item.hasPrice) anyPrice = true
                        if (item.hasNutrition) anyNutrition = true

                        sumWeight += retainedGramsOrMl
                        sumCost += cost
                        sumNutrition = sumNutrition + nutrition

                        val unitSym = if (item.defaultUnit == MeasureUnit.PIECES) item.pieceEquivalenceUnit.symbol else item.defaultUnit.symbol
                        val retentionNote = if (comp.weightRetentionPercent != 100.0) {
                            "$scaleSymbol Queda en el platillo: ${formatNumber(comp.weightRetentionPercent)}% (${formatNumber(retainedGramsOrMl)} $unitSym) | Costo cobra 100%"
                        } else ""

                        rows.add(
                            CalculatedComponentRow(
                                component = comp,
                                displayName = item.name,
                                storeName = item.storeName,
                                isSubRecipe = false,
                                rawGramsOrMl = rawGramsOrMl,
                                exactGramsOrMl = retainedGramsOrMl,
                                displayAmountText = displayAmount,
                                retentionNoteText = retentionNote,
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
                            subRecipeSymbol = subRecipeSymbol,
                            scaleSymbol = scaleSymbol,
                            visitedRecipeIds = nextVisited
                        )
                        val rawGramsUsed: Double
                        val displayAmount: String

                        if (recipe.isBakersPercentage) {
                            rawGramsUsed = targetTotalGrams * (comp.amountOrPercentage / totalPercentageSum)
                            displayAmount = "${formatNumber(comp.amountOrPercentage)}% → ${formatNumber(rawGramsUsed)} g"
                        } else if (comp.unit == MeasureUnit.PIECES) {
                            val pzWeight = subRecipe.gramsPerPiece.coerceAtLeast(1.0)
                            rawGramsUsed = comp.amountOrPercentage * pzWeight
                            displayAmount = "${formatNumber(comp.amountOrPercentage)} pz (${formatNumber(rawGramsUsed)} g)"
                        } else {
                            rawGramsUsed = comp.amountOrPercentage
                            displayAmount = "${formatNumber(rawGramsUsed)} g"
                        }

                        val retainedGramsUsed = rawGramsUsed * retentionRatio

                        val costRatio = if (subSummary.totalWeightGramsOrMl > 0.0) {
                            rawGramsUsed / subSummary.totalWeightGramsOrMl
                        } else 0.0

                        val nutritionRatio = if (subSummary.totalWeightGramsOrMl > 0.0) {
                            retainedGramsUsed / subSummary.totalWeightGramsOrMl
                        } else 0.0

                        val subCost = subSummary.totalCost * costRatio
                        val subNutrition = subSummary.totalNutrition.scale(nutritionRatio)

                        if (subSummary.anyHasPrice) anyPrice = true
                        if (subSummary.anyHasNutrition) anyNutrition = true

                        sumWeight += retainedGramsUsed
                        sumCost += subCost
                        sumNutrition = sumNutrition + subNutrition

                        val scaledSubRows = subSummary.rows.map { childRow ->
                            val scaledRaw = childRow.rawGramsOrMl * costRatio
                            val scaledRetained = childRow.exactGramsOrMl * nutritionRatio
                            CalculatedComponentRow(
                                component = childRow.component,
                                displayName = childRow.displayName,
                                storeName = childRow.storeName,
                                isSubRecipe = childRow.isSubRecipe,
                                rawGramsOrMl = scaledRaw,
                                exactGramsOrMl = scaledRetained,
                                displayAmountText = "${formatNumber(scaledRetained)} g",
                                retentionNoteText = childRow.retentionNoteText,
                                hasPriceIcon = childRow.hasPriceIcon,
                                hasNutritionIcon = childRow.hasNutritionIcon,
                                calculatedCost = childRow.calculatedCost * costRatio,
                                calculatedNutrition = childRow.calculatedNutrition.scale(nutritionRatio)
                            )
                        }

                        val retentionNote = if (comp.weightRetentionPercent != 100.0) {
                            "$scaleSymbol Queda en el platillo: ${formatNumber(comp.weightRetentionPercent)}% (${formatNumber(retainedGramsUsed)} g) | Costo cobra 100%"
                        } else ""

                        rows.add(
                            CalculatedComponentRow(
                                component = comp,
                                displayName = "$subRecipeSymbol ${subRecipe.name}",
                                storeName = "",
                                isSubRecipe = true,
                                rawGramsOrMl = rawGramsUsed,
                                exactGramsOrMl = retainedGramsUsed,
                                displayAmountText = displayAmount,
                                retentionNoteText = retentionNote,
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
