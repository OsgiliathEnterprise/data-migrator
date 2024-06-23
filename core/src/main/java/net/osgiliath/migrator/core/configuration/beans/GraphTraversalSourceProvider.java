package net.osgiliath.migrator.core.configuration.beans;

/*-
 * #%L
 * data-migrator-core
 * %%
 * Copyright (C) 2024 Osgiliath Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import net.osgiliath.migrator.core.api.metamodel.model.MetamodelVertex;
import net.osgiliath.migrator.core.api.model.ModelElement;
import net.osgiliath.migrator.core.configuration.DataMigratorConfiguration;
import net.osgiliath.migrator.core.configuration.model.GraphDatasourceType;
import net.osgiliath.migrator.core.metamodel.impl.internal.jpa.model.JpaMetamodelVertex;
import org.apache.tinkerpop.gremlin.driver.Client;
import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.io.Buffer;
import org.apache.tinkerpop.gremlin.structure.io.IoRegistry;
import org.apache.tinkerpop.gremlin.structure.io.binary.DataType;
import org.apache.tinkerpop.gremlin.structure.io.binary.GraphBinaryReader;
import org.apache.tinkerpop.gremlin.structure.io.binary.GraphBinaryWriter;
import org.apache.tinkerpop.gremlin.structure.io.binary.TypeSerializerRegistry;
import org.apache.tinkerpop.gremlin.structure.io.binary.types.CustomTypeSerializer;
import org.apache.tinkerpop.gremlin.structure.io.binary.types.SimpleTypeSerializer;
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONMapper;
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONVersion;
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONXModuleV3;
import org.apache.tinkerpop.gremlin.structure.io.graphson.TypeInfo;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerIoRegistryV3;
import org.apache.tinkerpop.gremlin.util.MessageSerializer;
import org.apache.tinkerpop.gremlin.util.ser.GraphBinaryMessageSerializerV1;
import org.apache.tinkerpop.gremlin.util.ser.GraphSONMessageSerializerV3;
import org.apache.tinkerpop.shaded.jackson.core.JacksonException;
import org.apache.tinkerpop.shaded.jackson.core.JsonGenerator;
import org.apache.tinkerpop.shaded.jackson.core.JsonParser;
import org.apache.tinkerpop.shaded.jackson.core.JsonToken;
import org.apache.tinkerpop.shaded.jackson.core.type.WritableTypeId;
import org.apache.tinkerpop.shaded.jackson.databind.DeserializationContext;
import org.apache.tinkerpop.shaded.jackson.databind.JsonDeserializer;
import org.apache.tinkerpop.shaded.jackson.databind.ObjectMapper;
import org.apache.tinkerpop.shaded.jackson.databind.SerializerProvider;
import org.apache.tinkerpop.shaded.jackson.databind.jsontype.TypeSerializer;
import org.apache.tinkerpop.shaded.jackson.databind.module.SimpleModule;
import org.apache.tinkerpop.shaded.jackson.databind.ser.std.StdSerializer;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigInteger;

import static org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource.traversal;

/**
 * Tinkerpop Graph traversal source provider.
 */
@Component
public class GraphTraversalSourceProvider {

    /**
     * The data migrator configuration.
     */
    private final DataMigratorConfiguration dataMigratorConfiguration;

    /**
     * Constructor.
     *
     * @param dataMigratorConfiguration the data migrator configuration.
     */
    public GraphTraversalSourceProvider(DataMigratorConfiguration dataMigratorConfiguration) {
        this.dataMigratorConfiguration = dataMigratorConfiguration;
    }

    /**
     * Gets the graph traversal source (graph instance) in regards to configuration.
     *
     * @return the graph traversal source.
     */
    public GraphTraversalSource getGraph() {
        if (dataMigratorConfiguration.getGraphDatasource().getType() == GraphDatasourceType.EMBEDDED) {
            Graph graph = TinkerGraph.open();
            return traversal().withEmbedded(graph);
        } else if (dataMigratorConfiguration.getGraphDatasource().getType() == GraphDatasourceType.REMOTE) {
            IoRegistry registry = TinkerIoRegistryV3.instance();
            TypeSerializerRegistry typeSerializerRegistry = TypeSerializerRegistry.build().addRegistry(registry)
                    .addCustomType(ModelElement.class, new ModelElementBinarySerializer(DataType.CUSTOM))
                    .addCustomType(JpaMetamodelVertex.class, new JpaMetamodelBinarySerializer(DataType.CUSTOM))
                    .create();

            GraphSONMapper.Builder builder = GraphSONMapper.build().
                    typeInfo(TypeInfo.PARTIAL_TYPES)
                    // .addCustomModule(new JpaMetamodelSerializationModule())
                    .addCustomModule(GraphSONXModuleV3.build())
                    .version(GraphSONVersion.V3_0);
            MessageSerializer serializer = new GraphSONMessageSerializerV3(builder); //typeSerializerRegistry);
            Cluster cluster = Cluster.build(dataMigratorConfiguration.getGraphDatasource().getHost()).port(dataMigratorConfiguration.getGraphDatasource().getPort()).
                    serializer(new GraphBinaryMessageSerializerV1(typeSerializerRegistry)).
                    create();

            Client client = cluster.connect();
            return traversal().withRemote(DriverRemoteConnection.using(client));
        } else {
            throw new UnsupportedOperationException("Only embedded graph is supported for now");
        }
    }

    private class JpaMetamodelSerializationModule extends SimpleModule {
        public JpaMetamodelSerializationModule() {
            super();
            this.addSerializer(new JpaMetamodelSerializer());
            this.addSerializer(new ModelElementSerializer());
            this.addSerializer(new BigIntegerAsStringSerializer());
            this.addDeserializer(JpaMetamodelVertex.class, new JpaMetamodelDeserializer());
            this.addDeserializer(ModelElement.class, new ModelElementDeserializer());
            this.registerSubtypes(JpaMetamodelVertex.class, ModelElement.class, BigInteger.class);

            // this.addDeserializer(BigInteger.class, new NumberDeserializers.BigIntegerDeserializer());
        }
    }

    private class ModelElementDeserializer extends JsonDeserializer<ModelElement> {

        @Override
        public ModelElement deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
            ObjectMapper mapper = new ObjectMapper();
            String string = deserializationContext.readValue(jsonParser, String.class);
            ModelElement vtx = mapper.readValue(string, ModelElement.class);
            return vtx;
        }

    }


    private class JpaMetamodelDeserializer extends JsonDeserializer<JpaMetamodelVertex> {

        @Override
        public JpaMetamodelVertex deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
            ObjectMapper mapper = new ObjectMapper();
            String string = deserializationContext.readValue(jsonParser, String.class);
            JpaMetamodelVertex vtx = mapper.readValue(string, JpaMetamodelVertex.class);
            return vtx;
        }
    }

    private class JpaMetamodelSerializer extends StdSerializer<JpaMetamodelVertex> {
        protected JpaMetamodelSerializer() {
            super(JpaMetamodelVertex.class);
        }

        @Override
        public void serialize(JpaMetamodelVertex jpaMetamodelVertex, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            ObjectMapper mapper = new ObjectMapper();
            String toSend = mapper.writeValueAsString(jpaMetamodelVertex);
            jsonGenerator.writeString(toSend);
        }

        @Override
        public void serializeWithType(JpaMetamodelVertex value, JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSer) throws IOException {
            WritableTypeId typeIdDef = typeSer.writeTypePrefix(gen, typeSer.typeId(value, JsonToken.VALUE_STRING));
            serialize(value, gen, serializers);
            typeSer.writeTypeSuffix(gen, typeIdDef);
        }
    }


    private class ModelElementSerializer extends StdSerializer<ModelElement> {
        protected ModelElementSerializer() {
            super(ModelElement.class);
        }

        @Override
        public void serialize(ModelElement jpaMetamodelVertex, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            ObjectMapper mapper = new ObjectMapper();
            String toSend = mapper.writeValueAsString(jpaMetamodelVertex);
            jsonGenerator.writeString(toSend);
        }

        @Override
        public void serializeWithType(ModelElement value, JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSer) throws IOException {
            WritableTypeId typeIdDef = typeSer.writeTypePrefix(gen, typeSer.typeId(value, JsonToken.VALUE_STRING));
            serialize(value, gen, serializers);
            typeSer.writeTypeSuffix(gen, typeIdDef);
        }

    }

    private class BigIntegerAsStringSerializer extends StdSerializer<BigInteger> {

        protected BigIntegerAsStringSerializer() {
            super(BigInteger.class);
        }

        @Override
        public void serialize(BigInteger value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            BigInteger val = (BigInteger) value;
            String text = val.toString();
            gen.writeString(text);
        }

        @Override
        public void serializeWithType(BigInteger value, JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSer) throws IOException {
            WritableTypeId typeIdDef = typeSer.writeTypePrefix(gen, typeSer.typeId(value, JsonToken.VALUE_STRING));
            serialize(value, gen, serializers);
            typeSer.writeTypeSuffix(gen, typeIdDef);
        }

        public String valueToString(Object value) {
            throw new IllegalStateException();
        }
    }

    private class JpaMetamodelBinarySerializer extends SimpleTypeSerializer<JpaMetamodelVertex> implements CustomTypeSerializer<JpaMetamodelVertex> {
        public JpaMetamodelBinarySerializer(DataType dataType) {
            super(dataType);
        }

        @Override
        public String getTypeName() {
            return JpaMetamodelVertex.class.getTypeName();
        }

        @Override
        protected JpaMetamodelVertex readValue(Buffer buffer, GraphBinaryReader context) throws IOException {
            ObjectMapper mapper = new ObjectMapper();
            String readFromContext = context.readValue(buffer, String.class, false);
            JpaMetamodelVertex vtx = mapper.readValue(readFromContext, JpaMetamodelVertex.class);
            return vtx;
        }

        @Override
        protected void writeValue(JpaMetamodelVertex value, Buffer buffer, GraphBinaryWriter context) throws IOException {
            ObjectMapper mapper = new ObjectMapper();
            String toSend = mapper.writeValueAsString(value);
            context.write(toSend, buffer);
        }
    }

    private class ModelElementBinarySerializer extends SimpleTypeSerializer<ModelElement> implements CustomTypeSerializer<ModelElement> {

        public ModelElementBinarySerializer(DataType dataType) {
            super(dataType);
        }

        @Override
        public String getTypeName() {
            return ModelElement.class.getTypeName();
        }

        @Override
        protected ModelElement readValue(Buffer buffer, GraphBinaryReader context) throws IOException {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(metamodelVertexModule());
            String readFromContext = context.readValue(buffer, String.class, false);
            ModelElement vtx = mapper.readValue(readFromContext, ModelElement.class);
            return vtx;
        }

        @Override
        protected void writeValue(ModelElement value, Buffer buffer, GraphBinaryWriter context) throws IOException {
            ObjectMapper mapper = new ObjectMapper();
            String toSend = mapper.writeValueAsString(value);
            context.write(toSend, buffer);
        }
    }

    org.apache.tinkerpop.shaded.jackson.databind.Module metamodelVertexModule() {
        MetamodelVertexModule module = new MetamodelVertexModule();
        return module;
    }

    private class MetamodelVertexModule extends SimpleModule {
        public MetamodelVertexModule() {
            super();
            addDeserializer(MetamodelVertex.class, new MetamodelGraphDeserializer());
        }

    }
}
