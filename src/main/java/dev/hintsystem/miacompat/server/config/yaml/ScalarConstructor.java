package dev.hintsystem.miacompat.server.config.yaml;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;

public class ScalarConstructor extends Constructor {
    private final Map<Class<?>, Function<String, ?>> stringParsers = new HashMap<>();

    public ScalarConstructor(Class<?> theRoot, LoaderOptions loadingConfig) {
        super(theRoot, loadingConfig);
    }

    public <P> ScalarConstructor addStringParser(
        Class<P> type,
        Function<String, P> parser
    ) {
        stringParsers.put(type, parser);
        return this;
    }

    @Override
    protected Object constructObject(Node node) {
        if (node instanceof ScalarNode scalar) {
            Function<String, ?> parser = stringParsers.get(node.getType());
            if (parser != null) {
                return parser.apply(scalar.getValue());
            }
        }

        return super.constructObject(node);
    }
}
