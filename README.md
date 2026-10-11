# Busy Bees

Fabric mod for Minecraft **26.3**. Owns bee gameplay: night torchflower work, 1.3× pitcher-plant pollen, weaker flyover vegetation, and overflow nest founding. Pair with **Functional Torchflowers** for torchflower light (glow-only if Busy Bees is not installed).

## Compatibility

| Component     | Version           |
|---------------|-------------------|
| Minecraft     | 26.3              |
| Fabric Loader | 0.19.5+           |
| Fabric API    | 0.162.0+26.3      |
| Java          | 25+               |

## Build

From this directory:

```bash
./gradlew build
```

The output JAR is at `build/libs/busy_bees-1.0.0.jar` (version from `gradle.properties`).

## Test

```bash
./gradlew test          # unit tests (JUnit + Fabric Loader bootstrap)
./gradlew runGameTest   # server GameTests
./gradlew build         # full CI gate (includes both)
```

Play the indexed local set from [`../dev-pack-26.3`](../dev-pack-26.3/README.md) (`./gradlew runClient` there).

## Install

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 26.3.
2. Download [Fabric API](https://modrinth.com/mod/fabric-api) for 26.3 and place it in your mods folder.
3. Copy the built JAR into your Minecraft `mods` folder.
4. Optional: install Functional Torchflowers for torchflower light.

## Branch Policy

Each supported Minecraft version has its own long-lived branch named after the exact `minecraft_version` in `gradle.properties`. Version branches are the source of truth; `main` is not used.

- **Default branch:** `26.3`
- **Feature/fix work:** branch from the target version as `{version}/feature-name`, merge back into the version branch

```bash
git clone https://github.com/spicato-minecraft/busy_bees.git
cd busy_bees
git checkout 26.3
```

## Releases and Modrinth

Merging code does **not** publish to Modrinth. Publishing happens only when a GitHub Release is created.

Release tags use `{minecraft_version}-{mod_version}` (e.g. `26.3-1.0.0`), matching `gradle.properties`.

1. Merge your changes into the version branch (e.g. `26.3`).
2. Bump `mod_version` in `gradle.properties` if shipping new player-facing changes.
3. Add a changelog at `releases/{tag}.md` — see `releases/TEMPLATE.md`.
4. Create a GitHub Release with tag `{minecraft_version}-{mod_version}` targeting the version branch.

See [minecraft-mods/docs/README.md](../docs/README.md) for org-wide release conventions.

## Test Strategy

| Layer      | Purpose |
|------------|---------|
| Unit tests | Qualifying torchflowers, pitcher plant vs crop, founding band, adult-only founding age, flyover pollen targets, in-hive breeding, registered hive occupancy |
| GameTests  | Night stay/target, founding skip/place/dwell-enter, overflow relocate vs founding, pitcher/flyover predicates |

Behavior tests are tracked in [Notion Tasks](https://app.notion.com/p/3b716e8d4da380adbd93cf0a73b39d5c). Manual UAT items live in the Mod Catalog.

## Architecture

Vanilla 26.3 keeps bees in hives via `EnvironmentAttributes.BEES_STAY_IN_HIVE`. This mod:

1. Opens the hive at night when a qualifying torchflower is in vanilla nectar range (rain still homes).
2. Restricts night nectar targeting to those torchflowers.
3. Marks nectar from a mature two-block pitcher plant and shortens crop-grow delay by 1.3×.
4. About every 4 seconds (staggered per bee), while carrying pollen, bone-meals the first bonemealable block or crop directly below the bee (no height cap). Bare farmland is skipped; a crop on farmland is used instead.
5. When a hive is full, the surplus bee first looks (out to 48 blocks, wider than the 22-block hive range) for another hive/nest that has a **registered** vacancy: stored occupants plus loaded bees whose hive is that block, max 3. A hive is not open just because a resident is out collecting pollen. If one is found, the bee claims that hive immediately and flies straight to it. If none, a surplus **adult** founds: picks a log/leaf nest site 22–40 out, flies there, and must stay within 2 blocks of that air block for ~30s. Leaving the spot resets the dwell. Then one nest appears and the bee enters it. Babies never found.
6. Using a bee-food flower on a hive or nest feeds stored bees with vanilla love rules: one flower loves one ready adult (`InLove` on that occupant’s NBT). That adult cannot be fed again until love ends and `Age` is 0. A second flower on a second ready adult breeds them (both get the 6000-tick cooldown). Hearts play only for love or birth. If no adult can fall in love, a stored baby can still be age-up fed (happy-villager particles). The baby stays in the hive if there is room, otherwise it appears in front of the hive.

Bees do not self-breed. Population still comes from player-fed flowers, including while bees are inside the hive.
