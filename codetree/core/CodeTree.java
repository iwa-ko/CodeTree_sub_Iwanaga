package codetree.core;

import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.*;

import codetree.common.VertexLabel;

public class CodeTree implements Serializable {
    GraphCode impl;
    public IndexNode root;
    public int delta;
    Random rand;

    public CodeTree(GraphCode impl, List<Graph> G, BufferedWriter bw, String dataset,
            BufferedWriter index) throws IOException {

        this.impl = impl;
        this.root = new IndexNode(null, null, 0, false);
        rand = new Random(2);

        List<CodeFragment> code = new ArrayList<>();

        int loop = 1;
        long start = System.nanoTime();

        switch (dataset) {
            case "NCI":
                delta = 5;
                break;

            case "AIDS":
                delta = 5;
                break;

            case "COLLAB":
                delta = 5;
                break;

            case "REDDIT-MULTI-5K":
                delta = 3;
                break;

            case "pdbs":
                delta = 20;
                break;

            case "IMDB-MULTI":
                delta = 4;
                break;

            case "pcms":
                delta = 10;
                // 10
                // delta = 5;
                // loop = 10;
                break;

            case "ppigo":
                delta = 5;
                // delta = 4;
                // loop = 100;
                break;
        }

        // Path p = Paths.get("patten.gfu");

        // try (BufferedWriter p_bw = Files.newBufferedWriter(p)) {

        int[] total5 = new int[63];
        for (int a = 0; a < 63; a++) {
            total5[a] = 0;
        }

        int[] aids = { 3, 1, 22, 7, 0 };
        int[] imdb = { 5, 8, 4, 1, 2 };
        int[] pcms = { 10, 11, 6, 16, 17 };

        int method = 2;// 0: 始点を等価性が高いものに 1: ランダムに部分グラフを取得後始点を等価性の高いもの 2: ランダムにコード生成

        if (method == 0) {

            for (Graph g : G) {
                boolean equifrag = false;
                int start_vertice = rand.nextInt(g.order);
                for (int l = 0; l < loop; l++) {
                    for (int an : aids) {
                        for (int v = 0; v < g.order; ++v) {
                            if (g.vertices[v] == an) {
                                start_vertice = v;
                                equifrag = true;
                                break;
                            }
                        }
                        if (equifrag)
                            break;
                    }

                    HashSet<Integer> targetVertices = g.getTargetVertices(delta, start_vertice);
                    Graph inducedGraph = g.generateInducedGraph(targetVertices);
                    start_vertice = rand.nextInt(inducedGraph.order);

                    equifrag = false;
                    for (int an : aids) {
                        for (int v = 0; v < inducedGraph.order; ++v) {
                            if (inducedGraph.vertices[v] == an) {
                                start_vertice = v;
                                equifrag = true;
                                break;
                            }
                        }
                        if (equifrag)
                            break;
                    }
                    code = impl.computeCanonicalCode(inducedGraph, start_vertice, delta);
                    String s = code.get(0).toString();
                    byte a = codetree.common.VertexLabel.string2id(s);
                    total5[a]++;
                    // print(code);
                    root.addPath(code, g.id, false, 0);
                }
            }
        } else if (method == 1) {

            // 等価性の高い頂点に絞ってそこからランダムにコード生成
            for (Graph g : G) {
                boolean equifrag = false;
                int start_vertice = rand.nextInt(g.order);
                for (int l = 0; l < loop; l++) {
                    for (int an : pcms) {
                        for (int v = 0; v < g.order; ++v) {
                            if (g.vertices[v] == an) {
                                start_vertice = v;
                                equifrag = true;
                                break;
                            }
                        }
                        if (equifrag)
                            break;
                    }

                    HashSet<Integer> targetVertices = g.getTargetVertices(delta, start_vertice);
                    Graph inducedGraph = g.generateInducedGraph(targetVertices);
                    start_vertice = rand.nextInt(inducedGraph.order);
                    code = impl.computeCanonicalCode(inducedGraph, start_vertice, delta);
                    // print(code);
                    root.addPath(code, g.id, false, 0);
                }
            }
        } else if (method == 2) {

            for (Graph g : G) {
                for (int l = 0; l < loop; l++) {
                    int start_vertice = rand.nextInt(g.order);
                    HashSet<Integer> targetVertices = g.getTargetVertices(delta, start_vertice);
                    Graph inducedGraph = g.generateInducedGraph(targetVertices);
                    start_vertice = rand.nextInt(inducedGraph.order);
                    code = impl.computeCanonicalCode(inducedGraph, start_vertice, delta);
                    String s = code.get(0).toString();
                    byte a = codetree.common.VertexLabel.string2id(s);
                    total5[a]++;
                    // print(code);
                    root.addPath(code, g.id, false, 0);
                }
            }
        }

        // どの頂点ラベルが先頭になったか
        for (int a = 0; a < 63; a++) {
            System.out.println("first fragment " + a + " : " + total5[a]);
        }

        List<ArrayList<CodeFragment>> codelist = impl.computeCanonicalCode(Graph.numOflabels(G));
        for (ArrayList<CodeFragment> c : codelist) {
            root.addPath(c, -1, false, 0);
        }
        codelist = null;

        root.sortVlabelChildren();

        index.write(dataset + "," + delta + ","
                + String.format("%.6f", (double) (System.nanoTime() - start) / 1000 / 1000 / 1000) + ",");

        int treesize = root.size();

        System.out.println("depth " + (delta));
        bw.write("delta" + (delta) + "\n");
        System.out.println("Tree size: " + treesize);
        System.out.println("addPathtoTree(s): "
                + String.format("%.6f", (double) (System.nanoTime() - start) / 1000 / 1000 / 1000));
        bw.write("addPathtoTree(s): " + String.format("%.6f", (double) (System.nanoTime() - start) / 1000 / 1000 / 1000)
                + "\n");

        index.write(treesize + ",");

        long time = System.nanoTime();
        List<Graph> leafGraphs = new ArrayList<>();
        root.getLeafGraph(leafGraphs);

        searchByLeafGraph(impl, leafGraphs);
        root.removeTree();
        root.init_removeNode();
        leafGraphs = null;
        treesize = root.size();
        System.out.println("tree size (枝刈り後): " + treesize);

        int[] histgram = new int[delta + 1];
        root.depth_size_histgram(histgram);
        System.out.println(Arrays.toString(histgram));

        bw.write("Tree size(new): " + treesize + "\n");
        index.write(
                treesize + "," + String.format("%.6f", (double) (System.nanoTime() - time) / 1000 / 1000 / 1000)
                        + ",");

        System.out.println(
                "remove node time(s) :"
                        + String.format("%.6f", (double) (System.nanoTime() - time) / 1000 / 1000 / 1000));

        time = System.nanoTime();
        System.out.println("グラフIDの計算中");
        root.addInfo();
        // root.sortVlabelRootChildren();
        inclusionCheck(impl, G);
        bw.write(
                "addIDtoTree(s): " + String.format("%.3f", (double) (System.nanoTime() - time) / 1000 / 1000 / 1000)
                        + "\n");
        System.out.println("\naddIDtoTree(s): " + (double) (System.nanoTime() - time) / 1000 /
                1000 / 1000);
        index.write(String.format("%.3f", (double) (System.nanoTime() - time) / 1000
                / 1000 / 1000));

        time = System.nanoTime();
        // root.addDescendantsIDs();
        // root.printCanSize();
        root.checkBacktrackNode();
        System.out.println("sortChildren(s): " + (double) (System.nanoTime() - time) / 1000 /
                1000 / 1000);

        bw.write("Build tree(s): "
                + String.format("%.6f",
                        (double) (System.nanoTime() - start) / 1000 / 1000 / 1000)
                +
                "\n");
        index.write(","
                + String.format("%.6f",
                        (double) (System.nanoTime() - start) / 1000 / 1000 / 1000)
                + ",");

        System.out.println("Build Time(s): "
                + String.format("%.6f",
                        (double) (System.nanoTime() - start) / 1000 / 1000 / 1000)
                + ",");

        try {
            String codetree = String.format("data_structure/%s.ser",
                    dataset);
            FileOutputStream fileOut = new FileOutputStream(codetree);
            ObjectOutputStream objout = new ObjectOutputStream(fileOut);
            objout.writeObject(this);
            objout.close();
            fileOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void print(List<CodeFragment> code) {
        for (CodeFragment c : code) {
            System.out.print(c.getVlabel() + ":" + Arrays.toString(c.getelabel()).toString() + " ");
        }
        System.out.println();
    }

    public CodeTree(GraphCode impl, List<Graph> G, int b) {
        this.impl = impl;
        this.root = new IndexNode(null, null, 0, true);

        System.out.print("Indexing");
        for (int i = 0; i < G.size(); ++i) {
            Graph g = G.get(i);

            List<CodeFragment> code = impl.computeCanonicalCode(g, b);// 準正準コードを得る
            root.addPath(code, i, true, 0);

            if (i % 100000 == 0) {
                System.out.println();
            } else if (i % 10000 == 0) {
                System.out.print("*");
            } else if (i % 1000 == 0) {
                System.out.print(".");
            }
        }
        root.addInfo();

        System.out.println();
        System.out.println("Tree size: " + root.size());
    }

    private void inclusionCheck(GraphCode impl, List<Graph> G) {
        for (Graph g : G) {
            if (g.id % 100000 == 0) {
            } else if (g.id % (G.size() / 2) == 0) {
                System.out.print("*");
            } else if (g.id % (G.size() / 10) == 0) {
                System.out.print(".");
            }
            root.addIDtoTree(g, impl);
            root.initTraverseNecessity();
        }
    }

    private void searchByLeafGraph(GraphCode impl, List<Graph> leafGraphs) {

        ArrayList<Integer> idList = new ArrayList<>();
        ArrayList<Integer> removeIDList = new ArrayList<>();

        for (Graph g : leafGraphs) {
            if (removeIDList.contains(g.id))
                continue;

            idList.add(g.id);
            root.pruningEquivalentNodes(g, impl, g.id, idList, removeIDList);

        }
    }

    public List<Integer> supergraphSearch(Graph query) {
        return root.search(query, impl);
    }

    public BitSet subgraphSearch(Graph query, BufferedWriter bw, String mode, String dataset,
            BufferedWriter bwout, BufferedWriter allbw, List<Graph> G,
            HashMap<Integer, ArrayList<String>> gMaps, BufferedWriter br_whole)
            throws IOException, InterruptedException {
        return root.subsearch(query, impl, bw, mode, dataset, bwout, allbw, G, "Query", gMaps, delta,
                br_whole);
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