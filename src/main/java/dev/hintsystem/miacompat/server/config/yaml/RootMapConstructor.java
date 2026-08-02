package dev.hintsystem.miacompat.server.config.yaml;

import java.util.Map;
import java.util.function.Supplier;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.introspector.MissingProperty;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;

public class RootMapConstructor<R extends Map<String, V>, V> extends ScalarConstructor {
    public RootMapConstructor(
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
                    Node valueNode = tuple.getValueNode();

                    // handle class properties (anything that isn't part of the map entries)
                    try {
                        Property property = getPropertyUtils().getProperty(rootType, key);
                        if (!(property instanceof MissingProperty)) {
                            valueNode.setType(property.getType());
                            property.set(root, constructObject(valueNode));

                            continue;
                        }
                    } catch (YAMLException ignored) {}
                    catch (Exception e) {
                        throw new YAMLException("Cannot create property=" + key);
                    }

                    valueNode.setType(valueType);

                    @SuppressWarnings("unchecked")
                    V value = (V) constructObject(valueNode);
                    root.put(key, value);
                }

                return root;
            }
        });
    }
}
