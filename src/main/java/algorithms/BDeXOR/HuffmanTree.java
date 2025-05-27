package algorithms.BDeXOR;

import utils.StreamWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public class HuffmanTree {
    protected int size = 0;
    protected int[] len;
    protected int[] code;
    protected int cnt = 0;
    protected Node root;

    protected class Node implements Comparable<Node> {
        public int f;
        public int v;

        public Node left, right;

        public Node(int f, int v) {
            this.f = f;
            this.v = v;
        }

        @Override
        public int compareTo(Node o) {
            return o.f - this.f;
        }
    }

    public HuffmanTree(int[] frequency) {
        this.size = frequency.length;
        this.len = new int[this.size];
        this.code = new int[this.size];
        this.buildTree(frequency);
    }

    protected void buildTree(int[] frequency) {
        PriorityQueue<Node> pq = new PriorityQueue<>();
        for (int v = 0; v < size; v++) {
            if (frequency[v] > 0) {
                cnt++;
                Node node = new Node(frequency[v], v);
                pq.add(node);
            }
        }
        if (pq.isEmpty()) return;

        while (pq.size() > 1) {
            Node a = pq.poll();
            Node b = pq.poll();

            Node c = new Node(a.f + b.f, -1);
            c.left = a;
            c.right = b;

            pq.add(c);
        }

        root = pq.poll();
    }


    protected void dfs(Node par, StreamWriter out, int cost, int len, int code) {
        if (par.v > 0) {
            out.write(par.v, cost);
            this.len[par.v] = len;
            this.code[par.v] = code;
        }
        if (par.left != null) {
            out.write(true);
            dfs(par.left, out, cost, len + 1, code << 1);
        } else out.write(false);

        if (par.right != null) {
            out.write(true);
            dfs(par.right, out, cost, len + 1, code << 1 | 1);
        } else out.write(false);
    }

    public boolean exist(int v){
        return len[v] > 0 || root.v == v;
    }

    public void serializeAndStore(StreamWriter out) {
        int cost = BDeXORTools.binaryLength(size);
        out.write(cnt, cost);
        List<Integer> dfs_list = new ArrayList<>();
        dfs(root, out, cost, 0, 0);
    }

    public void encode(int v, StreamWriter out) {
        out.write(code[v], len[v]);
    }
}
