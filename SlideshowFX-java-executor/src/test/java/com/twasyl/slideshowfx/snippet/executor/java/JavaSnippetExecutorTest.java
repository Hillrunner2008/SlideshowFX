package com.twasyl.slideshowfx.snippet.executor.java;

import com.twasyl.slideshowfx.snippet.executor.CodeSnippet;
import com.twasyl.slideshowfx.utils.ResourceHelper;
import org.junit.Test;

import java.io.IOException;

import static com.twasyl.slideshowfx.snippet.executor.java.JavaSnippetExecutor.CLASS_NAME_PROPERTY;
import static com.twasyl.slideshowfx.snippet.executor.java.JavaSnippetExecutor.IMPORTS_PROPERTY;
import static com.twasyl.slideshowfx.snippet.executor.java.JavaSnippetExecutor.WRAP_IN_MAIN_PROPERTY;
import static org.junit.Assert.*;

/**
 * Tests the class {@link JavaSnippetExecutor}.
 * @author Thierry Wasylczenko
 * @version 1.0
 * @since SlideshowFX 1.0.0
 */
public class JavaSnippetExecutorTest {

    private final JavaSnippetExecutor snippetExecutor = new JavaSnippetExecutor();

    @Test
    public void noClassName() {
        final CodeSnippet snippet = new CodeSnippet();

        assertEquals("Snippet", snippetExecutor.determineClassName(snippet));
    }

    @Test
    public void emptyClassName() {
        final CodeSnippet snippet = new CodeSnippet();
        snippet.getProperties().put(CLASS_NAME_PROPERTY, "");

        assertEquals("Snippet", snippetExecutor.determineClassName(snippet));
    }

    @Test
    public void givenClassName() {
        final CodeSnippet snippet = new CodeSnippet();
        snippet.getProperties().put(CLASS_NAME_PROPERTY, "ScalaTest");

        assertEquals("ScalaTest", snippetExecutor.determineClassName(snippet));
    }

    @Test
    public void hasNoImports() {
        final CodeSnippet snippet = new CodeSnippet();

        assertFalse(snippetExecutor.hasImports(snippet));
    }

    @Test
    public void hasEmptyImports() {
        final CodeSnippet snippet = new CodeSnippet();
        snippet.getProperties().put(IMPORTS_PROPERTY, "");

        assertFalse(snippetExecutor.hasImports(snippet));
    }

    @Test
    public void hasImports() {
        final CodeSnippet snippet = new CodeSnippet();
        snippet.getProperties().put(IMPORTS_PROPERTY, "import p._");

        assertTrue(snippetExecutor.hasImports(snippet));
    }

    @Test
    public void mustNotBeWrappedInMain() {
        final CodeSnippet snippet = new CodeSnippet();

        assertFalse(snippetExecutor.mustBeWrappedInMain(snippet));
    }

    @Test
    public void mustExplicitlyNotBeWrappedInMain() {
        final CodeSnippet snippet = new CodeSnippet();
        snippet.getProperties().put(WRAP_IN_MAIN_PROPERTY, "false");

        assertFalse(snippetExecutor.mustBeWrappedInMain(snippet));
    }

    @Test
    public void mustBeWrappedInMain() {
        final CodeSnippet snippet = new CodeSnippet();
        snippet.getProperties().put(WRAP_IN_MAIN_PROPERTY, "true");

        assertTrue(snippetExecutor.mustBeWrappedInMain(snippet));
    }

    @Test
    public void formatImportWithoutImportKeyword() {
        assertEquals("import mypackage;", snippetExecutor.formatImportLine("mypackage"));
    }

    @Test
    public void formatImportWithImportKeyword() {
        assertEquals("import mypackage;", snippetExecutor.formatImportLine("import mypackage;"));
    }

    @Test
    public void formatImportWithImportKeywordAndWithoutColumn() {
        assertEquals("import mypackage;", snippetExecutor.formatImportLine("import mypackage"));
    }

    @Test
    public void importsWithAndWithoutKeyword() throws IOException {
        final CodeSnippet snippet = new CodeSnippet();
        snippet.getProperties().put(IMPORTS_PROPERTY, "import mypackage\nmysecondpackage");

        assertEquals("import mypackage;\nimport mysecondpackage;", snippetExecutor.getImports(snippet));
    }

    @Test
    public void buildSourceCodeWithoutImportsAndWithoutWrapInMainAndWithoutClassName() throws IOException {
        final CodeSnippet snippet = new CodeSnippet();
        snippet.setCode("public static void main(String ... args) {\n\tSystem.out.println(\"Hello\");\n}");

        final String expected = ResourceHelper.readResource("/com/twasyl/slideshowfx/snippet/executor/java/buildSourceCodeWithoutImportsAndWithoutWrapInMainAndWithoutClassName_expected.txt");
        assertEquals(expected, snippetExecutor.buildSourceCode(snippet));
    }

    @Test
    public void buildSourceCodeWithoutImportsAndWithoutWrapInMain() throws IOException {
        final CodeSnippet snippet = new CodeSnippet();
        snippet.getProperties().put(CLASS_NAME_PROPERTY, "TestJava");

        snippet.setCode("public static void main(String ... args) {\n\tSystem.out.println(\"Hello\");\n}");

        final String expected = ResourceHelper.readResource("/com/twasyl/slideshowfx/snippet/executor/java/buildSourceCodeWithoutImportsAndWithoutWrapInMain_expected.txt");
        assertEquals(expected, snippetExecutor.buildSourceCode(snippet));
    }

    @Test
    public void buildSourceCodeWithoutWrapInMain() throws IOException {
        final CodeSnippet snippet = new CodeSnippet();
        snippet.getProperties().put(CLASS_NAME_PROPERTY, "TestJava");
        snippet.getProperties().put(IMPORTS_PROPERTY, "import mypackage\nmysecondpackage");

        snippet.setCode("public static void main(String ... args) {\n\tSystem.out.println(\"Hello\");\n}");

        final String expected = ResourceHelper.readResource("/com/twasyl/slideshowfx/snippet/executor/java/buildSourceCodeWithoutWrapInMain_expected.txt");
        assertEquals(expected, snippetExecutor.buildSourceCode(snippet));
    }

    @Test
    public void buildSourceCode() throws IOException {
        final CodeSnippet snippet = new CodeSnippet();
        snippet.getProperties().put(CLASS_NAME_PROPERTY, "TestJava");
        snippet.getProperties().put(IMPORTS_PROPERTY, "import mypackage\nmysecondpackage");
        snippet.getProperties().put(WRAP_IN_MAIN_PROPERTY, "true");

        snippet.setCode("System.out.println(\"Hello\");");

        final String expected = ResourceHelper.readResource("/com/twasyl/slideshowfx/snippet/executor/java/buildSourceCode_expected.txt");
        assertEquals(expected, snippetExecutor.buildSourceCode(snippet));
    }
}
