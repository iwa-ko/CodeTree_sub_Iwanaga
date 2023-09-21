package codetree.core;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import codetree.common.Pair;

public class IndexNode implements Serializable {
    public IndexNode parent;
    public ArrayList<IndexNode> children;
    public CodeFragment frag;
    protected ArrayList<Integer> matchGraphIndices;
    protected int count;
    protected int depth;
    protected boolean supNode;
    protected BitSet matchGraphIndicesBitSet;
    public HashSet<Byte> adjLabels;
    protected int nodeID;

    protected int traverse_num = 0;

    static BitSet Ex;
    static BitSet In = new BitSet();
    static BitSet Can = new BitSet();
    static BitSet result = new BitSet();
    static BitSet U = new BitSet();

    static ArrayList<Integer> can = new ArrayList<>();
    static final Runtime runtime = Runtime.getRuntime();
    static final Path out = Paths.get("datagraph.gfu");
    static ArrayList<String> lines = new ArrayList<>();
    static final Path path = Paths.get("output2.txt");

    static int nodeIDcount = 0;
    static long search_time = 0;
    static int totoal_kai = 0;
    static int doukeicount = 0;
    static long write_time = 0;
    static long process_time = 0;
    static long read_time = 0;
    static double verification_time = 0;
    static double total_time = 0;
    static double fileter_time = 0;
    static double verify_time = 0;
    static double query_per_veq = 0;
    static double query_per_veqF = 0;
    static double query_per_veqV = 0;
    static double query_per_sum = 0;
    static double query_per_time = 0;
    static double veq_Can_total = 0;
    static double veq_per_Can = 0;
    static double CT_verify = 0;
    static int nonfail = 0;
    static double FPper_q = 0;
    static double FPre = 0;
    static double filpertime = 0;
    static double SPper_q = 0;
    static String command;
    static Process p;
    static double FP = 0;
    static double SP = 0;
    static double FP2 = 0;
    static int fil_count = 0;
    static long a_filterTime = 0;
    static ArrayList<IndexNode> removeNode = new ArrayList<>();

    IndexNode(IndexNode parent, CodeFragment frag) {
        this.parent = parent;
        this.frag = frag;

        children = new ArrayList<>();
        matchGraphIndicesBitSet = new BitSet();
        matchGraphIndices = new ArrayList<>();

        supNode = false;
        count = 0;
        depth = 0;
        nodeID = 0;
        adjLabels = new HashSet<>();
        traverse_num = 0;
    }

    List<Integer> sizeOnDepth() {
        ArrayList<Integer> sizeList = new ArrayList<>();
        sizeList.add(children.size());

        ArrayList<IndexNode> nodeList1;
        ArrayList<IndexNode> nodeList2 = new ArrayList<>();
        nodeList2.addAll(children);

        while (!nodeList2.isEmpty()) {
            nodeList1 = nodeList2;
            nodeList2 = new ArrayList<>();
            int size = 0;
            for (IndexNode m : nodeList1) {
                size += m.children.size();
                nodeList2.addAll(m.children);
            }
            sizeList.add(size);
        }

        return sizeList;
    }

    int size() {
        int s = 1;
        for (IndexNode m : children) {
            s += m.size();
        }
        return s;
    }

    static int c = 0;
    static int query_per_c = 0;

    void find_once_node() {
        // System.out.println(this.nodeID + ":" + traverse_num);

        if (traverse_num == 1) {
            c++;
            // System.out.println(this);
        }
        for (IndexNode m : children) {
            m.find_once_node();
        }
    }

    void init_traverse() {
        traverse_num = 0;
        for (IndexNode m : children) {
            m.init_traverse();
        }
    }

    void addAdjLabels() {
        for (IndexNode m : children) {
            adjLabels.add(m.frag.getVlabel());
        }
        for (IndexNode m : children) {
            m.addAdjLabels();
        }
    }

    void getLeafGraph(List<Graph> leafGraphs) {
        for (IndexNode m : children) {
            if (m.children.size() == 0) {
                List<CodeFragment> code = m.getCode();
                Graph g = generateGraph(code, m.nodeID);
                leafGraphs.add(g);
            } else {
                m.getLeafGraph(leafGraphs);
            }
        }
    }

    Graph generateGraph(List<CodeFragment> code, int id) {
        byte[] vertices = new byte[code.size()];
        byte[][] edges = new byte[code.size()][code.size()];
        int index = 0;
        for (CodeFragment c : code) {
            vertices[index] = c.getVlabel();
            byte eLabels[] = c.getelabel();
            for (int i = 0; i < eLabels.length; i++) {
                if (eLabels[i] == 1) {
                    edges[index][i] = 1;
                    edges[i][index] = 1;
                }
            }
            index++;
        }
        return new Graph(id, vertices, edges);
    }

    List<CodeFragment> getCode() {
        List<CodeFragment> code = new ArrayList<>();
        for (IndexNode n = this; n != null; n = n.parent) {
            code.add(n.frag);
        }
        Collections.reverse(code);
        code.remove(0);
        return code;
    }

    void addPath(List<CodeFragment> code, int graphIndex, boolean supergraphSearch) {
        final int height = code.size();

        if (this.nodeID == 0) {
            nodeIDcount++;
            nodeID = nodeIDcount;
        }

        if (supergraphSearch) {
            ++count;
            supNode = true;
            if (height <= 0 && graphIndex != -1) {
                matchGraphIndices.add(graphIndex);
                return;
            }

        } else {
            if (graphIndex != -1)
                matchGraphIndicesBitSet.set(graphIndex, true);

            int dep = -1;
            for (IndexNode a = this; a != null; a = a.parent) {
                dep++;
            }
            depth = dep;
            if (height <= 0)
                return;
        }

        CodeFragment car = code.get(0);
        List<CodeFragment> cdr = code.subList(1, height);

        for (IndexNode m : children) {
            if (m.frag.equals(car)) {
                m.addPath(cdr, graphIndex, supergraphSearch);
                return;
            }
        }

        IndexNode m = new IndexNode(this, car);
        if (supergraphSearch)
            m.supNode = true;
        children.add(m);

        m.addPath(cdr, graphIndex, supergraphSearch);
    }

    List<Integer> search(Graph q, GraphCode impl) {
        HashSet<IndexNode> result0 = new HashSet<>();
        List<Pair<IndexNode, SearchInfo>> infoList = impl.beginSearch(q, this);
        for (Pair<IndexNode, SearchInfo> info : infoList) {
            if (info.left.supNode) {
                info.left.search(result0, q, info.right, impl);
            }
        }

        ArrayList<Integer> result = new ArrayList<>();
        for (IndexNode p : result0) {
            result.addAll(p.matchGraphIndices);

            final int c = p.matchGraphIndices.size();
            for (; p != null; p = p.parent) {
                p.count += c;
            }
        }

        Collections.sort(result);
        return result;
    }

    private void search(Set<IndexNode> result, Graph q, SearchInfo info, GraphCode impl) {
        final int c = matchGraphIndices.size();
        if (c > 0 && !result.contains(this)) {
            result.add(this);

            for (IndexNode p = this; p != null; p = p.parent) {// 最適化アルゴリズム発動
                p.count -= c;
            }
        }

        List<Pair<CodeFragment, SearchInfo>> nextFrags = impl.enumerateFollowableFragments(q, info, null);

        for (IndexNode m : children) {
            if (m.count > 0 && m.supNode) {
                for (Pair<CodeFragment, SearchInfo> frag : nextFrags) {
                    if (frag.left.contains1(m.frag)) {
                        m.search(result, q, frag.right, impl);
                    }
                }
            }
        }
    }

    BitSet subsearch(Graph q, GraphCode impl, int size, BufferedWriter bw, String mode, String dataset,
            BufferedWriter bw_data, BufferedWriter allbw)
            throws IOException, InterruptedException {

        long start = System.nanoTime();

        init_Bitset(q, size);

        List<Pair<IndexNode, SearchInfo>> infoList = impl.beginSearch_sub(q, this);
        for (Pair<IndexNode, SearchInfo> info : infoList) {
            info.left.subsearch(q, info.right, impl);
        }
        result.or(In);
        Can.or(Ex);
        Can.xor(In);

        fil_count += size - Can.cardinality();
        doukeicount += Can.cardinality();

        a_filterTime = System.nanoTime() - start;
        search_time += a_filterTime;// filtering time

        write_file_for_Ver(dataset);

        verification_VEQ(dataset, mode, q, size);

        write_file_indiv(q, bw_data, size);

        c = 0;
        find_once_node();
        query_per_c += c;
        // System.out.println(q.id + ":" + c);

        init_traverse();

        if (q.id == 99) {
            write_file(allbw, bw, size);
            init_param();
            System.out.println(q.size + mode + ":" + (double) query_per_c / 100);
            query_per_c = 0;
        }
        return result;
    }

    private void subsearch(Graph q, SearchInfo info, GraphCode impl) {
        Ex.and(matchGraphIndicesBitSet);

        traverse_num++;

        if (depth == q.order) {
            In.or(matchGraphIndicesBitSet);
            return;
        }

        if (children.size() == 0) {
            return;
        }

        if (!In.isEmpty()) {
            BitSet G = (BitSet) U.clone();
            G.xor(In);
            G.and(Ex); // G-In-Ex
            G.and(matchGraphIndicesBitSet);

            if (G.isEmpty()) { // if U and ID(n) = null then backtrack
                // backtrackCount++;
                return;
            }
        }

        List<Pair<CodeFragment, SearchInfo>> nextFrags = impl.enumerateFollowableFragments(q, info, adjLabels);

        for (IndexNode m : children) {
            for (Pair<CodeFragment, SearchInfo> frag : nextFrags) {
                if (m.frag.equals(frag.left)) {
                    m.subsearch(q, frag.right, impl);
                }
            }
        }
    }

    void addIDtoTree(Graph g, GraphCode impl, int id) {
        List<Pair<IndexNode, SearchInfo>> infoList = impl.beginSearch_sub(g, this);
        for (Pair<IndexNode, SearchInfo> info : infoList) {
            // ここで次数１の場合に同じ頂点ラベルあり、探索スキップ
            info.left.addIDtoTree(g, info.right, impl, id);
        }
    }

    static int pastV = 0;

    private void addIDtoTree(Graph g, SearchInfo info, GraphCode impl, int id) {
        matchGraphIndicesBitSet.set(id, true);
        if (info.getClose().cardinality() != depth) {
            System.out.println(info.getClose().cardinality() + "," + depth);
        }

        if (children.size() == 0) {
            return;
        }

        if (backtrackJudge(g, id)) {
            return;
        }

        for (int v = 0; v < g.order; v++) {
            if (!info.getOpen().get(v))
                continue;
            Pair<CodeFragment, SearchInfo> info0 = impl.enumerateFollowableFragments(g,
                    info, v, adjLabels);
            if (info0 == null)
                continue;

            for (IndexNode m : children) {
                if (info0.left.contains(m.frag)) {
                    m.addIDtoTree(g, info0.right, impl, id);
                    if (info.getClose().cardinality() > m.depth) {
                        impl.undo(g, info0.right);
                    }
                }
            }
            impl.undo(g, info0.right);
        }

        // for (int v = 0; v < g.order; v++) {
        // for (IndexNode m : children) {

        // if (!info.getOpen().get(v) || m.frag.getVlabel() != g.vertices[v])
        // continue;

        // SearchInfo info0 = impl.enumerateFollowableFragments(g, info, m, v);

        // if (info0 == null)
        // continue;
        // m.addIDtoTree(g, info0, impl, id);
        // impl.undo(g, info0, v);
        // }
        // }
    }

    private boolean backtrackJudge(Graph g, int id) {
        for (IndexNode m : children) {
            if (!m.matchGraphIndicesBitSet.get(id)) {
                return false;
            }
            if (m.depth <= g.order) {
                if (!m.backtrackJudge(g, id))
                    return false;
            }
        }
        return true;
    }

    void removeTree() {
        if (removeNode.size() == 0)
            return;
        ArrayList<IndexNode> target = new ArrayList<>();
        for (IndexNode m : children) {
            target.add(m);
        }
        for (IndexNode m : target) {
            if (removeNode.contains(m)) {
                m.parent.children.remove(m.parent.children.indexOf(m));
                for (IndexNode p = m.parent; p.children.size() < 1; p = p.parent) {
                    p.parent.children.remove(p.parent.children.indexOf(p));
                }
                removeNode.remove(removeNode.indexOf(m));
            }
            m.removeTree();
        }
    }

    void search_by_g2(Graph g, GraphCode impl, int leafID, ArrayList<Integer> idList, ArrayList<Integer> removeIDList) {
        List<Pair<IndexNode, SearchInfo>> infoList = impl.beginSearch(g, this);
        for (Pair<IndexNode, SearchInfo> info : infoList) {
            info.left.search_by_g2(g, info.right, impl, leafID, idList, removeIDList);
        }
    }

    private void search_by_g2(Graph g, SearchInfo info, GraphCode impl, int leafID, ArrayList<Integer> idList,
            ArrayList<Integer> removeIDList) {

        // this node is leaf and & not needed by index because of same path
        if (children.size() == 0 && leafID != this.nodeID && !removeNode.contains(this)
                && !idList.contains(this.nodeID)) {
            removeIDList.add(this.nodeID);
            removeNode.add(this);
            return;
        }

        List<Pair<CodeFragment, SearchInfo>> nextFrags = impl.enumerateFollowableFragments(g, info);

        for (IndexNode m : children) {
            for (Pair<CodeFragment, SearchInfo> frag : nextFrags) {
                if (frag.left.equals(m.frag)) {
                    m.search_by_g2(g, frag.right, impl, nodeID, idList, removeIDList);
                }
            }
        }
    }

    private void init_Bitset(Graph q, int size) {
        In.clear();
        Can.clear();
        result.clear();
        can.clear();
        if (q.id == 0) {
            U.clear();
            for (int j = 0; j < size; j++)
                U.flip(j);
        }
        Ex = (BitSet) U.clone();
    }

    private void outGraph(Path gPath, BufferedWriter bw2) {
        try (BufferedReader br = Files.newBufferedReader(gPath)) {
            String line;
            while ((line = br.readLine()) != null) {
                bw2.write(line + "\n");
            }
        } catch (IOException ex) {
            System.out.println(ex);
            System.exit(0);
        }

    }

    private void readAnswer(Path path, ArrayList<Integer> can, BitSet result) {
        try (BufferedReader br = Files.newBufferedReader(path)) {
            int id;
            String line;
            while ((line = br.readLine()) != null) {
                id = Integer.parseInt(line);
                result.set(can.get(id));
            }
            br.close();
        } catch (Exception e) {
            System.err.println(e);
            System.exit(1);
        }
    }

    private void write_file_for_Ver(String dataset) {
        long start = System.nanoTime();

        try (BufferedWriter bw2 = Files.newBufferedWriter(out)) {

            for (int trueIndex = Can.nextSetBit(0); trueIndex != -1; trueIndex = Can.nextSetBit(trueIndex)) {
                can.add(trueIndex);

                String file = String.format("%s/g%d.gfu", dataset, trueIndex);
                Path gPath = Paths.get(file);
                outGraph(gPath, bw2);

                trueIndex++;
            }

            bw2.close();
        } catch (IOException e) {
            System.exit(1);
        }
        write_time += System.nanoTime() - start;

    }

    private void verification_VEQ(String dataset, String mode, Graph q, int size) throws InterruptedException {
        long start = System.nanoTime();

        try {
            command = String.format("./VEQ_S -dg datagraph.gfu -qg Query/%s/%s/%d/q%d.gfu -o output2.txt",
                    dataset, mode, q.size, q.id);
            p = runtime.exec(command);
            boolean timelimit = p.waitFor(10, TimeUnit.MINUTES);
            if (!timelimit) {
                verification_time += 600000;// 10m
                query_per_time = 600000;
                query_per_veq = 0;
                veq_per_Can = 0;
                fileter_time = 0;// veq
                verify_time = 0;// veq
                a_filterTime = 0;
                FPper_q = 0;
                FPre = 0;
                filpertime = 0;
                SPper_q = 0;
            } else {
                nonfail++;
                InputStream inputStream = p.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.charAt(0) == 'F') {
                        String time = line.substring(21);
                        fileter_time = Double.parseDouble(time);
                        query_per_veqF += Double.parseDouble(time);
                    } else if (line.charAt(0) == 'V') {
                        String time = line.substring(24);
                        verify_time = Double.parseDouble(time);
                        query_per_veqV += Double.parseDouble(time);
                    } else if (line.startsWith("Number of C")) {
                        String answer = line.substring(28);
                        veq_per_Can = Double.parseDouble(answer);
                    }
                }
                query_per_veq = fileter_time + verify_time;
                CT_verify += query_per_veq;
                query_per_time = query_per_veq + (double) a_filterTime / 1000 / 1000;
                verification_time += query_per_veq;
                query_per_sum += query_per_veq;

                long start_read = System.nanoTime();
                readAnswer(path, can, result);
                read_time += System.nanoTime() - start_read;

                SPper_q = (double) In.cardinality() / result.cardinality();

                if (Can.cardinality() != 0) {
                    FPper_q = (double) (Can.cardinality() - result.cardinality()) /
                            (Can.cardinality());
                }

                if (size != result.cardinality()) {
                    FPre = (double) (size - Ex.cardinality())
                            / (size - result.cardinality());
                } else {
                    FPre = 1;
                }

                if (size - veq_per_Can != 0) {
                    filpertime = (size - veq_per_Can) / (((double) a_filterTime / 1000 / 1000) +
                            fileter_time);
                }

            }
            p.destroyForcibly();
        } catch (IOException ex) {
            System.out.println(ex);
            System.exit(0);
        }
        process_time += System.nanoTime() - start;
        totoal_kai += result.cardinality();
        veq_Can_total += veq_per_Can;
        FP += FPre;
        FP2 += FPper_q;
        SP += SPper_q;
    }

    private void write_file_indiv(Graph q, BufferedWriter bw_data, int size) throws IOException {
        if (q.id == 0) {
            bw_data.write(
                    "query_id,FP_ratio,(G-C)/(G-A),SP,filtering_time(ms),filtering_time(ms)(VEQ),verification_time(ms)(VEQ),VEQs_time(ms),query_time(ms),filtering_num,inclusion_num,Candidate_num,VEQs_Candidate_num,VEQs_filtering_num,answer_num,|G|,filter_time/filter_num\n");
        }
        bw_data.write(q.id + "," + FPper_q + "," + FPre + ","
                + SPper_q + ","
                + String.format("%.8f", (double) a_filterTime / 1000 / 1000) + ","
                + String.format("%.8f", fileter_time) + "," +
                String.format("%.8f", verify_time) + "," + String.format("%.8f",
                        query_per_veq)
                +
                "," + String.format("%.8f", query_per_time) + ","
                + (size - Ex.cardinality()) + "," + In.cardinality() + "," +
                Can.cardinality() + "," + veq_per_Can + ","
                + (Can.cardinality() - veq_per_Can) + ","
                + result.cardinality() + "," + size + ","
                + String.format("%.6f", filpertime)
                + "\n");
    }

    private void write_file(BufferedWriter allbw, BufferedWriter bw, int size) {
        try {
            allbw.write(String.format("%.5f", FP2 / nonfail) + "," +
                    String.format("%.5f", FP / nonfail) + ","
                    + String.format("%.5f", SP / nonfail) + ","
                    + String.format("%.6f", (double) search_time / 1000 / 1000 / nonfail) + ","
                    + String.format("%.6f", CT_verify / nonfail) + ","
                    + String.format("%.6f", ((double) search_time / 1000 / 1000 +
                            verification_time) / 100)
                    + ","
                    + String.format("%.6f", (((double) search_time / 1000 / 1000) / (size *
                            nonfail - doukeicount)))
                    + "," + (size * nonfail - doukeicount) + ","
                    + String.format("%.6f",
                            (size * nonfail - veq_Can_total)
                                    / (((double) search_time / 1000 / 1000) + query_per_veqF))
                    + "," + (size * nonfail - veq_Can_total) + "," + nonfail
                    + "\n");

            bw.write("(C)書き込み関数時間(ms): " + String.format("%.2f", (double) write_time /
                    1000 / 1000) + "\n");
            bw.write("(D)プロセス関数時間(ms): " + String.format("%.2f", (double) process_time /
                    1000 / 1000) + "\n");
            bw.write("(E)読み込み関数時間(ms): " + String.format("%.2f", (double) read_time /
                    1000 / 1000) + "\n");

            bw.write("Number of Filtering Graphs: " + fil_count + "\n");
            bw.write("Number of Candidate Graphs: " + doukeicount + "\n");
            bw.write("Number of Answer Graphs: " + totoal_kai + "\n");
            bw.write("filtering Presison : " + String.format("%.5f", FP / nonfail) +
                    "\n");
            bw.write("FP ratio : " + String.format("%.5f", FP2 / nonfail) + "\n");
            bw.write("inclusion Presison : " + String.format("%.5f", SP) + "%" + "\n");

            bw.write(
                    "filter_time/filter_num : "
                            + String.format("%.6f", (((double) search_time / 1000 / 1000) +
                                    query_per_veqF)
                                    / (size * nonfail - veq_Can_total))
                            + "\n");

            bw.write("(a)a VEQs Filtering Time (ms): " + String.format("%.6f",
                    query_per_veqF / nonfail) + "\n");
            bw.write("(b)a VEQs Verification Time (ms): " + String.format("%.6f",
                    query_per_veqV / nonfail) + "\n");

            bw.write("(A)a Filtering Time (ms): "
                    + String.format("%.6f", (double) search_time / 1000 / 1000 / nonfail)
                    + "\n");
            bw.write("(B=a+b)a Verification Time (ms): " + String.format("%.6f",
                    query_per_sum / nonfail)
                    + "\n");
            bw.write("(A)+(B)a Processing Time (ms): "
                    + String.format("%.6f", ((double) search_time / 1000 / 1000 +
                            verification_time) / 100)
                    + "\n");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void init_param() {
        nonfail = 0;
        CT_verify = 0;
        doukeicount = 0;
        verification_time = 0;
        process_time = 0;
        write_time = 0;
        read_time = 0;
        totoal_kai = 0;
        search_time = 0;
        query_per_veqF = 0;
        query_per_veqV = 0;
        query_per_sum = 0;
        veq_Can_total = 0;
        FP = 0;
        FP2 = 0;
        SP = 0;
        fil_count = 0;
    }
}

// / System.out.println("aboutfirst" + (double) nonfirstnode_num /
// traverse_num);
// // System.out.println("aboutnonfirst" + (double) firstnode_num /
// traverse_num);

// // about_traverse += (double) nonfirstnode_num / traverse_num;
// // about_nontraverse += (double) firstnode_num / traverse_num;
// //
// if (CodeTree.datasetSize - 1 == g.id) {
// // System.out.println(CodeTree.datasetSize);
// // System.out.println(nonfirstnode_num);
// // System.out.println(firstnode_num);
// // System.out.println(traverse_num);
// // System.out.println(about_traverse / CodeTree.datasetSize);
// // System.out.println(about_nontraverse / CodeTree.datasetSize);
// System.out.println(backtrackCount);
// traverse_num = 0;
// firstnode_num = 0;
// nonfirstnode_num = 0;
// about_traverse = 0;
// about_nontraverse = 0;
// }