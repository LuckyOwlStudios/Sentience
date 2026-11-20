# Races

A Minecraft 1.21.1 mod (Fabric + NeoForge) that adds races to the game, and a reputation with each race for every player.

## Races

Out of the box there are three races:

| Race | Members | Killing one | Trading / bartering | Winning a raid |
|------|---------|-------------|---------------------|----------------|
| Villagers | villager, wandering trader | -10 | +1 per trade | +30 |
| Illagers | `#minecraft:illager` (pillager, vindicator, evoker, illusioner), ravager | -5 | (no trades yet) | -30 |
| Piglins | piglin, piglin brute | -10 | +1 per barter | 0 |

Reputation starts at 0 and stays between -100 and 100. Every change shows in the action bar.

- **Killing** a creature lowers your reputation with its race. Kills by your arrows, and kills you get credit for (knocking something into lava), count too.
- **Trading** with any merchant that belongs to a race (villagers and wandering traders by default) raises your reputation with it.
- **Bartering**: when a piglin barters gold that *you* threw to it, your piglin reputation goes up.
- **Raids**: everyone who is a Hero of the Village when a raid is defeated gets each race's `raid_victory` amount: a big boost with villagers and a big drop with illagers.

## Adding a race

Races are data-driven. Add a JSON file to a datapack or your mod's resources at
`data/<namespace>/races/race/<name>.json`. The file's location is the race's id, so
`data/mymod/races/race/elves.json` is `mymod:elves`.

```json
{
  "name": { "translate": "race.mymod.elves" },
  "color": "#7FD17F",
  "members": "#mymod:elves",
  "reputation": { "default": 0, "min": -100, "max": 100 },
  "changes": {
    "killed_member": -10,
    "traded": 1,
    "raid_victory": 0
  }
}
```

- `members` (required): an entity type tag (`"#mymod:elves"`), a single entity id (`"mymod:elf"`), or a list of ids.
- `name`, `color`: how the race is shown. Default to `?` and white.
- `reputation`: the starting value and the bounds. Defaults to 0, -100 and 100.
- `changes`: how much each built-in action changes reputation with this race. Leave one out (or use 0) to turn it off.

Datapacks can also override the default races, e.g. replace `data/races/races/race/illagers.json` or add entities to the `races:villagers`, `races:illagers` and `races:piglins` entity type tags.

### Choosing a race in code

If membership depends on more than the entity type:

- implement `RaceMember` on your entity and return a race key from `races$getRace()`, or
- call `Races.registerResolver(entity -> ...)` during mod setup for entities you don't own.

Return `null` from either to fall back to the race's `members`.

## Reputation API

Everything is in `net.luckyowlstudios.races.api` and works the same on both loaders. Server side only.

```java
// Read
int rep = Reputation.get(player, Races.PIGLINS);
boolean friendly = Reputation.isAtLeast(player, Races.ILLAGERS, 50);
Map<ResourceKey<Race>, Integer> all = Reputation.getAll(player);

// Change (clamped to the race's bounds; each returns the new value)
Reputation.add(player, Races.VILLAGERS, 5);
Reputation.remove(player, Races.VILLAGERS, 10);
Reputation.set(player, Races.PIGLINS, 0);
Reputation.addForEntity(player, someMob, 3);          // whichever race the mob belongs to
Reputation.add(player, Races.PIGLINS, 5, new ReputationCause(ResourceLocation.fromNamespaceAndPath("mymod", "gift")));
Reputation.reset(player, Races.PIGLINS);
Reputation.resetAll(player);

// Offline players
Reputation.add(server, uuid, Races.VILLAGERS, 5, ReputationCause.OTHER);

// Races
Races.getRace(entity);                    // Optional<Holder.Reference<Race>>
Races.isMember(entity, Races.ILLAGERS);
Races.all(registryAccess);

// Listen for changes
ReputationEvents.onChanged((server, playerId, race, oldValue, newValue, cause) -> { ... });
```

Your own races get a key the same way: `Races.key(ResourceLocation.fromNamespaceAndPath("mymod", "elves"))`.

Reputation is saved with the world (`data/races_reputation.dat`), so it survives death and can be changed while a player is offline.

## Commands

For operators (permission level 2):

- `/reputation get <player> [race]`
- `/reputation set <players> <race> <value>`
- `/reputation add <players> <race> <amount>`
- `/reputation reset <players> [race]`

## Building

```
./gradlew build                  # both loaders
./gradlew :fabric:runGametest    # boots a server and runs the game tests
```
