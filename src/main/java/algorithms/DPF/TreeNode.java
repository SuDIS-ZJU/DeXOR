package algorithms.DPF;

import java.util.ArrayList;
import java.util.List;

public class TreeNode implements Comparable<TreeNode>{
    protected long prefix;

    protected int q;
    protected int delta = 0;
    protected int cost = 0;
    protected double gain = 0;

    protected int frequency = 0; // chains bypass this node

    protected TreeNode[] sons = new TreeNode[10];

    public TreeNode(long prefix, int q) {
        this.prefix = prefix;
        this.q = q;
        this.frequency = 1;
    }

    public int getFrequency() {
        return frequency;
    }

    public void appears(){
        this.frequency += 1;
    }

    public void setDelta(int delta) {
        this.delta = delta;
        this.cost = DPFTools.decimalBits(delta);
    }

    public int getDelta() {
        return delta;
    }

    public int getQ() {
        return q;
    }

    public long getPrefix() {
        return prefix;
    }

    public int getCost() {
        return cost;
    }

    public TreeNode getChild(int key) {
        return sons[key];
    }

    public void updateGain(){
        this.gain = frequency * delta * DPFTools.Log - cost - 9;
    }

    public double getGain() {
        return gain;
    }

    public int getKey(){
        return (int) (Math.abs(prefix) % 10);
    }

    public void addChild(int key, TreeNode node) {
        sons[key] = node;
    }

    public void removeChild(int key){
        sons[key] = null;
    }

    @Override
    public int compareTo(TreeNode o) {
        return (int) (this.gain - o.gain);
    }
}