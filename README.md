# BotTrainer (Paper 1.21.x)

Système d'entraînement PvP avec bots : modes **BASIC**, **MACE**, **CRYSTAL**. Difficultés **EASY → INSANE**. Arène dédiée, sélection d'équipement, inventaire/position restaurés.

## Build (CI)
- Commit/push → GitHub Actions build → télécharge le JAR en artifact.

## Local (optionnel)
```bash
gradle wrapper --gradle-version 9.1.0
./gradlew build
```
