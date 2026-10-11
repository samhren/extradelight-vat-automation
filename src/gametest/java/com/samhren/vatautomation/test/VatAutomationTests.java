package com.samhren.vatautomation.test;

import com.lance5057.extradelight.ExtraDelightBlocks;
import com.lance5057.extradelight.ExtraDelightConfig;
import com.lance5057.extradelight.workstations.vat.VatBlockEntity;
import com.samhren.vatautomation.VatAutomation;
import com.samhren.vatautomation.VatItemHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

@GameTestHolder(VatAutomation.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VatAutomationTests {
	private static final String EMPTY = "empty";
	private static final BlockPos VAT = new BlockPos(2, 1, 2);

	private static final String CABBAGE = "extradelight:shredded_cabbage_item";
	private static final String SALT = "extradelight:salt";
	private static final String BOWL = "minecraft:bowl";

	@GameTest(template = EMPTY)
	public static void routesIngredientsIntoLanes(GameTestHelper helper) {
		VatBlockEntity vat = placeVat(helper);
		IItemHandler items = items(helper, Direction.WEST);
		fill(helper, "minecraft:water", 1000);

		assertInserted(helper, items, item(BOWL, 4), 4);
		assertInserted(helper, items, item(CABBAGE, 64), 64);
		assertSlot(helper, vat, VatBlockEntity.CONTAINER_SLOT, BOWL, 4);
		assertSlot(helper, vat, 0, CABBAGE, 32);
		assertSlot(helper, vat, 1, CABBAGE, 32);
		assertSlot(helper, vat, 2, "minecraft:air", 0);

		assertInserted(helper, items, item("minecraft:dirt", 5), 0);
		// The recipe is ready, so exactly one salt is taken for the first stage.
		assertInserted(helper, items, item(SALT, 16), 1);
		assertSlot(helper, vat, VatBlockEntity.FERMENTATION_INPUT_SLOT, SALT, 1);
		assertInserted(helper, items, item(SALT, 16), 0);

		helper.assertTrue(items.extractItem(VatItemHandler.INPUT, 64, false).isEmpty(), "ingredients must not be extractable");
		helper.assertTrue(items.extractItem(VatItemHandler.OUTPUT, 64, false).isEmpty(), "output should be empty");
		helper.succeed();
	}

	@GameTest(template = EMPTY)
	public static void stageIngredientWaitsForRecipe(GameTestHelper helper) {
		placeVat(helper);
		IItemHandler items = items(helper, Direction.NORTH);
		fill(helper, "minecraft:water", 1000);
		assertInserted(helper, items, item(SALT, 4), 0);
		assertInserted(helper, items, item(CABBAGE, 2), 2);
		assertInserted(helper, items, item(SALT, 4), 0); // still no bowls
		assertInserted(helper, items, item(BOWL, 2), 2);
		assertInserted(helper, items, item(SALT, 4), 1);
		helper.succeed();
	}

	@GameTest(template = EMPTY)
	public static void buildsMultiIngredientLayout(GameTestHelper helper) {
		VatBlockEntity vat = placeVat(helper);
		IItemHandler items = items(helper, Direction.EAST);
		fill(helper, "minecraft:water", 1000);
		assertInserted(helper, items, item(BOWL, 8), 8);
		assertInserted(helper, items, item(CABBAGE, 8), 8);
		assertInserted(helper, items, item("extradelight:grated_ginger", 4), 4);
		assertInserted(helper, items, item("extradelight:garlic_clove", 4), 4);
		assertInserted(helper, items, item("extradelight:chili_powder", 4), 4);
		assertSlot(helper, vat, 0, CABBAGE, 4);
		assertSlot(helper, vat, 1, CABBAGE, 4);
		assertSlot(helper, vat, 2, "extradelight:grated_ginger", 4);
		assertSlot(helper, vat, 3, "extradelight:garlic_clove", 4);
		assertSlot(helper, vat, 4, "extradelight:chili_powder", 4);
		// Kimchi only uses two cabbage lanes, so more cabbage tops those up.
		assertInserted(helper, items, item(CABBAGE, 2), 2);
		assertSlot(helper, vat, 0, CABBAGE, 5);
		assertSlot(helper, vat, 5, "minecraft:air", 0);
		helper.succeed();
	}

	@GameTest(template = EMPTY)
	public static void fluidPicksBetweenOverlappingRecipes(GameTestHelper helper) {
		// Raw fish is used by fish sauce (1 fish, water) and pickled fish (4 fish, vinegar).
		VatBlockEntity vat = placeVat(helper);
		IItemHandler items = items(helper, Direction.SOUTH);
		fill(helper, "minecraft:water", 1000);
		assertInserted(helper, items, item("minecraft:cod", 8), 8);
		assertSlot(helper, vat, 0, "minecraft:cod", 8);
		assertSlot(helper, vat, 1, "minecraft:air", 0);

		BlockPos other = VAT.east(2);
		helper.setBlock(other, ExtraDelightBlocks.VAT.get());
		IFluidHandler tank = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(other), null);
		tank.fill(new FluidStack(fluid("extradelight:vinegar_fluid"), 1000), FluidAction.EXECUTE);
		VatBlockEntity pickler = helper.getBlockEntity(other);
		IItemHandler picklerItems = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(other), null);
		assertInserted(helper, picklerItems, item("minecraft:cod", 8), 8);
		for (int lane = 0; lane < 4; lane++) {
			assertSlot(helper, pickler, lane, "minecraft:cod", 2);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY, timeoutTicks = 200)
	public static void hopperFeedsLanes(GameTestHelper helper) {
		floor(helper);
		VatBlockEntity vat = placeVat(helper);
		fill(helper, "minecraft:water", 1000);
		BlockPos hopper = VAT.west();
		helper.setBlock(hopper, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.EAST));
		IItemHandler hopperItems = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(hopper), null);
		hopperItems.insertItem(0, item(CABBAGE, 6), false);
		hopperItems.insertItem(1, item(BOWL, 2), false);
		helper.succeedWhen(() -> {
			assertSlot(helper, vat, 0, CABBAGE, 3);
			assertSlot(helper, vat, 1, CABBAGE, 3);
			assertSlot(helper, vat, VatBlockEntity.CONTAINER_SLOT, BOWL, 2);
		});
	}

	/**
	 * The whole loop the way a player would build it: a Create funnel takes items thrown onto it, a second
	 * Create funnel pulls finished food out, and a comparator drives a sticky piston that slides the lid on
	 * and off. Runs two full sauerkraut batches (salt stage without lid, then a lidded stage).
	 */
	@GameTest(template = EMPTY, timeoutTicks = 1200)
	public static void createFunnelsAndPistonLidRunFullRecipe(GameTestHelper helper) {
		ExtraDelightConfig.ENABLE_DEBUG_MODE.set(true); // stages advance 1000 ticks per tick
		floor(helper);
		placeVat(helper);
		fill(helper, "minecraft:water", 1000);

		Block funnel = block("create:andesite_funnel");
		helper.setBlock(VAT.west(), withFacing(funnel.defaultBlockState(), Direction.WEST));
		helper.setBlock(VAT.east(), withFacing(funnel.defaultBlockState(), Direction.EAST)
				.setValue(bool(funnel, "extracting"), true));

		helper.setBlock(VAT.north(), Blocks.COMPARATOR.defaultBlockState().setValue(ComparatorBlock.FACING, Direction.SOUTH));
		BlockPos lidParked = VAT.north().above();
		BlockPos lidOn = VAT.above();
		helper.setBlock(VAT.north(2), Blocks.STONE);
		helper.setBlock(VAT.north(2).above(), Blocks.STICKY_PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.SOUTH));
		helper.setBlock(lidParked, ExtraDelightBlocks.LID.get());

		helper.runAfterDelay(5, () -> {
			throwInto(helper, VAT.west(), item(BOWL, 4));
			throwInto(helper, VAT.west(), item(CABBAGE, 4));
			throwInto(helper, VAT.west(), item(SALT, 2));
		});

		boolean[] sawLidOn = { false };
		int[] collected = { 0 };
		AABB outputArea = new AABB(helper.absolutePos(VAT.east()));
		helper.onEachTick(() -> {
			if (helper.getBlockState(lidOn).is(ExtraDelightBlocks.LID.get())) {
				sawLidOn[0] = true;
			}
			// Stand-in for the belt a real build would put in front of the output funnel.
			for (ItemEntity out : helper.getLevel().getEntities(EntityType.ITEM, outputArea, e -> true)) {
				helper.assertTrue(out.getItem().is(itemType("extradelight:sauerkraut_item")), "funnel pulled out " + out.getItem());
				collected[0] += out.getItem().getCount();
				out.discard();
			}
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(sawLidOn[0], "piston never put the lid on");
			helper.assertTrue(collected[0] == 4, "collected " + collected[0] + " sauerkraut, expected 4");
			helper.assertBlockPresent(ExtraDelightBlocks.LID.get(), lidParked);
			helper.assertTrue(countDropped(helper, CABBAGE) == 0 && countDropped(helper, SALT) == 0
					&& countDropped(helper, BOWL) == 0, "inputs should all have been consumed");
			ExtraDelightConfig.ENABLE_DEBUG_MODE.set(false);
		});
	}

	private static VatBlockEntity placeVat(GameTestHelper helper) {
		helper.setBlock(VAT, ExtraDelightBlocks.VAT.get());
		return helper.getBlockEntity(VAT);
	}

	private static void floor(GameTestHelper helper) {
		for (int x = 0; x < 5; x++)
			for (int z = 0; z < 5; z++)
				helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
	}

	private static IItemHandler items(GameTestHelper helper, Direction side) {
		return helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(VAT), side);
	}

	private static void fill(GameTestHelper helper, String fluid, int amount) {
		IFluidHandler tank = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(VAT), Direction.SOUTH);
		helper.assertTrue(tank != null, "vat has no fluid capability");
		int filled = tank.fill(new FluidStack(fluid(fluid), amount), FluidAction.EXECUTE);
		helper.assertTrue(filled == amount, "filled " + filled + " of " + amount);
	}

	private static void assertInserted(GameTestHelper helper, IItemHandler items, ItemStack stack, int expected) {
		helper.assertTrue(items != null, "vat has no item capability");
		int total = stack.getCount();
		ItemStack simulated = items.insertItem(VatItemHandler.INPUT, stack.copy(), true);
		ItemStack rest = items.insertItem(VatItemHandler.INPUT, stack.copy(), false);
		helper.assertTrue(simulated.getCount() == rest.getCount(), "simulate and execute disagree for " + stack);
		helper.assertTrue(total - rest.getCount() == expected,
				"inserted " + (total - rest.getCount()) + " of " + stack + ", expected " + expected);
	}

	private static void assertSlot(GameTestHelper helper, VatBlockEntity vat, int slot, String id, int count) {
		ItemStack actual = vat.getItemHandler().getStackInSlot(slot);
		boolean ok = count == 0 ? actual.isEmpty() : actual.is(itemType(id)) && actual.getCount() == count;
		helper.assertTrue(ok, "slot " + slot + " has " + actual + ", expected " + count + " " + id);
	}

	private static void throwInto(GameTestHelper helper, BlockPos funnelPos, ItemStack stack) {
		Direction front = helper.getBlockState(funnelPos).getValue(
				(DirectionProperty) helper.getBlockState(funnelPos).getBlock().getStateDefinition().getProperty("facing"));
		var center = helper.absolutePos(funnelPos).getCenter().add(front.getStepX() * 0.3, -0.4, front.getStepZ() * 0.3);
		ItemEntity entity = new ItemEntity(helper.getLevel(), center.x, center.y, center.z, stack, 0, 0, 0);
		helper.getLevel().addFreshEntity(entity);
	}

	private static int countDropped(GameTestHelper helper, String id) {
		AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(8);
		return helper.getLevel().getEntities(EntityType.ITEM, area, e -> e.getItem().is(itemType(id))).stream()
				.mapToInt(e -> e.getItem().getCount()).sum();
	}

	private static BlockState withFacing(BlockState state, Direction facing) {
		return state.setValue((DirectionProperty) state.getBlock().getStateDefinition().getProperty("facing"), facing);
	}

	private static BooleanProperty bool(Block block, String name) {
		return (BooleanProperty) block.getStateDefinition().getProperty(name);
	}

	private static ItemStack item(String id, int count) {
		return new ItemStack(itemType(id), count);
	}

	private static Item itemType(String id) {
		return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
	}

	private static Block block(String id) {
		return BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
	}

	private static Fluid fluid(String id) {
		return BuiltInRegistries.FLUID.get(ResourceLocation.parse(id));
	}
}
