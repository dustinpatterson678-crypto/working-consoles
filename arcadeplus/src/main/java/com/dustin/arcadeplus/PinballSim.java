package com.dustin.arcadeplus;

/**
 * 2D pinball physics on the playfield (u = across 0..14, v = up the table 0..28, in model pixels).
 * Runs on the server, 20 sub-steps per game tick. Plain circle-vs-segment and circle-vs-circle collisions,
 * with moving flippers that hand their surface speed to the ball.
 */
public final class PinballSim {
   public static final double R = 0.6;
   public static final double FLEN = 3.3;
   public static final double[] LPIV = {2.4, 4.5};
   public static final double[] RPIV = {10.0, 4.5};
   public static final double REST = -0.55;
   public static final double UP = 0.45;
   /** This table's playfield (bumpers, targets, spinner, saucer...). */
   public final PinballLayout L;
   private static final double G = 34.0;
   private static final double MAX_SPEED = 70.0;
   private static final double FLIP_SPEED = 20.0;
   private static final int SUBSTEPS = 20;
   private static final double DT = 1.0 / 20.0 / SUBSTEPS;

   public double x = 13.2, y = 1.3, vx, vy;
   public double aL = REST, aR = REST;
   public boolean inLane = true;
   public double charge;
   public int score;
   public int balls;
   public int targets;       // bit per standup target lit
   public int rollovers;     // bit per top lane lit
   public int mult = 1;
   public boolean kickback;  // left outlane kickback, armed by completing the target bank
   public boolean saveUsed;  // one ball save per ball
   public int ballTime;      // ticks since launch (ball save for the first 8 seconds)      // bonus multiplier 1..5
   public int bonus;         // end-of-ball bonus, paid times the multiplier when the ball drains
   private final int[] bumperCool;
   public int flash;         // bits: bumpers 0-4, slingshots 8-9, saucer 10, spinner 11, drop bank 12
   public int drops;         // bit per drop target knocked down
   private int dropReset;    // ticks until a completed drop bank pops back up
   public double spinAngle, spinVel;
   private double spinAcc;
   public int hold;          // ticks the saucer still holds the ball
   public int saucerCool;    // ticks before the saucer can catch the ball again (stops it re-catching the ball it just fired)
   private int stuck;        // ticks the ball has barely moved
   private double lastX, lastY;

   public PinballSim(PinballLayout layout) {
      this.L = layout;
      this.bumperCool = new int[layout.bumpers.length];
   }
   public boolean drained;
   private int stuckTicks;

   public static double[] leftTip(double a) {
      return new double[]{LPIV[0] + Math.cos(a) * FLEN, LPIV[1] + Math.sin(a) * FLEN};
   }

   public static double[] rightTip(double a) {
      return new double[]{RPIV[0] - Math.cos(a) * FLEN, RPIV[1] + Math.sin(a) * FLEN};
   }

   public void newBall() {
      this.x = 13.2;
      this.y = 1.3;
      this.vx = 0;
      this.vy = 0;
      this.inLane = true;
      this.charge = 0;
      this.drained = false;
      this.ballTime = 0;
   }

   /** Fresh game: clear multiplier, lanes and bonus. */
   public void reset() {
      this.score = 0;
      this.balls = 3;
      this.targets = 0;
      this.rollovers = 0;
      this.mult = 1;
      this.bonus = 0;
      this.kickback = false;
      this.drops = 0;
      this.hold = 0;
   }

   /** Called when a ball drains: pays the bonus and resets the multiplier. Returns points paid. */
   public int payBonus() {
      int paid = this.bonus * this.mult;
      this.score += paid;
      this.bonus = 0;
      this.mult = 1;
      this.rollovers = 0;
      return paid;
   }

   /** One game tick. Returns points scored this tick. */
   public int tick(boolean left, boolean right, boolean plunger) {
      int before = this.score;
      this.flash = 0;
      for (int i = 0; i < this.bumperCool.length; i++) {
         if (this.bumperCool[i] > 0) this.bumperCool[i]--;
      }
      // spinner keeps turning after the ball goes through, scoring each half turn
      if (Math.abs(this.spinVel) > 0.05) {
         this.spinAngle += this.spinVel * 0.05;
         this.spinAcc += Math.abs(this.spinVel) * 0.05;
         while (this.spinAcc > Math.PI) {
            this.spinAcc -= Math.PI;
            this.score += 10 * this.mult;
            this.bonus += 1;
            this.flash |= 1 << 11;
         }
         this.spinVel *= 0.93;
      }
      if (this.dropReset > 0 && --this.dropReset == 0) this.drops = 0;
      if (this.saucerCool > 0) this.saucerCool--;
      // saucer: holds the ball for a moment, then fires it back out
      if (this.hold > 0 && this.L.saucer != null) {
         this.x = this.L.saucer[0];
         this.y = this.L.saucer[1];
         this.vx = 0;
         this.vy = 0;
         if (--this.hold == 0) {
            double kx = this.L.saucer[2], ky = this.L.saucer[3], kl = Math.max(0.001, Math.hypot(kx, ky));
            double sp = Math.max(28, kl);                       // always fired hard enough to leave the area
            this.vx = kx / kl * sp;
            this.vy = ky / kl * sp;
            this.x += kx / kl * 1.4;                            // start just outside the cup, along the kick
            this.y += ky / kl * 1.4;
            this.saucerCool = 30;
         }
         return this.score - before;
      }
      if (this.inLane) {
         if (plunger) {
            this.charge = Math.min(1.0, this.charge + 0.05);
         } else if (this.charge > 0) {
            this.vy = 34 + 40 * this.charge;
            this.charge = 0;
            this.inLane = false;
         }
      }
      for (int s = 0; s < SUBSTEPS; s++) {
         double wl = 0, wr = 0;
         double tl = left ? UP : REST, tr = right ? UP : REST;
         if (this.aL != tl) { wl = Math.signum(tl - this.aL) * FLIP_SPEED; this.aL = step(this.aL, tl, FLIP_SPEED * DT); }
         if (this.aR != tr) { wr = Math.signum(tr - this.aR) * FLIP_SPEED; this.aR = step(this.aR, tr, FLIP_SPEED * DT); }
         if (this.inLane && this.vy == 0) {
            continue;
         }
         this.vy -= G * DT;
         this.lastX = this.x;
         this.lastY = this.y;
         this.x += this.vx * DT;
         this.y += this.vy * DT;
         // one-way gate at the top of the plunger lane: lets the launched ball out, deflects balls coming back down
         if (this.vy < 0 && this.x > 12.0 && this.y > 19.5 && this.y < 24.5) {
            this.segment(12.4, 21.0, 14.0, 23.6, R, 0, 0, 0.4, 0);
         }
         for (double[] w : this.L.walls) {
            this.segment(w[0], w[1], w[2], w[3], R, 0, 0, w[4] > 0 ? 0.9 : 0.45, w[4]);
         }
         // standup targets: solid, light up when hit; a full bank arms the kickback
         for (int i = 0; i < this.L.targets.length; i++) {
            double[] t = this.L.targets[i];
            if (this.segment(t[0], t[1], t[2], t[3], R + 0.2, 0, 0, 0.5, 0) && (this.targets & (1 << i)) == 0) {
               this.targets |= 1 << i;
               this.score += 50 * this.mult;
               this.bonus += 20;
               if (this.targets == (1 << this.L.targets.length) - 1) {
                  this.score += 500 * this.mult;
                  this.bonus += 100;
                  this.targets = 0;
                  this.kickback = true;
               }
            }
         }
         // drop targets: knocked flat when hit, the whole bank pays big and pops back up
         for (int i = 0; i < this.L.drops.length; i++) {
            if ((this.drops & (1 << i)) != 0) continue;
            double[] t = this.L.drops[i];
            if (this.segment(t[0], t[1], t[2], t[3], R + 0.2, 0, 0, 0.4, 0)) {
               this.drops |= 1 << i;
               this.score += 75 * this.mult;
               this.bonus += 25;
               if (this.drops == (1 << this.L.drops.length) - 1) {
                  this.score += 1000 * this.mult;
                  this.bonus += 200;
                  this.kickback = true;
                  this.dropReset = 30;
                  this.flash |= 1 << 12;
               }
            }
         }
         // spinner: the ball passes through and sets it spinning
         if (this.L.spinner != null) {
            double[] sp = this.L.spinner;
            if (crosses(sp[0], sp[1], sp[2], sp[3], this.lastX, this.lastY, this.x, this.y)) {
               this.spinVel += Math.signum(this.vy == 0 ? 1 : this.vy) * Math.min(60, Math.hypot(this.vx, this.vy)) * 0.9;
            }
         }
         // saucer: a slow enough ball drops in and is held
         if (this.L.saucer != null && this.saucerCool == 0 && Math.hypot(this.x - this.L.saucer[0], this.y - this.L.saucer[1]) < 0.9 && Math.hypot(this.vx, this.vy) < 40) {
            this.hold = 20;
            this.score += 250 * this.mult;
            this.bonus += 50;
            this.flash |= 1 << 10;
            break;
         }
         // flippers: capsule around the segment, surface velocity from the swing
         double[] lt = leftTip(this.aL);
         this.flipper(LPIV, lt, wl, false);
         double[] rt = rightTip(this.aR);
         this.flipper(RPIV, rt, wr, true);
         for (int i = 0; i < this.L.bumpers.length; i++) {
            double[] bp = this.L.bumpers[i];
            double dx = this.x - bp[0], dy = this.y - bp[1], d = Math.hypot(dx, dy), br = bp[2];
            if (d < br + R && d > 1e-6) {
               double nx = dx / d, ny = dy / d;
               this.x = bp[0] + nx * (br + R);
               this.y = bp[1] + ny * (br + R);
               // pop bumper: reflect with energy loss, then a modest kick (no more endless ping-pong)
               double vn = this.vx * nx + this.vy * ny;
               if (vn < 0) {
                  this.vx -= 1.6 * vn * nx;
                  this.vy -= 1.6 * vn * ny;
               }
               this.vx += nx * 12;
               this.vy += ny * 12;
               if (this.bumperCool[i] == 0) {
                  this.score += 10 * this.mult;
                  this.bonus += 2;
                  this.bumperCool[i] = 3;
               }
               this.flash |= 1 << i;
            }
         }
         // top rollover lanes: pass over all of them to raise the bonus multiplier
         for (int i = 0; i < this.L.rolls.length; i++) {
            double[] r = this.L.rolls[i];
            if (Math.abs(this.y - r[1]) < 0.5 && Math.abs(this.x - r[0]) < 0.7 && (this.rollovers & (1 << i)) == 0) {
               this.rollovers |= 1 << i;
               this.score += 25;
               this.bonus += 10;
               if (this.rollovers == (1 << this.L.rolls.length) - 1) {
                  this.rollovers = 0;
                  this.mult = Math.min(5, this.mult + 1);
                  this.flash |= 32;
               }
            }
         }
         double sp = Math.hypot(this.vx, this.vy);
         if (sp > MAX_SPEED) {
            this.vx *= MAX_SPEED / sp;
            this.vy *= MAX_SPEED / sp;
         }
         // kickback: fires a ball back up out of the left outlane once
         if (this.kickback && this.x < 1.5 && this.y < 6.0 && this.y > 1.0 && this.vy < 0) {
            this.kickback = false;
            this.vx = 1.5;
            this.vy = 48;
            this.flash |= 64;
         }
         if (this.y < -1.5) {
            this.drained = true;
            break;
         }
      }
      if (!this.inLane) {
         this.ballTime++;
      }
      // a ball resting in the plunger lane sits still on the lane floor
      if (!this.inLane && this.x > 12.4 && this.y < 2.0 && Math.abs(this.vy) < 1.0) {
         this.inLane = true;
         this.vx = 0;
         this.vy = 0;
         this.x = 13.2;
         this.y = 1.3;
      }
      // unstick a ball that has stopped somewhere on the field
      if (!this.inLane && Math.hypot(this.vx, this.vy) < 0.5) {
         if (++this.stuckTicks > 60) {
            this.vx = (this.x < 7 ? 1 : -1) * 6;
            this.vy = 8;
            this.stuckTicks = 0;
         }
      } else {
         this.stuckTicks = 0;
      }
      // a ball wedged somewhere (not on a flipper, not in the lane) gets a nudge, like a player shaking the table
      if (!this.inLane && this.y > 10 && Math.hypot(this.vx, this.vy) < 1.5) {
         if (++this.stuck > 50) {
            this.vx = this.x < 7 ? 14 : -14;
            this.vy = 10;
            this.stuck = 0;
         }
      } else {
         this.stuck = 0;
      }
      return this.score - before;
   }

   private static double step(double a, double target, double max) {
      double d = target - a;
      return Math.abs(d) <= max ? target : a + Math.signum(d) * max;
   }

   private void flipper(double[] piv, double[] tip, double omega, boolean mirrored) {
      // surface velocity at the closest point: omega x r (r from pivot), mirrored flipper swings the other way round
      double[] c = closest(piv[0], piv[1], tip[0], tip[1], this.x, this.y);
      double rx = c[0] - piv[0], ry = c[1] - piv[1];
      double w = mirrored ? -omega : omega;
      this.segment(piv[0], piv[1], tip[0], tip[1], R + 0.35, -w * ry, w * rx, 0.35, 0);
   }

   private static double[] closest(double x1, double y1, double x2, double y2, double px, double py) {
      double dx = x2 - x1, dy = y2 - y1, len2 = dx * dx + dy * dy;
      double t = len2 == 0 ? 0 : Math.max(0, Math.min(1, ((px - x1) * dx + (py - y1) * dy) / len2));
      return new double[]{x1 + dx * t, y1 + dy * t};
   }

   /** True when the path (ax,ay)->(bx,by) crosses the segment. */
   private static boolean crosses(double x1, double y1, double x2, double y2, double ax, double ay, double bx, double by) {
      double d1 = (x2 - x1) * (ay - y1) - (y2 - y1) * (ax - x1), d2 = (x2 - x1) * (by - y1) - (y2 - y1) * (bx - x1);
      if (d1 * d2 > 0) return false;
      double d3 = (bx - ax) * (y1 - ay) - (by - ay) * (x1 - ax), d4 = (bx - ax) * (y2 - ay) - (by - ay) * (x2 - ax);
      return d3 * d4 <= 0;
   }

   private boolean segment(double x1, double y1, double x2, double y2, double rad, double svx, double svy, double e, double kick) {
      double[] c = closest(x1, y1, x2, y2, this.x, this.y);
      double dx = this.x - c[0], dy = this.y - c[1], d = Math.hypot(dx, dy);
      if (d >= rad || d < 1e-9) {
         return false;
      }
      double nx = dx / d, ny = dy / d;
      this.x = c[0] + nx * rad;
      this.y = c[1] + ny * rad;
      double rvx = this.vx - svx, rvy = this.vy - svy;
      double vn = rvx * nx + rvy * ny;
      if (vn < 0) {
         rvx -= (1 + e) * vn * nx;
         rvy -= (1 + e) * vn * ny;
         this.vx = rvx + svx;
         this.vy = rvy + svy;
         if (kick > 0) {
            this.vx += nx * kick;
            this.vy += ny * kick;
            this.score += 1;
            this.bonus += 1;
            this.flash |= x1 < 7 ? 1 << 8 : 1 << 9;
         }
         return true;
      }
      return false;
   }
}
