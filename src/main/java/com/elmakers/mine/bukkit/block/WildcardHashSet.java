package com.elmakers.mine.bukkit.block;

import java.util.HashSet;

public class WildcardHashSet<T> extends HashSet {
    @Override
    public boolean contains(Object o) {
        return true;
    }
}
