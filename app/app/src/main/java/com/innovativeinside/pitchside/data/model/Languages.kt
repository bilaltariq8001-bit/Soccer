package com.innovativeinside.pitchside.data.model

/** Languages offered at onboarding and in Settings. Only English UI strings ship today (see FINAL_REPORT.md). */
object Languages {
    val all = listOf(
        Language("en", "English", "English"), Language("ur", "اردو", "Urdu"), Language("ar", "العربية", "Arabic"),
        Language("hi", "हिन्दी", "Hindi"), Language("es", "Español", "Spanish"), Language("pt", "Português", "Portuguese"),
        Language("fr", "Français", "French"), Language("tr", "Türkçe", "Turkish"), Language("de", "Deutsch", "German"),
    )
    fun byCode(code: String) = all.firstOrNull { it.code == code } ?: all.first()
}
