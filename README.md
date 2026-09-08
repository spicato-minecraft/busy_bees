# Busy Bees

Fabric mod for Minecraft **1.21.11**. Owns bee gameplay: night torchflower work, 1.3× pitcher-plant pollen, weaker flyover vegetation, and overflow nest founding. Pair with **Functional Torchflowers** for torchflower light (glow-only if Busy Bees is not installed).

## Compatibility

| Component     | Version           |
|---------------|-------------------|
| Minecraft     | 1.21.11           |
| Fabric Loader | 0.19.3+           |
| Fabric API    | 0.141.6+1.21.11   |
| Java          | 21+               |

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

Play the indexed local set from [`../dev-pack`](../dev-pack/README.md) (`./gradlew runClient` there).

## Install

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 1.21.11.
2. Download [Fabric API](https://modrinth.com/mod/fabric-api) for 1.21.11 and place it in your mods folder.
3. Copy the built JAR into your Minecraft `mods` folder.
4. Optional: install Functional Torchflowers for torchflower light.

## Branch Policy

Each supported Minecraft version has its own long-lived branch named after the exact `minecraft_version` in `gradle.properties`. Version branches are the source of truth; `main` is not used.

- **Default branch:** `1.21.11`
- **Feature/fix work:** branch from the target version as `{version}/feature-name`, merge back into the version branch

```bash
git clone https://github.com/spicato-minecraft/busy_bees.git
cd busy_bees
git checkout 1.21.11
```

## Releases and Modrinth

Merging code does **not** publish to Modrinth. Publishing happens only when a GitHub Release is created.

Release tags use `{minecraft_version}-{mod_version}` (e.g. `1.21.11-1.0.0`), matching `gradle.properties`.

1. Merge your changes into the version branch (e.g. `1.21.11`).
2. Bump `mod_version` in `gradle.properties` if shipping new player-facing changes.
3. Add a changelog at `releases/{tag}.md` — see `releases/TEMPLATE.md`.
4. Create a GitHub Release with tag `{minecraft_version}-{mod_version}` targeting the version branch.

See [minecraft-mods/docs/README.md](../docs/README.md) for org-wide release conventions.

## Test Strategy

| Layer      | Purpose |
|------------|---------|
| Unit tests | Qualifying torchflowers, pitcher plant vs crop, founding band, flyover exclusions |
| GameTests  | Night stay/target, founding skip/place, pitcher/flyover predicates |

Behavior tests are tracked in [Notion Tasks](https://app.notion.com/p/3b716e8d4da380adbd93cf0a73b39d5c). Manual UAT items live in the Mod Catalog.

## Architecture

Vanilla 1.21.11 keeps bees in hives via `EnvironmentAttributes.BEES_STAY_IN_HIVE`. This mod:

1. Opens the hive at night when a qualifying torchflower is in vanilla nectar range (rain still homes).
2. Restricts night nectar targeting to those torchflowers.
3. Marks nectar from a mature two-block pitcher plant and shortens crop-grow delay by 1.3×.
4. Applies throttled weaker bone meal to non-farmland surfaces while carrying pollen.
5. When a hive is full and no other hive/nest is in range, surplus bees found a nest on logs/leaves in a ~22–40 band.

Bees do not self-breed. Population still comes from player-fed vanilla breeding.
