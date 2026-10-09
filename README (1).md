# Medianet — Plateforme de réservation des cabines de travail

Projet de stage : réservation intelligente de cabines de travail (Spring Boot + Angular + PostgreSQL),
authentification passwordless (OTP + Google Sign-In), recommandation de cabine, chatbot.

## Structure du dépôt

```
backend/    API REST Spring Boot (Java 21, Spring Boot 3, PostgreSQL)
frontend/   Application Angular (standalone components)
```

## Configuration avant de lancer le backend

Le fichier `backend/src/main/resources/application.properties` lit plusieurs valeurs
sensibles depuis des **variables d'environnement** (jamais commitées) :

| Variable       | Description                                              |
|----------------|-----------------------------------------------------------|
| `DB_URL`       | URL JDBC PostgreSQL (défaut : `jdbc:postgresql://localhost:5432/medianet_reservation`) |
| `DB_USERNAME`  | Utilisateur PostgreSQL (défaut : `postgres`)              |
| `DB_PASSWORD`  | Mot de passe PostgreSQL                                   |
| `MAIL_USERNAME`| Adresse Gmail utilisée pour l'envoi des OTP                |
| `MAIL_PASSWORD`| Mot de passe d'application Gmail (pas le mot de passe du compte) |
| `JWT_SECRET`   | Clé secrète JWT en base64 (une valeur de dev par défaut existe, à changer en production) |

À définir avant de lancer l'application, par exemple dans IntelliJ (Run Configuration >
Environment variables) ou en ligne de commande :

```bash
export DB_PASSWORD=votre_mot_de_passe
export MAIL_USERNAME=votre_adresse@gmail.com
export MAIL_PASSWORD=votre_mot_de_passe_application_gmail
```

## Lancer le backend

```bash
cd backend
./mvnw spring-boot:run
```

## Lancer le frontend

```bash
cd frontend
npm install
npm start
```
