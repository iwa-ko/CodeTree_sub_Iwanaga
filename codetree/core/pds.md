BitSet subsearch(Graph q, GraphCode impl, BufferedWriter bw, String mode, String dataset,
            BufferedWriter bw_data, BufferedWriter allbw, List<Graph> G,
            String directory, HashMap<Integer, ArrayList<String>> gMaps, int delta, BufferedWriter br_whole)
            throws IOException, InterruptedException {

        if (q.id == 0 && q.size == 4) {
            System.out.println("\nコード木構築中辿った節点数" + traverse_cou);
            // System.out.println("backtrackNodeNum" + backtrackNodeNum);
            // System.out.println("backtrackNodeNum_leaf" + backtrackNodeNum_leaf);
            backtrackNodeNum = 0;
            backtrackNodeNum_leaf = 0;
        }

        if (fail == 50) {
            if (q.id == 99) {
                init_param();
            }
            return new BitSet();
        }
        long start = System.nanoTime();

        init_Bitset();
        init_traverse();// reset travers num
        root = this;
        Can.or(matchGraphIndicesBitSet);// Can ← G
        List<IndexNode> traversedNode = new ArrayList<>(this.children);

        List<Pair<IndexNode, SearchInfo>> infoList = impl.beginSearchforsearch(q, this);
        if (delta >= q.order) {
            for (Pair<IndexNode, SearchInfo> info : infoList) {
                traverse = true;
                int next = q.vector[info.right.getVertexIDs()[0]];
                if (next > 0) {// a != 0 && a != -1
                    q.changestartVerBitSet(info.right.getVertexIDs()[0], next);
                    info.left.doublesearch(q, info.right, impl, false, traversedNode);
                    q.backstartVerBitSet(info.right.getVertexIDs()[0], next);
                } else {
                    info.left.doublesearch(q, info.right, impl, false, traversedNode);
                }
                if (!traverse)
                    break;
            }
            result.or(In);