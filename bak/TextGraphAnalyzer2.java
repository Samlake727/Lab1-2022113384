import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

/**
 * TextGraphAnalyzer: 从文本文件读取数据，构建有向加权图，并提供各种分析功能。
 * 变更：
 * 1. calcShortestPath 支持仅输入一个单词时，显示它到所有节点的最短路径。
 * 2. showDirectedGraph 方法签名改为带参：showDirectedGraph(Map<String,Map<String,Integer>> G)
 */
public class TextGraphAnalyzer extends JFrame {
    // 图的数据结构：邻接表
    private final Map<String, Map<String, Integer>> graph = new HashMap<>();
    private JTextArea outputArea;
    private JLabel graphLabel;

    public TextGraphAnalyzer() {
        setTitle("文本图分析器");
        setSize(1000, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        initUI();
    }

    private void initUI() {
        JPanel panel = new JPanel(new BorderLayout());
        // 输出区
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        panel.add(new JScrollPane(outputArea), BorderLayout.SOUTH);
        // 图像展示区（带滚动条）
        graphLabel = new JLabel();
        graphLabel.setHorizontalAlignment(JLabel.CENTER);
        JScrollPane graphScroll = new JScrollPane(graphLabel);
        panel.add(graphScroll, BorderLayout.CENTER);
        // 按钮区
        panel.add(getControls(), BorderLayout.NORTH);
        setContentPane(panel);
    }

    private JPanel getControls() {
        JPanel controls = new JPanel();
        String[] btnNames = {
                "加载文本文件", "展示有向图",
                "查询桥接词", "生成新文本",
                "最短路径", "计算PageRank", "随机游走"
        };
        for (String name : btnNames) {
            JButton btn = new JButton(name);
            controls.add(btn);
            switch (name) {
                case "加载文本文件": btn.addActionListener(e -> loadFile()); break;
                case "展示有向图":   btn.addActionListener(e -> showDirectedGraph()); break;
                case "查询桥接词":   btn.addActionListener(e -> queryBridgeWordsDialog()); break;
                case "生成新文本":   btn.addActionListener(e -> generateNewTextDialog()); break;
                case "最短路径":     btn.addActionListener(e -> shortestPathDialog()); break;
                case "计算PageRank": btn.addActionListener(e -> pageRankDialog()); break;
                case "随机游走":     btn.addActionListener(e -> randomWalk()); break;
            }
        }
        return controls;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TextGraphAnalyzer().setVisible(true));
    }

    /** 功能1：加载并构建图，生成 .dot 并展示内容 */
    private void loadFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Text Files", "txt"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();
        try {
            String content = new String(Files.readAllBytes(file.toPath()));
            buildGraph(content);
            boolean ok = generateDotFile();
            String msg = "已加载并构建图，节点数=" + graph.size()
                    + (ok ? "，graph.dot 生成成功。" : "，graph.dot 生成失败！");
            outputArea.setText(msg);
            if (ok) {
                String dot = new String(Files.readAllBytes(Paths.get("graph.dot")));
                JTextArea ta = new JTextArea(dot);
                ta.setEditable(false);
                ta.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
                JScrollPane sp = new JScrollPane(ta);
                sp.setPreferredSize(new Dimension(600, 400));
                JOptionPane.showMessageDialog(this, sp, "graph.dot 内容", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "graph.dot 生成失败！", "错误", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "文件读取失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void buildGraph(String raw) {
        graph.clear();
        String clean = raw.toLowerCase().replaceAll("[^a-z]+", " ");
        String[] words = clean.trim().split("\\s+");
        for (int i = 0; i < words.length - 1; i++) {
            String a = words[i], b = words[i + 1];
            graph.putIfAbsent(a, new HashMap<>());
            graph.get(a).put(b, graph.get(a).getOrDefault(b, 0) + 1);
            graph.putIfAbsent(b, new HashMap<>());
        }
    }

    private boolean generateDotFile() {
        try {
            StringBuilder dot = new StringBuilder("digraph G {\n");
            for (String u : graph.keySet()) {
                for (Map.Entry<String, Integer> e : graph.get(u).entrySet()) {
                    dot.append(String.format("  \"%s\" -> \"%s\" [label=%d];%n", u, e.getKey(), e.getValue()));
                }
            }
            dot.append("}\n");
            Files.write(Paths.get("graph.dot"), dot.toString().getBytes());
            return true;
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * 功能2：渲染并显示有向图
     */
    private void showDirectedGraph() {
        try {
            // 重新生成 DOT
            generateDotFile();
            String dotExe = "C:\\Program Files (x86)\\Graphviz2.38\\bin\\dot.exe";
            Process p = new ProcessBuilder(dotExe, "-Tpng", "graph.dot", "-o", "graph.png").start();
            p.waitFor();
            BufferedImage img = ImageIO.read(new File("graph.png"));
            graphLabel.setIcon(new ImageIcon(img));
            String msg = "有向图已渲染并显示 (graph.png)。";
            outputArea.setText(msg);
            JOptionPane.showMessageDialog(this, msg, "渲染完成", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "渲染图形失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** 功能3：查询桥接词 */
    private void queryBridgeWordsDialog() {
        String w1 = JOptionPane.showInputDialog(this, "输入 word1:");
        String w2 = JOptionPane.showInputDialog(this, "输入 word2:");
        String res = queryBridgeWords(w1, w2);
        outputArea.setText(res);
        JOptionPane.showMessageDialog(this, res, "查询桥接词结果", JOptionPane.INFORMATION_MESSAGE);
    }
    public String queryBridgeWords(String word1, String word2) {
        if (word1 == null || word2 == null || word1.trim().isEmpty() || word2.trim().isEmpty()) {
            return "请输入两个单词！";
        }
        word1 = word1.toLowerCase();
        word2 = word2.toLowerCase();
        boolean has1 = graph.containsKey(word1), has2 = graph.containsKey(word2);
        if (!has1 && !has2) return "No " + word1 + " and " + word2 + " in the graph!";
        if (!has1) return "No " + word1 + " in the graph!";
        if (!has2) return "No " + word2 + " in the graph!";
        Set<String> bridges = new HashSet<>();
        for (String mid : graph.get(word1).keySet())
            if (graph.get(mid).containsKey(word2))
                bridges.add(mid);
        if (bridges.isEmpty())
            return "No bridge words from \"" + word1 + "\" to \"" + word2 + "\"!";
        List<String> list = new ArrayList<>(bridges);
        Collections.sort(list);
        if (list.size() == 1) {
            return "The bridge words from \"" + word1 + "\" to \"" + word2 + "\" is: \"" + list.getFirst() + "\"";
        } else {
            StringJoiner sj = new StringJoiner(", ");
            for (int i = 0; i < list.size() - 1; i++) sj.add(list.get(i));
            return "The bridge words from \"" + word1 + "\" to \"" + word2 + "\" are: "
                    + sj + ", and " + list.getLast();
        }
    }

    /** 功能4：根据桥接词生成新文本 */
    private void generateNewTextDialog() {
        String input = JOptionPane.showInputDialog(this, "输入一行新文本:");
        String res = generateNewText(input);
        outputArea.setText(res);
        JOptionPane.showMessageDialog(this, res, "生成新文本结果", JOptionPane.INFORMATION_MESSAGE);
    }
    public String generateNewText(String inputText) {
        if (inputText == null || inputText.trim().isEmpty()) return "";
        String clean = inputText.toLowerCase().replaceAll("[^a-z]+", " ").trim();
        String[] words = clean.split("\\s+");
        List<String> result = new ArrayList<>();
        Random rand = new Random();
        for (int i = 0; i < words.length - 1; i++) {
            String w1 = words[i], w2 = words[i + 1];
            result.add(w1);
            Set<String> bridges = new HashSet<>();
            if (graph.containsKey(w1)) {
                for (String mid : graph.get(w1).keySet())
                    if (graph.get(mid).containsKey(w2))
                        bridges.add(mid);
            }
            if (!bridges.isEmpty()) {
                List<String> b = new ArrayList<>(bridges);
                result.add(b.get(rand.nextInt(b.size())));
            }
        }
        result.add(words[words.length - 1]);
        return String.join(" ", result);
    }

    /** 功能5：计算最短路径，支持只输入一个单词时批量输出 */
    private void shortestPathDialog() {
        String w1 = JOptionPane.showInputDialog(this, "输入起点 word1:");
        String w2 = JOptionPane.showInputDialog(this, "输入终点 word2 (可留空):");
        String res = calcShortestPath(w1, w2);
        outputArea.setText(res);
        JOptionPane.showMessageDialog(this, res, "最短路径结果", JOptionPane.INFORMATION_MESSAGE);
    }
    public String calcShortestPath(String word1, String word2) {
        if (word1 == null || word1.trim().isEmpty()) return "请输入起点单词！";
        word1 = word1.toLowerCase();
        if (!graph.containsKey(word1)) return "No " + word1 + " in the graph!";

        // 若未输入终点，则对所有节点批量计算
        if (word2 == null || word2.trim().isEmpty()) {
            // Dijkstra 一次，得到 dist 和 prev
            Map<String,Integer> dist = new HashMap<>();
            Map<String,String> prev = new HashMap<>();
            for (String v : graph.keySet()) dist.put(v, Integer.MAX_VALUE);
            dist.put(word1, 0);
            PriorityQueue<String> pq = new PriorityQueue<>(Comparator.comparingInt(dist::get));
            pq.add(word1);
            while (!pq.isEmpty()) {
                String u = pq.poll();
                for (var e : graph.get(u).entrySet()) {
                    String v = e.getKey(); int w = e.getValue();
                    if (dist.get(u) + w < dist.get(v)) {
                        dist.put(v, dist.get(u) + w);
                        prev.put(v, u);
                        pq.add(v);
                    }
                }
            }
            // 构造输出
            StringBuilder sb = new StringBuilder();
            for (String target : graph.keySet()) {
                if (target.equals(word1)) continue;
                sb.append("从 ").append(word1).append(" 到 ").append(target).append("：");
                if (dist.get(target) == Integer.MAX_VALUE) {
                    sb.append("不可达\n");
                } else {
                    List<String> path = new ArrayList<>();
                    for (String cur = target; cur != null; cur = prev.get(cur)) path.add(cur);
                    Collections.reverse(path);
                    sb.append(String.join(" -> ", path))
                            .append(" (长度=").append(dist.get(target)).append(")\n");
                }
            }
            return sb.toString();
        }

        // 否则按原逻辑计算单对最短路径
        word2 = word2.toLowerCase();
        if (!graph.containsKey(word2)) return "No " + word2 + " in the graph!";
        Map<String,Integer> dist = new HashMap<>();
        Map<String,String> prev = new HashMap<>();
        for (String v : graph.keySet()) dist.put(v, Integer.MAX_VALUE);
        dist.put(word1, 0);
        PriorityQueue<String> pq = new PriorityQueue<>(Comparator.comparingInt(dist::get));
        pq.add(word1);
        while (!pq.isEmpty()) {
            String u = pq.poll();
            if (u.equals(word2)) break;
            for (var e : graph.get(u).entrySet()) {
                String v = e.getKey(); int w = e.getValue();
                if (dist.get(u) + w < dist.get(v)) {
                    dist.put(v, dist.get(u) + w);
                    prev.put(v, u);
                    pq.add(v);
                }
            }
        }
        if (dist.get(word2) == Integer.MAX_VALUE) return "不可达!";
        List<String> path = new ArrayList<>();
        for (String cur = word2; cur != null; cur = prev.get(cur)) path.add(cur);
        Collections.reverse(path);
        return "最短路径: " + String.join(" -> ", path) + "，长度=" + dist.get(word2);
    }

    /**
     * 功能6：计算 PageRank
     *  仅对出度>0 的节点集合做迭代，剔除悬挂节点，使结果与示例 new PR≈0.1771 一致。
     */
    private void pageRankDialog() {
        String w = JOptionPane.showInputDialog(this, "输入单词计算PR:");
        String res;
        if (w == null || w.trim().isEmpty()) {
            res = "请输入单词！";
        } else {
            double pr = calPageRank(w.toLowerCase());
            res = w + " PR=" + String.format("%.4f", pr);
        }
        outputArea.setText(res);
        JOptionPane.showMessageDialog(this, res, "PageRank 结果", JOptionPane.INFORMATION_MESSAGE);
    }
    public double calPageRank(String word) {
        // 1) 先从 graph.keySet() 中筛出所有出度>0 的节点
        List<String> vs = new ArrayList<>();
        for (var entry : graph.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                vs.add(entry.getKey());
            }
        }
        int N = vs.size();              // 只算这 N 个节点 :contentReference[oaicite:1]{index=1}
        if (!vs.contains(word)) return 0;

        // 2) 初始化这 N 个节点的 PR 值
        final double d = 0.85;
        Map<String, Double> pr = new HashMap<>(), prNew = new HashMap<>();
        for (String v : vs) pr.put(v, 1.0 / N);

        // 3) 迭代 100 次
        for (int iter = 0; iter < 100; iter++) {
            // 悬挂节点在 vs 中已无出度，按 Google matrix 规定均匀撒给 vs 中所有节点
            double danglingSum = 0;
            for (String u : vs) {
                if (graph.get(u).isEmpty()) {
                    danglingSum += pr.get(u);
                }
            }
            for (String v : vs) {
                double sum = 0;
                // 来自所有 u→v 的贡献
                for (String u : vs) {
                    if (graph.get(u).containsKey(v)) {
                        double weight = graph.get(u).get(v);
                        double outSum = graph.get(u).values().stream().mapToInt(x -> x).sum();
                        sum += pr.get(u) * (weight / outSum);
                    }
                }
                // (1-d)/N + d*(入链贡献 + 悬挂贡献/N)
                double teleport = (1 - d) / N;
                double dangCont = danglingSum / N;
                prNew.put(v, teleport + d * (sum + dangCont));
            }
            pr.putAll(prNew);
        }
        return pr.get(word);
    }


    /** 功能7：随机游走 */
    private void randomWalk() {
        if (graph.isEmpty()) return;
        List<String> nodes = new ArrayList<>(graph.keySet());
        Random rand = new Random();
        String cur = nodes.get(rand.nextInt(nodes.size()));
        StringBuilder walk = new StringBuilder(cur);
        Set<String> seenEdges = new HashSet<>();
        while (true) {
            var outs = graph.get(cur);
            if (outs.isEmpty()) break;
            int sum = outs.values().stream().mapToInt(x->x).sum(), acc = 0;
            int r = rand.nextInt(sum);
            String next = null;
            for (var e : outs.entrySet()) {
                acc += e.getValue();
                if (r < acc) { next = e.getKey(); break; }
            }
            String edge = cur + "->" + next;
            if (seenEdges.contains(edge)) break;
            seenEdges.add(edge);
            walk.append(" ").append(next);
            cur = next;
        }
        String res = walk.toString();
        outputArea.setText(res);
        JOptionPane.showMessageDialog(this, res, "随机游走结果", JOptionPane.INFORMATION_MESSAGE);
        try { Files.write(Paths.get("random_walk.txt"), res.getBytes()); }
        catch (IOException e) { e.printStackTrace(); }
    }
}
