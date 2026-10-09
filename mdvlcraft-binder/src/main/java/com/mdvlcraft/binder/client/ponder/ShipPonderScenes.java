package com.mdvlcraft.binder.client.ponder;

import java.util.List;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

final class ShipPonderScenes {
    private static final int FINISHED = 3;
    private static final List<ShipPonderScenes.Frame> ROWBOAT = List.of(
        new ShipPonderScenes.Frame(2, 2, "inner_left", "north"),
        new ShipPonderScenes.Frame(3, 2, "inner_right", "north"),
        new ShipPonderScenes.Frame(2, 3, "straight", "west"),
        new ShipPonderScenes.Frame(3, 3, "straight", "east"),
        new ShipPonderScenes.Frame(2, 4, "inner_right", "south"),
        new ShipPonderScenes.Frame(3, 4, "inner_left", "south")
    );
    private static final List<ShipPonderScenes.Frame> SLOOP = List.of(
        new ShipPonderScenes.Frame(4, 4, "inner_right", "west"),
        new ShipPonderScenes.Frame(5, 4, "straight", "north"),
        new ShipPonderScenes.Frame(6, 4, "straight", "north"),
        new ShipPonderScenes.Frame(7, 4, "straight", "north"),
        new ShipPonderScenes.Frame(8, 4, "inner_left", "east"),
        new ShipPonderScenes.Frame(2, 5, "inner_right", "west"),
        new ShipPonderScenes.Frame(3, 5, "straight", "north"),
        new ShipPonderScenes.Frame(4, 5, "outer_left", "north"),
        new ShipPonderScenes.Frame(5, 5, null, null),
        new ShipPonderScenes.Frame(6, 5, null, null),
        new ShipPonderScenes.Frame(7, 5, null, null),
        new ShipPonderScenes.Frame(8, 5, "straight", "east"),
        new ShipPonderScenes.Frame(2, 6, "inner_left", "west"),
        new ShipPonderScenes.Frame(3, 6, "straight", "south"),
        new ShipPonderScenes.Frame(4, 6, "outer_right", "south"),
        new ShipPonderScenes.Frame(5, 6, null, null),
        new ShipPonderScenes.Frame(6, 6, null, null),
        new ShipPonderScenes.Frame(7, 6, null, null),
        new ShipPonderScenes.Frame(8, 6, "straight", "east"),
        new ShipPonderScenes.Frame(4, 7, "inner_left", "west"),
        new ShipPonderScenes.Frame(5, 7, "straight", "south"),
        new ShipPonderScenes.Frame(6, 7, "straight", "south"),
        new ShipPonderScenes.Frame(7, 7, "straight", "south"),
        new ShipPonderScenes.Frame(8, 7, "inner_right", "east")
    );

    private ShipPonderScenes() {
    }

    static ResourceLocation item(String path) {
        return ResourceLocation.fromNamespaceAndPath("alekiships", path);
    }

    static void rowboat(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("rowboat_building", "Building a Rowboat");
        scene.configureBasePlate(0, 0, 6);
        scene.showBasePlate();
        showBuildArea(scene, util);
        scene.idle(10);
        placeFrames(scene, ROWBOAT);
        scene.overlay()
            .showText(80)
            .text("A rowboat is built on six watercraft frames, two wide and three long")
            .pointAt(util.vector().topOf(2, 1, 3))
            .placeNearTarget()
            .attachKeyFrame();
        scene.idle(90);
        fitPlanks(scene, ROWBOAT, util.vector().topOf(3, 1, 3));
        scene.overlay()
            .showText(80)
            .text("Place an oarlock on each middle frame, facing outwards")
            .pointAt(util.vector().topOf(2, 2, 3))
            .placeNearTarget()
            .attachKeyFrame();
        scene.world().setBlock(new BlockPos(2, 2, 3), with(state("oarlock"), "facing", "west"), false);
        scene.idle(10);
        scene.world().setBlock(new BlockPos(3, 2, 3), with(state("oarlock"), "facing", "east"), false);
        scene.idle(90);
        scene.overlay()
            .showText(100)
            .text("The last piece launches the rowboat. Right-click it with two oars to row; it sails on water")
            .pointAt(util.vector().topOf(2, 1, 2))
            .placeNearTarget()
            .attachKeyFrame();
        scene.idle(110);
        scene.markAsFinished();
    }

    static void sloop(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("sloop_building", "Building a Sloop");
        scene.configureBasePlate(0, 0, 11);
        scene.scaleSceneView(0.8F);
        scene.showBasePlate();
        showBuildArea(scene, util);
        scene.idle(10);
        placeFrames(scene, SLOOP);
        scene.overlay()
            .showText(90)
            .text("A sloop's hull takes 24 watercraft frames: angled frames round the edge, flat frames inside")
            .pointAt(util.vector().topOf(6, 1, 5))
            .placeNearTarget()
            .attachKeyFrame();
        scene.idle(100);
        fitPlanks(scene, SLOOP, util.vector().topOf(6, 1, 6));
        scene.overlay()
            .showText(90)
            .text("Place four cleats on the corner frames shown, each pair facing outwards across the hull")
            .pointAt(util.vector().topOf(4, 2, 4))
            .placeNearTarget()
            .attachKeyFrame();

        for (BlockPos pos : List.of(new BlockPos(4, 2, 4), new BlockPos(8, 2, 4))) {
            scene.world().setBlock(pos, with(state("cleat"), "facing", "north"), false);
            scene.idle(8);
        }

        for (BlockPos pos : List.of(new BlockPos(4, 2, 7), new BlockPos(8, 2, 7))) {
            scene.world().setBlock(pos, with(state("cleat"), "facing", "south"), false);
            scene.idle(8);
        }

        scene.idle(70);
        scene.overlay()
            .showText(120)
            .text(
                "The hull becomes a sloop under construction. Look at it to see what it needs next, and right-click it with that: stripped logs for the keel, planks for the deck, more stripped logs for bowsprit, mast and boom"
            )
            .pointAt(util.vector().topOf(6, 1, 6))
            .placeNearTarget()
            .attachKeyFrame();
        scene.idle(130);
        scene.overlay()
            .showText(120)
            .text("Then white wool for the mainsail and jib, fences for both railings, an anchor, and leads for the rigging")
            .pointAt(util.vector().topOf(6, 1, 6))
            .placeNearTarget();
        scene.idle(130);
        scene.markAsFinished();
    }

    private static void showBuildArea(SceneBuilder scene, SceneBuildingUtil util) {
        scene.world().showSection(util.select().layersFrom(1), Direction.DOWN);
    }

    private static void placeFrames(SceneBuilder scene, List<ShipPonderScenes.Frame> frames) {
        for (ShipPonderScenes.Frame frame : frames) {
            scene.world().setBlock(frame.pos(), genericFrame(frame), false);
            scene.idle(frames.size() > 10 ? 2 : 5);
        }
    }

    private static void fitPlanks(SceneBuilder scene, List<ShipPonderScenes.Frame> frames, Vec3 hint) {
        scene.overlay().showControls(hint, Pointing.DOWN, 60).rightClick().withItem(new ItemStack(Items.f_42647_));
        scene.overlay().showText(80).text("Right-click every frame with planks to give it a wood").pointAt(hint).placeNearTarget().attachKeyFrame();

        for (ShipPonderScenes.Frame frame : frames) {
            scene.world().setBlock(frame.pos(), woodFrame(frame, 0), false);
            scene.idle(frames.size() > 10 ? 2 : 5);
        }

        scene.idle(50);
        scene.overlay().showText(80).text("Then add three more planks to each frame until it is fully planked").pointAt(hint).placeNearTarget();

        for (int stage = 1; stage <= 3; stage++) {
            for (ShipPonderScenes.Frame frame : frames) {
                scene.world().setBlock(frame.pos(), woodFrame(frame, stage), false);
            }

            scene.idle(20);
        }

        scene.idle(30);
    }

    private static BlockState genericFrame(ShipPonderScenes.Frame frame) {
        return frame.shape == null ? state("watercraft_frame_flat") : angled(state("watercraft_frame_angled"), frame);
    }

    private static BlockState woodFrame(ShipPonderScenes.Frame frame, int processed) {
        String id = frame.shape == null ? "wood/watercraft_frame/flat/oak" : "wood/watercraft_frame/angled/oak";
        BlockState state = frame.shape == null ? state(id) : angled(state(id), frame);
        return with(state, "frame_processed", String.valueOf(processed));
    }

    private static BlockState angled(BlockState state, ShipPonderScenes.Frame frame) {
        return with(with(state, "shape", frame.shape), "facing", frame.facing);
    }

    private static BlockState state(String path) {
        if (!ForgeRegistries.BLOCKS.containsKey(item(path))) {
            throw new IllegalStateException("aleki's Nifty Ships has no block " + item(path));
        } else {
            return ((Block)ForgeRegistries.BLOCKS.getValue(item(path))).m_49966_();
        }
    }

    private static <T extends Comparable<T>> BlockState with(BlockState state, String name, String value) {
        Property<T> property = state.m_60734_().m_49965_().m_61081_(name);
        if (property == null) {
            throw new IllegalStateException(ForgeRegistries.BLOCKS.getKey(state.m_60734_()) + " has no property " + name);
        } else {
            T parsed = (T)property.m_6215_(value).orElseThrow(() -> new IllegalStateException(name + " cannot be " + value));
            return (BlockState)state.m_61124_(property, parsed);
        }
    }

    private record Frame(int x, int z, String shape, String facing) {
        BlockPos pos() {
            return new BlockPos(this.x, 1, this.z);
        }
    }
}
