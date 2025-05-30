package algorithms.FutureWork.DPF;

import java.util.*;

public class PrefixForest {
    protected TreeNode[][] root = new TreeNode[32][19]; // q in [-20,11], x in [-9,9]

    public PrefixForest() {
        init();
    }

    public void init() {
        for (int i = 0; i < 32; i++) {
            for (int j = 0; j < 19; j++) root[i][j] = null;
        }
    }

    public int addChain(double value, int q) {
        if (value == 0) return 0;

        long prefix = DPFTools.truncate(value * DPFTools.getP10(-q));

        int delta = 0; // suffix
        Stack<TreeNode> stack = new Stack<>();

        while (prefix != 0) {
            TreeNode node = new TreeNode(prefix, q + delta);
            stack.add(node);
            delta++;
            prefix /= 10;
        }

        int dep = 1; // prefix
        TreeNode pre_node = stack.pop();
        pre_node.setDelta(dep);

        if (root[pre_node.getQ() + 20][(int) (pre_node.getPrefix() + 9)] == null) {
            root[pre_node.getQ() + 20][(int) (pre_node.getPrefix() + 9)] = pre_node;
        }
        pre_node = root[pre_node.getQ() + 20][(int) (pre_node.getPrefix() + 9)];
        pre_node.appears();

        while (!stack.isEmpty()) {
            TreeNode node = stack.pop();
            node.setDelta(dep);
            dep++;

            prefix = node.prefix;
            int key = Math.abs((int) (prefix - (prefix / 10) * 10));
            TreeNode e_node = pre_node.getChild(key);
            if (e_node == null) {
                e_node = node;
                pre_node.addChild(key, e_node);
            }
            e_node.appears();

            pre_node = e_node;
        }

        return delta;
    }


    public ArrayList<TreeNode> getList(int batch_size) {
        PriorityQueue<TreeNode> queue = new PriorityQueue<>();
        for (int i = 0; i < 32; i++) {
            for (int j = 0; j < 19; j++) {
                if (root[i][j] != null) {
                    root[i][j].updateGain();
                    queue.add(root[i][j]);
                }
            }
        }

        ArrayList<TreeNode> list = new ArrayList<>();
        int count = 2;
        int threshold = 4;
        int bits = 2;

        while (!queue.isEmpty()) {
            TreeNode node = queue.poll();
            count++;
            if (count > threshold) {
                threshold <<= 1;
                bits++;
            }
            if (node.gain <= bits * batch_size) break;

            list.add(node);
            for (int key = 0; key < 10; key++) {
                TreeNode son = node.getChild(key);
                if (son != null) queue.add(son);
            }
        }

        return list;
    }
}