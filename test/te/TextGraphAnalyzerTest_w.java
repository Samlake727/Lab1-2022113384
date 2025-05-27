package te;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextGraphAnalyzerTest_w {

    private static final String TEST_TEXT = "To explore strange new worlds, To seek out new life and new civilizations";

    @Test
    void testCalcShortestPath_path1_nullStart() {
        TextGraphAnalyzer analyzer = new TextGraphAnalyzer();
        analyzer.buildGraph(TEST_TEXT);
        String result = analyzer.calcShortestPath(null, "new");
        assertEquals("请输入起点单词！", result);
    }

    @Test
    void testCalcShortestPath_path1_notExistStart() {
        TextGraphAnalyzer analyzer = new TextGraphAnalyzer();
        analyzer.buildGraph(TEST_TEXT);
        String result = analyzer.calcShortestPath("abc", "new");
        assertEquals("No abc in the graph!", result);
    }

    @Test
    void testCalcShortestPath_path2_startAllReachable() {
        TextGraphAnalyzer analyzer = new TextGraphAnalyzer();
        analyzer.buildGraph(TEST_TEXT);
        String result = analyzer.calcShortestPath("to", "");
        // 逐项验证所有关键输出
        assertTrue(result.contains("从 to 到 new：to -> explore -> strange -> new (长度=3)"));
        assertTrue(result.contains("从 to 到 worlds：to -> explore -> strange -> new -> worlds (长度=4)"));
        assertTrue(result.contains("从 to 到 explore：to -> explore (长度=1)"));
        assertTrue(result.contains("从 to 到 and：to -> explore -> strange -> new -> life -> and (长度=5)"));
        assertTrue(result.contains("从 to 到 civilizations：to -> explore -> strange -> new -> civilizations (长度=4)"));
        assertTrue(result.contains("从 to 到 seek：to -> seek (长度=1)"));
        assertTrue(result.contains("从 to 到 strange：to -> explore -> strange (长度=2)"));
        assertTrue(result.contains("从 to 到 life：to -> explore -> strange -> new -> life (长度=4)"));
        assertTrue(result.contains("从 to 到 out：to -> seek -> out (长度=2)"));
    }

    @Test
    void testCalcShortestPath_path3_startToEndReachable() {
        TextGraphAnalyzer analyzer = new TextGraphAnalyzer();
        analyzer.buildGraph(TEST_TEXT);
        String result = analyzer.calcShortestPath("new", "to");
        assertEquals("最短路径: new -> worlds -> to，长度=2", result);
    }

    @Test
    void testCalcShortestPath_path4_startToEndUnreachable() {
        TextGraphAnalyzer analyzer = new TextGraphAnalyzer();
        analyzer.buildGraph(TEST_TEXT);
        String result = analyzer.calcShortestPath("civilizations", "to");
        assertEquals("不可达!", result);
    }
}