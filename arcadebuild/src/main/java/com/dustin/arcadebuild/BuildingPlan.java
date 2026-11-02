package com.dustin.arcadebuild;

import java.util.ArrayList;
import java.util.List;

/**
 * A building's shape and finish, shared by the server (which fills it with invisible collision blocks) and the
 * client (which draws it as one model). Cells are in local plan coordinates: lx right, ly up (0 = floor slab),
 * lz into the building (0 = inner front wall, -1 = outer skin). A cell spans [l-0.5, l+0.5] in x and z.
 * Two buildings: the Nickel Arcade and Dustin's Arcade.
 */
public abstract class BuildingPlan {
   /** A solid region of cells (inclusive). Kinds: floor, roof, skin, front, wall. */
   public record Seg(int x0, int x1, int y0, int y1, int z0, int z1, int color, String kind) {}
   /** A textured surface. kind: F floor (faces up), C ceiling (faces down), X wall along x at depth c, Z wall along z at x = c. */
   public record Surface(String tex, char kind, double c, double a0, double a1, double b0, double b1, double tile, double tileH, int color, int light) {}
   /** A coloured box (local continuous coords). light: 0 outside, 1 inside, 2 glows while powered. */
   public record Box(double x0, double y0, double z0, double x1, double y1, double z1, int color, int light) {}

   public static final int OUT = 0, IN = 1, GLOW = 2;
   public static final int CEIL = 8;
   public final int id, HX, HALL_BACK, BACK;
   public final String name, sign;
   public final int signA, signB, awningA, awningB;
   private List<Seg> segs;
   private List<Surface> surfaces;
   private List<Box> boxes;

   protected BuildingPlan(int id, String name, int hx, int hallBack, int back, String sign, int signA, int signB, int awningA, int awningB) {
      this.id = id;
      this.name = name;
      this.HX = hx;
      this.HALL_BACK = hallBack;
      this.BACK = back;
      this.sign = sign;
      this.signA = signA;
      this.signB = signB;
      this.awningA = awningA;
      this.awningB = awningB;
   }

   public static final BuildingPlan NICKEL = new NickelPlan();
   public static final BuildingPlan DUSTIN = new DustinPlan();
   /** Dustin's Grand Hall: its own building; the front doors lead into the Grand Hall. */
   public static final BuildingPlan GRAND = new GrandPlan();
   /** Dustin's Arcade 1.1: the rebuilt, realistic Fresno-style nickel arcade. */
   public static final BuildingPlan OHWOW = new OhWowPlan();

   public static BuildingPlan of(int id) {
      return id == 1 ? DUSTIN : id == 2 ? GRAND : id == 3 ? OHWOW : NICKEL;
   }

   public int sx() {
      return this.HX + 2;
   }

   public int width() {
      return this.HX * 2 + 5;
   }

   public int depth() {
      return this.BACK + 7;
   }

   public final List<Seg> segs() {
      if (this.segs == null) {
         List<Seg> s = new ArrayList<>();
         int sx = this.sx(), HX = this.HX, BACK = this.BACK;
         s.add(new Seg(-sx, sx, 0, 0, -1, BACK + 1, 0xFF505058, "floor"));
         s.add(new Seg(-sx, sx, CEIL, CEIL + 1, -1, BACK + 1, 0xFF6A6A72, "roof"));
         s.add(new Seg(-sx, -HX - 1, 1, CEIL - 1, -1, BACK + 1, this.outsideColor(), "skin"));
         s.add(new Seg(HX + 1, sx, 1, CEIL - 1, -1, BACK + 1, this.outsideColor(), "skin"));
         s.add(new Seg(-HX, HX, 1, CEIL - 1, BACK, BACK + 1, this.outsideColor(), "skin"));
         this.frontSegs(s);
         this.innerWalls(s);
         for (int[] c : this.columns()) s.add(new Seg(c[0], c[0], 1, CEIL - 1, c[1], c[1], 0, "hidden"));
         this.segs = s;
      }
      return this.segs;
   }

   /** Front wall: the storefront around the windows and the 3-wide, 3-tall door. */
   protected void frontSegs(List<Seg> s) {
         int HX = this.HX;
         int[][] w = this.windows();
         s.add(new Seg(-HX, -2, 1, 1, -1, 0, this.outsideColor(), "front"));
         s.add(new Seg(2, HX, 1, 1, -1, 0, this.outsideColor(), "front"));
         s.add(new Seg(-HX, HX, 5, CEIL - 1, -1, 0, this.outsideColor(), "front"));
         s.add(new Seg(-HX, w[0][0] - 1, 2, 4, -1, 0, this.outsideColor(), "front"));
         s.add(new Seg(w[0][1] + 1, -2, 2, 4, -1, 0, this.outsideColor(), "front"));
         s.add(new Seg(2, w[1][0] - 1, 2, 4, -1, 0, this.outsideColor(), "front"));
         s.add(new Seg(w[1][1] + 1, HX, 2, 4, -1, 0, this.outsideColor(), "front"));
         s.add(new Seg(-1, 1, 4, 4, -1, 0, this.outsideColor(), "front"));
   }

   /** Where the sliding doors stand (local z of the glass). */
   public double doorZ() {
      return -1.5;
   }

   /** Round columns inside (lx, lz). */
   public int[][] columns() {
      return new int[0][];
   }

   /** The giant power strip, if this building has one: {lx, lz}. */
   public int[] strip() {
      return null;
   }

   public final List<Surface> surfaces() {
      if (this.surfaces == null) {
         List<Surface> s = new ArrayList<>();
         int sx = this.sx();
         // A real-shell building renders its walls, roof and storefront as actual blocks. The model only
         // supplies interior surface detail, the pavement, and the signs/posters on the facade.
         if (!this.realShell()) {
            String ext = this.exteriorTexture();
            s.add(new Surface(ext, 'Z', -sx - 0.51, -1.5, this.BACK + 1.5, 1.5, CEIL + 2, 2, 2, -1, OUT));
            s.add(new Surface(ext, 'Z', sx + 0.51, -1.5, this.BACK + 1.5, 1.5, CEIL + 2, 2, 2, -1, OUT));
            s.add(new Surface(ext, 'X', this.BACK + 1.51, -sx - 0.5, sx + 0.5, 1.5, CEIL + 2, 2, 2, -1, OUT));
         }
         for (Seg g : this.segs()) {
            if (g.kind().equals("front")) {
               if (!this.realShell()) {
                  s.add(new Surface(this.exteriorTexture(), 'X', -1.51, g.x0() - 0.5, g.x1() + 0.5, Math.max(1.5, g.y0()), g.y1() + 1, 2, 2, -1, OUT));
               }
               s.add(new Surface(this.hallWallTexture(), 'X', g.z1() + 0.51, g.x0() - 0.5, g.x1() + 0.5, g.y0(), g.y1() + 1, 1, 2, -1, IN));
            }
         }
         if (!this.realShell()) {
            s.add(new Surface(this.exteriorTexture(), 'X', -1.51, -sx - 0.5, sx + 0.5, CEIL, CEIL + 2, 2, 2, -1, OUT));
            s.add(new Surface("roof", 'F', CEIL + 2.01, -sx - 0.5, sx + 0.5, -1.5, this.BACK + 1.5, 2, 2, -1, OUT));
         }
         // the hall: ceiling and side walls (floor and back wall are added by interior())
         s.add(new Surface(this.ceilingTexture(), 'C', CEIL - 0.012, -this.HX - 0.5, this.HX + 0.5, 0.5, this.BACK - 0.5, 2, 2, -1, IN));
         s.add(new Surface(this.hallWallTexture(), 'Z', -this.HX - 0.49, 0.5, this.HALL_BACK - 0.5, 1, CEIL, 1, 2, -1, IN));
         s.add(new Surface(this.hallWallTexture(), 'Z', this.HX + 0.49, 0.5, this.HALL_BACK - 0.5, 1, CEIL, 1, 2, -1, IN));
         this.interior(s);
         this.surfaces = s;
      }
      return this.surfaces;
   }

   public final List<Box> boxes() {
      if (this.boxes == null) {
         List<Box> b = new ArrayList<>();
         int sx = this.sx(), BK = this.BACK;
         int red = this.trimColor();
         if (!this.realShell()) {
         b.add(new Box(-sx - 0.53, 1, -1.5, -sx - 0.5, 1.5, BK + 1.5, red, OUT));
         b.add(new Box(sx + 0.5, 1, -1.5, sx + 0.53, 1.5, BK + 1.5, red, OUT));
         b.add(new Box(-sx - 0.5, 1, BK + 1.5, sx + 0.5, 1.5, BK + 1.53, red, OUT));
         b.add(new Box(-sx - 0.5, 1, -1.53, -1.5, 1.5, -1.5, red, OUT));
         b.add(new Box(1.5, 1, -1.53, sx + 0.5, 1.5, -1.5, red, OUT));
         }
         for (int[] c : this.channels()) {
            b.add(new Box(c[0] - 0.3, 1.0, c[2] - 0.3, c[1] + 0.3, 1.03, c[3] + 0.3, 0xFF202024, IN));
            for (int x = c[0]; x <= c[1]; x++) for (int z = c[2]; z <= c[3]; z++) b.add(new Box(x - 0.24, 1.03, z - 0.24, x + 0.24, 1.04, z + 0.24, 0xFF8A8E96, IN));
         }
         int alu = this.frameColor();
         if (!this.realShell()) {
         for (int[] w : this.windows()) {
            for (int x = w[0]; x <= w[1] + 1; x += 2) b.add(new Box(x - 0.55, w[2], this.id == 1 ? -0.66 : -1.56, x - 0.45, w[3] + 1, this.id == 1 ? -0.44 : 0.56, alu, OUT));
            b.add(new Box(w[0] - 0.5, w[2], this.id == 1 ? -0.66 : -1.56, w[1] + 0.5, w[2] + 0.08, this.id == 1 ? -0.44 : 0.56, alu, OUT));
            b.add(new Box(w[0] - 0.5, w[3] + 0.92, this.id == 1 ? -0.66 : -1.56, w[1] + 0.5, w[3] + 1, this.id == 1 ? -0.44 : 0.56, alu, OUT));
         }
         }
         this.extraBoxes(b);
         this.boxes = b;
      }
      return this.boxes;
   }

   // ---- what each building fills in ----
   protected abstract int outsideColor();
   protected abstract int trimColor();
   protected abstract String exteriorTexture();
   protected abstract String hallWallTexture();
   /** True when the shell is made of real blocks and the renderer only draws details. */
   public boolean realShell() { return false; }
   /** The ceiling texture used by the model. */
   protected String ceilingTexture() { return "ceiling"; }
   /** True when the facade renderer draws its own bespoke storefront frames. */
   protected boolean customStorefront() { return false; }
   /** Storefront frame colour. */
   public int frameColor() { return 0xFFB8BCC4; }
   protected abstract void innerWalls(List<Seg> s);
   protected abstract void interior(List<Surface> s);
   protected abstract void extraBoxes(List<Box> b);
   public abstract int[][] windows();
   public abstract List<int[]> lights();
   public abstract int[][] outlets();
   public abstract int[][] channels();
   /** Where the main breaker goes: {lx, lz, facing char}. */
   public abstract int[] breaker();

   // =====================================================================================
   /** The Nickel Arcade: big arcade floor plus a hallway of eight console rooms. */
   public static final class GrandPlan extends NickelPlan {
      GrandPlan() {
         super(2, "Dustin's Grand Hall", "GRAND HALL", 0xFFFF3030, 0xFF3070FF, 0xFFD02020, 0xFF2050C8);
      }
   }

   public static class NickelPlan extends BuildingPlan {
      public static final int ROOMS = 4;
      public static final int[] ROOM_COLORS = {0xFF3060C8, 0xFF2A2A40, 0xFFC03030, 0xFF181820, 0xFF707078, 0xFFC87830, 0xFF3A9A3A, 0xFF7A3AA8};
      public static final int[] FLOOR_COLORS = {0xFF1E3E8A, 0xFF121218, 0xFF8A1E1E, 0xFF0C0C10, 0xFF404048, 0xFF6A4020, 0xFF505058, 0xFF208A8A};

      NickelPlan() {
         this(0, "Nickel Arcade", "NICKEL ARCADE", 0xFF40E8FF, 0xFFFFE040, 0xFFD02020, 0xFFF4F4F4);
      }

      protected NickelPlan(int id, String name, String sign, int signA, int signB, int awningA, int awningB) {
         super(id, name, 13, 30, 54, sign, signA, signB, awningA, awningB);
      }

      public int roomStart(int k) {
         return this.HALL_BACK + 1 + k * 6;
      }

      @Override protected int outsideColor() { return 0xFFE8DCC0; }
      @Override protected int trimColor() { return 0xFFB02020; }
      @Override protected String exteriorTexture() { return "stucco"; }
      @Override protected String hallWallTexture() { return "wall"; }

      @Override
      protected void innerWalls(List<Seg> s) {
         int HX = this.HX, HB = this.HALL_BACK;
         s.add(new Seg(-HX, -2, 1, CEIL - 1, HB, HB, 0xFF1A1424, "wall"));
         s.add(new Seg(2, HX, 1, CEIL - 1, HB, HB, 0xFF1A1424, "wall"));
         s.add(new Seg(-1, 1, 4, CEIL - 1, HB, HB, 0xFF1A1424, "wall"));
         for (int side = -1; side <= 1; side += 2) {
            int cx = 2 * side, z = HB + 1;
            for (int k = 0; k < ROOMS; k++) {
               int door = this.roomStart(k) + 2;
               s.add(new Seg(cx, cx, 1, CEIL - 1, z, door - 1, 0xFFF0F0F0, "wall"));
               s.add(new Seg(cx, cx, 3, CEIL - 1, door, door, 0xFFF0F0F0, "wall"));
               z = door + 1;
               if (k < ROOMS - 1) {
                  int part = this.roomStart(k) + 5;
                  s.add(new Seg(Math.min(3 * side, HX * side), Math.max(3 * side, HX * side), 1, CEIL - 1, part, part, ROOM_COLORS[k * 2 + (side > 0 ? 0 : 1)], "wall"));
               }
            }
            s.add(new Seg(cx, cx, 1, CEIL - 1, z, this.BACK - 1, 0xFFF0F0F0, "wall"));
         }
      }

      @Override
      protected void interior(List<Surface> s) {
         int HX = this.HX, HB = this.HALL_BACK, BK = this.BACK;
         s.add(new Surface("carpet", 'F', 1.012, -HX - 0.5, HX + 0.5, 0.5, HB - 0.5, 2, 2, -1, IN));
         s.add(new Surface("carpet_glow", 'F', 1.022, -HX - 0.5, HX + 0.5, 0.5, HB - 0.5, 2, 2, -1, GLOW));
         s.add(new Surface("hall_carpet", 'F', 1.012, -1.5, 1.5, HB + 0.5, BK - 0.5, 1, 1, -1, IN));
         for (int k = 0; k < ROOMS; k++) {
            int za = this.roomStart(k);
            s.add(new Surface("room_carpet", 'F', 1.012, 2.5, HX + 0.5, za - 0.5, za + 4.5, 1, 1, FLOOR_COLORS[k * 2], IN));
            s.add(new Surface("room_carpet", 'F', 1.012, -HX - 0.5, -2.5, za - 0.5, za + 4.5, 1, 1, FLOOR_COLORS[k * 2 + 1], IN));
            s.add(new Surface("room_wall", 'Z', HX + 0.49, za - 0.5, za + 4.5, 1, CEIL, 1, 2, ROOM_COLORS[k * 2], IN));
            s.add(new Surface("room_wall", 'Z', -HX - 0.49, za - 0.5, za + 4.5, 1, CEIL, 1, 2, ROOM_COLORS[k * 2 + 1], IN));
         }
         s.add(new Surface("room_wall", 'X', BK - 0.51, -HX - 0.5, HX + 0.5, 1, CEIL, 1, 2, 0xFFF0F0F0, IN));
         for (Seg g : this.segs()) {
            if (g.kind().equals("wall")) wallFaces(s, g, g.z0() == HB ? "wall" : "room_wall", g.z0() == HB ? -1 : g.color());
         }
      }

      @Override
      protected void extraBoxes(List<Box> b) {
         int HX = this.HX, HB = this.HALL_BACK;
         b.add(new Box(-HX - 0.48, 4.4, 0.5, -HX - 0.46, 4.6, HB - 0.5, 0xFFFF40C0, GLOW));
         b.add(new Box(HX + 0.46, 4.4, 0.5, HX + 0.48, 4.6, HB - 0.5, 0xFFFF40C0, GLOW));
         b.add(new Box(-HX - 0.5, 4.4, HB - 0.48, HX + 0.5, 4.6, HB - 0.46, 0xFF40E8FF, GLOW));
      }

      @Override
      public int[][] windows() {
         return new int[][]{{-this.HX + 2, -4, 2, 4}, {4, this.HX - 2, 2, 4}};
      }

      @Override
      public List<int[]> lights() {
         List<int[]> l = new ArrayList<>();
         for (int lz = 2; lz < this.HALL_BACK; lz += 5) for (int lx = -10; lx <= 10; lx += 5) l.add(new int[]{lx, lz});
         for (int k = 0; k < ROOMS; k++) {
            int z = this.roomStart(k) + 2;
            l.add(new int[]{0, z});
            l.add(new int[]{8, z});
            l.add(new int[]{-8, z});
         }
         return l;
      }

      @Override
      public int[][] outlets() {
         List<int[]> o = new ArrayList<>();
         for (int zA : new int[]{9, 15}) {
            for (int x : new int[]{-4, 4}) {
               o.add(new int[]{x, zA - 1, 'F'});
               o.add(new int[]{x, zA + 2, 'B'});
            }
         }
         o.add(new int[]{-11, 22, 'L'});
         o.add(new int[]{-11, 27, 'L'});
         o.add(new int[]{11, 22, 'R'});
         o.add(new int[]{11, 27, 'R'});
         o.add(new int[]{-7, 27, 'F'});
         o.add(new int[]{7, 27, 'F'});
         for (int k = 0; k < ROOMS; k++) {
            o.add(new int[]{8, this.roomStart(k) + 2, 'R'});
            o.add(new int[]{-8, this.roomStart(k) + 2, 'L'});
         }
         return o.toArray(new int[0][]);
      }

      @Override
      public int[][] channels() {
         return new int[][]{{-12, 12, 8, 8}, {-12, 12, 11, 11}, {-12, 12, 14, 14}, {-12, 12, 17, 17},
            {-11, -11, 4, 27}, {11, 11, 4, 27}, {-11, 11, 27, 27}, {0, 0, 1, this.BACK - 1}};
      }

      @Override
      public int[] breaker() {
         return new int[]{-4, 1, 'F'};
      }
   }

   // =====================================================================================
   /**
    * Dustin's Arcade: a small-town 1980s family fun center. Brick front with a big bulb-lit sign, wood-panelled
    * walls, cabinets along both walls and a centre island, and in the back: the office with the big main breaker
    * you throw every morning, and the power room where every cabinet cord runs to one giant power strip.
    */
   public static final class DustinPlan extends BuildingPlan {
      DustinPlan() {
         super(1, "Dustin's Arcade", 10, 22, 40, "DUSTIN'S ARCADE", 0xFFFF3030, 0xFFFFD040, 0xFF2050C8, 0xFFFFD040);
      }

      @Override
      protected void frontSegs(List<Seg> s) {
         int HX = this.HX, c = this.outsideColor();
         s.add(new Seg(-HX, -4, 1, 1, -1, 0, c, "front"));
         s.add(new Seg(4, HX, 1, 1, -1, 0, c, "front"));
         s.add(new Seg(-HX, HX, 5, CEIL - 1, -1, 0, c, "front"));
         s.add(new Seg(-HX, -9, 2, 4, -1, 0, c, "front"));
         s.add(new Seg(-4, -4, 2, 4, -1, 0, c, "front"));
         s.add(new Seg(4, 4, 2, 4, -1, 0, c, "front"));
         s.add(new Seg(8, HX, 2, 4, -1, 0, c, "front"));
         s.add(new Seg(-3, -2, 1, 4, -1, 0, c, "hidden"));          // angled glass of the recessed entry
         s.add(new Seg(2, 3, 1, 4, -1, 0, c, "hidden"));
         s.add(new Seg(-1, 1, 4, 4, 0, 0, c, "front"));
         // the corner tower (solid cells under the round tower)
         for (int x = -14; x <= -8; x++) for (int z = -4; z <= 1; z++) {
            if (Math.hypot(x - TOWER_X, z - TOWER_Z) <= TOWER_R + 0.3) s.add(new Seg(x, x, 0, 14, z, z, c, "hidden"));
         }
      }

      public static final double TOWER_X = -11.5, TOWER_Z = -1.6, TOWER_R = 2.1;

      /** Angled walls inside: x0,z0,x1,z1,height,type (0 full wall, 1 half wall, 2 glass, 3 full wall with a staff door). */
      public static final double[][] WALLS = {
         {-3.5, 0.6, -2.6, 3.4, 3.0, 2}, {3.5, 0.6, 2.6, 3.4, 3.0, 2},          // entrance vestibule, angled glass
         {-10.5, 2.6, -8.4, 0.5, 7.0, 0},                                       // chamfered corners
         {8.4, 0.5, 10.5, 3.0, 7.0, 0},                                         // ticket corner (neon TICKETS)
         {6.8, 21.5, 10.5, 17.8, 7.0, 0}                                        // back corner
      };
      /** Console lounge (behind the arcade floor) and the staff wing behind it. */
      public static final int LOUNGE_BACK = 32;
      /** Raised prize display: half disc behind the counter. */
      public static final double RISER_X = 10.6, RISER_Z = 6.2, RISER_R = 2.6, RISER_H = 1.0;
      /** Curved counter wings either side of the prize booth: cx, cz, r, from°, to°. */
      public static final double[][] ARCS = {};
      /** Thin alcove fins between machine groups: x(wall side), z, direction into room. */
      public static final double[][] FINS = {{-10.5, 5.5, 1}, {-10.5, 8.5, 1}, {-10.5, 11.5, 1}, {10.5, 10.5, -1}, {10.5, 13.5, -1}, {10.5, 14.5, -1}, {10.5, 17.5, -1}};

      /** Doorway cells cut through the staff wall (only the bottom two blocks). */
      public static final int[][] STAFF_DOOR = {{9, 19}, {9, 20}};

      private static void line(List<Seg> s, double x0, double z0, double x1, double z1, int h) {
         line(s, x0, z0, x1, z1, h, false);
      }

      private static void line(List<Seg> s, double x0, double z0, double x1, double z1, int h, boolean door) {
         java.util.Set<Long> done = new java.util.HashSet<>();
         for (double t = 0; t <= 1.0001; t += 0.05) {
            int x = (int)Math.floor(x0 + (x1 - x0) * t + 0.5), z = (int)Math.floor(z0 + (z1 - z0) * t + 0.5);
            if (!done.add(((long)x << 32) ^ (z & 0xffffffffL))) continue;
            boolean gap = false;
            if (door) for (int[] d : STAFF_DOOR) gap |= d[0] == x && d[1] == z;
            s.add(new Seg(x, x, gap ? 3 : 1, h, z, z, 0, "hidden"));
         }
      }

      @Override
      public double doorZ() {
         return 0.42;
      }

      @Override
      public int[][] columns() {
         return new int[][]{};                                   // no columns: an open floor
      }

      @Override
      public int[] strip() {
         return new int[]{4, 36};
      }

      @Override protected int outsideColor() { return 0xFF9A3A2A; }
      @Override protected int trimColor() { return 0xFF303034; }
      @Override protected String exteriorTexture() { return "brick"; }
      @Override protected String hallWallTexture() { return "wood_panel"; }
      @Override public int frameColor() { return 0xFF34363A; }

      @Override
      protected void innerWalls(List<Seg> s) {
         int HX = this.HX, HB = this.HALL_BACK, LB = LOUNGE_BACK;
         // back wall of the arcade floor: a wide archway into the console lounge
         s.add(new Seg(-HX, -3, 1, CEIL - 1, HB, HB, 0xFF5A3A22, "wall"));
         s.add(new Seg(3, HX, 1, CEIL - 1, HB, HB, 0xFF5A3A22, "wall"));
         s.add(new Seg(-2, 2, 4, CEIL - 1, HB, HB, 0xFF5A3A22, "wall"));
         // back wall of the lounge: doors to the office (left) and the power room (right)
         s.add(new Seg(-HX, -7, 1, CEIL - 1, LB, LB, 0xFF8A8A84, "wall"));
         s.add(new Seg(-5, 5, 1, CEIL - 1, LB, LB, 0xFF8A8A84, "wall"));
         s.add(new Seg(7, HX, 1, CEIL - 1, LB, LB, 0xFF8A8A84, "wall"));
         s.add(new Seg(-6, -6, 3, CEIL - 1, LB, LB, 0xFF8A8A84, "wall"));
         s.add(new Seg(6, 6, 3, CEIL - 1, LB, LB, 0xFF8A8A84, "wall"));
         // wall between the office and the power room
         s.add(new Seg(0, 0, 1, CEIL - 1, LB + 1, this.BACK - 1, 0xFF8A8A84, "wall"));
         // solid cells behind the modelled angled walls, riser and curved counters
         for (double[] w : WALLS) line(s, w[0], w[1], w[2], w[3], Math.max(1, (int)Math.round(w[4])), w[5] == 3);
         for (double[] a : ARCS) {
            double r0 = Math.toRadians(a[3]), r1 = Math.toRadians(a[4]);
            line(s, a[0] + Math.cos(r0) * a[2], a[1] + Math.sin(r0) * a[2], a[0] + Math.cos(r1) * a[2], a[1] + Math.sin(r1) * a[2], 1);
         }
      }

      @Override
      protected void interior(List<Surface> s) {
         int HX = this.HX, HB = this.HALL_BACK, BK = this.BACK;
         s.add(new Surface("carpet80", 'F', 1.012, -6.7, HX + 0.5, 0.5, HB - 0.5, 2, 2, -1, IN));
         s.add(new Surface("carpet80", 'F', 1.012, -HX - 0.5, -6.7, 0.5, HB - 0.5, 2, 2, 0xFF6A6488, IN));     // dim side zone
         s.add(new Surface("carpet80_glow", 'F', 1.022, -HX - 0.5, HX + 0.5, 0.5, HB - 0.5, 2, 2, -1, GLOW));
         int LB = LOUNGE_BACK;
         s.add(new Surface("room_carpet", 'F', 1.012, -HX - 0.5, HX + 0.5, HB + 0.5, LB - 0.5, 2, 2, 0xFF8A6AC8, IN));   // lounge
         s.add(new Surface("wood_panel", 'Z', -HX - 0.49, HB + 0.5, LB - 0.5, 1, CEIL, 1, 2, -1, IN));
         s.add(new Surface("wood_panel", 'Z', HX + 0.49, HB + 0.5, LB - 0.5, 1, CEIL, 1, 2, -1, IN));
         s.add(new Surface("checker", 'F', 1.012, -HX - 0.5, -0.5, LB + 0.5, BK - 0.5, 1, 1, -1, IN));          // office
         // the modelled sidewalk all round: plaza in front, strips down the sides and back
         int sx = this.sx();
         s.add(new Surface("concrete", 'F', 1.004, -sx - 1.5, sx + 1.5, -5.5, -1.5, 2, 2, 0xFFD8D4CC, OUT));
         s.add(new Surface("concrete", 'F', 1.004, -sx - 1.5, -sx - 0.5, -1.5, BK + 2.5, 2, 2, 0xFFD8D4CC, OUT));
         s.add(new Surface("concrete", 'F', 1.004, sx + 0.5, sx + 1.5, -1.5, BK + 2.5, 2, 2, 0xFFD8D4CC, OUT));
         s.add(new Surface("concrete", 'F', 1.004, -sx - 0.5, sx + 0.5, BK + 1.5, BK + 2.5, 2, 2, 0xFFD8D4CC, OUT));
         // close the two-block return between the storefront wall and the deeper side walls
         s.add(new Surface(this.exteriorTexture(), 'X', -1.51, HX + 0.5, sx + 0.51, 1.5, CEIL + 2, 2, 2, -1, OUT));
         s.add(new Surface(this.exteriorTexture(), 'X', -1.51, -sx - 0.51, -HX - 0.5, 1.5, CEIL + 2, 2, 2, -1, OUT));
         s.add(new Surface("concrete", 'F', 1.012, 0.5, HX + 0.5, LB + 0.5, BK - 0.5, 2, 2, -1, IN));            // power room
         s.add(new Surface("room_wall", 'Z', -HX - 0.49, LB + 0.5, BK - 0.5, 1, CEIL, 1, 2, 0xFFD8C8A0, IN));
         s.add(new Surface("room_wall", 'Z', HX + 0.49, LB + 0.5, BK - 0.5, 1, CEIL, 1, 2, 0xFF9A9A94, IN));
         s.add(new Surface("room_wall", 'X', BK - 0.51, -HX - 0.5, -0.5, 1, CEIL, 1, 2, 0xFFD8C8A0, IN));
         s.add(new Surface("room_wall", 'X', BK - 0.51, 0.5, HX + 0.5, 1, CEIL, 1, 2, 0xFF9A9A94, IN));
         for (Seg g : this.segs()) {
            if (g.kind().equals("wall")) wallFaces(s, g, g.z0() == HB ? "wood_panel" : "room_wall", g.z0() == HB ? -1 : g.z0() == LB ? 0xFFD8C8A0 : g.color());
         }
      }

      @Override
      protected void extraBoxes(List<Box> b) {
         int HX = this.HX, HB = this.HALL_BACK;
         // chair rail along the wood panelling, and a red neon line up high
         b.add(new Box(-HX - 0.48, 2.9, 0.5, -HX - 0.4, 3.05, HB - 0.5, 0xFF3A2416, IN));
         b.add(new Box(HX + 0.4, 2.9, 0.5, HX + 0.48, 3.05, HB - 0.5, 0xFF3A2416, IN));
         b.add(new Box(-HX - 0.48, 6.4, 0.5, -HX - 0.46, 6.55, HB - 0.5, 0xFFFF3030, GLOW));
         b.add(new Box(HX + 0.46, 6.4, 0.5, HX + 0.48, 6.55, HB - 0.5, 0xFFFF3030, GLOW));
         // baseboards and a lowered soffit with a neon cove light all round the arcade floor
         int c1 = 0xFF2A1A10;
         b.add(new Box(-HX - 0.49, 1.0, 0.5, -HX - 0.42, 1.3, HB - 0.5, c1, IN));
         b.add(new Box(HX + 0.42, 1.0, 0.5, HX + 0.49, 1.3, HB - 0.5, c1, IN));
         b.add(new Box(-HX - 0.5, 7.2, 0.5, -HX + 0.4, 8.0, HB - 0.5, 0xFF2E2420, IN));
         b.add(new Box(HX - 0.4, 7.2, 0.5, HX + 0.5, 8.0, HB - 0.5, 0xFF2E2420, IN));
         b.add(new Box(-HX - 0.5, 7.2, 0.5, HX + 0.5, 8.0, 1.4, 0xFF2E2420, IN));
         b.add(new Box(-HX + 0.4, 7.85, 0.5, -HX + 0.47, 7.95, HB - 0.5, 0xFFFF3AC8, GLOW));
         b.add(new Box(HX - 0.47, 7.85, 0.5, HX - 0.4, 7.95, HB - 0.5, 0xFFFF3AC8, GLOW));
         b.add(new Box(-HX + 0.4, 7.85, 1.4, HX - 0.4, 7.95, 1.47, 0xFF40E8FF, GLOW));
         // "OFFICE" and "POWER" door plates
         b.add(new Box(-6.6, 3.2, LOUNGE_BACK - 0.56, -5.4, 3.6, LOUNGE_BACK - 0.5, 0xFFF0F0F0, IN));
         b.add(new Box(5.4, 3.2, LOUNGE_BACK - 0.56, 6.6, 3.6, LOUNGE_BACK - 0.5, 0xFFFFD040, GLOW));
          // Real storefront framing. The brick wall is a single surface in the model, so the
          // reveal through the one-block-thick window opening has to be built explicitly.
          int stone = 0xFFE8DCC0, alu = this.frameColor();
          for (int[] w : this.windows()) {
             b.add(new Box(w[0] - 0.5, w[2], -1.49, w[0] - 0.43, w[3] + 1, -0.51, stone, OUT));
             b.add(new Box(w[1] + 0.43, w[2], -1.49, w[1] + 0.5, w[3] + 1, -0.51, stone, OUT));
             b.add(new Box(w[0] - 0.5, w[3] + 0.96, -1.49, w[1] + 0.5, w[3] + 1, -0.51, stone, OUT));
             b.add(new Box(w[0] - 0.5, w[2], -1.49, w[1] + 0.5, w[2] + 0.04, -0.51, stone, OUT));
             // dark aluminium frame at the outer brick face
             b.add(new Box(w[0] - 0.6, w[2] + 0.02, -1.6, w[0] - 0.46, w[3] + 0.96, -1.4, alu, OUT));
             b.add(new Box(w[1] + 0.46, w[2] + 0.02, -1.6, w[1] + 0.6, w[3] + 0.96, -1.4, alu, OUT));
             b.add(new Box(w[0] - 0.6, w[3] + 0.84, -1.6, w[1] + 0.6, w[3] + 0.96, -1.4, alu, OUT));
             b.add(new Box(w[0] - 0.6, w[2] + 0.02, -1.6, w[1] + 0.6, w[2] + 0.14, -1.4, alu, OUT));
             // the narrow bay window needs its right-hand jamb at the glass plane too
             if (((w[1] - w[0] + 1) & 1) == 1) {
                b.add(new Box(w[1] + 0.45, w[2], -0.66, w[1] + 0.55, w[3] + 1, -0.44, alu, OUT));
             }
          }
      }

      @Override
      public int[][] windows() {
         return new int[][]{{-8, -5, 2, 4}, {5, 7, 2, 4}};
      }

      @Override
      public List<int[]> lights() {
         List<int[]> l = new ArrayList<>();
         for (int lz = 6; lz < this.HALL_BACK; lz += 4) for (int lx = -4; lx <= 8; lx += 4) l.add(new int[]{lx, lz});   // the pinball alley stays dim
         l.add(new int[]{0, 2});
         for (int lx = -6; lx <= 6; lx += 6) { l.add(new int[]{lx, 25}); l.add(new int[]{lx, 29}); }   // lounge
         l.add(new int[]{-5, 36});
         l.add(new int[]{5, 36});
         return l;
      }

      @Override
      public int[][] outlets() {
         // every outlet sits against a wall, its back to the wall, facing the room
         return new int[][]{{-10, 5, 'R'}, {-10, 12, 'R'}, {-10, 16, 'R'}, {-10, 19, 'R'}, {10, 10, 'L'}, {10, 14, 'L'}, {-6, 21, 'B'}, {6, 21, 'B'}, {-10, 23, 'R'}, {10, 23, 'L'}, {-1, 31, 'B'}};
      }

      @Override
      public int[][] channels() {
         // every cord runs in the floor back to the giant power strip in the power room
         return new int[][]{{-10, -10, 5, 21}, {-10, 6, 21, 21}, {10, 10, 10, 14}, {6, 10, 14, 14}, {6, 6, 14, 21}, {0, 0, 21, 31}, {-10, 10, 23, 23}, {-1, 6, 31, 31}, {6, 6, 31, 36}, {4, 6, 36, 36}};
      }

      @Override
      public int[] breaker() {
         return new int[]{-HX, 37, 'R'};
      }
   }

   // =====================================================================================
   /**
    * Dustin's Arcade 1.1: the rebuilt Fresno-style nickel arcade. A real stucco strip-mall shell,
    * a dark-brown fascia with the sign, full-height storefront glass, a recessed entry, parking
    * and sidewalk outside, and the interactive arcade + console lounge inside.
    */
   public static final class OhWowPlan extends BuildingPlan {
      OhWowPlan() {
         super(3, "Dustin's Arcade 1.1", 13, 30, 42, "DUSTIN'S ARCADE", 0xFFFF5A2A, 0xFF2AD4D0, 0xFF4A3428, 0xFF2AD4D0);
      }

      @Override public boolean realShell() { return true; }
      @Override protected String ceilingTexture() { return "ceiling_dark"; }
      @Override public int frameColor() { return 0xFF2A2622; }
      @Override protected int outsideColor() { return 0xFFC9B18C; }
      @Override protected int trimColor() { return 0xFF4A3428; }
      @Override protected String exteriorTexture() { return "stucco_tan"; }
      @Override protected String hallWallTexture() { return "wall_panel"; }

      @Override
      protected void frontSegs(List<Seg> s) {
         int HX = this.HX, c = 0xFFC9B18C;
         // Solid strips between the three full-height glass bays; the door is the centre 3-wide opening.
         s.add(new Seg(-HX, -13, 1, 6, -1, 0, c, "front"));
         s.add(new Seg(-3, -2, 1, 6, -1, 0, c, "front"));
         s.add(new Seg(2, 3, 1, 6, -1, 0, c, "front"));
         s.add(new Seg(13, HX, 1, 6, -1, 0, c, "front"));
         s.add(new Seg(-HX, HX, 7, 7, -1, 0, c, "front"));
         s.add(new Seg(-1, 1, 4, 6, -1, 0, c, "front"));       // header above the door
      }

      @Override public double doorZ() { return -1.0; }

      @Override public int[][] windows() {
         return new int[][]{{-12, -4, 1, 6}, {4, 12, 1, 6}};
      }

      @Override
      protected void innerWalls(List<Seg> s) {
         int HX = this.HX, HB = this.HALL_BACK, BK = this.BACK;
         // wide arch from the main arcade into the lounge
         s.add(new Seg(-HX, -4, 1, 7, HB, HB, 0xFF1B1726, "wall"));
         s.add(new Seg(4, HX, 1, 7, HB, HB, 0xFF1B1726, "wall"));
         s.add(new Seg(-3, 3, 6, 7, HB, HB, 0xFF1B1726, "wall"));
         // party room in the back-left corner, with a 2-wide doorway
         s.add(new Seg(-HX, -10, 1, 7, 36, 36, 0xFF221A24, "wall"));
         s.add(new Seg(-7, -5, 1, 7, 36, 36, 0xFF221A24, "wall"));
         s.add(new Seg(-9, -8, 4, 7, 36, 36, 0xFF221A24, "wall"));
         s.add(new Seg(-4, -4, 1, 7, 36, BK - 1, 0xFF221A24, "wall"));
         // office / power room in the back-right corner, with a 2-wide doorway
         s.add(new Seg(5, 8, 1, 7, 36, 36, 0xFF1B2028, "wall"));
         s.add(new Seg(11, HX, 1, 7, 36, 36, 0xFF1B2028, "wall"));
         s.add(new Seg(9, 10, 4, 7, 36, 36, 0xFF1B2028, "wall"));
         s.add(new Seg(4, 4, 1, 7, 36, BK - 1, 0xFF1B2028, "wall"));
      }

      @Override
      public int[] strip() { return new int[]{8, 41}; }

      @Override
      public int[] breaker() { return new int[]{HX, 38, 'L'}; }

      @Override
      protected void interior(List<Surface> s) {
         int HX = this.HX, HB = this.HALL_BACK, BK = this.BACK, sx = this.sx();
         // main arcade carpet, lounge carpet, party-room and office floors
         s.add(new Surface("carpet_neon", 'F', 1.012, -HX - 0.5, HX + 0.5, 0.5, HB - 0.5, 2, 2, -1, IN));
         s.add(new Surface("carpet_neon", 'F', 1.012, -HX - 0.5, HX + 0.5, HB + 0.5, BK - 0.5, 2, 2, 0xFF9A8AC8, IN));
         s.add(new Surface("checker", 'F', 1.012, -HX - 0.5, -4.5, 36.5, BK - 0.5, 1, 1, 0xFF7A2A6A, IN));
         s.add(new Surface("checker", 'F', 1.012, 4.5, HX + 0.5, 36.5, BK - 0.5, 1, 1, 0xFFB0B0A8, IN));
         // lounge side walls and the back wall (the main-floor side walls are added by surfaces())
         s.add(new Surface("wall_panel", 'Z', -HX - 0.49, HB + 0.5, BK - 0.5, 1, CEIL, 1, 2, -1, IN));
         s.add(new Surface("wall_panel", 'Z', HX + 0.49, HB + 0.5, BK - 0.5, 1, CEIL, 1, 2, -1, IN));
         s.add(new Surface("wall_panel", 'X', BK - 0.51, -HX - 0.5, HX + 0.5, 1, CEIL, 1, 2, -1, IN));
         // both faces of every interior partition and the arch wall
         for (Seg g : this.segs()) {
            if (g.kind().equals("wall")) wallFaces(s, g, "wall_panel", 0xFF1B1726);
         }
         // roof, pavement, parking lot, and the wrap-around sidewalk
         s.add(new Surface("roof_gravel", 'F', 9.012, -sx - 0.5, sx + 0.5, -1.5, BK + 1.5, 2, 2, -1, OUT));
         s.add(new Surface("asphalt", 'F', 1.005, -sx - 4.5, sx + 4.5, -16.5, -4.5, 4, 4, -1, OUT));
         s.add(new Surface("sidewalk", 'F', 1.010, -sx - 2.5, sx + 2.5, -4.5, -1.5, 2, 2, -1, OUT));
         s.add(new Surface("sidewalk", 'F', 1.008, -sx - 1.0, -sx - 0.5, -1.5, BK + 2.5, 2, 2, -1, OUT));
         s.add(new Surface("sidewalk", 'F', 1.008, sx + 0.5, sx + 1.0, -1.5, BK + 2.5, 2, 2, -1, OUT));
         s.add(new Surface("sidewalk", 'F', 1.008, -sx - 0.5, sx + 0.5, BK + 1.5, BK + 2.5, 2, 2, -1, OUT));
         // dark-brown fascia and signboard surfaces over the real stucco blocks
         s.add(new Surface("fascia_brown", 'X', -1.761, -sx - 0.5, sx + 0.5, 6.95, 8.95, 4, 2, -1, OUT));
         s.add(new Surface("fascia_brown", 'Z', -sx - 0.56, -1.5, BK + 1.5, 7.05, 9.05, 4, 2, -1, OUT));
         s.add(new Surface("fascia_brown", 'Z', sx + 0.56, -1.5, BK + 1.5, 7.05, 9.05, 4, 2, -1, OUT));
         s.add(new Surface("fascia_brown", 'X', BK + 1.56, -sx - 0.5, sx + 0.5, 7.05, 9.05, 4, 2, -1, OUT));
         s.add(new Surface("sign_dustin", 'X', -1.845, -9.0, -2.0, 7.05, 8.55, 7.0, 1.5, -1, OUT));
         s.add(new Surface("address", 'X', -1.53, -0.55, 0.55, 4.45, 4.95, 1.1, 0.5, -1, OUT));
         // window posters, exactly where the real storefront hangs them
         s.add(new Surface("poster_feature", 'X', -0.52, -11.4, -8.2, 2.0, 4.9, 3.2, 2.9, -1, OUT));
         s.add(new Surface("poster_prizes", 'X', -0.52, -7.3, -4.9, 2.2, 4.6, 2.4, 2.4, -1, OUT));
         s.add(new Surface("poster_open", 'X', -0.52, 5.0, 7.6, 2.2, 4.6, 2.6, 2.4, -1, OUT));
         // pylon sign at the street edge
         s.add(new Surface("sign_pylon", 'X', -10.51, -18.3, -13.7, 2.96, 5.24, 4.6, 2.28, -1, OUT));
         s.add(new Surface("sign_pylon", 'X', -9.99, -18.3, -13.7, 2.96, 5.24, 4.6, 2.28, -1, OUT));
         // interior neon
         s.add(new Surface("neon_video", 'Z', -HX - 0.47, 7.5, 11.5, 4.6, 5.6, 4, 1, -1, GLOW));
         s.add(new Surface("neon_prizes", 'Z', HX + 0.47, 20.0, 24.0, 4.6, 5.6, 4, 1, -1, GLOW));
         s.add(new Surface("neon_console", 'X', BK - 0.53, -3.0, 3.0, 5.0, 6.0, 6, 1, -1, GLOW));
         s.add(new Surface("neon_arcade", 'X', 0.53, -2.5, 2.5, 5.0, 6.0, 5, 1, -1, GLOW));
         s.add(new Surface("neon_party", 'X', 35.47, -11.0, -7.0, 4.8, 5.5, 4, 0.7, -1, GLOW));
         s.add(new Surface("neon_staff", 'X', 35.47, 7.0, 11.0, 4.8, 5.5, 4, 0.7, -1, GLOW));
      }

      @Override
      protected void extraBoxes(List<Box> b) {
         int HX = this.HX, HB = this.HALL_BACK, BK = this.BACK;
         int c1 = 0xFF2A1E18;
         // baseboards, chair rail and neon cove around the main arcade and lounge
         b.add(new Box(-HX - 0.49, 1.0, 0.5, -HX - 0.42, 1.3, HB - 0.5, c1, IN));
         b.add(new Box(HX + 0.42, 1.0, 0.5, HX + 0.49, 1.3, HB - 0.5, c1, IN));
         b.add(new Box(-HX - 0.49, 1.0, HB + 0.5, -HX - 0.42, 1.3, BK - 0.5, c1, IN));
         b.add(new Box(HX + 0.42, 1.0, HB + 0.5, HX + 0.49, 1.3, BK - 0.5, c1, IN));
         b.add(new Box(-HX - 0.5, 7.15, 0.5, -HX + 0.4, 8.0, HB - 0.5, 0xFF201820, IN));
         b.add(new Box(HX - 0.4, 7.15, 0.5, HX + 0.5, 8.0, HB - 0.5, 0xFF201820, IN));
         b.add(new Box(-HX + 0.3, 7.82, 0.5, -HX + 0.38, 7.92, HB - 0.5, 0xFFFF3AC8, GLOW));
         b.add(new Box(HX - 0.38, 7.82, 0.5, HX - 0.3, 7.92, HB - 0.5, 0xFFFF3AC8, GLOW));
         b.add(new Box(-HX - 0.5, 7.15, 0.5, HX + 0.5, 8.0, 1.4, 0xFF201820, IN));
         b.add(new Box(-HX + 0.3, 7.82, 1.35, HX - 0.3, 7.92, 1.43, 0xFF40E8FF, GLOW));
         b.add(new Box(-HX - 0.5, 7.15, HB + 0.5, -HX + 0.4, 8.0, BK - 0.5, 0xFF201820, IN));
         b.add(new Box(HX - 0.4, 7.15, HB + 0.5, HX + 0.5, 8.0, BK - 0.5, 0xFF201820, IN));
         b.add(new Box(-HX + 0.3, 7.82, HB + 0.5, -HX + 0.38, 7.92, BK - 0.5, 0xFF40E8FF, GLOW));
         b.add(new Box(HX - 0.38, 7.82, HB + 0.5, HX - 0.3, 7.92, BK - 0.5, 0xFF40E8FF, GLOW));
      }

      @Override
      public List<int[]> lights() {
         List<int[]> l = new ArrayList<>();
         for (int z : new int[]{3, 8, 13, 18, 23, 28}) for (int x : new int[]{-9, -3, 3, 9}) l.add(new int[]{x, z});
         for (int z : new int[]{31, 34, 39}) for (int x : new int[]{-9, -3, 3, 9}) l.add(new int[]{x, z});
         return l;
      }

      @Override
      public int[][] outlets() {
         return new int[][]{
            {-12, 2, 'R'}, {-12, 4, 'R'}, {12, 2, 'L'}, {12, 8, 'L'},
            {-4, 8, 'F'}, {4, 8, 'F'}, {-4, 11, 'B'}, {4, 11, 'B'},
            {-4, 14, 'F'}, {4, 14, 'F'}, {-4, 17, 'B'}, {4, 17, 'B'},
            {-12, 19, 'R'}, {12, 19, 'L'}, {-11, 27, 'R'}, {11, 27, 'L'},
            {-7, 27, 'F'}, {7, 27, 'F'},
            {-11, 31, 'R'}, {11, 31, 'L'}, {-11, 34, 'R'}, {11, 34, 'L'},
            {-2, 39, 'B'}, {0, 39, 'B'}, {2, 39, 'B'}, {-11, 38, 'R'}, {-6, 39, 'B'}, {11, 39, 'L'}
         };
      }

      @Override public int[][] channels() { return new int[0][]; }
   }


   protected static void wallFaces(List<Surface> s, Seg g, String tex, int color) {
      double y0 = g.y0(), y1 = g.y1() + 1;
      if (g.x1() - g.x0() >= g.z1() - g.z0()) {
         s.add(new Surface(tex, 'X', g.z0() - 0.51, g.x0() - 0.5, g.x1() + 0.5, y0, y1, 1, 2, color, IN));
         s.add(new Surface(tex, 'X', g.z1() + 0.51, g.x0() - 0.5, g.x1() + 0.5, y0, y1, 1, 2, color, IN));
      } else {
         s.add(new Surface(tex, 'Z', g.x0() - 0.51, g.z0() - 0.5, g.z1() + 0.5, y0, y1, 1, 2, color, IN));
         s.add(new Surface(tex, 'Z', g.x1() + 0.51, g.z0() - 0.5, g.z1() + 0.5, y0, y1, 1, 2, color, IN));
      }
   }
}
