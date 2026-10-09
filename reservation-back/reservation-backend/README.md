# Backend - Plateforme de Réservation des Cabines de Travail (Medianet)

Backend Spring Boot conforme au cahier des charges et aux diagrammes UML fournis
(cas d'utilisation, classes, séquence, architecture logique/physique).

## Stack

- Spring Boot 3.3 / Java 17
- Spring Security + JWT (jjwt) — authentification passwordless (OTP)
- Spring Data JPA + **PostgreSQL**
- Spring Mail (envoi des OTP)
- WebFlux WebClient (appel du futur microservice IA FastAPI)

## Réponses aux points soulevés avant développement

### 1. PostgreSQL au lieu de MySQL
Fait — `pom.xml` utilise `org.postgresql:postgresql`, et `application.yml` pointe vers
`jdbc:postgresql://localhost:5432/medianet_reservation` avec le dialect Hibernate correspondant.

### 2. Concurrence : empêcher le double-booking d'une cabine
Deux mécanismes complémentaires :

- **Verrou pessimiste** : `CabineRepository.findByIdForUpdate()` exécute un
  `SELECT ... FOR UPDATE` sur la ligne de la cabine. `ReservationService.reserver()`
  l'utilise dans une transaction (`@Transactional`) : la première requête qui arrive
  verrouille la ligne jusqu'au commit/rollback ; toute requête concurrente sur la
  même cabine attend, ce qui élimine la fenêtre "vérifier disponibilité puis créer"
  où deux réservations pourraient passer en même temps.
- **Recommandation complémentaire (défense en profondeur)** : ajouter en base une
  contrainte d'exclusion PostgreSQL (extension `btree_gist`) sur
  `(cabine_id, tsrange(date_debut, date_fin))` empêcherait à l'échelle de la base
  elle-même deux réservations chevauchantes non annulées, même en cas de bug
  applicatif futur. Non ajoutée automatiquement ici pour rester compatible avec
  `ddl-auto: update` (qui ne gère pas les contraintes d'exclusion) ; à ajouter via
  une migration Flyway/Liquibase dès que l'équipe les intègre.

### 3. Sécurité OTP
- **Stockage** : le code OTP n'est **jamais stocké en clair**. Seul son hash BCrypt
  (`Otp.codeHash`, via `PasswordEncoder`) est persisté, comme un mot de passe.
- **Blocage** : après `app.otp.max-attempts` (5 par défaut) tentatives erronées sur
  un même OTP, le compte utilisateur (`Utilisateur.bloqueJusqua`) est bloqué pendant
  `app.otp.lock-minutes` (15 min). Le blocage est vérifié à la fois à la demande d'un
  nouvel OTP et à la vérification.
- **JWT** : expiration configurable (`app.jwt.expiration-ms`, 24h par défaut). Pas de
  refresh token pour l'instant (à ajouter si besoin de sessions plus longues).
  **Côté client (Angular)** : le token doit être conservé en mémoire (variable de
  service Angular) plutôt qu'en `localStorage`/`sessionStorage`, pour limiter le risque
  de vol via XSS ; à minima éviter le `localStorage`. Une alternative plus robuste
  serait un cookie `HttpOnly` + `Secure` + `SameSite=Strict` posé par le backend,
  mais cela suppose de revoir le flux CORS/Angular en conséquence — à trancher avec
  l'équipe frontend.

### 4. Données personnelles / durée de conservation
- **OTP** : purge automatique quotidienne (`PurgeOtpScheduler`, 3h du matin) des OTP
  expirés depuis plus de `app.otp.retention-hours` (24h par défaut). Aucune raison de
  les conserver au-delà de leur fenêtre de validité + une courte marge.
- **Historique des réservations** : conservé actuellement sans limite. Un paramètre
  `app.data-retention.historique-reservations-mois` (24 mois par défaut) est prévu
  dans la configuration ; **à confirmer avec le DPO / la direction Medianet** avant
  d'implémenter l'anonymisation/suppression automatique (impact sur les statistiques
  admin à long terme).
- **Logs applicatifs** : aucune politique de rétention n'est définie dans ce
  cahier des charges ; à définir également (ex. 30-90 jours selon les besoins de
  debug vs. minimisation des données).

### 5. Modèle IA : reformulé en moteur de scoring pondéré (V1)
Un Random Forest a besoin de données d'entraînement (historique de réservations,
taux d'acceptation/annulation...) qui n'existent pas encore au lancement du projet.
`RecommendationService` implémente donc un **moteur de scoring pondéré déterministe
et explicable** :
- score = 50% adéquation de la capacité + 50% taux de couverture des équipements
  souhaités, calculé uniquement sur les cabines disponibles sur le créneau demandé ;
- retourne le même contrat de sortie que prévu en 6.7.2 (cabine, score, justification).

Le microservice FastAPI + Random Forest reste prévu dans l'architecture (voir
`RecommendationClient`) et pourra être activé via `app.ia.activer-microservice=true`
une fois suffisamment de données réelles collectées pour l'entraîner, sans changer
le contrat d'API exposé au frontend Angular.

## Démarrage local

```bash
# Prérequis : PostgreSQL démarré, base "medianet_reservation" créée
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export MAIL_USERNAME=... 
export MAIL_PASSWORD=...
mvn spring-boot:run
```

Configuration dans `src/main/resources/application.properties` (toutes les valeurs
sont surchargeables via variables d'environnement, cf. fichier).

## Endpoints principaux

| Méthode | URL | Rôle | Description |
|---|---|---|---|
| POST | /api/auth/login | public | Demande d'un OTP |
| POST | /api/auth/verify-otp | public | Vérification OTP -> JWT |
| GET | /api/cabines | authentifié | Liste des cabines |
| POST | /api/cabines/recherche | authentifié | Recherche multicritère |
| POST | /api/reservations | authentifié | Réservation automatique |
| PATCH | /api/reservations/{id}/confirmer-presence | authentifié | Confirmation de présence |
| PATCH | /api/reservations/{id}/annuler | authentifié | Annulation |
| GET | /api/reservations/historique | authentifié | Historique employé |
| GET | /api/recommandations | authentifié | Recommandation intelligente |
| POST/PUT/DELETE | /api/admin/cabines/** | ADMINISTRATEUR | Gestion des cabines |
| POST/PUT/DELETE | /api/admin/equipements/** | ADMINISTRATEUR | Gestion des équipements |
| GET | /api/admin/dashboard | ADMINISTRATEUR | Statistiques globales |

## Points restant à faire (hors périmètre de cette itération)

- **Nouvelles colonnes sur `cabines`** (`heure_disponibilite_debut`, `heure_disponibilite_fin`,
  `photo_url`) : avec `ddl-auto=update`, Hibernate les ajoute automatiquement au redémarrage,
  mais les cabines existantes auront `heure_disponibilite_debut`/`fin` à `NULL` tant qu'elles
  n'auront pas été ré-enregistrées via le formulaire admin (les valeurs par défaut Java
  `09:00`/`18:00` ne s'appliquent qu'aux nouvelles cabines créées après ce changement).
  Pense à repasser sur chaque cabine existante via `/admin/cabines` pour renseigner ses horaires.

- Migrations Flyway/Liquibase (actuellement `ddl-auto: update`, à proscrire en prod)
- Contrainte d'exclusion PostgreSQL en défense en profondeur (voir point 2)
- Tests unitaires/intégration (JUnit + Testcontainers PostgreSQL recommandé)
- Politique de rétention validée par le DPO pour l'historique des réservations et les logs
- Endpoints de gestion des utilisateurs par l'administrateur (6.2/5.2 — à ajouter)
