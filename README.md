# Sentience

A Minecraft 1.21.1 mod (Fabric + NeoForge) that adds races to the game, and a reputation with each race for every player.

## Races

Out of the box there are three races:

| Race | Members | Killing one | Trading / bartering | Winning a raid |
|------|---------|-------------|---------------------|----------------|
| Villagers | villager, wandering trader | -10 | +1 per trade | +30, and +2 per raider you kill during a raid |
| Illagers | `#minecraft:illager` (pillager, vindicator, evoker, illusioner), ravager | -5 | (no trades yet) | -30 |
| Piglins | piglin, piglin brute | -10 | +1 per barter | 0 |

Reputation starts at 0 and stays between -100 and 100. Every change shows in the action bar.

- **Killing** a creature lowers your reputation with its race. Kills by your arrows, and kills you get credit for (knocking something into lava), count too.
- **Trading** with any merchant that belongs to a race (villagers and wandering traders by default) raises your reputation with it.
- **Bartering**: when a piglin barters gold that *you* threw to it, your piglin reputation goes up.
- **Village cats**: hurting a cat costs -2 villager reputation and killing one -15, but only if a villager is within 16 blocks to see it. Tamed cats (yours or anyone's) don't count.
- **Defending a raid**: every illager (or other raider) you kill while it's part of an active raid gives +2 villager reputation, on top of the usual illager penalty for the kill.
- **Raids**: everyone who is a Hero of the Village when a raid is defeated gets each race's `raid_victory` amount: a big boost with villagers and a big drop with illagers.

## Trust and trading

Once your reputation with **illagers** or **piglins** reaches **20**:

- They stop picking fights with you. Illagers (and ravagers) won't target you, and piglins treat you as if you wore gold. They still fight back if you hit them, and raiders in an active raid still attack.
- You can **trade** with them. Right-click a pillager, vindicator, evoker or piglin to open the normal trade screen. Piglins trade for gold, illagers for emeralds, and each mob type has its own list. Piglin brutes don't trade.
- If your reputation drops below 20, both stop: an open trade screen closes, and they turn hostile again.

Each mob picks a few trades from its list the first time someone trades with it, keeps them, and restocks once a day. Trades count toward reputation like villager trades (+1 each). Holding gold still lets you barter with piglins as usual, and sneaking skips the trade screen.

### Trade lists

One file per mob type at `data/<namespace>/sentience/trades/<name>.json`:

```json
{
  "entities": "minecraft:piglin",
  "offers_per_mob": 5,
  "restock_ticks": 24000,
  "trade_sound": "minecraft:entity.piglin.admiring_item",
  "trades": [
    {
      "wants": { "id": "minecraft:gold_ingot", "count": 4 },
      "wants_also": { "id": "minecraft:blackstone", "count": 8 },
      "gives": { "id": "minecraft:crying_obsidian", "count": 2 },
      "max_uses": 8,
      "xp": 1
    }
  ]
}
```

Only `entities`, `trades`, `wants` and `gives` are required. The reputation needed comes from the race's `trades_at`, and whether its members leave you alone from `peaceful_at`.

## Adding a race

Races are data-driven. Add a JSON file to a datapack or your mod's resources at
`data/<namespace>/sentience/race/<name>.json`. The file's location is the race's id, so
`data/mymod/sentience/race/elves.json` is `mymod:elves`.

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
  },
  "peaceful_at": 20,
  "trades_at": 20
}
```

- `members` (required): an entity type tag (`"#mymod:elves"`), a single entity id (`"mymod:elf"`), or a list of ids.
- `name`, `color`: how the race is shown. Default to `?` and white.
- `reputation`: the starting value and the bounds. Defaults to 0, -100 and 100.
- `changes`: how much each built-in action changes reputation with this race. Leave one out (or use 0) to turn it off.
- `changes.killed_raider`: applied when you kill a raider that's part of an active raid.
- `protects`: creatures the race cares about without being members. `{ "entities": "#sentience:village_pets", "hurt": -2, "killed": -15, "witness_radius": 16 }` means hurting or killing one costs that much, if a member is within `witness_radius` blocks. Tamed animals don't count. Add mobs to the `sentience:village_pets` tag to make villagers care about them too.
- `peaceful_at`: at or above this reputation, members don't start fights with you. Leave out for never.
- `trades_at`: at or above this reputation, members with a trade list trade with you. Leave out for never.

Datapacks can also override the default races, e.g. replace `data/sentience/sentience/race/illagers.json` or add entities to the `sentience:villagers`, `sentience:illagers` and `sentience:piglins` entity type tags.

### Choosing a race in code

If membership depends on more than the entity type:

- implement `RaceMember` on your entity and return a race key from `sentience$getRace()`, or
- call `Races.registerResolver(entity -> ...)` during mod setup for entities you don't own.

Return `null` from either to fall back to the race's `members`.

## Reputation API

Everything is in `net.luckyowlstudios.sentience.api` and works the same on both loaders. Server side only.

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

Reputation is saved with the world (`data/sentience_reputation.dat`), so it survives death and can be changed while a player is offline.

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
