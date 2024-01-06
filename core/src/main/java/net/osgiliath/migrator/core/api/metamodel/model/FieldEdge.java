package net.osgiliath.migrator.core.api.metamodel.model;

import org.jgrapht.graph.DefaultEdge;

import java.lang.reflect.Field;

public class FieldEdge extends DefaultEdge {
    private final Field metamodelField;

    public FieldEdge(Field metamodelField) {
        this.metamodelField = metamodelField;
    }

    public Field getMetamodelField() {
        return metamodelField;
    }

    public String getFieldName() {
        return metamodelField.getName();
    }
}
