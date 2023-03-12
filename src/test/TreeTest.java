package test;

import main.java.exceptions.NodeAlreadyExistsException;
import main.java.exceptions.NotALeafException;
import main.java.exceptions.ParentDoesNotExistException;
import main.java.Tree;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
class TreeTest {
    @org.junit.jupiter.api.Test
    void removeNode() {
        Tree t1 = new Tree();
        t1.addNode(1, 2);
        assertThrows(NotALeafException.class, () -> t1.removeNode(1));
        t1.removeNode(2);
        assertNull(t1.getNode(2));
        t1.removeNode(1);
        assertNull(t1.getNode(1));
        assertNull(t1.getRoot());
        t1.addNode(1, 3);
        t1.addNode(3, 4);
        t1.addNode(3, 5);
        assertThrows(NotALeafException.class, () -> t1.removeNode(3));
    }

    @org.junit.jupiter.api.Test
    void getNode() {
        Tree t1 = new Tree();
        Tree.Node n =  t1.addNode(1, 2);
        assertEquals(n, t1.getNode(2));
        assertNull(t1.getNode(3));
    }

    @org.junit.jupiter.api.Test
    void addNode() {
    }

    @org.junit.jupiter.api.Test
    void getTreeFromStringCorrectTest() throws NodeAlreadyExistsException, ParentDoesNotExistException {
        Tree t1 = Tree.getTreeFromString("[1, 9][9, 8][1, 6][6, 5][6, 2][1, 7]");
        Tree t2 = new Tree();
        t2.addNode(1, 9);
        t2.addNode(1, 6);
        t2.addNode(1, 7);
        t2.addNode(9, 8);
        t2.addNode(6, 5);
        t2.addNode(6, 2);
        Assertions.assertEquals(t2, t1);

        t2 = new Tree();
        t2.addNode(1, 9);
        t2.addNode(1, 6);
        t2.addNode(1, 7);
        t2.addNode(9, 8);
        t2.addNode(6, 5);
        t2.addNode(6, 3);
        Assertions.assertNotEquals(t2, t1);
    }

    @org.junit.jupiter.api.Test
    void getTreeFromStringEmptyTest() throws NodeAlreadyExistsException {
        Tree t1 = Tree.getTreeFromString("");
        Tree t2 = new Tree();
        assertEquals(t1, t2);
//        assertNull(t1);
    }

    @org.junit.jupiter.api.Test
    void getTreeFromStringExceptionTest() {
        // correct, should not throw
        Assertions.assertDoesNotThrow(() -> Tree.getTreeFromString("[1, 9][9, 8][1, 6][6, 5][6, 2][1, 7]"));
        // cycle
        assertThrows(IllegalArgumentException.class, () -> Tree.getTreeFromString("[1,2][2,1][1,3]"));
        // multiple roots
        assertThrows(IllegalArgumentException.class, () -> Tree.getTreeFromString("[1,2][5,3][1,3]"));
        // illegal structure
        assertThrows(IllegalArgumentException.class, () -> Tree.getTreeFromString("[1,2][1,3"));
        assertThrows(IllegalArgumentException.class, () -> Tree.getTreeFromString("[1,2][5,1,3]"));
        assertThrows(IllegalArgumentException.class, () -> Tree.getTreeFromString("[1,2]1,3]"));
        assertThrows(IllegalArgumentException.class, () -> Tree.getTreeFromString("[1,2]1,3]"));
        assertThrows(IllegalArgumentException.class, () -> Tree.getTreeFromString("aaa[1,2]1,3]"));
        assertThrows(IllegalArgumentException.class, () -> Tree.getTreeFromString("[1,2]1,3]"));
        assertThrows(IllegalArgumentException.class, () -> Tree.getTreeFromString("[1,a]1,3]"));
        assertThrows(IllegalArgumentException.class, () -> Tree.getTreeFromString("[a, b]"));
        assertThrows(IllegalArgumentException.class, () -> Tree.getTreeFromString("[]"));
        assertThrows(IllegalArgumentException.class, () -> Tree.getTreeFromString("[1]"));
        assertThrows(NodeAlreadyExistsException.class, () -> Tree.getTreeFromString("[1,2][1,3][3,2]"));
    }

    @Test
    void saveLoadTest() throws IOException {
        assertThrows(IllegalArgumentException.class, () -> Tree.deserializeTree("resources/incorrect1"));
        assertThrows(NumberFormatException.class, () -> Tree.deserializeTree("resources/incorrect3.tt"));
        assertThrows(NumberFormatException.class, () -> Tree.deserializeTree("resources/incorrect2.tt"));
        assertThrows(NodeAlreadyExistsException.class, () -> Tree.deserializeTree("resources/incorrect1.tt"));
        Tree t = new Tree();
        t.addNode(1,2);
        t.addNode(1,5);
        t.addNode(2,3);
        assertEquals(t, Tree.deserializeTree("resources/sample.tt"));
        t.serializeTree("resources/sample1.tt");
        assertEquals(t, Tree.deserializeTree("resources/sample1.tt"));
        Tree empty = new Tree();
        empty.serializeTree("resources/empty.tt");
        assertEquals(empty, Tree.deserializeTree("resources/empty.tt"));
    }
}
