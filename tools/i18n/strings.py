#!/usr/bin/env python3
"""
Textos de Repisa en todos los idiomas. Ejecuta este archivo para regenerar
app/src/main/res/values*/strings.xml:

    python3 tools/i18n/strings.py

Cada entrada: clave, {idioma: texto}. Idiomas base: en, es, fr, de, it, pt (Brasil).
Variantes (es419, enGB, ptPT) solo cuando difieren; si no, se usa la base.
Para añadir un idioma: añade su columna aquí y su carpeta en LOCALES.
"""
import os
import re

LOCALES = {
    "en": "values",            # inglés (EE. UU.): también es el idioma por defecto
    "enGB": "values-en-rGB",
    "es": "values-es",         # español (España)
    "es419": "values-b+es+419",  # español (Latinoamérica)
    "fr": "values-fr",
    "de": "values-de",
    "it": "values-it",
    "pt": "values-pt-rBR",
    "ptPT": "values-pt-rPT",
}
FALLBACK = {"enGB": "en", "es419": "es", "ptPT": "pt"}

S = {}


def s(key, en, es, fr, de, it, pt, **variants):
    S[key] = dict(en=en, es=es, fr=fr, de=de, it=it, pt=pt, **variants)


# ------------------------------------------------------------------ General
s("back", "Back", "Volver", "Retour", "Zurück", "Indietro", "Voltar")
s("close", "Close", "Cerrar", "Fermer", "Schließen", "Chiudi", "Fechar")
s("cancel", "Cancel", "Cancelar", "Annuler", "Abbrechen", "Annulla", "Cancelar")
s("save", "Save", "Guardar", "Enregistrer", "Speichern", "Salva", "Salvar", ptPT="Guardar")
s("search", "Search", "Buscar", "Rechercher", "Suchen", "Cerca", "Buscar", ptPT="Pesquisar")
s("settings", "Settings", "Ajustes", "Paramètres", "Einstellungen", "Impostazioni", "Configurações",
  es419="Configuración", ptPT="Definições")
s("sync", "Sync", "Sincronizar", "Synchroniser", "Synchronisieren", "Sincronizza", "Sincronizar")
s("sync_all", "Sync all", "Sincronizar todo", "Tout synchroniser", "Alles synchronisieren", "Sincronizza tutto",
  "Sincronizar tudo")
s("remove", "Remove", "Quitar", "Retirer", "Entfernen", "Rimuovi", "Remover")
s("done", "Done", "Listo", "Terminé", "Fertig", "Fatto", "Pronto", ptPT="Concluído")
s("ask", "Ask", "Preguntar", "Demander", "Fragen", "Chiedi", "Perguntar")

# ------------------------------------------------------------------ Repisa / barra superior
s("games_count_one", "%1$d game", "%1$d juego", "%1$d jeu", "%1$d Spiel", "%1$d gioco", "%1$d jogo")
s("games_count_other", "%1$d games", "%1$d juegos", "%1$d jeux", "%1$d Spiele", "%1$d giochi", "%1$d jogos")
s("hours_played", "%1$d h played", "%1$d h jugadas", "%1$d h jouées", "%1$d Std. gespielt", "%1$d h giocate",
  "%1$d h jogadas")
s("search_hint", "Search title, genre, studio…", "Buscar título, género, estudio…", "Titre, genre, studio…",
  "Titel, Genre, Studio suchen…", "Cerca titolo, genere, studio…", "Buscar título, gênero, estúdio…",
  ptPT="Pesquisar título, género, estúdio…")
s("close_search", "Close search", "Cerrar búsqueda", "Fermer la recherche", "Suche schließen", "Chiudi ricerca",
  "Fechar busca", ptPT="Fechar pesquisa")
s("my_ratings", "My ratings", "Mis calificaciones", "Mes notes", "Meine Bewertungen", "Le mie valutazioni",
  "Minhas avaliações", ptPT="As minhas avaliações")
s("filter_all", "All", "Todo", "Tout", "Alle", "Tutto", "Tudo")
s("sort_label", "Sort", "Ordenar", "Trier", "Sortieren", "Ordina", "Ordenar")
s("group_label", "Shelves by", "Repisas por", "Étagères par", "Regale nach", "Scaffali per", "Prateleiras por")
s("hint_controls", "Tap a box or use the controller · A: view · X: play · Y: favorite · Select: change shelves",
  "Toca una caja o muévete con el mando · A: ver · X: jugar · Y: favorito · Select: cambiar repisas",
  "Touchez une boîte ou utilisez la manette · A : voir · X : jouer · Y : favori · Select : changer d’étagères",
  "Box antippen oder Controller nutzen · A: ansehen · X: spielen · Y: Favorit · Select: Regale wechseln",
  "Tocca una scatola o usa il controller · A: vedi · X: gioca · Y: preferito · Select: cambia scaffali",
  "Toque numa caixa ou use o controle · A: ver · X: jogar · Y: favorito · Select: trocar prateleiras",
  enGB="Tap a box or use the controller · A: view · X: play · Y: favourite · Select: change shelves",
  es419="Toca una caja o muévete con el control · A: ver · X: jugar · Y: favorito · Select: cambiar repisas",
  ptPT="Toque numa caixa ou use o comando · A: ver · X: jogar · Y: favorito · Select: mudar prateleiras")
s("empty_filtered", "No box matches the filter", "Ninguna caja coincide con el filtro",
  "Aucune boîte ne correspond au filtre", "Keine Box passt zum Filter", "Nessuna scatola corrisponde al filtro",
  "Nenhuma caixa corresponde ao filtro")
s("empty_title", "Your shelf is empty", "Tu repisa está vacía", "Votre étagère est vide", "Dein Regal ist leer",
  "Il tuo scaffale è vuoto", "Sua prateleira está vazia", ptPT="A sua prateleira está vazia")
s("empty_body", "Connect Steam, Epic Games or choose your ROM, ES-DE or RetroArch folders to fill it.",
  "Conecta Steam, Epic Games o elige tus carpetas de ROMs, ES-DE o RetroArch para llenarla.",
  "Connectez Steam, Epic Games ou choisissez vos dossiers de ROM, ES-DE ou RetroArch pour la remplir.",
  "Verbinde Steam, Epic Games oder wähle deine ROM-, ES-DE- oder RetroArch-Ordner, um es zu füllen.",
  "Collega Steam, Epic Games o scegli le cartelle di ROM, ES-DE o RetroArch per riempirlo.",
  "Conecte a Steam, a Epic Games ou escolha suas pastas de ROMs, ES-DE ou RetroArch para enchê-la.",
  ptPT="Ligue a Steam, a Epic Games ou escolha as suas pastas de ROMs, ES-DE ou RetroArch para a encher.")
s("open_settings", "Open settings", "Abrir ajustes", "Ouvrir les paramètres", "Einstellungen öffnen",
  "Apri impostazioni", "Abrir configurações", es419="Abrir configuración", ptPT="Abrir definições")
s("msg_group_changed", "Shelves by: %1$s", "Repisas por: %1$s", "Étagères par : %1$s", "Regale nach: %1$s",
  "Scaffali per: %1$s", "Prateleiras por: %1$s")

# Orden y agrupación
s("sort_alpha", "A – Z", "A – Z", "A – Z", "A – Z", "A – Z", "A – Z")
s("sort_playtime", "Playtime", "Tiempo de juego", "Temps de jeu", "Spielzeit", "Tempo di gioco", "Tempo de jogo")
s("sort_recent", "Recently played", "Jugado recientemente", "Joué récemment", "Zuletzt gespielt",
  "Giocati di recente", "Jogados recentemente")
s("sort_year", "Year", "Año", "Année", "Jahr", "Anno", "Ano")
s("sort_console", "Console", "Consola", "Console", "Konsole", "Console", "Console", ptPT="Consola")
s("sort_rating", "My rating", "Mi nota", "Ma note", "Meine Wertung", "Il mio voto", "Minha nota", ptPT="A minha nota")
s("sort_metacritic", "Metacritic", "Metacritic", "Metacritic", "Metacritic", "Metacritic", "Metacritic")
s("group_none", "All together", "Todo junto", "Tout ensemble", "Alles zusammen", "Tutto insieme", "Tudo junto")
s("group_genre", "Genre", "Género", "Genre", "Genre", "Genere", "Gênero", ptPT="Género")
s("group_console", "Console", "Consola", "Console", "Konsole", "Console", "Console", ptPT="Consola")
s("group_developer", "Studio", "Estudio", "Studio", "Studio", "Studio", "Estúdio")
s("group_source", "Platform", "Plataforma", "Plateforme", "Plattform", "Piattaforma", "Plataforma")
s("group_decade", "Decade", "Década", "Décennie", "Jahrzehnt", "Decennio", "Década")
s("group_favorites", "Favorites", "Favoritos", "Favoris", "Favoriten", "Preferiti", "Favoritos",
  enGB="Favourites")
s("shelf_my_collection", "My collection", "Mi colección", "Ma collection", "Meine Sammlung", "La mia collezione",
  "Minha coleção", ptPT="A minha coleção")
s("shelf_no_genre", "No genre", "Sin género", "Sans genre", "Ohne Genre", "Senza genere", "Sem gênero",
  ptPT="Sem género")
s("shelf_unknown_studio", "Unknown studio", "Estudio desconocido", "Studio inconnu", "Unbekanntes Studio",
  "Studio sconosciuto", "Estúdio desconhecido")
s("shelf_unknown_year", "Unknown year", "Año desconocido", "Année inconnue", "Unbekanntes Jahr", "Anno sconosciuto",
  "Ano desconhecido")
s("shelf_decade", "The %1$ds", "Años %1$d", "Années %1$d", "%1$der", "Anni %1$d", "Anos %1$d")
s("shelf_favorites", "★ Favorites", "★ Favoritos", "★ Favoris", "★ Favoriten", "★ Preferiti", "★ Favoritos",
  enGB="★ Favourites")
s("shelf_rest", "Everything else", "El resto", "Le reste", "Der Rest", "Il resto", "O resto")
s("source_steam", "Steam", "Steam", "Steam", "Steam", "Steam", "Steam")
s("source_epic", "Epic Games", "Epic Games", "Epic Games", "Epic Games", "Epic Games", "Epic Games")
s("source_emulators", "Emulators", "Emuladores", "Émulateurs", "Emulatoren", "Emulatori", "Emuladores")
s("console_other", "Other", "Otros", "Autres", "Andere", "Altro", "Outros")

# Tiempo
s("time_none", "Not played", "Sin jugar", "Jamais joué", "Nicht gespielt", "Mai giocato", "Não jogado")
s("time_min", "%1$d min", "%1$d min", "%1$d min", "%1$d Min.", "%1$d min", "%1$d min")
s("time_h", "%1$d h", "%1$d h", "%1$d h", "%1$d Std.", "%1$d h", "%1$d h")
s("time_h_min", "%1$d h %2$d min", "%1$d h %2$d min", "%1$d h %2$d min", "%1$d Std. %2$d Min.", "%1$d h %2$d min",
  "%1$d h %2$d min")

# ------------------------------------------------------------------ Ficha del juego
s("rate", "Rate", "Calificar", "Noter", "Bewerten", "Valuta", "Avaliar")
s("tag_family", "Steam Family", "Familia Steam", "Famille Steam", "Steam-Familie", "Famiglia Steam", "Família Steam")
s("stat_playtime", "Playtime", "Tiempo de juego", "Temps de jeu", "Spielzeit", "Tempo di gioco", "Tempo de jogo")
s("stat_tracked", "(%1$s measured by Repisa)", "(%1$s medidos por Repisa)", "(%1$s mesurés par Repisa)",
  "(%1$s von Repisa gemessen)", "(%1$s misurati da Repisa)", "(%1$s medidos pelo Repisa)")
s("stat_last_played", "Last played", "Última partida", "Dernière partie", "Zuletzt gespielt", "Ultima partita",
  "Última partida")
s("stat_studio", "Studio", "Estudio", "Studio", "Studio", "Studio", "Estúdio")
s("stat_publisher", "Publisher", "Editora", "Éditeur", "Publisher", "Editore", "Editora")
s("stat_year", "Year", "Año", "Année", "Jahr", "Anno", "Ano")
s("stat_file", "File", "Archivo", "Fichier", "Datei", "File", "Arquivo", ptPT="Ficheiro")
s("stat_genre", "Genre", "Género", "Genre", "Genre", "Genere", "Gênero", ptPT="Género")
s("play", "Play", "Jugar", "Jouer", "Spielen", "Gioca", "Jogar")
s("view_steam", "View on Steam", "Ver en Steam", "Voir sur Steam", "Auf Steam ansehen", "Vedi su Steam", "Ver na Steam")
s("view_store", "View in store", "Ver en la tienda", "Voir dans la boutique", "Im Store ansehen", "Vedi nello store",
  "Ver na loja")
s("favorite", "Favorite", "Favorito", "Favori", "Favorit", "Preferito", "Favorito", enGB="Favourite")
s("mark_favorite", "Mark as favorite", "Marcar favorito", "Ajouter aux favoris", "Als Favorit markieren",
  "Aggiungi ai preferiti", "Marcar como favorito", enGB="Mark as favourite")
s("search_cover", "Find cover", "Buscar carátula", "Chercher une jaquette", "Cover suchen", "Cerca copertina",
  "Buscar capa", es419="Buscar portada", ptPT="Procurar capa")
s("edit_data", "Edit details", "Editar datos", "Modifier les infos", "Daten bearbeiten", "Modifica dati",
  "Editar dados")
s("edit_title", "Edit %1$s", "Editar %1$s", "Modifier %1$s", "%1$s bearbeiten", "Modifica %1$s", "Editar %1$s")
s("field_title", "Title", "Título", "Titre", "Titel", "Titolo", "Título")
s("field_genres", "Genres (comma separated)", "Géneros (separados por comas)", "Genres (séparés par des virgules)",
  "Genres (durch Kommas getrennt)", "Generi (separati da virgole)", "Gêneros (separados por vírgulas)",
  ptPT="Géneros (separados por vírgulas)")
s("field_extra_hours", "Extra hours played (manual)", "Horas extra jugadas (a mano)", "Heures en plus (manuel)",
  "Zusätzliche Spielstunden (manuell)", "Ore extra giocate (manuale)", "Horas extras jogadas (manual)")
s("field_cover_url", "Cover URL", "URL de la carátula", "URL de la jaquette", "Cover-URL", "URL della copertina",
  "URL da capa", es419="URL de la portada")
s("my_score", "My rating", "Mi nota", "Ma note", "Meine Wertung", "Il mio voto", "Minha nota", ptPT="A minha nota")
s("rate_action", "Rate ★", "Calificar ★", "Noter ★", "Bewerten ★", "Valuta ★", "Avaliar ★")
s("not_available", "Not available", "No disponible", "Indisponible", "Nicht verfügbar", "Non disponibile", "Não disponível")
s("press", "critics", "prensa", "presse", "Presse", "stampa", "crítica")
s("users", "users", "usuarios", "joueurs", "Nutzer", "utenti", "usuários", ptPT="utilizadores")
s("hltb_main", "Story", "Historia", "Histoire", "Story", "Storia", "História")
s("hltb_extra", "+ Extras", "+ Extras", "+ Extras", "+ Extras", "+ Extra", "+ Extras")
s("hltb_complete", "100%", "100 %", "100 %", "100 %", "100%", "100%")

# ------------------------------------------------------------------ Calificaciones
s("what_you_rate", "What you rate", "Qué calificas", "Ce que vous notez", "Was du bewertest", "Cosa valuti",
  "O que você avalia", ptPT="O que avalia")
s("weight_pct", "Weight %", "Peso %", "Poids %", "Gewicht %", "Peso %", "Peso %")
s("score_of", "Score /%1$d", "Nota /%1$d", "Note /%1$d", "Note /%1$d", "Voto /%1$d", "Nota /%1$d")
s("pct_of_score", "%1$s%% of the score", "%1$s %% de la nota", "%1$s %% de la note", "%1$s %% der Wertung",
  "%1$s%% del voto", "%1$s%% da nota")
s("new_aspect", "New aspect", "Nuevo aspecto", "Nouvel aspect", "Neuer Aspekt", "Nuovo aspetto", "Novo aspecto")
s("add_aspect", "Add something to rate", "Añadir algo que calificar", "Ajouter quelque chose à noter",
  "Etwas zum Bewerten hinzufügen", "Aggiungi qualcosa da valutare", "Adicionar algo para avaliar")
s("weights_sum",
  "Weights add up to %1$s%%: they're applied proportionally, so they don't need to add up to 100.",
  "Los pesos suman %1$s %%: se reparten en proporción, así que no hace falta que sumen 100.",
  "Les poids totalisent %1$s %% : ils sont répartis proportionnellement, pas besoin d’arriver à 100.",
  "Die Gewichte ergeben %1$s %%: Sie werden anteilig verteilt und müssen nicht 100 ergeben.",
  "I pesi sommano %1$s%%: vengono ripartiti in proporzione, non serve che facciano 100.",
  "Os pesos somam %1$s%%: são distribuídos proporcionalmente, não precisam somar 100.")
s("comment_optional", "Comment (optional)", "Comentario (opcional)", "Commentaire (facultatif)",
  "Kommentar (optional)", "Commento (facoltativo)", "Comentário (opcional)")
s("delete_rating", "Delete rating", "Borrar calificación", "Supprimer la note", "Bewertung löschen",
  "Elimina valutazione", "Apagar avaliação")
s("use_template", "Use template", "Usar plantilla", "Utiliser le modèle", "Vorlage verwenden", "Usa modello",
  "Usar modelo")
s("rated_empty", "You haven't rated any game yet. Open one and tap the star next to the X.",
  "Todavía no has calificado ningún juego. Abre uno y pulsa la estrella que hay junto a la X.",
  "Vous n’avez encore noté aucun jeu. Ouvrez-en un et touchez l’étoile à côté du X.",
  "Du hast noch kein Spiel bewertet. Öffne eines und tippe auf den Stern neben dem X.",
  "Non hai ancora valutato nessun gioco. Aprine uno e tocca la stella accanto alla X.",
  "Você ainda não avaliou nenhum jogo. Abra um e toque na estrela ao lado do X.",
  ptPT="Ainda não avaliou nenhum jogo. Abra um e toque na estrela ao lado do X.")
s("rated_subtitle", "%1$s · scale %2$s", "%1$s · escala %2$s", "%1$s · échelle %2$s", "%1$s · Skala %2$s",
  "%1$s · scala %2$s", "%1$s · escala %2$s")
s("rating_settings", "Rating settings", "Ajustes de calificación", "Réglages des notes", "Bewertungseinstellungen",
  "Impostazioni valutazioni", "Ajustes de avaliação", es419="Configuración de calificación",
  ptPT="Definições de avaliação")
s("scale_title", "Scale", "Escala", "Échelle", "Skala", "Scala", "Escala")
s("scale_hint", "Scale used to enter and show scores. You can change it anytime: existing scores convert automatically.",
  "Escala con la que pones y ves las notas. Puedes cambiarla cuando quieras: las notas ya puestas se convierten solas.",
  "Échelle pour saisir et afficher les notes. Modifiable à tout moment : les notes existantes sont converties.",
  "Skala zum Eingeben und Anzeigen. Jederzeit änderbar: vorhandene Wertungen werden umgerechnet.",
  "Scala per inserire e mostrare i voti. Puoi cambiarla quando vuoi: i voti esistenti si convertono da soli.",
  "Escala para dar e ver as notas. Pode mudar quando quiser: as notas já dadas são convertidas.")
s("template_title", "Template", "Plantilla", "Modèle", "Vorlage", "Modello", "Modelo")
s("template_hint",
  "What gets rated by default in each new game and how much each part weighs. In each game you can add, remove or rename anything.",
  "Lo que se califica por defecto en cada juego nuevo y cuánto pesa cada cosa. En cada juego puedes añadir, quitar o renombrar lo que quieras.",
  "Ce qui est noté par défaut pour chaque jeu et le poids de chaque partie. Dans chaque jeu, vous pouvez ajouter, retirer ou renommer.",
  "Was standardmäßig bei jedem Spiel bewertet wird und wie viel es zählt. In jedem Spiel kannst du alles hinzufügen, entfernen oder umbenennen.",
  "Cosa si valuta di default in ogni gioco e quanto pesa ogni parte. In ogni gioco puoi aggiungere, togliere o rinominare.",
  "O que é avaliado por padrão em cada jogo novo e quanto pesa cada parte. Em cada jogo pode adicionar, remover ou renomear.",
  ptPT="O que é avaliado por omissão em cada jogo novo e quanto pesa cada parte. Em cada jogo pode adicionar, remover ou mudar o nome.")
s("save_template", "Save template", "Guardar plantilla", "Enregistrer le modèle", "Vorlage speichern",
  "Salva modello", "Salvar modelo", ptPT="Guardar modelo")
s("restore_template", "Restore default", "Restaurar la original", "Rétablir l’original", "Original wiederherstellen",
  "Ripristina originale", "Restaurar o original")
s("scale_5", "0 to 5", "Del 0 al 5", "De 0 à 5", "0 bis 5", "Da 0 a 5", "De 0 a 5")
s("scale_10", "0 to 10", "Del 0 al 10", "De 0 à 10", "0 bis 10", "Da 0 a 10", "De 0 a 10")
s("scale_100", "0 to 100", "Del 0 al 100", "De 0 à 100", "0 bis 100", "Da 0 a 100", "De 0 a 100")
s("crit_gameplay", "Gameplay", "Jugabilidad", "Gameplay", "Gameplay", "Giocabilità", "Jogabilidade")
s("crit_story", "Story", "Historia", "Histoire", "Story", "Trama", "História")
s("crit_graphics", "Graphics & art", "Gráficos y arte", "Graphismes et direction artistique", "Grafik & Stil",
  "Grafica e arte", "Gráficos e arte")
s("crit_music", "Music & sound", "Música y sonido", "Musique et son", "Musik & Sound", "Musica e sonoro",
  "Música e som")
s("crit_length", "Length & replay value", "Duración y rejugabilidad", "Durée et rejouabilité",
  "Umfang & Wiederspielwert", "Durata e rigiocabilità", "Duração e rejogabilidade")

# ------------------------------------------------------------------ Buscador de carátulas
s("cover_search_title", "Find cover · %1$s", "Buscar carátula · %1$s", "Chercher une jaquette · %1$s",
  "Cover suchen · %1$s", "Cerca copertina · %1$s", "Buscar capa · %1$s", es419="Buscar portada · %1$s",
  ptPT="Procurar capa · %1$s")
s("game_name", "Game name", "Nombre del juego", "Nom du jeu", "Spielname", "Nome del gioco", "Nome do jogo")
s("sgdb_tip", "Tip: add your free SteamGridDB key in Settings → Covers & data for many more covers.",
  "Consejo: añade tu clave gratuita de SteamGridDB en Ajustes → Carátulas y datos para tener muchas más carátulas.",
  "Astuce : ajoutez votre clé SteamGridDB gratuite dans Paramètres → Jaquettes et données pour bien plus de jaquettes.",
  "Tipp: Füge deinen kostenlosen SteamGridDB-Schlüssel unter Einstellungen → Cover & Daten hinzu.",
  "Consiglio: aggiungi la tua chiave gratuita di SteamGridDB in Impostazioni → Copertine e dati.",
  "Dica: adicione sua chave gratuita do SteamGridDB em Configurações → Capas e dados para ter muito mais capas.",
  es419="Consejo: agrega tu clave gratuita de SteamGridDB en Configuración → Portadas y datos para tener muchas más portadas.",
  ptPT="Dica: adicione a sua chave gratuita do SteamGridDB em Definições → Capas e dados para ter muitas mais capas.")
s("cover_conn_failed", "Couldn't connect to: %1$s. Check your internet connection.",
  "No se pudo conectar con: %1$s. Revisa la conexión a internet.",
  "Connexion impossible à : %1$s. Vérifiez votre connexion internet.",
  "Keine Verbindung zu: %1$s. Prüfe deine Internetverbindung.",
  "Impossibile connettersi a: %1$s. Controlla la connessione.",
  "Não foi possível conectar a: %1$s. Verifique sua conexão.", ptPT="Não foi possível ligar a: %1$s. Verifique a ligação à internet.")
s("cover_none", "Nothing found. Try fewer words or the English name.",
  "No se encontró nada. Prueba con menos palabras o con el nombre en inglés.",
  "Rien trouvé. Essayez avec moins de mots ou le nom anglais.",
  "Nichts gefunden. Versuche weniger Wörter oder den englischen Namen.",
  "Nessun risultato. Prova con meno parole o con il nome inglese.",
  "Nada encontrado. Tente menos palavras ou o nome em inglês.")
s("all_sources", "all", "todos", "tous", "alle", "tutti", "todos")

# ------------------------------------------------------------------ Segunda pantalla
s("most_played", "Most played", "Los más jugados", "Les plus joués", "Am meisten gespielt", "I più giocati",
  "Os mais jogados")

# ------------------------------------------------------------------ Ajustes: cuentas
s("steam_linked", "Account linked: %1$s", "Cuenta vinculada: %1$s", "Compte lié : %1$s", "Konto verknüpft: %1$s",
  "Account collegato: %1$s", "Conta vinculada: %1$s", ptPT="Conta associada: %1$s")
s("steam_linked_key", "Linked with an API key", "Vinculada con clave de API", "Lié avec une clé API",
  "Mit API-Schlüssel verknüpft", "Collegato con chiave API", "Vinculada com chave de API", ptPT="Associada com chave de API")
s("steam_link_hint",
  "Sign in on Steam's official page (username or the Steam app QR code). Repisa only reads your games and hours; your password never goes through the app.",
  "Inicia sesión en la página oficial de Steam (con tu usuario o el QR de la app de Steam). Repisa solo lee tus juegos y horas; tu contraseña no pasa por la app.",
  "Connectez-vous sur la page officielle de Steam (identifiant ou QR de l’app Steam). Repisa ne lit que vos jeux et vos heures ; votre mot de passe ne passe pas par l’app.",
  "Melde dich auf der offiziellen Steam-Seite an (Benutzername oder QR der Steam-App). Repisa liest nur deine Spiele und Stunden; dein Passwort geht nicht durch die App.",
  "Accedi alla pagina ufficiale di Steam (utente o QR dell’app Steam). Repisa legge solo giochi e ore; la password non passa dall’app.",
  "Entre na página oficial da Steam (usuário ou QR do app da Steam). O Repisa só lê seus jogos e horas; sua senha não passa pelo app.",
  ptPT="Inicie sessão na página oficial da Steam (utilizador ou QR da app Steam). O Repisa só lê os seus jogos e horas; a palavra-passe não passa pela app.")
s("steam_link", "Link Steam account", "Vincular cuenta de Steam", "Lier le compte Steam", "Steam-Konto verknüpfen",
  "Collega account Steam", "Vincular conta da Steam", ptPT="Associar conta Steam")
s("unlink", "Unlink", "Desvincular", "Délier", "Trennen", "Scollega", "Desvincular", ptPT="Desassociar")
s("steam_advanced_show", "Advanced: use an API key instead", "Avanzado: usar una clave de API",
  "Avancé : utiliser une clé API", "Erweitert: API-Schlüssel verwenden", "Avanzato: usa una chiave API",
  "Avançado: usar uma chave de API")
s("steam_advanced_hide", "Hide advanced option", "Ocultar opción avanzada", "Masquer l’option avancée",
  "Erweiterte Option ausblenden", "Nascondi opzione avanzata", "Ocultar opção avançada")
s("steam_key_hint", "Steam Web API key and your SteamID64 or profile URL. 'Game details' must be Public.",
  "Clave de la Steam Web API y tu SteamID64 o URL de perfil. 'Detalles de juegos' debe estar en Público.",
  "Clé Steam Web API et votre SteamID64 ou URL de profil. « Détails des jeux » doit être public.",
  "Steam-Web-API-Schlüssel und deine SteamID64 oder Profil-URL. „Spieldetails“ muss öffentlich sein.",
  "Chiave Steam Web API e il tuo SteamID64 o URL del profilo. «Dettagli dei giochi» deve essere pubblico.",
  "Chave da Steam Web API e seu SteamID64 ou URL do perfil. 'Detalhes de jogos' deve estar público.")
s("steam_get_key", "Get a key at steamcommunity.com/dev/apikey", "Conseguir clave en steamcommunity.com/dev/apikey",
  "Obtenir une clé sur steamcommunity.com/dev/apikey", "Schlüssel auf steamcommunity.com/dev/apikey holen",
  "Ottieni una chiave su steamcommunity.com/dev/apikey", "Obter chave em steamcommunity.com/dev/apikey")
s("steam_key", "Steam API key", "Clave de API de Steam", "Clé API Steam", "Steam-API-Schlüssel", "Chiave API Steam",
  "Chave de API da Steam")
s("steam_user", "SteamID64 / profile", "SteamID64 / perfil", "SteamID64 / profil", "SteamID64 / Profil",
  "SteamID64 / profilo", "SteamID64 / perfil")
s("save_and_sync", "Save and sync", "Guardar y sincronizar", "Enregistrer et synchroniser", "Speichern und synchronisieren",
  "Salva e sincronizza", "Salvar e sincronizar", ptPT="Guardar e sincronizar")
s("epic_connected", "Connected as %1$s", "Conectado como %1$s", "Connecté en tant que %1$s", "Verbunden als %1$s",
  "Connesso come %1$s", "Conectado como %1$s", ptPT="Ligado como %1$s")
s("epic_connected_plain", "Connected", "Conectado", "Connecté", "Verbunden", "Connesso", "Conectado", ptPT="Ligado")
s("sign_out", "Sign out", "Cerrar sesión", "Se déconnecter", "Abmelden", "Esci", "Sair", ptPT="Terminar sessão")
s("epic_link_hint",
  "Sign in on Epic's official page inside the app with your email and password. Repisa only reads your library and playtime.",
  "Inicia sesión en la página oficial de Epic dentro de la app con tu correo y contraseña. Repisa solo lee tu biblioteca y tu tiempo de juego.",
  "Connectez-vous sur la page officielle d’Epic dans l’app avec votre e-mail et mot de passe. Repisa ne lit que votre bibliothèque et votre temps de jeu.",
  "Melde dich in der App auf der offiziellen Epic-Seite mit E-Mail und Passwort an. Repisa liest nur deine Bibliothek und Spielzeit.",
  "Accedi alla pagina ufficiale di Epic nell’app con email e password. Repisa legge solo la libreria e il tempo di gioco.",
  "Entre na página oficial da Epic dentro do app com e-mail e senha. O Repisa só lê sua biblioteca e tempo de jogo.",
  ptPT="Inicie sessão na página oficial da Epic dentro da app com e-mail e palavra-passe. O Repisa só lê a biblioteca e o tempo de jogo.")
s("epic_link", "Link Epic Games account", "Vincular cuenta de Epic Games", "Lier le compte Epic Games",
  "Epic-Games-Konto verknüpfen", "Collega account Epic Games", "Vincular conta da Epic Games",
  ptPT="Associar conta Epic Games")
s("other_methods_show", "Other sign-in methods", "Otros métodos de inicio de sesión", "Autres méthodes de connexion",
  "Andere Anmeldemethoden", "Altri metodi di accesso", "Outros métodos de login", ptPT="Outros métodos de início de sessão")
s("other_methods_hide", "Hide other methods", "Ocultar otros métodos", "Masquer les autres méthodes",
  "Andere Methoden ausblenden", "Nascondi altri metodi", "Ocultar outros métodos")
s("epic_code_hint",
  "With a code (if you sign in with Google, Xbox, PlayStation or Steam): the app shows a code and a QR for epicgames.com/activate.",
  "Con código (si entras con Google, Xbox, PlayStation o Steam): la app muestra un código y un QR para epicgames.com/activate.",
  "Avec un code (si vous vous connectez via Google, Xbox, PlayStation ou Steam) : l’app affiche un code et un QR pour epicgames.com/activate.",
  "Mit Code (bei Anmeldung über Google, Xbox, PlayStation oder Steam): Die App zeigt einen Code und QR für epicgames.com/activate.",
  "Con codice (se accedi con Google, Xbox, PlayStation o Steam): l’app mostra un codice e un QR per epicgames.com/activate.",
  "Com código (se entra com Google, Xbox, PlayStation ou Steam): o app mostra um código e um QR para epicgames.com/activate.",
  ptPT="Com código (se entra com Google, Xbox, PlayStation ou Steam): a app mostra um código e um QR para epicgames.com/activate.")
s("epic_link_code", "Link with a code", "Vincular con código", "Lier avec un code", "Mit Code verknüpfen",
  "Collega con codice", "Vincular com código", ptPT="Associar com código")
s("epic_browser_hint", "Or open it in the browser, sign in and paste the \"authorizationCode\" value here:",
  "O ábrelo en el navegador, inicia sesión y pega aquí el valor de \"authorizationCode\":",
  "Ou ouvrez-le dans le navigateur, connectez-vous et collez ici la valeur \"authorizationCode\" :",
  "Oder öffne es im Browser, melde dich an und füge hier den Wert \"authorizationCode\" ein:",
  "Oppure aprilo nel browser, accedi e incolla qui il valore \"authorizationCode\":",
  "Ou abra no navegador, entre e cole aqui o valor de \"authorizationCode\":")
s("epic_open_browser", "Open Epic login in the browser", "Abrir el login de Epic en el navegador",
  "Ouvrir la connexion Epic dans le navigateur", "Epic-Anmeldung im Browser öffnen", "Apri il login di Epic nel browser",
  "Abrir o login da Epic no navegador")
s("epic_auth_code", "Authorization code", "Código de autorización", "Code d’autorisation", "Autorisierungscode",
  "Codice di autorizzazione", "Código de autorização")
s("use_code", "Use code", "Usar código", "Utiliser le code", "Code verwenden", "Usa codice", "Usar código")
s("epic_requesting", "Requesting a code from Epic…", "Pidiendo un código a Epic…", "Demande d’un code à Epic…",
  "Code wird bei Epic angefordert…", "Richiesta di un codice a Epic…", "Pedindo um código à Epic…")
s("epic_step1", "1. Open epicgames.com/activate (or scan the QR with your phone)",
  "1. Abre epicgames.com/activate (o escanea el QR con el móvil)",
  "1. Ouvrez epicgames.com/activate (ou scannez le QR avec votre téléphone)",
  "1. Öffne epicgames.com/activate (oder scanne den QR mit dem Handy)",
  "1. Apri epicgames.com/activate (o scansiona il QR col telefono)",
  "1. Abra epicgames.com/activate (ou escaneie o QR com o celular)",
  es419="1. Abre epicgames.com/activate (o escanea el QR con el celular)",
  ptPT="1. Abra epicgames.com/activate (ou leia o QR com o telemóvel)")
s("epic_step2", "2. Sign in and enter this code:", "2. Inicia sesión y escribe este código:",
  "2. Connectez-vous et saisissez ce code :", "2. Melde dich an und gib diesen Code ein:",
  "2. Accedi e inserisci questo codice:", "2. Entre e digite este código:", ptPT="2. Inicie sessão e escreva este código:")
s("epic_waiting", "Waiting for you to accept on the web…", "Esperando a que aceptes en la web…",
  "En attente de votre validation sur le web…", "Warte auf deine Bestätigung im Web…",
  "In attesa che accetti sul web…", "Aguardando você aceitar na web…", ptPT="À espera que aceite na web…")
s("epic_failed", "Couldn't link: %1$s", "No se pudo vincular: %1$s", "Échec de la liaison : %1$s",
  "Verknüpfung fehlgeschlagen: %1$s", "Impossibile collegare: %1$s", "Não foi possível vincular: %1$s",
  ptPT="Não foi possível associar: %1$s")
s("epic_failed_hint", "If it keeps happening, try \"Other sign-in methods\".",
  "Si se repite, prueba con \"Otros métodos de inicio de sesión\".",
  "Si cela se reproduit, essayez « Autres méthodes de connexion ».",
  "Wenn es wieder passiert, versuche „Andere Anmeldemethoden“.",
  "Se succede ancora, prova «Altri metodi di accesso».",
  "Se acontecer de novo, tente \"Outros métodos de login\".")
s("epic_done", "Account linked! Loading your library…", "¡Cuenta vinculada! Cargando tu biblioteca…",
  "Compte lié ! Chargement de votre bibliothèque…", "Konto verknüpft! Bibliothek wird geladen…",
  "Account collegato! Caricamento della libreria…", "Conta vinculada! Carregando sua biblioteca…",
  ptPT="Conta associada! A carregar a sua biblioteca…")
s("open_here", "Open here", "Abrir aquí", "Ouvrir ici", "Hier öffnen", "Apri qui", "Abrir aqui")
s("new_code", "Get another code", "Pedir otro código", "Demander un autre code", "Neuen Code anfordern",
  "Chiedi un altro codice", "Pedir outro código")

# ------------------------------------------------------------------ Ajustes: menú y submenús
s("menu_emulators", "Emulators", "Emuladores", "Émulateurs", "Emulatoren", "Emulatori", "Emuladores")
s("menu_emulators_sub", "ROM folders, ES-DE, RetroArch, Cocoon and emulator per system",
  "Carpetas de ROMs, ES-DE, RetroArch, Cocoon y emulador por sistema",
  "Dossiers de ROM, ES-DE, RetroArch, Cocoon et émulateur par système",
  "ROM-Ordner, ES-DE, RetroArch, Cocoon und Emulator pro System",
  "Cartelle ROM, ES-DE, RetroArch, Cocoon ed emulatore per sistema",
  "Pastas de ROMs, ES-DE, RetroArch, Cocoon e emulador por sistema")
s("menu_covers", "Covers & data", "Carátulas y datos", "Jaquettes et données", "Cover & Daten", "Copertine e dati",
  "Capas e dados", es419="Portadas y datos")
s("menu_covers_sub", "SteamGridDB and missing covers", "SteamGridDB y carátulas que faltan",
  "SteamGridDB et jaquettes manquantes", "SteamGridDB und fehlende Cover", "SteamGridDB e copertine mancanti",
  "SteamGridDB e capas que faltam", es419="SteamGridDB y portadas que faltan")
s("menu_playtime", "Playtime", "Tiempo de juego", "Temps de jeu", "Spielzeit", "Tempo di gioco", "Tempo de jogo")
s("menu_playtime_sub", "Automatic sync and Usage access", "Sincronización automática y Acceso a datos de uso",
  "Synchro automatique et accès aux données d’utilisation", "Automatische Synchronisierung und Nutzungszugriff",
  "Sincronizzazione automatica e accesso ai dati di utilizzo", "Sincronização automática e acesso a dados de uso",
  ptPT="Sincronização automática e acesso aos dados de utilização")
s("menu_display", "Display", "Pantalla", "Affichage", "Anzeige", "Schermo", "Tela", ptPT="Ecrã")
s("menu_display_sub", "Second screen and box size", "Segunda pantalla y tamaño de las cajas",
  "Second écran et taille des boîtes", "Zweiter Bildschirm und Boxgröße", "Secondo schermo e dimensione delle scatole",
  "Segunda tela e tamanho das caixas", ptPT="Segundo ecrã e tamanho das caixas")
s("menu_language", "Language", "Idioma", "Langue", "Sprache", "Lingua", "Idioma")
s("rom_hint",
  "Choose the folder where you keep your ROMs (one subfolder per system: snes, psx, ps2, gba, switch…, like ES-DE). You can add several.",
  "Elige la carpeta donde guardas los ROMs (una subcarpeta por sistema: snes, psx, ps2, gba, switch…, como ES-DE). Puedes añadir varias.",
  "Choisissez le dossier de vos ROM (un sous-dossier par système : snes, psx, ps2, gba, switch…, comme ES-DE). Vous pouvez en ajouter plusieurs.",
  "Wähle den Ordner mit deinen ROMs (ein Unterordner pro System: snes, psx, ps2, gba, switch…, wie ES-DE). Du kannst mehrere hinzufügen.",
  "Scegli la cartella delle ROM (una sottocartella per sistema: snes, psx, ps2, gba, switch…, come ES-DE). Puoi aggiungerne più di una.",
  "Escolha a pasta onde guarda as ROMs (uma subpasta por sistema: snes, psx, ps2, gba, switch…, como o ES-DE). Pode adicionar várias.")
s("add_rom_folder", "Add ROM folder", "Añadir carpeta de ROMs", "Ajouter un dossier de ROM", "ROM-Ordner hinzufügen",
  "Aggiungi cartella ROM", "Adicionar pasta de ROMs", es419="Agregar carpeta de ROMs")
s("esde_hint", "Optional: ES-DE folder (uses its covers, genres, studios and dates).",
  "Opcional: carpeta ES-DE (usa sus carátulas, géneros, estudios y fechas).",
  "Facultatif : dossier ES-DE (utilise ses jaquettes, genres, studios et dates).",
  "Optional: ES-DE-Ordner (nutzt Cover, Genres, Studios und Daten).",
  "Facoltativo: cartella ES-DE (usa copertine, generi, studi e date).",
  "Opcional: pasta do ES-DE (usa capas, gêneros, estúdios e datas).", ptPT="Opcional: pasta do ES-DE (usa capas, géneros, estúdios e datas).")
s("choose_esde", "Choose ES-DE folder", "Elegir carpeta ES-DE", "Choisir le dossier ES-DE", "ES-DE-Ordner wählen",
  "Scegli cartella ES-DE", "Escolher pasta do ES-DE")
s("ra_hint", "Optional: RetroArch folder (reads playlists and playtime). In RetroArch enable Settings → Saving → 'Save runtime logs'.",
  "Opcional: carpeta RetroArch (lee las playlists y el tiempo de juego). Activa en RetroArch Ajustes → Guardado → 'Guardar registros de tiempo de juego'.",
  "Facultatif : dossier RetroArch (lit les playlists et le temps de jeu). Dans RetroArch, activez Réglages → Sauvegarde → « Journaux de temps de jeu ».",
  "Optional: RetroArch-Ordner (liest Playlists und Spielzeit). In RetroArch Einstellungen → Speichern → „Laufzeit-Logs speichern“ aktivieren.",
  "Facoltativo: cartella RetroArch (legge playlist e tempo di gioco). In RetroArch attiva Impostazioni → Salvataggio → «Salva registro tempo di gioco».",
  "Opcional: pasta do RetroArch (lê playlists e tempo de jogo). No RetroArch ative Configurações → Salvamento → 'Salvar registros de tempo de jogo'.")
s("choose_ra", "Choose RetroArch folder", "Elegir carpeta RetroArch", "Choisir le dossier RetroArch",
  "RetroArch-Ordner wählen", "Scegli cartella RetroArch", "Escolher pasta do RetroArch")
s("cocoon_hint",
  "Optional: Cocoon data folder. In Cocoon use Settings → Library & Data → Export Metadata; Repisa reads genres, dates, covers and playtime from it. Also works with Batocera, RetroBat and other frontends that export gamelist.xml.",
  "Opcional: carpeta de datos de Cocoon. En Cocoon usa Ajustes → Library & Data → Export Metadata; Repisa lee de ahí géneros, fechas, carátulas y tiempo de juego. Sirve también para Batocera, RetroBat y otros frontends que exporten gamelist.xml.",
  "Facultatif : dossier de données de Cocoon. Dans Cocoon, utilisez Settings → Library & Data → Export Metadata ; Repisa y lit genres, dates, jaquettes et temps de jeu. Fonctionne aussi avec Batocera, RetroBat, etc.",
  "Optional: Cocoon-Datenordner. In Cocoon Settings → Library & Data → Export Metadata nutzen; Repisa liest daraus Genres, Daten, Cover und Spielzeit. Funktioniert auch mit Batocera, RetroBat usw.",
  "Facoltativo: cartella dati di Cocoon. In Cocoon usa Settings → Library & Data → Export Metadata; Repisa legge generi, date, copertine e tempo di gioco. Funziona anche con Batocera, RetroBat, ecc.",
  "Opcional: pasta de dados do Cocoon. No Cocoon use Settings → Library & Data → Export Metadata; o Repisa lê gêneros, datas, capas e tempo de jogo. Também funciona com Batocera, RetroBat etc.",
  ptPT="Opcional: pasta de dados do Cocoon. No Cocoon use Settings → Library & Data → Export Metadata; o Repisa lê géneros, datas, capas e tempo de jogo. Também funciona com Batocera, RetroBat, etc.")
s("choose_cocoon", "Choose Cocoon folder", "Elegir carpeta de Cocoon", "Choisir le dossier Cocoon",
  "Cocoon-Ordner wählen", "Scegli cartella Cocoon", "Escolher pasta do Cocoon")
s("scan_emulators", "Scan emulators", "Escanear emuladores", "Analyser les émulateurs", "Emulatoren scannen",
  "Analizza emulatori", "Escanear emuladores", ptPT="Analisar emuladores")
s("emu_per_system_hint",
  "Emulator used to open each system (if none is chosen, Android will ask). Repisa measures the time you play what you open from here.",
  "Emulador para abrir cada sistema (si no eliges ninguno, Android preguntará). Repisa mide el tiempo que juegas a lo que abres desde aquí.",
  "Émulateur pour chaque système (sinon Android demandera). Repisa mesure le temps de jeu de ce que vous lancez d’ici.",
  "Emulator für jedes System (sonst fragt Android). Repisa misst die Spielzeit von allem, was du hier startest.",
  "Emulatore per ogni sistema (altrimenti Android chiederà). Repisa misura il tempo di ciò che avvii da qui.",
  "Emulador para cada sistema (se não escolher, o Android perguntará). O Repisa mede o tempo do que você abre daqui.",
  ptPT="Emulador para cada sistema (se não escolher, o Android pergunta). O Repisa mede o tempo do que abre daqui.")
s("emulator_for", "Emulator for %1$s", "Emulador para %1$s", "Émulateur pour %1$s", "Emulator für %1$s",
  "Emulatore per %1$s", "Emulador para %1$s")
s("ask_every_time", "Ask every time", "Preguntar cada vez", "Toujours demander", "Jedes Mal fragen",
  "Chiedi ogni volta", "Perguntar sempre")
s("covers_hint", "Each game has a \"Find cover\" button in its details. This button looks for covers for every game that doesn't have one.",
  "Cada juego tiene un botón \"Buscar carátula\" en su ficha. Este botón busca carátula para todos los juegos que no tienen.",
  "Chaque jeu a un bouton « Chercher une jaquette » dans sa fiche. Ce bouton en cherche pour tous les jeux qui n’en ont pas.",
  "Jedes Spiel hat in seinen Details eine Schaltfläche „Cover suchen“. Diese Schaltfläche sucht Cover für alle Spiele ohne.",
  "Ogni gioco ha un pulsante «Cerca copertina» nella scheda. Questo pulsante la cerca per tutti i giochi che non ce l’hanno.",
  "Cada jogo tem um botão \"Buscar capa\" na ficha. Este botão busca capa para todos os jogos que não têm.",
  es419="Cada juego tiene un botón \"Buscar portada\" en su ficha. Este botón busca portada para todos los juegos que no tienen.",
  ptPT="Cada jogo tem um botão \"Procurar capa\" na ficha. Este botão procura capa para todos os jogos que não têm.")
s("find_missing", "Find missing covers", "Buscar carátulas que faltan", "Chercher les jaquettes manquantes",
  "Fehlende Cover suchen", "Cerca copertine mancanti", "Buscar capas que faltam", es419="Buscar portadas que faltan",
  ptPT="Procurar capas em falta")
s("sgdb_hint", "Recommended: SteamGridDB, the cover database Cocoon uses (free). Create an account, go to Preferences → API and paste your key here.",
  "Recomendado: SteamGridDB, la base de carátulas que usa Cocoon (gratis). Crea una cuenta, ve a Preferencias → API y pega aquí tu clave.",
  "Recommandé : SteamGridDB, la base de jaquettes utilisée par Cocoon (gratuite). Créez un compte, allez dans Preferences → API et collez votre clé ici.",
  "Empfohlen: SteamGridDB, die Cover-Datenbank von Cocoon (kostenlos). Konto erstellen, Preferences → API öffnen und Schlüssel hier einfügen.",
  "Consigliato: SteamGridDB, il database di copertine usato da Cocoon (gratis). Crea un account, vai su Preferences → API e incolla qui la chiave.",
  "Recomendado: SteamGridDB, a base de capas que o Cocoon usa (grátis). Crie uma conta, vá em Preferences → API e cole sua chave aqui.",
  es419="Recomendado: SteamGridDB, la base de portadas que usa Cocoon (gratis). Crea una cuenta, ve a Preferences → API y pega aquí tu clave.",
  ptPT="Recomendado: SteamGridDB, a base de capas que o Cocoon usa (grátis). Crie uma conta, vá a Preferences → API e cole aqui a sua chave.")
s("sgdb_get", "Get a SteamGridDB key", "Conseguir clave de SteamGridDB", "Obtenir une clé SteamGridDB",
  "SteamGridDB-Schlüssel holen", "Ottieni chiave SteamGridDB", "Obter chave do SteamGridDB")
s("sgdb_key", "SteamGridDB key", "Clave de SteamGridDB", "Clé SteamGridDB", "SteamGridDB-Schlüssel",
  "Chiave SteamGridDB", "Chave do SteamGridDB")
s("save_key", "Save key", "Guardar clave", "Enregistrer la clé", "Schlüssel speichern", "Salva chiave",
  "Salvar chave", ptPT="Guardar chave")
s("libretro_toggle", "libretro-thumbnails covers for emulators", "Carátulas de libretro-thumbnails para emuladores",
  "Jaquettes libretro-thumbnails pour les émulateurs", "libretro-thumbnails-Cover für Emulatoren",
  "Copertine libretro-thumbnails per emulatori", "Capas do libretro-thumbnails para emuladores",
  es419="Portadas de libretro-thumbnails para emuladores")
s("autosync_hint",
  "Steam, Epic and emulators are read again automatically when you open Repisa or come back to it, so hours played on your PC, another frontend or RetroArch show up here.",
  "Steam, Epic y emuladores se vuelven a leer solos al abrir Repisa o volver a ella, así que las horas que juegues en el PC, en otro frontend o en RetroArch aparecen aquí.",
  "Steam, Epic et les émulateurs sont relus automatiquement à l’ouverture de Repisa : les heures jouées sur PC, un autre frontend ou RetroArch apparaissent ici.",
  "Steam, Epic und Emulatoren werden beim Öffnen von Repisa automatisch neu gelesen: Stunden auf dem PC, in einem anderen Frontend oder RetroArch erscheinen hier.",
  "Steam, Epic ed emulatori vengono riletti da soli quando apri Repisa: le ore giocate su PC, in un altro frontend o in RetroArch compaiono qui.",
  "Steam, Epic e emuladores são lidos de novo ao abrir o Repisa: as horas jogadas no PC, em outro frontend ou no RetroArch aparecem aqui.",
  es419="Steam, Epic y emuladores se vuelven a leer solos al abrir Repisa o volver a ella, así que las horas que juegues en la PC, en otro frontend o en RetroArch aparecen aquí.",
  ptPT="A Steam, a Epic e os emuladores voltam a ser lidos ao abrir o Repisa: as horas jogadas no PC, noutro frontend ou no RetroArch aparecem aqui.")
s("usage_granted", "Usage access: granted ✓", "Acceso a datos de uso: concedido ✓",
  "Accès aux données d’utilisation : accordé ✓", "Nutzungszugriff: erteilt ✓", "Accesso ai dati di utilizzo: concesso ✓",
  "Acesso a dados de uso: concedido ✓", ptPT="Acesso aos dados de utilização: concedido ✓")
s("usage_denied", "Usage access: not granted", "Acceso a datos de uso: no concedido",
  "Accès aux données d’utilisation : non accordé", "Nutzungszugriff: nicht erteilt",
  "Accesso ai dati di utilizzo: non concesso", "Acesso a dados de uso: não concedido",
  ptPT="Acesso aos dados de utilização: não concedido")
s("usage_hint",
  "With this Android permission (the same one Cocoon uses), Repisa measures only the time the emulator is actually on screen for games you open from here.",
  "Con este permiso de Android (el mismo que usa Cocoon), Repisa mide solo el tiempo que el emulador está realmente en pantalla en los juegos que abres desde aquí.",
  "Avec cette autorisation Android (la même que Cocoon), Repisa ne mesure que le temps où l’émulateur est réellement à l’écran.",
  "Mit dieser Android-Berechtigung (dieselbe wie bei Cocoon) misst Repisa nur die Zeit, in der der Emulator wirklich auf dem Bildschirm ist.",
  "Con questo permesso Android (lo stesso di Cocoon), Repisa misura solo il tempo in cui l’emulatore è davvero sullo schermo.",
  "Com esta permissão do Android (a mesma do Cocoon), o Repisa mede só o tempo em que o emulador está realmente na tela.",
  ptPT="Com esta permissão do Android (a mesma do Cocoon), o Repisa mede só o tempo em que o emulador está realmente no ecrã.")
s("grant", "Grant", "Conceder", "Autoriser", "Erteilen", "Concedi", "Conceder")
s("second_screen", "Use the second screen", "Usar la segunda pantalla", "Utiliser le second écran",
  "Zweiten Bildschirm verwenden", "Usa il secondo schermo", "Usar a segunda tela", ptPT="Usar o segundo ecrã")
s("second_screen_hint", "On the AYN Thor, the bottom screen shows the selected game's details.",
  "En la AYN Thor, la pantalla de abajo muestra la ficha del juego seleccionado.",
  "Sur l’AYN Thor, l’écran du bas affiche la fiche du jeu sélectionné.",
  "Auf dem AYN Thor zeigt der untere Bildschirm die Details des gewählten Spiels.",
  "Su AYN Thor, lo schermo inferiore mostra la scheda del gioco selezionato.",
  "No AYN Thor, a tela de baixo mostra a ficha do jogo selecionado.",
  ptPT="No AYN Thor, o ecrã de baixo mostra a ficha do jogo selecionado.")
s("box_size", "Box size: %1$d dp", "Tamaño de las cajas: %1$d dp", "Taille des boîtes : %1$d dp", "Boxgröße: %1$d dp",
  "Dimensione scatole: %1$d dp", "Tamanho das caixas: %1$d dp")
s("box_size_hint", "Height of a DVD case; the other boxes are scaled to their real size.",
  "Altura de una caja de DVD; el resto de cajas, a su tamaño real.",
  "Hauteur d’un boîtier DVD ; les autres boîtes sont à leur taille réelle.",
  "Höhe einer DVD-Hülle; die anderen Boxen haben ihre echte Größe.",
  "Altezza di una custodia DVD; le altre scatole sono in scala reale.",
  "Altura de uma caixa de DVD; as outras caixas ficam no tamanho real.")
s("welcome", "Welcome to Repisa", "Te damos la bienvenida a Repisa", "Bienvenue dans Repisa", "Willkommen bei Repisa",
  "Benvenuto in Repisa", "Boas-vindas ao Repisa")
s("choose_language", "Choose your language", "Elige tu idioma", "Choisissez votre langue", "Wähle deine Sprache",
  "Scegli la tua lingua", "Escolha seu idioma", ptPT="Escolha o seu idioma")

# ------------------------------------------------------------------ Mensajes
s("msg_sync_count", "%1$s: %2$d games", "%1$s: %2$d juegos", "%1$s : %2$d jeux", "%1$s: %2$d Spiele",
  "%1$s: %2$d giochi", "%1$s: %2$d jogos")
s("msg_connect_sources", "Connect Steam, Epic or your emulator folders in Settings.",
  "Conecta Steam, Epic o tus carpetas de emuladores en Ajustes.",
  "Connectez Steam, Epic ou vos dossiers d’émulateurs dans Paramètres.",
  "Verbinde Steam, Epic oder deine Emulator-Ordner in den Einstellungen.",
  "Collega Steam, Epic o le cartelle degli emulatori nelle Impostazioni.",
  "Conecte a Steam, a Epic ou suas pastas de emuladores nas Configurações.",
  es419="Conecta Steam, Epic o tus carpetas de emuladores en Configuración.",
  ptPT="Ligue a Steam, a Epic ou as pastas de emuladores nas Definições.")
s("msg_time_updated", "Playtime updated: %1$s (+%2$s)", "Tiempo actualizado: %1$s (+%2$s)",
  "Temps mis à jour : %1$s (+%2$s)", "Spielzeit aktualisiert: %1$s (+%2$s)", "Tempo aggiornato: %1$s (+%2$s)",
  "Tempo atualizado: %1$s (+%2$s)")
s("msg_and_more", "%1$s and %2$d more", "%1$s y %2$d más", "%1$s et %2$d de plus", "%1$s und %2$d weitere",
  "%1$s e altri %2$d", "%1$s e mais %2$d")
s("msg_family_error", "Steam: couldn't read the family group (%1$s)", "Steam: no se pudo leer el grupo familiar (%1$s)",
  "Steam : impossible de lire le groupe familial (%1$s)", "Steam: Familiengruppe konnte nicht gelesen werden (%1$s)",
  "Steam: impossibile leggere il gruppo famiglia (%1$s)", "Steam: não foi possível ler o grupo familiar (%1$s)")
s("msg_steam_linked", "Steam account linked", "Cuenta de Steam vinculada", "Compte Steam lié", "Steam-Konto verknüpft",
  "Account Steam collegato", "Conta da Steam vinculada", ptPT="Conta Steam associada")
s("msg_steam_link_failed", "Steam: couldn't link the account (%1$s).", "Steam: no se pudo vincular la cuenta (%1$s).",
  "Steam : impossible de lier le compte (%1$s).", "Steam: Konto konnte nicht verknüpft werden (%1$s).",
  "Steam: impossibile collegare l’account (%1$s).", "Steam: não foi possível vincular a conta (%1$s).",
  ptPT="Steam: não foi possível associar a conta (%1$s).")
s("msg_epic_linked", "Epic Games account linked", "Cuenta de Epic Games vinculada", "Compte Epic Games lié",
  "Epic-Games-Konto verknüpft", "Account Epic Games collegato", "Conta da Epic Games vinculada",
  ptPT="Conta Epic Games associada")
s("msg_epic_login_failed", "Epic: couldn't sign in (%1$s). The code expires in a few minutes; try again.",
  "Epic: no se pudo iniciar sesión (%1$s). El código caduca en unos minutos; vuelve a intentarlo.",
  "Epic : connexion impossible (%1$s). Le code expire en quelques minutes ; réessayez.",
  "Epic: Anmeldung fehlgeschlagen (%1$s). Der Code läuft nach wenigen Minuten ab; versuche es erneut.",
  "Epic: accesso non riuscito (%1$s). Il codice scade in pochi minuti; riprova.",
  "Epic: não foi possível entrar (%1$s). O código expira em poucos minutos; tente de novo.",
  es419="Epic: no se pudo iniciar sesión (%1$s). El código vence en unos minutos; vuelve a intentarlo.",
  ptPT="Epic: não foi possível iniciar sessão (%1$s). O código expira em poucos minutos; tente novamente.")
s("msg_epic_code_expired", "The code expired. Request a new one.", "El código caducó. Pide uno nuevo.",
  "Le code a expiré. Demandez-en un nouveau.", "Der Code ist abgelaufen. Fordere einen neuen an.",
  "Il codice è scaduto. Chiedine uno nuovo.", "O código expirou. Peça um novo.", es419="El código venció. Pide uno nuevo.")
s("msg_epic_no_conn", "couldn't connect to Epic Games. Check your internet connection.",
  "no se pudo conectar con Epic Games. Revisa la conexión a internet.",
  "connexion à Epic Games impossible. Vérifiez votre connexion.",
  "keine Verbindung zu Epic Games. Prüfe deine Internetverbindung.",
  "impossibile connettersi a Epic Games. Controlla la connessione.",
  "não foi possível conectar à Epic Games. Verifique sua conexão.",
  ptPT="não foi possível ligar à Epic Games. Verifique a ligação à internet.")
s("msg_session_added", "%1$s: +%2$s played", "%1$s: +%2$s de juego", "%1$s : +%2$s de jeu", "%1$s: +%2$s gespielt",
  "%1$s: +%2$s di gioco", "%1$s: +%2$s de jogo")
s("msg_session_approx", " (approx.)", " (aprox.)", " (env.)", " (ca.)", " (circa)", " (aprox.)")
s("msg_all_covers", "Every box has a cover", "Todas las cajas tienen carátula", "Toutes les boîtes ont une jaquette",
  "Alle Boxen haben ein Cover", "Tutte le scatole hanno la copertina", "Todas as caixas têm capa",
  es419="Todas las cajas tienen portada")
s("msg_covers_found", "Covers found: %1$d of %2$d.", "Carátulas encontradas: %1$d de %2$d.",
  "Jaquettes trouvées : %1$d sur %2$d.", "Gefundene Cover: %1$d von %2$d.", "Copertine trovate: %1$d su %2$d.",
  "Capas encontradas: %1$d de %2$d.", es419="Portadas encontradas: %1$d de %2$d.")
s("msg_covers_rest", " For the rest, open the game and use \"Find cover\".",
  " Para el resto, abre el juego y usa \"Buscar carátula\".",
  " Pour le reste, ouvrez le jeu et utilisez « Chercher une jaquette ».",
  " Für den Rest öffne das Spiel und nutze „Cover suchen“.",
  " Per il resto, apri il gioco e usa «Cerca copertina».",
  " Para o resto, abra o jogo e use \"Buscar capa\".",
  es419=" Para el resto, abre el juego y usa \"Buscar portada\".",
  ptPT=" Para o resto, abra o jogo e use \"Procurar capa\".")
s("status_syncing", "Syncing %1$s…", "Sincronizando %1$s…", "Synchronisation de %1$s…", "%1$s wird synchronisiert…",
  "Sincronizzazione di %1$s…", "Sincronizando %1$s…", ptPT="A sincronizar %1$s…")
s("status_checking_covers", "Checking covers…", "Comprobando carátulas…", "Vérification des jaquettes…",
  "Cover werden geprüft…", "Controllo copertine…", "Verificando capas…", es419="Revisando portadas…",
  ptPT="A verificar capas…")
s("status_finding_covers", "Finding covers (%1$d/%2$d)…", "Buscando carátulas (%1$d/%2$d)…",
  "Recherche de jaquettes (%1$d/%2$d)…", "Cover werden gesucht (%1$d/%2$d)…", "Ricerca copertine (%1$d/%2$d)…",
  "Buscando capas (%1$d/%2$d)…", es419="Buscando portadas (%1$d/%2$d)…", ptPT="A procurar capas (%1$d/%2$d)…")
s("status_metadata", "Looking for covers and details (%1$d/%2$d)…", "Buscando carátulas y datos (%1$d/%2$d)…",
  "Recherche de jaquettes et d’infos (%1$d/%2$d)…", "Cover und Daten werden gesucht (%1$d/%2$d)…",
  "Ricerca copertine e dati (%1$d/%2$d)…", "Buscando capas e dados (%1$d/%2$d)…",
  es419="Buscando portadas y datos (%1$d/%2$d)…", ptPT="A procurar capas e dados (%1$d/%2$d)…")
s("status_linking_steam", "Linking Steam account…", "Vinculando cuenta de Steam…", "Liaison du compte Steam…",
  "Steam-Konto wird verknüpft…", "Collegamento account Steam…", "Vinculando conta da Steam…",
  ptPT="A associar conta Steam…")
s("status_epic_connecting", "Connecting to Epic Games…", "Conectando con Epic Games…", "Connexion à Epic Games…",
  "Verbindung mit Epic Games…", "Connessione a Epic Games…", "Conectando à Epic Games…", ptPT="A ligar à Epic Games…")
s("status_scanning_roms", "Looking for ROMs in %1$s…", "Buscando ROMs en %1$s…", "Recherche de ROM dans %1$s…",
  "ROMs werden in %1$s gesucht…", "Ricerca ROM in %1$s…", "Buscando ROMs em %1$s…", ptPT="A procurar ROMs em %1$s…")
s("status_reading", "Reading %1$s data…", "Leyendo datos de %1$s…", "Lecture des données de %1$s…",
  "%1$s-Daten werden gelesen…", "Lettura dati di %1$s…", "Lendo dados do %1$s…", ptPT="A ler dados do %1$s…")
s("status_ra_playlists", "Reading RetroArch playlists…", "Leyendo playlists de RetroArch…",
  "Lecture des playlists RetroArch…", "RetroArch-Playlists werden gelesen…", "Lettura playlist di RetroArch…",
  "Lendo playlists do RetroArch…", ptPT="A ler playlists do RetroArch…")
s("status_ra_time", "Reading RetroArch playtime…", "Leyendo tiempo de juego de RetroArch…",
  "Lecture du temps de jeu RetroArch…", "RetroArch-Spielzeit wird gelesen…", "Lettura tempo di gioco di RetroArch…",
  "Lendo tempo de jogo do RetroArch…", ptPT="A ler tempo de jogo do RetroArch…")
s("err_steam_session", "the Steam session has expired. Link your account again in Settings.",
  "la sesión de Steam ha caducado. Vuelve a vincular tu cuenta en Ajustes.",
  "la session Steam a expiré. Liez à nouveau votre compte dans Paramètres.",
  "die Steam-Sitzung ist abgelaufen. Verknüpfe dein Konto in den Einstellungen erneut.",
  "la sessione Steam è scaduta. Ricollega l’account nelle Impostazioni.",
  "a sessão da Steam expirou. Vincule sua conta de novo nas Configurações.",
  es419="la sesión de Steam venció. Vuelve a vincular tu cuenta en Configuración.",
  ptPT="a sessão da Steam expirou. Associe a conta novamente nas Definições.")
s("err_launch_nofile", "I don't know where this game's file is. Add its ROM folder in Settings.",
  "No sé dónde está el archivo de este juego. Añade su carpeta de ROMs en Ajustes.",
  "Fichier du jeu introuvable. Ajoutez son dossier de ROM dans Paramètres.",
  "Die Spieldatei ist unbekannt. Füge ihren ROM-Ordner in den Einstellungen hinzu.",
  "Non so dov’è il file del gioco. Aggiungi la cartella ROM nelle Impostazioni.",
  "Não sei onde está o arquivo deste jogo. Adicione a pasta de ROMs nas Configurações.",
  es419="No sé dónde está el archivo de este juego. Agrega su carpeta de ROMs en Configuración.",
  ptPT="Não sei onde está o ficheiro deste jogo. Adicione a pasta de ROMs nas Definições.")
s("err_launch_noemu", "Emulator not found. Choose one for %1$s in Settings.",
  "No se encontró el emulador. Elige uno para %1$s en Ajustes.",
  "Émulateur introuvable. Choisissez-en un pour %1$s dans Paramètres.",
  "Emulator nicht gefunden. Wähle einen für %1$s in den Einstellungen.",
  "Emulatore non trovato. Scegline uno per %1$s nelle Impostazioni.",
  "Emulador não encontrado. Escolha um para %1$s nas Configurações.",
  es419="No se encontró el emulador. Elige uno para %1$s en Configuración.",
  ptPT="Emulador não encontrado. Escolha um para %1$s nas Definições.")
s("err_launch_denied", "Android didn't allow opening the game: %1$s", "Android no permitió abrir el juego: %1$s",
  "Android n’a pas autorisé l’ouverture du jeu : %1$s", "Android hat das Öffnen des Spiels verhindert: %1$s",
  "Android non ha permesso di aprire il gioco: %1$s", "O Android não permitiu abrir o jogo: %1$s")
s("err_steam_no_games_key", "Steam returned no games. In your Steam profile, Privacy → 'Game details' must be Public.",
  "Steam no devolvió juegos. En tu perfil de Steam, Privacidad → 'Detalles de juegos' debe estar en Público.",
  "Steam n’a renvoyé aucun jeu. Dans votre profil, Confidentialité → « Détails des jeux » doit être public.",
  "Steam hat keine Spiele geliefert. Im Profil muss Privatsphäre → „Spieldetails“ öffentlich sein.",
  "Steam non ha restituito giochi. Nel profilo, Privacy → «Dettagli dei giochi» deve essere pubblico.",
  "A Steam não devolveu jogos. No perfil, Privacidade → 'Detalhes de jogos' deve estar público.")
s("err_steam_no_games_linked", "Steam returned no games. Try unlinking and linking your account again.",
  "Steam no devolvió juegos. Prueba a desvincular y volver a vincular tu cuenta.",
  "Steam n’a renvoyé aucun jeu. Essayez de délier puis de relier votre compte.",
  "Steam hat keine Spiele geliefert. Trenne dein Konto und verknüpfe es erneut.",
  "Steam non ha restituito giochi. Prova a scollegare e ricollegare l’account.",
  "A Steam não devolveu jogos. Tente desvincular e vincular a conta de novo.",
  ptPT="A Steam não devolveu jogos. Tente desassociar e associar a conta novamente.")


# ------------------------------------------------------------------ Copias del mismo juego
s("copies_button", "Copies (%1$d)", "Copias (%1$d)", "Copies (%1$d)", "Kopien (%1$d)", "Copie (%1$d)", "Cópias (%1$d)")
s("copies_title", "Preferred copy", "Copia preferente", "Copie préférée", "Bevorzugte Kopie", "Copia preferita", "Cópia preferida")
s("copies_hint",
  "These copies of the same game are shown as a single box, with their playtime added up. Choose which one represents it (cover, details and store).",
  "Estas copias del mismo juego se muestran como una sola caja, con el tiempo de juego sumado. Elige cuál la representa (carátula, datos y tienda).",
  "Ces copies du même jeu sont affichées dans une seule boîte, avec le temps de jeu cumulé. Choisissez celle qui la représente (jaquette, infos et boutique).",
  "Diese Kopien desselben Spiels werden als eine Box angezeigt, mit addierter Spielzeit. Wähle, welche sie repräsentiert (Cover, Daten und Store).",
  "Queste copie dello stesso gioco sono mostrate come un’unica scatola, con il tempo di gioco sommato. Scegli quale la rappresenta (copertina, dati e store).",
  "Estas cópias do mesmo jogo aparecem como uma só caixa, com o tempo de jogo somado. Escolha qual a representa (capa, dados e loja).",
  es419="Estas copias del mismo juego se muestran como una sola caja, con el tiempo de juego sumado. Elige cuál la representa (portada, datos y tienda).")
s("copy_preferred", "Preferred", "Preferente", "Préférée", "Bevorzugt", "Preferita", "Preferida")
s("copy_split", "Separate", "Separar", "Séparer", "Trennen", "Separa", "Separar")
s("copy_split_hint", "\"Separate\" gives that copy its own box again.",
  "\"Separar\" vuelve a darle a esa copia su propia caja.", "« Séparer » redonne à cette copie sa propre boîte.",
  "„Trennen“ gibt dieser Kopie wieder eine eigene Box.", "«Separa» ridà a quella copia la sua scatola.",
  "\"Separar\" devolve a essa cópia a sua própria caixa.")


# ------------------------------------------------------------------ Mis calificaciones: orden y filtros
s("rated_sort_high", "Highest first", "Más alta primero", "Meilleure d’abord", "Höchste zuerst", "Più alto prima", "Maior primeiro")
s("rated_sort_low", "Lowest first", "Más baja primero", "Moins bonne d’abord", "Niedrigste zuerst", "Più basso prima", "Menor primeiro")
s("rated_filter_none", "No ratings in this range.", "No hay calificaciones en este rango.", "Aucune note dans cette plage.",
  "Keine Bewertungen in diesem Bereich.", "Nessuna valutazione in questo intervallo.", "Não há avaliações neste intervalo.")

# ------------------------------------------------------------------ Géneros (clave interna en español → texto)
GENRES = [
    ("Acción", "Action", "Acción", "Action", "Action", "Azione", "Ação"),
    ("Aventura", "Adventure", "Aventura", "Aventure", "Adventure", "Avventura", "Aventura"),
    ("Novela visual", "Visual novel", "Novela visual", "Visual novel", "Visual Novel", "Visual novel", "Visual novel"),
    ("Rol (RPG)", "RPG", "Rol (RPG)", "RPG", "Rollenspiel", "GDR", "RPG"),
    ("Estrategia", "Strategy", "Estrategia", "Stratégie", "Strategie", "Strategia", "Estratégia"),
    ("Simulación", "Simulation", "Simulación", "Simulation", "Simulation", "Simulazione", "Simulação"),
    ("Deportes", "Sports", "Deportes", "Sport", "Sport", "Sport", "Esportes"),
    ("Carreras", "Racing", "Carreras", "Course", "Rennspiel", "Corse", "Corrida"),
    ("Plataformas", "Platformer", "Plataformas", "Plateformes", "Jump ’n’ Run", "Piattaforme", "Plataforma"),
    ("Disparos", "Shooter", "Disparos", "Tir", "Shooter", "Sparatutto", "Tiro"),
    ("Lucha", "Fighting", "Lucha", "Combat", "Kampfspiel", "Picchiaduro", "Luta"),
    ("Beat 'em up", "Beat 'em up", "Beat 'em up", "Beat 'em up", "Beat 'em up", "Picchiaduro a scorrimento", "Beat 'em up"),
    ("Puzle", "Puzzle", "Puzle", "Réflexion", "Puzzle", "Rompicapo", "Quebra-cabeça"),
    ("Música / Ritmo", "Music / Rhythm", "Música / Ritmo", "Musique / Rythme", "Musik / Rhythmus", "Musica / Ritmo", "Música / Ritmo"),
    ("Arcade", "Arcade", "Arcade", "Arcade", "Arcade", "Arcade", "Arcade"),
    ("Indie", "Indie", "Indie", "Indé", "Indie", "Indie", "Indie"),
    ("Casual", "Casual", "Casual", "Casual", "Casual", "Casual", "Casual"),
    ("Mesa y cartas", "Board & cards", "Mesa y cartas", "Société et cartes", "Brett- & Kartenspiel", "Tavolo e carte", "Tabuleiro e cartas"),
    ("MMO", "MMO", "MMO", "MMO", "MMO", "MMO", "MMO"),
    ("Free to play", "Free to play", "Free to play", "Free-to-play", "Free-to-play", "Free to play", "Free to play"),
    ("Terror", "Horror", "Terror", "Horreur", "Horror", "Horror", "Terror"),
    ("Supervivencia", "Survival", "Supervivencia", "Survie", "Survival", "Sopravvivenza", "Sobrevivência"),
    ("Mundo abierto", "Open world", "Mundo abierto", "Monde ouvert", "Open World", "Mondo aperto", "Mundo aberto"),
    ("Metroidvania", "Metroidvania", "Metroidvania", "Metroidvania", "Metroidvania", "Metroidvania", "Metroidvania"),
    ("Roguelike", "Roguelike", "Roguelike", "Roguelike", "Roguelike", "Roguelike", "Roguelike"),
    ("MOBA", "MOBA", "MOBA", "MOBA", "MOBA", "MOBA", "MOBA"),
    ("Acceso anticipado", "Early access", "Acceso anticipado", "Accès anticipé", "Early Access", "Accesso anticipato", "Acesso antecipado"),
    ("Educativo", "Educational", "Educativo", "Éducatif", "Lernspiel", "Educativo", "Educativo"),
]
for i, (_, en, es, fr, de, it, pt) in enumerate(GENRES):
    s(f"genre_{i}", en, es, fr, de, it, pt)

PLURALS = {"games_count": ("games_count_one", "games_count_other")}


def esc(t: str) -> str:
    t = t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    t = t.replace("\\", "\\\\").replace("'", "\\'").replace('"', '\\"')
    if t.startswith("@") or t.startswith("?"):
        t = "\\" + t
    return t


def text(entry, lang):
    return entry.get(lang) or entry[FALLBACK.get(lang, "en")]


def main():
    root = os.path.join(os.path.dirname(__file__), "..", "..", "app", "src", "main", "res")
    for lang, folder in LOCALES.items():
        lines = ['<?xml version="1.0" encoding="utf-8"?>',
                 "<!-- Generado por tools/i18n/strings.py: no editar a mano. -->",
                 '<resources xmlns:tools="http://schemas.android.com/tools" tools:ignore="MissingTranslation,UnusedResources">']
        if lang == "en":
            lines.append('    <string name="app_name" translatable="false">Repisa</string>')
        plural_parts = {p for pair in PLURALS.values() for p in pair}
        for key, entry in S.items():
            if key in plural_parts:
                continue
            t = text(entry, lang)
            fmt = ' formatted="false"' if "%" in t and not re.search(r"%\d+\$", t) else ""
            lines.append(f'    <string name="{key}"{fmt}>{esc(t)}</string>')
        for name, (one, other) in PLURALS.items():
            lines.append(f'    <plurals name="{name}">')
            lines.append(f'        <item quantity="one">{esc(text(S[one], lang))}</item>')
            lines.append(f'        <item quantity="other">{esc(text(S[other], lang))}</item>')
            lines.append("    </plurals>")
        lines.append("    <string-array name=\"genre_keys\" translatable=\"false\">" if lang == "en" else "")
        if lang == "en":
            lines += [f"        <item>{esc(g[0])}</item>" for g in GENRES]
            lines.append("    </string-array>")
        lines.append("</resources>")
        out = os.path.join(root, folder)
        os.makedirs(out, exist_ok=True)
        with open(os.path.join(out, "strings.xml"), "w", encoding="utf-8") as f:
            f.write("\n".join(l for l in lines if l != "") + "\n")
    print(f"{len(S)} textos × {len(LOCALES)} idiomas")


if __name__ == "__main__":
    main()
