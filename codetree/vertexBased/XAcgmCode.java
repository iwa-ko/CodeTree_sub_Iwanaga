package codetree.vertexBased;

import java.io.Serializable;
import java.util.*;

import codetree.common.Pair;
import codetree.core.*;

public class XAcgmCode
        implements GraphCode, Serializable {
    @Override
    public List<CodeFragment> computeCanonicalCode(Graph g, int b) {
        final int n = g.order();
        ArrayList<CodeFragment> code = new ArrayList<>(n);

        ArrayList<AcgmSearchInfo> infoList1 = new ArrayList<>();
        ArrayList<AcgmSearchInfo> infoList2 = new ArrayList<>(b);

        final byte max = g.getMaxVertexLabel();// 最大ラベル（＝非頻出ラベル
        code.add(new XAcgmCodeFragment(max, 0));

        List<Integer> maxVertexList = g.getVertexList(max);
        for (int v0 : maxVertexList) {// 頂点id１の候補
            infoList1.add(new AcgmSearchInfo(g, v0));
        }

        for (int depth = 1; depth < n; ++depth) {
            XAcgmCodeFragment maxFrag = new XAcgmCodeFragment((byte) -1, depth);

            byte[] eLabels = new byte[depth];// 深さと辺ラベルの長さは同じより
            for (AcgmSearchInfo info : infoList1) {
                for (int v = 0; v < n; ++v) {
                    if (!info.open[v]) {// vが探索済みなら次の候補へ
                        continue;
                    }

                    for (int i = 0; i < depth; ++i) {// 辺ラベルの長さ＝深さ
                        final int u = info.vertexIDs[i];
                        eLabels[i] = g.edges[u][v];
                    }

                    int[] adj = g.adjList[v];

                    XAcgmCodeFragment frag = new XAcgmCodeFragment(g.vertices[v], adj.length, eLabels);
                    final int cmpres = maxFrag.isMoreCanonicalThan(frag);
                    if (cmpres < 0) {
                        maxFrag = frag;

                        infoList2.clear();// fragが更新されたのでリスト情報をリセット
                        infoList2.add(new AcgmSearchInfo(info, g, v));
                    } else if (cmpres == 0 && infoList2.size() < b) {
                        infoList2.add(new AcgmSearchInfo(info, g, v));
                    }
                }
            }

            code.add(maxFrag);

            infoList1 = infoList2;
            infoList2 = new ArrayList<>(b);
        }

        return code;
    }

    @Override
    public List<Pair<IndexNode, SearchInfo>> beginSearch(Graph graph, IndexNode root) {
        ArrayList<Pair<IndexNode, SearchInfo>> infoList = new ArrayList<>();

        for (IndexNode m : root.children) {
            for (int v = 0; v < graph.order(); ++v) {
                XAcgmCodeFragment frag = (XAcgmCodeFragment) m.frag;
                if (graph.vertices[v] == frag.vLabel) {
                    infoList.add(new Pair<IndexNode, SearchInfo>(m, new AcgmSearchInfo(graph, v)));
                }
            }
        }

        return infoList;
    }

    @Override
    public List<Pair<CodeFragment, SearchInfo>> enumerateFollowableFragments(Graph graph, SearchInfo info0) {
        ArrayList<Pair<CodeFragment, SearchInfo>> frags = new ArrayList<>();

        AcgmSearchInfo info = (AcgmSearchInfo) info0;

        final int n = graph.order();
        final int depth = info.vertexIDs.length;

        byte[] eLabels = new byte[depth];
        for (int v = 0; v < n; ++v) {
            if (!info.open[v]) {
                continue;
            }

            for (int i = 0; i < depth; ++i) {
                int u = info.vertexIDs[i];
                eLabels[i] = graph.edges[u][v];
            }

            int[] adj = graph.adjList[v];

            frags.add(new Pair<CodeFragment, SearchInfo>(
                    new XAcgmCodeFragment(graph.vertices[v], adj.length, eLabels),
                    new AcgmSearchInfo(info, graph, v)));
        }

        return frags;
    }

    @Override
    public List<CodeFragment> computeCanonicalCode(Graph g, int start, int limDepth) {
        final int n = g.order();
        ArrayList<CodeFragment> code = new ArrayList<>(n);

        ArrayList<AcgmSearchInfo> infoList1 = new ArrayList<>();

        code.add(new XAcgmCodeFragment(g.vertices[start], 0));

        // int[] adj2 = g.adjList[start];
        // byte [] e = new byte[1];
        // e[0] = 0;

        // code.add(new XAcgmCodeFragment(g.vertices[start],adj2.length, e));

        infoList1.add(new AcgmSearchInfo(g, start));

        Random rand = new Random(0);

        for (int depth = 1; depth < limDepth; ++depth) {

            byte[] eLabels = new byte[depth];// 深さと辺ラベルの長さは同じより
            ArrayList<Integer> next = new ArrayList<>();
            for (AcgmSearchInfo info : infoList1) {

                for (int v = 0; v < n; ++v) {
                    if (info.open[v]) {
                        next.add(v);
                    }
                }
                if (next.size() == 0) {
                    return code;
                }

                int random = rand.nextInt(next.size());
                int v2 = next.get(random);

                for (int i = 0; i < depth; ++i) {
                    final int u = info.vertexIDs[i];
                    eLabels[i] = g.edges[u][v2];
                }

                int[] adj = g.adjList[v2];

                XAcgmCodeFragment frag = new XAcgmCodeFragment(g.vertices[v2], adj.length, eLabels);
                infoList1.clear();
                infoList1.add(new AcgmSearchInfo(info, g, v2));
                code.add(frag);
            }
        }
        return code;
    }

    @Override
    public List<Pair<CodeFragment, SearchInfo>> enumerateFollowableFragments(Graph graph, SearchInfo info0,
            ArrayList<Byte> childrenVlabel) {

        ArrayList<Pair<CodeFragment, SearchInfo>> frags = new ArrayList<>();

        AcgmSearchInfo info = (AcgmSearchInfo) info0;

        final int n = graph.order();
        final int depth = info.vertexIDs.length;

        byte[] eLabels = new byte[depth];
        for (int v = 0; v < n; ++v) {
            if (!info.open[v] || !childrenVlabel.contains(graph.vertices[v])) {
                continue;
            }

            for (int i = 0; i < depth; ++i) {
                int u = info.vertexIDs[i];
                eLabels[i] = graph.edges[u][v];
            }

            int[] adj = graph.adjList[v];

            frags.add(new Pair<CodeFragment, SearchInfo>(
                    new XAcgmCodeFragment(graph.vertices[v], adj.length, eLabels),
                    new AcgmSearchInfo(info, graph, v)));
        }

        return frags;
    }

    @Override
    public List<ArrayList<CodeFragment>> computeCanonicalCode(int labels_length) {
        List<ArrayList<CodeFragment>> codeList = new ArrayList<>(labels_length);
        for (int i = 0; i < labels_length; i++) {
            ArrayList<CodeFragment> code = new ArrayList<>(1);
            code.add(new XAcgmCodeFragment((byte) i, 0));
            codeList.add(code);
        }
        return codeList;
    }

}
// package codetree.vertexBased;

// import java.util.*;

// import codetree.common.Pair;
// import codetree.core.*;

// public class XAcgmCode
// implements GraphCode{

// @Override
// public List<CodeFragment> computeCanonicalCode(Graph g, int b) {
// final int n = g.order();
// ArrayList<CodeFragment> code = new ArrayList<>(n);

// ArrayList<AcgmSearchInfo> infoList1 = new ArrayList<>();
// ArrayList<AcgmSearchInfo> infoList2 = new ArrayList<>(b);

// final byte max = g.getMaxVertexLabel();// 最大ラベル（＝非頻出ラベル
// code.add(new XAcgmCodeFragment(max, 0));

// List<Integer> maxVertexList = g.getVertexList(max);
// for (int v0 : maxVertexList) {// 頂点id１の候補
// infoList1.add(new AcgmSearchInfo(g, v0));
// }

// for (int depth = 1; depth < n; ++depth) {
// XAcgmCodeFragment maxFrag = new XAcgmCodeFragment((byte) -1, depth);

// byte[] eLabels = new byte[depth];// 深さと辺ラベルの長さは同じより
// for (AcgmSearchInfo info : infoList1) {
// for (int v = 0; v < n; ++v) {
// if (!info.open[v]) {// vが探索済みなら次の候補へ
// continue;
// }

// for (int i = 0; i < depth; ++i) {// 辺ラベルの長さ＝深さ
// final int u = info.vertexIDs[i];
// eLabels[i] = g.edges[u][v];
// }

// int[] adj = g.adjList[v];

// int e = 0;
// for (int u : adj) {// 線度計算
// if (info.closed[u]) {
// ++e;
// }
// }

// XAcgmCodeFragment frag = new XAcgmCodeFragment(g.vertices[v], e, adj.length,
// eLabels);
// final int cmpres = maxFrag.isMoreCanonicalThan(frag);
// if (cmpres < 0) {
// maxFrag = frag;

// infoList2.clear();// fragが更新されたのでリスト情報をリセット
// infoList2.add(new AcgmSearchInfo(info, g, v));
// } else if (cmpres == 0 && infoList2.size() < b) {
// infoList2.add(new AcgmSearchInfo(info, g, v));
// }
// }
// }

// code.add(maxFrag);

// infoList1 = infoList2;
// infoList2 = new ArrayList<>(b);
// }

// return code;
// }

// @Override
// public List<Pair<IndexNode, SearchInfo>> beginSearch(Graph graph, IndexNode
// root) {
// ArrayList<Pair<IndexNode, SearchInfo>> infoList = new ArrayList<>();

// for (IndexNode m : root.children) {
// for (int v = 0; v < graph.order(); ++v) {
// XAcgmCodeFragment frag = (XAcgmCodeFragment) m.frag;
// if (graph.vertices[v] == frag.vLabel) {
// infoList.add(new Pair<IndexNode, SearchInfo>(m, new AcgmSearchInfo(graph,
// v)));
// }
// }
// }

// return infoList;
// }

// @Override
// public List<Pair<CodeFragment, SearchInfo>>
// enumerateFollowableFragments(Graph graph, SearchInfo info0) {
// ArrayList<Pair<CodeFragment, SearchInfo>> frags = new ArrayList<>();

// AcgmSearchInfo info = (AcgmSearchInfo) info0;

// final int n = graph.order();
// final int depth = info.vertexIDs.length;

// byte[] eLabels = new byte[depth];
// for (int v = 0; v < n; ++v) {
// if (!info.open[v]) {
// continue;
// }

// for (int i = 0; i < depth; ++i) {
// int u = info.vertexIDs[i];
// eLabels[i] = graph.edges[u][v];
// }

// int[] adj = graph.adjList[v];

// int e = 0;
// for (int u : adj) {
// if (info.closed[u]) {
// ++e;
// }
// }

// frags.add(new Pair<CodeFragment, SearchInfo>(
// new XAcgmCodeFragment(graph.vertices[v], e, adj.length, eLabels),
// new AcgmSearchInfo(info, graph, v)));
// }

// return frags;
// }

// @Override
// public List<CodeFragment> computeCanonicalCode(Graph g, int start, int
// limDepth) {
// final int n = g.order();
// ArrayList<CodeFragment> code = new ArrayList<>(n);

// ArrayList<AcgmSearchInfo> infoList1 = new ArrayList<>();

// code.add(new XAcgmCodeFragment(g.vertices[start], 0));

// infoList1.add(new AcgmSearchInfo(g, start));

// Random rand = new Random(0);

// for (int depth = 1; depth < limDepth; ++depth) {

// byte[] eLabels = new byte[depth];// 深さと辺ラベルの長さは同じより
// ArrayList<Integer> next = new ArrayList<>();
// for (AcgmSearchInfo info : infoList1) {

// for (int v = 0; v < n; ++v) {
// if (info.open[v]) {
// next.add(v);
// }
// }
// if (next.size() == 0) {
// return code;
// }

// int random = rand.nextInt(next.size());
// int v2 = next.get(random);

// for (int i = 0; i < depth; ++i) {
// final int u = info.vertexIDs[i];
// eLabels[i] = g.edges[u][v2];
// }

// int[] adj = g.adjList[v2];

// int e = 0;
// for (int u : adj) {// 線度計算
// if (info.closed[u]) {
// ++e;
// }
// }

// XAcgmCodeFragment frag = new XAcgmCodeFragment(g.vertices[v2], e, adj.length,
// eLabels);
// infoList1.clear();
// infoList1.add(new AcgmSearchInfo(info, g, v2));
// code.add(frag);
// }
// }
// return code;
// }

// @Override
// public List<Pair<CodeFragment, SearchInfo>>
// enumerateFollowableFragments(Graph graph, SearchInfo info0,
// ArrayList<Byte> childrenVlabel) {

// ArrayList<Pair<CodeFragment, SearchInfo>> frags = new ArrayList<>();

// AcgmSearchInfo info = (AcgmSearchInfo) info0;

// final int n = graph.order();
// final int depth = info.vertexIDs.length;

// byte[] eLabels = new byte[depth];
// for (int v = 0; v < n; ++v) {
// if (!info.open[v] || !childrenVlabel.contains(graph.vertices[v])) {
// continue;
// }

// for (int i = 0; i < depth; ++i) {
// int u = info.vertexIDs[i];
// eLabels[i] = graph.edges[u][v];
// }

// int[] adj = graph.adjList[v];

// int e = 0;
// for (int u : adj) {
// if (info.closed[u]) {
// ++e;
// }
// }

// frags.add(new Pair<CodeFragment, SearchInfo>(
// new XAcgmCodeFragment(graph.vertices[v], e, adj.length, eLabels),
// new AcgmSearchInfo(info, graph, v)));
// }

// return frags;
// }

// @Override
// public List<ArrayList<CodeFragment>> computeCanonicalCode(int labels_length)
// {
// List<ArrayList<CodeFragment>> codeList = new ArrayList<>(labels_length);
// for (int i = 0; i < labels_length; i++) {
// ArrayList<CodeFragment> code = new ArrayList<>(1);
// code.add(new AcgmCodeFragment((byte) i, 0));
// codeList.add(code);
// }
// return codeList;
// }

// @Override
// public List<ArrayList<CodeFragment>> computeCanonicalCode2(int[] labels) {
// List<ArrayList<CodeFragment>> codeList = new ArrayList<>(labels.length *
// labels.length);
// byte[] eLabels = new byte[1];
// eLabels[0] = 1;

// for (int i = 0; i < labels.length; i++) {
// CodeFragment c = new AcgmCodeFragment((byte) i, 0);
// for (int j = i; j < labels.length; j++) {
// ArrayList<CodeFragment> code = new ArrayList<>(2);
// CodeFragment c2 = new AcgmCodeFragment((byte) j, eLabels);
// code.add(c);
// code.add(c2);
// codeList.add(code);
// }
// }
// return codeList;
// }

// @Override
// public List<ArrayList<CodeFragment>> computeCanonicalCode3(int[] labels) {
// List<ArrayList<CodeFragment>> codeList = new ArrayList<>(labels.length *
// labels.length);
// byte[] eLabels = new byte[1];
// byte[] eLabels2 = new byte[2];
// byte[] eLabels3 = new byte[2];
// byte[] eLabels4 = new byte[2];
// eLabels[0] = 1;
// eLabels2[0] = 1;
// eLabels3[1] = 1;
// eLabels4[0] = 1;
// eLabels4[1] = 1;

// for (int i = 0; i < labels.length; i++) {
// CodeFragment c = new AcgmCodeFragment((byte) i, 0);
// // for (int j = 0; j < labels.length; j++) {
// for (int j = i; j < labels.length; j++) {// improve var?
// CodeFragment c2 = new AcgmCodeFragment((byte) j, eLabels);
// for (int k = 0; k < labels.length; k++) {
// CodeFragment c3 = new AcgmCodeFragment((byte) k, eLabels2);
// CodeFragment c4 = new AcgmCodeFragment((byte) k, eLabels3);
// CodeFragment c5 = new AcgmCodeFragment((byte) k, eLabels4);
// ArrayList<CodeFragment> code = new ArrayList<>(3);
// ArrayList<CodeFragment> code2 = new ArrayList<>(3);
// ArrayList<CodeFragment> code3 = new ArrayList<>(3);
// code.add(c);
// code.add(c2);
// code.add(c3);
// code2.add(c);
// code2.add(c2);
// code2.add(c4);
// code3.add(c);
// code3.add(c2);
// code3.add(c5);
// codeList.add(code);
// codeList.add(code2);
// codeList.add(code3);
// }
// }
// }
// return codeList;
// }
// }
