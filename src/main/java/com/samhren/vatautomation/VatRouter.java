package com.samhren.vatautomation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.lance5057.extradelight.ExtraDelightRecipes;
import com.lance5057.extradelight.workstations.vat.VatBlockEntity;
import com.lance5057.extradelight.workstations.vat.recipes.VatRecipe;
import com.lance5057.extradelight.workstations.vat.recipes.VatRecipeWrapper;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/**
 * Decides where an automated insert goes inside the vat.
 * <p>
 * The vat matches recipes shapelessly over its six ingredient slots, one ingredient per non-empty
 * slot, and consumes one item from each slot per batch. Each slot is therefore a "lane": a recipe
 * that needs four cucumbers needs four lanes of cucumbers, and stacking more cucumbers into those
 * lanes queues up more batches. Incoming items open a new lane while some vat recipe can still use
 * one more of them (given the lanes, fluid and container already present), and otherwise top up the
 * existing lanes evenly. Items no recipe can use are rejected, so they stay on the belt.
 * <p>
 * Stage ingredients (salt, yeast) are only accepted one at a time, when the running recipe's current
 * stage asks for them, because the vat requires the fermentation slot to be empty during stages that
 * have no ingredient.
 */
final class VatRouter {
	private static final int LANES = 6;

	private VatRouter() {
	}

	static ItemStack insert(VatBlockEntity vat, Level level, ItemStack stack, boolean simulate) {
		IItemHandlerModifiable inv = vat.getItemHandler();
		ItemStack[] slots = new ItemStack[inv.getSlots()];
		for (int i = 0; i < slots.length; i++) {
			slots[i] = inv.getStackInSlot(i).copy();
		}
		int[] placed = new int[slots.length];
		ItemStack remaining = stack.copy();

		routeStageIngredient(vat, level, inv, slots, placed, remaining);
		List<VatRecipe> recipes = level.getRecipeManager().getAllRecipesFor(ExtraDelightRecipes.VAT.get()).stream()
				.map(RecipeHolder::value).toList();
		FluidStack fluid = vat.getFluidTank().getFluid();
		routeContainer(recipes, fluid, inv, slots, placed, remaining);
		routeIngredients(recipes, fluid, inv, slots, placed, remaining);

		if (!simulate) {
			for (int i = 0; i < placed.length; i++) {
				if (placed[i] > 0) {
					inv.insertItem(i, stack.copyWithCount(placed[i]), false);
				}
			}
		}
		return remaining;
	}

	private static void routeStageIngredient(VatBlockEntity vat, Level level, IItemHandlerModifiable inv,
			ItemStack[] slots, int[] placed, ItemStack remaining) {
		int slot = VatBlockEntity.FERMENTATION_INPUT_SLOT;
		if (remaining.isEmpty() || !slots[slot].isEmpty()) {
			return;
		}
		Optional<RecipeHolder<VatRecipe>> active = level.getRecipeManager()
				.getRecipeFor(ExtraDelightRecipes.VAT.get(), new VatRecipeWrapper(inv, vat.getFluidTank()), level);
		if (active.isEmpty()) {
			return;
		}
		List<VatRecipe.StageIngredient> stages = active.get().value().getStageIngredients();
		int stage = vat.getStage();
		if (stage >= stages.size()) {
			return;
		}
		Ingredient needed = stages.get(stage).ingredient;
		if (!needed.isEmpty() && needed.test(remaining)) {
			place(slots, placed, slot, remaining, 1);
		}
	}

	private static void routeContainer(List<VatRecipe> recipes, FluidStack fluid, IItemHandlerModifiable inv,
			ItemStack[] slots, int[] placed, ItemStack remaining) {
		int slot = VatBlockEntity.CONTAINER_SLOT;
		if (remaining.isEmpty() || !fitsSlot(slots[slot], remaining)) {
			return;
		}
		List<ItemStack> lanes = lanes(slots);
		boolean wanted = recipes.stream().anyMatch(r -> !r.getUsedItem().isEmpty()
				&& ItemStack.isSameItem(r.getUsedItem(), remaining) && isCompatible(r, fluid, remaining)
				&& canHoldLanes(r, lanes));
		if (wanted) {
			place(slots, placed, slot, remaining, room(inv, slots, slot, remaining));
		}
	}

	private static void routeIngredients(List<VatRecipe> recipes, FluidStack fluid, IItemHandlerModifiable inv,
			ItemStack[] slots, int[] placed, ItemStack remaining) {
		ItemStack container = slots[VatBlockEntity.CONTAINER_SLOT];

		// Open new lanes while a recipe can use another one of this item.
		while (!remaining.isEmpty()) {
			int empty = firstEmptyLane(slots);
			if (empty < 0) {
				break;
			}
			List<ItemStack> lanes = lanes(slots);
			lanes.add(remaining);
			if (recipes.stream().noneMatch(r -> isCompatible(r, fluid, container) && canHoldLanes(r, lanes))) {
				break;
			}
			place(slots, placed, empty, remaining, 1);
		}

		// Then spread the rest evenly over the lanes that already hold this item.
		while (!remaining.isEmpty()) {
			int target = -1;
			for (int i = 0; i < LANES; i++) {
				if (!slots[i].isEmpty() && ItemStack.isSameItemSameComponents(slots[i], remaining)
						&& room(inv, slots, i, remaining) > 0
						&& (target < 0 || slots[i].getCount() < slots[target].getCount())) {
					target = i;
				}
			}
			if (target < 0) {
				break;
			}
			place(slots, placed, target, remaining, 1);
		}
	}

	/** Whether the recipe's fluid and container agree with what is already in the vat. */
	private static boolean isCompatible(VatRecipe recipe, FluidStack fluid, ItemStack container) {
		if (!fluid.isEmpty() && !recipe.getFluid().ingredient().test(fluid)) {
			return false;
		}
		return container.isEmpty() || ItemStack.isSameItem(recipe.getUsedItem(), container);
	}

	/** Whether every lane can be matched to a distinct ingredient of the recipe. */
	static boolean canHoldLanes(VatRecipe recipe, List<ItemStack> lanes) {
		List<Ingredient> ingredients = recipe.getIngredients();
		if (lanes.size() > ingredients.size()) {
			return false;
		}
		int[] owner = new int[ingredients.size()];
		Arrays.fill(owner, -1);
		for (int lane = 0; lane < lanes.size(); lane++) {
			if (!assign(lane, lanes, ingredients, owner, new boolean[ingredients.size()])) {
				return false;
			}
		}
		return true;
	}

	// Augmenting-path step of a bipartite matching between lanes and ingredients.
	private static boolean assign(int lane, List<ItemStack> lanes, List<Ingredient> ingredients, int[] owner,
			boolean[] visited) {
		for (int i = 0; i < ingredients.size(); i++) {
			if (visited[i] || !ingredients.get(i).test(lanes.get(lane))) {
				continue;
			}
			visited[i] = true;
			if (owner[i] < 0 || assign(owner[i], lanes, ingredients, owner, visited)) {
				owner[i] = lane;
				return true;
			}
		}
		return false;
	}

	private static List<ItemStack> lanes(ItemStack[] slots) {
		List<ItemStack> lanes = new ArrayList<>();
		for (int i = 0; i < LANES; i++) {
			if (!slots[i].isEmpty()) {
				lanes.add(slots[i]);
			}
		}
		return lanes;
	}

	private static int firstEmptyLane(ItemStack[] slots) {
		for (int i = 0; i < LANES; i++) {
			if (slots[i].isEmpty()) {
				return i;
			}
		}
		return -1;
	}

	private static boolean fitsSlot(ItemStack existing, ItemStack incoming) {
		return existing.isEmpty() || ItemStack.isSameItemSameComponents(existing, incoming);
	}

	private static int room(IItemHandlerModifiable inv, ItemStack[] slots, int slot, ItemStack incoming) {
		int limit = Math.min(inv.getSlotLimit(slot), incoming.getMaxStackSize());
		return Math.max(0, limit - slots[slot].getCount());
	}

	private static void place(ItemStack[] slots, int[] placed, int slot, ItemStack remaining, int count) {
		count = Math.min(count, remaining.getCount());
		if (count <= 0) {
			return;
		}
		if (slots[slot].isEmpty()) {
			slots[slot] = remaining.copyWithCount(count);
		} else {
			slots[slot].grow(count);
		}
		placed[slot] += count;
		remaining.shrink(count);
	}
}
