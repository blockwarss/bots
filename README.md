# BotTrainer (Paper 1.21.x)

Système d'entraînement PvP avec bots : modes **BASIC**, **MACE**, **CRYSTAL**. Difficultés **EASY → INSANE**. Arène dédiée, sélection d'équipement, inventaire/position restaurés.

## Build
- Java 21
- Paper API 1.21

```bash
./gradlew build
```
Le JAR apparaît dans `build/libs/BotTrainer-1.0.0.jar`.

## Commandes
- `/bot <joueur> <difficulte> <mode> <temps>`
  - Ex. `/bot Moi NORMAL MACE 2m`
- `/botarena setspawn` — définit le spawn de l'arène (monde courant)
- `/botarena setbounds <1|2>` — définit les coins de la zone (optionnel)
- `/botstop [joueur]` — termine la session et restaure l'inventaire

## Permissions
- `bottrainer.use` — utiliser `/bot`, `/botstop`
- `bottrainer.start.others` — lancer pour un autre joueur
- `bottrainer.admin` — config. arène
- `bottrainer.mode.(basic|mace|crystal)`
- `bottrainer.diff.(easy|normal|hard|insane)`

## Notes techniques
- Pas de dépendance NMS. Pas besoin de Citizens.
- Le bot est un **Vindicator** sans IA, déplacé par tick (vitesse/strafe selon difficulté).
- **Totem infini** simulé : annule la mort, son + particules + soin.
- Dégâts chute désactivables, explosions de crystal modulables.
- GUI simple pour loadout. Ajoute épée/masse/cristaux/obsidienne/arc/pommes.

## Idées d'amélioration
- Pages GUI avancées (enchants personnalisés, presets).
- Intégration Citizens2 complète (NPC joueur).
- Stats de sessions, scoreboard, hologramme de temps restant.
- Multi-arènes nommées, presets de scénarios.
