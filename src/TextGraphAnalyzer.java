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


/**TextGraphAnalyzer: 从文本文件读取数据，构建有向加权图，并提供各种分析功能。**/
public class TextGraphAnalyzer extends JFrame {
    // 图的数据结构：邻接表，外层 key 为单词，内层 map 存储指向下一个单词及其权重（出现次数）
    private final Map<String, Map<String, Integer>> graph = new HashMap<>();
    private JTextArea outputArea; // 用于显示文本输出结果的区域
    private JLabel graphLabel; // 用于显示渲染后图像的标签

    public TextGraphAnalyzer() {
        setTitle("文本图分析器");            // 窗口标题
        setSize(1000, 700);               // 窗口大小
        setDefaultCloseOperation(EXIT_ON_CLOSE); // 关闭时退出应用
        initUI();                         // 构建界面
    }

    // 初始化用户界面布局和组件
    private void initUI() {
        JPanel panel = new JPanel(new BorderLayout());
        // 输出区：底部的可滚动文本区域
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        panel.add(new JScrollPane(outputArea), BorderLayout.SOUTH);

        // 图像展示区：中部用于显示 graph.png
        graphLabel = new JLabel();
        graphLabel.setHorizontalAlignment(JLabel.CENTER);
        JScrollPane graphScroll = new JScrollPane(graphLabel);
        panel.add(graphScroll, BorderLayout.CENTER);

        // 按钮区：顶部功能按钮
        panel.add(getControls(), BorderLayout.NORTH);
        setContentPane(panel);           // 设置主面板
    }

    // 创建功能按钮并绑定事件
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
                case "加载文本文件": btn.addActionListener(e -> loadFile()); break; // 载入并构建图
                case "展示有向图":   btn.addActionListener(e -> showDirectedGraph()); break; // 渲染
                case "查询桥接词":   btn.addActionListener(e -> queryBridgeWordsDialog()); break;
                case "生成新文本":   btn.addActionListener(e -> generateNewTextDialog()); break;
                case "最短路径":     btn.addActionListener(e -> shortestPathDialog()); break;
                case "计算PageRank": btn.addActionListener(e -> pageRankDialog()); break;
                case "随机游走":     btn.addActionListener(e -> randomWalk()); break;
            }
        }
        return controls;
    }

    // 在事件调度线程中启动 UI
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TextGraphAnalyzer().setVisible(true));
    }

    /** 功能1：加载文件、构建图、生成 dot 并展示内容 */
    private void loadFile() {
        // 弹出文件选择对话框，只显示 .txt 文件
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Text Files", "txt"));

        // 如果用户未选择“打开”则直接返回，不做任何操作
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();  // 获取用户选择的文件

        try {
            // 读取整个文件内容为一个字符串
            String content = new String(Files.readAllBytes(file.toPath()));
            buildGraph(content); // 根据内容构建图数据
            boolean ok = generateDotFile(); // 将图写入 graph.dot，返回是否成功

            // 在输出区显示节点数量及 dot 文件生成状态
            String msg = "已加载并构建图，节点数=" + graph.size()
                    + (ok ? "，graph.dot 生成成功。" : "，graph.dot 生成失败！");
            outputArea.setText(msg);

            if (ok) {
                // 读取生成的 graph.dot 文件内容
                String dot = new String(Files.readAllBytes(Paths.get("graph.dot")));
                // 在只读文本区域中显示 dot 内容
                JTextArea ta = new JTextArea(dot);
                ta.setEditable(false);
                ta.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
                JScrollPane sp = new JScrollPane(ta);
                sp.setPreferredSize(new Dimension(600, 400));
                JOptionPane.showMessageDialog(this, sp, "graph.dot 内容", JOptionPane.INFORMATION_MESSAGE);
            } else {
                // 若生成失败，弹出错误对话框
                JOptionPane.showMessageDialog(this, "graph.dot 生成失败！", "错误", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IOException ex) {
            ex.printStackTrace();  // 控制台打印异常栈
            // 弹出文件读取失败的错误提示
            JOptionPane.showMessageDialog(this, "文件读取失败: " + ex.getMessage(),
                    "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    // 将原始文本解析为有向加权图
    private void buildGraph(String raw) {
        graph.clear();
        // 将所有非英文字母字符替换为空格，并转为小写
        String clean = raw.toLowerCase().replaceAll("[^a-z]+", " ");
        // 按空白拆分成单词数组
        String[] words = clean.trim().split("\\s+");

        // 遍历相邻单词对 (a, b)
        for (int i = 0; i < words.length - 1; i++) {
            String a = words[i], b = words[i + 1];
            // 如果节点 a 不存在，则初始化它的邻接表
            graph.putIfAbsent(a, new HashMap<>());
            // 在 a 的邻接表中，将指向 b 的边权重加 1
            graph.get(a).put(b, graph.get(a).getOrDefault(b, 0) + 1);
            // 确保节点 b 也存在于图中（即使它暂时没有出边）
            graph.putIfAbsent(b, new HashMap<>());
        }
    }

    // 生成 Graphviz DOT 文件
    private boolean generateDotFile() {
        try {
            StringBuilder dot = new StringBuilder("digraph G {\n");
            for (String u : graph.keySet()) {
                for (Map.Entry<String, Integer> e : graph.get(u).entrySet()) {
                    // 按格式添加边
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

    /** 功能2：调用 Graphviz 渲染并显示 PNG */
    private void showDirectedGraph() {
        try {
            generateDotFile(); // 重新生成
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

    /** 功能3：弹窗输入并查询桥接词 */
    private void queryBridgeWordsDialog() {
        // 弹出第一个输入框，获取用户输入的 word1
        String w1 = JOptionPane.showInputDialog(this, "输入 word1:");
        // 弹出第二个输入框，获取用户输入的 word2
        String w2 = JOptionPane.showInputDialog(this, "输入 word2:");
        // 调用逻辑方法查询桥接词，并将结果保存到 res
        String res = queryBridgeWords(w1, w2);
        // 在界面下方的文本区域显示结果
        outputArea.setText(res);
        // 弹出对话框展示查询结果
        JOptionPane.showMessageDialog(this, res, "查询桥接词结果", JOptionPane.INFORMATION_MESSAGE);
    }

    // 计算两个单词之间的桥接词逻辑方法
    public String queryBridgeWords(String word1, String word2) {
        // 输入校验：确保两个单词都非空
        if (word1 == null || word2 == null || word1.trim().isEmpty() || word2.trim().isEmpty()) {
            return "请输入两个单词！";
        }
        // 统一转小写，简化匹配
        word1 = word1.toLowerCase(); word2 = word2.toLowerCase();
        // 检查图中是否存在这两个节点
        boolean has1 = graph.containsKey(word1), has2 = graph.containsKey(word2);
        if (!has1 && !has2) return "No " + word1 + " and " + word2 + " in the graph!";
        if (!has1) return "No " + word1 + " in the graph!";
        if (!has2) return "No " + word2 + " in the graph!";

        // 收集所有中间节点 mid，使得 word1->mid 且 mid->word2
        Set<String> bridges = new HashSet<>(); // 存放所有符合条件的桥接词
        // 遍历从 word1 出发的所有直接邻接节点 mid
        for (String mid : graph.get(word1).keySet()) { // mid又能直接到达 word2（即 mid 的邻接表中包含 word2）
            if (graph.get(mid).containsKey(word2)) {
                bridges.add(mid);
            }
        }
        // 如果没有找到桥接词，则返回提示
        if (bridges.isEmpty())
            return "No bridge words from \"" + word1 + "\" to \"" + word2 + "\"!";

        // 排序以保证输出顺序稳定
        List<String> list = new ArrayList<>(bridges);
        Collections.sort(list);
        // 只有一个桥接词时的返回格式
        if (list.size() == 1) {
            return "The bridge words from \"" + word1 + "\" to \"" + word2 + "\" is: \"" + list.get(0) + "\"";
        } else {
            // 拼接多个桥接词的返回格式，中间用逗号和 and 连接最后一个
            StringJoiner sj = new StringJoiner(", ");
            for (int i = 0; i < list.size() - 1; i++) sj.add(list.get(i));
            return "The bridge words from \"" + word1 + "\" to \"" + word2 + "\" are: "
                    + sj + ", and " + list.get(list.size()-1);
        }
    }

    /** 功能4：生成新文本 */
    private void generateNewTextDialog() {
        // 弹窗获取用户输入的原始文本
        String input = JOptionPane.showInputDialog(this, "输入一行新文本:");
        // 调用生成逻辑，并保存结果
        String res = generateNewText(input);
        // 在输出区域显示生成的新文本
        outputArea.setText(res);
        // 弹窗展示新文本结果
        JOptionPane.showMessageDialog(this, res, "生成新文本结果", JOptionPane.INFORMATION_MESSAGE);
    }
    
    // 生成新文本：在原文本相邻单词间随机插入桥接词
    public String generateNewText(String inputText) {
        // 输入为空则直接返回空字符串
        if (inputText == null || inputText.trim().isEmpty()) return "";
        // 预处理：非字母替为空格，转小写并拆词
        String clean = inputText.toLowerCase().replaceAll("[^a-z]+", " ").trim();
        String[] words = clean.split("\s+");
        List<String> result = new ArrayList<>(); // 用于存放最终生成的新文本单词序列
        Random rand = new Random(); // 用于稍后随机选取桥接词
        // 遍历每对相邻单词
        for (int i = 0; i < words.length - 1; i++) {
            String w1 = words[i], w2 = words[i + 1];
            result.add(w1);  // 每次循环先把当前单词w1加入result列表
            // 找出所有可能的桥接词
            Set<String> bridges = new HashSet<>(); // 新建集合 bridges 来收集所有 “w1 → mid → w2” 的中间词 mid
            if (graph.containsKey(w1)) { // 是否存在节点 w1
                for (String mid : graph.get(w1).keySet()) {
                    // 如果存在，遍历 w1 的所有出边目标 mid；再检查 mid 是否有出边指向 w2
                    if (graph.get(mid).containsKey(w2)) {
                        bridges.add(mid);
                    }
                }
            }
            // 如果有桥接词，则随机选一个插入结果中
            if (!bridges.isEmpty()) {
                List<String> b = new ArrayList<>(bridges);
                result.add(b.get(rand.nextInt(b.size())));
            }
        }
        // 最后加入最后一个单词
        result.add(words[words.length - 1]);
        // 用空格拼接成新的文本并返回
        return String.join(" ", result);
    }

    /** 功能5：最短路径，对单对或批量处理 */
    private void shortestPathDialog() {
        // 获取起点
        String w1 = JOptionPane.showInputDialog(this, "输入起点 word1:");
        // 获取终点，可为空
        String w2 = JOptionPane.showInputDialog(this, "输入终点 word2 (可留空):");
        // 计算最短路径结果
        String res = calcShortestPath(w1, w2);
        // 显示到界面文本区
        outputArea.setText(res);
        // 弹窗展示路径结果
        JOptionPane.showMessageDialog(this, res, "最短路径结果", JOptionPane.INFORMATION_MESSAGE);
    }

    // 计算最短路径。如果未指定终点，则对所有节点批量计算；否则仅计算 word1->word2 的最短路径。
    public String calcShortestPath(String word1, String word2) {
        // 输入校验：必须有起点
        if (word1 == null || word1.trim().isEmpty()) return "请输入起点单词！";
        word1 = word1.toLowerCase();
        // 起点必须存在于图中
        if (!graph.containsKey(word1)) return "No " + word1 + " in the graph!";

        // 若未输入终点，则进行批量计算，使用 Dijkstra 算法计算起点到所有其他节点的最短路径
        if (word2 == null || word2.trim().isEmpty()) {
            // 初始化距离和前驱映射
            Map<String,Integer> dist = new HashMap<>();
            Map<String,String> prev = new HashMap<>();
            // 初始距离设为无穷大
            for (String v : graph.keySet()) dist.put(v, Integer.MAX_VALUE);
            dist.put(word1, 0);  // 起点距离为 0
            // 最小优先队列，根据当前 dist 排序
            PriorityQueue<String> pq = new PriorityQueue<>(Comparator.comparingInt(dist::get));
            pq.add(word1);
            // Dijkstra 主循环
            while (!pq.isEmpty()) {
                // 从优先队列中取出当前距离起点最近的节点 u
                String u = pq.poll();
                // 遍历 u 的所有出边 (u -> v)，e.getValue() 为边权重 w
                for (var e : graph.get(u).entrySet()) {
                    String v = e.getKey();   // 邻接节点 v
                    int w = e.getValue();     // 边 u->v 的权重
                    // 判断通过 u 到达 v 的距离(dist[u] + w)是否小于当前记录的 dist[v]
                    if (dist.get(u) + w < dist.get(v)) {
                        // 如果更短，则更新 v 的最短距离
                        dist.put(v, dist.get(u) + w);
                        // 记录路径：将 v 的前驱节点设置为 u
                        prev.put(v, u);
                        // 将 v 重新加入优先队列，以便其新距离参与后续比较
                        pq.add(v);
                    }
                }
            }
            // 构造批量输出：遍历所有目标节点
            StringBuilder sb = new StringBuilder();
            for (String target : graph.keySet()) {
                if (target.equals(word1)) continue; // 跳过自己
                sb.append("从 ").append(word1).append(" 到 ").append(target).append("：");
                if (dist.get(target) == Integer.MAX_VALUE) {
                    sb.append("不可达\n");
                } else {
                    // 重建路径：从 target 沿 prev 回溯至起点
                    List<String> path = new ArrayList<>();
                    for (String cur = target; cur != null; cur = prev.get(cur)) path.add(cur);
                    Collections.reverse(path);
                    sb.append(String.join(" -> ", path))
                            .append(" (长度=").append(dist.get(target)).append(")\n");
                }
            }
            return sb.toString();
        }

        // 单对最短路径计算
        word2 = word2.toLowerCase();
        if (!graph.containsKey(word2)) return "No " + word2 + " in the graph!";
        // 初始化 dist 和 prev
        Map<String,Integer> dist = new HashMap<>();
        Map<String,String> prev = new HashMap<>();
        for (String v : graph.keySet()) dist.put(v, Integer.MAX_VALUE);
        dist.put(word1, 0);
        PriorityQueue<String> pq = new PriorityQueue<>(Comparator.comparingInt(dist::get));
        pq.add(word1);
        // Dijkstra 主循环：不断从优先队列中取出距离最小的节点，直到队列空或到达目标
        while (!pq.isEmpty()) {
            // 取出当前 dist 最小的节点 u
            String u = pq.poll();
            // 如果已经到达目标节点，则可提前退出循环，提高效率
            if (u.equals(word2)) break;
            // 遍历 u 的所有邻居 (u -> v) 及其权重 w
            for (var e : graph.get(u).entrySet()) {
                String v = e.getKey();     // 邻接节点 v
                int w = e.getValue();       // 边 u->v 的权重
                // 如果通过 u 到达 v 的新距离更短，则进行松弛操作
                if (dist.get(u) + w < dist.get(v)) {
                    // 更新 v 的最短距离
                    dist.put(v, dist.get(u) + w);
                    // 记录 v 的前驱节点为 u，用于后续重建路径
                    prev.put(v, u);
                    // 将 v 重新加入队列，以便基于更新后的 dist 再次处理
                    pq.add(v);
                }
            }
        }
        // 若终点不可达
        if (dist.get(word2) == Integer.MAX_VALUE) return "不可达!";
        // 重建并返回路径
        List<String> path = new ArrayList<>();
        for (String cur = word2; cur != null; cur = prev.get(cur)) path.add(cur);
        Collections.reverse(path);
        return "最短路径: " + String.join(" -> ", path) + "，长度=" + dist.get(word2);
    }

    /** 功能6：PageRank 计算 */
    private void pageRankDialog() {
        // 弹窗让用户输入一个单词，用于计算其 PageRank
        String w = JOptionPane.showInputDialog(this, "输入单词计算PR:");
        String res;
        // 如果用户没有输入或输入为空
        if (w == null || w.trim().isEmpty()) {
            res = "请输入单词！";
        } else {
            // 调用 calPageRank 计算该单词的 PR 值
            double pr = calPageRank(w.toLowerCase());
            // 格式化为小数点后 4 位
            res = w + " PR=" + String.format("%.4f", pr);
        }
        // 在主界面的文本区显示结果
        outputArea.setText(res);
        // 弹窗展示 PageRank 结果
        JOptionPane.showMessageDialog(this, res, "PageRank 结果", JOptionPane.INFORMATION_MESSAGE);
    }

    // 计算指定单词在当前图上的 PageRank
    public double calPageRank(String word) {
        // 如果图中没有该节点，则 PR 定义为 0
        if (!graph.containsKey(word)) return 0;

        final double d = 0.85; // 阻尼系数
        int N = graph.size();  // 节点总数
        // pr 存储当前迭代的 PR 值，prNew 存储下一轮计算值
        Map<String, Double> pr = new HashMap<>(), prNew = new HashMap<>();

        // 初始化：每个节点 PR = 1/N
        graph.keySet().forEach(v -> pr.put(v, 1.0 / N));

        // 迭代 100 次
        for (int iter = 0; iter < 100; iter++) {
            double danglingSum = 0;
            // 先累加所有“悬挂节点”（无出边节点）的 PR
            for (String u : graph.keySet()) {
                if (graph.get(u).isEmpty()) {
                    danglingSum += pr.get(u);
                }
            }
            // 对每个节点 v 计算新的 PR
            for (String v : graph.keySet()) {
                double sum = 0;
                // 来自所有指向 v 的入边贡献
                for (String u : graph.keySet()) {
                    if (graph.get(u).containsKey(v)) {
                        double weight = graph.get(u).get(v); // u->v 边权重
                        double outSum = graph.get(u).values() // u 的出边权重之和
                                .stream().mapToInt(x -> x).sum();
                        // u 对 v 的贡献 = PR(u) * (weight / outSum)
                        sum += pr.get(u) * (weight / outSum);
                    }
                }
                // 悬挂节点平均分配 + 随机跳转
                double danglingContribution = danglingSum / N;
                double teleport = (1 - d) / N;
                // 公式：PR_new(v) = teleport + d * (sum + danglingContribution)
                prNew.put(v, teleport + d * (sum + danglingContribution));
            }
            // 准备下一次迭代
            pr.putAll(prNew);
        }

        // 返回指定单词的最终 PR 值
        return pr.get(word);
    }


    /** 功能7：随机游走并写文件 */
    private void randomWalk() {
        // 空图时直接返回
        if (graph.isEmpty()) return;

        // 将所有节点装入列表，随机选择起点
        List<String> nodes = new ArrayList<>(graph.keySet()); // 将图中所有节点（graph.keySet()）拷贝到列表 nodes
        Random rand = new Random();
        String cur = nodes.get(rand.nextInt(nodes.size())); // 随机起点

        StringBuilder walk = new StringBuilder(cur); // 记录游走经过的节点序列，初始内容为起点 cur
        Set<String> seenEdges = new HashSet<>(); // 存储已走过的有向边

        // 不断沿随机出边前进
        while (true) {
            var outs = graph.get(cur);  // 当前节点的出边映射
            if (outs.isEmpty()) break; // 无出边则停止

            // 按权重随机选下一条边

            int sum = outs.values().stream().mapToInt(x -> x).sum(); // 计算所有出边权重之和 sum
            int r = rand.nextInt(sum), acc = 0; // 在[0, sum)范围内生成一个随机整数 r
            String next = null; // 用于保存被选中的下一个节点
            // 遍历每条出边，按权重区间检查随机落点 r
            for (var e : outs.entrySet()) {
                acc += e.getValue(); // 累加当前边的权重
                if (r < acc) { // 如果随机落点小于累积权重,则选中这条边对应的目标节点
                    next = e.getKey();
                    break;
                }
            }

            // 如果这条边已走过，则停止，避免循环
            String edge = cur + "->" + next;
            if (seenEdges.contains(edge)) break;
            seenEdges.add(edge);

            // 记录路径并移动当前节点
            walk.append(" ").append(next); // next即为按照权重随机选出的下一个节点
            cur = next;
        }

        String res = walk.toString();
        // 在界面和弹窗中显示游走结果
        outputArea.setText(res);
        JOptionPane.showMessageDialog(this, res, "随机游走结果", JOptionPane.INFORMATION_MESSAGE);

        // 将结果写入文件 random_walk.txt
        try {
            Files.write(Paths.get("random_walk.txt"), res.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}