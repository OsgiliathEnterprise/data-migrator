package net.osgiliath.migrator.core.exception;

public class PrimaryKeyMethodNotFoundException extends RawElementFieldOrMethodNotFoundException {
    public PrimaryKeyMethodNotFoundException(Exception e) {
        super(e);
    }
}
