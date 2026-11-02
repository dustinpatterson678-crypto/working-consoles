package com.dustin.wcore.block;

public enum PowerGrade {
   OFF("Off", 0, -11184811),
   UNSTABLE("Unstable", 2, -43691),
   LOW("Low voltage", 5, -22016),
   NORMAL("Normal", 9, -171),
   CLEAN("Clean", 13, -11141291),
   OVERCLOCK("Overclock", 15, -11141121);

   public static final int OVERCLOCK_AFTER_SECONDS = 60;
   public static final int UNSTABLE_FLICKERS = 2;
   public static final int BROWNOUT_FLICKERS = 3;
   public final String label;
   public final int comparator;
   public final int color;

   PowerGrade(String label, int comparator, int color) {
      this.label = label;
      this.comparator = comparator;
      this.color = color;
   }

   public static PowerGrade of(int signal, int flickers, long stableSeconds) {
      if (signal <= 0) {
         return OFF;
      } else if (flickers >= 2) {
         return UNSTABLE;
      } else if (signal < 8) {
         return LOW;
      } else if (signal < 15) {
         return NORMAL;
      } else {
         return stableSeconds >= 60L ? OVERCLOCK : CLEAN;
      }
   }
}
