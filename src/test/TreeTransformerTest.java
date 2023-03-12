package test;

import main.java.Tree;
import main.java.TreeTransformer;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;

import static main.java.TreeTransformer.getTreeFromFile;
import static main.java.TreeTransformer.handleInteractiveMode;
import static org.junit.jupiter.api.Assertions.*;

class TreeTransformerTest {
    String one = "resources/one.txt";
    String two = "resources/two.txt";
    String root_1 = "resources/root_1.txt";
    String root_2 = "resources/root_2.txt";
    String empty_file = "resources/empty.txt";

    @Test
    void getTransformationsEmptyToNotEmpty() throws IOException {
        Tree empty = getTreeFromFile(empty_file);
        Tree notEmpty = getTreeFromFile(one);
        assertEquals(new Tree(), empty);
        String transformations = TreeTransformer.getTransformations(empty, notEmpty);
        transformations = (transformations + "\nexit").replaceAll("\\), ", "\\)\n");
        InputStream in = new ByteArrayInputStream(transformations.getBytes(StandardCharsets.UTF_8));
        PrintStream out = new PrintStream(new ByteArrayOutputStream());
        PrintStream err = new PrintStream(new ByteArrayOutputStream());
        Tree rebuiltTree = handleInteractiveMode(in, out, err, new Tree());
        assertEquals(rebuiltTree, notEmpty);
    }

    @Test
    void getTransformationsNotEmptyToEmpty() throws IOException {
        Tree empty = getTreeFromFile(empty_file);
        Tree notEmpty = getTreeFromFile(one);
        assertEquals(new Tree(), empty);
        String transformations = TreeTransformer.getTransformations(notEmpty, empty);
        transformations = (transformations + "\nexit").replaceAll("\\), ", "\\)\n");
        InputStream in = new ByteArrayInputStream(transformations.getBytes(StandardCharsets.UTF_8));
        PrintStream out = new PrintStream(new ByteArrayOutputStream());
        PrintStream err = new PrintStream(new ByteArrayOutputStream());
        Tree rebuiltTree = handleInteractiveMode(in, out, err, notEmpty);
        assertEquals(rebuiltTree, empty);
    }

    @Test
    void getTransformationsForSameFilesTest() throws IOException {
        Tree tree = getTreeFromFile(one);
        String sameTrees = TreeTransformer.getTransformations(tree, tree);
        assertEquals("", sameTrees);
    }

    @Test
    void getTransformationsForDifferentRootsTest() throws IOException {
        Tree tree1 = getTreeFromFile(root_1);
        Tree tree2 = getTreeFromFile(root_2);
        String differentRoots = TreeTransformer.getTransformations(tree1, tree2);
        differentRoots = (differentRoots + "\nexit").replaceAll("\\), ", "\\)\n");
        InputStream in = new ByteArrayInputStream(differentRoots.getBytes(StandardCharsets.UTF_8));
        PrintStream out = new PrintStream(new ByteArrayOutputStream());
        PrintStream err = new PrintStream(new ByteArrayOutputStream());
        Tree rebuiltTree = handleInteractiveMode(in, out, err, tree1);
        assertEquals(rebuiltTree, tree2);
    }

    @Test
    void getTransformationsTest() throws IOException {
        Tree tree1 = getTreeFromFile(one);
        Tree tree2 = getTreeFromFile(two);
        String transformations = TreeTransformer.getTransformations(tree1, tree2);
        transformations = (transformations + "\nexit").replaceAll("\\), ", "\\)\n");
        InputStream in = new ByteArrayInputStream(transformations.getBytes(StandardCharsets.UTF_8));
        PrintStream out = new PrintStream(new ByteArrayOutputStream());
        PrintStream err = new PrintStream(new ByteArrayOutputStream());
        Tree rebuiltTree = handleInteractiveMode(in, out, err, tree1);
        assertEquals(rebuiltTree, tree2);
    }
}