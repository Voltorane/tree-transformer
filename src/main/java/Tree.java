package main.java;

import main.java.exceptions.NodeAlreadyExistsException;
import main.java.exceptions.NotALeafException;
import main.java.exceptions.ParentDoesNotExistException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

public class Tree {
    public static class Node {
        private final int index;
        private final Node parent;
        private final HashSet<Node> children;
        private final HashSet<Integer> childrenIndexes;

        public Node(int index, Node parent) {
            this.index = index;
            this.parent = parent;
            children = new HashSet<>();
            childrenIndexes = new HashSet<>();
        }

        public HashSet<Node> getChildren() {
            return children;
        }

        public int getIndex() {
            return index;
        }

        /**
         * Removes node from children
         * @param child node to be removed
         * @throws NotALeafException if child still has children and cannot be deleted
         * */
        public void removeChild(Node child) throws NotALeafException {
            if (!child.children.isEmpty()) {
                throw new NotALeafException("Cannot remove node: " + index + "! It still has children");
            }
            children.remove(child);
            childrenIndexes.remove(child.index);
        }

        /**
         * Adds a {@link Node} with the index as a child to this node
         * @param index index of a child to be added
         * @return child node that was added
         * @throws NodeAlreadyExistsException if child with this index already exists
         * */
        public Node addChild(int index) throws NodeAlreadyExistsException {
            if (childrenIndexes.contains(index))
                throw new NodeAlreadyExistsException("Child with this index already exists!");
            Node child = new  Node(index, this);
            children.add(child);
            childrenIndexes.add(index);
            return child;
        }
    }
    public static String EXTENSION = ".tt";

    private Node root;
    private final HashMap<Integer, Node> leaves; // store for O(1) deletion
    private final HashSet<Integer> takenIndexes; // ensures that we don't have nodes with same index

    public Tree() {
        leaves = new HashMap<>();
        takenIndexes = new HashSet<>();
    }

    public Node getRoot() {
        return root;
    }

    /**
     * Removes node with the given index from the tree
     * @param index index of node to be removed
     * @throws NotALeafException if node with index is not a leaf or is not presented in the tree (implies the first one)
     * */
    public void removeNode(int index) throws NotALeafException {
        if (!leaves.containsKey(index)) {
            throw new NotALeafException("Cannot remove node: " + index + "! It is not a leaf!");
        }
        Node n = leaves.get(index);
        Node parent = n.parent;
        // tree becomes empty
        if (parent == null) {
            root = null;
        } else {
            parent.removeChild(n);
            if (parent.children.isEmpty()) {
                leaves.put(parent.getIndex(), parent);
            }
        }
        takenIndexes.remove(index);
        leaves.remove(index);
    }

    /**
     * Returns a node with index from the tree
     * @param index index of node to be deleted
     * @return node with index, {@link null} if node with this index is not present in the tree
     * */
    public Node getNode(int index) {
        if (root == null)
            return null;
        Node current;
        Queue<Node> queue = new ArrayDeque<>();
        queue.add(root);
        current = queue.poll();
        while (current != null) {
            if (current.index == index) {
                return current;
            }
            queue.addAll(current.children);
            current = queue.poll();
        }
        return null;
    }

    /**
     * Add root node with index to the tree
     * @param index index of node to be added as root
     * @return true if root was added, false if it already existed
     * */
    private boolean addRootNode(int index) {
        if (root != null) {
            return false;
        }
        root = new Node(index, null);
        takenIndexes.add(index);
        leaves.put(index, root);
        return true;
    }

    /**
     * Add node as child with childIndex to the node with parentIndex
     * @param parentIndex index of the parent node
     * @param childIndex index of the child node to be added to the parent node
     * @return nodea that was added
     * @throws NodeAlreadyExistsException if node with childIndex already exists in the tree
     * @throws ParentDoesNotExistException if node with parentIndex doesn't exist in the tree
     * */
    public Node addNode(int parentIndex, int childIndex) throws NodeAlreadyExistsException, ParentDoesNotExistException {
        if (!takenIndexes.contains(parentIndex) && !addRootNode(parentIndex)) {
            throw new ParentDoesNotExistException("Cannot add node to : " + parentIndex + "! It is not present in the tree!");
        } else if (takenIndexes.contains(childIndex)) {
            throw new NodeAlreadyExistsException("Cannot add node: " + childIndex + "! It is already present in the tree!");
        }

        Node parent = getNode(parentIndex);
        if (parent == null) {
            takenIndexes.remove(parentIndex);
            // should not happen due to previous check
            throw new RuntimeException("Cannot add node to : " + parentIndex + "! It is not present in the tree!");
        }
        Node child = parent.addChild(childIndex);
        leaves.put(childIndex, child);
        takenIndexes.add(childIndex);
        // if parent was a leaf, it should be removed from there
        leaves.remove(parentIndex);
        return child;
    }

    /**
     * Returns post order representation of the tree (postOrder(child1), postOrder(child2), ..., postOrder(childN)), parent
     * @return list of tree indexes in post order
     * */
    public static List<Integer> getPostOrder(Node root) {
        List<Integer> result;
        if (root == null) {
            result = null;
        } else {
            result =  new LinkedList<>();
            getPostOrderHelper(root, result);
        }
        return result;
    }

    /**
     * Helper function for the recursive post order representation
     * */
    private static void getPostOrderHelper(Node root, List<Integer> acc) {
        for (Node child : root.children) {
            getPostOrderHelper(child, acc);
        }
        acc.add(root.index);
    }

    /**
     * Returns true if tree was given in the correct format
     * E.g. [1,2][2,3][1,4]
     * @param s String with tree description
     * @return true if tree description was in correct format
     * */
    private static boolean checkCorrectFormat(String s) {
        Stack<Character> stack = new Stack<>();
        for (char c : s.toCharArray()) {
            if (stack.isEmpty()) {
                stack.push(c);
            } else if (c == '[' && stack.pop() != ']') {
                return false;
            } else if (c == ']' && stack.pop() != '[') {
                return false;
            }
        }
        return stack.isEmpty();
    }

    /**
     * Builds a tree top-down from the root node and by the tree definition
     * @param root index of a node that needs to be the root
     * @param treeDefinition map of node indexes as keys and their children as HasSet<Integer> values
     * @return main.java.Tree built from root and by tree definition
     * @throws NodeAlreadyExistsException if a child with same index is defined by multiple parents
     * */
    public static Tree buildTree(int root, HashMap<Integer, HashSet<Integer>> treeDefinition) throws NodeAlreadyExistsException{
        Tree tree = new Tree();
        Queue<Integer> queue = new ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            int current = queue.poll();
            if (!treeDefinition.containsKey(current)) {
                // it is a leaf and thus has no children
                continue;
            }
            for (Integer child : treeDefinition.get(current)) {
                try {
                    tree.addNode(current, child);
                } catch (ParentDoesNotExistException ignored) {
                    // not possible as all the parents come from queue from previous nodes
                }
                queue.add(child);
            }
        }
        return tree;
    }

    /**
     * Returns edges in form of a string array like ["[1, 2]", "[1, 3]", ...]
     * @param s string with edge definition
     * @return edges in form of a string array like ["[1, 2]", "[1, 3]", ...]
     * */
    public static String[] getEdges(String s) {
        return Pattern.compile("(?<=\\[).*?(?=])")
                .matcher(s)
                .results()
                .map(MatchResult::group)
                .map(String::trim)
                .toArray(String[]::new);
    }

    /**
     * Fills treeDefinition with according information.
     * After execution the structure will have the following information:
     * treeDefinition - map with parent indexes as keys and sets of children indexes
     * @param treeDefinition empty HashMap<Integer, HashSet<Integer>> for data to be stored into
     * @return index of a root node
     * @throws IllegalArgumentException if incorrect tree structure was provided i.e.:
     * tree description format was invalid, tree had cycles, tree was not connected (multiple different root nodes)
     * @throws NumberFormatException if some nodes index was not integer
     * */
    public static Integer buildTreeDefinition(HashMap<Integer, HashSet<Integer>> treeDefinition, String[] edges) {
        HashSet<Integer> childrenIndexes = new HashSet<>();
        HashSet<Integer> rootCandidates = new HashSet<>();
        for (String edge : edges) {
            // splits "[1, 2]" into ["1", "2"]
            String[] elements = edge.replaceAll(" ", "").split(",");
            if (elements.length != 2) {
                throw new IllegalArgumentException("Incorrect tree structure provided! Edge " + edge + " is invalid!");
            }
            int parent, child;
            try {
                parent = Integer.parseInt(elements[0]);
            } catch (NumberFormatException e) {
                throw new NumberFormatException("Invalid parent index provided for edge " + edge);
            }
            try {
                child = Integer.parseInt(elements[1]);
            } catch (NumberFormatException e) {
                throw new NumberFormatException("Invalid child index provided for edge " + edge);
            }

            // check for cycles
            if (treeDefinition.containsKey(child) && treeDefinition.get(child).contains(parent)) {
                throw new IllegalArgumentException("Incorrect tree structure provided! Edge " + edge + " introduces a cycle!");
            }
            if (!treeDefinition.containsKey(parent)) {
                treeDefinition.put(parent, new HashSet<>());
            }
            treeDefinition.get(parent).add(child);

            // updating root candidates
            if (!childrenIndexes.contains(parent)) {
                rootCandidates.add(parent);
            }
            rootCandidates.remove(child); // if node is a child of any other node - it cannot be the root
            childrenIndexes.add(child);
        }
        if (rootCandidates.isEmpty()) {
            throw new IllegalArgumentException("Incorrect tree structure provided! No root can be selected");
        } else if (rootCandidates.size() > 1) {
            throw new IllegalArgumentException("Incorrect tree structure provided! main.java.Tree is not connected and multiple roots" +
                    "exist!\n" + Arrays.toString(rootCandidates.toArray()));
        }
        return (Integer) rootCandidates.toArray()[0];
    }

    /**
     * Transforms a string tree description into a main.java.Tree instance if it's described correctly
     * @param s String with tree description
     * @return Tree if description was correct (never null)
     * @throws IllegalArgumentException if incorrect tree structure was provided i.e.:
     * tree description format was invalid, tree had cycles, tree was not connected (multiple different root nodes)
     * @throws NumberFormatException if some nodes index was not integer
     * @throws NodeAlreadyExistsException if some node with same index was added several times to different parents
     * */
    public static Tree getTreeFromString(String s) throws IllegalArgumentException, NodeAlreadyExistsException {
        if (s == null || s.equals("")) {
            return new Tree();
        } else if (!checkCorrectFormat(s)) {
            throw new IllegalArgumentException("Incorrect tree structure provided!");
        }
        String[] edges = getEdges(s);
        HashMap<Integer, HashSet<Integer>> treeDefinition = new HashMap<>();
        int treeRoot = buildTreeDefinition(treeDefinition, edges);
        Tree tree;
        try {
            tree = buildTree(treeRoot, treeDefinition);
        } catch (NodeAlreadyExistsException e) {
            // if multiple nodes introduced with the same index
            throw new NodeAlreadyExistsException("Tree building failed! " + e.getMessage());
        }
        return tree;
    }

    /**
     * Returns a string representation of a tree for it's serialization
     * String will then have the following structure:
     *      root node index on the first line
     *      every other next line is as follows:
     *      <parentIndex>:<child1>, <child2>, ..., <childN>
     * @param tree tree to build a string for
     * @return string representation of a tree for it's serialization
     * */
    public static String getTreeData(Tree tree) {
        if (tree.root == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(tree.getRoot().index).append("\n");
        Queue<Node> queue = new ArrayDeque<>();
        queue.add(tree.getRoot());
        Node current;
        while (!queue.isEmpty()) {
            current = queue.poll();
            if (current.children.isEmpty())
                continue;
            sb.append(current.getIndex()).append(":");
            for (Node child : current.getChildren()) {
                sb.append(child.getIndex()).append(",");
                queue.add(child);
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Serialize this tree onto the path provided
     * @param path path for tree to be serialized to (MUST end with ".tt")
     * @throws IllegalArgumentException if path was provided with incorrect extension
     * @throws IOException if file operations raised an error
     * */
    public void serializeTree(String path) throws IOException {
        if (!path.endsWith(EXTENSION)) {
            throw new IllegalArgumentException("Incorrect file format! Only " + EXTENSION + " files are supported!");
        }
        FileWriter writer = new FileWriter(path);
        writer.write(getTreeData(this));
        writer.close();
    }

    /**
     * Performs tree deserialization for the given path
     * @param path path for tree to be deserialized from
     * @return tree deserialized from path
     * @throws IllegalArgumentException if path was provided with incorrect extension
     * @throws NumberFormatException if some node index was incorrectly provided
     * @throws NodeAlreadyExistsException if some node index was provided by two different parents
     * @throws IOException if file operations raised an error
     * */
    public static Tree deserializeTree(String path) throws IOException, NodeAlreadyExistsException {
        if (!path.endsWith(EXTENSION)) {
            throw new IllegalArgumentException("Incorrect file format! Only " + EXTENSION + " files are supported!");
        }
        File file = new File(path);
        Scanner scanner = new Scanner(file);
        Tree tree = new Tree();
        if (!scanner.hasNextLine()) {
            return tree;
        } else {
            // compute first line (root)
            String root = scanner.nextLine();
            try {
                tree.addRootNode(Integer.parseInt(root));
            } catch (NumberFormatException e) {
                throw new NumberFormatException("Root specification was invalid! " + e.getMessage());
            }
        }
        while (scanner.hasNextLine()) {
            String data = scanner.nextLine().replaceAll("[ \n\t]", "");
            if (data.equals(""))
                continue;
            String[] line = data.split(":"); //parent:child1,child2,...
            if (line.length != 2) {
                throw new RuntimeException("Invalid format of data!");
            }
            try {
                int parentIndex = Integer.parseInt(line[0]);
                String[] children = line[1].split(",");
                for (String child : children) {
                    int childIndex = Integer.parseInt(child);
                    tree.addNode(parentIndex, childIndex);
                }
            } catch (NumberFormatException e) {
                throw new NumberFormatException("Index in serialization was invalid " + e.getMessage());
            } catch (NodeAlreadyExistsException e) {
                throw new NodeAlreadyExistsException("Deserialization failed: " + e.getMessage());
            } catch (ParentDoesNotExistException ignored) {
                // not possible as all the parents are explicilty provided
            }
        }
        return tree;
    }

    /**
     * Fills sb with a pretty representation of a main.java.Tree (as a tail-recursive function)
     * @param root parent node for which sb will be computed
     * @param depth depth of the next node for the tree root (0)
     * @param acc accumulator StringBuilder that will have the result
     * */
    public void getTreeStringBuilder(Node root, int depth, StringBuilder acc) {
        int i = 0;
        for (Node child : root.children) {
            acc.append("   ".repeat(Math.max(0, depth)));
            if (i < root.children.size() - 1) {
                acc.append("├──").append(child.index).append("\n");
            } else {
                acc.append("└──").append(child.index).append("\n");
            }
            i++;
            getTreeStringBuilder(child, depth + 1, acc);
        }
    }

    @Override
    public String toString() {
        if (root == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("└──").append(root.index).append("\n");
        getTreeStringBuilder(root, 1, sb);
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tree tree = (Tree) o;
        // root equality check
        if (this.root == null) {
            return tree.root == null;
        } else if (tree.root == null) {
            return false;
        } else if (this.root.getIndex() != tree.root.getIndex()) {
            return false;
        }

        Tree.Node thisNode, otherNode;
        Queue<Map.Entry<Tree.Node, Tree.Node>> queue = new ArrayDeque<>();
        queue.add(new AbstractMap.SimpleImmutableEntry<>(this.root, tree.root));
        while (!queue.isEmpty()) {
            Map.Entry<Tree.Node, Tree.Node> desiredToGivenNodes = queue.poll();
            otherNode = desiredToGivenNodes.getKey();
            thisNode = desiredToGivenNodes.getValue();
            // indexes of children of desired node
            HashMap<Integer, Tree.Node> indexesOfChildren = new HashMap<>();
            for (Tree.Node child : otherNode.getChildren()) {
                indexesOfChildren.put(child.getIndex(), child);
            }
            for (Tree.Node child : thisNode.getChildren()) {
                if (!indexesOfChildren.containsKey(child.getIndex())) {
                    // node in given tree is not in the other tree
                    return false;
                } else {
                    // node in given tree is presented in desired one and we can add it to queue
                    queue.add(new AbstractMap.SimpleImmutableEntry<>(indexesOfChildren.get(child.getIndex()), child));
                    // we don't need to additionally create this node, it's already in the given tree
                    indexesOfChildren.remove(child.getIndex());
                }
            }
            if (!indexesOfChildren.isEmpty())
                return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(root, leaves, takenIndexes);
    }
}
