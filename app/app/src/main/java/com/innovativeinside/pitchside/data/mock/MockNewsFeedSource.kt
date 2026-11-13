package com.innovativeinside.pitchside.data.mock

import com.innovativeinside.pitchside.core.AppClock
import com.innovativeinside.pitchside.core.AppError
import com.innovativeinside.pitchside.data.model.NewsCategory
import com.innovativeinside.pitchside.data.model.NewsEdition
import com.innovativeinside.pitchside.data.model.NewsItem
import com.innovativeinside.pitchside.data.news.NewsFeedSource

/**
 * Offline stand-in for the 90min feeds, used by previews and UI tests (never by the shipped app, which
 * always reads the real RSS). Shares [MockBehavior] so tests can simulate offline and server errors.
 */
class MockNewsFeedSource(private val clock: AppClock, private val behavior: MockBehavior = MockBehavior()) : NewsFeedSource {
    override suspend fun fetch(edition: NewsEdition): List<NewsItem> {
        if (behavior.offline) throw AppError.Network()
        if (behavior.serverErrors) throw AppError.Server(503)
        val day = 86_400_000L
        val now = clock.nowMs()
        val stories = when (edition) {
            NewsEdition.France -> listOf(
                "France - Espagne : les Bleus affichent leurs ambitions avant la demi" to "La conférence de presse avant la demi-finale.",
                "Mercato : un défenseur central dans le viseur de Marseille" to "Les discussions avancent, rien n'est signé.",
            )
            NewsEdition.Italy -> listOf(
                "Serie A, le pagelle della giornata: chi sale e chi scende" to "Voti e giudizi dopo l'ultimo turno.",
                "Calciomercato: il punto sulle trattative di fine stagione" to "Tutte le novità del giorno.",
            )
            NewsEdition.Spain -> listOf(
                "LaLiga: las claves de la jornada en cinco datos" to "Lo que dejó el fin de semana.",
                "Mercado de fichajes: los nombres que suenan para el lateral" to "Primeros contactos sin acuerdo.",
            )
            NewsEdition.Brazil -> listOf(
                "Brasileirão: o que aprendemos com a rodada do fim de semana" to "Cinco lições da rodada.",
                "Mercado da bola: zagueiro entra na mira de clube europeu" to "Negociação ainda em fase inicial.",
            )
        }
        return stories.mapIndexed { i, (title, dek) ->
            NewsItem(
                id = "${edition.code}-sample$i", category = NewsCategory.Reports, title = title, dek = dek, body = "",
                source = "90min", publishedMs = now - (edition.ordinal * 3 + i + 1) * day / 4, readMinutes = 1,
                url = "https://www.90min.com/${edition.path.removeSuffix("/feed")}", imageUrl = null, imageCaption = null,
                edition = edition, author = "90min", language = edition.language,
            )
        }
    }
}
