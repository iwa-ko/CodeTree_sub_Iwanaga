package codetree.core;

import java.io.Serializable;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

public class CodeTree2 implements Serializable {
    GraphCode impl;
    public IndexNode root2;
    public static int datasetSize;
    Random rand;

    public CodeTree2(GraphCode impl, List<Graph> G, BufferedWriter bw, String dataset,
            BufferedWriter index) throws IOException {

        int limDepth = 0;
        datasetSize = G.size();// test
        this.impl = impl;
        this.root2 = new IndexNode(null, null);
        rand = new Random(2);

        List<CodeFragment> code = new ArrayList<>();

        long time = System.nanoTime();

        switch (dataset) {
            case "AIDS":
                limDepth = 5;
                // limDepth = 9;
                break;

            case "COLLAB":
                limDepth = 3;
                break;

            case "REDDIT-MULTI-5K":
                limDepth = 3;
                break;

            case "pdbs":
                limDepth = 10;
                break;

            case "IMDB-MULTI":
                limDepth = 3;
                break;

            case "pcms":
                limDepth = 5;// 5==10 edge filtering
                break;

            case "ppigo":
                limDepth = 5;
                // rand = new Random(1);
                break;
        }

        // int loop = 20;
        int loop = 10;

        for (Graph g : G) {
            for (int i = 0; i < loop; i++) {
                int start_vertice = rand.nextInt(g.order);
                code = impl.computeCanonicalCode_adj(g, start_vertice, limDepth);
                root2.addPath(code, g.id, false);
            }
        }

        List<ArrayList<CodeFragment>> codelist = impl.computeCanonicalCode(Graph.numOflabels(G));
        for (ArrayList<CodeFragment> c : codelist) {
            root2.addPath(c, -1, false);
        }

        index.write(limDepth + "," + String.format("%.6f", (double) (System.nanoTime() - time) / 1000 / 1000) +
                ",");

        // int sigma = Graph.numOflabels(G);
        // int start_label = sigma / 10;
        // // List<Integer> minVertexList = new ArrayList<>();
        // for (Graph g : G) {
        // for (int l = 0; l < sigma; l++) {
        // int minVertex = g.getVertex(l);
        // if (minVertex == -1)
        // continue;
        // for (int i = 0; i < loop; i++) {
        // code = impl.computeCanonicalCode_adj(g, minVertex, limDepth);
        // // code = impl.computeCanonicalCode(g, start_vertice, limDepth);
        // root2.addPath(code, g.id, false);
        // }
        // }
        // }

        System.out.println("depth " + (limDepth));
        bw.write("limDepth" + (limDepth) + "\n");
        System.out.println("Tree size: " + root2.size());
        System.out.println("addPathtoTree(ms): " + (System.nanoTime() - time) / 1000 /
                1000);
        bw.write("Tree size(original): " + root2.size() + "\n");
        bw.write("addPathtoTree(ms): " + String.format("%.6f", (double) (System.nanoTime() - time) / 1000 / 1000)
                + "\n");

        long start = System.nanoTime();

        int treesize = root2.size();

        System.out.println("tree size (original): " + treesize);
        index.write(treesize + ",");

        root2.addAdjLabels();

        List<Graph> leafGraphs = new ArrayList<>();
        root2.getLeafGraph(leafGraphs);
        // inclusionCheck2(impl, leafGraphs);
        root2.removeTree();
        treesize = root2.size();

        System.out.println("tree size (new): " + treesize);
        bw.write("Tree size(new): " + treesize + "\n");
        index.write(
                treesize + "," + String.format("%.6f", (double) (System.nanoTime() - time) / 1000 / 1000) + ",");

        System.out.println(
                "remove node time :" + String.format("%.6f", (double) (System.nanoTime() - time) / 1000 / 1000));

        start = System.nanoTime();
        System.out.println("グラフIDの計算中");
        inclusionCheck(impl, G);
        bw.write("addIDtoTree(ms): " + String.format("%.3f", (double) (System.nanoTime() - start) / 1000 / 1000)
                + "\n");
        System.out.println("\naddIDtoTree: " + (System.nanoTime() - start) / 1000 /
                1000 + "msec");
        index.write(String.format("%.3f", (double) (System.nanoTime() - start) / 1000
                / 1000));

        // try {
        // String codetree = String.format("data_structure/%s/depth%d_structure.ser",
        // dataset, limDepth);
        // FileOutputStream fileOut = new FileOutputStream(codetree);
        // ObjectOutputStream objout = new ObjectOutputStream(fileOut);
        // objout.writeObject(this);
        // objout.close();
        // fileOut.close();
        // System.out.println("データ構造がシリアライズされ、ファイルに保存されました。");
        // } catch (IOException e) {
        // e.printStackTrace();
        // }
    }

    private void inclusionCheck(GraphCode impl, List<Graph> G) {
        for (Graph g : G) {
            if (g.id % 100000 == 0) {
                // System.out.println();
            } else if (g.id % (G.size() / 2) == 0) {
                System.out.print("*");
            } else if (g.id % (G.size() / 10) == 0) {
                System.out.print(".");
            }
            root2.addIDtoTree2(g, impl, g.id, g.labels_Set());
            // root2.addIDtoTree(g, impl, g.id);

            root2.init_gtraverse_num();
        }
    }

    private void inclusionCheck2(GraphCode impl, List<Graph> leafGraphs) {

        ArrayList<Integer> idList = new ArrayList<>();
        ArrayList<Integer> removeIDList = new ArrayList<>();

        for (Graph g : leafGraphs) {
            if (removeIDList.contains(g.id))
                continue;

            idList.add(g.id);
            root2.pruningEquivalentNodes2(g, impl, g.id, idList, removeIDList);

        }
    }
}

// private void shirinkNEC(List<Graph> G) {
// int before = 0;
// int necNUM = 0;
// long shrinkTime = 0;
// long start = System.nanoTime();

// for (Graph g : G) {
// before = g.order;
// Graph g2 = g.shirinkNEC();
// G.set(g.id, g2);
// necNUM += before - g2.order;
// }
// shrinkTime = System.nanoTime() - start;

// System.out.println("削減頂点数:" + necNUM);
// System.out.println("削減関数時間[ms]" + shrinkTime / 1000 / 1000);
// }