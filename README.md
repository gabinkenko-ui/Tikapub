# Tikapub

Application Android qui génère automatiquement de courtes vidéos (citation + fond animé +
musique optionnelle) et les publie chaque jour sur TikTok, via l'API officielle **TikTok Content
Posting API**, sur le compte [@gabinkenko](https://www.tiktok.com/@gabinkenko).

> ⚠️ Ce projet a été écrit dans un environnement sans accès au SDK Android ni au dépôt Maven de
> Google (`dl.google.com` / `maven.google.com` bloqués par la politique réseau du bac à sable), il
> n'a donc **pas pu être compilé ni exécuté ici**. Ouvre le projet dans Android Studio (Koala ou
> plus récent) pour la première compilation : c'est là que d'éventuels ajustements mineurs de
> signatures d'API (notamment Media3 Transformer, dont l'API évolue vite) devront être faits.

## Ce que fait l'app

- Génère une vidéo verticale (1080×1920) à partir d'une citation piochée en rotation dans une
  bibliothèque locale (Room), avec fond dégradé animé (effet Ken Burns), texte overlay lisible, et
  une piste audio optionnelle prise au hasard dans un dossier que tu choisis.
- Se connecte à ton compte TikTok via OAuth 2.0 + PKCE (TikTok Login Kit) et publie la vidéo via
  l'API officielle `Content Posting API` (upload direct par chunks + suivi du statut).
- Planifie une publication quotidienne à heure fixe via `WorkManager`, avec un bouton "Publier
  maintenant" pour déclencher une publication à la demande.
- Garde un historique des publications (succès/échecs) et permet de gérer les citations et
  catégories depuis l'app.

## Architecture

```
app/src/main/java/com/gabinkenko/tikapub/
├── data/
│   ├── db/          Room: QuoteEntity/Dao, PublishLogEntity/Dao, seed de citations FR
│   └── settings/     DataStore (préférences) + EncryptedSharedPreferences (secrets/tokens)
├── tiktok/           OAuth (PKCE), Retrofit API, upload par chunks, polling de statut
├── video/            Génération du fond, du texte overlay, effet Ken Burns, sélection musique,
│                     orchestration Media3 Transformer -> fichier MP4
├── worker/           WorkManager: planification quotidienne + worker de publication
└── ui/                Jetpack Compose (Accueil, Citations, Historique, Réglages)
```

Pas de framework de DI (Hilt, etc.) : un simple conteneur manuel (`AppContainer`) instancié dans
`TikapubApplication`.

## Prérequis : créer ton app sur le TikTok Developer Portal

**Cette étape doit être faite par toi** (elle nécessite ton propre compte TikTok/entreprise et une
vérification d'identité que je ne peux pas faire à ta place) :

1. Va sur https://developers.tiktok.com/ et connecte-toi avec ton compte TikTok.
2. Crée une app. Note sa **Client Key** et son **Client Secret**.
3. Dans l'onglet **Products**, ajoute le produit **Login Kit** et **Content Posting API**.
4. Configure le **Redirect URI** avec l'URL de la page pont GitHub Pages (voir section suivante) -
   TikTok exige une URI en HTTPS, il n'accepte pas un schéma personnalisé du type `tikapub://...`.
5. Demande les scopes `user.info.basic`, `video.publish`, `video.upload`.
6. Tant que l'app n'est pas **auditée/approuvée par TikTok** :
   - Elle tourne en mode **sandbox** : seuls les comptes TikTok que tu ajoutes explicitement comme
     "target users" dans le Developer Portal peuvent se connecter et publier (ajoute
     @gabinkenko).
   - Les publications directes en `PUBLIC_TO_EVERYONE` ne sont en général pas fiables/autorisées :
     utilise `SELF_ONLY` (réglage par défaut dans l'app) jusqu'à validation de l'audit TikTok.
   - Une fois l'app auditée, tu peux passer le niveau de confidentialité sur "Public" dans les
     Réglages de l'app.
7. Ce sont des contraintes imposées par TikTok, pas par ce code : voir
   https://developers.tiktok.com/doc/content-posting-api-get-started/ pour le détail à jour.

Renseigne ensuite la **Client Key** et le **Client Secret** dans l'onglet **Réglages** de l'app
(stockés chiffrés sur l'appareil via `EncryptedSharedPreferences`, jamais commités dans le repo).

## Configurer la page de redirection OAuth (GitHub Pages)

TikTok redirige vers une URL HTTPS après connexion ; cette URL doit immédiatement rebondir vers
l'app via un lien profond `tikapub://oauth-callback`. Le fichier `docs/index.html` de ce repo fait
exactement ça (aucune donnée n'y transite ni n'y est stockée, c'est un simple rebond côté
navigateur).

1. Sur GitHub : Settings → Pages → Source = `Deploy from a branch`, branche = celle-ci, dossier =
   `/docs`.
2. Récupère l'URL générée, par ex. `https://gabinkenko-ui.github.io/tikapub/`.
3. Renseigne cette URL :
   - comme **Redirect URI** dans le TikTok Developer Portal (étape précédente),
   - dans le champ **Redirect URI** des Réglages de l'app Tikapub.

## Compiler et lancer

1. Ouvre le dossier racine dans Android Studio (il régénérera le wrapper Gradle automatiquement à
   l'ouverture du projet).
2. Laisse Android Studio synchroniser Gradle (compileSdk 34, minSdk 26).
3. Lance sur un appareil/émulateur Android 8.0+.
4. Dans l'app : Réglages → renseigne Client Key/Secret/Redirect URI → "Se connecter à TikTok" →
   autorise dans le navigateur → tu reviens automatiquement dans l'app, connecté.
5. Ajoute éventuellement un dossier de musiques libres de droits, choisis tes catégories de
   citations, l'heure de publication quotidienne, puis active "Publication automatique" - ou teste
   directement avec "Publier maintenant".

## Musique de fond

L'app ne fournit aucun fichier audio (droits d'auteur). Dans Réglages, choisis un dossier contenant
tes propres fichiers `.mp3/.m4a/.wav` libres de droits (ex: une bibliothèque royalty-free type
YouTube Audio Library, Pixabay Music...) ; une piste est tirée au hasard et coupée à la durée du
clip à chaque génération. Sans dossier configuré, les vidéos sont générées sans musique.

## Citations

Le projet démarre avec ~35 citations FR (proverbes/domaine public, catégories motivation, sagesse,
humour, business) dans `SeedQuotes.kt`, gérables ensuite depuis l'onglet Citations de l'app
(ajout, désactivation, suppression, catégories). Le worker choisit la citation la moins récemment
utilisée pour éviter les répétitions.

## Limites connues / points d'attention

- **Audit TikTok** : sans app auditée, la publication publique automatique n'est pas garantie -
  c'est une contrainte de la plateforme, pas de ce code (voir section Prérequis).
- **Précision de l'horaire** : `WorkManager` planifie un travail périodique quotidien avec un délai
  initial calculé pour la première heure demandée ; Android peut décaler l'exécution de quelques
  minutes (Doze, contraintes réseau), ce n'est pas une horloge temps réel.
- **Ce dépôt n'a pas été compilé dans cet environnement** (accès réseau à Google Maven bloqué) :
  attends-toi à d'éventuelles petites corrections de signatures d'API lors de la première
  compilation dans Android Studio, en particulier dans `video/VideoComposerEngine.kt`
  (Media3 Transformer/Effect).
- **Usage personnel** : ce projet est pensé pour publier sur ton propre compte, avec du contenu que
  tu contrôles. Respecte les règles de la communauté TikTok (pas de spam, pas de contenu trompeur).
