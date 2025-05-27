package te;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextGraphAnalyzerTest extends TextGraphAnalyzer {

    // 用于测试的文本内容
    private static final String TEST_TEXT = "To explore strange new worlds, To seek out new life and new civilizations";

    @Test
    public void testQueryBridgeWords_Case1() {
        TextGraphAnalyzer analyzer = new TextGraphAnalyzer();
        analyzer.buildGraph(TEST_TEXT);
        String result = analyzer.queryBridgeWords("new", "to");
        assertEquals("The bridge words from \"new\" to \"to\" is: \"worlds\"", result);
    }

    @Test
    public void testQueryBridgeWords_Case2() {
        TextGraphAnalyzer analyzer = new TextGraphAnalyzer();
        analyzer.buildGraph(TEST_TEXT);
        String result = analyzer.queryBridgeWords("explore", "life");
        assertEquals("No bridge words from \"explore\" to \"life\"!", result);
    }

    @Test
    public void testQueryBridgeWords_Case3() {
        TextGraphAnalyzer analyzer = new TextGraphAnalyzer();
        analyzer.buildGraph(TEST_TEXT);
        String result = analyzer.queryBridgeWords("hello", "worlds");
        assertEquals("No hello in the graph!", result);
    }

    @Test
    public void testQueryBridgeWords_Case4() {
        TextGraphAnalyzer analyzer = new TextGraphAnalyzer();
        analyzer.buildGraph(TEST_TEXT);
        String result = analyzer.queryBridgeWords("to", "test");
        assertEquals("No test in the graph!", result);
    }

    @Test
    public void testQueryBridgeWords_Case5() {
        TextGraphAnalyzer analyzer = new TextGraphAnalyzer();
        analyzer.buildGraph(TEST_TEXT);
        String result = analyzer.queryBridgeWords("abc", "xyz");
        assertEquals("No abc and xyz in the graph!", result);
    }

    @Test
    public void testQueryBridgeWords_Case6() {
        TextGraphAnalyzer analyzer = new TextGraphAnalyzer();
        analyzer.buildGraph(TEST_TEXT);
        String result = analyzer.queryBridgeWords("", "new");
        assertEquals("请输入两个单词！", result);
    }

    @Test
    public void testQueryBridgeWords_Case7() {
        TextGraphAnalyzer analyzer = new TextGraphAnalyzer();
        analyzer.buildGraph(TEST_TEXT);
        String result = analyzer.queryBridgeWords("to", "");
        assertEquals("请输入两个单词！", result);
    }

    @Test
    public void testQueryBridgeWords_Case8() {
        TextGraphAnalyzer analyzer = new TextGraphAnalyzer();
        analyzer.buildGraph(TEST_TEXT);
        String result = analyzer.queryBridgeWords("", "");
        assertEquals("请输入两个单词！", result);
    }

    @Test
    public void testQueryBridgeWords_Case9() {
        TextGraphAnalyzer analyzer = new TextGraphAnalyzer();
        analyzer.buildGraph(TEST_TEXT);
        String result = analyzer.queryBridgeWords(null, "new");
        assertEquals("请输入两个单词！", result);
    }

    @Test
    public void testQueryBridgeWords_Case10() {
        TextGraphAnalyzer analyzer = new TextGraphAnalyzer();
        analyzer.buildGraph(TEST_TEXT);
        String result = analyzer.queryBridgeWords("to", null);
        assertEquals("请输入两个单词！", result);
    }
}