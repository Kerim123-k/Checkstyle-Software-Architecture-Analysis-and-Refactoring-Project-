// Sample input crafted to fire every check in the metrics + sizes slice
// at least once when run with bench-config.xml. Used by T001 baseline
// capture, T069 RegressionDiffTest, and T070 PerCheckFireTest.
// Keep this file deterministic; any edit changes the pinned baseline.

package sample;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class SampleAllViolations {

    private ArrayList<String> a = new ArrayList<>();
    private HashMap<String, String> b = new HashMap<>();
    private HashSet<String> c = new HashSet<>();
    private LinkedList<String> d = new LinkedList<>();
    private TreeMap<String, String> e = new TreeMap<>();
    private BitSet bits = new BitSet();
    private AtomicInteger ai = new AtomicInteger();
    private AtomicLong al = new AtomicLong();
    private BigDecimal bd = new BigDecimal("0");
    private BigInteger bi = new BigInteger("0");

    public int longMethod(int p1, int p2, int p3, int p4) {
        int x = p1 + p2;
        int y = p3 + p4;
        int z = x + y;
        return z;
    }

    public int complex(int n) {
        int r = 0;
        if (n > 0 && n < 10 && n != 5 && n != 7) {
            r++;
        }
        if (n > 10 || n < -5 || n == 0) {
            r++;
        }
        if (n > 20) {
            r++;
        }
        return r;
    }

    public Runnable makeAnon() {
        return new Runnable() {
            @Override
            public void run() {
                int a1 = 1;
                int a2 = 2;
                int a3 = a1 + a2;
                System.out.println(a3);
            }
        };
    }

    public Runnable makeLambda() {
        return () -> {
            int l1 = 1;
            int l2 = 2;
            int l3 = l1 + l2;
            System.out.println(l3);
        };
    }

    public void m1() { }
    public void m2() { }
    public void m3() { }

    public record Big(int f1, int f2, int f3, int f4) { }
}

class SecondOuterType {
    public void noop() { }
}
