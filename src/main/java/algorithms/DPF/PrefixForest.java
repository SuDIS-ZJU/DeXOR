package algorithms.DPF;

import java.util.*;

public class PrefixForest {
    protected Map<DecimalPair, TreeNode> node_map = new HashMap<>();

    public PrefixForest() {
    }

    public static class DecimalPair {
        public long prefix;
        public int q;

        public DecimalPair(long prefix, int q) {
            this.prefix = prefix;
            this.q = q;
        }

        public double value() {
            return prefix * DPFTools.getP10(q);
        }
    }

    public static class TreeNode implements Comparable<TreeNode> {
        protected DecimalPair v;
        protected int delta =0;
        protected int cost = 0;
        protected int w = 0;

        protected TreeNode fa;
        protected List<TreeNode> sons = new ArrayList<>();

        public TreeNode(DecimalPair v) {
            this.v = v;
        }

        public DecimalPair getV() {
            return v;
        }

        public int getDelta() {
            return delta;
        }

        @Override
        public int compareTo(TreeNode o) {
            // 按照数值从小到大排序
            return (o.w - o.cost) - (this.w - this.cost);
        }
    }

    public void clear() {
        node_map.clear();
    }

    public TreeNode addNode(DecimalPair v) {
        if (node_map.containsKey(v)) {
            return node_map.get(v);
        }
        return new TreeNode(v);
    }


    public int addChain(double value, int q) {
        long prefix = DPFTools.truncate(value * DPFTools.getP10(-q));

        int dp = 0;
        List<TreeNode> chain = new ArrayList<>();
        while (prefix != 0) {
            DecimalPair v = new DecimalPair(prefix, q);
            TreeNode node = addNode(v);
            chain.add(node);
            node.w -= DPFTools.decimalBits(dp);
            prefix /= 10;
            q++;
            dp++;
        }
        int delta = dp;
        int tw = DPFTools.decimalBits(dp);
        for (TreeNode node : chain) {
            node.w += tw;
            node.cost = 5 + 5 + DPFTools.decimalBits(dp);
            node.delta = dp--;
        }
        return delta;
    }

    public List<TreeNode> greedy() {
        List<TreeNode> list = new ArrayList<>();
        int dep = 0;
        for (DecimalPair v : node_map.keySet()) {
            TreeNode node = node_map.get(v);
            if (node.w <= node.cost + dep + 1) {
                break;
            }
            list.add(node);
            dep++;
        }
        return list;
    }
}