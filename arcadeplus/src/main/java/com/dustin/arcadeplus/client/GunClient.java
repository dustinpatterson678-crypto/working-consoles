package com.dustin.arcadeplus.client;

import com.dustin.arcadeplus.ArcadePlusMod;
import com.dustin.arcadeplus.GunCore;
import com.dustin.arcadeplus.LightGunItem;
import com.dustin.wcore.emu.CoreFiles;
import com.dustin.wcore.retro.RetroConsoleBlock;
import com.dustin.wcore.retro.RetroConsoleBlockEntity;
import com.dustin.wcore.retro.RetroSystem;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Light gun aiming: Minecraft's own ray from your eyes is intersected with the gun cabinet's screen
 * every frame, and the hit point goes straight to the light-gun core as an absolute gun position.
 */
public final class GunClient {
   // gun cabinet screen in model pixels: centre height, distance in front of the block centre, width, height
   public static final double SY = 21.0, SF = 3.48, SW = 15.4, SH = 12.0;
   private static final int TRIGGER = 1, AUXA = 2, START = 8, COIN = 16;
   private static MethodHandle setGun;
   private static boolean loadFailed;
   private static final List<BlockPos> CABINETS = new ArrayList<>();
   private static int scanTimer, coinSeq = -1, lastButtons = -1, lastX, lastY, lastOff = -1, idleTicks;
   private static boolean wasTrigger;
   private static int aimX, aimY;
   private static boolean aimOff = true;
   private static long aimFrame;

   private GunClient() {
   }

   public static void init() {
      ClientTickEvents.END_CLIENT_TICK.register(GunClient::tick);
      // left click with the gun is the second gun button, not a punch
      ClientPreAttackCallback.EVENT.register((client, player, clicks) -> holding(player));
   }

   static boolean holding(Player p) {
      return p != null && (p.getMainHandItem().getItem() instanceof LightGunItem || p.getOffhandItem().getItem() instanceof LightGunItem);
   }

   private static boolean load() {
      if (setGun != null) return true;
      if (loadFailed || !GunCore.supported()) return false;
      try {
         for (String n : GunCore.PROXIES) {
            if (Files.exists(GunCore.proxyPath(n))) {
               SymbolLookup lib = SymbolLookup.libraryLookup(GunCore.proxyPath(n), Arena.global());
               setGun = Linker.nativeLinker().downcallHandle(lib.find("wcgun_set").orElseThrow(),
                  FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
               return true;
            }
         }
      } catch (Throwable t) {
         ArcadePlusMod.LOGGER.warn("Light gun link failed: {}", t.toString());
      }
      loadFailed = true;
      return false;
   }

   private static void send(int x, int y, int buttons, boolean off) {
      int o = off ? 1 : 0;
      if (x == lastX && y == lastY && buttons == lastButtons && o == lastOff) return;
      lastX = x;
      lastY = y;
      lastButtons = buttons;
      lastOff = o;
      if (!load()) return;
      try {
         setGun.invokeExact(0, x, y, buttons, o);
      } catch (Throwable t) {
         loadFailed = true;
         setGun = null;
      }
   }

   /** Called by the gun cabinet renderer every frame: ray from the eyes onto this cabinet's screen. */
   static void aim(BlockPos pos, Direction facing, float partial) {
      Minecraft mc = Minecraft.getInstance();
      Player p = mc.player;
      if (p == null || !holding(p)) return;
      Vec3 eye = p.getEyePosition(partial), dir = p.getViewVector(partial);
      Vec3 n = new Vec3(facing.getStepX(), 0, facing.getStepZ());
      Vec3 right = new Vec3(0, 1, 0).cross(n);
      Vec3 c = Vec3.atLowerCornerOf(pos).add(0.5, SY / 16.0, 0.5).add(n.scale(SF / 16.0));
      double denom = dir.dot(n);
      if (denom > -1e-4) return;                          // looking away from this screen
      double t = c.subtract(eye).dot(n) / denom;
      if (t <= 0 || t > 10) return;
      Vec3 d = eye.add(dir.scale(t)).subtract(c);
      double u = d.dot(right) / (SW / 16.0) + 0.5, v = 0.5 - d.y / (SH / 16.0);
      boolean off = u < 0 || u > 1 || v < 0 || v > 1;
      if (off && (u < -0.6 || u > 1.6 || v < -0.6 || v > 1.6)) return;   // not even near this cabinet
      aimX = (int)Math.round((Math.max(0, Math.min(1, u)) * 2 - 1) * 32767);
      aimY = (int)Math.round((Math.max(0, Math.min(1, v)) * 2 - 1) * 32767);
      aimOff = off;
      aimFrame = System.nanoTime();
      push(mc);
   }

   private static void push(Minecraft mc) {
      Player p = mc.player;
      boolean trigger = p != null && p.isUsingItem() && p.getUseItem().getItem() instanceof LightGunItem;
      int b = 0;
      if (coinSeq >= 0) {
         b |= coinSeq < 6 ? COIN : coinSeq >= 10 && coinSeq < 16 ? START : 0;
      } else if (trigger) {
         b |= TRIGGER;
      }
      if (mc.options.keyAttack.isDown()) b |= AUXA;
      boolean stale = System.nanoTime() - aimFrame > 250_000_000L;
      send(aimX, aimY, b, aimOff || stale);
   }

   private static void tick(Minecraft mc) {
      Player p = mc.player;
      if (p == null || mc.level == null) return;
      boolean hold = holding(p);
      boolean trigger = hold && p.isUsingItem() && p.getUseItem().getItem() instanceof LightGunItem;
      if (trigger && !wasTrigger) {
         if (p.isShiftKeyDown()) {
            coinSeq = 0;                                  // sneak + trigger: drop a coin and press start
         } else {
            p.playSound(SoundEvents.DISPENSER_LAUNCH, 0.3F, 1.8F);
         }
      }
      wasTrigger = trigger;
      if (coinSeq >= 0 && ++coinSeq > 18) coinSeq = -1;
      if (hold) {
         idleTicks = 0;
         push(mc);
      } else if (++idleTicks == 2) {
         send(0, 0, 0, true);                             // put the gun down: release everything
      }
      // fetch the real arcade core once a gun board goes into a cabinet nearby
      if (++scanTimer % 40 == 0) {
         CABINETS.clear();
         BlockPos base = p.blockPosition();
         for (BlockPos q : BlockPos.betweenClosed(base.offset(-12, -3, -12), base.offset(12, 3, 12))) {
            BlockState s = mc.level.getBlockState(q);
            if ((s.getBlock() == ArcadePlusMod.GUN_CAB.block || s.getBlock() == ArcadePlusMod.NIGHT_SHOOTER.block) && mc.level.getBlockEntity(q) instanceof RetroConsoleBlockEntity be && be.system() != null) {
               CABINETS.add(q.immutable());
               RetroSystem sys = be.system();
               if (sys.core.startsWith("wcgun_")) {
                  String real = sys.core.substring(6);
                  if (!Files.exists(CoreFiles.path(real))) {
                     CoreFiles.ensure(real, msg -> mc.execute(() -> {
                        if (mc.player != null) mc.player.sendSystemMessage(Component.literal("[Light Gun] " + msg).withStyle(ChatFormatting.AQUA));
                     }));
                  }
               }
            }
         }
      }
   }
}
