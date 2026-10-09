import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;
import org.objectweb.asm.tree.*;

/**
 * Rewrites a T.O Magic 'n Extras 5.5.0 jar, built for an older L_Ender's Cataclysm and Iron's Spells, so it
 * links against Cataclysm 3.31 and Iron's Spells 3.16, and registers Violent Skreech without Alex's Mobs. Usage (ASM 9 on the class path):
 *   java -cp asm.jar:asm-tree.jar:asm-commons.jar tools/PatchTravelOptics.java in.jar out.jar
 */
public class PatchTravelOptics {
    static final String C = "com/github/L_Ender/cataclysm/";
    static final String P = C + "client/particle/";
    static final String I = "io/redspace/ironsspellbooks/";
    static final Map<String, String> MOVED = Map.ofEntries(Map.entry(I + "entity/mobs/dead_king_boss/DeadKingAnimatedWarlockAttackGoal", I + "entity/mobs/dead_king_boss/goals/DeadKingAnimatedWarlockAttackGoal"),
        Map.entry(I + "network/spell/ClientboundParticleShockwave", I + "network/particles/ShockwaveParticlesPacket"),
        Map.entry(P + "LightningParticle$OrbData", P + "Options/LightningParticleOptions"),
        Map.entry(P + "LightTrailParticle$OrbData", P + "Options/LightTrailParticleOptions"),
        Map.entry(P + "StormParticle$OrbData", P + "Options/StormParticleOptions"),
        Map.entry(P + "Custom_Poof_Particle$PoofData", P + "Options/CustomPoofParticleOptions"),
        Map.entry(P + "Not_Spin_TrailParticle$NSTData", P + "Options/NotSpinTrailParticleOptions"),
        Map.entry(P + "RoarParticle$RoarData", P + "Options/RoarParticleOptions"),
        Map.entry(P + "CircleLightningParticle$CircleData", P + "Options/CircleLightningParticleOptions"),
        Map.entry(P + "RingParticle$RingData", P + "Options/RingParticleOptions"),
        Map.entry(C + "entity/effect/Boltstrike_Entity", C + "entity/effect/Bolt_strike_Entity"),
        Map.entry(C + "items/Dungeon_Eye/DungeonEyeItem", C + "items/DungeonEyeItem"));
    static final String SHIM = "com/mdvlcraft/binder/compat/traveloptics/CataclysmCompat";
    // constructors whose shape changed: new owner + old descriptor -> shim method
    static final Map<String, String[]> CTORS = Map.of(
        P + "Options/CircleLightningParticleOptions(III)V", new String[]{"circle", "(III)L" + P + "Options/CircleLightningParticleOptions;"},
        P + "Options/RingParticleOptions(FFIFFFFFZL" + P + "RingParticle$EnumRingBehavior;)V",
        new String[]{"ring", "(FFIFFFFFZL" + P + "RingParticle$EnumRingBehavior;)L" + P + "Options/RingParticleOptions;"});
    static final Map<String, String[]> FIELDS = Map.of(
        C + "config/CMConfig.HarbingerHealingMultiplier", new String[]{"harbingerHealingMultiplier", "()D"},
        C + "config/CMConfig.HarbingerLightFire", new String[]{"harbingerLightFire", "()Z"});
    // Violent (Paralyzing) Skreech is only registered with Alex's Mobs, which the pack does not have; it only
    // uses that mod's sounds and particle, so register it anyway with the Warden's sonic boom instead
    static final Map<String, Map<String, String>> STRINGS = Map.of(
        "com/gametechbc/traveloptics/init/TravelopticsSpells", Map.of("alexsmobs", "minecraft"),
        "com/gametechbc/traveloptics/compat/spells/eldritch/ViolentSkreechSpell", Map.of(
            "alexsmobs", "minecraft",
            "skreecher_call", "entity.warden.sonic_charge",
            "skreecher_clap", "entity.warden.sonic_boom",
            "alexsmobs:skulk_boom", "minecraft:sonic_boom"));

    public static void main(String[] args) throws IOException {
        int changed = 0;
        try (ZipInputStream in = new ZipInputStream(new FileInputStream(args[0]));
             ZipOutputStream out = new ZipOutputStream(new FileOutputStream(args[1]))) {
            for (ZipEntry e; (e = in.getNextEntry()) != null; ) {
                byte[] data = in.readAllBytes();
                if (e.getName().endsWith(".class")) {
                    byte[] patched = patch(data);
                    if (patched != data) {
                        changed++;
                        data = patched;
                    }
                }
                if (e.getName().startsWith("META-INF/") && (e.getName().endsWith(".SF") || e.getName().endsWith(".RSA"))) {
                    continue;
                }
                ZipEntry copy = new ZipEntry(e.getName());
                copy.setTime(e.getTime());
                out.putNextEntry(copy);
                out.write(data);
                out.closeEntry();
            }
        }
        System.out.println("patched " + changed + " classes");
    }

    static boolean mentions(byte[] data) {
        String s = new String(data, java.nio.charset.StandardCharsets.ISO_8859_1);
        for (String k : MOVED.keySet()) if (s.contains(k)) return true;
        return s.contains(C + "config/CMConfig") || s.contains("alexsmobs");
    }

    static byte[] patch(byte[] data) {
        if (!mentions(data)) return data;
        ClassNode node = new ClassNode();
        new ClassReader(data).accept(new ClassRemapper(node, new SimpleRemapper(MOVED)), 0);
        node.innerClasses.removeIf(ic -> MOVED.containsValue(ic.name));
        Map<String, String> strings = STRINGS.getOrDefault(node.name, Map.of());
        for (MethodNode m : node.methods) {
            Deque<TypeInsnNode> news = new ArrayDeque<>();
            for (AbstractInsnNode insn : m.instructions.toArray()) {
                if (insn instanceof TypeInsnNode t && t.getOpcode() == Opcodes.NEW) {
                    news.push(t);
                } else if (insn instanceof MethodInsnNode mi && mi.getOpcode() == Opcodes.INVOKESPECIAL && mi.name.equals("<init>")) {
                    TypeInsnNode created = news.isEmpty() ? null : news.peek();
                    if (created != null && created.desc.equals(mi.owner)) {
                        news.pop();
                        String[] shim = CTORS.get(mi.owner + mi.desc);
                        if (shim != null) {
                            AbstractInsnNode dup = created.getNext();
                            if (dup.getOpcode() != Opcodes.DUP) throw new IllegalStateException("NEW without DUP in " + node.name + "." + m.name);
                            for (AbstractInsnNode i = created; i != mi; i = i.getNext())
                                if (i instanceof FrameNode) throw new IllegalStateException("frame inside constructor call in " + node.name + "." + m.name);
                            m.instructions.remove(created);
                            m.instructions.remove(dup);
                            m.instructions.set(mi, new MethodInsnNode(Opcodes.INVOKESTATIC, SHIM, shim[0], shim[1], false));
                        }
                    }
                } else if (insn instanceof LdcInsnNode ldc && ldc.cst instanceof String text && strings.containsKey(text)) {
                    ldc.cst = strings.get(text);
                } else if (insn instanceof FieldInsnNode f && f.getOpcode() == Opcodes.GETSTATIC) {
                    String[] shim = FIELDS.get(f.owner + "." + f.name);
                    if (shim != null) m.instructions.set(f, new MethodInsnNode(Opcodes.INVOKESTATIC, SHIM, shim[0], shim[1], false));
                }
            }
        }
        ClassWriter w = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(w);
        return w.toByteArray();
    }
}
