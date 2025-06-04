package te;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.ImageIcon;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.image.BufferedImage;

import java.io.File;
import java.io.IOException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.StringJoiner;
// 这是一个github修改


/**
 * te.TextGraphAnalyzer: 从文本文件读取数据，构建有向加权图，并提供各种分析功能。
 * 变更：
 * 1. calcShortestPath 支持仅输入一个单词时，显示它到所有节点的最短路径。
 * 2. showDirectedGraph 方法签名改为带参：showDirectedGraph(Map<String,Map<String,Integer>> G)
 */
public final class TextGraphAnalyzer extends JFrame {
    /**
     * 邻接表结构存储图
     */
    private final Map<String, Map<String, Integer>> graph = new HashMap<>();

    /**
     * 输出文本区域
     */
    private JTextArea outputArea;

    /**
     * 显示图形的标签
     */
    private JLabel graphLabel;
    /**
     * 统一创建的 Random 对象
     */
    private static final SecureRandom rand = new SecureRandom();
    /**
     * 按钮名称常量数组，只创建一次
     */
    private static final String[] BTN_NAMES = {
            "加载文本文件", "展示有向图",
            "查询桥接词", "生成新文本",
            "最短路径", "计算PageRank", "随机游走"
    };
    /**
     * 构造函数，初始化界面
     */
    public TextGraphAnalyzer() {
        setTitle("文本图分析器");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        initUI();      // 先把 UI 组件都加好，并给它们的滚动面板设定首选尺寸

        pack();        // 让 JFrame 根据子组件首选大小自动调整
        setLocationRelativeTo(null); // 可选：让窗口居中显示
        // 如果你还是希望窗口初始时至少 1000×700，可以在 pack() 后再做最小尺寸限制：
         setMinimumSize(new Dimension(1000, 700));
    }



    /**
     * 初始化用户界面
     */
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

    /**
     * 获取按钮控件面板
     * @return 控件面板
     */
    private JPanel getControls() {
        JPanel controls = new JPanel();
        for (String name : BTN_NAMES) {
            JButton btn = new JButton(name);
            controls.add(btn);
            switch (name) {
                case "加载文本文件":
                    btn.addActionListener(e -> loadFile());
                    break;
                case "展示有向图":
                    btn.addActionListener(e -> showDirectedGraph());
                    break;
                case "查询桥接词":
                    btn.addActionListener(e -> queryBridgeWordsDialog());
                    break;
                case "生成新文本":
                    btn.addActionListener(e -> generateNewTextDialog());
                    break;
                case "最短路径":
                    btn.addActionListener(e -> shortestPathDialog());
                    break;
                case "计算PageRank":
                    btn.addActionListener(e -> pageRankDialog());
                    break;
                case "随机游走":
                    btn.addActionListener(e -> randomWalk());
                    break;
                default:
                    throw new IllegalArgumentException("未知功能按钮：" + name);
            }
        }
        return controls;
    }
    /*
     * 程序入口。启动Swing界面
     */
    @SuppressWarnings("checkstyle:UncommentedMain")
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TextGraphAnalyzer().setVisible(true));
    }
    /** 功能1：加载并构建图，生成 .dot 并展示内容 */
    private void loadFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Text Files", "txt"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        try {
            // 指定 UTF-8 解码，避免平台默认编码问题
            String content = Files.readString(file.toPath());
            buildGraph(content);
            // 如果没有抛出异常，说明生成成功
            boolean ok = generateDotFile();
            String msg = "已加载并构建图，节点数=" + graph.size()
                    + (ok ? "，graph.dot 生成成功。" : "，graph.dot 生成失败！");
            outputArea.setText(msg);
            if (ok) {
                // 读取并弹出 .dot 文件内容，指定 UTF-8 解码
                String dot = Files.readString(Paths.get("graph.dot"));
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
            System.err.println("文件读取失败: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "文件读取失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 根据文本构建有向图
     * @param raw 原始文本
     */
    void buildGraph(String raw) {
        graph.clear();
        String clean = raw.toLowerCase().replaceAll("[^a-z]+", " ");
        String[] words = clean.trim().split("\\s+");
        for (int i = 0; i < words.length - 1; i++) {
            String a = words[i];
            String b = words[i + 1];
            graph.putIfAbsent(a, new HashMap<>());
            graph.get(a).put(b, graph.get(a).getOrDefault(b, 0) + 1);
            graph.putIfAbsent(b, new HashMap<>());
        }
    }

    /**
     * 生成dot文件
     *
     * @return 是否生成成功
     */
    private boolean generateDotFile() throws IOException {
        StringBuilder dot = new StringBuilder("digraph G {\n");
        // 用 entrySet()，避免重复 get()
        for (Map.Entry<String, Map<String, Integer>> entry : graph.entrySet()) {
            String u = entry.getKey();
            Map<String, Integer> outMap = entry.getValue();
            for (Map.Entry<String, Integer> e : outMap.entrySet()) {
                dot.append(String.format("  \"%s\" -> \"%s\" [label=%d];%n",
                        u, e.getKey(), e.getValue()));
            }
        }
        dot.append("}\n");
        Files.writeString(Paths.get("graph.dot"), dot.toString(), StandardCharsets.UTF_8);
        return true;
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
            System.err.println("渲染图形失败: " + ex.getMessage());
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

    /**
     * 查询桥接词
     * @param word1 单词1
     * @param word2 单词2
     * @return 桥接词结果描述字符串
     */
    public String queryBridgeWords(String word1, String word2) {
        if (word1 == null || word2 == null || word1.trim().isEmpty() || word2.trim().isEmpty()) {
            return "请输入两个单词！";
        }
        word1 = word1.toLowerCase();
        word2 = word2.toLowerCase();
        boolean has1 = graph.containsKey(word1);
        boolean has2 = graph.containsKey(word2);
        if (!has1 && !has2) {
            return "No " + word1 + " and " + word2 + " in the graph!";
        }
        if (!has1) {
            return "No " + word1 + " in the graph!";
        }
        if (!has2) {
            return "No " + word2 + " in the graph!";
        }
        Set<String> bridges = new HashSet<>();
        for (String mid : graph.get(word1).keySet()) {
            if (graph.get(mid).containsKey(word2)) {
                bridges.add(mid);
            }
        }
        if (bridges.isEmpty()) {
            return "No bridge words from \"" + word1 + "\" to \"" + word2 + "\"!";
        }
        List<String> list = new ArrayList<>(bridges);
        Collections.sort(list);
        if (list.size() == 1) {
            return "The bridge words from \"" + word1 + "\" to \"" + word2 + "\" is: \"" + list.getFirst() + "\"";
        } else {
            StringJoiner sj = new StringJoiner(", ");
            for (int i = 0; i < list.size() - 1; i++) {
                sj.add(list.get(i));
            }
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

    /**
     * 根据桥接词生成新文本
     * @param inputText 输入文本
     * @return 结果文本
     */
    public String generateNewText(String inputText) {
        if (inputText == null || inputText.trim().isEmpty()) {
            return "";
        }
        String clean = inputText.toLowerCase().replaceAll("[^a-z]+", " ").trim();
        String[] words = clean.split("\\s+");

        // 预估 result 大小：words.length 单词 + 最多 words.length-1 个桥接词 => 用 2*words.length
        List<String> result = new ArrayList<>(words.length * 2);

        for (int i = 0; i < words.length - 1; i++) {
            String w1 = words[i];
            String w2 = words[i + 1];
            result.add(w1);

            Map<String, Integer> nextMap = graph.get(w1);
            if (nextMap == null || nextMap.isEmpty()) {
                continue;
            }

            // 预估 bridges 容量：不会超过 nextMap.size()
            Set<String> bridges = new HashSet<>(nextMap.size());
            for (String mid : nextMap.keySet()) {
                Map<String, Integer> midMap = graph.get(mid);
                if (midMap != null && midMap.get(w2) != null) {
                    bridges.add(mid);
                }
            }

            if (!bridges.isEmpty()) {
                // bList 初始容量设为 bridges.size()
                List<String> bList = new ArrayList<>(bridges.size());
                bList.addAll(bridges);
                result.add(bList.get(rand.nextInt(bList.size())));
            }
        }

        // 最后一个单词，result 容量预留已经够用
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

    /**
     * 计算最短路径
     * @param word1 起点
     * @param word2 终点
     * @return 路径描述信息
     */
    public String calcShortestPath(String word1, String word2) {
        if (word1 == null || word1.trim().isEmpty()) {
            return "请输入起点单词！";
        }
        word1 = word1.toLowerCase();
        if (!graph.containsKey(word1)) {
            return "No " + word1 + " in the graph!";
        }

        // 若未输入终点，则对所有节点批量计算
        if (word2 == null || word2.trim().isEmpty()) {
            // 预设 dist 和 prev 的初始容量为 graph.size()
            int n = graph.size();
            Map<String, Integer> dist = new HashMap<>(n);
            Map<String, String> prev = new HashMap<>(n);

            for (String v : graph.keySet()) {
                dist.put(v, Integer.MAX_VALUE);
            }
            dist.put(word1, 0);

            // pq 最多存 n 个元素，预设容量 n
            PriorityQueue<String> pq = new PriorityQueue<>(n, Comparator.comparingInt(dist::get));
            pq.add(word1);
            while (!pq.isEmpty()) {
                String u = pq.poll();
                for (Map.Entry<String, Integer> e : graph.get(u).entrySet()) {
                    String v = e.getKey();
                    int w = e.getValue();
                    if (dist.get(u) + w < dist.get(v)) {
                        dist.put(v, dist.get(u) + w);
                        prev.put(v, u);
                        pq.add(v);
                    }
                }
            }

            StringBuilder sb = new StringBuilder();
            for (String target : graph.keySet()) {
                if (target.equals(word1)) {
                    continue;
                }
                sb.append("从 ").append(word1).append(" 到 ").append(target).append('：');
                if (dist.get(target) == Integer.MAX_VALUE) {
                    sb.append("不可达").append('\n');
                } else {
                    // 构造 path 时预分配容量为 n
                    List<String> path = new ArrayList<>(n);
                    for (String cur = target; cur != null; cur = prev.get(cur)) {
                        path.add(cur);
                    }
                    Collections.reverse(path);
                    sb.append(String.join(" -> ", path))
                            .append(" (长度=").append(dist.get(target)).append(')').append('\n');
                }
            }
            return sb.toString();
        }

        // 否则按原逻辑计算单对最短路径
        word2 = word2.toLowerCase();
        if (!graph.containsKey(word2)) {
            return "No " + word2 + " in the graph!";
        }
        int n = graph.size();
        Map<String, Integer> dist = new HashMap<>(n);
        Map<String, String> prev = new HashMap<>(n);
        for (String v : graph.keySet()) {
            dist.put(v, Integer.MAX_VALUE);
        }
        dist.put(word1, 0);

        PriorityQueue<String> pq = new PriorityQueue<>(n, Comparator.comparingInt(dist::get));
        pq.add(word1);
        while (!pq.isEmpty()) {
            String u = pq.poll();
            if (u.equals(word2)) {
                break;
            }
            for (Map.Entry<String, Integer> e : graph.get(u).entrySet()) {
                String v = e.getKey();
                int w = e.getValue();
                if (dist.get(u) + w < dist.get(v)) {
                    dist.put(v, dist.get(u) + w);
                    prev.put(v, u);
                    pq.add(v);
                }
            }
        }
        if (dist.get(word2) == Integer.MAX_VALUE) {
            return "不可达!";
        }
        List<String> path = new ArrayList<>(n);
        for (String cur = word2; cur != null; cur = prev.get(cur)) {
            path.add(cur);
        }
        Collections.reverse(path);
        return "最短路径: " + String.join(" -> ", path) + "，长度=" + dist.get(word2);
    }

    /** 功能6：计算 PageRank **/
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

    /**
     * 计算PageRank
     * @param word 单词
     * @return PageRank值
     */
    public double calPageRank(String word) {
        if (!graph.containsKey(word)) {
            return 0;
        }
        final double d = 0.85;
        int nodeCount = graph.size();

        // 【1. 预先指定 HashMap 初始容量 为节点数】
        Map<String, Double> pr = new HashMap<>(nodeCount);
        Map<String, Double> prNew = new HashMap<>(nodeCount);

        // 初始化：每个节点 PR = 1/N
        for (String v : graph.keySet()) {
            pr.put(v, 1.0 / nodeCount);
        }

        // 迭代计算
        for (int iter = 0; iter < 100; iter++) {
            // 1. 计算所有“悬挂节点”（出度为 0）的总 PR
            double danglingSum = 0.0;
            // 【2. 用 entrySet 迭代，避免重复 get()】
            for (Map.Entry<String, Map<String, Integer>> uEntry : graph.entrySet()) {
                // uEntry.getKey() → 节点名，uEntry.getValue() → 出边 Map
                if (uEntry.getValue().isEmpty()) {
                    // 该节点出度为 0，累加它的 PR
                    danglingSum += pr.get(uEntry.getKey());
                }
            }

            // 2. 对每个节点 v 计算新的 PR
            for (Map.Entry<String, Map<String, Integer>> vEntry : graph.entrySet()) {
                String v = vEntry.getKey();

                // 计算所有指向 v 的边贡献
                double sum = 0.0;
                // 由于需要遍历“入链”，这里先遍历所有 u，再看 u 是否指向 v
                for (Map.Entry<String, Map<String, Integer>> uEntry : graph.entrySet()) {
                    Map<String, Integer> uOutMap = uEntry.getValue();
                    Integer weight = uOutMap.get(v);
                    if (weight != null) {
                        // uOutMap.get(v) 已经拿到非空的 weight
                        double outSum = 0.0;
                        for (int w : uOutMap.values()) {
                            outSum += w;
                        }
                        sum += pr.get(uEntry.getKey()) * (weight / outSum);
                    }
                }

                // 计算“悬挂节点平均分摊 + 随机跳转”部分
                double danglingContribution = danglingSum / nodeCount;
                double teleport = (1 - d) / nodeCount;
                prNew.put(v, teleport + d * (sum + danglingContribution));
            }

            // 更新 pr，准备用下一轮迭代
            pr.putAll(prNew);
            // 注意：如果要严格避免 prNew 不断膨胀，prNew.putAll(prNew) 之后
            // 可以再加一句 prNew.clear()。不过 SpotBugs 仅关注预分配容量和迭代方式。
        }

        return pr.get(word);
    }

    /** 功能7：随机游走 */
    private void randomWalk() {
        if (graph.isEmpty()) {
            return;
        }
        List<String> nodes = new ArrayList<>(graph.keySet());
        String cur = nodes.get(rand.nextInt(nodes.size()));
        StringBuilder walk = new StringBuilder(cur);
        Set<String> seenEdges = new HashSet<>();

        while (true) {
            Map<String, Integer> outs = graph.get(cur);
            if (outs.isEmpty()) {
                break;
            }
            int sum = 0;
            for (int x : outs.values()) {
                sum += x;
            }
            int r = rand.nextInt(sum);

            String next = null;
            int acc = 0;
            for (Map.Entry<String, Integer> e : outs.entrySet()) {
                acc += e.getValue();
                if (r < acc) {
                    next = e.getKey();
                    break;
                }
            }
            String edge = cur + " -> " + next;
            if (seenEdges.contains(edge)) {
                break;
            }
            seenEdges.add(edge);
            // 【改用 append(char) 代替 append(" ")】
            walk.append(' ').append(next);
            cur = next;
        }
        String res = walk.toString();
        outputArea.setText(res);
        JOptionPane.showMessageDialog(this, res, "随机游走结果", JOptionPane.INFORMATION_MESSAGE);
        try {
            Files.writeString(Paths.get("random_walk.txt"), res);
        } catch (IOException e) {
            System.err.println("写文件失败: " + e.getMessage());
        }
    }
}
