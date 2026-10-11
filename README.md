# ExtraDelight Vat Automation

A small NeoForge 1.21.1 addon that makes the [ExtraDelight](https://github.com/Lance5057/ExtraDelight) Vat automatable with Create, hoppers and any other mod that uses item and fluid capabilities.

ExtraDelight registers automation capabilities for all of its workstations except the Vat.
This addon adds them, with routing that understands how the Vat works.
It does not modify or redistribute any ExtraDelight code.

## Requirements

- Minecraft 1.21.1, NeoForge 21.1.4+
- ExtraDelight 2.6+ (tested against 2.6.6)

Only the server needs this mod.
It adds no blocks, items or network packets, so clients can join without it.

## What it does

### Items (funnels, belts, hoppers, chutes)

Push everything into the Vat from any side and it goes to the right slot:

- **Ingredients** fill the six ingredient slots. The Vat needs one slot per ingredient (pickled carrots need four slots of carrots), so the addon opens a new slot while some Vat recipe can still use one more of that item, then spreads the rest evenly over those slots to queue up more batches.
- **Containers** (bowls, glass bottles, lily pads, dripleaf) go to the container slot.
- **Salt and yeast** go to the stage slot one at a time, and only when the current stage asks for them. The Vat refuses to progress through stages that have no ingredient while that slot is occupied, so the rest wait on the belt until they are needed.
- **Anything no recipe can use is rejected**, so it stays on the belt instead of clogging the Vat.

Only the output slot can be extracted, so a pulling funnel or hopper never takes ingredients back out.

The fluid already in the Vat and the container already loaded decide between recipes that share ingredients.
For example, raw fish with water becomes fish sauce (one fish per batch), and raw fish with vinegar becomes pickled fish (four per batch).

### Fluids (Create pipes)

The Vat's tank is exposed on every side, so Create fluid pipes and pumps can fill it with water, vinegar, apple cider or lemon juice.

### Lid (comparator)

Some stages need the lid on and others need it off.
The Vat now gives a **comparator signal of 15 while the current stage needs the lid, and 0 otherwise**.
The lid is a normal pushable block, so a sticky piston can slide it on and off:

```
 side view (looking west)

   y+1   [sticky piston ->] [lid parked] [ vat top ]
   y     [ solid block   ] [comparator] [   VAT   ]
                            (facing the vat)
```

The comparator powers the solid block, which powers the sticky piston above it.
When the stage needs the lid, the piston pushes the lid onto the Vat; otherwise it pulls it back.
A Create mechanical piston also works.

## A Create setup

1. Place the Vat with the lid piston on one side as above.
2. Bring a belt into a funnel on another side with mixed ingredients, containers and salt or yeast. Filter it to the recipe you want.
3. Connect a fluid pipe and pump to any free side.
4. Put an extracting funnel on the last side, feeding a belt or chute.

**Tip:** use one Vat per recipe.
Recipes that overlap on the same ingredient, fluid and container are told apart by what is already in the Vat, so mixing them in one Vat can pick the wrong one.
Miso and soy sauce are the main case (both use cooked soybeans, water and glass bottles).
If the layout ends up wrong, drop the right items into the slots by hand once, and automation keeps that layout as long as the inputs never fully run out.

Stages that use yeast pop out an empty glass bottle on top of the Vat, as in the base mod.
A funnel or hopper next to it can collect them.

## Building

```
./gradlew build             # jar in build/libs
./gradlew runGameTestServer # runs the GameTests
```

The GameTests run in a real server with Create installed.
They cover slot routing, recipe disambiguation, a vanilla hopper, and a full two-batch sauerkraut run fed by Create funnels with a comparator-driven piston handling the lid.
