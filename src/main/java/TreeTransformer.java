package main.java;

import main.java.Tree.Node;
import main.java.exceptions.NodeAlreadyExistsException;
import main.java.exceptions.NotALeafException;
import main.java.exceptions.ParentDoesNotExistException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TreeTransformer {
    public static final String exitCommand = "exit";
    public static final String helpMessage = """
                    Tree Transformer supports the following commands:
                    ADD(<int: parent_index>, <int: child_index>), REMOVE(<int: leaf index>), SAVE <filename>.tt, LOAD <filename>.tt, EXIT
                    Please enter your command:""";
    public static final String interactiveGreet = "Welcome to the interactive mode of Tree Transformer!";
    public static HashMap<String, String> supportedCommandsMap;

    /**
     * Returns a sequence of remove instructions for the subtree including removal of the root
     * @param root node from which all children will be removed (root will also be removed)
     * @return string of remove instruction sequence (with a trailing comma!)
     * e.g.: Remove(1), Remove(2), Remove(3),
     * */
    public static String removeSubtree(Node root) {
        if (root == null)
            return "";
        StringBuilder sb = new StringBuilder();
        // remove bottom-up
        List<Integer> postOrder = Tree.getPostOrder(root);
        for (Integer index : postOrder) {
            sb.append(String.format("Remove(%s), ", index));
        }
        return sb.toString();
    }

    /**
     * Returns a sequence of create instructions for the subtree
     * @param root node which will be added to the parent and which will create subtree
     * @param parent node that will be the parent of root (if null, will be ignored!)
     * @return string of add instruction sequence (with a trailing comma!)
     * e.g.: Add(1, 2), Add(1, 3), Add(2, 3),
     * */
    public static String createSubtree(Node root, Node parent) {
        if (root == null)
            return "";
        StringBuilder sb = new StringBuilder();
        if (parent != null) {
            sb.append(String.format("Add(%d, %d), ", parent.getIndex(), root.getIndex()));
        }
        // add top-down
        Queue<Node> queue = new ArrayDeque<>();
        Node current = root;
        while (current != null) {
            for (Node child : current.getChildren()) {
                sb.append(String.format("Add(%d, %d), ", current.getIndex(), child.getIndex()));
                queue.add(child);
            }
            current = queue.poll();
        }
        return sb.toString();
    }

    /**
     * Function returns transformations that need to be performed on the givenTree in order to get desiredTree
     * Transformations are in form ADD(<ParentID>, <ChildID>) and Remove(<LeafID>)
     * @param givenTree tree from which transformations should take place
     * @param desiredTree tree to which given tree should be transformed
     * @return a sequence of transformation instructions (without a trailing comma)
     * E.g.: Remove(6), Remove(3), ADD(1, 6)
     * */
    public static String getTransformations(Tree givenTree, Tree desiredTree) {
        // handling edge cases with empty trees
        if (givenTree == null && desiredTree == null) {
            // empty to empty requires no transformations
            return "";
        } else if (givenTree == null) {
            // empty to non-empty needs to create the whole tree
            return createSubtree(desiredTree.getRoot(), null);
        } else if (desiredTree == null) {
            // non-empty to empty needs to remove the whole tree
            return removeSubtree(givenTree.getRoot());
        }

        StringBuilder transformation = new StringBuilder();
        StringBuilder additionBuffer = new StringBuilder();
        if (desiredTree.getRoot() == null
                || givenTree.getRoot() == null
                || (givenTree.getRoot().getIndex() != desiredTree.getRoot().getIndex())) {
            // if roots are different there is no way of making same tree without rebuilding it completely
            transformation.append(removeSubtree(givenTree.getRoot()));
            transformation.append(createSubtree(desiredTree.getRoot(), null));
            return transformation.toString();
        }
        // given and desired nodes are a tuple of nodes with same index being children of parent with same index
        Node givenNode, desiredNode;
        Queue<Map.Entry<Node, Node>> queue = new ArrayDeque<>();
        queue.add(new AbstractMap.SimpleImmutableEntry<>(desiredTree.getRoot(), givenTree.getRoot()));
        while (!queue.isEmpty()) {
            Map.Entry<Node, Node> desiredToGivenNodes = queue.poll();
            desiredNode = desiredToGivenNodes.getKey();
            givenNode = desiredToGivenNodes.getValue();
            // indexes of children of desired node
            HashMap<Integer, Node> desiredIndexesOfChildren = new HashMap<>();
            for (Node child : desiredNode.getChildren()) {
                desiredIndexesOfChildren.put(child.getIndex(), child);
            }
            for (Node child : givenNode.getChildren()) {
                if (!desiredIndexesOfChildren.containsKey(child.getIndex())) {
                    // node in given tree should not be in the desired tree
                    // whole subtree needs to be removed
                    transformation.append(removeSubtree(child));
                } else {
                    // node in given tree is presented in desired one and we can add it to queue
                    queue.add(new AbstractMap.SimpleImmutableEntry<>(desiredIndexesOfChildren.get(child.getIndex()), child));
                    // we don't need to additionally create this node, it's already in the given tree
                    desiredIndexesOfChildren.remove(child.getIndex());
                }
            }
            for (Map.Entry<Integer, Node> childEntry : desiredIndexesOfChildren.entrySet()) {
                // create subtrees that are left in map (i.e. are not present in givenSubtree)
                // add it to buffer to have no concurrency of remove/delete
                additionBuffer.append(createSubtree(childEntry.getValue(), desiredNode));
            }
        }
        transformation.append(additionBuffer);
        // remove last ", " from string
        if (transformation.length() > 2) {
            transformation.setLength(transformation.length() - 2);
        }
        return transformation.toString();
    }

    /**
     * Transforms a string tree description from the file into a Tree instance if it's described correctly
     * @param file Path to the tree description file
     * @return Tree if description was correct, null otherwise
     * @throws IllegalArgumentException if incorrect tree structure was provided i.e.:
     * tree description format was invalid, tree had cycles, tree was not connected (multiple different root nodes)
     * @throws NumberFormatException if some nodes index was not integer
     * @throws NodeAlreadyExistsException if some node with same index was added several times to different parents
     * @throws IOException if file operations failed
     * */
    public static Tree getTreeFromFile(String file) throws IOException {
        String t;
        try (BufferedReader reader = Files.newBufferedReader(Path.of(file))){
            t = reader.lines().collect(Collectors.joining());
        } catch (IOException e) {
            throw new IOException(e);
        }
        return Tree.getTreeFromString(t);
    }

    /**
     * Function computes transformation instruction sequence from given to desired tree
     * for the trees given in file1 and file2
     * @param file1 file where given tree is defined
     * @param file2 file where the desired tree is defined
     * @return a sequence of transformation instructions (without a trailing comma)
     * E.g.: Remove(6), Remove(3), ADD(1, 6)
     * */
    public static String getTransformationsFromFiles(String file1, String file2) throws IOException {
        StringBuilder result = new StringBuilder();
        Tree tree1 = getTreeFromFile(file1);
        Tree tree2 = getTreeFromFile(file2);
        result.append(tree1).append(tree2);
        return result.append(getTransformations(tree1, tree2)).toString();
    }

    /**
     * Returns a command name if command is supported, null otherwise
     * Check for commands existence in supportedCommandsMap, and it's compliance with a regex pattern
     * Command is accepted if there was a single occurrence of pattern in the command
     * @param command command to be checked
     * @return command name if command is supported, null otherwise
     * */
    private static String getCommand(String command) {
        command = command.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> nameToPattern : supportedCommandsMap.entrySet()) {
            if (command.startsWith(nameToPattern.getKey().toLowerCase(Locale.ROOT))) {
                return command.matches(nameToPattern.getValue())
                        ? nameToPattern.getKey().toLowerCase(Locale.ROOT)
                        : null;
            }
        }
        return null;
    }

    /**
     * Initializes supported commands in a form:
     * <command name>:<command regex>
     * */
    private static void initializeSupportedCommands() {
        supportedCommandsMap = new HashMap<>();
        // by convention, we use lower case for comand names here
        supportedCommandsMap.put("add", "( )*add\\((\\d)+,( )*(\\d)+\\)( )*");
        supportedCommandsMap.put("remove", "remove\\((\\d)+\\)");
        supportedCommandsMap.put("save", "save (.)*\\.(tt)");
        supportedCommandsMap.put("load", "load .*\\.(tt)");
    }

    /**
     * Handles interactive add command i.e. add node as child with childIndex to the node with parentIndex
     * @param command add command with parent and child indexes of node to be added
     * @param tree tree to which node should be added
     * @throws NodeAlreadyExistsException if node with childIndex already exists in the tree
     * @throws ParentDoesNotExistException if node with parentIndex doesn't exist in the tree
     * @throws IllegalStateException if some nodes index couldn't be found, or was invalid
     * @throws NumberFormatException if some nodes index was not integer
     * */
    private static void handleAdd(String command, Tree tree) {
        Pattern nums = Pattern.compile("(\\d)+");
        Matcher matcher = nums.matcher(command);
        int parent, child;
        try {
            if (!matcher.find()) {
                throw new IllegalStateException("Couldn't find parent index!");
            }
            parent = Integer.parseInt(matcher.group());
            if (!matcher.find()) {
                throw new IllegalStateException("Couldn't find child index!");
            }
            child = Integer.parseInt(matcher.group());
        } catch (NumberFormatException | IllegalStateException e) {
            throw new NumberFormatException("Could not parse index of a node: " + e.getMessage());
        }
        try {
            tree.addNode(parent, child);
        } catch (NodeAlreadyExistsException e) {
            throw new NodeAlreadyExistsException(e.getMessage());
        } catch (ParentDoesNotExistException e) {
            throw new ParentDoesNotExistException(e.getMessage());
        }
    }

    /**
     * Handles interactive remove command i.e. removes node with the given index from the tree
     * @param command remove command with index of node to be removed
     * @param tree tree from which node will be removed
     * @throws NotALeafException if node with index is not a leaf or is not presented in the tree (implies the first one)
     * @throws IllegalStateException if node index couldn't be found, or was invalid
     * @throws NumberFormatException if node index was not integer
     * */
    private static void handleRemove(String command, Tree tree) {
        Pattern nums = Pattern.compile("(\\d)+");
        Matcher matcher = nums.matcher(command);
        int index;
        try {
            if (!matcher.find()) {
                throw new IllegalStateException("Couldn't find node index!");
            }
            index = Integer.parseInt(matcher.group());
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Could not parse index of a node: " + e.getMessage());
        }
        try {
            tree.removeNode(index);
        } catch (NotALeafException e) {
            throw new NotALeafException(e.getMessage());
        }
    }

    /**
     * Handles interactive save command i.e. serializes tree onto the path provided in command
     * @param command save command with path for tree to be serialized as argument (MUST end with ".tt")
     * @param tree tree to be saved
     * @throws IllegalArgumentException if path was provided with incorrect extension
     * @throws IOException if file operations failed
     * */
    private static void handleSave(String command, Tree tree) throws IOException {
        String path = command.replace("save", "");
        path = Path.of(path).toString();
        try {
            tree.serializeTree(path);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Save failed! " + e.getMessage());
        } catch (IOException e) {
            throw new IOException("Save failed! " + e.getMessage());
        }
    }

    /**
     * Handles interactive load command i.e. performs tree deserialization for the given path
     * @param command load command with path to the tree description as argument
     * @return tree deserialized from command path
     * @throws IllegalArgumentException if path was provided with incorrect extension
     * @throws NumberFormatException if some node index was incorrectly provided
     * @throws NodeAlreadyExistsException if some node index was provided by two different parents
     * @throws IOException if file operations failed
     * */
    private static Tree handleLoad(String command) throws IOException {
        String path = command.replace("load", "");
        try {
            return Tree.deserializeTree(path);
        } catch (IOException e) {
            throw new IOException("Load failed! " + e.getMessage());
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Load failed! " + e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Load failed! " + e.getMessage());
        } catch (NodeAlreadyExistsException e) {
            throw new NodeAlreadyExistsException("Load failed! " + e.getMessage());
        }
    }

    /**
     * Handles interactive mode of the Tree Transformer. Following commands are supported:
     * ADD(<int: parent_index>, <int: child_index>), REMOVE(<int: leaf index>), SAVE <filename>.tt, LOAD <filename>.tt, EXIT
     * @param inputStream stream to get transformation instructions from
     * @param outputStream stream to print successful output information to
     * @param errorStream stream to print error information
     * */
    public static Tree handleInteractiveMode(InputStream inputStream, PrintStream outputStream, PrintStream errorStream, Tree tree) {
        outputStream.println(interactiveGreet);
        outputStream.println(helpMessage);
        initializeSupportedCommands();  //initialize supported commands
        Scanner scanner = new Scanner(inputStream);
        String command = scanner.nextLine();
        while (!command.equalsIgnoreCase(exitCommand)) {
            command = command.toLowerCase(Locale.ROOT);
            String commandName = getCommand(command);
            if (commandName == null) {
                errorStream.println("Invalid command received!");
            } else {
                try {
                    switch (commandName) {
                        case "add" -> handleAdd(command, tree);
                        case "remove" -> handleRemove(command, tree);
                        case "save" -> handleSave(command, tree);
                        case "load" -> tree = handleLoad(command);
                    }
                    outputStream.println(tree);
                } catch (Exception e) {
                    errorStream.println("Command execution failed! " + e.getMessage());
                }
            }
            outputStream.println(helpMessage);
            command = scanner.nextLine();
        }
        return tree;
    }

    public static void main(String[] args) {
        if (args.length == 2) {
            try {
                System.out.println(getTransformationsFromFiles(args[0], args[1]));
            } catch (Exception e) {
                System.err.println("Transformation failed! " + e.getMessage());
            }
        } else if (args.length == 0) {
            handleInteractiveMode(System.in, System.out, System.err, new Tree());
        }
        System.out.println("Thanks for using Tree Transformer! Hope to see you soon! :)");
    }
}
