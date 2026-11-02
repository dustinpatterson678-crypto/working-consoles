package com.dustin.arcadeplus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One table's playfield: where the bumpers, targets, drop targets, spinner, saucer, rollovers and guide rails sit.
 * Every themed table has its own layout, so each one plays differently. Units are playfield pixels:
 * u = across 0..14, v = up the table 0..28. Flippers, slingshots and the plunger lane are shared.
 */
public final class PinballLayout {
   public final String name;
   public final double[][] bumpers;   // u, v, radius
   public final double[][] walls;     // u1, v1, u2, v2, kick (0 plain, >0 slingshot), kind (0 frame, 1 guide rail)
   public final double[][] targets;   // standup targets: u1, v1, u2, v2
   public final double[][] drops;     // drop targets: u1, v1, u2, v2
   public final double[] spinner;     // u1, v1, u2, v2 (ball passes through and spins it) or null
   public final double[] saucer;      // u, v, kick-out vu, kick-out vv (holds the ball, then fires it) or null
   public final double[][] rolls;     // top rollover lanes: u, v

   private PinballLayout(String name, double[][] bumpers, double[][] extra, double[][] targets, double[][] drops, double[] spinner, double[] saucer, double[][] rolls) {
      this.name = name;
      this.bumpers = bumpers;
      List<double[]> w = new ArrayList<>();
      Collections.addAll(w, BASE);
      for (double[] e : extra) w.add(new double[]{e[0], e[1], e[2], e[3], 0, 1});
      this.walls = w.toArray(new double[0][]);
      this.targets = targets;
      this.drops = drops;
      this.spinner = spinner;
      this.saucer = saucer;
      this.rolls = rolls;
   }

   /** Frame, plunger lane, inlanes and slingshots: the same on every table. */
   private static final double[][] BASE = {
      {0, 0, 0, 23.5, 0, 0}, {0, 23.5, 3.5, 28, 0, 0}, {3.5, 28, 11.5, 28, 0, 0}, {11.5, 28, 14, 25.0, 0, 0},
      {14, 0, 14, 25.0, 0, 0}, {12.4, 0, 12.4, 21.0, 0, 0}, {12.4, 0.6, 14, 0.6, 0, 0},
      {1.6, 8.5, PinballSim.LPIV[0], PinballSim.LPIV[1], 0, 0}, {10.8, 8.5, PinballSim.RPIV[0], PinballSim.RPIV[1], 0, 0},
      {1.6, 8.5, 1.6, 11.2, 0, 0}, {10.8, 8.5, 10.8, 11.2, 0, 0}, {1.6, 8.5, 3.4, 7.4, 0, 0}, {10.8, 8.5, 9.0, 7.4, 0, 0},
      {1.6, 11.2, 3.4, 7.4, 16, 0}, {10.8, 11.2, 9.0, 7.4, 16, 0}
   };

   /** Dustin's Classic: three pop bumpers, a left standup bank, three top lanes. The original table. */
   public static final PinballLayout CLASSIC = new PinballLayout("classic",
      new double[][]{{3.8, 19.2, 1.1}, {8.6, 19.2, 1.1}, {6.2, 22.8, 1.1}},
      new double[][]{},
      new double[][]{{0.45, 12.4, 0.45, 13.6}, {0.45, 14.0, 0.45, 15.2}, {0.45, 15.6, 0.45, 16.8}},
      new double[][]{},
      null, null,
      new double[][]{{3.6, 26.0}, {6.2, 26.0}, {8.8, 26.0}});

   /** Galaxy Run: a long left orbit with a spinner, a center drop-target bank, a "black hole" saucer, two bumpers. */
   public static final PinballLayout GALAXY = new PinballLayout("galaxy",
      new double[][]{{8.0, 21.0, 1.2}, {10.2, 18.0, 1.0}},
      new double[][]{{2.7, 12.5, 2.7, 21.5}, {2.7, 21.5, 4.6, 24.6}},                 // left orbit guide
      new double[][]{},
      new double[][]{{4.6, 15.4, 5.8, 15.4}, {6.0, 15.4, 7.2, 15.4}, {7.4, 15.4, 8.6, 15.4}},
      new double[]{0.2, 17.0, 2.5, 17.0},
      new double[]{10.4, 23.4, -14, -6},
      new double[][]{{6.2, 26.0}, {8.8, 26.0}});

   /** Haunted Hollow: four bumpers in a diamond, a center "crypt" saucer, a right standup bank, a center spinner. */
   public static final PinballLayout HAUNTED = new PinballLayout("haunted",
      new double[][]{{6.2, 24.0, 1.0}, {4.0, 21.2, 1.0}, {8.4, 21.2, 1.0}, {6.2, 18.6, 1.0}},
      new double[][]{{4.6, 12.2, 4.6, 14.0}, {7.8, 12.2, 7.8, 14.0}},                 // short lane walls around the spinner
      new double[][]{{11.95, 13.0, 11.95, 14.2}, {11.95, 14.6, 11.95, 15.8}, {11.95, 16.2, 11.95, 17.4}},
      new double[][]{},
      new double[]{4.7, 13.4, 7.7, 13.4},
      new double[]{2.0, 18.5, 10, -10},
      new double[][]{{3.6, 26.0}, {6.2, 26.0}, {8.8, 26.0}});

   /** Pirate's Gold: a left bumper cove, a diagonal four-drop "cannon" bank on the right, a ship saucer, four top lanes. */
   public static final PinballLayout PIRATE = new PinballLayout("pirate",
      new double[][]{{3.0, 21.0, 1.0}, {5.2, 23.0, 1.0}, {3.6, 24.4, 0.8}},
      new double[][]{{6.8, 17.0, 6.8, 20.0}},                                          // divider between the cove and the cannons
      new double[][]{},
      new double[][]{{11.8, 13.2, 11.0, 14.2}, {10.9, 14.4, 10.1, 15.4}, {10.0, 15.6, 9.2, 16.6}, {9.1, 16.8, 8.3, 17.8}},
      new double[]{0.2, 15.0, 2.2, 15.0},
      new double[]{10.0, 22.0, -12, -8},
      new double[][]{{2.6, 26.0}, {5.0, 26.0}, {7.4, 26.0}, {9.8, 26.0}});

   /** Turbo Speedway: twin orbits like a race track, a center spinner, two bumpers at the top, a pit-stop drop bank. */
   public static final PinballLayout SPEEDWAY = new PinballLayout("speedway",
      new double[][]{{5.0, 22.6, 1.0}, {7.6, 22.6, 1.0}},
      new double[][]{{2.6, 12.5, 2.6, 21.0}, {2.6, 21.0, 4.2, 24.4}, {9.8, 12.5, 9.8, 21.0}, {9.8, 21.0, 8.2, 24.4}},
      new double[][]{},
      new double[][]{{4.0, 14.6, 5.2, 14.6}, {7.2, 14.6, 8.4, 14.6}},
      new double[]{5.4, 17.2, 7.0, 17.2},
      null,
      new double[][]{{3.6, 26.0}, {6.2, 26.0}, {8.8, 26.0}});

   public static PinballLayout of(String theme) {
      return switch (theme) {
         case "galaxy" -> GALAXY;
         case "haunted" -> HAUNTED;
         case "pirate" -> PIRATE;
         case "speedway" -> SPEEDWAY;
         default -> CLASSIC;
      };
   }
}
