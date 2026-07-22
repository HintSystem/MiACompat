package dev.hintsystem.miacompat.server.config.geary;

import java.util.List;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.PropertyUtils;

public class ItemYamlSchema {
    private Item setItem;
    public Observe observe;

    public static class Item {
        public String type;
        public String itemName;
        public String itemModel;
        public List<String> lore;
    }

    public static class Observe {
        public List<Action> itemLeftClick;
        public List<Action> itemRightClick;
    }

    public static class Action {
        public Ensure ensure;
        public Cooldown cooldown;
    }

    public static class Ensure {
        public List<FailAction> onFail;
    }

    public static class Cooldown {
        public String id;
        public String length;
        public String display;
    }

    public static class FailAction {
        public SendActionBar sendActionBar;
    }

    public static class SendActionBar {
        public String text;
    }

    public Item getItem() { return setItem; }

    public void setItem(Item item) { setItem = item; }

    public static Constructor constructor(LoaderOptions loaderOptions) {
        PropertyUtils propertyUtils = new PropertyUtils();
        propertyUtils.setSkipMissingProperties(true);

        TypeDescription itemDescription = new TypeDescription(ItemYamlSchema.class);
        itemDescription.substituteProperty("set.item", Item.class, "getItem", "setItem");

        Constructor constructor = new Constructor(ItemYamlSchema.class, loaderOptions);
        constructor.setPropertyUtils(propertyUtils);
        constructor.addTypeDescription(itemDescription);

        return constructor;
    }
}
