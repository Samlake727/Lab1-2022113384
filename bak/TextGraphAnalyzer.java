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
 * te.TextGraphAnalyzer: 从文本文件读取数据，构建有向加权图，并提供各种分析功能。
 * 新增：自动调用 Graphviz 渲染，并在界面中展示生成的图形。
 */
public class TextGraphAnalyzer extends JFrame {
    // 有向图结构：邻接表，Map<源, Map<目标, 权重>>
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
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        panel.add(new JScrollPane(outputArea), BorderLayout.SOUTH);

        graphLabel = new JLabel();
        graphLabel.setHorizontalAlignment(JLabel.CENTER);
        panel.add(new JScrollPane(graphLabel), BorderLayout.CENTER);

        JPanel controls = new JPanel();
        JButton btnLoad = new JButton("加载文本文件");
        JButton btnShowGraph = new JButton("展示有向图");
        JButton btnQueryBridge = new JButton("查询桥接词");
        JButton btnGenText = new JButton("生成新文本");
        JButton btnShortest = new JButton("最短路径");
        JButton btnPageRank = new JButton("计算PageRank");
        JButton btnRandomWalk = new JButton("随机游走");
        controls.add(btnLoad);
        controls.add(btnShowGraph);
        controls.add(btnQueryBridge);
        controls.add(btnGenText);
        controls.add(btnShortest);
        controls.add(btnPageRank);
        controls.add(btnRandomWalk);
        panel.add(controls, BorderLayout.NORTH);
        setContentPane(panel);

        btnLoad.addActionListener(e -> loadFile());
        btnShowGraph.addActionListener(e -> showDirectedGraph());
        btnQueryBridge.addActionListener(e -> queryBridgeWordsDialog());
        btnGenText.addActionListener(e -> generateNewTextDialog());
        btnShortest.addActionListener(e -> shortestPathDialog());
        btnPageRank.addActionListener(e -> pageRankDialog());
        btnRandomWalk.addActionListener(e -> randomWalk());
    }

    /** 主程序入口 */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TextGraphAnalyzer app = new TextGraphAnalyzer();
            app.setVisible(true);
        });
    }

    /** 功能1：加载并构建图 */
    private void loadFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Text Files", "txt"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                String content = new String(Files.readAllBytes(file.toPath()));
                buildGraph(content);
                outputArea.setText("已加载并构建图，节点数=" + graph.size());
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "文件读取失败: " + ex.getMessage());
            }
        }
    }

    /** 将原始文本清洗并构建有向加权图 */
    private void buildGraph(String raw) {
        graph.clear();
        String clean = raw.toLowerCase().replaceAll("[^a-z]+", " ");
        String[] words = clean.trim().split("\\s+");
        for (int i = 0; i < words.length - 1; i++) {
            String a = words[i], b = words[i + 1];
            graph.putIfAbsent(a, new HashMap<>());
            Map<String, Integer> targets = graph.get(a);
            targets.put(b, targets.getOrDefault(b, 0) + 1);
            graph.putIfAbsent(b, new HashMap<>());
        }
    }

    /** 功能2：展示图 —— 自动渲染并显示 */
    private void showDirectedGraph() {
        try {
            // 1. 生成 DOT 文件
            StringBuilder dot = new StringBuilder("digraph G {\n");
            for (String u : graph.keySet()) {
                for (Map.Entry<String, Integer> e : graph.get(u).entrySet()) {
                    dot.append(String.format("  \"%s\" -> \"%s\" [label=%d];\n",
                            u, e.getKey(), e.getValue()));
                }
            }
            dot.append("}\n");
            Files.write(Paths.get("graph.dot"), dot.toString().getBytes());

            // 2. 调用 Graphviz 的 dot.exe 生成 PNG
            String dotExe = "C:\\Program Files (x86)\\Graphviz2.38\\bin\\dot.exe";  // ← 指定完整路径
            Process p = new ProcessBuilder(dotExe, "-Tpng", "graph.dot", "-o", "graph.png").start();
            p.waitFor();

            // 3. 载入并显示 PNG
            BufferedImage img = ImageIO.read(new File("graph.png"));
            graphLabel.setIcon(new ImageIcon(img));
            outputArea.setText("图形已渲染并显示。");
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "渲染图形失败: " + ex.getMessage());
        }
    }

    // 以下方法与原版相同：桥接词、生成新文本、最短路径、PageRank、随机游走

    private void queryBridgeWordsDialog() {
        String w1 = JOptionPane.showInputDialog(this, "输入 word1:");
        String w2 = JOptionPane.showInputDialog(this, "输入 word2:");
        outputArea.setText(queryBridgeWords(w1, w2));
    }
    public String queryBridgeWords(String word1, String word2) {
        if (!graph.containsKey(word1) || !graph.containsKey(word2)) return "No " + word1 + " or " + word2 + " in the graph!";
        Set<String> bridges = new HashSet<>();
        for (String mid : graph.get(word1).keySet()) if (graph.get(mid).containsKey(word2)) bridges.add(mid);
        if (bridges.isEmpty()) return "No bridge words from " + word1 + " to " + word2 + "!";
        return "The bridge words from " + word1 + " to " + word2 + " are: " + String.join(", ", bridges);
    }

    private void generateNewTextDialog() {
        String input = JOptionPane.showInputDialog(this, "输入一行新文本:");
        outputArea.setText(generateNewText(input));
    }
    public String generateNewText(String inputText) {
        String clean = inputText.toLowerCase().replaceAll("[^a-z]+", " ").trim();
        String[] words = clean.split("\\s+");
        List<String> result = new ArrayList<>();
        Random rand = new Random();
        for (int i = 0; i < words.length - 1; i++) {
            String w1 = words[i], w2 = words[i + 1]; result.add(w1);
            Set<String> bridges = new HashSet<>();
            if (graph.containsKey(w1)) for (String mid : graph.get(w1).keySet()) if (graph.get(mid).containsKey(w2)) bridges.add(mid);
            if (!bridges.isEmpty()) result.add(new ArrayList<>(bridges).get(rand.nextInt(bridges.size())));
        }
        result.add(words[words.length - 1]);
        return String.join(" ", result);
    }

    private void shortestPathDialog() {
        String w1 = JOptionPane.showInputDialog(this, "输入起点 word1:");
        String w2 = JOptionPane.showInputDialog(this, "输入终点 word2:");
        outputArea.setText(calcShortestPath(w1, w2));
    }
    public String calcShortestPath(String word1, String word2) {
        if (!graph.containsKey(word1) || !graph.containsKey(word2)) return "输入词不存在于图中!";
        Map<String, Integer> dist = new HashMap<>(); Map<String, String> prev = new HashMap<>();
        for (String v : graph.keySet()) dist.put(v, Integer.MAX_VALUE);
        dist.put(word1, 0);
        PriorityQueue<String> pq = new PriorityQueue<>(Comparator.comparingInt(dist::get)); pq.add(word1);
        while (!pq.isEmpty()) {
            String u = pq.poll(); if (u.equals(word2)) break;
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
        List<String> path = new ArrayList<>(); for (String cur = word2; cur != null; cur = prev.get(cur)) path.add(cur);
        Collections.reverse(path);
        return "最短路径: " + String.join(" -> ", path) + "，长度=" + dist.get(word2);
    }

    private void pageRankDialog() {
        String w = JOptionPane.showInputDialog(this, "输入单词计算PR:");
        outputArea.setText(w + " PR=" + calPageRank(w));
    }
    public double calPageRank(String word) {
        if (!graph.containsKey(word)) return 0;
        final double d = 0.85; int N = graph.size();
        Map<String, Double> pr = new HashMap<>(), prNew = new HashMap<>();
        graph.keySet().forEach(v -> pr.put(v, 1.0 / N));
        for (int i = 0; i < 100; i++) {
            for (String v : graph.keySet()) {
                double sum = 0;
                for (String u : graph.keySet()) if (graph.get(u).containsKey(v)) {
                    double weight = graph.get(u).get(v);
                    double outSum = graph.get(u).values().stream().mapToInt(x -> x).sum();
                    sum += pr.get(u) * (weight / outSum);
                }
                prNew.put(v, (1 - d) / N + d * sum);
            }
            pr.putAll(prNew);
        }
        return pr.get(word);
    }

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
            int sum = outs.values().stream().mapToInt(x -> x).sum();
            int r = rand.nextInt(sum);
            String next = null;
            for (var e : outs.entrySet()) {
                r -= e.getValue(); if (r < 0) { next = e.getKey(); break; }
            }
            String edge = cur + "->" + next;
            if (seenEdges.contains(edge)) break;
            seenEdges.add(edge);
            walk.append(" ").append(next);
            cur = next;
        }
        outputArea.setText(walk.toString());
        try { Files.write(Paths.get("random_walk.txt"), walk.toString().getBytes()); } catch (IOException e) { e.printStackTrace(); }
    }
}