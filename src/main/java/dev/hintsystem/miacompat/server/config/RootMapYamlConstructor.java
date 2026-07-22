package dev.hintsystem.miacompat.server.config;

import java.util.Map;
import java.util.function.Supplier;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;

public class RootMapYamlConstructor<R extends Map<String, V>, V> extends Constructor {
    public RootMapYamlConstructor(
        Supplier<R> rootFactory,
        Class<R> rootType,
        Class<V> valueType,
        LoaderOptions options
    ) {
        super(rootType, options);

        yamlConstructors.put(new Tag(rootType), new ConstructMapping() {
            @Override
            public Object construct(Node node) {
                MappingNode mapping = (MappingNode) node;
                R root = rootFactory.get();

                for (NodeTuple tuple : mapping.getValue()) {
                    String key = (String) constructObject(tuple.getKeyNode());

                    tuple.getValueNode().setType(valueType);

                    @SuppressWarnings("unchecked")
                    V value = (V) constructObject(tuple.getValueNode());

                    root.put(key, value);
                }

                return root;
            }
        });
    }
}
