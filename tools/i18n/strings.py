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
s("folder_deep_hint",
  "Tip: just choose the main folder (for example \"RetroArch\", \"Cocoon\" or \"ROMs\"). Repisa searches all its subfolders until it finds what it needs.",
  "Consejo: basta con elegir la carpeta principal (por ejemplo «RetroArch», «Cocoon» o «ROMs»). Repisa busca en todas sus subcarpetas hasta encontrar lo que necesita.",
  "Astuce : choisissez simplement le dossier principal (par ex. « RetroArch », « Cocoon » ou « ROMs »). Repisa cherche dans tous ses sous-dossiers.",
  "Tipp: Wähle einfach den Hauptordner (z. B. „RetroArch“, „Cocoon“ oder „ROMs“). Repisa durchsucht alle Unterordner.",
  "Consiglio: basta scegliere la cartella principale (ad es. «RetroArch», «Cocoon» o «ROMs»). Repisa cerca in tutte le sottocartelle.",
  "Dica: basta escolher a pasta principal (por exemplo \"RetroArch\", \"Cocoon\" ou \"ROMs\"). O Repisa procura em todas as subpastas até encontrar o que precisa.")
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
  "Cocoon (optional) — how to:\n1. In Cocoon: Settings → Library & Data → Export Metadata. If it asks for a folder, pick or create one (e.g. \"Cocoon export\") and remember it.\n2. Optional, for playtime: in Cocoon create a backup (Backup & Restore); it is saved in Cocoon's data folder.\n3. Here, tap \"Choose Cocoon folder\" and pick that folder (the export folder or Cocoon's data folder; Repisa looks inside subfolders).\n4. Repisa reads it right away and shows below what it found. Also works with ES-DE, Batocera and RetroBat.",
  "Cocoon (opcional) — cómo hacerlo:\n1. En Cocoon: Ajustes → Library & Data → Export Metadata. Si te pide una carpeta, elige o crea una (p. ej. «Cocoon export») y recuérdala.\n2. Opcional, para el tiempo de juego: en Cocoon crea una copia de seguridad (Backup & Restore); se guarda en la carpeta de datos de Cocoon.\n3. Aquí, pulsa «Elegir carpeta de Cocoon» y elige esa carpeta (la de la exportación o la carpeta de datos de Cocoon; Repisa busca también dentro de las subcarpetas).\n4. Repisa la lee al momento y te muestra abajo lo que encontró. Sirve también para ES-DE, Batocera y RetroBat.",
  "Cocoon (facultatif) — mode d’emploi :\n1. Dans Cocoon : Settings → Library & Data → Export Metadata. S’il demande un dossier, choisissez-en ou créez-en un (ex. « Cocoon export »).\n2. Facultatif, pour le temps de jeu : créez une sauvegarde dans Cocoon (Backup & Restore) ; elle est enregistrée dans le dossier de données de Cocoon.\n3. Ici, touchez « Choisir le dossier Cocoon » et choisissez ce dossier (export ou dossier de données ; Repisa cherche aussi dans les sous-dossiers).\n4. Repisa le lit aussitôt et affiche ci-dessous ce qu’il a trouvé. Fonctionne aussi avec ES-DE, Batocera et RetroBat.",
  "Cocoon (optional) – so geht’s:\n1. In Cocoon: Settings → Library & Data → Export Metadata. Wenn ein Ordner verlangt wird, einen wählen oder anlegen (z. B. „Cocoon export“).\n2. Optional, für die Spielzeit: in Cocoon ein Backup erstellen (Backup & Restore); es liegt im Cocoon-Datenordner.\n3. Hier „Cocoon-Ordner wählen“ tippen und diesen Ordner wählen (Export- oder Datenordner; Repisa sucht auch in Unterordnern).\n4. Repisa liest ihn sofort und zeigt unten, was gefunden wurde. Funktioniert auch mit ES-DE, Batocera und RetroBat.",
  "Cocoon (facoltativo) — come fare:\n1. In Cocoon: Settings → Library & Data → Export Metadata. Se chiede una cartella, scegline o creane una (es. «Cocoon export»).\n2. Facoltativo, per il tempo di gioco: crea un backup in Cocoon (Backup & Restore); viene salvato nella cartella dati di Cocoon.\n3. Qui, tocca «Scegli cartella Cocoon» e scegli quella cartella (export o cartella dati; Repisa cerca anche nelle sottocartelle).\n4. Repisa la legge subito e mostra qui sotto cosa ha trovato. Funziona anche con ES-DE, Batocera e RetroBat.",
  "Cocoon (opcional) — como fazer:\n1. No Cocoon: Settings → Library & Data → Export Metadata. Se pedir uma pasta, escolha ou crie uma (ex. \"Cocoon export\").\n2. Opcional, para o tempo de jogo: crie um backup no Cocoon (Backup & Restore); ele fica na pasta de dados do Cocoon.\n3. Aqui, toque em \"Escolher pasta do Cocoon\" e escolha essa pasta (a da exportação ou a de dados; o Repisa procura também nas subpastas).\n4. O Repisa lê na hora e mostra abaixo o que encontrou. Também funciona com ES-DE, Batocera e RetroBat.",
  es419="Cocoon (opcional) — cómo hacerlo:\n1. En Cocoon: Ajustes → Library & Data → Export Metadata. Si te pide una carpeta, elige o crea una (p. ej. «Cocoon export») y recuérdala.\n2. Opcional, para el tiempo de juego: en Cocoon crea una copia de seguridad (Backup & Restore); se guarda en la carpeta de datos de Cocoon.\n3. Aquí, toca «Elegir carpeta de Cocoon» y elige esa carpeta (la de la exportación o la carpeta de datos de Cocoon; Repisa busca también dentro de las subcarpetas).\n4. Repisa la lee al momento y te muestra abajo lo que encontró. Sirve también para ES-DE, Batocera y RetroBat.",
  ptPT="Cocoon (opcional) — como fazer:\n1. No Cocoon: Settings → Library & Data → Export Metadata. Se pedir uma pasta, escolha ou crie uma (ex. \"Cocoon export\").\n2. Opcional, para o tempo de jogo: crie uma cópia de segurança no Cocoon (Backup & Restore); fica na pasta de dados do Cocoon.\n3. Aqui, toque em \"Escolher pasta do Cocoon\" e escolha essa pasta (a da exportação ou a de dados; o Repisa procura também nas subpastas).\n4. O Repisa lê de imediato e mostra abaixo o que encontrou. Também funciona com ES-DE, Batocera e RetroBat.")
s("frontend_report",
  "Last read: %1$d game lists (%2$s) · %3$d games · %4$d with genre · %5$d with playtime · %6$d matched to your library (%7$d with playtime).",
  "Última lectura: %1$d listas de juegos (%2$s) · %3$d juegos · %4$d con género · %5$d con tiempo de juego · %6$d emparejados con tu biblioteca (%7$d con tiempo).",
  "Dernière lecture : %1$d listes (%2$s) · %3$d jeux · %4$d avec genre · %5$d avec temps de jeu · %6$d associés à votre bibliothèque (%7$d avec temps).",
  "Zuletzt gelesen: %1$d Spielelisten (%2$s) · %3$d Spiele · %4$d mit Genre · %5$d mit Spielzeit · %6$d deiner Bibliothek zugeordnet (%7$d mit Spielzeit).",
  "Ultima lettura: %1$d elenchi (%2$s) · %3$d giochi · %4$d con genere · %5$d con tempo di gioco · %6$d abbinati alla tua libreria (%7$d con tempo).",
  "Última leitura: %1$d listas de jogos (%2$s) · %3$d jogos · %4$d com gênero · %5$d com tempo de jogo · %6$d ligados à sua biblioteca (%7$d com tempo).",
  ptPT="Última leitura: %1$d listas de jogos (%2$s) · %3$d jogos · %4$d com género · %5$d com tempo de jogo · %6$d ligados à sua biblioteca (%7$d com tempo).")
s("frontend_report_backup",
  "Backups: %1$d files, %2$d games with playtime.",
  "Copias de seguridad: %1$d archivos, %2$d juegos con tiempo de juego.",
  "Sauvegardes : %1$d fichiers, %2$d jeux avec temps de jeu.",
  "Backups: %1$d Dateien, %2$d Spiele mit Spielzeit.",
  "Backup: %1$d file, %2$d giochi con tempo di gioco.",
  "Backups: %1$d arquivos, %2$d jogos com tempo de jogo.",
  ptPT="Cópias de segurança: %1$d ficheiros, %2$d jogos com tempo de jogo.")
s("frontend_report_empty",
  "⚠ No gamelist.xml or backup was found in this folder. Choose the folder where Cocoon exported the metadata (or its data folder).",
  "⚠ No se encontró ningún gamelist.xml ni copia de seguridad en esta carpeta. Elige la carpeta donde Cocoon exportó los metadatos (o su carpeta de datos).",
  "⚠ Aucun gamelist.xml ni sauvegarde dans ce dossier. Choisissez le dossier où Cocoon a exporté les métadonnées (ou son dossier de données).",
  "⚠ In diesem Ordner wurde keine gamelist.xml und kein Backup gefunden. Wähle den Ordner, in den Cocoon die Metadaten exportiert hat (oder seinen Datenordner).",
  "⚠ Nessun gamelist.xml né backup in questa cartella. Scegli la cartella in cui Cocoon ha esportato i metadati (o la sua cartella dati).",
  "⚠ Nenhum gamelist.xml nem backup nesta pasta. Escolha a pasta onde o Cocoon exportou os metadados (ou a pasta de dados dele).")
s("frontend_report_no_time",
  "The exported lists have no playtime (ES-DE's format has no field for it). For Cocoon's playtime, create a backup in Cocoon and choose its data folder too, or open your games from Repisa with Usage access enabled.",
  "Las listas exportadas no traen tiempo de juego (el formato de ES-DE no tiene ese dato). Para el tiempo de Cocoon, crea una copia de seguridad en Cocoon y elige su carpeta de datos, o abre los juegos desde Repisa con el Acceso a datos de uso activado.",
  "Les listes exportées ne contiennent pas de temps de jeu (le format ES-DE n’a pas ce champ). Pour celui de Cocoon, créez une sauvegarde et choisissez son dossier de données, ou lancez vos jeux depuis Repisa avec l’accès aux données d’utilisation.",
  "Die exportierten Listen enthalten keine Spielzeit (das ES-DE-Format hat dafür kein Feld). Für Cocoons Spielzeit ein Backup erstellen und den Datenordner wählen oder Spiele aus Repisa mit Nutzungsdatenzugriff starten.",
  "Gli elenchi esportati non hanno il tempo di gioco (il formato ES-DE non lo prevede). Per quello di Cocoon crea un backup e scegli la sua cartella dati, oppure avvia i giochi da Repisa con l’accesso ai dati di utilizzo.",
  "As listas exportadas não trazem tempo de jogo (o formato do ES-DE não tem esse campo). Para o tempo do Cocoon, crie um backup e escolha a pasta de dados dele, ou abra os jogos pelo Repisa com o Acesso ao uso ativado.",
  ptPT="As listas exportadas não trazem tempo de jogo (o formato do ES-DE não tem esse campo). Para o tempo do Cocoon, crie uma cópia de segurança e escolha a pasta de dados, ou abra os jogos pelo Repisa com o acesso a dados de utilização ativado.")
s("frontend_report_no_match",
  "None of these games matched your library. Make sure your ROM folders are added above.",
  "Ninguno de estos juegos coincidió con tu biblioteca. Comprueba que tus carpetas de ROMs estén añadidas arriba.",
  "Aucun de ces jeux ne correspond à votre bibliothèque. Vérifiez que vos dossiers de ROM sont ajoutés ci-dessus.",
  "Keines dieser Spiele passt zu deiner Bibliothek. Prüfe, ob deine ROM-Ordner oben hinzugefügt sind.",
  "Nessuno di questi giochi corrisponde alla tua libreria. Controlla che le cartelle ROM siano aggiunte qui sopra.",
  "Nenhum destes jogos corresponde à sua biblioteca. Verifique se as pastas de ROMs estão adicionadas acima.")
s("hide_game", "Remove from shelf", "Quitar de la repisa", "Retirer de l’étagère", "Aus dem Regal entfernen",
  "Togli dallo scaffale", "Tirar da estante", ptPT="Retirar da estante")
s("hide_game_confirm", "“%1$s” will no longer appear, even after syncing. You can bring it back in Settings → Display.",
  "«%1$s» dejará de aparecer, también al sincronizar. Puedes volver a mostrarlo en Ajustes → Pantalla.",
  "« %1$s » n’apparaîtra plus, même après synchronisation. Vous pouvez le rétablir dans Paramètres → Affichage.",
  "„%1$s“ wird nicht mehr angezeigt, auch nach dem Synchronisieren. Wiederherstellen unter Einstellungen → Anzeige.",
  "«%1$s» non apparirà più, anche dopo la sincronizzazione. Puoi ripristinarlo in Impostazioni → Schermo.",
  "\"%1$s\" não vai mais aparecer, mesmo ao sincronizar. Você pode mostrá-lo de novo em Ajustes → Tela.",
  ptPT="\"%1$s\" deixa de aparecer, mesmo ao sincronizar. Pode mostrá-lo de novo em Definições → Ecrã.")
s("msg_hidden", "Removed from the shelf: %1$s", "Quitado de la repisa: %1$s", "Retiré de l’étagère : %1$s",
  "Aus dem Regal entfernt: %1$s", "Tolto dallo scaffale: %1$s", "Tirado da estante: %1$s", ptPT="Retirado da estante: %1$s")
s("hidden_count", "Games removed from the shelf: %1$d", "Juegos quitados de la repisa: %1$d", "Jeux retirés de l’étagère : %1$d",
  "Aus dem Regal entfernte Spiele: %1$d", "Giochi tolti dallo scaffale: %1$d", "Jogos tirados da estante: %1$d",
  ptPT="Jogos retirados da estante: %1$d")
s("unhide_all", "Show them again", "Volver a mostrarlos", "Les réafficher", "Wieder anzeigen", "Mostrali di nuovo", "Mostrar de novo")
s("field_hours", "Hours", "Horas", "Heures", "Stunden", "Ore", "Horas")
s("field_minutes", "Minutes", "Minutos", "Minutes", "Minuten", "Minuti", "Minutos")
s("field_extra_time", "Time played outside Repisa (added to what Repisa and your platforms record)",
  "Tiempo jugado fuera de Repisa (se suma a lo que registran Repisa y tus plataformas)",
  "Temps joué hors de Repisa (s’ajoute à ce qu’enregistrent Repisa et vos plateformes)",
  "Außerhalb von Repisa gespielte Zeit (wird zu dem addiert, was Repisa und deine Plattformen erfassen)",
  "Tempo giocato fuori da Repisa (si somma a quello registrato da Repisa e dalle piattaforme)",
  "Tempo jogado fora do Repisa (soma-se ao que o Repisa e suas plataformas registram)")
s("field_total_time", "Total time played (replaces the current total)", "Tiempo total jugado (reemplaza el total actual)",
  "Temps de jeu total (remplace le total actuel)", "Gesamte Spielzeit (ersetzt die aktuelle Summe)",
  "Tempo di gioco totale (sostituisce il totale attuale)", "Tempo total jogado (substitui o total atual)")
s("manual_time_mode", "When you type a game's time by hand (Edit data):", "Al escribir el tiempo de un juego a mano (Editar datos):",
  "Quand vous saisissez le temps d’un jeu à la main (Modifier) :", "Wenn du die Zeit eines Spiels von Hand eingibst (Bearbeiten):",
  "Quando scrivi a mano il tempo di un gioco (Modifica dati):", "Ao digitar o tempo de um jogo à mão (Editar dados):",
  ptPT="Ao escrever o tempo de um jogo à mão (Editar dados):")
s("manual_time_add", "Add it to the recorded time", "Sumarlo al tiempo registrado", "L’ajouter au temps enregistré",
  "Zur erfassten Zeit addieren", "Sommarlo al tempo registrato", "Somar ao tempo registrado")
s("manual_time_total", "Use it as the game's total time", "Usarlo como tiempo total del juego", "L’utiliser comme temps total du jeu",
  "Als Gesamtspielzeit verwenden", "Usarlo come tempo totale del gioco", "Usar como tempo total do jogo")
s("reset_time", "Reset time", "Reiniciar tiempo", "Remettre le temps à zéro", "Zeit zurücksetzen", "Azzera tempo", "Zerar tempo",
  ptPT="Repor tempo")
s("reset_time_confirm", "The time played of “%1$s” will be set to 0. From now on, new play time is added again.",
  "El tiempo jugado de «%1$s» se pondrá en 0. Desde ahora se vuelve a sumar lo que juegues.",
  "Le temps de jeu de « %1$s » passera à 0. Le temps joué ensuite s’ajoutera à nouveau.",
  "Die Spielzeit von „%1$s“ wird auf 0 gesetzt. Ab jetzt wird neue Spielzeit wieder addiert.",
  "Il tempo di gioco di «%1$s» verrà azzerato. Da ora in poi il nuovo tempo si somma di nuovo.",
  "O tempo jogado de \"%1$s\" vai para 0. A partir de agora, o que você jogar volta a ser somado.",
  ptPT="O tempo jogado de \"%1$s\" passa a 0. A partir de agora, o que jogar volta a ser somado.")
s("stat_origin", "Comes from", "Origen", "Provenance", "Herkunft", "Origine", "Origem")
s("origin_rom_folder", "your ROM folder", "tu carpeta de ROMs", "votre dossier de ROM", "deinem ROM-Ordner", "la tua cartella ROM",
  "sua pasta de ROMs", ptPT="a sua pasta de ROMs")
s("origin_retroarch", "a RetroArch playlist", "una playlist de RetroArch", "une playlist RetroArch", "einer RetroArch-Playlist",
  "una playlist di RetroArch", "uma playlist do RetroArch")
s("origin_frontend", "ES-DE / Cocoon (no file)", "ES-DE / Cocoon (sin archivo)", "ES-DE / Cocoon (sans fichier)",
  "ES-DE / Cocoon (ohne Datei)", "ES-DE / Cocoon (senza file)", "ES-DE / Cocoon (sem arquivo)", ptPT="ES-DE / Cocoon (sem ficheiro)")
s("menu_data", "Backup and updates", "Copia de seguridad y versión", "Sauvegarde et mises à jour", "Sicherung und Updates",
  "Backup e aggiornamenti", "Backup e atualizações", ptPT="Cópia de segurança e atualizações")
s("menu_data_sub", "Export or import your data, new versions", "Exportar o importar tus datos, versiones nuevas",
  "Exporter ou importer vos données, nouvelles versions", "Daten exportieren oder importieren, neue Versionen",
  "Esporta o importa i tuoi dati, nuove versioni", "Exportar ou importar seus dados, novas versões",
  ptPT="Exportar ou importar os seus dados, novas versões")
s("backup_title", "Backup", "Copia de seguridad", "Sauvegarde", "Sicherung", "Backup", "Backup", ptPT="Cópia de segurança")
s("backup_hint",
  "Saves a file with your ratings, manual time, favourites, removed games, preferred copies, chosen covers, edited data and settings. Steam/Epic sessions, keys and folders are not included: after importing, link your accounts and choose your folders again.",
  "Guarda un archivo con tus calificaciones, tiempo a mano, favoritos, juegos quitados, copias preferentes, carátulas elegidas, datos editados y ajustes. No incluye las sesiones de Steam/Epic, las claves ni las carpetas: tras importar, vuelve a vincular tus cuentas y a elegir tus carpetas.",
  "Enregistre un fichier avec vos notes, temps manuel, favoris, jeux retirés, copies préférées, jaquettes choisies, données modifiées et réglages. Les sessions Steam/Epic, clés et dossiers ne sont pas inclus : après l’import, reliez vos comptes et choisissez vos dossiers.",
  "Speichert eine Datei mit Bewertungen, manueller Zeit, Favoriten, entfernten Spielen, bevorzugten Kopien, gewählten Covern, bearbeiteten Daten und Einstellungen. Steam/Epic-Sitzungen, Schlüssel und Ordner sind nicht enthalten: Nach dem Import Konten neu verknüpfen und Ordner wählen.",
  "Salva un file con valutazioni, tempo manuale, preferiti, giochi tolti, copie preferite, copertine scelte, dati modificati e impostazioni. Sessioni Steam/Epic, chiavi e cartelle non sono incluse: dopo l’importazione ricollega gli account e scegli le cartelle.",
  "Salva um arquivo com suas notas, tempo manual, favoritos, jogos tirados, cópias preferidas, capas escolhidas, dados editados e ajustes. Não inclui as sessões do Steam/Epic, chaves nem pastas: depois de importar, vincule suas contas e escolha suas pastas de novo.",
  ptPT="Guarda um ficheiro com as suas notas, tempo manual, favoritos, jogos retirados, cópias preferidas, capas escolhidas, dados editados e definições. Não inclui as sessões do Steam/Epic, chaves nem pastas: depois de importar, volte a ligar as contas e a escolher as pastas.")
s("backup_export", "Export my data", "Exportar mis datos", "Exporter mes données", "Meine Daten exportieren", "Esporta i miei dati",
  "Exportar meus dados", ptPT="Exportar os meus dados")
s("backup_import", "Import a backup", "Importar una copia", "Importer une sauvegarde", "Sicherung importieren", "Importa un backup",
  "Importar um backup", ptPT="Importar uma cópia")
s("backup_saved", "Backup saved (%1$d games).", "Copia guardada (%1$d juegos).", "Sauvegarde enregistrée (%1$d jeux).",
  "Sicherung gespeichert (%1$d Spiele).", "Backup salvato (%1$d giochi).", "Backup salvo (%1$d jogos).", ptPT="Cópia guardada (%1$d jogos).")
s("backup_restored", "Backup imported (%1$d games). Link Steam/Epic and choose your folders if needed.",
  "Copia importada (%1$d juegos). Vincula Steam/Epic y elige tus carpetas si hace falta.",
  "Sauvegarde importée (%1$d jeux). Reliez Steam/Epic et choisissez vos dossiers si besoin.",
  "Sicherung importiert (%1$d Spiele). Verknüpfe Steam/Epic und wähle deine Ordner, falls nötig.",
  "Backup importato (%1$d giochi). Collega Steam/Epic e scegli le cartelle se serve.",
  "Backup importado (%1$d jogos). Vincule Steam/Epic e escolha suas pastas se precisar.",
  ptPT="Cópia importada (%1$d jogos). Ligue o Steam/Epic e escolha as pastas se for preciso.")
s("backup_failed", "Couldn't complete the backup: %1$s", "No se pudo completar la copia: %1$s", "Échec de la sauvegarde : %1$s",
  "Sicherung fehlgeschlagen: %1$s", "Backup non riuscito: %1$s", "Não foi possível concluir o backup: %1$s",
  ptPT="Não foi possível concluir a cópia: %1$s")
s("updates_title", "Version", "Versión", "Version", "Version", "Versione", "Versão")
s("updates_hint", "Repisa looks at the project's releases on GitHub and, if there is a newer one, shows its patch notes and installs it over this one.",
  "Repisa mira las releases del proyecto en GitHub y, si hay una más nueva, te muestra sus notas del parche y la instala encima de esta.",
  "Repisa consulte les versions du projet sur GitHub et, s’il y en a une plus récente, affiche ses notes et l’installe par-dessus.",
  "Repisa prüft die Releases des Projekts auf GitHub und zeigt bei einer neueren die Patchnotes und installiert sie darüber.",
  "Repisa controlla le release del progetto su GitHub e, se ce n’è una più nuova, mostra le note e la installa sopra questa.",
  "O Repisa verifica as releases do projeto no GitHub e, se houver uma mais nova, mostra as notas e instala por cima desta.")
s("updates_check_now", "Check now", "Buscar ahora", "Vérifier maintenant", "Jetzt prüfen", "Controlla ora", "Verificar agora")
s("update_none", "You have the latest version (%1$s).", "Tienes la última versión (%1$s).", "Vous avez la dernière version (%1$s).",
  "Du hast die neueste Version (%1$s).", "Hai l’ultima versione (%1$s).", "Você tem a versão mais recente (%1$s).",
  ptPT="Tem a versão mais recente (%1$s).")
s("update_private", "GitHub doesn't show the new versions: the Repisa project is private. Make it public (GitHub → Settings → Change visibility) so the app can update itself.",
  "GitHub no deja ver las versiones nuevas: el proyecto Repisa es privado. Hazlo público (GitHub → Settings → Change visibility) para que la app se actualice sola.",
  "GitHub ne montre pas les nouvelles versions : le projet Repisa est privé. Rendez-le public pour que l'app se mette à jour.",
  "GitHub zeigt die neuen Versionen nicht: das Projekt Repisa ist privat. Mach es öffentlich, damit sich die App aktualisiert.",
  "GitHub non mostra le nuove versioni: il progetto Repisa è privato. Rendilo pubblico perché l'app si aggiorni.",
  "O GitHub não mostra as versões novas: o projeto Repisa é privado. Torne-o público para o app se atualizar.")
s("update_check_failed", "Couldn't check for new versions (no connection, or the project isn't public).",
  "No se pudo buscar versiones nuevas (sin conexión o el proyecto no es público).",
  "Impossible de vérifier (pas de connexion, ou le projet n’est pas public).",
  "Prüfung nicht möglich (keine Verbindung oder das Projekt ist nicht öffentlich).",
  "Impossibile controllare (nessuna connessione o il progetto non è pubblico).",
  "Não foi possível verificar (sem conexão ou o projeto não é público).")
s("update_title", "Repisa %1$s is available", "Repisa %1$s está disponible", "Repisa %1$s est disponible", "Repisa %1$s ist verfügbar",
  "Repisa %1$s è disponibile", "Repisa %1$s está disponível")
s("update_required", "Install it to keep using Repisa (your library and settings stay as they are).",
  "Instálala para seguir usando Repisa (tu biblioteca y ajustes se quedan como están).",
  "Installez-la pour continuer à utiliser Repisa (bibliothèque et réglages conservés).",
  "Installiere sie, um Repisa weiter zu nutzen (Bibliothek und Einstellungen bleiben erhalten).",
  "Installala per continuare a usare Repisa (libreria e impostazioni restano come sono).",
  "Instale para continuar usando o Repisa (sua biblioteca e ajustes continuam iguais).")
s("update_no_notes", "No patch notes.", "Sin notas del parche.", "Pas de notes.", "Keine Patchnotes.", "Nessuna nota.", "Sem notas.")
s("update_now", "Update", "Actualizar", "Mettre à jour", "Aktualisieren", "Aggiorna", "Atualizar")
s("update_downloading", "Downloading… %1$d%%", "Descargando… %1$d%%", "Téléchargement… %1$d%%", "Wird heruntergeladen… %1$d%%",
  "Download… %1$d%%", "Baixando… %1$d%%", ptPT="A transferir… %1$d%%")
s("update_allow_install", "Allow Repisa to install apps (Android opens the setting) and come back: the update continues by itself.",
  "Permite que Repisa instale apps (Android abre el ajuste) y vuelve: la actualización sigue sola.",
  "Autorisez Repisa à installer des apps (Android ouvre le réglage) puis revenez : la mise à jour continue seule.",
  "Erlaube Repisa, Apps zu installieren (Android öffnet die Einstellung), und kehre zurück: das Update läuft weiter.",
  "Consenti a Repisa di installare app (Android apre l’impostazione) e torna: l’aggiornamento continua da solo.",
  "Permita que o Repisa instale apps (o Android abre o ajuste) e volte: a atualização continua sozinha.")
s("update_failed", "Couldn't download the update: %1$s", "No se pudo descargar la actualización: %1$s",
  "Échec du téléchargement : %1$s", "Download fehlgeschlagen: %1$s", "Download non riuscito: %1$s",
  "Não foi possível baixar a atualização: %1$s", ptPT="Não foi possível transferir a atualização: %1$s")
s("license_hint", "Repisa is free software under the GNU GPL v3 licence: you can use, study, share and modify it; modified versions must also be shared under the same licence.",
  "Repisa es software libre con licencia GNU GPL v3: puedes usarla, estudiarla, compartirla y modificarla; las versiones modificadas también deben compartirse con la misma licencia.",
  "Repisa est un logiciel libre sous licence GNU GPL v3 : vous pouvez l’utiliser, l’étudier, le partager et le modifier ; les versions modifiées doivent rester sous la même licence.",
  "Repisa ist freie Software unter der GNU GPL v3: Du darfst sie nutzen, untersuchen, teilen und ändern; geänderte Versionen müssen unter derselben Lizenz geteilt werden.",
  "Repisa è software libero con licenza GNU GPL v3: puoi usarlo, studiarlo, condividerlo e modificarlo; le versioni modificate vanno condivise con la stessa licenza.",
  "O Repisa é software livre sob a licença GNU GPL v3: você pode usar, estudar, compartilhar e modificar; versões modificadas também devem ser compartilhadas com a mesma licença.",
  ptPT="O Repisa é software livre com a licença GNU GPL v3: pode usá-lo, estudá-lo, partilhá-lo e modificá-lo; as versões modificadas também têm de ser partilhadas com a mesma licença.")
s("companion_session", "this session", "esta sesión", "cette session", "diese Sitzung", "questa sessione", "esta sessão")
s("companion_close", "Close panel", "Cerrar panel", "Fermer le panneau", "Panel schließen", "Chiudi pannello", "Fechar painel")
s("notes", "Notes", "Notas", "Notes", "Notizen", "Note", "Notas")
s("notes_empty", "No notes yet. Write where you left off, tips, anything.", "Sin notas. Apunta dónde lo dejaste, pistas, lo que quieras.",
  "Pas encore de notes. Notez où vous en êtes, des astuces…", "Noch keine Notizen. Notiere, wo du aufgehört hast, Tipps…",
  "Nessuna nota. Scrivi dove eri rimasto, consigli, quello che vuoi.", "Sem notas. Anote onde parou, dicas, o que quiser.")
s("notes_hint", "Where you left off, what to do next, tips…", "Dónde lo dejaste, qué hacer después, pistas…",
  "Où vous en êtes, la suite, des astuces…", "Wo du aufgehört hast, was als Nächstes kommt, Tipps…",
  "Dove eri rimasto, cosa fare dopo, consigli…", "Onde parou, o que fazer depois, dicas…")
s("companion_setting", "Game panel on the bottom screen while playing", "Panel del juego en la pantalla de abajo al jugar",
  "Panneau du jeu sur l’écran du bas pendant la partie", "Spiel-Panel auf dem unteren Bildschirm beim Spielen",
  "Pannello del gioco sullo schermo inferiore mentre giochi", "Painel do jogo na tela de baixo ao jogar",
  ptPT="Painel do jogo no ecrã de baixo ao jogar")
s("companion_setting_hint",
  "When you press Play, the game opens on the top screen and the bottom one keeps Repisa with a live timer, the clock, rating, favourite and notes. Not for DS and 3DS (they use both screens).",
  "Al pulsar Jugar, el juego se abre arriba y abajo se queda Repisa con el tiempo de la sesión en vivo, la hora, calificar, favorito y notas. No en DS ni 3DS (usan las dos pantallas).",
  "Avec Jouer, le jeu s’ouvre en haut et l’écran du bas garde Repisa avec le chrono, l’heure, la note, favori et notes. Pas pour DS et 3DS (deux écrans).",
  "Bei Spielen öffnet sich das Spiel oben, unten bleibt Repisa mit Live-Timer, Uhr, Bewertung, Favorit und Notizen. Nicht bei DS und 3DS (beide Bildschirme).",
  "Con Gioca il gioco si apre sopra e sotto resta Repisa con cronometro, ora, valutazione, preferito e note. Non per DS e 3DS (usano entrambi gli schermi).",
  "Ao tocar em Jogar, o jogo abre em cima e embaixo fica o Repisa com o tempo da sessão, a hora, avaliar, favorito e notas. Não no DS nem no 3DS (usam as duas telas).",
  ptPT="Ao tocar em Jogar, o jogo abre em cima e em baixo fica o Repisa com o tempo da sessão, a hora, avaliar, favorito e notas. Não no DS nem no 3DS (usam os dois ecrãs).")
s("status_backlog", "Backlog", "Pendiente", "À faire", "Ausstehend", "Da giocare", "Pendente")
s("status_playing", "Playing", "Jugando", "En cours", "Spiele gerade", "In corso", "Jogando", ptPT="A jogar")
s("status_finished", "Finished", "Terminado", "Terminé", "Beendet", "Finito", "Zerado", ptPT="Terminado")
s("status_completed", "100%", "100 %", "100 %", "100 %", "100%", "100%")
s("status_abandoned", "Dropped", "Abandonado", "Abandonné", "Abgebrochen", "Abbandonato", "Abandonado")
s("group_status", "Status", "Estado", "Statut", "Status", "Stato", "Status", ptPT="Estado")
s("shelf_no_status", "No status", "Sin estado", "Sans statut", "Ohne Status", "Senza stato", "Sem status", ptPT="Sem estado")
s("shelf_continue", "Continue playing", "Seguir jugando", "Continuer", "Weiterspielen", "Continua a giocare", "Continuar jogando",
  ptPT="Continuar a jogar")
s("shelf_continue_sub", "Played recently", "Jugados hace poco", "Joués récemment", "Kürzlich gespielt", "Giocati di recente",
  "Jogados recentemente")
s("remaining_main", "About %1$s h left to finish it (HowLongToBeat, story)", "Te faltan unas %1$s h para terminarlo (HowLongToBeat, historia)",
  "Il reste environ %1$s h pour le finir (HowLongToBeat, histoire)", "Noch etwa %1$s Std. bis zum Ende (HowLongToBeat, Story)",
  "Mancano circa %1$s h per finirlo (HowLongToBeat, storia)", "Faltam cerca de %1$s h para terminar (HowLongToBeat, história)")
s("remaining_complete", "About %1$s h left for 100%% (HowLongToBeat)", "Te faltan unas %1$s h para el 100 %% (HowLongToBeat)",
  "Il reste environ %1$s h pour le 100 %% (HowLongToBeat)", "Noch etwa %1$s Std. bis 100 %% (HowLongToBeat)",
  "Mancano circa %1$s h per il 100%% (HowLongToBeat)", "Faltam cerca de %1$s h para os 100%% (HowLongToBeat)")
s("setting_continue_shelf", "“Continue playing” shelf at the top", "Repisa «Seguir jugando» arriba", "Étagère « Continuer » en haut",
  "Regal „Weiterspielen“ oben", "Scaffale «Continua a giocare» in alto", "Estante \"Continuar jogando\" no topo",
  ptPT="Estante \"Continuar a jogar\" no topo")
s("setting_status_boxes", "Status ribbon on the boxes", "Cinta de estado en las cajas", "Ruban de statut sur les boîtes",
  "Statusband auf den Hüllen", "Nastro di stato sulle scatole", "Fita de status nas caixas", ptPT="Fita de estado nas caixas")
s("random_title", "What should I play?", "¿A qué juego?", "À quoi je joue ?", "Was soll ich spielen?", "A cosa gioco?", "O que eu jogo?",
  ptPT="O que vou jogar?")
s("random_count", "%1$d possible games", "%1$d juegos posibles", "%1$d jeux possibles", "%1$d mögliche Spiele", "%1$d giochi possibili",
  "%1$d jogos possíveis")
s("random_only_backlog", "Only backlog", "Solo pendientes", "Seulement à faire", "Nur ausstehende", "Solo da giocare", "Só pendentes")
s("random_max_hours", "≤ %1$d h", "≤ %1$d h", "≤ %1$d h", "≤ %1$d Std.", "≤ %1$d h", "≤ %1$d h")
s("random_by_taste", "My taste", "Según mis gustos", "Selon mes goûts", "Nach meinem Geschmack", "Secondo i miei gusti", "Pelo meu gosto",
  ptPT="Segundo os meus gostos")
s("random_none", "No game matches these filters.", "Ningún juego cumple estos filtros.", "Aucun jeu ne correspond.",
  "Kein Spiel passt zu diesen Filtern.", "Nessun gioco corrisponde ai filtri.", "Nenhum jogo corresponde aos filtros.")
s("random_hint", "Press the dice and let fate decide.", "Pulsa el dado y que decida la suerte.", "Lancez le dé et laissez faire le hasard.",
  "Würfle und lass das Glück entscheiden.", "Tira il dado e lascia decidere la sorte.", "Toque no dado e deixe a sorte decidir.",
  ptPT="Toque no dado e deixe a sorte decidir.")
s("random_roll", "Roll", "Tirar", "Lancer", "Würfeln", "Tira", "Rolar", ptPT="Lançar")
s("random_again", "Again", "Otra vez", "Encore", "Nochmal", "Ancora", "De novo")
s("random_open", "See details", "Ver ficha", "Voir la fiche", "Details", "Scheda", "Ver ficha")
s("random_hltb", "~%1$s h to finish", "~%1$s h para terminarlo", "~%1$s h pour le finir", "~%1$s Std. bis zum Ende", "~%1$s h per finirlo",
  "~%1$s h para terminar")
s("stats_title", "Statistics", "Estadísticas", "Statistiques", "Statistiken", "Statistiche", "Estatísticas")
s("stats_in_library", "in your library", "en tu biblioteca", "dans votre bibliothèque", "in deiner Bibliothek", "nella tua libreria",
  "na sua biblioteca")
s("stats_played", "played in total", "jugadas en total", "jouées au total", "insgesamt gespielt", "giocate in totale", "jogadas no total")
s("stats_done", "finished or 100%", "terminados o al 100 %", "terminés ou à 100 %", "beendet oder 100 %", "finiti o al 100%",
  "zerados ou 100%", ptPT="terminados ou a 100%")
s("stats_backlog_left", "in backlog (~%1$s h)", "pendientes (~%1$s h)", "à faire (~%1$s h)", "ausstehend (~%1$s Std.)", "da giocare (~%1$s h)",
  "pendentes (~%1$s h)")
s("stats_avg_rating", "average of %1$d rated", "nota media de %1$d calificados", "note moyenne de %1$d jeux", "Schnitt aus %1$d bewerteten",
  "media di %1$d valutati", "média de %1$d avaliados")
s("stats_by_status", "By status", "Por estado", "Par statut", "Nach Status", "Per stato", "Por status", ptPT="Por estado")
s("stats_no_status", "%1$d without status", "%1$d sin estado", "%1$d sans statut", "%1$d ohne Status", "%1$d senza stato",
  "%1$d sem status", ptPT="%1$d sem estado")
s("stats_hours_system", "Hours by system", "Horas por sistema", "Heures par système", "Stunden pro System", "Ore per sistema", "Horas por sistema")
s("stats_hours_genre", "Hours by genre", "Horas por género", "Heures par genre", "Stunden pro Genre", "Ore per genere", "Horas por gênero",
  ptPT="Horas por género")
s("stats_by_decade", "Games by release decade", "Juegos por década de salida", "Jeux par décennie", "Spiele nach Jahrzehnt",
  "Giochi per decennio", "Jogos por década de lançamento")
s("stats_most_played", "Most played", "Los más jugados", "Les plus joués", "Am meisten gespielt", "I più giocati", "Os mais jogados")
s("stats_other", "Others", "Otros", "Autres", "Andere", "Altri", "Outros")
s("cover_from_gallery", "From gallery", "Desde la galería", "Depuis la galerie", "Aus der Galerie", "Dalla galleria", "Da galeria")
s("crop_title", "Frame the cover (%1$s box)", "Encuadra la carátula (caja de %1$s)", "Cadrez la jaquette (boîte %1$s)",
  "Cover zuschneiden (%1$s-Hülle)", "Inquadra la copertina (scatola %1$s)", "Enquadre a capa (caixa de %1$s)")
s("crop_hint", "Drag to move and pinch (or − / +) to zoom. The frame has the real proportions of the box.",
  "Arrastra para mover y pellizca (o − / +) para ampliar. El marco tiene la proporción real de la caja.",
  "Glissez pour déplacer et pincez (ou − / +) pour zoomer. Le cadre a les proportions réelles de la boîte.",
  "Ziehen zum Verschieben, zwei Finger (oder − / +) zum Zoomen. Der Rahmen hat das echte Hüllenformat.",
  "Trascina per spostare e pizzica (o − / +) per ingrandire. La cornice ha le proporzioni reali della scatola.",
  "Arraste para mover e pince (ou − / +) para ampliar. O quadro tem a proporção real da caixa.")
s("crop_reset", "Reset", "Restablecer", "Réinitialiser", "Zurücksetzen", "Ripristina", "Redefinir")
s("achievements", "Achievements", "Logros", "Succès", "Erfolge", "Obiettivi", "Conquistas")
s("ach_source_steam", "Steam (your progress)", "Steam (tu progreso)", "Steam (votre progression)", "Steam (dein Fortschritt)",
  "Steam (i tuoi progressi)", "Steam (seu progresso)", ptPT="Steam (o seu progresso)")
s("ach_source_steam_list", "Steam list (mark them yourself)", "Lista de Steam (márcalos tú)", "Liste Steam (à cocher vous-même)",
  "Steam-Liste (selbst abhaken)", "Elenco Steam (segnali tu)", "Lista do Steam (marque você)", ptPT="Lista do Steam (marque-as)")
s("ach_source_ra", "RetroAchievements", "RetroAchievements", "RetroAchievements", "RetroAchievements", "RetroAchievements", "RetroAchievements")
s("ach_progress", "%1$d of %2$d (%3$d%%)", "%1$d de %2$d (%3$d %%)", "%1$d sur %2$d (%3$d %%)", "%1$d von %2$d (%3$d %%)",
  "%1$d di %2$d (%3$d%%)", "%1$d de %2$d (%3$d%%)")
s("ach_unlocked", "Unlocked (%1$d)", "Conseguidos (%1$d)", "Débloqués (%1$d)", "Freigeschaltet (%1$d)", "Sbloccati (%1$d)",
  "Desbloqueadas (%1$d)")
s("ach_locked", "To get (%1$d) · %% of players who have it", "Por conseguir (%1$d) · %% de jugadores que lo tienen",
  "À obtenir (%1$d) · %% de joueurs qui l’ont", "Noch offen (%1$d) · %% der Spieler mit diesem Erfolg",
  "Da ottenere (%1$d) · %% di giocatori che lo hanno", "A conseguir (%1$d) · %% de jogadores que têm")
s("open_web", "Open web", "Abrir web", "Ouvrir le site", "Webseite öffnen", "Apri sito", "Abrir site")
s("ach_repisa", "Repisa achievements (for every game)", "Logros de Repisa (para todos los juegos)", "Succès Repisa (pour tous les jeux)",
  "Repisa-Erfolge (für alle Spiele)", "Obiettivi Repisa (per tutti i giochi)", "Conquistas do Repisa (para todos os jogos)")
s("ach_hidden", "Hidden achievement", "Logro oculto", "Succès caché", "Versteckter Erfolg", "Obiettivo nascosto", "Conquista oculta")
s("ach_manual", "marked by you", "marcado a mano", "coché par vous", "selbst abgehakt", "segnato da te", "marcada por você",
  ptPT="marcada por si")
s("ach_loading", "Looking for achievements…", "Buscando logros…", "Recherche des succès…", "Suche Erfolge…", "Cerco gli obiettivi…",
  "Procurando conquistas…", ptPT="A procurar conquistas…")
s("ach_retry", "Reload", "Recargar", "Recharger", "Neu laden", "Ricarica", "Recarregar")
s("ach_error", "Couldn't reach the achievements service (it may be busy). Tap “Reload” in a moment.",
  "No se pudieron consultar los logros (el servicio puede estar saturado). Pulsa «Recargar» en un momento.",
  "Impossible de consulter les succès (service peut-être saturé). Touchez « Recharger » dans un instant.",
  "Erfolge konnten nicht abgerufen werden (Dienst evtl. überlastet). Gleich „Neu laden“ antippen.",
  "Impossibile leggere gli obiettivi (servizio forse sovraccarico). Tocca «Ricarica» tra poco.",
  "Não foi possível consultar as conquistas (serviço talvez ocupado). Toque em \"Recarregar\" daqui a pouco.")
s("ach_none", "No achievements found for this game.", "No se encontraron logros para este juego.", "Aucun succès trouvé pour ce jeu.",
  "Keine Erfolge für dieses Spiel gefunden.", "Nessun obiettivo trovato per questo gioco.", "Nenhuma conquista encontrada para este jogo.")
s("ach_no_ra_system", "%1$s has no achievements on RetroAchievements. You have the Repisa achievements below.",
  "%1$s no tiene logros en RetroAchievements. Tienes los logros de Repisa aquí abajo.",
  "%1$s n’a pas de succès sur RetroAchievements. Vous avez les succès Repisa ci-dessous.",
  "%1$s hat keine Erfolge bei RetroAchievements. Unten findest du die Repisa-Erfolge.",
  "%1$s non ha obiettivi su RetroAchievements. Qui sotto hai gli obiettivi di Repisa.",
  "%1$s não tem conquistas no RetroAchievements. As conquistas do Repisa estão abaixo.")
s("ach_link_ra", "Link RetroAchievements in Settings to see this game's achievements.",
  "Vincula RetroAchievements en Ajustes para ver los logros de este juego.",
  "Reliez RetroAchievements dans Paramètres pour voir les succès de ce jeu.",
  "Verknüpfe RetroAchievements in den Einstellungen, um die Erfolge zu sehen.",
  "Collega RetroAchievements nelle Impostazioni per vedere gli obiettivi.",
  "Vincule o RetroAchievements em Ajustes para ver as conquistas deste jogo.", ptPT="Ligue o RetroAchievements nas Definições para ver as conquistas.")
s("ach_note_epic", "These are the achievements of the Steam version. Epic doesn't let Repisa see yours, so tick the ones you get: the date and time are saved.",
  "Son los logros de la versión de Steam. Epic no deja que Repisa vea los tuyos, así que marca los que consigas: se guarda la fecha y la hora.",
  "Ce sont les succès de la version Steam. Epic ne laisse pas Repisa voir les vôtres : cochez ceux que vous obtenez (date et heure enregistrées).",
  "Das sind die Erfolge der Steam-Version. Epic zeigt Repisa deine nicht, also hake die erreichten ab: Datum und Uhrzeit werden gespeichert.",
  "Sono gli obiettivi della versione Steam. Epic non permette a Repisa di vedere i tuoi: segna quelli che ottieni (data e ora salvate).",
  "São as conquistas da versão Steam. A Epic não deixa o Repisa ver as suas: marque as que conseguir (data e hora ficam salvas).")
s("ach_note_private", "Steam isn't showing your achievements (your game details are private). In Steam: Profile → Edit profile → Privacy settings → Game details: Public. Or add your Steam API key in Settings → Steam → Advanced. Meanwhile you can tick them yourself.",
  "Steam no deja ver tus logros (los detalles de juego de tu perfil son privados). En Steam: Perfil → Editar perfil → Configuración de privacidad → Detalles de juego: Público. O pon tu clave de API de Steam en Ajustes → Steam → Avanzado. Mientras, puedes marcarlos a mano.",
  "Steam ne montre pas vos succès (détails de jeu privés). Dans Steam : Profil → Modifier → Confidentialité → Détails des jeux : Public. Ou ajoutez votre clé API Steam. En attendant, cochez-les vous-même.",
  "Steam zeigt deine Erfolge nicht (Spieldetails privat). In Steam: Profil → Profil bearbeiten → Privatsphäre → Spieldetails: Öffentlich. Oder Steam-API-Schlüssel eintragen. Bis dahin selbst abhaken.",
  "Steam non mostra i tuoi obiettivi (dettagli di gioco privati). In Steam: Profilo → Modifica → Privacy → Dettagli di gioco: Pubblico. Oppure aggiungi la chiave API di Steam. Intanto puoi segnarli tu.",
  "O Steam não mostra suas conquistas (detalhes de jogo privados). No Steam: Perfil → Editar perfil → Privacidade → Detalhes de jogo: Público. Ou coloque sua chave de API do Steam. Enquanto isso, marque-as você.")
s("ra_hint", "Achievements for emulated games (GB, GBA, DS, N64, SNES, PS1, PS2, PSP, GameCube…). Your Web API key is at retroachievements.org → Settings → Keys.",
  "Logros de juegos emulados (GB, GBA, DS, N64, SNES, PS1, PS2, PSP, GameCube…). Tu clave «Web API Key» está en retroachievements.org → Settings → Keys.",
  "Succès des jeux émulés (GB, GBA, DS, N64, SNES, PS1, PS2, PSP, GameCube…). Votre « Web API Key » est sur retroachievements.org → Settings → Keys.",
  "Erfolge für emulierte Spiele (GB, GBA, DS, N64, SNES, PS1, PS2, PSP, GameCube…). Deinen Web-API-Schlüssel findest du auf retroachievements.org → Settings → Keys.",
  "Obiettivi dei giochi emulati (GB, GBA, DS, N64, SNES, PS1, PS2, PSP, GameCube…). La «Web API Key» è su retroachievements.org → Settings → Keys.",
  "Conquistas de jogos emulados (GB, GBA, DS, N64, SNES, PS1, PS2, PSP, GameCube…). Sua \"Web API Key\" está em retroachievements.org → Settings → Keys.")
s("ra_user", "RetroAchievements user", "Usuario de RetroAchievements", "Utilisateur RetroAchievements", "RetroAchievements-Benutzer",
  "Utente RetroAchievements", "Usuário do RetroAchievements", ptPT="Utilizador do RetroAchievements")
s("ra_key", "Web API Key", "Web API Key", "Web API Key", "Web-API-Schlüssel", "Web API Key", "Web API Key")
s("ra_ok", "RetroAchievements linked. Looking for your achievements…", "RetroAchievements vinculado. Buscando tus logros…",
  "RetroAchievements relié. Recherche de vos succès…", "RetroAchievements verknüpft. Suche deine Erfolge…",
  "RetroAchievements collegato. Cerco i tuoi obiettivi…", "RetroAchievements vinculado. Procurando suas conquistas…",
  ptPT="RetroAchievements ligado. A procurar as suas conquistas…")
s("ra_failed", "RetroAchievements didn't accept that user and key.", "RetroAchievements no aceptó ese usuario y clave.",
  "RetroAchievements n’a pas accepté cet utilisateur et cette clé.", "RetroAchievements hat Benutzer und Schlüssel nicht akzeptiert.",
  "RetroAchievements non ha accettato utente e chiave.", "O RetroAchievements não aceitou esse usuário e chave.")
s("sort_achievements", "Achievements %", "% de logros", "% de succès", "Erfolge in %", "% obiettivi", "% de conquistas")
s("rach_first", "First session", "Primera partida", "Première partie", "Erste Partie", "Prima partita", "Primeira partida")
s("rach_first_d", "Play it for the first time.", "Juégalo por primera vez.", "Jouez-y pour la première fois.", "Spiele es zum ersten Mal.",
  "Giocalo per la prima volta.", "Jogue pela primeira vez.")
s("rach_finished", "To the end", "Hasta el final", "Jusqu’au bout", "Bis zum Ende", "Fino alla fine", "Até o fim", ptPT="Até ao fim")
s("rach_finished_d", "Mark it as finished.", "Márcalo como terminado.", "Marquez-le comme terminé.", "Markiere es als beendet.",
  "Segnalo come finito.", "Marque como zerado.", ptPT="Marque-o como terminado.")
s("rach_completed", "Completionist", "Completista", "Complétionniste", "Komplettist", "Completista", "Completista")
s("rach_completed_d", "Mark it as 100%.", "Márcalo al 100 %.", "Marquez-le à 100 %.", "Markiere es mit 100 %.", "Segnalo al 100%.",
  "Marque como 100%.")
s("rach_rated", "Critic", "Crítico", "Critique", "Kritiker", "Critico", "Crítico")
s("rach_rated_d", "Rate it.", "Califícalo.", "Notez-le.", "Bewerte es.", "Valutalo.", "Avalie-o.")
s("rach_notes", "Logbook", "Cuaderno de bitácora", "Carnet de bord", "Logbuch", "Diario di bordo", "Diário de bordo")
s("rach_notes_d", "Write notes about it.", "Escribe notas sobre él.", "Écrivez des notes.", "Schreib Notizen dazu.", "Scrivi delle note.",
  "Escreva notas sobre ele.")
s("rach_favorite", "One of mine", "Uno de los míos", "Un des miens", "Einer von meinen", "Uno dei miei", "Um dos meus")
s("rach_favorite_d", "Mark it as favourite.", "Márcalo como favorito.", "Marquez-le en favori.", "Markiere es als Favorit.",
  "Segnalo come preferito.", "Marque como favorito.")
s("onb_welcome_title", "Welcome to Repisa", "Te damos la bienvenida a Repisa", "Bienvenue dans Repisa", "Willkommen bei Repisa",
  "Benvenuto in Repisa", "Boas-vindas ao Repisa")
s("onb_welcome_body", "Repisa puts all your games — Steam, Epic Games and emulators — on wooden shelves as real boxes, with their cover, playtime, scores, achievements and your own ratings. Let's set it up in a minute.",
  "Repisa pone todos tus juegos (Steam, Epic Games y emuladores) en repisas de madera como cajas de verdad, con su carátula, tiempo jugado, notas, logros y tus calificaciones. Vamos a dejarla lista en un minuto.",
  "Repisa range tous vos jeux (Steam, Epic Games et émulateurs) sur des étagères en bois, comme de vraies boîtes, avec jaquette, temps de jeu, notes, succès et vos avis. Configurons-la en une minute.",
  "Repisa stellt all deine Spiele (Steam, Epic Games und Emulatoren) als echte Hüllen in Holzregale, mit Cover, Spielzeit, Wertungen, Erfolgen und deinen Bewertungen. Richten wir sie in einer Minute ein.",
  "Repisa mette tutti i tuoi giochi (Steam, Epic Games ed emulatori) su scaffali di legno come vere scatole, con copertina, tempo di gioco, voti, obiettivi e le tue valutazioni. Configuriamola in un minuto.",
  "O Repisa coloca todos os seus jogos (Steam, Epic Games e emuladores) em estantes de madeira como caixas de verdade, com capa, tempo jogado, notas, conquistas e suas avaliações. Vamos configurar em um minuto.")
s("onb_backup_body", "Do you have a Repisa backup (for example, from another console or an older version)? Import it now and everything comes back.",
  "¿Tienes una copia de seguridad de Repisa (por ejemplo, de otra consola o de una versión anterior)? Impórtala ahora y vuelve todo.",
  "Vous avez une sauvegarde Repisa (autre console, ancienne version) ? Importez-la maintenant et tout revient.",
  "Hast du eine Repisa-Sicherung (z. B. von einer anderen Konsole oder Version)? Importiere sie jetzt und alles ist wieder da.",
  "Hai un backup di Repisa (da un’altra console o una versione precedente)? Importalo ora e torna tutto.",
  "Tem um backup do Repisa (de outro console ou de uma versão anterior)? Importe agora e tudo volta.")
s("onb_start", "Let's start", "Empezar", "Commencer", "Los geht’s", "Iniziamo", "Começar")
s("onb_import", "I have a backup: import it", "Tengo una copia: importarla", "J’ai une sauvegarde : l’importer",
  "Ich habe eine Sicherung: importieren", "Ho un backup: importalo", "Tenho um backup: importar", ptPT="Tenho uma cópia: importar")
s("onb_pc_title", "Your PC games", "Tus juegos de PC", "Vos jeux PC", "Deine PC-Spiele", "I tuoi giochi PC", "Seus jogos de PC",
  ptPT="Os seus jogos de PC")
s("onb_pc_body", "Link Steam (also your Family group) and Epic Games by signing in on their official pages. Repisa only reads your games and hours. They are at the top of Settings; when you come back, the guide continues.",
  "Vincula Steam (también tu grupo familiar) y Epic Games iniciando sesión en sus páginas oficiales. Repisa solo lee tus juegos y horas. Están arriba del todo en Ajustes; al volver, la guía sigue.",
  "Reliez Steam (et votre groupe familial) et Epic Games en vous connectant sur leurs pages officielles. Repisa lit seulement vos jeux et heures. C’est en haut des Paramètres ; au retour, le guide continue.",
  "Verknüpfe Steam (auch deine Familiengruppe) und Epic Games über ihre offiziellen Anmeldeseiten. Repisa liest nur Spiele und Stunden. Ganz oben in den Einstellungen; danach geht die Anleitung weiter.",
  "Collega Steam (anche il gruppo famiglia) ed Epic Games accedendo alle loro pagine ufficiali. Repisa legge solo giochi e ore. Sono in cima alle Impostazioni; al ritorno la guida continua.",
  "Vincule o Steam (também o grupo familiar) e a Epic Games entrando nas páginas oficiais. O Repisa só lê seus jogos e horas. Ficam no topo de Ajustes; ao voltar, o guia continua.")
s("onb_pc_link", "Link Steam / Epic", "Vincular Steam / Epic", "Relier Steam / Epic", "Steam / Epic verknüpfen", "Collega Steam / Epic",
  "Vincular Steam / Epic", ptPT="Ligar Steam / Epic")
s("onb_skip_step", "Not now", "Ahora no", "Pas maintenant", "Jetzt nicht", "Non ora", "Agora não")
s("onb_next", "Next", "Siguiente", "Suivant", "Weiter", "Avanti", "Próximo", ptPT="Seguinte")
s("onb_emu_title", "Your emulators", "Tus emuladores", "Vos émulateurs", "Deine Emulatoren", "I tuoi emulatori", "Seus emuladores",
  ptPT="Os seus emuladores")
s("onb_emu_body", "Choose the folder where you keep your ROMs (just the main one: Repisa looks inside for switch/, gba/, ps2/…). Repisa will detect your installed emulators and open each game directly. Cocoon, ES-DE and RetroArch are in Settings → Emulators.",
  "Elige la carpeta donde tienes tus ROMs (basta la principal: Repisa busca dentro switch/, gba/, ps2/…). Repisa detectará tus emuladores instalados y abrirá cada juego directamente. Cocoon, ES-DE y RetroArch están en Ajustes → Emuladores.",
  "Choisissez le dossier de vos ROM (le principal suffit : Repisa cherche switch/, gba/, ps2/…). Repisa détectera vos émulateurs et ouvrira chaque jeu directement. Cocoon, ES-DE et RetroArch sont dans Paramètres → Émulateurs.",
  "Wähle den Ordner mit deinen ROMs (der Hauptordner reicht: Repisa sucht switch/, gba/, ps2/…). Repisa erkennt deine Emulatoren und öffnet jedes Spiel direkt. Cocoon, ES-DE und RetroArch unter Einstellungen → Emulatoren.",
  "Scegli la cartella delle ROM (basta la principale: Repisa cerca switch/, gba/, ps2/…). Repisa rileverà gli emulatori e aprirà ogni gioco direttamente. Cocoon, ES-DE e RetroArch sono in Impostazioni → Emulatori.",
  "Escolha a pasta das suas ROMs (basta a principal: o Repisa procura switch/, gba/, ps2/…). O Repisa detecta seus emuladores e abre cada jogo direto. Cocoon, ES-DE e RetroArch ficam em Ajustes → Emuladores.")
s("onb_emu_added", "%1$d folder(s) added", "%1$d carpeta(s) añadida(s)", "%1$d dossier(s) ajouté(s)", "%1$d Ordner hinzugefügt",
  "%1$d cartella/e aggiunta/e", "%1$d pasta(s) adicionada(s)")
s("onb_time_title", "Playtime", "Tiempo de juego", "Temps de jeu", "Spielzeit", "Tempo di gioco", "Tempo de jogo")
s("onb_time_body", "Allow “Usage access” so Repisa can measure exactly how long you play the games you open from here (only the time the emulator is on screen). Android will open the setting: find Repisa and turn it on.",
  "Permite el «Acceso a datos de uso» para que Repisa mida exactamente cuánto juegas a lo que abres desde aquí (solo el tiempo con el emulador en pantalla). Android abrirá el ajuste: busca Repisa y actívalo.",
  "Autorisez « Accès aux données d’utilisation » pour que Repisa mesure le temps de jeu exact de ce que vous lancez d’ici. Android ouvre le réglage : trouvez Repisa et activez-le.",
  "Erlaube den „Nutzungsdatenzugriff“, damit Repisa genau misst, wie lange du hier gestartete Spiele spielst. Android öffnet die Einstellung: Repisa suchen und aktivieren.",
  "Consenti l’«Accesso ai dati di utilizzo» così Repisa misura con precisione quanto giochi a ciò che avvii da qui. Android apre l’impostazione: trova Repisa e attivalo.",
  "Permita o \"Acesso ao uso\" para o Repisa medir exatamente quanto você joga o que abre daqui. O Android abre o ajuste: encontre o Repisa e ative.")
s("onb_done_title", "All set!", "¡Todo listo!", "C’est prêt !", "Alles bereit!", "Tutto pronto!", "Tudo pronto!")
s("onb_done_body", "Controller: D-pad to move · A details · X play · Y favourite · B back · L1/R1 platform · Select shelves · Start settings.\n🎲 next to the magnifier picks a game for you · tap the totals at the top for statistics · mark each game Backlog / Playing / Finished / 100% / Dropped · 🏆 achievements from Steam, RetroAchievements and Repisa.",
  "Mando: cruceta para moverte · A ficha · X jugar · Y favorito · B atrás · L1/R1 plataforma · Select repisas · Start ajustes.\n🎲 junto a la lupa elige un juego por ti · toca los totales de arriba para ver estadísticas · marca cada juego como Pendiente / Jugando / Terminado / 100 % / Abandonado · 🏆 logros de Steam, RetroAchievements y Repisa.",
  "Manette : croix pour bouger · A fiche · X jouer · Y favori · B retour · L1/R1 plateforme · Select étagères · Start réglages.\n🎲 à côté de la loupe choisit un jeu · touchez les totaux en haut pour les statistiques · marquez chaque jeu À faire / En cours / Terminé / 100 % / Abandonné · 🏆 succès Steam, RetroAchievements et Repisa.",
  "Controller: Steuerkreuz bewegen · A Details · X spielen · Y Favorit · B zurück · L1/R1 Plattform · Select Regale · Start Einstellungen.\n🎲 neben der Lupe wählt ein Spiel · tippe oben auf die Summen für Statistiken · markiere Spiele als Ausstehend / Spiele gerade / Beendet / 100 % / Abgebrochen · 🏆 Erfolge von Steam, RetroAchievements und Repisa.",
  "Controller: croce per muoverti · A scheda · X gioca · Y preferito · B indietro · L1/R1 piattaforma · Select scaffali · Start impostazioni.\n🎲 accanto alla lente sceglie un gioco · tocca i totali in alto per le statistiche · segna ogni gioco Da giocare / In corso / Finito / 100% / Abbandonato · 🏆 obiettivi Steam, RetroAchievements e Repisa.",
  "Controle: direcional para mover · A ficha · X jogar · Y favorito · B voltar · L1/R1 plataforma · Select estantes · Start ajustes.\n🎲 ao lado da lupa escolhe um jogo · toque nos totais em cima para estatísticas · marque cada jogo como Pendente / Jogando / Zerado / 100% / Abandonado · 🏆 conquistas do Steam, RetroAchievements e Repisa.")
s("onb_finish", "Let's play!", "¡A jugar!", "À vous de jouer !", "Los, spielen!", "Si gioca!", "Bora jogar!", ptPT="Vamos jogar!")
s("onb_skip_all", "Skip the guide", "Saltar la guía", "Passer le guide", "Anleitung überspringen", "Salta la guida", "Pular o guia",
  ptPT="Saltar o guia")
s("onb_again", "Show the start guide again", "Ver la guía de inicio otra vez", "Revoir le guide de démarrage", "Startanleitung erneut zeigen",
  "Rivedi la guida iniziale", "Ver o guia inicial de novo")
s("achov_unlocked", "achievements unlocked", "logros conseguidos", "succès débloqués", "Erfolge freigeschaltet", "obiettivi sbloccati",
  "conquistas desbloqueadas")
s("achov_games", "games with achievements", "juegos con logros", "jeux avec succès", "Spiele mit Erfolgen", "giochi con obiettivi",
  "jogos com conquistas")
s("achov_perfect", "games with every achievement", "juegos con todos los logros", "jeux à 100 % des succès", "Spiele mit allen Erfolgen",
  "giochi con tutti gli obiettivi", "jogos com todas as conquistas")
s("achov_repisa", "Repisa achievements", "logros de Repisa", "succès Repisa", "Repisa-Erfolge", "obiettivi Repisa", "conquistas do Repisa")
s("achov_recent", "Latest achievements", "Últimos logros", "Derniers succès", "Neueste Erfolge", "Ultimi obiettivi", "Últimas conquistas")
s("achov_by_game", "Progress by game", "Progreso por juego", "Progression par jeu", "Fortschritt pro Spiel", "Progressi per gioco",
  "Progresso por jogo")
s("achov_none", "No achievements loaded yet. They appear as Repisa checks your Steam games and RetroAchievements, or when you open a game's achievements.",
  "Aún no hay logros cargados. Aparecen a medida que Repisa revisa tus juegos de Steam y RetroAchievements, o al abrir los logros de un juego.",
  "Aucun succès chargé pour l’instant. Ils apparaissent quand Repisa vérifie vos jeux Steam et RetroAchievements, ou en ouvrant les succès d’un jeu.",
  "Noch keine Erfolge geladen. Sie erscheinen, während Repisa deine Steam- und RetroAchievements-Spiele prüft oder wenn du die Erfolge eines Spiels öffnest.",
  "Ancora nessun obiettivo. Compaiono mentre Repisa controlla i giochi Steam e RetroAchievements, o quando apri gli obiettivi di un gioco.",
  "Ainda não há conquistas carregadas. Elas aparecem conforme o Repisa verifica seus jogos do Steam e RetroAchievements, ou ao abrir as conquistas de um jogo.")
s("status_na", "Doesn't apply", "No aplica", "Sans objet", "Nicht zutreffend", "Non applicabile", "Não se aplica")
s("setting_auto_status", "Automatic status", "Estado automático", "Statut automatique", "Automatischer Status", "Stato automatico",
  "Status automático", ptPT="Estado automático")
s("setting_auto_status_hint", "Games you haven't marked: no playtime = Backlog, with playtime = Playing. A Backlog game you play (here, on PC, Steam or Epic) becomes Playing. Finished, 100%, Dropped and Doesn't apply are set by you.",
  "Juegos que no hayas marcado: sin tiempo jugado = Pendiente, con tiempo = Jugando. Un Pendiente que juegues (aquí, en el PC, Steam o Epic) pasa a Jugando. Terminado, 100 %, Abandonado y No aplica los pones tú.",
  "Jeux non marqués : sans temps de jeu = À faire, avec = En cours. Un jeu À faire que vous lancez (ici, sur PC, Steam ou Epic) passe En cours. Terminé, 100 %, Abandonné et Sans objet se marquent à la main.",
  "Nicht markierte Spiele: ohne Spielzeit = Ausstehend, mit Spielzeit = Spiele gerade. Ein ausstehendes Spiel, das du spielst (hier, am PC, Steam oder Epic), wechselt zu Spiele gerade. Beendet, 100 %, Abgebrochen und Nicht zutreffend setzt du selbst.",
  "Giochi non segnati: senza tempo = Da giocare, con tempo = In corso. Un gioco Da giocare che giochi (qui, su PC, Steam o Epic) passa a In corso. Finito, 100%, Abbandonato e Non applicabile li metti tu.",
  "Jogos que você não marcou: sem tempo jogado = Pendente, com tempo = Jogando. Um Pendente que você jogar (aqui, no PC, Steam ou Epic) passa a Jogando. Zerado, 100%, Abandonado e Não se aplica você marca.")
s("stats_most_played_hint", "Without the games marked “Doesn't apply” (Counter-Strike, Fortnite…).",
  "Sin los juegos marcados como «No aplica» (Counter-Strike, Fortnite…).",
  "Sans les jeux marqués « Sans objet » (Counter-Strike, Fortnite…).", "Ohne Spiele mit „Nicht zutreffend“ (Counter-Strike, Fortnite…).",
  "Senza i giochi segnati «Non applicabile» (Counter-Strike, Fortnite…).", "Sem os jogos marcados como \"Não se aplica\" (Counter-Strike, Fortnite…).")
s("msg_status_set", "%1$d game(s) → %2$s", "%1$d juego(s) → %2$s", "%1$d jeu(x) → %2$s", "%1$d Spiel(e) → %2$s", "%1$d gioco/i → %2$s",
  "%1$d jogo(s) → %2$s")
s("msg_added_collection", "%1$d game(s) added to “%2$s”", "%1$d juego(s) añadidos a «%2$s»", "%1$d jeu(x) ajouté(s) à « %2$s »",
  "%1$d Spiel(e) zu „%2$s“ hinzugefügt", "%1$d gioco/i aggiunti a «%2$s»", "%1$d jogo(s) adicionados a \"%2$s\"")
s("collection_default", "My collection", "Mi colección", "Ma collection", "Meine Sammlung", "La mia collezione", "Minha coleção",
  ptPT="A minha coleção")
s("collection", "Collection", "Colección", "Collection", "Sammlung", "Collezione", "Coleção")
s("collections", "Collections", "Colecciones", "Collections", "Sammlungen", "Collezioni", "Coleções")
s("collection_new", "New collection…", "Nueva colección…", "Nouvelle collection…", "Neue Sammlung…", "Nuova collezione…", "Nova coleção…")
s("collection_rename", "Rename", "Renombrar", "Renommer", "Umbenennen", "Rinomina", "Renomear", ptPT="Mudar o nome")
s("collection_hint", "e.g. Try out, Priority, With friends…", "p. ej. Probar, Prioridad, Con amigos…", "ex. À essayer, Priorité, Entre amis…",
  "z. B. Ausprobieren, Priorität, Mit Freunden…", "es. Da provare, Priorità, Con amici…", "ex. Experimentar, Prioridade, Com amigos…")
s("collections_empty", "You don't have collections yet. Create one: it appears as its own shelf at the top.",
  "Aún no tienes colecciones. Crea una: aparece como una repisa propia arriba del todo.",
  "Pas encore de collection. Créez-en une : elle apparaît comme une étagère en haut.",
  "Noch keine Sammlungen. Erstelle eine: Sie erscheint als eigenes Regal oben.",
  "Nessuna collezione. Creane una: appare come uno scaffale in alto.",
  "Você ainda não tem coleções. Crie uma: ela aparece como uma estante própria no topo.")
s("collections_hint", "Your own shelves (like Steam collections). Add games from a game's details (📂) or by selecting several. Deleting a collection doesn't delete its games.",
  "Tus propias repisas (como las colecciones de Steam). Añade juegos desde la ficha (📂) o seleccionando varios. Borrar una colección no borra sus juegos.",
  "Vos propres étagères (comme les collections Steam). Ajoutez des jeux depuis la fiche (📂) ou en en sélectionnant plusieurs. Supprimer une collection ne supprime pas ses jeux.",
  "Deine eigenen Regale (wie Steam-Sammlungen). Spiele über die Details (📂) oder per Mehrfachauswahl hinzufügen. Eine Sammlung löschen entfernt keine Spiele.",
  "I tuoi scaffali (come le collezioni di Steam). Aggiungi giochi dalla scheda (📂) o selezionandone diversi. Eliminare una collezione non elimina i giochi.",
  "Suas próprias estantes (como as coleções do Steam). Adicione jogos pela ficha (📂) ou selecionando vários. Apagar uma coleção não apaga os jogos.")
s("delete", "Delete", "Borrar", "Supprimer", "Löschen", "Elimina", "Apagar")
s("select_all", "Select all", "Seleccionar todo", "Tout sélectionner", "Alle auswählen", "Seleziona tutto", "Selecionar tudo")
s("selected_count", "%1$d selected · tap covers to add or remove", "%1$d seleccionados · toca carátulas para añadir o quitar",
  "%1$d sélectionnés · touchez les jaquettes", "%1$d ausgewählt · Cover antippen zum Hinzufügen/Entfernen",
  "%1$d selezionati · tocca le copertine", "%1$d selecionados · toque nas capas para adicionar ou tirar")
s("menu_collections", "Collections", "Colecciones", "Collections", "Sammlungen", "Collezioni", "Coleções")
s("menu_collections_sub", "Your own shelves: see, add, remove, delete", "Tus repisas propias: ver, añadir, quitar y borrar",
  "Vos étagères : voir, ajouter, retirer, supprimer", "Eigene Regale: ansehen, hinzufügen, entfernen, löschen",
  "I tuoi scaffali: vedi, aggiungi, togli, elimina", "Suas prateleiras: ver, adicionar, tirar, apagar")
s("collection_new", "New collection", "Nueva colección", "Nouvelle collection", "Neue Sammlung", "Nuova collezione", "Nova coleção")
s("collection_add_games", "Add games", "Añadir juegos", "Ajouter des jeux", "Spiele hinzufügen", "Aggiungi giochi", "Adicionar jogos")
s("collection_remove_from", "Remove from collection", "Quitar de colección", "Retirer de la collection", "Aus Sammlung entfernen",
  "Togli dalla collezione", "Tirar da coleção", ptPT="Retirar da coleção")
s("collection_delete_confirm", "Delete “%1$s”? Its games stay in your library.", "¿Borrar «%1$s»? Sus juegos siguen en tu biblioteca.",
  "Supprimer « %1$s » ? Ses jeux restent dans votre bibliothèque.", "„%1$s“ löschen? Die Spiele bleiben in deiner Bibliothek.",
  "Eliminare «%1$s»? I giochi restano nella tua libreria.", "Apagar \"%1$s\"? Os jogos continuam na sua biblioteca.")
s("collection_no_games", "No games yet: use “Add games”.", "Aún no tiene juegos: usa «Añadir juegos».",
  "Pas encore de jeux : utilisez « Ajouter des jeux ».", "Noch keine Spiele: „Spiele hinzufügen“ verwenden.",
  "Ancora nessun gioco: usa «Aggiungi giochi».", "Ainda sem jogos: use \"Adicionar jogos\".")
s("collection_pick_title", "Games in “%1$s”", "Juegos de «%1$s»", "Jeux de « %1$s »", "Spiele in „%1$s“", "Giochi in «%1$s»", "Jogos de \"%1$s\"")
s("msg_removed_collection", "%1$d game(s) removed from “%2$s”", "%1$d juego(s) quitados de «%2$s»", "%1$d jeu(x) retiré(s) de « %2$s »",
  "%1$d Spiel(e) aus „%2$s“ entfernt", "%1$d gioco/i tolti da «%2$s»", "%1$d jogo(s) tirados de \"%2$s\"")
s("launch_anim", "Animation when playing", "Animación al jugar", "Animation au lancement", "Animation beim Spielen", "Animazione all'avvio", "Animação ao jogar")
s("launch_anim_hint", "The case opens, the cartridge or disc goes into its console and the screen turns on. Tap or press any button to skip it.",
  "La caja se abre, el cartucho o el disco entra en su consola y se enciende la pantalla. Toca o pulsa cualquier botón para saltarla.",
  "La boîte s'ouvre, la cartouche ou le disque entre dans sa console et l'écran s'allume. Touchez ou appuyez sur un bouton pour passer.",
  "Die Hülle öffnet sich, Modul oder Disc kommt in die Konsole und der Bildschirm geht an. Tippen oder Taste drücken zum Überspringen.",
  "La custodia si apre, la cartuccia o il disco entra nella console e lo schermo si accende. Tocca o premi un tasto per saltarla.",
  "A caixa abre, o cartucho ou disco entra no console e a tela acende. Toque ou aperte qualquer botão para pular.")
s("launch_anim_always", "Always", "Siempre", "Toujours", "Immer", "Sempre", "Sempre")
s("launch_anim_daily", "First time each day", "La primera vez del día", "La première fois du jour", "Einmal am Tag", "La prima volta al giorno", "A primeira vez do dia")
s("launch_anim_never", "Never", "Nunca", "Jamais", "Nie", "Mai", "Nunca")
s("launch_anim_preview_all", "See the animations", "Ver las animaciones", "Voir les animations", "Animationen ansehen", "Vedi le animazioni", "Ver as animações")
s("launch_anim_others", "Others", "Otras", "Autres", "Andere", "Altre", "Outras")
s("status_order", "Order of the shelves by status", "Orden de las repisas por estado", "Ordre des étagères par statut",
  "Reihenfolge der Regale nach Status", "Ordine degli scaffali per stato", "Ordem das prateleiras por status", ptPT="Ordem das prateleiras por estado")
s("status_order_hint", "Used when grouping by status. Move each one with ▲ ▼.", "Se usa al agrupar por estado. Mueve cada uno con ▲ ▼.",
  "Utilisé pour le regroupement par statut. Déplacez avec ▲ ▼.", "Gilt beim Gruppieren nach Status. Mit ▲ ▼ verschieben.",
  "Usato quando raggruppi per stato. Sposta con ▲ ▼.", "Usado ao agrupar por status. Mova com ▲ ▼.")
s("status_order_reset", "Restore order", "Restaurar orden", "Rétablir l'ordre", "Reihenfolge zurücksetzen", "Ripristina ordine", "Restaurar ordem")
s("shelf_fold_hint", "Tip: tap a shelf's golden plaque to fold or unfold it (it stays that way).",
  "Truco: toca la placa dorada de una repisa para plegarla o desplegarla (se queda así).",
  "Astuce : touchez la plaque dorée d'une étagère pour la replier ou la déplier.",
  "Tipp: Tippe auf das goldene Schild eines Regals, um es ein- oder auszuklappen.",
  "Suggerimento: tocca la targhetta dorata di uno scaffale per chiuderlo o aprirlo.",
  "Dica: toque na placa dourada de uma prateleira para recolhê-la ou abri-la.")
s("status_clear", "Remove status (automatic)", "Quitar estado (automático)", "Retirer le statut (automatique)", "Status entfernen (automatisch)",
  "Togli stato (automatico)", "Tirar status (automático)", ptPT="Retirar estado (automático)")
s("random_platform", "Platform", "Plataforma", "Plateforme", "Plattform", "Piattaforma", "Plataforma")
s("random_any_platform", "Any platform", "Cualquier plataforma", "Toutes les plateformes", "Alle Plattformen", "Qualsiasi piattaforma",
  "Qualquer plataforma")
s("random_duration", "Length", "Duración", "Durée", "Dauer", "Durata", "Duração")
s("random_any_duration", "Any length", "Cualquier duración", "Toute durée", "Beliebige Dauer", "Qualsiasi durata", "Qualquer duração")
s("diag_copy", "Copy diagnostics", "Copiar diagnóstico", "Copier le diagnostic", "Diagnose kopieren", "Copia diagnostica", "Copiar diagnóstico")
s("diag_copied", "Copied ✓", "Copiado ✓", "Copié ✓", "Kopiert ✓", "Copiato ✓", "Copiado ✓")
s("diag_share", "Share diagnostics", "Compartir diagnóstico", "Partager le diagnostic", "Diagnose teilen", "Condividi diagnostica", "Compartilhar diagnóstico",
  ptPT="Partilhar diagnóstico")
s("frontend_reading", "Reading…", "Leyendo…", "Lecture…", "Wird gelesen…", "Lettura…", "Lendo…", ptPT="A ler…")
s("choose_cocoon", "Choose Cocoon folder", "Elegir carpeta de Cocoon", "Choisir le dossier Cocoon",
  "Cocoon-Ordner wählen", "Scegli cartella Cocoon", "Escolher pasta do Cocoon")
s("scan_emulators", "Scan emulators", "Escanear emuladores", "Analyser les émulateurs", "Emulatoren scannen",
  "Analizza emulatori", "Escanear emuladores", ptPT="Analisar emuladores")
s("emu_per_system_hint",
  "Repisa detects the emulators you have installed and opens each game directly (Eden for Switch, melonDS or DraStic for DS, Azahar for 3DS…). Tap a system to choose a different one. Repisa measures the time you play what you open from here.",
  "Repisa detecta los emuladores que tienes instalados y abre cada juego directamente (Eden para Switch, melonDS o DraStic para DS, Azahar para 3DS…). Toca un sistema para elegir otro. Repisa mide el tiempo que juegas a lo que abres desde aquí.",
  "Repisa détecte vos émulateurs installés et ouvre chaque jeu directement (Eden pour Switch, melonDS ou DraStic pour DS, Azahar pour 3DS…). Touchez un système pour en choisir un autre. Repisa mesure le temps de jeu de ce que vous lancez d’ici.",
  "Repisa erkennt deine installierten Emulatoren und öffnet jedes Spiel direkt (Eden für Switch, melonDS oder DraStic für DS, Azahar für 3DS…). Tippe auf ein System, um einen anderen zu wählen. Repisa misst die Spielzeit von hier gestarteter Spiele.",
  "Repisa rileva gli emulatori installati e apre ogni gioco direttamente (Eden per Switch, melonDS o DraStic per DS, Azahar per 3DS…). Tocca un sistema per sceglierne un altro. Repisa misura il tempo di gioco di ciò che avvii da qui.",
  "O Repisa detecta os emuladores instalados e abre cada jogo diretamente (Eden para Switch, melonDS ou DraStic para DS, Azahar para 3DS…). Toque num sistema para escolher outro. O Repisa mede o tempo que você joga o que abre daqui.")
s("emu_section", "Emulator for each system", "Emulador de cada sistema", "Émulateur de chaque système",
  "Emulator für jedes System", "Emulatore per ogni sistema", "Emulador de cada sistema")
s("emu_auto", "Automatic: %1$s", "Automático: %1$s", "Automatique : %1$s", "Automatisch: %1$s", "Automatico: %1$s", "Automático: %1$s")
s("emu_not_found", "No emulator found", "Sin emulador detectado", "Aucun émulateur détecté", "Kein Emulator gefunden",
  "Nessun emulatore rilevato", "Nenhum emulador detectado")
s("emu_auto_option", "Automatic (use the detected emulator)", "Automático (usar el emulador detectado)",
  "Automatique (utiliser l’émulateur détecté)", "Automatisch (erkannten Emulator verwenden)",
  "Automatico (usa l’emulatore rilevato)", "Automático (usar o emulador detectado)")
s("msg_emus_detected", "Emulators: %1$s", "Emuladores: %1$s", "Émulateurs : %1$s", "Emulatoren: %1$s", "Emulatori: %1$s", "Emuladores: %1$s")
s("emulator_for", "Emulator for %1$s", "Emulador para %1$s", "Émulateur pour %1$s", "Emulator für %1$s",
  "Emulatore per %1$s", "Emulador para %1$s")
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

# ------------------------------------------------------------------ Temas, calendario, resumen del año, compartir, lista de deseos, salvapantallas
s("menu_themes", "Themes", "Temas", "Thèmes", "Themen", "Temi", "Temas")
s("menu_themes_sub", "Wood, metal, glass or retro shop", "Madera, metal, cristal o tienda retro", "Bois, métal, verre ou boutique rétro",
  "Holz, Metall, Glas oder Retro-Laden", "Legno, metallo, vetro o negozio retrò", "Madeira, metal, vidro ou loja retrô", ptPT="Madeira, metal, vidro ou loja retro")
s("themes_hint", "Choose how your display cabinet looks. It changes every screen, including the bottom one.",
  "Elige cómo se ve tu vitrina. Cambia todas las pantallas, también la de abajo.",
  "Choisissez l'apparence de votre vitrine. Cela change tous les écrans, y compris celui du bas.",
  "Wähle das Aussehen deiner Vitrine. Es ändert alle Bildschirme, auch den unteren.",
  "Scegli l'aspetto della tua vetrina. Cambia tutte le schermate, anche quella in basso.",
  "Escolha a aparência da sua estante. Muda todas as telas, inclusive a de baixo.", ptPT="Escolha o aspeto da sua vitrine. Muda todos os ecrãs, incluindo o de baixo.")
s("theme_walnut", "Walnut", "Nogal", "Noyer", "Nussbaum", "Noce", "Nogueira")
s("theme_oak", "Light oak", "Madera clara", "Chêne clair", "Helle Eiche", "Rovere chiaro", "Carvalho claro")
s("theme_ebony", "Dark wood", "Madera oscura", "Bois sombre", "Dunkles Holz", "Legno scuro", "Madeira escura")
s("theme_metal", "Brushed metal", "Metal cepillado", "Métal brossé", "Gebürstetes Metall", "Metallo spazzolato", "Metal escovado")
s("theme_glass", "Glass", "Cristal", "Verre", "Glas", "Vetro", "Vidro")
s("theme_retro", "Retro game shop", "Tienda retro", "Boutique rétro", "Retro-Laden", "Negozio retrò", "Loja retrô", ptPT="Loja retro")

s("preview_sample_tag", "(sample data)", "(datos de ejemplo)", "(données d'exemple)", "(Beispieldaten)", "(dati di esempio)", "(dados de exemplo)")

s("cal_title", "Play calendar", "Calendario de partidas", "Calendrier de jeu", "Spielkalender", "Calendario delle partite", "Calendário de partidas")
s("cal_no_games_day", "No games this day.", "Este día no jugaste.", "Aucune partie ce jour-là.", "An diesem Tag nicht gespielt.",
  "Nessuna partita in questo giorno.", "Nenhuma partida neste dia.")
s("cal_day_total", "%1$s in %2$d sessions", "%1$s en %2$d partidas", "%1$s en %2$d parties", "%1$s in %2$d Sitzungen",
  "%1$s in %2$d partite", "%1$s em %2$d partidas")
s("cal_month_total", "This month: %1$s over %2$d days", "Este mes: %1$s en %2$d días", "Ce mois-ci : %1$s sur %2$d jours",
  "Diesen Monat: %1$s an %2$d Tagen", "Questo mese: %1$s in %2$d giorni", "Este mês: %1$s em %2$d dias")
s("cal_month_top", "Most played: %1$s (%2$s)", "El más jugado: %1$s (%2$s)", "Le plus joué : %1$s (%2$s)", "Am meisten gespielt: %1$s (%2$s)",
  "Il più giocato: %1$s (%2$s)", "O mais jogado: %1$s (%2$s)")
s("cal_session_synced", "%1$s · new hours seen at %2$s", "%1$s · horas nuevas vistas a las %2$s", "%1$s · nouvelles heures vues à %2$s",
  "%1$s · neue Stunden um %2$s erkannt", "%1$s · nuove ore rilevate alle %2$s", "%1$s · horas novas vistas às %2$s")
s("cal_game_sessions", "Sessions: %1$s", "Partidas: %1$s", "Parties : %1$s", "Sitzungen: %1$s", "Partite: %1$s", "Partidas: %1$s")
s("cal_game_none", "No sessions recorded yet. They are noted when you play from Repisa, or when a sync finds new hours (played on PC, in Steam…).",
  "Aún no hay partidas anotadas. Se apuntan al jugar desde Repisa o cuando al sincronizar aparecen horas nuevas (jugado en el PC, en Steam…).",
  "Aucune partie enregistrée pour l'instant. Elles sont notées quand vous jouez depuis Repisa ou quand une synchronisation trouve de nouvelles heures.",
  "Noch keine Sitzungen. Sie werden notiert, wenn du über Repisa spielst oder die Synchronisierung neue Stunden findet.",
  "Ancora nessuna partita registrata. Vengono annotate quando giochi da Repisa o quando la sincronizzazione trova nuove ore.",
  "Ainda não há partidas anotadas. São registradas ao jogar pelo Repisa ou quando a sincronização encontra horas novas.",
  ptPT="Ainda não há partidas anotadas. São registadas ao jogar a partir do Repisa ou quando a sincronização encontra horas novas.")
s("cal_game_button", "Sessions", "Partidas", "Parties", "Sitzungen", "Partite", "Partidas")

s("wrapped_name", "Your year in Repisa", "Tu año en Repisa", "Votre année sur Repisa", "Dein Jahr in Repisa", "Il tuo anno su Repisa", "Seu ano no Repisa",
  ptPT="O seu ano no Repisa")
s("wrapped_intro_title", "Your %1$d in Repisa", "Tu %1$d en Repisa", "Votre %1$d sur Repisa", "Dein %1$d in Repisa", "Il tuo %1$d su Repisa",
  "Seu %1$d no Repisa", ptPT="O seu %1$d no Repisa")
s("wrapped_intro_sub", "Let's see what happened on your shelves this year", "Vamos a ver qué ha pasado en tu repisa este año",
  "Voyons ce qui s'est passé sur vos étagères cette année", "Mal sehen, was dieses Jahr in deinem Regal passiert ist",
  "Vediamo cos'è successo sui tuoi scaffali quest'anno", "Vamos ver o que aconteceu na sua estante este ano")
s("wrapped_hours_pre", "In %1$d you played", "En %1$d jugaste", "En %1$d, vous avez joué", "%1$d hast du", "Nel %1$d hai giocato",
  "Em %1$d você jogou", ptPT="Em %1$d jogou")
s("wrapped_hours_unit", "hours", "horas", "heures", "Stunden gespielt", "ore", "horas")
s("wrapped_hours_detail", "%1$d sessions · %2$d days with games · %3$d different games",
  "%1$d partidas · %2$d días con juego · %3$d juegos distintos", "%1$d parties · %2$d jours de jeu · %3$d jeux différents",
  "%1$d Sitzungen · %2$d Spieltage · %3$d verschiedene Spiele", "%1$d partite · %2$d giorni di gioco · %3$d giochi diversi",
  "%1$d partidas · %2$d dias com jogo · %3$d jogos diferentes")
s("wrapped_hours_days", "That's %1$s whole days non-stop!", "¡Eso son %1$s días enteros sin parar!", "Soit %1$s jours entiers sans s'arrêter !",
  "Das sind %1$s ganze Tage am Stück!", "Sono %1$s giorni interi senza sosta!", "Isso dá %1$s dias inteiros sem parar!")
s("wrapped_top_title", "Your top 5 of the year", "Tu top 5 del año", "Votre top 5 de l'année", "Deine Top 5 des Jahres", "La tua top 5 dell'anno",
  "Seu top 5 do ano", ptPT="O seu top 5 do ano")
s("wrapped_nothing", "No sessions recorded this year yet.", "Aún no hay partidas anotadas este año.", "Aucune partie enregistrée cette année.",
  "Dieses Jahr noch keine Sitzungen.", "Ancora nessuna partita registrata quest'anno.", "Ainda não há partidas registradas este ano.",
  ptPT="Ainda não há partidas registadas este ano.")
s("wrapped_months_title", "Month by month", "Mes a mes", "Mois par mois", "Monat für Monat", "Mese per mese", "Mês a mês")
s("wrapped_months_best", "Your busiest month: %1$s (%2$s)", "Tu mes más jugón: %1$s (%2$s)", "Votre mois le plus joueur : %1$s (%2$s)",
  "Dein spielreichster Monat: %1$s (%2$s)", "Il tuo mese più giocato: %1$s (%2$s)", "Seu mês mais jogado: %1$s (%2$s)", ptPT="O seu mês mais jogado: %1$s (%2$s)")
s("wrapped_facts_title", "Your year in a nutshell", "Tu año en datos", "Votre année en bref", "Dein Jahr in Zahlen", "Il tuo anno in breve", "Seu ano em números",
  ptPT="O seu ano em números")
s("wrapped_fact_genre", "favorite genre", "género favorito", "genre préféré", "Lieblingsgenre", "genere preferito", "gênero favorito", ptPT="género favorito")
s("wrapped_fact_console", "most played system", "sistema más jugado", "système le plus joué", "meistgespieltes System", "sistema più giocato", "sistema mais jogado")
s("wrapped_fact_finished", "games finished", "juegos terminados", "jeux terminés", "Spiele beendet", "giochi finiti", "jogos terminados")
s("wrapped_fact_streak", "longest streak", "racha más larga", "plus longue série", "längste Serie", "serie più lunga", "maior sequência")
s("wrapped_fact_streak_value", "%1$d days", "%1$d días", "%1$d jours", "%1$d Tage", "%1$d giorni", "%1$d dias")
s("wrapped_card_title", "My %1$d in games", "Mi %1$d en juegos", "Mon %1$d en jeux", "Mein %1$d in Spielen", "Il mio %1$d nei giochi", "Meu %1$d em jogos",
  ptPT="O meu %1$d em jogos")
s("wrapped_card_line", "%1$s · %2$d games · %3$d days playing", "%1$s · %2$d juegos · %3$d días jugando", "%1$s · %2$d jeux · %3$d jours de jeu",
  "%1$s · %2$d Spiele · %3$d Spieltage", "%1$s · %2$d giochi · %3$d giorni di gioco", "%1$s · %2$d jogos · %3$d dias jogando",
  ptPT="%1$s · %2$d jogos · %3$d dias a jogar")
s("wrapped_card_finished", "%1$d finished", "%1$d terminados", "%1$d terminés", "%1$d beendet", "%1$d finiti", "%1$d terminados")
s("wrapped_card_streak", "%1$d-day streak", "racha de %1$d días", "série de %1$d jours", "%1$d Tage am Stück", "serie di %1$d giorni", "sequência de %1$d dias")
s("wrapped_share_text", "My %1$d in games, made with Repisa 🎮", "Mi %1$d en juegos, hecho con Repisa 🎮", "Mon %1$d en jeux, fait avec Repisa 🎮",
  "Mein %1$d in Spielen, erstellt mit Repisa 🎮", "Il mio %1$d nei giochi, creato con Repisa 🎮", "Meu %1$d em jogos, feito com o Repisa 🎮",
  ptPT="O meu %1$d em jogos, feito com o Repisa 🎮")

s("share_save", "Save to gallery", "Guardar en la galería", "Enregistrer dans la galerie", "In Galerie speichern", "Salva nella galleria",
  "Salvar na galeria", ptPT="Guardar na galeria")
s("share_share", "Share", "Compartir", "Partager", "Teilen", "Condividi", "Compartilhar", ptPT="Partilhar")
s("share_saved", "Saved in Pictures › Repisa", "Guardada en Imágenes › Repisa", "Enregistrée dans Images › Repisa", "In Bilder › Repisa gespeichert",
  "Salvata in Immagini › Repisa", "Salva em Imagens › Repisa", ptPT="Guardada em Imagens › Repisa")
s("share_failed", "Couldn't save the image", "No se pudo guardar la imagen", "Impossible d'enregistrer l'image", "Bild konnte nicht gespeichert werden",
  "Impossibile salvare l'immagine", "Não foi possível salvar a imagem", ptPT="Não foi possível guardar a imagem")

s("wish_shelf", "Wishlist", "Lista de deseos", "Liste de souhaits", "Wunschliste", "Lista dei desideri", "Lista de desejos")
s("wish_on_sale", "%1$d on sale", "%1$d en oferta", "%1$d en promo", "%1$d im Angebot", "%1$d in offerta", "%1$d em promoção")
s("wish_no_price", "No price yet", "Sin precio", "Pas de prix", "Kein Preis", "Senza prezzo", "Sem preço")
s("wish_desc", "Your Steam wishlist as a shelf of its own, with the current price of each game. Tapping a box opens its store page.",
  "Tu lista de deseos de Steam en una repisa propia, con el precio actual de cada juego. Al tocar una caja se abre su página de la tienda.",
  "Votre liste de souhaits Steam sur une étagère à part, avec le prix actuel de chaque jeu. Touchez une boîte pour ouvrir sa page du magasin.",
  "Deine Steam-Wunschliste als eigenes Regal mit dem aktuellen Preis jedes Spiels. Antippen öffnet die Shopseite.",
  "La tua lista dei desideri di Steam in uno scaffale tutto suo, con il prezzo attuale di ogni gioco. Tocca una scatola per aprire la pagina del negozio.",
  "Sua lista de desejos da Steam numa estante própria, com o preço atual de cada jogo. Tocar numa caixa abre a página da loja.")
s("wish_show", "Show the wishlist shelf", "Mostrar la repisa de la lista de deseos", "Afficher l'étagère de la liste de souhaits",
  "Wunschlisten-Regal anzeigen", "Mostra lo scaffale della lista dei desideri", "Mostrar a estante da lista de desejos")
s("wish_alerts", "Notify me when a price drops", "Avisarme cuando baje un precio", "M'avertir quand un prix baisse", "Benachrichtigen, wenn ein Preis sinkt",
  "Avvisami quando un prezzo scende", "Avisar quando um preço baixar")
s("wish_alerts_hint", "Prices are checked every time Repisa syncs (when you open it).", "Los precios se miran cada vez que Repisa sincroniza (al abrirla).",
  "Les prix sont vérifiés à chaque synchronisation de Repisa (à l'ouverture).", "Die Preise werden bei jeder Synchronisierung geprüft (beim Öffnen).",
  "I prezzi vengono controllati a ogni sincronizzazione di Repisa (all'apertura).", "Os preços são verificados sempre que o Repisa sincroniza (ao abrir).")
s("wish_none_yet", "Not loaded yet.", "Todavía no se ha cargado.", "Pas encore chargée.", "Noch nicht geladen.", "Non ancora caricata.", "Ainda não carregada.")
s("wish_loaded", "%1$d games on your wishlist", "%1$d juegos en tu lista de deseos", "%1$d jeux dans votre liste de souhaits",
  "%1$d Spiele auf deiner Wunschliste", "%1$d giochi nella tua lista dei desideri", "%1$d jogos na sua lista de desejos")
s("wish_refresh", "Update now", "Actualizar ahora", "Mettre à jour", "Jetzt aktualisieren", "Aggiorna ora", "Atualizar agora")
s("wish_empty", "Your Steam wishlist is empty (or private).", "Tu lista de deseos de Steam está vacía (o es privada).",
  "Votre liste de souhaits Steam est vide (ou privée).", "Deine Steam-Wunschliste ist leer (oder privat).",
  "La tua lista dei desideri di Steam è vuota (o privata).", "Sua lista de desejos da Steam está vazia (ou é privada).")
s("wish_error", "Couldn't load the wishlist: %1$s", "No se pudo cargar la lista de deseos: %1$s", "Impossible de charger la liste de souhaits : %1$s",
  "Wunschliste konnte nicht geladen werden: %1$s", "Impossibile caricare la lista dei desideri: %1$s", "Não foi possível carregar a lista de desejos: %1$s")
s("wish_no_account", "Link your Steam account first", "Vincula primero tu cuenta de Steam", "Liez d'abord votre compte Steam",
  "Verknüpfe zuerst dein Steam-Konto", "Collega prima il tuo account Steam", "Vincule primeiro sua conta Steam", ptPT="Associe primeiro a sua conta Steam")
s("wish_channel", "Wishlist price drops", "Bajadas de precio de la lista de deseos", "Baisses de prix de la liste de souhaits",
  "Preissenkungen der Wunschliste", "Cali di prezzo della lista dei desideri", "Quedas de preço da lista de desejos")
s("wish_drop_title", "💸 %1$s is cheaper", "💸 %1$s ha bajado de precio", "💸 %1$s est moins cher", "💸 %1$s ist günstiger",
  "💸 %1$s costa meno", "💸 %1$s baixou de preço")
s("wish_drop_text", "%1$s → %2$s", "%1$s → %2$s", "%1$s → %2$s", "%1$s → %2$s", "%1$s → %2$s", "%1$s → %2$s")

s("screensaver_title", "Screensaver", "Salvapantallas", "Économiseur d'écran", "Bildschirmschoner", "Salvaschermo", "Proteção de tela",
  ptPT="Proteção de ecrã")
s("screensaver_desc", "After a while without touching anything, your covers float slowly across the cabinet with a big clock. Any button or touch closes it.",
  "Tras un rato sin tocar nada, tus carátulas flotan despacio por la vitrina con un reloj grande. Cualquier botón o toque lo quita.",
  "Après un moment sans rien toucher, vos jaquettes flottent lentement dans la vitrine avec une grande horloge. N'importe quel bouton ou toucher le ferme.",
  "Nach einer Weile ohne Eingabe schweben deine Cover langsam durch die Vitrine, mit großer Uhr. Jede Taste oder Berührung beendet ihn.",
  "Dopo un po' senza toccare nulla, le copertine fluttuano lentamente nella vetrina con un grande orologio. Qualsiasi tasto o tocco lo chiude.",
  "Depois de um tempo sem tocar em nada, suas capas flutuam devagar pela estante com um relógio grande. Qualquer botão ou toque o fecha.",
  ptPT="Depois de algum tempo sem tocar em nada, as capas flutuam devagar pela vitrine com um relógio grande. Qualquer botão ou toque fecha-a.")
s("screensaver_never", "Never", "Nunca", "Jamais", "Nie", "Mai", "Nunca")
s("screensaver_min", "%1$d min", "%1$d min", "%1$d min", "%1$d Min.", "%1$d min", "%1$d min")
s("screensaver_off", "Screensaver off", "Salvapantallas desactivado", "Économiseur désactivé", "Bildschirmschoner aus", "Salvaschermo disattivato",
  "Proteção de tela desativada", ptPT="Proteção de ecrã desativada")
s("screensaver_after", "Starts after %1$d min without touching anything", "Salta a los %1$d min sin tocar nada", "Démarre après %1$d min d'inactivité",
  "Startet nach %1$d Min. ohne Eingabe", "Parte dopo %1$d min senza toccare nulla", "Começa após %1$d min sem tocar em nada")
s("screensaver_try", "Try the screensaver", "Probar salvapantallas", "Essayer l'économiseur", "Bildschirmschoner testen", "Prova il salvaschermo",
  "Testar proteção de tela", ptPT="Experimentar proteção de ecrã")
s("screensaver_hint", "Press any button to come back", "Pulsa cualquier botón para volver", "Appuyez sur un bouton pour revenir",
  "Beliebige Taste drücken, um zurückzukehren", "Premi un tasto qualsiasi per tornare", "Pressione qualquer botão para voltar", ptPT="Prima qualquer botão para voltar")

s("ra_link", "Link RetroAchievements", "Vincular RetroAchievements", "Lier RetroAchievements", "RetroAchievements verknüpfen", "Collega RetroAchievements",
  "Vincular RetroAchievements", ptPT="Associar RetroAchievements")
s("ra_linked", "Linked as %1$s", "Vinculado como %1$s", "Lié en tant que %1$s", "Verknüpft als %1$s", "Collegato come %1$s", "Vinculado como %1$s",
  ptPT="Associado como %1$s")
s("field_tap_to_write", "Press to type", "Pulsa para escribir", "Appuyez pour écrire", "Zum Schreiben drücken", "Premi per scrivere", "Toque para escrever",
  ptPT="Prima para escrever")


s("theme_custom", "Custom", "Personalizado", "Personnalisé", "Eigenes", "Personalizzato", "Personalizado")
s("theme_custom_hint", "Choose a texture and the colors of the wall, the shelves and the light. Changes apply at once.",
  "Elige una textura y los colores de la pared, las baldas y la luz. Los cambios se aplican al momento.",
  "Choisissez une texture et les couleurs du mur, des étagères et de la lumière. Les changements s'appliquent tout de suite.",
  "Wähle eine Textur und die Farben von Wand, Regalböden und Licht. Änderungen gelten sofort.",
  "Scegli una texture e i colori della parete, dei ripiani e della luce. Le modifiche si applicano subito.",
  "Escolha uma textura e as cores da parede, das prateleiras e da luz. As mudanças são aplicadas na hora.",
  ptPT="Escolha uma textura e as cores da parede, das prateleiras e da luz. As alterações aplicam-se logo.")
s("theme_texture", "Texture", "Textura", "Texture", "Textur", "Texture", "Textura")
s("texture_wood", "Wood", "Madera", "Bois", "Holz", "Legno", "Madeira")
s("texture_metal", "Metal", "Metal", "Métal", "Metall", "Metallo", "Metal")
s("texture_glass", "Glass", "Cristal", "Verre", "Glas", "Vetro", "Vidro")
s("texture_pegboard", "Pegboard", "Panel perforado", "Panneau perforé", "Lochwand", "Pannello forato", "Painel perfurado")
s("theme_color_wall", "Wall color", "Color de la pared", "Couleur du mur", "Wandfarbe", "Colore della parete", "Cor da parede")
s("theme_color_shelf", "Shelf color", "Color de las baldas", "Couleur des étagères", "Farbe der Regalböden", "Colore dei ripiani", "Cor das prateleiras")
s("theme_color_light", "Light color", "Color de la luz", "Couleur de la lumière", "Lichtfarbe", "Colore della luce", "Cor da luz")
s("cal_day_last_only", "Last day you played %1$d game(s) (time not known)", "Último día que jugaste %1$d juego(s) (sin duración)",
  "Dernier jour où vous avez joué à %1$d jeu(x) (durée inconnue)", "Letzter Spieltag für %1$d Spiel(e) (Dauer unbekannt)",
  "Ultimo giorno in cui hai giocato a %1$d gioco/i (durata sconosciuta)", "Último dia em que você jogou %1$d jogo(s) (sem duração)",
  ptPT="Último dia em que jogou %1$d jogo(s) (sem duração)")
s("cal_last_played", "Last time played (from Steam / the emulator, no duration)", "Última vez que lo jugaste (según Steam o el emulador, sin duración)",
  "Dernière partie (selon Steam ou l'émulateur, sans durée)", "Zuletzt gespielt (laut Steam/Emulator, ohne Dauer)",
  "Ultima volta giocato (secondo Steam o l'emulatore, senza durata)", "Última vez que jogou (segundo a Steam ou o emulador, sem duração)")
s("wish_checked", "checked %1$s · store: %2$s", "consultada %1$s · tienda: %2$s", "vérifiée %1$s · magasin : %2$s", "geprüft %1$s · Shop: %2$s",
  "controllata %1$s · negozio: %2$s", "consultada %1$s · loja: %2$s")

s("wish_sort_steam", "Steam order", "Orden de Steam", "Ordre de Steam", "Steam-Reihenfolge", "Ordine di Steam", "Ordem da Steam")
s("wish_sort_name", "Name", "Nombre", "Nom", "Name", "Nome", "Nome")
s("wish_sort_price_desc", "Price: high to low", "Precio: de mayor a menor", "Prix décroissant", "Preis: absteigend", "Prezzo: dal più alto",
  "Preço: maior para menor")
s("wish_sort_price_asc", "Price: low to high", "Precio: de menor a mayor", "Prix croissant", "Preis: aufsteigend", "Prezzo: dal più basso",
  "Preço: menor para maior")
s("wish_sort_discount", "Biggest discount", "Mayor descuento", "Plus grosse remise", "Größter Rabatt", "Sconto maggiore", "Maior desconto")
s("wish_refresh_prices", "Update prices", "Actualizar precios", "Actualiser les prix", "Preise aktualisieren", "Aggiorna prezzi", "Atualizar preços")
s("wish_refreshing", "Checking…", "Consultando…", "Vérification…", "Wird geprüft…", "Controllo…", "Consultando…")
s("wish_background", "Also check with Repisa closed", "Revisar también con Repisa cerrada", "Vérifier aussi avec Repisa fermée",
  "Auch bei geschlossenem Repisa prüfen", "Controlla anche con Repisa chiusa", "Verificar também com o Repisa fechado")
s("wish_background_hint", "Every 6 hours, when there's internet. You'll get a notification for each game that drops in price.",
  "Cada 6 horas, cuando haya internet. Te llega una notificación por cada juego que baje de precio.",
  "Toutes les 6 heures, avec internet. Une notification par jeu dont le prix baisse.",
  "Alle 6 Stunden, wenn Internet da ist. Eine Benachrichtigung pro Spiel, das günstiger wird.",
  "Ogni 6 ore, quando c'è internet. Una notifica per ogni gioco che cala di prezzo.",
  "A cada 6 horas, quando houver internet. Você recebe uma notificação por jogo que baixar de preço.",
  ptPT="A cada 6 horas, quando houver internet. Recebe uma notificação por cada jogo que baixe de preço.")

# ------------------------------------------------------------------ Logros: categorías, platino y recordatorio
s("tier_bronze", "Bronze", "Bronce", "Bronze", "Bronze", "Bronzo", "Bronze")
s("tier_silver", "Silver", "Plata", "Argent", "Silber", "Argento", "Prata")
s("tier_gold", "Gold", "Oro", "Or", "Gold", "Oro", "Ouro")
s("tier_platinum", "Platinum", "Platino", "Platine", "Platin", "Platino", "Platina")
s("plat_title", "Platinum", "Platino", "Platine", "Platin", "Platino", "Platina")
s("plat_got", "Platinum unlocked!", "¡Platino conseguido!", "Platine obtenu !", "Platin freigeschaltet!", "Platino ottenuto!", "Platina conquistada!")
s("plat_left_platform", "%1$d achievements left", "Faltan %1$d logros", "Il reste %1$d succès", "Noch %1$d Erfolge", "Mancano %1$d obiettivi",
  "Faltam %1$d conquistas")
s("plat_left_repisa", "%1$d bronze/silver Repisa achievements left", "Faltan %1$d logros de Repisa de bronce o plata",
  "Il reste %1$d succès Repisa bronze/argent", "Noch %1$d Repisa-Erfolge (Bronze/Silber)", "Mancano %1$d obiettivi Repisa di bronzo/argento",
  "Faltam %1$d conquistas Repisa de bronze/prata")
s("plat_left_status", "mark it as 100 %", "márcalo como 100 %", "marquez-le à 100 %", "auf 100 % setzen", "segnalo al 100 %", "marque como 100 %")
s("plat_new", "Platinum! %1$s", "¡Platino! %1$s", "Platine ! %1$s", "Platin! %1$s", "Platino! %1$s", "Platina! %1$s")
s("plat_count", "platinums", "platinos", "platines", "Platin", "platini", "platinas")
s("plat_collection", "Platinum collection", "Colección de platinos", "Collection de platines", "Platin-Sammlung", "Collezione di platini",
  "Coleção de platinas")
s("plat_none", "No platinums yet. A game gets one when it's marked 100 % and has all its achievements (Steam, RetroAchievements… or, if it has none, all of Repisa's bronze and silver ones).",
  "Aún no hay platinos. Un juego lo consigue cuando está marcado como 100 % y tiene todos sus logros (Steam, RetroAchievements… o, si no tiene, todos los de Repisa de bronce y plata).",
  "Pas encore de platine. Un jeu l'obtient quand il est à 100 % et a tous ses succès (Steam, RetroAchievements… ou, s'il n'en a pas, tous ceux de Repisa en bronze et argent).",
  "Noch kein Platin. Ein Spiel bekommt es, wenn es auf 100 % steht und alle Erfolge hat (Steam, RetroAchievements … oder, wenn es keine hat, alle Repisa-Erfolge in Bronze und Silber).",
  "Ancora nessun platino. Un gioco lo ottiene quando è al 100 % e ha tutti gli obiettivi (Steam, RetroAchievements… o, se non ne ha, tutti quelli di Repisa di bronzo e argento).",
  "Ainda não há platinas. Um jogo ganha quando está em 100 % e tem todas as conquistas (Steam, RetroAchievements… ou, se não tiver, todas as da Repisa de bronze e prata).")
s("aach_title", "Repisa achievements", "Logros de Repisa", "Succès Repisa", "Repisa-Erfolge", "Obiettivi di Repisa", "Conquistas da Repisa")
s("aach_count", "Repisa achievements", "logros de la app", "succès de l'app", "App-Erfolge", "obiettivi dell'app", "conquistas do app")
s("aach_new", "Achievement: %1$s", "Logro: %1$s", "Succès : %1$s", "Erfolg: %1$s", "Obiettivo: %1$s", "Conquista: %1$s")
s("reminder_title", "Play today…", "Hoy juego…", "Aujourd'hui je joue…", "Heute spiele ich …", "Oggi gioco…", "Hoje eu jogo…")
s("reminder_stale", "You haven't touched it in %1$d days", "Llevas %1$d días sin tocarlo", "Pas touché depuis %1$d jours", "Seit %1$d Tagen nicht gespielt",
  "Non lo tocchi da %1$d giorni", "Faz %1$d dias que você não joga", ptPT="Há %1$d dias que não joga")
s("reminder_backlog_hltb", "Waiting in your backlog · about %1$s h", "Esperando en pendientes · unas %1$s h", "En attente · environ %1$s h",
  "Wartet auf dich · ca. %1$s Std.", "In attesa · circa %1$s h", "Esperando nos pendentes · cerca de %1$s h")
s("reminder_backlog", "Waiting in your backlog", "Esperando en pendientes", "En attente dans vos jeux à faire", "Wartet in deinem Backlog",
  "In attesa tra i giochi da fare", "Esperando nos pendentes")
s("cal_release_day", "Wishlist releases (%1$d)", "Estrenos de tu lista de deseos (%1$d)", "Sorties de votre liste de souhaits (%1$d)",
  "Veröffentlichungen deiner Wunschliste (%1$d)", "Uscite della tua lista dei desideri (%1$d)", "Lançamentos da sua lista de desejos (%1$d)")
s("cal_release_soon", "Releases this day", "Sale este día", "Sort ce jour-là", "Erscheint an diesem Tag", "Esce questo giorno", "Lança neste dia")
s("cal_release_out", "Released this day", "Salió este día", "Sorti ce jour-là", "An diesem Tag erschienen", "Uscito questo giorno", "Lançado neste dia")
s("week_title", "Your week", "Tu semana", "Votre semaine", "Deine Woche", "La tua settimana", "Sua semana", ptPT="A sua semana")
s("week_summary", "%1$s · %2$d days playing", "%1$s · %2$d días jugando", "%1$s · %2$d jours de jeu", "%1$s · %2$d Spieltage",
  "%1$s · %2$d giorni di gioco", "%1$s · %2$d dias jogando", ptPT="%1$s · %2$d dias a jogar")

# Descripciones con número
s("rd_hours1", "Play it for 1 hour.", "Juégalo 1 hora.", "Jouez-y 1 heure.", "Spiele es 1 Stunde.", "Giocaci 1 ora.", "Jogue por 1 hora.")
s("rd_hours", "Play it for %1$d hours.", "Juégalo %1$d horas.", "Jouez-y %1$d heures.", "Spiele es %1$d Stunden.", "Giocaci %1$d ore.", "Jogue por %1$d horas.")
s("rd_sessions", "%1$d recorded sessions.", "%1$d partidas anotadas.", "%1$d parties enregistrées.", "%1$d erfasste Sitzungen.",
  "%1$d partite registrate.", "%1$d partidas registradas.")
s("rd_days", "Play it on %1$d different days.", "Juégalo en %1$d días distintos.", "Jouez-y %1$d jours différents.",
  "Spiele es an %1$d verschiedenen Tagen.", "Giocaci in %1$d giorni diversi.", "Jogue em %1$d dias diferentes.")
s("rd_streak", "Play it %1$d days in a row.", "Juégalo %1$d días seguidos.", "Jouez-y %1$d jours d'affilée.", "Spiele es %1$d Tage am Stück.",
  "Giocaci %1$d giorni di fila.", "Jogue %1$d dias seguidos.")
s("rd_months", "Play it in %1$d different months.", "Juégalo en %1$d meses distintos.", "Jouez-y pendant %1$d mois différents.",
  "Spiele es in %1$d verschiedenen Monaten.", "Giocaci in %1$d mesi diversi.", "Jogue em %1$d meses diferentes.")
s("rd_session_hours", "One session of %1$d hours in a row (launched from Repisa).", "Una partida de %1$d horas seguidas (abierta desde Repisa).",
  "Une partie de %1$d heures d'affilée (lancée depuis Repisa).", "Eine Sitzung von %1$d Stunden am Stück (über Repisa gestartet).",
  "Una partita di %1$d ore di fila (avviata da Repisa).", "Uma partida de %1$d horas seguidas (aberta pelo Repisa).")
s("rd_day_hours", "%1$d hours in a single day.", "%1$d horas en un mismo día.", "%1$d heures dans la même journée.", "%1$d Stunden an einem Tag.",
  "%1$d ore in un solo giorno.", "%1$d horas no mesmo dia.")

# Logros de cada juego: nombres y descripciones propias
s("rach_hour1", "Warming up", "Calentando", "Échauffement", "Aufwärmen", "Riscaldamento", "Aquecendo", ptPT="A aquecer")
s("rach_from_repisa", "From the shelf", "Desde la repisa", "Depuis l'étagère", "Aus dem Regal", "Dallo scaffale", "Da estante")
s("rach_from_repisa_d", "Launch it from Repisa and play.", "Ábrelo desde Repisa y juega.", "Lancez-le depuis Repisa et jouez.",
  "Starte es über Repisa und spiele.", "Avvialo da Repisa e gioca.", "Abra pelo Repisa e jogue.")
s("rach_sessions_3", "Be right back", "Vuelvo enseguida", "Je reviens vite", "Bin gleich zurück", "Torno subito", "Já volto")
s("rach_days_2", "Day two", "Segundo día", "Deuxième jour", "Tag zwei", "Secondo giorno", "Segundo dia")
s("rach_session_30m", "Coffee break", "Pausa para el café", "Pause café", "Kaffeepause", "Pausa caffè", "Pausa para o café")
s("rach_session_30m_d", "A 30-minute session (launched from Repisa).", "Una partida de 30 minutos seguidos (abierta desde Repisa).",
  "Une partie de 30 minutes d'affilée (lancée depuis Repisa).", "Eine 30-minütige Sitzung (über Repisa gestartet).",
  "Una partita di 30 minuti di fila (avviata da Repisa).", "Uma partida de 30 minutos seguidos (aberta pelo Repisa).")
s("rach_weekend", "Weekend", "Fin de semana", "Week-end", "Wochenende", "Fine settimana", "Fim de semana")
s("rach_weekend_d", "Play it on a Saturday or Sunday.", "Juégalo un sábado o un domingo.", "Jouez-y un samedi ou un dimanche.",
  "Spiele es an einem Samstag oder Sonntag.", "Giocaci di sabato o domenica.", "Jogue num sábado ou domingo.")
s("rach_evening", "After hours", "Después del trabajo", "Après le travail", "Feierabend", "Dopo il lavoro", "Depois do trabalho")
s("rach_evening_d", "Play it in the evening (7–11 pm).", "Juégalo por la tarde-noche (19–23 h).", "Jouez-y en soirée (19 h–23 h).",
  "Spiele es abends (19–23 Uhr).", "Giocaci di sera (19–23).", "Jogue à noite (19–23 h).")
s("rach_collection", "Well kept", "Bien guardado", "Bien rangé", "Gut verstaut", "Ben conservato", "Bem guardado")
s("rach_collection_d", "Add it to one of your collections.", "Añádelo a una de tus colecciones.", "Ajoutez-le à une de vos collections.",
  "Füge es einer deiner Sammlungen hinzu.", "Aggiungilo a una delle tue collezioni.", "Adicione a uma das suas coleções.")
s("rach_status", "Labelled", "Con estado", "Étiqueté", "Beschriftet", "Etichettato", "Com estado")
s("rach_status_d", "Set its status yourself.", "Ponle un estado a mano.", "Définissez son statut vous-même.", "Setze seinen Status selbst.",
  "Imposta tu il suo stato.", "Defina o estado manualmente.")
s("rach_session_2h", "Long session", "Sesión larga", "Longue session", "Lange Sitzung", "Sessione lunga", "Sessão longa")
s("rach_day_3h", "Gaming afternoon", "Tarde de juego", "Après-midi de jeu", "Spielnachmittag", "Pomeriggio di gioco", "Tarde de jogo")
s("rach_sessions_10", "Regular", "Habitual", "Habitué", "Stammgast", "Abituale", "Habitual")
s("rach_days_7", "A week of play", "Una semana de juego", "Une semaine de jeu", "Eine Woche Spielzeit", "Una settimana di gioco", "Uma semana de jogo")
s("rach_streak_3", "Three in a row", "Tres seguidos", "Trois d'affilée", "Drei am Stück", "Tre di fila", "Três seguidos")
s("rach_three_a_day", "Can't stop", "No puedo parar", "Impossible d'arrêter", "Kann nicht aufhören", "Non riesco a smettere", "Não consigo parar")
s("rach_three_a_day_d", "Three sessions on the same day.", "Tres partidas el mismo día.", "Trois parties le même jour.", "Drei Sitzungen am selben Tag.",
  "Tre partite nello stesso giorno.", "Três partidas no mesmo dia.")
s("rach_night", "Night owl", "Búho", "Oiseau de nuit", "Nachteule", "Nottambulo", "Coruja")
s("rach_night_d", "Start a session between midnight and 5 am (from Repisa).", "Empieza una partida entre medianoche y las 5 (desde Repisa).",
  "Lancez une partie entre minuit et 5 h (depuis Repisa).", "Starte eine Sitzung zwischen Mitternacht und 5 Uhr (über Repisa).",
  "Inizia una partita tra mezzanotte e le 5 (da Repisa).", "Comece uma partida entre meia-noite e as 5 (pelo Repisa).")
s("rach_morning", "Early bird", "Madrugador", "Lève-tôt", "Frühaufsteher", "Mattiniero", "Madrugador")
s("rach_morning_d", "Start a session between 5 and 9 am (from Repisa).", "Empieza una partida entre las 5 y las 9 de la mañana (desde Repisa).",
  "Lancez une partie entre 5 h et 9 h (depuis Repisa).", "Starte eine Sitzung zwischen 5 und 9 Uhr (über Repisa).",
  "Inizia una partita tra le 5 e le 9 (da Repisa).", "Comece uma partida entre as 5 e as 9 da manhã (pelo Repisa).")
s("rach_months_3", "Old friend", "Viejo conocido", "Vieil ami", "Alter Bekannter", "Vecchio amico", "Velho conhecido")
s("rach_weekdays", "All week long", "Toda la semana", "Toute la semaine", "Die ganze Woche", "Tutta la settimana", "A semana toda")
s("rach_weekdays_d", "Play it on every day of the week (Monday to Sunday, any week).", "Juégalo en cada día de la semana (de lunes a domingo, en cualquier semana).",
  "Jouez-y chaque jour de la semaine (du lundi au dimanche, n'importe quelle semaine).", "Spiele es an jedem Wochentag (Montag bis Sonntag, beliebige Woche).",
  "Giocaci ogni giorno della settimana (da lunedì a domenica, qualsiasi settimana).", "Jogue em todos os dias da semana (de segunda a domingo, em qualquer semana).")
s("rach_comeback", "The comeback", "El regreso", "Le retour", "Die Rückkehr", "Il ritorno", "O regresso")
s("rach_comeback_d", "Come back to it after 30 days or more.", "Vuelve a jugarlo tras 30 días o más sin tocarlo.", "Reprenez-le après 30 jours ou plus.",
  "Spiele es nach 30 Tagen oder mehr wieder.", "Riprendilo dopo 30 giorni o più.", "Volte a jogar após 30 dias ou mais.")
s("rach_hltb_main", "The story", "La historia", "L'histoire", "Die Story", "La storia", "A história")
s("rach_hltb_main_d", "Play it as long as its main story lasts (HowLongToBeat; 10 h if unknown).", "Juégalo lo que dura su historia (HowLongToBeat; 10 h si no se sabe).",
  "Jouez-y la durée de l'histoire (HowLongToBeat ; 10 h si inconnu).", "Spiele es so lange wie die Story (HowLongToBeat; 10 Std., wenn unbekannt).",
  "Giocaci quanto dura la storia (HowLongToBeat; 10 h se non si sa).", "Jogue o quanto dura a história (HowLongToBeat; 10 h se não se souber).")
s("rach_hltb_extra", "Explorer", "Explorador", "Explorateur", "Entdecker", "Esploratore", "Explorador")
s("rach_hltb_extra_d", "Play it as long as story + extras last (HowLongToBeat; 20 h if unknown).", "Juégalo lo que duran historia y extras (HowLongToBeat; 20 h si no se sabe).",
  "Jouez-y la durée histoire + extras (HowLongToBeat ; 20 h si inconnu).", "Spiele es so lange wie Story + Extras (HowLongToBeat; 20 Std., wenn unbekannt).",
  "Giocaci quanto durano storia ed extra (HowLongToBeat; 20 h se non si sa).", "Jogue o quanto duram história e extras (HowLongToBeat; 20 h se não se souber).")
s("rach_critic", "Critic", "Crítico", "Critique", "Kritiker", "Critico", "Crítico")
s("rach_critic_d", "Rate it with 4 or more criteria.", "Califícalo con 4 criterios o más.", "Notez-le avec 4 critères ou plus.", "Bewerte es mit 4 oder mehr Kriterien.",
  "Valutalo con 4 o più criteri.", "Avalie com 4 critérios ou mais.")
s("rach_diary", "Game diary", "Diario de juego", "Journal de jeu", "Spieltagebuch", "Diario di gioco", "Diário de jogo")
s("rach_diary_d", "Write notes of 300 characters or more.", "Escribe notas de 300 caracteres o más.", "Écrivez des notes de 300 caractères ou plus.",
  "Schreibe Notizen mit 300 oder mehr Zeichen.", "Scrivi note di 300 caratteri o più.", "Escreva notas de 300 caracteres ou mais.")
s("rach_session_4h", "Marathon", "Maratón", "Marathon", "Marathon", "Maratona", "Maratona")
s("rach_session_8h", "No sleep", "Sin dormir", "Nuit blanche", "Ohne Schlaf", "Senza dormire", "Sem dormir")
s("rach_day_6h", "All-day gaming", "Día entero", "Journée entière", "Ganzer Tag", "Giornata intera", "Dia inteiro")
s("rach_sessions_50", "Routine", "Rutina", "Routine", "Routine", "Routine", "Rotina")
s("rach_sessions_100", "One hundred sessions", "Cien partidas", "Cent parties", "Hundert Sitzungen", "Cento partite", "Cem partidas")
s("rach_days_30", "A month of play", "Un mes de juego", "Un mois de jeu", "Ein Monat Spielzeit", "Un mese di gioco", "Um mês de jogo")
s("rach_streak_7", "Seven in a row", "Siete seguidos", "Sept d'affilée", "Sieben am Stück", "Sette di fila", "Sete seguidos")
s("rach_streak_30", "Never miss a day", "Un mes sin fallar", "Sans faute pendant un mois", "Einen Monat ohne Pause", "Un mese senza saltare", "Um mês sem falhar")
s("rach_months_6", "Half a year", "Medio año", "Six mois", "Ein halbes Jahr", "Mezzo anno", "Meio ano")
s("rach_months_12", "All year round", "Todo el año", "Toute l'année", "Das ganze Jahr", "Tutto l'anno", "O ano todo")
s("rach_seasons", "Four seasons", "Cuatro estaciones", "Quatre saisons", "Vier Jahreszeiten", "Quattro stagioni", "Quatro estações")
s("rach_seasons_d", "Play it in spring, summer, autumn and winter.", "Juégalo en primavera, verano, otoño e invierno.", "Jouez-y au printemps, en été, en automne et en hiver.",
  "Spiele es im Frühling, Sommer, Herbst und Winter.", "Giocaci in primavera, estate, autunno e inverno.", "Jogue na primavera, verão, outono e inverno.")
s("rach_midnight", "Midnight chimes", "Doce campanadas", "Les douze coups", "Mitternacht", "Mezzanotte", "Doze badaladas")
s("rach_midnight_d", "A session that goes past midnight (from Repisa).", "Una partida que pase la medianoche (desde Repisa).",
  "Une partie qui dépasse minuit (depuis Repisa).", "Eine Sitzung über Mitternacht hinaus (über Repisa).", "Una partita oltre la mezzanotte (da Repisa).",
  "Uma partida que passe da meia-noite (pelo Repisa).")
s("rach_new_year", "New Year", "Año nuevo", "Nouvel an", "Neujahr", "Capodanno", "Ano novo")
s("rach_new_year_d", "Play it on January 1st.", "Juégalo un 1 de enero.", "Jouez-y un 1er janvier.", "Spiele es an einem 1. Januar.", "Giocaci il 1° gennaio.",
  "Jogue num 1º de janeiro.", ptPT="Jogue num 1 de janeiro.")
s("rach_anniversary", "Anniversary", "Aniversario", "Anniversaire", "Jahrestag", "Anniversario", "Aniversário")
s("rach_anniversary_d", "Play it a year (or more) after your first recorded session.", "Juégalo un año (o más) después de tu primera partida anotada.",
  "Jouez-y un an (ou plus) après votre première partie enregistrée.", "Spiele es ein Jahr (oder mehr) nach deiner ersten erfassten Sitzung.",
  "Giocaci un anno (o più) dopo la prima partita registrata.", "Jogue um ano (ou mais) depois da sua primeira partida registrada.")
s("rach_hltb_complete", "Completionist", "Completista", "Complétionniste", "Komplettist", "Completista", "Completista")
s("rach_hltb_complete_d", "Play it as long as a 100 % run takes (HowLongToBeat; 40 h if unknown).", "Juégalo lo que dura completarlo al 100 % (HowLongToBeat; 40 h si no se sabe).",
  "Jouez-y la durée d'un 100 % (HowLongToBeat ; 40 h si inconnu).", "Spiele es so lange wie ein 100-%-Durchgang (HowLongToBeat; 40 Std., wenn unbekannt).",
  "Giocaci quanto serve per il 100 % (HowLongToBeat; 40 h se non si sa).", "Jogue o quanto leva para fazer 100 % (HowLongToBeat; 40 h se não se souber).")
s("rach_hltb_double", "No rush", "Sin prisa", "Sans se presser", "Keine Eile", "Senza fretta", "Sem pressa")
s("rach_hltb_double_d", "Play it twice as long as a 100 % run (HowLongToBeat; 80 h if unknown).", "Juégalo el doble de lo que dura completarlo al 100 % (HowLongToBeat; 80 h si no se sabe).",
  "Jouez-y deux fois la durée d'un 100 % (HowLongToBeat ; 80 h si inconnu).", "Spiele es doppelt so lange wie ein 100-%-Durchgang (HowLongToBeat; 80 Std., wenn unbekannt).",
  "Giocaci il doppio del 100 % (HowLongToBeat; 80 h se non si sa).", "Jogue o dobro do tempo de 100 % (HowLongToBeat; 80 h se não se souber).")
s("rach_speedrun", "Speedrunner", "Rápido", "Rapide", "Speedrunner", "Velocista", "Rápido")
s("rach_speedrun_d", "Finish it in less time than its main story (HowLongToBeat).", "Termínalo jugando menos de lo que dura su historia (HowLongToBeat).",
  "Terminez-le en moins de temps que son histoire (HowLongToBeat).", "Beende es in weniger Zeit als die Story (HowLongToBeat).",
  "Finiscilo in meno tempo della storia (HowLongToBeat).", "Termine jogando menos do que dura a história (HowLongToBeat).")
s("rach_hours_50", "50 hours", "50 horas", "50 heures", "50 Stunden", "50 ore", "50 horas")
s("rach_hours_100", "100 hours", "100 horas", "100 heures", "100 Stunden", "100 ore", "100 horas")
s("rach_hours_250", "250 hours", "250 horas", "250 heures", "250 Stunden", "250 ore", "250 horas")
s("rach_hours_500", "500 hours", "500 horas", "500 heures", "500 Stunden", "500 ore", "500 horas")
s("rach_masterpiece", "Masterpiece", "Obra maestra", "Chef-d'œuvre", "Meisterwerk", "Capolavoro", "Obra-prima")
s("rach_masterpiece_d", "Give it a rating of 95 % or more.", "Dale una calificación de 95 % o más.", "Donnez-lui une note de 95 % ou plus.",
  "Bewerte es mit 95 % oder mehr.", "Dagli un voto del 95 % o più.", "Dê uma nota de 95 % ou mais.")
s("rach_hunter", "Trophy hunter", "Cazatrofeos", "Chasseur de trophées", "Trophäenjäger", "Cacciatore di trofei", "Caçador de troféus")
s("rach_hunter_d", "Unlock all of its own achievements (Steam, RetroAchievements…).", "Consigue todos sus logros propios (Steam, RetroAchievements…).",
  "Débloquez tous ses succès (Steam, RetroAchievements…).", "Schalte alle eigenen Erfolge frei (Steam, RetroAchievements …).",
  "Sblocca tutti i suoi obiettivi (Steam, RetroAchievements…).", "Desbloqueie todas as conquistas dele (Steam, RetroAchievements…).")

# Logros de la app
s("ad_games", "Have %1$d games in Repisa.", "Ten %1$d juegos en Repisa.", "Ayez %1$d jeux dans Repisa.", "Habe %1$d Spiele in Repisa.", "Avere %1$d giochi in Repisa.",
  "Tenha %1$d jogos no Repisa.")
s("ad_hours", "Add up %1$d hours across all your games.", "Suma %1$d horas entre todos tus juegos.", "Cumulez %1$d heures sur tous vos jeux.",
  "Sammle %1$d Stunden über alle Spiele.", "Somma %1$d ore tra tutti i tuoi giochi.", "Some %1$d horas entre todos os seus jogos.")
s("ad_finished", "Finished games: %1$d.", "Juegos terminados: %1$d.", "Jeux terminés : %1$d.", "Beendete Spiele: %1$d.", "Giochi finiti: %1$d.", "Jogos terminados: %1$d.")
s("ad_completed", "Games at 100 %%: %1$d.", "Juegos al 100 %%: %1$d.", "Jeux à 100 %% : %1$d.", "Spiele auf 100 %%: %1$d.", "Giochi al 100 %%: %1$d.",
  "Jogos em 100 %%: %1$d.")
s("ad_platinum", "Platinums: %1$d.", "Platinos: %1$d.", "Platines : %1$d.", "Platin: %1$d.", "Platini: %1$d.", "Platinas: %1$d.")
s("ad_rated", "Rated games: %1$d.", "Juegos calificados: %1$d.", "Jeux notés : %1$d.", "Bewertete Spiele: %1$d.", "Giochi valutati: %1$d.", "Jogos avaliados: %1$d.")
s("ad_systems", "Play on %1$d different systems.", "Juega en %1$d sistemas distintos.", "Jouez sur %1$d systèmes différents.", "Spiele auf %1$d verschiedenen Systemen.",
  "Gioca su %1$d sistemi diversi.", "Jogue em %1$d sistemas diferentes.")
s("ad_streak", "Play something %1$d days in a row.", "Juega a lo que sea %1$d días seguidos.", "Jouez à n'importe quoi %1$d jours d'affilée.",
  "Spiele %1$d Tage am Stück, egal was.", "Gioca a qualcosa %1$d giorni di fila.", "Jogue qualquer coisa %1$d dias seguidos.")
s("ad_sessions", "Recorded sessions: %1$d.", "Partidas anotadas: %1$d.", "Parties enregistrées : %1$d.", "Erfasste Sitzungen: %1$d.", "Partite registrate: %1$d.",
  "Partidas registradas: %1$d.")
s("ad_collections", "Collections created: %1$d.", "Colecciones creadas: %1$d.", "Collections créées : %1$d.", "Erstellte Sammlungen: %1$d.",
  "Collezioni create: %1$d.", "Coleções criadas: %1$d.")
s("ad_ach", "Steam / RetroAchievements achievements unlocked: %1$d.", "Logros de Steam o RetroAchievements conseguidos: %1$d.",
  "Succès Steam / RetroAchievements débloqués : %1$d.", "Freigeschaltete Steam-/RetroAchievements-Erfolge: %1$d.",
  "Obiettivi Steam / RetroAchievements sbloccati: %1$d.", "Conquistas Steam / RetroAchievements desbloqueadas: %1$d.")
s("ad_perfect", "Games with all their achievements: %1$d.", "Juegos con todos sus logros: %1$d.", "Jeux avec tous leurs succès : %1$d.",
  "Spiele mit allen Erfolgen: %1$d.", "Giochi con tutti gli obiettivi: %1$d.", "Jogos com todas as conquistas: %1$d.")
s("aach_lib_10", "Getting started", "Primeras cajas", "Premières boîtes", "Erste Hüllen", "Prime scatole", "Primeiras caixas")
s("aach_lib_50", "A proper shelf", "Una buena repisa", "Une belle étagère", "Ein ordentliches Regal", "Uno scaffale vero", "Uma boa estante")
s("aach_lib_100", "Collector", "Coleccionista", "Collectionneur", "Sammler", "Collezionista", "Colecionador")
s("aach_lib_250", "Museum", "Museo", "Musée", "Museum", "Museo", "Museu")
s("aach_lib_500", "Living library", "Biblioteca viviente", "Bibliothèque vivante", "Lebende Bibliothek", "Biblioteca vivente", "Biblioteca viva")
s("aach_tot_100", "100 hours on the clock", "100 horas en el contador", "100 heures au compteur", "100 Stunden auf der Uhr", "100 ore sul contatore",
  "100 horas no contador")
s("aach_tot_500", "Veteran", "Veterano", "Vétéran", "Veteran", "Veterano", "Veterano")
s("aach_tot_1000", "A thousand hours", "Mil horas", "Mille heures", "Tausend Stunden", "Mille ore", "Mil horas")
s("aach_tot_2500", "Legend", "Leyenda", "Légende", "Legende", "Leggenda", "Lenda")
s("aach_fin_1", "Roll the credits", "Créditos", "Générique", "Abspann", "Titoli di coda", "Créditos")
s("aach_fin_10", "Ten endings", "Diez finales", "Dix fins", "Zehn Enden", "Dieci finali", "Dez finais")
s("aach_fin_25", "Closer", "Rematador", "Finisseur", "Abschließer", "Finalizzatore", "Finalizador")
s("aach_fin_50", "Fifty adventures", "Cincuenta aventuras", "Cinquante aventures", "Fünfzig Abenteuer", "Cinquanta avventure", "Cinquenta aventuras")
s("aach_fin_100", "Living history", "Historia viva", "Histoire vivante", "Lebende Geschichte", "Storia vivente", "História viva")
s("aach_cmp_1", "One hundred percent", "Cien por cien", "Cent pour cent", "Hundert Prozent", "Cento per cento", "Cem por cento")
s("aach_cmp_10", "Perfectionist", "Perfeccionista", "Perfectionniste", "Perfektionist", "Perfezionista", "Perfeccionista")
s("aach_cmp_25", "Nothing left", "Nada pendiente", "Plus rien à faire", "Nichts mehr offen", "Niente in sospeso", "Nada pendente")
s("aach_plat_1", "First platinum", "Primer platino", "Premier platine", "Erstes Platin", "Primo platino", "Primeira platina")
s("aach_plat_5", "Platinum collector", "Coleccionista de platinos", "Collectionneur de platines", "Platin-Sammler", "Collezionista di platini",
  "Colecionador de platinas")
s("aach_plat_10", "Platinum crown", "Corona de platino", "Couronne de platine", "Platinkrone", "Corona di platino", "Coroa de platina")
s("aach_plat_25", "Platinum legend", "Leyenda de platino", "Légende de platine", "Platin-Legende", "Leggenda di platino", "Lenda de platina")
s("aach_rate_1", "First opinion", "Primera opinión", "Premier avis", "Erste Meinung", "Prima opinione", "Primeira opinião")
s("aach_rate_10", "Reviewer", "Reseñador", "Critique", "Rezensent", "Recensore", "Resenhista", ptPT="Crítico")
s("aach_rate_50", "Professor", "Catedrático", "Professeur", "Professor", "Professore", "Catedrático")
s("aach_sys_3", "Multiplatform", "Multiplataforma", "Multiplateforme", "Multiplattform", "Multipiattaforma", "Multiplataforma")
s("aach_sys_10", "Console hopper", "Saltaconsolas", "Zappeur de consoles", "Konsolenhüpfer", "Saltaconsole", "Saltador de consolas")
s("aach_sys_20", "Time traveller", "Viajero del tiempo", "Voyageur du temps", "Zeitreisender", "Viaggiatore nel tempo", "Viajante do tempo")
s("aach_streak_7", "Gaming week", "Semana jugona", "Semaine de jeu", "Spielwoche", "Settimana di gioco", "Semana de jogo")
s("aach_streak_30", "Unstoppable", "Imparable", "Inarrêtable", "Unaufhaltsam", "Inarrestabile", "Imparável")
s("aach_streak_100", "One hundred days", "Cien días", "Cent jours", "Hundert Tage", "Cento giorni", "Cem dias")
s("aach_ses_10", "Taking notes", "Tomando nota", "Prise de notes", "Mitgeschrieben", "Prendo nota", "Anotando", ptPT="A tomar nota")
s("aach_ses_100", "Logbook", "Bitácora", "Journal de bord", "Logbuch", "Diario di bordo", "Diário de bordo")
s("aach_ses_500", "Chronicler", "Cronista", "Chroniqueur", "Chronist", "Cronista", "Cronista")
s("aach_coll_1", "Organised", "Ordenado", "Organisé", "Ordentlich", "Ordinato", "Organizado")
s("aach_coll_5", "Archivist", "Archivero", "Archiviste", "Archivar", "Archivista", "Arquivista")
s("aach_ach_100", "Hunter", "Cazador", "Chasseur", "Jäger", "Cacciatore", "Caçador")
s("aach_ach_1000", "Great hunter", "Gran cazador", "Grand chasseur", "Großer Jäger", "Grande cacciatore", "Grande caçador")
s("aach_ach_5000", "Hall of fame", "Salón de la fama", "Panthéon", "Ruhmeshalle", "Hall of fame", "Hall da fama")
s("aach_perf_1", "Perfect game", "Juego perfecto", "Jeu parfait", "Perfektes Spiel", "Gioco perfetto", "Jogo perfeito")
s("aach_perf_10", "Ten perfect games", "Diez perfectos", "Dix parfaits", "Zehn perfekte", "Dieci perfetti", "Dez perfeitos")
s("aach_abandon", "Life's too short", "La vida es corta", "La vie est courte", "Das Leben ist kurz", "La vita è breve", "A vida é curta")
s("aach_abandon_d", "Mark a game as abandoned.", "Marca un juego como abandonado.", "Marquez un jeu comme abandonné.", "Markiere ein Spiel als abgebrochen.",
  "Segna un gioco come abbandonato.", "Marque um jogo como abandonado.")
s("aach_theme", "Redecorating", "Redecorando", "Nouvelle déco", "Neu eingerichtet", "Nuovo arredamento", "Redecorando", ptPT="A redecorar")
s("aach_theme_d", "Use a cabinet theme other than walnut.", "Usa un tema de vitrina distinto del nogal.", "Utilisez un thème de vitrine autre que le noyer.",
  "Nutze ein anderes Vitrinen-Thema als Nussbaum.", "Usa un tema della vetrina diverso dal noce.", "Use um tema de estante diferente da nogueira.")
s("aach_custom_theme", "Interior designer", "Interiorista", "Décorateur", "Innenarchitekt", "Arredatore", "Decorador")
s("aach_custom_theme_d", "Use your own custom theme.", "Usa tu propio tema personalizado.", "Utilisez votre propre thème personnalisé.",
  "Nutze dein eigenes Thema.", "Usa il tuo tema personalizzato.", "Use o seu próprio tema personalizado.")
s("aach_wish_10", "Dreamer", "Soñador", "Rêveur", "Träumer", "Sognatore", "Sonhador")
s("aach_wish_10_d", "Have 10 games on your Steam wishlist.", "Ten 10 juegos en tu lista de deseos de Steam.", "Ayez 10 jeux dans votre liste de souhaits Steam.",
  "Habe 10 Spiele auf deiner Steam-Wunschliste.", "Avere 10 giochi nella lista dei desideri di Steam.", "Tenha 10 jogos na sua lista de desejos da Steam.")

s("consoles_title", "My consoles", "Mis consolas", "Mes consoles", "Meine Konsolen", "Le mie console", "Meus consoles", ptPT="As minhas consolas")
s("consoles_sub", "%1$d systems · %2$s · tap one to see its games", "%1$d sistemas · %2$s · toca una para ver sus juegos",
  "%1$d systèmes · %2$s · touchez-en une pour voir ses jeux", "%1$d Systeme · %2$s · tippe eine an, um ihre Spiele zu sehen",
  "%1$d sistemi · %2$s · tocca una per vedere i suoi giochi", "%1$d sistemas · %2$s · toque em um para ver os jogos",
  ptPT="%1$d sistemas · %2$s · toque numa para ver os jogos")
s("console_card", "%1$s · %2$s on this console", "%1$s · %2$s en esta consola", "%1$s · %2$s sur cette console", "%1$s · %2$s auf dieser Konsole",
  "%1$s · %2$s su questa console", "%1$s · %2$s neste console", ptPT="%1$s · %2$s nesta consola")

s("status_genres", "Checking genres (%1$d/%2$d)…", "Revisando géneros (%1$d/%2$d)…", "Vérification des genres (%1$d/%2$d)…",
  "Genres werden geprüft (%1$d/%2$d)…", "Controllo dei generi (%1$d/%2$d)…", "Revisando gêneros (%1$d/%2$d)…", ptPT="A rever géneros (%1$d/%2$d)…")
s("also_known_as", "Also known as: %1$s", "También conocido como: %1$s", "Aussi connu sous : %1$s", "Auch bekannt als: %1$s",
  "Noto anche come: %1$s", "Também conhecido como: %1$s")
s("genres_pick", "Tap to add or remove:", "Toca para añadir o quitar:", "Touchez pour ajouter ou retirer :",
  "Zum Hinzufügen oder Entfernen tippen:", "Tocca per aggiungere o togliere:", "Toque para adicionar ou remover:")

s("status_genres_steam", "Checking Steam genres…", "Revisando géneros de Steam…", "Vérification des genres Steam…",
  "Steam-Genres werden geprüft…", "Controllo dei generi di Steam…", "Revisando gêneros da Steam…", ptPT="A rever géneros da Steam…")
s("find_missing_genres", "Find missing genres", "Buscar géneros que faltan", "Chercher les genres manquants",
  "Fehlende Genres suchen", "Cerca generi mancanti", "Buscar gêneros que faltam", ptPT="Procurar géneros em falta")
s("genres_hint", "Genres come from Steam community tags (Steam and Epic) and from Wikidata (emulators). Games without one are searched again.",
  "Los géneros salen de las etiquetas de la comunidad de Steam (Steam y Epic) y de Wikidata (emuladores). Los juegos sin género se vuelven a buscar.",
  "Les genres viennent des tags de la communauté Steam (Steam et Epic) et de Wikidata (émulateurs). Les jeux sans genre sont recherchés à nouveau.",
  "Genres stammen aus den Steam-Community-Tags (Steam und Epic) und aus Wikidata (Emulatoren). Spiele ohne Genre werden erneut gesucht.",
  "I generi vengono dai tag della community di Steam (Steam ed Epic) e da Wikidata (emulatori). I giochi senza genere vengono cercati di nuovo.",
  "Os gêneros vêm das tags da comunidade Steam (Steam e Epic) e do Wikidata (emuladores). Jogos sem gênero são buscados de novo.",
  ptPT="Os géneros vêm das etiquetas da comunidade Steam (Steam e Epic) e do Wikidata (emuladores). Os jogos sem género são procurados de novo.")
s("msg_genres_found", "Genres found for %1$d of %2$d games without one.", "Géneros encontrados para %1$d de %2$d juegos sin género.",
  "Genres trouvés pour %1$d jeux sur %2$d sans genre.", "Genres für %1$d von %2$d Spielen ohne Genre gefunden.",
  "Generi trovati per %1$d di %2$d giochi senza genere.", "Gêneros encontrados para %1$d de %2$d jogos sem gênero.",
  ptPT="Géneros encontrados para %1$d de %2$d jogos sem género.")

s("msg_genres_retry", "%1$d couldn't be checked now (the service asked to wait): they'll be retried automatically.",
  "%1$d no se pudieron consultar ahora (el servicio pidió esperar): se reintentarán solos.",
  "%1$d n'ont pas pu être vérifiés (le service a demandé d'attendre) : nouvel essai automatique plus tard.",
  "%1$d konnten gerade nicht geprüft werden (der Dienst bat zu warten): sie werden automatisch erneut versucht.",
  "%1$d non sono stati controllati ora (il servizio ha chiesto di attendere): verranno ritentati da soli.",
  "%1$d não puderam ser consultados agora (o serviço pediu para esperar): serão tentados de novo sozinhos.",
  ptPT="%1$d não puderam ser consultados agora (o serviço pediu para esperar): serão tentados de novo automaticamente.")

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
    # 0.15.2: géneros a partir de las etiquetas de Steam y Wikidata (se añaden al final: el índice es el id del texto).
    ("FPS", "FPS", "FPS", "FPS", "Ego-Shooter", "FPS", "FPS"),
    ("Gacha", "Gacha", "Gacha", "Gacha", "Gacha", "Gacha", "Gacha"),
    ("Soulslike", "Soulslike", "Soulslike", "Soulslike", "Soulslike", "Soulslike", "Soulslike"),
    ("Hack and slash", "Hack and slash", "Hack and slash", "Hack'n'slash", "Hack and Slay", "Hack and slash", "Hack and slash"),
    ("Sigilo", "Stealth", "Sigilo", "Infiltration", "Stealth", "Stealth", "Furtividade"),
    ("Fantasía", "Fantasy", "Fantasía", "Fantasy", "Fantasy", "Fantasy", "Fantasia"),
    ("Ciencia ficción", "Sci-fi", "Ciencia ficción", "Science-fiction", "Science-Fiction", "Fantascienza", "Ficção científica"),
    ("Battle royale", "Battle royale", "Battle royale", "Battle royale", "Battle Royale", "Battle royale", "Battle royale"),
    ("Party", "Party", "Party", "Party game", "Partyspiel", "Party game", "Party"),
]
for i, (_, en, es, fr, de, it, pt) in enumerate(GENRES):
    s(f"genre_{i}", en, es, fr, de, it, pt)

PLURALS = {"games_count": ("games_count_one", "games_count_other")}


def esc(t: str) -> str:
    t = t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    t = t.replace("\\", "\\\\").replace("'", "\\'").replace('"', '\\"')
    t = t.replace("\n", "\\n")
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
