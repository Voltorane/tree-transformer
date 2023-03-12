package main.java.exceptions;

public class ParentDoesNotExistException extends RuntimeException{
    public ParentDoesNotExistException(String s) {
        super(s);
    }
}
