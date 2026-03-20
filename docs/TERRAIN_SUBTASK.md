# Terrain Sub-task — Implementation Guide

**Complexity:** 1 unit  
**Dependencies:** Map, roads, vehicles, BuildController

---

## 1. Requirements Summary

| Requirement | Description |
|-------------|-------------|
| Tile heights | Tiles have varying heights (integer, e.g. 0–3 or 0–7) |
| Visual slopes | Different slope-type sprites for each height/slope configuration |
| Road building | Roads only between tiles with same height or ±1 difference |
| Vehicle speed | Vehicles move slower on uphill slopes |
| Terraform | Player can raise/lower empty tiles by 1, for a cost |
| Constraint | Adjacent tiles: max height difference = 2 |

---

## 2. Model Changes

### 2.1 `Tile` — Add Height

```java
public class Tile {
    private final TileType type;
    private int height;  // e.g. 0–7, represents elevation

    public int getHeight() { return height; }
    public void setHeight(int height) {
        if (height < 0 || height > MAX_HEIGHT) throw new IllegalArgumentException(...);
        this.height = height;
    }
}
```

**Constants:** Define `MIN_HEIGHT`, `MAX_HEIGHT` (e.g. 0 and 7).

---

### 2.2 `SlopeType` — Slope Between Adjacent Tiles

Each tile needs a slope type for rendering. The slope describes how this tile connects to its 4 neighbors (N, S, E, W).

**Option A — Simple:** Store only `height` per tile. Compute slope at render time by comparing with neighbors.

**Option B — Cached:** Store `SlopeType` on tile, update when tile or neighbors change.

**SlopeType enum (simplified set):**
```java
public enum SlopeType {
    FLAT,           // all neighbors same height
    N, S, E, W,     // slope up in one direction
    NE, NW, SE, SW, // corner slopes
    // Add more as needed for smooth transitions
}
```

**Computation:** Given tile height `h` and neighbor heights `nN, nS, nE, nW`, determine which edges "rise" (neighbor lower) or "fall" (neighbor higher). Map that to a `SlopeType`.

---

### 2.3 `GameMap` — Height Helpers

```java
public int getHeight(int x, int y) {
    if (!isInBounds(x, y)) return -1; // or throw
    return getTile(x, y).getHeight();
}

public int getHeightDiff(int x1, int y1, int x2, int y2) {
    return Math.abs(getHeight(x1, y1) - getHeight(x2, y2));
}

public boolean canTerraform(int x, int y, int delta) {
    // delta = +1 (raise) or -1 (lower)
    Tile t = getTile(x, y);
    if (t.getType() != TileType.EMPTY) return false;  // only empty tiles
    int newHeight = t.getHeight() + delta;
    if (newHeight < MIN_HEIGHT || newHeight > MAX_HEIGHT) return false;
    // Check all 4 neighbors: |newHeight - neighborHeight| <= 2
    for (int[] dir : NEIGHBORS) {
        int nh = getHeight(x + dir[0], y + dir[1]);
        if (nh >= 0 && Math.abs(newHeight - nh) > 2) return false;
    }
    return true;
}
```

---

## 3. Road Building Validation

**In `BuildController` (or a `RoadValidator`):**

```java
public boolean canBuildRoadBetween(int x1, int y1, int x2, int y2) {
    if (!areAdjacent(x1, y1, x2, y2)) return false;
    int h1 = gameMap.getHeight(x1, y1);
    int h2 = gameMap.getHeight(x2, y2);
    int diff = Math.abs(h1 - h2);
    return diff <= 1;  // same height or ±1
}
```

When building a road segment, validate both endpoints against all connected tiles. Reject if any pair has `|h1 - h2| > 1`.

---

## 4. Vehicle Speed on Uphill Slopes

**In `Vehicle` or `VehicleController`:**

When moving from tile A to tile B:
- `heightDiff = height(B) - height(A)`
- If `heightDiff > 0` (uphill): apply speed multiplier, e.g. `speed *= 0.5` or `speed *= 0.7`
- If `heightDiff <= 0`: normal speed

```java
public float getEffectiveSpeed(Vehicle v, int fromX, int fromY, int toX, int toY) {
    float baseSpeed = v.getType().getSpeed();
    int hFrom = gameMap.getHeight(fromX, fromY);
    int hTo = gameMap.getHeight(toX, toY);
    if (hTo > hFrom) {
        return baseSpeed * UPHILL_SPEED_MULTIPLIER;  // e.g. 0.6
    }
    return baseSpeed;
}
```

---

## 5. Terraform

### 5.1 `TerraformController` (or extend `BuildController`)

```java
public class TerraformController {
    private static final long TERRAFORM_COST = 1000;  // per +1 or -1

    public boolean terraform(int x, int y, int delta) {
        // delta: +1 raise, -1 lower
        if (delta != 1 && delta != -1) return false;
        if (!gameMap.canTerraform(x, y, delta)) return false;
        if (economy.getCapital() < TERRAFORM_COST) return false;

        economy.deductCost(TERRAFORM_COST);
        gameMap.getTile(x, y).setHeight(gameMap.getTile(x, y).getHeight() + delta);
        return true;
    }
}
```

**Rules:**
- Only on `EMPTY` tiles (no roads, no cities, no facilities)
- Cost per terraform action
- After change: all adjacent height differences must remain ≤ 2

---

## 6. View — Slope Sprites

### 6.1 Sprite Mapping

For each `SlopeType`, have a sprite (or sprite region in an atlas):

| SlopeType | Sprite | Visual |
|-----------|--------|--------|
| FLAT | `tile_flat.png` | Level ground |
| N | `tile_slope_n.png` | Rises toward north |
| S | `tile_slope_s.png` | Rises toward south |
| ... | ... | ... |

**Alternative:** Use a single tileset where each tile has multiple variants (flat, slope N, slope S, etc.).

### 6.2 `MapView` — Render by Slope

```java
for (int y = 0; y < mapHeight; y++) {
    for (int x = 0; x < mapWidth; x++) {
        Tile t = gameMap.getTile(x, y);
        SlopeType slope = computeSlope(x, y);  // from tile + neighbors
        Sprite sprite = slopeSprites.get(slope);
        batch.draw(sprite, screenX, screenY);
    }
}
```

**`computeSlope(x, y)`:** Compare tile height with N, S, E, W neighbors; return appropriate `SlopeType`.

---

## 7. UI — Terraform Mode

### 7.1 Terraform Tool

- Add "Terraform" button or tool in build menu
- When active: clicking a tile raises it (+1), right-click lowers it (-1)
- Or: two buttons "Raise" / "Lower", then click tile
- Show cost before/after: "Terraform: 1000"

### 7.2 Validation Feedback

- Valid tile: highlight green or show "Raise/Lower (1000)"
- Invalid: highlight red, tooltip "Cannot terraform here" (not empty / would violate max diff 2)

---

## 8. Implementation Checklist

### Model
- [ ] Add `height` to `Tile`
- [ ] Add `SlopeType` enum (or compute at render time)
- [ ] `GameMap.canTerraform(x, y, delta)`
- [ ] `GameMap.getHeight(x, y)`, `getHeightDiff`

### BuildController
- [ ] Road validation: `|h1 - h2| <= 1` for adjacent road tiles
- [ ] Reject road build if height diff > 1

### TerraformController
- [ ] `terraform(x, y, +1)` and `terraform(x, y, -1)`
- [ ] Cost check, `canTerraform` check

### VehicleController
- [ ] `getEffectiveSpeed()` with uphill multiplier
- [ ] Apply when moving vehicle between tiles

### View
- [ ] Slope sprites for each `SlopeType`
- [ ] `MapView` renders tile by slope (not just type)
- [ ] `computeSlope(x, y)` from heights

### UI
- [ ] Terraform tool in build menu
- [ ] Click to raise, right-click to lower (or separate buttons)
- [ ] Cost display, validation feedback

---

## 9. Suggested Task Split (4 people)

| Person | Tasks |
|--------|-------|
| **Ramez** | `Tile` height, `SlopeType`, `GameMap.canTerraform`, `getHeight` |
| **Komron** | Road build validation (height diff ≤ 1), `TerraformController` |
| **Harry** | Slope sprites, `MapView` slope rendering, terraform UI |
| **Uluk** | Vehicle speed on uphill in `VehicleController`, integration |

---

## 10. Slope Type Reference (OpenTTD-style)

For a full set, you typically need 16 slope types:

- FLAT
- N, S, E, W (one edge raised)
- NE, NW, SE, SW (two adjacent edges)
- NSE, NSW, ESW, ENW (three edges)
- All four (plateau)

Start with FLAT + N, S, E, W if time is limited; expand as needed.

---

*Document version: 1.0*  
*Team 404 — Mini Transport Tycoon*
