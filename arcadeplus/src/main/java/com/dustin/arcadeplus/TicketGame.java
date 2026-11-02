package com.dustin.arcadeplus;

/** The redemption games and their tuning. Times are in game ticks (20 per second). */
public enum TicketGame {
   SKEEBALL("Alley Roller", 0),
   BASKETBALL("Hoop Frenzy", 600),
   WHACKAMOLE("Mole Bonk", 600),
   COINPUSHER("Coin Cascade", 0),
   CLAWMACHINE("Prize Claw", 0),
   STACKER("Tower Stack", 0),
   CYCLONE("Light Chaser", 0),
   GATORS("Swamp Smash", 600),
   BIGWHEEL("Big Wheel", 0),
   ECHO("Color Echo", 0),
   DRUMWHEEL("Bonus Drum", 0),
   CLOWNS("Clown Knockdown", 600),
   PLINKO("Plinko Drop", 0),
   STRIKER("High Striker", 0),
   HOCKEY("Air Hockey", 1200),
   LOCKER("Lucky Locker", 0),
   KEYVAULT("Key Vault", 0),
   SPEEDLIGHTS("Speed Lights", 600),
   DROPZONE("Drop Zone", 0),
   ROCKETRACE("Rocket Race", 0),
   LANES("Galaxy Lanes", 0),
   METEOR("Meteor Strike", 600),
   SNIPIT("Snip It", 0),
   POOL("Pool Table", 0),
   POWERPUNCH("Power Punch", 0),
   DEEPSEA("Deep Sea Catch", 0),
   BEATBLASTER("Beat Blaster", 600);

   public final String title;
   public final int roundTicks;

   TicketGame(String title, int roundTicks) {
      this.title = title;
      this.roundTicks = roundTicks;
   }

   /** How many cells the machine reaches behind its main block (filled with solid invisible parts). */
   public int depth() {
      return this == HOCKEY || this == LANES || this == SKEEBALL || this == BASKETBALL ? 1 : this == POOL ? 2 : 0;
   }

   /** Extra cells the machine fills: {cells to its left side (as you face it... the clockwise side), cells behind}. */
   public int[][] cells() {
      if (this == POOL) return new int[][]{{0, 1}, {0, 2}, {1, 0}, {1, 1}, {1, 2}};
      int[][] c = new int[this.depth()][];
      for (int k = 0; k < c.length; k++) c[k] = new int[]{0, k + 1};
      return c;
   }

   /** Plays without being plugged in. */
   public boolean unpowered() {
      return this == POOL;
   }

   public String id() {
      return this.name().toLowerCase();
   }

   // ---- shared maths so the server's result and the client's animation always agree ----

   /** Skee-Ball roll strength, swings 0..1 about every 2.6 seconds. */
   public static double skeePower(double t) {
      return (1.0 - Math.cos(t * 0.17)) / 2.0;
   }

   public static int skeeScore(double p) {
      if (p < 0.15) return 0;
      if (p < 0.35) return 10;
      if (p < 0.50) return 20;
      if (p < 0.65) return 30;
      if (p < 0.78) return 40;
      if (p < 0.88) return 50;
      if (p < 0.95) return 100;
      return 0;
   }

   /** Basketball hoop offset in pixels: still for 10 seconds, then slides side to side. */
   public static double hoopX(double t) {
      return t > 100 ? Math.sin(t * 0.12) * 4.5 : 0.0;
   }

   // ---- basketball: a real thrown ball. Same code on server (scoring) and client (drawing) ----
   public static final double BALL_R = 1.0, RIM_Y = 17.8, RIM_Z = 14.6, RIM_R = 2.3, BOARD_Z = 17.9, FLOOR_Y = 7.6;
   public static final double SHOT_G = 0.07;

   /**
    * Where a shot is t ticks after release. aim -1..1 (left/right), power 0..1, at = game ticks since the round started when shot.
    * Returns {x, y, z, state, madeAt}: state 0 flying, 1 dropping through the net (a basket), 2 gone.
    */
   public static double[] shot(double aim, double power, double at, double t) {
      double x = 0, y = 9.6, z = -4.0;
      double v = 0.95 + 0.85 * power, ang = Math.toRadians(64);
      double vx = aim * 0.40, vz = v * Math.cos(ang), vy = v * Math.sin(ang);
      double dt = 0.25, time = 0, madeAt = -1;
      while (time < t) {
         if (madeAt >= 0) {
            double k = time - madeAt;
            return new double[]{hoopX(at + time), RIM_Y - k * 0.45, RIM_Z, RIM_Y - k * 0.45 < FLOOR_Y + 2 ? 2 : 1, madeAt};
         }
         double py = y;
         vy -= SHOT_G * dt;
         x += vx * dt;
         y += vy * dt;
         z += vz * dt;
         if (Math.abs(x) > 6.6 - BALL_R) {           // cage sides
            x = Math.signum(x) * (6.6 - BALL_R);
            vx = -vx * 0.6;
         }
         if (z + BALL_R > BOARD_Z && vz > 0 && y > 13 && y < 30) {   // backboard: bank shots work
            z = BOARD_Z - BALL_R;
            vz = -vz * 0.9;
         }
         if (y + BALL_R > 30.5 && vy > 0) {          // cage roof
            vy = -vy * 0.4;
         }
         if (py >= RIM_Y && y < RIM_Y) {             // coming down through rim height
            double hx = hoopX(at + time);
            double d = Math.hypot(x - hx, z - RIM_Z);
            if (d < RIM_R - BALL_R * 1.0) {
               madeAt = time;
               continue;
            } else if (d < RIM_R + BALL_R) {          // clank off the rim
               double nx = (x - hx) / d, nz = (z - RIM_Z) / d;
               vx = nx * 0.45 + vx * 0.3;
               vz = nz * 0.45 + vz * 0.3;
               vy = Math.abs(vy) * 0.45;
               y = RIM_Y;
            }
         }
         if (y < FLOOR_Y) {
            return new double[]{x, FLOOR_Y, z, 2, -1};
         }
         time += dt;
      }
      return new double[]{x, y, z, madeAt >= 0 ? 1 : 0, madeAt};
   }

   /** Full outcome of a shot: {made 0/1, tick it drops through}. */
   public static double[] shotResult(double aim, double power, double at) {
      for (int t = 1; t <= 120; t++) {
         double[] s = shot(aim, power, at, t);
         if (s[3] >= 1 && s[4] >= 0) return new double[]{1, Math.ceil(s[4])};
         if (s[3] == 2) return new double[]{0, t};
      }
      return new double[]{0, 120};
   }

   // ---- Stacker: 7 x 12 light grid, stop each row on top of the last ----
   public static final int STACK_COLS = 7, STACK_ROWS = 12, STACK_MINOR = 8;

   /** Widest the moving row may be on row r (it narrows as you climb, like the real cabinet). */
   public static int stackMaxWidth(int row) {
      return row < 4 ? 3 : row < 8 ? 2 : 1;
   }

   /** Ticks per step on row r: the higher you get, the faster it moves. */
   public static int stackStep(int row) {
      return Math.max(1, 4 - row / 3);
   }

   /** Left column (as the player sees it) of the moving row, t ticks after the row started. */
   public static int stackPos(double t, int row, int width) {
      int range = STACK_COLS - width;
      if (range <= 0) return 0;
      int steps = (int)Math.floor(t / stackStep(row));
      int m = steps % (2 * range);
      return m <= range ? m : 2 * range - m;
   }

   // ---- Cyclone: a light runs round 24 lamps; stop it on the jackpot ----
   public static final int CYC_LAMPS = 24;
   /** Tickets per lamp; lamp 0 is the jackpot (paid from the machine's pool), lamp 12 the 50-ticket bonus. */
   public static final int[] CYC_VALUES = {0, 0, 1, 2, 1, 0, 5, 1, 2, 3, 1, 0, 10, 0, 1, 2, 1, 0, 5, 1, 2, 3, 1, 0};

   public static int cycLamp(double t) {
      return (int)Math.floor(t * 2.0) % CYC_LAMPS;
   }

   // ---- Gator Bash: five gators lunge out of their caves toward you ----
   public static final double[] GATOR_X = {6, 3, 0, -3, -6};

   // ---- Big Wheel: the wheel spins; stop it and it coasts exactly 2.5 turns more ----
   public static final int WHEEL_SEGS = 20;
   public static final int[] WHEEL_VALUES = {50, 1, 0, 1, 3, 0, 1, 5, 0, 2, 0, 1, 0, 4, 0, 2, 1, 3, 0, 1};
   public static final double WHEEL_SPEED = 0.30, WHEEL_COAST = Math.PI * 5, WHEEL_STOP_TICKS = 60;

   /** Wheel angle while spinning freely, t ticks since start. */
   public static double wheelSpin(double t) {
      return t * WHEEL_SPEED;
   }

   /** Angle t ticks after the player hit STOP at angle a0 (eases out to a0 + coast). */
   public static double wheelCoast(double a0, double t) {
      double k = Math.min(1, t / WHEEL_STOP_TICKS);
      return a0 + WHEEL_COAST * (1 - (1 - k) * (1 - k));
   }

   /** Segment under the pointer (at the top) for a wheel angle. */
   public static int wheelSegment(double angle) {
      double seg = Math.PI * 2 / WHEEL_SEGS;
      double a = ((-angle) % (Math.PI * 2) + Math.PI * 2) % (Math.PI * 2);
      return (int)Math.floor(a / seg) % WHEEL_SEGS;
   }

   // ---- Color Echo: repeat the light pattern; one more step every round ----
   public static int echoColor(long seed, int i) {
      long h = seed * 6364136223846793005L + (i + 1) * 1442695040888963407L;
      h ^= h >>> 29;
      return (int)((h * 0x9E3779B97F4A7C15L) >>> 62);
   }

   public static final int ECHO_STEP = 9;

   // ---- Drum Wheel: pull the handle, the drum rolls and lands where the machine decides ----
   public static final int[] DRUM_VALUES = {1, 2, 1, 25, 1, 2, 1, 10, 0, 2, 1, 15, 1, 2, 0, 250, 1, 2, 1, 10};
   public static final int DRUM_SEGS = 20;
   public static final double DRUM_TICKS = 90;

   /** Drum angle t ticks after the pull, from angle a0 to the angle that puts segment target under the pointer. */
   public static double drumAngle(double a0, int target, double t) {
      double seg = Math.PI * 2 / DRUM_SEGS;
      double want = -(target + 0.5) * seg;                     // pointer reads angle 0
      double base = a0 + Math.PI * 2 * 4;                      // at least four full turns
      double fin = want + Math.ceil((base - want) / (Math.PI * 2)) * Math.PI * 2;
      double k = Math.min(1, t / DRUM_TICKS);
      double ease = 1 - Math.pow(1 - k, 3);
      return a0 + (fin - a0) * ease;
   }

   public static int drumSegment(double angle) {
      double seg = Math.PI * 2 / DRUM_SEGS;
      double a = ((-angle) % (Math.PI * 2) + Math.PI * 2) % (Math.PI * 2);
      return (int)Math.floor(a / seg) % DRUM_SEGS;
   }

   // ---- Down the Clown: four clown heads slide along a rail; knock them down with balls ----
   public static final double CLOWN_Z = 2.6, CLOWN_Y = 16.0;

   public static double clownX(int i, double t) {
      double speed = 0.06 + Math.min(0.07, t / 600.0 * 0.07);   // they speed up through the round
      return -4.5 + i * 3.0 + Math.sin(t * speed + i * 1.7) * 1.4;
   }

   public static double clawSwing(double t, double speed) {
      return Math.sin(t * speed) * 5.5;
   }


   // ---- Plinko Drop: slide the dropper, let the puck bounce down the pegs into a slot ----
   public static final int PLINKO_ROWS = 9, PLINKO_PUCKS = 3, PLINKO_ROW_TICKS = 5;
   public static final int[] PLINKO_SLOTS = {1, 0, 0, 1, 4, 1, 0, 0, 1};
   public static final double PLINKO_TOP = 34.0, PLINKO_STEP = 2.0, PLINKO_HALF = 6.0;

   /** Dropper x in model pixels (+x is the player's left), sliding side to side. */
   public static double plinkoX(double t) {
      return Math.sin(t * 0.07) * 5.2;
   }

   /** The puck's x at each peg row (index 0 = drop point, PLINKO_ROWS = slot level). */
   public static double[] plinkoPath(double x0, int seed) {
      double[] xs = new double[PLINKO_ROWS + 1];
      xs[0] = x0;
      long h = seed;
      for (int r = 0; r < PLINKO_ROWS; r++) {
         h = h * 6364136223846793005L + 1442695040888963407L;
         double dir = ((h >>> 40) & 1) == 0 ? -1 : 1;
         double x = xs[r] + dir * 1.0;
         if (x > PLINKO_HALF - 0.6) x = PLINKO_HALF - 0.6 - 0.5;
         if (x < -PLINKO_HALF + 0.6) x = -PLINKO_HALF + 0.6 + 0.5;
         xs[r + 1] = x;
      }
      return xs;
   }

   public static int plinkoSlot(double x) {
      int n = PLINKO_SLOTS.length;
      int i = (int)Math.floor((PLINKO_HALF - x) / (2 * PLINKO_HALF / n));   // slot 0 on the player's left
      return Math.max(0, Math.min(n - 1, i));
   }

   /** Puck position {x, y} t ticks after the drop. */
   public static double[] plinkoPos(double x0, int seed, double t) {
      double[] xs = plinkoPath(x0, seed);
      double k = t / PLINKO_ROW_TICKS;
      int r = (int)Math.floor(k);
      if (r >= PLINKO_ROWS) return new double[]{xs[PLINKO_ROWS], PLINKO_TOP - PLINKO_ROWS * PLINKO_STEP - 0.4};
      double f = k - r;
      double x = xs[r] + (xs[r + 1] - xs[r]) * f;
      double y = PLINKO_TOP - (r + f) * PLINKO_STEP + Math.sin(f * Math.PI) * 0.6;   // little hop off each peg
      return new double[]{x, y};
   }

   public static int plinkoTicks() {
      return PLINKO_ROWS * PLINKO_ROW_TICKS;
   }

   // ---- High Striker: hit the pad when the meter peaks; ring the bell ----
   public static final int STRIKER_PERIOD = 30;

   /** Ticks away from the peak (0 = perfect), from game ticks since the round started. */
   public static int strikerOff(long t) {
      int m = (int)(t % STRIKER_PERIOD);
      return Math.abs(m - STRIKER_PERIOD / 2);
   }

   public static double strikerMeter(double t) {
      double m = t % STRIKER_PERIOD;
      return 1.0 - Math.abs(m - STRIKER_PERIOD / 2.0) / (STRIKER_PERIOD / 2.0);
   }

   public static int strikerTickets(int off) {
      return off == 0 ? 50 : off == 1 ? 10 : off == 2 ? 5 : off == 3 ? 3 : off <= 5 ? 2 : off <= 8 ? 1 : 0;
   }

   /** How high the puck flies (0..1) for a hit that far off the peak. */
   public static double strikerHeight(int off) {
      return Math.max(0.08, 1.0 - off / 12.0);
   }

   // ---- Air Hockey: you against the machine, 60 seconds ----
   /** Table half width / half length (model px) and the table's centre, half a block behind the main block. */
   public static final double HK_W = 6.8, HK_L = 14.4, HK_GOAL = 2.8, HK_PUCK = 0.9, HK_MALLET = 1.3, HK_CZ = 8.0;

   // ---- Lucky Locker: the light hops around 12 lockers; stop it on the gold one ----
   public static final int LOCKERS = 12;
   public static final int[] LOCKER_ORDER = {0, 1, 2, 3, 7, 11, 10, 9, 8, 4, 5, 6};   // round the outside, then the middle

   public static int lockerAt(double t) {
      return LOCKER_ORDER[(int)Math.floor(t / 2.0) % LOCKERS];
   }


   // ---- Key Vault: stop the key across, then up; it has to line up exactly with a lock ----
   public static final int KEY_XSTEPS = 32, KEY_YSTEPS = 24;
   public static final int[] KEY_COLS = {2, 9, 16, 23, 30};     // x step of each lock (x = 6.4 - 0.4 * step px)
   public static final int[] KEY_ROWS = {20, 12, 4};           // y step of each row, top row first (y = 19 + 0.5 * step px)
   public static final String[] KEY_PRIZES = {
      "arcadeplus:jumbo_panda", "arcadeplus:lava_lamp", "arcadeplus:golden_trophy", "arcadeplus:lava_lamp", "arcadeplus:jumbo_panda",
      "arcadeplus:dino_plush", "arcadeplus:mystery_box", "arcadeplus:cat_plush", "arcadeplus:mystery_box", "arcadeplus:dino_plush",
      "arcadeplus:teddy_bear", "arcadeplus:bunny_plush", "arcadeplus:duck_plush", "arcadeplus:bunny_plush", "arcadeplus:teddy_bear"};

   public static double tri(double t, int steps) {
      double m = t % (2.0 * steps);
      return m <= steps ? m : 2.0 * steps - m;
   }

   public static double keyX(double step) {
      return 6.4 - 0.4 * step;
   }

   public static double keyY(double step) {
      return 19.0 + 0.5 * step;
   }

   // ---- Speed Lights: hit the lit button, fast ----
   public static final double[][] SL_BUTTONS = {{4.5, -5.0}, {0.0, -5.0}, {-4.5, -5.0}, {4.5, -1.6}, {0.0, -1.6}, {-4.5, -1.6}, {4.5, 1.8}, {0.0, 1.8}, {-4.5, 1.8}};

   // ---- Drop Zone: drop the ball onto the spinning turntable ----
   public static final int[] DZ_VALUES = {1, 0, 2, 0, 1, 0, 5, 0, 1, 0, 2, 0, 1, 0, 25, 0};
   public static final int DZ_FALL = 10;

   public static double dzAngle(double t) {
      return t * 0.13;
   }

   /** Sector of the disc under the drop tube (at the front) for a turntable angle. */
   public static int dzSector(double angle) {
      double w = Math.PI * 2 / DZ_VALUES.length;
      double a = ((-Math.PI / 2 + angle) % (Math.PI * 2) + Math.PI * 2) % (Math.PI * 2);
      return (int)Math.floor(a / w) % DZ_VALUES.length;
   }


   // ---- Rocket Race: stop the meter high to boost your rocket; beat the three machine rockets to the finish ----
   public static final int RACE_LEN = 24, ROCKET_COOLDOWN = 8;
   public static final double[] ROCKET_AI = {0.10, 0.115, 0.13};

   public static double rocketMeter(double t) {
      double m = t % 20;
      return m <= 10 ? m / 10 : (20 - m) / 10;
   }

   public static int rocketBoost(double meter) {
      return meter > 0.92 ? 3 : meter > 0.65 ? 2 : 1;
   }

   // ---- Galaxy Lanes: aim and roll; knock down the ten pins ----
   public static final int LANE_BALLS = 5, LANE_ROLL = 40;
   /** Pin spots (x across the lane, z down the lane) in model pixels, head pin first. */
   public static final double[][] PINS = {{0, 19.0}, {-0.7, 20.2}, {0.7, 20.2}, {-1.4, 21.4}, {0, 21.4}, {1.4, 21.4}, {-2.1, 22.6}, {-0.7, 22.6}, {0.7, 22.6}, {2.1, 22.6}};

   /** Pins knocked down by a ball that arrives at x (px) with power p: returns a bit mask. */
   public static int pinsDown(double x, double p, long seed) {
      if (p < 0.25 || Math.abs(x) > 3.2) return 0;                      // too soft, or in the gutter
      double spread = 0.6 + Math.min(1, p) * 1.6;
      if (Math.abs(x) < 0.25 && p > 0.6 && p < 0.95) return 1023;           // in the pocket: strike
      int mask = 0;
      long h = seed;
      for (int i = 0; i < 10; i++) {
         h = h * 6364136223846793005L + 1442695040888963407L;
         double r = ((h >>> 33) & 1023) / 1023.0;
         double d = Math.abs(PINS[i][0] - x) - (PINS[i][1] - 19.0) * 0.25;
         if (d < spread * (0.5 + r * 0.6)) mask |= 1 << i;
      }
      return mask;
   }

   // ---- Meteor Strike: the cannon sweeps under five columns; fire up the column a meteor is falling in ----
   public static final int METEOR_COLS = 5;

   public static int cannonCol(double t) {
      int k = (int)Math.floor(t / 5) % 8;
      return k <= 4 ? k : 8 - k;
   }

   public static double meteorFall(double t) {
      return Math.max(28, 60 - t / 20);                                 // ticks to fall; faster as the round goes
   }

   // ---- Snip It: the scissors sweep across; cut the swaying string and the prize drops ----
   public static double scissorX(double t) {
      double m = t % 36;
      return (m <= 18 ? m / 18 : (36 - m) / 18) * 2 - 1;                   // -1..1
   }

   public static double stringX(double t) {
      return Math.sin(t * 0.07) * 0.55;
   }

   public static final String[] SNIP_PRIZES = {"arcadeplus:jumbo_panda", "arcadeplus:lava_lamp", "arcadeplus:golden_trophy", "arcadeplus:dino_plush", "arcadeplus:mystery_box"};

   /** Whack-a-Mole holes and claw-machine prizes, in model pixels (x, z). */
   public static final double[][] HOLES = {{-4.5, -3.5}, {4.5, -3.5}, {0, 0}, {-4.5, 3.5}, {4.5, 3.5}};
   public static final double[][] PRIZES = {{-4, 2}, {-1, 4}, {2, 1}, {4, 4}, {-3, -1}, {1, -2}, {5, -1}};

   // ---------------- Power Punch ----------------
   public static final int PUNCH_PERIOD = 22, PUNCHES = 3;

   /** The power meter sweeps up and down; the very top is a narrow target. */
   public static double punchMeter(double t) {
      double m = t % PUNCH_PERIOD;
      return 1.0 - Math.abs(m - PUNCH_PERIOD / 2.0) / (PUNCH_PERIOD / 2.0);
   }

   public static int punchPower(double meter) {
      return (int)Math.round(Math.pow(Math.max(0, Math.min(1, meter)), 1.7) * 999);
   }

   // ---------------- Deep Sea Catch ----------------
   public static final int FISH = 8, CASTS = 5, HOOK_DROP = 8;
   public static final double FISH_R = 0.24, FISH_SPEED = 0.034;
   public static final int[] FISH_VALUES = {1, 2, 1, 5, 1, 2, 1, 10};

   /** Angle of fish i around the pond; 0 is right under the hook at the front. */
   public static double fishAngle(int i, double t) {
      return t * FISH_SPEED + i * Math.PI * 2 / FISH;
   }

   public static boolean fishOpen(int i, double t) {
      return Math.sin(t * 0.21 + i * 2.1) > 0.3;
   }

   /** Which fish the hook takes if it's dropped at time t (it reaches the water HOOK_DROP ticks later), or -1. */
   public static int fishAtHook(double t) {
      double at = t + HOOK_DROP;
      for (int i = 0; i < FISH; i++) {
         double a = fishAngle(i, at) % (Math.PI * 2);
         double d = Math.min(a, Math.PI * 2 - a);
         if (d < 0.24 && fishOpen(i, at)) return i;
      }
      return -1;
   }

   // ---------------- Beat Blaster ----------------
   public static final int BEAT = 10, NOTE_STEP = 4, NOTE_LEDS = 8, BEAT_START = 40;
   public static final double[][] BB_PADS = {{4.0, -2.88}, {0.0, -2.88}, {-4.0, -2.88}};

   /** Lane (0..2) of the note on beat b, or -1 for a rest. Same chart on client and server from the round's seed. */
   public static int noteLane(int seed, int b) {
      if (b < 0) return -1;
      int h = (seed ^ (b * 0x9E3779B1)) * 0x85EBCA6B;
      h ^= h >>> 13;
      h *= 0xC2B2AE35;
      h ^= h >>> 16;
      int r = (h >>> 1) % 100;
      return r < 22 ? -1 : r % 3;
   }

   /** Round time at which beat b's note reaches the hit line. */
   public static int beatTime(int b) {
      return BEAT_START + b * BEAT;
   }

   public static int lastBeat() {
      return (BASS_END - BEAT_START) / BEAT;
   }

   public static final int BASS_END = 580;
}
