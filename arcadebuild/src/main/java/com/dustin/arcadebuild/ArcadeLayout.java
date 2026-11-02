package com.dustin.arcadebuild;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * The Nickel Arcade floor plan, in local coordinates: lx runs left/right (+ = right as you walk in), lz runs
 * from the front wall (0) to the back, ly is height (0 = floor). Main hall: lobby + prize booth, three double
 * rows of video cabinets, the redemption floor with every ticket game; behind it a hallway with eight
 * console rooms (slim disc, black box, cartridge, theater, disc, woodgrain & retro, computer lab, handhelds).
 */
public final class ArcadeLayout {

   final List<BuildJob.Op> ops = new ArrayList<>();
   final List<BuildJob.Wire> wires = new ArrayList<>();
   final List<BlockPos> outlets = new ArrayList<>();
   final Set<String> missing = new LinkedHashSet<>();
   private final BlockPos origin;
   private final Direction fwd, right;

   private final BuildingPlan plan;

   ArcadeLayout(BlockPos builder, Direction facing, BuildingPlan plan) {
      this.plan = plan;
      this.fwd = facing;
      this.right = facing.getClockWise();
      this.origin = builder.below().relative(facing);   // floor level, front wall line
   }

   // ---------- coordinates ----------

   BlockPos at(int lx, int ly, int lz) {
      return this.origin.relative(this.right, lx).relative(this.fwd, lz).above(ly);
   }

   /** Local direction -> world: "F" = deeper into the building, "B" = toward the front door, "R"/"L" = right/left. */
   Direction dir(char d) {
      return switch (d) {
         case 'F' -> this.fwd;
         case 'B' -> this.fwd.getOpposite();
         case 'R' -> this.right;
         default -> this.right.getOpposite();
      };
   }

   // ---------- block helpers ----------

   /** A vanilla block by id (colored blocks are grouped differently in 26.3, so look them up by name). */
   private static Block mc(String path) {
      return BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(path));
   }

   private BlockState state(String id) {
      Identifier key = Identifier.parse(id);
      if (!BuiltInRegistries.BLOCK.containsKey(key)) {
         this.missing.add(id);
         return null;
      }
      return BuiltInRegistries.BLOCK.getValue(key).defaultBlockState();
   }

   private static BlockState face(BlockState s, Direction d) {
      if (s.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) return s.setValue(BlockStateProperties.HORIZONTAL_FACING, d);
      if (s.hasProperty(BlockStateProperties.FACING)) return s.setValue(BlockStateProperties.FACING, d);
      return s;
   }

   @SuppressWarnings({"unchecked", "rawtypes"})
   private static Property<DoubleBlockHalf> halfProp(BlockState s) {
      for (Property<?> p : s.getProperties()) {
         if (p.getName().equals("half") && p.getValueClass() == DoubleBlockHalf.class) return (Property)p;
      }
      return null;
   }

   void set(int lx, int ly, int lz, BlockState s) {
      if (s != null) this.ops.add(new BuildJob.Op(this.at(lx, ly, lz), s));
   }

   void set(int lx, int ly, int lz, Block b) {
      this.set(lx, ly, lz, b.defaultBlockState());
   }

   /** Place a (possibly two-tall) block facing a local direction. Returns its world position, or null if not installed. */
   BlockPos put(String id, int lx, int ly, int lz, char facing) {
      BlockState s = this.state(id);
      if (s == null) return null;
      s = face(s, this.dir(facing));
      Property<DoubleBlockHalf> half = halfProp(s);
      if (half != null) {
         this.set(lx, ly, lz, s.setValue(half, DoubleBlockHalf.LOWER));
         this.set(lx, ly + 1, lz, s.setValue(half, DoubleBlockHalf.UPPER));
      } else {
         this.set(lx, ly, lz, s);
      }
      return this.at(lx, ly, lz);
   }

   /** A machine that needs power: placed, and wired to the outlet later. */
   void machine(String id, int lx, int lz, char facing, BlockPos outlet) {
      BlockPos p = this.put(id, lx, 1, lz, facing);
      if (p != null && outlet != null) this.wires.add(new BuildJob.Wire(p, null, outlet));
   }

   final java.util.Map<Long, BlockPos> outletAt = new java.util.HashMap<>();

   /** Floor outlet standing on a hidden power cell in the slab (the breaker switches the cell). */
   BlockPos floorOutlet(int lx, int lz, char facing) {
      BlockState o = this.state("wcore:outlet");
      if (o == null) return null;
      this.set(lx, 0, lz, Blocks.REDSTONE_BLOCK.defaultBlockState());
      this.set(lx, 1, lz, face(o, this.dir(facing)));
      BlockPos p = this.at(lx, 1, lz);
      this.outlets.add(p);
      return p;
   }

   /** The plan outlet nearest to a plan cell. */
   private BlockPos nearestOutlet(int lx, int lz) {
      BlockPos best = null;
      double bd = 1e9;
      for (int[] o : this.plan.outlets()) {
         double d = Math.hypot(o[0] - lx, o[1] - lz);
         if (d < bd) {
            bd = d;
            best = this.at(o[0], 1, o[1]);
         }
      }
      return best;
   }

   /** A machine that needs power: placed, and cabled to the nearest floor outlet. */
   void machine(String id, int lx, int lz, char facing) {
      BlockPos p = this.put(id, lx, 1, lz, facing);
      if (p != null) this.wires.add(new BuildJob.Wire(p, null, this.nearestOutlet(lx, lz)));
   }

   // ---------- the plan ----------

   void generate() {
      BlockState shell = ArcadeBuildMod.SHELL.defaultBlockState(), air = Blocks.AIR.defaultBlockState();
      int sx = this.plan.HX + 2;
      boolean real = this.plan.realShell();
      if (real) {
         this.clearOhWow(air);
      } else {
         // clear the site (and a sidewalk in front) and lay the building's invisible structure
         for (int lz = -5; lz <= this.plan.BACK + 2; lz++) {
            for (int lx = -sx - 1; lx <= sx + 1; lx++) {
               for (int ly = 0; ly <= 14; ly++) {
                  boolean inside = lz >= -1 && lz <= this.plan.BACK + 1 && Math.abs(lx) <= sx;
                  this.set(lx, ly, lz, ly == 0 && !inside ? Blocks.SMOOTH_STONE.defaultBlockState() : air);   // the sidewalk is a solid base under the model
               }
            }
         }
      }
      for (BuildingPlan.Seg g : this.plan.segs()) {
         for (int lx = g.x0(); lx <= g.x1(); lx++)
            for (int ly = g.y0(); ly <= g.y1(); ly++)
               for (int lz = g.z0(); lz <= g.z1(); lz++) {
                  if (real && g.kind().equals("roof") && ly > g.y0()) continue;   // single-ply roof: the fascia sits at y=9
                  BlockState st = shell;
                  if (real) {
                     st = switch (g.kind()) {
                        case "floor" -> Blocks.SMOOTH_STONE.defaultBlockState();
                        case "roof" -> ArcadeBuildMod.CEILING_TILE.defaultBlockState();
                        case "wall" -> ArcadeBuildMod.ARCADE_WALL.defaultBlockState();
                        default -> ArcadeBuildMod.STUCCO.defaultBlockState();
                     };
                  } else if (this.plan.id == 1 && g.kind().equals("floor")) {
                     st = Blocks.SMOOTH_STONE.defaultBlockState();     // never leave a see-through hole under the carpet
                  }
                  this.set(lx, ly, lz, st);
               }
      }
      for (int[] wnd : this.plan.windows()) {
         for (int lx = wnd[0]; lx <= wnd[1]; lx++)
            for (int ly = wnd[2]; ly <= wnd[3]; ly++) {
               if (real) {
                  this.set(lx, ly, -1, air);                       // recessed one block, like a real storefront
                  this.set(lx, ly, 0, ArcadeBuildMod.STOREFRONT_GLASS.defaultBlockState());
               } else {
                  if (this.plan.id == 1) {
                     this.set(lx, ly, -1, air);                    // real glass set into the storefront
                     this.set(lx, ly, 0, ArcadeBuildMod.STOREFRONT_GLASS.defaultBlockState());
                  } else {
                     for (int lz = -1; lz <= 0; lz++) this.set(lx, ly, lz, shell);   // glass is drawn by the model
                  }
               }
            }
      }
      // the building's core block (draws everything) sits in the front wall above the door
      Block facade = this.plan.id == 1 ? ArcadeBuildMod.DUSTIN_FACADE : this.plan.id == 2 ? ArcadeBuildMod.GRAND_FACADE
         : this.plan.id == 3 ? ArcadeBuildMod.DUSTIN_11_FACADE : ArcadeBuildMod.FACADE;
      this.set(FacadeBlockEntity.FX, FacadeBlockEntity.FY, FacadeBlockEntity.FZ, facade.defaultBlockState().setValue(FacadeBlock.FACING, this.fwd));
      if (this.plan.id == 2) {                           // the Grand Hall building: walking in takes you to the hall, nothing to fill
         this.facade = this.at(FacadeBlockEntity.FX, FacadeBlockEntity.FY, FacadeBlockEntity.FZ);
         return;
      }
      // ceiling lights (invisible light sources under the drawn light panels)
      for (int[] l : this.plan.lights()) {
         this.set(l[0], BuildingPlan.CEIL - 1, l[1], Blocks.LIGHT.defaultBlockState().setValue(net.minecraft.world.level.block.LightBlock.LEVEL, 15));
      }
      for (int[] o : this.plan.outlets()) this.floorOutlet(o[0], o[1], (char)o[2]);
      // main breaker by the front door
      int[] br = this.plan.breaker();
      this.set(br[0], 1, br[1], ArcadeBuildMod.BREAKER.defaultBlockState().setValue(BreakerBlock.FACING, this.dir((char)br[2])).setValue(BreakerBlock.POWERED, true));
      this.breaker = this.at(br[0], 1, br[1]);
      this.facade = this.at(FacadeBlockEntity.FX, FacadeBlockEntity.FY, FacadeBlockEntity.FZ);
      if (this.plan.id == 1) {
         this.dustins();
      } else if (this.plan.id == 3) {
         this.ohWow();
      } else {
         this.lobby();
         this.videoRows();
         this.redemption();
         this.annex();
      }
   }

   /** Clear a realistic strip-mall site: the building, a sidewalk and a parking apron. */
   private void clearOhWow(BlockState air) {
      int sx = this.plan.HX + 2;
      for (int lz = -2; lz <= this.plan.BACK + 4; lz++) {
         for (int lx = -sx - 3; lx <= sx + 3; lx++) {
            for (int ly = 0; ly <= 14; ly++) {
               this.set(lx, ly, lz, ly == 0 ? Blocks.SMOOTH_STONE.defaultBlockState() : air);
            }
         }
      }
      for (int lz = -17; lz <= -2; lz++) {
         for (int lx = -sx - 5; lx <= sx + 5; lx++) {
            for (int ly = 0; ly <= 14; ly++) {
               this.set(lx, ly, lz, ly == 0 ? Blocks.SMOOTH_STONE.defaultBlockState() : air);
            }
         }
      }
   }

   /** Dustin's Arcade: cabinets along both walls and a centre island, a few classics at the back, office and power room. */
   private void dustins() {
      // Everything lines the walls; the middle of the floor is left empty for you to fill.
      // left wall: a row of redemption games, one of each
      String[] leftRow = {"arcadeplus:whackamole", "arcadeplus:gators", "arcadeplus:echo", "arcadeplus:clowns", "arcadeplus:basketball", "arcadeplus:coinpusher"};
      for (int i = 0; i < 6; i++) this.machine(leftRow[i], -10, 6 + i, 'R');
      // the dim pinball alley: three tables and the big wheel
      String[] tables = {"arcadeplus:pinball_galaxy", "arcadeplus:pinball_haunted", "arcadeplus:pinball_speedway"};
      for (int lz = 13; lz <= 15; lz++) this.machine(tables[lz - 13], -9, lz, 'R');
      this.machine("arcadeplus:bigwheel", -9, 17, 'R');
      // right wall: two alcoves of video cabinets, no two alike
      String[] right = {"arcadeplus:racing_cabinet", "arcadeplus:gun_cabinet", "arcadeplus:brawler_cabinet", "arcadeplus:candy_cabinet", "arcadeplus:retro_upright", "arcadeplus:dustin_multi"};
      int[] rz = {11, 12, 13, 15, 16, 17};
      for (int i = 0; i < 6; i++) this.machine(right[i], 10, rz[i], 'L');
      // back wall: ticket games (kept clear of the archway into the console lounge)
      this.machine("arcadeplus:cyclone", -8, 20, 'B');
      this.machine("arcadeplus:stacker", -7, 20, 'B');
      this.machine("arcadeplus:skeeball", -5, 20, 'B');
      this.machine("arcadeplus:skeeball", -4, 20, 'B');
      this.machine("arcadeplus:clawmachine", 4, 20, 'B');
      this.machine("arcadeplus:drumwheel", 5, 20, 'B');
      this.machine("arcadeplus:hockey", -6, 20, 'B');
      this.machine("arcadeplus:plinko", 6, 20, 'B');
      this.machine("arcadeplus:striker", 7, 20, 'B');
      this.machine("arcadeplus:locker", 8, 20, 'B');
      this.machine("arcadeplus:keyvault", -9, 18, 'R');
      this.machine("arcadeplus:dropzone", -9, 19, 'R');
      this.machine("arcadeplus:speedlights", 10, 19, 'L');
      // ticket redemption corner: the booth's prize wall stands against the right wall, ticket counter beside it
      for (int lz = 4; lz <= 8; lz++) this.put("arcadeplus:prize_booth", 8, 1, lz, 'L');   // glass prize counter you redeem at
      this.put("arcadeplus:prize_wall", 10, 1, 6, 'L');                                     // the real prizes on the wall behind
      this.put("wcore:ticket_counter", 10, 1, 9, 'L');
      this.put("arcadebuild:giant_teddy", 10, 1, 4, 'L');
      // lounge by the door
      this.put("wcore:change_machine", -10, 1, 3, 'R');
      this.put("wcore:drink_machine", -10, 1, 4, 'R');
      this.put("wcore:arcade_directory", -5, 1, 5, 'B');
      this.put("wcore:trash_can", 4, 1, 5, 'B');
      // ---- console lounge behind the archway: a station per console, sofas facing each wall ----
      String[] leftWall = {"ps2:console", "disc:ps1", "nintendo:n64", "nintendo:snes"};
      String[] rightWall = {"sega:genesis", "sega:saturn", "nintendo:nes", "retro:neogeo"};
      BlockPos lo = this.at(-10, 1, 23), ro = this.at(10, 1, 23), bo = this.at(-1, 1, 31);
      for (int i = 0; i < 4; i++) {
         this.console(leftWall[i], -10, 24 + i * 2, 'R', lo, true);
         this.console(rightWall[i], 10, 24 + i * 2, 'L', ro, true);
      }
      this.console("retro:atari2600", -4, 31, 'B', bo, true);
      this.console("stream:smart_tv", 0, 31, 'B', bo, true);
      this.console("classic:sms", 3, 31, 'B', bo, true);
      for (int lz = 25; lz <= 29; lz++) {
         this.put("arcadebuild:sofa", -6, 1, lz, 'L');
         this.put("arcadebuild:sofa", 6, 1, lz, 'R');
      }
      for (int lx = -2; lx <= 2; lx++) this.put("arcadebuild:sofa", lx, 1, 27, 'F');
      this.put("arcadebuild:coffee_table", -1, 1, 29, 'F');
      this.put("arcadebuild:coffee_table", 1, 1, 29, 'F');
      this.put("arcadebuild:potted_palm", -8, 1, 23, 'R');
      this.put("arcadebuild:potted_palm", 8, 1, 23, 'L');
      // ---- the office: manager's desk and chair, files, security monitors, water cooler ----
      this.put("arcadebuild:manager_desk", -5, 1, 36, 'B');
      this.put("arcadebuild:office_chair", -5, 1, 37, 'B');
      this.put("arcadebuild:filing_cabinet", -2, 1, 39, 'B');
      this.put("arcadebuild:filing_cabinet", -1, 1, 39, 'B');
      this.put("arcadebuild:security_monitors", -8, 1, 39, 'B');
      this.put("arcadebuild:water_cooler", -2, 1, 33, 'F');
      this.put("arcadebuild:potted_palm", -9, 1, 33, 'F');
      // ---- the power room: wall panels and the giant power strip every cord runs to ----
      for (int lx = 3; lx <= 7; lx += 2) this.put("arcadebuild:electrical_panel", lx, 1, 39, 'B');
      this.put("arcadebuild:electrical_panel", 10, 1, 35, 'L');
      this.put("arcadebuild:electrical_panel", 10, 1, 37, 'L');
      this.set(4, 1, 36, ArcadeBuildMod.POWER_STRIP.defaultBlockState().setValue(PowerStripBlock.FACING, this.dir('B')));
      // ---- out front and round the back ----
      for (int lx : new int[]{-5, -4, 4, 5}) this.put("arcadebuild:bench", lx, 1, -4, 'B');
      this.put("arcadebuild:planter_tree", -8, 1, -4, 'B');
      this.put("arcadebuild:planter_tree", 8, 1, -4, 'B');
      this.put("arcadebuild:lamp_post", -13, 1, -5, 'B');
      this.put("arcadebuild:lamp_post", 13, 1, -5, 'B');
      this.put("arcadebuild:dumpster", 6, 1, this.plan.BACK + 2, 'F');
   }

   /** Dustin's Arcade 1.1: the existing proven machine floor, plus a console lounge and realistic rooms. */
   private void ohWow() {
      this.lobby();
      this.videoRows();
      this.redemption();

      // A centre island of six classic cabinets, back to back, fills the redemption floor without blocking the walls.
      String[] island = {"arcadeplus:retro_upright", "arcadeplus:brawler_cabinet", "arcadeplus:versus_fighter",
         "arcadeplus:candy_cabinet", "arcadeplus:gun_cabinet", "arcadeplus:racing_cabinet"};
      for (int i = 0; i < island.length; i++) {
         this.machine(island[i], -4 + i, 22, 'B');
         this.machine(island[island.length - 1 - i], -4 + i, 23, 'F');
      }

      // ---- console lounge: the old eight separate rooms are now one open, social room ----
      BlockPos lo = this.at(-11, 1, 31), ro = this.at(11, 1, 31), bo = this.at(0, 1, 39);
      String[] left = {"ps2:console", "disc:ps1", "nintendo:n64"};
      String[] right = {"sega:genesis", "sega:saturn", "retro:neogeo"};
      for (int i = 0; i < 3; i++) {
         this.console(left[i], -13, 31 + i * 2, 'R', lo);
         this.console(right[i], 13, 31 + i * 2, 'L', ro);
      }
      this.console("retro:atari2600", -2, 41, 'B', bo);
      this.console("stream:smart_tv", 0, 41, 'B', bo);
      this.console("classic:sms", 2, 41, 'B', bo);
      for (int lx = -2; lx <= 2; lx++) this.put("arcadebuild:sofa", lx, 1, 33, 'F');
      this.put("arcadebuild:coffee_table", -1, 1, 34, 'F');
      this.put("arcadebuild:coffee_table", 1, 1, 34, 'F');
      this.put("arcadebuild:potted_palm", -5, 1, 31, 'F');
      this.put("arcadebuild:potted_palm", 5, 1, 31, 'F');

      // ---- party room: two stations and a lounge corner ----
      BlockPos po = this.at(-11, 1, 38);
      this.console("nintendo:snes", -13, 38, 'R', po);
      this.console("retro:32x", -13, 40, 'R', po);
      this.console("classic:gamegear", -6, 41, 'B', this.at(-6, 1, 39));
      this.put("arcadebuild:sofa", -8, 1, 38, 'B');
      this.put("arcadebuild:coffee_table", -8, 1, 39, 'F');

      // ---- office / power room: desk, files, monitors, panels, the giant strip ----
      this.put("arcadebuild:manager_desk", 8, 1, 40, 'B');
      this.put("arcadebuild:office_chair", 8, 1, 39, 'B');
      this.put("arcadebuild:filing_cabinet", 12, 1, 41, 'B');
      this.put("arcadebuild:security_monitors", 11, 1, 41, 'B');
      this.put("arcadebuild:water_cooler", 6, 1, 40, 'F');
      this.put("arcadebuild:electrical_panel", 13, 1, 37, 'L');
      this.console("stream:smart_tv", 13, 39, 'L', this.at(11, 1, 39));
      int[] st = this.plan.strip();
      if (st != null) this.set(st[0], 1, st[1], ArcadeBuildMod.POWER_STRIP.defaultBlockState().setValue(PowerStripBlock.FACING, this.dir('B')));

      // ---- outside: benches, planters, lamps and the dumpster ----
      for (int lx : new int[]{-5, -4, 4, 5}) this.put("arcadebuild:bench", lx, 1, -3, 'B');
      this.put("arcadebuild:planter_tree", -8, 1, -3, 'F');
      this.put("arcadebuild:planter_tree", 8, 1, -3, 'F');
      this.put("arcadebuild:lamp_post", -14, 1, -5, 'F');
      this.put("arcadebuild:lamp_post", 14, 1, -5, 'F');
      this.put("arcadebuild:dumpster", 8, 1, this.plan.BACK + 2, 'F');
   }


   private final List<int[]> hallOutlets = new ArrayList<>();

   private void hallOutlet(int lx, int lz, char facing) {
      if (this.floorOutlet(lx, lz, facing) != null) this.hallOutlets.add(new int[]{lx, lz});
   }

   private BlockPos nearestHallOutlet(int lx, int lz) {
      BlockPos best = null;
      double bd = 1e9;
      for (int[] o : this.hallOutlets) {
         double d = Math.hypot(o[0] - lx, o[1] - lz);
         if (d < bd) {
            bd = d;
            best = this.at(o[0], 1, o[1]);
         }
      }
      return bd <= 12 ? best : null;
   }

   private void hm(String id, int lx, int lz, char facing) {
      this.machine(id, lx, lz, facing, this.nearestHallOutlet(lx, lz));
   }

   /** A row of machines along lz (step dz), with an outlet behind or beside every few machines. */
   private void hallRow(String[] ids, int lx, int lz0, int dz, char facing) {
      for (int i = 0; i < ids.length; i++) if (ids[i] != null) this.hm(ids[i], lx, lz0 + i * dz, facing);
   }

   /** A row of machines along lx. */
   private void hallRowX(String[] ids, int lx0, int dx, int lz, char facing) {
      for (int i = 0; i < ids.length; i++) if (ids[i] != null) this.hm(ids[i], lx0 + i * dx, lz, facing);
   }

   /** The Grand Hall: about 51 x 64 blocks of arcade floor plus a console lounge, every machine on the floor and plugged in. */
   void grandHall() {
      BlockState shell = ArcadeBuildMod.HALL_SHELL.defaultBlockState();
      int HX = GrandHall.HX, D = GrandHall.DEPTH, C = GrandHall.CEIL, LZ = GrandHall.LOUNGE;
      for (int lz = -1; lz <= D; lz++) {
         for (int lx = -HX - 1; lx <= HX + 1; lx++) {
            this.set(lx, 0, lz, shell);
            this.set(lx, C, lz, shell);
            boolean edge = lz == -1 || lz == D || Math.abs(lx) == HX + 1;
            boolean divider = lz == LZ && !(Math.abs(lx) <= GrandHall.ARCH);
            for (int ly = 1; ly < C; ly++) {
               boolean archHead = lz == LZ && Math.abs(lx) <= GrandHall.ARCH && ly > GrandHall.ARCH_H;
               this.set(lx, ly, lz, edge || divider || archHead ? shell : Blocks.AIR.defaultBlockState());
            }
            for (int ly = -10; ly <= -1; ly++) {                     // under the floor: just ground, like anywhere else
               int hsh = (lx * 73856093) ^ (lz * 19349663) ^ (ly * 83492791);
               Block b = ly >= -2 ? Blocks.DIRT : ly >= -6 ? (hsh & 63) == 0 ? Blocks.COAL_ORE : Blocks.STONE : ly >= -9 ? (hsh & 127) == 0 ? Blocks.DEEPSLATE_IRON_ORE : Blocks.DEEPSLATE : Blocks.BEDROCK;
               this.set(lx, ly, lz, b.defaultBlockState());
            }
         }
      }
      // light: invisible light sources a few blocks up, on a grid (the drawn ceiling panels are just the look)
      for (int lz = 1; lz < D; lz += 4) {
         for (int lx = -HX + 1; lx <= HX - 1; lx += 4) {
            this.set(lx, 4, lz, Blocks.LIGHT.defaultBlockState().setValue(net.minecraft.world.level.block.LightBlock.LEVEL, 15));
         }
      }
      this.set(-GrandHall.CORE_LX, GrandHall.CORE_LY, GrandHall.CORE_LZ, ArcadeBuildMod.HALL_CORE.defaultBlockState());
      // ---- outlets first, so every machine can be cabled to the nearest one ----
      for (int lz = 4; lz <= 48; lz += 8) {
         this.hallOutlet(-HX, lz, 'R');                  // behind the left wall row
         this.hallOutlet(HX, lz + 4, 'L');               // behind the right wall row
      }
      int[][] islands = {{-9, 9}, {9, 9}, {0, 15}, {-6, 36}, {-9, 21}, {9, 21}, {0, 41}};
      for (int[] is : islands) this.hallOutlet(is[0], is[1], 'F');
      // ---- lobby: change, snacks, the redemption booth ----
      this.put("wcore:change_machine", -5, 1, 0, 'F');
      this.put("wcore:change_machine", 5, 1, 0, 'F');
      this.put("wcore:snack_machine", -8, 1, 0, 'F');
      this.put("wcore:drink_machine", -9, 1, 0, 'F');
      this.put("wcore:trash_can", -4, 1, 0, 'F');
      this.put("wcore:trash_can", 4, 1, 0, 'F');
      this.put("wcore:arcade_directory", 7, 1, 0, 'F');
      this.put("arcadebuild:potted_palm", -3, 1, 0, 'F');
      this.put("arcadebuild:potted_palm", 3, 1, 0, 'F');
      // front right: two pool tables (3 long, 2 wide)
      this.hm("arcadeplus:pool", 20, 4, 'L');
      this.hm("arcadeplus:pool", 20, 9, 'L');
      // Every machine appears once along the walls, the favourites once more out on the floor: no rows of clones.
      // ---- left wall: video cabinets, the pinball alley, then the skill games ----
      String[] video = {"arcadeplus:racing_cabinet", "arcadeplus:racing_cabinet", "arcadeplus:gun_cabinet", "arcadeplus:gun_cabinet", "arcadeplus:brawler_cabinet",
         "arcadeplus:brawler_cabinet", "arcadeplus:candy_cabinet", "arcadeplus:candy_cabinet", "arcadeplus:retro_upright", "arcadeplus:dustin_multi", "arcadeplus:turbo_moto", "arcadeplus:versus_fighter"};
      this.hallRow(video, -HX + 1, 2, 1, 'R');
      String[] pins = {"arcadeplus:pinball_galaxy", "arcadeplus:pinball_haunted", "arcadeplus:pinball_pirate", "arcadeplus:pinball_speedway", "arcadeplus:pinball"};
      this.hallRow(pins, -HX + 1, 17, 1, 'R');
      String[] leftBack = {"arcadeplus:basketball", "arcadeplus:basketball", null, "arcadeplus:lanes", "arcadeplus:lanes", null, null,
         "arcadeplus:whackamole", "arcadeplus:gators", "arcadeplus:clowns", "arcadeplus:echo", "arcadeplus:speedlights", "arcadeplus:meteor", null, null,
         "arcadeplus:skeeball", "arcadeplus:skeeball", null, null, "arcadeplus:coinpusher", "arcadeplus:coinpusher"};
      this.hallRow(leftBack, -HX + 1, 24, 1, 'R');
      // ---- right wall (after the booth): the big-prize and luck games ----
      String[] right = {"arcadeplus:clawmachine", "arcadeplus:clawmachine", null, "arcadeplus:snipit", "arcadeplus:keyvault", "arcadeplus:locker", null,
         "arcadeplus:bigwheel", "arcadeplus:drumwheel", "arcadeplus:cyclone", "arcadeplus:stacker", null, "arcadeplus:plinko", "arcadeplus:striker",
         "arcadeplus:dropzone", null, "arcadeplus:rocketrace", "arcadeplus:meteor", "arcadeplus:speedlights"};
      this.hallRow(right, HX - 1, 15, 1, 'L');
      // ---- the floor: two islands of favourites, air hockey, a chill-out ring under the big halo, pinball row ----
      String[] isA1 = {"arcadeplus:rocketrace", "arcadeplus:plinko", "arcadeplus:striker", "arcadeplus:dropzone"};
      String[] isA2 = {"arcadeplus:whackamole", "arcadeplus:gators", "arcadeplus:clowns", "arcadeplus:echo"};
      this.hallRowX(isA1, -11, 1, 8, 'B');
      this.hallRowX(isA2, -11, 1, 10, 'F');
      String[] isB1 = {"arcadeplus:keyvault", "arcadeplus:locker", "arcadeplus:snipit", "arcadeplus:bigwheel"};
      String[] isB2 = {"arcadeplus:drumwheel", "arcadeplus:cyclone", "arcadeplus:stacker", "arcadeplus:beatblaster"};
      this.hallRowX(isB1, 8, 1, 8, 'B');
      this.hallRowX(isB2, 8, 1, 10, 'F');
      this.hm("arcadeplus:hockey", -2, 15, 'B');
      this.hm("arcadeplus:hockey", 2, 15, 'B');
      for (int lx = -2; lx <= 2; lx++) {
         this.put("arcadebuild:sofa", lx, 1, 25, 'F');
         this.put("arcadebuild:sofa", lx, 1, 29, 'B');
      }
      this.put("arcadebuild:coffee_table", -1, 1, 27, 'F');
      this.put("arcadebuild:coffee_table", 1, 1, 27, 'F');
      for (int[] pp : new int[][]{{-4, 25}, {4, 25}, {-4, 29}, {4, 29}}) this.put("arcadebuild:potted_palm", pp[0], 1, pp[1], 'F');
      String[] isPin = {"arcadeplus:pinball_speedway", "arcadeplus:pinball", "arcadeplus:pinball_galaxy", "arcadeplus:pinball_haunted", "arcadeplus:pinball_pirate"};
      this.hallRowX(isPin, -11, 1, 36, 'B');                         // one row only: the tables are two blocks long
      // the Dustin's Arcade cabinet line: two video islands and a centre row (wide cabinets get a gap)
      this.hallRowX(new String[]{"arcadeplus:retro_upright", "arcadeplus:versus_fighter", "arcadeplus:dustin_multi", null, "arcadeplus:night_shooter", null, "arcadeplus:turbo_moto"}, -12, 1, 20, 'B');
      this.hallRowX(new String[]{"arcadeplus:dustin_multi", "arcadeplus:versus_fighter", "arcadeplus:retro_upright", null, "arcadeplus:star_pilot", null, "arcadeplus:night_shooter"}, -12, 1, 22, 'F');
      this.hallRowX(new String[]{"arcadeplus:turbo_moto", null, "arcadeplus:star_pilot", null, "arcadeplus:racing_cabinet", "arcadeplus:candy_cabinet", "arcadeplus:brawler_cabinet"}, 6, 1, 20, 'B');
      this.hallRowX(new String[]{"arcadeplus:powerpunch", "arcadeplus:deepsea", "arcadeplus:gun_cabinet", null, "arcadeplus:racing_cabinet", "arcadeplus:candy_cabinet", "arcadeplus:brawler_cabinet"}, 6, 1, 22, 'F');
      this.hallRowX(new String[]{"arcadeplus:retro_upright", "arcadeplus:dustin_multi", "arcadeplus:versus_fighter", null, "arcadeplus:gun_cabinet", "arcadeplus:beatblaster", "arcadeplus:powerpunch"}, -3, 1, 40, 'B');
      // a rest area on the right
      for (int lz = 40; lz <= 45; lz++) this.put("arcadebuild:sofa", HX - 2, 1, lz, 'L');
      for (int lz : new int[]{39, 46}) this.put("arcadebuild:potted_palm", HX - 2, 1, lz, 'L');
      this.put("wcore:snack_machine", HX - 1, 1, 49, 'B');
      this.put("wcore:drink_machine", HX - 2, 1, 49, 'B');
      // ---- behind the archway: the prize room, the manager's office and the power room ----
      int PX = 13;                                                   // partition wall (layout x), doors at z 54 and 60
      for (int lz = GrandHall.LOUNGE + 1; lz < D; lz++) {
         for (int ly = 1; ly < C; ly++) {
            boolean door = (lz == 54 || lz == 60) && ly <= 3;
            if (!door) this.set(PX, ly, lz, shell);
         }
      }
      for (int lx = PX + 1; lx <= HX; lx++) for (int ly = 1; ly < C; ly++) this.set(lx, ly, 58, shell);
      // prize room
      this.put("arcadeplus:prize_stand", -4, 1, 59, 'B');
      this.put("wcore:ticket_counter", 6, 1, 53, 'B');
      for (int lz : new int[]{54, 58}) this.put("arcadeplus:prize_wall", -HX, 1, lz, 'R');
      this.put("arcadeplus:prize_wall", 11, 1, 57, 'L');
      String[][] plush = {{"arcadeplus:jumbo_panda", "-11", "62"}, {"arcadeplus:golden_trophy", "3", "62"}, {"arcadeplus:lava_lamp", "4", "62"},
         {"arcadeplus:teddy_bear", "-12", "53"}, {"arcadeplus:dino_plush", "9", "62"}, {"arcadeplus:bunny_plush", "-14", "62"}};
      for (String[] pl : plush) this.put(pl[0], Integer.parseInt(pl[1]), 1, Integer.parseInt(pl[2]), 'B');
      for (int[] pp : new int[][]{{-8, 52}, {8, 52}, {-20, 62}, {11, 62}}) this.put("arcadebuild:potted_palm", pp[0], 1, pp[1], 'F');
      for (int lx = -18; lx <= -15; lx++) this.put("arcadebuild:sofa", lx, 1, 56, 'F');
      // the manager's office
      this.put("arcadebuild:manager_desk", 19, 1, 55, 'L');
      this.put("arcadebuild:office_chair", 20, 1, 55, 'L');
      this.put("arcadebuild:filing_cabinet", HX, 1, 52, 'L');
      this.put("arcadebuild:filing_cabinet", HX, 1, 53, 'L');
      this.put("arcadebuild:security_monitors", HX, 1, 55, 'L');
      this.put("arcadebuild:water_cooler", PX + 1, 1, 57, 'R');
      this.put("arcadebuild:potted_palm", PX + 1, 1, 52, 'R');
      // the power room: panels, the main breaker and the giant power strip every cord runs to
      for (int lz : new int[]{59, 61, 63}) this.put("arcadebuild:electrical_panel", HX, 1, lz, 'L');
      this.set(19, 1, 62, ArcadeBuildMod.POWER_STRIP.defaultBlockState().setValue(PowerStripBlock.FACING, this.dir('B')));
      this.set(PX + 1, 1, 62, ArcadeBuildMod.BREAKER.defaultBlockState().setValue(BreakerBlock.FACING, this.dir('R')).setValue(BreakerBlock.POWERED, true));
      this.set(16, 1, 63, shell);                                    // stock crates (drawn by the hall model)
      this.set(16, 2, 63, shell);
      this.set(17, 1, 63, shell);
      this.breaker = this.at(PX + 1, 1, 62);
      this.facade = this.at(-GrandHall.CORE_LX, GrandHall.CORE_LY, GrandHall.CORE_LZ);
   }

   BlockPos breaker, facade;

   private void lobby() {
      int hx = this.plan.HX;
      this.put("arcadeplus:prize_booth", hx - 2, 1, 4, 'L');       // counter faces the room, prize wall behind
      this.put("wcore:change_machine", -hx, 1, 3, 'R');
      this.put("wcore:ticket_counter", -hx, 1, 5, 'R');
      this.put("wcore:snack_machine", -hx + 3, 1, 1, 'F');
      this.put("wcore:drink_machine", -hx + 5, 1, 1, 'F');
      this.put("wcore:trash_can", 3, 1, 1, 'F');
      this.put("wcore:arcade_directory", 4, 1, 3, 'B');
   }

   /** Two double rows of video cabinets, back to back. */
   private void videoRows() {
      String[][] rows = {
         {"arcadeplus:retro_upright", "arcadeplus:brawler_cabinet", "arcadeplus:versus_fighter", "arcadeplus:candy_cabinet"},
         {"arcadeplus:racing_cabinet", "arcadeplus:gun_cabinet", "arcadeplus:racing_cabinet", "arcadeplus:gun_cabinet"},
      };
      int[] cabX = {-8, -7, -6, -5, 5, 6, 7, 8};
      int[] zA = {9, 15};
      for (int r = 0; r < 2; r++) {
         for (int k = 0; k < cabX.length; k++) {
            String id = rows[r][k / 2];
            this.machine(id, cabX[k], zA[r], 'B');
            this.machine(id, cabX[k], zA[r] + 1, 'F');
         }
      }
   }

   /** The redemption floor: ticket games along both walls, pinball and light guns at the back. */
   private void redemption() {
      int w = this.plan.HX - 1;
      String[] left = {"arcadeplus:skeeball", "arcadeplus:skeeball", null, "arcadeplus:basketball", null, "arcadeplus:whackamole", "arcadeplus:gators", "arcadeplus:clowns", "arcadeplus:echo"};
      String[] right = {"arcadeplus:clawmachine", "arcadeplus:clawmachine", null, "arcadeplus:coinpusher", "arcadeplus:stacker", "arcadeplus:cyclone", "arcadeplus:bigwheel", "arcadeplus:drumwheel", "arcadeplus:coinpusher"};
      for (int i = 0; i < left.length; i++) {
         if (left[i] != null) this.machine(left[i], -w, 20 + i, 'R');
         if (right[i] != null) this.machine(right[i], w, 20 + i, 'L');
      }
      this.machine("arcadeplus:pinball", -8, 28, 'B');
      this.machine("arcadeplus:pinball_pirate", -6, 28, 'B');
      this.machine("arcadeplus:gun_cabinet", 6, 29, 'B');
      this.machine("arcadeplus:gun_cabinet", 8, 29, 'B');
   }

   // ---------- console rooms ----------

   private record Room(String name, String[] consoles) {}

   private void annex() {
      Room[] right = {
         new Room("Slim Disc", new String[]{"ps2:console", "ps2:console", "ps2:console"}),
         new Room("Black Box", new String[]{"sega:genesis", "sega:saturn", "retro:32x", "retro:segacd", "classic:sms", "classic:gamegear"}),
         new Room("Cartridge", new String[]{"nintendo:nes", "nintendo:snes", "nintendo:n64", "retro:vb", "retro:nds"}),
         new Room("Theater", new String[]{"stream:smart_tv"}),
      };
      Room[] left = {
         new Room("Disc", new String[]{"disc:ps1", "disc:tdo", "disc:pcfx", "retro:neogeo", "retro:pce"}),
         new Room("Woodgrain & Retro", new String[]{"retro:atari2600", "retro:atari7800", "retro:jaguar", "retro:colecovision", "retro:intellivision", "retro:vectrex"}),
         new Room("Computer Lab", new String[]{"classic:c64", "classic:amiga", "classic:atarist", "classic:dospc", "classic:zxspectrum", "classic:atari800"}),
         new Room("Handhelds", new String[]{"retro:lynx", "retro:ngp", "retro:wonderswan", "retro:supervision", "retro:pokemonmini", "retro:gameandwatch"}),
      };
      for (int k = 0; k < BuildingPlan.NickelPlan.ROOMS; k++) {
         this.room(right[k], k, 1);
         this.room(left[k], k, -1);
      }
   }

   private void room(Room room, int k, int side) {
      int za = ((BuildingPlan.NickelPlan)this.plan).roomStart(k), zb = za + 4;
      int xOut = this.plan.HX * side;
      char toHall = side > 0 ? 'L' : 'R';
      List<int[]> slots = new ArrayList<>();
      for (int lz = za; lz <= zb; lz += 2) slots.add(new int[]{xOut, lz, toHall});         // along the outer wall
      for (int lx = 5; lx <= 11; lx += 3) slots.add(new int[]{lx * side, zb, 'B'});        // along the far wall
      int n = 0;
      BlockPos outlet = this.at(8 * side, 1, za + 2);
      for (String id : room.consoles) {
         if (n >= slots.size()) break;
         int[] s = slots.get(n++);
         this.console(id, s[0], s[1], (char)s[2], outlet);
      }
      this.put("wcore:arcade_stool", 6 * side, 1, za + 1, toHall == 'L' ? 'R' : 'L');
   }

   /** A console on a TV stand with a TV, all cabled to the outlet. Built-in-screen machines skip the TV. */
   private void console(String id, int lx, int lz, char facing, BlockPos outlet) {
      this.console(id, lx, lz, facing, outlet, false);
   }

   /** beside: the console sits up on a second media console next to the TV instead of on the floor. */
   private void console(String id, int lx, int lz, char facing, BlockPos outlet, boolean beside) {
      BlockState probe = this.state(id);
      if (probe == null) return;
      boolean builtIn = probe.getBlock() instanceof com.dustin.wcore.retro.RetroConsoleBlock rb && rb.builtInScreen;
      if (id.equals("stream:smart_tv") || builtIn) {
         this.put("arcadebuild:media_console", lx, 1, lz, facing);
         BlockPos p = this.put(id, lx, 2, lz, facing);
         if (p != null && outlet != null) this.wires.add(new BuildJob.Wire(p, null, outlet));
         return;
      }
      // TV on the stand against the wall, console on the floor one step toward the room
      this.put("arcadebuild:media_console", lx, 1, lz, facing);
      BlockPos tv = this.put("wcore:tv", lx, 2, lz, facing);
      Direction d = this.dir(facing);
      if (beside) {
         boolean alongX = d == this.fwd || d == this.fwd.getOpposite();
         int sx = alongX ? lx + 1 : lx, sz = alongX ? lz : lz + 1;
         this.put("arcadebuild:media_console", sx, 1, sz, facing);
         BlockPos c = this.put(id, sx, 2, sz, facing);
         if (c != null && outlet != null) this.wires.add(new BuildJob.Wire(c, tv, outlet));
         return;
      }
      int cx = lx + (d == this.right ? 1 : d == this.right.getOpposite() ? -1 : 0);
      int cz = lz + (d == this.fwd ? 1 : d == this.fwd.getOpposite() ? -1 : 0);
      BlockPos c = this.put(id, cx, 1, cz, facing);
      if (c != null && outlet != null) this.wires.add(new BuildJob.Wire(c, tv, outlet));
   }
}
