# Frontend - Plateforme de Reservation des Cabines (Medianet)

Angular 18 (standalone components) + Angular Material, conforme au cahier des charges.

## Etat d'avancement

- [x] **Phase 1** : setup projet, authentification OTP/JWT, liste des cabines (lecture seule)
- [x] **Phase 2** : recherche multicritere (nombre de participants, equipements, disponibilite/date)
- [x] **Phase 3** : reservations (creer avec dates, confirmer presence, historique, annuler)
- [x] **Phase 4** : recommandation IA (integree dans la page Cabines, scoring pondere V1 backend)
- [x] **Phase 5** : espace admin complet (CRUD cabines + maintenance, CRUD equipements, dashboard statistiques)

Toutes les phases du cahier des charges sont couvertes cote frontend.

## Corrections de conformite (suite a relecture du cahier des charges)

- L'administrateur n'a plus acces aux pages "Cabines" et "Mes reservations" (5.2 : il ne
  participe jamais au processus de reservation). Voir `employe.guard.ts`.
- La reservation se fait desormais sur **un seul jour** (une date + heure de debut + heure
  de fin), plus deux dates separees. Voir `cabine-list.component.ts`.
- Validation : la date ne peut pas etre anterieure a aujourd'hui, et l'heure de debut doit
  etre avant l'heure de fin. Voir `core/validators/reservation-validators.ts`.
- La confirmation de presence n'est activable qu'**a partir de l'heure de debut** de la
  reservation (6.8), pas n'importe quand a l'avance. Voir `peutConfirmerPresence()` dans
  `reservation-historique.component.ts`.
- Une reservation deja **CONFIRMEE** ne peut plus etre annulee (seule une reservation
  encore EN_ATTENTE peut l'etre). Voir `peutAnnuler()`.
- Quand le creneau (date/heure) selectionne est deja pris par une autre reservation, le
  bouton "Reserver" est masque et remplace par le message "La cabine est deja reservee
  sur ce creneau" (rafraichi automatiquement a chaque changement de date/heure, et en cas
  de conflit 409 detecte au moment meme de la reservation). Voir
  `rafraichirDisponibiliteCreneau()` dans `cabine-list.component.ts`.

## Ecarts connus restants vs le cahier des charges (non traites dans cette iteration)

- **6.1.2** : le JWT est stocke en `sessionStorage` cote frontend, alors que le cahier
  precise un cookie `HttpOnly`/`Secure`. Necessite d'adapter le backend pour poser le
  cookie lui-meme (et gerer CSRF en consequence).
- **6.7.2** : la ponderation exacte du scoring (capacite 40%, equipements 30%,
  localisation 15%, historique d'utilisation 15%) n'est pas encore implementee cote
  backend (`RecommendationService.scoringPondereV1` actuel : 50% capacite / 50% equipements).

## Installation

```bash
npm install
```

Bootstrap 5 et Bootstrap Icons sont inclus (utilises pour la mise en page/utilitaires CSS,
en complement d'Angular Material pour les composants). Le CSS et le JS de Bootstrap sont
deja references dans `angular.json` (`styles` et `scripts`), aucune configuration
supplementaire n'est necessaire apres `npm install`.

## Configuration du backend

Adapte l'URL de l'API dans `src/environments/environment.development.ts` (et `environment.ts`
pour la prod) si ton backend Spring Boot ne tourne pas sur `http://localhost:8081` :

```ts
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8081/api'
};
```

**Important cote backend** : verifie que `app.cors.allowed-origins` dans le
`application.properties` du backend inclut bien l'origine du frontend Angular
(par defaut `http://localhost:4200`, port du `ng serve`).

## Lancement en developpement

```bash
npm start
```

Ouvre ensuite http://localhost:4200 — tu dois arriver sur le formulaire de connexion
(email puis code OTP), et apres connexion, la liste des cabines.

## Nouvelles fonctionnalites (evolution du cahier des charges initial)

- **L'administrateur peut desormais reserver des cabines** comme un employe (acces a
  "Cabines" et "Mes reservations"). Ceci revient sur la regle initiale du cahier des
  charges (5.2 : "l'administrateur ne participe jamais au processus de reservation"),
  suite a une demande explicite d'evolution.
- **Horaires de disponibilite par cabine** (ex: Cabine A disponible de 9h a 16h) :
  champ configurable par l'admin (`heureDisponibiliteDebut`/`heureDisponibiliteFin`),
  controle cote backend (`ReservationService.reserver`) et cote frontend (formulaire
  de reservation, page de detail cabine).
- **Photo par cabine** : URL d'image configurable par l'admin, affichee dans la liste,
  le detail et le tableau admin des cabines.
- **Page de detail cabine** (`/cabines/:id`) : en cliquant sur une cabine dans la liste,
  l'employe (ou l'admin) voit tous les details (photo, capacite, localisation,
  equipements, horaires) et reserve directement depuis cette page.
- **Page Admin "Disponibilite"** (`/admin/disponibilite`) : selection d'une cabine,
  affichage de ses details/photo, calendrier des reservations groupees par jour pour
  cette cabine, et tableau listant TOUTES les reservations (toutes cabines, tous
  employes confondus).
- **Annulation par l'administrateur** : l'admin peut annuler n'importe quelle
  reservation (pas seulement les siennes), depuis la page Disponibilite.
- **Grille de disponibilite** (`/admin/disponibilite`) : refonte en tableau cabines (lignes)
  x jours (colonnes), inspiree des logiciels de gestion hoteliere. Chaque case est coloree
  (vert = disponible, orange = partiellement reservee, rouge = complete, gris = maintenance)
  et cliquable pour ouvrir le detail des reservations de ce jour precis (heure de debut/fin,
  employe, statut), avec possibilite d'annuler directement depuis ce detail. Navigation par
  periode de 14 jours (boutons precedent/suivant/aujourd'hui).
- **Annulation limitee au futur** : une reservation deja passee (heure de debut atteinte)
  ne peut plus etre annulee, ni par l'employe, ni par l'administrateur (regle appliquee
  cote backend ET frontend, defense en profondeur).

## Architecture

```
src/app/
├── core/
│   ├── models/         # Interfaces TS alignees sur les DTOs backend
│   ├── services/       # Appels HTTP vers l'API Spring Boot
│   ├── interceptors/    # Injection JWT + gestion des erreurs 401
│   ├── guards/          # Protection des routes (auth / admin)
│   └── validators/      # Validateurs de formulaire reutilisables (dates/heures)
├── features/
│   ├── auth/login/                  # Connexion en 2 etapes (email -> OTP)
│   ├── cabines/
│   │   ├── cabine-list/              # Liste + recherche multicritere + recommandation IA
│   │   └── cabine-detail/            # Detail complet d'une cabine + reservation dediee
│   ├── reservations/reservation-historique/  # Historique employe/admin
│   └── admin/
│       ├── admin-cabines/            # CRUD cabines (photo, horaires, equipements)
│       ├── admin-equipements/        # CRUD equipements
│       ├── admin-disponibilite/      # Calendrier par cabine + liste globale + annulation
│       └── admin-dashboard/          # Statistiques globales
└── shared/
    └── navbar/            # Barre de navigation (menu selon le role)
```

## Notes de securite

Le token JWT est stocke en `sessionStorage` (efface a la fermeture de l'onglet),
pas en `localStorage`, pour limiter la fenetre d'exposition en cas de faille XSS.
Voir la discussion correspondante cote backend (README du backend) pour l'alternative
cookie `HttpOnly` si une securite plus forte est necessaire plus tard.
