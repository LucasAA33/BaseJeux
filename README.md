# BaseJeux

BaseJeux is a **2D Java game engine** designed to create tile-based RPG games.

The engine provides a **map editor, JSON-driven object system, and gameplay framework** for building RPG-style games similar to tools like RPG Maker.

---

## Features

- 2D tile-based rendering
- Map editor for creating game worlds
- JSON-driven object system
- Environment objects (trees, houses, rocks)
- NPC system
- Item system
- Animation system
- Interaction system
- Support for **Action RPG gameplay**
- Planned support for **Turn-Based RPG gameplay**

---

## Project Architecture

```text
BaseJeux/
├── src/
│   └── basejeux/
│       ├── data/        # JSON loading and management system
│       ├── editor/      # Map editor tools
│       ├── gameplay/    # Game mechanics and logic systems
│       ├── model/       # Game entities and object definitions
│       └── event/       # Event management system
├── assets/
│   ├── data/            # JSON definitions (NPCs, items, maps...)
│   ├── images/          # Sprites and terrain textures
│   └── ui/              # Editor interface icons
├── lib/                 # External libraries (JSON / GSON)
└── images/              # Screenshots for documentation (README)
```


## Example JSON Object

Example of an object definition used by the engine:

```json
{
  "id": "tree_oak",
  "category": "environment",
  "sprite": "assets/images/environment/tree_oak.png",
  "hp": 10
}
